"""
Скины NPC Tokyo Ghoul RPG в двойном разрешении (128 x 128, развёртка как у скина игрока 64 x 64).
Все персонажи оригинальные. Свет падает спереди сверху: верх светлее, бока и низ темнее.
Запуск: python3 skins_hd.py <папка textures/entity>
"""
import os
import random
import sys

from PIL import Image

S = 2  # во сколько раз детальнее обычного скина

# Коробки модели игрока в единицах скина 64 x 64: (u, v, ширина, высота, глубина)
BOXES = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
}
SIDES = ("front", "back", "right", "left")
SHADE = {"top": 1.10, "front": 1.0, "right": 0.88, "left": 0.88, "back": 0.80, "bottom": 0.66}


# ------------------------------------------------------------------ цвета
def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mul(c, k):
    r, g, b = rgb(c) if isinstance(c, int) else c
    return (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)))


def mix(a, b, t):
    a = rgb(a) if isinstance(a, int) else a
    b = rgb(b) if isinstance(b, int) else b
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def region(part, face):
    u, v, w, h, d = BOXES[part]
    x, y, fw, fh = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }[face]
    return x * S, y * S, fw * S, fh * S


class Skin:
    def __init__(self, seed, size=128):
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.rng = random.Random(seed)

    # ---- пиксели
    def put(self, x, y, c, alpha=255):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            c = rgb(c) if isinstance(c, int) else c
            self.img.putpixel((x, y), (c[0], c[1], c[2], alpha))

    def clear(self, x, y):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.img.putpixel((x, y), (0, 0, 0, 0))

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def local(self, part, face):
        """Рисование в координатах грани: (x, y) от левого верхнего угла грани."""
        x0, y0, w, h = region(part, face)
        skin = self

        class Face:
            width, height = w, h
            name = face

            def put(self, x, y, c, alpha=255):
                if 0 <= x < w and 0 <= y < h:
                    skin.put(x0 + x, y0 + y, c, alpha)

            def clear(self, x, y):
                if 0 <= x < w and 0 <= y < h:
                    skin.clear(x0 + x, y0 + y)

            def get(self, x, y):
                return skin.get(x0 + x, y0 + y)

            def rect(self, xa, ya, xb, yb, c):
                for yy in range(max(0, ya), min(h, yb)):
                    for xx in range(max(0, xa), min(w, xb)):
                        self.put(xx, yy, c)

        return Face()

    # ---- ткань: заливка грани с освещением, градиентом сверху вниз и лёгкой фактурой
    def cloth(self, part, faces, base, noise=0.035, grad=0.10, rows=None):
        for face in faces:
            f = self.local(part, face)
            k_face = SHADE[face]
            y_from, y_to = (0, f.height) if rows is None else rows
            for y in range(y_from, min(y_to, f.height)):
                t = y / max(1, f.height - 1)
                k_grad = 1.0 + grad * 0.5 - grad * t if face in SIDES else 1.0
                for x in range(f.width):
                    k = k_face * k_grad * (1.0 + self.rng.uniform(-noise, noise))
                    f.put(x, y, mul(base, k))

    def whole(self, part, base, **kw):
        self.cloth(part, ("top", "bottom") + SIDES, base, **kw)


