package com.guven.sahra;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import java.util.Locale;
import java.util.regex.Pattern;

/** Yüzen robot, ses tanıma ve konuşma. Çalışırken bildirimi görünür. */
public class SahraService extends Service {
    static final String KOMUT = "komut";
    static final String YENILE = "yenile";
    static final String DURDUR = "durdur";
    static boolean calisiyor;

    private final Handler ana = new Handler(Looper.getMainLooper());
    private WindowManager pencereler;
    private FrameLayout balon;
    private WebView web;
    private TextView yazi;
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
            yazi.setVisibility(View.GONE);
        }
    };

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
        yazi.setTextColor(Color.WHITE);
        yazi.setBackgroundColor(0xEE2C2422);
        yazi.setPadding(dp(12), dp(8), dp(12), dp(8));
        yazi.setMaxLines(4);
        yazi.setVisibility(View.GONE);
        // Yazı balonu dokunmaları alttaki uygulamaya bırakır
        yaziAyar = pencereAyari(-1, -2, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        yaziAyar.gravity = Gravity.BOTTOM;
        yaziAyar.y = dp(90);
        yaziAyar.alpha = 0.8f;
        pencereler.addView(yazi, yaziAyar);
    }

    private WindowManager.LayoutParams pencereAyari(int en, int boy, int ek) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(en, boy,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | ek, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void durum() {
        String d = dinliyor && elleDinleme || konusuyor ? "active" : "idle";
        web.evaluateJavascript("window.robot&&robot(" + JSONObject.quote(d) + "," + JSONObject.quote(Ayar.renk(this)) + ")", null);
    }

    private void goster(String metin) {
        yazi.setText(metin);
        yazi.setVisibility(View.VISIBLE);
        ana.removeCallbacks(yaziGizle);
        ana.postDelayed(yaziGizle, 5000);
    }

    private void boyutla(int yeni) {
        int b = Math.max(46, Math.min(yeni, 184));
        Ayar.al(this).edit().putInt("boyut", b).apply();
        balonAyar.width = dp(b);
        balonAyar.height = dp(b);
        pencereler.updateViewLayout(balon, balonAyar);
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
        if (tanima != null) {
            tanima.destroy();
            tanima = null;
        }
        dinliyor = false;
        elleDinleme = false;
        durum();
    }

    private void dinle(boolean elle) {
        dinlemeyiBirak();
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            goster("Telefonda ses tanıma hizmeti bulunamadı.");
            return;
        }
        tanima = SpeechRecognizer.createSpeechRecognizer(this);
        tanima.setRecognitionListener(new Dinleyici());
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        ConnectivityManager ag = getSystemService(ConnectivityManager.class);
        if (ag.getActiveNetwork() == null) i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        dinliyor = true;
        elleDinleme = elle;
        if (elle) goster("Dinliyorum " + Ayar.hitap(this) + "…");
        durum();
        tanima.startListening(i);
    }

    private final class Dinleyici implements RecognitionListener {
        @Override
        public void onResults(Bundle sonuclar) {
            ArrayList<String> l = sonuclar.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            boolean elle = elleDinleme;
            dinliyor = false;
            elleDinleme = false;
            durum();
            if (l != null && !l.isEmpty()) {
                String s = l.get(0);
                // Sürekli dinlemede yalnızca "Sahra" ile başlayan cümleler uygulanır
                if (elle || Pattern.compile("^(hey |tamam )?sahra\\b").matcher(Komut.sade(s)).find()) calistir(s);
            }
            if (surekli()) ana.postDelayed(yenidenDinle, 700);
        }

        @Override
        public void onError(int hata) {
            boolean elle = elleDinleme;
            dinliyor = false;
            elleDinleme = false;
            durum();
            if (elle) {
                goster(hata == SpeechRecognizer.ERROR_NO_MATCH || hata == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ? "Sizi duyamadım, tekrar dokunun."
                        : "Ses tanıma hatası (" + hata + "). İnterneti veya Türkçe çevrimdışı dil paketini kontrol edin.");
            }
            if (surekli()) ana.postDelayed(yenidenDinle, 1500);
        }

        @Override public void onReadyForSpeech(Bundle b) { }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float v) { }
        @Override public void onBufferReceived(byte[] b) { }
        @Override public void onEndOfSpeech() { }
        @Override public void onPartialResults(Bundle b) { }
        @Override public void onEvent(int t, Bundle b) { }
    }

    // ---------- Komut ve konuşma ----------

    private void calistir(String cumle) {
        Komut k = Komut.coz(cumle);
        switch (k.tur) {
            case KAPAN:
                stopSelf();
                return;
            case BUYU:
            case KUCUL:
                boyutla(Ayar.boyut(this) + (k.tur == Komut.Tur.BUYU ? 20 : -20));
                soyle("Tamam " + Ayar.hitap(this) + ".");
                return;
            case RENK:
                Ayar.al(this).edit().putString("renk", k.metin).apply();
                durum();
                soyle("Tamam " + Ayar.hitap(this) + ".");
                return;
            default:
                soyle(new Yurutucu(this).uygula(k));
        }
    }

    private void konusmaKur() {
        konusma = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int sonuc) {
                if (sonuc != TextToSpeech.SUCCESS) return;
                konusma.setLanguage(new Locale("tr", "TR"));
                konusma.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) { bitti(); }
                    @Override public void onError(String id) { bitti(); }
                });
                konusmaHazir = true;
                if (bekleyenSoz != null) soyle(bekleyenSoz);
                bekleyenSoz = null;
            }
        });
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
        goster(metin);
        if (!konusmaHazir) {
            bekleyenSoz = metin;
            return;
        }
        // Sahra kendi sesini komut sanmasın
        if (dinliyor) dinlemeyiBirak();
        konusuyor = true;
        durum();
        if (konusma.speak(metin, TextToSpeech.QUEUE_FLUSH, null, "sahra") != TextToSpeech.SUCCESS) konusuyor = false;
    }

    private DisplayMetrics ekran() {
        return getResources().getDisplayMetrics();
    }

    private int dp(int v) {
        return Math.round(v * ekran().density);
    }
}
