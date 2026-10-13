"""
Квинке: переводит модели Blockbench (полигональные меши, .bbmodel) в файлы мода и рисует иконки.

  python3 quinque_mesh.py <папка с .bbmodel> <src/main/resources/assets/tokyoghoulrpg>

Что получается:
  quinque/<имя>.tgqm          — сетка модели для отрисовки в руке (двоичный файл, см. ниже)
  textures/quinque/palette.png — общая текстура всех квинке
  textures/item/<имя>.png     — иконка 32 x 32 для инвентаря (отрисовка той же модели)

Формат .tgqm (big-endian, как DataInputStream в Java):
  "TGQM", int версия=1, int число частей;
  у каждой части: UTF имя, 3 float точка вращения (в блоках), int число четырёхугольников,
  затем на каждый четырёхугольник 4 вершины по 5 float (x, y, z в блоках; u, v от 0 до 1)
  и 3 float нормали. Треугольники записаны как четырёхугольник с повторённой последней вершиной.
Начало координат — точка хвата, модель вытянута вдоль +Y (как в Blockbench у Jason, IXA, Kura).
Narukami в исправленной версии вытянута вдоль +Z: её поворачиваем.
"""
import base64
import io
import json
import math
import os
import struct
import sys

from PIL import Image

# имя в моде -> (файл, поворот +Z в +Y)
MODELS = {
    "quinque_jason": ("Anime revision/13s_Jason.bbmodel", False),
    "quinque_ixa": ("Anime revision/IXA.bbmodel", False),
    "quinque_kura": ("Anime revision/Kura.bbmodel", False),
    "quinque_narukami": ("Narukami corrected/Narukami_Closed.bbmodel", True),
    "quinque_narukami_open": ("Narukami corrected/Narukami_Open.bbmodel", True),
}
ICONS = ["quinque_jason", "quinque_ixa", "quinque_kura", "quinque_narukami"]


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def norm(a):
    n = math.sqrt(dot(a, a))
    return (a[0] / n, a[1] / n, a[2] / n) if n > 1e-12 else (0.0, 1.0, 0.0)


def sort_quad(keys, pos):
    """Blockbench хранит вершины четырёхугольника не обязательно по кругу: выбираем порядок без самопересечения."""
    best, best_score = keys, -1e18
    for order in ((0, 1, 2, 3), (0, 1, 3, 2), (0, 2, 1, 3)):
        p = [pos[keys[i]] for i in order]
        n = cross(sub(p[2], p[0]), sub(p[3], p[1]))
        score = min(dot(cross(sub(p[(i + 1) % 4], p[i]), sub(p[(i + 2) % 4], p[(i + 1) % 4])), n) for i in range(4))
        if score > best_score:
            best, best_score = [keys[i] for i in order], score
    return best


def load(path, z_to_y):
    d = json.load(open(path))
    tex_w = d["textures"][0].get("uv_width") or d["resolution"]["width"]
    tex_h = d["textures"][0].get("uv_height") or d["resolution"]["height"]
    uuid_part = {}
    pivots = {}

    def walk(nodes, part):
        for n in nodes:
            if isinstance(n, dict):
                name = n["name"]
                pivots[name] = n.get("origin", [0, 0, 0])
                walk(n.get("children", []), name)
            else:
                uuid_part[n] = part

    walk(d["outliner"], None)

    def fix(v):
        x, y, z = v
        return (x, z, -y) if z_to_y else (x, y, z)

    parts = {}
    for e in d["elements"]:
        if e.get("type") != "mesh" or e.get("visibility") is False:
            continue
        o = e.get("origin", [0, 0, 0])
        assert not any(abs(a) > 1e-6 for a in e.get("rotation", [0, 0, 0])), "поворот меша не поддержан"
        pos = {k: fix((o[0] + v[0], o[1] + v[1], o[2] + v[2])) for k, v in e["vertices"].items()}
        quads = parts.setdefault(uuid_part.get(e["uuid"], "root"), [])
        for face in e["faces"].values():
            keys = list(face["vertices"])
            if len(keys) == 4:
                keys = sort_quad(keys, pos)
            elif len(keys) == 3:
                keys = keys + [keys[2]]
            else:
                continue
            p = [pos[k] for k in keys]
            n = norm(cross(sub(p[2], p[0]), sub(p[3], p[1])) if len(set(face["vertices"])) == 4
                     else cross(sub(p[1], p[0]), sub(p[2], p[0])))
            verts = [(pp[0] / 16.0, pp[1] / 16.0, pp[2] / 16.0,
                      face["uv"][k][0] / tex_w, face["uv"][k][1] / tex_h) for pp, k in zip(p, keys)]
            quads.append((verts, n))
    texture = None
    src = d["textures"][0].get("source", "")
    if src.startswith("data:image"):
        texture = Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")
    return parts, {k: fix(v) for k, v in pivots.items()}, texture


