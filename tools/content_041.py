"""
Новый контент 0.41.0 (всё оригинальное):
  скины — подопытный (2 варианта) и Скупщик с чёрного рынка;
  иконки — аптечка, дымовая шашка, записи лаборатории.
Запуск: python3 content_041.py <папка assets/tokyoghoulrpg/textures>
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SIDES, SHADE, Skin, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_hat_hair, paint_head_rest, paint_leg, paint_pants_overlay, save)
from skins_extra import kakugan  # noqa: E402

RC = (60, 220, 200)


def bandage(f, rows, cols, c=(222, 216, 204)):
    for y in rows:
        for x in cols:
            f.put(x, y, mul(c, 0.92 if (x + y) % 3 == 0 else 1.0))


def strap(sk, part, y, color=(70, 46, 30), buckle=(180, 180, 186)):
    for face in SIDES:
        f = sk.local(part, face)
        for x in range(f.width):
            f.put(x, y, mul(color, SHADE[face]))
            f.put(x, y + 1, mul(color, SHADE[face] * 0.85))
        if face == "front":
            f.put(f.width // 2, y, buckle)
            f.put(f.width // 2, y + 1, mul(buckle, 0.8))


def one_kakugan(sk, skin_c):
    """Только левый (на лице справа) глаз — глаз гуля, второй человеческий, мутный."""
    f = sk.local("head", "front")
    sclera = (22, 14, 16)
    for x in range(10, 13):
        f.put(x, 8, sclera)
        f.put(x, 9, sclera)
    f.put(10, 8, (235, 40, 50)); f.put(11, 8, (255, 180, 180)); f.put(10, 9, (150, 10, 20)); f.put(11, 9, (235, 40, 50))
    f.put(12, 10, mix(skin_c, (180, 30, 40), 0.6))
    f.put(13, 11, mix(skin_c, (180, 30, 40), 0.4))


def subject_gown(folder):
    """Подопытный: бритая серая голова, бледная кожа, один глаз гуля, бинты и капельница-катетер,
    больничная рубашка цвета мятой бирюзы, ремни через грудь, босые ноги."""
    sk = Skin(5101)
    skin_c, hair = (214, 206, 200), (150, 150, 152)
    paint_head_rest(sk, skin_c, hair, "buzz")
    paint_face(sk, skin_c, ((120, 120, 118), (80, 80, 80)), hair,
               [1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1], brows=False, mouth="stern")
    one_kakugan(sk, skin_c)
    f = sk.local("head", "front")
    bandage(f, (3, 4), range(2, 9))                     # бинт через лоб
    for y in range(9, 14):                              # шов на щеке
        f.put(3, y, mix(skin_c, (120, 40, 50), 0.5))
    side = sk.local("head", "right")
    for x in (4, 5, 6):                                 # номер на затылке-виске
        side.put(x, 4, (40, 40, 44))
    gown = (150, 186, 180)
    sk.whole("body", gown, noise=0.05)
    bf = sk.local("body", "front")
    for y in range(0, 4):
        for x in range(5, 11):
            if y < 3 - abs(x - 7.5) // 2:
                bf.put(x, y, skin_c)                    # вырез
    for y in range(4, 24, 5):
        bf.put(8, y, mul(gown, 0.7))                    # завязки
    # номер подопытного на груди
    for x in (3, 4, 5):
        bf.put(x, 6, (50, 50, 54)); bf.put(x, 8, (50, 50, 54))
    bf.put(3, 7, (50, 50, 54)); bf.put(5, 7, (50, 50, 54))
    # кровь и RC на подоле
    for (x, y) in ((11, 18), (12, 19), (11, 20), (4, 21), (5, 22)):
        bf.put(x, y, (120, 20, 28))
    for (x, y) in ((13, 15), (2, 17)):
        bf.put(x, y, RC)
    strap(sk, "jacket", 8)
    strap(sk, "jacket", 15)
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, gown, skin_c, sleeve_rows=6)
        a = sk.local(arm, "front")
        bandage(a, range(12, 18), range(0, 8))
    a = sk.local("rarm", "front")
    for y in range(9, 12):
        a.put(3, y, (200, 200, 210))                    # катетер
    a.put(3, 12, RC)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, gown, skin_c, mul(skin_c, 0.8), shoe_rows=3, seam=False)
        strap(sk, "rpants" if leg == "rleg" else "lpants", 18)
    save(sk, os.path.join(folder, "entity"), "test_subject_0")


def subject_jacket(folder):
    """Подопытный в смирительной куртке: растрёпанные тёмные волосы, оба глаза гуля,
    металлический намордник, белая куртка с ремнями и пряжками, серые штаны, босиком."""
    sk = Skin(5202)
    skin_c, hair = (206, 196, 192), (36, 32, 36)
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((235, 40, 50), (150, 10, 20)), hair,
               [5, 7, 4, 8, 6, 3, 7, 5, 8, 4, 6, 7, 3, 8, 5, 6], brows=True, mouth="stern")
    kakugan(sk, skin_c)
    f = sk.local("head", "front")
    metal = (128, 132, 140)
    for y in range(11, 16):                             # намордник-решётка
        for x in range(3, 13):
            if y in (11, 15) or x in (3, 12) or x % 2 == 0:
                f.put(x, y, mul(metal, 1.0 - 0.05 * (y - 11)))
    for face in ("right", "left"):
        s = sk.local("head", face)
        for x in range(16):
            s.put(x, 12, mul((70, 46, 30), 0.85))       # ремень намордника
    paint_hat_hair(sk, hair, [5, 7, 4, 8, 6, 3, 7, 5, 8, 4, 6, 7, 3, 8, 5, 6],
                   lambda d: 8 if d < 6 else 11, lambda x: 12 + (x * 5) % 3, tufts=(2, 5, 9, 12))
    white = (226, 224, 218)
    sk.whole("body", white, noise=0.04)
    sk.whole("jacket", white, noise=0.04)
    for y in (6, 12, 18):
        strap(sk, "jacket", y)
    # «скрещённые» рукава: ремни на руках и рукава до кистей
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, white, skin_c, cuff=mul(white, 0.85), sleeve_rows=22)
        strap(sk, sleeve, 9)
        strap(sk, sleeve, 17)
    bf = sk.local("body", "front")
    for (x, y) in ((6, 10), (7, 11), (9, 14), (10, 15), (5, 19)):
        bf.put(x, y, (130, 22, 30))                     # пятна крови
    bf.put(12, 8, RC); bf.put(3, 16, RC)
    pants = (96, 98, 104)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, skin_c, mul(skin_c, 0.8), shoe_rows=3)
    for p in ("rpants", "lpants"):
        paint_pants_overlay(sk, p, pants, 18)
    save(sk, os.path.join(folder, "entity"), "test_subject_1")


def fence(folder):
    """Скупщик: мужчина средних лет, зачёсанные назад тёмные волосы с проседью, тонкие усы,
    золотая серьга, длинное тёмно-коричневое пальто с меховым воротником, золотая цепь, серый шарф."""
    sk = Skin(6303)
    skin_c, hair = (222, 190, 160), (44, 36, 32)
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((96, 70, 40), (60, 44, 26)), hair,
               [3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3], mouth="smile")
    f = sk.local("head", "front")
    for x in range(5, 11):
        f.put(x, 12, mul(hair, 1.1))                    # тонкие усы
    for x in (4, 11):
        f.put(x, 13, mul(hair, 1.1))
    for x in (2, 6, 10, 13):
        f.put(x, 1, (130, 128, 126))                    # проседь
    side = sk.local("head", "left")
    side.put(7, 11, (230, 180, 60)); side.put(7, 12, (200, 150, 40))   # серьга
    paint_hat_hair(sk, hair, [2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2],
                   lambda d: 6, lambda x: 9)
    coat, fur, scarf, shirt = (74, 50, 34), (150, 128, 100), (120, 118, 122), (36, 30, 40)
    sk.whole("body", coat)
    bf = sk.local("body", "front")
    for y in range(0, 24):
        for x in (7, 8):
            bf.put(x, y, shirt)                         # рубашка в распахнутом пальто
    for y in range(2, 14):
        bf.put(6, y, (210, 170, 60) if y % 2 == 0 else (170, 130, 40))   # золотая цепь
    for y in range(0, 4):
        for x in range(2, 14):
            bf.put(x, y, mul(scarf, 1.0 - 0.04 * y))    # шарф
    for y in range(4, 12):
        bf.put(10, y, mul(scarf, 0.9)); bf.put(11, y, mul(scarf, 0.85))
    jf = sk.local("jacket", "front")
    for x in range(16):                                 # меховой воротник
        for y in (0, 1, 2):
            if x < 5 or x > 10:
                jf.put(x, y, mul(fur, 1.0 + 0.08 * ((x + y) % 2)))
    for face in ("back", "right", "left"):
        jf2 = sk.local("jacket", face)
        for x in range(jf2.width):
            for y in (0, 1, 2):
                jf2.put(x, y, mul(fur, SHADE[face] * (1.0 + 0.08 * ((x + y) % 2))))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, coat, skin_c, cuff=fur, sleeve_rows=20)
    a = sk.local("larm", "front")
    a.put(3, 21, (230, 180, 60))                        # перстень
    pants, boots = (40, 38, 44), (30, 22, 18)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, boots, (14, 10, 8), shoe_rows=6)
    from skins_hd import coat_skirt
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, coat, 14, trim=mul(coat, 0.8))
    save(sk, os.path.join(folder, "entity"), "fence_0")


# ------------------------------------------------------------------ иконки 16 x 16
def icon():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def medkit():
    img = icon()
    body, dark, cross = (226, 226, 222), (150, 150, 150), (200, 30, 36)
    for y in range(5, 14):
        for x in range(2, 14):
            c = body if (x not in (2, 13) and y not in (5, 13)) else dark
            img.putpixel((x, y), c + (255,))
    for x in range(6, 10):                              # ручка
        img.putpixel((x, 3), dark + (255,))
    img.putpixel((6, 4), dark + (255,)); img.putpixel((9, 4), dark + (255,))
    for i in range(7, 12):
        img.putpixel((7, i), cross + (255,)); img.putpixel((8, i), cross + (255,))
    for i in range(5, 11):
        img.putpixel((i, 9), cross + (255,)); img.putpixel((i, 10), cross + (255,))
    img.putpixel((3, 6), (250, 250, 250, 255))
    return img


def smoke_bomb():
    img = icon()
    can, dark = (90, 96, 92), (52, 56, 54)
    for y in range(6, 15):
        for x in range(4, 11):
            c = mul(can, 1.1 if x == 5 else (0.85 if x == 10 else 1.0))
            img.putpixel((x, y), c + (255,))
    for x in range(4, 11):
        img.putpixel((x, 9), (200, 170, 40, 255))      # жёлтая полоса
        img.putpixel((x, 14), dark + (255,))
    for x in (6, 7, 8):
        img.putpixel((x, 5), dark + (255,))             # колпачок
    for (x, y) in ((10, 4), (11, 4), (12, 5), (11, 3)):
        img.putpixel((x, y), (180, 180, 180, 255))      # кольцо
    for (x, y, a) in ((8, 2, 200), (9, 1, 160), (6, 3, 180), (10, 0, 120), (5, 1, 140)):
        img.putpixel((x, y), (210, 210, 214, a))        # дымок
    return img


def lab_notes():
    img = icon()
    paper, line, clip = (232, 226, 206), (140, 150, 170), (150, 154, 160)
    for y in range(2, 15):
        for x in range(3, 13):
            img.putpixel((x, y), mul(paper, 0.92 if x == 12 or y == 14 else 1.0) + (255,))
    for y in (6, 8, 10, 12):
        for x in range(5, 11 if y != 12 else 9):
            img.putpixel((x, y), line + (255,))
    for x in (6, 7, 8, 9):
        img.putpixel((x, 1), clip + (255,)); img.putpixel((x, 2), mul(clip, 0.8) + (255,))
    img.putpixel((10, 11), (150, 24, 30, 255)); img.putpixel((11, 12), (150, 24, 30, 255))   # пятно крови
    img.putpixel((5, 4), RC + (255,)); img.putpixel((6, 4), RC + (255,))                      # бирюзовый штамп
    return img


if __name__ == "__main__":
    out = sys.argv[1]
    subject_gown(out)
    subject_jacket(out)
    fence(out)
    medkit().save(os.path.join(out, "item", "medkit.png"))
    smoke_bomb().save(os.path.join(out, "item", "smoke_bomb.png"))
    lab_notes().save(os.path.join(out, "item", "lab_notes.png"))
    print("ok")
