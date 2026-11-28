package com.guven.sahra;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Odak (kumanda) modu: öndeki uygulamaya verilen kısa komutlar ve afişler arasında gezinme.
 * Yalnızca cümlenin tamamı bir kumanda sözüyse eşleşir.
 */
public final class Kumanda {
    public enum Tur {
        BASLAT, CIK, ILERI, ONCEKI, ALT_SATIR, UST_SATIR, SEC, OYNAT, DURDUR, GERI,
        KAYDIR_ASAGI, KAYDIR_YUKARI, ILERI_SAR, GERI_SAR, SES_AC, SES_ARTIR, SES_AZALT, SES_KAPAT,
        TAM_EKRAN, KATEGORILER, KATEGORI, ANA_EKRAN
    }

    public static final class Istek {
        public final Tur tur;
        /** KATEGORI için kategori adı; öbürlerinde boş. */
        public final String ad;
        /** Söylenen yalın cümle (sadeleştirilmiş). */
        public final String soz;

        private Istek(Tur tur, String ad, String soz) {
            this.tur = tur;
            this.ad = ad;
            this.soz = soz;
        }
    }

    private static final Object[][] SOZLER = {
            {Tur.BASLAT, "(?:kumanda|odak)(?: modu(?:nu)?)?(?: ac| baslat| gec)?|kumandayi ac|(?:buraya |bu uygulamaya |bu siteye )?odaklan"},
            {Tur.CIK, "(?:kumanda|odak)(?: modu(?:nu)?|yi|i)? (?:kapat|bitir)|(?:odaktan|kumandadan|moddan) cik|cik|bitir|islemi bitir"},
            {Tur.ANA_EKRAN, "ana (?:ekran|sayfa)(?:a|ya)?(?: don| git)?"},
            {Tur.ILERI, "ilerle|(?:afis|afiste|afislerde) ilerle|ileri(?: git| gec)?|sonraki(?: afis| film| video)?|siradaki(?: afis| film| video)?|bir sonraki|sag(?:a)?(?: gec| git| kay)?"},
            {Tur.ONCEKI, "onceki(?: afis| film| video)?|bir onceki|sol(?:a)?(?: gec| git| kay)?|(?:afis|afiste) geri(?: gel| git)?|geri ilerle"},
            {Tur.ALT_SATIR, "alta gec|alta in|alt sira(?:ya gec)?|alttaki|bir alt(?:a)?|asagi gec"},
            {Tur.UST_SATIR, "uste gec|uste cik|ust sira(?:ya gec)?|ustteki|bir ust(?:e)?|yukari gec"},
            {Tur.SEC, "ac|sec|gir|tikla|tamam|(?:bunu|onu|sunu) (?:ac|sec)|(?:ac|sec) (?:bunu|onu)|(?:bu )?(?:filmi|videoyu|diziyi) (?:ac|sec)"},
            {Tur.OYNAT, "oynat|baslat|devam(?: et)?|(?:filmi|videoyu|diziyi) (?:oynat|baslat|devam ettir)"},
            {Tur.DURDUR, "durdur|duraklat|dur|(?:filmi|videoyu|diziyi) (?:durdur|duraklat)"},
            {Tur.GERI, "geri(?: gel| git| don)?"},
            {Tur.KAYDIR_ASAGI, "kaydir|asagi(?: kaydir| in)?|sayfayi kaydir|devamini goster"},
            {Tur.KAYDIR_YUKARI, "yukari(?: kaydir| cik)?"},
            {Tur.ILERI_SAR, "ileri (?:sar|al)|(?:biraz )?ileri sar"},
            {Tur.GERI_SAR, "geri (?:sar|al)|(?:biraz )?geri sar"},
            {Tur.SES_AC, "ses(?:i)? ac"},
            {Tur.SES_ARTIR, "ses(?:i)? (?:artir|arttir|yukselt)|daha yuksek"},
            {Tur.SES_AZALT, "ses(?:i)? (?:azalt|kis|dusur)|daha kisik"},
            {Tur.SES_KAPAT, "ses(?:i)? (?:kapat|kes)|sessiz|sessize al|sus"},
            {Tur.TAM_EKRAN, "tam ekran(?: yap| ac)?"},
            {Tur.KATEGORILER, "(?:kategori|kategoriler|kategorileri|turler|turleri)(?: ac| goster)?|kategorilere gir|menu(?:yu)? ac"},
    };

    private static final Pattern KATEGORI = Pattern.compile("^(.+?) (?:kategorisi(?:ni|ne)?|kategori(?:yi|ye)?|turu(?:nu|ne)?) (?:ac|gir|sec|goster)$");

    public static Istek coz(String cumle) {
        String ham = cumle == null ? "" : cumle.trim().replaceAll("\\s+", " ").replaceAll("[!?,]+", "").replaceAll("\\.+$", "");
        String s = Komut.sade(ham);
        Matcher bas = Pattern.compile("^(?:(?:hey|tamam) )?" + Isim.desen() + "\\b ?").matcher(s);
        if (bas.find()) {
            ham = ham.substring(bas.end());
            s = s.substring(bas.end());
        }
        Matcher dolgu = Pattern.compile("^(?:(?:hadi|simdi|peki|lutfen) )+").matcher(s);
        if (dolgu.find()) {
            ham = ham.substring(dolgu.end());
            s = s.substring(dolgu.end());
        }
        s = s.replaceFirst("(?: (?:lutfen|artik|hemen))+$", "");
        if (s.isEmpty()) return null;
        for (Object[] k : SOZLER) {
            if (s.matches((String) k[1])) return new Istek((Tur) k[0], "", s);
        }
        Matcher m = KATEGORI.matcher(s);
        if (m.find()) return new Istek(Tur.KATEGORI, ham.substring(0, m.end(1)), s);
        return null;
    }

    /**
     * Arkada dinlerken adı söylenmeden de kabul edilen söz mü? Film sesinden kolay karışacak
     * "tamam", "dur" gibi sözler ve moda sokan sözler sayılmaz.
     */
    public static boolean adsizOlur(Istek i) {
        return i != null && i.tur != Tur.BASLAT && i.tur != Tur.KATEGORI
                && !i.soz.matches("tamam|dur|gir|bitir|cik|devam|sag|sol|sessiz|sus");
    }

    /** Yeni vurgunun yeri; o yönde ekranda öğe kalmadıysa -1. simdiki -1 ise ilk öğe. */
    public static int tasi(List<Secim.Oge> l, int simdiki, Tur yon) {
        if (l.isEmpty()) return -1;
        if (simdiki < 0 || simdiki >= l.size()) return yon == Tur.ONCEKI || yon == Tur.UST_SATIR ? l.size() - 1 : 0;
        if (yon == Tur.ILERI) return simdiki + 1 < l.size() ? simdiki + 1 : -1;
        if (yon == Tur.ONCEKI) return simdiki - 1;
        Secim.Oge s = l.get(simdiki);
        boolean asagi = yon == Tur.ALT_SATIR;
        int iyi = -1;
        long puan = Long.MAX_VALUE;
        for (int i = 0; i < l.size(); i++) {
            Secim.Oge o = l.get(i);
            int fark = asagi ? o.ust - s.ust : s.ust - o.ust;
            if (fark < s.boy() / 2) continue;
            long p = fark * 10000L + Math.abs((o.sol + o.sag) - (s.sol + s.sag));
            if (p < puan) {
                puan = p;
                iyi = i;
            }
        }
        return iyi;
    }
}
