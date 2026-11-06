"""Animations for Touka's Ukaku wings (ukaku_kagune.py).
Writes animations/ukaku_kagune.animation.json with the controller names (kagune_appear, kagune_retract, kagune_idle,
kagune_walk, kagune_rage, kagune_break_blocks, kagune_block_*, kagune_dash) and the wing one-shots
(wings_fire, wings_big, wings_hail, wings_storm). Kaneki-only names are kept empty so the model has every name.

Axes (checked in the previewer): on wing_right +Z raises the wing, +Y sweeps it back (folds behind the back),
+X turns it forward over the shoulder. The left wing is mirrored: same X, negated Y and Z (helper `wings`).
Usage: python3 ukaku_anim.py <out.animation.json>"""
import importlib.util, json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('ukaku_kagune', os.path.join(HERE, 'ukaku_kagune.py'))
u = importlib.util.module_from_spec(spec); spec.loader.exec_module(u)
NAMES = [b['name'] for b in u.build()]
ROOT = 'ukaku'
ARMS = {side: [n for n in NAMES if n.startswith(f'wing_{side}_arm')] for side in ('right', 'left')}
SIGN = {'right': 1, 'left': -1}

def key(x):
    s = f'{x:.3f}'.rstrip('0')
    return s + '0' if s.endswith('.') else s

def track(points):
    return {key(tm): [round(float(v), 3) for v in vec] for tm, vec in points}

def anim(length, bones, loop=None):
    a = {'animation_length': length, 'bones': bones}
    if loop is not None:
        a['loop'] = loop
    return a

def merge(*parts):
    out = {}
    for p in parts:
        for name, b in p.items():
            out.setdefault(name, {}).update(b)
    return out

def wings(points):
    """points: (time, [x, sweep, raise]) for the right wing; the left one is mirrored."""
    return {f'wing_{side}': {'rotation': track([(tm, [x, s * y, s * z]) for tm, (x, y, z) in points])}
            for side, s in SIGN.items()}

def curl(points):
    """Bend the leading edge: every arm bone past the elbow turns back by `angle` (they add up into a curve)."""
    out = {}
    for side, s in SIGN.items():
        for k, name in enumerate(ARMS[side]):
            w = max(0.0, (k - 5) / (len(ARMS[side]) - 5))
            if w <= 0:
                continue
            out[name] = {'rotation': track([(tm, [0, s * a * w, -s * a * w * 0.4]) for tm, a in points])}
    return out

def root(rotation=None, scale=None, position=None):
    b = {}
    if rotation: b['rotation'] = track(rotation)
    if scale: b['scale'] = track([(tm, [v, v, v] if isinstance(v, (int, float)) else v) for tm, v in scale])
    if position: b['position'] = track(position)
    return {ROOT: b}

def shards(points):
    """Loose embers above the wings: drift (position) and grow / vanish (scale)."""
    points = [(p[0], p[1][0], p[1][1]) if len(p) == 2 else p for p in points]
    out = {}
    for side in SIGN:
        b = {}
        b['position'] = track([(tm, [0, dy, 0]) for tm, dy, sc in points])
        # scale is about the wing root, so above 1 the embers would fly off: 0 hides them, 1 is the rest size
        b['scale'] = track([(tm, [min(sc, 1.0)] * 3) for tm, dy, sc in points])
        out[f'wing_{side}_shards'] = b
    return out

def wave(length, n, fn):
    """Sample fn(t) -> value at n+1 points across a looping animation (first == last)."""
    return [(length * i / n, fn(length * i / n)) for i in range(n + 1)]

A = {}
FOLD = [0, 75, -35]          # wings pressed flat along the back, tips down
# ---------------------------------------------------------------- appear / retract
# appear: the wings burst out of the shoulder blades folded, then snap open with one strong beat and settle
A['kagune_appear'] = anim(1.5, merge(
    root(scale=[(0.0, 0.05), (0.3, 0.55), (0.55, 1.0), (1.5, 1.0)]),
    wings([(0.0, FOLD), (0.35, [0, 60, -25]), (0.65, [-8, -12, 28]), (0.9, [4, 6, -10]), (1.15, [0, -2, 6]), (1.5, [0, 0, 0])]),
    curl([(0.0, 14), (0.45, 10), (0.75, -6), (1.1, 2), (1.5, 0)]),
    shards([(0.0, -4, 0.0), (0.6, -2, 0.0), (0.9, 1.5, 1.2), (1.5, 0, 1.0)])), 'hold_on_last_frame')
