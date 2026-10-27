package com.focus.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;

/** Уведомления: вопрос «Чем занят?» и отбой. */
public final class Notifier {

    public static final String CH_ASK = "ask_v1";
    public static final String CH_BED = "bed_v1";
    public static final int ID_ASK = 1;
    public static final int ID_BED = 2;

    private Notifier() {
    }

    public static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel ask = new NotificationChannel(CH_ASK,
                "Вопрос «Чем ты занят?»", NotificationManager.IMPORTANCE_HIGH);
        ask.enableVibration(true);
        ask.setVibrationPattern(new long[]{0, 300, 200, 300});
        ask.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ask);

        NotificationChannel bed = new NotificationChannel(CH_BED,
                "Отбой", NotificationManager.IMPORTANCE_HIGH);
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarm != null) {
            bed.setSound(alarm, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
        bed.enableVibration(true);
        bed.setVibrationPattern(new long[]{0, 600, 300, 600, 300, 600});
        bed.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(bed);
    }

    public static void showAsk(Context c) {
        channels(c);
        boolean afterGirl = "girl".equals(Prefs.lastAnswerKey(c));
        String title = afterGirl ? "Ну что, начал заниматься?" : "Чем ты сейчас занят?";

        Intent open = new Intent(c, AskActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CH_ASK)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText("Нажми, чтобы ответить")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .addAction(action(c, "phone", "📱 Телефон", 21))
                .addAction(action(c, "games", "🎮 Игры", 22))
                .addAction(action(c, "project", "💻 Учёба", 23))
                .build();
        notify(c, ID_ASK, n);
    }

    public static void showBedtime(Context c) {
        channels(c);
        Intent open = new Intent(c, BedtimeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 11, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CH_BED)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("🌙 Пора спать")
                .setContentText("Отложи телефон. Завтра скажешь себе спасибо.")
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .build();
        notify(c, ID_BED, n);
    }

    private static Notification.Action action(Context c, String key, String title, int req) {
        Intent i = new Intent(c, AnswerReceiver.class)
                .setAction(AnswerReceiver.ACTION)
                .putExtra(AnswerReceiver.EXTRA, key);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_notify), title, pi).build();
    }

    private static void notify(Context c, int id, Notification n) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        try {
            nm.notify(id, n);
        } catch (SecurityException ignored) {
            // нет разрешения на уведомления
        }
    }

    public static void cancelAsk(Context c) {
        cancel(c, ID_ASK);
    }

    public static void cancel(Context c, int id) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel(id);
        }
    }
}
