package com.focus.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Окно «Чем ты сейчас занят?» с пятью кнопками. */
public class AskActivity extends Activity {

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

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 24);
        root.setPadding(p, p, p, p);
        sv.addView(root);

        boolean afterGirl = "girl".equals(Prefs.lastAnswerKey(this));
        String title = afterGirl ? "Ну что, начал заниматься?" : "Чем ты сейчас занят?";

        TextView clock = Ui.text(this, Scheduler.hm(System.currentTimeMillis()), 18, Ui.MUTED, false);
        root.addView(clock);
        TextView t = Ui.text(this, title, 28, Ui.TEXT, true);
        t.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        root.addView(t);

        for (int i = 0; i < Answers.KEYS.length; i++) {
            final String key = Answers.KEYS[i];
            root.addView(Ui.button(this, Answers.LABELS[i], Answers.COLORS[i], v -> {
                String msg = Answers.handle(AskActivity.this, key);
                Toast.makeText(AskActivity.this, msg, Toast.LENGTH_LONG).show();
                finish();
            }));
        }

        TextView hint = Ui.text(this,
                "Не ответишь — спрошу снова через 10 минут.", 14, Ui.MUTED, false);
        hint.setPadding(0, Ui.dp(this, 16), 0, 0);
        root.addView(hint);

        setContentView(sv);
    }
}
