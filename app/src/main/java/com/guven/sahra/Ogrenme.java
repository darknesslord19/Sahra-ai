package com.guven.sahra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Onaylı öğrenme kuralları: "film zamanı deyince YouTube aç" gibi öğretmeleri çözer, öğrenilen sözü bulur,
 * neyin öğrenilebileceğine ve geçmişe neyin yazılabileceğine karar verir. Yapay zekâ kullanmaz.
 */
public final class Ogrenme {
    public enum Tur { OGRET, TEKRAR, LISTE, SONUNCUYU_UNUT, HEPSINI_UNUT }

    public static final class Istek {
        public final Tur tur;
        /** OGRET için: söylenecek söz ve yapılacak komut. */
        public final String soz, hedef;

        private Istek(Tur tur, String soz, String hedef) {
            this.tur = tur;
            this.soz = soz;
            this.hedef = hedef;
        }
    }

    private static final Pattern OGRET = Pattern.compile("^(?:(?:bunu )?ogren[ :]+|bundan sonra )?(?:ben )?(.+?) (?:dedigimde|dedigim zaman|deyince|diyince|dersem) (.+)$");

    public static Istek coz(String cumle) {
        String ham = cumle == null ? "" : cumle.trim().replaceAll("\\s+", " ").replaceAll("[.!?]+$", "");
        String s = Komut.sade(ham);
        Matcher bas = Pattern.compile("^(?:(?:hey|tamam) )?" + Isim.desen() + "\\b[ ,]*").matcher(s);
        if (bas.find()) {
            ham = ham.substring(bas.end());
            s = s.substring(bas.end());
        }
        if (s.matches("(?:(?:son )?(?:komutu|islemi) tekrarla|(?:tekrar|yine|bir daha) yap|ayni(?:sini)?(?: seyi)? (?:yine |tekrar )?yap|tekrarla)")) {
            return new Istek(Tur.TEKRAR, "", "");
        }
        if (s.matches("(?:neler ogrendin|ne(?:ler)? ogrendin bana soyle|ogrendiklerini (?:soyle|say|goster|oku))")) return new Istek(Tur.LISTE, "", "");
        if (s.matches("(?:son ogrendigini unut|son ogrendigini sil)")) return new Istek(Tur.SONUNCUYU_UNUT, "", "");
        if (s.matches("(?:ogrendiklerini|ogrendigin her seyi|ogrendiklerinin hepsini) (?:unut|sil)")) return new Istek(Tur.HEPSINI_UNUT, "", "");
        if (s.matches("(?:yaz|mesaj yaz|sunu yaz|not al|sor)\\b.*")) return null;
        Matcher m = OGRET.matcher(s);
        if (!m.find()) return null;
        String soz = ham.substring(m.start(1), m.end(1)).replaceAll("^[\"“'‘]+|[\"”'’,:]+$", "").trim();
        String hedef = ham.substring(m.start(2), m.end(2)).replaceAll("^[\"“,: ]+|[\"” ]+$", "").trim();
        return new Istek(Tur.OGRET, soz, hedef);
    }

    public static String anahtar(String soz) {
        return Ozel.anahtar(soz);
    }

    /** Onay cevaplarıyla karışacak ya da çok kısa sözler öğrenilmez. */
    public static boolean sozUygun(String soz) {
        String a = anahtar(soz);
        return a.length() >= 3 && a.length() <= 60 && Onay.coz(a) == 0 && !a.matches("gonder|yolla");
    }

    /**
     * Öğrenilen bir sözün çalıştırabileceği komut mu? Mesaj gönderme ve yazma, form doldurma
     * ve silme komutları hiçbir zaman başka bir söze bağlanmaz.
     */
    public static boolean hedefUygun(String hedef) {
        if (Kumanda.coz(hedef) != null || Secim.coz(hedef) != null || Beyin.coz(hedef, false) != null) return true;
        Komut.Tur t = Komut.coz(hedef).tur;
        switch (t) {
            case BILINMIYOR: case GONDER: case YAZ: case FORM_DOLDUR: case NOT_SIL: case HAFIZA_SIL:
                return false;
            default:
                return true;
        }
    }

    /** Geçmişe yazılabilecek komut mu? İçinde kişisel metin, kişi ya da arama sözü olabilenler yazılmaz. */
    public static boolean gecmiseUygun(Komut.Tur t) {
        switch (t) {
            case SAAT: case TARIH: case BILGI: case HAVA: case KUR:
            case SESSIZ: case SES_AC: case SES_ARTIR: case SES_AZALT:
            case MUZIK_CAL: case MUZIK_DURDUR: case SONRAKI: case ONCEKI:
            case UYGULAMA_AC: case FENER: case KAMERA: case PARLAKLIK: case AYAR_AC:
            case GERI: case ANA_EKRAN: case ASAGI: case YUKARI: case SON_UYGULAMALAR: case BILDIRIMLER:
            case EFEKT: case GUZEL_SOZ: case OZET: case ARABA: case DAVRANIS: case RENK: case BUYU: case KUCUL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Söylenen söz öğrenilenlerden birine uyuyorsa onun anahtarı, yoksa null. Uzun sözlerde ses tanımanın
     * tek harflik farkı hoş görülür; iki ayrı söz aynı derecede yakınsa hiçbiri seçilmez.
     */
    public static String bul(Map<String, String> ogrenilen, String soylenen) {
        String a = anahtar(soylenen);
        if (ogrenilen.containsKey(a)) return a;
        if (a.length() < 10) return null;
        String yakin = null;
        for (String k : ogrenilen.keySet()) {
            if (birFark(a, k)) {
                if (yakin != null) return null;
                yakin = k;
            }
        }
        return yakin;
    }

    private static boolean birFark(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return false;
        int i = 0;
        while (i < a.length() && i < b.length() && a.charAt(i) == b.charAt(i)) i++;
        int ka = a.length() >= b.length() ? 1 : 0;
        int kb = b.length() >= a.length() ? 1 : 0;
        return a.substring(Math.min(a.length(), i + ka)).equals(b.substring(Math.min(b.length(), i + kb)));
    }

    /**
     * Ses tanımanın seçenekleri arasından karar: öğretilmiş bir söz varsa o; ilk seçenek anlaşılmıyorsa
     * daha önce kullanılmış bir komuta uyan; ikisi de yoksa ilk seçenek.
     */
    public static String sec(List<String> secenekler, Map<String, String> ogrenilen, Map<String, Integer> gecmis) {
        String ilk = secenekler.get(0);
        if (ogrenilen.containsKey(anahtar(ilk))) return ilk;
        for (String s : secenekler) {
            if (ogrenilen.containsKey(anahtar(s))) return s;
        }
        if (Komut.coz(ilk).tur != Komut.Tur.BILINMIYOR || Kumanda.coz(ilk) != null || Secim.coz(ilk) != null) return ilk;
        String iyi = ilk;
        int enCok = 0;
        for (String s : new ArrayList<String>(secenekler)) {
            Integer n = gecmis.get(anahtar(s));
            if (n != null && n > enCok) {
                enCok = n;
                iyi = s;
            }
        }
        return iyi;
    }
}
