"""Kaneki's kagune_dash (the user's own animation) starts slowly: the strong motion arrives only after 0.25 s, while
the dash itself lasts 3-5 ticks, so in game the tentacles move after the dash is already over. This squeezes the
opening 0.25 s into 0.08 s and stretches the rest a little so the length stays 1.1 s (the same pose curve, just earlier).
Already applied to the shipped file in 0.73.2: do not run it again on that file, or the opening would be squeezed twice.
Usage: python3 retime_dash.py <kaneki_crimson_kagune.animation.json>"""
import json, sys
p = sys.argv[1]
data = json.load(open(p))
a = data['animations']['kagune_dash']
L, CUT, NEW = float(a['animation_length']), 0.25, 0.08
def f(t):
    return t * NEW / CUT if t <= CUT else NEW + (t - CUT) * (L - NEW) / (L - CUT)
def k(t):
    s = f'{t:.4f}'.rstrip('0'); return s + '0' if s.endswith('.') else s
for bone in a['bones'].values():
    for ch in ('rotation', 'position', 'scale'):
        tr = bone.get(ch)
        if isinstance(tr, dict):
            bone[ch] = {k(f(float(t))): v for t, v in sorted(tr.items(), key=lambda kv: float(kv[0]))}
json.dump(data, open(p, 'w'), indent=1)
print('ok')
