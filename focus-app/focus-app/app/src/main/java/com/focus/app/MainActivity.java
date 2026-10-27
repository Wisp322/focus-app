package com.focus.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/** Главный экран: статус, разрешения и настройки. */
public class MainActivity extends Activity {

    private static final int[] DOW = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };
    private static final String[] DOW_NAMES = {
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
    };

    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notifier.channels(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        root.setPadding(p, Ui.dp(this, 28), p, Ui.dp(this, 40));
        sv.addView(root);
        setContentView(sv);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Scheduler.rescheduleAll(this, true);
        build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        build();
    }

    private void changed() {
        Scheduler.rescheduleAll(this, false);
        root.post(this::build);
    }

    private void build() {
        root.removeAllViews();

        root.addView(Ui.text(this, "Фокус", 32, Ui.TEXT, true));
        root.addView(Ui.text(this, "Возвращает тебя к делу и укладывает спать вовремя 💪",
                15, Ui.MUTED, false));

        // ---- Статус ----
        LinearLayout status = Ui.card(this);
        status.addView(Ui.header(this, "Сейчас"));
        if (Prefs.enabled(this)) {
            status.addView(Ui.text(this, "Следующий вопрос: "
                    + Scheduler.formatWhen(Prefs.nextAsk(this)), 16, Ui.TEXT, false));
            status.addView(Ui.text(this, "Отбой: "
                    + Scheduler.formatWhen(Scheduler.nextBedtime(this, System.currentTimeMillis())),
                    16, Ui.TEXT, false));
            long at = Prefs.lastAnswerAt(this);
            if (at > 0) {
                status.addView(Ui.text(this, "Последний ответ: "
                        + Answers.label(Prefs.lastAnswerKey(this)) + " (" + Scheduler.formatWhen(at) + ")",
                        14, Ui.MUTED, false));
            }
        } else {
            status.addView(Ui.text(this, "Приложение выключено", 16, Ui.MUTED, false));
        }
        status.addView(Ui.smallButton(this, "Ответить сейчас: чем я занят", 0xFFB07A20,
                v -> startActivity(new Intent(this, AskActivity.class))));
        root.addView(status);

        // ---- Разрешения ----
        buildPermissions();

        // ---- Основное ----
        LinearLayout main = Ui.card(this);
        main.addView(Ui.header(this, "Основное"));
        Switch sw = new Switch(this);
        sw.setText("Приложение включено");
        sw.setTextColor(Ui.TEXT);
        sw.setTextSize(16);
        sw.setChecked(Prefs.enabled(this));
        sw.setOnCheckedChangeListener((b, checked) -> {
            Prefs.setEnabled(this, checked);
            changed();
        });
        main.addView(sw);

        int bed = Prefs.bedtime(this);
        String bedStr = String.format(Locale.US, "%02d:%02d", bed / 60, bed % 60);
        main.addView(Ui.smallButton(this, "🌙 Время отбоя: " + bedStr, 0xFF3949AB, v ->
                new TimePickerDialog(this, (tp, h, m) -> {
                    Prefs.setBedtime(this, h * 60 + m);
                    changed();
                }, bed / 60, bed % 60, true).show()));
        root.addView(main);

        // ---- Дни недели ----
        LinearLayout days = Ui.card(this);
        days.addView(Ui.header(this, "Режим по дням недели"));
        for (int i = 0; i < DOW.length; i++) {
            final int dow = DOW[i];
            days.addView(Ui.text(this, DOW_NAMES[i], 15, Ui.TEXT, true));
            Spinner sp = new Spinner(this);
            ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, Prefs.MODE_NAMES);
            sp.setAdapter(ad);
            sp.setSelection(Prefs.dayMode(this, dow), false);
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    if (pos != Prefs.dayMode(MainActivity.this, dow)) {
                        Prefs.setDayMode(MainActivity.this, dow, pos);
                        changed();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            days.addView(sp);
        }
        root.addView(days);

        // ---- Особые даты ----
        LinearLayout special = Ui.card(this);
        special.addView(Ui.header(this, "Особые даты"));
        special.addView(Ui.text(this,
                "Например: рабочее воскресенье раз в месяц или занятая среда.",
                14, Ui.MUTED, false));
        String todayKey = Prefs.dateKey(Scheduler.midnight(System.currentTimeMillis()));
        int count = 0;
        for (Map.Entry<String, Integer> e : Prefs.overrides(this).entrySet()) {
            final String key = e.getKey();
            if (key.compareTo(todayKey) < 0) {
                Prefs.removeKey(this, key); // прошедшие даты убираем
                continue;
            }
            count++;
            String date = key.substring(5); // yyyy-MM-dd
            String pretty = date.substring(8, 10) + "." + date.substring(5, 7) + "." + date.substring(0, 4);
            int mode = e.getValue();
            String modeName = (mode >= 0 && mode < Prefs.MODE_SHORT.length) ? Prefs.MODE_SHORT[mode] : "?";
            special.addView(Ui.smallButton(this, pretty + " — " + modeName + "   ✕", 0xFF37474F, v -> {
                Prefs.removeKey(this, key);
                changed();
            }));
        }
        if (count == 0) {
            special.addView(Ui.text(this, "Пока нет особых дат.", 14, Ui.MUTED, false));
        }
        special.addView(Ui.smallButton(this, "+ Добавить дату", 0xFF239B56, v -> addDate()));
        root.addView(special);

        // ---- Проверка ----
        LinearLayout test = Ui.card(this);
        test.addView(Ui.header(this, "Проверка"));
        test.addView(Ui.smallButton(this, "Показать уведомление-вопрос", 0xFF2E86C1, v ->
                Notifier.showAsk(this)));
        test.addView(Ui.smallButton(this, "Проверить будильник отбоя", 0xFF3949AB, v ->
                startActivity(new Intent(this, BedtimeActivity.class)
                        .putExtra(BedtimeActivity.EXTRA_TEST, true))));
        root.addView(test);

        TextView tip = Ui.text(this,
                "Совет: на Xiaomi, Samsung, Huawei включи для «Фокус» автозапуск и отключи "
                        + "экономию батареи, иначе напоминания могут опаздывать.",
                13, Ui.MUTED, false);
        tip.setPadding(0, Ui.dp(this, 16), 0, 0);
        root.addView(tip);
    }

    private void buildPermissions() {
        String pkg = getPackageName();
        boolean notifOk = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        boolean exactOk = true;
        AlarmManager am = getSystemService(AlarmManager.class);
        if (Build.VERSION.SDK_INT >= 31 && am != null) {
            exactOk = am.canScheduleExactAlarms();
        }
        boolean fullOk = true;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 34 && nm != null) {
            fullOk = nm.canUseFullScreenIntent();
        }
        boolean batteryOk = true;
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null) {
            batteryOk = pm.isIgnoringBatteryOptimizations(pkg);
        }
        if (notifOk && exactOk && fullOk && batteryOk) {
            return;
        }

        LinearLayout card = Ui.card(this);
        card.addView(Ui.header(this, "⚠️ Нужны разрешения"));
        card.addView(Ui.text(this, "Без них напоминания могут не приходить.", 14, Ui.MUTED, false));
        if (!notifOk) {
            card.addView(Ui.smallButton(this, "Разрешить уведомления", Ui.WARN, v -> open(
                    new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, pkg))));
        }
        if (!exactOk && Build.VERSION.SDK_INT >= 31) {
            card.addView(Ui.smallButton(this, "Разрешить точные будильники", Ui.WARN, v -> open(
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + pkg)))));
        }
        if (!fullOk && Build.VERSION.SDK_INT >= 34) {
            card.addView(Ui.smallButton(this, "Разрешить окно на весь экран", Ui.WARN, v -> open(
                    new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + pkg)))));
        }
        if (!batteryOk) {
            card.addView(Ui.smallButton(this, "Не ограничивать в фоне (батарея)", Ui.WARN, v -> open(
                    new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + pkg)))));
        }
        root.addView(card);
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
                // нечего открыть
            }
        }
    }

    private void addDate() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (dp, y, m, d) -> {
            Calendar day = Calendar.getInstance();
            day.clear();
            day.set(y, m, d);
            new AlertDialog.Builder(this)
                    .setTitle(String.format(Locale.US, "%02d.%02d.%04d — какой режим?", d, m + 1, y))
                    .setItems(Prefs.MODE_NAMES, (dlg, which) -> {
                        Prefs.setDateOverride(this, day, which);
                        changed();
                    })
                    .show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }
}
