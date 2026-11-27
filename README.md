# Sahra

Telefonda yüzen, Türkçe sesli ve yazılı komut alan robot asistan.

## Beyin ve bellek

Sahra son aramayı, çıkan sonuçları ve girdiği siteyi 10 dakika aklında tutar: "Chrome'da Trendyol ara" → "aç gir" → "sonraki siteye gir" → "ikinci siteye gir". "X sitesine gir" arayıp doğrudan girer; "trendyol nokta com'a gir" adresi açar. Adı alan adıyla birebir eşleşen siteler öğrenilir ve sonraki sefer aramadan açılır ("yanlış site" derseniz silinir; **Yetenekler → Beyin ve bellek** bölümünden kapatılıp temizlenebilir). Sonuca girmek için ekran kontrolü gerekir ve Chrome üzerinde çalışır. Bağlantılar telefonda işlenir; yapay zekâya ya da yedeğe gönderilmez.

## Kayıt formu doldurma

**Yetenekler → Kayıt bilgilerim** bölümüne ad, soyad, e-posta, kullanıcı adı, telefon ve şifrenizi kaydedin. Kayıt sayfasında "formu doldur" deyin: Sahra site adını söyleyip onay ister, tanıdığı boş kutuları (şifre ve tekrarı dahil) doldurur; kayıt düğmesine siz basarsınız. Kart, kimlik ve doğrulama kodu kutuları, şifre değiştirme formları, banka sayfaları ve kilitli ekran doldurulmaz. Bilgiler yalnızca telefonda durur, yedeğe ve yapay zekâya gitmez; şifre Android Anahtar Deposu anahtarıyla şifrelenir. Ekran kontrolü gerekir. Onaydaki site adını kontrol edin: Sahra sitenin sahte olup olmadığını anlayamaz.

## Ad ve hitap

"Ad ve hitap" bölümünden asistanın size nasıl sesleneceğini ve asistanın adını değiştirebilirsiniz. Ad tek sözcük ve 2-15 harf olmalıdır; komut sözcükleri kabul edilmez. Sesle uyandırma yalnızca yeni adı bekler; yazılı komutta "Sahra" da kabul edilir. Telefondaki uygulama simgesinin adı Sahra olarak kalır. Telefonda uyandırma modeli her adı aynı doğrulukta tanımayabilir.

## Güzel sözler

Ana ekrandaki "Boştayken gözlerde yazı" bölümünden kendi SQLite dosyanızı yükleyebilirsiniz (`kategoriler(ID, KatAdi)` ve `sozler(ID, KatId, Soz)` tabloları). Dosya depoya ya da APK'ya girmez, yalnızca telefonda saklanır.

- Cuma mesajları ile Anne ve Anneler Günü kategorileri alınmaz; tekrar eden sözler bir kez alınır.
- Her kategori için aç-kapat anahtarı vardır. "Karışık" açıkken sözler rastgele, kapalıyken sırayla gelir.
- Robot boştayken gözlerinde 90 karaktere kadar sözler kayar; sıklık seyrek (5 dk), normal (2 dk) ya da sık (30 sn).
- Komutlar: "güzel söz söyle", "rastgele söz", "komik söz söyle", "romantik söz" gibi. Adıyla istenen kategori kapalı olsa da okunur.
- Kutuya kendi sözlerinizi yazdıysanız onlar dosyanın önüne geçer.

## Kenar ışığı

Varsayılan **Orijinal ışık**tır (yeşil nefes, dinlerken gökkuşağı). **Robot → Kenar ışığı** bölümünden 10 renkten bir ya da birkaçını seçin. Yanma şekli (nefes, yanıp sönme, kalp atışı, dönen renkler, sabit), hız, en parlak ve en sönük hâl, yayılma, göz rengi ve dinlerken kullanılacak renkler ayarlanabilir. Işık sürekli hareket ettiği için sabit ışığa göre biraz daha fazla pil kullanabilir.

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
