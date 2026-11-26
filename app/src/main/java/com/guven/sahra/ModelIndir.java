package com.guven.sahra;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** "Sahra" uyandırması için Türkçe ses modelini (Vosk, Apache 2.0) indirir. */
final class ModelIndir {
    static final String ADRES = "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip";
    /** İndirilen dosyanın beklenen özeti; derleme denetiminde ölçülen değer. */
    static final String OZET = "8c8d07cec1bce31add14967c16891c84152cdf76d391e33c08278119b9ea96e5";

    interface Ilerleme {
        void yuzde(int y);
    }

    private static File kok(Context c) {
        return new File(c.getFilesDir(), "uyandirma");
    }

    private static File isaret(Context c) {
        return new File(kok(c), "hazir");
    }

    static boolean hazir(Context c) {
        return isaret(c).exists() && klasor(c) != null;
    }

    /** Model dosyalarının bulunduğu klasör; yoksa null. */
    static File klasor(Context c) {
        File[] l = kok(c).listFiles();
        if (l != null) {
            for (File f : l) if (new File(f, "final.mdl").isFile()) return f;
        }
        return null;
    }

    static void sil(Context c) {
        sil(kok(c));
    }

    private static void sil(File f) {
        File[] l = f.listFiles();
        if (l != null) {
            for (File a : l) sil(a);
        }
        f.delete();
    }

    /** Ana iş parçacığında çağrılmaz. */
    static void indir(Context c, Ilerleme ilerleme) throws IOException {
        File zip = new File(c.getCacheDir(), "model.zip");
        try {
            String ozet = dosyayaIndir(zip, ilerleme);
            if (!OZET.equals(ozet)) throw new IOException("İndirilen dosya beklenenle aynı değil; kurulmadı.");
            sil(c);
            ac(zip, kok(c));
            if (klasor(c) == null || !isaret(c).createNewFile()) throw new IOException("Model açılamadı.");
        } finally {
            zip.delete();
        }
    }

    private static String dosyayaIndir(File hedef, Ilerleme ilerleme) throws IOException {
        HttpURLConnection b = Bilgi.baglan(ADRES);
        b.setReadTimeout(60000);
        try {
            if (b.getResponseCode() != 200) throw new IOException("Model indirilemedi (" + b.getResponseCode() + ").");
            long toplam = b.getContentLength();
            long alinan = 0;
            int son = -1;
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            InputStream in = b.getInputStream();
            OutputStream out = new FileOutputStream(hedef);
            try {
                byte[] parca = new byte[32768];
                int n;
                while ((n = in.read(parca)) > 0) {
                    out.write(parca, 0, n);
                    d.update(parca, 0, n);
                    alinan += n;
                    if (alinan > 100000000) throw new IOException("Dosya beklenenden büyük.");
                    int y = toplam > 0 ? (int) (alinan * 100 / toplam) : 0;
                    if (y != son) ilerleme.yuzde(son = y);
                }
            } finally {
                out.close();
                in.close();
            }
            StringBuilder s = new StringBuilder();
            for (byte x : d.digest()) s.append(String.format("%02x", x & 255));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            b.disconnect();
        }
    }

    private static void ac(File zip, File hedef) throws IOException {
        String sinir = hedef.getCanonicalPath() + File.separator;
        ZipInputStream z = new ZipInputStream(new FileInputStream(zip));
        try {
            byte[] parca = new byte[32768];
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                File f = new File(hedef, e.getName());
                // Klasör dışına yazmaya çalışan kayıtlar reddedilir
                if (!f.getCanonicalPath().startsWith(sinir)) throw new IOException("Geçersiz arşiv.");
                if (e.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                f.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(f);
                try {
                    int n;
                    while ((n = z.read(parca)) > 0) out.write(parca, 0, n);
                } finally {
                    out.close();
                }
            }
        } finally {
            z.close();
        }
    }
}
