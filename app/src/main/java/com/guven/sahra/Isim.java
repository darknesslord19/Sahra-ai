package com.guven.sahra;

/** Asistanın kullanıcının verdiği adı. Android'e bağlı değildir. */
public final class Isim {
    public static final String VARSAYILAN = "Sahra";
    // Komutların başında geçen sözcükler ad olursa komutlar bozulur
    private static final String AYRILMIS = "hey|ey|tamam|merhaba|selam|evet|hayir|yok|dur|olur|olmaz|yap|iptal|onayla|gonder|"
            + "ac|ara|kapat|oku|yaz|git|gir|sil|sor|cevir|kaydet|geri|ileri|asagi|yukari|saat|not|alarm|hava|dolar|euro|altin|"
            + "soz|google|youtube|chrome|mesaj|sessiz|ses|muzik|hatirlat|formu|lutfen|hadi|simdi|peki|yardim|gunaydin|"
            + "bana|beni|sonraki|onceki|birinci|ikinci";

    private static volatile String ham = VARSAYILAN;
    private static volatile String sade = "sahra";

    /** Tek sözcük, yalnızca harf, 2-15 karakter ve komut sözcüğü değilse düzenlenmiş adı, yoksa null döndürür. */
    public static String gecerli(String ad) {
        if (ad == null) return null;
        String a = ad.trim();
        if (!a.matches("\\p{L}{2,15}") || Komut.sade(a).matches(AYRILMIS)) return null;
        char ilk = a.charAt(0);
        return (ilk == 'i' ? 'İ' : Character.toUpperCase(ilk)) + a.substring(1);
    }

    /** Geçersiz ad verilirse varsayılana döner. */
    public static void ayarla(String ad) {
        if (ham.equals(ad)) return;
        String a = gecerli(ad);
        if (a == null) a = VARSAYILAN;
        sade = Komut.sade(a);
        ham = a;
    }

    public static String ad() {
        return ham;
    }

    /** Yazılı komutun başında kabul edilen adlar: yeni ad ve her zaman "sahra". */
    static String desen() {
        String s = sade;
        return s.equals("sahra") ? "sahra" : "(?:sahra|" + s + ")";
    }

    /** Sesle uyandıran sözcük: yalnızca seçili ad. */
    static String uyanDeseni() {
        String s = sade;
        // Tanıma hizmeti "Sahra"yı bazen "Sahara" diye yazar
        return s.equals("sahra") ? "(?:sahra|sahara)" : s;
    }

    static boolean uyandiran(String sozcuk) {
        return Komut.sade(sozcuk).matches(uyanDeseni());
    }
}
