package com.guven.sahra;

import java.util.ArrayList;
import java.util.List;

/** Söylenen adı rehberdeki adlarla eşleştirir. Android'e bağlı değildir. */
public final class KisiBul {
    /** "annemi", "Ahmet'i" gibi bir ifadeden denenecek yalın adlar, en olasıdan başlayarak. */
    public static List<String> adaylar(String soylenen) {
        List<String> l = new ArrayList<String>();
        String ham = soylenen.trim();
        // Kesme işareti varsa ek kesin bellidir
        int kesme = Math.max(ham.lastIndexOf('\''), ham.lastIndexOf('’'));
        if (kesme > 0) ekle(l, Komut.sade(ham.substring(0, kesme)));
        String s = Komut.sade(ham).replaceAll("\\s+", " ").trim();
        ekle(l, s);
        if (s.matches(".{2,}[^aeiou ][iu]")) ekle(l, s.substring(0, s.length() - 1));
        if (s.matches(".{2,}y[iu]")) ekle(l, s.substring(0, s.length() - 2));
        // "annem" → "anne"
        for (String a : new ArrayList<String>(l)) {
            if (a.matches(".{3,}[aeiou]m")) ekle(l, a.substring(0, a.length() - 1));
        }
        return l;
    }

    private static void ekle(List<String> l, String s) {
        String t = s.trim();
        if (!t.isEmpty() && !l.contains(t)) l.add(t);
    }

    /**
     * Eşleşen adların sıra numaraları. Önce tam ad aranır; yoksa adın bir sözcüğüyle
     * başlayanlar. İlk sonuç veren aday kullanılır.
     */
    public static List<Integer> bul(String soylenen, List<String> adlar) {
        List<String> adaylar = adaylar(soylenen);
        for (int tur = 0; tur < 2; tur++) {
            for (String a : adaylar) {
                List<Integer> bulunan = new ArrayList<Integer>();
                for (int i = 0; i < adlar.size(); i++) {
                    String ad = Komut.sade(adlar.get(i)).trim();
                    if (tur == 0 ? ad.equals(a) : (" " + ad + " ").contains(" " + a + " ")) bulunan.add(i);
                }
                if (!bulunan.isEmpty()) return bulunan;
            }
        }
        return new ArrayList<Integer>();
    }
}
