"""16x16 texture for the flying wing spikes (client/ShardSpikes.java): v runs from the bright tip (top) through the
red widest part (v~0.65) to the dark tail; the four faces (u quarters) alternate lighter / darker like crystal facets.
Colors are taken from the Ukaku wing palette. Usage: python3 kagune_spike_texture.py <out.png>"""
import sys
from PIL import Image
STOPS = [(0.0, (255, 222, 120)), (0.12, (255, 160, 48)), (0.45, (222, 48, 14)), (0.7, (150, 16, 7)), (1.0, (52, 5, 3))]
def color(v):
    for (a, ca), (b, cb) in zip(STOPS, STOPS[1:]):
        if v <= b:
            t = (v - a) / (b - a)
            return tuple(round(x + (y - x) * t) for x, y in zip(ca, cb))
    return STOPS[-1][1]
im = Image.new('RGBA', (16, 16))
for y in range(16):
    base = color((y + 0.5) / 16)
    for x in range(16):
        face = x // 4
        k = (1.08, 0.82, 1.0, 0.7)[face]
        edge = 1.12 if x % 4 == 0 else 1.0     # thin light edge on each facet
        im.putpixel((x, y), tuple(min(255, round(c * k * edge)) for c in base) + (255,))
im.save(sys.argv[1] if len(sys.argv) > 1 else 'kagune_spike.png')
