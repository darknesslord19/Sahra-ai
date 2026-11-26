package com.guven.sahra;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Gemini'nin sesiyle konuşur. Bir şey ters giderse çağırana telefon sesine dönmesi söylenir. */
final class CevrimiciSes {
    static final String[] SESLER = {"Kore", "Aoede", "Leda", "Zephyr", "Puck", "Charon", "Orus", "Fenrir"};

    interface Sonuc {
        void bitti();

        void olmadi(String neden);
    }

    private final Context c;
    private final Handler ana = new Handler(Looper.getMainLooper());
    private MediaPlayer calar;
    /** Her istek ve durdurma sayacı artırır; geç gelen eski cevap çalınmaz. */
    private int sira;

    CevrimiciSes(Context c) {
        this.c = c;
    }

    static String ses(Context c) {
        return Ayar.al(c).getString("netSes", SESLER[0]);
    }

    /** Ana iş parçacığından çağrılır. */
    void soyle(final String metin, final Sonuc sonuc) {
        durdur();
        final int bu = sira;
        final String ses = ses(c);
        new Thread() {
            @Override
            public void run() {
                try {
                    final byte[] wav = Gemini.seslendir(c, metin, ses);
                    ana.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bu == sira) cal(wav, bu, sonuc);
                        }
                    });
                } catch (final Gemini.Hata e) {
                    ana.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bu == sira) sonuc.olmadi(e.getMessage());
                        }
                    });
                }
            }
        }.start();
    }

    private void cal(byte[] wav, final int bu, final Sonuc sonuc) {
        try {
            File dosya = new File(c.getCacheDir(), "sahra-ses.wav");
            FileOutputStream o = new FileOutputStream(dosya);
            try {
                o.write(wav);
            } finally {
                o.close();
            }
            calar = new MediaPlayer();
            calar.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            calar.setDataSource(dosya.getPath());
            calar.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer m) {
                    if (bu == sira) {
                        birak();
                        sonuc.bitti();
                    }
                }
            });
            calar.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer m, int a, int b) {
                    if (bu == sira) {
                        birak();
                        sonuc.olmadi("Ses çalınamadı.");
                    }
                    return true;
                }
            });
            calar.prepare();
            calar.start();
        } catch (IOException | RuntimeException e) {
            birak();
            sonuc.olmadi("Ses çalınamadı.");
        }
    }

    private void birak() {
        if (calar != null) {
            calar.release();
            calar = null;
        }
    }

    void durdur() {
        sira++;
        birak();
        new File(c.getCacheDir(), "sahra-ses.wav").delete();
    }
}
