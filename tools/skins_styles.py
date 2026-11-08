"""
Скины стилей боя (128 x 128). Персонажи оригинальные.
  dojo_master_0  — Мастер додзё (босс стиля «Боец»)
  style_mentor_0 — наставник додзё
  clown_0        — Клоун из Пьеро (босс стиля «Тень»), им же нарисованы его двойники
  white_reaper_0 — Белый жнец (босс стиля «Жнец»)
  shadow_decoy_0 — приманка «Подмены» (тёмная фигура из дыма)
Запуск: python3 skins_styles.py <папка textures/entity>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SIDES, SHADE, Skin, coat_skirt, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_hat_hair, paint_head_rest, paint_leg, save)


def gi_jacket(sk, cloth, v_depth, v_open=2, belt=None, belt_rows=(14, 15), tails=True, trim=None):
    """
    Кимоно бойца на внешнем слое корпуса: запах с V-вырезом (тело видно в вырезе), пояс-оби с узлом.
    v_depth — до какой строки идёт вырез, v_open — ширина выреза наверху (в каждую сторону от центра).
    """
    for face in ("front", "back", "right", "left", "top", "bottom"):
        f = sk.local("jacket", face)
        for y in range(f.height):
            for x in range(f.width):
                if face == "front" and y < v_depth:
                    half = v_open * (v_depth - y) / v_depth + 0.5
                    if abs(x - 7.5) < half:
                        continue
                k = SHADE[face] * (1.04 - 0.08 * y / max(1, f.height)) * (1 + sk.rng.uniform(-0.03, 0.03))
                f.put(x, y, mul(cloth, k))
    jf = sk.local("jacket", "front")
    # край запаха: левая пола поверх правой, светлая окантовка
    for y in range(0, 14):
        x = int(7.5 - (v_open * (v_depth - y) / v_depth + 0.5)) if y < v_depth else 7
        jf.put(x, y, trim or mul(cloth, 1.35))
    for y in range(16, 24):
        jf.put(7, y, mul(cloth, 0.7))
    if belt is not None:
        for face in SIDES:
            f = sk.local("jacket", face)
            for x in range(f.width):
                for y in belt_rows:
                    f.put(x, y, mul(belt, SHADE[face] * (1.1 if y == belt_rows[0] else 0.9)))
        jf.rect(6, belt_rows[0] - 1, 10, belt_rows[1] + 2, mul(belt, 1.2))      # узел
        jf.put(7, belt_rows[0], mul(belt, 0.7)); jf.put(8, belt_rows[0] + 1, mul(belt, 0.7))
        if tails:
            for y in range(belt_rows[1] + 2, belt_rows[1] + 7):
                jf.put(6, y, mul(belt, 1.1 - 0.04 * y)); jf.put(9, y + (1 if y % 2 else 0) - 1, mul(belt, 1.0))


def dojo_master(folder):
    """
    Мастер додзё: крепкий загорелый мужчина лет сорока. Чёрные волосы собраны в короткий пучок,
    тёмно-красная повязка на лбу с концами сзади, шрам через левую бровь. Тёмное поношенное кимоно
    с короткими рукавами, чёрный пояс, бинты на предплечьях и кулаках, широкие штаны, босые ноги.
    """
    sk = Skin(4242)
    skin_c, hair = 0xB98058, 0x14110F
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((168, 120, 40), (110, 70, 20)), hair,
               [2, 3, 3, 2, 2, 1, 1, 1, 1, 1, 1, 2, 2, 3, 3, 2], mouth="stern")
    f = sk.local("head", "front")
    for (x, y) in ((11, 4), (11, 5), (12, 6), (12, 9), (13, 10)):
        f.put(x, y, mix(skin_c, (150, 70, 70), 0.55))           # шрам через бровь
    for x in (3, 4, 5, 10, 11, 12):
        f.put(x, 5, mul(hair, 1.4))                              # густые брови
    f.put(10, 6, mul(skin_c, 0.8)); f.put(5, 6, mul(skin_c, 0.8))
    for x in range(5, 11):
        f.put(x, 14, mul(skin_c, 0.85))                          # тяжёлый подбородок, щетина
        if x % 2:
            f.put(x, 13, mix(skin_c, hair, 0.25))
    # объём волос и пучок на затылке
    paint_hat_hair(sk, hair, [0] * 16, lambda d: 0 if d < 5 else 3, lambda x: 0)
    hb = sk.local("hat", "back")
    for y in range(1, 6):
        for x in range(6, 10):
            hb.put(x, y, mul(hair, SHADE["back"] * (1.2 if y == 1 else 1.0)))
    top = sk.local("hat", "top")
    for y in range(0, 4):
        for x in range(6, 10):
            top.put(x, y, mul(hair, SHADE["top"] * (1.1 if x in (7, 8) else 0.9)))
    # повязка на лбу: полоса по кругу шапочного слоя, концы свисают сзади
    band = 0x7A1418
    for face in ("front", "right", "left", "back"):
        hf = sk.local("hat", face)
        for x in range(16):
            for y in (3, 4):
                hf.put(x, y, mul(band, SHADE[face] * (1.1 if y == 3 else 0.9) * (1 + sk.rng.uniform(-0.04, 0.04))))
    hb = sk.local("hat", "back")
    for y in range(5, 13):
        hb.put(6 + (y % 2), y, mul(band, 0.85))
        hb.put(9 - (y % 2), y + 1, mul(band, 0.75))
    # тело: грудь в вырезе кимоно
    sk.cloth("body", ("top", "bottom") + SIDES, skin_c, noise=0.02, grad=0.06)
    bf = sk.local("body", "front")
    for y in range(3, 10):
        bf.put(7, y, mul(skin_c, 0.84)); bf.put(8, y, mul(skin_c, 0.86))
    for x in range(5, 11):
        bf.put(x, 7, mul(skin_c, 0.9))
    gi, belt = 0x2B2724, 0x0C0B0B
    gi_jacket(sk, gi, v_depth=11, v_open=3, belt=belt, trim=mul(gi, 1.6))
    jf = sk.local("jacket", "front")
    for (x, y) in ((3, 18), (4, 19), (12, 5), (13, 21)):
        jf.put(x, y, mul(gi, 1.45))                              # потёртости
    jb = sk.local("jacket", "back")
    for y in range(4, 11):                                       # иероглиф-нашивка на спине (свой знак: кулак в круге)
        for x in range(5, 11):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 2.6 < d < 3.6:
                jb.put(x, y, (176, 60, 40))
    jb.rect(7, 6, 9, 9, (176, 60, 40))
    # руки: короткие рукава до локтя, дальше кожа и бинты
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, gi, skin_c, sleeve_rows=9)
        for face in SIDES:
            fa = sk.local(arm, face)
            for y in range(10, 22):
                fa.put(2 + (y // 3) % 2, y, mix(mul(skin_c, SHADE[face]), (90, 40, 30), 0.25))   # вены
    for sleeve in ("rsleeve", "lsleeve"):
        for face in SIDES:
            fs = sk.local(sleeve, face)
            for x in range(fs.width):
                fs.put(x, 8, mul(gi, 1.25 * SHADE[face]))       # край рукава
                for y in range(14, 24):
                    if y == 24 - 1 and x in (0, 7):
                        continue
                    c = (220, 210, 188) if (x + y) % 4 else (186, 176, 156)
                    fs.put(x, y, mul(c, SHADE[face]))
            fs.put(3, 17, (140, 50, 44)); fs.put(4, 21, (120, 40, 36))   # кровь на бинтах
    # ноги: широкие тёмные штаны, босые ступни
    pants = 0x1E1D22
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, skin_c, mul(skin_c, 0.7), shoe_rows=3, seam=False)
        fl = sk.local(leg, "front")
        for x in (1, 3, 5):
            fl.put(x, 22, mul(skin_c, 0.82))                     # пальцы
    for part in ("rpants", "lpants"):
        coat_skirt(sk, part, pants, 19, trim=mul(pants, 0.8))
    save(sk, folder, "dojo_master_0")


def style_mentor(folder):
    """
    Наставник додзё: старик с седыми волосами, лысиной и длинной белой бородой. Светлое кимоно,
    поверх — тёмно-коричневая накидка без пояса, серо-синие широкие штаны, белые носки и деревянные сандалии.
    """
    sk = Skin(5151)
    skin_c, hair = 0xD2AA88, 0xC4C0B8
    paint_head_rest(sk, skin_c, hair, "short")
    top = sk.local("head", "top")
    for y in range(16):
        for x in range(16):
            edge = x in (0, 1, 14, 15) or y in (0, 1)
            top.put(x, y, mul(hair if edge else skin_c, SHADE["top"] * (1 + sk.rng.uniform(-0.02, 0.02))))
    paint_face(sk, skin_c, ((70, 58, 46), (40, 32, 26)), hair,
               [0] * 16, mouth="calm", beard=0xE2DED6)
    f = sk.local("head", "front")
    for x in (2, 3, 4, 5, 10, 11, 12, 13):
        f.put(x, 5, (236, 234, 228))                             # пышные седые брови
    for x in range(3, 13):
        f.put(x, 2, mul(skin_c, 0.9))                            # морщины на лбу
    # длинная борода на внешнем слое головы (спускается ниже подбородка)
    hf = sk.local("hat", "front")
    for y in range(12, 16):
        for x in range(4, 12):
            if y == 15 and x in (4, 11):
                continue
            hf.put(x, y, mul(0xE2DED6, 1.0 - 0.04 * (y - 12) + sk.rng.uniform(-0.04, 0.04)))
    robe, haori, pants = 0xE4DCCB, 0x3A2A1E, 0x2C2E3A
    sk.cloth("body", ("top", "bottom") + SIDES, robe, noise=0.02)
    bf = sk.local("body", "front")
    for y in range(0, 24):                                       # борода спускается на грудь
        for x in range(5, 11):
            if y < 9 - abs(x - 7.5) * 0.8:
                bf.put(x, y, mul(0xE2DED6, 0.96 - 0.02 * y + sk.rng.uniform(-0.03, 0.03)))
    for y in range(9, 24):
        bf.put(7, y, mul(robe, 0.82))
    for x in range(16):
        bf.put(x, 14, (60, 46, 34)); bf.put(x, 15, (48, 36, 26))
    # накидка: открыта спереди, видно кимоно и бороду
    for face in ("back", "right", "left", "top"):
        sk.cloth("jacket", (face,), haori, noise=0.03)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            if 3 < x < 12:
                continue
            c = mul(haori, (1.04 - 0.08 * y / 24) * (1 + sk.rng.uniform(-0.03, 0.03)))
            if x in (3, 12):
                c = mul(haori, 1.4)
            jf.put(x, y, c)
    jf.put(3, 7, (200, 180, 120)); jf.put(12, 7, (200, 180, 120))   # завязки накидки
    jb = sk.local("jacket", "back")
    for y in range(3, 8):                                         # герб додзё на спине (свой знак)
        for x in range(6, 10):
            if (x + y) % 2 == 0:
                jb.put(x, y, (200, 180, 120))
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, haori, skin_c, cuff=mul(haori, 1.3), sleeve_rows=19)
        for face in SIDES:
            fs = sk.local(sleeve, face)
            for y in range(12, 19):                               # широкие рукава свисают
                for x in range(fs.width):
                    fs.put(x, y, mul(haori, SHADE[face] * (0.95 - 0.02 * (y - 12))))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, 0xE6E2DA, 0x6A4A2A, shoe_rows=3, seam=False)
    for part in ("rpants", "lpants"):
        coat_skirt(sk, part, pants, 20, trim=mul(pants, 0.78))
        fp = sk.local(part, "front")
        for y in range(4, 19):
            fp.put(2, y, mul(pants, 0.82)); fp.put(5, y, mul(pants, 0.82))   # складки штанов
    save(sk, folder, "style_mentor_0")


def clown(folder):
    """
    Клоун из Пьеро (свой персонаж): белая фарфоровая маска на всё лицо — фиолетовые ромбы вокруг глаз,
    длинная чёрная улыбка с красными стежками. Тёмно-фиолетовый капюшон с двумя свисающими концами,
    куртка в чёрно-фиолетовые ромбы, белый гофрированный воротник, белые перчатки,
    штаны в чёрно-белую полоску, острые чёрные туфли.
    """
    sk = Skin(7171)
    mask, hood, dark, violet = (238, 234, 228), 0x3A2458, 0x141018, 0x7A50C0
    paint_head_rest(sk, (210, 200, 196), hood, "long")
    f = sk.local("head", "front")
    for y in range(16):
        for x in range(16):
            k = 1.0 - 0.05 * abs(x - 7.5) / 7.5 - 0.04 * y / 15
            f.put(x, y, mul(mask, k * (1 + sk.rng.uniform(-0.015, 0.015))))
    for x in range(16):                                           # край капюшона над маской
        f.put(x, 0, mul(hood, 0.9)); f.put(x, 1, mul(hood, 1.05))
    for cx in (4, 11):                                            # ромбы вокруг глаз
        for dy in range(-3, 4):
            w = 2 - abs(dy) // 2
            for dx in range(-w, w + 1):
                f.put(cx + dx, 7 + dy, mul(violet, 1.0 - 0.1 * abs(dy)))
        f.put(cx, 7, (10, 8, 12)); f.put(cx, 8, (10, 8, 12))      # пустые прорези глаз
        f.put(cx - 1, 7, (30, 20, 40))
    for x in range(2, 14):                                        # длинная улыбка
        y = 12 - (1 if x in (3, 12) else 0) - (2 if x in (2, 13) else 0)
        f.put(x, y, (20, 14, 18))
        if 3 < x < 12 and x % 2 == 0:
            f.put(x, y - 1, (170, 30, 40)); f.put(x, y + 1, (170, 30, 40))   # стежки
    f.put(7, 10, mul(mask, 0.85)); f.put(8, 10, mul(mask, 0.85))  # нос
    # капюшон: внешний слой головы, два конца свисают сзади на бок
    for face in ("top", "right", "left", "back"):
        sk.cloth("hat", (face,), hood, noise=0.04)
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(3):
            hf.put(x, y, mul(hood, 1.1 - 0.05 * y))
        if x in (0, 1, 14, 15):
            for y in range(3, 16):
                hf.put(x, y, mul(hood, 0.95))
    hb = sk.local("hat", "back")
    for y in range(4, 16):
        hb.put(3 + y // 5, y, (230, 200, 60) if y == 15 else mul(hood, 0.8))     # кисточка на конце
        hb.put(12 - y // 5, y, (230, 200, 60) if y == 15 else mul(hood, 0.8))
    # корпус: куртка в ромбы
    def diamonds(part, faces, a, b):
        for face in faces:
            g = sk.local(part, face)
            for y in range(g.height):
                for x in range(g.width):
                    cell = ((x + y) // 6 + (x - y + 66) // 6) % 2
                    c = a if cell else b
                    g.put(x, y, mul(c, SHADE[face] * (1 + sk.rng.uniform(-0.03, 0.03))))
    diamonds("body", ("top", "bottom") + SIDES, dark, mul(violet, 0.75))
    diamonds("jacket", ("back", "right", "left"), dark, mul(violet, 0.75))
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            cell = ((x + y) // 6 + (x - y + 66) // 6) % 2
            jf.put(x, y, mul(dark if cell else mul(violet, 0.75), 1.0 + sk.rng.uniform(-0.03, 0.03)))
    for x in range(16):                                           # белый гофрированный воротник
        for y in range(0, 3):
            jf.put(x, y, mul((240, 238, 232), 1.0 if (x + y) % 2 else 0.86))
    for face in ("back", "right", "left"):
        g = sk.local("jacket", face)
        for x in range(g.width):
            for y in range(0, 3):
                g.put(x, y, mul((240, 238, 232), SHADE[face] * (1.0 if (x + y) % 2 else 0.86)))
    for y in range(4, 22, 4):                                     # жёлтые пуговицы-бубенцы
        jf.put(7, y, (230, 200, 60)); jf.put(8, y, (190, 160, 40))
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, dark, (236, 234, 228), sleeve_rows=19)
        diamonds(sleeve, SIDES + ("top",), dark, mul(violet, 0.75))
        for face in SIDES:
            g = sk.local(sleeve, face)
            for y in range(19, 24):
                for x in range(g.width):
                    g.put(x, y, mul((236, 234, 228), SHADE[face]))  # белые перчатки
            for x in range(g.width):
                g.put(x, 18, mul((240, 238, 232), SHADE[face] * (1.0 if x % 2 else 0.85)))   # манжета-рюш
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, (236, 236, 236), dark, (6, 6, 8), shoe_rows=4, seam=False)
        for face in SIDES:
            g = sk.local(leg, face)
            for y in range(0, 20):
                if (y // 2) % 2:
                    for x in range(g.width):
                        g.put(x, y, mul(dark, SHADE[face]))
        fl = sk.local(leg, "front")
        fl.put(3, 23, mul(dark, 1.4)); fl.put(4, 23, mul(dark, 1.4))   # острый нос туфли
    save(sk, folder, "clown_0")


def white_reaper(folder):
    """
    Белый жнец (свой персонаж): длинный бледно-серый плащ с глубоким капюшоном, костяная полумаска на верхней
    половине лица с одной узкой прорезью, под ней — бледные губы. Под плащом тёмная одежда с серебряными пряжками,
    серые перчатки, полы плаща до колен, тёмные сапоги.
    """
    sk = Skin(8383)
    skin_c, hair = (214, 206, 200), (200, 202, 206)
    cloak, under, bone = (206, 210, 214), (38, 40, 46), (232, 228, 214)
    paint_head_rest(sk, skin_c, hair, "bob")
    paint_face(sk, skin_c, ((40, 40, 46), (20, 20, 24)), hair, [3] * 16, mouth="stern")
    f = sk.local("head", "front")
    for y in range(2, 10):                                        # костяная полумаска
        for x in range(1, 15):
            f.put(x, y, mul(bone, (1.0 - 0.05 * abs(x - 7.5) / 7.5) * (1 + sk.rng.uniform(-0.02, 0.02))))
    for x in range(3, 13):
        f.put(x, 7, (16, 16, 20))                                 # прорезь
    f.put(2, 7, mul(bone, 0.8)); f.put(13, 7, mul(bone, 0.8))
    for (x, y) in ((5, 3), (6, 4), (10, 4), (11, 3)):
        f.put(x, y, mul(bone, 0.82))                              # резьба на маске
    for x in range(5, 11):
        f.put(x, 9, mul(bone, 0.85))                              # нижний край маски
    # капюшон
    for face in ("top", "right", "left", "back"):
        sk.cloth("hat", (face,), cloak, noise=0.03)
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(2):
            hf.put(x, y, mul(cloak, 1.05 - 0.08 * y))
        if x in (0, 1, 14, 15):
            for y in range(2, 16):
                hf.put(x, y, mul(cloak, 0.86))
    for x in range(2, 14):
        hf.put(x, 2, mul(cloak, 0.7))                             # тень капюшона над маской
    # корпус: тёмная одежда, ремни с пряжками
    sk.cloth("body", ("top", "bottom") + SIDES, under, noise=0.03)
    bf = sk.local("body", "front")
    for y in range(24):
        bf.put(4 + y // 4, y, (90, 92, 100))                      # ремень через грудь
    for x in range(16):
        bf.put(x, 15, (70, 72, 80)); bf.put(x, 16, (60, 62, 68))
    bf.rect(7, 14, 10, 18, (190, 196, 204))                       # пряжка
    # плащ на внешнем слое: открыт спереди
    for face in ("back", "right", "left", "top"):
        sk.cloth("jacket", (face,), cloak, noise=0.03)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            if 3 < x < 12:
                continue
            c = mul(cloak, (1.03 - 0.08 * y / 24) * (1 + sk.rng.uniform(-0.03, 0.03)))
            if x in (3, 12):
                c = mul(cloak, 0.8)
            jf.put(x, y, c)
    jf.put(3, 2, (190, 196, 204)); jf.put(12, 2, (190, 196, 204))   # застёжка плаща
    jb = sk.local("jacket", "back")
    for y in range(4, 20):                                         # складки плаща сзади
        jb.put(5, y, mul(cloak, 0.88)); jb.put(10, y, mul(cloak, 0.88))
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, cloak, (110, 112, 120), sleeve_rows=18)
        for face in SIDES:
            g = sk.local(sleeve, face)
            for y in range(0, 18):
                for x in range(g.width):
                    g.put(x, y, mul(cloak, SHADE[face] * (1.02 - 0.06 * y / 18)))
            for x in range(g.width):
                g.put(x, 17, mul(cloak, SHADE[face] * 0.8))
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, under, (28, 28, 32), (14, 14, 16), shoe_rows=7, seam=False)
    for part in ("rpants", "lpants"):
        coat_skirt(sk, part, cloak, 15, trim=mul(cloak, 0.8))
    save(sk, folder, "white_reaper_0")


def shadow_decoy(folder):
    """Приманка «Подмены»: силуэт из густого фиолетово-чёрного дыма, два бледных огонька вместо глаз."""
    sk = Skin(9090)
    base = (26, 18, 36)
    for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
        for face in ("top", "bottom") + SIDES:
            g = sk.local(part, face)
            for y in range(g.height):
                for x in range(g.width):
                    swirl = 0.5 + 0.5 * __import__("math").sin((x * 0.9 + y * 0.6) + sk.rng.uniform(-0.6, 0.6))
                    c = mix(base, (70, 46, 110), 0.35 * swirl)
                    g.put(x, y, mul(c, SHADE[face]))
    f = sk.local("head", "front")
    for (x, y) in ((4, 8), (5, 8), (10, 8), (11, 8)):
        f.put(x, y, (200, 180, 255))
    f.put(4, 9, (120, 100, 180)); f.put(11, 9, (120, 100, 180))
    # клочья дыма на внешнем слое
    for part in ("hat", "jacket", "rsleeve", "lsleeve", "rpants", "lpants"):
        for face in ("top",) + SIDES:
            g = sk.local(part, face)
            for y in range(g.height):
                for x in range(g.width):
                    if sk.rng.random() < 0.18:
                        g.put(x, y, mul((60, 40, 96), SHADE[face] * sk.rng.uniform(0.8, 1.2)))
    save(sk, folder, "shadow_decoy_0")


if __name__ == "__main__":
    out = sys.argv[1]
    dojo_master(out)
    style_mentor(out)
    clown(out)
    white_reaper(out)
    shadow_decoy(out)
    print("ok")
