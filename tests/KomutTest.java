import com.guven.sahra.Komut;

public class KomutTest {
    static int hata = 0;

    static void t(String girdi, String tur, String metin) {
        Komut k = Komut.coz(girdi);
        if (!k.tur.name().equals(tur) || !k.metin.equals(metin)) {
            hata++;
            System.out.println("HATA: " + girdi + " -> " + k.tur + " [" + k.metin + "]");
        }
    }

    static void denetle(String ad, boolean dogru) {
        if (!dogru) {
            hata++;
            System.out.println("HATA " + ad);
        }
    }

    public static void main(String[] a) {
        t("Merhaba Sahra", "SELAM", "");
        t("Sahra", "SELAM", "");
        t("Hey Sahra saat kaç?", "SAAT", "");
        t("telefonu sessize al", "SESSIZ", "");
        t("sus", "SESSIZ", "");
        t("sesini değiştir", "SES_DEGISTIR", "");
        t("Sesi aç", "SES_AC", "");
        t("sesi kıs", "SES_AZALT", "");
        t("sesi yükselt", "SES_ARTIR", "");
        t("müzik aç", "MUZIK_CAL", "");
        t("müziği durdur", "MUZIK_DURDUR", "");
        t("sonraki şarkı", "SONRAKI", "");
        t("Tarkan Kuzu Kuzu çal", "MUZIK_ARA", "Tarkan Kuzu Kuzu");
        t("YouTube'dan aksiyon filmi aç", "YOUTUBE", "aksiyon filmi");
        t("Kurtlar Vadisi filmi aç", "YOUTUBE", "Kurtlar Vadisi filmi");
        t("Chrome'da hava durumu ara", "WEB_ARA", "hava durumu");
        t("WhatsApp aç", "UYGULAMA_AC", "WhatsApp");
        t("WhatsApp'ı aç", "UYGULAMA_AC", "WhatsApp");
        t("Ayarlar uygulamasını aç", "UYGULAMA_AC", "Ayarlar");
        t("mesaj yaz Akşam geliyorum, müzik aç", "YAZ", "Akşam geliyorum, müzik aç");
        t("Nasılsın canım yaz", "YAZ", "Nasılsın canım");
        t("gönder", "GONDER", "");
        t("ekranı oku", "EKRAN_OKU", "");
        t("geri dön", "GERI", "");
        t("ana ekrana git", "ANA_EKRAN", "");
        t("aşağı kaydır", "ASAGI", "");
        t("rengini mavi yap", "RENK", "mavi");
        t("Tamam butonuna tıkla", "TIKLA", "Tamam");
        t("bugün canım sıkılıyor", "BILINMIYOR", "bugün canım sıkılıyor");
        t("pil kaç", "BILGI", "");
        t("bütün bilgileri göster", "BILGI", "");
        String uzun = new String(new char[100]).replace("\0", "kelime ").trim();
        java.util.List<String> p = com.guven.sahra.Sayfa.bol(uzun, 260);
        if (!String.join(" ", p).equals(uzun) || p.size() < 3) hata++;
        for (String x : p) if (x.length() > 260) hata++;
        if (com.guven.sahra.Sayfa.bol("", 260).size() != 1) hata++;
        java.util.List<com.guven.sahra.Ozel> ozeller = java.util.Arrays.asList(
                new com.guven.sahra.Ozel("İş modu", "sessize al\nWhatsApp aç; mesaj yaz geldim", ""));
        if (com.guven.sahra.Ozel.bul(ozeller, "Sahra, iş modu.") == null) hata++;
        if (com.guven.sahra.Ozel.bul(ozeller, "iş modunu kapat") != null) hata++;
        if (com.guven.sahra.Ozel.bul(ozeller, "") != null) hata++;
        if (com.guven.sahra.Ozel.adimlar(ozeller.get(0).isler).size() != 3) hata++;
        t("Atatürk kim", "SORU", "Atatürk");
        t("Sahra, Atatürk kimdir?", "SORU", "Atatürk");
        t("yapay zekâ nedir", "SORU", "yapay zekâ");
        t("Ankara hakkında bilgi ver", "SORU", "Ankara");
        t("İstanbul'un nüfusu kaç", "GOOGLE_SORU", "İstanbul'un nüfusu kaç");
        t("maç ne zaman", "GOOGLE_SORU", "maç ne zaman");
        t("pencereyi kapat", "PENCERE_KAPAT", "");
        t("kalp göz yap", "DAVRANIS", "heart");
        t("rastgele davran", "DAVRANIS", "rastgele");
        t("davranışını normal yap", "DAVRANIS", "normal");
        t("gözlerini çarpı göz yap", "DAVRANIS", "xeyes");
        t("rengini mavi yap", "RENK", "mavi");
        t("müziği kapat", "MUZIK_DURDUR", "");
        t("saat kaç", "SAAT", "");
        t("mesaj yaz neredesin", "YAZ", "neredesin");
        if (com.guven.sahra.Davranis.LISTE.length != 22) hata++;
        t("oku", "EKRAN_OKU", "");
        t("Sahra oku", "EKRAN_OKU", "");
        t("bunu bana oku", "EKRAN_OKU", "");
        t("cevabı sesli oku", "EKRAN_OKU", "");
        t("Google'daki sonuçları okur musun", "EKRAN_OKU", "");
        t("okumayı durdur", "OKUMA_DUR", "");
        t("dur", "OKUMA_DUR", "");
        t("durdur", "MUZIK_DURDUR", "");
        t("mesaj yaz bunu oku", "YAZ", "bunu oku");
        String[][] uyanma = {{"Sahra", ""}, {"hey Sahra", ""}, {"Sahara.", ""},
                {"Sahra, WhatsApp aç", "WhatsApp aç"}, {"sahra Atatürk kim", "Atatürk kim"}};
        for (String[] u : uyanma) {
            if (!u[1].equals(com.guven.sahra.Uyandirma.komut(u[0]))) {
                hata++;
                System.out.println("HATA uyanma: " + u[0]);
            }
        }
        for (String yabanci : new String[]{"WhatsApp aç", "dün sahra geldi", "sahralar çok sıcak", "Zehra bak", "gönder", ""}) {
            if (com.guven.sahra.Uyandirma.komut(yabanci) != null) {
                hata++;
                System.out.println("HATA uyanmamalıydı: " + yabanci);
            }
        }
        t("hava durumu", "HAVA", "");
        t("hava nasıl", "HAVA", "");
        t("Chrome'da hava durumu ara", "WEB_ARA", "hava durumu");
        t("dolar kaç", "KUR", "");
        t("altın ne kadar", "KUR", "");
        t("döviz kurları", "KUR", "");
        t("güzel bir söz söyle", "GUZEL_SOZ", "");
        t("kar yağdır", "EFEKT", "kar");
        t("yağmur yağdır", "EFEKT", "yagmur");
        t("yaprak dök", "EFEKT", "yaprak");
        t("güneş aç", "EFEKT", "gunes");
        String xml = "<Currency Kod=\"USD\" CurrencyCode=\"USD\"><ForexBuying>41.1</ForexBuying><ForexSelling>41.25</ForexSelling></Currency>";
        if (com.guven.sahra.Veri.kur(xml, "USD") != 41.25 || com.guven.sahra.Veri.kur(xml, "EUR") != 0) hata++;
        if (Math.abs(com.guven.sahra.Veri.gramAltin(3110.34768, 40) - 4000) > 0.001) hata++;
        if (!com.guven.sahra.Veri.efekt(73, true, 0, 1).equals("kar")) hata++;
        if (!com.guven.sahra.Veri.efekt(63, true, 0, 10).equals("yagmur")) hata++;
        if (!com.guven.sahra.Veri.efekt(0, true, 5, 7).equals("gunes")) hata++;
        if (!com.guven.sahra.Veri.efekt(3, true, 5, 10).equals("yaprak")) hata++;
        if (!com.guven.sahra.Veri.efekt(3, false, 5, 1).equals("")) hata++;
        t("sor bana bir fıkra anlat", "YAPAY_SORU", "bana bir fıkra anlat");
        t("Sahra, Gemini yumurta nasıl haşlanır", "YAPAY_SORU", "yumurta nasıl haşlanır");
        if (!Komut.coz("Sahra, Atatürk kimdir").cumle.equals("Atatürk kimdir")) hata++;
        if (!Komut.coz("yumurta nasıl haşlanır").cumle.equals("yumurta nasıl haşlanır")) hata++;
        if (!com.guven.sahra.Veri.guncelSoru("maç ne zaman")) hata++;
        if (!com.guven.sahra.Veri.guncelSoru("bugün hangi gün tatil")) hata++;
        if (com.guven.sahra.Veri.guncelSoru("yumurta nasıl haşlanır")) hata++;
        if (com.guven.sahra.Veri.guncelSoru("su nasıl kaynar")) hata++;
        String duz = com.guven.sahra.Veri.duzMetin("## Başlık\n**Kalın** metin\n* madde");
        if (!duz.equals("Başlık\nKalın metin\n• madde")) {
            hata++;
            System.out.println("HATA duz: " + duz);
        }
        t("7'ye alarm kur", "ALARM", "420");
        t("yediye alarm kur", "ALARM", "420");
        t("saat 07:30 alarm kur", "ALARM", "450");
        t("sabah altı buçukta beni uyandır", "ALARM", "390");
        t("akşam sekize alarm kur", "ALARM", "1200");
        t("yirmi iki otuza alarm kur", "ALARM", "1350");
        t("saat 8'e çeyrek kala alarm kur", "ALARM", "465");
        t("10 dakika sonra alarm kur", "ALARM", "+600");
        t("alarm kur", "ALARM", "");
        t("10 dakika say", "ZAMANLAYICI", "600");
        t("1 saat 30 dakika zamanlayıcı kur", "ZAMANLAYICI", "5400");
        t("yarım saat zamanlayıcı kur", "ZAMANLAYICI", "1800");
        t("akşam 8'de ilacı hatırlat", "HATIRLAT", "akşam 8'de ilacı hatırlat");
        t("Annemi ara", "ARA", "Annemi");
        t("Sahra, Ahmet'i ara", "ARA", "Ahmet'i");
        t("Chrome'da hava durumu ara", "WEB_ARA", "hava durumu");
        t("YouTube'da yemek tarifi ara", "YOUTUBE", "yemek tarifi");
        t("feneri aç", "FENER", "ac");
        t("feneri kapat", "FENER", "kapat");
        t("kamerayı aç", "KAMERA", "");
        t("parlaklığı artır", "PARLAKLIK", "+");
        t("parlaklığı yüzde 50 yap", "PARLAKLIK", "50");
        t("Wi-Fi aç", "AYAR_AC", "wifi");
        t("wifi'yi aç", "AYAR_AC", "wifi");
        t("bluetooth'u kapat", "AYAR_AC", "bluetooth");
        t("eve yol tarifi", "YOL_TARIFI", "ev");
        t("eve git", "YOL_TARIFI", "ev");
        t("Kızılay için yol tarifi", "YOL_TARIFI", "Kızılay");
        t("geri git", "GERI", "");
        t("ana ekrana git", "ANA_EKRAN", "");
        t("günaydın", "OZET", "");
        t("bildirimleri oku", "BILDIRIM_OKU", "");
        t("bildirimleri aç", "BILDIRIMLER", "");
        t("mesajları oku", "EKRAN_OKU", "");
        t("not al süt alınacak", "NOT_AL", "süt alınacak");
        t("Not al: Ayşe'ye mektup yaz", "NOT_AL", "Ayşe'ye mektup yaz");
        t("ekmek alınacak diye not al", "NOT_AL", "ekmek alınacak");
        t("notlarımı oku", "NOT_OKU", "");
        t("notlarımı sil", "NOT_SIL", "");
        t("İngilizceye çevir nasılsın", "CEVIR", "ingilizce|nasılsın");
        t("bunu İngilizceye çevir", "CEVIR", "ingilizce|");
        t("günaydın'ı almancaya çevir", "CEVIR", "almanca|günaydın");
        t("araba modu", "ARABA", "ac");
        t("araba modunu kapat", "ARABA", "kapat");
        t("müziği kapat", "MUZIK_DURDUR", "");
        t("sesi kapat", "SESSIZ", "");
        t("WhatsApp aç", "UYGULAMA_AC", "WhatsApp");
        t("mesaj yaz yarın beni ara", "YAZ", "yarın beni ara");
        denetle("sure", com.guven.sahra.Zaman.sure("2 bucuk saat") == 9000);
        java.util.List<String> rehber = java.util.Arrays.asList("Annem", "Ahmet Yılmaz", "Ahmet Kaya", "Ali", "Anne Tarafı Dayı");
        denetle("annemi", com.guven.sahra.KisiBul.bul("Annemi", rehber).toString().equals("[0]"));
        denetle("ahmet", com.guven.sahra.KisiBul.bul("Ahmet'i", rehber).toString().equals("[1, 2]"));
        denetle("ali", com.guven.sahra.KisiBul.bul("Ali'yi", rehber).toString().equals("[3]"));
        denetle("yok", com.guven.sahra.KisiBul.bul("Zeynep'i", rehber).isEmpty());
        denetle("evet", com.guven.sahra.Onay.coz("evet gönder") == 1 && com.guven.sahra.Onay.coz("Tamam.") == 1);
        denetle("hayir", com.guven.sahra.Onay.coz("hayır gönderme") == -1);
        denetle("belirsiz", com.guven.sahra.Onay.coz("tamam mı değil mi") == 0 && com.guven.sahra.Onay.coz("annemi ara") == 0);
        denetle("sira", com.guven.sahra.Onay.sira("ikinci", rehber) == 1 && com.guven.sahra.Onay.sira("Ahmet Kaya", rehber) == 2);
        denetle("serbest", com.guven.sahra.Serbest.ayikla("KOMUT: sesi kıs").equals("sesi kıs"));
        denetle("serbest-yok", com.guven.sahra.Serbest.ayikla("YOK").isEmpty() && com.guven.sahra.Serbest.ayikla("Tabii!\nKOMUT: gönder").isEmpty());
        denetle("karar", com.guven.sahra.Serbest.karar(Komut.coz("gönder").tur) == com.guven.sahra.Serbest.Karar.ONAY_ISTE
                && com.guven.sahra.Serbest.karar(Komut.coz("Tamam butonuna tıkla").tur) == com.guven.sahra.Serbest.Karar.ONAY_ISTE
                && com.guven.sahra.Serbest.karar(Komut.coz("ekranı oku").tur) == com.guven.sahra.Serbest.Karar.REDDET
                && com.guven.sahra.Serbest.karar(Komut.coz("bildirimleri oku").tur) == com.guven.sahra.Serbest.Karar.REDDET
                && com.guven.sahra.Serbest.karar(Komut.coz("notlarımı oku").tur) == com.guven.sahra.Serbest.Karar.REDDET
                && com.guven.sahra.Serbest.karar(Komut.coz("sesi kıs").tur) == com.guven.sahra.Serbest.Karar.CALISTIR);
        denetle("banka", com.guven.sahra.Suzgec.okunmaz("com.ziraat.ziraatmobil", "Ziraat Mobil", "a", "b"));
        denetle("kod", com.guven.sahra.Suzgec.gizliIcerik("Google", "G-482913") && com.guven.sahra.Suzgec.gizliIcerik("X", "Giriş kodunuz 4821")
                && com.guven.sahra.Suzgec.gizliIcerik("SMS", "Doğrulama: 123456") && com.guven.sahra.Suzgec.gizliIcerik("a", "Your code is 123 456"));
        denetle("normal", !com.guven.sahra.Suzgec.okunmaz("com.whatsapp", "WhatsApp", "Ayşe", "Akşam 8'de geliyorum"));
        com.guven.sahra.Hafiza.ekle("a", "b", 1000);
        denetle("hafiza", com.guven.sahra.Hafiza.al(5, 2000).size() == 1 && com.guven.sahra.Hafiza.al(5, 400000).isEmpty());
        denetle("konu1", com.guven.sahra.Zaman.hatirlatKonusu("akşam 8'de ilacı hatırlat").equals("ilacı"));
        denetle("konu2", com.guven.sahra.Zaman.hatirlatKonusu("10 dakika sonra fırını kapatmayı hatırlat").equals("fırını kapatmayı"));
        denetle("konu3", com.guven.sahra.Zaman.hatirlatKonusu("yarın saat dokuzda toplantıyı bana hatırlatır mısın").equals("toplantıyı"));
        denetle("yakin1", com.guven.sahra.Zaman.yakinSaat("8 de ilac", 480, 600) == 1200);
        denetle("yakin2", com.guven.sahra.Zaman.yakinSaat("7 de", 420, 1320) == 420);
        denetle("yakin3", com.guven.sahra.Zaman.yakinSaat("sabah 8 de", 480, 600) == 480);
        byte[] wav = com.guven.sahra.Wav.hazirla(new byte[]{1, 2, 3, 4}, "audio/L16;codec=pcm;rate=24000");
        denetle("wav", wav.length == 48 && wav[0] == 'R' && com.guven.sahra.Wav.hazirla(wav, "audio/wav") == wav);
        System.out.println(hata == 0 ? "Tüm komut testleri geçti" : hata + " hata");
        if (hata > 0) System.exit(1);
    }
}