# ------------------------------------------------------------------ лицо и волосы
def paint_face(sk, skin_c, eyes, hair, fringe, brows=True, mouth="calm", blush=False, glasses=None,
               beard=None, lashes=False):
    """Лицо 16 x 16. fringe: на сколько строк спускается чёлка в каждом из 16 столбцов."""
    f = sk.local("head", "front")
    hi, lo = mul(skin_c, 1.06), mul(skin_c, 0.84)
    for y in range(16):
        for x in range(16):
            c = skin_c
            if x in (0, 15) or y == 15:
                c = lo
            elif y == 14 and x in (1, 2, 13, 14):
                c = lo
            elif 4 <= y <= 9 and x in (2, 3, 12, 13):
                c = mix(skin_c, hi, 0.5)
            f.put(x, y, c)
    # глаза: 3 x 3, радужка 2 x 2 с бликом, белок снаружи
    iris, iris_dark = eyes
    lash = mul(hair, 0.55)
    for side in (0, 1):
        x0 = 3 if side == 0 else 10
        outer = x0 if side == 0 else x0 + 2
        irs = (x0 + 1, x0 + 2) if side == 0 else (x0, x0 + 1)
        for x in range(x0, x0 + 3):
            f.put(x, 7, lash)
        if lashes:
            f.put(outer - 1 if side == 0 else outer + 1, 7, lash)
        f.put(outer, 8, (240, 238, 236))
        f.put(outer, 9, (214, 210, 206))
        f.put(irs[0], 8, (250, 250, 250))       # блик
        f.put(irs[1], 8, iris)
        f.put(irs[0], 9, iris)
        f.put(irs[1], 9, iris_dark)
    if brows:
        for x in (3, 4, 5, 10, 11, 12):
            f.put(x, 5, mul(hair, 0.8))
    f.put(8, 11, mul(skin_c, 0.82))               # нос
    f.put(7, 11, mul(skin_c, 0.92))
    if mouth == "calm":
        f.put(7, 13, mul(skin_c, 0.72)); f.put(8, 13, mul(skin_c, 0.72))
    elif mouth == "stern":
        for x in (6, 7, 8, 9):
            f.put(x, 13, mul(skin_c, 0.70))
    elif mouth == "smile":
        f.put(6, 12, mul(skin_c, 0.75)); f.put(9, 12, mul(skin_c, 0.75))
        f.put(7, 13, mul(skin_c, 0.66)); f.put(8, 13, mul(skin_c, 0.66))
    elif mouth == "grin":
        for x in (6, 7, 8, 9):
            f.put(x, 13, (238, 234, 226))
        f.put(5, 13, mul(skin_c, 0.6)); f.put(10, 13, mul(skin_c, 0.6))
        for x in (6, 7, 8, 9):
            f.put(x, 14, mul(skin_c, 0.66))
    if blush:
        for x, y in ((2, 11), (3, 11), (12, 11), (13, 11)):
            f.put(x, y, mix(skin_c, (232, 120, 130), 0.35))
    if beard:
        for y in range(11, 16):
            for x in range(1, 15):
                edge = (y == 11 and x in (1, 2, 13, 14))
                if (y >= 12 or x in range(1, 5) or x in range(11, 15)) and not edge:
                    shade = 1.0 - 0.06 * (y - 11) + sk.rng.uniform(-0.05, 0.05)
                    f.put(x, y, mul(beard, shade))
        for x in (6, 7, 8, 9):
            f.put(x, 13, mul(beard, 0.55))
    if glasses:
        for x in range(2, 14):
            if x not in (6, 7, 8, 9) or x in (7, 8):
                f.put(x, 6, glasses)
        for x in (2, 6, 9, 13):
            for y in (7, 8, 9):
                f.put(x, y, glasses)
        for x in range(2, 7):
            f.put(x, 10, glasses)
        for x in range(9, 14):
            f.put(x, 10, glasses)
    # чёлка поверх лица
    hair_strands(f, hair, fringe, sk.rng, front=True)


STRAND = (1.0, 1.06, 0.93, 1.02, 0.9, 1.08, 0.96, 1.0)


