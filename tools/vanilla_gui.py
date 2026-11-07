"""Перекрашивает обычные меню Minecraft в стиль мода.

Берёт исходные текстуры 1.20.1 из tools/vanilla/ и кладёт новые в
src/main/resources/assets/minecraft/textures/gui/ (мод подменяет текстуры игры, как ресурспак):
  widgets.png      — кнопки (обычная, наведённая, недоступная); остальное на листе (хотбар, значки) не трогается
  slider.png       — ползунки настроек
  tab_button.png, checkbox.png, options_background.png — вкладки, галочки и фон списков (вместо земли)
  container/*.png, recipe_book.png — инвентарь, сундуки, печи и т. д. в тёмном стиле (подписи светлеют в GuiGraphicsMixin)
  title/background/panorama_0..5.png — панорама главного меню: ночной Токио с красной луной и Токийской башней
Запуск: python3 tools/vanilla_gui.py
"""
import math, os, random
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'tools', 'vanilla')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'minecraft', 'textures', 'gui')
ACC = (224, 48, 58)
DARK_T = (28, 21, 23)
DARK_B = (10, 8, 9)


def mix(a, b, t):
    return tuple(int(round(x + (y - x) * t)) for x, y in zip(a, b))


def button(w, h, state):
    """state: 0 — недоступна, 1 — обычная, 2 — наведена. Края 20×4 не растягиваются (нарезка игры)."""
    im = Image.new('RGBA', (w, h), (0, 0, 0, 255))
    px = im.load()
    rnd = random.Random(7 + state)
    hov = 1.0 if state == 2 else 0.0
    for y in range(h):
        t = y / (h - 1)
        if state == 0:
            c = mix((22, 22, 22), (12, 12, 12), t)
        else:
            c = mix(mix(DARK_T, ACC, 0.10 + 0.24 * hov), mix(DARK_B, ACC, 0.03 + 0.10 * hov), t)
        for x in range(w):
            n = rnd.randint(-2, 2)
            px[x, y] = (max(0, c[0] + n), max(0, c[1] + n), max(0, c[2] + n), 255)
    frame = (42, 42, 42) if state == 0 else mix(mix((14, 10, 11), ACC, 0.55), ACC, hov)
    for x in range(w):
        px[x, 0] = frame + (255,)
        px[x, h - 1] = frame + (255,)
    for y in range(h):
        px[0, y] = frame + (255,)
        px[w - 1, y] = frame + (255,)
    if state:
        hi = mix(px[2, 1][:3], (255, 255, 255), 0.07 + 0.12 * hov)
        for x in range(1, w - 1):
            px[x, 1] = hi + (255,)
        line = mix(ACC, (255, 255, 255), 0.15 * hov) if hov else mix((40, 14, 16), ACC, 0.6)
        for x in range(2, w - 2):
            px[x, h - 2] = line + (255,)
        # уголки в левом верхнем и правом нижнем углу
        b = mix(ACC, (255, 200, 200), 0.3 * hov)
        for i in range(4):
            px[i, 0] = b + (255,)
            px[w - 1 - i, h - 1] = b + (255,)
        for i in range(3):
            px[0, i] = b + (255,)
            px[w - 1, h - 1 - i] = b + (255,)
    return im


def widgets():
    im = Image.open(os.path.join(SRC, 'widgets.png')).convert('RGBA')
    for state, v in ((0, 46), (1, 66), (2, 86)):
        im.paste(button(200, 20, state), (0, v))
    im.save(os.path.join(OUT, 'widgets.png'))


def slider():
    im = Image.open(os.path.join(SRC, 'slider.png')).convert('RGBA')
    # 0 — дорожка, 20 — дорожка в фокусе, 40 — ручка, 60 — ручка под мышью
    for i, v in enumerate((0, 20)):
        b = button(200, 20, 1)
        d = ImageDraw.Draw(b)
        d.rectangle([2, 2, 197, 17], fill=(8, 6, 7, 255))
        d.rectangle([0, 0, 199, 19], outline=mix((60, 20, 24), ACC, 0.4 + 0.6 * i))
        im.paste(b, (0, v))
    for i, v in enumerate((40, 60)):
        h = Image.new('RGBA', (200, 20), (0, 0, 0, 255))
        hp = h.load()
        top = mix(ACC, (255, 255, 255), 0.18 + 0.2 * i)
        bot = mix(ACC, (60, 8, 12), 0.45 - 0.2 * i)
        for y in range(20):
            c = mix(top, bot, y / 19)
            for x in range(200):
                hp[x, y] = c + (255,)
        d = ImageDraw.Draw(h)
        d.rectangle([0, 0, 199, 19], outline=mix((40, 6, 10), (255, 220, 220), 0.6 * i))
        im.paste(h, (0, v))
    im.save(os.path.join(OUT, 'slider.png'))


def remap(c):
    """Серое → тёмное с красным отливом, белое → красный акцент."""
    r, g, b, a = c
    if a == 0:
        return c
    lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    if lum < 0.45:
        o = mix((8, 5, 6), (40, 18, 22), lum / 0.45)
    elif lum < 0.85:
        o = mix((40, 18, 22), ACC, (lum - 0.45) / 0.4)
    else:
        o = mix(ACC, (255, 205, 205), (lum - 0.85) / 0.15)
    return o + (a,)


