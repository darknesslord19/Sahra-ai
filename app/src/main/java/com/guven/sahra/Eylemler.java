package com.guven.sahra;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Alarm, arama, hızlı kontroller ve takvim gibi telefon işleri. */
final class Eylemler {
    private Eylemler() {
    }

    static boolean izinVar(Context c, String izin) {
        return c.checkSelfPermission(izin) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean baslat(Context c, Intent i) {
        try {
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ---------- Saat ----------

    static boolean alarm(Context c, int dakika, String ad) {
        return baslat(c, new Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, dakika / 60)
                .putExtra(AlarmClock.EXTRA_MINUTES, dakika % 60)
                .putExtra(AlarmClock.EXTRA_MESSAGE, ad)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true));
    }

    static boolean zamanlayici(Context c, int saniye, String ad) {
        return baslat(c, new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, saniye)
                .putExtra(AlarmClock.EXTRA_MESSAGE, ad)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true));
    }

    /** Takvimde yeni etkinlik ekranı açar; kaydetmek kullanıcıya kalır. */
    static boolean takvimeEkle(Context c, long baslangic, String ad) {
        Intent i = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, ad);
        if (baslangic > 0) {
            i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, baslangic)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, baslangic + 30 * 60000);
        }
        return baslat(c, i);
    }

    /** Bugünün kalan takvim kayıtları: "14:00 Toplantı". İzin yoksa null. */
    static List<String> bugun(Context c) {
        if (!izinVar(c, Manifest.permission.READ_CALENDAR)) return null;
        Calendar son = Calendar.getInstance();
        son.set(Calendar.HOUR_OF_DAY, 23);
        son.set(Calendar.MINUTE, 59);
        Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(b, System.currentTimeMillis());
        ContentUris.appendId(b, son.getTimeInMillis());
        List<String> l = new ArrayList<String>();
        try {
            Cursor k = c.getContentResolver().query(b.build(), new String[]{CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY}, null, null,
                    CalendarContract.Instances.BEGIN + " ASC");
            if (k == null) return l;
            try {
                while (k.moveToNext() && l.size() < 5) {
                    String saat = k.getInt(2) == 1 ? "Gün boyu"
                            : new SimpleDateFormat("HH:mm", Locale.US).format(new Date(k.getLong(0)));
                    l.add(saat + " " + (k.getString(1) == null ? "adsız etkinlik" : k.getString(1)));
                }
            } finally {
                k.close();
            }
        } catch (RuntimeException e) {
            return null;
        }
        return l;
    }

    // ---------- Arama ----------

    /** Eşleşen kişiler: {ad, numara}. Her ad bir kez, mobil/varsayılan numara öncelikli. */
    static List<String[]> kisiler(Context c, String soylenen) {
        List<String> adlar = new ArrayList<String>();
        List<String> numaralar = new ArrayList<String>();
        List<Integer> puanlar = new ArrayList<Integer>();
        Cursor k = c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.TYPE}, null, null, null);
        if (k != null) {
            try {
                while (k.moveToNext()) {
                    String ad = k.getString(0);
                    String no = k.getString(1);
                    if (ad == null || no == null) continue;
                    int puan = (k.getInt(2) != 0 ? 2 : 0)
                            + (k.getInt(3) == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE ? 1 : 0);
                    int i = adlar.indexOf(ad);
                    if (i < 0) {
                        adlar.add(ad);
                        numaralar.add(no);
                        puanlar.add(puan);
                    } else if (puan > puanlar.get(i)) {
                        numaralar.set(i, no);
                        puanlar.set(i, puan);
                    }
                }
            } finally {
                k.close();
            }
        }
        List<String[]> l = new ArrayList<String[]>();
        for (int i : KisiBul.bul(soylenen, adlar)) l.add(new String[]{adlar.get(i), numaralar.get(i)});
        return l;
    }

    /** Arama izni varsa arar, yoksa numarayı çevirme ekranına yazar. Doğrudan arandıysa true. */
    static boolean ara(Context c, String numara) {
        Uri u = Uri.fromParts("tel", numara, null);
        if (izinVar(c, Manifest.permission.CALL_PHONE) && baslat(c, new Intent(Intent.ACTION_CALL, u))) return true;
        baslat(c, new Intent(Intent.ACTION_DIAL, u));
        return false;
    }

    // ---------- Hızlı kontroller ----------

    static boolean fener(Context c, boolean ac) {
        CameraManager m = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : m.getCameraIdList()) {
                Boolean flas = m.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flas != null && flas) {
                    m.setTorchMode(id, ac);
                    return true;
                }
            }
        } catch (CameraAccessException | RuntimeException e) {
            return false;
        }
        return false;
    }

    static boolean kamera(Context c) {
        return baslat(c, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
    }

    /** istek: "+", "-" ya da yüzde. Yeni yüzdeyi döndürür; izin yoksa -1. */
    static int parlaklik(Context c, String istek) {
        if (!Settings.System.canWrite(c)) return -1;
        int simdi = Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128) * 100 / 255;
        int yeni = istek.equals("+") ? simdi + 25 : istek.equals("-") ? simdi - 25 : Integer.parseInt(istek);
        yeni = Math.max(5, Math.min(100, yeni));
        Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, yeni * 255 / 100);
        return yeni;
    }

    static boolean yolTarifi(Context c, String yer) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(yer)));
        if (baslat(c, new Intent(i).setPackage("com.google.android.apps.maps"))) return true;
        return baslat(c, new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(yer))));
    }

    static boolean ayarEkrani(Context c, String tur) {
        String eylem = tur.equals("wifi") ? Settings.ACTION_WIFI_SETTINGS
                : tur.equals("bluetooth") ? Settings.ACTION_BLUETOOTH_SETTINGS
                : tur.equals("konum") ? Settings.ACTION_LOCATION_SOURCE_SETTINGS
                : tur.equals("ucak") ? Settings.ACTION_AIRPLANE_MODE_SETTINGS
                : tur.equals("veri") ? Settings.ACTION_DATA_ROAMING_SETTINGS
                : Settings.ACTION_WIRELESS_SETTINGS;
        return baslat(c, new Intent(eylem));
    }
}
