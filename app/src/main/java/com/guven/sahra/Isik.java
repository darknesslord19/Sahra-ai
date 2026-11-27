package com.guven.sahra;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Robotun kenar ışığı: renkler, yanma şekilleri ve robot.html'e giden ayar metni. */
public final class Isik {
    /** {kimlik, görünen ad, renk} */
    public static final String[][] RENKLER = {
            {"yesil", "Yeşil", "#34d399"}, {"turkuaz", "Turkuaz", "#22d3ee"}, {"mavi", "Mavi", "#3b82f6"},
            {"mor", "Mor", "#8b5cf6"}, {"pembe", "Pembe", "#ec4899"}, {"kirmizi", "Kırmızı", "#ef4444"},
            {"turuncu", "Turuncu", "#fb923c"}, {"sari", "Sarı", "#facc15"}, {"beyaz", "Beyaz", "#e8f0ff"},
            {"lime", "Lime", "#a3e635"}};
    /** {kimlik, görünen ad, açıklama} */
    public static final String[][] SEKILLER = {
            {"nefes", "Nefes", "Yavaşça parlar, yavaşça söner. Renk en sönük anda değişir."},
            {"yanson", "Yanıp sönme", "Daha belirgin: yanar, kısa süre kalır, söner."},
            {"kalp", "Kalp atışı", "Art arda iki vuruş, sonra kısa bir bekleme."},
            {"donen", "Dönen renkler", "Seçili renkler kenarda birlikte döner ve parlaklığı azalıp çoğalır."},
            {"sabit", "Sabit", "Parlaklık değişmez; renkler yumuşakça sırayla geçer."}};
    /** {görünen ad, renk kimlikleri} */
    public static final String[][] TAKIMLAR = {
            {"Zümrüt", "yesil"}, {"Okyanus", "turkuaz,mavi"}, {"Gün batımı", "turuncu,pembe,mor"},
            {"Orman", "yesil,lime,turkuaz"}, {"Gökkuşağı", "kirmizi,turuncu,sari,yesil,turkuaz,mavi,mor"}};

    public static final String ILK_RENKLER = "yesil,turkuaz,mor";
    public static final int ILK_HIZ = 62, ILK_UST = 100, ILK_ALT = 15, ILK_YAY = 45;

    private static int sira(String[][] liste, String kimlik) {
        for (int i = 0; i < liste.length; i++) {
            if (liste[i][0].equals(kimlik)) return i;
        }
        return -1;
    }

    /** Tanınmayan ve yinelenen adları atar; hiç renk kalmazsa varsayılanı döndürür. */
    public static List<String> secili(String metin) {
        List<String> l = new ArrayList<String>();
        for (String a : String.valueOf(metin).split(",")) {
            if (sira(RENKLER, a.trim()) >= 0 && !l.contains(a.trim())) l.add(a.trim());
        }
        return l.isEmpty() ? secili(ILK_RENKLER) : l;
    }

    /** Rengi ekler ya da çıkarır. Seçim sırası korunur; son renk çıkarılamaz. */
    public static String degistir(String metin, String kimlik) {
        List<String> l = secili(metin);
        if (!l.contains(kimlik)) l.add(kimlik);
        else if (l.size() > 1) l.remove(kimlik);
        return String.join(",", secili(String.join(",", l)));
    }

    public static String sekil(String kimlik) {
        return sira(SEKILLER, kimlik) >= 0 ? kimlik : SEKILLER[0][0];
    }

    public static String sekilAdi(String kimlik) {
        return SEKILLER[sira(SEKILLER, sekil(kimlik))][1];
    }

    /** Hız kaydırıcısı (0-100) sağa gittikçe hızlanır. En hızlısı 0,8 sn: göz yoran çakma olmaz. */
    public static double saniye(int hiz) {
        return Math.round((5 - 4.2 * sinirla(hiz, 0, 100) / 100) * 10) / 10.0;
    }

    private static int sinirla(int d, int az, int cok) {
        return Math.max(az, Math.min(d, cok));
    }

    /** Yalnızca listelerdeki değerler ve sınırlanmış sayılar yazılır. */
    public static String json(String renkler, String sekil, int hiz, int ust, int alt, int yay,
                              boolean goz, boolean dinle) {
        StringBuilder r = new StringBuilder();
        for (String a : secili(renkler)) {
            if (r.length() > 0) r.append(',');
            r.append('"').append(RENKLER[sira(RENKLER, a)][2]).append('"');
        }
        int enAz = sinirla(alt, 0, 70);
        return String.format(Locale.US,
                "{\"renkler\":[%s],\"sekil\":\"%s\",\"sure\":%.1f,\"ust\":%.2f,\"alt\":%.2f,\"yay\":%.2f,\"goz\":%b,\"dinle\":%b}",
                r, sekil(sekil), saniye(hiz), Math.max(enAz, sinirla(ust, 40, 100)) / 100.0, enAz / 100.0,
                sinirla(yay, 0, 100) / 100.0, goz, dinle);
    }
}
