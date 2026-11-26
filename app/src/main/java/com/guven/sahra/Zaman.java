package com.guven.sahra;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Türkçe saat ve süre ifadelerini çözer. Girdi Komut.sade() çıktısıdır. Android'e bağlı değildir. */
public final class Zaman {
    private static final String[] BIRLER = {"sifir", "bir", "iki", "uc", "dort", "bes", "alti", "yedi", "sekiz", "dokuz"};
    private static final String[] ONLAR = {"on", "yirmi", "otuz", "kirk", "elli"};
    private static final String EK = "(?:y?[ea]|[dt][ea]n?|y?[iu])?";

    /** "yirmi beşe" gibi sözcükleri rakama çevirir, rakamların eklerini atar: "7 ye" → "7". */
    public static String rakamla(String s) {
        String m = " " + s.replaceAll("\\bdord(e|u)\\b", "dort") + " ";
        for (int on = ONLAR.length; on >= 1; on--) {
            for (int b = 9; b >= 1; b--) {
                m = m.replaceAll(" " + ONLAR[on - 1] + " ?" + BIRLER[b] + EK + "(?= )", " " + (on * 10 + b));
            }
            m = m.replaceAll(" " + ONLAR[on - 1] + EK + "(?= )", " " + on * 10);
        }
        for (int b = 9; b >= 1; b--) m = m.replaceAll(" " + BIRLER[b] + EK + "(?= )", " " + b);
        return m.replaceAll("(\\d) ?(?:ye|ya|yi|yu|de|da|te|ta|den|dan|ten|tan|e|a|i|u)(?= )", "$1").trim();
    }

    /** Cümledeki saat, gece yarısından dakika olarak; yoksa -1. */
    public static int saat(String sade) {
        String s = " " + rakamla(sade) + " ";
        int saat = -1;
        int dakika = 0;
        Matcher m = Pattern.compile(" (\\d{1,2})(?:[:.](\\d{2}))? (bucuk\\S*|ceyrek (?:gece|kala|var)|(\\d{1,2}) (?:gece|kala|var))").matcher(s);
        if (m.find()) {
            saat = Integer.parseInt(m.group(1));
            String ek = m.group(3);
            int fark = ek.startsWith("bucuk") ? 30 : ek.startsWith("ceyrek") ? 15 : Integer.parseInt(m.group(4));
            if (ek.endsWith("kala") || ek.endsWith("var")) {
                saat = (saat + 23) % 24;
                dakika = 60 - fark;
            } else {
                dakika = fark;
            }
        } else {
            m = Pattern.compile(" (\\d{1,2})[:.](\\d{2}) ").matcher(s);
            if (m.find()) {
                saat = Integer.parseInt(m.group(1));
                dakika = Integer.parseInt(m.group(2));
            } else {
                // Süre bildiren sayılar ("10 dakika") saat sanılmaz
                m = Pattern.compile(" (\\d{1,2})(?: (\\d{2}))?(?= (?!dakika|saniye|saat|gun|hafta))").matcher(s);
                if (m.find()) {
                    saat = Integer.parseInt(m.group(1));
                    if (m.group(2) != null) dakika = Integer.parseInt(m.group(2));
                }
            }
        }
        if (saat < 0 || saat > 23 || dakika > 59) return -1;
        if (s.matches(".* (aksam|ogleden sonra|aksamustu)\\S* .*") && saat < 12) saat += 12;
        if (s.matches(".* gece\\S* .*") && saat >= 7 && saat < 12) saat += 12;
        if (s.matches(".* gece\\S* .*") && saat == 12) saat = 0;
        return saat * 60 + dakika;
    }

    /** "1 saat 30 dakika", "yarım saat", "90 saniye" → saniye; süre yoksa 0. */
    public static int sure(String sade) {
        String s = " " + rakamla(sade.replace("yarim saat", "30 dakika").replace("ceyrek saat", "15 dakika")
                .replaceAll("\\bbucuk saat", "saat 30 dakika")) + " ";
        s = s.replaceAll(" (\\d+) saat 30 dakika", " $1 saat 30 dakika");
        long toplam = 0;
        Matcher m = Pattern.compile(" (\\d{1,4}) (saat|dakika|dk|saniye|sn)").matcher(s);
        while (m.find()) {
            long n = Long.parseLong(m.group(1));
            toplam += m.group(2).equals("saat") ? n * 3600 : m.group(2).startsWith("d") ? n * 60 : n;
        }
        return toplam > 86400 ? 0 : (int) toplam;
    }

    public static String yaz(int dakika) {
        return String.format(java.util.Locale.US, "%02d:%02d", dakika / 60, dakika % 60);
    }

    public static String sureYaz(int saniye) {
        StringBuilder s = new StringBuilder();
        if (saniye >= 3600) s.append(saniye / 3600).append(" saat ");
        if (saniye % 3600 >= 60) s.append(saniye % 3600 / 60).append(" dakika ");
        if (saniye % 60 > 0) s.append(saniye % 60).append(" saniye");
        return s.toString().trim();
    }

    /** Hatırlatma cümlesinden zaman ve komut sözcüklerini atıp konuyu bırakır. */
    public static String hatirlatKonusu(String ham) {
        StringBuilder konu = new StringBuilder();
        for (String soz : ham.trim().replaceAll("[.!?]+$", "").split("\\s+")) {
            String s = Komut.sade(soz).replaceAll("[ :,]+", " ").trim();
            String sayi = rakamla(s);
            if (s.isEmpty() || sayi.matches("[\\d .:]+(de|da|te|ta|e|a|ye|ya)?")
                    || s.matches("(sahra|bana|beni|saat\\w*|sabah|aksam\\w*|gece\\w*|ogleden|ogle\\w*|sonra\\w*|once|bugun|yarin|dakika\\w*|saniye\\w*|yarim|bucuk\\w*|ceyrek|gece|kala|var|hatirlat\\w*( \\w+)?|misin|kur|ekle|olustur|et|diye|icin)")) {
                continue;
            }
            konu.append(soz).append(' ');
        }
        return konu.toString().replaceAll("^[:, ]+|[:, ]+$", "").trim();
    }

    /** "sabah" ya da "akşam" denmemiş 12'den küçük saat için en yakın vakti seçer. */
    public static int yakinSaat(String sade, int dakika, int simdi) {
        if (dakika >= 12 * 60 || sade.matches(".*\\b(sabah|gece|ogle)\\w*.*")) return dakika;
        int ilk = (dakika - simdi + 1440) % 1440;
        int ikinci = (dakika + 720 - simdi + 1440) % 1440;
        return (ikinci != 0 && (ilk == 0 || ikinci < ilk)) ? (dakika + 720) % 1440 : dakika;
    }
}
