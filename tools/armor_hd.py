"""
Маски и одежда Tokyo Ghoul RPG в двойном разрешении (128 x 64, развёртка брони 64 x 32).
Все рисунки оригинальные. Запуск: python3 armor_hd.py <папка textures/models/armor>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import SHADE, SIDES, Skin, mul  # noqa: E402


def _armor(seed):
    from PIL import Image
    sk = Skin(seed)
    sk.img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    return sk


def save(sk, folder, name):
    os.makedirs(folder, exist_ok=True)
    sk.img.save(os.path.join(folder, name + ".png"))


# ------------------------------------------------------------------ ремешок маски
def strap(sk):
    band, hi = (26, 26, 28), (60, 60, 64)
    for face in ("right", "left", "back"):
        f = sk.local("head", face)
        for x in range(16):
            f.put(x, 7, hi)
            f.put(x, 8, band)
            f.put(x, 9, mul(band, 0.8))
    b = sk.local("head", "back")
    for y in range(6, 11):                      # пряжка на затылке
        for x in (7, 8):
            b.put(x, y, (150, 150, 156) if y in (6, 10) or x == 7 else (90, 90, 96))


def mask_base(sk, base, noise=0.04):
    """Лицевая часть маски 16 x 16 с объёмом: светлее сверху и в центре, темнее по краям."""
    f = sk.local("head", "front")
    for y in range(16):
        for x in range(16):
            edge = min(x, 15 - x, y, 15 - y)
            k = 1.08 - 0.1 * y / 15
            if edge == 0:
                k *= 0.72
            elif edge == 1:
                k *= 0.88
            if 3 <= x <= 12 and 1 <= y <= 3:
                k *= 1.06
            k *= 1 + sk.rng.uniform(-noise, noise)
            f.put(x, y, mul(base, k))
    return f


def mask_fang(folder):
    sk = _armor(11)
    base = (92, 14, 20)
    f = mask_base(sk, base)
    # шов по центру лба
    for y in range(0, 5):
        f.put(7, y, mul(base, 0.72)); f.put(8, y, mul(base, 1.12))
    # прорези глаз с красными зрачками
    for side in (0, 1):
        x0 = 2 if side == 0 else 9
        for x in range(x0, x0 + 5):
            f.put(x, 5, mul(base, 1.25))                 # верхний край прорези
        for x in range(x0, x0 + 5):
            f.put(x, 6, (8, 6, 8))
        for x in range(x0 + 1, x0 + 4):
            f.put(x, 7, (8, 6, 8))
        pupil = x0 + 2
        f.put(pupil, 6, (235, 40, 52)); f.put(pupil + (1 if side == 0 else -1), 6, (150, 18, 30))
    # пасть: тёмная щель и клыки сверху и снизу
    for y in (11, 12, 13):
        for x in range(2, 14):
            if not (y == 13 and x in (2, 13)):
                f.put(x, y, (14, 4, 6))
    tooth, tooth_shadow = (240, 234, 222), (186, 176, 160)
    for x in (2, 4, 6, 9, 11, 13):                       # верхние клыки
        f.put(x, 10, tooth); f.put(x + (1 if x < 8 else -1), 10, tooth)
        f.put(x, 11, tooth); f.put(x + (1 if x < 8 else -1), 11, tooth_shadow)
        f.put(x, 12, tooth_shadow)
    for x in (3, 5, 10, 12):                             # нижние клыки
        f.put(x, 13, tooth); f.put(x, 12, tooth_shadow)
    for x in range(2, 14):
        f.put(x, 14, mul(base, 0.7))
    strap(sk)
    save(sk, folder, "mask_fang")


def mask_bone(folder):
    sk = _armor(12)
    base = (228, 220, 198)
    f = mask_base(sk, base, noise=0.03)
    socket = (26, 20, 16)
    for side in (0, 1):
        x0 = 2 if side == 0 else 9
        for y in range(5, 10):
            for x in range(x0, x0 + 5):
                corner = (y in (5, 9)) and x in (x0, x0 + 4)
                if not corner:
                    f.put(x, y, socket)
        for x in range(x0 + 1, x0 + 4):
            f.put(x, 4, mul(base, 0.78))                 # тень надбровья
        f.put(x0 + 2, 7, (110, 20, 24))                  # тлеющая точка в глазнице
    for (x, y) in ((7, 10), (8, 10), (7, 11), (8, 11), (6, 11), (9, 11)):
        f.put(x, y, socket if y == 11 and x in (7, 8) else mul(base, 0.6))
    # зубы
    for x in range(3, 13):
        f.put(x, 12, mul(base, 0.62))
        f.put(x, 13, mul(base, 1.05) if x % 2 else mul(base, 0.86))
        f.put(x, 14, mul(base, 0.62))
    # трещины
    for (x, y) in ((11, 0), (11, 1), (12, 2), (12, 3), (13, 3), (3, 1), (4, 2), (4, 3), (14, 11), (13, 12)):
        f.put(x, y, (120, 110, 96))
    strap(sk)
    save(sk, folder, "mask_bone")


def mask_crow(folder):
    sk = _armor(13)
    base = (30, 30, 36)
    f = mask_base(sk, base, noise=0.05)
    # перья: ряды светлых дужек
    for y in range(1, 15, 3):
        for x in range((y // 3) % 2, 16, 3):
            f.put(x, y, mul(base, 1.45)); f.put(x + 1, y + 1, mul(base, 1.25))
    # глаза
    for x0 in (2, 11):
        for x in (x0, x0 + 1, x0 + 2):
            f.put(x, 5, (14, 14, 16)); f.put(x, 7, (14, 14, 16))
        f.put(x0, 6, (240, 192, 32)); f.put(x0 + 1, 6, (20, 16, 8)); f.put(x0 + 2, 6, (200, 150, 20))
    # клюв: широкий у глаз, сужается к низу; блик слева, тень справа, линия рта
    beak, beak_hi, beak_lo, edge = (140, 136, 130), (196, 190, 180), (98, 95, 90), (58, 56, 54)
    shape = {4: (6, 9), 5: (5, 10), 6: (5, 10), 7: (5, 10), 8: (5, 10), 9: (5, 10), 10: (6, 9),
             11: (6, 9), 12: (6, 9), 13: (7, 9), 14: (7, 8), 15: (7, 8)}
    for y, (a, b) in shape.items():
        for x in range(a, b + 1):
            if x == a:
                c = beak_hi
            elif x == a + 1 and b - a >= 3:
                c = mul(beak_hi, 0.92)
            elif x == b:
                c = edge
            elif x == b - 1:
                c = beak_lo
            else:
                c = beak
            f.put(x, y, c)
    for x in range(6, 10):
        f.put(x, 10, (34, 34, 38))                       # линия рта
    f.put(6, 7, beak_lo); f.put(9, 7, beak_lo)             # ноздри
    strap(sk)
    save(sk, folder, "mask_crow")


def mask_oni(folder):
    sk = _armor(14)
    base = (168, 26, 26)
    f = mask_base(sk, base)
    horn, horn_hi, horn_lo = (232, 220, 192), (250, 244, 228), (170, 156, 128)
    for (x, y, c) in ((1, 0, horn_hi), (2, 0, horn), (1, 1, horn), (2, 1, horn), (3, 1, horn_lo), (2, 2, horn), (3, 2, horn_lo),
                      (14, 0, horn), (13, 0, horn_hi), (14, 1, horn), (13, 1, horn), (12, 1, horn_lo), (13, 2, horn), (12, 2, horn_lo)):
        f.put(x, y, c)
    brow = (70, 8, 10)
    for (x, y) in ((2, 4), (3, 4), (4, 5), (5, 5), (6, 6), (13, 4), (12, 4), (11, 5), (10, 5), (9, 6)):
        f.put(x, y, brow)
    for x0 in (3, 9):
        for x in range(x0, x0 + 4):
            f.put(x, 7, (240, 200, 40))
        f.put(x0 + 1 if x0 == 3 else x0 + 2, 7, (20, 10, 6))
        for x in range(x0, x0 + 4):
            f.put(x, 8, mul(base, 0.6))
    for (x, y) in ((6, 9), (7, 9), (8, 9), (9, 9), (6, 10), (9, 10)):
        f.put(x, y, mul(base, 1.18))                     # нос
    f.put(7, 10, (60, 6, 8)); f.put(8, 10, (60, 6, 8))
    for x in range(3, 13):                               # пасть
        f.put(x, 12, (24, 4, 6)); f.put(x, 13, (24, 4, 6))
    for x in range(4, 12):
        f.put(x, 12, (236, 230, 216) if x % 2 == 0 else (200, 192, 176))
    for (x, y) in ((3, 11), (3, 12), (12, 11), (12, 12)):
        f.put(x, y, horn)                                # клыки
    for x in range(3, 13):
        f.put(x, 14, mul(base, 0.72))
    strap(sk)
    save(sk, folder, "mask_oni")


# ------------------------------------------------------------------ одежда
def fill(sk, part, faces, base, rows=None, noise=0.035, grad=0.1):
    sk.cloth(part, faces, base, noise=noise, grad=grad, rows=rows)


def ghoul_cloth(folder):
    """Плащ гуля: тёмный, с красной подкладкой по краю, стоячий воротник, пояс; штаны и ботинки."""
    coat, red, inner = (36, 36, 42), (138, 20, 26), (20, 20, 26)
    l1 = _armor(21)
    fill(l1, "body", ("top", "bottom") + SIDES, coat)
    f = l1.local("body", "front")
    for y in range(24):
        f.put(7, y, inner); f.put(8, y, inner)
        f.put(6, y, red); f.put(9, y, mul(red, 0.8))
    for y in range(0, 4):                                # воротник
        for x in list(range(2, 6)) + list(range(10, 14)):
            f.put(x, y, mul(coat, 1.18 if y == 0 else 1.05))
        f.put(5, y, red); f.put(10, y, red)
    for x in range(16):                                  # пояс
        if x not in (7, 8):
            f.put(x, 14, (22, 22, 24)); f.put(x, 15, (16, 16, 18))
    f.put(5, 14, (150, 150, 156)); f.put(5, 15, (110, 110, 116))
    for (x, y) in ((2, 9), (3, 10), (13, 9), (12, 10), (3, 19), (4, 20), (12, 19), (11, 20)):
        f.put(x, y, mul(coat, 0.84))                     # складки
    b = l1.local("body", "back")
    for x in range(16):
        b.put(x, 14, (20, 20, 22)); b.put(x, 15, (14, 14, 16))
    for y in range(16, 24):
        b.put(7, y, mul(coat, 0.62))
    # рукава: открыты кисти (нижние 4 пикселя прозрачные), красная манжета
    for face in ("top",) + SIDES:
        a = l1.local("rarm", face)
        for y in range(a.height if face == "top" else 20):
            for x in range(a.width):
                k = SHADE[face] * (1.04 - 0.1 * y / 24) * (1 + l1.rng.uniform(-0.035, 0.035))
                a.put(x, y, mul(coat, k))
        if face in SIDES:
            for x in range(a.width):
                a.put(x, 18, mul(red, SHADE[face])); a.put(x, 19, mul(red, SHADE[face] * 0.8))
            for (x, y) in ((1, 8), (2, 9), (3, 9), (4, 10)):
                a.put(x, y, mul(coat, 0.84 * SHADE[face]))
    # ботинки: нижние 8 пикселей ноги
    boot, sole, lace = (26, 26, 28), (10, 10, 10), (120, 24, 30)
    for face in SIDES:
        g = l1.local("rleg", face)
        for y in range(16, 24):
            for x in range(g.width):
                c = sole if y == 23 else mul(boot, SHADE[face] * (1.12 if y == 16 else 1.0))
                g.put(x, y, c)
        if face == "front":
            for y in (17, 19, 21):
                g.put(3, y, lace); g.put(4, y, lace)
    bottom = l1.local("rleg", "bottom")
    bottom.rect(0, 0, bottom.width, bottom.height, sole)
    save(l1, folder, "ghoul_cloth_layer_1")

    l2 = _armor(22)
    pants = (40, 40, 46)
    for face in SIDES:                                   # пояс штанов на корпусе
        w = l2.local("body", face)
        for y in range(18, 24):
            for x in range(w.width):
                w.put(x, y, mul(pants, SHADE[face] * (1 + l2.rng.uniform(-0.03, 0.03))))
        for x in range(w.width):
            w.put(x, 18, (20, 20, 22))
    fill(l2, "rleg", ("top", "bottom") + SIDES, pants)
    for face in ("right", "left"):
        g = l2.local("rleg", face)
        for y in range(24):
            g.put(4, y, mul(pants, 0.8 * SHADE[face]))
    g = l2.local("rleg", "front")
    for (x, y) in ((1, 12), (2, 13), (3, 13), (4, 14)):
        g.put(x, y, mul(pants, 0.82))
    save(l2, folder, "ghoul_cloth_layer_2")


def ccg_uniform(folder):
    """Форма CCG: белый двубортный плащ с лацканами и поясом; чёрные брюки и туфли."""
    coat, grey, button = (234, 234, 228), (160, 160, 156), (120, 120, 124)
    l1 = _armor(31)
    fill(l1, "body", ("top", "bottom") + SIDES, coat, noise=0.025)
    f = l1.local("body", "front")
    for y in range(0, 6):                                # лацканы
        f.put(5 + y // 2, y, grey); f.put(10 - y // 2, y, grey)
    for y in range(0, 3):
        for x in range(6, 10):
            f.put(x, y, (40, 40, 48))                    # тень у ворота
    for y in range(6, 24):
        f.put(9, y, mul(coat, 0.82))                     # край запаха
    for y in (7, 10, 18):                                # пуговицы в два ряда
        f.put(5, y, button); f.put(11, y, button)
    for x in range(16):                                  # пояс с пряжкой
        f.put(x, 14, mul(coat, 0.84)); f.put(x, 15, mul(coat, 0.76))
    for (x, y) in ((7, 14), (8, 14), (7, 15), (8, 15)):
        f.put(x, y, (70, 70, 76))
    for (x, y) in ((2, 19), (3, 20), (13, 19), (12, 20)):
        f.put(x, y, mul(coat, 0.86))
    b = l1.local("body", "back")
    for x in range(16):
        b.put(x, 14, mul(coat, 0.7)); b.put(x, 15, mul(coat, 0.64))
    for y in range(16, 24):
        b.put(7, y, mul(coat, 0.6))
    for face in ("top",) + SIDES:
        a = l1.local("rarm", face)
        for y in range(a.height if face == "top" else 20):
            for x in range(a.width):
                k = SHADE[face] * (1.04 - 0.08 * y / 24) * (1 + l1.rng.uniform(-0.025, 0.025))
                a.put(x, y, mul(coat, k))
        if face in SIDES:
            for x in range(a.width):
                a.put(x, 16, mul(grey, SHADE[face])); a.put(x, 17, mul(coat, 0.8 * SHADE[face]))
            a.put(2, 16, mul(button, SHADE[face]))
    shoe, sole = (18, 18, 20), (6, 6, 6)
    for face in SIDES:
        g = l1.local("rleg", face)
        for y in range(18, 24):
            for x in range(g.width):
                c = sole if y == 23 else mul(shoe, SHADE[face] * (1.2 if y == 18 else 1.0))
                g.put(x, y, c)
        if face == "front":
            g.put(2, 19, (70, 70, 76)); g.put(5, 19, (70, 70, 76))
    bottom = l1.local("rleg", "bottom")
    bottom.rect(0, 0, bottom.width, bottom.height, sole)
    save(l1, folder, "ccg_uniform_layer_1")

    l2 = _armor(32)
    pants = (30, 30, 38)
    for face in SIDES:
        w = l2.local("body", face)
        for y in range(18, 24):
            for x in range(w.width):
                w.put(x, y, mul(pants, SHADE[face] * (1 + l2.rng.uniform(-0.02, 0.02))))
        for x in range(w.width):
            w.put(x, 18, (14, 14, 16))
    w = l2.local("body", "front")
    w.put(7, 18, (150, 150, 156)); w.put(8, 18, (120, 120, 126))
    fill(l2, "rleg", ("top", "bottom") + SIDES, pants, noise=0.02)
    g = l2.local("rleg", "front")
    for y in range(0, 22):
        g.put(4, y, mul(pants, 0.78)); g.put(3, y, mul(pants, 1.12))   # стрелка
    save(l2, folder, "ccg_uniform_layer_2")


if __name__ == "__main__":
    out = sys.argv[1]
    mask_fang(out)
    mask_bone(out)
    mask_crow(out)
    mask_oni(out)
    ghoul_cloth(out)
    ccg_uniform(out)
    print("armor:", sorted(os.listdir(out)))
