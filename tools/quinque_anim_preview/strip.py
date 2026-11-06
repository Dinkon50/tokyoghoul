# usage: strip.py anim.json quinque move1,move2 [times] [views] [duration]
import json, sys, subprocess, os
anim, q, moves = sys.argv[1], sys.argv[2], sys.argv[3].split(',')
times = [float(x) for x in sys.argv[4].split(',')] if len(sys.argv) > 4 and sys.argv[4] else None
views = sys.argv[5].split(',') if len(sys.argv) > 5 and sys.argv[5] else ['front', 'back', 'fp']
dur = float(sys.argv[6]) if len(sys.argv) > 6 else 0
A = json.load(open(anim))
style = {'kura': 'blade', 'narukami': 'blade', 'jason': 'scythe', 'ixa': 'spear'}[q]
jobs = []
names = []
for mv in moves:
    m = A['styles'][style].get(mv) or A['styles'][style].get('strong_bolt')
    L = dur if dur > 0 else m['length']
    ts = times or [round(L * i / 7, 2) for i in range(8)]
    for v in views:
        row = []
        for t in ts:
            n = f'{q}_{mv}_{v}_{t:05.2f}'
            j = {'name': n, 'view': v, 'quinque': q, 'move': mv, 't': t, 'duration': dur}
            if v == 'fp': j.update(w=256, h=144)
            else: j.update(w=240, h=240)
            jobs.append(j); row.append(n)
        names.append(row)
json.dump(jobs, open('jobs_strip.json', 'w'))
env = dict(os.environ, ANIM=anim)
subprocess.run(['node', 'run.js', 'jobs_strip.json', 'outs'], env=env, check=True, stdout=subprocess.DEVNULL)
from PIL import Image, ImageDraw
rows = []
for row in names:
    ims = [Image.open('outs/' + n + '.png') for n in row]
    w = sum(i.width for i in ims); h = ims[0].height + 12
    r = Image.new('RGB', (w, h), (25, 25, 25)); d = ImageDraw.Draw(r); x = 0
    for n, im in zip(row, ims):
        r.paste(im, (x, 12)); d.text((x + 2, 0), n.split('_')[-1], fill=(255, 255, 0)); x += im.width
    rows.append(r)
W = max(r.width for r in rows); H = sum(r.height for r in rows)
out = Image.new('RGB', (W, H), (25, 25, 25)); y = 0
for r in rows: out.paste(r, (0, y)); y += r.height
fn = f'strip_{q}_{"-".join(moves)}.png'; out.save(fn); print(fn, out.size)
