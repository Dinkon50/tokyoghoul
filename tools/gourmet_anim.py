"""Animations for Tsukiyama's "Gourmet" kagune (the ribbon spiral on the right arm).
Writes animations/gourmet_kagune.animation.json with the same animation names the kagune controller plays
(kagune_idle, kagune_walk, kagune_block_*, ...) plus the Gourmet one-shots (gourmet_slash_a/b, gourmet_thrust,
gourmet_lunge, gourmet_whirl, gourmet_dance). The arm itself is moved by the vanilla arm swing (the server swings
the main hand on every strike); these animations move the ribbon: the root bone 'gourmet' sits on the arm axis at the
shoulder, so rotating it about Y twists the spiral around the arm and scaling it grows / tightens the coil;
the tapered end (TIP_BONES of gourmet_kagune.py) whips, curls and stabs.
Usage: python3 gourmet_anim.py <out.animation.json>"""
import importlib.util, json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location('gourmet_kagune', os.path.join(HERE, 'gourmet_kagune.py'))
g = importlib.util.module_from_spec(spec); spec.loader.exec_module(g)
g.build()
TIP = list(g.TIP_BONES)          # tip bones from the base of the taper to the point
ROOT = 'gourmet'

def key(x):
    s = f'{x:.3f}'.rstrip('0')
    return s + '0' if s.endswith('.') else s

def track(points):
    """points: list of (time, [x, y, z])."""
    return {key(tm): [round(float(v), 3) for v in vec] for tm, vec in points}

def anim(length, bones, loop=None):
    a = {'animation_length': length, 'bones': bones}
    if loop is not None:
        a['loop'] = loop
    return a

def bone(rotation=None, scale=None, position=None):
    b = {}
    if rotation: b['rotation'] = track(rotation)
    if scale: b['scale'] = track(scale)
    if position: b['position'] = track(position)
    return b

def tip_wave(length, amp, axis, cycles=1.0, delay=0.06, base=0.0, n=9):
    """A wave travelling down the tip: every tip bone swings about `axis` a little later than the previous one."""
    out = {}
    for k, name in enumerate(TIP):
        pts = []
        for i in range(n + 1):
            tm = length * i / n
            ph = 2 * math.pi * cycles * (tm / length) - k * delay * 2 * math.pi / max(length, 1e-3)
            v = [0.0, 0.0, 0.0]
            v[axis] = base + amp * math.sin(ph) * (0.6 + 0.4 * k / max(1, len(TIP) - 1))
            pts.append((tm, v))
        out[name] = bone(rotation=pts)
    return out

def tip_pose(times_angles, axis):
    """Same curl on every tip bone (they compound into a hook)."""
    out = {}
    for name in TIP:
        pts = []
        for tm, ang in times_angles:
            v = [0.0, 0.0, 0.0]; v[axis] = ang
            pts.append((tm, v))
        out[name] = bone(rotation=pts)
    return out

def merge(*parts):
    out = {}
    for p in parts:
        for name, b in p.items():
            out.setdefault(name, {}).update(b)
    return out

def stab(times_scales):
    """The tip shoots out like a rapier: scale the first tip bone along the arm (its children follow)."""
    return {TIP[0]: bone(scale=[(tm, [1.0, s, 1.0]) for tm, s in times_scales])}

A = {}
# ---------------------------------------------------------------- state animations (controller names)
A['kagune_appear'] = anim(1.2, {ROOT: bone(
    rotation=[(0.0, [0, -540, 0]), (0.8, [0, -25, 0]), (1.2, [0, 0, 0])],
    scale=[(0.0, [0.05, 0.05, 0.05]), (0.4, [0.7, 0.35, 0.7]), (0.8, [1.08, 1.0, 1.08]), (1.2, [1, 1, 1])])},
    'hold_on_last_frame')
A['kagune_retract'] = anim(0.9, {ROOT: bone(
    rotation=[(0.0, [0, 0, 0]), (0.5, [0, 200, 0]), (0.9, [0, 540, 0])],
    scale=[(0.0, [1, 1, 1]), (0.5, [1.05, 0.65, 1.05]), (0.9, [0.02, 0.02, 0.02])])},
    'hold_on_last_frame')
A['kagune_idle'] = anim(3.0, merge(
    {ROOT: bone(rotation=[(0.0, [0, 0, 0]), (1.5, [0, 6, 0]), (3.0, [0, 0, 0])],
                scale=[(0.0, [1, 1, 1]), (1.5, [1.03, 1.0, 1.03]), (3.0, [1, 1, 1])])},
    tip_wave(3.0, 5.0, 0, cycles=1.0, delay=0.15)), True)
A['kagune_walk'] = anim(1.0, merge(
    {ROOT: bone(rotation=[(0.0, [0, -4, 0]), (0.5, [0, 4, 0]), (1.0, [0, -4, 0])])},
    tip_wave(1.0, 9.0, 0, cycles=1.0, delay=0.06)), True)
A['kagune_rage'] = anim(0.8, merge(
    {ROOT: bone(rotation=[(0.0, [0, -7, 0]), (0.4, [0, 7, 0]), (0.8, [0, -7, 0])],
                scale=[(0.0, [1, 1, 1]), (0.2, [1.09, 1.03, 1.09]), (0.4, [1, 1, 1]), (0.6, [1.09, 1.03, 1.09]), (0.8, [1, 1, 1])])},
    tip_wave(0.8, 12.0, 2, cycles=2.0, delay=0.04)), True)
