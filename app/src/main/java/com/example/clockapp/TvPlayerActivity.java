package com.example.clockapp;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TvPlayerActivity extends AppCompatActivity {

    private static final String PREFS         = "iptv";
    private static final String KEY_URL       = "m3u_url";
    private static final String KEY_LAST_URL  = "last_ch_url";
    private static final String KEY_LAST_NAME = "last_ch_name";

    private ExoPlayer player;
    private View sidebarView;
    private View osdView;
    private View hintBar;
    private TextView osdNumber;
    private TextView osdName;
    private TextView osdGroup;
    private RecyclerView sidebarList;
    private TvChannelAdapter sidebarAdapter;

    private List<Channel> channels;
    private int currentIndex      = 0;
    private int sidebarFocusIndex = 0;
    private boolean sidebarVisible = false;

    private final Handler handler            = new Handler(Looper.getMainLooper());
    private final ExecutorService executor   = Executors.newSingleThreadExecutor();
    private final Runnable hideOsdRunnable   = () -> osdView.setVisibility(View.GONE);
    private final Runnable hideHintRunnable  = () -> hintBar.setVisibility(View.GONE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_tv_player);

        channels     = ChannelRepository.get().getChannels();
        currentIndex = getIntent().getIntExtra("position", 0);

        osdView     = findViewById(R.id.osdView);
        osdNumber   = findViewById(R.id.osdChannelNumber);
        osdName     = findViewById(R.id.osdChannelName);
        osdGroup    = findViewById(R.id.osdChannelGroup);
        sidebarView = findViewById(R.id.sidebarView);
        hintBar     = findViewById(R.id.hintBar);
        sidebarList = findViewById(R.id.sidebarList);

        sidebarAdapter = new TvChannelAdapter((channel, pos) -> {
            currentIndex = pos;
            playChannel(currentIndex);
            hideSidebar();
        });
        sidebarAdapter.setSidebarMode(true);
        sidebarList.setLayoutManager(new LinearLayoutManager(this));
        sidebarList.setAdapter(sidebarAdapter);
        sidebarAdapter.setChannels(channels);

        PlayerView playerView = findViewById(R.id.playerView);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                if (currentIndex < channels.size()) {
                    showOsd(currentIndex, "Erro ao reproduzir stream");
                }
            }
        });

        playChannel(currentIndex);
        handler.postDelayed(hideHintRunnable, 5000);
    }

    // ── Controle remoto ──────────────────────────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_CHANNEL_UP:
                if (sidebarVisible) moveSidebarFocus(-1);
                else switchChannel(-1);
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                if (sidebarVisible) moveSidebarFocus(1);
                else switchChannel(1);
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (sidebarVisible) {
                    currentIndex = sidebarFocusIndex;
                    playChannel(currentIndex);
                    hideSidebar();
                } else {
                    showSidebar();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!sidebarVisible) showSidebar();
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (sidebarVisible) hideSidebar();
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (sidebarVisible) {
                    hideSidebar();
                } else {
                    showExitMenu();
                }
                return true;

            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SETTINGS:
                toggleHint();
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    // ── Menu flutuante (BACK sem sidebar) ────────────────────────────────────────

    private void showExitMenu() {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"Editar lista M3U", "Sair"}, (dialog, which) -> {
                    if (which == 0) showUrlDialog();
                    else finishAffinity();
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
                        reloadPlaylist(url);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void reloadPlaylist(String url) {
        Toast.makeText(this, "Carregando lista...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                List<Channel> result = M3uParser.parse(url);
                handler.post(() -> {
                    ChannelRepository.get().setChannels(result);
                    channels = ChannelRepository.get().getChannels();
                    sidebarAdapter.setChannels(channels);
                    // Garante que currentIndex ainda é válido
                    if (currentIndex >= channels.size()) currentIndex = 0;
                    sidebarAdapter.setSelectedIndex(currentIndex);
                    Toast.makeText(this, result.size() + " canais carregados",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                handler.post(() ->
                        Toast.makeText(this, "Erro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }

    // ── Navegação de canais ──────────────────────────────────────────────────────

    private void switchChannel(int delta) {
        if (channels.isEmpty()) return;
        currentIndex = (currentIndex + delta + channels.size()) % channels.size();
        playChannel(currentIndex);
    }

    private void moveSidebarFocus(int delta) {
        if (channels.isEmpty()) return;
        sidebarFocusIndex = Math.max(0, Math.min(channels.size() - 1,
                sidebarFocusIndex + delta));
        sidebarAdapter.setFocusedIndex(sidebarFocusIndex);
        sidebarList.scrollToPosition(sidebarFocusIndex);
    }

    private void playChannel(int index) {
        if (channels.isEmpty() || index < 0 || index >= channels.size()) return;
        Channel ch = channels.get(index);
        player.stop();
        player.setMediaItem(MediaItem.fromUri(ch.url));
        player.prepare();
        player.play();
        sidebarAdapter.setSelectedIndex(index);
        ChannelRepository.get().setCurrentIndex(index);

        // Persiste o último canal assistido
        prefs().edit()
                .putString(KEY_LAST_URL, ch.url)
                .putString(KEY_LAST_NAME, ch.name)
                .apply();

        showOsd(index, null);
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────────

    private void showSidebar() {
        sidebarFocusIndex = currentIndex;
        sidebarAdapter.setFocusedIndex(sidebarFocusIndex);
        sidebarList.scrollToPosition(sidebarFocusIndex);
        sidebarView.setVisibility(View.VISIBLE);
        sidebarVisible = true;
    }

    private void hideSidebar() {
        sidebarView.setVisibility(View.GONE);
        sidebarAdapter.setFocusedIndex(-1);
        sidebarVisible = false;
    }

    // ── OSD e dicas ──────────────────────────────────────────────────────────────

    private void showOsd(int index, String overrideName) {
        Channel ch = channels.get(index);
        osdNumber.setText("Canal " + (index + 1));
        osdName.setText(overrideName != null ? overrideName : ch.name);
        osdGroup.setText(ch.group != null && !ch.group.isEmpty() ? ch.group : "");
        osdView.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideOsdRunnable);
        handler.postDelayed(hideOsdRunnable, 3500);
    }

    private void toggleHint() {
        handler.removeCallbacks(hideHintRunnable);
        if (hintBar.getVisibility() == View.VISIBLE) {
            hintBar.setVisibility(View.GONE);
        } else {
            hintBar.setVisibility(View.VISIBLE);
            handler.postDelayed(hideHintRunnable, 5000);
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────────

    @Override
    protected void onPause() {
        super.onPause();
        player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        player.release();
        executor.shutdown();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }
}
