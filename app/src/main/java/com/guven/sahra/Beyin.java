package com.guven.sahra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sahra'nın bağlam belleği. Son aramayı, çıkan sonuçları ve girilen siteyi hatırlar;
 * "aç gir", "ikinciye gir", "sonraki siteye bak" gibi devam cümlelerini bunlara göre çözer.
 * Telefonun içinde çalışır; ekrandaki hiçbir şey dışarı gönderilmez.
 */
public final class Beyin {
    public enum Tip { GIR, SIRADAKI, ADLI_SONUC, SITE, ADRES, SON_SITE, NE_ARADIK }

    public static final class Istek {
        public final Tip tip;
        /** 1'den başlar; söylenmediyse 0. */
        public final int sira;
        public final String ad;
        /** SITE için: bulunca içine de girilsin mi. */
        public final boolean gir;

        private Istek(Tip tip, int sira, String ad, boolean gir) {
            this.tip = tip;
            this.sira = sira;
            this.ad = ad;
            this.gir = gir;
        }
    }

    // ---------- Cümle çözme ----------

    private static final String NESNE = "(?:web sitesi\\w*|site\\w*|sonuc\\w*|link\\w*|baglanti\\w*|sayfa(?:ya|yi|sina|sini)?)";
    private static final String EK = "(?:s?ine|s?ini|n?e|n?i|ye|yi|ya|na|ni|sine|sini)?";
    private static final String SIRA = "(ilk|birinci|ikinci|ucuncu|dorduncu|besinci|[1-5]\\.?)";
    private static final String FIIL = "(?:ac|gir|git|tikla|bas|dokun|girsene|acsana|girelim|acalim|gir bakalim|ac bakalim)";
    private static final String FIILLER = FIIL + "(?: (?:ve )?" + FIIL + ")*";

    private static String temizle(String cumle) {
        String s = Komut.sade(cumle == null ? "" : cumle.trim().replaceAll("\\s+", " "))
                .replaceAll("[!?,]+", "").replaceAll("\\.+$", "").trim();
        s = s.replaceFirst("^(?:(?:hey|tamam) )?sahra\\b ?", "");
        s = s.replaceFirst("^(?:(?:hadi|simdi|tamam|peki|evet|lutfen) )+", "");
        return s.replaceFirst("(?: (?:lutfen|bana|artik|hemen))+$", "").trim();
    }

    private static int sayi(String s) {
        if (s == null) return 0;
        if (s.startsWith("ilk") || s.startsWith("bir")) return 1;
        if (s.startsWith("iki")) return 2;
        if (s.startsWith("ucu")) return 3;
        if (s.startsWith("dor")) return 4;
        if (s.startsWith("bes")) return 5;
        return s.charAt(0) - '0';
    }

    private static Istek istek(Tip t, int sira, String ad, boolean gir) {
        return new Istek(t, sira, ad, gir);
    }

