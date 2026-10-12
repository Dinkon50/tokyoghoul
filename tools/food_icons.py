"""Иконки еды гуля 16 x 16: плоть (кусок мяса с тёмной кромкой) и кофе (стеклянная бутылочка с тёмным напитком)."""
import os
import sys
from PIL import Image

def px(img, pts, c):
    for x, y in pts:
        img.putpixel((x, y), c + (255,) if len(c) == 3 else c)

def flesh():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # силуэт куска: неровный овал
    rows = {3: (6, 10), 4: (4, 12), 5: (3, 13), 6: (2, 13), 7: (2, 14), 8: (2, 14), 9: (2, 13),
            10: (3, 13), 11: (3, 12), 12: (5, 11), 13: (7, 9)}
    for y, (a, b) in rows.items():
        for x in range(a, b + 1):
            edge = x in (a, b) or y in (3, 13) or (y == 12 and x in (5, 11))
            t = (x - 2) / 12 + (y - 3) / 20
            base = (168, 38, 46)
            c = tuple(int(v * (1.12 - 0.35 * t)) for v in base)
            if edge:
                c = (82, 14, 20)
            img.putpixel((x, y), c + (255,))
    # светлые прожилки жира и блик
    px(img, [(5, 6), (6, 6), (7, 7), (8, 7), (9, 8), (10, 8)], (230, 170, 160))
    px(img, [(5, 9), (6, 10), (7, 10)], (210, 120, 120))
    px(img, [(4, 5), (5, 5), (4, 6)], (240, 120, 120))
    # тёмные капли
    px(img, [(11, 11), (12, 9)], (110, 16, 24))
    px(img, [(8, 14)], (120, 18, 26))
    return img

def coffee():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    glass_edge = (60, 70, 80)
    # горлышко и пробка
    for y in (1, 2):
        for x in (7, 8):
            img.putpixel((x, y), (150, 110, 70, 255))
    for y in (3, 4):
        img.putpixel((6, y), glass_edge + (255,)); img.putpixel((9, y), glass_edge + (255,))
        for x in (7, 8):
            img.putpixel((x, y), (190, 215, 225, 160))
    # тело бутылочки
    body = {5: (5, 10), 6: (4, 11), 7: (4, 11), 8: (4, 11), 9: (4, 11), 10: (4, 11), 11: (4, 11), 12: (4, 11), 13: (5, 10)}
    for y, (a, b) in body.items():
        for x in range(a, b + 1):
            if x in (a, b) or y in (5, 13):
                img.putpixel((x, y), glass_edge + (255,))
            elif y <= 6:
                img.putpixel((x, y), (190, 215, 225, 150))
            else:
                k = 1.0 - 0.05 * (y - 7) - (0.08 if x > 8 else 0)
                c = tuple(int(v * k) for v in (92, 56, 34))
                img.putpixel((x, y), c + (255,))
    # пенка и блик
    for x in range(5, 11):
        img.putpixel((x, 7), (196, 160, 112, 255))
    px(img, [(5, 8), (5, 9), (5, 10)], (150, 120, 96))
    px(img, [(6, 9)], (220, 230, 235))
    # кофейное зёрнышко на этикетке
    px(img, [(8, 10), (9, 10), (8, 11), (9, 11)], (200, 180, 140))
    px(img, [(8, 11)], (120, 80, 50))
    return img

if __name__ == "__main__":
    out = sys.argv[1]
    flesh().save(os.path.join(out, "flesh.png"))
    coffee().save(os.path.join(out, "coffee.png"))
    prev = Image.new("RGBA", (16 * 2 * 12 + 24, 16 * 12 + 16), (90, 100, 90, 255))
    prev.alpha_composite(flesh().resize((192, 192), Image.NEAREST), (8, 8))
    prev.alpha_composite(coffee().resize((192, 192), Image.NEAREST), (208, 8))
    prev.save("/tmp/claude-0/art/food_prev.png")
