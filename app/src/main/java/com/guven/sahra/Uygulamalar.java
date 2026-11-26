package com.guven.sahra;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Telefondaki açılabilir uygulamalar. */
final class Uygulamalar {
    static final class Uygulama {
        final String ad;
        final String paket;

        Uygulama(String ad, String paket) {
            this.ad = ad;
            this.paket = paket;
        }
    }

    static List<Uygulama> liste(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<Uygulama> sonuc = new ArrayList<Uygulama>();
        for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
            sonuc.add(new Uygulama(r.loadLabel(pm).toString(), r.activityInfo.packageName));
        }
        Collections.sort(sonuc, new Comparator<Uygulama>() {
            @Override
            public int compare(Uygulama a, Uygulama b) {
                return a.ad.compareToIgnoreCase(b.ad);
            }
        });
        return sonuc;
    }

    /** Bulamazsa "kamerayı", "ayarları" gibi sondaki ekleri atıp yeniden dener. */
    static Uygulama bul(Context c, String istenen) {
        String a = Komut.sade(istenen).replace(" ", "");
        Uygulama u = ara(c, a);
        if (u == null && a.length() > 4 && a.matches(".*y[iu]")) u = ara(c, a.substring(0, a.length() - 2));
        if (u == null && a.length() > 3 && a.matches(".*[iu]")) u = ara(c, a.substring(0, a.length() - 1));
        return u;
    }

    /** Önce tam ad, sonra adın başı, en son içinde geçen. Bulamazsa null. */
    private static Uygulama ara(Context c, String a) {
        if (a.isEmpty()) return null;
        Uygulama baslayan = null;
        Uygulama iceren = null;
        for (Uygulama u : liste(c)) {
            String ad = Komut.sade(u.ad).replace(" ", "");
            if (ad.equals(a)) return u;
            if (baslayan == null && ad.startsWith(a)) baslayan = u;
            if (iceren == null && a.length() > 2 && ad.contains(a)) iceren = u;
        }
        return baslayan != null ? baslayan : iceren;
    }
}
