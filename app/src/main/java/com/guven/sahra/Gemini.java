package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Soruları Gemini'ye sorar. Yalnızca soru cümlesi gönderilir.
 * Anahtar ayrı bir dosyada durur; JSON yedeğine ve koda girmez.
 */
final class Gemini {
    /** Biri bulunamaz ya da meşgulse sıradaki denenir. */
    private static final String[] MODELLER = {"gemini-3.8-flash", "gemini-flash-latest", "gemini-3.5-flash-lite"};

    /** Kullanıcıya gösterilebilecek Türkçe hata. */
    static final class Hata extends Exception {
        Hata(String mesaj) {
            super(mesaj);
        }
    }

    private static SharedPreferences gizli(Context c) {
        return c.getSharedPreferences("gizli", Context.MODE_PRIVATE);
    }

    static String anahtar(Context c) {
        return gizli(c).getString("gemini", "");
    }

    static boolean hazir(Context c) {
        return !anahtar(c).isEmpty();
    }

    static void anahtarKaydet(Context c, String deger) {
        gizli(c).edit().putString("gemini", deger.trim()).apply();
    }

    /** Ana iş parçacığında çağrılmaz. */
    static String sor(Context c, String soru) throws Hata {
        String talimat = "Adın Sahra, Türkçe konuşan bir telefon asistanısın. Kullanıcıya \"" + Ayar.hitap(c)
                + "\" diye hitap et. Soruyu en fazla 5 kısa cümleyle, düz metin olarak yanıtla; "
                + "yıldız, başlık, tablo ve bağlantı kullanma. Emin olmadığın ya da güncel veri gerektiren "
                + "bir şeyi uydurma, bilmediğini söyle.";
        String govde;
        try {
            govde = new JSONObject()
                    .put("system_instruction", new JSONObject().put("parts",
                            new JSONArray().put(new JSONObject().put("text", talimat))))
                    .put("contents", new JSONArray().put(new JSONObject().put("role", "user").put("parts",
                            new JSONArray().put(new JSONObject().put("text", soru)))))
                    .toString();
        } catch (JSONException e) {
            throw new Hata("İstek hazırlanamadı.");
        }
        Hata son = new Hata("Gemini'ye ulaşılamadı.");
        for (String model : MODELLER) {
            HttpURLConnection b = null;
            try {
                b = (HttpURLConnection) new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent").openConnection();
                b.setConnectTimeout(10000);
                b.setReadTimeout(30000);
                b.setRequestMethod("POST");
                b.setDoOutput(true);
                // Anahtar adreste değil başlıkta gider; yönlendirme izlenmez
                b.setInstanceFollowRedirects(false);
                b.setRequestProperty("x-goog-api-key", anahtar(c));
                b.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                OutputStream out = b.getOutputStream();
                out.write(govde.getBytes("UTF-8"));
                out.close();
                int kod = b.getResponseCode();
                if (kod == 200) {
                    String cevap = cevapMetni(oku(b.getInputStream()));
                    if (cevap.isEmpty()) throw new Hata("Gemini bu soruya cevap vermedi.");
                    return cevap;
                }
                String ayrinti = hataAyrintisi(b.getErrorStream());
                if (kod == 400 || kod == 401 || kod == 403) {
                    throw new Hata("Anahtar geçersiz ya da yetkisiz (" + kod + "). " + ayrinti);
                }
                if (kod == 429) {
                    son = new Hata("Gemini kullanım sınırı doldu. Biraz sonra tekrar deneyin.");
                } else if (kod == 404 || kod >= 500) {
                    son = new Hata("Gemini şu an cevap veremiyor (" + kod + "). " + ayrinti);
                } else {
                    throw new Hata("Gemini hatası (" + kod + "). " + ayrinti);
                }
            } catch (IOException | JSONException e) {
                throw new Hata("Gemini'ye bağlanılamadı. İnterneti kontrol edin.");
            } finally {
                if (b != null) b.disconnect();
            }
        }
        throw son;
    }

    private static String cevapMetni(String json) throws JSONException {
        JSONArray adaylar = new JSONObject(json).optJSONArray("candidates");
        if (adaylar == null || adaylar.length() == 0) return "";
        JSONObject icerik = adaylar.getJSONObject(0).optJSONObject("content");
        JSONArray parcalar = icerik == null ? null : icerik.optJSONArray("parts");
        StringBuilder s = new StringBuilder();
        for (int i = 0; parcalar != null && i < parcalar.length(); i++) {
            JSONObject p = parcalar.getJSONObject(i);
            if (!p.optBoolean("thought")) s.append(p.optString("text"));
        }
        return Veri.duzMetin(s.toString());
    }

    private static String hataAyrintisi(InputStream in) {
        try {
            String m = new JSONObject(oku(in)).getJSONObject("error").optString("message");
            return m.length() > 160 ? m.substring(0, 160) + "…" : m;
        } catch (IOException | JSONException | RuntimeException e) {
            return "";
        }
    }

    private static String oku(InputStream in) throws IOException {
        if (in == null) return "";
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] parca = new byte[4096];
            int n;
            while ((n = in.read(parca)) > 0 && o.size() < 500000) o.write(parca, 0, n);
            return o.toString("UTF-8");
        } finally {
            in.close();
        }
    }
}
