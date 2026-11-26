package com.guven.sahra;

import java.util.ArrayList;
import java.util.List;

/** Son birkaç soru ve cevabı yalnızca bellekte tutar. */
public final class Hafiza {
    private static final int EN_COK = 4;
    private static final List<String[]> LISTE = new ArrayList<String[]>();
    private static long son;

    public static synchronized void ekle(String soru, String cevap, long simdi) {
        if (cevap.length() > 600) cevap = cevap.substring(0, 600);
        LISTE.add(new String[]{soru, cevap});
        while (LISTE.size() > EN_COK) LISTE.remove(0);
        son = simdi;
    }

    /** Süresi dolmamış geçmiş: {soru, cevap} listesi. */
    public static synchronized List<String[]> al(int dakika, long simdi) {
        if (simdi - son > dakika * 60000L) LISTE.clear();
        return new ArrayList<String[]>(LISTE);
    }

    public static synchronized void temizle() {
        LISTE.clear();
    }
}
