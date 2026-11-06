"""Tsukiyama's koukaku kagune ("Gourmet"): a bright purple ribbon that winds around the player's RIGHT ARM like a spiral
bandage, straight along the arm from shoulder to hand, and ends in a short tapered tip at the fist (no separate blade).
It is NOT held: it hugs the arm and follows its swing.
Generates GeckoLib geo.json + palette texture.
Authoring space G = in-game GeckoLib space: +X player's right, +Y up, +Z behind the player, pixels (16 px = 1 block).
The right arm at rest occupies x in [4, 8], y in [12, 24], z in [-2, 2]; its shoulder pivot is (5, 22, 0). The model is drawn
relative to the arm by KaguneLayer (it renders it in the right arm's space, so the rest pose here is the arm hanging down)."""
import json, math
import numpy as np
from PIL import Image

PAL = {  # swatch name -> rgb: bright purple ribbon (reference: "bright purple kagune wrapped around his arm as a spiraling ribbon")
    'void': (28, 10, 48), 'iron': (82, 30, 140), 'violet': (138, 52, 214), 'lilac': (176, 108, 244),
    'steel': (196, 140, 250), 'silver': (214, 170, 255), 'edge': (236, 210, 255), 'wine': (104, 24, 170),
}
NAMES = list(PAL)
TW = TH = 32

def tex(path):
    im = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    for i, n in enumerate(NAMES):
        x, y = (i % 8) * 4, (i // 8) * 4
        for dx in range(4):
            for dy in range(4):
                im.putpixel((x + dx, y + dy), PAL[n] + (255,))
    im.save(path)

def uv(name):
    i = NAMES.index(name); u, v = (i % 8) * 4 + 1, (i // 8) * 4 + 1
    f = {'uv': [u, v], 'uv_size': [2, 2]}
    return {k: dict(f) for k in ('north', 'east', 'south', 'west', 'up', 'down')}

def norm(v): v = np.asarray(v, float); return v / np.linalg.norm(v)

def zyx(R):
    gy = math.asin(max(-1, min(1, -R[2, 0]))); gx = math.atan2(R[2, 1], R[2, 2]); gz = math.atan2(R[1, 0], R[0, 0])
    return gx, gy, gz

def r(x): return round(float(x), 4)

def cube(J, R, w, L, t, color, off=0.0):
    """Box in local frame (x: width, y: length from J, z: thickness), rotated by R about J (G space).
    off shifts the box along local z (thickness direction) in pixels."""
    gx, gy, gz = zyx(R)
    return {'origin': [r(-(J[0] + w / 2)), r(J[1]), r(J[2] - t / 2 + off)], 'size': [r(w), r(L), r(t)],
            'pivot': [r(-J[0]), r(J[1]), r(J[2])], 'rotation': [r(-math.degrees(gx)), r(-math.degrees(gy)), r(math.degrees(gz))],
            'uv': uv(color)}

def frame(d, wref):
    Y = norm(d); w = np.asarray(wref, float)
    X = w - np.dot(w, Y) * Y
    if np.linalg.norm(X) < 1e-6: X = np.array([1.0, 0.0, 0.0]) - np.dot([1.0, 0.0, 0.0], Y) * Y
    X = norm(X); Z = np.cross(X, Y)
    return np.column_stack([X, Y, Z])

def bone(name, parent, pivot, cubes=None):
    b = {'name': name, 'pivot': [r(-pivot[0]), r(pivot[1]), r(pivot[2])]}
    if parent: b['parent'] = parent
    if cubes: b['cubes'] = cubes
    return b

def smooth(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)));  return t * t * (3 - 2 * t)

def lerp(a, b, t): return a + (b - a) * t