def strand_shade(x):
    """Оттенок пряди по столбцу: пряди шириной 2 пикселя, светлее и темнее друг друга."""
    return STRAND[(x // 2) % len(STRAND)]


def hair_color(hair, x, y, d, ring=None, rng=None):
    """Цвет волоса в точке: прядь, кольцо блика на строках ring, затемнение к кончику."""
    k = strand_shade(x)
    if ring and y in ring:
        k *= 1.28
    if d > 1:
        k *= 1.0 - 0.18 * max(0, y - (d - 3)) / 3.0
    if rng:
        k *= 1.0 + rng.uniform(-0.025, 0.025)
    return mul(hair, k)


def hair_strands(f, hair, depth, rng, front=False):
    """Чёлка: пряди с бликом на 2-й и 3-й строке и тёмными заострёнными кончиками."""
    for x in range(f.width):
        d = depth[x] if isinstance(depth, (list, tuple)) else depth
        for y in range(d):
            c = hair_color(hair, x, y, d, ring=(1, 2) if front else None, rng=rng)
            if y == d - 1:
                c = mul(hair, 0.62)               # кончик пряди
            if isinstance(depth, (list, tuple)) and y >= d - 2:
                left = depth[x - 1] if x > 0 else d
                right = depth[x + 1] if x < f.width - 1 else d
                if left > d or right > d:
                    c = mul(c, 0.86)              # тень от соседней, более длинной пряди
            f.put(x, y, c)


def paint_head_rest(sk, skin_c, hair, style):
    """Бока, затылок, макушка и низ головы. style: short, long, bob, buzz, hood."""
    lo = mul(skin_c, 0.84)
    top = sk.local("head", "top")
    for y in range(16):
        for x in range(16):
            # пряди идут от макушки к лбу; ближе к лбу (нижние строки) светлее
            k = SHADE["top"] * strand_shade(x) * (0.94 + 0.1 * y / 15) * (1 + sk.rng.uniform(-0.025, 0.025))
            top.put(x, y, mul(hair, k))
    bottom = sk.local("head", "bottom")
    bottom.rect(0, 0, 16, 16, mul(skin_c, 0.7))
    # бока: «right» — передний край справа (столбец 15), «left» — передний край слева (столбец 0)
    for face in ("right", "left"):
        f = sk.local("head", face)
        for y in range(16):
            for x in range(16):
                f.put(x, y, mul(skin_c, 0.9))
        front_col = (lambda c: 15 - c) if face == "right" else (lambda c: c)
        # ухо
        for y in (8, 9, 10, 11):
            for c in (6, 7):
                f.put(front_col(c), y, mul(skin_c, 0.82))
            f.put(front_col(8), y, mul(skin_c, 0.74))
        # волосы
        for x in range(16):
            dist_front = 15 - x if face == "right" else x       # 0 у лица, 15 у затылка
            if style == "buzz":
                d = 4 if dist_front > 2 else 3
            elif style == "short":
                d = 6 if dist_front < 4 else (9 if dist_front < 9 else 13)
            elif style == "bob":
                d = 12 if dist_front < 3 else 14
            elif style == "long":
                d = 16
            else:
                d = 6
            for y in range(d):
                c = hair_color(hair, x, y, d, ring=(2, 3), rng=sk.rng)
                f.put(x, y, mul(c, SHADE["right"]))
    back = sk.local("head", "back")
    for y in range(16):
        for x in range(16):
            back.put(x, y, mul(skin_c, 0.8))
    nape = {"buzz": 11, "short": 13, "bob": 15, "long": 16}.get(style, 13)
    for y in range(nape):
        for x in range(16):
            d = nape - (1 if x % 4 == 1 else 0)          # неровный край на шее
            if y >= d:
                continue
            c = hair_color(hair, x, y, d, ring=(2, 3), rng=sk.rng)
            back.put(x, y, mul(c, SHADE["back"]))


def paint_hat_hair(sk, hair, fringe_tips, side_depth, back_depth, tufts=()):
    """
    Объём волос на внешнем слое головы. Слой чуть больше головы, поэтому пряди на нём
    выглядят приподнятыми над основными волосами. fringe_tips: длина прядей над лбом
    по столбцам (0 — пусто), side_depth(расстояние от лица) и back_depth(x) — длина прядей
    сбоку и сзади. tufts: столбцы, где на макушке у лба торчат хохолки.
    """
    top = sk.local("hat", "top")
    for x in tufts:
        for y in range(11, 16):
            if y >= 13 or x % 2 == 0:
                top.put(x, y, mul(hair, SHADE["top"] * strand_shade(x) * 1.05))
    front = sk.local("hat", "front")
    for x, d in enumerate(fringe_tips):
        for y in range(d):
            c = hair_color(hair, x, y, d + 1, ring=(1,), rng=sk.rng)
            front.put(x, y, mul(c, 1.03))
    for face in ("right", "left"):
        f = sk.local("hat", face)
        for x in range(16):
            dist_front = 15 - x if face == "right" else x
            d = side_depth(dist_front) - (1 if x % 3 == 2 else 0)
            for y in range(max(0, d)):
                f.put(x, y, mul(hair_color(hair, x, y, d, ring=(2, 3), rng=sk.rng), SHADE["right"]))
    b = sk.local("hat", "back")
    for x in range(16):
        d = back_depth(x) - (1 if x % 3 == 1 else 0)
        for y in range(max(0, d)):
            b.put(x, y, mul(hair_color(hair, x, y, d, ring=(2, 3), rng=sk.rng), SHADE["back"]))


# ------------------------------------------------------------------ руки и ноги
def paint_arm(sk, part, sleeve, hand, cuff=None, sleeve_rows=18):
    for face in ("top", "bottom") + SIDES:
        f = sk.local(part, face)
        for y in range(f.height):
            for x in range(f.width):
                if face == "bottom":
                    c = mul(hand, SHADE[face])
                elif face == "top" or y < sleeve_rows:
                    t = y / max(1, f.height)
                    c = mul(sleeve, SHADE[face] * (1.04 - 0.1 * t) * (1 + sk.rng.uniform(-0.03, 0.03)))
                    if face in SIDES and (y, x) in ((8, 1), (9, 2), (9, 3), (10, 4), (11, 5)):
                        c = mul(c, 0.88)       # складка на локте
                    elif face in SIDES and (y, x) in ((8, 2), (9, 4), (10, 5)):
                        c = mul(c, 1.08)
                else:
                    c = mul(hand, SHADE[face] * (1.0 if y < f.height - 1 else 0.86))
                f.put(x, y, c)
        if cuff and face in SIDES:
            for x in range(f.width):
                f.put(x, sleeve_rows - 1, mul(cuff, SHADE[face]))
                f.put(x, sleeve_rows - 2, mul(cuff, SHADE[face] * 1.05))


def paint_leg(sk, part, pants, shoe, sole, shoe_rows=5, seam=True):
    for face in ("top", "bottom") + SIDES:
        f = sk.local(part, face)
        for y in range(f.height):
            for x in range(f.width):
                if face == "bottom":
                    c = mul(sole, 0.8)
                elif face == "top":
                    c = mul(pants, SHADE[face])
                elif y >= f.height - shoe_rows:
                    c = sole if y == f.height - 1 else mul(shoe, SHADE[face] * (1.08 if y == f.height - shoe_rows else 1.0))
                else:
                    t = y / f.height
                    c = mul(pants, SHADE[face] * (1.05 - 0.12 * t) * (1 + sk.rng.uniform(-0.03, 0.03)))
                    if face == "front" and 9 <= y <= 12 and 2 <= x <= 5:
                        c = mul(c, 1.06)       # колено чуть светлее
                    if face in SIDES and (y, x) in ((13, 1), (14, 2), (14, 3), (15, 4)):
                        c = mul(c, 0.88)       # складка под коленом
                f.put(x, y, c)
        if seam and face in ("right", "left"):
            mid = f.width // 2
            for y in range(f.height - shoe_rows):
                f.put(mid, y, mul(f.get(mid, y)[:3], 0.88))
        if face == "front":
            f.put(2, f.height - shoe_rows + 1, mul(shoe, 1.3))   # блик на носке
            f.put(5, f.height - shoe_rows + 1, mul(shoe, 1.3))


def paint_pants_overlay(sk, part, pants, rows):
    """Подвёрнутые штанины на внешнем слое ног: светлая полоса с тенью снизу."""
    for face in SIDES:
        f = sk.local(part, face)
        for x in range(f.width):
            f.put(x, rows, mul(pants, 1.18 * SHADE[face]))
            f.put(x, rows + 1, mul(pants, 0.9 * SHADE[face]))


def coat_skirt(sk, part, coat, rows, face_open_col=None, trim=None):
    """Полы длинного плаща на внешнем слое ног: верхние rows строк."""
    for face in SIDES:
        f = sk.local(part, face)
        for y in range(rows):
            for x in range(f.width):
                c = mul(coat, SHADE[face] * (1.0 - 0.06 * y / rows) * (1 + sk.rng.uniform(-0.03, 0.03)))
                if y == rows - 1:
                    c = mul(trim or coat, SHADE[face] * 0.85)
                if face == "front" and face_open_col is not None and x == face_open_col:
                    c = mul(coat, 0.7)
                f.put(x, y, c)
        # одна мягкая складка на каждой грани: тень и блик рядом
        for y in range(rows // 3, rows - 1):
            f.put(5, y, mul(f.get(5, y)[:3], 0.88))
            f.put(4, y, mul(f.get(4, y)[:3], 1.05))


def save(sk, folder, name):
    os.makedirs(folder, exist_ok=True)
    sk.img.save(os.path.join(folder, name + ".png"))


# ------------------------------------------------------------------ персонажи
def ghoul_hoodie(folder):
    """Гуль в тёмной толстовке с капюшоном, джинсы, кеды."""
    sk = Skin(101)
    skin_c, hair = 0xE6D2C2, 0x1A1A20
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((200, 30, 44), (130, 14, 24)), hair,
               [5, 6, 7, 6, 5, 6, 7, 8, 7, 6, 5, 6, 7, 6, 5, 5], mouth="stern")
    hood = 0x2C2C34
    # капюшон на внешнем слое: макушка, затылок, бока, край вокруг лица
    sk.cloth("hat", ("top", "back", "right", "left"), hood, noise=0.03)
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(16):
            edge = y < 3 or x < 2 or x > 13
            if edge:
                c = mul(hood, 1.08 if y == 2 or x in (1, 14) else 0.95)
                hf.put(x, y, c)
    # корпус: толстовка, карман-кенгуру, шнурки, манжета снизу
    sk.cloth("body", ("top", "bottom") + SIDES, hood)
    bf = sk.local("body", "front")
    for x in (5, 10):
        for y in range(1, 7):
            bf.put(x, y, (200, 196, 190) if y < 6 else (150, 30, 40))
    bf.rect(3, 13, 13, 19, mul(hood, 0.86))
    for x in range(3, 13):
        bf.put(x, 13, mul(hood, 1.12))
    for y in range(13, 19):
        bf.put(3, y, mul(hood, 0.75)); bf.put(12, y, mul(hood, 0.75))
    for x in range(16):
        bf.put(x, 22, mul(hood, 0.78)); bf.put(x, 23, mul(hood, 0.7))
    for (x, y) in ((2, 8), (3, 9), (4, 9), (13, 8), (12, 9), (11, 9)):
        bf.put(x, y, mul(hood, 0.86))                  # складки на груди
    # рукава и манжеты
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, hood, skin_c, cuff=mul(hood, 0.75), sleeve_rows=19)
    # джинсы и кеды
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x262A36, 0x1C1C1C, 0xE8E4DC)
    for pants in ("rpants", "lpants"):
        paint_pants_overlay(sk, pants, 0x262A36, 18)
    save(sk, folder, "ghoul_0")


