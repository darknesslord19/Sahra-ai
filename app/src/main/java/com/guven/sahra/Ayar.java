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

    /** Robotun kenar uzunluğu, dp. */
    static int boyut(Context c) {
        return al(c).getInt("boyut", 92);
    }

    static String muzikPaketi(Context c) {
        return al(c).getString("muzik", "");
    }
}
