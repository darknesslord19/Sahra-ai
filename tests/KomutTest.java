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
        denetle("isik1", com.guven.sahra.Isik.degistir("yesil", "yesil").equals("yesil"));
        denetle("isik2", com.guven.sahra.Isik.degistir("yesil,mor", "mavi").equals("yesil,mor,mavi")
                && com.guven.sahra.Isik.degistir("yesil,mor", "yesil").equals("mor"));
        denetle("isik3", String.join(",", com.guven.sahra.Isik.secili("\"x,)alert(1),mor,mor")).equals("mor")
                && com.guven.sahra.Isik.secili("").size() == 3);
        denetle("isik4", com.guven.sahra.Isik.saniye(0) == 5.0 && com.guven.sahra.Isik.saniye(500) == 0.8);
        denetle("isik5", com.guven.sahra.Isik.json("mor", "bozuk", 62, 10, 70, 45, true, false).equals(
                "{\"renkler\":[\"#8b5cf6\"],\"sekil\":\"orijinal\",\"sure\":2.4,\"ust\":0.70,\"alt\":0.70,\"yay\":0.45,\"goz\":true,\"dinle\":false}"));
        denetle("isik6", com.guven.sahra.Isik.sekil(null).equals("orijinal") && com.guven.sahra.Isik.sekil("kalp").equals("kalp")
                && com.guven.sahra.Isik.sekilAdi("orijinal").equals("Orijinal"));
        // Beyin: devam cümleleri
        String[] girCumleleri = {"aç gir", "aç", "gir", "siteye gir", "Sahra siteyi aç", "bu linke tıkla", "içine gir",
                "hadi siteye gir lütfen", "bunu aç", "siteye de gir", "buldun aç gir"};
        for (String c : girCumleleri) {
            if (c.startsWith("buldun")) continue;
            com.guven.sahra.Beyin.Istek i = com.guven.sahra.Beyin.coz(c, true);
            denetle("beyin gir " + c, i != null && i.tip == com.guven.sahra.Beyin.Tip.GIR && i.sira == 0);
        }
        String[][] sirali = {{"ilkine gir", "1"}, {"ilk siteyi aç", "1"}, {"ikinciye gir", "2"}, {"ikinci siteye gir", "2"},
                {"üçüncü sonuca tıkla", "3"}, {"2. siteyi aç", "2"}, {"birincisine tıkla", "1"}};
        for (String[] c : sirali) {
            com.guven.sahra.Beyin.Istek i = com.guven.sahra.Beyin.coz(c[0], false);
            denetle("beyin sira " + c[0], i != null && i.tip == com.guven.sahra.Beyin.Tip.GIR && i.sira == Integer.parseInt(c[1]));
        }
        for (String c : new String[]{"sonraki siteye gir", "diğerine bak", "başka siteye gir", "yanlış site", "bu site değil"}) {
            com.guven.sahra.Beyin.Istek i = com.guven.sahra.Beyin.coz(c, false);
            denetle("beyin sonraki " + c, i != null && i.tip == com.guven.sahra.Beyin.Tip.SIRADAKI);
        }
        // Var olan komutlar beyne kaçmamalı
        for (String c : new String[]{"geri git", "geri", "ana ekrana git", "ana sayfaya git", "sonraki şarkı", "sonraki", "feneri aç",
                "kamerayı aç", "whatsapp aç", "müzik aç", "sesi aç", "wifi aç", "eve git", "gönder", "yaz siteye gir",
                "google'da hava durumu ara", "youtube da tarkan aç", "gönder butonuna tıkla", "pencereyi kapat", "araba modu aç",
                "bildirimleri aç", "not al siteye gir", "saat kaç", "annemi ara"}) {
            denetle("beyin karismaz " + c, com.guven.sahra.Beyin.coz(c, true) == null);
        }
        denetle("beyin baglamsiz ac", com.guven.sahra.Beyin.coz("aç", false) == null && com.guven.sahra.Beyin.coz("gir", false) == null);
        com.guven.sahra.Beyin.Istek bi = com.guven.sahra.Beyin.coz("Chrome'da sahibinden sitesini bul", false);
        denetle("beyin site bul", bi != null && bi.tip == com.guven.sahra.Beyin.Tip.SITE && bi.ad.equals("sahibinden") && !bi.gir);
        bi = com.guven.sahra.Beyin.coz("sahibinden sitesine gir", false);
        denetle("beyin site gir", bi != null && bi.tip == com.guven.sahra.Beyin.Tip.SITE && bi.ad.equals("sahibinden") && bi.gir);
        bi = com.guven.sahra.Beyin.coz("google da e devlet bul ve gir", false);
        denetle("beyin bul gir", bi != null && bi.tip == com.guven.sahra.Beyin.Tip.SITE && bi.ad.equals("e devlet") && bi.gir);
        bi = com.guven.sahra.Beyin.coz("trendyol nokta com'a gir", false);
        denetle("beyin adres", bi != null && bi.tip == com.guven.sahra.Beyin.Tip.ADRES && bi.ad.equals("trendyol.com"));
        bi = com.guven.sahra.Beyin.coz("wikipedia olana gir", false);
        denetle("beyin adli", bi != null && bi.tip == com.guven.sahra.Beyin.Tip.ADLI_SONUC && bi.ad.equals("wikipedia"));
        java.util.List<String[]> ham = new java.util.ArrayList<String[]>();
        for (String[] b : new String[][]{{"https://www.google.com/search?q=a&tbm=isch", "Görseller"}, {"javascript:void(0)", "x"},
                {"https://reklam.example.com/a", "Sponsorlu Reklam"}, {"https://www.googleadservices.com/pagead/aclk?x=1", "y"},
                {"https://accounts.google.com.tr/login", "Oturum aç"}, {"https://tr.wikipedia.org/wiki/A", "A - Vikipedi"},
                {"https://tr.wikipedia.org/wiki/B", "B"}, {"https://www.sahibinden.com/", "sahibinden.com"},
                {"https://google.evil.com/", "kendi sitesi"}}) ham.add(b);
        java.util.List<String[]> temiz = com.guven.sahra.Beyin.sonuclar(ham);
        denetle("beyin suzme", temiz.size() == 3 && temiz.get(0)[0].endsWith("/wiki/A") && temiz.get(1)[0].contains("sahibinden")
                && temiz.get(2)[0].contains("evil"));
        denetle("beyin bul", com.guven.sahra.Beyin.bul(temiz, "sahibinden") == 1 && com.guven.sahra.Beyin.bul(temiz, "vikipedi") == 0
                && com.guven.sahra.Beyin.bul(temiz, "yokboyle") == -1);
        denetle("beyin guven", com.guven.sahra.Beyin.guvenliEslesme("sahibinden", "https://www.sahibinden.com/")
                && !com.guven.sahra.Beyin.guvenliEslesme("sahibinden", "https://sahibinden-giris.xyz/")
                && !com.guven.sahra.Beyin.guvenliEslesme("com", "https://a.com/"));
        denetle("beyin cubuk", com.guven.sahra.Beyin.aramaAdresi("google.com/search?q=x") && com.guven.sahra.Beyin.aramaAdresi("")
                && !com.guven.sahra.Beyin.aramaAdresi("sahibinden.com/ilan"));
        com.guven.sahra.Beyin.aramaYapildi("x", 1000);
        com.guven.sahra.Beyin.sonuclariSakla(temiz, 1000);
        denetle("beyin sure", com.guven.sahra.Beyin.saklanan(2000).size() == 3 && com.guven.sahra.Beyin.sonArama(700000).isEmpty()
                && com.guven.sahra.Beyin.saklanan(700000).isEmpty());
        // Kayıt formu
        String[][] formKomut = {{"formu doldur", ""}, {"kayıt formunu doldur", ""}, {"bilgilerimi gir", ""}, {"Sahra bu formu doldur", ""},
                {"otomatik doldur", ""}, {"mailimi yaz", "EPOSTA"}, {"e-postamı yaz", "EPOSTA"}, {"gmail adresimi gir", "EPOSTA"},
                {"adımı yaz", "AD"}, {"soyadımı yaz", "SOYAD"}, {"adımı soyadımı yaz", "ADSOYAD"}, {"kullanıcı adımı yaz", "KULLANICI"},
                {"telefon numaramı yaz", "TELEFON"}, {"şifremi yaz", "SIFRE"}};
        for (String[] c : formKomut) {
            com.guven.sahra.Komut f = com.guven.sahra.Komut.coz(c[0]);
            denetle("form komut " + c[0], f.tur == com.guven.sahra.Komut.Tur.FORM_DOLDUR && f.metin.equals(c[1])
                    && com.guven.sahra.Beyin.coz(c[0], true) == null);
        }
        denetle("form yazdirma kalir", com.guven.sahra.Komut.coz("yarın geliyorum yaz").tur == com.guven.sahra.Komut.Tur.YAZ
                && com.guven.sahra.Komut.coz("mesaj yaz şifremi unuttum").tur == com.guven.sahra.Komut.Tur.YAZ);
        denetle("form serbest red", com.guven.sahra.Serbest.karar(com.guven.sahra.Komut.Tur.FORM_DOLDUR) == com.guven.sahra.Serbest.Karar.REDDET);
        String[][] kutuTurleri = {{"E-posta adresi", "EPOSTA"}, {"user_email", "EPOSTA"}, {"Adınız", "AD"}, {"Ad", "AD"},
                {"firstName", "AD"}, {"Soyadınız", "SOYAD"}, {"last_name", "SOYAD"}, {"Ad Soyad", "ADSOYAD"},
                {"Adınız ve Soyadınız", "ADSOYAD"}, {"full-name", "ADSOYAD"}, {"Kullanıcı adı", "KULLANICI"}, {"username", "KULLANICI"},
                {"Cep telefonu", "TELEFON"}, {"Adres", "ATLA"}, {"TC Kimlik No", "ATLA"}, {"Kart üzerindeki isim", "ATLA"},
                {"Doğrulama kodu", "ATLA"}, {"Posta kodu", "ATLA"}, {"Ara", "ATLA"}, {"company name", "ATLA"}, {"", "ATLA"}};
        for (String[] c : kutuTurleri) {
            denetle("kutu " + c[0], com.guven.sahra.Form.tur(new com.guven.sahra.Form.Kutu(c[0], "", false, true)).name().equals(c[1]));
        }
        denetle("kutu onceki yazi", com.guven.sahra.Form.tur(new com.guven.sahra.Form.Kutu("input-7", "Soyadınız *", false, true)).name().equals("SOYAD"));
        java.util.Set<com.guven.sahra.Form.Alan> hepsiKayitli = java.util.EnumSet.allOf(com.guven.sahra.Form.Alan.class);
        java.util.List<com.guven.sahra.Form.Kutu> kayitFormu = java.util.Arrays.asList(
                new com.guven.sahra.Form.Kutu("ad", "", false, true), new com.guven.sahra.Form.Kutu("email", "", false, false),
                new com.guven.sahra.Form.Kutu("Şifre", "", true, true), new com.guven.sahra.Form.Kutu("Şifre tekrar", "", true, true),
                new com.guven.sahra.Form.Kutu("Şifre", "", false, true));
        java.util.List<com.guven.sahra.Form.Is> pl = com.guven.sahra.Form.plan(kayitFormu, hepsiKayitli);
        denetle("form plan", pl.size() == 3 && pl.get(0).sira == 0 && pl.get(1).sira == 2 && pl.get(2).sira == 3
                && com.guven.sahra.Form.ozet(pl).equals("ad, şifre, şifre tekrarı"));
        denetle("form sifre degistirme", com.guven.sahra.Form.plan(java.util.Arrays.asList(
                new com.guven.sahra.Form.Kutu("Mevcut şifre", "", true, true), new com.guven.sahra.Form.Kutu("Yeni şifre", "", true, true)),
                hepsiKayitli).isEmpty());
        denetle("form uc sifre", com.guven.sahra.Form.plan(java.util.Arrays.asList(new com.guven.sahra.Form.Kutu("p1", "", true, true),
                new com.guven.sahra.Form.Kutu("p2", "", true, true), new com.guven.sahra.Form.Kutu("p3", "", true, true)), hepsiKayitli).isEmpty());
        System.out.println(hata == 0 ? "Tüm komut testleri geçti" : hata + " hata");
        if (hata > 0) System.exit(1);
    }
}
