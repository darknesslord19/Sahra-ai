package com.guven.sahra;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import java.util.EnumSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Formlara yazılacak kayıt bilgileri. Yalnızca bu telefonda durur; yedeğe, yapay zekâya ve günlüğe gitmez.
 * Şifre, Android Anahtar Deposu'ndaki anahtarla şifrelenerek saklanır.
 */
final class KayitBilgi {
    private static final String ANAHTAR = "sahra_kayit";

    private static SharedPreferences al(Context c) {
        return c.getSharedPreferences("kayit", Context.MODE_PRIVATE);
    }

    static String oku(Context c, String ad) {
        return al(c).getString(ad, "");
    }

    static void yaz(Context c, String ad, String deger) {
        al(c).edit().putString(ad, deger.trim()).apply();
    }

    static boolean sifreVar(Context c) {
        return al(c).contains("sifre");
    }

    static void sil(Context c) {
        al(c).edit().clear().apply();
        try {
            KeyStore d = depo();
            if (d.containsAlias(ANAHTAR)) d.deleteEntry(ANAHTAR);
        } catch (Exception e) {
            // Kayıtlar silindi; kalan anahtar tek başına bir şey açmaz
        }
    }

    private static KeyStore depo() throws Exception {
        KeyStore d = KeyStore.getInstance("AndroidKeyStore");
        d.load(null);
        return d;
    }

    private static SecretKey anahtar() throws Exception {
        KeyStore d = depo();
        if (!d.containsAlias(ANAHTAR)) {
            KeyGenerator u = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            u.init(new KeyGenParameterSpec.Builder(ANAHTAR,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build());
            return u.generateKey();
        }
        return (SecretKey) d.getKey(ANAHTAR, null);
    }

    /** Şifrelenemezse hiçbir şey saklanmaz ve false döner. */
    static boolean sifreSakla(Context c, String sifre) {
        try {
            Cipher s = Cipher.getInstance("AES/GCM/NoPadding");
            s.init(Cipher.ENCRYPT_MODE, anahtar());
            byte[] gizli = s.doFinal(sifre.getBytes("UTF-8"));
            al(c).edit().putString("sifre", Base64.encodeToString(s.getIV(), Base64.NO_WRAP) + ":"
                    + Base64.encodeToString(gizli, Base64.NO_WRAP)).apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sifre(Context c) {
        try {
            String[] p = oku(c, "sifre").split(":");
            if (p.length != 2) return "";
            Cipher s = Cipher.getInstance("AES/GCM/NoPadding");
            s.init(Cipher.DECRYPT_MODE, anahtar(), new GCMParameterSpec(128, Base64.decode(p[0], Base64.NO_WRAP)));
            return new String(s.doFinal(Base64.decode(p[1], Base64.NO_WRAP)), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    static String deger(Context c, Form.Alan a) {
        switch (a) {
            case EPOSTA: return oku(c, "eposta");
            case AD: return oku(c, "ad");
            case SOYAD: return oku(c, "soyad");
            case ADSOYAD: return (oku(c, "ad") + " " + oku(c, "soyad")).trim();
            case KULLANICI: return oku(c, "kullanici");
            case TELEFON: return oku(c, "telefon");
            case SIFRE: return sifre(c);
            default: return "";
        }
    }

    static Set<Form.Alan> kayitli(Context c) {
        Set<Form.Alan> s = EnumSet.noneOf(Form.Alan.class);
        for (Form.Alan a : Form.Alan.values()) {
            if (a == Form.Alan.SIFRE ? sifreVar(c) : !deger(c, a).isEmpty()) s.add(a);
        }
        return s;
    }
}
