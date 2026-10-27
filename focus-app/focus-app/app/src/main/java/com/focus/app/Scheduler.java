package com.focus.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Расписание: когда спрашивать и когда звонить отбой. */
public final class Scheduler {

    public static final String ACTION_ASK = "com.focus.app.ASK";
    public static final String ACTION_BED = "com.focus.app.BED";
    public static final int REQ_ASK = 1;
    public static final int REQ_BED = 2;
    public static final long MIN = 60_000L;

    private Scheduler() {
    }

    public static Calendar midnight(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** Момент времени: день + минуты от полуночи (можно больше 24 часов). */
    public static long at(Calendar day, int minutes) {
        Calendar c = (Calendar) day.clone();
        c.add(Calendar.MINUTE, minutes);
        return c.getTimeInMillis();
    }

    /** Отбой после полуночи (например 00:30) считается концом предыдущего дня. */
    public static int effectiveBed(Context c) {
        int bed = Prefs.bedtime(c);
        return bed < 12 * 60 ? bed + 24 * 60 : bed;
    }

    /** Окна, когда я дома и приложение может спрашивать. */
    public static List<long[]> windows(Context c, Calendar day) {
        List<long[]> w = new ArrayList<>();
        int bed = effectiveBed(c);
        int mode = Prefs.modeFor(c, day);
        if (mode == Prefs.MODE_WORK) {
            add(w, day, 13 * 60 + 15, 15 * 60 + 30);
            add(w, day, 20 * 60 + 30, bed);
        } else if (mode == Prefs.MODE_SUNDAY) {
            add(w, day, 13 * 60, bed);
        } else if (mode == Prefs.MODE_OFF) {
            add(w, day, 10 * 60, bed);
        }
        return w;
    }

    private static void add(List<long[]> w, Calendar day, int start, int end) {
        if (end > start) {
            w.add(new long[]{at(day, start), at(day, end)});
        }
    }

    /** Ближайший момент не раньше from, попадающий в окно. -1 если нет. */
    public static long nextAskFrom(Context c, long from) {
        Calendar day = midnight(from);
        day.add(Calendar.DAY_OF_MONTH, -1);
        for (int i = 0; i < 10; i++) {
            for (long[] win : windows(c, day)) {
                if (from < win[1]) {
                    return Math.max(from, win[0]);
                }
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return -1L;
    }

    /** Ближайший отбой после from. */
    public static long nextBedtime(Context c, long from) {
        Calendar day = midnight(from);
        day.add(Calendar.DAY_OF_MONTH, -1);
        int bed = effectiveBed(c);
        for (int i = 0; i < 10; i++) {
            if (Prefs.modeFor(c, day) != Prefs.MODE_SKIP) {
                long t = at(day, bed);
                if (t > from) {
                    return t;
                }
            }
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        return -1L;
    }

    /** Когда спросить снова после ответа. */
    public static long afterAnswer(Context c, String answer, long now) {
        long base;
        switch (answer) {
            case "phone":
                base = now + 10 * MIN;
                break;
            case "games":
            case "chores":
                base = now + 30 * MIN;
                break;
            case "girl": {
                long t22 = at(midnight(now), 22 * 60);
                if (t22 > now && t22 - now <= 3 * 60 * MIN) {
                    base = t22;
                } else {
                    base = now + 60 * MIN;
                }
                break;
            }
            case "project": {
                long bed = nextBedtime(c, now);
                base = bed > 0 ? bed + MIN : now + 60 * MIN;
                break;
            }
            default:
                base = now + 10 * MIN;
        }
        return nextAskFrom(c, base);
    }

    public static void scheduleAsk(Context c, long t) {
        Prefs.setNextAsk(c, t);
        setAlarm(c, REQ_ASK, ACTION_ASK, t);
    }

    public static void scheduleBedtime(Context c, long t) {
        setAlarm(c, REQ_BED, ACTION_BED, t);
    }

    /**
     * Пересчитать всё.
     * @param keepSilence true — не сбрасывать «не беспокоить» после ответа.
     */
    public static void rescheduleAll(Context c, boolean keepSilence) {
        long now = System.currentTimeMillis();
        if (!Prefs.enabled(c)) {
            scheduleAsk(c, -1L);
            scheduleBedtime(c, -1L);
            return;
        }
        long stored = Prefs.nextAsk(c);
        long base = (keepSilence && stored > now) ? stored : now + MIN;
        scheduleAsk(c, nextAskFrom(c, base));
        scheduleBedtime(c, nextBedtime(c, now));
    }

    public static void setAlarm(Context c, int req, String action, long t) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Intent i = new Intent(c, AlarmReceiver.class).setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (am == null) {
            return;
        }
        if (t <= 0) {
            am.cancel(pi);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }

    /** «в 14:30», «завтра в 13:15», «05.11 в 10:00». */
    public static String formatWhen(long t) {
        if (t <= 0) {
            return "—";
        }
        String hm = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(t));
        long today = midnight(System.currentTimeMillis()).getTimeInMillis();
        Calendar tomorrowCal = midnight(System.currentTimeMillis());
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
        long tomorrow = tomorrowCal.getTimeInMillis();
        tomorrowCal.add(Calendar.DAY_OF_MONTH, 1);
        long after = tomorrowCal.getTimeInMillis();
        if (t >= today && t < tomorrow) {
            return "в " + hm;
        }
        if (t >= tomorrow && t < after) {
            return "завтра в " + hm;
        }
        String d = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(t));
        return d + " в " + hm;
    }

    public static String hm(long t) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(t));
    }
}
