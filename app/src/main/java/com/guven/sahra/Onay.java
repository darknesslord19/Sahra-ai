package com.guven.sahra;

import java.util.List;

/** Onay sorusuna verilen cevabı yorumlar. */
public final class Onay {
    /** 1: evet, -1: hayır, 0: başka bir şey. Belirsiz cevap asla evet sayılmaz. */
    public static int coz(String cumle) {
        String s = Komut.sade(cumle == null ? "" : cumle).replaceAll("[.,!?]", " ").replaceAll("\\s+", " ").trim();
        s = s.replaceAll("^(sahra )", "").replaceAll("( sahra| hocam| lutfen)$", "");
        if (s.matches("(hayir|yok|iptal( et)?|vazgec(tim)?|istemiyorum|dur|olmaz|gonderme|arama|yapma|silme)( .*)?")) return -1;
        if (s.matches("(evet|tamam|olur|onayla|onayliyorum|yap)( (evet|tamam|olur|onayla|onayliyorum|yap|gonder|ara|sil|yaz))?")
                || s.matches("(gonder|ara|sil)")) return 1;
        return 0;
    }

    /** Birden çok kişi arasından "birinci", "2" gibi seçim; anlaşılmazsa -1. */
    public static int sira(String cumle, List<String> secenekler) {
        String s = " " + Komut.sade(cumle).replaceAll("[.,!?]", " ").trim() + " ";
        String[][] siralar = {{"1", "bir", "birinci", "ilki", "ilk"}, {"2", "iki", "ikinci"}, {"3", "uc", "ucuncu"},
                {"4", "dort", "dorduncu"}, {"5", "bes", "besinci"}};
        for (int i = 0; i < Math.min(secenekler.size(), siralar.length); i++) {
            for (String a : siralar[i]) {
                if (s.trim().equals(a) || s.trim().equals(a + " numara") || s.trim().equals(a + " olan")) return i;
            }
        }
        for (int i = 0; i < secenekler.size(); i++) {
            if (Komut.sade(secenekler.get(i)).trim().equals(s.trim())) return i;
        }
        return -1;
    }
}
