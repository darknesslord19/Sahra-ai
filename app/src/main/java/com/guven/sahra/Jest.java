package com.guven.sahra;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** Robotu tutup çevirme, sallama ve art arda dokunmayı ayırt eder. Android'e bağlı değildir. */
public final class Jest {
    public static final String DON = "don";
    public static final String SALLA = "salla";

    // çevirme: bu süre içinde aynı yöne toplam bir buçuk tur
    private static final long DONME_SURESI = 4000;
    private static final double DONME_ACISI = 3 * Math.PI;
    // sallama: bu süre içinde beş kez geri dönüş
    private static final long SALLAMA_SURESI = 1800;
    private static final int SALLAMA_SAYISI = 5;
    private static final double GERI_DONUS = 2.4;
    private static final long DOKUNUS_ARASI = 900;

    private final double adim;
    private final Deque<double[]> donusler = new ArrayDeque<double[]>();
    private final Deque<Long> geriler = new ArrayDeque<Long>();
    private double x;
    private double y;
    private double yon;
    private boolean yonVar;
    private boolean dondu;
    private boolean sallandi;
    private int dokunus;
    private long sonDokunus;

    /** adim: yön hesabı için parmağın en az gitmesi gereken yol (piksel). */
    public Jest(double adim) {
        this.adim = adim;
    }

    /** Parmak robota değdiğinde. */
    public void basla(double ilkX, double ilkY) {
        x = ilkX;
        y = ilkY;
        yonVar = false;
        dondu = false;
        sallandi = false;
        donusler.clear();
        geriler.clear();
    }

    /** Sürüklerken; algılanan hareketi döndürür, yoksa null. Her hareket bir tutuşta bir kez bildirilir. */
    public String ekle(double yeniX, double yeniY, long an) {
        double dx = yeniX - x;
        double dy = yeniY - y;
        if (Math.hypot(dx, dy) < adim) return null;
        x = yeniX;
        y = yeniY;
        double a = Math.atan2(dy, dx);
        if (!yonVar) {
            yonVar = true;
            yon = a;
            return null;
        }
        double fark = a - yon;
        while (fark > Math.PI) fark -= 2 * Math.PI;
        while (fark < -Math.PI) fark += 2 * Math.PI;
        yon = a;
        if (Math.abs(fark) > GERI_DONUS) {
            geriler.add(an);
        } else {
            donusler.add(new double[]{an, fark});
        }
        while (!geriler.isEmpty() && an - geriler.peekFirst() > SALLAMA_SURESI) geriler.removeFirst();
        while (!donusler.isEmpty() && an - donusler.peekFirst()[0] > DONME_SURESI) donusler.removeFirst();
        double toplam = 0;
        for (Iterator<double[]> i = donusler.iterator(); i.hasNext(); ) toplam += i.next()[1];
        if (!dondu && Math.abs(toplam) >= DONME_ACISI) {
            dondu = true;
            return DON;
        }
        if (!sallandi && !dondu && geriler.size() >= SALLAMA_SAYISI) {
            sallandi = true;
            return SALLA;
        }
        return null;
    }

    /** Bu tutuşta çevirme ya da sallama oldu mu? */
    public boolean tepkiOldu() {
        return dondu || sallandi;
    }

    /** Kısa bir dokunuşu sayar; art arda kaçıncı olduğunu döndürür. */
    public int dokunus(long an) {
        dokunus = dokunus > 0 && an - sonDokunus <= DOKUNUS_ARASI ? dokunus + 1 : 1;
        sonDokunus = an;
        return dokunus;
    }
}
