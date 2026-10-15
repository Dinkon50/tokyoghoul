"""
Алый Монарх (оригинальный персонаж): скин 128 x 128 и иконка Алой короны 16 x 16.
Чёрные волосы назад, оба глаза-какугана, чёрная полумаска с алым швом, золотой обруч,
длинный тёмно-алый плащ с золотой каймой и высоким воротом, чёрные перчатки и сапоги.
Запуск: python3 monarch_assets.py <папка textures>
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import SIDES, SHADE, Skin, mix, mul, paint_arm, paint_face, paint_hat_hair, paint_head_rest, paint_leg, save  # noqa: E402
from skins_half_ghoul import kakugan_one  # noqa: E402
from skins_hd import coat_skirt  # noqa: E402

GOLD, CRIMSON, BLACK = (214, 168, 64), (120, 10, 24), (16, 12, 14)


def monarch(folder):
    sk = Skin(4505)
    skin_c, hair = 0xE4CFC4, 0x141016
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((40, 40, 44), (20, 20, 24)), hair,
               [3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3], mouth="stern")
    kakugan_one(sk, skin_c, 0, iris=(255, 30, 50))
    kakugan_one(sk, skin_c, 1, iris=(255, 30, 50))
    f = sk.local("head", "front")
    for y in range(11, 16):                                  # чёрная полумаска на нижней части лица
        for x in range(2, 14):
            f.put(x, y, mul(BLACK, 1.0 + 0.04 * (y - 11)))
    for x in range(4, 12):
        f.put(x, 13, CRIMSON if x % 2 == 0 else mul(CRIMSON, 0.7))   # алый шов
    paint_hat_hair(sk, hair, [2, 2, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 2, 2], lambda d: 7, lambda x: 9)
    hf = sk.local("hat", "front")
    for x in range(16):                                      # золотой обруч
        hf.put(x, 3, GOLD if x % 3 else mul(GOLD, 1.2))
    for x in (3, 7, 8, 12):
        hf.put(x, 2, GOLD)
    for face in ("right", "left", "back"):
        hs = sk.local("hat", face)
        for x in range(hs.width):
            hs.put(x, 3, mul(GOLD, SHADE[face]))
    coat = (70, 8, 18)
    sk.cloth("body", ("top", "bottom") + SIDES, BLACK)
    sk.cloth("jacket", ("top", "bottom", "back", "right", "left"), coat, noise=0.03)
    jf = sk.local("jacket", "front")
    for y in range(24):
        for x in range(16):
            c = mul(coat, (1.08 - 0.1 * y / 24) * (1 + sk.rng.uniform(-0.03, 0.03)))
            if 6 < x < 9:
                c = mul(BLACK, 1.2)                          # распах плаща
            if x in (6, 9):
                c = GOLD                                     # золотая кайма
            if y < 4 and x in (1, 2, 13, 14):
                c = mul(GOLD, 0.9)                           # высокий ворот
            jf.put(x, y, c)
    for y in (6, 10, 14):
        jf.put(5, y, GOLD); jf.put(10, y, GOLD)
    jb = sk.local("jacket", "back")
    for y in range(24):
        jb.put(7, y, mul(GOLD, 0.7)); jb.put(8, y, mul(GOLD, 0.7))
    for sleeve in ("rsleeve", "lsleeve"):
        sk.cloth(sleeve, ("top",) + SIDES, coat, noise=0.03, rows=(0, 17))
    for arm in ("rarm", "larm"):
        paint_arm(sk, arm, coat, BLACK, cuff=GOLD, sleeve_rows=17)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, 0x120C10, 0x0A0608, 0x050304, shoe_rows=13)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, coat, 18, trim=GOLD)
    save(sk, os.path.join(folder, "entity"), "crimson_monarch_0")


def crown(folder):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = [
        "..G...G...G..",
        "..G..GRG..G..",
        ".GGG.GGG.GGG.",
        ".GGGGGGGGGGG.",
        ".GRGGGRGGGRG.",
        ".GGGGGGGGGGG.",
        "..DDDDDDDDD..",
    ]
    col = {"G": (226, 178, 70), "R": (220, 20, 44), "D": (140, 96, 30)}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = col[ch]
                if ch == "G" and y > 3:
                    c = mul(c, 0.88)
                img.putpixel((x + 1, y + 5), c + (255,))
    img.putpixel((3, 7), (255, 240, 180, 255))
    img.save(os.path.join(folder, "item", "crimson_crown.png"))


if __name__ == "__main__":
    monarch(sys.argv[1])
    crown(sys.argv[1])
    print("ok")
