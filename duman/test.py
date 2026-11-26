import os, re, subprocess, time, xml.etree.ElementTree as ET
P = 'com.guven.sahra'
os.makedirs('ekranlar', exist_ok=True)
def adb(*a):
    return subprocess.run(['adb', *a], capture_output=True, timeout=180).stdout.decode('utf-8', 'replace')
def sh(c): return adb('shell', c)
def yaz(*a): print('SONUC', *a, flush=True)
def dugumler():
    for _ in range(3):
        sh('rm -f /sdcard/u.xml; uiautomator dump /sdcard/u.xml')
        try:
            return list(ET.fromstring(sh('cat /sdcard/u.xml')).iter('node'))
        except ET.ParseError:
            time.sleep(1.5)
    return []
def merkez(n):
    a = list(map(int, re.findall(r'\d+', n.get('bounds'))))
    return (a[0] + a[2]) // 2, (a[1] + a[3]) // 2
def bul(metin, tam=True):
    for n in dugumler():
        for t in (n.get('text'), n.get('content-desc')):
            if t and ((t == metin) if tam else (metin in t)):
                return n
def dokun(n):
    x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(1.5)
def kaydirip_bul(metin, tam=True):
    for _ in range(10):
        n = bul(metin, tam)
        if n is not None and merkez(n)[1] < 2150: return n
        sh('input swipe 540 1700 540 900 400'); time.sleep(0.8)
def basa():
    for _ in range(5): sh('input swipe 540 700 540 2000 150')
    time.sleep(1)
def ekran(ad):
    open(f'ekranlar/{ad}.png', 'wb').write(subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True).stdout)
def cokme(nerede):
    log = adb('logcat', '-d', '-b', 'crash')
    if P in log:
        yaz('COKME', nerede)
        for l in log.splitlines()[:40]: yaz('log', l[:250])
        adb('logcat', '-b', 'crash', '-c'); return True
    return False
def ac(): sh(f'am start -n {P}/.MainActivity'); time.sleep(3)

yaz('kurulum', adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk').strip())
adb('logcat', '-b', 'crash', '-c')
for i in ['RECORD_AUDIO', 'POST_NOTIFICATIONS', 'READ_CONTACTS', 'CALL_PHONE', 'READ_CALENDAR']:
    sh(f'pm grant {P} android.permission.{i}')
sh(f'appops set {P} SYSTEM_ALERT_WINDOW allow')
ac(); time.sleep(5); ekran('00-ana'); cokme('açılış')
basa(); n = bul("Sahra'yı başlat")
if n is not None: dokun(n)
time.sleep(8); cokme('başlatma'); ekran('30-calisiyor')
yaz('hizmet çalışıyor', 'SahraService' in sh(f'dumpsys activity services {P}'))
def komut(c, ad):
    ac(); basa()
    n = bul('Yazılı komut')
    if n is None: yaz('komut satırı yok', c); sh('input keyevent 4'); return
    dokun(n)
    kutu = next((x for x in dugumler() if x.get('class') == 'android.widget.EditText'), None)
    if kutu is None: yaz('kutu yok', c); return
    dokun(kutu); sh('input keyevent 123'); sh('input keyevent ' + ' '.join(['67'] * 50))
    sh("input text '" + c.replace(' ', '%s') + "'"); time.sleep(0.5)
    g = bul('Gönder')
    if g is not None: dokun(g)
    time.sleep(3); ekran('k-' + ad)
    on = re.findall(r'(?:topResumedActivity|ResumedActivity).*? (\S+/\S+)', sh('dumpsys activity activities'))
    yaz('komut', c, '| öndeki:', on[0] if on else '?', '| çökme:', cokme(c))
    sh('input keyevent 4'); time.sleep(1)
adb('logcat', '-c')
for j, c in enumerate(['10 dakika say', '7 ye alarm kur', 'feneri ac']):
    komut(c, f'{j:02d}')
    time.sleep(3)
    for l in adb('logcat', '-d').splitlines():
        if re.search(r'SET_ALARM|SET_TIMER|[Tt]orch', l) and 'SONUC' not in l: yaz('iz', c, '::', l[31:250])
    adb('logcat', '-c')
yaz('fener durumu', [l.strip() for l in sh('dumpsys media.camera').splitlines() if 'orch' in l][-4:])
yaz('alarm kaydı', [l.strip()[:160] for l in sh('dumpsys alarm').splitlines() if 'deskclock' in l.lower()][:5])
# araba modu açık kalmasın diye uygulama yeniden başlatılır
sh(f'am force-stop {P}')
sh(f'am start -n {P}/.MainActivity'); time.sleep(2)

ac(); basa(); n = kaydirip_bul('"Sahra" uyandırma')
if n is not None:
    dokun(n); d = bul('Modeli indir')
    if d is not None:
        dokun(d)
        for _ in range(40):
            time.sleep(5)
            if bul('Model yüklü.') is not None: break
            h = bul('Model kurulamadı', False)
            if h is not None: yaz('model hatası', h.get('text')); break
        yaz('model yüklü', bul('Model yüklü.') is not None); ekran('50-model')
        s = bul('Adımı telefonda tanı', False)
        if s is not None:
            adb('logcat', '-c')
            dokun(bul('Adımı telefonda tanı', False) or s); time.sleep(5)
            yaz('anahtar', (bul('Adımı telefonda tanı', False).get('checked') if bul('Adımı telefonda tanı', False) is not None else '?'))
            sh('input keyevent 4'); time.sleep(1); basa()
            b2 = bul("Sahra'yı başlat")
            if b2 is not None: dokun(b2)
            time.sleep(30); ekran('51-uyandirma')
            yaz('uyandırma açıldı, çökme:', cokme('uyandırma'))
        sh('input keyevent 4'); sh('input keyevent 3'); time.sleep(15); ekran('52-sonra'); cokme('uyandırma sonrası')
    else:
        yaz('indir düğmesi yok')
yaz('mikrofon', [l.strip()[:200] for l in sh('dumpsys audio').splitlines() if P in l and ('rec' in l.lower())][-4:])
yaz('hizmet', 'SahraService' in sh(f'dumpsys activity services {P}'))
yaz('süreç yaşıyor', bool(sh(f'pidof {P}').strip()))
for l in adb('logcat', '-d').splitlines():
    if re.search(r'UnsatisfiedLink|VoskAPI|Background activity start|AudioRecord|UyandirmaMotoru', l): yaz('günlük', l[:220])
cokme('son')
