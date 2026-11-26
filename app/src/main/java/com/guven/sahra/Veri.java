package com.guven.sahra;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hava ve piyasa verisini yorumlayan, Android'e bağlı olmayan yardımcılar. */
public final class Veri {
    /** Open-Meteo'nun WMO hava kodunun Türkçesi. */
    public static String havaAdi(int kod) {
        if (kod == 0) return "Açık";
        if (kod <= 2) return "Parçalı bulutlu";
        if (kod == 3) return "Bulutlu";
        if (kod == 45 || kod == 48) return "Sisli";
        if (kod >= 51 && kod <= 57) return "Çiseliyor";
        if (kod >= 61 && kod <= 67) return "Yağmurlu";
        if (kod >= 71 && kod <= 77) return "Karlı";
        if (kod >= 80 && kod <= 82) return "Sağanak";
        if (kod == 85 || kod == 86) return "Kar sağanağı";
        if (kod >= 95) return "Gök gürültülü";
        return "Değişken";
    }

    /** Havaya uyan ekran efekti: kar, yagmur, gunes, yaprak; uyan yoksa boş. ay: 1-12 */
    public static String efekt(int kod, boolean gunduz, double ruzgar, int ay) {
        if (kod >= 71 && kod <= 77 || kod == 85 || kod == 86) return "kar";
        if (kod >= 51 && kod <= 67 || kod >= 80 && kod <= 82 || kod >= 95) return "yagmur";
        if (ruzgar >= 20 || ay >= 9 && ay <= 11) return "yaprak";
        if (kod <= 1 && gunduz) return "gunes";
        return "";
    }

    /** TCMB today.xml içinden döviz satış kuru; bulunamazsa 0. */
    public static double kur(String xml, String kod) {
        Matcher blok = Pattern.compile("CurrencyCode=\"" + kod + "\">(.*?)</Currency>", Pattern.DOTALL).matcher(xml);
        if (!blok.find()) return 0;
        Matcher deger = Pattern.compile("<ForexSelling>\\s*([0-9.]+)\\s*</ForexSelling>").matcher(blok.group(1));
        return deger.find() ? Double.parseDouble(deger.group(1)) : 0;
    }

    /** Ons fiyatı (dolar) ve dolar kurundan gram altının lira karşılığı. */
    public static double gramAltin(double onsDolar, double dolarTl) {
        return onsDolar / 31.1034768 * dolarTl;
    }

    public static String para(double d) {
        return String.format(new Locale("tr", "TR"), "%,.2f", d);
    }
}
