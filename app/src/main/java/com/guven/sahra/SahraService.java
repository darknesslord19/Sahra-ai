package com.guven.sahra;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import android.speech.tts.Voice;
import android.text.TextUtils;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Yüzen robot, ses tanıma ve konuşma. Çalışırken bildirimi görünür. */
public class SahraService extends Service {
    static final String KOMUT = "komut";
    static final String YENILE = "yenile";
    static final String DURDUR = "durdur";
    static final String SES_DENE = "ses_dene";
    static final String SES_SIRADAKI = "ses_siradaki";
    static boolean calisiyor;

    private final Handler ana = new Handler(Looper.getMainLooper());
    private WindowManager pencereler;
    private FrameLayout balon;
    private WebView web;
    private TextView yazi;
    private BalonCizim balonCizim;
    private int deneme;
    private String sonSoz = "";
    private boolean yerelSes;
    private boolean sesBasladi;
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
            if (surekli() && !dinliyor && !konusuyor) dinle(false);
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
            soyle("Merhaba " + Ayar.hitap(this) + ", emirlerinizi yerine getiriyorum.");
        }
        if (intent != null && intent.hasExtra(KOMUT)) {
            calistir(intent.getStringExtra(KOMUT));
        }
        if (SES_SIRADAKI.equals(eylem)) sesDegistir();
        if (SES_DENE.equals(eylem)) soyle("Merhaba " + Ayar.hitap(this) + ", sesim şimdi böyle. Nasıl buldunuz?");
        if (YENILE.equals(eylem)) {
            boyutla(Ayar.boyut(this));
            if (surekli()) {
                ana.postDelayed(yenidenDinle, 500);
            } else if (!elleDinleme) {
                dinlemeyiBirak();
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        calisiyor = false;
        ana.removeCallbacksAndMessages(null);
        if (tanima != null) tanima.destroy();
        if (konusma != null) konusma.shutdown();
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

    private void bildirim() {
        NotificationManager n = getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel("sahra", "Sahra", NotificationManager.IMPORTANCE_LOW));
        PendingIntent ac = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dur = PendingIntent.getService(this, 1,
                new Intent(this, SahraService.class).setAction(DURDUR), PendingIntent.FLAG_IMMUTABLE);
        Notification b = new Notification.Builder(this, "sahra")
                .setSmallIcon(R.drawable.ikon)
                .setContentTitle("Sahra çalışıyor")
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
        String d = dinliyor && (elleDinleme || sesBasladi) || konusuyor ? "active" : "idle";
        web.evaluateJavascript("window.robot&&robot(" + JSONObject.quote(d) + "," + JSONObject.quote(Ayar.renk(this)) + ")", null);
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
                        if (konusma != null) konusma.stop();
                        konusuyor = false;
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
        if (baslatici != null) ana.removeCallbacks(baslatici);
        if (tanima != null) {
            tanima.destroy();
            tanima = null;
        }
        dinliyor = false;
        elleDinleme = false;
        sesBasladi = false;
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
        if (elle) goster("🎤 Dinliyorum " + Ayar.hitap(this) + "…");
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
            sesBasladi = false;
            deneme = 0;
            Ayar.al(SahraService.this).edit().putString("tanima", ad(secilen)).apply();
            durum();
            if (l != null && !l.isEmpty()) {
                String s = l.get(0);
                // Sürekli dinlemede yalnızca "Sahra" ile başlayan cümleler uygulanır
                if (elle || Pattern.compile("^(hey |tamam )?sahra\\b").matcher(Komut.sade(s)).find()) calistir(s);
            }
            if (surekli()) ana.postDelayed(yenidenDinle, 900);
        }

        @Override
        public void onError(int hata) {
            final boolean elle = elleDinleme;
            dinliyor = false;
            elleDinleme = false;
            sesBasladi = false;
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
        @Override
        public void onBeginningOfSpeech() {
            sesBasladi = true;
            durum();
        }

        @Override public void onRmsChanged(float v) { }
        @Override public void onBufferReceived(byte[] b) { }
        @Override public void onEndOfSpeech() { }
        /** Duyulanı söylerken balonda gösterir. */
        @Override
        public void onPartialResults(Bundle b) {
            ArrayList<String> l = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (l == null || l.isEmpty() || l.get(0).isEmpty()) return;
            String s = l.get(0);
            if (elleDinleme || Pattern.compile("^(hey |tamam )?sahra\\b").matcher(Komut.sade(s)).find()) goster("🎤 " + s);
        }

        @Override public void onEvent(int t, Bundle b) { }
    }

    // ---------- Komut ve konuşma ----------

    private final List<Runnable> sirada = new ArrayList<Runnable>();

    private void calistir(String cumle) {
        // Yeni komut gelince önceki özel komutun kalan adımları iptal edilir
        for (Runnable r : sirada) ana.removeCallbacks(r);
        sirada.clear();
        duyulan = cumle;
        Ozel ozel = Ozel.bul(Yedek.komutlar(this), cumle);
        if (ozel != null) {
            ozelCalistir(ozel);
        } else {
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
                if (bekleyenSoz != null) soyle(bekleyenSoz);
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
                konusuyor = false;
                durum();
                if (surekli()) ana.postDelayed(yenidenDinle, 600);
            }
        });
    }

    private void soyle(String metin) {
        // Balonda hem duyulan komut hem cevap görünür
        goster(duyulan.isEmpty() ? metin : "Siz: " + duyulan + "\n\nSahra: " + metin);
        duyulan = "";
        if (!konusmaHazir) {
            bekleyenSoz = metin;
            return;
        }
        // Sahra kendi sesini komut sanmasın
        if (dinliyor) dinlemeyiBirak();
        konusuyor = true;
        durum();
        yerelSes = false;
        sonSoz = metin;
        seslendir(metin);
    }

    private void seslendir(String metin) {
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
