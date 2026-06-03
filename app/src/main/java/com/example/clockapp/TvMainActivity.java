package com.example.clockapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TvMainActivity extends AppCompatActivity {

    private static final String PREFS         = "iptv";
    private static final String KEY_URL       = "m3u_url";
    private static final String KEY_LAST_URL  = "last_ch_url";
    private static final String KEY_LAST_NAME = "last_ch_name";

    private RecyclerView recyclerView;
    private View emptyView;
    private ProgressBar progressBar;
    private EditText searchEdit;
    private TextView channelCount;
    private TvChannelAdapter adapter;

    private final List<Channel> allChannels = new ArrayList<>();
    private final ExecutorService executor   = Executors.newSingleThreadExecutor();
    private final Handler mainHandler        = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_main);

        recyclerView  = findViewById(R.id.recyclerView);
        emptyView     = findViewById(R.id.emptyView);
        progressBar   = findViewById(R.id.progressBar);
        searchEdit    = findViewById(R.id.searchEdit);
        channelCount  = findViewById(R.id.channelCount);

        adapter = new TvChannelAdapter((channel, position) -> {
            prefs().edit()
                    .putString(KEY_LAST_URL, channel.url)
                    .putString(KEY_LAST_NAME, channel.name)
                    .apply();
            ChannelRepository.get().setCurrentIndex(position);
            Intent intent = new Intent(this, TvPlayerActivity.class);
            intent.putExtra("position", position);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                applyFilter(s.toString());
            }
        });

        String savedUrl = prefs().getString(KEY_URL, null);
        if (savedUrl != null && !ChannelRepository.get().isEmpty()) {
            // Lista já em memória — voltou do player, não faz auto-resume
            allChannels.addAll(ChannelRepository.get().getChannels());
            applyFilter("");
            showList();
        } else if (savedUrl != null) {
            // Primeira carga na sessão → carrega + auto-resume
            loadPlaylist(savedUrl, /*autoResume=*/true);
        } else {
            showEmpty();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.setSelectedIndex(ChannelRepository.get().getCurrentIndex());
    }

    // ── Teclas ──────────────────────────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitMenu();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            showSettingsMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ── Menus ────────────────────────────────────────────────────────────────────

    private void showExitMenu() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Editar lista M3U", "Sair"}, (dialog, which) -> {
                    if (which == 0) showUrlDialog();
                    else finishAffinity();
                })
                .show();
    }

    private void showSettingsMenu() {
        String[] options = {"Adicionar / trocar lista M3U", "Recarregar lista", "Limpar lista"};
        new AlertDialog.Builder(this)
                .setTitle("Configurações")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showUrlDialog(); break;
                        case 1:
                            String url = prefs().getString(KEY_URL, null);
                            if (url != null) loadPlaylist(url, false);
                            break;
                        case 2:
                            prefs().edit().remove(KEY_URL).apply();
                            allChannels.clear();
                            ChannelRepository.get().setChannels(allChannels);
                            adapter.setChannels(allChannels);
                            showEmpty();
                            break;
                    }
                })
                .show();
    }

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
                    Toast.makeText(this, result.size() + " canais carregados",
                            Toast.LENGTH_SHORT).show();

                    if (autoResume) resumeLastChannel(result);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showEmpty();
                    Toast.makeText(this, "Erro: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void resumeLastChannel(List<Channel> channels) {
        String lastUrl = prefs().getString(KEY_LAST_URL, null);
        if (lastUrl == null) return;
        for (int i = 0; i < channels.size(); i++) {
            if (channels.get(i).url.equals(lastUrl)) {
                ChannelRepository.get().setCurrentIndex(i);
                Intent intent = new Intent(this, TvPlayerActivity.class);
                intent.putExtra("position", i);
                startActivity(intent);
                return;
            }
        }
    }

    // ── Filtro ───────────────────────────────────────────────────────────────────

    private void applyFilter(String query) {
        List<Channel> filtered;
        if (query.isEmpty()) {
            filtered = allChannels;
        } else {
            String lower = query.toLowerCase();
            filtered = new ArrayList<>();
            for (Channel ch : allChannels) {
                if (ch.name.toLowerCase().contains(lower)
                        || ch.group.toLowerCase().contains(lower)) {
                    filtered.add(ch);
                }
            }
        }
        adapter.setChannels(filtered);
        channelCount.setText(filtered.size() + " canais");
        adapter.setSelectedIndex(ChannelRepository.get().getCurrentIndex());
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
