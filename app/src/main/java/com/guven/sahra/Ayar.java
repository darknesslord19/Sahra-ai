package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;

/** Kalıcı ayarlar. */
final class Ayar {
    static SharedPreferences al(Context c) {
        SharedPreferences p = c.getSharedPreferences("sahra", Context.MODE_PRIVATE);
        // komut ayrıştırıcıları Android'e bağlı olmadığından ad onlara buradan aktarılır
        Isim.ayarla(p.getString("ad", Isim.VARSAYILAN));
        return p;
    }

    /** Asistanın adı. */
    static String ad(Context c) {
        al(c);
        return Isim.ad();
    }

    static String hitap(Context c) {
        return al(c).getString("hitap", "Güven hocam");
    }

    static boolean sahra1(Context c) {
        return "sahra1".equals(al(c).getString("model", "sahra"));
    }

    static String robotSayfasi(Context c) {
        return "file:///android_asset/" + (sahra1(c) ? "robot1.html" : "robot.html");
    }

    /** Sahra 1'de eller gövdenin dışında durduğu için yüzen pencere daha geniştir. */
    static int pencereBoyu(Context c, int boyut) {
        return sahra1(c) ? Math.round(boyut * 1.55f) : boyut;
    }

    /** robot1.html içindeki robotAyar() için ayar metni. */
    static String sahra1Ayari(Context c) {
        SharedPreferences p = al(c);
        return Sahra1.json(p.getString("s1Poz", "oto"), p.getString("s1Hareket", "suzul"), p.getInt("s1Yon", 0),
                p.getString("s1Donus", "sabit"), p.getString("s1Agiz", "oto"), p.getBoolean("s1Eller", true),
                p.getInt("s1Hiz", 100));
    }

    static String renk(Context c) {
        return al(c).getString("renk", "beyaz");
    }

    /** robot.html içindeki robotIsik() için ayar metni. */
    static String isik(Context c) {
        android.content.SharedPreferences p = al(c);
        return Isik.json(p.getString("isikRenk", Isik.ILK_RENKLER), p.getString("isikSekil", Isik.ORIJINAL),
                p.getInt("isikHiz", Isik.ILK_HIZ), p.getInt("isikUst", Isik.ILK_UST),
                p.getInt("isikAlt", Isik.ILK_ALT), p.getInt("isikYay", Isik.ILK_YAY),
                p.getBoolean("isikGoz", true), p.getBoolean("isikDinle", false));
    }

    static String davranis(Context c) {
        return al(c).getString("davranis", "rastgele");
    }

    /** Robotun kenar uzunluğu, dp. */
    static int boyut(Context c) {
        return al(c).getInt("boyut", 92);
    }

    static String muzikPaketi(Context c) {
        return al(c).getString("muzik", "");
    }
}
