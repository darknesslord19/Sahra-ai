package com.guven.sahra;

import java.util.ArrayList;
import java.util.List;

/** Kullanıcının eklediği komut: söylenen cümle, yapılacak işler ve isteğe bağlı cevap. */
public final class Ozel {
    public final String cumle;
    public final String isler;
    public final String cevap;

    public Ozel(String cumle, String isler, String cevap) {
        this.cumle = cumle.trim();
        this.isler = isler.trim();
        this.cevap = cevap.trim();
    }

    /** Karşılaştırma biçimi: küçük harf, sade Türkçe, noktalama ve baştaki "sahra" olmadan. */
    public static String anahtar(String s) {
        return Komut.sade(s == null ? "" : s).replaceAll("[.,!?]+", " ").trim().replaceAll("\\s+", " ")
                .replaceFirst("^(hey |tamam )?sahra ", "");
    }

    public static Ozel bul(List<Ozel> liste, String soylenen) {
        String a = anahtar(soylenen);
        for (Ozel o : liste) {
            if (!a.isEmpty() && anahtar(o.cumle).equals(a)) return o;
        }
        return null;
    }

    /** İşler noktalı virgül ya da yeni satırla ayrılır. */
    public static List<String> adimlar(String isler) {
        List<String> l = new ArrayList<String>();
        for (String s : isler.split("[;\\n]+")) {
            if (!s.trim().isEmpty()) l.add(s.trim());
        }
        return l;
    }
}