    /**
     * Devam ya da site cümlesiyse isteği, değilse null döndürür.
     * aramaVar: yakın zamanda web araması yapıldı; yalnız "aç" ya da "gir" ancak o zaman anlam taşır.
     */
    public static Istek coz(String cumle, boolean aramaVar) {
        String s = temizle(cumle);
        if (s.isEmpty() || s.matches("(?:yaz|mesaj yaz|sunu yaz|not al|sor)\\b.*")) return null;

        if (s.matches("(?:en )?son (?:girdigin |girdigimiz |actigin )?(?:siteyi|siteye|siteyi tekrar|siteye tekrar) (?:ac|gir|git|don)")) {
            return istek(Tip.SON_SITE, 0, "", true);
        }
        if (s.matches("(?:en son )?(?:ne|neyi) (?:aramistik|aramistim|arattik|arattim|aradik|aradim)|en son (?:nereye|hangi siteye) (?:girdik|girdim)")) {
            return istek(Tip.NE_ARADIK, 0, "", false);
        }

        // Adres söylendiyse doğrudan açılır: "trendyol nokta com a gir"
        String a = s.replaceAll(" ?\\bnokta\\b ?", ".");
        Matcher m = Pattern.compile("^(?:(?:chrome|google|krom|internet)\\w* )?((?:[a-z0-9-]+\\.)+(?:com|net|org|tr|edu|gov|io|co|app|dev|tv|info|me))"
                + "(?: (?:a|e|ya|ye|u|i|yu|yi))?(?: (?:adresine|adresini|sitesine|sitesini|sitesi))? " + FIILLER + "$").matcher(a);
        if (m.find()) return istek(Tip.ADRES, 0, m.group(1), true);

        if (s.matches("(?:bu (?:site )?degil|yanlis (?:site|sonuc)\\w*)(?: .*)?")
                || s.matches("(?:bir )?(?:sonraki|siradaki|diger|baska|obur|alttaki)(?: bir)? " + NESNE + " (?:" + FIILLER + "|bak)")
                || s.matches("(?:digerine|oburune|sonrakine|siradakine|alttakine|baskasina) (?:" + FIILLER + "|bak)")) {
            return istek(Tip.SIRADAKI, 0, "", true);
        }

        // "ikinciye gir", "ilk siteyi aç", "2. sonuca tıkla"
        m = Pattern.compile("^(?:bu |su |o )?" + SIRA + EK + "(?: siradaki)?(?: " + NESNE + ")? " + FIILLER + "$").matcher(s);
        if (m.find()) return istek(Tip.GIR, sayi(m.group(1)), "", true);

        // "siteye gir", "bu linke tıkla", "içine gir", "bunu aç"
        if (s.matches("(?:(?:bu|su|o|buldugun) )?(?:web )?(?:site(?:ye|yi|sine|sini)|sonuc(?:a|u|una|unu)|link(?:e|i|ine|ini)|baglanti(?:ya|yi|sina|sini))(?: (?:de|da))? " + FIILLER)
                || s.matches("(?:(?:icine|iceri|ona|onu|buna|bunu|suna|sunu|oraya|orayi) )" + FIILLER)) {
            return istek(Tip.GIR, 0, "", true);
        }
        if (aramaVar && s.matches("(?:ac gir|gir ac|acip gir|ac|gir|girsene|tikla|ac onu|gir ona|ac bunu|gir buna)")) {
            return istek(Tip.GIR, 0, "", true);
        }

        // "wikipedia olana gir"
        m = Pattern.compile("^(.+?) (?:olana|olani|yazana|yazani|olan " + NESNE + ") " + FIILLER + "$").matcher(s);
        if (m.find()) return istek(Tip.ADLI_SONUC, 0, m.group(1), true);

        // "X sitesine gir", "chrome da X sitesini bul"
        String onek = "^(?:(?:chrome|krom|google|internet)(?: ?(?:da|de|dan|den|ta|te))? )?";
        m = Pattern.compile(onek + "(.+?) (?:web sitesi\\w*|sitesi(?:ni|ne)?|resmi sayfasi(?:ni|na)?) "
                + "(bul(?: ve| sonra)? " + FIILLER + "|bulup " + FIILLER + "|" + FIILLER + "|bul|ara)$").matcher(s);
        if (m.find() && !m.group(1).matches("(?:bu|su|o|bir|internet|web)")) {
            return istek(Tip.SITE, 0, m.group(1), !m.group(2).matches("bul|ara"));
        }
        // "chrome da X bul ve gir", "X aratıp aç"
        m = Pattern.compile(onek + "(.+?) (?:(?:bul|ara|arat)(?: ve| sonra)? " + FIILLER + "|(?:bulup|aratip|arayip) " + FIILLER + ")$").matcher(s);
        if (m.find() && !s.startsWith("youtube")) return istek(Tip.SITE, 0, m.group(1), true);
        return null;
    }

    // ---------- Sonuç bağlantıları ----------

    public static String alanAdi(String adres) {
        Matcher m = Pattern.compile("^(?:https?://)?(?:[^/@?#]*@)?([^/:?#]+)").matcher(adres == null ? "" : adres.trim());
        return m.find() ? m.group(1).toLowerCase(Locale.ROOT).replaceFirst("^(www|m)\\.", "") : "";
    }

    private static boolean googleSayfasi(String alan) {
        return alan.matches("(?:.*\\.)?(?:google(?:\\.[a-z]{2,3}){1,2}|gstatic\\.com|googleusercontent\\.com|googleadservices\\.com|doubleclick\\.net|g\\.co|goo\\.gl)");
    }

    /** Adres çubuğundaki yazı bir arama sayfasını mı gösteriyor? Boşsa bilinmiyor sayılır ve true döner. */
    public static boolean aramaAdresi(String cubuk) {
        String c = cubuk == null ? "" : cubuk.trim().toLowerCase(Locale.ROOT);
        if (c.isEmpty() || c.contains(" ") || c.contains("arama.html")) return true;
        String alan = alanAdi(c);
        return googleSayfasi(alan) || alan.matches("(?:.*\\.)?(?:bing\\.com|duckduckgo\\.com|yandex\\.[a-z.]+|search\\.yahoo\\.com)");
    }