def ghoul_jacket(folder):
    """Гуль в коричневой кожаной куртке поверх красной футболки, светлые волосы."""
    sk = Skin(202)
    skin_c, hair = 0xE8CDB0, 0xB8A27A
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((210, 40, 40), (120, 10, 20)), hair,
               [4, 5, 6, 5, 4, 4, 5, 6, 5, 4, 4, 5, 6, 5, 4, 4], mouth="grin")
    paint_hat_hair(sk, hair, [3, 4, 0, 0, 3, 5, 0, 0, 4, 6, 0, 0, 5, 4, 0, 3],
                   lambda d: 5 if d < 5 else 8, lambda x: 9 + (x % 3), tufts=(2, 3, 6, 7, 10, 11, 13))
    shirt, leather = 0x8A1C22, 0x5A3A26
    sk.cloth("body", ("top", "bottom") + SIDES, shirt)
    # куртка на внешнем слое: открыта спереди, видна футболка
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), leather, noise=0.05)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            if x < 5 or x > 10:
                c = mul(leather, (1.04 - 0.1 * y / 24) * (1 + sk.rng.uniform(-0.05, 0.05)))
                if x in (4, 11):
                    c = mul(leather, 0.75)       # край полы
                if y < 4 and x in (3, 4, 11, 12):
                    c = mul(leather, 1.2)        # воротник
                jf.put(x, y, c)
    for y in range(3, 22, 4):
        jf.put(4, y, (200, 190, 170))            # молния
    for x in range(16):
        if x < 5 or x > 10:
            jf.put(x, 21, mul(leather, 0.7))
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, leather, skin_c, cuff=mul(leather, 0.7), sleeve_rows=19)
        for face in SIDES:
            f = sk.local(sleeve, face)
            for x in range(f.width):
                f.put(x, 8, mul(leather, 1.15 * SHADE[face]))    # блик кожи на рукаве
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x2E2E2E, 0x3A2416, 0x1A120C, shoe_rows=6)
    save(sk, folder, "ghoul_1")


