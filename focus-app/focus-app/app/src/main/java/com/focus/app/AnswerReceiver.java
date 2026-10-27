package com.focus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Ответ кнопкой прямо из уведомления. */
public class AnswerReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.focus.app.ANSWER";
    public static final String EXTRA = "answer";

    @Override
    public void onReceive(Context c, Intent intent) {
        String key = intent.getStringExtra(EXTRA);
        if (key == null) {
            return;
        }
        String msg = Answers.handle(c, key);
        Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }
}
