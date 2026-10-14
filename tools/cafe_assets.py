"""
Иконки ядер гулей (I, II, III), текстуры знака кофейни и скин хозяина кофейни (оригинальный персонаж).
Запуск: python3 cafe_assets.py <папка assets/tokyoghoulrpg/textures>
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SIDES, SHADE, Skin, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_hat_hair, paint_head_rest, paint_leg, paint_pants_overlay, save)


def core_icon(tier):
    """Ядро: неровная «живая» сфера с прожилками. I — тусклое, II — багровое, III — чёрно-красное с золотом."""
    rng = random.Random(500 + tier)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base = {1: (150, 60, 66), 2: (190, 24, 44), 3: (90, 6, 20)}[tier]
    vein = {1: (90, 30, 36), 2: (100, 6, 20), 3: (255, 60, 70)}[tier]
    glow = {1: (220, 150, 150), 2: (255, 120, 120), 3: (255, 200, 90)}[tier]
    cx, cy, r = 7.5, 8.0, 5.6 + (tier - 1) * 0.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            wobble = 0.5 * math.sin(math.atan2(dy, dx) * 5 + tier)
            d = math.hypot(dx, dy)
            if d > r + wobble:
                continue
            shade = 1.15 - 0.45 * (dx + dy + r) / (2 * r)          # свет слева сверху
            c = mul(base, max(0.45, shade))
            if abs(math.sin(dx * 1.3 + dy * 0.7 + tier) * math.cos(dy * 1.1 - dx * 0.4)) < 0.12:
                c = vein                                             # прожилки
            if d > r + wobble - 1.0:
                c = mul(c, 0.6)                                      # тёмный край
            img.putpixel((x, y), c + (255,))
    for (x, y) in ((5, 5), (6, 5), (5, 6)):
        img.putpixel((x, y), glow + (255,))                          # блик
    if tier == 3:
        # золотой ободок-трещина и искры
        for a in range(0, 360, 30):
            x = int(round(cx + math.cos(math.radians(a)) * (r + 0.3)))
            y = int(round(cy + math.sin(math.radians(a)) * (r + 0.3)))
            if 0 <= x < 16 and 0 <= y < 16 and img.getpixel((x, y))[3] > 0:
                img.putpixel((x, y), (230, 180, 60, 255))
        for (x, y) in ((13, 2), (2, 12), (14, 11)):
            img.putpixel((x, y), (255, 210, 110, 255))
    elif tier == 2:
        for (x, y) in ((13, 3), (2, 13)):
            img.putpixel((x, y), (220, 60, 70, 255))
    return img


def wood(rng, base, w=16, h=16, plank=4):
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        row = y // plank
        for x in range(w):
            seam = (y % plank == plank - 1) or ((x + row * 5) % 16 == 0)
            c = mul(base, 0.72 if seam else (0.95 + 0.1 * math.sin(x * 0.9 + row * 2) + rng.uniform(-0.04, 0.04)))
            img.putpixel((x, y), c + (255,))
    return img


def sign_textures(folder):
    rng = random.Random(9101)
    dark = (74, 48, 30)
    top = wood(rng, dark)
    # кофейная чашка с паром по центру
    cream, cup, coffee = (236, 222, 196), (222, 210, 186), (92, 54, 30)
    for x in range(4, 11):
        for y in range(8, 12):
            top.putpixel((x, y), cup + (255,))
    for x in range(5, 10):
        top.putpixel((x, 8), coffee + (255,))
    for y in (9, 10):
        top.putpixel((11, y), cup + (255,))
    top.putpixel((12, 9), cup + (255,)); top.putpixel((12, 10), cup + (255,))
    for x in range(3, 13):
        top.putpixel((x, 12), mul(cream, 0.8) + (255,))              # блюдце
    for (x, y) in ((6, 6), (7, 5), (7, 4), (8, 6), (9, 5), (9, 3)):
        top.putpixel((x, y), (210, 205, 200, 255))                   # пар
    for i in range(16):                                              # латунная рамка
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            top.putpixel((x, y), (176, 132, 64, 255))
    top.save(os.path.join(folder, "block", "cafe_sign_top.png"))
    side = wood(rng, dark)
    for x in range(16):
        side.putpixel((x, 0), (176, 132, 64, 255))
        side.putpixel((x, 7), (150, 104, 50, 255))
    side.save(os.path.join(folder, "block", "cafe_sign_side.png"))
    wood(rng, mul(dark, 0.8)).save(os.path.join(folder, "block", "cafe_sign_bottom.png"))


def cafe_owner(folder):
    """Хозяин кофейни: немолодой мужчина, аккуратные седые волосы, белая рубашка, чёрный жилет, бордовый галстук, коричневый фартук."""
    sk = Skin(4404)
    skin_c, hair = 0xE6CDB4, 0x9A9AA2
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((86, 70, 56), (52, 42, 34)), hair,
               [4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4], mouth="calm")
    f = sk.local("head", "front")
    for x in (3, 4, 5, 10, 11, 12):
        f.put(x, 7, (60, 56, 52))                                    # тонкие очки
    f.put(7, 8, (60, 56, 52)); f.put(8, 8, (60, 56, 52))
    for x in range(5, 11):
        f.put(x, 12, mix(skin_c, (150, 150, 155), 0.45))             # аккуратная бородка
    paint_hat_hair(sk, hair, [2, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 2],
                   lambda d: 6, lambda x: 8)
    shirt, vest, tie, apron = 0xEDEBE6, 0x18181C, 0x6A1220, 0x6B4A2E
    sk.cloth("body", ("top", "bottom") + SIDES, vest)
    bf = sk.local("body", "front")
    for y in range(0, 12):
        for x in (6, 7, 8, 9):
            bf.put(x, y, shirt)                                      # рубашка в вырезе жилета
    for y in range(1, 10):
        bf.put(7, y, tie); bf.put(8, y, mul(tie, 0.85))              # галстук
    for y in range(12, 24):
        for x in range(1, 15):
            bf.put(x, y, mul(apron, 1.0 - 0.01 * (y - 12)))          # фартук
    for x in range(1, 15):
        bf.put(x, 12, mul(apron, 0.75))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, shirt, skin_c, cuff=mul(shirt, 0.9), sleeve_rows=16)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x2A2A30, 0x1A1410, 0x0A0806, shoe_rows=6)
    for p in ("rpants", "lpants"):
        paint_pants_overlay(sk, p, 0x2A2A30, 17)
    save(sk, os.path.join(folder, "entity"), "cafe_owner_0")


if __name__ == "__main__":
    out = sys.argv[1]
    for t in (1, 2, 3):
        core_icon(t).save(os.path.join(out, "item", f"rc_core_{t}.png"))
    sign_textures(out)
    cafe_owner(out)
    print("ok")
