package com.guven.sahra;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Yalnızca seçilen uygulamaların bildirimlerini, yalnızca bellekte tutar.
 * Banka uygulamaları ve kod içeren bildirimler hiç kaydedilmez.
 */
public class BildirimService extends NotificationListenerService {
    static final class Kayit {
        String anahtar;
        String paket;
        String uygulama;
        String baslik;
        String metin;

        String oku() {
            return uygulama + ", " + baslik + ": " + metin;
        }
    }

    interface Yeni {
        void geldi(Kayit k);
    }

    private static final List<Kayit> KAYITLAR = new ArrayList<Kayit>();
    static Yeni dinleyen;

    static Set<String> secili(Context c) {
        return new HashSet<String>(Ayar.al(c).getStringSet("bildirimUygulamalari", new HashSet<String>()));
    }

    static boolean erisimVar(Context c) {
        String s = android.provider.Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(c.getPackageName() + "/");
    }

    /** Okunmamış bildirimleri verir ve listeyi boşaltır. Seçim sonradan değiştiyse yine süzülür. */
    static synchronized List<Kayit> al(Context c) {
        List<Kayit> l = new ArrayList<Kayit>();
        Set<String> secili = secili(c);
        for (Kayit k : KAYITLAR) if (secili.contains(k.paket)) l.add(k);
        KAYITLAR.clear();
        return l;
    }

    static synchronized void temizle() {
        KAYITLAR.clear();
    }

    private static synchronized boolean ekle(Kayit yeni) {
        for (int i = 0; i < KAYITLAR.size(); i++) {
            Kayit k = KAYITLAR.get(i);
            if (k.anahtar.equals(yeni.anahtar) && k.metin.equals(yeni.metin)) return false;
        }
        KAYITLAR.add(yeni);
        while (KAYITLAR.size() > 20) KAYITLAR.remove(0);
        return true;
    }

    @Override
    public void onListenerDisconnected() {
        temizle();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification s) {
        if (!Ayar.al(this).getBoolean("bildirim", false) || !secili(this).contains(s.getPackageName())) return;
        Notification n = s.getNotification();
        if (s.isOngoing() || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0
                || n.visibility == Notification.VISIBILITY_SECRET) return;
        Bundle e = n.extras;
        CharSequence baslik = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence metin = e.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence uzun = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (baslik == null || metin == null) return;
        String ad = uygulamaAdi(s.getPackageName());
        // Süzgeç hem kısa hem uzun metne bakar
        if (Suzgec.okunmaz(s.getPackageName(), ad, baslik.toString(), metin.toString())
                || uzun != null && Suzgec.gizliIcerik(baslik.toString(), uzun.toString())) return;
        Kayit k = new Kayit();
        k.anahtar = s.getKey();
        k.paket = s.getPackageName();
        k.uygulama = ad;
        k.baslik = baslik.toString();
        k.metin = metin.length() > 300 ? metin.subSequence(0, 300) + "…" : metin.toString();
        if (ekle(k) && dinleyen != null) dinleyen.geldi(k);
    }

    private String uygulamaAdi(String paket) {
        try {
            ApplicationInfo a = getPackageManager().getApplicationInfo(paket, 0);
            return getPackageManager().getApplicationLabel(a).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return paket;
        }
    }
}
