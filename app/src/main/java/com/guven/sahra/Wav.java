package com.guven.sahra;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ses cevabını oynatılabilir WAV'a getirir. Android'e bağlı değildir. */
public final class Wav {
    /** Veri zaten WAV ise aynen döner; ham 16 bit tek kanal PCM ise başlık eklenir. */
    public static byte[] hazirla(byte[] veri, String tur) {
        if (veri.length > 12 && veri[0] == 'R' && veri[1] == 'I' && veri[2] == 'F' && veri[3] == 'F') return veri;
        Matcher m = Pattern.compile("rate=(\\d+)").matcher(tur == null ? "" : tur);
        int hiz = m.find() ? Integer.parseInt(m.group(1)) : 24000;
        ByteBuffer b = ByteBuffer.allocate(44 + veri.length).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R', 'I', 'F', 'F'}).putInt(36 + veri.length).put(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
        b.putInt(16).putShort((short) 1).putShort((short) 1).putInt(hiz).putInt(hiz * 2).putShort((short) 2).putShort((short) 16);
        b.put(new byte[]{'d', 'a', 't', 'a'}).putInt(veri.length).put(veri);
        return b.array();
    }
}
