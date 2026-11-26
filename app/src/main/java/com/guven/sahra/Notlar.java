package com.guven.sahra;

import android.content.Context;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Sesli notlar. Yalnızca telefonda durur; JSON yedeğine girmez. */
final class Notlar {
    private static final int EN_COK = 200;

    static List<String> liste(Context c) {
        List<String> l = new ArrayList<String>();
        try {
            JSONArray d = new JSONArray(c.getSharedPreferences("notlar", Context.MODE_PRIVATE).getString("liste", "[]"));
            for (int i = 0; i < d.length(); i++) l.add(d.getString(i));
        } catch (org.json.JSONException e) {
            // Bozuk kayıt boş sayılır
        }
        return l;
    }

    static void ekle(Context c, String not) {
        List<String> l = liste(c);
        l.add(new SimpleDateFormat("d MMMM HH:mm", new Locale("tr", "TR")).format(new Date()) + " — " + not);
        while (l.size() > EN_COK) l.remove(0);
        kaydet(c, l);
    }

    static void kaydet(Context c, List<String> l) {
        c.getSharedPreferences("notlar", Context.MODE_PRIVATE).edit()
                .putString("liste", new JSONArray(l).toString()).apply();
    }
}
