import re, struct, subprocess, time, xml.etree.ElementTree as ET
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
def sinir(n): return list(map(int, re.findall(r'\d+', n.get('bounds'))))
def merkez(n):
    a = sinir(n); return (a[0]+a[2])//2, (a[1]+a[3])//2
def bul(m):
    for n in dugumler():
        if m in (n.get('text'), n.get('content-desc')): return n
def piksel(x, y):
    ham = subprocess.run(['adb', 'exec-out', 'screencap'], capture_output=True).stdout
    w, h = struct.unpack('<II', ham[:8]); bas = len(ham) - w*h*4
    i = bas + (y*w+x)*4
    return '#%02x%02x%02x' % tuple(ham[i:i+3])
yaz('kurulum', adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk').strip())
sh(f'am start -n {P}/.MainActivity'); time.sleep(8)
for i in ['RECORD_AUDIO', 'POST_NOTIFICATIONS']: sh(f'pm grant {P} android.permission.{i}')
sh(f'appops set {P} SYSTEM_ALERT_WINDOW allow')
sh(f'am force-stop {P}'); sh(f'am start -n {P}/.MainActivity'); time.sleep(6)
def kaydirip(m):
    for _ in range(14):
        n = bul(m)
        if n is not None and merkez(n)[1] < 2150: return n
        sh('input swipe 540 1800 540 1200 500'); time.sleep(1)
def dokun(m):
    n = kaydirip(m)
    if n is None: yaz('bulunamadı', m); return False
    x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(1.5); return True
def ayar(): return ' '.join(l.strip() for l in sh(f"run-as {P} sh -c 'cat shared_prefs/*.xml'").splitlines() if 'isik' in l)
def bas():
    for _ in range(5): sh('input swipe 540 900 540 2100 150')
    time.sleep(1)
bas(); dokun("Sahra'yı başlat"); time.sleep(6)
yaz('hizmet', 'SahraService' in sh(f'dumpsys activity services {P}'))
dokun('Kenar ışığı'); time.sleep(4)
w = next((d for d in dugumler() if d.get('class') == 'android.webkit.WebView'), None)
yaz('önizleme', w.get('bounds') if w is not None else None)
def ornek():
    a = sinir(w); x, y = (a[0]+a[2])//2, (a[1]+a[3])//2; en = a[2]-a[0]
    l = []
    for i in range(7):
        l.append(piksel(x - int(en*0.36), y)); time.sleep(0.4)
    return l
if w is not None: yaz('kenar pikselleri (varsayılan)', ornek())
dokun('Zümrüt'); yaz('ayar1', ayar())
dokun('ışık kirmizi'); yaz('ayar2', ayar())
dokun('ışık yesil'); yaz('ayar3', ayar())
if w is not None: yaz('kenar pikselleri (kırmızı)', ornek())
dokun('Sabit'); time.sleep(2); yaz('ayar4', ayar())
if w is not None: yaz('kenar pikselleri (sabit)', ornek())
for m in ['Kalp atışı', 'Dönen renkler', 'Nefes', 'Gözler de aynı renkte olsun', 'Dinlerken de benim renklerim']:
    dokun(m)
yaz('ayar5', ayar())
for _ in range(3): sh('input swipe 540 1900 540 1100 300'); time.sleep(0.6)
yaz('panel sonu', [d.get('text') for d in dugumler() if d.get('text')][-8:])
sh('input keyevent 4'); time.sleep(2); bas()
sh('input keyevent 3'); time.sleep(8)
yaz('hizmet sonda', 'SahraService' in sh(f'dumpsys activity services {P}'))
yaz('konsol', [l[-200:] for l in adb('logcat', '-d').splitlines() if 'chromium' in l and 'CONSOLE' in l][:5])
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
