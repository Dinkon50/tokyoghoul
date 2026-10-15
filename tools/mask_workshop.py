"""
Мастерская масок: оригинальные текстуры блока 16 x 16 (верх — стол мастера с заготовкой маски
и инструментами, бок — тёмное дерево с висящими масками, низ — тёмные доски).
Запуск: python3 mask_workshop.py <папка textures>
"""
import math
import os
import random
import sys

from PIL import Image

WOOD = (92, 58, 36)
DARK = (58, 36, 24)
BONE = (226, 218, 198)
RED = (168, 28, 34)
METAL = (150, 156, 164)


def planks(rng, c, rows=4):
    img = Image.new("RGBA", (16, 16))
    h = 16 // rows
    for y in range(16):
        row = y // h
        shade = 0.92 + 0.08 * (row % 2)
        for x in range(16):
            n = rng.uniform(-1, 1)
            k = shade + 0.06 * n + 0.04 * math.sin((x + row * 5) * 0.9)
            if y % h == 0:
                k *= 0.7
            if (x + row * 7) % 16 == 0:
                k *= 0.75
            img.putpixel((x, y), tuple(max(0, min(255, int(v * k))) for v in c) + (255,))
    return img


def mask(img, ox, oy, w, h, color, eyes=RED):
    cx, cy = ox + w / 2 - 0.5, oy + h / 2 - 0.5
    for y in range(oy, oy + h):
        for x in range(ox, ox + w):
            if ((x - cx) / (w / 2)) ** 2 + ((y - cy) / (h / 2)) ** 2 <= 1.0:
                k = 1.0 - 0.18 * max(0.0, (y - cy) / h)
                img.putpixel((x, y), tuple(int(v * k) for v in color) + (255,))
    ey = oy + h // 2 - 1
    for ex in (ox + w // 2 - 2, ox + w // 2 + 1):
        if 0 <= ex < 16 and 0 <= ey < 16:
            img.putpixel((ex, ey), eyes + (255,))
    return img


def top():
    rng = random.Random(11)
    img = planks(rng, WOOD)
    # рамка стола
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(p, DARK + (255,))
    # заготовка маски в центре
    mask(img, 4, 3, 8, 9, BONE)
    # зубы: тёмные щели на нижней части маски
    for x in range(5, 11):
        img.putpixel((x, 9), (70, 50, 44, 255) if x % 2 else BONE + (255,))
    img.putpixel((5, 6), RED + (255,))
    img.putpixel((4, 7), RED + (255,))
    # резец и игла
    for i in range(4):
        img.putpixel((2, 9 + i), METAL + (255,))
    img.putpixel((2, 13), DARK + (255,))
    for i in range(5):
        img.putpixel((13, 3 + i), (200, 200, 210, 255))
    img.putpixel((13, 8), RED + (255,))
    return img


def side():
    rng = random.Random(23)
    img = planks(rng, DARK, rows=2)
    # верхняя кромка столешницы
    for x in range(16):
        img.putpixel((x, 0), WOOD + (255,))
        img.putpixel((x, 1), tuple(int(v * 0.8) for v in WOOD) + (255,))
    # две маски на гвоздях
    mask(img, 1, 5, 6, 7, BONE)
    mask(img, 9, 5, 6, 7, (60, 60, 64), eyes=(220, 40, 40))
    for x in (4, 11):
        img.putpixel((x, 3), METAL + (255,))
        img.putpixel((x, 4), (110, 110, 116, 255))
    return img


def bottom():
    return planks(random.Random(31), DARK)


def main(root):
    out = os.path.join(root, "block")
    os.makedirs(out, exist_ok=True)
    top().save(os.path.join(out, "mask_workshop_top.png"))
    side().save(os.path.join(out, "mask_workshop_side.png"))
    bottom().save(os.path.join(out, "mask_workshop_bottom.png"))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "../src/main/resources/assets/tokyoghoulrpg/textures")
