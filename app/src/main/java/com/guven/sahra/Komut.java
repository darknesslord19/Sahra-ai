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
        BUYU, KUCUL, RENK, SES_DEGISTIR, BILGI, SORU, GOOGLE_SORU, PENCERE_KAPAT, DAVRANIS, OKUMA_DUR, HAVA, KUR, EFEKT, GUZEL_SOZ, YAPAY_SORU, KAPAN,
        ALARM, ZAMANLAYICI, HATIRLAT, ARA, FENER, KAMERA, PARLAKLIK, YOL_TARIFI, AYAR_AC,
        OZET, BILDIRIM_OKU, NOT_AL, NOT_OKU, NOT_SIL, CEVIR, ARABA, HAFIZA_SIL, FORM_DOLDUR, BILINMIYOR
    }

    public final Tur tur;
    public final String metin;
    /** Komutun söylendiği hâliyle tamamı (baştaki "Sahra" olmadan). */
    public String cumle = "";

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
        Komut k = ayristir(girdi);
        String c = girdi == null ? "" : girdi.trim().replaceAll("\\s+", " ");
        if (sade(c).matches("(hey |tamam )?sahra\\b[ ,]+.*")) c = c.replaceFirst("^(?i)(\\S+ )?sahra[ ,]+", "");
        k.cumle = c;
        return k;
    }

    private static Komut ayristir(String girdi) {
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

        // Açıkça yapay zekâya sorulanlar: "sor ...", "Gemini ..."
        m = Pattern.compile("^(?:geminiye sor|gemini ye sor|yapay zekaya sor|gemini|sor)[ :,]+(.+)$").matcher(s);
        if (m.find()) return parca(Tur.YAPAY_SORU, ham, m);

        // Not ve çeviri yazdırmadan önce: içlerindeki "yaz" sözcüğü komut sayılmaz
        m = Pattern.compile("^(?:not al|not et|sunu not al|bir not al|not yaz)[ :,]+(.+)$").matcher(s);
        if (m.find()) return parca(Tur.NOT_AL, ham, m);
        m = Pattern.compile("^(.+?) (?:diye )?not (?:al|et)$").matcher(s);
        if (m.find()) return parca(Tur.NOT_AL, ham, m);
        if (s.matches("not (al|et)")) return new Komut(Tur.NOT_AL, "");
        m = Pattern.compile("^(?:bunu |su cumleyi )?(" + DILLER + ")(?:ye|ya) cevir[ :,]+(.+)$").matcher(s);
        if (m.find()) return ceviri(m.group(1), ham.substring(m.start(2), m.end(2)));
        m = Pattern.compile("^(.+?) (" + DILLER + ")(?:ye|ya) cevir$").matcher(s);
        if (m.find()) {
            String metin = ham.substring(m.start(1), m.end(1));
            if (m.group(1).matches("bunu|su cumleyi|sunu")) metin = "";
            return ceviri(m.group(2), metin.replaceAll("['’](y?[ıiuü])$", ""));
        }
        m = Pattern.compile("^(" + DILLER + ")(?:ye|ya) cevir$").matcher(s);
        if (m.find()) return ceviri(m.group(1), "");

        // Kayıt bilgileri: "mailimi yaz" sözcüğü sözcüğüne yazdırma sayılmasın diye yazdırmadan öncedir
        if (s.matches("(?:bu )?(?:(?:kayit )?formu(?:nu)?(?: otomatik)?|(?:kayit )?bilgilerimi|bilgileri|otomatik) (?:doldur|gir|yaz)(?:ur musun| bana)?|kayit bilgilerimle doldur|otomatik doldur")) {
            return new Komut(Tur.FORM_DOLDUR, "");
        }
        m = Pattern.compile("^(e[- ]?postami|e[- ]?posta adresimi|mailimi|mail adresimi|gmailimi|gmail adresimi|adimi soyadimi|ad soyadimi|adimi|ismimi|soyadimi|soy ?ismimi|telefonumu|telefon numarami|numarami|kullanici adimi|sifremi|parolami) (?:yaz|gir|doldur)$").matcher(s);
        if (m.find()) {
            String a = m.group(1);
            Form.Alan alan = a.contains("posta") || a.contains("mail") ? Form.Alan.EPOSTA
                    : a.startsWith("kullanici") ? Form.Alan.KULLANICI
                    : a.startsWith("sifre") || a.startsWith("parola") ? Form.Alan.SIFRE
                    : a.contains("telefon") || a.startsWith("numara") ? Form.Alan.TELEFON
                    : a.contains(" soyad") ? Form.Alan.ADSOYAD
                    : a.startsWith("soy") ? Form.Alan.SOYAD : Form.Alan.AD;
            return new Komut(Tur.FORM_DOLDUR, alan.name());
        }

        // Yazdırma en önde: yazılacak metnin içindeki sözcükler komut sayılmaz
        m = Pattern.compile("^(?:mesaj yaz|mesaja yaz|sunu yaz|yaz)[ :,]+(.+)$").matcher(s);
        if (m.find()) return parca(Tur.YAZ, ham, m);
        m = Pattern.compile("^(.+) yaz$").matcher(s);
        if (m.find()) return parca(Tur.YAZ, ham, m);

        if (s.matches("(gunaydin)( sahra| hocam)?|(sabah|gunun|gunluk) ozeti?( ver| soyle| oku)?")) return k(Tur.OZET);
        Komut yeni = yeniKomut(ham, s);
        if (yeni != null) return yeni;
        if (s.matches("(merhaba|selam|gunaydin|iyi aksamlar|naber|nasilsin)( .*)?")) return k(Tur.SELAM);
        if (s.matches(".*\\bsaat kac\\b.*")) return k(Tur.SAAT);
        if (s.matches(".*(bugun gunlerden|tarih ne|bugunun tarihi|ayin kaci).*")) return k(Tur.TARIH);
        if (s.matches("(sesini|ses tonunu|konusma sesini) degistir")) return k(Tur.SES_DEGISTIR);
        if (s.matches("(butun |tum )?(bilgi(ler)?(i)?( ver| goster| soyle)?|durum( raporu)?( ver)?|telefon durumu|pil( durumu| kac| yuzde kac)?|sarj( kac| durumu| yuzde kac)?)")) return k(Tur.BILGI);
        if (s.matches("(robotu kapat|kendini kapat|kapan|gorusuruz)")) return k(Tur.KAPAN);

        if (s.matches("(.* )?(sessize al|sesi kapat|sesi kes|sus)")) return k(Tur.SESSIZ);
        if (s.matches(".*(sesi ac|sessizden cik|sessizden al)$")) return k(Tur.SES_AC);
        if (s.matches(".*sesi (artir|yukselt|biraz ac)$")) return k(Tur.SES_ARTIR);
        if (s.matches(".*sesi (azalt|kis|dusur)$")) return k(Tur.SES_AZALT);

        if (s.matches("(bugun )?hava( durumu)?( nasil| ne| kac derece| soyle)?|(disarisi )?kac derece")) return k(Tur.HAVA);
        if (s.matches("(bugun )?(dolar|euro|avro|altin|gram altin|doviz|kurlar|doviz kurlari)( ne kadar| kac( tl| lira)?| durumu| kuru| fiyati| soyle)?")) return k(Tur.KUR);
        if (s.matches("(bana )?(guzel )?(bir )?(guzel soz|soz soyle|atasozu)( soyle)?")) return k(Tur.GUZEL_SOZ);
        if (s.matches("kar yagdir")) return new Komut(Tur.EFEKT, "kar");
        if (s.matches("yagmur yagdir")) return new Komut(Tur.EFEKT, "yagmur");
        if (s.matches("yaprak(lari)? (dok|dokul|dokur|yagdir)")) return new Komut(Tur.EFEKT, "yaprak");
        if (s.matches("gunes (ac|actir|cikar)")) return new Komut(Tur.EFEKT, "gunes");
        if (s.matches("efekti (kapat|durdur)")) return new Komut(Tur.EFEKT, "");
        if (s.matches("okumayi (durdur|kes|birak)|okuma|dur|yeter")) return k(Tur.OKUMA_DUR);
        if (s.matches("(pencereyi|bilgiyi|popup ?i?) kapat|kapat")) return k(Tur.PENCERE_KAPAT);
        for (String[] d : Davranis.LISTE) {
            String ad = sade(d[1]);
            if (s.matches("(davranisini |gozlerini |gozunu )" + ad + "( goz)?( yap| ol)?")
                    || s.matches(ad + " (davran|goz(ler)?( yap)?|mod(u)?)")) return new Komut(Tur.DAVRANIS, d[0]);
        }

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
        if (s.matches("ekranda ne var|(.* )?(oku|okur musun|okuyabilir misin|okusana)( bana| lutfen)?")) return k(Tur.EKRAN_OKU);
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

        // Arama: web ve YouTube aramaları yukarıda ayrıldığı için kalan "X ara" kişi aramasıdır
        m = Pattern.compile("^(?:telefonla )?(.+?)(?: telefonla| i telefonla)? (?:ara|arar misin)$").matcher(s);
        if (m.find() && m.group(1).split(" ").length <= 4 && !m.group(1).matches("(.* )?(internet\\S*|google\\S*|youtube\\S*|chrome\\S*|web\\S*|arama)( .*)?")) {
            return parca(Tur.ARA, ham, m);
        }
        m = Pattern.compile("^(.+?) icin yol tarifi(?: ver| ac| al| goster)?$").matcher(s);
        if (m.find()) return yer(ham, m);
        m = Pattern.compile("^(.+?) (?:yol tarifi)(?: ver| ac| al| goster)?$").matcher(s);
        if (m.find()) return yer(ham, m);
        m = Pattern.compile("^yol tarifi (?:ver |ac |al )?(.+)$").matcher(s);
        if (m.find()) return yer(ham, m);
        m = Pattern.compile("^(.+?) (?:git|gidelim|gotur|beni gotur|navigasyon(?:u)?(?: ac| baslat)?)$").matcher(s);
        if (m.find() && (m.group(1).matches(".*(e|a)") || ham.indexOf('\'') > 0 || ham.indexOf('’') > 0)) return yer(ham, m);

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

        // Sorular en sonda: yukarıdaki komutlardan hiçbiri değilse
        m = Pattern.compile("^(.+?) (?:kim|kimdir|kimdi|nedir|ne demek)$").matcher(s);
        if (m.find()) return parca(Tur.SORU, ham, m);
        m = Pattern.compile("^(.+?) hakkinda(?: bilgi)?(?: ver| goster| soyle)?$").matcher(s);
        if (m.find()) return parca(Tur.SORU, ham, m);
        m = Pattern.compile("^(.+?) arastir$").matcher(s);
        if (m.find()) return parca(Tur.GOOGLE_SORU, ham, m);
        if (s.indexOf(' ') > 0 && s.matches("(.* )?(nerede|neresi|nereli|ne zaman|nasil|neden|niye|kac|kacta|kimin|hangi|mi|mu|midir|mudur)( .*)?")) {
            return new Komut(Tur.GOOGLE_SORU, ham);
        }
        return new Komut(Tur.BILINMIYOR, ham);
    }

    static final String DILLER = "ingilizce|almanca|fransizca|arapca|rusca|ispanyolca|italyanca|farsca|turkce";

    private static Komut ceviri(String dil, String metin) {
        return new Komut(Tur.CEVIR, dil + "|" + metin.trim());
    }

    private static Komut yer(String ham, Matcher m) {
        String y = ham.substring(m.start(1), m.end(1)).replaceAll("['’](y?[ea])$", "").trim();
        String s = sade(y);
        if (s.matches("ev|eve|evime|eve dogru")) y = "ev";
        else if (s.matches("is|ise|isyeri|is yerine|isime|is yerime")) y = "iş";
        return new Komut(Tur.YOL_TARIFI, y);
    }

    /** Alarm, hızlı kontroller, bildirim, not ve araba modu komutları; uymuyorsa null. */
    private static Komut yeniKomut(String ham, String s) {
        if (s.matches(".*\\balarm\\w*\\b.*(kur|ayarla|olustur)$|(.* )?(beni )?uyandir$|.*alarm kur\\w*( .*)?")) {
            int sure = Zaman.sure(s);
            if (sure > 0 && s.contains("sonra")) return new Komut(Tur.ALARM, "+" + sure);
            int dk = Zaman.saat(s);
            return new Komut(Tur.ALARM, dk < 0 ? "" : String.valueOf(dk));
        }
        if (s.matches(".*(hatirlat|hatirlatici)( kur| ekle| olustur| et)?(r misin)?$|(hatirlat|hatirlatici)[ :,].*")) {
            return new Komut(Tur.HATIRLAT, ham);
        }
        if (s.matches(".*(zamanlayici|sayac|geri sayim|kronometre).*(kur|baslat|ac|say|ayarla)$|(.+ )?(dakika|saniye|saat) (say|tut)$")) {
            int sure = Zaman.sure(s);
            return new Komut(Tur.ZAMANLAYICI, sure > 0 ? String.valueOf(sure) : "");
        }
        if (s.matches("(el )?fener(i|ini)? (ac|yak)|isik (ac|yak)")) return new Komut(Tur.FENER, "ac");
        if (s.matches("(el )?fener(i|ini)? (kapat|sondur)|isigi (kapat|sondur)")) return new Komut(Tur.FENER, "kapat");
        if (s.matches("(kamera(yi)?|fotograf makinesi(ni)?) ac|(fotograf|resim) cek")) return k(Tur.KAMERA);
        if (s.matches("(ekran )?(parlakligi(ni)?|isigini) (artir|yukselt|ac|fazlalastir)")) return new Komut(Tur.PARLAKLIK, "+");
        if (s.matches("(ekran )?(parlakligi(ni)?|isigini) (azalt|kis|dusur)")) return new Komut(Tur.PARLAKLIK, "-");
        Matcher m = Pattern.compile("^(?:ekran )?parlakligi(?:ni)? (?:yuzde )?(\\d{1,3})(?: ?(?:e|a|ye|ya))?(?: yap| ayarla)?$").matcher(s);
        if (m.find()) return new Komut(Tur.PARLAKLIK, m.group(1));
        m = Pattern.compile("^(wi[- ]?fi|vay[- ]?fay|kablosuz ag|bluetooth|blutut|konum|gps|ucak modu|mobil veri|internet paylasimi|hotspot)(?: ?(?:u|yu|i|yi|nu|ni|unu))?(?: ayarlarini| ayari| ayarlari)? (?:ac|kapat|goster)$").matcher(s);
        if (m.find()) {
            String a = m.group(1);
            String tur = a.startsWith("w") || a.startsWith("v") || a.startsWith("kab") ? "wifi"
                    : a.startsWith("bl") ? "bluetooth" : a.equals("konum") || a.equals("gps") ? "konum"
                    : a.equals("ucak modu") ? "ucak" : a.equals("mobil veri") ? "veri" : "paylasim";
            return new Komut(Tur.AYAR_AC, tur);
        }
        if (s.matches("(bildirimleri(mi)?|bildirimlerimi|(yeni|gelen) (mesajlar|bildirimler)(i|imi)?) oku(r musun)?")) return k(Tur.BILDIRIM_OKU);
        if (s.matches("(notlarimi|notlari) (oku|goster|soyle)|notlarim( neler)?")) return k(Tur.NOT_OKU);
        if (s.matches("(notlarimi|notlari|tum notlari|butun notlari) (sil|temizle)")) return k(Tur.NOT_SIL);
        if (s.matches("araba modu(nu)?( ac| baslat)?|surus modu(nu)?( ac)?")) return new Komut(Tur.ARABA, "ac");
        if (s.matches("(araba|surus) modu(nu|ndan)? (kapat|bitir|cik)")) return new Komut(Tur.ARABA, "kapat");
        if (s.matches("(hafizayi|sohbet hafizasini|konusmayi) (temizle|sil|unut)|konustuklarimizi unut")) return k(Tur.HAFIZA_SIL);
        return null;
    }

    /** Eşleşen bölümü, Türkçe harfleri korunmuş asıl cümleden alır. */
    private static Komut parca(Tur t, String ham, Matcher m) {
        return new Komut(t, ham.substring(m.start(1), m.end(1)).trim());
    }
}
