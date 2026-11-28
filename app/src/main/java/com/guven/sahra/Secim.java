package com.guven.sahra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Öndeki uygulamada ya da sitede görünenlerden seçim: "ikinci filmi aç", "ordaki bir filmi oynat",
 * "oynat". Cümleyi çözer ve ekrandaki öğeler arasından hangisine dokunulacağını bulur.
 * Android'e ve belirli bir uygulamaya ya da siteye bağlı değildir.
 */
public final class Secim {
    public enum Tip { SIRA, ADLI, OYNAT }

    public static final int HERHANGI = 0;
    public static final int SONUNCU = -1;
    public static final int RASTGELE = -2;

    public static final class Istek {
        public final Tip tip;
        /** 1'den başlar; ya da HERHANGI, SONUNCU, RASTGELE. */
        public final int sira;
        public final String ad;
        /** "ordaki", "ekrandaki" gibi sözlerle ekran açıkça kastedildi. */
        public final boolean burada;

        private Istek(Tip tip, int sira, String ad, boolean burada) {
            this.tip = tip;
            this.sira = sira;
            this.ad = ad;
            this.burada = burada;
        }
    }

    /** Ekranda dokunulabilen bir öğe. */
    public static final class Oge {
        public final String ad;
        public final int sol, ust, sag, alt;
        public final boolean gorsel;

        public Oge(String ad, int sol, int ust, int sag, int alt, boolean gorsel) {
            this.ad = ad;
            this.sol = sol;
            this.ust = ust;
            this.sag = sag;
            this.alt = alt;
            this.gorsel = gorsel;
        }

        int en() { return sag - sol; }

        int boy() { return alt - ust; }

        boolean ayni(Oge o) {
            return sol == o.sol && ust == o.ust && sag == o.sag && alt == o.alt && ad.equals(o.ad);
        }

        boolean kapsar(Oge o) {
            return sol <= o.sol && ust <= o.ust && sag >= o.sag && alt >= o.alt;
        }
    }

    // ---------- Cümle çözme ----------

    private static final String BURADA = "(?:(?:ordaki|oradaki|burdaki|buradaki|surdaki|suradaki|ekrandaki|sayfadaki|sitedeki|listedeki|uygulamadaki"
            + "|ordan|oradan|burdan|buradan|surdan|suradan|ekrandan|sayfadan|siteden|listeden"
            + "|orada|burada|surada|orda|burda|ekranda|sayfada|sitede|listede|uygulamada|karsimdaki)(?: ki)?)";
    private static final String NESNE = "(?:(?:film|dizi|video|afis|poster|kart|resim|resm|gorsel|bolum|icerik)"
            + "(?:ler|lerden|lerin|lerinden)?(?:i|u|e|a|yi|yu|ye|ya|ni|si|ini|unu|ine|una|sini|sunu|sine)?)";
    private static final String SIRA = "(ilk|birinci|ikinci|ucuncu|dorduncu|besinci|altinci|yedinci|sekizinci|dokuzuncu|onuncu"
            + "|sonuncu|son|sondaki|bastaki|(?:1?[1-9]|10|20)\\.?)(?:sini|sunu|sine|ini|unu|ine|yi|ye|ni|ne|ki)?";
    private static final String HERHANGI_BIRI = "(rastgele(?: bir)?|herhangi bir(?:ini|isini)?|herhangi|bir tanesi(?:ni)?|bir tane|birini|birisini|bir)";
    private static final String FIIL = "(?:ac|oynat|izle|baslat|sec|tikla|bas|dokun|gir|goster|acsana|oynatsana|secsene|izleyelim|acalim|oynatalim|acar misin|oynatir misin|acip oynat)";
    private static final String FIILLER = "(" + FIIL + "(?: (?:ve )?" + FIIL + ")*)";
    private static final Pattern SIRALI = Pattern.compile("^(?:(?:bu|su|o) )?(?:" + SIRA + "(?: siradaki)?|" + HERHANGI_BIRI + ")?"
            + " ?(" + NESNE + ")? ?" + FIILLER + "$");
    private static final Pattern ADLI_NESNELI = Pattern.compile("^(.+?) (?:adli |isimli )?(?:filmini|filmi|film|dizisini|dizisi|diziyi|dizi"
            + "|videosunu|videosu|videoyu|video|afisini|afisine|afisi|posterini|posterine|posteri) " + FIILLER + "$");
    private static final Pattern ADLI_YALIN = Pattern.compile("^(.+?) (?:oynat|izle|sec)$");