def ghoul_coat(folder):
    """Девушка-гуль: длинные тёмно-каштановые волосы, чёрное пальто, серый шарф."""
    sk = Skin(303)
    skin_c, hair = 0xF0DCCC, 0x4A2218
    paint_head_rest(sk, skin_c, hair, "long")
    paint_face(sk, skin_c, ((220, 50, 60), (140, 20, 30)), hair,
               [6, 7, 7, 6, 5, 5, 4, 4, 5, 6, 7, 7, 8, 8, 8, 8], mouth="calm", blush=True, lashes=True)
    paint_hat_hair(sk, hair, [7, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 9, 11, 12],
                   lambda d: 16, lambda x: 16)
    coat, scarf = 0x1C1C22, 0x8A8A90
    sk.cloth("body", ("top", "bottom") + SIDES, coat)
    sk.cloth("jacket", ("top", "bottom") + SIDES, coat, noise=0.03)
    jf = sk.local("jacket", "front")
    for y in range(24):
        jf.put(7, y, mul(coat, 0.7)); jf.put(8, y, mul(coat, 1.15))      # застёжка
    for y in (8, 13, 18):
        jf.put(6, y, (150, 150, 158)); jf.put(9, y, (150, 150, 158))    # пуговицы
    for x in range(16):                                                   # шарф
        for y in range(5):
            jf.put(x, y, mul(scarf, (1.08 if y == 0 else 1.0 - 0.05 * y) * (1 + sk.rng.uniform(-0.04, 0.04))))
    jf.rect(3, 5, 6, 11, scarf)
    for y in range(5, 11):
        jf.put(3, y, mul(scarf, 0.8))
    jb = sk.local("jacket", "back")                                       # волосы на спине
    for x in range(1, 15):
        for y in range(9 + (x % 3)):
            jb.put(x, y, mul(hair, 0.85 + sk.rng.uniform(-0.05, 0.05)))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, coat, skin_c, cuff=mul(coat, 0.7), sleeve_rows=19)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x2A2A30, 0x141416, 0x0A0A0A, shoe_rows=7)
    for pants in ("rpants", "lpants"):
        coat_skirt(sk, pants, coat, 12, face_open_col=None, trim=mul(coat, 0.8))
    save(sk, folder, "ghoul_2")