# retract: a last flare, the wings fold flat behind the back and sink into the shoulder blades
A['kagune_retract'] = anim(1.1, merge(
    root(scale=[(0.0, 1.0), (0.55, 1.0), (0.85, 0.5), (1.1, 0.02)]),
    wings([(0.0, [0, 0, 0]), (0.15, [-4, -6, 12]), (0.6, FOLD), (1.1, FOLD)]),
    curl([(0.0, 0), (0.15, -4), (0.6, 14), (1.1, 14)]),
    shards([(0.0, 0, 1.0), (0.3, 2, 0.6), (0.5, 3, 0.0), (1.1, 3, 0.0)])), 'hold_on_last_frame')

# ---------------------------------------------------------------- state loops
A['kagune_idle'] = anim(3.0, merge(
    wings(wave(3.0, 6, lambda t: [0, 3 * math.sin(2 * math.pi * t / 3), 4 * math.sin(2 * math.pi * t / 3 + 0.6)])),
    curl(wave(3.0, 6, lambda t: 3 * math.sin(2 * math.pi * t / 3 + 1.2))),
    shards(wave(3.0, 6, lambda t: (1.2 * math.sin(2 * math.pi * t / 3), 1.0)))), True)
A['kagune_walk'] = anim(1.2, merge(
    wings(wave(1.2, 6, lambda t: [0, 14 + 3 * math.sin(2 * math.pi * t / 1.2), -4 + 6 * math.sin(2 * math.pi * t / 1.2)])),
    curl(wave(1.2, 6, lambda t: 5 + 2 * math.sin(2 * math.pi * t / 1.2 + 0.8))),
    shards(wave(1.2, 6, lambda t: (0.8 * math.sin(2 * math.pi * t / 1.2), 1.0)))), True)
# rage: raised and spread wide, quivering; the embers flare
A['kagune_rage'] = anim(0.6, merge(
    root(scale=wave(0.6, 4, lambda t: 1.08 + 0.03 * math.sin(2 * math.pi * t / 0.3))),
    wings(wave(0.6, 6, lambda t: [0, -10 + 3 * math.sin(2 * math.pi * t / 0.3), 18 + 5 * math.sin(2 * math.pi * t / 0.3)])),
    curl(wave(0.6, 6, lambda t: -4 + 3 * math.sin(2 * math.pi * t / 0.3 + 1.0))),
    shards(wave(0.6, 6, lambda t: (2.5 + 1.5 * math.sin(2 * math.pi * t / 0.3), 1.35)))), True)
A['kagune_break_blocks'] = anim(0.5, wings([(0.0, [0, 0, 0]), (0.15, [-6, -4, 10]), (0.5, [0, 0, 0])]), True)

# ---------------------------------------------------------------- block: the wings wrap forward into a cocoon
SHIELD = [-10, -60, -12]
A['kagune_block_start'] = anim(0.3, merge(
    wings([(0.0, [0, 0, 0]), (0.18, [-12, -68, -8]), (0.3, SHIELD)]),
    curl([(0.0, 0), (0.3, -16)])), 'hold_on_last_frame')
A['kagune_block_hold'] = anim(1.0, merge(
    wings(wave(1.0, 4, lambda t: [SHIELD[0], SHIELD[1] + 2 * math.sin(2 * math.pi * t), SHIELD[2] + 1.5 * math.sin(2 * math.pi * t + 1)])),
    curl([(0.0, -16), (0.5, -18), (1.0, -16)])), True)
A['kagune_block_end'] = anim(0.7, merge(
    wings([(0.0, SHIELD), (0.3, [2, 8, 6]), (0.7, [0, 0, 0])]),
    curl([(0.0, -16), (0.35, 3), (0.7, 0)])), 'hold_on_last_frame')

# ---------------------------------------------------------------- dash: wings swept hard back, one beat to recover
A['kagune_dash'] = anim(1.1, merge(
    wings([(0.0, [0, 0, 0]), (0.12, [6, 55, -12]), (0.45, [4, 45, -8]), (0.75, [-4, -6, 10]), (1.1, [0, 0, 0])]),
    curl([(0.0, 0), (0.12, 14), (0.45, 10), (0.75, -3), (1.1, 0)]),
    shards([(0.0, 0, 1.0), (0.12, -2, 0.7), (0.6, 0, 1.0), (1.1, 0, 1.0)])), 'hold_on_last_frame')

# ---------------------------------------------------------------- wing one-shots
# LMB auto-fire (kept alive by the server while shots go out): the wings kick in turn, right then left
def kick(t0):
    return [(0.0, [0, 0, 0]), (t0, [0, 0, 0]), (t0 + 0.04, [-5, -3, 6]), (t0 + 0.12, [0, 0, 0]), (0.3, [0, 0, 0])]
