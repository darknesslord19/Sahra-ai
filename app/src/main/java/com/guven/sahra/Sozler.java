package com.guven.sahra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Yüklenen söz listesinin kuralları. Android'e bağlı değildir. */
public final class Sozler {
    /** Gözlerde kayan yazı için en uzun söz. */
    public static final int GOZ_SINIRI = 90;
    public static final int EN_UZUN = 1000;
    /** Sıklık seçenekleri: iki söz arasında en az geçecek süre. */
    public static final String[] SIKLIK_ADLARI = {"Seyrek", "Normal", "Sık"};
    private static final long[] SIKLIK = {5 * 60 * 1000, 2 * 60 * 1000, 30 * 1000};

    public static final class Soz {
        public final int kat;
        public final String metin;

        public Soz(int kat, String metin) {
            this.kat = kat;
            this.metin = metin;
        }
    }

    public static long aralik(int siklik) {
        return SIKLIK[Math.max(0, Math.min(2, siklik))];
    }

    /** Cuma mesajları ve anneler günü kategorileri istek üzerine hiç alınmaz. */
    public static boolean haric(String kategori) {
        String s = " " + Komut.sade(kategori) + " ";
        return s.matches(".* (cuma|anne|anneler) .*");
    }

    /** Boşlukları toplar; boş ya da aşırı uzun metni atar. */
    public static String temizle(String s) {
        if (s == null) return "";
        String t = s.replaceAll("[\\s\\p{Cntrl}]+", " ").trim();
        return t.length() > EN_UZUN ? "" : t;
    }

    /** Aynı sözün ikinci kopyasını ayırt etmek için. */
    public static String anahtar(String metin) {
        return Komut.sade(metin).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Verilen kategorilerden, uzunluk sınırına uyan sözlerin yerleri. */
    public static List<Integer> uygunlar(List<Soz> liste, Set<Integer> kategoriler, int enCok) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < liste.size(); i++) {
            Soz s = liste.get(i);
            if (kategoriler.contains(s.kat) && s.metin.length() <= enCok) l.add(i);
        }
        return l;
    }

    public static Set<Integer> acikKategoriler(List<String> adlar, String kapali) {
        Set<String> k = new HashSet<String>();
        for (String s : kapali.split("\n")) if (!s.isEmpty()) k.add(s);
        Set<Integer> a = new HashSet<Integer>();
        for (int i = 0; i < adlar.size(); i++) if (!k.contains(adlar.get(i))) a.add(i);
        return a;
    }

    /** Karışıkta rastgele (son gösterilen hariç); değilse sonSira'dan sonraki. Liste boşsa -1. */
    public static int sec(List<Integer> uygun, boolean karisik, int sonSira, Random r) {
        if (uygun.isEmpty()) return -1;
        if (!karisik) {
            for (int i : uygun) if (i > sonSira) return i;
            return uygun.get(0);
        }
        int i = uygun.get(r.nextInt(uygun.size()));
        if (i == sonSira && uygun.size() > 1) i = uygun.get((uygun.indexOf(i) + 1) % uygun.size());
        return i;
    }

    /** "komik" ya da "agir abi" gibi sade bir ada uyan kategori; yoksa -1. */
    public static int kategoriBul(List<String> adlar, String aranan) {
        String a = aranan.equals("giderli") ? "atarli" : aranan;
        for (int i = 0; i < adlar.size(); i++) {
            if ((" " + anahtar(adlar.get(i)) + " ").contains(" " + a + " ")) return i;
        }
        return -1;
    }
}
