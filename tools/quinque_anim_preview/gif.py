import json, sys, subprocess, os
from PIL import Image, ImageDraw, ImageFont
q = sys.argv[1]; step = 0.5
style = {'kura': 'blade', 'narukami': 'blade', 'jason': 'scythe', 'ixa': 'spear'}[q]
A = json.load(open('quinque_animations.json'))['styles'][style]
names = {'attack_1': 'ЛКМ: удар 1', 'attack_2': 'ЛКМ: удар 2', 'attack_3': 'ЛКМ: удар 3', 'strong': 'ПКМ x2: сильный удар',
         'lunge': 'Shift+ЛКМ: выпад', 'flurry': 'K: шквал', 'release': 'J: Освобождение', 'guard': 'L: блок'}
seq = [('attack_1', 0), ('attack_2', 0), ('attack_3', 0), ('strong', 0), ('lunge', 11), ('flurry', 30), ('release', 0), ('guard', 0)]
jobs, frames = [], []
for mv, dur in seq:
    if mv == 'guard':
        L = 14
    else:
        L = dur if dur else A[mv]['length']
    t = 0.0
    while t < L + 1.0:
        tt = min(t, L - 0.01) if mv != 'guard' else 0.5
        ok = mv != 'guard' or t < L
        f = []
        for v, w, h in (('front', 300, 300), ('fp', 300, 169)):
            n = f'{q}_{mv}_{v}_{t:06.2f}'
            jobs.append({'name': n, 'view': v, 'quinque': q, 'move': mv if (t < L or mv == 'guard') else None, 't': tt,
                         'duration': dur, 'w': w, 'h': h, 'open': mv == 'release' and q == 'narukami'})
            f.append(n)
        frames.append((names[mv], f)); t += step
json.dump(jobs, open(f'jobs_gif_{q}.json', 'w'))
subprocess.run(['node', 'run.js', f'jobs_gif_{q}.json', f'gif_{q}'], check=True, stdout=subprocess.DEVNULL)
try:
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', 15)
except Exception:
    font = ImageFont.load_default()
out = []
for label, (a, b) in frames:
    im = Image.new('RGB', (300, 300 + 169 + 24), (20, 20, 24))
    im.paste(Image.open(f'gif_{q}/{a}.png').convert('RGB'), (0, 24)); im.paste(Image.open(f'gif_{q}/{b}.png').convert('RGB'), (0, 324))
    d = ImageDraw.Draw(im); d.text((8, 3), label, fill=(255, 230, 120), font=font)
    out.append(im.quantize(colors=128, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE))
out[0].save(f'quinque_{q}.gif', save_all=True, append_images=out[1:], duration=25, loop=0, optimize=True)
print(q, len(out), 'frames', os.path.getsize(f'quinque_{q}.gif') // 1024, 'KB')