fire = {}
for side, s in SIGN.items():
    pts = kick(0.0 if side == 'right' else 0.15)
    pts = sorted({tm: v for tm, v in pts}.items())
    fire[f'wing_{side}'] = {'rotation': track([(tm, [x, s * y, s * z]) for tm, (x, y, z) in pts])}
A['wings_fire'] = anim(0.3, merge(
    {name: {'rotation': b['rotation']} for name, b in fire.items()},
    root(rotation=[(0.0, [0, 0, 0]), (0.04, [-2, 0, 0]), (0.15, [0, 0, 0]), (0.19, [-2, 0, 0]), (0.3, [0, 0, 0])]),
    shards(wave(0.3, 4, lambda t: (0.6 * math.sin(2 * math.pi * t / 0.15), 1.1)))), True)
# RMB x2, big shard: the wings rise and pull back to gather it (windup 5 ticks), then whip forward as it flies
A['wings_big'] = anim(0.7, merge(
    root(rotation=[(0.0, [0, 0, 0]), (0.25, [10, 0, 0]), (0.32, [-10, 0, 0]), (0.7, [0, 0, 0])],
         scale=[(0.0, 1.0), (0.25, 1.1), (0.32, 0.96), (0.7, 1.0)]),
    wings([(0.0, [0, 0, 0]), (0.25, [12, 30, 22]), (0.32, [-16, -24, 4]), (0.5, [-4, -6, 2]), (0.7, [0, 0, 0])]),
    curl([(0.0, 0), (0.25, 10), (0.32, -10), (0.7, 0)]),
    shards([(0.0, 0, 1.0), (0.25, -2, 1.4), (0.3, 0, 0.0), (0.55, 0, 0.0), (0.7, 0, 1.0)])), 'hold_on_last_frame')
# K, hail: wings spread forward and shudder as volleys go out (loops while K is held)
A['wings_hail'] = anim(0.4, merge(
    root(rotation=[(0.0, [-8, 0, 0]), (0.4, [-8, 0, 0])]),
    wings(wave(0.4, 8, lambda t: [-10 + 3 * math.sin(2 * math.pi * t / 0.1), -18, 12 + 5 * math.sin(2 * math.pi * t / 0.2)])),
    curl(wave(0.4, 4, lambda t: -6 + 2 * math.sin(2 * math.pi * t / 0.2))),
    shards(wave(0.4, 4, lambda t: (1.5 * math.sin(2 * math.pi * t / 0.2), 1.2)))), True)
# Alt+J, storm: 1 s charge - wings rise high and wide, trembling harder and harder; 2 s fire - thrust forward, shaking
storm_w = [(0.0, [0, 0, 0]), (0.3, [0, -14, 30])]
for i in range(1, 8):
    tm = 0.3 + i * 0.1
    a = 1.5 + i * 0.6
    storm_w.append((tm, [0, -14 + (a if i % 2 else -a), 30 + (a if i % 2 else -a)]))
storm_w.append((1.05, [-16, -26, 16]))
for i in range(1, 20):
    tm = 1.05 + i * 0.1
    storm_w.append((tm, [-16 + (3 if i % 2 else -3), -26, 16 + (6 if i % 2 else -6)]))
storm_w += [(3.0, [-16, -26, 16]), (3.3, [0, 0, 0])]
A['wings_storm'] = anim(3.3, merge(
    root(rotation=[(0.0, [0, 0, 0]), (1.0, [6, 0, 0]), (1.1, [-10, 0, 0]), (3.0, [-10, 0, 0]), (3.3, [0, 0, 0])],
         scale=[(0.0, 1.0), (1.0, 1.15), (1.1, 1.1), (3.0, 1.1), (3.3, 1.0)]),
    wings(storm_w),
    curl([(0.0, 0), (1.0, -10), (1.1, -6), (3.0, -6), (3.3, 0)]),
    shards([(0.0, 0, 1.0), (1.0, 4, 1.6), (1.1, 0, 0.8), (3.0, 0, 0.8), (3.3, 0, 1.0)])), 'hold_on_last_frame')

# Kaneki one-shots: never played for the wings, kept empty so every name exists
for name in ('kagune_attack_right_thrust', 'kagune_attack_left_sweep', 'kagune_attack_double', 'kagune_grab_pull',
             'kagune_grapple', 'kagune_barrage'):
    A[name] = anim(0.05, {})

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'ukaku_kagune.animation.json'
    json.dump({'format_version': '1.8.0', 'animations': A}, open(out, 'w'), indent=1)
    print(len(A), 'animations ->', out)
