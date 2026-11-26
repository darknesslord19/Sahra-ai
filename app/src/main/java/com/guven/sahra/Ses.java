package com.guven.sahra;

import android.content.Context;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Türkçe konuşma sesi seçimi. */
final class Ses {
    static final String GOOGLE = "com.google.android.tts";

    static boolean googleVar(Context c) {
        try {
            c.getPackageManager().getPackageInfo(GOOGLE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Yüklü Türkçe sesler, en kalitelisi başta. İnternet yoksa yalnızca telefondakiler. */
    static List<Voice> turkce(TextToSpeech tts, boolean internet) {
        List<Voice> l = new ArrayList<Voice>();
        Set<Voice> hepsi;
        try {
            hepsi = tts.getVoices();
        } catch (RuntimeException e) {
            return l;
        }
        if (hepsi == null) return l;
        for (Voice v : hepsi) {
            if (!"tr".equals(v.getLocale().getLanguage())) continue;
            if (v.getFeatures() != null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
            if (!internet && v.isNetworkConnectionRequired()) continue;
            l.add(v);
        }
        Collections.sort(l, new Comparator<Voice>() {
            @Override
            public int compare(Voice a, Voice b) {
                int fark = puan(b) - puan(a);
                return fark != 0 ? fark : a.getName().compareTo(b.getName());
            }
        });
        return l;
    }

    private static int puan(Voice v) {
        return v.getQuality() * 10 + (v.isNetworkConnectionRequired() ? 5 : 0);
    }

    /** Kullanıcının seçtiği ses; yoksa listedeki ilk ses. */
    static Voice sec(Context c, List<Voice> sesler) {
        String secim = Ayar.al(c).getString("ses", "");
        for (Voice v : sesler) {
            if (v.getName().equals(secim)) return v;
        }
        return sesler.isEmpty() ? null : sesler.get(0);
    }
}
