package com.guven.sahra;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import android.provider.Browser;
import android.speech.tts.Voice;
import android.text.TextUtils;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.view.accessibility.AccessibilityNodeInfo;
import android.app.KeyguardManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Yüzen robot, ses tanıma ve konuşma. Çalışırken bildirimi görünür. */
public class SahraService extends Service {
    static final String KOMUT = "komut";
    static final String YENILE = "yenile";
    static final String DURDUR = "durdur";
    static final String SES_DENE = "ses_dene";
    static final String EFEKT_DENE = "efekt_dene";
    static final String SES_SIRADAKI = "ses_siradaki";
    static final String NET_SES_DENE = "net_ses_dene";
    static final String DINLE = "dinle";
    static final String SOR_VE_YAP = "sor_ve_yap";
    static boolean calisiyor;

    private final Handler ana = new Handler(Looper.getMainLooper());
    private WindowManager pencereler;
    private FrameLayout balon;
    private WebView web;
    private TextView yazi;
    private BalonCizim balonCizim;
    private BilgiPenceresi pencere;
    private Efekt efekt;
    private int yaziSirasi;
    private long sonSozZamani;
    private long sonEfekt = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
    private boolean yenileniyor;

    /** Boştayken sıradaki bilgiyi robotun gözlerinde kaydırır. */
    private final Runnable panoTuru = new Runnable() {
        @Override
        public void run() {
            ana.removeCallbacks(this);
            ana.postDelayed(this, 14000);
            if (!bosta()) return;
            veriYenile();
            if (!Pano.acik(SahraService.this, "yaziAcik")) return;
            long simdi = System.currentTimeMillis();
            int siklik = Ayar.al(SahraService.this).getInt("sozSiklik", 1);
            List<String> l = Pano.yazilar(SahraService.this, simdi - sonSozZamani >= Sozler.aralik(siklik));
            if (!l.isEmpty()) {
                // zamanı gelen söz beklemez; arada diğer bilgiler sırayla döner
                String s = l.contains(Pano.SOZ_YERI) ? Pano.SOZ_YERI : l.get(yaziSirasi++ % l.size());
                if (s.equals(Pano.SOZ_YERI)) {
                    s = Pano.soz(SahraService.this, Sozler.GOZ_SINIRI);
                    sonSozZamani = simdi;
                }
                // uzun yazı yarıda kesilmesin diye sonraki tur geciktirilir
                ana.removeCallbacks(this);
                ana.postDelayed(this, Math.max(14000, 5000 + s.length() * 290L));
                android.util.Log.i("SAHRADBG", "GOZ " + s.length() + " " + s);
                web.evaluateJavascript("window.robotYazi&&robotYazi(" + JSONObject.quote(s) + ")", null);
            }
        }
    };

    /** Belirlenen aralıkla havaya uyan ekran efektini oynatır. */
    private final Runnable efektTuru = new Runnable() {
        @Override
        public void run() {
            ana.postDelayed(this, 30000);
            long aralik = Ayar.al(SahraService.this).getInt("efektDakika", 5) * 60000L;
            if (!bosta() || !Pano.acik(SahraService.this, "efektAcik") || pencere.acik()
                    || System.currentTimeMillis() - sonEfekt < aralik) return;
            String tur = Pano.havaEfekti(SahraService.this);
            if (!tur.isEmpty()) efektOynat(tur);
        }
    };

    private final Runnable efektBitir = new Runnable() {
        @Override
        public void run() {
            efektKaldir();
        }
    };

    /** "Komik söz söyle" gibi istekler; anahtarı kapalı olsa da adıyla istenen kategori okunur. */
    private String kategoriSozu(String ad) {
        if (!SozDepo.var(this)) return "Önce Boştayken gözlerde yazı bölümünden söz dosyasını yükleyin.";
        int kat = Sozler.kategoriBul(SozDepo.al(this).kategoriler, ad);
        String s = kat < 0 ? null : SozDepo.sec(this, java.util.Collections.singleton(kat), Sozler.EN_UZUN);
        return s == null ? "Dosyada bu kategoride söz bulamadım." : Ayar.hitap(this) + ". " + s;
    }

    private boolean bosta() {
        return !konusuyor && !(dinliyor && (elleDinleme || uyandi))
                && getSystemService(PowerManager.class).isInteractive();
    }

