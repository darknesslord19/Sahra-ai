package com.guven.sahra;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/** Erişilebilirlik hizmeti. Yalnızca komut geldiğinde ekrana bakar; hiçbir şeyi kaydetmez. */
public class EkranService extends AccessibilityService {
    static EkranService acik;

    @Override
    protected void onServiceConnected() {
        acik = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        acik = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    private List<AccessibilityNodeInfo> dugumler() {
        List<AccessibilityNodeInfo> hepsi = new ArrayList<AccessibilityNodeInfo>();
        topla(getRootInActiveWindow(), hepsi, 0);
        return hepsi;
    }

    private void topla(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> hepsi, int derinlik) {
        if (n == null || derinlik > 40 || hepsi.size() > 600) return;
        if (n.isVisibleToUser()) hepsi.add(n);
        for (int i = 0; i < n.getChildCount(); i++) topla(n.getChild(i), hepsi, derinlik + 1);
    }

    private static String etiket(AccessibilityNodeInfo n) {
        CharSequence t = n.getText();
        if (t == null || t.length() == 0) t = n.getContentDescription();
        return t == null ? "" : t.toString().trim();
    }

    /** Ekrandaki yazılar. Şifre alanları atlanır. */
    String oku() {
        StringBuilder s = new StringBuilder();
        for (AccessibilityNodeInfo n : dugumler()) {
            if (n.isPassword()) continue;
            String t = etiket(n);
            if (!t.isEmpty() && s.indexOf(t) < 0) s.append(t).append(". ");
            if (s.length() > 1500) break;
        }
        return s.toString().trim();
    }

    boolean yaz(String metin) {
        AccessibilityNodeInfo n = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (n == null || !n.isEditable() || n.isPassword()) return false;
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, metin);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    /** tamAd true ise yalnızca adı birebir aynı olan öğeye dokunur. */
    boolean tikla(String ad, boolean tamAd) {
        String aranan = Komut.sade(ad);
        List<AccessibilityNodeInfo> hepsi = dugumler();
        for (int tur = 0; tur < (tamAd ? 1 : 2); tur++) {
            for (AccessibilityNodeInfo n : hepsi) {
                String t = Komut.sade(etiket(n));
                boolean uyar = tur == 0 ? t.equals(aranan) : t.contains(aranan);
                if (!uyar || n.isEditable()) continue;
                for (AccessibilityNodeInfo p = n; p != null; p = p.getParent()) {
                    if (p.isClickable()) return p.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
        return false;
    }

    /** Öndeki sayfadaki bağlantılar, sayfa sırasıyla: {adres, görünen ad}. Adresi Chrome verir. */
    List<String[]> baglantilar() {
        List<String[]> l = new ArrayList<String[]>();
        baglantiTopla(getRootInActiveWindow(), l, 0, new int[]{0});
        return l;
    }

    private void baglantiTopla(AccessibilityNodeInfo n, List<String[]> l, int derinlik, int[] sayac) {
        // Sonuç sayfalarının ağacı derindir; ekran dışında kalan bağlantılar da alınır
        if (n == null || derinlik > 90 || sayac[0]++ > 4000) return;
        Bundle ek = n.getExtras();
        CharSequence adres = ek == null ? null : ek.getCharSequence("AccessibilityNodeInfo.targetUrl");
        if (adres != null && adres.length() > 0) {
            StringBuilder ad = new StringBuilder(etiket(n));
            altYazi(n, ad, 0);
            l.add(new String[]{adres.toString(), ad.toString().trim()});
        }
        for (int i = 0; i < n.getChildCount(); i++) baglantiTopla(n.getChild(i), l, derinlik + 1, sayac);
    }

    private void altYazi(AccessibilityNodeInfo n, StringBuilder s, int derinlik) {
        for (int i = 0; i < n.getChildCount() && derinlik < 6 && s.length() < 200; i++) {
            AccessibilityNodeInfo a = n.getChild(i);
            if (a == null || a.isPassword()) continue;
            String t = etiket(a);
            if (!t.isEmpty()) s.append(' ').append(t);
            altYazi(a, s, derinlik + 1);
        }
    }

    /** Chrome'un adres çubuğundaki yazı; görünmüyorsa boş. */
    String adresCubugu() {
        AccessibilityNodeInfo kok = getRootInActiveWindow();
        if (kok == null) return "";
        for (AccessibilityNodeInfo n : kok.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")) {
            return etiket(n);
        }
        return "";
    }

    /** Öndeki uygulamanın paket adı; bilinmiyorsa boş. */
    String ondeki() {
        AccessibilityNodeInfo kok = getRootInActiveWindow();
        return kok == null || kok.getPackageName() == null ? "" : kok.getPackageName().toString();
    }

    /** Ekranda görünen yazı kutuları. nodlar ve kutular aynı sıradadır; kutu içerikleri okunup saklanmaz. */
    void formKutulari(List<AccessibilityNodeInfo> nodlar, List<Form.Kutu> kutular) {
        kutuTopla(getRootInActiveWindow(), nodlar, kutular, new String[]{""}, 0, new int[]{0});
    }

    private void kutuTopla(AccessibilityNodeInfo n, List<AccessibilityNodeInfo> nodlar, List<Form.Kutu> kutular,
                           String[] onceki, int derinlik, int[] sayac) {
        if (n == null || derinlik > 90 || sayac[0]++ > 4000) return;
        if (n.isEditable()) {
            // Gizli kutular ve tarayıcının adres çubuğu doldurulmaz
            String kimlik = String.valueOf(n.getViewIdResourceName());
            if (n.isVisibleToUser() && !kimlik.startsWith("com.android.chrome:id/")) {
                boolean ipucuGorunuyor = Build.VERSION.SDK_INT >= 26 && n.isShowingHintText();
                CharSequence yazi = n.getText();
                String ipucu = (Build.VERSION.SDK_INT >= 26 ? bosluklu(n.getHintText()) : "")
                        + bosluklu(n.getContentDescription()) + bosluklu(n.getViewIdResourceName() == null ? null
                        : n.getViewIdResourceName().replaceFirst("^.*:id/", ""))
                        + bosluklu(n.getExtras().getCharSequence("AccessibilityNodeInfo.hint"))
                        + bosluklu(n.getLabeledBy() == null ? null : etiket(n.getLabeledBy()));
                nodlar.add(n);
                kutular.add(new Form.Kutu(ipucu, onceki[0], n.isPassword(),
                        ipucuGorunuyor || yazi == null || yazi.length() == 0));
            }
            onceki[0] = "";
            return;
        }
        String t = etiket(n);
        if (!t.isEmpty() && t.length() < 60 && n.isVisibleToUser()) onceki[0] = t;
        for (int i = 0; i < n.getChildCount(); i++) kutuTopla(n.getChild(i), nodlar, kutular, onceki, derinlik + 1, sayac);
    }

    private static String bosluklu(CharSequence s) {
        return s == null || s.length() == 0 ? "" : s + " ";
    }

    boolean doldur(AccessibilityNodeInfo n, String deger) {
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, deger);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    /** Odaktaki yazı kutusu; yoksa null. */
    AccessibilityNodeInfo odakKutusu() {
        AccessibilityNodeInfo n = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        return n != null && n.isEditable() ? n : null;
    }

    void kaydir(boolean asagi) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        Path yol = new Path();
        yol.moveTo(m.widthPixels / 2f, m.heightPixels * (asagi ? 0.72f : 0.30f));
        yol.lineTo(m.widthPixels / 2f, m.heightPixels * (asagi ? 0.30f : 0.72f));
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(yol, 0, 300)).build(), null, null);
    }
}
