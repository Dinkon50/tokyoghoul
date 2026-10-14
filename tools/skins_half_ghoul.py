"""
Скины полугулей (свита) и Кровавого полугуля (босс), 128 x 128. Персонажи оригинальные:
у полугулей один глаз-какуган, у босса белые волосы с багровыми кончиками и вены на половине лица.
Запуск: python3 skins_half_ghoul.py <папка textures/entity>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SIDES, SHADE, Skin, coat_skirt, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_hat_hair, paint_head_rest, paint_leg, paint_pants_overlay, save)


def kakugan_one(sk, skin_c, side, iris=(240, 36, 48), dark=(150, 8, 20)):
    """Один глаз гуля: чёрный белок, красная радужка, прожилки под глазом. side 0 — левый на текстуре."""
    f = sk.local("head", "front")
    x0 = 3 if side == 0 else 10
    for x in range(x0, x0 + 3):
        f.put(x, 8, (22, 14, 16))
        f.put(x, 9, (22, 14, 16))
    irs = (x0 + 1, x0 + 2) if side == 0 else (x0, x0 + 1)
    f.put(irs[0], 8, (255, 190, 190))
    f.put(irs[1], 8, iris)
    f.put(irs[0], 9, iris)
    f.put(irs[1], 9, dark)
    vx = x0 if side == 0 else x0 + 2
    f.put(vx, 10, mix(skin_c, (180, 30, 40), 0.6))
    f.put(vx, 11, mix(skin_c, (180, 30, 40), 0.35))


def half_ghoul_turtleneck(folder):
    """Полугуль: седой парень, белая полумаска на правой половине лица, чёрная водолазка, бинт на руке."""
    sk = Skin(4101)
    skin_c, hair = 0xE9D6C6, 0xD4D4DA
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((120, 120, 130), (70, 70, 80)), hair,
               [4, 5, 6, 5, 4, 5, 6, 5, 4, 3, 4, 5, 6, 5, 4, 4], mouth="stern")
    kakugan_one(sk, skin_c, 1)
    paint_hat_hair(sk, hair, [3, 0, 4, 2, 0, 3, 0, 2, 0, 3, 0, 4, 2, 0, 3, 2],
                   lambda d: 7 if d < 6 else 10, lambda x: 10 + (x * 5) % 3)
    # полумаска на внешнем слое: левая половина текстуры (правая сторона лица), красная полоса посередине
    hf = sk.local("hat", "front")
    for y in range(6, 15):
        for x in range(1, 8):
            c = (232, 230, 226) if x != 4 else (170, 20, 34)
            if (x, y) in ((3, 8), (4, 8), (5, 8), (3, 9), (5, 9)):
                hf.clear(x, y)                       # прорезь для глаза
                continue
            hf.put(x, y, mul(c, 1.0 - 0.02 * (y - 6)))
    for y in (12, 13):
        for x in (2, 3, 5, 6):
            hf.put(x, y, (40, 36, 40))               # «зубы» на маске
    neck, pants = 0x141418, 0x2A2C32
    sk.cloth("body", ("top", "bottom") + SIDES, neck)
    bf = sk.local("body", "front")
    for x in range(16):
        bf.put(x, 0, mul(neck, 1.3)); bf.put(x, 1, mul(neck, 1.15))     # высокий ворот
    for (x, y) in ((4, 9), (5, 10), (11, 9), (10, 10), (7, 16), (8, 17)):
        bf.put(x, y, mul(neck, 1.4))                                     # складки
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, neck, skin_c, cuff=mul(neck, 1.3), sleeve_rows=19)
    for face in SIDES:                                                   # бинт с пятнами крови на левой руке
        f = sk.local("lsleeve", face)
        for y in range(10, 18):
            for x in range(f.width):
                f.put(x, y, mul((222, 216, 204) if (x + y) % 3 else (190, 182, 168), SHADE[face]))
        f.put(1, 13, (140, 20, 30)); f.put(2, 14, (120, 16, 26))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, 0x0E0E10, 0x060606, shoe_rows=6)
    for p in ("rpants", "lpants"):
        paint_pants_overlay(sk, p, pants, 17)
    save(sk, folder, "half_ghoul_0")


def half_ghoul_trench(folder):
    """Полугуль: девушка, тёмно-вишнёвое каре, серый плащ с чёрным поясом, красный шарф, один какуган."""
    sk = Skin(4202)
    skin_c, hair = 0xF0D8C8, 0x4A1018
    paint_head_rest(sk, skin_c, hair, "bob")
    paint_face(sk, skin_c, ((90, 70, 60), (52, 40, 34)), hair,
               [6, 6, 5, 5, 4, 3, 3, 3, 3, 3, 4, 5, 6, 6, 7, 7], mouth="calm", lashes=True)
    kakugan_one(sk, skin_c, 0)
    paint_hat_hair(sk, hair, [6, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 8, 8, 8],
                   lambda d: 13 if d < 3 else 15, lambda x: 15)
    coat, belt, scarf = 0x6A6C72, 0x16161A, 0x9A1424
    sk.cloth("body", ("top", "bottom") + SIDES, 0x1E1E24)
    sk.cloth("jacket", ("top", "bottom") + SIDES, coat, noise=0.03)
    jf = sk.local("jacket", "front")
    for y in range(24):
        jf.put(8, y, mul(coat, 0.74))
    for x in range(16):
        for y in range(4):
            jf.put(x, y, mul(scarf, (1.1 if y == 0 else 1.0 - 0.05 * y) * (1 + sk.rng.uniform(-0.04, 0.04))))
    jf.rect(3, 4, 6, 11, scarf)                                          # конец шарфа
    for face in SIDES:
        f = sk.local("jacket", face)
        for x in range(f.width):
            f.put(x, 14, mul(belt, SHADE[face])); f.put(x, 15, mul(belt, SHADE[face] * 0.85))
    jf.put(8, 14, (170, 170, 176)); jf.put(8, 15, (130, 130, 136))     # пряжка
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, coat, skin_c, cuff=mul(coat, 0.78), sleeve_rows=19)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x18181C, 0x221416, 0x0A0606, shoe_rows=11)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, coat, 12, trim=mul(coat, 0.8))
    save(sk, folder, "half_ghoul_1")


def half_ghoul_boss(folder):
    """
    «Кровавый полугуль»: длинные белые волосы с багровыми кончиками, вены на левой половине лица,
    один какуган. Чёрный плащ с высоким воротом и багровой подкладкой, серебряные застёжки,
    тёмно-красные «наручи» из кагуне на кистях, высокие сапоги.
    """
    sk = Skin(4303)
    skin_c, hair = 0xEDE2DA, 0xE8E8EE
    tips = (150, 16, 30)
    paint_head_rest(sk, skin_c, hair, "long")
    paint_face(sk, skin_c, ((150, 150, 160), (90, 90, 100)), hair,
               [5, 6, 7, 6, 5, 4, 3, 2, 2, 3, 4, 5, 6, 7, 7, 6], mouth="stern")
    kakugan_one(sk, skin_c, 1, iris=(255, 40, 60))
    f = sk.local("head", "front")
    for (x, y) in ((11, 11), (12, 12), (13, 11), (12, 13), (13, 14), (14, 12), (11, 6), (12, 5), (13, 6)):
        f.put(x, y, mix(skin_c, (170, 20, 40), 0.7))                   # вены от глаза-какугана
    paint_hat_hair(sk, hair, [5, 7, 0, 4, 0, 3, 0, 0, 0, 0, 3, 0, 4, 0, 7, 6],
                   lambda d: 16, lambda x: 16)
    # багровые кончики длинных прядей сзади и по бокам
    for part in ("head", "hat"):
        for face in ("back", "right", "left"):
            fc = sk.local(part, face)
            for x in range(16):
                for y in (13, 14, 15):
                    px = fc.get(x, y)
                    if px[3] > 0 and (part == "hat" or face == "back"):
                        fc.put(x, y, mix(px[:3], tips, 0.55 + 0.15 * (y - 13)))
    coat, lining, clasp, gaunt = 0x0E0C12, 0x7A0A1C, 0xC8CCD4, 0x5A0812
    sk.cloth("body", ("top", "bottom") + SIDES, 0x1A1418)
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), coat, noise=0.025)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            if 6 < x < 9 and y > 12:
                jf.put(x, y, mul(lining, 1.0 - 0.02 * y))              # распахнутые полы с багровой подкладкой
                continue
            c = mul(coat, (1.06 - 0.08 * y / 24) * (1 + sk.rng.uniform(-0.03, 0.03)))
            if y < 4 and x in (2, 3, 12, 13):
                c = mul(lining, 0.9)                                    # высокий ворот изнутри
            jf.put(x, y, c)
    for y in (5, 8, 11):
        jf.put(6, y, clasp); jf.put(9, y, clasp)
        jf.put(7, y, mul(clasp, 0.7)); jf.put(8, y, mul(clasp, 0.7))  # серебряные застёжки-цепочки
    jb = sk.local("jacket", "back")
    for y in range(4, 22):
        jb.put(7, y, mul(lining, 0.55)); jb.put(8, y, mul(lining, 0.55))
    for sleeve in ("rsleeve", "lsleeve"):
        sk.cloth(sleeve, ("top",) + SIDES, coat, noise=0.025, rows=(0, 17))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, coat, gaunt, cuff=lining, sleeve_rows=17)
        for face in SIDES:
            fa = sk.local(arm, face)
            for y in range(17, 24):
                for x in range(fa.width):
                    fa.put(x, y, mul(gaunt, SHADE[face] * (1.25 if (x + y) % 4 == 0 else 1.0)))   # наручи-кагуне
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x121016, 0x0A080C, 0x050405, shoe_rows=12)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, coat, 16, trim=lining)
    save(sk, folder, "half_ghoul_boss_0")


if __name__ == "__main__":
    out = sys.argv[1]
    half_ghoul_turtleneck(out)
    half_ghoul_trench(out)
    half_ghoul_boss(out)
    print("ok")
