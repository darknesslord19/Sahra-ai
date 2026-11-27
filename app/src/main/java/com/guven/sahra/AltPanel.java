package com.guven.sahra;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Alttan kayarak açılan panel. Tutamacı aşağı çekerek, ✕ ile, dışarı dokunarak ya da geri tuşuyla kapanır. */
final class AltPanel extends Dialog {
    final LinearLayout icerik;
    final ScrollView kaydir;

    AltPanel(Activity a, String baslik) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final LinearLayout kok = new LinearLayout(a) {
            /** Panel ekranın en çok %88'ini kaplar; fazlası içeride kaydırılır. */
            @Override
            protected void onMeasure(int en, int boy) {
                int enCok = Math.round(getResources().getDisplayMetrics().heightPixels * 0.88f);
                int sinir = MeasureSpec.getMode(boy) == MeasureSpec.UNSPECIFIED ? enCok
                        : Math.min(enCok, MeasureSpec.getSize(boy));
                super.onMeasure(en, MeasureSpec.makeMeasureSpec(sinir, MeasureSpec.AT_MOST));
            }
        };
        kok.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable zemin = new GradientDrawable();
        zemin.setColor(0xFF181B24);
        float r = dp(26);
        zemin.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        zemin.setStroke(dp(1), 0xFF2B3040);
        kok.setBackground(zemin);

        LinearLayout ust = new LinearLayout(a);
        ust.setOrientation(LinearLayout.VERTICAL);
        ust.setPadding(dp(16), dp(10), dp(16), dp(8));
        View tutamac = new View(a);
        GradientDrawable cizgi = new GradientDrawable();
        cizgi.setColor(0xFF4A5166);
        cizgi.setCornerRadius(dp(3));
        tutamac.setBackground(cizgi);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(dp(44), dp(5));
        tl.gravity = Gravity.CENTER_HORIZONTAL;
        tl.bottomMargin = dp(8);
        ust.addView(tutamac, tl);

        LinearLayout satir = new LinearLayout(a);
        satir.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(a);
        t.setText(baslik);
        t.setTextSize(19);
        t.setTextColor(0xFFF2F4F8);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        satir.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        ImageView kapat = new ImageView(a);
        kapat.setImageResource(R.drawable.ik_carpi);
        kapat.setContentDescription("Kapat");
        kapat.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable daire = new GradientDrawable();
        daire.setShape(GradientDrawable.OVAL);
        daire.setColor(0xFF222632);
        kapat.setBackground(daire);
        kapat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        satir.addView(kapat, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ust.addView(satir);
        ust.setOnTouchListener(new View.OnTouchListener() {
            private float ilk;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                // Pencere sabit kaldığı için ekran koordinatı kullanılır
                float fark = Math.max(0, e.getRawY() - ilk);
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        ilk = e.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        kok.setTranslationY(fark);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (fark > dp(90)) dismiss();
                        else kok.animate().translationY(0).setDuration(150);
                        break;
                    default:
                        kok.animate().translationY(0).setDuration(150);
                }
                return true;
            }
        });
        kok.addView(ust, new LinearLayout.LayoutParams(-1, -2));

        icerik = new LinearLayout(a);
        icerik.setOrientation(LinearLayout.VERTICAL);
        icerik.setPadding(dp(16), 0, dp(16), dp(24));
        kaydir = new ScrollView(a);
        kaydir.setVerticalScrollBarEnabled(false);
        kaydir.addView(icerik);
        kok.addView(kaydir, new LinearLayout.LayoutParams(-1, -2));
        setContentView(kok);

        Window w = getWindow();
        w.setBackgroundDrawable(new ColorDrawable(0));
        w.getDecorView().setPadding(0, 0, 0, 0);
        w.setGravity(Gravity.BOTTOM);
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(0.65f);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        w.setWindowAnimations(R.style.PanelHareketi);
        setCanceledOnTouchOutside(true);
    }

    /** Görünümü başlığın altına, kaydırılan alanın dışına koyar. */
    void sabitle(View v, LinearLayout.LayoutParams l) {
        LinearLayout kok = (LinearLayout) kaydir.getParent();
        kok.addView(v, kok.indexOfChild(kaydir), l);
    }

    @Override
    public void show() {
        super.show();
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        Context c = getContext();
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
