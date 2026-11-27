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
for deneme in range(3):
    bas()
    if dokun('Kenar ışığı'): break
    yaz('ekrandakiler', [d.get('text') for d in dugumler() if d.get('text')][:15])
    sh(f'am force-stop {P}'); sh(f'am start -n {P}/.MainActivity'); time.sleep(8)
time.sleep(4)
w = next((d for d in dugumler() if d.get('class') == 'android.webkit.WebView'), None)
yaz('önizleme', w.get('bounds') if w is not None else None)
def ornek():
    a = sinir(w); x, y = (a[0]+a[2])//2, (a[1]+a[3])//2; en = a[2]-a[0]
    l = []
    for i in range(7):
        l.append(piksel(x - int(en*0.36), y)); time.sleep(0.4)
    return l
if w is not None: yaz('kenar pikselleri (varsayılan)', ornek())
dokun('Zümrüt'); dokun('ışık kirmizi'); dokun('ışık yesil'); time.sleep(3); yaz('ayar', ayar())
def ham():
    h = subprocess.run(['adb', 'exec-out', 'screencap'], capture_output=True).stdout
    w, y = struct.unpack('<II', h[:8]); return h, w, len(h) - w*y*4
a = sinir(w); cx, cy = (a[0]+a[2])//2, (a[1]+a[3])//2
adb('logcat', '-c')
for i in range(12):
    h, gen, bas0 = ham()
    def px(x, y):
        j = bas0 + (y*gen+x)*4; return '#%02x%02x%02x' % tuple(h[j:j+3])
    yaz('kesit sol', ' '.join(px(cx+d, cy) for d in range(-124, -90, 4)), '| sağ', ' '.join(px(cx-d, cy) for d in range(-112, -96, 4)), '| üst gövde', px(cx, cy-100), '| zemin', px(a[0]+4, a[1]+4))
    time.sleep(0.3)
for l in [l for l in adb('logcat', '-d').splitlines() if 'DBG' in l][-24:]: yaz(l[l.index('DBG'):][:330])
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