    /**
     * {adres, görünen ad} listesinden arama sonuçlarını ayıklar: sırayı korur; Google'ın kendi
     * bağlantılarını, reklamları, http dışı adresleri ve yinelenen siteleri atar.
     */
    public static List<String[]> sonuclar(List<String[]> baglantilar) {
        Map<String, String[]> l = new LinkedHashMap<String, String[]>();
        for (String[] b : baglantilar) {
            String adres = b[0] == null ? "" : b[0].trim();
            if (!adres.matches("(?i)https?://.+")) continue;
            String etiket = b[1] == null ? "" : b[1];
            String sadeEtiket = Komut.sade(etiket);
            if (sadeEtiket.startsWith("sponsorlu") || sadeEtiket.startsWith("reklam") || adres.contains("/aclk?")) continue;
            String alan = alanAdi(adres);
            if (alan.isEmpty() || googleSayfasi(alan) || !aramaDisi(alan)) continue;
            // Bir sitenin alt bağlantıları aynı sonuç sayılır
            if (!l.containsKey(alan)) l.put(alan, new String[]{adres, etiket});
        }
        return new ArrayList<String[]>(l.values());
    }

    private static boolean aramaDisi(String alan) {
        return !alan.matches("(?:.*\\.)?(?:bing\\.com|duckduckgo\\.com)");
    }

    private static String yalin(String s) {
        return Komut.sade(s == null ? "" : s).replaceAll("[^a-z0-9]", "");
    }

    /** Adı alan adında geçen ilk sonuç; yoksa etiketinde geçen; hiçbiri değilse -1. */
    public static int bul(List<String[]> liste, String ad) {
        String a = yalin(ad.replaceAll("\\b(?:sitesi|resmi|web)\\b", ""));
        if (a.length() < 2) return -1;
        for (int i = 0; i < liste.size(); i++) {
            if (yalin(alanAdi(liste.get(i)[0])).contains(a)) return i;
        }
        for (int i = 0; i < liste.size(); i++) {
            if (yalin(liste.get(i)[1]).contains(a)) return i;
        }
        return -1;
    }

    /** Ad, alan adıyla eşleşiyorsa true: yalnızca bu güvenli eşleşmeler kalıcı olarak hatırlanır. */
    public static boolean guvenliEslesme(String ad, String adres) {
        String a = yalin(ad.replaceAll("\\b(?:sitesi|resmi|web)\\b", ""));
        String[] parcalar = alanAdi(adres).split("\\.");
        for (int i = 0; i < parcalar.length - 1; i++) {
            if (a.length() >= 3 && yalin(parcalar[i]).equals(a)) return true;
        }
        return false;
    }

    public static String anahtar(String ad) {
        return yalin(ad);
    }

    // ---------- Kısa süreli bellek ----------

    private static final long SURE = 10 * 60 * 1000L;
    private static String arama = "";
    private static long aramaZamani;
    private static final List<String[]> LISTE = new ArrayList<String[]>();
    private static int konum = -1;
    private static String sonAlinanAd = "";

    /** Yeni arama: eski sonuçlar unutulur. */
    public static synchronized void aramaYapildi(String aranan, long simdi) {
        arama = aranan;
        aramaZamani = simdi;
        LISTE.clear();
        konum = -1;
        sonAlinanAd = "";
    }

    public static synchronized String sonArama(long simdi) {
        if (simdi - aramaZamani > SURE) unut();
        return arama;
    }

    public static synchronized void sonuclariSakla(List<String[]> liste, long simdi) {
        LISTE.clear();
        LISTE.addAll(liste);
        konum = -1;
        aramaZamani = simdi;
    }

    public static synchronized List<String[]> saklanan(long simdi) {
        sonArama(simdi);
        return new ArrayList<String[]>(LISTE);
    }

    public static synchronized void girildi(int sira, long simdi) {
        konum = sira;
        aramaZamani = simdi;
    }

    /** En son girilen sonucun sırası; hiç girilmediyse -1. */
    public static synchronized int konum() {
        return konum;
    }

    public static synchronized void adHatirla(String ad) {
        sonAlinanAd = ad;
    }

    public static synchronized String hatirlananAd() {
        return sonAlinanAd;
    }

    public static synchronized void unut() {
        arama = "";
        aramaZamani = 0;
        LISTE.clear();
        konum = -1;
        sonAlinanAd = "";
    }
}