def write_tgqm(path, parts, pivots):
    out = io.BytesIO()
    out.write(b"TGQM")
    out.write(struct.pack(">ii", 1, len(parts)))
    for name, quads in parts.items():
        b = name.encode("utf-8")
        out.write(struct.pack(">H", len(b)) + b)
        pv = pivots.get(name, (0, 0, 0))
        out.write(struct.pack(">fff", pv[0] / 16.0, pv[1] / 16.0, pv[2] / 16.0))
        out.write(struct.pack(">i", len(quads)))
        for verts, n in quads:
            for v in verts:
                out.write(struct.pack(">fffff", *v))
            out.write(struct.pack(">fff", *n))
    open(path, "wb").write(out.getvalue())
    return out.tell()


# ------------------------------------------------------------------ иконки
def render_icon(parts, texture, size=32, ss=8):
    """Простая отрисовка сетки: вид спереди, модель повёрнута по диагонали (остриём вправо вверх), как меч в инвентаре."""
    big = size * ss
    tris = []
    a = math.radians(-45.0)
    ca, sa = math.cos(a), math.sin(a)
    for quads in parts.values():
        for verts, n in quads:
            pts = []
            for x, y, z, u, v in verts:
                rx, ry = x * ca - y * sa, x * sa + y * ca
                pts.append((rx, ry, z, u, v))
            nx, ny = n[0] * ca - n[1] * sa, n[0] * sa + n[1] * ca
            shade = 0.55 + 0.45 * max(0.0, dot(norm((nx, ny, n[2])), norm((-0.3, 0.5, 0.8))))
            shade = max(shade, 0.55 + 0.45 * max(0.0, dot(norm((-nx, -ny, -n[2])), norm((-0.3, 0.5, 0.8)))) * 0.6)
            tris.append((pts[0], pts[1], pts[2], shade))
            tris.append((pts[0], pts[2], pts[3], shade))
    xs = [p[0] for t in tris for p in t[:3]]
    ys = [p[1] for t in tris for p in t[:3]]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    span = max(maxx - minx, maxy - miny)
    margin = 1.0 * ss
    k = (big - 2 * margin) / span
    ox = margin + ((big - 2 * margin) - (maxx - minx) * k) / 2
    oy = margin + ((big - 2 * margin) - (maxy - miny) * k) / 2
    zbuf = [[-1e9] * big for _ in range(big)]
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    px = img.load()
    tex = texture.load()
    tw, th = texture.size
    for p0, p1, p2, shade in tris:
        P = [((p[0] - minx) * k + ox, big - ((p[1] - miny) * k + oy), p[2], p[3], p[4]) for p in (p0, p1, p2)]
        x0, x1 = int(max(0, math.floor(min(q[0] for q in P)))), int(min(big - 1, math.ceil(max(q[0] for q in P))))
        y0, y1 = int(max(0, math.floor(min(q[1] for q in P)))), int(min(big - 1, math.ceil(max(q[1] for q in P))))
        (ax, ay, az, au, av), (bx, by, bz, bu, bv), (cx, cy, cz, cu, cv) = P
        den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(den) < 1e-9:
            continue
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1):
                fx, fy = xx + 0.5, yy + 0.5
                w0 = ((by - cy) * (fx - cx) + (cx - bx) * (fy - cy)) / den
                w1 = ((cy - ay) * (fx - cx) + (ax - cx) * (fy - cy)) / den
                w2 = 1 - w0 - w1
                if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                    continue
                z = w0 * az + w1 * bz + w2 * cz
                if z <= zbuf[yy][xx]:
                    continue
                zbuf[yy][xx] = z
                u = (w0 * au + w1 * bu + w2 * cu) * tw
                v = (w0 * av + w1 * bv + w2 * cv) * th
                c = tex[min(tw - 1, max(0, int(u))), min(th - 1, max(0, int(v)))]
                px[xx, yy] = (int(c[0] * shade), int(c[1] * shade), int(c[2] * shade), 255)
    small = img.resize((size, size), Image.LANCZOS)
    # чёткие края как у пиксельных иконок: альфа либо 0, либо 255
    data = [(r, g, b, 255 if al > 90 else 0) for r, g, b, al in small.getdata()]
    small.putdata(data)
    # тёмный контур, чтобы иконка читалась на любом фоне
    out = small.copy()
    sp, op = small.load(), out.load()
    for y in range(size):
        for x in range(size):
            if sp[x, y][3] == 0 and any(0 <= x + dx < size and 0 <= y + dy < size and sp[x + dx, y + dy][3] > 0
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                op[x, y] = (20, 18, 24, 255)
    return out


if __name__ == "__main__":
    src_dir, assets = sys.argv[1], sys.argv[2]
    os.makedirs(os.path.join(assets, "quinque"), exist_ok=True)
    os.makedirs(os.path.join(assets, "textures", "quinque"), exist_ok=True)
    palette = None
    meshes = {}
    for name, (rel, z_to_y) in MODELS.items():
        parts, pivots, tex = load(os.path.join(src_dir, rel), z_to_y)
        meshes[name] = (parts, tex)
        palette = palette or tex
        size = write_tgqm(os.path.join(assets, "quinque", name + ".tgqm"), parts, pivots)
        print(name, "частей", len(parts), "четырёхугольников", sum(len(q) for q in parts.values()), "байт", size)
    palette.save(os.path.join(assets, "textures", "quinque", "palette.png"))
    for name in ICONS:
        parts, tex = meshes[name]
        render_icon(parts, tex).save(os.path.join(assets, "textures", "item", name + ".png"))
        print("иконка", name)
