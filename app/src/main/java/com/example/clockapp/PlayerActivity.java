package com.example.clockapp;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {

    private static final String PREFS         = "iptv";
    private static final String KEY_URL       = "m3u_url";
    private static final String KEY_LAST_URL  = "last_ch_url";
    private static final String KEY_LAST_NAME = "last_ch_name";

    private ExoPlayer player;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler      = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        setContentView(R.layout.activity_player);

        String url  = getIntent().getStringExtra("url");
        String name = getIntent().getStringExtra("name");

        // Salva o último canal assistido
        if (url != null) {
            prefs().edit()
                    .putString(KEY_LAST_URL, url)
                    .putString(KEY_LAST_NAME, name != null ? name : "")
                    .apply();
        }

        TextView nameView = findViewById(R.id.channelName);
        nameView.setText(name != null ? name : "");

        PlayerView playerView = findViewById(R.id.playerView);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Erro ao reproduzir: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
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

    // ── Editar lista direto do player ────────────────────────────────────────────

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
                mainHandler.post(() -> {
                    ChannelRepository.get().setChannels(result);
                    Toast.makeText(this,
                            result.size() + " canais carregados", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Erro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
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
        player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
        executor.shutdown();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }
}
