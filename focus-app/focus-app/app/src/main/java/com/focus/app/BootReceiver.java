package com.focus.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки или смены времени — заново ставим будильники. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        Scheduler.rescheduleAll(c, true);
    }
}
