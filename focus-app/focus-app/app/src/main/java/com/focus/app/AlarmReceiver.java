package com.focus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Срабатывание будильников: вопрос и отбой. */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (action == null || !Prefs.enabled(c)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (Scheduler.ACTION_ASK.equals(action)) {
            Notifier.showAsk(c);
            // Если не ответишь — спрошу снова через 10 минут.
            Scheduler.scheduleAsk(c, Scheduler.nextAskFrom(c, now + 10 * Scheduler.MIN));
        } else if (Scheduler.ACTION_BED.equals(action)) {
            Notifier.cancelAsk(c);
            Notifier.showBedtime(c);
            Scheduler.scheduleBedtime(c, Scheduler.nextBedtime(c, now + Scheduler.MIN));
        }
    }
}
