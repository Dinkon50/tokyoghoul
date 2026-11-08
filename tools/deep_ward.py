"""
Данж «Глубины 24-го района» — самая большая постройка мода: 97 x 52 x 97, очень редкая.

  Поверхность (y 41..51): разрушенное святилище — площадка, сломанные врата-тории, а в середине
      шахта с винтовой лестницей вниз.
  Ярус 1 «Руины» (пол y 30): лабиринт 16 x 16 клеток с петлями, залы с добычей и гнёздами, ловушки
      со стрелами, паутина, раскалённый пол. В середине — зал под шахтой, в северо-западном углу — спуск.
  Ярус 2 «Глубины» (пол y 8): девять пещер 29 x 29, соединённых тоннелями:
      СЗ «Колодец костей» (сюда выводит спуск), С «Пропасть» (мост над лавой), СВ «Склеп»,
      З «Сокровищница», Ц «Кровавое озеро» (тропы над лавой), В «Галерея стражей» (два стража у врат),
      ЮЗ «Гнездо», Ю «Темница», ЮВ «Логово Древнего гуля» (круглая арена, трон, сундуки за троном).
  Незаданные клетки не трогаются (как structure_void): вокруг остаётся камень мира; все пустоты
  обложены стенами, чтобы вода и лава мира не затекали.

Запуск: python3 deep_ward.py <data/tokyoghoulrpg/structures>  (и карты ярусов: --maps <папка>)
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from structures import Build  # noqa: E402
from nbt import BYTE, COMPOUND, INT, LIST, SHORT, STRING, Tag  # noqa: E402

W, H, D = 97, 52, 97
C = 48
GROUND = 41          # верхний блок земли на поверхности (вход)
F1 = 30              # пол первого яруса
F2 = 8               # пол второго яруса
L1_TOP = 35          # потолок первого яруса — y 36

AIR = "air"
OPEN = {"minecraft:air", "minecraft:lava", "minecraft:cobweb"}


class Deep(Build):
    def __init__(self):
        super().__init__(W, H, D, 24024)
        self.void_unset = True

    def air(self, x, y, z):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D:
            self.set(x, y, z, AIR)

    def is_open(self, x, y, z):
        key = self.blocks.get((x, y, z))
        return key is not None and key[0] in OPEN

    def spawner(self, x, y, z, entity, count=2, nearby=5, rng_=4, delay=(240, 600)):
        self.set(x, y, z, "spawner")
        ent = Tag(COMPOUND, {"entity": Tag(COMPOUND, {"id": Tag(STRING, "tokyoghoulrpg:" + entity)})})
        self.block_nbt[(x, y, z)] = {
            "id": Tag(STRING, "minecraft:mob_spawner"),
            "SpawnData": ent,
            "SpawnPotentials": Tag(LIST, [Tag(COMPOUND, {"weight": Tag(INT, 1), "data": ent})], item_type=COMPOUND),
            "Delay": Tag(SHORT, 40),
            "MinSpawnDelay": Tag(SHORT, delay[0]),
            "MaxSpawnDelay": Tag(SHORT, delay[1]),
            "SpawnCount": Tag(SHORT, count),
            "MaxNearbyEntities": Tag(SHORT, nearby),
            "RequiredPlayerRange": Tag(SHORT, 16),
            "SpawnRange": Tag(SHORT, rng_),
        }

    def barrel(self, x, y, z, loot):
        self.set(x, y, z, "barrel", facing="up", open="false")
        self.block_nbt[(x, y, z)] = {"id": Tag(STRING, "minecraft:barrel"),
                                     "LootTable": Tag(STRING, f"tokyoghoulrpg:chests/{loot}")}

    def arrow_trap(self, plate, disp, facing):
        """Нажимная плита и раздатчик со стрелами в стене, смотрящий на плиту."""
        self.set(*plate, "polished_blackstone_pressure_plate", powered="false")
        self.set(*disp, "dispenser", facing=facing, triggered="false")
        self.block_nbt[disp] = {"id": Tag(STRING, "minecraft:dispenser"),
                                "Items": Tag(LIST, [Tag(COMPOUND, {"Slot": Tag(BYTE, s), "id": Tag(STRING, "minecraft:arrow"),
                                                                   "Count": Tag(BYTE, 32)}) for s in (0, 1)],
                                             item_type=COMPOUND)}


b = Deep()
rng = b.rng


# ----------------------------------------------------------------------------- шум для пещер
def hash2(x, z, s):
    h = (x * 374761393 + z * 668265263 + s * 2147483647) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


def noise(x, z, scale, s):
    fx, fz = x / scale, z / scale
    x0, z0 = math.floor(fx), math.floor(fz)
    tx, tz = fx - x0, fz - z0
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, c = hash2(x0, z0, s), hash2(x0 + 1, z0, s)
    d, e = hash2(x0, z0 + 1, s), hash2(x0 + 1, z0 + 1, s)
    return (a + (c - a) * tx) + ((d + (e - d) * tx) - (a + (c - a) * tx)) * tz


# ----------------------------------------------------------------------------- материалы
L1_WALL = ["deepslate_bricks"] * 4 + ["cracked_deepslate_bricks"] * 2 + ["deepslate_tiles", "cracked_deepslate_tiles",
                                                                           "polished_blackstone_bricks", "mossy_cobblestone"]
L1_FLOOR = ["polished_deepslate"] * 3 + ["deepslate_tiles", "cobbled_deepslate", "cracked_deepslate_tiles", "tuff"]
L2_ROCK = ["deepslate"] * 4 + ["tuff"] * 2 + ["blackstone"] * 2 + ["smooth_basalt", "cobbled_deepslate"]
L2_FLOOR = ["blackstone"] * 3 + ["deepslate"] * 2 + ["cobbled_deepslate", "tuff", "polished_blackstone"]
FLESH = ["nether_wart_block", "nether_wart_block", "crimson_hyphae"]


def shell(x, y, z):
    if y >= F1 - 1:
        return rng.choice(L1_WALL)
    return rng.choice(L2_ROCK)


# ============================================================================= поверхность и шахта
def surface():
    # площадка-святилище радиусом 9, над ней расчищаем место
    for x in range(C - 10, C + 11):
        for z in range(C - 10, C + 11):
            r = math.hypot(x - C, z - C)
            if r > 10.3:
                continue
            for y in range(GROUND + 1, GROUND + 9):
                b.air(x, y, z)
            if r <= 9.2:
                b.set(x, GROUND, z, rng.choice(["stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks",
                                                "cracked_stone_bricks", "andesite"]))
                b.set(x, GROUND - 1, z, "stone")
            elif rng.random() < 0.5:
                b.set(x, GROUND, z, rng.choice(["mossy_cobblestone", "gravel", "coarse_dirt"]))
    # обод шахты из тёмного кирпича
    for x in range(C - 3, C + 4):
        for z in range(C - 3, C + 4):
            if max(abs(x - C), abs(z - C)) == 3:
                b.set(x, GROUND, z, "polished_blackstone_bricks")
                if (x + z) % 2 == 0:
                    b.set(x, GROUND + 1, z, "polished_blackstone_brick_slab", type="bottom", waterlogged="false")
    # сломанные врата-тории с севера и юга
    for side in (-1, 1):
        z = C + side * 7
        for x in (C - 3, C + 3):
            top = GROUND + (5 if x == C - 3 else (3 if side == 1 else 5))
            for y in range(GROUND + 1, top + 1):
                b.set(x, y, z, "red_nether_bricks")
        if side == -1:
            for x in range(C - 4, C + 5):
                b.set(x, GROUND + 6, z, "dark_oak_planks" if abs(x - C) < 4 else "dark_oak_slab",
                      **({} if abs(x - C) < 4 else {"type": "bottom", "waterlogged": "false"}))
            b.set(C, GROUND + 5, z, "chain", axis="y", waterlogged="false")
            b.set(C, GROUND + 4, z, "soul_lantern", hanging="true", waterlogged="false")
        else:
            # упавшая перекладина
            for x in range(C - 2, C + 3):
                b.set(x, GROUND + 1, z + 2, "dark_oak_log", axis="x")
    # обломки колонн по кругу
    for k in range(8):
        a = k * math.pi / 4 + 0.3
        x, z = int(round(C + math.cos(a) * 8)), int(round(C + math.sin(a) * 8))
        if abs(z - C) >= 6 and abs(x - C) <= 4:
            continue
        hgt = rng.randint(1, 4)
        for y in range(GROUND + 1, GROUND + 1 + hgt):
            b.set(x, y, z, rng.choice(["deepslate_bricks", "cracked_deepslate_bricks", "polished_blackstone_bricks"]))
        if hgt >= 3 and rng.random() < 0.6:
            b.set(x, GROUND + 1 + hgt, z, "soul_lantern", hanging="false", waterlogged="false")
    for _ in range(6):
        x, z = C + rng.randint(-8, 8), C + rng.randint(-8, 8)
        if max(abs(x - C), abs(z - C)) > 4 and b.get(x, GROUND + 1, z) == "minecraft:air":
            b.set(x, GROUND + 1, z, "skeleton_skull", rotation=rng.randint(0, 15))


def spiral(x0, z0, top_floor, bottom_floor, wall_top):
    """
    Винтовая лестница в колодце 5 x 5 (x0, z0 — угол): кольцо ступеней вокруг столба 3 x 3.
    top_floor — пол верхней ступени, bottom_floor — нижней. Возвращает клетки кольца по порядку.
    """
    ring = [(x0 + i, z0) for i in range(5)] + [(x0 + 4, z0 + i) for i in range(1, 5)] + \
           [(x0 + 3 - i, z0 + 4) for i in range(4)] + [(x0, z0 + 3 - i) for i in range(3)]
    steps = top_floor - bottom_floor + 1
    used = {}
    for k in range(steps):
        p = ring[k % len(ring)]
        y = top_floor - k
        used.setdefault(p, []).append(y)
    # кольцо: всё сплошное, кроме ступеней и места над ними
    for p in ring:
        for y in range(bottom_floor, wall_top + 1):
            b.set(p[0], y, p[1], "deepslate_bricks")
    for k in range(steps):
        p = ring[k % len(ring)]
        y = top_floor - k
        if k == 0:
            b.set(p[0], y, p[1], "polished_deepslate")
        else:
            q = ring[(k - 1) % len(ring)]
            dx, dz = q[0] - p[0], q[1] - p[1]
            facing = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(dx, dz)]
            b.set(p[0], y, p[1], "deepslate_brick_stairs", facing=facing, half="bottom", shape="straight",
                  waterlogged="false")
        for h in range(1, 4):
            if y + h <= wall_top:
                b.air(p[0], y + h, p[1])
    # столб в середине, на нём через виток — фонарь
    for x in range(x0 + 1, x0 + 4):
        for z in range(z0 + 1, z0 + 4):
            for y in range(bottom_floor, wall_top + 1):
                b.set(x, y, z, "chiseled_deepslate" if (y - bottom_floor) % 8 == 0 else "polished_deepslate")
    return ring


def shaft():
    # колодец от поверхности до пола зала первого яруса
    x0, z0 = C - 2, C - 2
    spiral(x0, z0, GROUND, F1, GROUND + 1)
    b.set(C, GROUND + 2, C, "soul_lantern", hanging="false", waterlogged="false")


# ============================================================================= ярус 1: руины
NC = 16


def cell(i, j):
    """Левый верхний угол открытой части клетки 3 x 3 (стены между клетками — по 3 блока)."""
    return 2 + 6 * i, 2 + 6 * j


ROOMS = [
    ("center", 7, 7, 8, 8),
    ("descent", 0, 0, 1, 1),
    ("loot", 3, 2, 4, 3), ("loot", 11, 3, 12, 4), ("loot", 2, 11, 3, 12), ("loot", 12, 12, 13, 13),
    ("nest", 6, 3, 6, 4), ("nest", 13, 7, 14, 8), ("nest", 7, 12, 8, 12), ("nest", 3, 7, 3, 8),
    ("shrine", 10, 9, 11, 10),
]


def l1():
    room_of = {}
    for idx, (_, i0, j0, i1, j1) in enumerate(ROOMS):
        for i in range(i0, i1 + 1):
            for j in range(j0, j1 + 1):
                room_of[(i, j)] = idx

    def node(c):
        return ("r", room_of[c]) if c in room_of else c

    # сплошной блок яруса: пол, стены, потолок
    for x in range(W):
        for z in range(D):
            b.set(x, F1, z, rng.choice(L1_FLOOR))
            b.set(x, L1_TOP + 1, z, rng.choice(["deepslate_tiles", "deepslate_bricks"]))
            for y in range(F1 + 1, L1_TOP + 1):
                b.set(x, y, z, rng.choice(L1_WALL))

    def carve_rect(x0, z0, x1, z1, top=L1_TOP):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for y in range(F1 + 1, top + 1):
                    b.air(x, y, z)

    for i in range(NC):
        for j in range(NC):
            x, z = cell(i, j)
            carve_rect(x, z, x + 2, z + 2)
    for idx, (kind, i0, j0, i1, j1) in enumerate(ROOMS):
        (x0, z0), (x1, z1) = cell(i0, j0), cell(i1, j1)
        top = L1_TOP if kind != "center" else L1_TOP
        carve_rect(x0, z0, x1 + 2, z1 + 2, top)
    # лабиринт: обход в глубину по «узлам» (клетка или целый зал)
    nodes_cells = {}
    for i in range(NC):
        for j in range(NC):
            nodes_cells.setdefault(node((i, j)), []).append((i, j))
    start = node((0, 0))
    seen = {start}
    stack = [start]
    links = []

    def neighbours(n):
        out = []
        for (i, j) in nodes_cells[n]:
            for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                c = (i + di, j + dj)
                if 0 <= c[0] < NC and 0 <= c[1] < NC and node(c) != n:
                    out.append(((i, j), c))
        return out

    while stack:
        n = stack[-1]
        options = [(a, c) for a, c in neighbours(n) if node(c) not in seen]
        if not options:
            stack.pop()
            continue
        a, c = rng.choice(options)
        seen.add(node(c))
        links.append((a, c))
        stack.append(node(c))
    # петли: лишние проходы
    extra = 0
    while extra < 30:
        i, j = rng.randrange(NC), rng.randrange(NC)
        di, dj = rng.choice(((1, 0), (0, 1)))
        c = (i + di, j + dj)
        if c[0] >= NC or c[1] >= NC or node(c) == node((i, j)):
            continue
        links.append(((i, j), c))
        extra += 1

    def open_link(a, c):
        (i1, j1), (i2, j2) = sorted((a, c))
        x, z = cell(i1, j1)
        if i2 > i1:      # стена x + 3 .. x + 5
            for xx in range(x + 3, x + 6):
                for zz in range(z, z + 3):
                    for y in range(F1 + 1, F1 + 5):
                        b.air(xx, y, zz)
        else:
            for zz in range(z + 3, z + 6):
                for xx in range(x, x + 3):
                    for y in range(F1 + 1, F1 + 5):
                        b.air(xx, y, zz)

    for a, c in links:
        open_link(a, c)
    degree = {}
    for a, c in links:
        for k in (a, c):
            if k not in room_of:
                degree[k] = degree.get(k, 0) + 1

    # зал под шахтой выше: до потолка y 39, к нему спускается лестница
    cx0, cz0 = cell(7, 7)
    for x in range(cx0, cx0 + 9):
        for z in range(cz0, cz0 + 9):
            for y in range(L1_TOP + 1, GROUND - 1):
                b.air(x, y, z)
            b.set(x, GROUND - 1, z, "deepslate_tiles")
    for x, z in ((cx0, cz0), (cx0 + 8, cz0), (cx0, cz0 + 8), (cx0 + 8, cz0 + 8)):
        for y in range(F1 + 1, GROUND - 1):
            b.set(x, y, z, "polished_blackstone_bricks")
        b.set(x + (1 if x == cx0 else -1), GROUND - 2, z, "soul_lantern", hanging="true", waterlogged="false")
    return links, degree, room_of


def l1_dress(links, degree, room_of):
    cells = [(i, j) for i in range(NC) for j in range(NC) if (i, j) not in room_of]
    # ловушки: плита посреди коридорной клетки, раздатчик в глухой стене
    linked = set()
    for a, c in links:
        linked.add(frozenset((a, c)))
    traps = 0
    rng.shuffle(cells)
    for (i, j) in cells:
        if traps >= 14:
            break
        x, z = cell(i, j)
        for (di, dj, facing, disp) in ((1, 0, "west", (x + 3, F1 + 1, z + 1)), (-1, 0, "east", (x - 1, F1 + 1, z + 1)),
                                       (0, 1, "north", (x + 1, F1 + 1, z + 3)), (0, -1, "south", (x + 1, F1 + 1, z - 1))):
            c = (i + di, j + dj)
            if 0 <= c[0] < NC and 0 <= c[1] < NC and frozenset(((i, j), c)) not in linked and c not in room_of:
                plate = (x + 1 - di, F1 + 1, z + 1 - dj)
                b.arrow_trap(plate, disp, facing)
                traps += 1
                break
    # убранство коридоров
    for (i, j) in cells:
        x, z = cell(i, j)
        r = rng.random()
        if r < 0.08:
            for _ in range(rng.randint(2, 5)):
                b.set(x + rng.randint(0, 2), F1 + rng.choice((1, 4)), z + rng.randint(0, 2), "cobweb")
        elif r < 0.13:
            for dx in range(0, 3):
                for dz in range(0, 3):
                    if rng.random() < 0.6:
                        b.set(x + dx, F1, z + dz, "magma_block")
        elif r < 0.2:
            b.set(x + rng.choice((0, 2)), F1 + 1, z + rng.choice((0, 2)), "bone_block", axis="y")
            if rng.random() < 0.5:
                b.set(x + 1, F1 + 1, z + 1, "skeleton_skull", rotation=rng.randint(0, 15))
        if rng.random() < 0.07:
            b.set(x + 1, L1_TOP, z + 1, "soul_lantern", hanging="true", waterlogged="false")
    # тупики: сундук
    dead = [c for c in cells if degree.get(c, 0) == 1]
    rng.shuffle(dead)
    for (i, j) in dead[:9]:
        x, z = cell(i, j)
        b.chest(x + 1, F1 + 1, z + 1, rng.choice(["north", "south", "east", "west"]), "deep_common")
    # гули и плеватели в коридорах
    rng.shuffle(cells)
    for k, (i, j) in enumerate(cells[:30]):
        x, z = cell(i, j)
        if b.get(x + 1, F1 + 1, z + 1) == "minecraft:air":
            b.mob(x + 1, F1 + 1, z + 1, "deep_spitter" if k % 6 == 5 else "deep_ghoul", k % 2)
    # залы
    for idx, (kind, i0, j0, i1, j1) in enumerate(ROOMS):
        (x0, z0), (x1, z1) = cell(i0, j0), cell(i1, j1)
        x1, z1 = x1 + 2, z1 + 2
        mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
        if kind == "loot":
            b.chest(x0, F1 + 1, z0, "south", "deep_common")
            b.chest(x1, F1 + 1, z1, "north", "deep_common")
            b.barrel(x1, F1 + 1, z0, "deep_common")
            for x in range(x0 + 2, x1 - 1, 3):
                b.set(x, F1 + 1, mz, "polished_blackstone_bricks")
                b.set(x, F1 + 2, mz, "soul_lantern", hanging="false", waterlogged="false")
            b.mob(mx, F1 + 1, mz + 2, "deep_ghoul", 0)
            b.mob(mx + 1, F1 + 1, mz - 2, "deep_ghoul", 1)
        elif kind == "nest":
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    if rng.random() < 0.35:
                        b.set(x, F1, z, rng.choice(FLESH))
                    if rng.random() < 0.06:
                        b.set(x, F1 + 1, z, "bone_block", axis="y")
            b.spawner(mx, F1 + 1, mz, "deep_ghoul")
            b.mob(mx - 1, F1 + 1, mz + 1, "deep_ghoul", 0)
            b.mob(mx + 1, F1 + 1, mz - 1, "deep_ghoul", 1)
        elif kind == "shrine":
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    b.set(x, F1, z, "polished_blackstone" if (x + z) % 2 else "gilded_blackstone")
            b.set(mx, F1 + 1, mz, "crying_obsidian")
            b.chest(mx, F1 + 2, mz, "south", "deep_vault")
            for x, z in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
                b.set(x, F1 + 1, z, "polished_blackstone_bricks")
                b.set(x, F1 + 2, z, "soul_lantern", hanging="false", waterlogged="false")
            b.mob(mx + 2, F1 + 1, mz + 2, "deep_brute")
            b.mob(mx - 2, F1 + 1, mz - 2, "deep_ghoul", 0)
        elif kind == "center":
            b.mob(mx - 4, F1 + 1, mz + 4, "deep_ghoul", 0)
            b.mob(mx + 4, F1 + 1, mz - 4, "deep_ghoul", 1)


# ============================================================================= ярус 2: глубины
def zone_box(zx, zz):
    return 32 * zx + 2, 32 * zz + 2, 32 * zx + 30, 32 * zz + 30


def zone_center(zx, zz):
    return 32 * zx + 16, 32 * zz + 16


def cavern(zx, zz, height, seed, roundness=0.9, floor_mat=L2_FLOOR):
    """Пещера: рваные края и неровный потолок. Возвращает клетки пола."""
    x0, z0, x1, z1 = zone_box(zx, zz)
    cx, cz = zone_center(zx, zz)
    floor = []
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            dx, dz = abs(x - cx) / 14.5, abs(z - cz) / 14.5
            d = (dx ** 4 + dz ** 4) ** 0.25 * roundness + max(dx, dz) * (1 - roundness)
            n = noise(x, z, 5.0, seed)
            if d > 0.78 + 0.25 * n:
                continue
            top = F2 + 1 + int(height * (0.75 + 0.35 * noise(x, z, 7.0, seed + 1)) * (1.0 - 0.35 * d * d))
            b.set(x, F2, z, rng.choice(floor_mat))
            for y in range(F2 + 1, top + 1):
                b.air(x, y, z)
            floor.append((x, z))
    return floor


def tunnel(a, c, width=5, height=5, floor_mat="polished_blackstone_bricks", lantern_every=9):
    """Прямой тоннель между серединами двух пещер (по X или по Z). Под ним пол — над лавой это мост."""
    (ax, az), (cx, cz) = a, c
    half = width // 2
    if az == cz:
        xs = range(min(ax, cx), max(ax, cx) + 1)
        pts = [(x, az + o) for x in xs for o in range(-half, half + 1)]
        line = [(x, az) for x in xs]
    else:
        zs = range(min(az, cz), max(az, cz) + 1)
        pts = [(ax + o, z) for z in zs for o in range(-half, half + 1)]
        line = [(ax, z) for z in zs]
    for (x, z) in pts:
        key = b.blocks.get((x, F2, z))
        if key is None or key[0] in OPEN:
            b.set(x, F2, z, floor_mat if rng.random() < 0.8 else "cracked_polished_blackstone_bricks")
        for y in range(F2 + 1, F2 + 1 + height):
            b.air(x, y, z)
    for k, (x, z) in enumerate(line):
        if k % lantern_every == 4:
            b.set(x, F2 + height, z, "soul_lantern", hanging="true", waterlogged="false")
    return line


def place_mobs(floor, kinds, avoid=None):
    spots = [p for p in floor if b.get(p[0], F2 + 1, p[1]) == "minecraft:air" and b.get(p[0], F2 + 2, p[1]) == "minecraft:air"
             and b.get(p[0], F2, p[1]) not in ("minecraft:lava", "minecraft:magma_block")]
    if avoid:
        spots = [p for p in spots if math.hypot(p[0] - avoid[0], p[1] - avoid[1]) > avoid[2]]
    rng.shuffle(spots)
    for k, kind in enumerate(kinds):
        if k < len(spots):
            x, z = spots[k]
            b.mob(x, F2 + 1, z, kind, k % 2)


def free_spot(floor, near=None, radius=99):
    spots = [p for p in floor if b.get(p[0], F2 + 1, p[1]) == "minecraft:air" and b.get(p[0], F2, p[1]) != "minecraft:lava"]
    if near:
        spots = [p for p in spots if math.hypot(p[0] - near[0], p[1] - near[1]) <= radius]
    return rng.choice(spots) if spots else None


def wall_spot(floor):
    """Клетка пола у стены пещеры (для сундуков)."""
    out = []
    for (x, z) in floor:
        if b.get(x, F2 + 1, z) != "minecraft:air":
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, F2 + 1, z + dz) not in b.blocks:
                out.append((x, z))
                break
    return out


def l2():
    zones = {}
    # СЗ: колодец костей (сюда выходит спуск)
    zones["nw"] = cavern(0, 0, 10, 11)
    # С: пропасть — вся пещера до лавы, мосты кладут тоннели
    x0, z0, x1, z1 = zone_box(1, 0)
    cx, cz = zone_center(1, 0)
    chasm = []
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            dx, dz = abs(x - cx) / 14.5, abs(z - cz) / 14.5
            if (dx ** 3 + dz ** 3) ** (1 / 3) > 0.82 + 0.2 * noise(x, z, 5.0, 21):
                continue
            b.set(x, 0, z, "basalt", axis="y")
            b.set(x, 1, z, "lava", level=0)
            b.set(x, 2, z, "lava", level=0)
            top = F2 + 9 + int(5 * noise(x, z, 6.0, 22))
            for y in range(3, top + 1):
                b.air(x, y, z)
            chasm.append((x, z))
    zones["n"] = chasm
    zones["ne"] = cavern(2, 0, 8, 31, roundness=0.4)
    zones["w"] = cavern(0, 1, 9, 41, roundness=0.3)
    # Ц: кровавое озеро — пещера с лавой посередине
    lake = cavern(1, 1, 11, 51)
    lcx, lcz = zone_center(1, 1)
    for (x, z) in lake:
        r = math.hypot(x - lcx, z - lcz)
        if r < 9.5 + 2.0 * noise(x, z, 4.0, 52):
            b.set(x, F2, z, "lava", level=0)
            b.set(x, F2 - 1, z, "basalt", axis="y")
        elif r < 11.5:
            b.set(x, F2, z, "magma_block")
    zones["c"] = lake
    # В: галерея стражей — высокий прямой зал с колоннами
    gx0, gz0, gx1, gz1 = zone_box(2, 1)
    gallery = []
    for x in range(gx0 + 6, gx1 - 5):
        for z in range(gz0, gz1 + 1):
            b.set(x, F2, z, "polished_blackstone" if abs(x - 80) > 1 else "red_nether_bricks")
            for y in range(F2 + 1, F2 + 15):
                b.air(x, y, z)
            gallery.append((x, z))
    for z in range(gz0 + 2, gz1 - 1, 5):
        for x in (gx0 + 7, gx1 - 6):
            for y in range(F2 + 1, F2 + 15):
                b.set(x, y, z, "polished_blackstone_bricks" if y % 5 else "chiseled_polished_blackstone")
            b.set(x + (1 if x < 80 else -1), F2 + 6, z, "soul_wall_torch", facing="east" if x < 80 else "west")
            for y in range(F2 + 9, F2 + 14):
                b.set(x + (1 if x < 80 else -1), y, z, "crimson_hyphae", axis="y")
    zones["e"] = gallery
    zones["sw"] = cavern(0, 2, 9, 61)
    zones["s"] = cavern(1, 2, 8, 71, roundness=0.2)
    # ЮВ: логово — круглая арена с куполом
    acx, acz = zone_center(2, 2)
    arena = []
    for x in range(acx - 15, acx + 16):
        for z in range(acz - 15, acz + 16):
            r = math.hypot(x - acx, z - acz)
            if r > 14.5:
                continue
            ring = int(r) % 5 == 0
            b.set(x, F2, z, "red_nether_bricks" if ring else ("polished_blackstone" if (x + z) % 2 else "blackstone"))
            top = F2 + 16 - int(7 * (r / 14.5) ** 2)
            for y in range(F2 + 1, top + 1):
                b.air(x, y, z)
            arena.append((x, z))
    zones["se"] = arena
    return zones


def l2_links():
    p = {k: zone_center(*v) for k, v in {"nw": (0, 0), "n": (1, 0), "ne": (2, 0), "w": (0, 1), "c": (1, 1),
                                          "e": (2, 1), "sw": (0, 2), "s": (1, 2), "se": (2, 2)}.items()}
    lines = {}
    for a, c in (("nw", "n"), ("n", "ne"), ("nw", "w"), ("w", "sw"), ("sw", "s"), ("s", "c"), ("c", "n")):
        lines[(a, c)] = tunnel(p[a], p[c])
    lines[("ne", "e")] = tunnel(p["ne"], p["e"], width=5, height=6)
    lines[("c", "e")] = tunnel(p["c"], (p["e"][0] - 9, p["e"][1]), width=3, height=4)
    # врата логова: широкий проход из галереи в арену
    gate = tunnel((80, 56), (80, 70), width=7, height=9, floor_mat="polished_blackstone")
    for y in range(F2 + 1, F2 + 11):
        for x in (76, 84):
            for z in (63, 64):
                b.set(x, y, z, "chiseled_polished_blackstone" if y % 3 == 0 else "polished_blackstone_bricks")
    for x in range(76, 85):
        for z in (63, 64):
            b.set(x, F2 + 10, z, "crying_obsidian" if x in (79, 80, 81) else "polished_blackstone_bricks")
    b.set(80, F2 + 9, 62, "soul_lantern", hanging="true", waterlogged="false")
    return lines, gate


def l2_dress(zones):
    # ---- спуск: винтовая лестница с первого яруса в Колодец костей
    spiral(3, 3, F1, F2, F1 + 4)
    for x in range(2, 9):
        for z in range(2, 9):
            for y in range(F1 + 1, F1 + 5):
                if b.get(x, y, z) not in ("minecraft:deepslate_bricks", "minecraft:deepslate_brick_stairs",
                                          "minecraft:polished_deepslate", "minecraft:chiseled_deepslate"):
                    b.air(x, y, z)
    # выход из колодца вниз — прорубаем дверь на восток у самого низа
    for y in range(F2 + 1, F2 + 4):
        for z in range(4, 7):
            b.air(8, y, z)
            b.air(9, y, z)
        b.set(8, F2, 5, "polished_deepslate")

    nw = zones["nw"]
    for (x, z) in nw:
        if rng.random() < 0.1:
            b.set(x, F2, z, "bone_block", axis="y")
        elif rng.random() < 0.025 and b.get(x, F2 + 1, z) == "minecraft:air" and not (x < 10 and z < 10):
            b.set(x, F2 + 1, z, "bone_block", axis=rng.choice("xz"))
    for k in range(2):
        s = free_spot(nw, (16, 16), 10)
        if s:
            b.spawner(s[0], F2 + 1, s[1], "deep_ghoul")
    for s in rng.sample(wall_spot(nw), 2):
        b.chest(s[0], F2 + 1, s[1], "south", "deep_common")
    place_mobs(nw, ["deep_ghoul"] * 5 + ["deep_spitter"], avoid=(5, 5, 7))

    # ---- пропасть: островки-столбы с плевателями, на одном — сундук сокровищницы
    # три столба с плевателями (стреляют по мосту) и один с сундуком: до него прыжок через 1 блок лавы
    for k, (px, pz) in enumerate(((40, 7), (56, 8), (39, 26), (56, 21))):
        for x in range(px - 1, px + 2):
            for z in range(pz - 1, pz + 2):
                for y in range(0, F2):
                    b.set(x, y, z, "basalt", axis="y")
                b.set(x, F2, z, "blackstone")
                for y in range(F2 + 1, F2 + 4):
                    b.air(x, y, z)
        if k == 3:
            b.chest(px, F2 + 1, pz + 1, "north", "deep_vault")
        else:
            b.mob(px, F2 + 1, pz, "deep_spitter")
    # дыра в мосту: прыжок через лаву
    for o in range(-2, 3):
        b.air(42, F2, 16 + o)
    for (x, z) in zones["n"]:
        if rng.random() < 0.015 and b.get(x, F2, z) != "minecraft:air":
            b.set(x, F2 + 1, z, "soul_lantern", hanging="false", waterlogged="false")

    # ---- склеп: ниши-гробы в стенах, черепа, паутина
    ne = zones["ne"]
    for s in wall_spot(ne):
        r = rng.random()
        if r < 0.12:
            b.set(s[0], F2 + 1, s[1], "skeleton_skull", rotation=rng.randint(0, 15))
        elif r < 0.2:
            b.set(s[0], F2 + 1, s[1], "cobweb")
            b.set(s[0], F2 + 2, s[1], "cobweb")
    for x in range(68, 93, 6):
        for z in range(6, 29, 6):
            if b.get(x, F2 + 1, z) == "minecraft:air":
                for y in range(F2 + 1, F2 + 9):
                    if b.get(x, y, z) == "minecraft:air":
                        b.set(x, y, z, "polished_blackstone_bricks")
    for kind in ("deep_ghoul", "deep_spitter"):
        s = free_spot(ne)
        if s:
            b.spawner(s[0], F2 + 1, s[1], kind)
    for s, loot in zip(rng.sample(wall_spot(ne), 3), ("deep_common", "deep_common", "deep_vault")):
        b.chest(s[0], F2 + 1, s[1], "north", loot)
    place_mobs(ne, ["deep_ghoul"] * 4 + ["deep_spitter"] * 2 + ["deep_brute"])

    # ---- сокровищница: золочёный пол, шесть сундуков вдоль стен, ловушки у входов, стража
    w = zones["w"]
    for (x, z) in w:
        b.set(x, F2, z, "gilded_blackstone" if rng.random() < 0.3 else "polished_blackstone_bricks")
    spots = wall_spot(w)
    rng.shuffle(spots)
    for s in spots[:6]:
        b.chest(s[0], F2 + 1, s[1], "east", "deep_vault")
    for s in spots[6:9]:
        b.barrel(s[0], F2 + 1, s[1], "deep_common")
    for s in spots[9:13]:
        b.set(s[0], F2 + 1, s[1], "gold_block")
    for kind in ("deep_ghoul", "deep_brute"):
        s = free_spot(w, (16, 48), 6)
        if s:
            b.spawner(s[0], F2 + 1, s[1], kind, count=1 if kind == "deep_brute" else 2, nearby=3)
    place_mobs(w, ["deep_brute"] * 2 + ["deep_ghoul"] * 4 + ["deep_spitter"] * 2)

    # ---- кровавое озеро: скелеты на берегу, мутанты
    c = zones["c"]
    for (x, z) in c:
        if b.get(x, F2, z) == "minecraft:magma_block" and rng.random() < 0.08 and b.get(x, F2 + 1, z) == "minecraft:air":
            b.set(x, F2 + 1, z, "bone_block", axis=rng.choice("xz"))
    s = free_spot(c)
    if s:
        b.spawner(s[0], F2 + 1, s[1], "deep_brute", count=1, nearby=2, delay=(400, 900))
    for s in rng.sample(wall_spot(c), 2):
        b.chest(s[0], F2 + 1, s[1], "west", "deep_common")
    place_mobs(c, ["deep_brute"] * 2 + ["deep_spitter"] * 2 + ["deep_ghoul"] * 3)

    # ---- гнездо: плоть, кости, паутина, три спавнера
    sw = zones["sw"]
    for (x, z) in sw:
        if rng.random() < 0.45:
            b.set(x, F2, z, rng.choice(FLESH))
        if rng.random() < 0.04 and b.get(x, F2 + 1, z) == "minecraft:air":
            b.set(x, F2 + 1, z, rng.choice(["cobweb", "bone_block", "nether_wart_block"]))
    for kind in ("deep_ghoul", "deep_ghoul", "deep_spitter"):
        s = free_spot(sw)
        if s:
            b.spawner(s[0], F2 + 1, s[1], kind)
    for s, loot in zip(rng.sample(wall_spot(sw), 3), ("deep_common", "deep_common", "deep_vault")):
        b.chest(s[0], F2 + 1, s[1], "south", loot)
    place_mobs(sw, ["deep_ghoul"] * 8 + ["deep_brute"])

    # ---- темница: средний проход (x 46..50), по бокам камеры за решётками; проход с запада (z 78..82)
    s_floor = zones["s"]
    aisle = range(45, 52)
    for x in range(34, 63):
        for z in range(66, 95):
            if b.get(x, F2 + 1, z) != "minecraft:air" or x in aisle or 77 <= z <= 83:
                continue
            if x in (44, 52) and z % 5 != 2:
                # решётка камеры вдоль прохода; у каждой третьей камеры прутья выломаны
                if (z // 5) % 2 == 0 and z % 5 in (1, 3):
                    continue
                for y in range(F2 + 1, F2 + 4):
                    b.set(x, y, z, "iron_bars", north="true", south="true", east="false", west="false",
                          waterlogged="false")
            elif z % 5 == 2 and (x < 44 or x > 52):
                for y in range(F2 + 1, F2 + 4):
                    b.set(x, y, z, "polished_blackstone_bricks")
    for k, (x, z) in enumerate(((39, 73), (57, 73), (39, 91), (57, 91), (40, 69), (58, 69))):
        if b.get(x, F2 + 1, z) == "minecraft:air":
            if k < 3:
                b.chest(x, F2 + 1, z, "east" if x < 48 else "west", "deep_common")
            else:
                b.set(x, F2 + 1, z, "skeleton_skull", rotation=rng.randint(0, 15))
                b.set(x, F2, z, "bone_block", axis="y")
    s = free_spot(s_floor)
    if s:
        b.spawner(s[0], F2 + 1, s[1], "deep_ghoul")
    place_mobs(s_floor, ["deep_ghoul"] * 4 + ["deep_brute"] * 2 + ["deep_spitter"])

    # ---- галерея стражей: два стража у врат, у входа — пара гулей
    b.mob(75, F2 + 1, 57, "deep_guardian_sting")
    b.mob(85, F2 + 1, 57, "deep_guardian_wing")
    for x, z in ((76, 40), (84, 44)):
        b.mob(x, F2 + 1, z, "deep_ghoul", 0)

    # ---- логово: трон, сундуки за троном, кольцо огней
    acx, acz = zone_center(2, 2)
    tz = acz + 10
    for x in range(acx - 3, acx + 4):
        for z in range(tz - 1, tz + 3):
            for y in range(F2 + 1, F2 + 3):
                b.set(x, y, z, "polished_blackstone_bricks")
    for x in range(acx - 3, acx + 4):
        b.set(x, F2 + 1, tz - 2, "polished_blackstone_brick_stairs", facing="south", half="bottom", shape="straight",
              waterlogged="false")
        b.set(x, F2 + 2, tz - 1, "polished_blackstone_brick_stairs", facing="south", half="bottom", shape="straight",
              waterlogged="false")
    b.set(acx, F2 + 3, tz, "polished_blackstone_brick_stairs", facing="south", half="bottom", shape="straight",
          waterlogged="false")
    for y in range(F2 + 3, F2 + 9):
        for x in range(acx - 1, acx + 2):
            b.set(x, y, tz + 1, "crying_obsidian" if x == acx else "chiseled_polished_blackstone")
    b.set(acx - 1, F2 + 3, tz, "red_candle", candles=3, lit="true", waterlogged="false")
    b.set(acx + 1, F2 + 3, tz, "red_candle", candles=3, lit="true", waterlogged="false")
    for k, x in enumerate((acx - 3, acx - 2, acx + 2, acx + 3)):
        b.chest(x, F2 + 1, tz + 3, "north", "deep_boss")
    for k in range(10):
        a = k * math.pi * 2 / 10
        x, z = int(round(acx + math.cos(a) * 12.5)), int(round(acz + math.sin(a) * 12.5))
        if z < acz - 9 and abs(x - acx) < 5:
            continue
        for y in range(F2 + 1, F2 + 4):
            b.set(x, y, z, "polished_blackstone_bricks")
        b.set(x, F2 + 4, z, "soul_campfire", lit="true", signal_fire="false", facing="north", waterlogged="false")
    for (x, z) in zones["se"]:
        if rng.random() < 0.05 and b.get(x, F2, z) != "minecraft:air":
            b.set(x, F2, z, rng.choice(FLESH))
    b.mob(acx, F2 + 1, acz + 6, "ancient_ghoul")


# ============================================================================= обкладка пустот
def seal():
    """Каждая пустота (воздух, лава, паутина) ниже поверхности обложена стеной, чтобы мир не затекал."""
    opens = [p for p, key in b.blocks.items() if key[0] in OPEN and p[1] <= GROUND]
    for (x, y, z) in opens:
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            q = (x + dx, y + dy, z + dz)
            if not (0 <= q[0] < W and 0 <= q[1] < H and 0 <= q[2] < D):
                continue
            if q not in b.blocks:
                b.blocks[q] = ("minecraft:" + shell(*q), ())

# ============================================================================= свет
LANTERN_GRID = 8      # ярус 2: стоячий фонарь душ на каждую клетку 8 x 8, где есть куда поставить
L1_LIGHT_GRID = 7     # ярус 1: невидимый источник света (блок light) на каждую клетку 7 x 7


def add_light():
    """Больше света в данже. Запускается ПОСЛЕ seal(): ничего из прежней раскладки (и rng) не меняется,
    свет ставится только в уже существующий воздух, так что стены остаются целыми."""
    def is_air(x, y, z):
        key = b.blocks.get((x, y, z))
        return key is not None and key[0] == "minecraft:air"

    def solid_floor(x, y, z):
        key = b.blocks.get((x, y, z))
        return key is not None and key[0] not in OPEN and "lava" not in key[0] and key[0] not in (
            "minecraft:magma_block", "minecraft:iron_bars", "minecraft:soul_lantern", "minecraft:spawner",
            "minecraft:chest", "minecraft:barrel", "minecraft:red_candle", "minecraft:soul_campfire")

    mob_cells = set()
    for e in b.entities:
        mx, my, mz = (int(t.value) for t in e["blockPos"].value)
        mob_cells.add((mx, mz))

    def near_mob(x, z):
        return any((x + dx, z + dz) in mob_cells for dx in (-1, 0, 1) for dz in (-1, 0, 1))

    def light(x, y, z, level):
        if is_air(x, y, z):
            b.set(x, y, z, "light", level=level, waterlogged="false")
            return 1
        return 0

    acx, acz = zone_center(2, 2)
    gx0, gz0, gx1, gz1 = zone_box(2, 1)

    def in_boss_area(x, z):
        return math.hypot(x - acx, z - acz) < 16 or (gx0 + 5 <= x <= gx1 - 4 and gz0 <= z <= gz1 + 8)

    lanterns = lights = 0
    # ярус 2: фонари на полу (кроме логова и галереи стражей: там нужен простор для боя)
    for gx in range(0, W, LANTERN_GRID):
        for gz in range(0, D, LANTERN_GRID):
            best = None
            for x in range(gx, min(gx + LANTERN_GRID, W)):
                for z in range(gz, min(gz + LANTERN_GRID, D)):
                    if in_boss_area(x, z) or near_mob(x, z):
                        continue
                    if solid_floor(x, F2, z) and is_air(x, F2 + 1, z) and is_air(x, F2 + 2, z):
                        h = hash2(x, z, 4242)
                        if best is None or h < best[0]:
                            best = (h, x, z)
            if best:
                b.set(best[1], F2 + 1, best[2], "soul_lantern", hanging="false", waterlogged="false")
                lanterns += 1
    # логово Древнего гуля: невидимые источники под куполом, чтобы вся арена была видна
    lights += light(acx, F2 + 7, acz, 12)
    for k in range(6):
        a = k * math.pi / 3
        lights += light(int(round(acx + math.cos(a) * 8)), F2 + 7, int(round(acz + math.sin(a) * 8)), 12)
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        lights += light(int(round(acx + math.cos(a) * 13)), F2 + 6, int(round(acz + math.sin(a) * 13)), 12)
    # галерея стражей и врата в логово
    for z in range(gz0 + 4, gz1 - 2, 7):
        for x in (76, 84):
            lights += light(x, F2 + 9, z, 12)
    lights += light(80, F2 + 6, 66, 12)
    lights += light(80, F2 + 6, 72, 12)
    # ярус 1: невидимый мягкий свет под потолком коридоров и залов
    for gx in range(0, W, L1_LIGHT_GRID):
        for gz in range(0, D, L1_LIGHT_GRID):
            best = None
            for x in range(gx, min(gx + L1_LIGHT_GRID, W)):
                for z in range(gz, min(gz + L1_LIGHT_GRID, D)):
                    if all(is_air(x, F1 + dy, z) for dy in (1, 2, 3)) and solid_floor(x, F1, z):
                        h = hash2(x, z, 4343)
                        if best is None or h < best[0]:
                            best = (h, x, z)
            if best:
                lights += light(best[1], F1 + 3, best[2], 9)
    print(f"deep_ward: свет — фонарей {lanterns}, невидимых источников {lights}")


def build():
    surface()
    links, degree, room_of = l1()
    shaft()
    l1_dress(links, degree, room_of)
    zones = l2()
    l2_links()
    l2_dress(zones)
    seal()
    add_light()
    return b, zones


def maps(folder):
    """Карты ярусов сверху (для проверки): ярус 1 на высоте y 31, ярус 2 на y 9."""
    from PIL import Image
    colors = {"air": (24, 22, 26), "lava": (230, 90, 20), "chest": (240, 200, 60), "spawner": (60, 200, 255),
              "barrel": (200, 150, 60), "dispenser": (255, 255, 255), "polished_blackstone_pressure_plate": (255, 0, 255)}
    mobs = {}
    for e in b.entities:
        x, y, z = (int(t.value) for t in e["blockPos"].value)
        mobs[(x, y, z)] = e["nbt"].value["id"].value.split(":")[1]
    for name, y in (("level1", F1 + 1), ("level2", F2 + 1), ("surface", GROUND + 1)):
        img = Image.new("RGB", (W, D), (90, 90, 90))
        for x in range(W):
            for z in range(D):
                key = b.blocks.get((x, y, z))
                if key is None:
                    c = (12, 12, 12)
                else:
                    n = key[0].split(":")[1]
                    c = colors.get(n, (24, 22, 26) if n in ("cobweb", "soul_lantern") else (120, 110, 105))
                    if n == "air":
                        below = b.blocks.get((x, y - 1, z))
                        if below and below[0].endswith("lava"):
                            c = (180, 60, 10)
                        elif below is None or below[0] == "minecraft:air":
                            c = (60, 20, 20)
                img.putpixel((x, z), c)
        for (x, yy, z), kind in mobs.items():
            if abs(yy - y) <= 3:
                img.putpixel((x, z), {"deep_guardian_sting": (255, 40, 40), "deep_guardian_wing": (255, 140, 40),
                                      "ancient_ghoul": (255, 0, 120), "deep_brute": (160, 255, 60)}.get(kind, (80, 220, 80)))
        img.resize((W * 6, D * 6), Image.NEAREST).save(os.path.join(folder, f"deep_{name}.png"))


if __name__ == "__main__":
    out = sys.argv[1]
    build()
    b.save(os.path.join(out, "deep_ward.nbt"))
    chests = sum(1 for v in b.block_nbt.values() if v.get("id") and v["id"].value in ("minecraft:chest", "minecraft:barrel"))
    spawners = sum(1 for v in b.block_nbt.values() if v.get("id") and v["id"].value == "minecraft:mob_spawner")
    print(f"deep_ward: blocks {len(b.blocks)}, entities {len(b.entities)}, chests/barrels {chests}, spawners {spawners}")
    if len(sys.argv) > 3 and sys.argv[2] == "--maps":
        maps(sys.argv[3])
