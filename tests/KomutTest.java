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
        System.out.println(hata == 0 ? "Tüm komut testleri geçti" : hata + " hata");
        if (hata > 0) System.exit(1);
    }
}
