package com.example.clockapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS          = "iptv";
    private static final String KEY_URL        = "m3u_url";
    private static final String KEY_LAST_URL   = "last_ch_url";
    private static final String KEY_LAST_NAME  = "last_ch_name";

    private RecyclerView recyclerView;
    private View emptyView;
    private ProgressBar progressBar;
    private EditText searchEdit;
    private ChannelAdapter adapter;

    private final List<Channel> allChannels = new ArrayList<>();
    private final ExecutorService executor   = Executors.newSingleThreadExecutor();
    private final Handler mainHandler        = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView    = findViewById(R.id.emptyView);
        progressBar  = findViewById(R.id.progressBar);
        searchEdit   = findViewById(R.id.searchEdit);

        adapter = new ChannelAdapter(channel -> {
            prefs().edit()
                    .putString(KEY_LAST_URL, channel.url)
                    .putString(KEY_LAST_NAME, channel.name)
                    .apply();
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("url", channel.url);
            intent.putExtra("name", channel.name);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
        });

        String savedUrl = prefs().getString(KEY_URL, null);
        if (savedUrl != null) {
            loadPlaylist(savedUrl, /*autoResume=*/true);
        } else {
            showEmpty();
        }
    }

    // ── Menu de opções ──────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_playlist) {
            showUrlDialog();
            return true;
        } else if (id == R.id.action_reload) {
            String url = prefs().getString(KEY_URL, null);
            if (url != null) loadPlaylist(url, false);
            else Toast.makeText(this, "Nenhuma lista configurada", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_clear_playlist) {
            prefs().edit().remove(KEY_URL).apply();
            allChannels.clear();
            adapter.setChannels(allChannels);
            showEmpty();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Botão voltar → menu flutuante ────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        showExitMenu();
    }

    private void showExitMenu() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Editar lista M3U", "Sair"}, (dialog, which) -> {
                    if (which == 0) showUrlDialog();
                    else finishAffinity();
                })
                .show();
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────────

    private void showUrlDialog() {
        EditText input = new EditText(this);
        input.setHint("https://exemplo.com/lista.m3u");
        String current = prefs().getString(KEY_URL, "");
        input.setText(current);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("URL da lista M3U")
                .setView(input)
                .setPositiveButton("Carregar", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        prefs().edit().putString(KEY_URL, url).apply();
                        loadPlaylist(url, false);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ── Carregamento da lista ────────────────────────────────────────────────────

    private void loadPlaylist(String url, boolean autoResume) {
        showLoading();
        executor.execute(() -> {
            try {
                List<Channel> result = M3uParser.parse(url);
                mainHandler.post(() -> {
                    allChannels.clear();
                    allChannels.addAll(result);
                    ChannelRepository.get().setChannels(result);
                    searchEdit.setText("");
                    applyFilter("");
                    if (allChannels.isEmpty()) showEmpty();
                    else showList();
                    Toast.makeText(this,
                            result.size() + " canais carregados", Toast.LENGTH_SHORT).show();

                    if (autoResume) resumeLastChannel(result);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showEmpty();
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void resumeLastChannel(List<Channel> channels) {
        String lastUrl = prefs().getString(KEY_LAST_URL, null);
        if (lastUrl == null) return;
        for (Channel ch : channels) {
            if (ch.url.equals(lastUrl)) {
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("url", ch.url);
                intent.putExtra("name", ch.name);
                startActivity(intent);
                return;
            }
        }
    }

    // ── Filtro ───────────────────────────────────────────────────────────────────

    private void applyFilter(String query) {
        if (query.isEmpty()) {
            adapter.setChannels(allChannels);
            return;
        }
        String lower = query.toLowerCase();
        List<Channel> filtered = new ArrayList<>();
        for (Channel ch : allChannels) {
            if (ch.name.toLowerCase().contains(lower)
                    || ch.group.toLowerCase().contains(lower)) {
                filtered.add(ch);
            }
        }
        adapter.setChannels(filtered);
    }

    // ── Estados visuais ──────────────────────────────────────────────────────────

    private void showEmpty() {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void showLoading() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void showList() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
