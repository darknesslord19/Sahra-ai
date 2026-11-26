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
        System.out.println(hata == 0 ? "Tüm komut testleri geçti" : hata + " hata");
        if (hata > 0) System.exit(1);
    }
}
