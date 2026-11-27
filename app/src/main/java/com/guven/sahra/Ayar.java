package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;

/** Kalıcı ayarlar. */
final class Ayar {
    static SharedPreferences al(Context c) {
        return c.getSharedPreferences("sahra", Context.MODE_PRIVATE);
    }

    static String hitap(Context c) {
        return al(c).getString("hitap", "Güven hocam");
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