A['kagune_break_blocks'] = anim(0.5, tip_pose([(0.0, 0), (0.15, -22), (0.5, 0)], 0), True)
# block: the coil closes (turns pressed together, a bit tighter around the arm) and the tip curls up over the fist
TIGHT = [0.93, 0.88, 0.93]
A['kagune_block_start'] = anim(0.25, merge(
    {ROOT: bone(scale=[(0.0, [1, 1, 1]), (0.15, [0.9, 0.86, 0.9]), (0.25, TIGHT)],
                rotation=[(0.0, [0, 0, 0]), (0.25, [0, 30, 0])])},
    tip_pose([(0.0, 0), (0.25, 28)], 0)), 'hold_on_last_frame')
A['kagune_block_hold'] = anim(1.0, merge(
    {ROOT: bone(scale=[(0.0, TIGHT), (0.5, [0.95, 0.88, 0.95]), (1.0, TIGHT)],
                rotation=[(0.0, [0, 30, 0]), (1.0, [0, 30, 0])])},
    tip_pose([(0.0, 28), (0.5, 32), (1.0, 28)], 0)), True)
A['kagune_block_end'] = anim(0.7, merge(
    {ROOT: bone(scale=[(0.0, TIGHT), (0.3, [1.04, 1.02, 1.04]), (0.7, [1, 1, 1])],
                rotation=[(0.0, [0, 30, 0]), (0.7, [0, 0, 0])])},
    tip_pose([(0.0, 28), (0.7, 0)], 0)), 'hold_on_last_frame')
# dash: the tip streams back behind the hand
A['kagune_dash'] = anim(1.1, merge(
    {ROOT: bone(rotation=[(0.0, [0, 0, 0]), (0.2, [0, -20, 0]), (1.1, [0, 0, 0])])},
    tip_pose([(0.0, 0), (0.15, 26), (0.6, 18), (1.1, 0)], 0)), 'hold_on_last_frame')

# ---------------------------------------------------------------- Gourmet strikes
def slash(sign):
    return anim(0.3, merge(
        {ROOT: bone(rotation=[(0.0, [0, 0, 0]), (0.08, [0, -12 * sign, 0]), (0.16, [0, 28 * sign, 0]), (0.3, [0, 0, 0])])},
        tip_pose([(0.0, 0), (0.08, 18 * sign), (0.16, -30 * sign), (0.3, 0)], 2)), 'hold_on_last_frame')
A['gourmet_slash_a'] = slash(1)
A['gourmet_slash_b'] = slash(-1)
A['gourmet_thrust'] = anim(0.45, merge(
    {ROOT: bone(scale=[(0.0, [1, 1, 1]), (0.08, [1.07, 0.86, 1.07]), (0.18, [0.95, 1.12, 0.95]), (0.45, [1, 1, 1])],
                rotation=[(0.0, [0, 0, 0]), (0.08, [0, -40, 0]), (0.18, [0, 25, 0]), (0.45, [0, 0, 0])])},
    stab([(0.0, 1.0), (0.1, 0.8), (0.18, 2.4), (0.3, 1.8), (0.45, 1.0)])), 'hold_on_last_frame')
A['gourmet_lunge'] = anim(0.6, merge(
    {ROOT: bone(scale=[(0.0, [1, 1, 1]), (0.2, [1.08, 0.82, 1.08]), (0.3, [0.94, 1.14, 0.94]), (0.6, [1, 1, 1])],
                rotation=[(0.0, [0, 0, 0]), (0.2, [0, -70, 0]), (0.3, [0, 40, 0]), (0.6, [0, 0, 0])])},
    stab([(0.0, 1.0), (0.2, 0.7), (0.28, 2.9), (0.45, 2.2), (0.6, 1.0)])), 'hold_on_last_frame')
# whirl: the spiral spins around the arm and flares out, the tip flung outwards sweeps around
spin = [(i * 0.25, [0, 180 * i, 0]) for i in range(9)]
A['gourmet_whirl'] = anim(2.0, merge(
    {ROOT: bone(rotation=spin,
                scale=[(0.0, [1, 1, 1]), (0.15, [1.16, 0.94, 1.16]), (1.85, [1.16, 0.94, 1.16]), (2.0, [1, 1, 1])])},
    tip_pose([(0.0, 0), (0.15, -40), (1.85, -40), (2.0, 0)], 2)), 'hold_on_last_frame')
# dance (ultimate): a short shimmer, then seven lightning stabs
dance_root = [(0.0, [0, 0, 0]), (0.3, [0, -360, 0])]
dance_scale = [(0.0, [1, 1, 1]), (0.15, [1.12, 1.0, 1.12]), (0.3, [1, 1, 1])]
dance_tip = [(0.0, 1.0), (0.3, 1.0)]
for k in range(7):
    t0 = 0.3 + k * 0.1
    dance_root.append((t0 + 0.05, [0, -360 + (22 if k % 2 == 0 else -22), 0]))
    dance_tip += [(t0 + 0.04, 2.6), (t0 + 0.09, 1.1)]
dance_root.append((1.2, [0, -360, 0]))
dance_tip.append((1.2, 1.0))
A['gourmet_dance'] = anim(1.2, merge({ROOT: bone(rotation=dance_root, scale=dance_scale)}, stab(dance_tip)),
                          'hold_on_last_frame')

# Kaneki one-shots: never played for Gourmet, kept empty so the model always has every name
for name, length in (('kagune_attack_right_thrust', 0.85), ('kagune_attack_left_sweep', 0.95),
                     ('kagune_attack_double', 1.1), ('kagune_barrage', 4.5), ('kagune_grapple', 1.0),
                     ('kagune_grab_pull', 1.0)):
    A[name] = anim(length, {})

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'gourmet_kagune.animation.json'
    json.dump({'format_version': '1.8.0', 'animations': A}, open(out, 'w'), indent=1)
    print('animations', len(A), 'tip', TIP)
