package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Öğrenilen siteler ve son girilen site. Yalnızca telefonda durur; yedeğe ve yapay zekâya gitmez. */
final class Bellek {
    private static SharedPreferences al(Context c) {
        return c.getSharedPreferences("beyin", Context.MODE_PRIVATE);
    }

    static boolean acik(Context c) {
        return Ayar.al(c).getBoolean("beyinOgren", true);
    }

    static String site(Context c, String ad) {
        return acik(c) ? al(c).getString("site." + Beyin.anahtar(ad), "") : "";
    }

    static void ogren(Context c, String ad, String adres) {
        if (acik(c) && Beyin.guvenliEslesme(ad, adres)) {
            al(c).edit().putString("site." + Beyin.anahtar(ad), adres).apply();
        }
    }

    static void unutSite(Context c, String ad) {
        al(c).edit().remove("site." + Beyin.anahtar(ad)).apply();
    }

    static String sonSite(Context c) {
        return al(c).getString("son", "");
    }

    static void sonSite(Context c, String adres) {
        if (acik(c)) al(c).edit().putString("son", adres).apply();
    }

    /** "ad  →  alanadi" satırları. */
    static List<String> liste(Context c) {
        List<String> l = new ArrayList<String>();
        for (Map.Entry<String, ?> g : al(c).getAll().entrySet()) {
            if (g.getKey().startsWith("site.")) {
                l.add(g.getKey().substring(5) + "  →  " + Beyin.alanAdi(String.valueOf(g.getValue())));
            }
        }
        Collections.sort(l);
        return l;
    }

    static void temizle(Context c) {
        al(c).edit().clear().apply();
        Beyin.unut();
    }
}
