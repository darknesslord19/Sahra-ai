# Sahra

Telefonda yüzen, Türkçe sesli ve yazılı komut alan robot asistan.

## APK
**Releases** bölümünden en son `Sahra.apk` dosyasını indirin.

APK, `imza/` klasöründeki Android **testkey** ile imzalanır; yeni sürümler eskisinin üstüne kurulur.

Dikkat: testkey herkese açık bir anahtardır, imza APK'nın kimden geldiğini kanıtlamaz. Sahra'yı yalnızca bu deponun Releases bölümünden kurun.

## İlk kurulum
1. Mikrofon ve "diğer uygulamaların üstünde göster" izinlerini verin.
2. Ekranı okuma ve yazma için erişilebilirlik iznini açın.
3. "Sahra'yı başlat" düğmesine basın, robota dokunup konuşun.

## Kendi komutların ve yedek
Uygulamadaki "Kendi komutlarım" bölümünden komut ekleyin. Komutlar ve ayarlar İndirilenler/Sahra/sahra-yedek.json dosyasına yedeklenir (Android 10+).

## Yetenekler (0.2)
Ayar ekranında her satır alttan açılan bir panel getirir.

- Alarm, zamanlayıcı, hatırlatma; onaylı arama
- Fener, kamera, parlaklık, yol tarifi, ayar ekranları
- Arka planda kalma yardımı
- Sabah özeti, seçili uygulamaların bildirimleri, sesli not, çeviri, araba modu
- İsteğe bağlı: serbest cümle anlama, sohbet hafızası, çevrimiçi ses (Gemini)
- Deneysel: "Sahra" adını telefonda tanıma. Vosk kitaplığı ve vosk-model-small-tr-0.3 modeli (Apache 2.0); model uygulama içinden indirilir.

İkonlar `araclar/ikonlar.py` ile üretilir.

## Gizlilik
- Gemini'ye yalnızca ilgili metin gider. Anahtarı koda, depoya ya da APK'ya koymayın; yedeğe de girmez.
- Rehber, bildirim, not ve ekran yazıları Gemini'ye gönderilmez. Metni siz "çevir" ya da "sor" diyerek verirseniz o metin gider.
- Arama onaysız yapılmaz. Banka uygulamaları ve kod içeren bildirimler kural dayalı süzgeçle atlanır; süzgeç her biçimi garanti edemez.

## Durum
Komutlar telefonda hazır kalıplarla anlaşılır. İnternetsiz ses tanıma, telefonun Türkçe çevrimdışı dil desteğine bağlıdır.
