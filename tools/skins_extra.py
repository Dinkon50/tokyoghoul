"""
Скины боссов и информаторов (128 x 128). Все персонажи оригинальные.
Запуск: python3 skins_extra.py <папка textures/entity>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SIDES, SHADE, Skin, coat_skirt, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_hat_hair, paint_head_rest, paint_leg, paint_pants_overlay, save)


def kakugan(sk, skin_c, iris=(235, 40, 50), dark=(150, 10, 20)):
    """Глаза гуля: чёрный белок, красная радужка с прожилками у края."""
    f = sk.local("head", "front")
    sclera = (22, 14, 16)
    for side in (0, 1):
        x0 = 3 if side == 0 else 10
        for x in range(x0, x0 + 3):
            f.put(x, 8, sclera)
            f.put(x, 9, sclera)
        irs = (x0 + 1, x0 + 2) if side == 0 else (x0, x0 + 1)
        f.put(irs[0], 8, (255, 180, 180))
        f.put(irs[1], 8, iris)
        f.put(irs[0], 9, iris)
        f.put(irs[1], 9, dark)
        # тонкие красные прожилки под глазом
        vx = x0 if side == 0 else x0 + 2
        f.put(vx, 10, mix(skin_c, (180, 30, 40), 0.55))


def ghoul_boss(folder):
    """
    «Пожиратель»: огромный гуль. Тёмно-багровые взъерошенные волосы, рваная чёрная майка,
    голые жилистые руки с бинтами на предплечьях, широкий ремень с цепью, карго и тяжёлые ботинки.
    """
    sk = Skin(909)
    skin_c, hair = 0xD9BCA6, 0x3A0C12
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((235, 40, 50), (150, 10, 20)), hair,
               [6, 7, 5, 8, 6, 4, 7, 9, 6, 5, 8, 6, 4, 7, 5, 6], mouth="grin")
    kakugan(sk, skin_c)
    paint_hat_hair(sk, hair, [4, 0, 6, 3, 0, 5, 7, 0, 4, 6, 0, 3, 7, 0, 5, 4],
                   lambda d: 7 if d < 6 else 11, lambda x: 10 + (x * 7) % 4,
                   tufts=(1, 2, 5, 6, 9, 10, 13, 14))
    # корпус: кожа с рельефом, поверх рваная майка
    tank, belt = 0x17171B, 0x2A1C14
    sk.cloth("body", ("top", "bottom") + SIDES, skin_c, noise=0.02, grad=0.06)
    bf = sk.local("body", "front")
    for y in range(2, 10):                       # грудные мышцы и пресс
        bf.put(7, y, mul(skin_c, 0.86)); bf.put(8, y, mul(skin_c, 0.86))
    for x in range(3, 13):
        bf.put(x, 9, mul(skin_c, 0.88))
    for y in (12, 15):
        for x in range(5, 11):
            bf.put(x, y, mul(skin_c, 0.9))
    # майка на внешнем слое: широкие проймы, рваный низ, дыры
    for face in ("front", "back", "right", "left", "top"):
        f = sk.local("jacket", face)
        for y in range(f.height):
            for x in range(f.width):
                if face in ("front", "back") and y < 6 and 2 < x < 13 and face == "front" and y < 3:
                    continue                      # вырез горловины
                if face in ("front", "back") and y < 4 and (x < 3 or x > 12):
                    continue                      # широкие лямки
                rag = 20 + (x * 5) % 3
                if face in SIDES and y > rag:
                    continue                      # рваный край
                k = SHADE[face] * (1.04 - 0.1 * y / 24) * (1 + sk.rng.uniform(-0.05, 0.05))
                f.put(x, y, mul(tank, k))
    jf = sk.local("jacket", "front")
    for (x, y) in ((4, 11), (5, 11), (4, 12), (11, 16), (12, 16), (12, 17), (6, 18)):
        jf.clear(x, y)                            # дыры, сквозь них видно тело
    for (x, y) in ((3, 6), (4, 7), (12, 6), (11, 7), (6, 13), (9, 14)):
        jf.put(x, y, mul(tank, 1.35))             # потёртости
    for (x, y) in ((9, 8), (10, 9), (10, 10), (9, 11)):
        jf.put(x, y, (96, 14, 20))                # пятна крови
    # ремень с цепью на корпусе
    for face in SIDES:
        f = sk.local("body", face)
        for x in range(f.width):
            f.put(x, 19, mul(belt, SHADE[face] * 1.1)); f.put(x, 20, mul(belt, SHADE[face] * 0.9))
    bf.put(7, 19, (170, 170, 176)); bf.put(8, 19, (130, 130, 136)); bf.put(7, 20, (110, 110, 116))
    for x in range(9, 15):
        bf.put(x, 21 + (x % 2), (150, 150, 158))  # цепь
    # руки: голые, вены, бинты на предплечьях
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, skin_c, skin_c, sleeve_rows=0)
        for face in SIDES:
            f = sk.local(arm, face)
            for y in range(f.height):
                for x in range(f.width):
                    k = 1.0 + (0.06 if x in (2, 3) else -0.04) * (1 if y < 10 else 0)
                    f.put(x, y, mul(skin_c, SHADE[face] * k * (1 + sk.rng.uniform(-0.02, 0.02))))
            for y in range(3, 11):               # вена
                vx = 1 + (y // 3) % 2
                f.put(vx, y, mix(mul(skin_c, SHADE[face]), (120, 30, 50), 0.35))
    for sleeve in ("rsleeve", "lsleeve"):
        for face in SIDES:
            f = sk.local(sleeve, face)
            for y in range(12, 21):
                for x in range(f.width):
                    c = (214, 206, 190) if (x + y) % 3 else (180, 172, 158)
                    f.put(x, y, mul(c, SHADE[face]))
            f.put(2, 15, (130, 30, 34)); f.put(3, 16, (110, 22, 28))
    # ноги: карго с карманами, тяжёлые ботинки
    cargo, boot = 0x2E3026, 0x18120E
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, cargo, boot, 0x080606, shoe_rows=8)
        for face in ("right", "left"):
            f = sk.local(leg, face)
            for y in range(8, 14):
                for x in range(2, 7):
                    f.put(x, y, mul(cargo, SHADE[face] * (1.12 if y == 8 else 0.92)))
        f = sk.local(leg, "front")
        for x in range(f.width):
            f.put(x, 16, mul(boot, 1.5))          # шнуровка
    for pants in ("rpants", "lpants"):
        for face in SIDES:
            f = sk.local(pants, face)
            for x in range(f.width):
                f.put(x, 15, mul(cargo, 1.15 * SHADE[face]))   # штанины заправлены в ботинки
    save(sk, folder, "ghoul_boss_0")


def ccg_boss(folder):
    """
    «Старший следователь»: женщина средних лет, тёмные волосы собраны в пучок,
    тёмно-синий плащ с серебряной окантовкой и нашивкой, белые перчатки, бордовый шейный платок.
    """
    sk = Skin(1001)
    skin_c, hair = 0xE2C0A0, 0x1C1A22
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((70, 110, 150), (40, 64, 90)), hair,
               [4, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 4, 4], mouth="stern", lashes=True)
    # пучок на затылке
    paint_hat_hair(sk, hair, [0] * 16, lambda d: 0 if d < 6 else 4, lambda x: 0)
    hb = sk.local("hat", "back")
    for y in range(4, 11):
        for x in range(5, 11):
            if (y in (4, 10)) and x in (5, 10):
                continue
            hb.put(x, y, mul(hair, SHADE["back"] * (1.15 if y == 5 else 1.0) * (1 + sk.rng.uniform(-0.04, 0.04))))
    for x in range(5, 11):
        hb.put(x, 11, (150, 150, 160))             # заколка
    coat, suit, trim, scarf = 0x1E2A44, 0x16161C, 0xC8CCD4, 0x6A1424
    shirt = 0xEDEDE8
    sk.cloth("body", ("top", "bottom") + SIDES, suit)
    bf = sk.local("body", "front")
    for y in range(10):
        for x in (6, 7, 8, 9):
            bf.put(x, y, mul(shirt, 1.0 - 0.02 * y))
    for y in range(0, 5):
        for x in (6, 7, 8, 9):
            bf.put(x, y, mul(scarf, 1.1 - 0.05 * y))
    bf.put(7, 5, scarf); bf.put(8, 5, mul(scarf, 0.8)); bf.put(7, 6, mul(scarf, 0.8))
    # плащ: застёгнут до пояса, ниже распахнут, серебряная окантовка
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), coat, noise=0.025)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            open_part = (5 < x < 10 and y < 6) or (6 < x < 9 and y > 15)
            if open_part:
                continue
            c = mul(coat, (1.04 - 0.08 * y / 24) * (1 + sk.rng.uniform(-0.025, 0.025)))
            if (x in (5, 10) and y < 6) or (x in (6, 9) and y > 15):
                c = trim                            # окантовка лацканов и пол
            if y < 4 and x in (4, 11):
                c = mul(trim, 0.85)                 # воротник-стойка
            jf.put(x, y, c)
    for y in range(6, 16):
        jf.put(7, y, mul(coat, 0.72)); jf.put(8, y, mul(coat, 1.12))
    for y in (7, 10, 13):
        jf.put(6, y, trim); jf.put(9, y, trim)      # серебряные пуговицы в два ряда
    for x in range(16):
        if not (6 < x < 9):
            jf.put(x, 15, mul(coat, 0.7))           # пояс
    jf.put(7, 15, trim); jf.put(8, 15, trim)
    jb = sk.local("jacket", "back")
    for x in range(16):
        jb.put(x, 15, mul(coat, 0.62))
    for y in range(16, 24):
        jb.put(7, y, mul(coat, 0.58))
    # нашивка на правом плече: белый щит с синим крестом-стрелкой (оригинальная эмблема)
    for sleeve in ("rsleeve", "lsleeve"):
        sk.cloth(sleeve, ("top",) + SIDES, coat, noise=0.025, rows=(0, 19))
        for face in SIDES:
            f = sk.local(sleeve, face)
            for x in range(f.width):
                f.put(x, 18, mul(trim, SHADE[face]))
    rs = sk.local("rsleeve", "right")
    for y in range(2, 7):
        for x in range(1, 7):
            if y == 6 and x in (1, 6):
                continue
            rs.put(x, y, (232, 232, 236))
    for y in range(3, 6):
        rs.put(3, y, (30, 60, 120)); rs.put(4, y, (30, 60, 120))
    rs.put(2, 4, (30, 60, 120)); rs.put(5, 4, (30, 60, 120))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, suit, (238, 238, 240), cuff=trim, sleeve_rows=19)   # белые перчатки
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, suit, 0x0E0E12, 0x050505, shoe_rows=7)
        f = sk.local(leg, "front")
        for y in range(0, 17):
            f.put(4, y, mul(suit, 0.8))
    for pants in ("rpants", "lpants"):
        coat_skirt(sk, pants, coat, 14, trim=trim)
    save(sk, folder, "ccg_boss_0")


def informant_bomber(folder):
    """Информатор: парень в шапке-бини, горчичный бомбер поверх серой толстовки, наушники на шее."""
    sk = Skin(1111)
    skin_c, hair = 0xEACDB4, 0x2A2420
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((110, 80, 50), (60, 42, 26)), hair,
               [3, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 3], mouth="smile")
    beanie = 0x2E3440
    sk.cloth("hat", ("top", "back", "right", "left"), beanie, noise=0.04)
    for face in ("back", "right", "left"):
        f = sk.local("hat", face)
        for y in range(8, 16):
            for x in range(16):
                f.clear(x, y)
        for x in range(16):
            f.put(x, 6, mul(beanie, 0.8 * SHADE[face])); f.put(x, 7, mul(beanie, 0.75 * SHADE[face]))
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(4):
            hf.put(x, y, mul(beanie, (1.05 if y < 2 else 0.85) * (1 + sk.rng.uniform(-0.04, 0.04))))
        if x % 2 == 0:
            hf.put(x, 3, mul(beanie, 0.7))           # вязаные рубчики
    hood, bomber = 0x6E6E74, 0xC49A2C
    sk.cloth("body", ("top", "bottom") + SIDES, hood)
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), bomber, noise=0.04)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            if x < 6 or x > 9:
                c = mul(bomber, (1.04 - 0.08 * y / 24) * (1 + sk.rng.uniform(-0.04, 0.04)))
                if x in (5, 10):
                    c = (170, 170, 176)              # молнии
                jf.put(x, y, c)
    for face in ("front", "back", "right", "left"):
        f = sk.local("jacket", face)
        for x in range(f.width):
            if face == "front" and 5 < x < 10:
                continue
            f.put(x, 22, mul(0x1E1E22, 1.1)); f.put(x, 23, (30, 30, 34))     # резинка внизу
    for x in range(16):                                                       # воротник-резинка
        jf.put(x, 0, (36, 36, 40)) if (x < 6 or x > 9) else None
    for x, y in ((2, 9), (3, 9), (2, 10), (3, 10)):
        jf.put(x, y, mul(bomber, 0.78))              # карман на рукаве
    # наушники на шее
    bf = sk.local("body", "front")
    for x in range(4, 12):
        bf.put(x, 1, (24, 24, 28))
    for y in (1, 2, 3):
        bf.put(4, y, (24, 24, 28)); bf.put(11, y, (24, 24, 28))
    bf.put(4, 3, (200, 50, 50)); bf.put(11, 3, (200, 50, 50))
    for y in range(4, 20):
        bf.put(7, y, (220, 220, 220) if y < 9 else mul(hood, 1.0))           # шнурки толстовки
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, bomber, skin_c, cuff=(36, 36, 40), sleeve_rows=20)
        for face in SIDES:
            f = sk.local(sleeve, face)
            for x in range(f.width):
                f.put(x, 4, mul(bomber, 1.12 * SHADE[face]))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x1F2636, 0xE8E6E0, 0x8A8A88, shoe_rows=4)
        f = sk.local(leg, "front")
        f.put(3, 21, (200, 50, 50)); f.put(4, 21, (200, 50, 50))           # полоска на кедах
    for pants in ("rpants", "lpants"):
        paint_pants_overlay(sk, pants, 0x1F2636, 18)
    save(sk, folder, "informant_0")


def informant_trench(folder):
    """Информатор: женщина в очках, каштановое каре, бежевый тренч, бордовый шарф, высокие сапоги."""
    sk = Skin(1212)
    skin_c, hair = 0xF2D8C4, 0x5A3020
    paint_head_rest(sk, skin_c, hair, "bob")
    paint_face(sk, skin_c, ((70, 110, 80), (40, 66, 46)), hair,
               [5, 5, 4, 4, 4, 3, 3, 3, 3, 4, 5, 6, 6, 6, 7, 7], mouth="smile",
               lashes=True, glasses=(60, 36, 30))
    paint_hat_hair(sk, hair, [5, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 7, 8, 8, 8],
                   lambda d: 13 if d < 3 else 15, lambda x: 15)
    trench, scarf, belt = 0xC2A57A, 0x6E1C2A, 0x7A5A3A
    sk.cloth("body", ("top", "bottom") + SIDES, 0x2A2A30)
    sk.cloth("jacket", ("top", "bottom") + SIDES, trench, noise=0.03)
    jf = sk.local("jacket", "front")
    for y in range(24):
        jf.put(9, y, mul(trench, 0.74))             # двубортный запах
    for y in (7, 11):
        for x in (5, 11):
            jf.put(x, y, (70, 50, 36))              # пуговицы
    for x in range(16):
        for y in range(4):
            jf.put(x, y, mul(scarf, (1.1 if y == 0 else 1.0 - 0.05 * y) * (1 + sk.rng.uniform(-0.04, 0.04))))
    jf.rect(10, 4, 13, 12, scarf)
    for y in range(4, 12):
        jf.put(12, y, mul(scarf, 0.8))
    jf.put(10, 11, mul(scarf, 0.7)); jf.put(11, 11, mul(scarf, 0.7))
    for face in SIDES:
        f = sk.local("jacket", face)
        for x in range(f.width):
            f.put(x, 14, mul(belt, SHADE[face])); f.put(x, 15, mul(belt, SHADE[face] * 0.85))
    jf.put(7, 14, (190, 170, 110)); jf.put(7, 15, (160, 140, 90))   # пряжка
    for x in range(3, 13):                           # погончики и кокетка
        jf.put(x, 5, mul(trench, 0.88)) if x not in range(10, 13) else None
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, trench, skin_c, cuff=mul(trench, 0.78), sleeve_rows=19)
        for face in SIDES:
            f = sk.local(sleeve, face)
            for x in range(f.width):
                f.put(x, 16, mul(belt, 0.9 * SHADE[face]))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x2A2A30, 0x2A1C16, 0x0E0806, shoe_rows=12)
    for pants in ("rpants", "lpants"):
        coat_skirt(sk, pants, trench, 13, trim=mul(trench, 0.82))
    save(sk, folder, "informant_1")


if __name__ == "__main__":
    out = sys.argv[1]
    ghoul_boss(out)
    ccg_boss(out)
    informant_bomber(out)
    informant_trench(out)
    print("ok")