def ccg_agent(folder, name, seed, skin_c, hair, hair_style, coat, suit, tie, eyes, glasses=None,
              female=False, fringe=None, tips=None):
    """Следователь CCG: плащ поверх костюма, белая рубашка, галстук, тёмные туфли."""
    sk = Skin(seed)
    paint_head_rest(sk, skin_c, hair, hair_style)
    paint_face(sk, skin_c, eyes, hair, fringe, mouth="stern" if not female else "calm",
               glasses=glasses, lashes=female, blush=female)
    if tips:
        side = (lambda d: 13 if d < 3 else 15) if hair_style == "bob" else (lambda d: 0 if d < 5 else 5)
        back = (lambda x: 15) if hair_style == "bob" else (lambda x: 4)
        paint_hat_hair(sk, hair, tips, side, back)
    shirt = 0xF2F2EE
    # корпус: пиджак, рубашка и галстук посередине
    sk.cloth("body", ("top", "bottom") + SIDES, suit)
    bf = sk.local("body", "front")
    for y in range(24):
        for x in (6, 7, 8, 9):
            if y < 12:
                bf.put(x, y, mul(shirt, 1.0 - 0.02 * y))
    for y in range(1, 12):
        bf.put(7, y, mul(tie, 1.1 if y < 2 else 1.0)); bf.put(8, y, mul(tie, 0.85))
    bf.put(7, 12, mul(tie, 0.8)); bf.put(8, 12, mul(tie, 0.7))
    for y in range(0, 4):
        bf.put(5 + y // 2, y, mul(suit, 1.25)); bf.put(10 - y // 2, y, mul(suit, 1.25))   # лацканы
    # плащ на внешнем слое: распахнут спереди
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), coat, noise=0.025)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            if x < 5 or x > 10:
                c = mul(coat, (1.03 - 0.08 * y / 24) * (1 + sk.rng.uniform(-0.025, 0.025)))
                if x in (4, 11):
                    c = mul(coat, 0.82)
                if y < 5 and x in (3, 4, 11, 12):
                    c = mul(coat, 1.08 if y < 3 else 0.9)
                jf.put(x, y, c)
    for x in list(range(0, 5)) + list(range(11, 16)):          # пояс плаща
        jf.put(x, 14, mul(coat, 0.78)); jf.put(x, 15, mul(coat, 0.72))
    jf.put(2, 14, (60, 60, 64)); jf.put(13, 14, (60, 60, 64))  # пряжки
    for y in (6, 10, 18):
        jf.put(2, y, mul(coat, 0.7)); jf.put(13, y, mul(coat, 0.7))
    jb = sk.local("jacket", "back")
    for x in range(16):
        jb.put(x, 14, mul(coat, 0.62)); jb.put(x, 15, mul(coat, 0.58))
    for y in range(16, 24):
        jb.put(7, y, mul(coat, 0.6))                            # шлица сзади
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, suit, skin_c, cuff=shirt, sleeve_rows=19)
        sk.cloth(sleeve, ("top",) + SIDES, coat, noise=0.025, rows=(0, 18))
        for face in SIDES:
            f = sk.local(sleeve, face)
            for x in range(f.width):
                f.put(x, 16, mul(coat, 0.72 * SHADE[face]))     # хлястик на рукаве
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, suit, 0x121214, 0x050505, shoe_rows=5)
        f = sk.local(leg, "front")
        for y in range(0, 19):
            f.put(4, y, mul(suit, 0.82))                        # стрелка на брюках
    for pants in ("rpants", "lpants"):
        coat_skirt(sk, pants, coat, 10, trim=mul(coat, 0.85))
    save(sk, folder, name)


