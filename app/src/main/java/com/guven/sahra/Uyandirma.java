package com.guven.sahra;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sürekli dinlemede "Sahra" adını ayırt eder. */
public final class Uyandirma {
    // Tanıma hizmeti adı bazen "Sahara" diye yazar
    private static final Pattern AD = Pattern.compile(
            "^\\s*(?:(?:hey|ey|tamam|merhaba|selam) )?(?:sahra|sahara)(?:\\b[ ,.!?]*)");

    /** Ad yoksa null; yalnızca ad söylendiyse boş metin; yoksa addan sonraki komut. */
    public static String komut(String cumle) {
        if (cumle == null) return null;
        // sade() karakter sayısını değiştirmediği için aynı konumdan kesilebilir
        Matcher m = AD.matcher(Komut.sade(cumle));
        return m.find() ? cumle.substring(m.end()).trim() : null;
    }

    /** Tanıma seçeneklerinden adı içeren ilkine bakar. */
    public static String komut(List<String> secenekler) {
        for (String s : secenekler) {
            String k = komut(s);
            if (k != null) return k;
        }
        return null;
    }
}
