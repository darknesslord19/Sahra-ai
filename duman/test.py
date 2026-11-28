import math, re, subprocess, time, xml.etree.ElementTree as ET
P = 'com.guven.sahra'
def adb(*a): return subprocess.run(['adb', *a], capture_output=True, timeout=180).stdout.decode('utf-8', 'replace')
def sh(c): return adb('shell', c)
def yaz(*a): print('SONUC', *a, flush=True)
def dugumler():
    for _ in range(3):
        sh('rm -f /sdcard/u.xml; uiautomator dump /sdcard/u.xml')
        try: return list(ET.fromstring(sh('cat /sdcard/u.xml')).iter('node'))
        except ET.ParseError: time.sleep(1.5)
    return []
def merkez(n):
    a = list(map(int, re.findall(r'\d+', n.get('bounds')))); return (a[0]+a[2])//2, (a[1]+a[3])//2
def bul(m, d=None):
    for n in (d or dugumler()):
        if m in (n.get('text'), n.get('content-desc'), n.get('hint')): return n
def kaydirip(m, yon=1):
    for _ in range(30):
        n = bul(m)
        if n is not None and merkez(n)[1] < 2150: return n
        sh('input swipe 540 1800 540 1300 600' if yon > 0 else 'input swipe 540 1300 540 1800 600'); time.sleep(1)
def dokun(m, yon=1):
    n = kaydirip(m, yon)
    if n is None:
        yaz('bulunamadı', m, '| ekranda:', [x.get('text') for x in dugumler() if x.get('text')][:30]); return False
    x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(2); return True