def trader_old(folder):
    """Старый торговец: седая борода, кепка, кожаный фартук поверх рубашки."""
    sk = Skin(404)
    skin_c, hair = 0xD8AA88, 0x9A9690
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((90, 110, 70), (50, 66, 40)), hair,
               [3, 3, 4, 4, 3, 3, 3, 3, 3, 3, 3, 4, 4, 3, 3, 3], mouth="smile", beard=0xB4B0A8)
    cap = 0x3A3428
    sk.cloth("hat", ("top", "back", "right", "left"), cap, rows=None)
    for face in ("back", "right", "left"):
        f = sk.local("hat", face)
        for y in range(6, 16):
            for x in range(16):
                f.clear(x, y)
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(5):
            hf.put(x, y, mul(cap, 1.05 if y < 4 else 0.7))
    shirt, apron = 0xC8B898, 0x6A4428
    sk.cloth("body", ("top", "bottom") + SIDES, shirt)
    bf = sk.local("body", "front")
    bf.rect(3, 6, 13, 24, apron)
    for y in range(6, 24):
        bf.put(3, y, mul(apron, 0.75)); bf.put(12, y, mul(apron, 0.75))
    for y in range(0, 6):
        bf.put(4, y, mul(apron, 0.85)); bf.put(11, y, mul(apron, 0.85))    # лямки
    bf.rect(5, 13, 11, 18, mul(apron, 0.85))                              # карман
    for x in range(5, 11):
        bf.put(x, 13, mul(apron, 1.15))
    for y in range(6, 24):
        for x in range(4, 12):
            if sk.rng.random() < 0.08:
                bf.put(x, y, mul(apron, 0.9))
    for x in range(16):
        bf.put(x, 11, mul(apron, 0.6) if 3 <= x <= 12 else mul(shirt, 0.8))   # завязки
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, shirt, skin_c, cuff=mul(shirt, 0.8), sleeve_rows=15)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x4A4636, 0x3A2416, 0x1A120C, shoe_rows=6)
    save(sk, folder, "trader_0")


