package com.guven.sahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Ayar ekranı: gruplu liste; her satır alttan kayan bir panel açar. */
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

    private static final int FARKLI_KAYDET = 10;
    private static final int GERI_YUKLE = 11;
    private static final int SOZ_DOSYASI = 12;

    private LinearLayout kok;
    private LinearLayout kart;
    private ScrollView kaydir;
    private WebView onizleme;
    private WebView davranisOnizleme;
    private WebView isikOnizleme;
    private WebView gorunumOnizleme;
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
    protected void onPause() {
        Yedek.gerekirseYedekle(this);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int istek, int sonuc, Intent veri) {
        if (sonuc != RESULT_OK || veri == null || veri.getData() == null) return;
        if (istek == SOZ_DOSYASI) {
            sozYukle(veri.getData());
            return;
        }
        try {
            if (istek == FARKLI_KAYDET) {
                Yedek.dosyayaYaz(this, veri.getData(), Yedek.json(this));
                uyar("Yedek kaydedildi.");
            } else if (istek == GERI_YUKLE) {
                int sayi = Yedek.geriYukle(this, Yedek.dosyadanOku(this, veri.getData()));
                yenile();
                ciz();
                uyar("Geri yüklendi. Yedekteki komut sayısı: " + sayi);
            }
        } catch (IOException | JSONException | RuntimeException e) {
            uyar("Yedek işlemi yapılamadı. Geçerli bir Sahra JSON dosyası seçin.");
        }
    }

    @Override
    protected void onDestroy() {
        if (panel != null) {
            panel.setOnDismissListener(null);
            panelTemizle();
            panel.dismiss();
            panel = null;
        }
        if (onizleme != null) onizleme.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        ciz();
    }

    /** Ana listede bir satır; dokununca paneli açılır. */
    private static final class Bolum {
        final String grup;
        final String ad;
        final String ozet;
        final int ikon;
        final Runnable ciz;

        Bolum(String grup, String ad, String ozet, int ikon, Runnable ciz) {
            this.grup = grup;
            this.ad = ad;
            this.ozet = ozet;
            this.ikon = ikon;
            this.ciz = ciz;
        }
    }

    private AltPanel panel;
    private Bolum acikBolum;

    private boolean mikrofonVar() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hazir() {
        return mikrofonVar() && Settings.canDrawOverlays(this);
    }

    private List<Bolum> bolumler() {
        List<Bolum> l = new ArrayList<Bolum>();
        l.add(new Bolum("Kullanım", "Yazılı komut", "Komut yazın, sürekli dinlemeyi açın", R.drawable.ik_klavye, new Runnable() {
            @Override public void run() { komutKarti(hazir()); }
        }));
        l.add(new Bolum("Kullanım", "Kendi komutlarım", "Bir cümleyle birkaç iş", R.drawable.ik_yildiz, new Runnable() {
            @Override public void run() { ozelKarti(hazir()); }
        }));
        l.add(new Bolum("Kurulum", "İzinler", hazir() ? "Temel izinler tamam" : "Mikrofon ve üstte gösterme gerekli", R.drawable.ik_kilit, new Runnable() {
            @Override public void run() { izinKarti(mikrofonVar(), Settings.canDrawOverlays(MainActivity.this)); }
        }));
        l.add(new Bolum("Kurulum", "Arka planda kalma", "Telefon Sahra'yı kapatmasın", R.drawable.ik_pil, new Runnable() {
            @Override public void run() { arkaPlanKarti(); }
        }));
        l.add(new Bolum("Kurulum", "Yapay zekâ (Gemini)", Gemini.hazir(this) ? "Anahtar kayıtlı" : "Sorular için, isteğe bağlı", R.drawable.ik_pirilti, new Runnable() {
            @Override public void run() { geminiKarti(); }
        }));
        l.add(new Bolum("Anlama ve ses", "Serbest cümle anlama", durumu("serbest", false), R.drawable.ik_anlama, new Runnable() {
            @Override public void run() { serbestKarti(); }
        }));
        l.add(new Bolum("Anlama ve ses", "Sohbet hafızası", durumu("hafiza", false), R.drawable.ik_sohbet, new Runnable() {
            @Override public void run() { hafizaKarti(); }
        }));
        l.add(new Bolum("Anlama ve ses", "\"Sahra\" uyandırma", durumu("uyan", false), R.drawable.ik_kulak, new Runnable() {
            @Override public void run() { uyandirmaKarti(); }
        }));
        l.add(new Bolum("Anlama ve ses", "Sahra'nın sesi", "Telefon sesi, hız ve ton", R.drawable.ik_hoparlor, new Runnable() {
            @Override public void run() { sesKarti(); }
        }));
        l.add(new Bolum("Anlama ve ses", "Doğal çevrimiçi ses", durumu("sesNet", false), R.drawable.ik_mikrofon, new Runnable() {
            @Override public void run() { netSesKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Beyin ve bellek", durumu("beyinOgren", true), R.drawable.ik_anlama, new Runnable() {
            @Override public void run() { beyinKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Kayıt bilgilerim",
                KayitBilgi.kayitli(this).isEmpty() ? "Formları otomatik doldur" : "Kayıtlı", R.drawable.ik_kilit, new Runnable() {
            @Override public void run() { kayitKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Alarm ve hatırlatıcı", durumu("alarm", true), R.drawable.ik_saat, new Runnable() {
            @Override public void run() { alarmKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Arama", "\"Annemi ara\", onaylı", R.drawable.ik_telefon, new Runnable() {
            @Override public void run() { aramaKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Hızlı kontroller", "Fener, kamera, parlaklık, yol tarifi", R.drawable.ik_fener, new Runnable() {
            @Override public void run() { hizliKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Sabah özeti", "\"Günaydın\" deyin", R.drawable.ik_sabah, new Runnable() {
            @Override public void run() { sabahKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Bildirim okuma", durumu("bildirim", false), R.drawable.ik_zil, new Runnable() {
            @Override public void run() { bildirimKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Notlar ve çeviri", Notlar.liste(this).size() + " not", R.drawable.ik_not, new Runnable() {
            @Override public void run() { notKarti(); }
        }));
        l.add(new Bolum("Yetenekler", "Araba modu", "Büyük yazı, tek komut", R.drawable.ik_araba, new Runnable() {
            @Override public void run() { arabaKarti(); }
        }));
        l.add(new Bolum("Robot", "Robot görünümü", "Renk ve boyut", R.drawable.ik_palet, new Runnable() {
            @Override public void run() { gorunumKarti(); }
        }));
        l.add(new Bolum("Robot", "Kenar ışığı",
                Isik.sekilAdi(Ayar.al(this).getString("isikSekil", Isik.ORIJINAL)), R.drawable.ik_isik, new Runnable() {
            @Override public void run() { isikKarti(); }
        }));
        l.add(new Bolum("Robot", "Robot davranışı", Davranis.ad(Ayar.davranis(this)), R.drawable.ik_yuz, new Runnable() {
            @Override public void run() { davranisKarti(); }
        }));
        l.add(new Bolum("Robot", "Boştayken gözlerde yazı", "Hava, söz, döviz, altın", R.drawable.ik_yazi, new Runnable() {
            @Override public void run() { panoKarti(); }
        }));
        l.add(new Bolum("Robot", "Hava efektleri", durumu("efektAcik", true), R.drawable.ik_kar, new Runnable() {
            @Override public void run() { efektKarti(); }
        }));
        l.add(new Bolum("Diğer", "Genel", "Hitap ve müzik uygulaması", R.drawable.ik_ayar, new Runnable() {
            @Override public void run() { genelKarti(); }
        }));
        l.add(new Bolum("Diğer", "Yedekleme (JSON)", "İndirilenler/Sahra", R.drawable.ik_yedek, new Runnable() {
            @Override public void run() { yedekKarti(); }
        }));
        l.add(new Bolum("Diğer", "Komutlar", "Neler söyleyebilirsiniz?", R.drawable.ik_kitap, new Runnable() {
            @Override public void run() { yardimKarti(); }
        }));
        return l;
    }

    private String durumu(String ayarAdi, boolean ilk) {
        return Ayar.al(this).getBoolean(ayarAdi, ilk) ? "Açık" : "Kapalı";
    }

    private void ciz() {
        anaListe();
        if (panel != null && panel.isShowing()) panelCiz();
    }

    private void anaListe() {
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
        anaDugme(hazir());

        String grup = "";
        LinearLayout kutusu = null;
        for (final Bolum b : bolumler()) {
            if (!b.grup.equals(grup)) {
                grup = b.grup;
                TextView g = metin(grup, 12, SOLUK);
                g.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                g.setPadding(dp(4), 0, 0, 0);
                ekle(kok, g, 20, -2);
                kutusu = new LinearLayout(this);
                kutusu.setOrientation(LinearLayout.VERTICAL);
                kutusu.setPadding(0, dp(2), 0, dp(2));
                kutusu.setBackground(sekil(KART, 18, CIZGI));
                ekle(kok, kutusu, 8, -2);
            } else {
                View ayrac = new View(this);
                ayrac.setBackgroundColor(CIZGI);
                kutusu.addView(ayrac, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1) / 2)));
            }
            kutusu.addView(satirGorunumu(b), new LinearLayout.LayoutParams(-1, -2));
        }

        kaydir.post(new Runnable() {
            @Override
            public void run() {
                kaydir.scrollTo(0, konum);
            }
        });
    }

    private View satirGorunumu(final Bolum b) {
        LinearLayout s = new LinearLayout(this);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setPadding(dp(14), dp(12), dp(12), dp(12));
        s.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, sekil(0xFFFFFFFF, 18, 0)));
        ImageView ikon = new ImageView(this);
        ikon.setImageResource(b.ikon);
        ikon.setPadding(dp(8), dp(8), dp(8), dp(8));
        ikon.setBackground(sekil(ALAN, 12, CIZGI));
        s.addView(ikon, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout yazilar = new LinearLayout(this);
        yazilar.setOrientation(LinearLayout.VERTICAL);
        yazilar.addView(metin(b.ad, 15, METIN));
        TextView ozet = metin(b.ozet, 12, SOLUK);
        ozet.setSingleLine(true);
        ozet.setEllipsize(TextUtils.TruncateAt.END);
        yazilar.addView(ozet);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, -2, 1f);
        l.leftMargin = dp(12);
        s.addView(yazilar, l);
        ImageView ok = new ImageView(this);
        ok.setImageResource(R.drawable.ik_ileri);
        s.addView(ok, new LinearLayout.LayoutParams(dp(18), dp(18)));
        s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                panelAc(b);
            }
        });
        return s;
    }

    private void panelAc(Bolum b) {
        if (panel != null && panel.isShowing()) return;
        acikBolum = b;
        panel = new AltPanel(this, b.ad);
        panel.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface d) {
                panelTemizle();
                panel = null;
                acikBolum = null;
                Yedek.gerekirseYedekle(MainActivity.this);
                if (!isFinishing() && !isDestroyed()) anaListe();
            }
        });
        panelCiz();
        panel.show();
    }

    /** Paneldeki robot önizlemelerini kapatır. */
    private void panelTemizle() {
        for (WebView w : new WebView[]{gorunumOnizleme, davranisOnizleme, isikOnizleme}) {
            if (w != null) {
                ((LinearLayout) w.getParent()).removeView(w);
                w.destroy();
            }
        }
        gorunumOnizleme = null;
        davranisOnizleme = null;
        isikOnizleme = null;
        modelDurumu = null;
    }

    private void panelCiz() {
        final int konum = panel.kaydir.getScrollY();
        panelTemizle();
        panel.icerik.removeAllViews();
        kart = panel.icerik;
        // Özet bilgiler değişmiş olabilir; aynı adlı bölümün güncel hâli çizilir
        for (Bolum b : bolumler()) {
            if (b.ad.equals(acikBolum.ad)) b.ciz.run();
        }
        final AltPanel p = panel;
        p.kaydir.post(new Runnable() {
            @Override
            public void run() {
                p.kaydir.scrollTo(0, konum);
            }
        });
    }

    // ---------- Yeni bölümler ----------

    private void aciklama(String s) {
        ekle(kart, metin(s, 13, SOLUK), 6, -2);
    }

    private void dikkat(String s) {
        TextView t = metin(s, 12, UYARI);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setBackground(sekil(0x14FBBF24, 12, 0x40FBBF24));
        ekle(kart, t, 12, -2);
    }

    private void soyleyin(String... cumleler) {
        ekle(kart, metin("Şöyle söyleyin", 13, VURGU), 14, -2);
        for (String c : cumleler) ekle(kart, metin("“" + c + "”", 14, METIN), 4, -2);
    }

    private void tehlikeliIzin(String ad, String aciklama, final String izinAdi) {
        izin(ad, aciklama, Eylemler.izinVar(this, izinAdi), new Runnable() {
            @Override
            public void run() {
                requestPermissions(new String[]{izinAdi}, 2);
            }
        });
    }

    private void izinReddiNotu() {
        aciklama("İzin penceresi çıkmıyorsa Uygulama bilgisi → İzinler bölümünden verin.");
        ekle(kart, dugme("Uygulama bilgisi", false, ayarAc(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true)), 8, 44);
    }

    private void geminiGerekli() {
        if (!Gemini.hazir(this)) dikkat("Bu özellik için önce Yapay zekâ (Gemini) bölümüne anahtar kaydedin.");
    }

    private void arkaPlanKarti() {
        aciklama("Bazı telefonlar pil tasarrufu için Sahra'yı kapatır. Aşağıdaki adımlar bunu azaltır.");
        boolean serbestPil = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        izin("Pil kısıtlaması", "Listede Sahra'yı bulup \"kısıtlama yok\" yapın", serbestPil,
                ayarAc(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, false));
        ekle(kart, dugme("Otomatik başlatma ekranı", false, new Runnable() {
            @Override
            public void run() {
                // Xiaomi telefonlarda doğrudan ekran; diğerlerinde uygulama bilgisi
                Intent i = new Intent().setClassName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity");
                if (!Eylemler.baslat(MainActivity.this, i)) {
                    ayarAc(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true).run();
                }
            }
        }), 10, 46);
        ekle(kart, dugme("Uygulama bilgisi (pil ve diğer izinler)", false,
                ayarAc(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, true)), 8, 44);
        aciklama("Xiaomi'de \"Arka planda açılır pencere göster\" iznini de açın; yoksa Sahra arkadayken uygulama açamayabilir. Son uygulamalar ekranında Sahra kartını kilitleyin.");
        dikkat("Menü adları telefona göre değişir. Bu adımlar Sahra'nın hiç kapanmayacağını garanti etmez.");
    }

    private void serbestKarti() {
        aciklama("Kalıba uymayan cümle Gemini'ye sorulur; o bir komut önerir, ne yapılacağına telefon karar verir.");
        anahtar("Serbest cümleyi anla", "Yalnızca tanınmayan cümleler Google'a gönderilir.", "serbest", false);
        geminiGerekli();
        soyleyin("Çok ses var", "Etraf çok karanlık");
        dikkat("Mesaj yazma ve gönderme, düğmeye basma, arama ve alarm önce onay ister. Ekran, bildirim ve not okuma bu yolla çalışmaz. Yapay zekâ yanlış anlayabilir.");
    }

    private void hafizaKarti() {
        aciklama("Takip sorularını anlar. Son 4 soru ve cevap yalnızca bellekte tutulur; Sahra kapanınca silinir.");
        anahtar("Sohbeti hatırla", "Açıkken yeni soruyla birlikte önceki soru ve cevaplar da Google'a gönderilir.", "hafiza", false);
        kaydirici("Unutma süresi", "hsure", 1, 15, 5, " dk", SahraService.YENILE);
        geminiGerekli();
        soyleyin("Atatürk kim", "Nerede doğdu", "Konuştuklarımızı unut");
        ekle(kart, dugme("Hafızayı temizle", false, new Runnable() {
            @Override
            public void run() {
                Hafiza.temizle();
                uyar("Hafıza temizlendi.");
            }
        }), 12, 44);
    }

    private TextView modelDurumu;
    private static int modelYuzde = -1;

    private void uyandirmaKarti() {
        aciklama("\"Sahra\" adı telefonda beklenir; ad duyulana kadar ses internete gönderilmez ve bip sesleri azalır. Önce yalnızca \"Sahra\" deyin, \"Efendim\" cevabından sonra komutunuzu söyleyin.");
        final boolean var = ModelIndir.hazir(this);
        if (!var) {
            modelDurumu = metin(modelYuzde >= 0 ? "İndiriliyor… %" + modelYuzde
                    : "Türkçe ses modeli gerekli (yaklaşık 35 MB, bir kez indirilir).", 13, UYARI);
            ekle(kart, modelDurumu, 10, -2);
            if (modelYuzde < 0) {
                ekle(kart, dugme("Modeli indir", true, new Runnable() {
                    @Override
                    public void run() {
                        modelIndir();
                    }
                }), 10, 46);
            }
        } else {
            ekle(kart, metin("Model yüklü.", 13, VURGU), 10, -2);
            Switch s = anahtar("Adımı telefonda tanı", "Açınca sürekli dinleme de açılır.", "uyan", false);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean acik) {
                    SharedPreferences.Editor e = Ayar.al(MainActivity.this).edit().putBoolean("uyan", acik);
                    if (acik) e.putBoolean("surekli", true);
                    e.apply();
                    yenile();
                }
            });
            kaydirici("Hassasiyet", "hassas", 1, 10, 5, "", SahraService.YENILE);
            ekle(kart, dugme("Modeli sil", false, new Runnable() {
                @Override
                public void run() {
                    Ayar.al(MainActivity.this).edit().putBoolean("uyan", false).apply();
                    yenile();
                    ModelIndir.sil(MainActivity.this);
                    ciz();
                }
            }), 12, 44);
        }
        dikkat("Deneysel: telefonda henüz denenmedi. Yanlış uyanabilir ya da adı kaçırabilir; pil harcar. Sorun olursa kapatın. Model: Vosk (Apache 2.0).");
    }

    private void modelIndir() {
        if (modelYuzde >= 0) return;
        modelYuzde = 0;
        ciz();
        final Context uygulama = getApplicationContext();
        new Thread() {
            @Override
            public void run() {
                String sorun = "";
                try {
                    ModelIndir.indir(uygulama, new ModelIndir.Ilerleme() {
                        @Override
                        public void yuzde(final int y) {
                            modelYuzde = y;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (modelDurumu != null) modelDurumu.setText("İndiriliyor… %" + y);
                                }
                            });
                        }
                    });
                } catch (IOException e) {
                    sorun = "Model kurulamadı: " + e.getMessage();
                }
                final String hata = sorun;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        modelYuzde = -1;
                        if (isFinishing() || isDestroyed()) return;
                        ciz();
                        if (!hata.isEmpty()) uyar(hata);
                    }
                });
            }
        }.start();
    }

    private void netSesKarti() {
        aciklama("Gemini'nin ses modeliyle daha doğal konuşur. Kayıtlı Gemini anahtarınızı kullanır.");
        anahtar("Çevrimiçi ses", "Yalnızca yapay zekâ, ansiklopedi ve çeviri cevapları ile deneme cümlesi. Bildirim, rehber, not ve ekran yazıları telefon sesiyle okunur.", "sesNet", false);
        anahtar("Olmazsa telefon sesine dön", "İnternet, kota ya da hizmet sorunu olursa", "sesYedek", true);
        geminiGerekli();
        ekle(kart, dugme("Ses:  " + CevrimiciSes.ses(this), false, new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Ses seçin")
                        .setItems(CevrimiciSes.SESLER, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int i) {
                                Ayar.al(MainActivity.this).edit().putString("netSes", CevrimiciSes.SESLER[i]).apply();
                                ciz();
                            }
                        }).show();
            }
        }), 12, 46);
        ekle(kart, dugme("Çevrimiçi sesi dene", true, sesIstegi(SahraService.NET_SES_DENE)), 8, 46);
        dikkat("Okunan metin Google'a gider. Ses isteklerinin ücretsiz sınırı ya da ücreti olabilir; Google'ın güncel fiyat sayfasına bakın. Cevap telefon sesinden daha geç başlar. Gerçek anahtarla henüz denenmedi.");
    }

    private void alarmKarti() {
        anahtar("Alarm komutları", "Telefonunuzun saat uygulaması kullanılır.", "alarm", true);
        soyleyin("7'ye alarm kur", "10 dakika say", "Akşam 8'de ilacı hatırlat", "Yarın saat 9'da toplantıyı hatırlat");
        aciklama("Yakın hatırlatmalar saat uygulamasında alarm ya da zamanlayıcı olur. \"Yarın\" ya da gün adı geçerse takvim açılır; tarihi kontrol edip siz kaydedersiniz.");
    }

    private void aramaKarti() {
        tehlikeliIzin("Rehber", "Adı bulmak için", Manifest.permission.READ_CONTACTS);
        tehlikeliIzin("Telefon araması", "Verilmezse numarayı yazar, aramayı siz başlatırsınız", Manifest.permission.CALL_PHONE);
        izinReddiNotu();
        soyleyin("Annemi ara", "Ahmet'i ara");
        aciklama("Aramadan önce her zaman sorulur. Aynı adda birden fazla kişi varsa Sahra hangisi diye sorar. Rehber telefonda kalır.");
    }

    private void hizliKarti() {
        izin("Sistem ayarlarını değiştirme", "Yalnızca parlaklık için", Settings.System.canWrite(this),
                ayarAc(Settings.ACTION_MANAGE_WRITE_SETTINGS, true));
        soyleyin("Feneri aç", "Feneri kapat", "Kamerayı aç", "Parlaklığı artır", "Parlaklığı yüzde 50 yap",
                "Kızılay için yol tarifi", "Wi-Fi aç");
        aciklama("Wi-Fi, Bluetooth, konum ve uçak modunda yalnızca ayar ekranı açılır; düğmeye siz basarsınız.");
    }

    private void sabahKarti() {
        aciklama("\"Günaydın\" deyince açık olan bölümler sırayla okunur. Hava için şehir seçili olmalı.");
        anahtar("Hava durumu", "Kaynak: Open-Meteo.com", "shava", true);
        anahtar("Dolar, euro, altın", "TCMB gösterge kuru; gram altın hesaplanır", "skur", true);
        anahtar("Bugünkü takvim", "Aşağıdaki izin gerekir", "stak", false);
        tehlikeliIzin("Takvim okuma", "Yalnızca bugünün kayıtları, telefonda okunur", Manifest.permission.READ_CALENDAR);
        anahtar("Güzel söz", "", "ssoz", true);
        String sehir = Ayar.al(this).getString("sehir", "");
        ekle(kart, dugme(sehir.isEmpty() ? "Şehir seç" : "Şehir:  " + sehir, sehir.isEmpty(), new Runnable() {
            @Override
            public void run() {
                sehirPenceresi();
            }
        }), 12, 46);
        soyleyin("Günaydın");
    }

    private void bildirimKarti() {
        aciklama("Kapalı başlar. Yalnızca seçtiğiniz uygulamaların bildirimleri bellekte tutulur ve telefon sesiyle okunur; internete gönderilmez.");
        anahtar("Bildirimleri oku", "", "bildirim", false);
        izin("Bildirim erişimi", "Listeden Sahra bildirim okuma'yı açın", BildirimService.erisimVar(this),
                ayarAc(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, false));
        int sayi = BildirimService.secili(this).size();
        ekle(kart, dugme(sayi == 0 ? "Uygulamaları seç" : "Seçili uygulama: " + sayi, sayi == 0, new Runnable() {
            @Override
            public void run() {
                uygulamaSecimi();
            }
        }), 12, 46);
        soyleyin("Bildirimleri oku");
        dikkat("Banka ve ödeme uygulamaları listede yoktur. Kod, şifre ve hesap bilgisi içeren bildirimler kural dayalı süzgeçle atlanır; her biçimi yakalayacağı garanti edilemez. SMS uygulamasını seçerken bunu düşünün. Hoparlörden okunanı yanınızdakiler duyar.");
    }

    private void uygulamaSecimi() {
        final List<Uygulamalar.Uygulama> l = new ArrayList<Uygulamalar.Uygulama>();
        for (Uygulamalar.Uygulama u : Uygulamalar.liste(this)) {
            if (!u.paket.equals(getPackageName()) && !Suzgec.bankaUygulamasi(u.paket, u.ad)) l.add(u);
        }
        final Set<String> secili = BildirimService.secili(this);
        String[] adlar = new String[l.size()];
        boolean[] isaretli = new boolean[l.size()];
        for (int i = 0; i < adlar.length; i++) {
            adlar[i] = l.get(i).ad;
            isaretli[i] = secili.contains(l.get(i).paket);
        }
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Okunacak uygulamalar")
                .setMultiChoiceItems(adlar, isaretli, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i, boolean acik) {
                        if (acik) secili.add(l.get(i).paket);
                        else secili.remove(l.get(i).paket);
                    }
                })
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Kaydet", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                        Ayar.al(MainActivity.this).edit().putStringSet("bildirimUygulamalari", secili).apply();
                        BildirimService.temizle();
                        ciz();
                    }
                }).show();
    }

    private void notKarti() {
        final List<String> notlar = Notlar.liste(this);
        aciklama("Notlar yalnızca telefonda saklanır ve JSON yedeğine girmez. Silmek için nota dokunun.");
        if (notlar.isEmpty()) aciklama("Henüz not yok.");
        for (int i = notlar.size() - 1; i >= Math.max(0, notlar.size() - 30); i--) {
            final int sira = i;
            TextView t = metin(notlar.get(i), 14, METIN);
            t.setPadding(dp(12), dp(10), dp(12), dp(10));
            t.setBackground(basilabilir(sekil(ALAN, 14, 0)));
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Dialog_Alert)
                            .setMessage("Bu not silinsin mi?\n\n" + notlar.get(sira))
                            .setNegativeButton("Vazgeç", null)
                            .setPositiveButton("Sil", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    List<String> guncel = Notlar.liste(MainActivity.this);
                                    if (guncel.remove(notlar.get(sira))) Notlar.kaydet(MainActivity.this, guncel);
                                    ciz();
                                }
                            }).show();
                }
            });
            ekle(kart, t, 8, -2);
        }
        soyleyin("Not al süt alınacak", "Notlarımı oku", "İngilizceye çevir yarın görüşürüz", "Bunu Almancaya çevir");
        aciklama("\"Bunu çevir\" son yapay zekâ ya da ansiklopedi cevabını çevirir.");
        dikkat("Çeviri için Gemini anahtarı gerekir ve metin Google'a gider.");
    }

    private void arabaKarti() {
        ekle(kart, dugme("Araba modunu aç", true, new Runnable() {
            @Override
            public void run() {
                Ayar.al(MainActivity.this).edit().putBoolean("araba", true).apply();
                startActivity(new Intent(MainActivity.this, ArabaActivity.class));
                panel.dismiss();
            }
        }), 8, 48);
        anahtar("Mesajları sesli oku", "Araba modunda yeni bildirimi okur. Bildirim okuma bölümü de açık olmalı.", "aoku", false);
        adres("Ev adresi (\"eve git\" için)", "evAdresi");
        adres("İş adresi (\"işe git\" için)", "isAdresi");
        aciklama("Adresler telefonda kalır, yedeğe girmez. Yol tarifi için harita uygulaması açılır.");
        soyleyin("Araba modu", "Eve git", "Araba modunu kapat");
        dikkat("Sürerken telefona bakmayın ve dokunmayın. OBD araç verisi henüz yok.");
    }

    private void adres(String ad, final String anahtarAdi) {
        ekle(kart, metin(ad, 13, SOLUK), 12, -2);
        EditText e = kutu("Mahalle, sokak, ilçe", Ayar.al(this).getString(anahtarAdi, ""));
        izle(e, new Yazildi() {
            @Override
            public void oldu(String s) {
                Ayar.al(MainActivity.this).edit().putString(anahtarAdi, s.trim()).apply();
            }
        });
        ekle(kart, e, 6, 48);
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

        onizleme = robotGorunumu();
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
        ekle(k, renkSirasi(), 10, -2);
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

        anahtar("Sürekli dinleme", "Yalnızca \"Sahra\" ile başlayan cümleye tepki verir. Sadece \"Sahra\" derseniz komutunuzu bekler. Adınızı ayırt etmek için mikrofon açık kalır ve duyulan ses telefonun tanıma hizmetine gidebilir. Pili daha çok harcar, bazı telefonlarda bip sesi çıkar.",
                "surekli", false);
    }

    private void izinKarti(boolean mikrofon, boolean ustte) {
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

    private void geminiKarti() {
        ekle(kart, metin("Yalnızca sorularınız için kullanılır; yalnızca soru cümlesi Google'a gönderilir. Ücretsiz kullanımda Google bunları ürün geliştirmede kullanabilir ve insanlar inceleyebilir. Kişisel bilgi ya da şifre sormayın.", 12, SOLUK), 4, -2);
        String kayitli = Gemini.anahtar(this);
        ekle(kart, metin(kayitli.isEmpty() ? "Anahtar kayıtlı değil."
                : "Anahtar kayıtlı (sonu " + kayitli.substring(Math.max(0, kayitli.length() - 4)) + ")", 13,
                kayitli.isEmpty() ? UYARI : VURGU), 10, -2);
        final EditText e = kutu(kayitli.isEmpty() ? "Anahtarı buraya yapıştırın" : "Değiştirmek için yeni anahtar", "");
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ekle(kart, e, 8, 48);
        ekle(kart, dugme("Anahtarı kaydet", true, new Runnable() {
            @Override
            public void run() {
                String yeni = e.getText().toString().trim();
                if (!yeni.matches("[A-Za-z0-9._-]{20,512}")) {
                    uyar("Anahtar eksik ya da hatalı görünüyor. Boşluk olmadan tamamını yapıştırın.");
                    return;
                }
                Gemini.anahtarKaydet(MainActivity.this, yeni);
                ciz();
                uyar("Kaydedildi. Şimdi \"Anahtarı dene\" düğmesine basın.");
            }
        }), 10, 46);
        if (kayitli.isEmpty()) return;
        ekle(kart, dugme("Anahtarı dene", false, new Runnable() {
            @Override
            public void run() {
                anahtarDene();
            }
        }), 8, 44);
        ekle(kart, dugme("Anahtarı sil", false, new Runnable() {
            @Override
            public void run() {
                Gemini.anahtarKaydet(MainActivity.this, "");
                ciz();
            }
        }), 8, 44);
    }

    private void anahtarDene() {
        final AlertDialog bekle = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setMessage("Gemini deneniyor…").show();
        new Thread() {
            @Override
            public void run() {
                String sonuc;
                try {
                    sonuc = "Anahtar çalışıyor.\n\n" + Gemini.sor(MainActivity.this, "Merhaba de.");
                } catch (Gemini.Hata e) {
                    sonuc = e.getMessage();
                }
                final String mesaj = sonuc;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        bekle.dismiss();
                        uyar(mesaj);
                    }
                });
            }
        }.start();
    }

    private void sesKarti() {
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
        gorunumOnizleme = robotGorunumu();
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(dp(96), dp(96));
        gl.gravity = Gravity.CENTER_HORIZONTAL;
        kart.addView(gorunumOnizleme, gl);
        ekle(kart, renkSirasi(), 10, -2);
        kaydirici("Boyut", "boyut", 46, 184, 92, " dp", SahraService.YENILE);
    }

    private void beyinKarti() {
        aciklama("Sahra son aramayı, çıkan sonuçları ve girdiği siteyi 10 dakika aklında tutar; devamını ona göre anlar. "
                + "Sonuca girebilmesi için ekran kontrolü açık olmalı.");
        dikkat(EkranService.acik != null ? "Ekran kontrolü açık." : "Ekran kontrolü kapalı: ana ekrandaki Kurulum bölümünden açın.");
        soyleyin("Chrome'da Trendyol ara", "Aç gir", "İkinci siteye gir", "Sonraki siteye gir",
                "Sahibinden sitesine gir", "trendyol nokta com'a gir", "Son siteyi aç", "Ne aramıştık");
        anahtar("Girdiğim siteleri hatırla",
                "Adı site adresiyle birebir eşleşenleri öğrenir; sonra aramadan doğrudan açar. Yalnızca telefonda durur. "
                        + "“Yanlış site” derseniz o kaydı siler.", "beyinOgren", true);
        List<String> siteler = Bellek.liste(this);
        ekle(kart, metin(siteler.isEmpty() ? "Henüz öğrenilen site yok." : "Öğrenilen siteler\n" + TextUtils.join("\n", siteler),
                13, SOLUK), 14, -2);
        ekle(kart, dugme("Belleği temizle", false, new Runnable() {
            @Override
            public void run() {
                Bellek.temizle(MainActivity.this);
                panelCiz();
            }
        }), 10, 46);
    }

    private void kayitKarti() {
        aciklama("Siteye kayıt olurken “formu doldur” deyin. Sahra site adını söyleyip onay ister, tanıdığı boş kutuları "
                + "doldurur. Kayıt düğmesine siz basarsınız.");
        dikkat("Onay sorusundaki site adını kontrol edin; sahte sitelere bilgi yazdırmayın. Bilgiler yalnızca bu telefonda "
                + "durur; yedeğe ve yapay zekâya gitmez. Şifre şifrelenerek saklanır. Banka şifrenizi buraya kaydetmeyin.");
        final String[][] alanlar = {{"ad", "Ad"}, {"soyad", "Soyad"}, {"eposta", "E-posta (Gmail)"},
                {"kullanici", "Kullanıcı adı (isteğe bağlı)"}, {"telefon", "Telefon (isteğe bağlı)"}};
        final int[] turler = {InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_CLASS_PHONE};
        final EditText[] kutular = new EditText[alanlar.length];
        for (int i = 0; i < alanlar.length; i++) {
            kutular[i] = kutu(alanlar[i][1], KayitBilgi.oku(this, alanlar[i][0]));
            kutular[i].setInputType(turler[i]);
            ekle(kart, kutular[i], 10, 46);
        }
        final EditText sifre = gizliKutu(KayitBilgi.sifreVar(this) ? "Şifre kayıtlı (değiştirmek için yazın)" : "Şifre");
        ekle(kart, sifre, 10, 46);
        final EditText tekrar = gizliKutu("Şifre tekrarı");
        ekle(kart, tekrar, 10, 46);
        ekle(kart, dugme("Kaydet", true, new Runnable() {
            @Override
            public void run() {
                String s = sifre.getText().toString();
                if (!s.equals(tekrar.getText().toString())) {
                    uyar("Şifre ile tekrarı aynı değil.");
                    return;
                }
                for (int i = 0; i < alanlar.length; i++) {
                    KayitBilgi.yaz(MainActivity.this, alanlar[i][0], kutular[i].getText().toString());
                }
                if (!s.isEmpty() && !KayitBilgi.sifreSakla(MainActivity.this, s)) {
                    uyar("Şifre güvenli biçimde saklanamadı; kaydedilmedi.");
                    return;
                }
                panelCiz();
                uyar("Kaydedildi.");
            }
        }), 12, 46);
        ekle(kart, dugme("Kayıt bilgilerimi sil", false, new Runnable() {
            @Override
            public void run() {
                KayitBilgi.sil(MainActivity.this);
                panelCiz();
            }
        }), 10, 46);
        soyleyin("Formu doldur", "E-postamı yaz (önce kutuya dokunun)", "Şifremi yaz (önce kutuya dokunun)");
    }

    private EditText gizliKutu(String ipucu) {
        EditText e = kutu(ipucu, "");
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    private void isikKarti() {
        // Önizleme kaydırılan alanın dışında durur; ayar yaparken hep görünür
        isikOnizleme = robotGorunumu();
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(dp(112), dp(112));
        ol.gravity = Gravity.CENTER_HORIZONTAL;
        panel.sabitle(isikOnizleme, ol);

        final TextView[] takimlar = new TextView[Isik.TAKIMLAR.length];
        final TextView[] sekiller = new TextView[Isik.SEKILLER.length];
        final TextView[] daireler = new TextView[Isik.RENKLER.length];
        final TextView sekilAciklama = metin("", 12, SOLUK);
        final TextView orijinal = metin("Orijinal ışık (varsayılan)", 14, METIN);
        final LinearLayout ozel = new LinearLayout(this);
        ozel.setOrientation(LinearLayout.VERTICAL);
        final Runnable boyaSecim = new Runnable() {
            @Override
            public void run() {
                List<String> secili = Isik.secili(Ayar.al(MainActivity.this).getString("isikRenk", Isik.ILK_RENKLER));
                String liste = TextUtils.join(",", secili);
                String sekli = Isik.sekil(Ayar.al(MainActivity.this).getString("isikSekil", Isik.ORIJINAL));
                boolean ilkHali = sekli.equals(Isik.ORIJINAL);
                cipBoya(orijinal, ilkHali);
                ozel.setVisibility(ilkHali ? View.GONE : View.VISIBLE);
                sekilAciklama.setVisibility(ilkHali ? View.GONE : View.VISIBLE);
                for (int i = 0; i < takimlar.length; i++) {
                    cipBoya(takimlar[i], !ilkHali && Isik.TAKIMLAR[i][1].equals(liste));
                }
                for (int i = 0; i < sekiller.length; i++) {
                    boolean bu = Isik.SEKILLER[i][0].equals(sekli);
                    cipBoya(sekiller[i], bu);
                    if (bu) sekilAciklama.setText(Isik.SEKILLER[i][2]);
                }
                for (int i = 0; i < daireler.length; i++) {
                    int sira = ilkHali ? -1 : secili.indexOf(Isik.RENKLER[i][0]);
                    GradientDrawable g = sekil(Color.parseColor(Isik.RENKLER[i][2]), 22, 0);
                    g.setStroke(dp(sira >= 0 ? 3 : 1), sira >= 0 ? 0xFFFFFFFF : 0x40FFFFFF);
                    daireler[i].setBackground(g);
                    daireler[i].setText(sira >= 0 ? String.valueOf(sira + 1) : "");
                    daireler[i].setSelected(sira >= 0);
                }
            }
        };

        orijinal.setGravity(Gravity.CENTER);
        orijinal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isikSakla("isikSekil", Isik.ORIJINAL, boyaSecim);
            }
        });
        ekle(kart, orijinal, 6, 46);
        ekle(kart, metin("İlk hâli: yumuşak yeşil ışık, dinlerken gökkuşağı. Değiştirmek için aşağıdan seçin.", 12, SOLUK), 6, -2);

        ekle(kart, metin("Hazır takımlar", 13, VURGU), 16, -2);
        LinearLayout satir = null;
        for (int i = 0; i < takimlar.length; i++) {
            if (i % 3 == 0) {
                satir = new LinearLayout(this);
                ekle(kart, satir, 8, -2);
            }
            takimlar[i] = isikCipi(satir, Isik.TAKIMLAR[i][0], "isikRenk", Isik.TAKIMLAR[i][1], boyaSecim);
        }
        satir.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

        ekle(kart, metin("Renkler", 13, VURGU), 16, -2);
        ekle(kart, metin("Birden fazla seçin; ışık seçtiğiniz sırayla renk değiştirir.", 12, SOLUK), 4, -2);
        for (int i = 0; i < daireler.length; i++) {
            if (i % 5 == 0) {
                satir = new LinearLayout(this);
                ekle(kart, satir, 8, -2);
            }
            final String kimlik = Isik.RENKLER[i][0];
            LinearLayout hucre = new LinearLayout(this);
            hucre.setOrientation(LinearLayout.VERTICAL);
            hucre.setGravity(Gravity.CENTER_HORIZONTAL);
            daireler[i] = metin("", 14, 0xFF0B0D13);
            daireler[i].setGravity(Gravity.CENTER);
            daireler[i].setTypeface(Typeface.DEFAULT_BOLD);
            daireler[i].setContentDescription("ışık " + kimlik);
            hucre.addView(daireler[i], new LinearLayout.LayoutParams(dp(40), dp(40)));
            hucre.addView(metin(Isik.RENKLER[i][1], 11, SOLUK));
            View.OnClickListener sec = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String eski = Ayar.al(MainActivity.this).getString("isikRenk", Isik.ILK_RENKLER);
                    // Orijinalden çıkarken yalnızca dokunulan renkle başlanır
                    isikSakla("isikRenk", orijinalIsik() ? kimlik : Isik.degistir(eski, kimlik), boyaSecim);
                }
            };
            hucre.setOnClickListener(sec);
            daireler[i].setOnClickListener(sec);
            satir.addView(hucre, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        ekle(kart, metin("Yanma şekli", 13, VURGU), 16, -2);
        for (int i = 0; i < sekiller.length; i++) {
            if (i % 3 == 0) {
                satir = new LinearLayout(this);
                ekle(kart, satir, 8, -2);
            }
            sekiller[i] = isikCipi(satir, Isik.SEKILLER[i][1], "isikSekil", Isik.SEKILLER[i][0], boyaSecim);
        }
        satir.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        ekle(kart, sekilAciklama, 8, -2);
        // Hız ve parlaklık ayarları yalnızca kendi ışığınız seçiliyken görünür
        final LinearLayout panelIcerik = kart;
        ekle(panelIcerik, ozel, 0, -2);
        kart = ozel;
        kaydirici("Yanıp sönme hızı", "isikHiz", 0, 100, Isik.ILK_HIZ, "", SahraService.YENILE);
        kaydirici("En parlak hâli", "isikUst", 40, 100, Isik.ILK_UST, "%", SahraService.YENILE);
        kaydirici("En sönük hâli", "isikAlt", 0, 70, Isik.ILK_ALT, "%", SahraService.YENILE);
        kaydirici("Işığın yayılması", "isikYay", 0, 100, Isik.ILK_YAY, "%", SahraService.YENILE);
        anahtar("Gözler de aynı renkte olsun", "Kalp, yıldız, kızgın gibi davranışlar kendi renginde kalır.",
                "isikGoz", true);
        anahtar("Dinlerken de benim renklerim", "Kapalıyken dinlerken ve konuşurken gökkuşağı yanar.",
                "isikDinle", false);
        kart = panelIcerik;
        boyaSecim.run();
    }

    private boolean orijinalIsik() {
        return Isik.sekil(Ayar.al(this).getString("isikSekil", Isik.ORIJINAL)).equals(Isik.ORIJINAL);
    }

    private TextView isikCipi(LinearLayout satir, String ad, final String anahtarAdi, final String deger,
                              final Runnable sonra) {
        TextView c = metin(ad, 13, METIN);
        c.setGravity(Gravity.CENTER);
        c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isikSakla(anahtarAdi, deger, sonra);
            }
        });
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, dp(40), 1f);
        l.leftMargin = dp(3);
        l.rightMargin = dp(3);
        satir.addView(c, l);
        return c;
    }

    private void isikSakla(String anahtarAdi, String deger, Runnable sonra) {
        // Renk ya da takım seçmek orijinal ışıktan "Nefes"e geçirir
        if (anahtarAdi.equals("isikRenk") && orijinalIsik()) {
            Ayar.al(this).edit().putString("isikSekil", Isik.SEKILLER[0][0]).apply();
        }
        Ayar.al(this).edit().putString(anahtarAdi, deger).apply();
        sonra.run();
        onizlemeBoya();
        yenile();
    }

    private void cipBoya(TextView c, boolean secili) {
        c.setSelected(secili);
        c.setTextColor(secili ? 0xFF06231A : METIN);
        c.setBackground(basilabilir(sekil(secili ? VURGU : ALAN, 12, secili ? 0 : CIZGI)));
    }

    private void davranisKarti() {
        ekle(kart, metin("Rastgele: 20 davranış sırayla karışık oynar. Normal: sade gözler. Birini seçerseniz hep onu yapar.", 12, SOLUK), 4, -2);
        davranisOnizleme = robotGorunumu();
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(dp(104), dp(104));
        ol.gravity = Gravity.CENTER_HORIZONTAL;
        ol.topMargin = dp(8);
        kart.addView(davranisOnizleme, ol);
        final TextView[] cipler = new TextView[Davranis.LISTE.length];
        LinearLayout satir = null;
        for (int i = 0; i < cipler.length; i++) {
            // İlk satırda iki kip, sonra üçlü sıralar
            if (i == 0 || i >= 2 && (i - 2) % 3 == 0) {
                satir = new LinearLayout(this);
                ekle(kart, satir, 8, -2);
            }
            final String kimlik = Davranis.LISTE[i][0];
            cipler[i] = metin(Davranis.LISTE[i][1], 13, METIN);
            cipler[i].setGravity(Gravity.CENTER);
            cipler[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Ayar.al(MainActivity.this).edit().putString("davranis", kimlik).apply();
                    cipleriBoya(cipler);
                    onizlemeBoya();
                    yenile();
                }
            });
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, dp(40), 1f);
            l.leftMargin = dp(3);
            l.rightMargin = dp(3);
            satir.addView(cipler[i], l);
        }
        cipleriBoya(cipler);
    }

    private void cipleriBoya(TextView[] cipler) {
        for (int i = 0; i < cipler.length; i++) {
            boolean secili = Davranis.LISTE[i][0].equals(Ayar.davranis(this));
            cipler[i].setTextColor(secili ? 0xFF06231A : METIN);
            cipler[i].setBackground(basilabilir(sekil(secili ? VURGU : ALAN, 12, secili ? 0 : CIZGI)));
        }
    }

    private void panoKarti() {
        anahtar("Kayan yazılar", "Sahra beklerken aşağıda açık olanları sırayla gösterir.", "yaziAcik", true);
        anahtar("Hava durumu", "Kaynak: Open-Meteo.com", "havaAcik", true);
        String sehir = Ayar.al(this).getString("sehir", "");
        ekle(kart, dugme(sehir.isEmpty() ? "Şehir seç" : "Şehir:  " + sehir, sehir.isEmpty(), new Runnable() {
            @Override
            public void run() {
                sehirPenceresi();
            }
        }), 8, 46);
        anahtar("Güzel sözler", "Aşağıya kendi sözlerinizi yazarsanız yalnızca onlar gösterilir.", "sozAcik", true);
        EditText sozler = kutu("Her satıra bir söz", Ayar.al(this).getString("kendiSozler", ""));
        sozler.setSingleLine(false);
        sozler.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        sozler.setMinLines(2);
        sozler.setMaxLines(6);
        sozler.setGravity(Gravity.TOP);
        sozler.setPadding(dp(14), dp(10), dp(14), dp(10));
        izle(sozler, new Yazildi() {
            @Override
            public void oldu(String s) {
                Ayar.al(MainActivity.this).edit().putString("kendiSozler", s).apply();
            }
        });
        ekle(kart, sozler, 8, -2);
        sozDosyasiBolumu();
        anahtar("Dolar, euro, altın",
                "Döviz: TCMB gösterge satış kuru. Altın: uluslararası ons fiyatından hesaplanır; kuyumcu fiyatı farklıdır.",
                "kurAcik", true);
    }

    /** Telefondan yüklenen söz dosyası: yükleme, karışık, sıklık ve kategori anahtarları. */
    private void sozDosyasiBolumu() {
        final SozDepo.Veri v = SozDepo.al(this);
        boolean var = !v.sozler.isEmpty();
        ekle(kart, metin(var ? "Söz dosyası: " + v.sozler.size() + " söz, " + v.kategoriler.size() + " kategori"
                : "Söz dosyanız (.sqlite) yalnızca bu telefonda saklanır. Cuma mesajları ile Anne ve Anneler Günü kategorileri alınmaz.",
                var ? 14 : 12, var ? METIN : SOLUK), 14, -2);
        ekle(kart, dugme(var ? "Başka dosya yükle" : "SQLite dosyasından yükle", !var, new Runnable() {
            @Override
            public void run() {
                dosyaSec(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*"), SOZ_DOSYASI);
            }
        }), 8, 46);
        if (var && !Ayar.al(this).getString("kendiSozler", "").trim().isEmpty()) {
            ekle(kart, metin("Yukarıdaki kutuda kendi sözünüz yazılı olduğu için şu an dosya yerine onlar gösteriliyor. Dosyayı kullanmak için kutuyu boşaltın.",
                    12, UYARI), 8, -2);
        }
        anahtar("Karışık (rastgele)", "Açıkken açık kategorilerdeki sözler karışık gelir; kapalıyken sırayla.", "sozKarisik", true);
        ekle(kart, metin("Gözlerde ne sıklıkla söz görünsün", 13, SOLUK), 14, -2);
        LinearLayout satir = new LinearLayout(this);
        int secili = Ayar.al(this).getInt("sozSiklik", 1);
        for (int i = 0; i < Sozler.SIKLIK_ADLARI.length; i++) {
            final int n = i;
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, dp(40), 1f);
            l.leftMargin = i == 0 ? 0 : dp(8);
            satir.addView(dugme(Sozler.SIKLIK_ADLARI[i], i == secili, new Runnable() {
                @Override
                public void run() {
                    Ayar.al(MainActivity.this).edit().putInt("sozSiklik", n).apply();
                    ciz();
                }
            }), l);
        }
        ekle(kart, satir, 6, -2);
        ekle(kart, metin("Gözlerde yalnızca 90 karaktere kadar olan sözler kayar. \"Güzel söz söyle\" derseniz uzunlar da okunur.",
                12, SOLUK), 4, -2);
        if (!var) return;
        ekle(kart, metin("Kategoriler", 15, METIN), 16, -2);
        for (int i = 0; i < v.kategoriler.size(); i++) kategoriAnahtari(v.kategoriler.get(i), v.sayi(i));
        if (SozDepo.acik(this).isEmpty()) {
            ekle(kart, metin("Bütün kategoriler kapalı: şu an uygulamanın hazır sözleri gösteriliyor.", 12, UYARI), 8, -2);
        }
        ekle(kart, dugme("Hepsini aç", false, new Runnable() {
            @Override
            public void run() {
                Ayar.al(MainActivity.this).edit().putString("sozKapali", "").apply();
                ciz();
            }
        }), 12, 42);
        ekle(kart, dugme("Hepsini kapat", false, new Runnable() {
            @Override
            public void run() {
                StringBuilder hepsi = new StringBuilder();
                for (String ad : v.kategoriler) hepsi.append(ad).append('\n');
                Ayar.al(MainActivity.this).edit().putString("sozKapali", hepsi.toString()).apply();
                ciz();
            }
        }), 8, 42);
        ekle(kart, dugme("Yüklenen sözleri sil", false, new Runnable() {
            @Override
            public void run() {
                SozDepo.sil(MainActivity.this);
                ciz();
            }
        }), 8, 42);
    }

    private void kategoriAnahtari(final String ad, int sayi) {
        Switch s = new Switch(this);
        s.setText(ad + "  (" + sayi + ")");
        s.setContentDescription(ad);
        s.setTextSize(14);
        s.setTextColor(METIN);
        final String kapali = "\n" + Ayar.al(this).getString("sozKapali", "") + "\n";
        s.setChecked(!kapali.contains("\n" + ad + "\n"));
        int[][] durumlar = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(durumlar, new int[]{VURGU, 0xFFB8BFCE}));
        s.setTrackTintList(new ColorStateList(durumlar, new int[]{0x8834D399, 0x55B8BFCE}));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean acik) {
                boolean hepsiKapaliydi = SozDepo.acik(MainActivity.this).isEmpty();
                StringBuilder yeni = new StringBuilder();
                for (String x : Ayar.al(MainActivity.this).getString("sozKapali", "").split("\n")) {
                    if (!x.isEmpty() && !x.equals(ad)) yeni.append(x).append('\n');
                }
                if (!acik) yeni.append(ad).append('\n');
                Ayar.al(MainActivity.this).edit().putString("sozKapali", yeni.toString()).apply();
                // yalnızca "hepsi kapalı" uyarısı değişeceğinde panel yeniden çizilir; kaydırma yeri korunur
                if (SozDepo.acik(MainActivity.this).isEmpty() != hepsiKapaliydi) ciz();
            }
        });
        ekle(kart, s, 10, -2);
    }

    private void sozYukle(final Uri dosya) {
        new Thread() {
            @Override
            public void run() {
                String sonuc;
                try {
                    sonuc = SozDepo.yukle(getApplicationContext(), dosya) + " söz yüklendi.";
                } catch (IOException e) {
                    sonuc = "Söz dosyası yüklenemedi. kategoriler ve sozler tabloları olan bir SQLite dosyası seçin.";
                }
                final String mesaj = sonuc;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        ciz();
                        uyar(mesaj);
                    }
                });
            }
        }.start();
    }

    private void efektKarti() {
        anahtar("Ekranda hava efekti", "Seçtiğiniz şehrin havasına göre ara sıra kar, yağmur, yaprak ya da güneş oynar. Dokunuşlarınızı engellemez.",
                "efektAcik", true);
        kaydirici("Kaç dakikada bir", "efektDakika", 1, 30, 5, " dk", SahraService.YENILE);
        String[][] denemeler = {{"Kar", "kar"}, {"Yağmur", "yagmur"}, {"Yaprak", "yaprak"}, {"Güneş", "gunes"}};
        int[] simgeler = {R.drawable.ik_kar, R.drawable.ik_damla, R.drawable.ik_yaprak, R.drawable.ik_gunes};
        LinearLayout satir = null;
        for (int i = 0; i < denemeler.length; i++) {
            if (i % 2 == 0) {
                satir = new LinearLayout(this);
                ekle(kart, satir, 8, -2);
            }
            final String tur = denemeler[i][1];
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, dp(44), 1f);
            l.leftMargin = dp(3);
            l.rightMargin = dp(3);
            TextView efektDugmesi = dugme(denemeler[i][0], false, new Runnable() {
                @Override
                public void run() {
                    if (SahraService.calisiyor) {
                        startForegroundService(new Intent(MainActivity.this, SahraService.class)
                                .putExtra(SahraService.EFEKT_DENE, tur));
                    } else {
                        uyar("Efekti denemek için önce Sahra'yı başlatın.");
                    }
                }
            });
            efektDugmesi.setCompoundDrawablesRelativeWithIntrinsicBounds(simgeler[i], 0, 0, 0);
            efektDugmesi.setPadding(dp(16), 0, dp(16), 0);
            satir.addView(efektDugmesi, l);
        }
    }

    private void sehirPenceresi() {
        final EditText e = kutu("örnek: Ankara", "");
        LinearLayout ic = new LinearLayout(this);
        ic.setPadding(dp(20), dp(12), dp(20), dp(4));
        ic.addView(e, new LinearLayout.LayoutParams(-1, dp(48)));
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Şehir ara").setView(ic)
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Ara", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                        sehirAra(e.getText().toString().trim());
                    }
                }).show();
    }

    private void sehirAra(final String aranan) {
        if (aranan.length() < 2) return;
        new Thread() {
            @Override
            public void run() {
                List<String[]> bulunan = null;
                try {
                    bulunan = Pano.sehirAra(aranan);
                } catch (IOException | JSONException e) {
                    // Aşağıda bildirilir
                }
                final List<String[]> sonuc = bulunan;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sehirListesi(sonuc);
                    }
                });
            }
        }.start();
    }

    private void sehirListesi(final List<String[]> sonuc) {
        if (isFinishing() || isDestroyed()) return;
        if (sonuc == null || sonuc.isEmpty()) {
            uyar(sonuc == null ? "Arama yapılamadı. İnterneti kontrol edin." : "Şehir bulunamadı.");
            return;
        }
        String[] adlar = new String[sonuc.size()];
        for (int i = 0; i < adlar.length; i++) adlar[i] = sonuc.get(i)[0];
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Şehrinizi seçin")
                .setItems(adlar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                        Pano.sehirSec(MainActivity.this, sonuc.get(i));
                        yenile();
                        ciz();
                    }
                }).show();
    }

    private void genelKarti() {
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
        ekle(kart, metin("Robota dokunup söyleyin. Basılı tutunca bu ekran açılır.", 12, SOLUK), 4, -2);
        String[][] gruplar = {
                {"Genel", "merhaba · saat kaç · tarih ne · pil kaç · bilgileri göster · robotu kapat"},
                {"Uygulama", "WhatsApp aç · YouTube'dan aksiyon filmi aç · Chrome'da hava durumu ara"},
                {"Müzik ve ses", "müzik aç · sonraki şarkı · müziği durdur · sessize al · sesi aç · sesi kıs"},
                {"Mesaj", "mesaj yaz akşam geliyorum · gönder"},
                {"Ekran", "ekranı oku · Tamam butonuna tıkla · geri · ana ekran · aşağı kaydır"},
                {"Sorular", "Atatürk kim · sor bana bir fıkra anlat · yapay zekâ nedir · Ankara hakkında bilgi · maç ne zaman · oku · okumayı durdur · pencereyi kapat"},
                {"Hava ve piyasa", "hava durumu · dolar kaç · altın kaç · güzel söz söyle · komik söz söyle · rastgele söz"},
                {"Efekt", "kar yağdır · yağmur yağdır · yaprak dök · güneş aç"},
                {"Alarm ve arama", "7'ye alarm kur · 10 dakika say · 8'de ilacı hatırlat · annemi ara"},
                {"Hızlı", "feneri aç · kamerayı aç · parlaklığı artır · eve git · Wi-Fi aç"},
                {"Günlük", "günaydın · bildirimleri oku · not al süt alınacak · notlarımı oku · İngilizceye çevir nasılsın · araba modu"},
                {"Robot", "büyü · küçül · rengini mavi yap · sesini değiştir · kalp göz yap · rastgele davran"}};
        for (String[] g : gruplar) {
            ekle(kart, metin(g[0], 13, VURGU), 12, -2);
            ekle(kart, metin(g[1], 14, METIN), 2, -2);
        }
    }

    private void ozelKarti(final boolean hazir) {
        final List<Ozel> liste = Yedek.komutlar(this);
        ekle(kart, metin(liste.isEmpty() ? "Henüz komut eklemediniz." : "Düzenlemek ya da çalıştırmak için komuta dokunun.", 12, SOLUK), 4, -2);
        for (final Ozel o : liste) {
            LinearLayout satir = new LinearLayout(this);
            satir.setOrientation(LinearLayout.VERTICAL);
            satir.setPadding(dp(12), dp(10), dp(12), dp(10));
            satir.setBackground(basilabilir(sekil(ALAN, 14, 0)));
            satir.addView(metin("“" + o.cumle + "”", 15, METIN));
            satir.addView(metin(o.isler.isEmpty() ? "Cevap: " + o.cevap : o.isler.replace("\n", " ; "), 12, SOLUK));
            satir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    secenekler(o, hazir);
                }
            });
            ekle(kart, satir, 8, -2);
        }
        ekle(kart, dugme("+ Komut ekle", true, new Runnable() {
            @Override
            public void run() {
                komutPenceresi(null);
            }
        }), 12, 46);
    }

    private void secenekler(final Ozel o, final boolean hazir) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle(o.cumle)
                .setItems(new String[]{"Çalıştır", "Düzenle", "Sil"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                        if (i == 1) {
                            komutPenceresi(o);
                        } else if (i == 2) {
                            komutDegistir(o.cumle, null);
                        } else if (hazir) {
                            startForegroundService(new Intent(MainActivity.this, SahraService.class)
                                    .putExtra(SahraService.KOMUT, o.cumle));
                        } else {
                            uyar("Önce mikrofon ve üstte gösterme izinlerini verin.");
                        }
                    }
                }).show();
    }

    private void komutPenceresi(final Ozel eski) {
        LinearLayout ic = new LinearLayout(this);
        ic.setOrientation(LinearLayout.VERTICAL);
        ic.setPadding(dp(20), dp(8), dp(20), dp(8));
        final EditText cumle = alan(ic, "Söyleyeceğiniz cümle", "örnek: iş modu", eski == null ? "" : eski.cumle, 1);
        final EditText isler = alan(ic, "Yapılacaklar (her satıra bir komut)", "sessize al\nWhatsApp aç",
                eski == null ? "" : eski.isler, 3);
        final EditText cevap = alan(ic, "Sahra'nın cevabı (isteğe bağlı)", "İş modu hazır hocam",
                eski == null ? "" : eski.cevap, 1);
        ScrollView s = new ScrollView(this);
        s.addView(ic);
        final AlertDialog d = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(eski == null ? "Yeni komut" : "Komutu düzenle").setView(s)
                .setPositiveButton("Kaydet", null).setNegativeButton("Vazgeç", null).create();
        d.show();
        // Hata varsa pencere kapanmasın diye düğme sonradan bağlanır
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ozel yeni = new Ozel(cumle.getText().toString(), isler.getText().toString(), cevap.getText().toString());
                if (Ozel.anahtar(yeni.cumle).isEmpty()) {
                    cumle.setError("Bir cümle yazın");
                    return;
                }
                if (yeni.isler.isEmpty() && yeni.cevap.isEmpty()) {
                    isler.setError("Yapılacak bir iş ya da cevap yazın");
                    return;
                }
                for (String adim : Ozel.adimlar(yeni.isler)) {
                    if (Komut.coz(adim).tur == Komut.Tur.BILINMIYOR) {
                        isler.setError("Sahra bu komutu bilmiyor: " + adim);
                        return;
                    }
                }
                d.dismiss();
                komutDegistir(eski == null ? null : eski.cumle, yeni);
            }
        });
    }

    /** eskiCumle silinir; yeni null değilse eklenir. Ardından yedeklenir. */
    private void komutDegistir(String eskiCumle, Ozel yeni) {
        List<Ozel> liste = Yedek.komutlar(this);
        int yer = liste.size();
        for (String silinecek : new String[]{eskiCumle, yeni == null ? null : yeni.cumle}) {
            Ozel var = silinecek == null ? null : Ozel.bul(liste, silinecek);
            if (var != null) {
                yer = Math.min(yer, liste.indexOf(var));
                liste.remove(var);
            }
        }
        if (yeni != null) liste.add(Math.min(yer, liste.size()), yeni);
        Yedek.kaydet(this, liste);
        Yedek.otomatik(this);
        ciz();
    }

    private EditText alan(LinearLayout nereye, String etiket, String ipucu, String deger, int satir) {
        ekle(nereye, metin(etiket, 13, SOLUK), 10, -2);
        EditText e = kutu(ipucu, deger);
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        if (satir > 1) {
            e.setSingleLine(false);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setMinLines(satir);
            e.setGravity(Gravity.TOP);
        }
        ekle(nereye, e, 6, -2);
        return e;
    }

    private void yedekKarti() {
        String zaman = Ayar.al(this).getString("yedekZamani", "");
        ekle(kart, metin((zaman.isEmpty() ? "Henüz otomatik yedek alınmadı." : "Son otomatik yedek: " + zaman)
                + "\nKonum: İndirilenler/Sahra/" + Yedek.DOSYA
                + "\nKomutlarınız ve ayarlarınız yedeklenir. Dosyayı okuyabilen herkes içindekileri görebilir.", 12, SOLUK), 4, -2);
        ekle(kart, dugme("Şimdi yedekle", true, new Runnable() {
            @Override
            public void run() {
                boolean oldu = Yedek.otomatik(MainActivity.this);
                ciz();
                uyar(oldu ? "Yedek İndirilenler/Sahra klasörüne kaydedildi."
                        : "Otomatik yedek yapılamadı. \"Farklı kaydet\" düğmesini kullanın.");
            }
        }), 10, 46);
        ekle(kart, dugme("Farklı kaydet", false, new Runnable() {
            @Override
            public void run() {
                dosyaSec(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE, Yedek.DOSYA), FARKLI_KAYDET);
            }
        }), 8, 44);
        ekle(kart, dugme("Yedekten geri yükle", false, new Runnable() {
            @Override
            public void run() {
                // Bazı telefonlar .json dosyasının türünü tanımadığı için tüm dosyalar gösterilir
                dosyaSec(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*"), GERI_YUKLE);
            }
        }), 8, 44);
    }

    private void dosyaSec(Intent i, int istek) {
        try {
            startActivityForResult(i.addCategory(Intent.CATEGORY_OPENABLE), istek);
        } catch (RuntimeException e) {
            uyar("Telefonda dosya seçici açılamadı.");
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
        TextView rozet = metin(tamam ? "Açık" : "İzin ver", 12, tamam ? VURGU : UYARI);
        rozet.setPadding(dp(10), dp(5), dp(10), dp(5));
        rozet.setBackground(sekil(tamam ? 0x2234D399 : 0x22FBBF24, 12, 0));
        satir.addView(rozet);
        ekle(kart, satir, 8, -2);
    }

    private Switch anahtar(String ad, String aciklama, final String anahtarAdi, boolean ilk) {
        Switch s = new Switch(this);
        s.setText(ad);
        s.setTextSize(15);
        s.setTextColor(METIN);
        s.setChecked(Ayar.al(this).getBoolean(anahtarAdi, ilk));
        int[][] durumlar = {{android.R.attr.state_checked}, {}};
        s.setThumbTintList(new ColorStateList(durumlar, new int[]{VURGU, 0xFFB8BFCE}));
        s.setTrackTintList(new ColorStateList(durumlar, new int[]{0x8834D399, 0x55B8BFCE}));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean acik) {
                Ayar.al(MainActivity.this).edit().putBoolean(anahtarAdi, acik).apply();
                onizlemeBoya();
                yenile();
            }
        });
        ekle(kart, s, 14, -2);
        if (!aciklama.isEmpty()) ekle(kart, metin(aciklama, 12, SOLUK), 4, -2);
        return s;
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
                onizlemeBoya();
                if (SahraService.calisiyor) {
                    startForegroundService(new Intent(MainActivity.this, SahraService.class).setAction(eylem));
                }
            }
        });
        ekle(kart, s, 6, -2);
    }

    /** Dokuz rengin tamamını tek sırada, kaydırma gerektirmeden gösterir. */
    private LinearLayout renkSirasi() {
        LinearLayout renkler = new LinearLayout(this);
        for (int i = 0; i < Komut.RENKLER.length; i++) {
            final String ad = Komut.RENKLER[i];
            FrameLayout hucre = new FrameLayout(this);
            View daire = new View(this);
            daire.setContentDescription(ad);
            hucre.addView(daire, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
            hucre.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View t) {
                    Ayar.al(MainActivity.this).edit().putString("renk", ad).apply();
                    renkIsaretle((LinearLayout) t.getParent());
                    onizlemeBoya();
                    yenile();
                }
            });
            renkler.addView(hucre, new LinearLayout.LayoutParams(0, dp(44), 1f));
        }
        renkIsaretle(renkler);
        return renkler;
    }

    private void renkIsaretle(LinearLayout renkler) {
        for (int i = 0; i < renkler.getChildCount(); i++) {
            boolean secili = Komut.RENKLER[i].equals(Ayar.renk(this));
            GradientDrawable g = sekil(RENK_ORNEK[i], 20, 0);
            g.setStroke(dp(secili ? 3 : 1), secili ? VURGU : 0x55FFFFFF);
            ((FrameLayout) renkler.getChildAt(i)).getChildAt(0).setBackground(g);
        }
    }

    private WebView robotGorunumu() {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.TRANSPARENT);
        w.getSettings().setJavaScriptEnabled(true);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String adres) {
                boya(v);
            }
        });
        w.loadUrl("file:///android_asset/robot.html");
        return w;
    }

    private void boya(WebView w) {
        if (w != null) {
            w.evaluateJavascript("window.robot&&robot('idle'," + JSONObject.quote(Ayar.renk(this)) + ","
                    + JSONObject.quote(Ayar.davranis(this)) + ")", null);
            w.evaluateJavascript("window.robotIsik&&robotIsik(" + Ayar.isik(this) + ")", null);
        }
    }

    private void onizlemeBoya() {
        boya(onizleme);
        boya(gorunumOnizleme);
        boya(davranisOnizleme);
        boya(isikOnizleme);
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
