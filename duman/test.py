import http.server, re, struct, subprocess, threading, time, urllib.parse, xml.etree.ElementTree as ET
P = 'com.guven.sahra'
def adb(*a): return subprocess.run(['adb', *a], capture_output=True, timeout=180).stdout.decode('utf-8', 'replace')
def sh(c): return adb('shell', c)
def yaz(*a): print('SONUC', *a, flush=True)
GELEN = []
class S(http.server.SimpleHTTPRequestHandler):
    def __init__(s, *a, **k): super().__init__(*a, directory='duman/web', **k)
    def log_message(s, *a): pass
    def do_GET(s):
        if s.path.startswith('/log?'):
            GELEN.append(urllib.parse.unquote(s.path[5:])); s.send_response(204); s.end_headers()
        else: super().do_GET()
threading.Thread(target=http.server.ThreadingHTTPServer(('127.0.0.1', 8000), S).serve_forever, daemon=True).start()
def dugumler():
    for _ in range(3):
        sh('rm -f /sdcard/u.xml; uiautomator dump /sdcard/u.xml')
        try: return list(ET.fromstring(sh('cat /sdcard/u.xml')).iter('node'))
        except ET.ParseError: time.sleep(1.5)
    return []
def merkez(n):
    a = list(map(int, re.findall(r'\d+', n.get('bounds')))); return (a[0]+a[2])//2, (a[1]+a[3])//2
def bul(m):
    for n in dugumler():
        if m in (n.get('text'), n.get('content-desc'), n.get('hint')): return n
def kaydirip(m):
    for _ in range(14):
        n = bul(m)
        if n is not None and merkez(n)[1] < 2100: return n
        sh('input swipe 540 1800 540 1200 500'); time.sleep(1)
def dokun(m):
    n = kaydirip(m)
    if n is None: yaz('bulunamadı', m); return False
    x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(1.5); return True
yaz('kurulum', adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk').strip())
adb('reverse', 'tcp:8000', 'tcp:8000')
for i in ['RECORD_AUDIO', 'POST_NOTIFICATIONS']: sh(f'pm grant {P} android.permission.{i}')
sh(f'appops set {P} SYSTEM_ALERT_WINDOW allow')
sh("echo 'chrome --disable-fre --no-default-browser-check --no-first-run --disable-search-engine-choice-screen' > /data/local/tmp/chrome-command-line")
sh('am set-debug-app --persistent com.android.chrome')
sh('pm grant com.android.chrome android.permission.POST_NOTIFICATIONS')
yaz('chrome', re.findall(r'versionName=(\S+)', sh('dumpsys package com.android.chrome'))[:1])
sh(f'am start -n {P}/.MainActivity'); time.sleep(8)
# 1) Kayıt bilgilerini arayüzden kaydet
if dokun('Kayıt bilgilerim'):
    time.sleep(2)
    kutular = lambda: [d for d in dugumler() if d.get('class') == 'android.widget.EditText']
    degerler = ['Deneme', 'Kisi', 'deneme@example.com', 'denemekisi', '5550000000', 'Gizli-1234', 'Gizli-1234']
    for i, v in enumerate(degerler):
        # her alan, ipucu sırasına göre bulunur
        ipuclari = ['Ad', 'Soyad', 'E-posta (Gmail)', 'Kullanıcı adı (isteğe bağlı)', 'Telefon (isteğe bağlı)', 'Şifre', 'Şifre tekrarı']
        n = None
        for _ in range(6):
            n = next((d for d in kutular() if ipuclari[i] in (d.get('hint'), d.get('text'))), None)
            if n is not None and merkez(n)[1] < 2100: break
            sh('input swipe 540 1800 540 1400 500'); time.sleep(1)
        if n is None: yaz('kutu yok', ipuclari[i]); continue
        x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(1); sh(f"input text '{v}'"); time.sleep(0.5)
        sh('input keyevent 111'); time.sleep(1)
    if bul('Kayıt bilgilerim') is None and bul('Kaydet') is None: dokun('Kayıt bilgilerim')
    dokun('Kaydet'); time.sleep(2)
    yaz('uyarı', [d.get('text') for d in dugumler() if d.get('text') in ('Kaydedildi.', 'Şifre ile tekrarı aynı değil.')])
    dokun('Tamam')
xml = sh(f"run-as {P} cat shared_prefs/kayit.xml")
yaz('kayıt dosyası alanları', re.findall(r'name="(\w+)">', xml))
yaz('şifre düz metin olarak YOK', 'Gizli-1234' not in xml and 'name="sifre"' in xml)
yaz('şifre hiçbir dosyada düz değil', 'Gizli-1234' not in sh(f"run-as {P} sh -c 'cat shared_prefs/*.xml'"))
sh(f'am force-stop {P}')
# 2) Ekran kontrolü aç (bundan sonra uiautomator kullanılmaz: erişilebilirliği askıya alır)
sh(f'am start -n {P}/.MainActivity'); time.sleep(4)
sh(f'settings put secure enabled_accessibility_services {P}/{P}.EkranService'); sh('settings put secure accessibility_enabled 1'); time.sleep(5)
yaz('erişilebilirlik', 'EkranService' in sh('dumpsys accessibility'))
adb('logcat', '-c')
def komut(c, bekle=8):
    adb('logcat', '-c')
    sh(f"am start-foreground-service -n {P}/.SahraService -a komut --es komut \"{c}\""); time.sleep(bekle)
    yaz('>>>', c)
    for l in adb('logcat', '-d').splitlines():
        if 'SAHRADBG' in l: yaz('   ', l[l.index('SAHRADBG'):][:300])
def ac(adres, bekle=12):
    sh(f"am start -a android.intent.action.VIEW -d '{adres}' -p com.android.chrome"); time.sleep(bekle)
komut('merhaba', 5)
# 3) Form doldurma
ac('http://localhost:8000/kayit.html', 25)
GELEN.clear()
komut('formu doldur', 9)
yaz('onaydan önce sayfaya yazılan', GELEN)
komut('evet', 10)
son = {}
for g in GELEN:
    k, _, v = g.partition('='); son[k] = v
yaz('sayfadaki değerler', son)
komut('formu doldur', 7)
komut('hayır', 4)
# 4) Sahte arama sayfası: sonuca gir, sonraki, sıra
komut("google'da deneme ara", 3)
ac('http://localhost:8000/arama.html', 10)
komut('aç gir', 14)
komut('sonraki siteye gir', 12)
komut('birinci siteye gir', 12)
komut('en son nereye girdik', 5)
# 5) Gerçek Google
komut('chrome da wikipedia sitesine gir', 40)
komut('en son nereye girdik', 5)
komut("chrome da turkiye vikipedi ara", 15)
komut('aç gir', 25)
komut('sonraki siteye gir', 12)
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
for l in adb('logcat', '-d', '-b', 'crash').splitlines()[:25]: yaz('log', l[:250])
