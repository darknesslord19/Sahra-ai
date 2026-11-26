package com.guven.sahra;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/** Diğer uygulamaların üstünde açılan bilgi penceresi. Dışındaki dokunuşlar alttaki uygulamaya gider. */
final class BilgiPenceresi {
    private final Context c;
    private final WindowManager pencereler;
    private final Runnable kapaninca;
    private final Handler ana = new Handler(Looper.getMainLooper());
    private LinearLayout kok;
    private TextView baslik;
    private FrameLayout govde;
    private WebView web;
    private String soru = "";
    private String okunacak = "";
    private boolean yukleniyor;
    private final Runnable okuIstegi;

    interface Metin {
        void hazir(String metin);
    }

    /** Google sayfasından menü ve bağlantı satırlarını atıp cümle gibi olan satırları alır. */
    private static final String SAYFA_METNI = "(function(){"
            + "var k=document.querySelector('#rso')||document.querySelector('#main')||document.body;"
            + "if(!k)return '';"
            + "function sec(m,az){var g={},c=[],n=0;m.split('\\n').forEach(function(x){x=x.trim();"
            + "if(x.length<az||g[x]||/https?:|www\\.|\u203a/.test(x)||n>2200)return;g[x]=1;c.push(x);n+=x.length});return c.join('\\n')}"
            + "var t=sec(k.innerText||'',40);"
            + "if(t.length<80)t=sec(document.body.innerText||'',20);"
            + "return t})()";
    private int sira;

    BilgiPenceresi(Context c, WindowManager pencereler, Runnable kapaninca, Runnable okuIstegi) {
        this.okuIstegi = okuIstegi;
        this.c = c;
        this.pencereler = pencereler;
        this.kapaninca = kapaninca;
    }

    boolean acik() {
        return kok != null;
    }

    /** Pencereyi açar; dönen numara sonradan gelen cevabın hâlâ güncel olup olmadığını gösterir. */
    int yukleniyor(String yeniSoru) {
        soru = yeniSoru;
        ac();
        baslik.setText(yeniSoru);
        yaz("Araştırıyorum…");
        okunacak = "";
        yukleniyor = true;
        return ++sira;
    }

    boolean guncel(int numara) {
        return acik() && numara == sira;
    }

    /** Penceredeki yazıyı verir. Henüz hazır değilse ya da yazı yoksa boş metin. */
    void metniAl(final Metin alici) {
        if (web == null) {
            alici.hazir(yukleniyor ? "" : okunacak);
            return;
        }
        web.evaluateJavascript(SAYFA_METNI, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String deger) {
                String t = "";
                try {
                    Object o = new JSONTokener(deger).nextValue();
                    if (o instanceof String) t = (String) o;
                } catch (JSONException e) {
                    // Sayfa okunamadı; boş döner
                }
                alici.hazir(t.trim());
            }
        });
    }

    void goster(Bilgi.Sonuc s) {
        okunacak = s.baslik + ". " + s.metin;
        yukleniyor = false;
        baslik.setText(s.baslik);
        temizle();
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        if (!s.resim.isEmpty()) {
            ImageView r = new ImageView(c);
            r.setAdjustViewBounds(true);
            r.setScaleType(ImageView.ScaleType.FIT_CENTER);
            r.setMaxHeight(dp(170));
            l.addView(r, new LinearLayout.LayoutParams(-1, -2));
            resimYukle(s.resim, r);
        }
        TextView t = metin(s.metin, 15, 0xFFF2F4F8);
        t.setLineSpacing(0, 1.15f);
        t.setPadding(0, dp(8), 0, dp(8));
        l.addView(t);
        l.addView(metin("Kaynak: " + s.kaynak, 12, 0xFF9AA2B5));
        ScrollView k = new ScrollView(c);
        k.addView(l);
        govde.addView(k, new FrameLayout.LayoutParams(-1, -1));
    }

    void yaz(String s) {
        yukleniyor = false;
        okunacak = s;
        temizle();
        govde.addView(metin(s, 15, 0xFFF2F4F8));
    }

    /** Google sonuçlarını pencerenin içinde açar. */
    void google(String aranan) {
        soru = aranan;
        ac();
        ++sira;
        baslik.setText(aranan);
        temizle();
        web = new WebView(c);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest istek) {
                // Yalnızca güvenli web adresleri açılır
                return !"https".equals(istek.getUrl().getScheme());
            }
        });
        web.loadUrl("https://www.google.com/search?hl=tr&q=" + Uri.encode(aranan));
        govde.addView(web, new FrameLayout.LayoutParams(-1, -1));
    }

    void kapat() {
        if (kok == null) return;
        ++sira;
        temizle();
        pencereler.removeView(kok);
        kok = null;
    }

    private void temizle() {
        govde.removeAllViews();
        if (web != null) {
            web.destroy();
            web = null;
        }
    }

    private void ac() {
        if (kok != null) return;
        kok = new LinearLayout(c);
        kok.setOrientation(LinearLayout.VERTICAL);
        kok.setPadding(dp(16), dp(14), dp(16), dp(12));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFA181B24);
        g.setCornerRadius(dp(22));
        g.setStroke(dp(1), 0xFF34D399);
        kok.setBackground(g);

        baslik = metin("", 19, 0xFFF2F4F8);
        baslik.setTypeface(Typeface.DEFAULT_BOLD);
        baslik.setMaxLines(2);
        kok.addView(baslik);
        govde = new FrameLayout(c);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, 0, 1f);
        gl.topMargin = dp(8);
        kok.addView(govde, gl);

        LinearLayout dugmeler = new LinearLayout(c);
        dugmeler.addView(dugme("Google", false, new Runnable() {
            @Override
            public void run() {
                google(soru);
            }
        }), new LinearLayout.LayoutParams(0, dp(44), 1f));
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(0, dp(44), 1f);
        ol.leftMargin = dp(8);
        dugmeler.addView(dugme("Oku", false, okuIstegi), ol);
        LinearLayout.LayoutParams kl = new LinearLayout.LayoutParams(0, dp(44), 1f);
        kl.leftMargin = dp(8);
        dugmeler.addView(dugme("Kapat", true, new Runnable() {
            @Override
            public void run() {
                kapat();
                kapaninca.run();
            }
        }), kl);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-1, -2);
        dl.topMargin = dp(10);
        kok.addView(dugmeler, dl);

        DisplayMetrics m = c.getResources().getDisplayMetrics();
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(m.widthPixels - dp(24),
                Math.round(m.heightPixels * 0.62f), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.CENTER;
        pencereler.addView(kok, p);
    }

    private void resimYukle(final String adres, final ImageView hedef) {
        if (!adres.startsWith("https://")) return;
        new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection b = Bilgi.baglan(adres);
                    InputStream in = b.getInputStream();
                    final Bitmap resim = BitmapFactory.decodeStream(in);
                    in.close();
                    ana.post(new Runnable() {
                        @Override
                        public void run() {
                            hedef.setImageBitmap(resim);
                        }
                    });
                } catch (IOException e) {
                    // Resim gelmezse yazı yine gösterilir
                }
            }
        }.start();
    }

    private TextView metin(String s, int boy, int renk) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(boy);
        t.setTextColor(renk);
        return t;
    }

    private TextView dugme(String s, boolean birincil, final Runnable is) {
        TextView t = metin(s, 14, birincil ? 0xFF06231A : 0xFFF2F4F8);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(birincil ? 0xFF34D399 : 0xFF2B3040);
        g.setCornerRadius(dp(14));
        t.setBackground(g);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is.run();
            }
        });
        return t;
    }

    private int dp(int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
