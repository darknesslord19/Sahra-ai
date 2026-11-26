package com.guven.sahra;

/** Hangi bildirimlerin okunmayacağına karar verir. Kural dayalıdır; kuşkuda okumaz. */
public final class Suzgec {
    /** Banka ve ödeme uygulamaları, seçilmiş olsa bile okunmaz. */
    public static boolean bankaUygulamasi(String paket, String ad) {
        String s = Komut.sade((paket == null ? "" : paket) + " " + (ad == null ? "" : ad));
        return s.matches(".*(bank|banka|ziraat|garanti|akbank|isbank|iscep|yapikredi|ykb|vakif|halkbank|enpara|cepteteb|teb\\b|qnb|finans|kuveyt|albaraka|odeabank|fibabanka|ing\\.mobil|papara|ininal|tosla|paycell|wallet|cuzdan|paypal|revolut|wise|binance|btcturk|paribu|authenticator|dogrulayici|turkiye\\.gov|edevlet).*");
    }

    /** Doğrulama kodu, şifre ya da hesap hareketi içeren yazılar okunmaz. */
    public static boolean gizliIcerik(String baslik, String metin) {
        String s = Komut.sade((baslik == null ? "" : baslik) + " " + (metin == null ? "" : metin));
        if (s.matches("(?s).*(dogrulama|onay kod|guvenlik kod|sifre|parola|tek kullanimlik|\\botp\\b|\\bpin\\b|verification|password|passcode|one.time|security code|kimseyle paylasma|hesabiniz|\\biban\\b|bakiye|kredi kart|banka kart|\\bcvv\\b|havale|\\beft\\b).*")) return true;
        // 4-8 haneli sayı ile birlikte "kod" geçiyorsa
        boolean sayi = s.matches("(?s).*(?<![\\d.,:])\\d{3}[ -]?\\d{1,5}(?![\\d.,:]).*");
        if (sayi && s.matches("(?s).*(\\bkod|\\bcode|\\bonay|giris).*")) return true;
        // Yalnızca koddan oluşan kısa mesaj
        return metin != null && metin.trim().matches("(G-)?\\d{4,8}");
    }

    public static boolean okunmaz(String paket, String ad, String baslik, String metin) {
        return bankaUygulamasi(paket, ad) || gizliIcerik(baslik, metin);
    }
}
