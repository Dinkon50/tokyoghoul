"""
Броня особого класса CCG: развёртка 128 x 64 (как остальная броня мода) и иконки 16 x 16.
Оригинальный дизайн: графитовые пластины, серебряная окантовка, голубые светящиеся линии, визор на шлеме.
Запуск: python3 elite_armor.py <папка textures>
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import SHADE, SIDES, mul  # noqa: E402
from armor_hd import _armor, fill, save  # noqa: E402

PLATE, DARK, TRIM, GLOW = (58, 62, 72), (30, 32, 38), (196, 200, 208), (80, 220, 240)


def plates(sk, part, faces):
    fill(sk, part, faces, PLATE, noise=0.03, grad=0.12)
    for face in faces:
        f = sk.local(part, face)
        for x in range(f.width):                                # швы между пластинами
            for y in range(4, f.height, 6):
                f.put(x, y, mul(DARK, SHADE.get(face, 1.0)))


def layer1(folder):
    sk = _armor(41)
    # шлем: закрытый, визор с голубой полосой, серебряный гребень
    plates(sk, "head", ("top", "bottom") + SIDES)
    f = sk.local("head", "front")
    for x in range(2, 14):
        f.put(x, 7, DARK); f.put(x, 8, GLOW); f.put(x, 9, mul(GLOW, 0.7))
    for y in range(10, 16):
        f.put(2, y, TRIM); f.put(13, y, TRIM)
    for x in range(16):
        f.put(x, 0, TRIM)
    t = sk.local("head", "top")
    for y in range(t.height):
        t.put(7, y, TRIM); t.put(8, y, mul(TRIM, 0.85))
    for face in ("right", "left", "back"):
        s = sk.local("head", face)
        for x in range(s.width):
            s.put(x, 8, mul(GLOW, 0.55 * SHADE[face]))
    # нагрудник: эмблема-ромб, голубые линии, наплечники
    plates(sk, "body", ("top", "bottom") + SIDES)
    f = sk.local("body", "front")
    for y in range(24):
        f.put(7, y, mul(DARK, 0.9)); f.put(8, y, mul(DARK, 0.9))
    for d in range(4):                                          # ромб
        for (x, y) in ((7 - d, 6 + d), (8 + d, 6 + d), (7 - d, 12 - d), (8 + d, 12 - d)):
            f.put(x, y, TRIM)
    f.put(7, 9, GLOW); f.put(8, 9, GLOW)
    for y in range(2, 22, 5):
        f.put(1, y, GLOW); f.put(14, y, GLOW)
    for x in range(16):
        f.put(x, 0, TRIM); f.put(x, 23, mul(TRIM, 0.8))
    for arm in ("rarm",):
        plates(sk, arm, ("top",) + SIDES)
        for face in SIDES:
            a = sk.local(arm, face)
            for x in range(a.width):
                a.put(x, 0, TRIM); a.put(x, 1, mul(TRIM, 0.8))
                a.put(x, 10, mul(GLOW, 0.7 * SHADE[face]))
                for y in range(20, 24):
                    a.put(x, y, (0, 0, 0, 0))                   # кисти открыты
    # ботинки: нижние 8 пикселей ноги
    for face in SIDES:
        g = sk.local("rleg", face)
        for y in range(16, 24):
            for x in range(g.width):
                c = DARK if y == 23 else mul(PLATE, SHADE[face] * (1.15 if y == 16 else 1.0))
                g.put(x, y, c)
        for x in range(g.width):
            g.put(x, 19, mul(GLOW, 0.6 * SHADE[face]))
    b = sk.local("rleg", "bottom")
    b.rect(0, 0, b.width, b.height, DARK)
    save(sk, folder, "ccg_elite_layer_1")


def layer2(folder):
    sk = _armor(42)
    for face in SIDES:
        w = sk.local("body", face)
        for y in range(18, 24):
            for x in range(w.width):
                w.put(x, y, mul(PLATE, SHADE[face]))
        for x in range(w.width):
            w.put(x, 18, TRIM)
    plates(sk, "rleg", ("top", "bottom") + SIDES)
    for face in SIDES:
        g = sk.local("rleg", face)
        for y in range(g.height):
            g.put(g.width // 2, y, mul(GLOW, 0.5 * SHADE[face]) if y % 6 == 2 else mul(DARK, SHADE[face]))
        for x in range(g.width):
            g.put(x, 8, mul(TRIM, SHADE[face]))                  # наколенник
    save(sk, folder, "ccg_elite_layer_2")


def icon(kind):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shapes = {
        "helmet": ["..XXXXXXXXXX....", ".XXXXXXXXXXXX...", ".XXXXXXXXXXXX...", ".XGGGGGGGGGGX...",
                   ".XXXXXXXXXXXX...", ".XX........XX...", ".XX........XX..."],
        "chestplate": ["XXX......XXX", "XXXX....XXXX", "XXXXXXXXXXXX", ".XXXXTTXXXX.", ".XXXTGGTXXX.",
                       ".XXXXTTXXXX.", ".XXXXXXXXXX.", ".XXGXXXXGXX.", ".XXXXXXXXXX.", ".XXXXXXXXXX.", "..XXXXXXXX.."],
        "leggings": ["XXXXXXXXXX", "XXXXTTXXXX", "XXXX..XXXX", "XXX....XXX", "XGX....XGX", "XXX....XXX",
                     "XXX....XXX", "TTT....TTT", "XXX....XXX", "XXX....XXX"],
        "boots": ["XXX....XXX", "XXX....XXX", "XGX....XGX", "XXXX...XXXX", "XXXXX..XXXXX", "DDDDD..DDDDD"],
    }
    rows = shapes[kind]
    oy = (16 - len(rows)) // 2
    for y, row in enumerate(rows):
        ox = (16 - len(row)) // 2
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            c = {"X": mul(PLATE, 1.2 - 0.04 * y), "G": GLOW, "T": TRIM, "D": DARK}[ch]
            img.putpixel((ox + x, oy + y), c + (255,))
    # тёмный контур
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), (18, 18, 22, 255))
    return out


if __name__ == "__main__":
    root = sys.argv[1]
    layer1(os.path.join(root, "models", "armor"))
    layer2(os.path.join(root, "models", "armor"))
    for k in ("helmet", "chestplate", "leggings", "boots"):
        icon(k).save(os.path.join(root, "item", f"ccg_elite_{k}.png"))
    print("ok")
