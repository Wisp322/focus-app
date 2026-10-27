package com.focus.app;

import android.content.Context;

/** Пять вариантов ответа и что делать после каждого. */
public final class Answers {

    public static final String[] KEYS = {"phone", "games", "chores", "girl", "project"};

    public static final String[] LABELS = {
            "📱  Телефон / YouTube / соцсети",
            "🎮  PlayStation / игры",
            "🧹  Домашние дела",
            "❤️  С девушкой",
            "💻  Проекты / учёба"
    };

    public static final int[] COLORS = {
            0xFFC0392B, // телефон — красный
            0xFF8E44AD, // игры — фиолетовый
            0xFF2E86C1, // дела — синий
            0xFFD6457A, // девушка — розовый
            0xFF239B56  // проекты — зелёный
    };

    private Answers() {
    }

    public static String label(String key) {
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) {
                return LABELS[i];
            }
        }
        return "—";
    }

    /** Обработать ответ, перепланировать, вернуть сообщение для показа. */
    public static String handle(Context c, String key) {
        long now = System.currentTimeMillis();
        long next = Scheduler.afterAnswer(c, key, now);
        Prefs.setLastAnswer(c, key, now);
        Scheduler.scheduleAsk(c, next);
        Notifier.cancelAsk(c);
        String when = Scheduler.formatWhen(next);
        switch (key) {
            case "phone":
                return "Завязывай с телефоном 🙂 Спрошу снова " + when;
            case "games":
                return "Окей, поиграй немного. Спрошу снова " + when;
            case "chores":
                return "Отлично! Спрошу снова " + when;
            case "girl":
                return "Хорошего времени ❤️ Спрошу " + when + ", начал ли ты заниматься";
            case "project":
                return "Красавчик! 💪 Не беспокою до отбоя";
            default:
                return "Спрошу снова " + when;
        }
    }
}
