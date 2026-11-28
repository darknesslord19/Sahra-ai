package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asistanın hafızası: onaylanarak öğretilen sözler ve sık kullanılan komutlar.
 * Yalnızca telefonda durur; yedeğe ve yapay zekâya gitmez.
 */
final class Profil {
    private static final int EN_COK_OGRENILEN = 200;
    private static final int EN_COK_GECMIS = 150;

    private static SharedPreferences al(Context c) {
        return c.getSharedPreferences("profil", Context.MODE_PRIVATE);
    }

    static boolean ogrenmeAcik(Context c) {
        return Ayar.al(c).getBoolean("ogrenme", true);
    }

    static boolean gecmisAcik(Context c) {
        return Ayar.al(c).getBoolean("gecmisAcik", true);
    }

    /** Öğretilen sözler, eskiden yeniye: anahtar → yapılacak komut. */
    static Map<String, String> ogrenilen(Context c) {
        Map<String, String> l = new LinkedHashMap<String, String>();
        if (!ogrenmeAcik(c)) return l;
        return hepsi(c);
    }

    static Map<String, String> hepsi(Context c) {
        Map<String, String> l = new LinkedHashMap<String, String>();
        for (String satir : al(c).getString("ogrenilen", "").split("\n")) {
            String[] p = satir.split("\t");
            if (p.length == 2) l.put(p[0], p[1]);
        }
        return l;
    }

    private static void yaz(Context c, Map<String, String> l) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> g : l.entrySet()) s.append(g.getKey()).append('\t').append(g.getValue()).append('\n');
        al(c).edit().putString("ogrenilen", s.toString()).apply();
    }

    static void ogren(Context c, String soz, String hedef) {
        Map<String, String> l = hepsi(c);
        String a = Ogrenme.anahtar(soz);
        l.remove(a);
        l.put(a, hedef.replaceAll("[\\t\\n]", " "));
        while (l.size() > EN_COK_OGRENILEN) l.remove(l.keySet().iterator().next());
        yaz(c, l);
    }

    static void unut(Context c, String anahtar) {
        Map<String, String> l = hepsi(c);
        l.remove(anahtar);
        yaz(c, l);
    }

    static void ogrenilenleriSil(Context c) {
        al(c).edit().remove("ogrenilen").apply();
    }

    /** Kullanılan komut → kaç kez. */
    static Map<String, Integer> gecmis(Context c) {
        Map<String, Integer> l = new LinkedHashMap<String, Integer>();
        if (!gecmisAcik(c)) return l;
        for (String satir : al(c).getString("gecmis", "").split("\n")) {
            String[] p = satir.split("\t");
            if (p.length == 2 && p[1].matches("\\d{1,6}")) l.put(p[0], Integer.parseInt(p[1]));
        }
        return l;
    }

    static void kullanildi(Context c, String cumle) {
        if (!gecmisAcik(c)) return;
        Map<String, Integer> l = gecmis(c);
        String a = Ogrenme.anahtar(cumle);
        if (a.isEmpty() || a.length() > 60) return;
        Integer n = l.remove(a);
        l.put(a, n == null ? 1 : Math.min(999999, n + 1));
        while (l.size() > EN_COK_GECMIS) l.remove(l.keySet().iterator().next());
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, Integer> g : l.entrySet()) s.append(g.getKey()).append('\t').append(g.getValue()).append('\n');
        al(c).edit().putString("gecmis", s.toString()).apply();
    }

    static void gecmisiSil(Context c) {
        al(c).edit().remove("gecmis").apply();
    }

    /** En sık kullanılanlar: "komut (kez)". */
    static List<String> sikKullanilan(Context c, int adet) {
        List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String, Integer>>(gecmis(c).entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        List<String> s = new ArrayList<String>();
        for (int i = 0; i < Math.min(adet, l.size()); i++) s.add(l.get(i).getKey() + "  (" + l.get(i).getValue() + " kez)");
        return s;
    }
}
