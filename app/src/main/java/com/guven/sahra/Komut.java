package com.guven.sahra;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Türkçe cümleyi komuta çevirir. Android'e bağlı değildir, internetsiz çalışır. */
public final class Komut {
    public enum Tur {
        SELAM, SAAT, TARIH, SESSIZ, SES_AC, SES_ARTIR, SES_AZALT,
        MUZIK_CAL, MUZIK_ARA, MUZIK_DURDUR, SONRAKI, ONCEKI,
        YOUTUBE, WEB_ARA, UYGULAMA_AC, YAZ, GONDER, EKRAN_OKU, TIKLA,
        GERI, ANA_EKRAN, SON_UYGULAMALAR, BILDIRIMLER, ASAGI, YUKARI,
        BUYU, KUCUL, RENK, SES_DEGISTIR, KAPAN, BILINMIYOR
    }

    public final Tur tur;
    public final String metin;

    private Komut(Tur tur, String metin) {
        this.tur = tur;
        this.metin = metin;
    }

    private static final Locale TR = new Locale("tr", "TR");
    static final String[] RENKLER = {"beyaz", "siyah", "mavi", "turkuaz", "yesil", "mor", "pembe", "kirmizi", "altin"};

    /** Küçük harfe çevirir; Türkçe harfleri sadeleştirir. Karakter sayısı değişmez. */
    static String sade(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            String k = String.valueOf(c[i]).toLowerCase(TR);
            char h = k.charAt(0);
            switch (h) {
                case 'ç': h = 'c'; break;
                case 'ğ': h = 'g'; break;
                case 'ı': h = 'i'; break;
                case 'ö': h = 'o'; break;
                case 'ş': h = 's'; break;
                case 'ü': h = 'u'; break;
                case 'â': h = 'a'; break;
                case '\'': case '’': h = ' '; break;
                default: break;
            }
            c[i] = h;
        }
        return new String(c);
    }

    private static Komut k(Tur t) {
        return new Komut(t, "");
    }

    public static Komut coz(String girdi) {
        String ham = girdi == null ? "" : girdi.trim().replaceAll("\\s+", " ").replaceAll("[.!?]+$", "");
        String s = sade(ham);

        // Baştaki "sahra" ve "hey sahra" atılır
        Matcher bas = Pattern.compile("^(hey |tamam )?sahra\\b[ ,]*").matcher(s);
        if (bas.find()) {
            ham = ham.substring(bas.end());
            s = s.substring(bas.end());
            if (s.isEmpty()) return k(Tur.SELAM);
        }
        Matcher m;

        // Yazdırma en önde: yazılacak metnin içindeki sözcükler komut sayılmaz
        m = Pattern.compile("^(?:mesaj yaz|mesaja yaz|sunu yaz|yaz)[ :,]+(.+)$").matcher(s);
        if (m.find()) return parca(Tur.YAZ, ham, m);
        m = Pattern.compile("^(.+) yaz$").matcher(s);
        if (m.find()) return parca(Tur.YAZ, ham, m);

        if (s.matches("(merhaba|selam|gunaydin|iyi aksamlar|naber|nasilsin)( .*)?")) return k(Tur.SELAM);
        if (s.matches(".*\\bsaat kac\\b.*")) return k(Tur.SAAT);
        if (s.matches(".*(bugun gunlerden|tarih ne|bugunun tarihi|ayin kaci).*")) return k(Tur.TARIH);
        if (s.matches("(sesini|ses tonunu|konusma sesini) degistir")) return k(Tur.SES_DEGISTIR);
        if (s.matches("(robotu kapat|kendini kapat|kapan|gorusuruz)")) return k(Tur.KAPAN);

        if (s.matches("(.* )?(sessize al|sesi kapat|sesi kes|sus)")) return k(Tur.SESSIZ);
        if (s.matches(".*(sesi ac|sessizden cik|sessizden al)$")) return k(Tur.SES_AC);
        if (s.matches(".*sesi (artir|yukselt|biraz ac)$")) return k(Tur.SES_ARTIR);
        if (s.matches(".*sesi (azalt|kis|dusur)$")) return k(Tur.SES_AZALT);

        if (s.matches("(muzigi |sarkiyi |videoyu )?(durdur|duraklat)|muzigi kapat")) return k(Tur.MUZIK_DURDUR);
        if (s.matches("(sonraki|siradaki)( sarki| parca| video)?|sarkiyi gec")) return k(Tur.SONRAKI);
        if (s.matches("(onceki)( sarki| parca| video)?")) return k(Tur.ONCEKI);
        if (s.matches("(muzik (ac|cal|baslat))|muzigi (ac|baslat|devam ettir)|devam et|(sarki|muzik) cal")) return k(Tur.MUZIK_CAL);

        // YouTube: "youtube dan X aç", "X youtube da ara"
        m = Pattern.compile("^youtube(?: ?(?:da|de|dan|den))? (.+?) (?:ac|ara|izle|oynat|bul)$").matcher(s);
        if (m.find()) return parca(Tur.YOUTUBE, ham, m);
        m = Pattern.compile("^(.+?) youtube(?: ?(?:da|de|dan|den))? (?:ac|ara|izle|oynat|bul)$").matcher(s);
        if (m.find()) return parca(Tur.YOUTUBE, ham, m);
        m = Pattern.compile("^(.+ (?:filmi|film|videosu)) (?:ac|izle|oynat)$").matcher(s);
        if (m.find()) return parca(Tur.YOUTUBE, ham, m);

        m = Pattern.compile("^(?:chrome|krom|google|internet)(?: ?(?:da|de|dan|den|ta|te))? (.+?) (?:ara|arat|bul|ac)$").matcher(s);
        if (m.find()) return parca(Tur.WEB_ARA, ham, m);

        m = Pattern.compile("^(.+?) (?:sarkisini |sarkisi |muzigini )?cal$").matcher(s);
        if (m.find()) return parca(Tur.MUZIK_ARA, ham, m);

        if (s.matches("(mesaji )?gonder|yolla")) return k(Tur.GONDER);
        if (s.matches("(ekrani|ekranda ne var|ekrandakini|ekrandakileri)( oku)?")) return k(Tur.EKRAN_OKU);
        if (s.matches("geri( git| don| gel)?")) return k(Tur.GERI);
        if (s.matches("ana (ekran|sayfa)(a|ya)?( git| don)?")) return k(Tur.ANA_EKRAN);
        if (s.matches("son uygulamalar(i)?( ac| goster)?")) return k(Tur.SON_UYGULAMALAR);
        if (s.matches("bildirimler(i)?( ac| goster)?")) return k(Tur.BILDIRIMLER);
        if (s.matches("(asagi)( kaydir| in)?")) return k(Tur.ASAGI);
        if (s.matches("(yukari)( kaydir| cik)?")) return k(Tur.YUKARI);
        if (s.matches("(buyu|robotu buyut)")) return k(Tur.BUYU);
        if (s.matches("(kucul|robotu kucult)")) return k(Tur.KUCUL);
        for (String r : RENKLER) {
            if (s.matches("(rengini |robotu |rengin )?" + r + "( yap| ol)")) return new Komut(Tur.RENK, r);
        }

        m = Pattern.compile("^(.+?) (?:tikla|bas|dokun)$").matcher(s);
        if (m.find()) {
            Komut t = parca(Tur.TIKLA, ham, m);
            return new Komut(t.tur, t.metin.replaceAll("(?i)( (butonuna|dugmesine|tusuna)|['’](a|e|ya|ye))$", ""));
        }
        m = Pattern.compile("^(.+?) (?:uygulamasini )?(?:ac|baslat)$").matcher(s);
        if (m.find()) {
            Komut t = parca(Tur.UYGULAMA_AC, ham, m);
            return new Komut(t.tur, t.metin.replaceAll("['’](y?[ıiuü])$", ""));
        }
        return new Komut(Tur.BILINMIYOR, ham);
    }

    /** Eşleşen bölümü, Türkçe harfleri korunmuş asıl cümleden alır. */
    private static Komut parca(Tur t, String ham, Matcher m) {
        return new Komut(t, ham.substring(m.start(1), m.end(1)).trim());
    }
}
