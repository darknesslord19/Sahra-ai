package com.guven.sahra;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Kuyruklu, yuvarlak konuşma balonu. */
final class BalonCizim extends Drawable {
    boolean kuyrukAltta = true;
    float kuyrukX;
    private final float birim;
    private final Paint dolgu = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cizgi = new Paint(Paint.ANTI_ALIAS_FLAG);

    BalonCizim(float birim) {
        this.birim = birim;
        dolgu.setColor(0xFFFFFFFF);
        cizgi.setColor(0xFF34D399);
        cizgi.setStyle(Paint.Style.STROKE);
        cizgi.setStrokeWidth(1.5f * birim);
    }

    @Override
    public void draw(Canvas tuval) {
        float en = getBounds().width();
        float boy = getBounds().height();
        float kuyruk = 9 * birim;
        float kenar = birim;
        float ust = kuyrukAltta ? kenar : kuyruk;
        float alt = kuyrukAltta ? boy - kuyruk : boy - kenar;
        Path govde = new Path();
        govde.addRoundRect(new RectF(kenar, ust, en - kenar, alt), 18 * birim, 18 * birim, Path.Direction.CW);
        float x = Math.max(26 * birim, Math.min(kuyrukX, en - 26 * birim));
        float taban = kuyrukAltta ? alt - birim : ust + birim;
        Path uc = new Path();
        uc.moveTo(x - 8 * birim, taban);
        uc.lineTo(x, kuyrukAltta ? boy - kenar : kenar);
        uc.lineTo(x + 8 * birim, taban);
        uc.close();
        govde.op(uc, Path.Op.UNION);
        tuval.drawPath(govde, dolgu);
        tuval.drawPath(govde, cizgi);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter filtre) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
