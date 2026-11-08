"""
Скины «Глубин 24-го района» (128 x 128). Все персонажи оригинальные.
  deep_ghoul_0/1        — глубинные гули (лохматый и лысый)
  deep_brute_0          — гуль-мутант с наростами кагуне
  deep_spitter_0        — плеватель в рваном капюшоне с костяной полумаской
  deep_guardian_sting_0 — страж «Жало»: чёрная броня, белая маска с клыками, алые полосы
  deep_guardian_wing_0  — страж «Крыло»: чёрная броня, маска-клюв, янтарные полосы
  ancient_ghoul_0       — Древний гуль: седые длинные волосы, костяная корона, тёмно-багровая мантия с золотом
Запуск: python3 skins_deep.py <папка textures/entity>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SHADE, SIDES, Skin, coat_skirt, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_hat_hair, paint_head_rest, paint_leg, save)
from skins_extra import kakugan  # noqa: E402

BLACK = (18, 14, 16)
BONE = (226, 220, 206)


def veins(sk, skin_c, n=10):
    """Чёрные вены по лицу и рукам."""
    f = sk.local("head", "front")
    for x, y in ((2, 10), (2, 11), (13, 10), (13, 11), (3, 6), (12, 6), (1, 9), (14, 9))[:n]:
        f.put(x, y, mix(skin_c, (30, 10, 16), 0.55))
    for arm in ("rarm", "larm"):
        for face in SIDES:
            fa = sk.local(arm, face)
            for y in range(4, 22, 3):
                fa.put((y // 3) % fa.width, y, mix(mul(skin_c, SHADE[face]), (40, 12, 20), 0.5))


def rags(sk, part, color, rows, holes=0.12):
    """Рваная ткань на внешнем слое: дыры и бахрома снизу."""
    for face in SIDES:
        f = sk.local(part, face)
        for y in range(rows):
            for x in range(f.width):
                if sk.rng.random() < holes or (y >= rows - 2 and sk.rng.random() < 0.4):
                    continue
                f.put(x, y, mul(color, SHADE[face] * (1 + sk.rng.uniform(-0.08, 0.08))))


def blood_mouth(sk):
    f = sk.local("head", "front")
    for x, y in ((6, 13), (7, 13), (8, 13), (9, 13), (7, 14), (9, 14), (6, 15)):
        f.put(x, y, (120, 10, 20) if (x + y) % 2 else (90, 6, 14))


def deep_ghoul(folder, variant):
    sk = Skin(7000 + variant)
    skin_c = 0xBDB8AE if variant == 0 else 0xA9A49C
    hair = 0x15121A
    paint_head_rest(sk, skin_c, hair, "short" if variant == 0 else "buzz")
    paint_face(sk, skin_c, ((235, 40, 50), (150, 10, 20)), hair,
               [3, 4, 5, 4, 3, 4, 5, 4, 4, 5, 4, 3, 4, 5, 4, 3] if variant == 0 else [0] * 16, mouth="grin")
    kakugan(sk, skin_c)
    veins(sk, skin_c)
    blood_mouth(sk)
    f = sk.local("head", "front")
    for x in (3, 4, 11, 12):
        f.put(x, 11, mul(skin_c, 0.72))                       # впалые щёки
    if variant == 0:
        paint_hat_hair(sk, hair, [3, 4, 6, 5, 3, 4, 6, 5, 5, 6, 4, 3, 5, 6, 4, 3], lambda d: 9, lambda x: 11)
    else:
        top = sk.local("head", "top")
        for y in range(16):
            for x in range(16):
                top.put(x, y, mul(skin_c, SHADE["top"] * (0.95 + sk.rng.uniform(-0.04, 0.04))))
        top.put(5, 6, (120, 40, 44)); top.put(6, 7, (120, 40, 44)); top.put(7, 8, (120, 40, 44))   # шрам
    # голый худой торс: рёбра
    sk.cloth("body", ("top", "bottom") + SIDES, skin_c, noise=0.03)
    bf = sk.local("body", "front")
    for y in (5, 7, 9, 11):
        for x in list(range(2, 6)) + list(range(10, 14)):
            bf.put(x, y, mul(skin_c, 0.82))
    for y in range(3, 16):
        bf.put(7, y, mul(skin_c, 0.86))
    for y in range(16, 24):
        for x in range(16):
            bf.put(x, y, mul(0x2A2622, 1 + sk.rng.uniform(-0.06, 0.06)))
    rags(sk, "jacket", 0x4A443C, 9, holes=0.35)
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, skin_c, skin_c, sleeve_rows=0)
    for face in SIDES:
        for arm in ("rarm", "larm"):
            fa = sk.local(arm, face)
            for x in range(fa.width):
                fa.put(x, 23, (40, 20, 24))                 # тёмные когти
    veins(sk, skin_c)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x2A2622, skin_c, mul(skin_c, 0.7), shoe_rows=2, seam=False)
    for p in ("rpants", "lpants"):
        rags(sk, p, 0x3A342E, 20, holes=0.2)
    save(sk, folder, f"deep_ghoul_{variant}")


def growth(f, x0, y0, w, h, rng):
    """Нарост кагуне: тёмно-красные кристаллы с алым краем."""
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if rng.random() < 0.75:
                c = (120, 10, 24) if rng.random() < 0.6 else (190, 26, 44)
                f.put(x, y, c)


def deep_brute(folder):
    sk = Skin(7100)
    skin_c, hair = 0x9C8478, 0x1A1214
    paint_head_rest(sk, skin_c, hair, "buzz")
    paint_face(sk, skin_c, ((235, 40, 50), (150, 10, 20)), hair, [0] * 16, mouth="grin")
    kakugan(sk, skin_c)
    blood_mouth(sk)
    f = sk.local("head", "front")
    growth(f, 11, 1, 4, 4, sk.rng)                            # нарост на лбу
    top = sk.local("hat", "top")
    growth(top, 2, 2, 6, 5, sk.rng)
    sk.cloth("body", ("top", "bottom") + SIDES, skin_c, noise=0.04)
    bf = sk.local("body", "front")
    for y in range(4, 13):
        bf.put(7, y, mul(skin_c, 0.8)); bf.put(8, y, mul(skin_c, 0.8))
    for x in range(2, 14):
        bf.put(x, 8, mul(skin_c, 0.85))
    for y in range(17, 24):
        for x in range(16):
            bf.put(x, y, mul(0x2E2420, 1 + sk.rng.uniform(-0.06, 0.06)))
    for face in ("front", "back"):
        growth(sk.local("jacket", face), 0, 0, 5, 7, sk.rng)
        growth(sk.local("jacket", face), 11, 0, 5, 7, sk.rng)
    growth(sk.local("jacket", "back"), 6, 8, 4, 10, sk.rng)
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, skin_c, skin_c, sleeve_rows=0)
        for face in SIDES:
            growth(sk.local(sleeve, face), 0, 0, 8, 6, sk.rng)
            growth(sk.local(sleeve, face), 2, 12, 4, 5, sk.rng)
    veins(sk, skin_c)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x2E2420, skin_c, mul(skin_c, 0.7), shoe_rows=2, seam=False)
    for p in ("rpants", "lpants"):
        rags(sk, p, 0x3A2E28, 18, holes=0.15)
    save(sk, folder, "deep_brute_0")


def deep_spitter(folder):
    sk = Skin(7200)
    skin_c, hair = 0xB4B0A6, 0x101014
    cloak = 0x2C383E
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((235, 40, 50), (150, 10, 20)), hair, [2] * 16, mouth="calm")
    kakugan(sk, skin_c)
    f = sk.local("head", "front")
    for y in range(11, 16):                                   # костяная полумаска с зубами
        for x in range(2, 14):
            f.put(x, y, mul(BONE, 1.0 - 0.03 * (y - 11)))
    for x in range(3, 13):
        f.put(x, 13, (40, 30, 30) if x % 2 else BONE)
    # капюшон на внешнем слое
    for face in ("right", "left", "back", "top"):
        hf = sk.local("hat", face)
        for y in range(hf.height):
            for x in range(hf.width):
                hf.put(x, y, mul(cloak, SHADE[face] * (1 + sk.rng.uniform(-0.06, 0.06))))
    hf = sk.local("hat", "front")
    for y in range(0, 5):
        for x in range(16):
            hf.put(x, y, mul(cloak, 1.05))
    for y in range(5, 16):
        for x in (0, 1, 14, 15):
            hf.put(x, y, mul(cloak, 0.9))
    sk.cloth("body", ("top", "bottom") + SIDES, 0x23262A, noise=0.04)
    rags(sk, "jacket", cloak, 24, holes=0.08)
    # укаку на спине: светлые кристаллы-перья
    jb = sk.local("jacket", "back")
    for y in range(2, 12):
        for x in range(3, 13):
            if (x + y) % 3 == 0:
                jb.put(x, y, (150, 200, 230) if y % 2 else (90, 150, 200))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, cloak, skin_c, sleeve_rows=18)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x23262A, 0x1A1A1E, 0x0A0A0C, shoe_rows=4)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, cloak, 16, trim=mul(cloak, 0.7))
    save(sk, folder, "deep_spitter_0")


def guardian(folder, name, accent, mask_kind, hair_c):
    sk = Skin(7300 + len(name))
    skin_c = 0xC8BFB6
    paint_head_rest(sk, skin_c, hair_c, "short")
    paint_face(sk, skin_c, ((235, 40, 50), (150, 10, 20)), hair_c, [3] * 16, mouth="stern")
    f = sk.local("head", "front")
    # маска на всё лицо, глаза — прорези с кагуганом
    for y in range(3, 16):
        for x in range(1, 15):
            f.put(x, y, mul(BONE, 1.0 - 0.02 * abs(x - 7.5) - 0.01 * y))
    kakugan(sk, BONE)
    for x in range(1, 15):
        f.put(x, 6, mul(BONE, 0.82))
    if mask_kind == "fangs":
        for x in range(3, 13):
            f.put(x, 12, BLACK)
            if x % 2:
                f.put(x, 13, BLACK)
        for x in (4, 11):
            f.put(x, 13, BONE); f.put(x, 14, mul(BONE, 0.9))          # клыки
        for y in range(3, 15):
            f.put(7 if y % 2 else 8, y, accent) if y < 6 else None
    else:
        for y in range(10, 16):                                         # клюв
            for x in range(7 - (15 - y) // 2, 9 + (15 - y) // 2):
                f.put(x, y, mul(BONE, 0.9 - 0.03 * (y - 10)))
        for x in range(5, 11):
            f.put(x, 12, mul(BONE, 0.65))
    for x in (2, 13):
        for y in range(4, 12):
            f.put(x, y, accent if y % 3 else mul(accent, 0.7))          # полосы цвета стража
    paint_hat_hair(sk, hair_c, [2] * 16, lambda d: 6, lambda x: 7)
    armor = (24, 20, 24)
    sk.cloth("body", ("top", "bottom") + SIDES, armor, noise=0.04)
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), (34, 28, 32), noise=0.05)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            c = mul((34, 28, 32), 1.0 + sk.rng.uniform(-0.05, 0.05))
            if y in (6, 7) or (x in (2, 13) and y < 18):
                c = mul((54, 48, 52), 1.0)
            if 6 < x < 9 and y > 10:
                c = accent
            jf.put(x, y, c)
    for y in range(3, 20, 4):
        sk.local("jacket", "back").put(7, y, accent); sk.local("jacket", "back").put(8, y, accent)
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, (34, 28, 32), (20, 16, 18), cuff=accent, sleeve_rows=20)
    for sleeve in ("rsleeve", "lsleeve"):
        for face in SIDES:
            fs = sk.local(sleeve, face)
            for x in range(fs.width):
                for y in range(0, 5):
                    fs.put(x, y, mul((54, 48, 52), SHADE[face]))       # наплечники
                fs.put(x, 5, mul(accent, SHADE[face]))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, armor, (14, 12, 14), (6, 5, 6), shoe_rows=8)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, (34, 28, 32), 14, trim=accent)
    save(sk, folder, name)


def ancient(folder):
    sk = Skin(7400)
    skin_c, hair = 0xCFC6C0, 0xE6E2DC
    robe, gold = (52, 8, 16), (206, 162, 70)
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((255, 30, 50), (160, 8, 20)), hair, [2, 3, 4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 3, 2],
               mouth="stern")
    kakugan(sk, skin_c, iris=(255, 40, 60))
    veins(sk, skin_c)
    f = sk.local("head", "front")
    for x in range(2, 14):
        f.put(x, 3, mul(skin_c, 0.85))                       # морщины
    for x, y in ((5, 12), (10, 12), (4, 13), (11, 13)):
        f.put(x, y, mul(skin_c, 0.78))
    # длинные седые волосы до лопаток
    paint_hat_hair(sk, hair, [2, 3, 4, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 4, 3, 2], lambda d: 16, lambda x: 16)
    # костяная корона: зубцы над головой (внешний слой, верхние строки)
    for face in ("front", "right", "left", "back"):
        hf = sk.local("hat", face)
        for x in range(hf.width):
            hf.put(x, 2, mul(BONE, SHADE[face]))
            if x % 3 == 1:
                hf.put(x, 1, mul(BONE, SHADE[face] * 1.05)); hf.put(x, 0, mul(BONE, SHADE[face] * 1.1))
    sk.local("hat", "front").put(7, 1, (220, 20, 44)); sk.local("hat", "front").put(8, 1, (220, 20, 44))
    sk.cloth("body", ("top", "bottom") + SIDES, BLACK)
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), robe, noise=0.04)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            c = mul(robe, (1.06 - 0.12 * y / 24) * (1 + sk.rng.uniform(-0.04, 0.04)))
            if x in (6, 9):
                c = gold
            elif 6 < x < 9:
                c = mul(BLACK, 1.3)
            if y < 3 and x in (1, 2, 3, 12, 13, 14):
                c = gold
            jf.put(x, y, c)
    jb = sk.local("jacket", "back")
    for y in range(4, 20):
        for x in range(4, 12):
            if abs(x - 7.5) + abs(y - 11) < 5 and (x + y) % 2 == 0:
                jb.put(x, y, mul(gold, 0.85))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, robe, mix(skin_c, (40, 10, 20), 0.2), cuff=gold, sleeve_rows=19)
    for sleeve in ("rsleeve", "lsleeve"):
        for face in SIDES:
            fs = sk.local(sleeve, face)
            for x in range(fs.width):
                for y in range(14, 20):
                    fs.put(x, y, mul(robe, SHADE[face] * 0.9))
                fs.put(x, 19, mul(gold, SHADE[face]))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, BLACK, (10, 8, 10), (4, 3, 4), shoe_rows=6)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, robe, 22, trim=gold)
    save(sk, folder, "ancient_ghoul_0")


if __name__ == "__main__":
    out = sys.argv[1]
    deep_ghoul(out, 0)
    deep_ghoul(out, 1)
    deep_brute(out)
    deep_spitter(out)
    guardian(out, "deep_guardian_sting_0", (176, 20, 36), "fangs", 0xE8E4DE)
    guardian(out, "deep_guardian_wing_0", (224, 110, 40), "beak", 0x141014)
    ancient(out)
    print("ok")
