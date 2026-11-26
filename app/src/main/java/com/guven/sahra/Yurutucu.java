package com.guven.sahra;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.MediaStore;
import android.view.KeyEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Komutu telefonda uygular ve Sahra'nın söyleyeceği cevabı döndürür. */
final class Yurutucu {
    private final Context c;
    private final AudioManager ses;

    Yurutucu(Context c) {
        this.c = c;
        ses = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    }

    String uygula(Komut k) {
        String h = Ayar.hitap(c);
        String[] onaylar = {"Tamam " + h + ". ", "Hemen " + h + ". ", "Peki " + h + ". ", "Tabii " + h + ". "};
        String tamam = onaylar[(int) (Math.random() * onaylar.length)];
        switch (k.tur) {
            case SELAM:
                return "Merhaba " + h + ", emirlerinizi yerine getiriyorum.";
            case SAAT:
                return h + ", saat " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
            case TARIH:
                return h + ", bugün " + new SimpleDateFormat("d MMMM yyyy EEEE", new Locale("tr", "TR")).format(new Date());
            case BILGI:
                return bilgi(h);
            case SESSIZ:
                return sessiz(true) ? tamam + "Sessize aldım." : tamam + "Medya sesini kapattım. Zil sesi için Rahatsız Etmeyin izni gerekli.";
            case SES_AC:
                sessiz(false);
                return tamam + "Sesi açtım.";
            case SES_ARTIR:
            case SES_AZALT:
                int yon = k.tur == Komut.Tur.SES_ARTIR ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                for (int i = 0; i < 2; i++) ses.adjustStreamVolume(AudioManager.STREAM_MUSIC, yon, AudioManager.FLAG_SHOW_UI);
                return tamam;
            case MUZIK_CAL:
                return muzikCal(tamam);
            case MUZIK_ARA:
                return muzikAra(tamam, k.metin);
            case MUZIK_DURDUR:
                tus(KeyEvent.KEYCODE_MEDIA_PAUSE);
                return tamam + "Durdurdum.";
            case SONRAKI:
                tus(KeyEvent.KEYCODE_MEDIA_NEXT);
                return tamam;
            case ONCEKI:
                tus(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                return tamam;
            case YOUTUBE:
                return ac("https://www.youtube.com/results?search_query=" + Uri.encode(k.metin), "com.google.android.youtube")
                        ? tamam + "YouTube'da " + k.metin + " arıyorum." : h + ", YouTube'u açamadım.";
            case WEB_ARA:
                return ac("https://www.google.com/search?q=" + Uri.encode(k.metin), "com.android.chrome")
                        ? tamam + k.metin + " arıyorum." : h + ", tarayıcıyı açamadım.";
            case UYGULAMA_AC: {
                Uygulamalar.Uygulama u = Uygulamalar.bul(c, k.metin);
                if (u == null || !baslat(u.paket)) return h + ", " + k.metin + " uygulamasını açamadım.";
                return tamam + u.ad + " açılıyor.";
            }
            case YAZ:
            case GONDER:
            case EKRAN_OKU:
            case TIKLA:
            case GERI:
            case ANA_EKRAN:
            case SON_UYGULAMALAR:
            case BILDIRIMLER:
            case ASAGI:
            case YUKARI:
                return ekran(k, h, tamam);
            default:
                return h + ", bu komutu henüz bilmiyorum.";
        }
    }

    /** Saat, tarih, pil, internet, medya sesi ve izin durumu. */
    private String bilgi(String h) {
        BatteryManager pil = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
        ConnectivityManager ag = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        int enCok = Math.max(1, ses.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        return h + ", durum şöyle.\n"
                + "Saat: " + new SimpleDateFormat("HH:mm", Locale.US).format(new Date()) + ".\n"
                + "Tarih: " + new SimpleDateFormat("d MMMM EEEE", new Locale("tr", "TR")).format(new Date()) + ".\n"
                + "Pil: yüzde " + pil.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                + (pil.isCharging() ? ", şarj oluyor" : "") + ".\n"
                + "İnternet: " + (ag.getActiveNetwork() != null ? "bağlı" : "yok") + ".\n"
                + "Medya sesi: yüzde " + ses.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / enCok + ".\n"
                + "Ekran kontrolü: " + (EkranService.acik != null ? "açık" : "kapalı") + ".";
    }

    private String ekran(Komut k, String h, String tamam) {
        EkranService e = EkranService.acik;
        if (e == null) return h + ", bunun için Sahra uygulamasından ekran kontrolü iznini açmalısınız.";
        switch (k.tur) {
            case YAZ:
                return e.yaz(k.metin) ? tamam + "Yazdım. Göndermem için gönder deyin." : h + ", önce yazı kutusuna dokunun.";
            case GONDER:
                return e.tikla("Gönder", true) || e.tikla("Send", true) ? tamam + "Gönderdim." : h + ", gönder düğmesini bulamadım.";
            case EKRAN_OKU: {
                String yazi = e.oku();
                return yazi.isEmpty() ? h + ", ekranda okunacak yazı bulamadım." : yazi;
            }
            case TIKLA:
                return e.tikla(k.metin, false) ? tamam : h + ", " + k.metin + " bulunamadı.";
            case GERI:
                e.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return tamam;
            case ANA_EKRAN:
                e.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                return tamam;
            case SON_UYGULAMALAR:
                e.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                return tamam;
            case BILDIRIMLER:
                e.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                return tamam;
            default:
                e.kaydir(k.tur == Komut.Tur.ASAGI);
                return tamam;
        }
    }

    /** Medya sesini, izin varsa zil sesini de kapatır ya da açar. Zil de değiştiyse true. */
    private boolean sessiz(boolean kapat) {
        ses.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                kapat ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);
        NotificationManager n = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!n.isNotificationPolicyAccessGranted()) return false;
        ses.setRingerMode(kapat ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
        return true;
    }

    private void tus(int kod) {
        ses.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, kod));
        ses.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, kod));
    }

    private String muzikCal(String tamam) {
        String paket = Ayar.muzikPaketi(c);
        if (!paket.isEmpty() && !ses.isMusicActive()) baslat(paket);
        tus(KeyEvent.KEYCODE_MEDIA_PLAY);
        return tamam + "Müzik açılıyor.";
    }

    private String muzikAra(String tamam, String aranan) {
        Intent i = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*");
        i.putExtra(SearchManager.QUERY, aranan);
        String paket = Ayar.muzikPaketi(c);
        if (!paket.isEmpty()) i.setPackage(paket);
        if (dene(i)) return tamam + aranan + " açılıyor.";
        return Ayar.hitap(c) + ", seçtiğiniz uygulama sesle şarkı açmayı desteklemiyor.";
    }

    private boolean ac(String adres, String paket) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(adres));
        if (dene(new Intent(i).setPackage(paket))) return true;
        return dene(i);
    }

    private boolean baslat(String paket) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(paket);
        return i != null && dene(i);
    }

    private boolean dene(Intent i) {
        try {
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