def trader_scarf(folder):
    """Торговка: зелёный платок на голове, длинное коричневое пальто, сумка через плечо."""
    sk = Skin(505)
    skin_c, hair = 0xC8946E, 0x2A1A12
    paint_head_rest(sk, skin_c, hair, "bob")
    paint_face(sk, skin_c, ((70, 50, 30), (40, 28, 16)), hair,
               [4, 4, 5, 5, 4, 3, 3, 3, 3, 3, 4, 5, 5, 4, 4, 4], mouth="smile", lashes=True, blush=True)
    scarf = 0x2E5A3A
    sk.cloth("hat", ("top", "back", "right", "left"), scarf)
    for face in ("right", "left"):
        f = sk.local("hat", face)
        for y in range(11, 16):
            for x in range(16):
                f.clear(x, y)
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(4):
            hf.put(x, y, mul(scarf, 1.08 if y < 3 else 0.75))
        if x < 2 or x > 13:
            for y in range(4, 11):
                hf.put(x, y, mul(scarf, 0.9))
    for x in range(16):
        if x % 4 == 2:
            hf.put(x, 1, (200, 170, 90))                          # узор на платке
    coat, bag = 0x6A4A30, 0x3A2416
    sk.cloth("body", ("top", "bottom") + SIDES, 0xB8A888)
    sk.cloth("jacket", ("top", "bottom") + SIDES, coat, noise=0.04)
    jf = sk.local("jacket", "front")
    for y in range(24):
        jf.put(7, y, mul(coat, 0.72))
    for i in range(16):                                           # ремень сумки по диагонали
        y = 1 + i * 14 // 16
        jf.put(i, y, mul(bag, 1.1)); jf.put(i, y + 1, bag)
    jf.rect(10, 15, 15, 21, bag)
    for x in range(10, 15):
        jf.put(x, 15, mul(bag, 1.3))
    jf.put(12, 17, (200, 170, 90))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, coat, skin_c, cuff=mul(coat, 0.75), sleeve_rows=19)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x4A3A2A, 0x2A1A10, 0x140C08, shoe_rows=6)
    for pants in ("rpants", "lpants"):
        coat_skirt(sk, pants, coat, 13, trim=mul(coat, 0.8))
    save(sk, folder, "trader_1")


if __name__ == "__main__":
    out = sys.argv[1]
    ghoul_hoodie(out)
    ghoul_jacket(out)
    ghoul_coat(out)
    ccg_agent(out, "ccg_agent_0", 606, 0xE6C4A4, 0x3A2A1E, "short", 0xEDEDE8, 0x2A2C36, 0x22305A,
              ((70, 90, 110), (40, 50, 64)), fringe=[4, 4, 5, 4, 4, 3, 3, 3, 3, 3, 3, 4, 4, 5, 4, 4],
              tips=[0, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 2, 0, 0])
    ccg_agent(out, "ccg_agent_1", 707, 0xDDB896, 0x141418, "short", 0xB8BCC0, 0x18181E, 0x7A1A20,
              ((60, 60, 70), (30, 30, 36)), glasses=(40, 40, 44),
              fringe=[3, 3, 3, 4, 4, 4, 3, 3, 3, 3, 3, 3, 4, 4, 3, 3],
              tips=[0, 0, 2, 3, 3, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])
    ccg_agent(out, "ccg_agent_2", 808, 0xF0D6C0, 0x6A3420, "bob", 0xF2F2EE, 0x1E1E24, 0x1E1E24,
              ((100, 70, 50), (60, 40, 30)), female=True,
              fringe=[6, 6, 5, 5, 4, 4, 4, 3, 3, 4, 4, 5, 6, 7, 8, 9],
              tips=[6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 7, 8, 9])
    trader_old(out)
    trader_scarf(out)
    print("skins:", sorted(n for n in os.listdir(out) if n.endswith(".png")))
