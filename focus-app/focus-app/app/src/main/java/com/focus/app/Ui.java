package com.focus.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Небольшие помощники для интерфейса без XML. */
public final class Ui {

    public static final int BG = 0xFF0F141B;
    public static final int CARD = 0xFF1B2330;
    public static final int TEXT = 0xFFF2F4F7;
    public static final int MUTED = 0xFF9AA4B2;
    public static final int ACCENT = 0xFFFFB547;
    public static final int WARN = 0xFFE67E22;

    private Ui() {
    }

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    public static GradientDrawable rounded(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    public static TextView button(Context c, String s, int color, View.OnClickListener l) {
        TextView b = text(c, s, 18, 0xFFFFFFFF, true);
        b.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, 16);
        b.setPadding(dp(c, 20), p, dp(c, 20), p);
        b.setBackground(rounded(color, dp(c, 14)));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        b.setLayoutParams(lp);
        return b;
    }

    public static TextView smallButton(Context c, String s, int color, View.OnClickListener l) {
        TextView b = button(c, s, color, l);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER);
        int p = dp(c, 12);
        b.setPadding(p, p, p, p);
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setBackground(rounded(CARD, dp(c, 16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 14);
        l.setLayoutParams(lp);
        return l;
    }

    public static TextView header(Context c, String s) {
        TextView t = text(c, s, 17, ACCENT, true);
        t.setPadding(0, 0, 0, dp(c, 6));
        return t;
    }
}
