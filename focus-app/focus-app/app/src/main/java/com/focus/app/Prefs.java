package com.focus.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Все настройки и состояние приложения. */
public final class Prefs {

    public static final int MODE_WORK = 0;
    public static final int MODE_SUNDAY = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_SKIP = 3;

    public static final String[] MODE_NAMES = {
            "Рабочий день (13:15–15:30 и 20:30–отбой)",
            "Рабочее воскресенье (с 13:00)",
            "Выходной (с 10:00)",
            "Пропустить день"
    };

    public static final String[] MODE_SHORT = {
            "Рабочий день",
            "Рабочее воскресенье",
            "Выходной",
            "Пропустить"
    };

    private Prefs() {
    }

    public static SharedPreferences get(Context c) {
        SharedPreferences p = c.getApplicationContext()
                .getSharedPreferences("focus", Context.MODE_PRIVATE);
        if (!p.getBoolean("seeded", false)) {
            // Особые даты, о которых рассказал пользователь.
            p.edit()
                    .putBoolean("seeded", true)
                    .putInt("date_2026-10-28", MODE_SKIP)
                    .putInt("date_2026-11-01", MODE_SUNDAY)
                    .apply();
        }
        return p;
    }

    public static boolean enabled(Context c) {
        return get(c).getBoolean("enabled", true);
    }

    public static void setEnabled(Context c, boolean v) {
        get(c).edit().putBoolean("enabled", v).apply();
    }

    /** Время отбоя в минутах от полуночи. */
    public static int bedtime(Context c) {
        return get(c).getInt("bedtime", 23 * 60 + 30);
    }

    public static void setBedtime(Context c, int minutes) {
        get(c).edit().putInt("bedtime", minutes).apply();
    }

    public static int defaultDayMode(int dow) {
        if (dow == Calendar.WEDNESDAY || dow == Calendar.SUNDAY) {
            return MODE_OFF;
        }
        return MODE_WORK;
    }

    public static int dayMode(Context c, int dow) {
        return get(c).getInt("dow_" + dow, defaultDayMode(dow));
    }

    public static void setDayMode(Context c, int dow, int mode) {
        get(c).edit().putInt("dow_" + dow, mode).apply();
    }

    public static String dateKey(Calendar d) {
        return String.format(Locale.US, "date_%04d-%02d-%02d",
                d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH));
    }

    public static int modeFor(Context c, Calendar day) {
        SharedPreferences p = get(c);
        String k = dateKey(day);
        if (p.contains(k)) {
            return p.getInt(k, MODE_WORK);
        }
        return dayMode(c, day.get(Calendar.DAY_OF_WEEK));
    }

    public static void setDateOverride(Context c, Calendar day, int mode) {
        get(c).edit().putInt(dateKey(day), mode).apply();
    }

    public static void removeKey(Context c, String key) {
        get(c).edit().remove(key).apply();
    }

    /** Особые даты: ключ "date_yyyy-MM-dd" -> режим, по порядку. */
    public static TreeMap<String, Integer> overrides(Context c) {
        TreeMap<String, Integer> out = new TreeMap<>();
        for (Map.Entry<String, ?> e : get(c).getAll().entrySet()) {
            if (e.getKey().startsWith("date_") && e.getValue() instanceof Integer) {
                out.put(e.getKey(), (Integer) e.getValue());
            }
        }
        return out;
    }

    public static long nextAsk(Context c) {
        return get(c).getLong("next_ask", -1L);
    }

    public static void setNextAsk(Context c, long t) {
        get(c).edit().putLong("next_ask", t).apply();
    }

    public static String lastAnswerKey(Context c) {
        return get(c).getString("last_answer", "");
    }

    public static long lastAnswerAt(Context c) {
        return get(c).getLong("last_answer_at", 0L);
    }

    public static void setLastAnswer(Context c, String key, long at) {
        get(c).edit().putString("last_answer", key).putLong("last_answer_at", at).apply();
    }
}
