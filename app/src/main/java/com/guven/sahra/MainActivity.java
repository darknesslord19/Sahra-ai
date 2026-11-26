package com.guven.sahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

/** Ayar ekranı. Ek kütüphane kullanmadan kart düzeni. */
public class MainActivity extends Activity {
    private static final int ZEMIN = 0xFF0E1016;
    private static final int KART = 0xFF181B24;
    private static final int ALAN = 0xFF222632;
    private static final int CIZGI = 0xFF2B3040;
    private static final int METIN = 0xFFF2F4F8;
    private static final int SOLUK = 0xFF9AA2B5;
    private static final int VURGU = 0xFF34D399;
    private static final int VURGU2 = 0xFF22D3EE;
    private static final int UYARI = 0xFFFBBF24;
    private static final int[] RENK_ORNEK = {0xFFE8ECF4, 0xFF262A34, 0xFF2B7BE0, 0xFF14B0AA, 0xFF2FA84A,
            0xFF7A4CE0, 0xFFEC5599, 0xFFD93630, 0xFFD9A21F};

    private LinearLayout kok;
    private LinearLayout kart;
    private ScrollView kaydir;
    private WebView onizleme;
    private String yazilanKomut = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(ZEMIN);
        w.setNavigationBarColor(ZEMIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ciz();
    }

    @Override
    protected void onDestroy() {
        if (onizleme != null) onizleme.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        ciz();
    }

    private void ciz() {
        final int konum = kaydir == null ? 0 : kaydir.getScrollY();
        if (onizleme != null) {
            ((LinearLayout) onizleme.getParent()).removeView(onizleme);
            onizleme.destroy();
        }
        kok = new LinearLayout(this);
        kok.setOrientation(LinearLayout.VERTICAL);
        kok.setPadding(dp(16), dp(12), dp(16), dp(28));
        kaydir = new ScrollView(this);
        kaydir.setBackgroundColor(ZEMIN);
        kaydir.setFitsSystemWindows(true);
        kaydir.setVerticalScrollBarEnabled(false);
        kaydir.addView(kok);
        setContentView(kaydir);

        baslikKarti();
        boolean mikrofon = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean ustte = Settings.canDrawOverlays(this);
        anaDugme(mikrofon && ustte);
        komutKarti(mikrofon && ustte);
        izinKarti(mikrofon, ustte);
        sesKarti();
        gorunumKarti();
        genelKarti();
        yardimKarti();

        kaydir.post(new Runnable() {
            @Override
            public void run() {
                kaydir.scrollTo(0, konum);
            }
        });
    }

    // ---------- Bölümler ----------

