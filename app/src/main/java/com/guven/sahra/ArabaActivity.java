package com.guven.sahra;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Araba modu: büyük yazı ve dört büyük düğme. Açıkken ekran kapanmaz. */
public class ArabaActivity extends Activity {
    private static ArabaActivity acik;

    static void kapat() {
        if (acik != null) acik.finish();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        acik = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(0xFF000000);
        getWindow().setNavigationBarColor(0xFF000000);

        LinearLayout kok = new LinearLayout(this);
        kok.setOrientation(LinearLayout.VERTICAL);
        kok.setBackgroundColor(0xFF000000);
        kok.setPadding(dp(12), dp(16), dp(12), dp(12));
        kok.setFitsSystemWindows(true);

        TextView ad = yazi(Ayar.hitap(this), 34);
        kok.addView(ad);
        kok.addView(yazi("Nereye gidiyoruz?", 20));

        LinearLayout ust = satir(kok);
        dugme(ust, "Yol", R.drawable.ik_pusula, new Runnable() {
            @Override
            public void run() {
                hizmet(new Intent().putExtra(SahraService.SOR_VE_YAP, "yol"));
            }
        });
        dugme(ust, "Müzik", R.drawable.ik_muzik, new Runnable() {
            @Override
            public void run() {
                hizmet(new Intent().putExtra(SahraService.KOMUT, "müzik aç"));
            }
        });
        LinearLayout alt = satir(kok);
        dugme(alt, "Ara", R.drawable.ik_telefon, new Runnable() {
            @Override
            public void run() {
                hizmet(new Intent().putExtra(SahraService.SOR_VE_YAP, "ara"));
            }
        });
        dugme(alt, "Konuş", R.drawable.ik_mikrofon, new Runnable() {
            @Override
            public void run() {
                hizmet(new Intent().setAction(SahraService.DINLE));
            }
        });

        TextView cik = yazi("Araba modunu kapat", 18);
        cik.setPadding(0, dp(18), 0, dp(18));
        cik.setBackground(sekil(0xFF222632));
        cik.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ayar.al(ArabaActivity.this).edit().putBoolean("araba", false).apply();
                finish();
            }
        });
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(10);
        kok.addView(cik, l);
        setContentView(kok);
    }

    @Override
    protected void onDestroy() {
        if (acik == this) acik = null;
        super.onDestroy();
    }

    /** Geri tuşu da modu kapatır; mesajlar arka planda okunmaya devam etmez. */
    @Override
    public void onBackPressed() {
        Ayar.al(this).edit().putBoolean("araba", false).apply();
        finish();
    }

    private void hizmet(Intent i) {
        if (!SahraService.calisiyor) {
            android.widget.Toast.makeText(this, "Önce Sahra'yı başlatın.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        startForegroundService(i.setClass(this, SahraService.class));
    }

    private LinearLayout satir(LinearLayout kok) {
        LinearLayout s = new LinearLayout(this);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, 0, 1f);
        l.topMargin = dp(10);
        kok.addView(s, l);
        return s;
    }

    private void dugme(LinearLayout satir, String ad, int ikon, final Runnable is) {
        TextView t = yazi(ad, 28);
        Drawable d = getDrawable(ikon).mutate();
        d.setBounds(0, 0, dp(64), dp(64));
        t.setCompoundDrawables(null, d, null, null);
        t.setCompoundDrawablePadding(dp(10));
        t.setBackground(sekil(0xFF1A1D27));
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is.run();
            }
        });
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, -1, 1f);
        l.leftMargin = dp(5);
        l.rightMargin = dp(5);
        satir.addView(t, l);
    }

    private TextView yazi(String s, int boy) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(boy);
        t.setTextColor(0xFFF2F4F8);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private GradientDrawable sekil(int renk) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(renk);
        g.setCornerRadius(dp(22));
        return g;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
