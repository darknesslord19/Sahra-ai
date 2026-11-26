package com.guven.sahra;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Kendi komutları ve ayarları saklar; depolamaya JSON olarak yedekler. */
final class Yedek {
    static final String DOSYA = "sahra-yedek.json";
    private static final String[] YAZI_AYARLARI = {"hitap", "renk", "muzik", "ses"};
    private static final String[] SAYI_AYARLARI = {"boyut", "hiz", "ton"};

    // ---------- Komutlar ----------

    static List<Ozel> komutlar(Context c) {
        try {
            return oku(new JSONArray(Ayar.al(c).getString("ozel", "[]")));
        } catch (JSONException e) {
            return new ArrayList<Ozel>();
        }
    }

    static void kaydet(Context c, List<Ozel> liste) {
        Ayar.al(c).edit().putString("ozel", yaz(liste).toString()).apply();
    }

    private static List<Ozel> oku(JSONArray d) {
        List<Ozel> l = new ArrayList<Ozel>();
        for (int i = 0; i < d.length(); i++) {
            JSONObject o = d.optJSONObject(i);
            if (o == null || o.optString("cumle").trim().isEmpty()) continue;
            l.add(new Ozel(o.optString("cumle"), o.optString("isler"), o.optString("cevap")));
        }
        return l;
    }

    private static JSONArray yaz(List<Ozel> liste) {
        JSONArray d = new JSONArray();
        for (Ozel k : liste) {
            try {
                d.put(new JSONObject().put("cumle", k.cumle).put("isler", k.isler).put("cevap", k.cevap));
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }
        return d;
    }

    // ---------- JSON ----------

    static String json(Context c) {
        try {
            SharedPreferences p = Ayar.al(c);
            JSONObject ayarlar = new JSONObject();
            for (String a : YAZI_AYARLARI) if (p.contains(a)) ayarlar.put(a, p.getString(a, ""));
            for (String a : SAYI_AYARLARI) if (p.contains(a)) ayarlar.put(a, p.getInt(a, 0));
            if (p.contains("surekli")) ayarlar.put("surekli", p.getBoolean("surekli", false));
            return new JSONObject().put("uygulama", "sahra").put("surum", 1)
                    .put("komutlar", yaz(komutlar(c))).put("ayarlar", ayarlar).toString(2);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Yedekteki komutları ekler (aynı cümle varsa yedekteki geçerli olur) ve ayarları uygular. */
    static int geriYukle(Context c, String metin) throws JSONException {
        JSONObject kok = new JSONObject(metin);
        if (!"sahra".equals(kok.optString("uygulama"))) throw new JSONException("Sahra yedeği değil");
        List<Ozel> gelen = oku(kok.getJSONArray("komutlar"));
        List<Ozel> hepsi = komutlar(c);
        for (Ozel yeni : gelen) {
            Ozel eski = Ozel.bul(hepsi, yeni.cumle);
            if (eski != null) hepsi.remove(eski);
            hepsi.add(yeni);
        }
        kaydet(c, hepsi);
        JSONObject a = kok.optJSONObject("ayarlar");
        if (a != null) {
            SharedPreferences.Editor e = Ayar.al(c).edit();
            for (String ad : YAZI_AYARLARI) if (a.has(ad)) e.putString(ad, a.optString(ad));
            if (a.has("boyut")) e.putInt("boyut", sinirla(a.optInt("boyut", 92), 46, 184));
            if (a.has("hiz")) e.putInt("hiz", sinirla(a.optInt("hiz", 100), 70, 140));
            if (a.has("ton")) e.putInt("ton", sinirla(a.optInt("ton", 100), 70, 140));
            if (a.has("surekli")) e.putBoolean("surekli", a.optBoolean("surekli"));
            e.apply();
        }
        return gelen.size();
    }

    private static int sinirla(int d, int az, int cok) {
        return Math.max(az, Math.min(d, cok));
    }

    // ---------- Dosya ----------

    static String dosyadanOku(Context c, Uri adres) throws IOException {
        InputStream in = c.getContentResolver().openInputStream(adres);
        if (in == null) throw new IOException("Dosya açılamadı");
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] parca = new byte[4096];
            int n;
            while ((n = in.read(parca)) > 0) {
                b.write(parca, 0, n);
                if (b.size() > 1000000) throw new IOException("Dosya çok büyük");
            }
            return b.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    static void dosyayaYaz(Context c, Uri adres, String metin) throws IOException {
        OutputStream out = c.getContentResolver().openOutputStream(adres, "wt");
        if (out == null) throw new IOException("Dosya açılamadı");
        try {
            out.write(metin.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /** İndirilenler/Sahra klasörüne yazar (Android 10 ve üstü, ek izin istemez). Başarılıysa true. */
    static boolean otomatik(Context c) {
        if (Build.VERSION.SDK_INT < 29) return false;
        String metin = json(c);
        SharedPreferences p = Ayar.al(c);
        String eski = p.getString("yedekAdresi", "");
        if (!eski.isEmpty()) {
            try {
                dosyayaYaz(c, Uri.parse(eski), metin);
                isaretle(c, metin);
                return true;
            } catch (IOException | RuntimeException e) {
                // Dosya silinmiş olabilir; aşağıda yenisi açılır
            }
        }
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, DOSYA);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Sahra");
            Uri yeni = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (yeni == null) return false;
            dosyayaYaz(c, yeni, metin);
            p.edit().putString("yedekAdresi", yeni.toString()).apply();
            isaretle(c, metin);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Ekrandan çıkarken, yedek varsa ya da komut eklenmişse ve bir şey değiştiyse yedekler. */
    static void gerekirseYedekle(Context c) {
        SharedPreferences p = Ayar.al(c);
        boolean ilgili = p.contains("yedekAdresi") || !komutlar(c).isEmpty();
        if (ilgili && !json(c).equals(p.getString("yedekIcerik", ""))) otomatik(c);
    }

    private static void isaretle(Context c, String metin) {
        Ayar.al(c).edit().putString("yedekIcerik", metin)
                .putString("yedekZamani", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(new Date())).apply();
    }
}