    private void baslikKarti() {
        LinearLayout k = new LinearLayout(this);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setGravity(Gravity.CENTER_HORIZONTAL);
        k.setPadding(dp(16), dp(18), dp(16), dp(18));
        GradientDrawable zemin = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF14302B, 0xFF171B2A, 0xFF1B1830});
        zemin.setCornerRadius(dp(24));
        k.setBackground(zemin);

        onizleme = new WebView(this);
        onizleme.setBackgroundColor(Color.TRANSPARENT);
        onizleme.getSettings().setJavaScriptEnabled(true);
        onizleme.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String adres) {
                onizlemeBoya();
            }
        });
        onizleme.loadUrl("file:///android_asset/robot.html");
        k.addView(onizleme, new LinearLayout.LayoutParams(dp(112), dp(112)));

        TextView ad = metin("Sahra", 28, METIN);
        ad.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        ad.setGravity(Gravity.CENTER);
        k.addView(ad);
        TextView alt = metin("Kişisel sesli asistanınız, " + Ayar.hitap(this), 14, SOLUK);
        alt.setGravity(Gravity.CENTER);
        k.addView(alt);

        boolean acik = SahraService.calisiyor;
        TextView durum = metin(acik ? "●  Çalışıyor" : "●  Kapalı", 13, acik ? VURGU : SOLUK);
        durum.setPadding(dp(14), dp(6), dp(14), dp(6));
        durum.setBackground(sekil(0x33000000, 20, 0));
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = dp(10);
        k.addView(durum, l);
        kok.addView(k, new LinearLayout.LayoutParams(-1, -2));
    }

    private void anaDugme(final boolean hazir) {
        final boolean acik = SahraService.calisiyor;
        TextView d = dugme(acik ? "Sahra'yı kapat" : "Sahra'yı başlat", !acik, new Runnable() {
            @Override
            public void run() {
                if (acik) {
                    stopService(new Intent(MainActivity.this, SahraService.class));
                } else if (!hazir) {
                    uyar("Önce İzinler bölümünden mikrofon ve üstte gösterme izinlerini verin.");
                    return;
                } else {
                    startForegroundService(new Intent(MainActivity.this, SahraService.class));
                }
                kok.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ciz();
                    }
                }, 500);
            }
        });
        d.setTextSize(17);
        ekle(kok, d, 14, 54);
    }

    private void komutKarti(final boolean hazir) {
        kart("Yazılı komut");
        LinearLayout satir = new LinearLayout(this);
        final EditText e = kutu("YouTube'dan film aç", yazilanKomut);
        izle(e, new Yazildi() {
            @Override
            public void oldu(String s) {
                yazilanKomut = s;
            }
        });
        satir.addView(e, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView g = dugme("Gönder", true, new Runnable() {
            @Override
            public void run() {
                if (!hazir) {
                    uyar("Önce mikrofon ve üstte gösterme izinlerini verin.");
                } else if (!yazilanKomut.trim().isEmpty()) {
                    startForegroundService(new Intent(MainActivity.this, SahraService.class)
                            .putExtra(SahraService.KOMUT, yazilanKomut));
                }
            }
        });
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(92), dp(48));
        l.leftMargin = dp(8);
        satir.addView(g, l);
        ekle(kart, satir, 10, -2);

        anahtar("Sürekli dinleme", "Cümleye \"Sahra\" diye başlayın. Pili daha çok harcar, bazı telefonlarda bip sesi çıkar. Ses, telefonun tanıma hizmetine gider.",
                "surekli");
    }

    private void izinKarti(boolean mikrofon, boolean ustte) {
        kart("İzinler");
        izin("Mikrofon ve bildirim", "Sesli komut için", mikrofon, new Runnable() {
            @Override
            public void run() {
                requestPermissions(Build.VERSION.SDK_INT >= 33
                        ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}
                        : new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        });
        izin("Üstte gösterme", "Yüzen robot için", ustte,
                ayarAc(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, true));
        izin("Ekran kontrolü", "Okuma, yazma, dokunma", EkranService.acik != null,
                ayarAc(Settings.ACTION_ACCESSIBILITY_SETTINGS, false));
        android.app.NotificationManager n = getSystemService(android.app.NotificationManager.class);
        izin("Zili sessize alma", "İsteğe bağlı", n.isNotificationPolicyAccessGranted(),
                ayarAc(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, false));
        if (EkranService.acik == null) {
            ekle(kart, metin("Ekran kontrolü gri görünürse: Uygulama bilgisi → ⋮ → Kısıtlanmış ayarlara izin ver.", 12, SOLUK), 10, -2);
            ekle(kart, dugme("Uygulama bilgisi", false, ayarAc(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true)), 8, 44);
        }
    }

    private void sesKarti() {
        kart("Sahra'nın sesi");
        ekle(kart, metin("Her dokunuşta telefondaki sıradaki Türkçe sese geçer. İnternet açıkken daha doğal sesler kullanılabilir.", 12, SOLUK), 4, -2);
        ekle(kart, dugme("Sıradaki sesi dene", true, sesIstegi(SahraService.SES_SIRADAKI)), 10, 46);
        kaydirici("Konuşma hızı", "hiz", 70, 140, 100, "%", SahraService.SES_DENE);
        kaydirici("Ses tonu", "ton", 70, 140, 100, "%", SahraService.SES_DENE);
        ekle(kart, dugme("Telefonun ses ayarları (yeni ses indir)", false, new Runnable() {
            @Override
            public void run() {
                try {
                    startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
                } catch (RuntimeException e) {
                    uyar("Ayarlarda \"Metin okuma\" diye aratın.");
                }
            }
        }), 10, 44);
    }

    private void gorunumKarti() {
        kart("Robot görünümü");
        LinearLayout renkler = new LinearLayout(this);
        for (int i = 0; i < Komut.RENKLER.length; i++) {
            final String ad = Komut.RENKLER[i];
            final View v = new View(this);
            v.setTag(i);
            v.setContentDescription(ad);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View t) {
                    Ayar.al(MainActivity.this).edit().putString("renk", ad).apply();
                    renkIsaretle((LinearLayout) t.getParent());
                    onizlemeBoya();
                    yenile();
                }
            });
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(40), dp(40));
            l.rightMargin = dp(10);
            renkler.addView(v, l);
        }
        renkIsaretle(renkler);
        HorizontalScrollView yatay = new HorizontalScrollView(this);
        yatay.setHorizontalScrollBarEnabled(false);
        yatay.addView(renkler);
        ekle(kart, yatay, 10, -2);
        kaydirici("Boyut", "boyut", 46, 184, 92, " dp", SahraService.YENILE);
    }

    private void genelKarti() {
        kart("Genel");
        ekle(kart, metin("Size nasıl hitap etsin?", 13, SOLUK), 6, -2);
        EditText hitap = kutu("Güven hocam", Ayar.hitap(this));
        izle(hitap, new Yazildi() {
            @Override
            public void oldu(String s) {
                String v = s.trim();
                Ayar.al(MainActivity.this).edit().putString("hitap", v.isEmpty() ? "Güven hocam" : v).apply();
            }
        });
        ekle(kart, hitap, 6, 48);

        final List<Uygulamalar.Uygulama> hepsi = Uygulamalar.liste(this);
        String secili = "Seçilmedi";
        for (Uygulamalar.Uygulama u : hepsi) {
            if (u.paket.equals(Ayar.muzikPaketi(this))) secili = u.ad;
        }
        ekle(kart, dugme("Müzik uygulaması:  " + secili, false, new Runnable() {
            @Override
            public void run() {
                String[] adlar = new String[hepsi.size()];
                for (int i = 0; i < adlar.length; i++) adlar[i] = hepsi.get(i).ad;
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert)
                        .setTitle("Müzik uygulaması")
                        .setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, adlar),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int i) {
                                        Ayar.al(MainActivity.this).edit().putString("muzik", hepsi.get(i).paket).apply();
                                        ciz();
                                    }
                                }).show();
            }
        }), 10, 46);
    }

    private void yardimKarti() {
        kart("Komutlar");
        ekle(kart, metin("Robota dokunup söyleyin. Basılı tutunca bu ekran açılır.", 12, SOLUK), 4, -2);
        String[][] gruplar = {
                {"Genel", "merhaba · saat kaç · tarih ne · robotu kapat"},
                {"Uygulama", "WhatsApp aç · YouTube'dan aksiyon filmi aç · Chrome'da hava durumu ara"},
                {"Müzik ve ses", "müzik aç · sonraki şarkı · müziği durdur · sessize al · sesi aç · sesi kıs"},
                {"Mesaj", "mesaj yaz akşam geliyorum · gönder"},
                {"Ekran", "ekranı oku · Tamam butonuna tıkla · geri · ana ekran · aşağı kaydır"},
                {"Robot", "büyü · küçül · rengini mavi yap · sesini değiştir"}};
        for (String[] g : gruplar) {
            ekle(kart, metin(g[0], 13, VURGU), 12, -2);
            ekle(kart, metin(g[1], 14, METIN), 2, -2);
        }
    }

    // ---------- Parçalar ----------

    private interface Yazildi {
        void oldu(String s);
    }

    private void izle(EditText e, final Yazildi y) {
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { y.oldu(s.toString()); }
        });
    }

    private Runnable ayarAc(final String eylem, final boolean paketli) {
        return new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(eylem);
                if (paketli) i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        };
    }

    private Runnable sesIstegi(final String eylem) {
        return new Runnable() {
            @Override
            public void run() {
                if (SahraService.calisiyor) {
                    startForegroundService(new Intent(MainActivity.this, SahraService.class).setAction(eylem));
                } else {
                    uyar("Sesi denemek için önce Sahra'yı başlatın.");
                }
            }
        };
    }

    private void kart(String baslik) {
        kart = new LinearLayout(this);
        kart.setOrientation(LinearLayout.VERTICAL);
        kart.setPadding(dp(16), dp(14), dp(16), dp(16));
        kart.setBackground(sekil(KART, 20, CIZGI));
        TextView t = metin(baslik, 17, METIN);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        kart.addView(t);
        ekle(kok, kart, 14, -2);
    }

    private void izin(String ad, String aciklama, boolean tamam, final Runnable is) {
        LinearLayout satir = new LinearLayout(this);
        satir.setGravity(Gravity.CENTER_VERTICAL);
        satir.setPadding(dp(12), dp(10), dp(10), dp(10));
        satir.setBackground(basilabilir(sekil(ALAN, 14, 0)));
        satir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is.run();
            }
        });
        LinearLayout yazilar = new LinearLayout(this);
        yazilar.setOrientation(LinearLayout.VERTICAL);
        yazilar.addView(metin(ad, 15, METIN));
        yazilar.addView(metin(aciklama, 12, SOLUK));
        satir.addView(yazilar, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView rozet = metin(tamam ? "✓ Açık" : "İzin ver", 12, tamam ? VURGU : UYARI);
        rozet.setPadding(dp(10), dp(5), dp(10), dp(5));
        rozet.setBackground(sekil(tamam ? 0x2234D399 : 0x22FBBF24, 12, 0));
        satir.addView(rozet);
        ekle(kart, satir, 8, -2);
    }

    private void anahtar(String ad, String aciklama, final String anahtarAdi) {
        Switch s = new Switch(this);
        s.setText(ad);
        s.setTextSize(15);
        s.setTextColor(METIN);
        s.setChecked(Ayar.al(this).getBoolean(anahtarAdi, false));
        int[][] durumlar = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(durumlar, new int[]{VURGU, 0xFFB8BFCE}));
        s.setTrackTintList(new ColorStateList(durumlar, new int[]{0x8834D399, 0x55B8BFCE}));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean acik) {
                Ayar.al(MainActivity.this).edit().putBoolean(anahtarAdi, acik).apply();
                yenile();
            }
        });
        ekle(kart, s, 14, -2);
        ekle(kart, metin(aciklama, 12, SOLUK), 4, -2);
    }

    private void kaydirici(final String ad, final String anahtarAdi, final int enAz, int enCok, int ilk,
                           final String birim, final String eylem) {
        int deger = Ayar.al(this).getInt(anahtarAdi, ilk);
        final TextView etiket = metin(ad + ":  " + deger + birim, 13, SOLUK);
        ekle(kart, etiket, 14, -2);
        SeekBar s = new SeekBar(this);
        s.setMax(enCok - enAz);
        s.setProgress(deger - enAz);
        s.setProgressTintList(ColorStateList.valueOf(VURGU));
        s.setThumbTintList(ColorStateList.valueOf(VURGU));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar b, int v, boolean kullanici) {
                etiket.setText(ad + ":  " + (v + enAz) + birim);
            }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override
            public void onStopTrackingTouch(SeekBar b) {
                Ayar.al(MainActivity.this).edit().putInt(anahtarAdi, b.getProgress() + enAz).apply();
                if (SahraService.calisiyor) {
                    startForegroundService(new Intent(MainActivity.this, SahraService.class).setAction(eylem));
                }
            }
        });
        ekle(kart, s, 6, -2);
    }

    private void renkIsaretle(LinearLayout renkler) {
        for (int i = 0; i < renkler.getChildCount(); i++) {
            boolean secili = Komut.RENKLER[i].equals(Ayar.renk(this));
            GradientDrawable g = sekil(RENK_ORNEK[i], 20, 0);
            g.setStroke(dp(secili ? 3 : 1), secili ? VURGU : 0x55FFFFFF);
            renkler.getChildAt(i).setBackground(g);
        }
    }

    private void onizlemeBoya() {
        if (onizleme != null) {
            onizleme.evaluateJavascript("window.robot&&robot('idle'," + JSONObject.quote(Ayar.renk(this)) + ")", null);
        }
    }

    private TextView metin(String s, int boy, int renk) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(boy);
        t.setTextColor(renk);
        return t;
    }

    private EditText kutu(String ipucu, String deger) {
        EditText e = new EditText(this);
        e.setHint(ipucu);
        e.setText(deger);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(METIN);
        e.setHintTextColor(0xFF6B7287);
        e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(sekil(ALAN, 14, CIZGI));
        return e;
    }

    private TextView dugme(String s, boolean birincil, final Runnable is) {
        TextView b = metin(s, 14, birincil ? 0xFF06231A : METIN);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), 0, dp(10), 0);
        GradientDrawable g = birincil
                ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{VURGU, VURGU2})
                : sekil(ALAN, 14, CIZGI);
        g.setCornerRadius(dp(14));
        b.setBackground(basilabilir(g));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is.run();
            }
        });
        return b;
    }

    private RippleDrawable basilabilir(GradientDrawable g) {
        return new RippleDrawable(ColorStateList.valueOf(0x44FFFFFF), g, null);
    }

    private GradientDrawable sekil(int renk, int kose, int cerceve) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(renk);
        g.setCornerRadius(dp(kose));
        if (cerceve != 0) g.setStroke(dp(1), cerceve);
        return g;
    }

    private void ekle(LinearLayout nereye, View v, int ustBosluk, int boy) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, boy < 0 ? -2 : dp(boy));
        l.topMargin = dp(ustBosluk);
        nereye.addView(v, l);
    }

    private void yenile() {
        if (SahraService.calisiyor) {
            startForegroundService(new Intent(this, SahraService.class).setAction(SahraService.YENILE));
        }
    }

    private void uyar(String s) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setMessage(s).setPositiveButton("Tamam", null).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
