"""
Знамя клана: оригинальные текстуры 16 x 16 (бок — алое полотнище с тёмной эмблемой-когтем на древке,
верх и низ — тёмное дерево с золотой оковкой).
Запуск: python3 clan_banner.py <папка textures>
"""
import os
import random
import sys

from PIL import Image

RED, DARK_RED, GOLD, WOOD = (168, 22, 30), (96, 10, 16), (212, 170, 60), (60, 40, 26)


def side():
    rng = random.Random(4242)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 1.0 + rng.uniform(-0.05, 0.05) - 0.012 * y
            img.putpixel((x, y), tuple(int(c * k) for c in RED) + (255,))
    for x in range(16):
        img.putpixel((x, 0), GOLD + (255,))
        img.putpixel((x, 1), WOOD + (255,))
        img.putpixel((x, 15), DARK_RED + (255,))
    for x in (2, 13):
        for y in range(2, 15):
            img.putpixel((x, y), DARK_RED + (255,))
    # эмблема: три когтя
    for i, cx in enumerate((5, 8, 11)):
        for y in range(4, 12):
            x = cx + (y - 4) // 3 - 1
            if 0 <= x < 16:
                img.putpixel((x, y), (30, 6, 10, 255))
        img.putpixel((cx - 1, 3), GOLD + (255,))
    return img


def top():
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(77)
    for y in range(16):
        for x in range(16):
            k = 1.0 + rng.uniform(-0.06, 0.06)
            img.putpixel((x, y), tuple(int(c * k) for c in WOOD) + (255,))
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(p, GOLD + (255,))
    for x in range(6, 10):
        for y in range(6, 10):
            img.putpixel((x, y), RED + (255,))
    return img


if __name__ == "__main__":
    out = os.path.join(sys.argv[1], "block")
    side().save(os.path.join(out, "clan_banner_side.png"))
    top().save(os.path.join(out, "clan_banner_top.png"))
    print("ok")
