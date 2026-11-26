package com.guven.sahra;

import java.util.ArrayList;
import java.util.List;

/** Uzun metni, sözcükleri bölmeden balon sayfalarına ayırır. */
public final class Sayfa {
    public static List<String> bol(String metin, int sinir) {
        List<String> l = new ArrayList<String>();
        String kalan = metin;
        while (kalan.length() > sinir) {
            int yer = Math.max(kalan.lastIndexOf(' ', sinir), kalan.lastIndexOf('\n', sinir));
            if (yer <= 0) yer = sinir;
            l.add(kalan.substring(0, yer).trim());
            kalan = kalan.substring(yer).trim();
        }
        l.add(kalan);
        return l;
    }
}