def recolor(name):
    im = Image.open(os.path.join(SRC, name)).convert('RGBA')
    im.putdata([remap(p) for p in list(im.getdata())])
    im.save(os.path.join(OUT, name))


def background():
    # земля → тёмный камень с бордовым отливом: узор сохраняется, а под списками почти чёрно
    im = Image.open(os.path.join(SRC, 'options_background.png')).convert('RGBA')
    out = []
    for r, g, b, a in list(im.getdata()):
        lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        out.append(mix((26, 12, 15), (96, 40, 46), min(1.0, lum * 1.6)) + (255,))
    im.putdata(out)
    im.save(os.path.join(OUT, 'options_background.png'))


# ---------- панорама ----------
# Один длинный ночной пейзаж на четыре стороны (рисуется вдвое крупнее и сглаживается), потом режется на грани:
# дома на стыках граней не обрываются. Слои: небо с подсвеченными облаками, дальние, средние и ближние дома
# с дымкой между ними, окна по этажам, огни на крышах, неоновые вывески, Токийская башня, улицы внизу.
N = 1024
K = 2                      # во сколько раз крупнее рисуем
HORIZON = 610              # низ домов (в пикселях грани)
SKY_TOP = (5, 3, 8)
SKY_MID = (26, 8, 16)
SKY_LOW = (96, 22, 30)
GROUND = (3, 2, 3)
FOG = (110, 30, 40)


def _np():
    import numpy
    return numpy


