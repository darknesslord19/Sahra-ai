package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Robotun boştayken gösterdiği bilgiler. İndirme işleri ana iş parçacığında çağrılmaz. */
final class Pano {
    private static final long TAZE = 30 * 60 * 1000;
    private static final String[] SOZLER = {
            "Damlaya damlaya göl olur", "Sabreden derviş muradına ermiş", "Tatlı dil yılanı deliğinden çıkarır",
            "İşleyen demir ışıldar", "Bir elin nesi var, iki elin sesi var", "Ağaç yaşken eğilir",
            "Bugünün işini yarına bırakma", "Gülü seven dikenine katlanır", "Dost kara günde belli olur",
            "Ne ekersen onu biçersin", "Akıl akıldan üstündür", "Güneş balçıkla sıvanmaz",
            "Her yeni gün yeni bir fırsattır", "Küçük bir adım da yoldur", "Gülümsemek size yakışıyor",
            "Bugün güzel şeyler olsun"};

    static boolean acik(Context c, String anahtar) {
        return Ayar.al(c).getBoolean(anahtar, true);
    }

    static boolean sehirVar(Context c) {
        return !Ayar.al(c).getString("sehir", "").isEmpty();
    }

    // ---------- Kayan yazılar ----------

    /** Sırayla gösterilecek yazılar. Verisi henüz gelmeyen bölümler atlanır. */
    static List<String> yazilar(Context c) {
        List<String> l = new ArrayList<String>();
        if (acik(c, "havaAcik") && !hava(c).isEmpty()) l.add(hava(c));
        if (acik(c, "sozAcik")) l.add(soz(c));
        if (acik(c, "kurAcik") && !kurlar(c).isEmpty()) {
            for (String s : kurlar(c).split("\n")) l.add(s);
        }
        return l;
    }

    /** Kullanıcının kendi sözleri varsa yalnızca onlardan, yoksa hazır listeden seçer. */
    static String soz(Context c) {
        List<String> kendi = new ArrayList<String>();
        for (String s : Ayar.al(c).getString("kendiSozler", "").split("\n")) {
            if (!s.trim().isEmpty()) kendi.add(s.trim());
        }
        if (!kendi.isEmpty()) return kendi.get((int) (Math.random() * kendi.size()));
        return SOZLER[(int) (Math.random() * SOZLER.length)];
    }

    /** Örnek: "Ankara 12° Yağmurlu". Veri 3 saatten eskiyse boş. */
    static String hava(Context c) {
        SharedPreferences p = Ayar.al(c);
        if (!sehirVar(c) || System.currentTimeMillis() - p.getLong("havaZamani", 0) > 6 * TAZE) return "";
        return p.getString("sehir", "") + " " + p.getInt("derece", 0) + "° " + Veri.havaAdi(p.getInt("havaKodu", 0));
    }

    static String havaEfekti(Context c) {
        if (hava(c).isEmpty()) return "";
        SharedPreferences p = Ayar.al(c);
        return Veri.efekt(p.getInt("havaKodu", 0), p.getBoolean("gunduz", true), p.getInt("ruzgar", 0),
                Calendar.getInstance().get(Calendar.MONTH) + 1);
    }

    /** Satır satır dolar, euro ve gram altın. Veri bir günden eskiyse boş. */
    static String kurlar(Context c) {
        SharedPreferences p = Ayar.al(c);
        if (System.currentTimeMillis() - p.getLong("kurZamani", 0) > 48 * TAZE) return "";
        return p.getString("kurlar", "");
    }

    // ---------- İndirme ----------

    /** Süresi dolan verileri yeniler. */
    static void yenile(Context c) {
        SharedPreferences p = Ayar.al(c);
        long simdi = System.currentTimeMillis();
        if (sehirVar(c) && simdi - p.getLong("havaZamani", 0) > TAZE) {
            try {
                havaIndir(c);
            } catch (IOException | JSONException | RuntimeException e) {
                // Eski veri kalır; sonraki turda yeniden denenir
            }
        }
        if (acik(c, "kurAcik") && simdi - p.getLong("kurZamani", 0) > TAZE) {
            try {
                kurIndir(c);
            } catch (IOException | RuntimeException e) {
                // Eski veri kalır
            }
        }
    }

