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
n = None
for _ in range(12):
    n = bul('Robot görünümü')
    if n is not None and merkez(n)[1] < 2150: break
    sh('input swipe 540 1700 540 900 400'); time.sleep(1)
x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(4)
adlar = ['beyaz','siyah','mavi','turkuaz','yesil','mor','pembe','kirmizi','altin']
for d in dugumler():
    ad = d.get('content-desc') or d.get('text') or ''
    s = d.get('class', '').split('.')[-1]
    if ad in adlar:
        yaz('renk', ad, d.get('bounds'), piksel(*merkez(d)))
    else:
        yaz('düğüm', s, d.get('bounds'), ad[:40])
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
