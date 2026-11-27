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
sh(f'am start -n {P}/.MainActivity'); time.sleep(8)
def komut(c, bekle=6):
    sh(f"am start-foreground-service -n {P}/.SahraService -a komut --es komut \"{c}\""); time.sleep(bekle)
def kayit(baslik):
    yaz('---', baslik)
    for l in adb('logcat', '-d').splitlines():
        if 'SAHRADBG' in l: yaz('   ', l[l.index('SAHRADBG') + 8:][:160])
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
def piksel(ad):
    """Ekran görüntüsünü saklamadan robot penceresindeki koyu (vizör) ve açık (gövde) piksel oranını ölçer."""
    a = pencere()
    ham = subprocess.run(['adb', 'exec-out', 'screencap'], capture_output=True, timeout=60).stdout
    w = int.from_bytes(ham[0:4], 'little'); h = int.from_bytes(ham[4:8], 'little'); ofs = len(ham) - w * h * 4
    koyu = acik = n = 0
    for y in range(a[1], min(a[3], h), 3):
        for x in range(max(a[0], 0), min(a[2], w), 3):
            r, g, b = ham[ofs + (y*w+x)*4: ofs + (y*w+x)*4 + 3]; n += 1
            koyu += max(r, g, b) < 35; acik += min(r, g, b) > 200
    yaz(ad, 'pencere', a, 'koyu %%%d' % (100*koyu//n), 'açık %%%d' % (100*acik//n))
def hareket(noktalar):
    k = [f'input motionevent DOWN {noktalar[0][0]} {noktalar[0][1]}']
    k += [f'input motionevent MOVE {x} {y}' for x, y in noktalar[1:]]
    k += [f'input motionevent UP {noktalar[-1][0]} {noktalar[-1][1]}']
    t = time.time(); sh('; '.join(k)); yaz('hareket süresi sn', round(time.time()-t, 1), 'nokta', len(noktalar))
def merkezi():
    a = pencere(); return (a[0]+a[2])//2, (a[1]+a[3])//2
def cevir():
    x, y = merkezi(); cx, cy = 540, y
    hareket([(x, y)] + [(int(cx + 160*math.cos(i*2*math.pi/8)), int(cy + 160*math.sin(i*2*math.pi/8))) for i in range(21)])
def salla():
    x, y = merkezi()
    hareket([(x, y)] + [(540 + (130 if i % 2 else -130), y) for i in range(14)])
def dokunuslar(n):
    x, y = merkezi(); sh('; '.join([f'input tap {x} {y}'] * n))
komut('merhaba'); sh('input keyevent 3'); time.sleep(3)
piksel('SAHRA'); kayit('başlangıç')
def deneyler(ad):
    cevir(); time.sleep(2.5); piksel(ad + ' dönerken'); time.sleep(8); kayit(ad + ' çevirme')
    salla(); time.sleep(8); kayit(ad + ' sallama')
    x, y = merkezi(); sh(f'input swipe {x} {y} {x+300} {y+300} 1500'); time.sleep(4); kayit(ad + ' taşıma')
    dokunuslar(1); time.sleep(5); kayit(ad + ' tek dokunuş')
    dokunuslar(3); time.sleep(8); kayit(ad + ' üç dokunuş')
    dokunuslar(6); time.sleep(9); kayit(ad + ' altı dokunuş')
deneyler('SAHRA')
sh(f'am start -n {P}/.MainActivity'); time.sleep(4)
dokun('Robot görünümü'); dokun('Sahra 1 (elli)'); time.sleep(5)
sh('input keyevent 4'); time.sleep(2)
dokun('Sahra 1 ayarları', -1) if bul('Sahra 1 ayarları') is not None else dokun('Sahra 1 ayarları')
time.sleep(3)
yaz('panel', [m for m in metinler() if '▸' in m or 'Sahra' in m])
dokun('▸  Hazır karışımlar'); dokun('Seni seviyorum'); 
def tercih():
    t = sh(f'run-as {P} cat shared_prefs/sahra.xml')
    return re.findall(r'name="(model|s1\w+|davranis|tepki\w*)"(?: value="([^"]*)")?\s*/?>([^<\n]*)', t)
yaz('hazır sonrası ayar', tercih())
dokun('▾  Hazır karışımlar', -1); dokun('▸  El-kol:  Kalp yapma'); dokun('Sağ el selam')
yaz('el-kol sonrası ayar', tercih())
dokun("Sahra 1'i ilk ayarlarına döndür")
yaz('sıfırlama sonrası ayar', tercih())
kayit('Sahra 1 seçildi')
sh(f'am force-stop {P}'); sh(f'am start -n {P}/.MainActivity'); time.sleep(4)
komut('merhaba'); sh('input keyevent 3'); time.sleep(8)
piksel('SAHRA1'); kayit('Sahra 1 açılış')
deneyler('SAHRA1')
piksel('SAHRA1 sonda')
x, y = merkezi(); sh(f'input swipe {x} {y} {x} {y} 900'); time.sleep(3)
yaz('basılı tutunca ayarlar', f'{P}/.MainActivity' in sh('dumpsys activity activities | grep -i resumed'))
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
for l in adb('logcat', '-d', '-b', 'crash').splitlines()[:25]: yaz('log', l[:250])
