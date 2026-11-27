package com.guven.sahra;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Kullanıcının telefondan seçtiği SQLite söz dosyasını uygulamanın özel alanına aktarır.
 * Beklenen yapı: kategoriler(ID, KatAdi) ve sozler(ID, KatId, Soz).
 */
final class SozDepo {
    private static final String DOSYA = "sozler.json";
    private static final long EN_BUYUK = 20 * 1024 * 1024;
    private static final int EN_COK_SOZ = 50000;
    private static final Random RASTGELE = new Random();

    static final class Veri {
        final List<String> kategoriler = new ArrayList<String>();
        final List<Sozler.Soz> sozler = new ArrayList<Sozler.Soz>();

        int sayi(int kategori) {
            int n = 0;
            for (Sozler.Soz s : sozler) if (s.kat == kategori) n++;
            return n;
        }
    }

    private static Veri bellek;

    static synchronized Veri al(Context c) {
        if (bellek != null) return bellek;
        Veri v = new Veri();
        File f = new File(c.getFilesDir(), DOSYA);
        if (f.exists()) {
            try (InputStream i = new FileInputStream(f)) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                kopyala(i, b);
                JSONObject o = new JSONObject(new String(b.toByteArray(), "UTF-8"));
                JSONArray k = o.getJSONArray("kategoriler");
                for (int n = 0; n < k.length(); n++) v.kategoriler.add(k.getString(n));
                JSONArray s = o.getJSONArray("sozler");
                for (int n = 0; n < s.length(); n++) {
                    JSONArray x = s.getJSONArray(n);
                    v.sozler.add(new Sozler.Soz(x.getInt(0), x.getString(1)));
                }
            } catch (IOException | JSONException e) {
                v = new Veri();
            }
        }
        bellek = v;
        return v;
    }

    static boolean var(Context c) {
        return !al(c).sozler.isEmpty();
    }

    /** Uzun sürebilir; ana iş parçacığında çağrılmaz. Alınan söz sayısını döndürür. */
    static int yukle(Context c, Uri kaynak) throws IOException {
        File gecici = File.createTempFile("soz", ".sqlite", c.getCacheDir());
        try {
            try (InputStream i = c.getContentResolver().openInputStream(kaynak);
                 OutputStream o = new FileOutputStream(gecici)) {
                if (i == null) throw new IOException("Dosya açılamadı");
                kopyala(i, o);
            }
            Veri v = oku(gecici);
            if (v.sozler.isEmpty()) throw new IOException("Dosyada söz bulunamadı");
            yaz(c, v);
            return v.sozler.size();
        } catch (JSONException | RuntimeException e) {
            throw new IOException("Geçerli bir söz dosyası değil", e);
        } finally {
            SQLiteDatabase.deleteDatabase(gecici);
        }
    }

    private static Veri oku(File f) {
        Veri v = new Veri();
        SQLiteDatabase db = SQLiteDatabase.openDatabase(f.getPath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try {
            Map<Long, Integer> yer = new HashMap<Long, Integer>();
            try (Cursor k = db.rawQuery("SELECT ID, KatAdi FROM kategoriler ORDER BY ID LIMIT 200", null)) {
                while (k.moveToNext()) {
                    String ad = Sozler.temizle(k.getString(1));
                    if (ad.isEmpty() || Sozler.haric(ad)) continue;
                    yer.put(k.getLong(0), v.kategoriler.size());
                    v.kategoriler.add(ad);
                }
            }
            Set<String> gorulen = new HashSet<String>();
            try (Cursor s = db.rawQuery("SELECT KatId, Soz FROM sozler ORDER BY ID LIMIT " + EN_COK_SOZ, null)) {
                while (s.moveToNext()) {
                    Integer kat = yer.get(s.getLong(0));
                    if (kat == null) continue;
                    String metin = Sozler.temizle(s.getString(1));
                    if (!metin.isEmpty() && gorulen.add(Sozler.anahtar(metin))) v.sozler.add(new Sozler.Soz(kat, metin));
                }
            }
        } finally {
            db.close();
        }
        return v;
    }

    private static synchronized void yaz(Context c, Veri v) throws IOException, JSONException {
        JSONArray s = new JSONArray();
        for (Sozler.Soz x : v.sozler) s.put(new JSONArray().put(x.kat).put(x.metin));
        JSONObject o = new JSONObject().put("kategoriler", new JSONArray(v.kategoriler)).put("sozler", s);
        File yeni = new File(c.getFilesDir(), DOSYA + ".yeni");
        try (OutputStream cikti = new FileOutputStream(yeni)) {
            cikti.write(o.toString().getBytes("UTF-8"));
        }
        if (!yeni.renameTo(new File(c.getFilesDir(), DOSYA))) throw new IOException("Kaydedilemedi");
        bellek = v;
        Ayar.al(c).edit().remove("sozSira").apply();
    }

    private static void kopyala(InputStream i, OutputStream o) throws IOException {
        byte[] b = new byte[8192];
        long toplam = 0;
        for (int n; (n = i.read(b)) > 0; ) {
            toplam += n;
            if (toplam > EN_BUYUK) throw new IOException("Dosya çok büyük");
            o.write(b, 0, n);
        }
    }

    static synchronized void sil(Context c) {
        new File(c.getFilesDir(), DOSYA).delete();
        bellek = null;
        Ayar.al(c).edit().remove("sozSira").apply();
    }

    static Set<Integer> acik(Context c) {
        return Sozler.acikKategoriler(al(c).kategoriler, Ayar.al(c).getString("sozKapali", ""));
    }

    /** Açık kategorilerden bir söz; uygun söz yoksa null. */
    static String sec(Context c, int enCok) {
        return sec(c, acik(c), enCok);
    }

    static String sec(Context c, Set<Integer> kategoriler, int enCok) {
        Veri v = al(c);
        boolean karisik = Ayar.al(c).getBoolean("sozKarisik", true);
        int i = Sozler.sec(Sozler.uygunlar(v.sozler, kategoriler, enCok), karisik,
                Ayar.al(c).getInt("sozSira", -1), RASTGELE);
        if (i < 0) return null;
        Ayar.al(c).edit().putInt("sozSira", i).apply();
        return v.sozler.get(i).metin;
    }
}
