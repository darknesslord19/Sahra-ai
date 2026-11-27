import re, sqlite3, subprocess, time, xml.etree.ElementTree as ET
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
    for _ in range(12):
        n = bul(m)
        if n is not None and merkez(n)[1] < 2150: return n
        sh('input swipe 540 1800 540 1300 600' if yon > 0 else 'input swipe 540 1300 540 1800 600'); time.sleep(1)
def dokun(m, yon=1):
    n = kaydirip(m, yon)
    if n is None:
        yaz('bulunamadı', m, '| ekranda:', [x.get('text') for x in dugumler() if x.get('text')][:30]); return False
    x, y = merkez(n); sh(f'input tap {x} {y}'); time.sleep(2); return True
def metinler(): return [x.get('text') for x in dugumler() if x.get('text')]
# Uydurma deneme veritabanı (kullanıcının dosyası depoya konmaz)
db = sqlite3.connect('/tmp/deneme.sqlite')
db.execute('create table kategoriler(ID integer primary key, KatAdi text, Icon text)')
db.execute('create table sozler(ID integer unique, KatId integer, Soz text, FavDurum integer, GunSoz integer)')
for i, a in [(1, 'Komik Sözler'), (2, 'Dini Sözler'), (3, 'Romantik Sözler'), (17, 'Cuma Günü Mesajları'), (18, 'Anne ve Anneler Günü İçin Sözler')]:
    db.execute('insert into kategoriler values(?,?,?)', (i, a, ''))
n = 0
def ekle(k, s):
    global n; n += 1; db.execute('insert into sozler values(?,?,?,0,0)', (n, k, s))
for i in range(1, 4):
    ekle(1, f'KOMIK-{i} çok güzel şaka')
    ekle(2, f'DINI-{i} güzel öğüt')
    ekle(3, f'ROMANTIK-{i} sevgi')
    ekle(17, f'CUMA-{i}'); ekle(18, f'ANNE-{i}')
ekle(1, 'KOMIK-UZUN ' + 'çok uzun bir söz ' * 8)
ekle(1, '  KOMIK-1   çok güzel şaka '); ekle(2, '')
db.commit(); db.close()
yaz('kurulum', adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk').strip())
adb('push', '/tmp/deneme.sqlite', '/sdcard/Download/deneme.sqlite')
sh('am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/deneme.sqlite')
for i in ['RECORD_AUDIO', 'POST_NOTIFICATIONS']: sh(f'pm grant {P} android.permission.{i}')
sh(f'appops set {P} SYSTEM_ALERT_WINDOW allow')
sh(f'am start -n {P}/.MainActivity'); time.sleep(8)
def komut(c, bekle=6):
    adb('logcat', '-c')
    sh(f"am start-foreground-service -n {P}/.SahraService -a komut --es komut \"{c}\""); time.sleep(bekle)
    yaz('>>>', c)
    for l in adb('logcat', '-d').splitlines():
        if 'SAHRADBG' in l and ' SOZ ' in l: yaz('   ', l[l.index(' SOZ '):][:200])
komut('komik söz söyle')
sh(f'am start -n {P}/.MainActivity'); time.sleep(3)
dokun('Boştayken gözlerde yazı')
dokun('SQLite dosyasından yükle'); time.sleep(4)
if bul('deneme.sqlite') is None:
    yaz('seçici ilk ekran', metinler()[:25])
    dokun('Show roots'); dokun('Downloads'); time.sleep(2)
dokun('deneme.sqlite'); time.sleep(4)
yaz('yükleme sonrası', [m for m in metinler() if 'söz' in m.lower()])
dokun('Sık')
for k in ['Komik Sözler  (4)', 'Dini Sözler  (3)', 'Romantik Sözler  (3)']:
    yaz('kategori görünüyor', k, kaydirip(k) is not None)
yaz('cuma/anne yok', not any('Cuma Günü' in m or 'Anneler' in m and '(' in m for m in metinler()))
dokun('Dini Sözler', 1)
def ayar(): return sh(f'run-as {P} cat shared_prefs/{pref}')
dosyalar = sh(f'run-as {P} ls shared_prefs').split()
for pref in dosyalar:
    if 'sozKapali' in sh(f'run-as {P} cat shared_prefs/{pref}'):
        yaz('ayar', re.findall(r'<(?:string|int|boolean) name="soz\w+"[^<]*(?:</string>)?', ayar()))
yaz('uygulama dosyaları', sh(f'run-as {P} ls files cache'))
sh(f'am force-stop {P}'); time.sleep(2)
for c in ['güzel söz söyle'] * 4 + ['rastgele söz', 'komik söz söyle', 'dini söz', 'romantik söz oku', 'damar söz']:
    komut(c)
komut('dur', 3)
adb('logcat', '-c'); sh(f'am force-stop {P}'); sh(f'am start -n {P}/.MainActivity'); time.sleep(3)
komut('merhaba', 4)
sh('input keyevent 3')
adb('logcat', '-c'); time.sleep(150)
for l in adb('logcat', '-d', '-v', 'time').splitlines():
    if 'SAHRADBG' in l and 'GOZ' in l: yaz('   ', l[6:18], l[l.index('GOZ'):][:200])
# Sırayla modu
sh(f'am force-stop {P}'); sh(f'am start -n {P}/.MainActivity'); time.sleep(4)
dokun('Boştayken gözlerde yazı'); dokun('Karışık (rastgele)')
dokun('Hepsini kapat'); yaz('hepsi kapalı uyarısı', any('Bütün kategoriler kapalı' in m for m in metinler()))
dokun('Hepsini aç', -1); dokun('Yüklenen sözleri sil')
yaz('silince', [m for m in metinler() if 'dosya' in m.lower()])
sh(f'am force-stop {P}')
komut('komik söz söyle')
yaz('çökme', P in adb('logcat', '-d', '-b', 'crash'))
for l in adb('logcat', '-d', '-b', 'crash').splitlines()[:25]: yaz('log', l[:250])