    private void veriYenile() {
        if (yenileniyor || !cevrimici()) return;
        yenileniyor = true;
        new Thread() {
            @Override
            public void run() {
                Pano.yenile(SahraService.this);
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        yenileniyor = false;
                    }
                });
            }
        }.start();
    }

    private void efektOynat(String tur) {
        efektKaldir();
        if (tur.isEmpty()) return;
        sonEfekt = System.currentTimeMillis();
        efekt = new Efekt(this, tur);
        // Dokunuşlar alttaki uygulamaya geçer; Android bunun için en çok %80 görünürlüğe izin verir
        WindowManager.LayoutParams p = pencereAyari(-1, -1, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        p.alpha = 0.8f;
        pencereler.addView(efekt, p);
        String ifade = "kar".equals(tur) ? "wow" : "yagmur".equals(tur) ? "sad" : "gunes".equals(tur) ? "cool" : "happy";
        web.evaluateJavascript("window.oyna&&oyna(" + JSONObject.quote(ifade) + ")", null);
        ana.postDelayed(efektBitir, Efekt.SURE + 100);
    }

    private void efektKaldir() {
        ana.removeCallbacks(efektBitir);
        if (efekt != null) {
            pencereler.removeView(efekt);
            efekt = null;
        }
    }

    /** İstenen veri eskiyse önce indirir, sonra cevaplar. */
    private void veriSoyle(final boolean havaMi) {
        final String h = Ayar.hitap(this);
        if (havaMi && !Pano.sehirVar(this)) {
            soyle(h + ", önce Sahra uygulamasından şehir seçin.");
            return;
        }
        final String istek = duyulan;
        new Thread() {
            @Override
            public void run() {
                Pano.yenile(SahraService.this);
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!calisiyor) return;
                        String veri = havaMi ? Pano.hava(SahraService.this) : Pano.kurlar(SahraService.this);
                        duyulan = istek;
                        soyle(veri.isEmpty() ? h + ", bilgiyi şu an alamadım. İnterneti kontrol edin."
                                : h + ".\n" + veri.replace("≈", "yaklaşık"));
                    }
                });
            }
        }.start();
        duyulan = "";
    }
    private int deneme;
    private String sonSoz = "";
    private boolean yerelSes;
    private boolean uyandi;
    private boolean komutBekle;
    private String duyulan = "";
    private final List<String> sayfalar = new ArrayList<String>();
    private int sayfa;
    private final Runnable sonrakiSayfa = new Runnable() {
        @Override
        public void run() {
            if (++sayfa < sayfalar.size()) sayfaGoster();
        }
    };
    private WindowManager.LayoutParams balonAyar;
    private WindowManager.LayoutParams yaziAyar;
    private SpeechRecognizer tanima;
    private TextToSpeech konusma;
    private boolean konusmaHazir;
    private String bekleyenSoz;
    private boolean dinliyor;
    private boolean konusuyor;
    private boolean elleDinleme;
    private boolean basladi;

    private final Runnable yenidenDinle = new Runnable() {
        @Override
        public void run() {
            if (!surekli() || dinliyor || konusuyor) return;
            if (!motorKipi()) {
                dinle(false);
            } else if (!motorHatasi) {
                motor.basla();
            }
        }
    };
    private final Runnable yaziGizle = new Runnable() {
        @Override
        public void run() {
            // Konuşurken ya da elle dinlerken balon kaybolmaz
            if (konusuyor || elleDinleme || sayfa < sayfalar.size() - 1) {
                ana.postDelayed(this, 800);
                return;
            }
            yazi.animate().alpha(0f).setDuration(220).withEndAction(new Runnable() {
                @Override
                public void run() {
                    yazi.setVisibility(View.GONE);
                }
            });
        }
    };
    private Runnable baslatici;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String eylem = intent == null ? null : intent.getAction();
        if (DURDUR.equals(eylem) || !Settings.canDrawOverlays(this)
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!basladi) {
            basladi = true;
            calisiyor = true;
            bildirim();
            robotKur();
            konusmaKur();
            netSes = new CevrimiciSes(this);
            motor = new UyandirmaMotoru(this, new UyandirmaMotoru.Olay() {
                @Override
                public void duyuldu() {
                    komutBekle = true;
                    duyulan = "";
                    soyle("Efendim " + Ayar.hitap(SahraService.this) + "?");
                }

                @Override
                public void hata(String neden) {
                    // Eski sürekli dinlemeye dönülür; ayar değişince yeniden denenir
                    motorHatasi = true;
                    goster(neden);
                    ana.postDelayed(yenidenDinle, 3000);
                }
            });
            BildirimService.dinleyen = new BildirimService.Yeni() {
                @Override
                public void geldi(final BildirimService.Kayit k) {
                    ana.post(new Runnable() {
                        @Override
                        public void run() {
                            if (calisiyor && ayar("araba", false) && ayar("aoku", false) && bosta()
                                    && bekleyenGirdi == null) {
                                duyulan = "";
                                uzunSoyle(k.oku());
                            }
                        }
                    });
                }
            };
            soyle("Merhaba " + Ayar.hitap(this) + ", emirlerinizi yerine getiriyorum.");
            veriYenile();
            ana.postDelayed(panoTuru, 8000);
            ana.postDelayed(efektTuru, 20000);
        }
        if (intent != null && intent.hasExtra(KOMUT)) {
            calistir(intent.getStringExtra(KOMUT));
        }
        if (SES_SIRADAKI.equals(eylem)) sesDegistir();
        if (DINLE.equals(eylem)) {
            sustur();
            deneme = 0;
            dinle(true);
        }
        if (intent != null && intent.hasExtra(SOR_VE_YAP)) hedefSor(intent.getStringExtra(SOR_VE_YAP));
        if (NET_SES_DENE.equals(eylem)) {
            netEngel = 0;
            duyulan = "";
            acikSoyle("Merhaba " + Ayar.hitap(this) + ", bu çevrimiçi sesim. Nasıl buldunuz?", true);
        }
        if (intent != null && intent.hasExtra(EFEKT_DENE)) efektOynat(intent.getStringExtra(EFEKT_DENE));
        if (SES_DENE.equals(eylem)) soyle("Merhaba " + Ayar.hitap(this) + ", sesim şimdi böyle. Nasıl buldunuz?");
        if (YENILE.equals(eylem)) {
            boyutla(Ayar.boyut(this));
            veriYenile();
            if (!Pano.acik(this, "efektAcik")) efektKaldir();
            motorHatasi = false;
            if (!elleDinleme) dinlemeyiBirak();
            if (surekli()) ana.postDelayed(yenidenDinle, 500);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        calisiyor = false;
        ana.removeCallbacksAndMessages(null);
        BildirimService.dinleyen = null;
        Hafiza.temizle();
        if (motor != null) motor.kapat();
        if (netSes != null) netSes.durdur();
        if (tanima != null) tanima.destroy();
        if (konusma != null) konusma.shutdown();
        if (pencere != null) pencere.kapat();
        if (pencereler != null) efektKaldir();
        if (balon != null) {
            pencereler.removeView(balon);
            pencereler.removeView(yazi);
            web.destroy();
        }
        super.onDestroy();
    }

    private boolean surekli() {
        return Ayar.al(this).getBoolean("surekli", false);
    }

    private boolean ayar(String ad, boolean ilk) {
        return Ayar.al(this).getBoolean(ad, ilk);
    }

    /** "Sahra" adı telefonda mı beklenecek? */
    private boolean motorKipi() {
        return ayar("uyan", false) && !motorHatasi && ModelIndir.hazir(this);
    }

    private void bildirim() {
        NotificationManager n = getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel("sahra", "Sahra", NotificationManager.IMPORTANCE_LOW));
        PendingIntent ac = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dur = PendingIntent.getService(this, 1,
                new Intent(this, SahraService.class).setAction(DURDUR), PendingIntent.FLAG_IMMUTABLE);
        Notification b = new Notification.Builder(this, "sahra")
                .setSmallIcon(R.drawable.ikon)
                .setContentTitle(Ayar.ad(this) + " çalışıyor")
                .setContentText("Robota dokunup komut verin")
                .setContentIntent(ac)
                .addAction(new Notification.Action.Builder(null, "Kapat", dur).build())
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(1, b, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1, b);
        }
    }

    // ---------- Yüzen robot ----------

    private void robotKur() {
        pencereler = (WindowManager) getSystemService(WINDOW_SERVICE);
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                durum();
            }
        });
        web.loadUrl("file:///android_asset/robot.html");

        View dokunma = new View(this);
        dokunma.setContentDescription("Sahra robotu");
        dokunma.setOnTouchListener(new Surukle());
        balon = new FrameLayout(this);
        balon.addView(web, new FrameLayout.LayoutParams(-1, -1));
        balon.addView(dokunma, new FrameLayout.LayoutParams(-1, -1));

        int boy = dp(Ayar.boyut(this));
        balonAyar = pencereAyari(boy, boy, 0);
        balonAyar.x = Ayar.al(this).getInt("x", 0);
        balonAyar.y = Ayar.al(this).getInt("y", ekran().heightPixels / 4);
        pencereler.addView(balon, balonAyar);

        yazi = new TextView(this);
        yazi.setTextColor(0xFF14161F);
        yazi.setTextSize(15);
        yazi.setLineSpacing(0, 1.1f);
        yazi.setMaxLines(12);
        yazi.setEllipsize(TextUtils.TruncateAt.END);
        yazi.setMaxWidth(Math.min(dp(260), ekran().widthPixels - dp(24)));
        balonCizim = new BalonCizim(ekran().density);
        yazi.setBackground(balonCizim);
        yazi.setVisibility(View.GONE);
        // Balon dokunmaları alttaki uygulamaya bırakır (Android bunun için saydamlık ister)
        yaziAyar = pencereAyari(-2, -2, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        yaziAyar.alpha = 0.8f;
        pencereler.addView(yazi, yaziAyar);

        pencere = new BilgiPenceresi(this, pencereler, new Runnable() {
            @Override
            public void run() {
                sustur();
                if (surekli()) ana.postDelayed(yenidenDinle, 600);
            }
        }, new Runnable() {
            @Override
            public void run() {
                if (dinliyor) dinlemeyiBirak();
                pencereyiOku();
            }
        });
    }

    /** Balonu robotun üstüne, yer yoksa altına koyar; kuyruk robotu gösterir. */
    private void balonYerlestir() {
        int bosluk = dp(12);
        boolean ustte = true;
        int en = 0;
        int boy = 0;
        for (int i = 0; i < 2; i++) {
            balonCizim.kuyrukAltta = ustte;
            yazi.setPadding(dp(16), dp(ustte ? 11 : 20), dp(16), dp(ustte ? 20 : 11));
            yazi.measure(View.MeasureSpec.makeMeasureSpec(ekran().widthPixels - bosluk * 2, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            en = yazi.getMeasuredWidth();
            boy = yazi.getMeasuredHeight();
            if (ustte && balonAyar.y - boy < 0) ustte = false;
            else break;
        }
        int orta = balonAyar.x + balonAyar.width / 2;
        yaziAyar.x = Math.max(bosluk, Math.min(orta - en / 2, ekran().widthPixels - en - bosluk));
        yaziAyar.y = ustte ? balonAyar.y - boy : balonAyar.y + balonAyar.height;
        balonCizim.kuyrukX = orta - yaziAyar.x;
        balonCizim.invalidateSelf();
        pencereler.updateViewLayout(yazi, yaziAyar);
    }

    private WindowManager.LayoutParams pencereAyari(int en, int boy, int ek) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(en, boy,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | ek, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void durum() {
        String d = dinliyor && (elleDinleme || uyandi) || konusuyor ? "active" : "idle";
        web.evaluateJavascript("window.robot&&robot(" + JSONObject.quote(d) + "," + JSONObject.quote(Ayar.renk(this)) + "," + JSONObject.quote(Ayar.davranis(this)) + ")", null);
        web.evaluateJavascript("window.robotIsik&&robotIsik(" + Ayar.isik(this) + ")", null);
    }

    /** Metni balonda gösterir; uzunsa sayfalara bölüp sırayla hepsini gösterir. */
    private void goster(String metin) {
        ana.removeCallbacks(sonrakiSayfa);
        sayfalar.clear();
        sayfalar.addAll(Sayfa.bol(metin, 260));
        sayfa = 0;
        sayfaGoster();
    }

    private void sayfaGoster() {
        String metin = sayfalar.get(sayfa);
        if (sayfalar.size() > 1) {
            metin += "\n" + (sayfa + 1) + "/" + sayfalar.size();
            ana.postDelayed(sonrakiSayfa, 2000 + sayfalar.get(sayfa).length() * 60);
        }
        boolean yeni = yazi.getVisibility() != View.VISIBLE;
        yazi.animate().cancel();
        yazi.setText(metin);
        balonYerlestir();
        yazi.setVisibility(View.VISIBLE);
        if (yeni) {
            yazi.setAlpha(0f);
            yazi.animate().alpha(1f).setDuration(180);
        } else {
            yazi.setAlpha(1f);
        }
        ana.removeCallbacks(yaziGizle);
        ana.postDelayed(yaziGizle, Math.min(9000, 2500 + metin.length() * 55));
    }

    private void boyutla(int yeni) {
        int b = Math.max(46, Math.min(yeni, 184));
        Ayar.al(this).edit().putInt("boyut", b).apply();
        balonAyar.width = dp(b);
        balonAyar.height = dp(b);
        pencereler.updateViewLayout(balon, balonAyar);
        if (yazi.getVisibility() == View.VISIBLE) balonYerlestir();
        durum();
    }

    private final class Surukle implements View.OnTouchListener {
        private float ilkX;
        private float ilkY;
        private int bas;
        private int ust;
        private boolean tasiniyor;
        private long an;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ilkX = e.getRawX();
                    ilkY = e.getRawY();
                    bas = balonAyar.x;
                    ust = balonAyar.y;
                    tasiniyor = false;
                    an = e.getEventTime();
                    break;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - ilkX;
                    float dy = e.getRawY() - ilkY;
                    if (Math.hypot(dx, dy) > ViewConfiguration.get(SahraService.this).getScaledTouchSlop()) tasiniyor = true;
                    if (tasiniyor) {
                        balonAyar.x = Math.max(0, Math.min(bas + Math.round(dx), ekran().widthPixels - balonAyar.width));
                        balonAyar.y = Math.max(0, Math.min(ust + Math.round(dy), ekran().heightPixels - balonAyar.height));
                        pencereler.updateViewLayout(balon, balonAyar);
                        if (yazi.getVisibility() == View.VISIBLE) balonYerlestir();
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                    if (tasiniyor) {
                        Ayar.al(SahraService.this).edit().putInt("x", balonAyar.x).putInt("y", balonAyar.y).apply();
                    } else if (e.getEventTime() - an > 600) {
                        startActivity(new Intent(SahraService.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } else {
                        sustur();
                        deneme = 0;
                        dinle(true);
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    // ---------- Dinleme ----------

    private void dinlemeyiBirak() {
        ana.removeCallbacks(yenidenDinle);
        if (motor != null) motor.durdur();
        if (baslatici != null) ana.removeCallbacks(baslatici);
        if (tanima != null) {
            tanima.destroy();
            tanima = null;
        }
        dinliyor = false;
        elleDinleme = false;
        uyandi = false;
        durum();
    }

    /** Denenecek ses tanıma hizmetleri: son çalışan, Google, sonra telefonun varsayılanı (null). */
    private List<ComponentName> tanimaAdaylari() {
        List<ComponentName> l = new ArrayList<ComponentName>();
        List<ResolveInfo> hizmetler = getPackageManager()
                .queryIntentServices(new Intent("android.speech.RecognitionService"), 0);
        for (String paket : new String[]{"com.google.android.googlequicksearchbox", "com.google.android.tts"}) {
            for (ResolveInfo r : hizmetler) {
                if (r.serviceInfo.packageName.equals(paket)) {
                    l.add(new ComponentName(paket, r.serviceInfo.name));
                    break;
                }
            }
        }
        l.add(null);
        String iyi = Ayar.al(this).getString("tanima", "");
        for (int i = 0; i < l.size(); i++) {
            if (ad(l.get(i)).equals(iyi)) {
                l.add(0, l.remove(i));
                break;
            }
        }
        return l;
    }

    private static String ad(ComponentName c) {
        return c == null ? "varsayilan" : c.flattenToString();
    }

    private ComponentName secilen;

    private void dinle(final boolean elle) {
        dinlemeyiBirak();
        List<ComponentName> adaylar = tanimaAdaylari();
        secilen = adaylar.get(deneme % adaylar.size());
        if (secilen == null && !SpeechRecognizer.isRecognitionAvailable(this)) {
            goster("Telefonda ses tanıma hizmeti bulunamadı. Google uygulamasını kurun.");
            return;
        }
        dinliyor = true;
        elleDinleme = elle;
        duyulan = "";
        if (elle) goster("Dinliyorum " + Ayar.hitap(this) + "…");
        durum();
        // Eski bağlantı kapanmadan yenisi açılırsa hizmet kopabiliyor; kısa süre beklenir
        baslatici = new Runnable() {
            @Override
            public void run() {
                tanima = secilen == null ? SpeechRecognizer.createSpeechRecognizer(SahraService.this)
                        : SpeechRecognizer.createSpeechRecognizer(SahraService.this, secilen);
                tanima.setRecognitionListener(new Dinleyici());
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                if (!cevrimici()) i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
                tanima.startListening(i);
            }
        };
        ana.postDelayed(baslatici, 250);
    }

    private boolean cevrimici() {
        return getSystemService(ConnectivityManager.class).getActiveNetwork() != null;
    }

    private static String hataMetni(int hata) {
        switch (hata) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Sizi duyamadım, tekrar dokunun.";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "İnternet bağlantısı sorunu. Çevrimdışı kullanım için Türkçe dil paketini indirin.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Mikrofon izni eksik. Sahra'ya ve Google uygulamasına mikrofon izni verin.";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Mikrofon kullanılamıyor. Başka bir uygulama kullanıyor olabilir.";
            case 12:
            case 13:
                return "Türkçe ses tanıma hazır değil. Google uygulamasından Türkçe dil paketini indirin.";
            default:
                return "Ses tanıma bağlanamadı (" + hata + "). Google uygulamasının güncel ve mikrofon izninin açık olduğunu kontrol edin.";
        }
    }

    private final class Dinleyici implements RecognitionListener {
        @Override
        public void onResults(Bundle sonuclar) {
            ArrayList<String> l = sonuclar.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            boolean elle = elleDinleme;
            dinliyor = false;
            elleDinleme = false;
            uyandi = false;
            deneme = 0;
            Ayar.al(SahraService.this).edit().putString("tanima", ad(secilen)).apply();
            durum();
            if (l != null && !l.isEmpty()) {
                if (elle) {
                    calistir(l.get(0));
                } else {
                    // "Sahra" ile başlamayan her şey iz bırakmadan atılır
                    String komut = Uyandirma.komut(l);
                    if (komut != null && komut.isEmpty()) {
                        komutBekle = true;
                        duyulan = "";
                        soyle("Efendim " + Ayar.hitap(SahraService.this) + "?");
                    } else if (komut != null) {
                        calistir(komut);
                    }
                }
            }
            if (surekli()) ana.postDelayed(yenidenDinle, 900);
        }

        @Override
        public void onError(int hata) {
            final boolean elle = elleDinleme;
            dinliyor = false;
            elleDinleme = false;
            uyandi = false;
            durum();
            boolean sessizlik = hata == SpeechRecognizer.ERROR_NO_MATCH || hata == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
            // Bağlantı hatalarında (5, 8, 10, 11, 12, 13) sıradaki hizmetle yeniden dene
            boolean baglanti = hata == SpeechRecognizer.ERROR_CLIENT || hata == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                    || hata >= 10 && hata <= 13 || hata == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
            if (baglanti && deneme < 3) {
                deneme++;
                ana.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!dinliyor && !konusuyor) dinle(elle);
                    }
                }, 700);
                return;
            }
            if (elle) goster(hataMetni(hata));
            if (!sessizlik) deneme++;
            // Hata sürerse sürekli dinleme giderek daha seyrek dener
            if (surekli()) ana.postDelayed(yenidenDinle, sessizlik ? 1200 : Math.min(30000, 2000 * Math.max(1, deneme)));
        }

        @Override public void onReadyForSpeech(Bundle b) { }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float v) { }
        @Override public void onBufferReceived(byte[] b) { }
        @Override public void onEndOfSpeech() { }
        /** Elle dinlemede duyulan hemen görünür; sürekli dinlemede ancak "Sahra" duyulunca. */
        @Override
        public void onPartialResults(Bundle b) {
            ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (l == null || l.isEmpty() || l.get(0).isEmpty()) return;
            if (!elleDinleme) {
                if (Uyandirma.komut(l.get(0)) == null) return;
                if (!uyandi) {
                    uyandi = true;
                    durum();
                }
            }
            goster("“" + l.get(0) + "”");
        }

        @Override public void onEvent(int t, Bundle b) { }
    }

    // ---------- Komut ve konuşma ----------

    private final List<Runnable> sirada = new ArrayList<Runnable>();

    private void calistir(String cumle) {
        // Yeni komut gelince önceki özel komutun kalan adımları iptal edilir
        for (Runnable r : sirada) ana.removeCallbacks(r);
        sirada.clear();
        komutSirasi++;
        duyulan = cumle;
        // Sahra bir soru sorduysa gelen cümle önce onun cevabı sayılır
        Girdi girdi = bekleyenGirdi;
        bekleyenGirdi = null;
        if (girdi != null && System.currentTimeMillis() - bekleyenZaman < 45000) {
            String yalin = Uyandirma.komut(cumle);
            if (girdi.al(yalin == null || yalin.isEmpty() ? cumle : yalin)) return;
        }
        Ozel ozel = Ozel.bul(Yedek.komutlar(this), cumle);
        if (ozel != null) {
            ozelCalistir(ozel);
        } else if (!beyinIsle(cumle)) {
            tekKomut(Komut.coz(cumle), null);
        }
    }

    /** Adımlar sırayla, uygulamaların açılmasına zaman tanıyarak çalışır. */
    private void ozelCalistir(final Ozel ozel) {
        List<String> adimlar = Ozel.adimlar(ozel.isler);
        final List<String> cevaplar = new ArrayList<String>();
        long bekle = 0;
        for (final String adim : adimlar) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    tekKomut(Komut.coz(adim), cevaplar);
                }
            };
            sirada.add(r);
            ana.postDelayed(r, bekle);
            bekle += 1800;
        }
        Runnable son = new Runnable() {
            @Override
            public void run() {
                String soz = ozel.cevap.isEmpty() ? TextUtils.join("\n", cevaplar) : ozel.cevap;
                // Soru adımı kendi cevabını söyler; üstüne konuşulmaz
                // Kendi cevabını söyleyen adım (alarm, hava gibi) da kesilmez
                if (soz.isEmpty() && (pencere.acik() || konusuyor)) return;
                soyle(soz.isEmpty() ? "Tamam " + Ayar.hitap(SahraService.this) + "." : soz);
            }
        };
        sirada.add(son);
        ana.postDelayed(son, Math.max(0, bekle - 1500));
    }

    /** toplanan null değilse cevap hemen söylenmez, listeye eklenir. */
    private void tekKomut(Komut k, List<String> toplanan) {
        String cevap = "Tamam " + Ayar.hitap(this) + ".";
        switch (k.tur) {
            case KAPAN:
                stopSelf();
                return;
            case BUYU:
            case KUCUL:
                boyutla(Ayar.boyut(this) + (k.tur == Komut.Tur.BUYU ? 20 : -20));
                break;
            case RENK:
                Ayar.al(this).edit().putString("renk", k.metin).apply();
                durum();
                break;
            case SES_DEGISTIR:
                sesDegistir();
                return;
            case DAVRANIS:
                Ayar.al(this).edit().putString("davranis", k.metin).apply();
                durum();
                break;
            case PENCERE_KAPAT:
                pencere.kapat();
                break;
            case HAVA:
            case KUR:
                veriSoyle(k.tur == Komut.Tur.HAVA);
                return;
            case EFEKT:
                efektOynat(k.metin);
                break;
            case GUZEL_SOZ:
                if (k.metin == null || k.metin.isEmpty()) {
                    cevap = Ayar.hitap(this) + ". " + Pano.soz(this);
                } else {
                    cevap = kategoriSozu(k.metin);
                }
                break;
            case OKUMA_DUR:
                sustur();
                goster("Okumayı durdurdum.");
                if (surekli()) ana.postDelayed(yenidenDinle, 600);
                return;
            case EKRAN_OKU:
                // Pencere açıksa arkadaki uygulama değil, penceredeki yazı okunur
                if (pencere.acik()) {
                    duyulan = "";
                    yazi.setVisibility(View.GONE);
                    pencereyiOku();
                    return;
                }
                cevap = new Yurutucu(this).uygula(k);
                break;
            case SORU:
            case GOOGLE_SORU:
            case YAPAY_SORU:
                sor(k);
                return;
            case ALARM:
                alarmKur(k.metin);
                return;
            case ZAMANLAYICI:
                zamanlayiciKur(k.metin);
                return;
            case HATIRLAT:
                hatirlat(k.metin);
                return;
            case ARA:
                kisiAra(k.metin);
                return;
            case OZET:
                sabahOzeti();
                return;
            case BILDIRIM_OKU:
                bildirimOku();
                return;
            case NOT_AL:
                notAl(k.metin);
                return;
            case NOT_OKU: {
                List<String> notlar = Notlar.liste(this);
                if (notlar.isEmpty()) {
                    cevap = Ayar.hitap(this) + ", kayıtlı notunuz yok.";
                    break;
                }
                int ilk = Math.max(0, notlar.size() - 10);
                uzunSoyle(notlar.size() + " notunuz var" + (ilk > 0 ? ", son 10 tanesi" : "") + ".\n"
                        + TextUtils.join(".\n", notlar.subList(ilk, notlar.size())));
                return;
            }
            case NOT_SIL:
                onaySor("Bütün notlar silinsin mi, " + Ayar.hitap(this) + "?", new Runnable() {
                    @Override
                    public void run() {
                        Notlar.kaydet(SahraService.this, new ArrayList<String>());
                        soyle("Notlar silindi.");
                    }
                });
                return;
            case CEVIR:
                ceviri(k.metin);
                return;
            case ARABA:
                arabaModu(k.metin.equals("ac"));
                cevap = k.metin.equals("ac") ? "Araba modu açıldı " + Ayar.hitap(this) + "."
                        : "Araba modu kapandı " + Ayar.hitap(this) + ".";
                break;
            case HAFIZA_SIL:
                Hafiza.temizle();
                Beyin.unut();
                cevap = "Konuştuklarımızı unuttum " + Ayar.hitap(this) + ".";
                break;
            case BILINMIYOR:
                if (toplanan == null && ayar("serbest", false) && Gemini.hazir(this) && cevrimici()) {
                    serbestAnla(k);
                    return;
                }
                cevap = new Yurutucu(this).uygula(k);
                break;
            case FORM_DOLDUR:
                formDoldur(k.metin);
                return;
            case WEB_ARA:
                Beyin.aramaYapildi(k.metin, System.currentTimeMillis());
                cevap = new Yurutucu(this).uygula(k);
                break;
            case YOUTUBE:
            case UYGULAMA_AC:
            case ANA_EKRAN:
                // Başka yere geçildi: yalnız "aç" artık eski aramayı kastetmez
                Beyin.unut();
                cevap = new Yurutucu(this).uygula(k);
                break;
            default:
                cevap = new Yurutucu(this).uygula(k);
        }
        if (toplanan == null) {
            soyle(cevap);
        } else {
            toplanan.add(cevap);
        }
    }

    private void konusmaKur() {
        TextToSpeech.OnInitListener hazir = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int sonuc) {
                if (sonuc != TextToSpeech.SUCCESS) return;
                konusma.setLanguage(new Locale("tr", "TR"));
                konusma.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) { bitti(); }
                    @Override public void onError(String id) { sesHatasi(); }
                });
                konusmaHazir = true;
                if (bekleyenSoz != null) konus(bekleyenSoz);
                bekleyenSoz = null;
            }
        };
        // Google ses motoru varsa onu kullan; Türkçe sesleri genelde daha doğaldır
        konusma = Ses.googleVar(this) ? new TextToSpeech(this, hazir, Ses.GOOGLE) : new TextToSpeech(this, hazir);
    }

    /** İnternetli ses başarısız olursa aynı cümle telefondaki sesle söylenir. */
    private void sesHatasi() {
        ana.post(new Runnable() {
            @Override
            public void run() {
                if (!yerelSes && !sonSoz.isEmpty()) {
                    yerelSes = true;
                    seslendir(sonSoz);
                } else {
                    bitti();
                }
            }
        });
    }

    private void sesDegistir() {
        if (!konusmaHazir) return;
        List<Voice> sesler = Ses.turkce(konusma, cevrimici());
        if (sesler.size() < 2) {
            soyle(Ayar.hitap(this) + ", telefonda başka Türkçe ses bulamadım. Ses ayarlarından indirebilirsiniz.");
            return;
        }
        Voice simdiki = Ses.sec(this, sesler);
        int sira = (sesler.indexOf(simdiki) + 1) % sesler.size();
        Ayar.al(this).edit().putString("ses", sesler.get(sira).getName()).apply();
        soyle("Merhaba " + Ayar.hitap(this) + ". Bu, " + sesler.size() + " sesten " + (sira + 1) + " numaralı olanı.");
    }

    private void bitti() {
        ana.post(new Runnable() {
            @Override
            public void run() {
                if (konusma != null && konusma.isSpeaking()) return;
                if (konusuyor && !okumaKuyrugu.isEmpty()) {
                    yerelSes = false;
                    sonSoz = okumaKuyrugu.remove(0);
                    seslendir(sonSoz);
                    return;
                }
                konusuyor = false;
                durum();
                if (komutBekle) {
                    // Yalnızca adı söylendi: sıradaki cümle "Sahra" demeden komut sayılır
                    komutBekle = false;
                    dinle(true);
                    return;
                }
                if (surekli()) ana.postDelayed(yenidenDinle, 600);
            }
        });
    }

    private void soyle(String metin) {
        android.util.Log.i("SAHRADBG", " SOZ " + metin.replace('\n', ' '));
        // Balonda hem duyulan komut hem cevap görünür
        goster(duyulan.isEmpty() ? metin : "Siz: " + duyulan + "\n\n" + Ayar.ad(this) + ": " + metin);
        duyulan = "";
        konus(metin);
    }

    /**
     * Soruyu arka planda cevaplar. Anahtar varsa önce Gemini'ye sorulur; olmazsa
     * "kim, nedir" soruları Vikipedi'de, diğerleri Google'da açılır.
     */
    private void sor(final Komut k) {
        duyulan = "";
        ana.removeCallbacks(yaziGizle);
        yazi.setVisibility(View.GONE);
        if (!cevrimici()) {
            soyle(Ayar.hitap(this) + ", bunu araştırmak için internet gerekiyor.");
            return;
        }
        final boolean acikca = k.tur == Komut.Tur.YAPAY_SORU;
        if (acikca && !Gemini.hazir(this)) {
            soyle(Ayar.hitap(this) + ", önce Sahra uygulamasından Gemini anahtarınızı kaydedin.");
            return;
        }
        final String soru = acikca ? k.metin : k.cumle;
        final boolean yapay = Gemini.hazir(this) && (acikca || !Veri.guncelSoru(soru));
        if (!yapay && k.tur == Komut.Tur.GOOGLE_SORU) {
            googleGoster(k.metin);
            return;
        }
        final boolean hatirlasin = ayar("hafiza", false);
        final List<String[]> gecmis = hatirlasin
                ? Hafiza.al(Ayar.al(this).getInt("hsure", 5), System.currentTimeMillis()) : new ArrayList<String[]>();
        final int numara = pencere.yukleniyor(soru);
        new Thread() {
            @Override
            public void run() {
                Bilgi.Sonuc bulunan = null;
                String sorun = "";
                if (yapay) {
                    try {
                        String cevap = Gemini.sor(SahraService.this, soru, gecmis);
                        bulunan = new Bilgi.Sonuc();
                        bulunan.baslik = soru;
                        bulunan.metin = cevap;
                        bulunan.resim = "";
                        bulunan.kaynak = "Gemini (yapay zekâ, yanlış olabilir)";
                    } catch (Gemini.Hata e) {
                        sorun = e.getMessage();
                    }
                }
                if (bulunan == null && k.tur == Komut.Tur.SORU) {
                    try {
                        bulunan = Bilgi.ara(k.metin);
                    } catch (IOException | JSONException e) {
                        // Aşağıda Google sonuçlarına geçilir
                    }
                }
                final Bilgi.Sonuc s = bulunan;
                final String hata = sorun;
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!calisiyor || !pencere.guncel(numara)) return;
                        if (s != null) {
                            pencere.goster(s);
                            sonCevap = s.metin;
                            if (hatirlasin) Hafiza.ekle(soru, s.metin, System.currentTimeMillis());
                            // Yapay zekâ cevabı kısadır, tamamı okunur; ansiklopedi metninin başı okunur
                            List<String> parcalar = Sayfa.bol(s.metin, 350);
                            konus(parcalar.get(0), true);
                            if (s.kaynak.startsWith("Gemini")) okumaKuyrugu.addAll(parcalar.subList(1, parcalar.size()));
                        } else if (acikca) {
                            pencere.yaz(hata);
                            konus(hata);
                        } else {
                            googleGoster(k.metin);
                        }
                        // Gemini kullanılamadıysa nedeni balonda görünür
                        if (!hata.isEmpty() && !acikca) goster(hata);
                    }
                });
            }
        }.start();
    }

    // ---------- Onay ve ek bilgi isteme ----------

    /** Sahra'nın sorusuna verilen cevap. false dönerse cümle yeni komut sayılır. */
    private interface Girdi {
        boolean al(String cumle);
    }

    private Girdi bekleyenGirdi;
    private long bekleyenZaman;
    private int komutSirasi;
    private CevrimiciSes netSes;
    private UyandirmaMotoru motor;
    private boolean motorHatasi;
    private boolean netUygun;
    private long netEngel;
    private String sonCevap = "";

    /** Soruyu söyler ve ardından cevabı dinler. */
    private void bekle(String soru, Girdi girdi) {
        for (Runnable r : sirada) ana.removeCallbacks(r);
        sirada.clear();
        bekleyenGirdi = girdi;
        bekleyenZaman = System.currentTimeMillis();
        komutBekle = true;
        soyle(soru);
    }

    /** Yalnızca açık bir "evet" işi yapar. */
    private void onaySor(String soru, final Runnable evet) {
        bekle(soru + "\n(Evet ya da hayır deyin.)", new Girdi() {
            @Override
            public boolean al(String cumle) {
                int c = Onay.coz(cumle);
                if (c == 1) {
                    evet.run();
                } else if (c == -1) {
                    soyle("Vazgeçtim " + Ayar.hitap(SahraService.this) + ".");
                }
                return c != 0;
            }
        });
    }

    private static boolean vazgecti(String cumle) {
        return Onay.coz(cumle) == -1;
    }

    private void uzunSoyle(String metin) {
        goster(duyulan.isEmpty() ? metin : "Siz: " + duyulan + "\n\n" + Ayar.ad(this) + ": " + metin);
        duyulan = "";
        List<String> parcalar = Sayfa.bol(metin, 350);
        konus(parcalar.get(0));
        okumaKuyrugu.addAll(parcalar.subList(1, parcalar.size()));
    }

    private void acikSoyle(String metin, boolean paylasilabilir) {
        goster(duyulan.isEmpty() ? metin : "Siz: " + duyulan + "\n\n" + Ayar.ad(this) + ": " + metin);
        duyulan = "";
        konus(metin, paylasilabilir);
    }

    // ---------- Saat ----------

    private boolean saatKapali() {
        if (ayar("alarm", true)) return false;
        soyle("Alarm komutları kapalı " + Ayar.hitap(this) + ". Alarm ve hatırlatıcı bölümünden açabilirsiniz.");
        return true;
    }

    private static int simdiDakika() {
        Calendar t = Calendar.getInstance();
        return t.get(Calendar.HOUR_OF_DAY) * 60 + t.get(Calendar.MINUTE);
    }

    private void alarmKur(String istek) {
        if (saatKapali()) return;
        if (istek.isEmpty()) {
            bekle("Saat kaça kurayım, " + Ayar.hitap(this) + "?", new Girdi() {
                @Override
                public boolean al(String cumle) {
                    int dk = Zaman.saat(Komut.sade(cumle));
                    if (dk >= 0) alarmKur(String.valueOf(dk));
                    else if (vazgecti(cumle)) soyle("Vazgeçtim.");
                    return dk >= 0 || vazgecti(cumle);
                }
            });
            return;
        }
        // "+saniye": şimdiden sonrası
        int dk = istek.startsWith("+") ? (simdiDakika() + (Integer.parseInt(istek.substring(1)) + 59) / 60) % 1440
                : Integer.parseInt(istek);
        soyle(Eylemler.alarm(this, dk, "Sahra")
                ? "Tamam " + Ayar.hitap(this) + ", saat " + Zaman.yaz(dk) + " için alarm kuruyorum."
                : Ayar.hitap(this) + ", saat uygulaması alarm isteğini açamadı.");
    }

    private void zamanlayiciKur(String istek) {
        if (saatKapali()) return;
        if (istek.isEmpty()) {
            bekle("Kaç dakika sayayım?", new Girdi() {
                @Override
                public boolean al(String cumle) {
                    String s = Komut.sade(cumle);
                    int sure = Zaman.sure(s);
                    if (sure == 0 && Zaman.rakamla(s).matches("\\d{1,3}")) sure = Integer.parseInt(Zaman.rakamla(s)) * 60;
                    if (sure > 0) zamanlayiciKur(String.valueOf(sure));
                    else if (vazgecti(cumle)) soyle("Vazgeçtim.");
                    return sure > 0 || vazgecti(cumle);
                }
            });
            return;
        }
        int sure = Integer.parseInt(istek);
        soyle(Eylemler.zamanlayici(this, sure, "Sahra")
                ? "Tamam " + Ayar.hitap(this) + ", " + Zaman.sureYaz(sure) + " sayıyorum."
                : Ayar.hitap(this) + ", saat uygulaması zamanlayıcıyı açamadı.");
    }

    /** Yakın hatırlatmalar saat uygulamasına, ileri tarihliler takvime gider. */
    private void hatirlat(final String cumle) {
        if (saatKapali()) return;
        String s = Komut.sade(cumle);
        String konu = Zaman.hatirlatKonusu(cumle);
        if (konu.isEmpty()) konu = "Hatırlatma";
        int sure = s.contains("sonra") ? Zaman.sure(s) : 0;
        int saat = Zaman.saat(s);
        String h = Ayar.hitap(this);
        if (s.matches(".*\\b(yarin|haftaya|gun sonra|pazartesi|sali|carsamba|persembe|cuma|cumartesi|pazar)\\b.*")) {
            long baslangic = 0;
            if (s.contains("yarin")) {
                Calendar t = Calendar.getInstance();
                t.add(Calendar.DAY_OF_MONTH, 1);
                int dk = saat < 0 ? 9 * 60 : saat;
                t.set(Calendar.HOUR_OF_DAY, dk / 60);
                t.set(Calendar.MINUTE, dk % 60);
                t.set(Calendar.SECOND, 0);
                baslangic = t.getTimeInMillis();
            }
            soyle(Eylemler.takvimeEkle(this, baslangic, konu)
                    ? h + ", takvimi açtım. Tarihi kontrol edip kaydedin."
                    : h + ", takvim uygulaması açılamadı.");
        } else if (sure > 0) {
            boolean oldu = sure <= 3600 ? Eylemler.zamanlayici(this, sure, konu)
                    : Eylemler.alarm(this, (simdiDakika() + sure / 60) % 1440, konu);
            soyle(oldu ? "Tamam " + h + ", " + Zaman.sureYaz(sure) + " sonra “" + konu + "” için uyaracağım."
                    : h + ", saat uygulaması açılamadı.");
        } else if (saat >= 0) {
            int dk = Zaman.yakinSaat(s, saat, simdiDakika());
            soyle(Eylemler.alarm(this, dk, konu)
                    ? "Tamam " + h + ", saat " + Zaman.yaz(dk) + " için “" + konu + "” alarmı kuruyorum."
                    : h + ", saat uygulaması açılamadı.");
        } else {
            bekle("Ne zaman hatırlatayım?", new Girdi() {
                @Override
                public boolean al(String zaman) {
                    String z = Komut.sade(zaman);
                    if (vazgecti(zaman)) {
                        soyle("Vazgeçtim.");
                        return true;
                    }
                    if (Zaman.saat(z) < 0 && Zaman.sure(z) == 0) return false;
                    hatirlat(zaman + " " + cumle);
                    return true;
                }
            });
        }
    }

    // ---------- Arama ----------

    private void kisiAra(final String soylenen) {
        final String h = Ayar.hitap(this);
        final String numara = soylenen.replaceAll("[^0-9+]", "");
        if (numara.length() >= 7 && soylenen.matches("[0-9+ ()-]+(['’]\\S*)?")) {
            araOnayi(numara, numara);
            return;
        }
        if (!Eylemler.izinVar(this, Manifest.permission.READ_CONTACTS)) {
            soyle(h + ", önce Sahra uygulamasının Arama bölümünden rehber izni verin.");
            return;
        }
        final int bu = komutSirasi;
        final String istek = duyulan;
        duyulan = "";
        new Thread() {
            @Override
            public void run() {
                List<String[]> bulunan = new ArrayList<String[]>();
                try {
                    bulunan = Eylemler.kisiler(SahraService.this, soylenen);
                } catch (RuntimeException e) {
                    // Rehber okunamazsa kişi bulunamadı denir
                }
                final List<String[]> l = bulunan;
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!calisiyor || bu != komutSirasi) return;
                        duyulan = istek;
                        if (l.isEmpty()) {
                            soyle(h + ", rehberde “" + soylenen + "” bulamadım.");
                        } else if (l.size() == 1) {
                            araOnayi(l.get(0)[0], l.get(0)[1]);
                        } else {
                            kisiSec(l.subList(0, Math.min(5, l.size())));
                        }
                    }
                });
            }
        }.start();
    }

    private void kisiSec(final List<String[]> l) {
        final List<String> adlar = new ArrayList<String>();
        StringBuilder soru = new StringBuilder("Hangisini arayayım?");
        for (String[] k : l) {
            adlar.add(k[0]);
            soru.append('\n').append(adlar.size()).append(". ").append(k[0]);
        }
        bekle(soru.toString(), new Girdi() {
            @Override
            public boolean al(String cumle) {
                int i = Onay.sira(cumle, adlar);
                if (i >= 0) araOnayi(l.get(i)[0], l.get(i)[1]);
                else if (vazgecti(cumle)) soyle("Vazgeçtim.");
                return i >= 0 || vazgecti(cumle);
            }
        });
    }

    private void araOnayi(final String ad, final String numara) {
        onaySor("“" + ad + "” aransın mı, " + Ayar.hitap(this) + "?", new Runnable() {
            @Override
            public void run() {
                duyulan = "";
                boolean dogrudan = Eylemler.ara(SahraService.this, numara);
                goster(dogrudan ? ad + " aranıyor." : "Numarayı yazdım, arama düğmesine siz basın.");
            }
        });
    }

    // ---------- Sabah özeti, bildirim, not, çeviri ----------

    private void sabahOzeti() {
        final int bu = komutSirasi;
        final String istek = duyulan;
        duyulan = "";
        goster("Özeti hazırlıyorum…");
        new Thread() {
            @Override
            public void run() {
                Pano.yenile(SahraService.this);
                final List<String> takvim = ayar("stak", false) ? Eylemler.bugun(SahraService.this) : null;
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!calisiyor || bu != komutSirasi) return;
                        StringBuilder o = new StringBuilder("Günaydın " + Ayar.hitap(SahraService.this) + ".");
                        if (ayar("shava", true) && !Pano.hava(SahraService.this).isEmpty()) {
                            o.append("\nHava: ").append(Pano.hava(SahraService.this)).append('.');
                        }
                        if (ayar("skur", true) && !Pano.kurlar(SahraService.this).isEmpty()) {
                            o.append('\n').append(Pano.kurlar(SahraService.this).replace("≈", "yaklaşık"));
                        }
                        if (ayar("stak", false)) {
                            if (takvim == null) o.append("\nTakvimi okuyamadım; izni kontrol edin.");
                            else if (takvim.isEmpty()) o.append("\nBugün için takvimde kayıt yok.");
                            else o.append("\nBugün: ").append(TextUtils.join("; ", takvim)).append('.');
                        }
                        if (ayar("ssoz", true)) o.append("\nGünün sözü: ").append(Pano.soz(SahraService.this, 200)).append('.');
                        duyulan = istek;
                        uzunSoyle(o.toString());
                    }
                });
            }
        }.start();
    }

    private void bildirimOku() {
        String h = Ayar.hitap(this);
        if (!ayar("bildirim", false) || !BildirimService.erisimVar(this)) {
            soyle(h + ", önce Bildirim okuma bölümünden özelliği ve bildirim erişimini açın.");
            return;
        }
        List<BildirimService.Kayit> l = BildirimService.al(this);
        if (l.isEmpty()) {
            soyle(h + ", seçtiğiniz uygulamalardan yeni bildirim yok.");
            return;
        }
        StringBuilder s = new StringBuilder(l.size() + " bildirim var.");
        for (BildirimService.Kayit k : l) s.append('\n').append(k.oku()).append('.');
        uzunSoyle(s.toString());
    }

    private void notAl(String metin) {
        if (metin.isEmpty()) {
            bekle("Ne not edeyim?", new Girdi() {
                @Override
                public boolean al(String cumle) {
                    if (vazgecti(cumle)) soyle("Vazgeçtim.");
                    else notAl(cumle);
                    return true;
                }
            });
            return;
        }
        Notlar.ekle(this, metin);
        soyle("Not aldım " + Ayar.hitap(this) + ": " + metin);
    }

    private void ceviri(String istek) {
        final int ayrac = istek.indexOf('|');
        final String dil = istek.substring(0, ayrac);
        String metin = istek.substring(ayrac + 1);
        if (metin.isEmpty()) metin = sonCevap;
        final String h = Ayar.hitap(this);
        if (!Gemini.hazir(this) || !cevrimici()) {
            soyle(h + ", çeviri için internet ve Gemini anahtarı gerekiyor.");
            return;
        }
        if (metin.isEmpty()) {
            bekle("Neyi çevireyim?", new Girdi() {
                @Override
                public boolean al(String cumle) {
                    if (vazgecti(cumle)) soyle("Vazgeçtim.");
                    else ceviri(dil + "|" + cumle);
                    return true;
                }
            });
            return;
        }
        final String asil = metin;
        final String adi = dilAdi(dil);
        final int bu = komutSirasi;
        duyulan = "";
        goster("Çeviriyorum…");
        new Thread() {
            @Override
            public void run() {
                String sonuc = null;
                String sorun = "";
                try {
                    sonuc = Gemini.cevir(SahraService.this, adi, asil);
                } catch (Gemini.Hata e) {
                    sorun = e.getMessage();
                }
                final String c = sonuc;
                final String hata = sorun;
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!calisiyor || bu != komutSirasi) return;
                        if (c == null) {
                            soyle(hata);
                            return;
                        }
                        goster(adi + ": " + c);
                        konus(c, true);
                        // Telefon sesiyle okunacaksa hedef dilin sesi kullanılır
                        if (konusmaHazir && !"turkce".equals(dil) && !ayar("sesNet", false)) {
                            konusma.stop();
                            konusma.setLanguage(Locale.forLanguageTag(dilKodu(dil)));
                            konusma.speak(c, TextToSpeech.QUEUE_FLUSH, null, "sahra");
                        }
                    }
                });
            }
        }.start();
    }

    private static final String[][] DIL_BILGISI = {
            {"ingilizce", "İngilizce", "en-US"}, {"almanca", "Almanca", "de-DE"}, {"fransizca", "Fransızca", "fr-FR"},
            {"arapca", "Arapça", "ar"}, {"rusca", "Rusça", "ru-RU"}, {"ispanyolca", "İspanyolca", "es-ES"},
            {"italyanca", "İtalyanca", "it-IT"}, {"farsca", "Farsça", "fa-IR"}, {"turkce", "Türkçe", "tr-TR"}};

    private static String dilAdi(String k) {
        for (String[] d : DIL_BILGISI) if (d[0].equals(k)) return d[1];
        return k;
    }

    private static String dilKodu(String k) {
        for (String[] d : DIL_BILGISI) if (d[0].equals(k)) return d[2];
        return "tr-TR";
    }

    // ---------- Araba modu ----------

    private void arabaModu(boolean ac) {
        Ayar.al(this).edit().putBoolean("araba", ac).apply();
        if (ac) {
            Eylemler.baslat(this, new Intent(this, ArabaActivity.class));
        } else {
            ArabaActivity.kapat();
        }
    }

    /** Araba ekranındaki Yol ve Ara düğmeleri: hedefi ya da kişiyi sesle sorar. */
    private void hedefSor(final String tur) {
        sustur();
        duyulan = "";
        bekle(tur.equals("yol") ? "Nereye gidelim?" : "Kimi arayayım?", new Girdi() {
            @Override
            public boolean al(String cumle) {
                if (vazgecti(cumle)) {
                    soyle("Vazgeçtim.");
                } else if (tur.equals("yol")) {
                    Komut k = Komut.coz(cumle + " için yol tarifi");
                    tekKomut(k.tur == Komut.Tur.YOL_TARIFI ? k : Komut.coz(cumle), null);
                } else {
                    kisiAra(cumle);
                }
                return true;
            }
        });
    }

    // ---------- Beyin: bağlam belleği ----------

    /** Devam ya da site cümlesiyse işler ve true döner. */
    private boolean beyinIsle(String cumle) {
        long simdi = System.currentTimeMillis();
        Beyin.Istek i = Beyin.coz(cumle, !Beyin.sonArama(simdi).isEmpty());
        if (i == null) return false;
        String h = Ayar.hitap(this);
        switch (i.tip) {
            case ADRES:
                siteAc("https://" + i.ad, h + ", " + i.ad + " açılıyor.");
                return true;
            case SON_SITE: {
                String son = Bellek.sonSite(this);
                if (son.isEmpty()) soyle(h + ", hatırladığım bir site yok.");
                else siteAc(son, h + ", " + Beyin.alanAdi(son) + " açılıyor.");
                return true;
            }
            case NE_ARADIK: {
                String arama = Beyin.sonArama(simdi);
                String son = Bellek.sonSite(this);
                String soz = (arama.isEmpty() ? "" : "Son aramamız: " + arama + ". ")
                        + (son.isEmpty() ? "" : "Son girdiğimiz site: " + Beyin.alanAdi(son) + ".");
                soyle(soz.isEmpty() ? h + ", aklımda bir arama yok." : h + ". " + soz);
                return true;
            }
            case SITE: {
                String bilinen = Bellek.site(this, i.ad);
                if (i.gir && !bilinen.isEmpty()) {
                    Beyin.aramaYapildi(i.ad, simdi);
                    Beyin.adHatirla(i.ad);
                    siteAc(bilinen, h + ", " + Beyin.alanAdi(bilinen) + " açılıyor. Bunu hatırlıyorum.");
                    return true;
                }
                Beyin.aramaYapildi(i.ad, simdi);
                Beyin.adHatirla(i.ad);
                String soz = new Yurutucu(this).uygula(Komut.coz("google'da " + i.ad + " ara"));
                if (!i.gir) {
                    soyle(soz + " Girmemi isterseniz “aç gir” deyin.");
                } else if (EkranService.acik == null) {
                    soyle(ekranIzniGerekli());
                } else {
                    goster(i.ad + " aranıyor, sonuca gireceğim…");
                    duyulan = "";
                    sonucBekle(komutSirasi, 1, i.ad, 0, "", 2500);
                }
                return true;
            }
            case SIRADAKI: {
                // Yanlış site denirse o ad için öğrenilen adres silinir
                if (!Beyin.hatirlananAd().isEmpty()) Bellek.unutSite(this, Beyin.hatirlananAd());
                List<String[]> liste = Beyin.saklanan(simdi);
                int sonraki = Beyin.konum() + 1;
                if (sonraki < liste.size() && !liste.isEmpty()) {
                    sonucaGir(liste, sonraki);
                } else if (!liste.isEmpty()) {
                    soyle(h + ", gördüğüm başka sonuç kalmadı.");
                } else {
                    sonucIste(2, "");
                }
                return true;
            }
            case ADLI_SONUC:
                sonucIste(0, i.ad);
                return true;
            default:
                sonucIste(i.sira, "");
                return true;
        }
    }

    private String ekranIzniGerekli() {
        return Ayar.hitap(this) + ", sonuca girebilmem için Sahra uygulamasından ekran kontrolü iznini açmalısınız.";
    }

    /** sira 0 ise ve ad boşsa ilk sonuç kastedilir. */
    private void sonucIste(int sira, String ad) {
        EkranService e = EkranService.acik;
        long simdi = System.currentTimeMillis();
        List<String[]> eski = Beyin.saklanan(simdi);
        if (e == null) {
            soyle(ekranIzniGerekli());
            return;
        }
        String cubuk = e.adresCubugu();
        if (Beyin.aramaAdresi(cubuk)) {
            List<String[]> ekranda = Beyin.sonuclar(e.baglantilar());
            if (!ekranda.isEmpty()) {
                Beyin.sonuclariSakla(ekranda, simdi);
                secVeGir(ekranda, sira, ad);
                return;
            }
        }
        // Artık bir sitenin içindeyiz: sıra ya da ad söylendiyse hatırlanan sonuçlardan seçilir
        if (!eski.isEmpty()) {
            if (sira > 0 || !ad.isEmpty()) {
                secVeGir(eski, sira, ad);
            } else if (Beyin.konum() >= 0) {
                soyle(Ayar.hitap(this) + ", " + eski.get(Beyin.konum())[2]
                        + " sitesine girdim. Başkası için “sonraki siteye gir” deyin.");
            } else {
                secVeGir(eski, 1, "");
            }
            return;
        }
        String arama = Beyin.sonArama(simdi);
        if (arama.isEmpty()) {
            soyle(Ayar.hitap(this) + ", ekranda arama sonucu göremiyorum. Önce ne arayacağımı söyleyin.");
            return;
        }
        new Yurutucu(this).uygula(Komut.coz("google'da " + arama + " ara"));
        goster("Sonuçlar bekleniyor…");
        duyulan = "";
        sonucBekle(komutSirasi, sira, ad, 0, "", 2500);
    }

    /** Sayfa yüklenirken sonuçları yoklar; ilk bağlantı iki yoklamada aynı kalınca girer. */
    private void sonucBekle(final int bu, final int sira, final String ad, final int deneme,
                            final String onceki, long gecikme) {
        ana.postDelayed(new Runnable() {
            @Override
            public void run() {
                EkranService e = EkranService.acik;
                if (!calisiyor || bu != komutSirasi || e == null) return;
                List<String[]> liste = Beyin.aramaAdresi(e.adresCubugu())
                        ? Beyin.sonuclar(e.baglantilar()) : new ArrayList<String[]>();
                String ilk = liste.isEmpty() ? "" : liste.get(0)[0];
                if (!ilk.isEmpty() && (ilk.equals(onceki) || deneme >= 9)) {
                    Beyin.sonuclariSakla(liste, System.currentTimeMillis());
                    // Site adıyla gelindiyse adı eşleşen sonuç, yoksa ilki seçilir
                    String hatirlanan = Beyin.hatirlananAd();
                    if (sira == 1 && !ad.isEmpty() && ad.equals(hatirlanan)) {
                        sonucaGir(liste, Math.max(0, Beyin.bul(liste, ad)));
                    } else {
                        secVeGir(liste, sira, ad);
                    }
                } else if (deneme >= 9) {
                    soyle(Ayar.hitap(SahraService.this) + ", sonuç bağlantılarını göremedim. Chrome'da sonuçlar açıkken “aç gir” deyin.");
                } else {
                    sonucBekle(bu, sira, ad, deneme + 1, ilk, 1000);
                }
            }
        }, gecikme);
    }

    private void secVeGir(List<String[]> liste, int sira, String ad) {
        int yer = ad.isEmpty() ? Math.max(1, sira) - 1 : Beyin.bul(liste, ad);
        if (yer < 0) {
            soyle(Ayar.hitap(this) + ", sonuçlarda " + ad + " bulamadım.");
        } else if (yer >= liste.size()) {
            soyle(Ayar.hitap(this) + ", yalnızca " + liste.size() + " sonuç görebiliyorum.");
        } else {
            sonucaGir(liste, yer);
        }
    }

    private void sonucaGir(List<String[]> liste, int yer) {
        String adres = liste.get(yer)[0];
        String alan = liste.get(yer)[2];
        // Yönlendirme adresleri geçicidir; hatırlanan, sitenin kendi adresidir
        String kalici = "https://" + alan + "/";
        Beyin.girildi(yer, System.currentTimeMillis());
        String ad = Beyin.hatirlananAd();
        if (!ad.isEmpty()) Bellek.ogren(this, ad, kalici);
        siteAc(adres, kalici, Ayar.hitap(this) + ", " + alan + " sitesine giriyorum.");
    }

    private void siteAc(String adres, String soz) {
        siteAc(adres, adres, soz);
    }

    private void siteAc(String adres, String hatirlanacak, String soz) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(adres)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Chrome aynı sekmede açsın, her seferinde yeni sekme birikmesin
                .putExtra(Browser.EXTRA_APPLICATION_ID, "com.android.chrome");
        try {
            try {
                startActivity(new Intent(i).setPackage("com.android.chrome"));
            } catch (RuntimeException e) {
                startActivity(i);
            }
            Bellek.sonSite(this, hatirlanacak);
            soyle(soz);
        } catch (RuntimeException e) {
            soyle(Ayar.hitap(this) + ", tarayıcıyı açamadım.");
        }
    }

    // ---------- Kayıt formu ----------

    /** Formun açık olduğu yer: tarayıcıda site adı, uygulamada uygulama adı. Bilinemiyorsa boş. */
    private String formYeri(EkranService e) {
        String paket = e.ondeki();
        if (paket.equals("com.android.chrome")) {
            String cubuk = e.adresCubugu();
            return cubuk.contains(" ") ? "" : Beyin.alanAdi(cubuk);
        }
        try {
            return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(paket, 0)) + " uygulaması";
        } catch (PackageManager.NameNotFoundException x) {
            return "";
        }
    }

    /** Onaylanan yer hâlâ öndeyse ve ekran kilitli değilse true. */
    private boolean ayniYer(EkranService e, String paket, String yer) {
        KeyguardManager kilit = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return e != null && !kilit.isKeyguardLocked() && e.ondeki().equals(paket) && formYeri(e).equals(yer);
    }

    /** alanAdi boşsa görünen bütün kayıt kutuları, değilse yalnızca dokunulan kutu doldurulur. */
    private void formDoldur(String alanAdi) {
        final String h = Ayar.hitap(this);
        final EkranService e = EkranService.acik;
        if (e == null) {
            soyle(h + ", form doldurmak için Sahra uygulamasından ekran kontrolü iznini açmalısınız.");
            return;
        }
        if (((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()) {
            soyle(h + ", telefon kilitliyken kayıt bilgisi yazmam.");
            return;
        }
        final String paket = e.ondeki();
        if (paket.isEmpty() || paket.equals(getPackageName())) {
            soyle(h + ", önce doldurulacak formu açın, sonra “formu doldur” deyin.");
            return;
        }
        final String yer = formYeri(e);
        if (yer.isEmpty()) {
            soyle(h + ", sitenin adresini göremiyorum. Sayfayı en üste kaydırıp tekrar söyleyin.");
            return;
        }
        if (Suzgec.bankaUygulamasi(paket, yer)) {
            soyle(h + ", banka, ödeme ve doğrulama sayfalarını doldurmam.");
            return;
        }
        if (!alanAdi.isEmpty()) {
            tekKutuDoldur(Form.Alan.valueOf(alanAdi), paket, yer);
            return;
        }
        List<AccessibilityNodeInfo> nodlar = new ArrayList<AccessibilityNodeInfo>();
        List<Form.Kutu> kutular = new ArrayList<Form.Kutu>();
        e.formKutulari(nodlar, kutular);
        List<Form.Is> isler = Form.plan(kutular, KayitBilgi.kayitli(this));
        if (isler.isEmpty()) {
            soyle(KayitBilgi.kayitli(this).isEmpty()
                    ? h + ", önce Sahra'daki Kayıt bilgilerim bölümünü doldurun."
                    : h + ", ekranda doldurabileceğim boş kayıt kutusu göremiyorum.");
            return;
        }
        onaySor(yer + " sayfasına şunlar yazılsın mı: " + Form.ozet(isler) + "?", new Runnable() {
            @Override
            public void run() {
                EkranService simdi = EkranService.acik;
                if (!ayniYer(simdi, paket, yer)) {
                    soyle(h + ", açık sayfa değişti; güvenlik için hiçbir şey yazmadım.");
                    return;
                }
                // Sayfa onay beklerken değişmiş olabilir; kutular yeniden bulunur
                List<AccessibilityNodeInfo> n = new ArrayList<AccessibilityNodeInfo>();
                List<Form.Kutu> k = new ArrayList<Form.Kutu>();
                simdi.formKutulari(n, k);
                int yazilan = 0;
                List<Form.Is> yeni = Form.plan(k, KayitBilgi.kayitli(SahraService.this));
                for (Form.Is i : yeni) {
                    String deger = KayitBilgi.deger(SahraService.this, i.alan);
                    if (!deger.isEmpty() && simdi.doldur(n.get(i.sira), deger)) yazilan++;
                }
                soyle(yazilan == 0 ? h + ", kutulara yazamadım."
                        : h + ", " + yazilan + " kutuyu doldurdum. Kontrol edin; kayıt düğmesine siz basın. "
                        + "Aşağıda kutu kaldıysa kaydırıp tekrar “formu doldur” deyin.");
            }
        });
    }

    private void tekKutuDoldur(final Form.Alan alan, final String paket, final String yer) {
        final String h = Ayar.hitap(this);
        if (KayitBilgi.deger(this, alan).isEmpty()) {
            soyle(h + ", " + Form.ad(alan) + " bilgisi Kayıt bilgilerim bölümünde yok.");
            return;
        }
        final Runnable yaz = new Runnable() {
            @Override
            public void run() {
                EkranService e = EkranService.acik;
                AccessibilityNodeInfo n = ayniYer(e, paket, yer) ? e.odakKutusu() : null;
                if (n == null) {
                    soyle(h + ", önce yazılacak kutuya dokunun.");
                } else if (n.isPassword() != (alan == Form.Alan.SIFRE)) {
                    // Şifre görünen bir kutuya, başka bilgi de şifre kutusuna yazılmaz
                    soyle(h + ", dokunduğunuz kutu bunun için uygun değil.");
                } else {
                    soyle(e.doldur(n, KayitBilgi.deger(SahraService.this, alan))
                            ? h + ", yazdım." : h + ", kutuya yazamadım.");
                }
            }
        };
        if (alan == Form.Alan.SIFRE) onaySor(yer + " sayfasında dokunduğunuz kutuya şifreniz yazılsın mı?", yaz);
        else yaz.run();
    }

    // ---------- Serbest cümle ----------

    /** Kalıba uymayan cümle için yapay zekâdan komut önerisi alır; kararı telefon verir. */
    private void serbestAnla(final Komut asil) {
        final int bu = komutSirasi;
        final String istek = duyulan;
        duyulan = "";
        goster("Anlamaya çalışıyorum…");
        new Thread() {
            @Override
            public void run() {
                String oneri = "";
                String sorun = "";
                try {
                    oneri = Gemini.komutOner(SahraService.this, asil.cumle);
                } catch (Gemini.Hata e) {
                    sorun = e.getMessage();
                }
                final String o = oneri;
                final String hata = sorun;
                ana.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!calisiyor || bu != komutSirasi) return;
                        duyulan = istek;
                        final Komut k = o.isEmpty() ? null : Komut.coz(o);
                        Serbest.Karar karar = k == null ? Serbest.Karar.REDDET : Serbest.karar(k.tur);
                        if (karar == Serbest.Karar.CALISTIR) {
                            tekKomut(k, null);
                        } else if (karar == Serbest.Karar.ONAY_ISTE) {
                            onaySor("Şunu yapayım mı: “" + o + "”?", new Runnable() {
                                @Override
                                public void run() {
                                    tekKomut(k, null);
                                }
                            });
                        } else {
                            soyle(hata.isEmpty() ? Ayar.hitap(SahraService.this)
                                    + ", bunu bir komuta çeviremedim. Soru için “sor” diyerek başlayın." : hata);
                        }
                    }
                });
            }
        }.start();
    }

    private void googleGoster(String aranan) {
        pencere.google(aranan);
        konus(Ayar.hitap(this) + ", Google sonuçlarını gösteriyorum.");
    }

    private final List<String> okumaKuyrugu = new ArrayList<String>();

    private void sustur() {
        komutBekle = false;
        okumaKuyrugu.clear();
        sonSoz = "";
        bekleyenSoz = null;
        if (konusma != null) konusma.stop();
        if (netSes != null) netSes.durdur();
        konusuyor = false;
        durum();
    }

    /** Penceredeki cevabı ya da Google sonuçlarını sesli okur. */
    private void pencereyiOku() {
        pencere.metniAl(new BilgiPenceresi.Metin() {
            @Override
            public void hazir(String metin) {
                if (!calisiyor || !pencere.acik()) return;
                if (metin.isEmpty()) {
                    konus(Ayar.hitap(SahraService.this) + ", okunacak yazı henüz hazır değil. Biraz sonra tekrar deneyin.");
                    return;
                }
                // Uzun yazı parça parça okunur
                List<String> parcalar = Sayfa.bol(metin, 350);
                konus(parcalar.get(0));
                okumaKuyrugu.addAll(parcalar.subList(1, parcalar.size()));
            }
        });
    }

    /** Telefonun sesiyle konuşur. Rehber, bildirim, not ve ekran yazıları hep bu yolu kullanır. */
    private void konus(String metin) {
        konus(metin, false);
    }

    /** paylasilabilir: yapay zekâ ya da ansiklopedi cevabı; açıksa çevrimiçi sesle okunabilir. */
    private void konus(String metin, boolean paylasilabilir) {
        netUygun = paylasilabilir;
        okumaKuyrugu.clear();
        if (!konusmaHazir) {
            bekleyenSoz = metin;
            return;
        }
        // Sahra kendi sesini komut sanmasın
        if (dinliyor || motor != null && motor.dinliyor()) dinlemeyiBirak();
        if (netSes != null) netSes.durdur();
        konusuyor = true;
        durum();
        yerelSes = false;
        sonSoz = metin;
        seslendir(metin);
    }

    private void seslendir(final String metin) {
        if (netUygun && !yerelSes && ayar("sesNet", false) && Gemini.hazir(this) && cevrimici()
                && System.currentTimeMillis() > netEngel && metin.length() <= 700) {
            konusma.stop();
            netSes.soyle(metin, new CevrimiciSes.Sonuc() {
                @Override
                public void bitti() {
                    SahraService.this.bitti();
                }

                @Override
                public void olmadi(String neden) {
                    // Bir süre telefon sesi kullanılır; kota boşuna zorlanmaz
                    netEngel = System.currentTimeMillis() + 5 * 60000;
                    netUygun = false;
                    goster("Çevrimiçi ses kullanılamadı: " + neden);
                    if (ayar("sesYedek", true)) {
                        yerelSeslendir(metin);
                    } else {
                        SahraService.this.bitti();
                    }
                }
            });
            return;
        }
        yerelSeslendir(metin);
    }

    private void yerelSeslendir(String metin) {
        konusma.setLanguage(new Locale("tr", "TR"));
        Voice v = Ses.sec(this, Ses.turkce(konusma, cevrimici() && !yerelSes));
        if (v != null) konusma.setVoice(v);
        konusma.setSpeechRate(Ayar.al(this).getInt("hiz", 100) / 100f);
        konusma.setPitch(Ayar.al(this).getInt("ton", 100) / 100f);
        if (konusma.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "sahra") != TextToSpeech.SUCCESS) {
            konusuyor = false;
            durum();
        }
    }

    private DisplayMetrics ekran() {
        return getResources().getDisplayMetrics();
    }

    private int dp(int v) {
        return Math.round(v * ekran().density);
    }
}
