package com.guven.sahra;

/** Sahra 1'in ayar listeleri. Kimlikler robot1.html içindeki adlarla aynıdır. Android'e bağlı değildir. */
public final class Sahra1 {
    /** {kimlik, görünen ad}; "oto" ara sıra kendiliğinden el hareketi yapar. */
    public static final String[][] POZLAR = {
            {"oto", "Otomatik"},
            {"dinlen", "Dinlenme"},
            {"selam", "Sağ el selam"},
            {"selamsol", "Sol el selam"},
            {"ikiselam", "İki el selam"},
            {"sevinc", "Eller havaya"},
            {"sagagoster", "Sağı göster"},
            {"solagoster", "Solu göster"},
            {"yukari", "Yukarıyı göster"},
            {"beni", "Seni göster"},
            {"begen", "Beğendim"},
            {"cifte", "Çifte beğeni"},
            {"zafer", "Zafer işareti"},
            {"alkis", "Alkış"},
            {"kucak", "Kucak açma"},
            {"dusun", "Düşünme"},
            {"bel", "Eller belde"},
            {"bilmem", "Bilmem ki"},
            {"asker", "Asker selamı"},
            {"kulak", "Seni dinliyorum"},
            {"dur", "Dur"},
            {"kalp", "Kalp yapma"},
            {"sakla", "Utanıp saklanma"},
            {"yumruk", "Güç gösterisi"},
            {"boks", "Boks"},
            {"davul", "Davul çalma"},
            {"yaz", "Yazı yazma"},
            {"dans", "Disko"},
            {"dalga", "Dalga dansı"},
            {"kos", "Koşu kolları"},
            {"kanat", "Kanat çırpma"},
    };

    public static final String[][] HAREKETLER = {
            {"suzul", "Süzülme"},
            {"sabit", "Sabit"},
            {"gezinti", "Sağa-sola yürüyüş"},
            {"bakin", "Sağa-sola bakınma"},
            {"evet", "Evet (baş sallama)"},
            {"hayir", "Hayır"},
            {"merak", "Merakla başını eğme"},
            {"reverans", "Reverans"},
            {"zipla", "Zıplama"},
            {"dansg", "Dans"},
            {"salin", "Salınma"},
            {"topac", "Topaç dönüşü"},
            {"takla", "Takla"},
            {"yuvarlan", "Yana yuvarlanma"},
            {"ucus", "Uçuş turu"},
            {"titre", "Titreme"},
            {"uyukla", "Uyuklama"},
    };

    /** Hazır karışımlar: {ad, el-kol, göz, hareket, açı}. */
    public static final String[][] HAZIR = {
            {"Merhaba Güven hocam", "selam", "happy", "suzul", "0"},
            {"Emriniz başım üstüne", "asker", "normal", "reverans", "0"},
            {"Seni dinliyorum", "kulak", "wow", "merak", "-25"},
            {"Düşünüyorum", "dusun", "think", "merak", "25"},
            {"Görev tamam", "cifte", "star", "zipla", "0"},
            {"Tebrikler", "alkis", "laugh", "salin", "0"},
            {"Seni seviyorum", "kalp", "heart", "salin", "0"},
            {"Parti zamanı", "dans", "music", "dansg", "0"},
            {"Disko turu", "dalga", "cool", "topac", "0"},
            {"Bilmiyorum", "bilmem", "suspect", "merak", "0"},
            {"Olmaz", "dur", "angry", "hayir", "0"},
            {"Tabii ki", "begen", "wink", "evet", "0"},
            {"Şuraya bak", "sagagoster", "excited", "bakin", "20"},
            {"Utandım", "sakla", "happy", "salin", "0"},
            {"İyi geceler", "dinlen", "sleepy", "uyukla", "-20"},
            {"Üzgünüm", "dinlen", "sad", "uyukla", "0"},
            {"Uçuşa geçtim", "kanat", "excited", "ucus", "0"},
            {"Yürüyüşteyim", "kos", "normal", "gezinti", "0"},
            {"Mesaj yazıyorum", "yaz", "scan", "suzul", "20"},
            {"Başım döndü", "kanat", "dizzy", "yuvarlan", "0"},
    };

    public static final String[][] AGIZLAR = {{"oto", "İfadeye göre"}, {"yok", "Ağız yok"}, {"gulus", "Gülümseme"}, {"konus", "Konuşma"}};
    public static final String[][] DONUSLER = {{"sabit", "Seçili açıda dur"}, {"oto", "Sağa-sola dön"}, {"tur", "Tam tur 360°"}};
    public static final String[][] YONLER = {{"0", "Ön"}, {"-45", "Sola çapraz"}, {"45", "Sağa çapraz"}, {"-90", "Sol yan"},
            {"90", "Sağ yan"}, {"-135", "Sol arka"}, {"135", "Sağ arka"}, {"180", "Arka"}};

    public static String ad(String[][] liste, String kimlik) {
        for (String[] s : liste) {
            if (s[0].equals(kimlik)) return s[1];
        }
        return liste[0][1];
    }

    private static String sec(String[][] liste, String kimlik, String yoksa) {
        for (String[] s : liste) {
            if (s[0].equals(kimlik)) return kimlik;
        }
        return yoksa;
    }

    /** robotAyar() için ayar metni; yalnızca listelerdeki değerler ve sınırlanmış sayılar yazılır. */
    public static String json(String poz, String hareket, int yon, String donus, String agiz, boolean eller, int hiz) {
        return "{\"poz\":\"" + sec(POZLAR, poz, "oto") + "\",\"hareket\":\"" + sec(HAREKETLER, hareket, "suzul")
                + "\",\"yon\":" + Math.max(-180, Math.min(180, yon)) + ",\"donus\":\"" + sec(DONUSLER, donus, "sabit")
                + "\",\"agiz\":\"" + sec(AGIZLAR, agiz, "oto") + "\",\"eller\":" + eller
                + ",\"hiz\":" + Math.max(40, Math.min(180, hiz)) + "}";
    }
}
