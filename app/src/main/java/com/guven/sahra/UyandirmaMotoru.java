package com.guven.sahra;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;
import java.io.IOException;

/**
 * "Sahra" adını internete ses göndermeden telefonda bekler (Vosk).
 * Serbest tanıma kullanılır ve cümle bu adla başlıyorsa uyanır; duyulanlar saklanmaz.
 * (Yalnızca "sahra"yı arayan kısıtlı sözlük, denemede "saat kaç" gibi cümlelerde de uyandığı için kullanılmadı.)
 */
final class UyandirmaMotoru {
    interface Olay {
        void duyuldu();

        void hata(String neden);
    }

    private final Context c;
    private final Olay olay;
    private final Handler ana = new Handler(Looper.getMainLooper());
    private Model model;
    private Recognizer tanici;
    private SpeechService hizmet;
    private boolean istenen;
    private boolean yukleniyor;

    UyandirmaMotoru(Context c, Olay olay) {
        this.c = c;
        this.olay = olay;
    }

    boolean dinliyor() {
        return istenen;
    }

    /** Ana iş parçacığından çağrılır. Model ilk seferde arka planda yüklenir. */
    void basla() {
        if (istenen) return;
        istenen = true;
        if (model != null) {
            ac();
        } else if (!yukleniyor) {
            yukleniyor = true;
            final File klasor = ModelIndir.klasor(c);
            new Thread() {
                @Override
                public void run() {
                    Model yuklenen = null;
                    try {
                        if (klasor != null) yuklenen = new Model(klasor.getAbsolutePath());
                    } catch (Throwable e) {
                        yuklenen = null;
                    }
                    final Model m = yuklenen;
                    ana.post(new Runnable() {
                        @Override
                        public void run() {
                            yukleniyor = false;
                            model = m;
                            if (m == null) {
                                istenen = false;
                                olay.hata("Uyandırma modeli yüklenemedi. Modeli silip yeniden indirin.");
                            } else if (istenen) {
                                ac();
                            }
                        }
                    });
                }
            }.start();
        }
    }

    private void ac() {
        try {
            tanici = new Recognizer(model, 16000f);
            tanici.setWords(true);
            hizmet = new SpeechService(tanici, 16000f);
            hizmet.startListening(new RecognitionListener() {
                @Override
                public void onPartialResult(String s) {
                }

                @Override
                public void onResult(String s) {
                    incele(s);
                }

                @Override
                public void onFinalResult(String s) {
                }

                @Override
                public void onError(Exception e) {
                    durdur();
                    olay.hata("Mikrofon kullanılamadı.");
                }

                @Override
                public void onTimeout() {
                }
            });
        } catch (IOException | RuntimeException | UnsatisfiedLinkError e) {
            durdur();
            olay.hata("Telefonda uyandırma başlatılamadı.");
        }
    }

    private void incele(String json) {
        if (!istenen) return;
        // Hassasiyet arttıkça daha düşük güvenle de uyanır
        double esik = 1.0 - Ayar.al(c).getInt("hassas", 5) * 0.05;
        try {
            JSONObject sonuc = new JSONObject(json);
            if (Uyandirma.komut(sonuc.optString("text")) == null) return;
            JSONArray sozler = sonuc.optJSONArray("result");
            for (int i = 0; sozler != null && i < sozler.length(); i++) {
                JSONObject s = sozler.getJSONObject(i);
                if (s.optString("word").matches("sahra|sahara") && s.optDouble("conf", 0) >= esik) {
                    durdur();
                    olay.duyuldu();
                    return;
                }
            }
        } catch (JSONException e) {
            // Anlaşılamayan sonuç yok sayılır
        }
    }

    /** Mikrofonu bırakır; model bellekte kalır. */
    void durdur() {
        istenen = false;
        if (hizmet != null) {
            hizmet.stop();
            hizmet.shutdown();
            hizmet = null;
        }
        if (tanici != null) {
            tanici.close();
            tanici = null;
        }
    }

    void kapat() {
        durdur();
        if (model != null) {
            model.close();
            model = null;
        }
    }
}
