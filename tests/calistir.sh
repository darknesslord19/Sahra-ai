#!/bin/sh
set -e
cd "$(dirname "$0")/.."
out=$(mktemp -d)
javac -encoding UTF-8 -d "$out" app/src/main/java/com/guven/sahra/Komut.java app/src/main/java/com/guven/sahra/Sayfa.java app/src/main/java/com/guven/sahra/Ozel.java app/src/main/java/com/guven/sahra/Davranis.java app/src/main/java/com/guven/sahra/Uyandirma.java app/src/main/java/com/guven/sahra/Veri.java $(for a in Zaman KisiBul Onay Serbest Suzgec Hafiza Wav Isik Beyin Form Sozler Isim Jest Sahra1 Secim Kumanda Ogrenme; do printf 'app/src/main/java/com/guven/sahra/%s.java ' $a; done) tests/KomutTest.java
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$out" KomutTest