    private static int sayi(String s) {
        if (s.startsWith("son")) return SONUNCU;
        String[] adlar = {"birinci", "ikinci", "ucuncu", "dorduncu", "besinci", "altinci", "yedinci", "sekizinci", "dokuzuncu", "onuncu"};
        for (int i = 0; i < adlar.length; i++) {
            if (s.equals(adlar[i])) return i + 1;
        }
        if (s.equals("ilk") || s.equals("bastaki")) return 1;
        return Integer.parseInt(s.replace(".", ""));
    }

    /** Ekrandan seçim cümlesiyse isteği, değilse null döndürür. YouTube ya da tarayıcı adı geçen cümleler sayılmaz. */
    public static Istek coz(String cumle) {
        String ham = cumle == null ? "" : cumle.trim().replaceAll("\\s+", " ").replaceAll("[!?,]+", "").replaceAll("\\.+$", "");
        String s = Komut.sade(ham);
        Matcher bas = Pattern.compile("^(?:(?:hey|tamam) )?" + Isim.desen() + "\\b ?").matcher(s);
        if (bas.find()) {
            ham = ham.substring(bas.end());
            s = s.substring(bas.end());
        }
        Matcher dolgu = Pattern.compile("^(?:(?:hadi|simdi|peki|lutfen|bana) )+").matcher(s);
        if (dolgu.find()) {
            ham = ham.substring(dolgu.end());
            s = s.substring(dolgu.end());
        }
        s = s.replaceFirst("(?: (?:lutfen|bana|artik|hemen))+$", "");
        ham = ham.substring(0, s.length());
        if (s.isEmpty() || s.matches(".*\\b(?:youtube|chrome|google|krom|muzik|sarki)\\w*.*")
                || s.matches("(?:yaz|mesaj yaz|sunu yaz|not al|sor)\\b.*")) return null;

        // "ordaki" gibi sözler cümlenin neresinde olursa olsun ayrılır
        String yalin = s.replaceAll("\\b" + BURADA + "\\b", " ").replaceAll(" +", " ").trim();
        boolean burada = !yalin.equals(s);

        if (yalin.matches("(?:(?:bunu|sunu|onu) )?(?:oynat|izle|oynatsana|oynatir misin)")
                || yalin.matches("(?:bu |su |o )?(?:(?:filmi|diziyi|videoyu|bolumu) (?:oynat|izle|baslat)|videoyu ac|play)")) {
            return new Istek(Tip.OYNAT, HERHANGI, "", burada);
        }
        Matcher m = SIRALI.matcher(yalin);
        if (m.find() && (m.group(1) != null || m.group(2) != null || m.group(3) != null || burada)) {
            String herhangi = m.group(2);
            int sira = m.group(1) != null ? sayi(m.group(1))
                    : herhangi != null && herhangi.startsWith("rastgele") ? RASTGELE : HERHANGI;
            return new Istek(Tip.SIRA, sira, "", burada);
        }
        m = ADLI_NESNELI.matcher(s);
        boolean bulundu = m.find();
        if (!bulundu) {
            m = ADLI_YALIN.matcher(s);
            bulundu = m.find();
        }
        if (!bulundu) return null;
        String ad = ham.substring(m.start(1), m.end(1)).trim();
        String adSade = Komut.sade(ad).replaceAll("\\b" + BURADA + "\\b", " ").replaceAll(" +", " ").trim();
        if (burada) {
            StringBuilder kalan = new StringBuilder();
            for (String p : ad.split(" ")) {
                if (!Komut.sade(p).matches(BURADA)) kalan.append(kalan.length() > 0 ? " " : "").append(p);
            }
            ad = kalan.toString();
        }
        if (adSade.isEmpty() || adSade.split(" ").length > 10) return null;
        return new Istek(Tip.ADLI, HERHANGI, ad, burada);
    }

