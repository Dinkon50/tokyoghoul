"""
Контент 0.43.0 (оригинальное): скин наёмника и иконка контракта наёмника.
Запуск: python3 content_043.py <папка assets/tokyoghoulrpg/textures>
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SIDES, SHADE, Skin, coat_skirt, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_head_rest, paint_leg, save)


def mercenary(folder):
    """Наёмник: капюшон тёмно-оливкового плаща, нижняя половина лица закрыта серым шарфом,
    шрам через бровь, кожаный нагрудник с ремнём, наручи, тёмные штаны, высокие сапоги."""
    sk = Skin(7404)
    skin_c, hair = (214, 176, 146), (52, 40, 32)
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((70, 110, 90), (40, 66, 54)), hair,
               [3, 3, 2, 2, 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3], mouth="stern")
    f = sk.local("head", "front")
    scarf = (96, 98, 96)
    for y in range(11, 16):
        for x in range(0, 16):
            f.put(x, y, mul(scarf, 1.0 - 0.04 * (y - 11) + (0.05 if (x + y) % 4 == 0 else 0.0)))
    for (x, y) in ((11, 4), (11, 5), (12, 6), (12, 7)):
        f.put(x, y, mix(skin_c, (150, 70, 70), 0.6))       # шрам через бровь
    cloak, cloak_dark = (66, 74, 50), (44, 50, 34)
    # капюшон на внешнем слое головы: верх, бока, затылок и рамка вокруг лица
    for face in ("top", "back", "right", "left"):
        h = sk.local("hat", face)
        for y in range(h.height):
            for x in range(h.width):
                h.put(x, y, mul(cloak, SHADE[face] * (1.0 + sk.rng.uniform(-0.04, 0.04))))
    hf = sk.local("hat", "front")
    for x in range(16):
        for y in range(0, 3):
            hf.put(x, y, mul(cloak, 1.02))
    for y in range(3, 16):
        for x in (0, 1, 14, 15):
            hf.put(x, y, mul(cloak_dark, 1.0))
    for face in ("right", "left"):
        s = sk.local("head", face)
        for y in range(11, 16):
            for x in range(16):
                s.put(x, y, mul(scarf, SHADE[face]))
    # корпус: кожаный нагрудник поверх тёмной рубахи, ремень через грудь
    leather, shirt, belt = (98, 66, 42), (40, 40, 44), (60, 40, 26)
    sk.whole("body", shirt)
    bf = sk.local("body", "front")
    for y in range(2, 18):
        for x in range(2, 14):
            bf.put(x, y, mul(leather, 1.0 - 0.01 * y + (0.06 if x in (2, 13) else 0.0)))
    for i in range(0, 16):
        x, y = 2 + i * 12 // 16, 2 + i
        bf.put(x, y, belt); bf.put(x + 1, y, mul(belt, 0.85))  # перевязь
    for x in range(16):
        bf.put(x, 18, belt); bf.put(x, 19, mul(belt, 0.8))      # пояс
    bf.put(8, 18, (190, 170, 90))
    # плащ: спина и плечи на внешнем слое корпуса
    for face in ("back", "right", "left"):
        j = sk.local("jacket", face)
        for y in range(j.height):
            for x in range(j.width):
                j.put(x, y, mul(cloak, SHADE[face] * (1.0 - 0.006 * y)))
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, shirt, skin_c, cuff=leather, sleeve_rows=20)
        a = sk.local(arm, "front")
        for y in range(13, 19):
            for x in range(8):
                a.put(x, y, mul(leather, 0.95))                 # наручи
        sl = sk.local(sleeve, "top")
        for y in range(sl.height):
            for x in range(sl.width):
                sl.put(x, y, mul(cloak, SHADE["top"]))
    pants, boots = (46, 44, 40), (58, 40, 28)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, boots, (24, 16, 10), shoe_rows=9)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, cloak, 8, trim=cloak_dark)
    save(sk, os.path.join(folder, "entity"), "mercenary_0")


def contract_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    paper, ink, seal = (226, 212, 176), (90, 70, 52), (170, 26, 32)
    for y in range(2, 14):
        for x in range(3, 13):
            img.putpixel((x, y), (paper if x not in (3, 12) else tuple(int(c * 0.85) for c in paper)) + (255,))
    for x in range(2, 14):
        img.putpixel((x, 1), (180, 150, 100, 255)); img.putpixel((x, 14), (180, 150, 100, 255))
    for y in (4, 6, 8):
        for x in range(5, 11):
            img.putpixel((x, y), ink + (255,))
    for (x, y) in ((9, 10), (10, 10), (9, 11), (10, 11), (11, 11), (10, 12)):
        img.putpixel((x, y), seal + (255,))                      # печать
    for (x, y) in ((4, 11), (5, 12), (6, 11), (7, 12)):
        img.putpixel((x, y), ink + (255,))                       # подпись
    return img


if __name__ == "__main__":
    out = sys.argv[1]
    mercenary(out)
    contract_icon().save(os.path.join(out, "item", "merc_contract.png"))
    print("ok")
