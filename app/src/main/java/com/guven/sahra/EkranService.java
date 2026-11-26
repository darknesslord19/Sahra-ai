package com.guven.sahra;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
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

    void kaydir(boolean asagi) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        Path yol = new Path();
        yol.moveTo(m.widthPixels / 2f, m.heightPixels * (asagi ? 0.72f : 0.30f));
        yol.lineTo(m.widthPixels / 2f, m.heightPixels * (asagi ? 0.30f : 0.72f));
        dispatchGesture(new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(yol, 0, 300)).build(), null, null);
    }
}
