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
import android.text.InputType;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

    private static final int FARKLI_KAYDET = 10;
    private static final int GERI_YUKLE = 11;

    private LinearLayout kok;
    private LinearLayout kart;
    private ScrollView kaydir;
    private WebView onizleme;
    private WebView davranisOnizleme;
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
        if (onizleme != null) onizleme.destroy();
        if (davranisOnizleme != null) davranisOnizleme.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        ciz();
    }

    private void ciz() {
        final int konum = kaydir == null ? 0 : kaydir.getScrollY();
        for (WebView eski : new WebView[]{onizleme, davranisOnizleme}) {
            if (eski != null) {
                ((LinearLayout) eski.getParent()).removeView(eski);
                eski.destroy();
            }
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
        ozelKarti(mikrofon && ustte);
        izinKarti(mikrofon, ustte);
        sesKarti();
        gorunumKarti();
        davranisKarti();
        genelKarti();
        yedekKarti();
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

    private void davranisKarti() {
        kart("Robot davranışı");
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
                {"Genel", "merhaba · saat kaç · tarih ne · pil kaç · bilgileri göster · robotu kapat"},
                {"Uygulama", "WhatsApp aç · YouTube'dan aksiyon filmi aç · Chrome'da hava durumu ara"},
                {"Müzik ve ses", "müzik aç · sonraki şarkı · müziği durdur · sessize al · sesi aç · sesi kıs"},
                {"Mesaj", "mesaj yaz akşam geliyorum · gönder"},
                {"Ekran", "ekranı oku · Tamam butonuna tıkla · geri · ana ekran · aşağı kaydır"},
                {"Sorular", "Atatürk kim · yapay zekâ nedir · Ankara hakkında bilgi · maç ne zaman · pencereyi kapat"},
                {"Robot", "büyü · küçül · rengini mavi yap · sesini değiştir · kalp göz yap · rastgele davran"}};
        for (String[] g : gruplar) {
            ekle(kart, metin(g[0], 13, VURGU), 12, -2);
            ekle(kart, metin(g[1], 14, METIN), 2, -2);
        }
    }

    private void ozelKarti(final boolean hazir) {
        kart("Kendi komutlarım");
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
        kart("Yedekleme (JSON)");
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
        }
    }

    private void onizlemeBoya() {
        boya(onizleme);
        boya(davranisOnizleme);
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
