#!/usr/bin/env python3
"""app/src/main/assets/robot1.html dosyasını bu klasördeki parçalardan üretir."""
import pathlib

K = pathlib.Path(__file__).resolve().parent
kod = '\n'.join((K / f).read_text(encoding='utf-8') for f in ['motor.js', 'yuz.js', 'model.js', 'poz.js', 'uygulama.js'])
(K.parents[1] / 'app/src/main/assets/robot1.html').write_text(
    '<!DOCTYPE html>\n<html lang="tr"><head><meta charset="utf-8">\n'
    '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
    '<style>html,body{margin:0;height:100%;overflow:hidden;background:transparent;-webkit-tap-highlight-color:transparent}'
    'canvas{display:block;width:100%;height:100%}</style></head>\n'
    '<body><canvas id="tuval"></canvas>\n<script>\n' + kod + '\n</script></body></html>\n', encoding='utf-8')
