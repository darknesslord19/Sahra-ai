package com.guven.sahra;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Kayıt formundaki kutuları tanır ve hangisine ne yazılacağını belirler. Kuşkuda boş bırakır. */
public final class Form {
    public enum Alan { EPOSTA, AD, SOYAD, ADSOYAD, KULLANICI, TELEFON, SIFRE, ATLA }

    public static final class Kutu {
        final String ipucu;
        final String onceki;
        final boolean parola;
        final boolean bos;

        /** ipucu: kutunun kendi adı ve ipuçları; onceki: kutudan hemen önceki yazı. */
        public Kutu(String ipucu, String onceki, boolean parola, boolean bos) {
            this.ipucu = ipucu == null ? "" : ipucu;
            this.onceki = onceki == null ? "" : onceki;
            this.parola = parola;
            this.bos = bos;
        }
    }

    public static final class Is {
        public final int sira;
        public final Alan alan;

        Is(int sira, Alan alan) {
            this.sira = sira;
            this.alan = alan;
        }
    }

    private static String duz(String s) {
        return Komut.sade(s).replaceAll("[_\\-*:.]+", " ").replaceAll("\\s+", " ").trim();
    }

    /** Kart, kimlik, kod gibi asla doldurulmayacak kutular. */
    private static final String YASAK = ".*(kart|\\bcard|cvv|cvc|iban|\\btc\\b|t c kimlik|kimlik|vergi|\\bkod|\\bcode|captcha|\\bara\\b|arama|search|kupon|davet|referans|promosyon|sirket|firma|company|okul|marka|banka|anne|baba|kizlik|yorum|mesaj|\\bpin\\b|\\bsms\\b|\\botp\\b).*";

    private static Alan metinden(String s) {
        if (s.isEmpty()) return null;
        if (s.matches(YASAK)) return Alan.ATLA;
        if (s.matches(".*(e ?posta|e ?mail|gmail|\\bmail).*")) return Alan.EPOSTA;
        if (s.matches(".*(kullanici ?ad|user ?name|nickname|takma ad).*")) return Alan.KULLANICI;
        if (s.matches(".*(\\bad\\w{0,5} (ve )?soyad|isim.{0,5}soy|full ?name).*")) return Alan.ADSOYAD;
        if (s.matches(".*(soyad|soyisim|soy isim|last ?name|surname|family ?name|\\blname\\b).*")) return Alan.SOYAD;
        if (s.matches(".*(\\bad(iniz|i|in)?\\b|\\bisim|isminiz|first ?name|\\bfname\\b|given ?name|\\bname\\b).*")) return Alan.AD;
        if (s.matches(".*(telefon|\\btel\\b|\\bgsm\\b|\\bcep\\b|phone|mobile).*")) return Alan.TELEFON;
        return null;
    }

    public static Alan tur(Kutu k) {
        String kendi = duz(k.ipucu);
        if (k.parola) {
            String hepsi = kendi + " " + duz(k.onceki);
            // Eski şifre, kart güvenlik kodu ya da doğrulama kodu isteniyorsa yazılmaz
            return hepsi.matches(".*(eski|mevcut|current|old|cvv|cvc|\\bpin\\b|\\bsms\\b|\\botp\\b|kart|card|\\bkod|\\bcode).*")
                    ? Alan.ATLA : Alan.SIFRE;
        }
        Alan a = metinden(kendi);
        // Kutunun kendi adı bir şey söylemiyorsa üstündeki yazıya bakılır
        if (a == null) a = metinden(duz(k.onceki));
        return a == null ? Alan.ATLA : a;
    }

    /** Yalnızca boş, tanınan ve bilgisi kayıtlı kutular. Şifre yalnızca gizli kutuya yazılır. */
    public static List<Is> plan(List<Kutu> kutular, Set<Alan> kayitli) {
        int parola = 0;
        boolean sifreDegistirme = false;
        for (Kutu k : kutular) {
            if (!k.parola) continue;
            parola++;
            if (duz(k.ipucu + " " + k.onceki).matches(".*(eski|mevcut|current|old).*")) sifreDegistirme = true;
        }
        List<Is> l = new ArrayList<Is>();
        for (int i = 0; i < kutular.size(); i++) {
            Kutu k = kutular.get(i);
            Alan a = tur(k);
            if (a == Alan.ATLA || !k.bos || !kayitli.contains(a)) continue;
            // Kayıtta en çok iki şifre kutusu olur: şifre ve tekrarı
            if (a == Alan.SIFRE && (parola > 2 || sifreDegistirme)) continue;
            l.add(new Is(i, a));
        }
        return l;
    }

    public static String ad(Alan a) {
        switch (a) {
            case EPOSTA: return "e-posta";
            case AD: return "ad";
            case SOYAD: return "soyad";
            case ADSOYAD: return "ad soyad";
            case KULLANICI: return "kullanıcı adı";
            case TELEFON: return "telefon";
            case SIFRE: return "şifre";
            default: return "";
        }
    }

    /** Onay cümlesi için alan adları; değerlerin kendisi hiç yazılmaz. */
    public static String ozet(List<Is> isler) {
        Set<String> adlar = new LinkedHashSet<String>();
        int sifre = 0;
        for (Is i : isler) {
            if (i.alan == Alan.SIFRE && ++sifre == 2) adlar.add("şifre tekrarı");
            else adlar.add(ad(i.alan));
        }
        return String.join(", ", adlar);
    }
}
