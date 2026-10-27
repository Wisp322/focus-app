package com.focus.app;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Полноэкранный будильник «Пора спать». */
public class BedtimeActivity extends Activity {

    public static final String EXTRA_TEST = "test";

    private MediaPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final boolean test = getIntent().getBooleanExtra(EXTRA_TEST, false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF0B1026);
        int p = Ui.dp(this, 28);
        root.setPadding(p, p, p, p);

        TextView moon = Ui.text(this, "🌙", 72, Ui.TEXT, false);
        moon.setGravity(Gravity.CENTER);
        root.addView(moon);

        TextView title = Ui.text(this, "Пора спать", 34, Ui.TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = Ui.text(this,
                "Сейчас " + Scheduler.hm(System.currentTimeMillis())
                        + ".\nОтложи телефон — завтра днём не будет клонить в сон.",
                17, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 24));
        root.addView(sub);

        root.addView(Ui.button(this, "😴  Иду спать", 0xFF3949AB, v -> {
            stopSound();
            Notifier.cancel(this, Notifier.ID_BED);
            Toast.makeText(this, "Спокойной ночи 🌙", Toast.LENGTH_SHORT).show();
            finish();
        }));

        if (!test) {
            root.addView(Ui.button(this, "⏰  Ещё 5 минут", 0xFF37474F, v -> {
                stopSound();
                Notifier.cancel(this, Notifier.ID_BED);
                Scheduler.setAlarm(this, Scheduler.REQ_BED, Scheduler.ACTION_BED,
                        System.currentTimeMillis() + 5 * Scheduler.MIN);
                Toast.makeText(this, "Ладно, 5 минут — и спать!", Toast.LENGTH_SHORT).show();
                finish();
            }));
        }

        setContentView(root);
        startSound();
    }

    private void startSound() {
        try {
            Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (u == null) {
                u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (u == null) {
                return;
            }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, u);
            player.setLooping(true);
            player.prepare();
            player.start();
            handler.postDelayed(this::stopSound, 60_000L);
        } catch (Exception e) {
            player = null;
        }
    }

    private void stopSound() {
        handler.removeCallbacksAndMessages(null);
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
                // уже остановлен
            }
            player.release();
            player = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopSound();
        super.onDestroy();
    }
}
