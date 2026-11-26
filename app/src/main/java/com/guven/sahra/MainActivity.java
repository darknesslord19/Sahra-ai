package com.guven.sahra;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout kok;

    @Override
    protected void onResume() {
        super.onResume();
        ciz();
    }

    @Override
    public void onRequestPermissionsResult(int kod, String[] izinler, int[] sonuc) {
        ciz();
    }

    private void ciz() {
        kok = new LinearLayout(this);
        kok.setOrientation(LinearLayout.VERTICAL);
        int p = Math.round(20 * getResources().getDisplayMetrics().density);
        kok.setPadding(p, p, p, p);
        ScrollView kaydir = new ScrollView(this);
        kaydir.setFitsSystemWindows(true);
        kaydir.addView(kok);
        setContentView(kaydir);

        baslik("Sahra");
        yazi("Merhaba " + Ayar.hitap(this) + ", emirlerinizi yerine getiriyorum.");

        baslik("1. İzinler");
        final boolean mikrofon = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        final boolean ustte = Settings.canDrawOverlays(this);
        dugme((mikrofon ? "✓ " : "") + "Mikrofon ve bildirim izni", new Runnable() {
            @Override
            public void run() {
                requestPermissions(Build.VERSION.SDK_INT >= 33
                        ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}
                        : new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        });
        dugme((ustte ? "✓ " : "") + "Diğer uygulamaların üstünde göster", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        });
        dugme((EkranService.acik != null ? "✓ " : "") + "Ekran kontrolü (erişilebilirlik)", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        yazi("Erişilebilirlik gri görünürse: Uygulama bilgisi → sağ üst ⋮ → Kısıtlanmış ayarlara izin ver.");
        dugme("Uygulama bilgisi", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            }
        });
        dugme("Zili sessize alma izni (isteğe bağlı)", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        });

        baslik("2. Sahra");
        dugme("▶ Sahra'yı başlat", new Runnable() {
            @Override
            public void run() {
                if (!mikrofon || !ustte) {
                    uyar("Önce mikrofon ve üstte gösterme izinlerini verin.");
                    return;
                }
                servis(new Intent(MainActivity.this, SahraService.class));
            }
        });
        dugme("■ Sahra'yı kapat", new Runnable() {
            @Override
            public void run() {
                stopService(new Intent(MainActivity.this, SahraService.class));
            }
        });
        final EditText komut = kutu("Yazılı komut (örnek: YouTube'dan film aç)", "");
        dugme("Komutu gönder", new Runnable() {
            @Override
            public void run() {
                if (!mikrofon || !ustte) {
                    uyar("Önce mikrofon ve üstte gösterme izinlerini verin.");
                    return;
                }
                servis(new Intent(MainActivity.this, SahraService.class).putExtra(SahraService.KOMUT, komut.getText().toString()));
            }
        });

        Switch surekli = new Switch(this);
        surekli.setText("Sürekli dinleme (\"Sahra\" diye başlayın)");
        surekli.setChecked(Ayar.al(this).getBoolean("surekli", false));
        surekli.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean acik) {
                Ayar.al(MainActivity.this).edit().putBoolean("surekli", acik).apply();
                yenile();
            }
        });
        kok.addView(surekli);
        yazi("Sürekli dinleme pili daha çok harcar ve bazı telefonlarda bip sesi çıkarır. Duyulan ses, telefonun ses tanıma hizmetine gider.");

        baslik("3. Ayarlar");
        yazi("Size nasıl hitap etsin?");
        EditText hitap = kutu("Güven hocam", Ayar.hitap(this));
        hitap.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                String v = s.toString().trim();
                Ayar.al(MainActivity.this).edit().putString("hitap", v.isEmpty() ? "Güven hocam" : v).apply();
            }
        });

        final String paket = Ayar.muzikPaketi(this);
        String muzikAdi = "seçilmedi";
        final List<Uygulamalar.Uygulama> hepsi = Uygulamalar.liste(this);
        for (Uygulamalar.Uygulama u : hepsi) {
            if (u.paket.equals(paket)) muzikAdi = u.ad;
        }
        dugme("Müzik uygulaması: " + muzikAdi, new Runnable() {
            @Override
            public void run() {
                String[] adlar = new String[hepsi.size()];
                for (int i = 0; i < adlar.length; i++) adlar[i] = hepsi.get(i).ad;
                new AlertDialog.Builder(MainActivity.this).setTitle("Müzik uygulaması")
                        .setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, adlar),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int i) {
                                        Ayar.al(MainActivity.this).edit().putString("muzik", hepsi.get(i).paket).apply();
                                        ciz();
                                    }
                                }).show();
            }
        });
        dugme("Robot rengi: " + Ayar.renk(this), new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this).setTitle("Robot rengi")
                        .setItems(Komut.RENKLER, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int i) {
                                Ayar.al(MainActivity.this).edit().putString("renk", Komut.RENKLER[i]).apply();
                                yenile();
                                ciz();
                            }
                        }).show();
            }
        });
        yazi("Robot boyutu");
        SeekBar boyut = new SeekBar(this);
        boyut.setMax(184 - 46);
        boyut.setProgress(Ayar.boyut(this) - 46);
        boyut.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean kullanici) { }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override
            public void onStopTrackingTouch(SeekBar s) {
                Ayar.al(MainActivity.this).edit().putInt("boyut", s.getProgress() + 46).apply();
                yenile();
            }
        });
        kok.addView(boyut);

        baslik("Komutlar");
        yazi("Robota dokunun ve söyleyin. Basılı tutunca bu ekran açılır.\n\n"
                + "• merhaba, saat kaç, tarih ne\n• WhatsApp aç\n• müzik aç, müziği durdur, sonraki şarkı\n"
                + "• Tarkan Kuzu Kuzu çal\n• YouTube'dan aksiyon filmi aç\n• Chrome'da hava durumu ara\n"
                + "• sessize al, sesi aç, sesi kıs, sesi artır\n• mesaj yaz akşam geliyorum, gönder\n"
                + "• ekranı oku, Tamam butonuna tıkla\n• geri, ana ekran, bildirimler, aşağı kaydır\n"
                + "• büyü, küçül, rengini mavi yap\n• robotu kapat");
    }

    private void servis(Intent i) {
        startForegroundService(i);
    }

    private void yenile() {
        if (SahraService.calisiyor) servis(new Intent(this, SahraService.class).setAction(SahraService.YENILE));
    }

    private void uyar(String s) {
        new AlertDialog.Builder(this).setMessage(s).setPositiveButton("Tamam", null).show();
    }

    private void baslik(String s) {
        TextView t = yazi(s);
        t.setTextSize(20);
        t.setPadding(0, 32, 0, 8);
    }

    private TextView yazi(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, 8, 0, 8);
        kok.addView(t);
        return t;
    }

    private EditText kutu(String ipucu, String deger) {
        EditText e = new EditText(this);
        e.setHint(ipucu);
        e.setText(deger);
        e.setSingleLine(true);
        kok.addView(e);
        return e;
    }

    private void dugme(String s, final Runnable is) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is.run();
            }
        });
        kok.addView(b);
    }
}
