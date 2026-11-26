package com.guven.sahra;

/** Robotun göz davranışları. Kimlikler robot.html içindeki adlarla aynıdır. */
public final class Davranis {
    /** İlk iki satır kip, kalan 20 satır davranıştır: {kimlik, görünen ad}. */
    public static final String[][] LISTE = {
            {"rastgele", "Rastgele"}, {"normal", "Normal"},
            {"heart", "Kalp"}, {"star", "Yıldız"}, {"happy", "Mutlu"}, {"laugh", "Kahkaha"},
            {"wink", "Göz kırpma"}, {"wow", "Şaşkın"}, {"angry", "Kızgın"}, {"sad", "Üzgün"},
            {"sleepy", "Uykulu"}, {"dizzy", "Sersem"}, {"scan", "Tarama"}, {"side", "Yan bakış"},
            {"roll", "Göz devirme"}, {"cool", "Havalı"}, {"music", "Müzik"}, {"think", "Düşünen"},
            {"suspect", "Şüpheli"}, {"pixel", "Piksel"}, {"xeyes", "Çarpı göz"}, {"excited", "Heyecanlı"}};

    public static String ad(String kimlik) {
        for (String[] d : LISTE) {
            if (d[0].equals(kimlik)) return d[1];
        }
        return LISTE[0][1];
    }
}
