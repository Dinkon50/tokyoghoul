"""Предпросмотр скинов 128 x 128: вид спереди, сбоку и сзади со всеми слоями, плюс броня поверх."""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from skins_hd import region  # noqa: E402


def crop(img, part, face):
    x, y, w, h = region(part, face)
    return img.crop((x, y, x + w, y + h))


def view(skin, side="front", armor=None, armor2=None, mask=None):
    """Плоский вид 32 x 64 (2x): голова, корпус, руки, ноги, внешние слои и броня."""
    out = Image.new("RGBA", (32, 64), (0, 0, 0, 0))
    layout = {
        "front": [("head", "hat", "front", 8, 0), ("body", "jacket", "front", 8, 16),
                  ("rarm", "rsleeve", "front", 0, 16), ("larm", "lsleeve", "front", 24, 16),
                  ("rleg", "rpants", "front", 8, 40), ("lleg", "lpants", "front", 16, 40)],
        "back": [("head", "hat", "back", 8, 0), ("body", "jacket", "back", 8, 16),
                 ("larm", "lsleeve", "back", 0, 16), ("rarm", "rsleeve", "back", 24, 16),
                 ("lleg", "lpants", "back", 8, 40), ("rleg", "rpants", "back", 16, 40)],
        "side": [("head", "hat", "right", 8, 0), ("body", "jacket", "right", 12, 16),
                 ("rarm", "rsleeve", "right", 12, 16), ("rleg", "rpants", "right", 12, 40)],
    }[side]
    for base, over, face, x, y in layout:
        out.alpha_composite(crop(skin, base, face), (x, y))
        out.alpha_composite(crop(skin, over, face), (x, y))

    def armor_crop(img, part, face):
        # броня 128 x 64: те же координаты, что у скина
        x, y, w, h = region(part, face)
        return img.crop((x, y, x + w, y + h))

    for layer in (armor2, armor):
        if layer is None:
            continue
        for base, _, face, x, y in layout:
            src = {"rarm": "rarm", "larm": "rarm", "rleg": "rleg", "lleg": "rleg"}.get(base, base)
            if base == "head":
                continue
            out.alpha_composite(armor_crop(layer, src, face), (x, y))
    if mask is not None:
        out.alpha_composite(armor_crop(mask, "head", "front" if side == "front" else ("back" if side == "back" else "right")), (8, 0))
    return out


def sheet(entries, path, scale=5, bg=(96, 112, 96, 255)):
    """entries: список (скин, броня1, броня2, маска). Для каждого три вида."""
    cell_w, cell_h = 32 * scale, 64 * scale
    img = Image.new("RGBA", (len(entries) * (3 * cell_w + 24) + 8, cell_h + 16), bg)
    x = 8
    for skin, a1, a2, m in entries:
        for side in ("front", "side", "back"):
            v = view(skin, side, a1, a2, m).resize((cell_w, cell_h), Image.NEAREST)
            img.alpha_composite(v, (x, 8))
            x += cell_w
        x += 24
    img.save(path)


if __name__ == "__main__":
    folder = sys.argv[1]
    names = sys.argv[2].split(",")
    out = sys.argv[3]
    entries = [(Image.open(f"{folder}/{n}.png").convert("RGBA"), None, None, None) for n in names]
    sheet(entries, out)