def metinler(): return [x.get('text') for x in dugumler() if x.get('text')]
yaz('kurulum', adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk').strip())
for i in ['RECORD_AUDIO', 'POST_NOTIFICATIONS']: sh(f'pm grant {P} android.permission.{i}')
sh(f'appops set {P} SYSTEM_ALERT_WINDOW allow')
def komut(c, bekle=6):
    sh(f"am start-foreground-service -n {P}/.SahraService -a komut --es komut \"{c}\""); time.sleep(bekle)
def kayit(baslik):
    yaz('---', baslik)
    for l in adb('logcat', '-d').splitlines():
        if 'SAHRADBG' in l: yaz('   ', l[l.index('SAHRADBG') + 8:][:1500])
        elif 'chromium' in l and 'CONSOLE' in l and 'robot' in l: yaz('   KONSOL', l[-220:])
    adb('logcat', '-c')
def pencere():
    d = sh('dumpsys window windows')
    for blok in re.split(r'\n  Window #', d):
        if P in blok and 'APPLICATION_OVERLAY' in blok:
            m = re.search(r'\bframe=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]', blok)
            if m:
                a = list(map(int, m.groups()))
                if abs((a[2]-a[0]) - (a[3]-a[1])) <= 2 and a[2]-a[0] < 700: return a
    yaz('robot penceresi bulunamadı')

import http.server, threading
ADLAR = ['Mavi Deniz', 'Dağ Yolu', 'Yıldız Gecesi', 'Sessiz Orman', 'Kırmızı Balon', 'Eski Liman', 'Gümüş Köprü', 'Uzak Ada']
istekler = []
class S(http.server.BaseHTTPRequestHandler):
    def log_message(self, *a): pass
    def ver(self, tip, v):
        self.send_response(200); self.send_header('Content-Type', tip); self.send_header('Cache-Control', 'no-store'); self.end_headers(); self.wfile.write(v.encode())
    def do_GET(self):
        p = self.path
        if p.startswith('/afis'):
            return self.ver('image/svg+xml', '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="300"><rect width="200" height="300" fill="#%d68"/></svg>' % (2 + int(p[-1])))
        if p != '/favicon.ico': istekler.append(p)
        bas = '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0;font-family:sans-serif}.g{display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:10px}.g a{display:block}.g img{width:100%;aspect-ratio:2/3;display:block}</style>'
        if p.startswith('/kategori'):
            return self.ver('text/html', bas + ('<h2>Korku filmleri</h2>' if 'korku' in p else '<h2>Kategoriler</h2><p><a style="display:block;font-size:24px;padding:20px" href="/kategori-komedi">Komedi</a><a style="display:block;font-size:24px;padding:20px" href="/kategori-korku">Korku</a></p>'))
        if p.startswith('/film/'):
            i = int(p[6]); return self.ver('text/html', bas + '<h2>%s</h2><button style="font-size:24px;margin:20px;padding:20px" onclick="fetch(\'/oynatildi/%d\')">Hemen İzle</button>' % (ADLAR[i-1], i))
        if p == '/':
            k = ''.join('<a href="/film/%d"><img src="/afis%d" alt="%s"></a>' % (i+1, i, a) for i, a in enumerate(ADLAR))
            return self.ver('text/html', bas + '<p><a href="/kategoriler">Kategoriler</a> Deneme afiş sayfası</p><a href="/serit"><img src="/afis1" alt="Duyuru" style="width:100%;height:50px"></a><div class="g">' + k + '</div>')
        self.ver('text/plain', 'ok')
srv = http.server.ThreadingHTTPServer(('0.0.0.0', 8765), S)
threading.Thread(target=srv.serve_forever, daemon=True).start()
sh("echo 'chrome --disable-fre --no-default-browser-check --no-first-run' > /data/local/tmp/chrome-command-line")
sh('am set-debug-app --persistent com.android.chrome')

def ekranYazilari():
    return [x.get('text') for x in dugumler() if x.get('text')]
sh(f'am start -n {P}/.MainActivity'); time.sleep(8)
m = ekranYazilari(); yaz('ana ekran dinleme seçimi', 'Dokununca dinle' in m, 'Arkada dinle' in m)
dokun('Arkada dinle'); yaz('anahtar sonrası', re.findall(r'name="surekli" value="(\w+)"', sh(f'run-as {P} cat shared_prefs/sahra.xml')))
dokun('Dokununca dinle', -1); yaz('geri', re.findall(r'name="surekli" value="(\w+)"', sh(f'run-as {P} cat shared_prefs/sahra.xml')))
for ad in ['Hafıza ve profil', 'Kumanda (odak) modu']:
    if dokun(ad):
        yaz(ad, ekranYazilari()[:12]); sh('input keyevent 4'); time.sleep(2)
sh(f'settings put secure enabled_accessibility_services {P}/{P}.EkranService'); sh('settings put secure accessibility_enabled 1'); time.sleep(4)
def ac(yol='/'):
    sh(f'am start -a android.intent.action.VIEW -d http://10.0.2.2:8765{yol} com.android.chrome'); time.sleep(9)
def yesil():
    ham = subprocess.run(['adb', 'exec-out', 'screencap'], capture_output=True, timeout=60).stdout
    w = int.from_bytes(ham[0:4], 'little'); h = int.from_bytes(ham[4:8], 'little'); ofs = len(ham) - w * h * 4
    xs, ys = [], []
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            r, g, b = ham[ofs + (y*w+x)*4: ofs + (y*w+x)*4 + 3]
            if g > 150 and r < 100 and 100 < b < 185 and g - r > 90 and g - b > 20: xs.append(x); ys.append(y)
    return (min(xs), min(ys), max(xs), max(ys), len(xs)) if xs else None
def dene(baslik, cumle, bekle=8, cerceve=False):
    istekler.clear(); adb('logcat', '-c'); komut(cumle, bekle if not cerceve else 3)
    if cerceve: yaz('   yeşil çerçeve alanı', yesil()); time.sleep(3)
    kayit(baslik + ' :: ' + cumle); yaz('   istekler', list(istekler))
komut('merhaba', 5)
sh('input keyevent 3'); time.sleep(2)
ac(); time.sleep(4)
dene('açılır pencere', 'no thanks butonuna tıkla', 6)
ac(); 
dene('kumanda', 'kumanda modu', 6)
yaz('önce yeşil', yesil())
dene('ilerle1', 'afiş ilerle', cerceve=True)
dene('ilerle2', 'ilerle', cerceve=True)
dene('alta', 'alta geç', cerceve=True)
dene('onceki', 'önceki', cerceve=True)
dene('aç', 'aç')
dene('oynat', 'oynat')
dene('geri', 'geri')
dene('kaydır', 'kaydır', 6)
dene('yukarı', 'yukarı', 6)
for n in range(6):
    dene('sona doğru %d' % n, 'ilerle', 5)
dene('aç2', 'aç')
dene('geri', 'geri')
ac()
dene('kategoriler', 'kategoriler')
dene('korku', 'korku kategorisini aç')
dene('geri', 'geri', 6)
dene('menüde gez', 'ilerle', cerceve=True)
dene('durdur', 'durdur', 5)
dene('kapat', 'kumandayı kapat', 6)
dene('kapalıyken', 'ilerle', 7)
dene('öğret', 'film zamanı deyince saat kaç', 7)
dene('onay', 'evet', 6)
dene('öğrenilen', 'film zamanı', 7)
dene('liste', 'neler öğrendin', 9)
dene('bilinmeyen', 'abur cubur', 7)
dene('doğrusu', 'bugünün tarihi', 12)
dene('onay2', 'evet', 6)
dene('ikinci kez', 'abur cubur', 7)
dene('tekrar', 'tekrar yap', 7)
dene('yasak', 'hadi bakalım deyince gönder', 7)
dene('arkada', 'arkada dinle', 6)
yaz('ayar', re.findall(r'name="surekli" value="(\w+)"', sh(f'run-as {P} cat shared_prefs/sahra.xml')))
yaz('bildirim', sorted(set(re.findall(r'(Arkada dinliyor|Dokununca dinle|Arkada dinle|Robota dokunup komut verin)', sh('dumpsys notification --noredact')))))
dene('dokunarak', 'dokununca dinle', 6)
yaz('ayar', re.findall(r'name="surekli" value="(\w+)"', sh(f'run-as {P} cat shared_prefs/sahra.xml')))
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
for l in adb('logcat', '-d', '-b', 'crash').splitlines()[:25]: yaz('log', l[:250])
