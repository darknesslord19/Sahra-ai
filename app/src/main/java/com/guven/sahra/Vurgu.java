package com.guven.sahra;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/** Kumanda modunda seçili afişin çevresindeki çerçeve. Dokunuşlar alttaki uygulamaya geçer. */
final class Vurgu {
    private final Context c;
    private final WindowManager pencereler;
    private View cerceve;

    Vurgu(Context c, WindowManager pencereler) {
        this.c = c;
        this.pencereler = pencereler;
    }

    void goster(Rect r) {
        final float d = c.getResources().getDisplayMetrics().density;
        kaldir();
        cerceve = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x2634D399);
        g.setCornerRadius(10 * d);
        g.setStroke((int) (5 * d), 0xFF34D399);
        cerceve.setBackground(g);
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(r.width(), r.height(),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = r.left;
        p.y = r.top;
        // Android, dokunuşların geçmesi için üst pencerenin yarı saydam olmasını ister
        p.alpha = 0.8f;
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        final View v = cerceve;
        final int sol = r.left, ust = r.top;
        pencereler.addView(v, p);
        // Telefon pencereyi durum çubuğu kadar kaydırırsa ölçülüp düzeltilir
        v.post(new Runnable() {
            @Override
            public void run() {
                if (cerceve != v || !v.isAttachedToWindow()) return;
                int[] yer = new int[2];
                v.getLocationOnScreen(yer);
                if (yer[0] != sol || yer[1] != ust) {
                    p.x += sol - yer[0];
                    p.y += ust - yer[1];
                    pencereler.updateViewLayout(v, p);
                }
            }
        });
    }

    void kaldir() {
        if (cerceve != null) {
            if (cerceve.isAttachedToWindow() || cerceve.getParent() != null) pencereler.removeView(cerceve);
            cerceve = null;
        }
    }
}