    private static String indir(String adres) throws IOException {
        HttpURLConnection b = Bilgi.baglan(adres);
        try {
            if (b.getResponseCode() != 200) throw new IOException("Yanıt " + b.getResponseCode());
            InputStream in = b.getInputStream();
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] parca = new byte[4096];
            int n;
            while ((n = in.read(parca)) > 0 && o.size() < 500000) o.write(parca, 0, n);
            in.close();
            return o.toString("UTF-8");
        } finally {
            b.disconnect();
        }
    }

    private static void havaIndir(Context c) throws IOException, JSONException {
        SharedPreferences p = Ayar.al(c);
        String sehir = p.getString("sehir", "");
        JSONObject s = new JSONObject(indir("https://api.open-meteo.com/v1/forecast?latitude=" + p.getString("enlem", "")
                + "&longitude=" + p.getString("boylam", "")
                + "&current=temperature_2m,weather_code,is_day,wind_speed_10m")).getJSONObject("current");
        // İndirme sürerken şehir değiştiyse eski şehrin havası yazılmaz
        if (!sehir.equals(p.getString("sehir", ""))) return;
        p.edit().putInt("derece", (int) Math.round(s.getDouble("temperature_2m")))
                .putInt("havaKodu", s.getInt("weather_code"))
                .putBoolean("gunduz", s.getInt("is_day") == 1)
                .putInt("ruzgar", (int) Math.round(s.getDouble("wind_speed_10m")))
                .putLong("havaZamani", System.currentTimeMillis()).apply();
    }

    private static void kurIndir(Context c) throws IOException {
        String xml = indir("https://www.tcmb.gov.tr/kurlar/today.xml");
        double dolar = Veri.kur(xml, "USD");
        double euro = Veri.kur(xml, "EUR");
        if (dolar <= 0 || euro <= 0) throw new IOException("Kur bulunamadı");
        String metin = "Dolar " + Veri.para(dolar) + " TL\nEuro " + Veri.para(euro) + " TL";
        try {
            double ons = new JSONObject(indir("https://api.gold-api.com/price/XAU")).getDouble("price");
            if (ons > 0) metin += "\nGram altın ≈ " + Veri.para(Veri.gramAltin(ons, dolar)) + " TL";
        } catch (IOException | JSONException e) {
            // Altın alınamazsa döviz yine gösterilir
        }
        Ayar.al(c).edit().putString("kurlar", metin).putLong("kurZamani", System.currentTimeMillis()).apply();
    }

    /** Şehir araması. Her sonuç: {görünen ad, kısa ad, enlem, boylam}. */
    static List<String[]> sehirAra(String aranan) throws IOException, JSONException {
        JSONArray d = new JSONObject(indir("https://geocoding-api.open-meteo.com/v1/search?count=8&language=tr&name="
                + URLEncoder.encode(aranan, "UTF-8"))).optJSONArray("results");
        List<String[]> l = new ArrayList<String[]>();
        for (int i = 0; d != null && i < d.length(); i++) {
            JSONObject o = d.getJSONObject(i);
            String ad = o.getString("name");
            String uzun = ad;
            if (!o.optString("admin1").isEmpty()) uzun += ", " + o.optString("admin1");
            if (!o.optString("country").isEmpty()) uzun += ", " + o.optString("country");
            l.add(new String[]{uzun, ad, String.valueOf(o.getDouble("latitude")), String.valueOf(o.getDouble("longitude"))});
        }
        return l;
    }

    static void sehirSec(Context c, String[] s) {
        Ayar.al(c).edit().putString("sehir", s[1]).putString("enlem", s[2]).putString("boylam", s[3])
                .remove("havaZamani").apply();
    }
}
