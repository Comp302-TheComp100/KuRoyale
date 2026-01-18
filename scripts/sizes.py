from pathlib import Path
from PIL import Image

p = Path('src/main/resources/images/animations/troops')
SCALE = 64.0

for d in sorted(p.iterdir()):
    if d.is_dir():
        png = next(d.rglob('*.png'), None)
        if png:
            w, h = Image.open(png).size
            tile = round(max(w, h) / SCALE, 2)
            print(f'{d.name}: {w}x{h} -> {tile}')
