package com.guven.sahra;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** Tüm ekranda kar, yağmur, yaprak ya da güneş canlandırması. */
final class Efekt extends View {
    static final long SURE = 9000;

    private final String tur;
    private final float birim;
    private final Paint boya = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path yaprak = new Path();
    private final float[][] tane;
    private final long basla = SystemClock.uptimeMillis();
    private static final int[] YAPRAK_RENKLERI = {0xFFD97706, 0xFFB45309, 0xFFDC2626, 0xFFF59E0B, 0xFF92400E};

    Efekt(Context c, String tur) {
        super(c);
        this.tur = tur;
        birim = c.getResources().getDisplayMetrics().density;
        int adet = "kar".equals(tur) ? 130 : "yagmur".equals(tur) ? 160 : "yaprak".equals(tur) ? 34 : 0;
        Random r = new Random();
        tane = new float[adet][5];
        for (float[] t : tane) {
            for (int i = 0; i < t.length; i++) t[i] = r.nextFloat();
        }
        yaprak.moveTo(0, -10);
        yaprak.quadTo(9, -4, 0, 10);
        yaprak.quadTo(-9, -4, 0, -10);
        yaprak.close();
    }

    @Override
    protected void onDraw(Canvas tuval) {
        long gecen = SystemClock.uptimeMillis() - basla;
        if (gecen > SURE) return;
        float s = gecen / 1000f;
        // Başta belirir, sonda söner
        float guc = Math.min(1f, Math.min(gecen / 700f, (SURE - gecen) / 1500f));
        float en = getWidth();
        float boy = getHeight();
        if ("gunes".equals(tur)) {
            gunes(tuval, en, s, guc);
        } else {
            for (float[] t : tane) {
                if ("kar".equals(tur)) {
                    float y = (t[1] * boy + s * (40 + 60 * t[2]) * birim) % (boy + 20) - 10;
                    float x = t[0] * en + (float) Math.sin(s * (0.8 + t[3]) + t[4] * 6.3) * 18 * birim;
                    boya.setColor(0xFFFFFFFF);
                    boya.setAlpha((int) (guc * (150 + 105 * t[3])));
                    boya.setShadowLayer(3 * birim, 0, 0, 0x8890C8FF);
                    tuval.drawCircle(x, y, (1.5f + 3 * t[2]) * birim, boya);
                } else if ("yagmur".equals(tur)) {
                    float y = (t[1] * boy + s * (620 + 380 * t[2]) * birim) % (boy + 60) - 30;
                    float x = (t[0] * (en + 80) - y * 0.08f);
                    boya.setColor(0xFF7DBBFF);
                    boya.setAlpha((int) (guc * (110 + 110 * t[3])));
                    boya.setStrokeWidth((1 + t[2]) * birim);
                    tuval.drawLine(x, y, x - 2 * birim, y + (14 + 12 * t[2]) * birim, boya);
                } else {
                    float y = (t[1] * boy + s * (55 + 75 * t[2]) * birim) % (boy + 60) - 30;
                    float x = t[0] * en + (float) Math.sin(s * (1 + t[3]) + t[4] * 6.3) * 45 * birim;
                    boya.setColor(YAPRAK_RENKLERI[(int) (t[4] * YAPRAK_RENKLERI.length) % YAPRAK_RENKLERI.length]);
                    boya.setAlpha((int) (guc * 235));
                    tuval.save();
                    tuval.translate(x, y);
                    tuval.rotate(s * (60 + 120 * t[3]) + t[4] * 360);
                    float olcek = (0.8f + t[2]) * birim;
                    tuval.scale(olcek * (float) (0.4 + 0.6 * Math.abs(Math.cos(s * 2 + t[0] * 6))), olcek);
                    tuval.drawPath(yaprak, boya);
                    tuval.restore();
                }
            }
        }
        postInvalidateOnAnimation();
    }

    /** Sağ üstte parlayan güneş ve yavaşça dönen ışınlar. */
    private void gunes(Canvas tuval, float en, float s, float guc) {
        float x = en * 0.85f;
        float y = 90 * birim;
        float yaricap = (200 + 14 * (float) Math.sin(s * 2)) * birim;
        boya.setShader(new RadialGradient(x, y, yaricap,
                new int[]{0xFFFFF3B0, 0x99FFC83D, 0x00FFB000}, new float[]{0f, 0.28f, 1f}, Shader.TileMode.CLAMP));
        boya.setAlpha((int) (guc * 255));
        tuval.drawCircle(x, y, yaricap, boya);
        boya.setShader(null);
        boya.setColor(0xFFFFD45A);
        boya.setAlpha((int) (guc * 90));
        boya.setStrokeWidth(7 * birim);
        boya.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14 + s * 0.25;
            float uzunluk = (i % 2 == 0 ? 230 : 170) * birim;
            tuval.drawLine(x + (float) Math.cos(a) * 70 * birim, y + (float) Math.sin(a) * 70 * birim,
                    x + (float) Math.cos(a) * uzunluk, y + (float) Math.sin(a) * uzunluk, boya);
        }
    }
}
