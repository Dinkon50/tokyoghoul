"""Ukaku wing kagune: two crystal wings from the shoulder blades. Generates GeckoLib geo.json + palette texture.
Authoring space G = in-game GeckoLib space: +X player's right, +Y up, +Z behind the player, pixels."""
import json, math, random
import numpy as np
from PIL import Image

PAL = {  # swatch name -> rgb: fire red, from dark embers at the root to orange-yellow flame at the tips
    'core': (38, 4, 2), 'plum': (78, 8, 4), 'deep': (112, 10, 5), 'crimson': (176, 20, 8),
    'rose': (222, 48, 14), 'pink': (250, 98, 24), 'light': (255, 160, 48), 'glow': (255, 222, 120),
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

def cube(J, R, w, L, t, color, back=0.0):
    """Box in local frame (x: width, y: length from J, z: thickness), rotated by R about J (G space)."""
    gx, gy, gz = zyx(R)
    return {'origin': [r(-(J[0] + w / 2)), r(J[1]), r(J[2] - t / 2 + back)], 'size': [r(w), r(L), r(t)],
            'pivot': [r(-J[0]), r(J[1]), r(J[2])], 'rotation': [r(-math.degrees(gx)), r(-math.degrees(gy)), r(math.degrees(gz))],
            'uv': uv(color)}

def r(x): return round(float(x), 4)

def frame(d, wref):
    Y = norm(d); X = norm(wref - np.dot(wref, Y) * Y); Z = np.cross(X, Y)
    return np.column_stack([X, Y, Z])

def bone(name, parent, pivot, cubes=None):
    b = {'name': name, 'pivot': [r(-pivot[0]), r(pivot[1]), r(pivot[2])]}
    if parent: b['parent'] = parent
    if cubes: b['cubes'] = cubes
    return b

ARM = [(2.3, 21.0, 2.3), (5.0, 25.5, 4.4), (8.2, 30.5, 6.4), (11.8, 34.5, 8.0), (15.2, 37.0, 9.0), (17.8, 37.8, 9.6), (19.6, 36.8, 9.9)]

def catmull(pts, n):
    pts = [np.array(p, float) for p in pts]
    P = [pts[0] * 2 - pts[1]] + pts + [pts[-1] * 2 - pts[-2]]
    out = []
    for i in range(1, len(P) - 2):
        for k in range(n):
            t = k / n; p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(pts[-1]); return out

def lerp(a, b, t): return a + (b - a) * t

def build():
    rnd = random.Random(11)
    bones = [bone('ukaku', None, (0, 20.5, 2.0))]
    bones.append(bone('ukaku_core', 'ukaku', (0, 21, 2.0), [
        cube(np.array([0, 18.8, 2.2]), frame([0, 1, 0.3], np.array([1, 0, 0])), 2.6, 3.6, 1.2, 'deep'),
        cube(np.array([0, 21.8, 2.4]), frame([0, 1, 0.55], np.array([1, 0, 0])), 1.6, 2.4, 1.0, 'crimson')]))
    for s, side in ((1, 'right'), (-1, 'left')):
        mir = lambda p: np.array([p[0] * s, p[1], p[2]])
        K = 0.5 if s > 0 else 0.72                        # the left wing is bigger, the right one smaller
        pts = catmull([mir(np.array(ARM[0]) + (np.array(p) - np.array(ARM[0])) * K) for p in ARM], 3)   # leading edge
        wing = f'wing_{side}'
        root = pts[0]
        bones.append(bone(wing, 'ukaku', root, [
            cube(root + np.array([s * rnd.uniform(-0.5, 0.5), rnd.uniform(-1, 1), 0.1]),
                 frame(norm([s * rnd.uniform(0.2, 1), rnd.uniform(-0.3, 1), 0.8]), np.array([0, 0, 1.0])),
                 rnd.uniform(1.4, 2.0), rnd.uniform(1.8, 2.6), 1.4, rnd.choice(['deep', 'plum'])) for _ in range(4)]))
        # arm: chain of bones along the leading edge (for animation later)
        parent = wing; n = len(pts) - 1
        arm_bones = []
        for i in range(n):
            a, b = pts[i], pts[i + 1]; d = b - a; L = np.linalg.norm(d)
            t = i / (n - 1)
            c = cube(a, frame(d, np.array([0, 0, 1.0])), lerp(2.4, 1.0, t), L + 0.6, lerp(1.8, 1.0, t),
                     'deep' if t < 0.35 else 'crimson')
            name = f'{wing}_arm{i + 1}'
            bones.append(bone(name, parent, a, [c])); arm_bones.append(name); parent = name
        # feathers hang from the arm: near the body they point down and back, at the tip outward
        NF = 11
        for k in range(NF):
            u = k / (NF - 1)
            idx = min(n - 1, int(round(lerp(1, n - 1, u))))
            A = pts[idx] + np.array([0, -0.4, 0.35 * (k % 2) + 0.1])
            down = norm([s * 0.2, -1.0, 0.45]); out = norm([s * 1.0, 0.45, 0.35])
            d0 = norm(lerp(down, out, u ** 1.25))
            length = [9, 12, 15, 17, 18, 18, 17, 15, 13, 11, 8][k] * rnd.uniform(0.96, 1.04) * K
            width = lerp(3.4, 2.6, u)
            tangent = norm(pts[min(idx + 1, n)] - pts[max(idx - 1, 0)])
            fname = f'{wing}_feather{k + 1}'
            bones.append(bone(fname, arm_bones[idx - 1] if idx > 0 else wing, A))
            parent = fname; J = A.copy(); d = d0
            segs = [(0.40, 0.95, 0.8, 'crimson'), (0.36, 1.0, 0.7, 'rose'), (0.24, 0.6, 0.55, 'pink')]
            for j, (frac, wf, tf, color) in enumerate(segs):
                L = length * frac
                c = cube(J, frame(d, tangent), width * wf, L + 0.6, tf, color)
                bn = f'{fname}_s{j + 1}'
                bones.append(bone(bn, parent, J, [c])); parent = bn
                J = J + d * L
                d = norm(d + np.array([0, 0, 0.12]) + np.array([s * 0.06, 0.03, 0]))   # tips curl back a little
            bones[-1]['cubes'].append(cube(J - d * 0.4, frame(d, tangent), width * 0.25, 2.2, 0.45, 'light'))
        # coverts: short dark feathers on top of the base row, give the wing depth
        for k in range(7):
            u = k / 6
            idx = min(n - 1, int(round(lerp(1, n - 2, u))))
            A = pts[idx] + np.array([0, -0.6, 0.9])
            d = norm(lerp(norm([s * 0.2, -1, 0.5]), norm([s * 1, -0.5, 0.35]), u))
            tangent = norm(pts[min(idx + 1, n)] - pts[max(idx - 1, 0)])
            L = lerp(6.5, 8.5, math.sin(u * math.pi)) * K
            bones.append(bone(f'{wing}_covert{k + 1}', arm_bones[idx - 1] if idx > 0 else wing, A,
                              [cube(A, frame(d, tangent), 3.0, L, 0.7, 'plum'),
                               cube(A + d * (L - 0.8), frame(d, tangent), 2.0, 1.8, 0.6, 'crimson')]))
        # loose shards drifting up off the outer edge, like embers
        sh = []
        for k in range(7):
            u = rnd.uniform(0.45, 1.0)
            idx = min(n - 1, int(round(lerp(1, n - 1, u))))
            down = norm([s * 0.2, -1.0, 0.45]); out = norm([s * 1.0, 0.45, 0.35])
            d = norm(lerp(down, out, u ** 1.25))
            J = pts[idx] + d * rnd.uniform(12, 16) * K + np.array([s * rnd.uniform(0.5, 2.0), rnd.uniform(0.5, 2.5), rnd.uniform(0, 1.5)])
            sh.append(cube(J, frame(norm(np.array([s * rnd.uniform(0, .5), 1.0, rnd.uniform(0, .4)])), np.array([0, 0, 1.0])),
                           rnd.uniform(0.5, 0.8), rnd.uniform(0.9, 1.4), 0.5, rnd.choice(['light', 'glow'])))
        bones.append(bone(f'{wing}_shards', wing, root, sh))
    return bones

if __name__ == '__main__':
    import sys
    out = sys.argv[1] if len(sys.argv) > 1 else 'assets/ukaku'
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.ukaku_kagune', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 6, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
        'bones': build()}]}
    json.dump(geo, open(out + '.geo.json', 'w'), indent=1)
    tex(out + '.png')
    print('bones', len(geo['minecraft:geometry'][0]['bones']), 'cubes', sum(len(b.get('cubes', [])) for b in geo['minecraft:geometry'][0]['bones']))
