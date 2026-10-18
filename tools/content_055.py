"""
Контент 0.55.0 (оригинальное): скин стража территории клана.
Запуск: python3 content_055.py <папка assets/tokyoghoulrpg/textures>
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import (SHADE, Skin, coat_skirt, mix, mul, paint_arm, paint_face,  # noqa: E402
                      paint_head_rest, paint_leg, save)


def guard(folder):
    """Страж клана: короткая стрижка, глухой стальной полумаск-визор на глазах с узкой прорезью,
    тёмно-графитовый стёганый доспех с наплечниками, красный кушак клана, латные перчатки и сапоги."""
    sk = Skin(5505)
    skin_c, hair = (198, 160, 130), (30, 28, 30)
    paint_head_rest(sk, skin_c, hair, "short")
    paint_face(sk, skin_c, ((150, 40, 40), (90, 20, 20)), hair,
               [2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2], mouth="stern")
    f = sk.local("head", "front")
    steel, steel_dark, slit = (132, 136, 144), (86, 90, 98), (20, 10, 12)
    for y in range(4, 10):                                   # визор на глазах
        for x in range(1, 15):
            f.put(x, y, mul(steel, 1.0 - 0.03 * (y - 4) + (0.06 if y == 4 else 0.0)))
    for x in range(3, 13):
        f.put(x, 7, slit)                                    # узкая прорезь
    for x in (3, 12):
        f.put(x, 6, (190, 40, 40))                           # красные огоньки в прорези
    for x in (1, 14):
        for y in range(4, 10):
            f.put(x, y, steel_dark)
    for face in ("right", "left"):
        s = sk.local("head", face)
        for y in range(4, 10):
            for x in range(s.width):
                s.put(x, y, mul(steel_dark, SHADE[face]))       # ремень визора
    # корпус: стёганый графитовый доспех
    armor, armor_dark, sash = (58, 60, 66), (38, 40, 46), (150, 26, 30)
    sk.whole("body", armor)
    bf = sk.local("body", "front")
    for y in range(bf.height):
        for x in range(bf.width):
            quilt = 0.92 if (y % 4 == 0 or x % 4 == 0) else 1.0
            bf.put(x, y, mul(armor, quilt))
    for x in range(bf.width):
        for y in (14, 15, 16):
            bf.put(x, y, mul(sash, 1.0 - 0.06 * (y - 14)))     # кушак
    for y in range(16, 22):
        bf.put(10, y, mul(sash, 0.9)); bf.put(11, y, mul(sash, 0.8))   # свисающий конец
    for y in range(0, 14):
        bf.put(8, y, armor_dark)                              # застёжка
    for face in ("back", "right", "left"):
        b = sk.local("body", face)
        for y in (14, 15, 16):
            for x in range(b.width):
                b.put(x, y, mul(sash, SHADE[face]))
    for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
        paint_arm(sk, arm, armor, skin_c, cuff=steel_dark, sleeve_rows=24)
        for face in ("front", "back", "right", "left"):
            a = sk.local(arm, face)
            for y in range(18, a.height):
                for x in range(a.width):
                    a.put(x, y, mul(steel_dark, SHADE.get(face, 1.0)))   # латные перчатки
        top = sk.local(sleeve, "top")
        for y in range(top.height):
            for x in range(top.width):
                top.put(x, y, mul(steel, SHADE["top"]))       # наплечник
        for face in ("front", "back", "right", "left"):
            sl = sk.local(sleeve, face)
            for y in range(0, 5):
                for x in range(sl.width):
                    sl.put(x, y, mul(steel, SHADE.get(face, 1.0) * (1.0 - 0.05 * y)))
    pants, boots = (44, 44, 50), (70, 72, 80)
    for leg in ("rleg", "lleg"):
        paint_leg(sk, leg, pants, boots, (30, 30, 34), shoe_rows=10)
    for p in ("rpants", "lpants"):
        coat_skirt(sk, p, armor_dark, 6, trim=sash)
    save(sk, os.path.join(folder, "entity"), "guard_0")


if __name__ == "__main__":
    guard(sys.argv[1])
    print("ok")
