package com.guven.sahra;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

/** Türkçe Vikipedi'den kısa bilgi alır. Anahtar gerektirmez. Ana iş parçacığında çağrılmaz. */
final class Bilgi {
    static final class Sonuc {
        String baslik;
        String metin;
        String resim;
    }

    static final String KIMLIK = "Sahra/0.1 (https://github.com/darknesslord19/Sahra-ai)";

    static HttpURLConnection baglan(String adres) throws IOException {
        HttpURLConnection b = (HttpURLConnection) new URL(adres).openConnection();
        b.setConnectTimeout(8000);
        b.setReadTimeout(8000);
        b.setRequestProperty("User-Agent", KIMLIK);
        return b;
    }

    /** Bulunamazsa null döner. */
    static Sonuc ara(String konu) throws IOException, JSONException {
        HttpURLConnection b = baglan("https://tr.wikipedia.org/w/api.php?action=query&format=json"
                + "&generator=search&gsrlimit=1&prop=extracts%7Cpageimages&exintro=1&explaintext=1&pithumbsize=500"
                + "&gsrsearch=" + URLEncoder.encode(konu, "UTF-8"));
        try {
            if (b.getResponseCode() != 200) throw new IOException("Yanıt " + b.getResponseCode());
            InputStream in = b.getInputStream();
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] parca = new byte[4096];
            int n;
            while ((n = in.read(parca)) > 0 && o.size() < 500000) o.write(parca, 0, n);
            in.close();
            JSONObject sorgu = new JSONObject(o.toString("UTF-8")).optJSONObject("query");
            if (sorgu == null) return null;
            JSONObject sayfalar = sorgu.getJSONObject("pages");
            Iterator<String> anahtarlar = sayfalar.keys();
            if (!anahtarlar.hasNext()) return null;
            JSONObject s = sayfalar.getJSONObject(anahtarlar.next());
            Sonuc r = new Sonuc();
            r.baslik = s.optString("title");
            r.metin = s.optString("extract").trim();
            JSONObject kucuk = s.optJSONObject("thumbnail");
            r.resim = kucuk == null ? "" : kucuk.optString("source");
            // Anlam ayrımı sayfaları cevap sayılmaz
            if (r.metin.length() < 40 || r.metin.contains("şu anlamlara gelebilir")) return null;
            return r;
        } finally {
            b.disconnect();
        }
    }
}