    // ---------- Ekrandaki öğeler ----------

    private static final Pattern YASAK = Pattern.compile("\\b(?:reklam\\w*|sponsorlu|sponsored|satin al|abone ol|uye ol|odeme|ode|indir|yukle|kirala|bahis|casino|bonus|sil|gonder)\\b");

    private static String duz(String s) {
        return Komut.sade(s == null ? "" : s).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /**
     * Afiş ya da video kartına benzeyen öğeler, okuma sırasıyla (yukarıdan aşağı, soldan sağa).
     * Küçük düğmeler, ince reklam şeritleri, sayfayı kaplayan kutular ve satın alma gibi öğeler atılır.
     */
    public static List<Oge> kartlar(List<Oge> hepsi, int ekranEn, int ekranBoy) {
        List<Oge> aday = new ArrayList<Oge>();
        for (Oge o : hepsi) {
            if (!buyuklukUygun(o, ekranEn, ekranBoy) || o.en() < ekranEn * 18 / 100 || o.boy() < ekranBoy * 9 / 100) continue;
            if (!o.gorsel && o.boy() * 10 < o.en() * 12) continue;
            if (!YASAK.matcher(duz(o.ad)).find()) aday.add(o);
        }
        // Görsel bilgisi vermeyen uygulamalarda: geniş ve adı olan büyük satırlar
        if (aday.isEmpty()) {
            for (Oge o : hepsi) {
                if (buyuklukUygun(o, ekranEn, ekranBoy) && o.en() >= ekranEn * 8 / 10 && o.boy() >= ekranBoy * 15 / 100
                        && o.ad.length() > 3 && !YASAK.matcher(duz(o.ad)).find()) aday.add(o);
            }
        }
        // İçinde başka aday bulunan kutu, kartların tamamını saran kapsayıcıdır
        List<Oge> sade = new ArrayList<Oge>();
        for (int i = 0; i < aday.size(); i++) {
            Oge a = aday.get(i);
            boolean at = false;
            for (int j = 0; j < aday.size() && !at; j++) {
                Oge b = aday.get(j);
                if (i != j && a.kapsar(b)) at = !b.kapsar(a) || j < i;
            }
            if (!at) sade.add(a);
        }
        List<Oge> dik = new ArrayList<Oge>();
        for (Oge o : sade) {
            if (o.boy() * 100 >= o.en() * 115) dik.add(o);
        }
        // Dikey afişler varsa yatay görseller (kapak, duyuru) sayılmaz
        return sirala(dik.size() >= 2 ? dik : sade);
    }

    private static boolean buyuklukUygun(Oge o, int ekranEn, int ekranBoy) {
        int x = (o.sol + o.sag) / 2, y = (o.ust + o.alt) / 2;
        return o.en() > 0 && o.boy() > 0 && x >= 0 && x <= ekranEn && y >= 0 && y <= ekranBoy
                && o.boy() <= ekranBoy * 7 / 10 && (long) o.en() * o.boy() <= (long) ekranEn * ekranBoy * 55 / 100;
    }

    private static List<Oge> sirala(List<Oge> l) {
        List<Oge> kalan = new ArrayList<Oge>(l);
        Collections.sort(kalan, new Comparator<Oge>() {
            @Override
            public int compare(Oge a, Oge b) {
                return a.ust != b.ust ? Integer.compare(a.ust, b.ust) : Integer.compare(a.sol, b.sol);
            }
        });
        List<Oge> sonuc = new ArrayList<Oge>();
        while (!kalan.isEmpty()) {
            Oge ilk = kalan.get(0);
            List<Oge> satir = new ArrayList<Oge>();
            for (Oge o : kalan) {
                if (o.ust - ilk.ust < ilk.boy() / 2) satir.add(o);
            }
            if (satir.isEmpty()) satir.add(ilk);
            kalan.removeAll(satir);
            Collections.sort(satir, new Comparator<Oge>() {
                @Override
                public int compare(Oge a, Oge b) {
                    return Integer.compare(a.sol, b.sol);
                }
            });
            sonuc.addAll(satir);
        }
        return sonuc;
    }

    /** Kumanda modunda gezilecek öğeler: afişler; hiç yoksa (menü, kategori listesi) adı olan dokunulabilir öğeler. */
    public static List<Oge> gezilecekler(List<Oge> hepsi, int ekranEn, int ekranBoy) {
        List<Oge> l = kartlar(hepsi, ekranEn, ekranBoy);
        if (!l.isEmpty()) return l;
        List<Oge> adli = new ArrayList<Oge>();
        for (Oge o : hepsi) {
            if (!o.ad.isEmpty() && buyuklukUygun(o, ekranEn, ekranBoy) && !YASAK.matcher(duz(o.ad)).find()) adli.add(o);
        }
        return sirala(adli);
    }

    /** Adı (bütünüyle) desene uyan ilk öğe; bağlantı ve görselin yinelenen adı da sayılır. Yoksa -1. */
    public static int dugme(List<Oge> l, String desen) {
        for (int i = 0; i < l.size(); i++) {
            if (duz(kisaAd(l.get(i).ad)).matches(desen)) return i;
        }
        return -1;
    }

    /** Öğenin adı söylenenle birebir aynı mı? */
    public static boolean tamAd(String oge, String soylenen) {
        return duz(kisaAd(oge)).equals(duz(soylenen));
    }

    /** sira için listedeki yer; liste o kadar uzun değilse -1. zar: 0 ile 1 arası rastgele sayı. */
    public static int yer(int sira, int adet, double zar) {
        if (adet == 0) return -1;
        if (sira == SONUNCU) return adet - 1;
        if (sira == RASTGELE) return Math.min(adet - 1, (int) (zar * adet));
        int i = Math.max(1, sira) - 1;
        return i < adet ? i : -1;
    }

    private static boolean sozcukVar(String[] sozcukler, String aranan) {
        // "Mucizeyi" gibi ek almış söyleniş, yalın adla da eşleşir
        String kok = aranan.length() > 4 ? aranan.replaceFirst("(?:y?[iu])$", "") : aranan;
        for (String s : sozcukler) {
            if (s.equals(aranan) || (kok.length() > 3 && s.equals(kok))
                    || (aranan.length() > 3 && s.startsWith(aranan))) return true;
        }
        return false;
    }

    /** Söylenen adın bütün sözcüklerini taşıyan öğelerden adı en kısa olanın yeri; yoksa -1. */
    public static int adla(List<Oge> l, String ad) {
        String a = duz(ad);
        if (a.replace(" ", "").length() < 2) return -1;
        int iyi = -1, puan = Integer.MAX_VALUE;
        for (int i = 0; i < l.size(); i++) {
            String e = duz(l.get(i).ad);
            if (e.isEmpty() || YASAK.matcher(e).find()) continue;
            String[] sozcukler = e.split(" ");
            boolean hepsi = true;
            for (String p : a.split(" ")) hepsi &= sozcukVar(sozcukler, p);
            int p = e.equals(a) ? 0 : e.length();
            if (hepsi && p < puan) {
                iyi = i;
                puan = p;
            }
        }
        return iyi;
    }

    /** Sayfadaki oynat düğmesinin yeri; yoksa -1. Yalnızca adı bilinen oynat düğmeleri sayılır. */
    public static int oynatDugmesi(List<Oge> l) {
        for (int i = 0; i < l.size(); i++) {
            if (duz(l.get(i).ad).matches("(?:oynat|play|izle|hemen izle|simdi izle|filmi izle|videoyu oynat(?: k)?|video oynat|play video(?: k)?|oynatmayi baslat)")) return i;
        }
        return -1;
    }

    /** Sahra'nın söyleyeceği kısa ad. */
    public static String kisaAd(String ad) {
        String s = ad == null ? "" : ad.trim().replaceAll("\\s+", " ");
        // Bağlantının ve içindeki görselin adı aynıysa bir kez söylenir
        int orta = s.length() / 2;
        if (s.length() % 2 == 1 && s.charAt(orta) == ' ' && s.substring(0, orta).equals(s.substring(orta + 1))) {
            s = s.substring(0, orta);
        }
        return s.length() <= 50 ? s : s.substring(0, 50).trim() + "…";
    }
}