# ---------------------------------------------------------------- the ribbon path
CX = 6.0                       # axis of the arm (x): the arm is 4 px wide, x in [4, 8], so the coil is centred on it
TURNS, Y0, Y1 = 2.9, 22.8, 12.4   # arm is y in [12, 24]; the strap is 3 px wide, so it covers the whole arm
A, EXP = 2.95, 4.0             # coil cross-section: a rounded square (superellipse) so the corners of the arm stay inside
STRAP = 2.3                    # strap width along the arm; the pitch is ~3.6 px, so there are ~1.3 px gaps between the turns
TIP_BONES = []                 # filled by build(): bones of the tapered end (gourmet_anim.py animates them)
TIP = 6.0                      # length of the tapered end of the ribbon past the wrist

PH = -2 * math.pi * TURNS      # the ribbon arrives at the front of the wrist (z = -A) at t = 1

def helix(t):
    th = 2 * math.pi * TURNS * t + PH
    s_, c_ = math.sin(th), math.cos(th)
    k = (abs(s_) ** EXP + abs(c_) ** EXP) ** (1.0 / EXP)     # radius scale that squares the circle
    return np.array([CX + A * s_ / k, lerp(Y0, Y1, t), -A * c_ / k])

def catmull(pts, n):
    pts = [np.array(p, float) for p in pts]
    P = [pts[0] * 2 - pts[1]] + pts + [pts[-1] * 2 - pts[-2]]
    out = []
    for i in range(1, len(P) - 2):
        for k in range(n):
            t = k / n; p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(pts[-1]); return out

def path():
    N = 72
    pts = [helix(i / N) for i in range(N + 1)]
    end = pts[-1]
    # the free end leaves the last turn and runs straight down along the arm, past the fist, tapering to a point
    lead = [end + np.array([0.0, -1.6, -0.3]), end + np.array([0.0, -3.4, -0.6]), end + np.array([0.0, -TIP - 1.0, -0.8])]
    return catmull(pts[::2] + lead, 4)

def resample(curve, step):
    out = [curve[0]]; acc = 0.0
    for a, b in zip(curve, curve[1:]):
        seg = np.linalg.norm(b - a)
        while acc + seg >= step:
            f = (step - acc) / seg; a = a + (b - a) * f; out.append(a); seg = np.linalg.norm(b - a); acc = 0.0
        acc += seg
    out.append(curve[-1]); return out

def build():
    pts = resample(path(), 1.6)
    n = len(pts) - 1
    lens = [np.linalg.norm(pts[i + 1] - pts[i]) for i in range(n)]
    total = sum(lens)
    bones = [bone('gourmet', None, (CX, 22.0, 0)), bone('gourmet_root', 'gourmet', (CX, 22.0, 0))]
    parent = 'gourmet_root'; walked = 0.0
    wref = np.array([0.0, 1.0, 0.0])          # strap is wide along the arm axis, thin radially
    for i in range(n):
        a, b = pts[i], pts[i + 1]; d = b - a
        mid = walked + lens[i] / 2; walked += lens[i]
        to_end = total - mid
        in_tip = to_end <= TIP
        L = lens[i] + 0.5
        if not in_tip:
            colour = 'lilac' if i % 9 == 4 else 'violet'
            cubes = [cube(a, frame(d, wref), STRAP, L, 1.0, colour)]
        else:
            u = 1.0 - to_end / TIP
            cubes = [cube(a, frame(d, wref), lerp(STRAP, 0.4, u ** 0.9), L, lerp(1.0, 0.5, u), 'lilac' if u > 0.5 else 'violet')]
        bname = f'gourmet_seg{i + 1}'
        bones.append(bone(bname, parent, a, cubes)); parent = bname
        if in_tip:
            TIP_BONES.append(bname)
    return bones

if __name__ == '__main__':
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else 'assets/gourmet'
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.gourmet_kagune', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 6, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': build()}]}
    json.dump(geo, open(out + '.geo.json', 'w'), indent=1)
    tex(out + '.png')
    print('bones', len(geo['minecraft:geometry'][0]['bones']), 'cubes', sum(len(b.get('cubes', [])) for b in geo['minecraft:geometry'][0]['bones']))
