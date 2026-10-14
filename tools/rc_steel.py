"""
Руда RC-стали (камень и глубинный сланец), сырая RC-сталь и слиток. Оригинальные текстуры 16 x 16.
Запуск: python3 rc_steel.py <папка textures>
"""
import math
import os
import random
import sys

from PIL import Image

GEM = [(48, 70, 92), (90, 170, 200), (150, 230, 245), (230, 252, 255)]   # от тени к блику


def base(rng, c, strata=False):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.uniform(-1, 1)
            k = 1.0 + 0.08 * n + (0.05 * math.sin(y * 1.7) if strata else 0.0)
            if rng.random() < 0.08:
                k *= 0.82
            img.putpixel((x, y), tuple(max(0, min(255, int(v * k))) for v in c) + (255,))
    return img


def flecks(img, rng, clusters):
    for cx, cy in clusters:
        pts = [(cx, cy), (cx + 1, cy), (cx, cy + 1), (cx + 1, cy + 1), (cx - 1, cy), (cx + 1, cy - 1)]
        for i, (x, y) in enumerate(pts[:rng.randint(4, 6)]):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), GEM[min(3, 1 + (i == 0) + (i == 1 and rng.random() < 0.5))] + (255,))
        if 0 <= cx + 2 < 16 and 0 <= cy + 1 < 16:
            img.putpixel((cx + 2, cy + 1), GEM[0] + (255,))


def ore(seed, c, strata):
    rng = random.Random(seed)
    img = base(rng, c, strata)
    flecks(img, rng, [(3, 3), (10, 2), (6, 8), (12, 10), (2, 12), (9, 13)])
    return img


def raw():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(77)
    cx, cy = 7.5, 8.5
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - cx) * 0.9, y - cy) + 0.8 * math.sin(math.atan2(y - cy, x - cx) * 4)
            if d < 5.6:
                c = (110, 116, 124) if rng.random() < 0.6 else (84, 90, 98)
                if rng.random() < 0.3:
                    c = GEM[rng.randint(1, 2)]
                if d > 4.6:
                    c = tuple(int(v * 0.6) for v in c[:3])
                img.putpixel((x, y), c[:3] + (255,))
    img.putpixel((6, 5), GEM[3] + (255,))
    return img


def ingot():
    """Слиток: верхняя грань светлая с бликом, передняя темнее, голубая жилка RC по боку."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = [
        "....TTTTTTTT....",
        "...THHTTTTTTT...",
        "..TTTTTTTTTTTT..",
        ".FFFFFFFFFFFFFF.",
        ".FFFFGGGGGGFFFFS",
        ".FFFFFFFFFFFFFFS",
        ".DDDDDDDDDDDDDDS",
    ]
    col = {"T": (178, 214, 228), "H": (240, 252, 255), "F": (124, 152, 170), "G": (90, 190, 220),
           "D": (70, 88, 104), "S": (58, 72, 86)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y + 5), col[ch] + (255,))
    return img


if __name__ == "__main__":
    root = sys.argv[1]
    ore(901, (124, 124, 126), False).save(os.path.join(root, "block", "rc_steel_ore.png"))
    ore(902, (78, 78, 84), True).save(os.path.join(root, "block", "deepslate_rc_steel_ore.png"))
    raw().save(os.path.join(root, "item", "raw_rc_steel.png"))
    ingot().save(os.path.join(root, "item", "rc_steel_ingot.png"))
    print("ok")