def sky_strip(W, H):
    np = _np()
    y = np.arange(H)[:, None] / (HORIZON * K)
    t = np.clip(y, 0, 1)
    top, mid, low = (np.array(c, float) for c in (SKY_TOP, SKY_MID, SKY_LOW))
    a = np.clip(t / 0.65, 0, 1)[..., None]
    b = np.clip((t - 0.65) / 0.35, 0, 1)[..., None] ** 1.6
    col = top * (1 - a) + mid * a
    col = col * (1 - b) + low * b
    img = np.repeat(col, W, axis=1)
    img[int(HORIZON * K):] = GROUND
    # облака: шум, размытый и подсвеченный снизу светом города
    rnd = np.random.default_rng(11)
    def layer_noise(h, w, blur):
        n = Image.fromarray((rnd.random((h, w)) * 255).astype('uint8')).resize((W, H), Image.BICUBIC)
        n = np.asarray(n.filter(ImageFilter.GaussianBlur(blur)), float) / 255.0
        return (n - n.mean()) / (n.std() + 1e-6)
    # вытянутые по горизонтали слои облаков
    cloud = layer_noise(H // 48, W // 12, 18 * K) * 0.7 + layer_noise(H // 20, W // 5, 6 * K) * 0.3
    cloud = np.clip((cloud - 0.3) * 0.7, 0, 1)
    band = np.clip(1 - np.abs(y - 0.45) / 0.35, 0, 1)
    cloud *= band
    lit = np.array((150, 40, 50), float) * (0.25 + 0.75 * t)[..., None]
    img = img * (1 - cloud[..., None] * 0.55) + lit * cloud[..., None] * 0.55
    # звёзды только там, где нет облаков и выше засветки
    stars = rnd.random((H, W)) > 0.99965
    stars &= (cloud < 0.1) & (t < 0.55)
    v = rnd.uniform(90, 220, (H, W))
    for c, k in enumerate((1.0, 0.9, 0.95)):
        img[..., c] = np.where(stars, np.maximum(img[..., c], v * k * (1 - t * 1.4).clip(0.2, 1)), img[..., c])
    return Image.fromarray(img.clip(0, 255).astype('uint8'))


def fog(img, strength, height):
    """Дымка у земли: сильнее всего внизу домов, сходит на нет выше."""
    np = _np()
    W, H = img.size
    y = np.arange(H)[:, None]
    base = HORIZON * K
    a = np.clip(1 - (base - y) / (height * K), 0, 1) ** 1.5 * strength
    a[y[:, 0] > base] = 0.0
    arr = np.asarray(img, float)
    arr = arr * (1 - a[..., None]) + np.array(FOG, float) * a[..., None]
    return Image.fromarray(arr.clip(0, 255).astype('uint8'))


def buildings(d, light, W, rnd, layer):
    """layer 0 — дальние, 1 — средние, 2 — ближние. d — дома, light — огни (потом светятся)."""
    base = HORIZON * K
    hmin, hmax, wmin, wmax = [(70, 260, 14, 46), (50, 230, 26, 70), (25, 150, 40, 110)][layer]
    body = [(40, 14, 20), (18, 7, 11), (8, 4, 6)][layer]
    win = [(1, 2, 3, 4), (2, 3, 5, 6), (3, 4, 7, 8)][layer]   # ширина, высота окна, шаг по x, шаг по y
    x = -rnd.randint(0, wmax) * K
    while x < W:
        w = rnd.randint(wmin, wmax) * K
        h = rnd.randint(hmin, hmax) * K
        if rnd.random() < (0.12 if layer == 2 else 0.18):
            h = int(h * rnd.uniform(1.4, 2.0))   # редкие высотки
        x1 = x + w
        top = base - h
        c = tuple(max(0, v + rnd.randint(-3, 3)) for v in body)
        d.rectangle([x, top, x1, base], fill=c)
        # уступы и надстройки на крыше
        if h > 160 * K and rnd.random() < 0.5:
            sw = int(w * rnd.uniform(0.5, 0.75))
            sx = x + (w - sw) // 2
            sh = rnd.randint(15, 50) * K
            d.rectangle([sx, top - sh, sx + sw, top], fill=c)
            top2 = top - sh
            if rnd.random() < 0.5:
                d.rectangle([sx + sw // 3, top2 - rnd.randint(8, 20) * K, sx + sw // 3 + 5 * K, top2], fill=c)
        else:
            top2 = top
        if rnd.random() < 0.35:
            ax = x + rnd.randint(w // 5, max(w // 5 + 1, w * 4 // 5))
            ah = rnd.randint(12, 45) * K
            d.rectangle([ax, top2 - ah, ax + max(1, K), top2], fill=c)
            if layer >= 1:
                light.ellipse([ax - 2 * K, top2 - ah - 2 * K, ax + 2 * K, top2 - ah + 2 * K], fill=(255, 30, 30))
        if layer == 2 and rnd.random() < 0.25:
            # бак с водой на крыше
            tx = x + rnd.randint(4, max(5, w // K - 20)) * K
            d.rectangle([tx, top - 9 * K, tx + 10 * K, top], fill=c)
        # окна по этажам
        ww, wh, sx_, sy_ = (v * K for v in win)
        warm = rnd.choice([(255, 196, 120), (255, 214, 150), (230, 236, 255), (255, 170, 100)])
        p_lit = rnd.uniform(0.03, 0.22) if rnd.random() < 0.8 else rnd.uniform(0.3, 0.5)
        margin = rnd.randint(2, 5) * K
        y = top + 4 * K
        while y < base - wh - 2 * K:
            floor_on = rnd.random() < 0.05
            floor_off = rnd.random() < 0.35
            xx = x + margin
            while xx < x1 - margin - ww:
                lit = (floor_on and rnd.random() < 0.85) or (not floor_off and rnd.random() < p_lit)
                if lit:
                    k = rnd.uniform(0.35, 1.0) * [0.45, 0.7, 1.0][layer]
                    col = tuple(int(v * k) for v in warm)
                    light.rectangle([xx, y, xx + ww - 1, y + wh - 1], fill=col)
                else:
                    d.rectangle([xx, y, xx + ww - 1, y + wh - 1], fill=tuple(min(255, v + 4) for v in c))
                xx += sx_
            y += sy_
        # неоновые вывески на ближних домах
        if layer >= 1 and rnd.random() < (0.35 if layer == 2 else 0.12) and w > 34 * K and h > 120 * K:
            col = rnd.choice([(255, 40, 70), (255, 60, 150), (255, 120, 40), (80, 200, 255), (255, 230, 90)])
            if rnd.random() < 0.6:
                nw, nh = rnd.randint(4, 7) * K, rnd.randint(30, 80) * K
            else:
                nw, nh = rnd.randint(20, max(21, w // K - 10)) * K, rnd.randint(6, 12) * K
            nx = x + rnd.randint(3 * K, max(3 * K + 1, w - nw - 3 * K))
            ny = top + rnd.randint(8 * K, max(8 * K + 1, h // 3))
            d.rectangle([nx - K, ny - K, nx + nw + K, ny + nh + K], fill=(4, 2, 3))
            light.rectangle([nx, ny, nx + nw, ny + nh], fill=col)
            # «иероглифы» тёмными полосками внутри вывески
            for i in range(rnd.randint(2, 5)):
                if nw < nh:
                    yy = ny + (i + 1) * nh // 6
                    light.rectangle([nx + K, yy, nx + nw - K, yy + K], fill=tuple(v // 3 for v in col))
                else:
                    xx = nx + (i + 1) * nw // 6
                    light.rectangle([xx, ny + K, xx + K, ny + nh - K], fill=tuple(v // 3 for v in col))
        x = x1 + rnd.randint(0, 3 if layer else 1) * K


def tower(d, light, cx):
    """Токийская башня: красно-белая решётчатая, подсвеченная."""
    base = HORIZON * K
    h = 470 * K
    top = base - h
    bw = 70 * K
    for i in range(0, h, K):
        y = base - i
        t = i / h
        half = int(bw * (1 - t) ** 1.8 + 2 * K)
        band = int(t * 9) % 2 == 1 and t < 0.8
        col = (235, 235, 230) if band else (235, 90, 40)
        if i % (18 * K) < 2 * K or t > 0.82:
            light.line([(cx - half, y), (cx + half, y)], fill=tuple(int(v * 0.8) for v in col))
        else:
            light.line([(cx - half, y), (cx - half + 2 * K, y)], fill=col)
            light.line([(cx + half - 2 * K, y), (cx + half, y)], fill=col)
    # решётка крест-накрест
    for i in range(0, int(h * 0.8), 18 * K):
        y0, y1 = base - i, base - i - 18 * K
        h0 = int(bw * (1 - i / h) ** 1.8 + 2 * K)
        h1 = int(bw * (1 - (i + 18 * K) / h) ** 1.8 + 2 * K)
        light.line([(cx - h0, y0), (cx + h1, y1)], fill=(200, 70, 30), width=K)
        light.line([(cx + h0, y0), (cx - h1, y1)], fill=(200, 70, 30), width=K)
    # смотровые площадки
    for t in (0.33, 0.58):
        y = base - int(h * t)
        half = int(bw * (1 - t) ** 1.8 + 8 * K)
        light.rectangle([cx - half, y - 7 * K, cx + half, y], fill=(255, 230, 190))
    light.ellipse([cx - 3 * K, top - 3 * K, cx + 3 * K, top + 3 * K], fill=(255, 40, 40))


def city_strip():
    np = _np()
    W, H = 4 * N * K, N * K
    rnd = random.Random(2026)
    img = sky_strip(W, H)
    # красная луна со свечением (на передней грани)
    glow = Image.new('RGB', (W, H))
    gd = ImageDraw.Draw(glow)
    mx, my, mr = int(0.68 * N * K), int(0.25 * N * K), 62 * K
    gd.ellipse([mx - mr * 3, my - mr * 3, mx + mr * 3, my + mr * 3], fill=(120, 14, 24))
    glow = glow.filter(ImageFilter.GaussianBlur(60 * K))
    img = ImageChops.add(img, glow)
    moon = Image.new('RGB', (W, H))
    md = ImageDraw.Draw(moon)
    md.ellipse([mx - mr, my - mr, mx + mr, my + mr], fill=(215, 38, 46))
    # пятна «морей» на луне: мягкие, разного размера
    spots = Image.new('L', (W, H))
    sd = ImageDraw.Draw(spots)
    for _ in range(22):
        ang, dist = rnd.uniform(0, 6.283), rnd.uniform(0, 0.8) * mr
        cx, cy = int(mx + math.cos(ang) * dist), int(my + math.sin(ang) * dist)
        r = int(rnd.uniform(0.06, 0.28) * mr)
        sd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=rnd.randint(60, 140))
    spots = spots.filter(ImageFilter.GaussianBlur(5 * K))
    md_dark = Image.new('RGB', (W, H), (150, 22, 32))
    moon = Image.composite(md_dark, moon, spots)
    mask = Image.new('L', (W, H))
    ImageDraw.Draw(mask).ellipse([mx - mr, my - mr, mx + mr, my + mr], fill=255)
    img = Image.composite(moon, img, mask)
    for layer in range(3):
        d = ImageDraw.Draw(img)
        light = Image.new('RGB', (W, H))
        ld = ImageDraw.Draw(light)
        buildings(d, ld, W, rnd, layer)
        # огни: сами по себе и мягкое свечение вокруг
        bloom = light.filter(ImageFilter.GaussianBlur(6 * K))
        bloom = Image.eval(bloom, lambda v: min(255, int(v * 1.6)))
        img = ImageChops.lighter(img, light)
        img = ImageChops.add(img, bloom)
        img = fog(img, [0.55, 0.3, 0.1][layer], [220, 160, 90][layer])
    # Токийская башня перед городом, слегка в дымке
    d = ImageDraw.Draw(img)
    light = Image.new('RGB', (W, H))
    tower(d, ImageDraw.Draw(light), int(2.45 * N * K))
    img = ImageChops.lighter(img, light)
    img = ImageChops.add(img, Image.eval(light.filter(ImageFilter.GaussianBlur(8 * K)), lambda v: min(255, int(v * 1.3))))
    img = fog(img, 0.12, 200)
    # улицы внизу: фонари и фары
    d = ImageDraw.Draw(img)
    base = HORIZON * K
    light = Image.new('RGB', (W, H))
    ld = ImageDraw.Draw(light)
    # улицы рядами: ближе — реже и ярче, дальше — гуще и тусклее; немного случайных огней между ними
    for row, (dy, step, k0) in enumerate(((3, 7, 0.35), (8, 10, 0.45), (16, 14, 0.55), (30, 20, 0.6), (52, 28, 0.7), (82, 40, 0.75))):
        y = base + dy * K
        x = rnd.randint(0, step) * K
        while x < W:
            c = rnd.choice([(255, 170, 80), (255, 190, 120), (255, 170, 80), (230, 230, 255)])
            r = max(1, (1 + row // 2)) * K // 2 + K // 2
            k = k0 * rnd.uniform(0.6, 1.0)
            ld.ellipse([x - r, y - r, x + r, y + r], fill=tuple(int(v * k) for v in c))
            if rnd.random() < 0.08:
                # машины: белые фары и красные стоп-сигналы полосками
                c2 = (255, 40, 40) if rnd.random() < 0.5 else (255, 240, 210)
                ld.rectangle([x, y + K, x + rnd.randint(6, 18) * K, y + 2 * K], fill=tuple(int(v * k) for v in c2))
            x += int(step * K * rnd.uniform(0.7, 1.3))
    for _ in range(700):
        x = rnd.randrange(W)
        y = base + rnd.randint(2, 90) * K
        c = rnd.choice([(255, 170, 80), (255, 200, 130), (230, 230, 255)])
        k = rnd.uniform(0.1, 0.35) * (1 - (y - base) / (100 * K))
        ld.ellipse([x - K, y - K, x + K, y + K], fill=tuple(int(v * k) for v in c))
    img = ImageChops.add(img, light.filter(ImageFilter.GaussianBlur(2 * K)))
    img = ImageChops.lighter(img, light)
    # общая засветка над городом
    arr = np.asarray(img, float)
    y = np.arange(H)[:, None]
    g = np.clip(1 - np.abs(y - base) / (260 * K), 0, 1) ** 2 * 0.22
    arr = arr + np.array((150, 30, 40), float) * g[..., None]
    img = Image.fromarray(arr.clip(0, 255).astype('uint8'))
    return img.resize((4 * N, N), Image.LANCZOS)


def flat(color, stars):
    rnd = random.Random(500 + stars)
    im = Image.new('RGB', (N, N), color)
    d = ImageDraw.Draw(im)
    for _ in range(stars):
        v = rnd.randint(90, 210)
        d.point((rnd.randrange(N), rnd.randrange(N)), fill=(v, v - 20, v - 10))
    return im


def panorama():
    out = os.path.join(OUT, 'title', 'background')
    os.makedirs(out, exist_ok=True)
    strip = city_strip()
    for f in range(4):
        strip.crop((f * N, 0, (f + 1) * N, N)).save(os.path.join(out, f'panorama_{f}.png'))
    flat(SKY_TOP, 260).save(os.path.join(out, 'panorama_4.png'))
    flat(GROUND, 0).save(os.path.join(out, 'panorama_5.png'))



# ---------- окна с ячейками (инвентарь, сундуки, печи...) ----------
# Серое → тёмное: панель тёмно-бордовая, ячейки ещё темнее, светлые края — красноватая кайма.
# Цветные значки (огонь, стрелки, пузырьки, книга) сохраняют цвет. Подписи окон светлеют в коде (GuiGraphicsMixin).
CONTAINERS = ['inventory', 'generic_54', 'shulker_box', 'crafting_table', 'furnace', 'blast_furnace', 'smoker',
              'brewing_stand', 'enchanting_table', 'anvil', 'smithing', 'legacy_smithing', 'beacon', 'hopper',
              'dispenser', 'horse', 'villager2', 'cartography_table', 'grindstone', 'loom', 'stonecutter',
              'creative_inventory/tabs', 'creative_inventory/tab_items', 'creative_inventory/tab_inventory',
              'creative_inventory/tab_item_search']
# опорные точки: яркость серого исходника → новый цвет
ANCHORS = [(0.0, (3, 2, 2)), (0.20, (5, 3, 4)), (0.333, (8, 5, 6)), (0.545, (16, 10, 12)), (0.70, (30, 18, 21)),
           (0.776, (36, 23, 26)), (0.90, (70, 34, 40)), (1.0, (128, 52, 60))]


def dark_gray(lum):
    for (l0, c0), (l1, c1) in zip(ANCHORS, ANCHORS[1:]):
        if lum <= l1:
            return mix(c0, c1, (lum - l0) / max(1e-6, l1 - l0))
    return ANCHORS[-1][1]


def dark_pixel(c):
    r, g, b, a = c
    if a == 0:
        return c
    mx, mn = max(r, g, b), min(r, g, b)
    sat = 0 if mx == 0 else (mx - mn) / mx
    lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    gray = dark_gray(lum)
    if sat < 0.12:
        return gray + (a,)
    # цветное: оставляем, чуть приглушая, и подмешиваем тёмный фон у слабо окрашенных
    k = min(1.0, (sat - 0.12) / 0.25)
    col = tuple(int(v * 0.92) for v in (r, g, b))
    return mix(gray, col, k) + (a,)


# Размер самого окна на листе и строки подписей: (y, x начала, x конца, 'L' — подпись слева, 'C' — по центру)
WINDOWS = {
    'inventory': ((176, 166), [(6, 95, 170, 'L'), (72, 6, 170, 'L')]),
    'generic_54': ((176, 222), [(6, 6, 170, 'L'), (128, 6, 170, 'L')]),
    'shulker_box': ((176, 167), [(6, 6, 170, 'L'), (73, 6, 170, 'L')]),
    'crafting_table': ((176, 166), [(6, 27, 170, 'L'), (72, 6, 170, 'L')]),
    'furnace': ((176, 166), [(6, 6, 170, 'C'), (72, 6, 170, 'L')]),
    'blast_furnace': ((176, 166), [(6, 6, 170, 'C'), (72, 6, 170, 'L')]),
    'smoker': ((176, 166), [(6, 6, 170, 'C'), (72, 6, 170, 'L')]),
    'brewing_stand': ((176, 166), [(6, 6, 170, 'C'), (72, 6, 170, 'L')]),
    'dispenser': ((176, 166), [(6, 6, 170, 'C'), (72, 6, 170, 'L')]),
    'hopper': ((176, 133), [(6, 6, 170, 'L'), (39, 6, 170, 'L')]),
    'enchanting_table': ((176, 166), [(6, 6, 170, 'L'), (72, 6, 170, 'L')]),
    'anvil': ((176, 166), [(6, 58, 170, 'L'), (72, 6, 170, 'L')]),
    'smithing': ((176, 166), [(15, 42, 170, 'L'), (72, 6, 170, 'L')]),
    'legacy_smithing': ((176, 166), [(72, 6, 170, 'L')]),
    'horse': ((176, 166), [(6, 6, 170, 'L'), (72, 6, 170, 'L')]),
    'villager2': ((276, 166), [(72, 105, 270, 'L')]),
    'cartography_table': ((176, 166), [(6, 6, 170, 'L'), (72, 6, 170, 'L')]),
    'grindstone': ((176, 166), [(6, 6, 170, 'L'), (72, 6, 170, 'L')]),
    'loom': ((176, 166), [(6, 6, 170, 'L'), (72, 6, 170, 'L')]),
    'stonecutter': ((176, 166), [(6, 6, 170, 'L'), (72, 6, 170, 'L')]),
    'beacon': ((230, 219), []),
    'creative_inventory/tab_items': ((195, 136), [(6, 6, 160, 'L')]),
    'creative_inventory/tab_item_search': ((195, 136), [(6, 6, 160, 'L')]),
    'creative_inventory/tab_inventory': ((195, 136), []),
}
PANEL_TOP = (44, 28, 32)
PANEL_BOTTOM = (20, 12, 15)
OUTLINE = (52, 16, 22)


def styled_window(name, im):
    """Окно мода по серому исходнику: градиент и фактура панели, красная кайма, уголки, полосы под подписями."""
    src = im.load()
    out = im.copy()
    px = out.load()
    W, H = im.size
    (w, h), labels = WINDOWS.get(name, ((0, 0), []))
    rnd = random.Random(hash(name) & 0xFFFF)

    def opaque(x, y):
        return 0 <= x < W and 0 <= y < H and src[x, y][3] > 0

    band = {}
    for ly, xa, xb, mode in labels:
        for y in range(ly - 2, ly + 11):
            band[y] = (ly, xa, xb, mode)
    for y in range(H):
        for x in range(W):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            inside = x < w and y < h
            grayish = max(r, g, b) - min(r, g, b) < 12
            if not inside or not grayish:
                px[x, y] = dark_pixel((r, g, b, a))
                continue
            if (r, g, b) == (198, 198, 198):
                # панель: сверху светлее, снизу темнее, лёгкая фактура
                t = y / max(1, h - 1)
                c = mix(PANEL_TOP, PANEL_BOTTOM, t)
                n = rnd.randint(-2, 2)
                c = tuple(max(0, v + n) for v in c)
                if y in band:
                    ly, xa, xb, mode = band[y]
                    if mode == 'L':
                        f = 1.0 - min(1.0, max(0.0, (x - xa) / max(1, xb - xa)))
                    else:
                        f = 1.0 - min(1.0, abs(x - w / 2) / (w / 2))
                    f = max(0.0, f) ** 1.4
                    if y == ly + 10:
                        c = mix(c, ACC, 0.75 * f)
                    elif y < ly + 10:
                        c = mix(c, ACC, 0.10 * f * (1 - abs(y - (ly + 3.5)) / 7))
                px[x, y] = c + (255,)
                continue
            if (r, g, b) == (0, 0, 0):
                edge = not (opaque(x - 1, y) and opaque(x + 1, y) and opaque(x, y - 1) and opaque(x, y + 1))
                if edge:
                    near_corner = (x <= 7 or x >= w - 8) and (y <= 7 or y >= h - 8)
                    px[x, y] = (ACC if near_corner else OUTLINE) + (255,)
                    continue
            if (r, g, b) == (255, 255, 255):
                # светлые края: у окна и ячеек — красноватая кайма
                px[x, y] = (122, 40, 50, 255)
                continue
            if (r, g, b) == (85, 85, 85):
                px[x, y] = (10, 6, 8, 255)
                continue
            if (r, g, b) == (139, 139, 139):
                # ячейка: чуть темнее сверху
                yy = y
                while yy > 0 and src[x, yy - 1][:3] == (139, 139, 139):
                    yy -= 1
                k = min(1.0, (y - yy) / 15)
                px[x, y] = mix((10, 6, 7), (19, 12, 14), k) + (255,)
                continue
            if (r, g, b) == (55, 55, 55):
                px[x, y] = (4, 2, 3, 255)
                continue
            px[x, y] = dark_pixel((r, g, b, a))
    if name == 'inventory':
        # «сцена» под фигуркой игрока: темно сверху, красное свечение у пола
        for y in range(8, 78):
            for x in range(26, 75):
                if src[x, y][:3] == (0, 0, 0) and src[x, y][3] > 0:
                    t = (y - 8) / 70
                    c = mix((4, 2, 3), (34, 8, 12), t ** 2)
                    dx = (x - 50.5) / 24
                    dy = (y - 74) / 10
                    gl = max(0.0, 1 - (dx * dx + dy * dy)) ** 1.5
                    px[x, y] = mix(c, (150, 24, 34), 0.55 * gl) + (255,)
    return out


def containers():
    src = os.path.join(SRC, 'container')
    out = os.path.join(OUT, 'container')
    for name in CONTAINERS:
        path = os.path.join(src, name + '.png')
        if not os.path.exists(path):
            continue
        im = Image.open(path).convert('RGBA')
        im = styled_window(name, im)
        dst = os.path.join(out, name + '.png')
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        im.save(dst)
    rb = os.path.join(SRC, 'recipe_book.png')
    if os.path.exists(rb):
        im = Image.open(rb).convert('RGBA')
        im.putdata([dark_pixel(p) for p in list(im.getdata())])
        im.save(os.path.join(OUT, 'recipe_book.png'))

# ---------- экран в игре и остальное ----------
import colorsys


def hue(c):
    r, g, b = c[:3]
    h, l, sat = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
    return h * 360, l, sat


def remap_gray(c):
    """Серое — в стиль мода (тёмное, светлые края красные), цветное (значки) не трогаем."""
    r, g, b, a = c
    if a == 0 or max(r, g, b) - min(r, g, b) > 24:
        return c
    return remap(c)


def hud():
    """Сердца — тёмно-алые, голод — сырое мясо, броня — тёмная сталь, полоса опыта — кровавая."""
    im = Image.open(os.path.join(SRC, 'icons.png')).convert('RGBA')
    px = im.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, sat = hue((r, g, b))
            if (y < 9 or 45 <= y < 54) and x >= 16 and sat > 0.45 and (h < 15 or h > 345) and l < 0.8:
                # обычные красные сердца: глубже и темнее, блик розовый
                px[x, y] = mix((120, 6, 18), (255, 70, 84), min(1.0, l / 0.62)) + (a,)
            elif 27 <= y < 36 and x >= 16 and sat > 0.25 and 15 <= h <= 45:
                # мясо на «ножке»: сырое, красное
                px[x, y] = mix((70, 6, 12), (232, 84, 92), min(1.0, l / 0.62)) + (a,)
            elif 9 <= y < 18 and x >= 16 and sat < 0.12:
                # броня: тёмная сталь с красноватым отливом
                px[x, y] = mix((18, 14, 16), (160, 128, 134), l) + (a,)
            elif 64 <= y < 74 and x < 182:
                if sat > 0.3 and 60 < h < 160:
                    # зелёное заполнение опыта → кровавое
                    px[x, y] = mix((100, 6, 14), (236, 52, 60), min(1.0, l / 0.6)) + (a,)
                elif sat < 0.25:
                    px[x, y] = mix((6, 3, 4), (90, 30, 38), l) + (a,)
    im.save(os.path.join(OUT, 'icons.png'))


def hotbar_and_icons():
    """Хотбар тёмный с красной рамкой выбора; маленькие кнопки-значки (язык, замок) в стиле кнопок мода."""
    path = os.path.join(OUT, 'widgets.png')
    im = Image.open(path).convert('RGBA')
    px = im.load()
    d = ImageDraw.Draw(im)
    # хотбар 182×22: полупрозрачная тёмная полоса, 9 ячеек с объёмом, бордовая рамка
    d.rectangle([0, 0, 181, 21], fill=(16, 10, 12, 215))
    d.rectangle([0, 0, 181, 21], outline=(70, 20, 28, 255))
    for i in range(9):
        x = 1 + 20 * i
        d.rectangle([x + 1, 1, x + 19, 20], fill=(8, 5, 6, 200))
        d.line([(x + 1, 1), (x + 19, 1)], fill=(3, 2, 2, 255))
        d.line([(x + 1, 1), (x + 1, 20)], fill=(3, 2, 2, 255))
        d.line([(x + 1, 20), (x + 19, 20)], fill=(96, 30, 40, 255))
        d.line([(x + 19, 1), (x + 19, 20)], fill=(96, 30, 40, 255))
    for cx, cy, sx, sy in ((0, 0, 1, 1), (181, 0, -1, 1), (0, 21, 1, -1), (181, 21, -1, -1)):
        d.line([(cx, cy), (cx + 4 * sx, cy)], fill=ACC + (255,))
        d.line([(cx, cy), (cx, cy + 4 * sy)], fill=ACC + (255,))
    # рамка выбранной ячейки 24×24: яркая красная, с уголками светлее
    d.rectangle([0, 22, 23, 45], fill=(0, 0, 0, 0))
    d.rectangle([0, 22, 23, 45], outline=(120, 14, 26, 255))
    d.rectangle([1, 23, 22, 44], outline=ACC + (255,))
    d.rectangle([2, 24, 21, 43], outline=(150, 30, 40, 160))
    for cx, cy, sx, sy in ((1, 23, 1, 1), (22, 23, -1, 1), (1, 44, 1, -1), (22, 44, -1, -1)):
        d.line([(cx, cy), (cx + 4 * sx, cy)], fill=(255, 150, 156, 255))
        d.line([(cx, cy), (cx, cy + 4 * sy)], fill=(255, 150, 156, 255))
    # ячейка второй руки 29×24 (24..52, 22..45)
    d.rectangle([24, 22, 52, 45], fill=(0, 0, 0, 0))
    d.rectangle([24, 22, 52, 45], outline=(70, 20, 28, 255))
    d.rectangle([25, 23, 51, 44], fill=(16, 10, 12, 215))
    d.rectangle([27, 25, 45, 43], fill=(8, 5, 6, 200))
    d.line([(27, 43), (45, 43)], fill=(96, 30, 40, 255))
    d.line([(45, 25), (45, 43)], fill=(96, 30, 40, 255))
    for y in range(106, 186):
        for x in range(0, 40):
            px[x, y] = remap_gray(px[x, y])
    im.save(path)
    acc = Image.open(os.path.join(SRC, 'accessibility.png')).convert('RGBA')
    acc.putdata([remap_gray(p) for p in list(acc.getdata())])
    acc.save(os.path.join(OUT, 'accessibility.png'))


def boss_bars():
    """Полоски боссов: фон почти чёрный с цветной каймой, заполнение с бликом сверху и тенью снизу."""
    im = Image.open(os.path.join(SRC, 'bars.png')).convert('RGBA')
    px = im.load()
    for i in range(7):
        bg_y, fill_y = i * 10, i * 10 + 5
        for x in range(182):
            for dy in range(5):
                r, g, b, a = px[x, bg_y + dy]
                if a:
                    edge = dy in (0, 4) or x in (0, 181)
                    k = 0.55 if edge else 0.18
                    px[x, bg_y + dy] = (int(r * k), int(g * k), int(b * k), a)
                r, g, b, a = px[x, fill_y + dy]
                if a:
                    k = [1.25, 1.05, 0.95, 0.8, 0.55][dy]
                    px[x, fill_y + dy] = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), a)
    im.save(os.path.join(OUT, 'bars.png'))


def toasts():
    """Всплывающие окна: тёмные — тёмно-бордовые с красной каймой; светлые — «пергамент» с бордовой каймой
    (на них игра пишет тёмный текст)."""
    im = Image.open(os.path.join(SRC, 'toasts.png')).convert('RGBA')
    px = im.load()
    for y in range(128):
        for x in range(160):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            row = y // 32
            h, l, sat = hue((r, g, b))
            if row in (0, 2):
                if l < 0.06:
                    px[x, y] = (40, 10, 16, a)
                elif l > 0.6 or sat > 0.5 and row == 2 and 40 < h < 70:
                    px[x, y] = (r, g, b, a) if sat > 0.5 else (196, 52, 62, a)
                else:
                    px[x, y] = mix((14, 8, 10), (46, 22, 26), l / 0.6) + (a,)
            else:
                if l < 0.06:
                    px[x, y] = (60, 12, 20, a)
                elif l > 0.9:
                    px[x, y] = (250, 238, 232, a)
                else:
                    px[x, y] = mix((120, 34, 44), (236, 222, 214), (l - 0.06) / 0.84) + (a,)
    im.save(os.path.join(OUT, 'toasts.png'))


def book():
    """Книга: тёмно-алая кожа вместо коричневой, страница — старая бумага с бледными пятнами крови."""
    im = Image.open(os.path.join(SRC, 'book.png')).convert('RGBA')
    px = im.load()
    rnd = random.Random(42)
    stains = [(rnd.randint(30, 150), rnd.randint(10, 170), rnd.randint(4, 14)) for _ in range(5)]
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, sat = hue((r, g, b))
            if x < 192 and y < 192 and l > 0.75:
                c = (int(r * 0.97), int(g * 0.91), int(b * 0.86))
                for sx, sy, sr in stains:
                    dd = ((x - sx) ** 2 + (y - sy) ** 2) ** 0.5
                    if dd < sr:
                        c = mix(c, (190, 120, 110), 0.18 * (1 - dd / sr))
                px[x, y] = c + (a,)
            elif x < 192 and y < 192 and 10 <= h <= 40 and l < 0.5:
                px[x, y] = mix((24, 4, 8), (130, 24, 34), min(1.0, l / 0.45)) + (a,)
    im.save(os.path.join(OUT, 'book.png'))


def advancements():
    """Окно достижений и вкладки — тёмные, как окна с ячейками."""
    out = os.path.join(OUT, 'advancements')
    os.makedirs(out, exist_ok=True)
    for name in ('window', 'tabs'):
        im = Image.open(os.path.join(SRC, 'advancements', name + '.png')).convert('RGBA')
        im.putdata([dark_pixel(p) for p in list(im.getdata())])
        im.save(os.path.join(out, name + '.png'))


def edition():
    """Надпись под логотипом Minecraft вместо «Java Edition»: TOKYO GHOUL RPG буквами самой игры.
    Игра берёт область 98×14 из листа 128×16; лист вчетверо крупнее для чёткости (392×56 из 512×64)."""
    glyphs = Image.open(os.path.join(SRC, 'font', 'ascii.png')).convert('RGBA')
    text = 'TOKYO GHOUL RPG'
    scale = 4

    def glyph(ch):
        code = ord(ch)
        gx, gy = (code % 16) * 8, (code // 16) * 8
        g = glyphs.crop((gx, gy, gx + 8, gy + 8))
        if ch == ' ':
            return Image.new('RGBA', (3, 8))
        cols = [x for x in range(8) if any(g.getpixel((x, y))[3] > 0 for y in range(8))]
        return g.crop((0, 0, (max(cols) + 1) if cols else 3, 8))

    parts = [glyph(c) for c in text]
    width = sum(p.size[0] + 1 for p in parts) + 1   # +1 — жирный шрифт сдвигом на пиксель
    line = Image.new('L', (width, 8))
    x = 0
    for p in parts:
        m = p.getchannel('A')
        line.paste(255, (x, 0), m)
        line.paste(255, (x + 1, 0), m)
        x += p.size[0] + 1
    W, H, bw, bh = 512, 64, 392, 56
    big = line.resize((width * scale, 8 * scale), Image.NEAREST)
    ox, oy = (bw - big.size[0]) // 2, (bh - big.size[1]) // 2 - 2
    mask = Image.new('L', (W, H))
    mask.paste(big, (ox, oy))
    out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    outline = mask.filter(ImageFilter.MaxFilter(5))
    out.paste((24, 2, 6, 255), (scale, scale), outline)
    out.paste((24, 2, 6, 255), (0, 0), outline)
    grad = Image.new('RGBA', (W, H))
    gp = grad.load()
    for y in range(H):
        t = min(1.0, max(0.0, (y - oy) / max(1, big.size[1])))
        c = mix((255, 128, 132), (170, 14, 26), t)
        for xx in range(W):
            gp[xx, y] = c + (255,)
    out.paste(grad, (0, 0), mask)
    out.save(os.path.join(OUT, 'title', 'edition.png'))


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    widgets()
    slider()
    recolor('tab_button.png')
    recolor('checkbox.png')
    background()
    containers()
    hud()
    hotbar_and_icons()
    boss_bars()
    toasts()
    book()
    advancements()
    edition()
    panorama()
    print('ok')
