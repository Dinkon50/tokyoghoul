"""
Постройки мода в формате .nbt (как сохраняет структурный блок Minecraft 1.20.1, DataVersion 3465).
  ccg_office    — двухэтажный офис CCG: белые стены, кварцевые колонны, окна, кабинет босса на 2-м этаже.
  ghoul_hideout — разрушенное логово гулей: глубинносланцевые руины, паутина, кости, кровь на полу.
  half_ghoul_arena — круглая арена Кровавого полугуля: алтарь в центре, колонны, сундуки с редкой добычей.
  cafe          — кофейня: мирная зона, стойка с хозяином, столики, кухня; открыта днём.
  rc_lab        — заброшенная лаборатория RC: камеры, колбы, архив с добычей, подопытные.
  black_market  — чёрный рынок: Скупщик, торговец, мастерская масок.
  subway        — заброшенное метро: павильон, спуск, лабиринт с ловушками, зал мини-босса.
Запуск: python3 structures.py <папка data/tokyoghoulrpg/structures> [имена построек]
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402
from nbt import BYTE, COMPOUND, DOUBLE, FLOAT, INT, LIST, STRING, Tag  # noqa: E402

DATA_VERSION = 3465
MOD = "tokyoghoulrpg"


class Build:
    def __init__(self, w, h, d, seed):
        self.w, self.h, self.d = w, h, d
        self.blocks = {}
        self.block_nbt = {}
        self.entities = []
        self.rng = random.Random(seed)
        # True — незаданные клетки не пишутся (остаётся земля/камень мира, как structure_void)
        self.void_unset = False

    def set(self, x, y, z, name, **props):
        assert 0 <= x < self.w and 0 <= y < self.h and 0 <= z < self.d, (x, y, z)
        self.blocks[(x, y, z)] = (name if ":" in name else "minecraft:" + name,
                                  tuple(sorted((k, str(v).lower()) for k, v in props.items())))

    def fill(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, **props)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), ("minecraft:air", ()))[0]

    def chest(self, x, y, z, facing, loot):
        self.set(x, y, z, "chest", facing=facing, type="single", waterlogged="false")
        self.block_nbt[(x, y, z)] = {"id": Tag(STRING, "minecraft:chest"),
                                     "LootTable": Tag(STRING, f"{MOD}:chests/{loot}")}

    def door(self, x, y, z, name, facing, hinge="left"):
        for half, dy in (("lower", 0), ("upper", 1)):
            self.set(x, y + dy, z, name, facing=facing, half=half, hinge=hinge, open="false", powered="false")

    def mob(self, x, y, z, entity, variant=0, hand=None, armor=(None, None, None, None), drop_head=0.085, extra=None):
        """Моб с готовым снаряжением: так он одет, даже если игра не вызовет finalizeSpawn."""
        def stack(item):
            if item is None:
                return Tag(COMPOUND, {})
            return Tag(COMPOUND, {"id": Tag(STRING, item), "Count": Tag(BYTE, 1)})
        tag = {
            "id": Tag(STRING, f"{MOD}:{entity}"),
            "PersistenceRequired": Tag(BYTE, 1),
            "SkinVariant": Tag(INT, variant),
            "HandItems": Tag(LIST, [stack(hand), stack(None)], item_type=COMPOUND),
            "ArmorItems": Tag(LIST, [stack(a) for a in armor], item_type=COMPOUND),
            "HandDropChances": Tag(LIST, [Tag(FLOAT, 0.085), Tag(FLOAT, 0.085)], item_type=FLOAT),
            "ArmorDropChances": Tag(LIST, [Tag(FLOAT, 0.085), Tag(FLOAT, 0.085), Tag(FLOAT, 0.085),
                                           Tag(FLOAT, drop_head)], item_type=FLOAT),
            "Rotation": Tag(LIST, [Tag(FLOAT, float(self.rng.randint(0, 359))), Tag(FLOAT, 0.0)], item_type=FLOAT),
        }
        if extra:
            tag.update(extra)
        self.entities.append({
            "pos": Tag(LIST, [Tag(DOUBLE, x + 0.5), Tag(DOUBLE, float(y)), Tag(DOUBLE, z + 0.5)], item_type=DOUBLE),
            "blockPos": Tag(LIST, [Tag(INT, x), Tag(INT, y), Tag(INT, z)], item_type=INT),
            "nbt": Tag(COMPOUND, tag),
        })

    def save(self, path):
        palette, index = [], {}
        blocks = []
        # Все клетки объёма: пустые тоже записываются воздухом, чтобы постройка вырезала землю внутри
        for y in range(self.h):
            for z in range(self.d):
                for x in range(self.w):
                    if self.void_unset and (x, y, z) not in self.blocks:
                        continue
                    key = self.blocks.get((x, y, z), ("minecraft:air", ()))
                    if key not in index:
                        index[key] = len(palette)
                        entry = {"Name": Tag(STRING, key[0])}
                        if key[1]:
                            entry["Properties"] = Tag(COMPOUND, {k: Tag(STRING, v) for k, v in key[1]})
                        palette.append(Tag(COMPOUND, entry))
                    block = {"pos": Tag(LIST, [Tag(INT, x), Tag(INT, y), Tag(INT, z)], item_type=INT),
                             "state": Tag(INT, index[key])}
                    if (x, y, z) in self.block_nbt:
                        block["nbt"] = Tag(COMPOUND, self.block_nbt[(x, y, z)])
                    blocks.append(Tag(COMPOUND, block))
        root = {
            "DataVersion": Tag(INT, DATA_VERSION),
            "size": Tag(LIST, [Tag(INT, self.w), Tag(INT, self.h), Tag(INT, self.d)], item_type=INT),
            "palette": Tag(LIST, palette, item_type=COMPOUND),
            "blocks": Tag(LIST, blocks, item_type=COMPOUND),
            "entities": Tag(LIST, [Tag(COMPOUND, e) for e in self.entities], item_type=COMPOUND),
        }
        nbt.save(path, root)
        return len(palette), len(blocks), len(self.entities)


# ---------------------------------------------------------------------------------------------
def ccg_office():
    """
    Офис CCG 15 x 12 x 11. Вход с севера (z = 0). Первый этаж: приёмная со стойкой, скамьи,
    книжные шкафы, лестница-стремянка в углу. Второй этаж: кабинет старшего следователя,
    сундук с добычей. Плоская крыша с парапетом.
    """
    W, H, D = 15, 12, 11
    b = Build(W, H, D, 7101)
    # фундамент и полы
    b.fill(0, 0, 0, W - 1, 0, D - 1, "polished_andesite")
    b.fill(1, 0, 1, W - 2, 0, D - 2, "smooth_stone")
    b.fill(1, 5, 1, W - 2, 5, D - 2, "smooth_quartz")
    # стены
    for y in range(1, 10):
        for x in range(W):
            for z in (0, D - 1):
                b.set(x, y, z, "white_concrete")
        for z in range(D):
            for x in (0, W - 1):
                b.set(x, y, z, "white_concrete")
    # межэтажный пояс и колонны по углам
    for x in range(W):
        for z in (0, D - 1):
            b.set(x, 5, z, "light_gray_concrete")
    for z in range(D):
        for x in (0, W - 1):
            b.set(x, 5, z, "light_gray_concrete")
    for x, z in ((0, 0), (W - 1, 0), (0, D - 1), (W - 1, D - 1)):
        for y in range(1, 11):
            b.set(x, y, z, "quartz_pillar", axis="y")
    # окна: на обоих этажах, по две клетки в высоту
    for y0 in (2, 7):
        for x in (2, 3, 10, 11, 12) if y0 == 2 else (2, 3, 5, 6, 7, 8, 11, 12):
            for y in (y0, y0 + 1):
                b.set(x, y, 0, "light_blue_stained_glass")
                b.set(x, y, D - 1, "light_blue_stained_glass")
        for z in (3, 4, 6, 7):
            for y in (y0, y0 + 1):
                b.set(0, y, z, "light_blue_stained_glass")
                b.set(W - 1, y, z, "light_blue_stained_glass")
    # вход: двойная дверь посередине, по бокам кварцевые колонны
    b.door(6, 1, 0, "dark_oak_door", "south", hinge="left")
    b.door(7, 1, 0, "dark_oak_door", "south", hinge="right")
    for y in range(1, 4):
        b.set(5, y, 0, "quartz_pillar", axis="y"); b.set(8, y, 0, "quartz_pillar", axis="y")
    # вывеска над входом: синий бетон и эмблема из белого
    for x in range(4, 10):
        b.set(x, 4, 0, "blue_concrete")
    b.set(6, 4, 0, "sea_lantern"); b.set(7, 4, 0, "sea_lantern")
    # крыша и парапет
    b.fill(0, 10, 0, W - 1, 10, D - 1, "smooth_quartz")
    for x in range(W):
        b.set(x, 11, 0, "white_concrete"); b.set(x, 11, D - 1, "white_concrete")
    for z in range(D):
        b.set(0, 11, z, "white_concrete"); b.set(W - 1, 11, z, "white_concrete")
    # свет: морские фонари в потолках
    for x, z in ((3, 3), (11, 3), (3, 7), (11, 7), (7, 5)):
        b.set(x, 5, z, "sea_lantern")
        b.set(x, 10, z, "sea_lantern")
    # ---- первый этаж
    for z in range(1, D - 1):
        b.set(7, 0, z, "blue_wool")                                  # ковровая дорожка (шерсть в полу)
    b.fill(4, 1, 6, 10, 1, 6, "quartz_block")                        # стойка приёмной
    b.set(4, 1, 7, "quartz_block"); b.set(10, 1, 7, "quartz_block")
    b.set(5, 2, 6, "lantern", hanging="false", waterlogged="false")
    b.set(9, 2, 6, "potted_fern")
    for x in (2, 12):                                                # скамьи вдоль стен
        for z in (3, 4):
            b.set(x, 1, z, "spruce_stairs", facing="west" if x == 12 else "east", half="bottom",
                  shape="straight", waterlogged="false")
    for x in range(1, 6):
        b.set(x, 1, D - 2, "bookshelf"); b.set(x, 2, D - 2, "bookshelf")
    for x in range(9, 12):
        b.set(x, 1, D - 2, "bookshelf"); b.set(x, 2, D - 2, "bookshelf")
    b.set(1, 1, 1, "potted_bamboo"); b.set(W - 2, 1, 1, "potted_bamboo")
    # лестница-стремянка на второй этаж (у задней стены) и проём в перекрытии
    for y in range(1, 6):
        b.set(13, y, D - 2, "ladder", facing="north", waterlogged="false")
    b.set(13, 5, D - 2, "ladder", facing="north", waterlogged="false")
    b.set(13, 6, D - 2, "air")
    # ---- второй этаж: кабинет
    for z in range(1, D - 1):
        b.set(7, 5, z, "blue_wool")
    b.fill(5, 6, 6, 9, 6, 6, "dark_oak_planks")                      # рабочий стол
    b.set(5, 7, 6, "lantern", hanging="false", waterlogged="false")
    b.set(9, 7, 6, "potted_white_tulip")
    b.set(7, 6, 7, "spruce_stairs", facing="north", half="bottom", shape="straight", waterlogged="false")
    b.chest(3, 6, D - 2, "north", "ccg_office")
    b.chest(11, 6, D - 2, "north", "ccg_office")
    for x in (1, 2, 4, 5, 9, 10, 12):
        b.set(x, 6, D - 2, "bookshelf"); b.set(x, 7, D - 2, "bookshelf")
    b.set(2, 6, 1, "potted_bamboo"); b.set(12, 6, 1, "potted_bamboo")
    # ---- жители офиса
    uniform = ("tokyoghoulrpg:ccg_boots", "tokyoghoulrpg:ccg_pants", "tokyoghoulrpg:ccg_coat", None)
    b.mob(4, 1, 3, "ccg_agent", 0, hand="minecraft:iron_sword", armor=uniform)
    b.mob(10, 1, 3, "ccg_agent", 1, hand="minecraft:iron_sword", armor=uniform)
    b.mob(7, 6, 4, "ccg_boss", 0, hand="minecraft:diamond_sword")
    return b


def ghoul_hideout():
    """
    Логово гулей 13 x 7 x 11: полуразрушенный дом из глубинного сланца, провалившаяся крыша,
    паутина, кости и следы крови. Вход — пролом в северной стене. Внутри Пожиратель и два гуля.
    """
    W, H, D = 13, 7, 11
    b = Build(W, H, D, 6601)
    rng = b.rng
    wall_mix = ["deepslate_bricks", "cracked_deepslate_bricks", "cobbled_deepslate", "deepslate_bricks",
                "mossy_cobblestone", "cracked_deepslate_bricks"]
    # пол
    for x in range(W):
        for z in range(D):
            b.set(x, 0, z, rng.choice(["cobbled_deepslate", "deepslate_tiles", "cracked_deepslate_tiles",
                                       "coarse_dirt", "cobbled_deepslate"]))
    # стены с разной высотой (разрушены сверху)
    for x in range(W):
        for z in range(D):
            edge = x in (0, W - 1) or z in (0, D - 1)
            if not edge:
                continue
            top = 4 if (x + z) % 5 else 2
            top -= rng.choice([0, 0, 1])
            for y in range(1, top + 1):
                b.set(x, y, z, rng.choice(wall_mix))
    # колонны по углам целые
    for x, z in ((0, 0), (W - 1, 0), (0, D - 1), (W - 1, D - 1)):
        for y in range(1, 6):
            b.set(x, y, z, "polished_deepslate")
    # пролом-вход в северной стене
    for x in (5, 6, 7):
        for y in (1, 2, 3):
            b.set(x, y, 0, "air")
    b.set(4, 3, 0, "air"); b.set(8, 3, 0, "air")
    # остатки крыши: балки и доски с дырами
    for x in range(1, W - 1):
        b.set(x, 5, 3, "dark_oak_log", axis="x")
        b.set(x, 5, 7, "dark_oak_log", axis="x")
    for x in range(1, W - 1):
        for z in range(1, D - 1):
            if z in (3, 7):
                continue
            if rng.random() < 0.45:
                b.set(x, 5, z, rng.choice(["dark_oak_planks", "dark_oak_planks", "spruce_planks"]))
    # паутина в углах и под крышей
    for _ in range(18):
        x, y, z = rng.randint(1, W - 2), rng.randint(2, 4), rng.randint(1, D - 2)
        if b.get(x, y, z) == "minecraft:air":
            b.set(x, y, z, "cobweb")
    # кровь на полу (красные ковры), кости, черепа
    for _ in range(14):
        x, z = rng.randint(1, W - 2), rng.randint(1, D - 2)
        b.set(x, 1, z, rng.choice(["red_carpet", "red_carpet", "brown_carpet"]))
    for x, z in ((2, 8), (10, 2), (9, 8)):
        b.set(x, 1, z, "bone_block", axis="y")
    b.set(2, 2, 8, "skeleton_skull", rotation=4)
    b.set(10, 1, 3, "skeleton_skull", rotation=10)
    # «стол» из досок и свет: фонари душ
    b.set(6, 1, 8, "dark_oak_planks"); b.set(7, 1, 8, "dark_oak_planks")
    b.set(6, 2, 8, "soul_lantern", hanging="false", waterlogged="false")
    b.set(2, 1, 2, "soul_lantern", hanging="false", waterlogged="false")
    b.set(10, 1, 7, "soul_lantern", hanging="false", waterlogged="false")
    # сундуки с добычей
    b.chest(1, 1, D - 2, "east", "ghoul_hideout")
    b.chest(W - 2, 1, D - 2, "west", "ghoul_hideout")
    # проход внутрь свободен
    for z in range(1, 5):
        for x in (5, 6, 7):
            for y in (1, 2, 3):
                if b.get(x, y, z) == "minecraft:cobweb":
                    b.set(x, y, z, "air")
    # вокруг обитателей паутины нет: им нужно место, чтобы встать
    for mx, mz in ((6, 5), (3, 4), (9, 5)):
        for x in range(mx - 1, mx + 2):
            for z in range(mz - 1, mz + 2):
                for y in range(1, 5):
                    if b.get(x, y, z) in ("minecraft:cobweb", "minecraft:red_carpet", "minecraft:brown_carpet"):
                        b.set(x, y, z, "air")
    # обитатели
    b.mob(6, 1, 5, "ghoul_boss", 0, armor=(None, None, None, "tokyoghoulrpg:mask_oni"), drop_head=1.0)
    b.mob(3, 1, 4, "ghoul", 0, armor=(None, None, None, "tokyoghoulrpg:mask_fang"))
    b.mob(9, 1, 5, "ghoul", 2, armor=(None, None, "tokyoghoulrpg:ghoul_coat", "tokyoghoulrpg:mask_bone"))
    return b


def half_ghoul_arena():
    """
    Арена Кровавого полугуля 39 x 13 x 39. Круглая площадка радиусом 18 из чернокаменного кирпича,
    кольца из багровых и плачущих обсидиановых плит, в центре алтарь арены (он призывает босса).
    Низкая стена по кругу с четырьмя проходами, восемь колонн с фонарями душ, четыре сундука с редкой добычей.
    Внутри всё освещено, чтобы на арене не появлялись обычные монстры.
    """
    W, H, D = 39, 13, 39
    C = 19
    b = Build(W, H, D, 7707)
    rng = b.rng
    import math
    for x in range(W):
        for z in range(D):
            r = math.hypot(x - C, z - C)
            if r > 18.6:
                # углы квадрата: земля и обломки, чтобы не было дыр
                b.set(x, 0, z, rng.choice(["coarse_dirt", "blackstone", "coarse_dirt", "gravel"]))
                continue
            if r < 1.0:
                continue                                         # центр — алтарь
            if r < 3.2:
                block = "crying_obsidian" if int(r * 3) % 2 == 0 else "obsidian"
            elif r < 4.6:
                block = "red_nether_bricks"
            elif abs(r - 9.0) < 0.6:
                block = "shroomlight" if (x - C) * (z - C) == 0 or abs(abs(x - C) - abs(z - C)) <= 0 else "red_nether_bricks"
            elif abs(r - 14.0) < 0.5:
                block = "chiseled_polished_blackstone"
            else:
                angle = math.atan2(z - C, x - C)
                spoke = min(abs(((angle / (math.pi / 4)) % 1.0) - 0.5) for _ in range(1)) < 0.06
                block = "polished_blackstone" if spoke else rng.choice(
                    ["polished_blackstone_bricks"] * 6 + ["cracked_polished_blackstone_bricks"] * 2 + ["gilded_blackstone"])
            b.set(x, 0, z, block)
    b.set(C, 0, C, "tokyoghoulrpg:arena_altar")
    # светящиеся пятна крови на площадке
    for _ in range(26):
        x, z = rng.randint(4, W - 5), rng.randint(4, D - 5)
        if 4.6 < math.hypot(x - C, z - C) < 16.5 and b.get(x, 0, z) != "minecraft:shroomlight":
            b.set(x, 0, z, rng.choice(["crimson_nylium", "red_nether_bricks", "crying_obsidian"]))
    # стена по кругу высотой 2 с полуплитами сверху и четыре прохода (север, юг, запад, восток)
    for x in range(W):
        for z in range(D):
            r = math.hypot(x - C, z - C)
            if 17.6 <= r <= 18.6:
                if abs(x - C) <= 2 or abs(z - C) <= 2:
                    continue
                b.set(x, 1, z, "polished_blackstone_bricks")
                b.set(x, 2, z, "polished_blackstone_bricks" if rng.random() < 0.85 else "cracked_polished_blackstone_bricks")
                b.set(x, 3, z, "polished_blackstone_brick_slab", type="bottom", waterlogged="false")
    # восемь колонн радиусом 13 с фонарями
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        px, pz = C + round(math.cos(a) * 13), C + round(math.sin(a) * 13)
        for dx in (0, 1):
            for dz in (0, 1):
                for y in range(1, 9):
                    mid = y in (4, 5)
                    b.set(px + dx, y, pz + dz, "shroomlight" if (mid and (dx + dz) % 2 == 0) else
                          ("chiseled_polished_blackstone" if y in (1, 8) else "polished_blackstone"))
                b.set(px + dx, 9, pz + dz, "crying_obsidian")
        b.set(px, 10, pz, "soul_lantern", hanging="false", waterlogged="false")
        b.set(px + 1, 10, pz + 1, "soul_lantern", hanging="false", waterlogged="false")
    # сундуки с редкой добычей по диагоналям, лицом к центру
    for sx, sz, facing in ((C - 11, C - 11, "south"), (C + 11, C - 11, "south"), (C - 11, C + 11, "north"), (C + 11, C + 11, "north")):
        b.chest(sx, 1, sz, facing, "half_ghoul_arena")
        b.set(sx, 0, sz, "gilded_blackstone")
    return b


def cafe():
    """
    Кофейня 15 x 8 x 15 (квадрат: зона кофейни одинакова при любом повороте постройки). Вход с севера (z = 0).
    В полу за стойкой знак кофейни (7, 0, 8): на нём стоит хозяин, от него считается мирная зона.
    Спереди витрины и два столика со стульями, U-образная стойка с «кофемашиной», сзади кухня.
    """
    W, H, D = 15, 8, 15
    b = Build(W, H, D, 7808)
    # пол: тёмное дерево, «шахматка» у стойки
    b.fill(0, 0, 0, W - 1, 0, D - 1, "spruce_planks")
    for x in range(3, 12):
        for z in range(6, 11):
            b.set(x, 0, z, "dark_oak_planks" if (x + z) % 2 == 0 else "stripped_dark_oak_wood", axis="y")
    b.set(7, 0, 8, "tokyoghoulrpg:cafe_sign")
    # стены: низ — тёмный дуб, выше — белая терракота; углы — брёвна
    for y in range(1, 5):
        for x in range(W):
            for z in (0, D - 1):
                b.set(x, y, z, "dark_oak_planks" if y == 1 else "white_terracotta")
        for z in range(D):
            for x in (0, W - 1):
                b.set(x, y, z, "dark_oak_planks" if y == 1 else "white_terracotta")
    for x, z in ((0, 0), (W - 1, 0), (0, D - 1), (W - 1, D - 1)):
        for y in range(1, 6):
            b.set(x, y, z, "stripped_dark_oak_log", axis="y")
    # витрины спереди и окна по бокам
    for x in (2, 3, 4, 10, 11, 12):
        for y in (2, 3):
            b.set(x, y, 0, "brown_stained_glass")
    for z in (3, 4, 10, 11):
        for y in (2, 3):
            b.set(0, y, z, "brown_stained_glass")
            b.set(W - 1, y, z, "brown_stained_glass")
    # дверь и полосатый навес над входом
    b.door(7, 1, 0, "spruce_door", "south", hinge="left")
    for x in range(3, 12):
        b.set(x, 4, 0, "red_wool" if x % 2 == 0 else "white_wool")
    # крыша и бортик
    b.fill(0, 5, 0, W - 1, 5, D - 1, "dark_oak_planks")
    for x in range(W):
        b.set(x, 6, 0, "dark_oak_slab", type="bottom", waterlogged="false")
        b.set(x, 6, D - 1, "dark_oak_slab", type="bottom", waterlogged="false")
    for z in range(D):
        b.set(0, 6, z, "dark_oak_slab", type="bottom", waterlogged="false")
        b.set(W - 1, 6, z, "dark_oak_slab", type="bottom", waterlogged="false")
    b.set(7, 6, 7, "campfire", facing="north", lit="false", signal_fire="false", waterlogged="false")  # «труба»
    # свет: фонари под потолком
    for x, z in ((3, 3), (11, 3), (7, 3), (3, 11), (11, 11), (7, 12)):
        b.set(x, 4, z, "lantern", hanging="true", waterlogged="false")
    # U-образная стойка, хозяин внутри
    for x in range(3, 12):
        b.set(x, 1, 6, "spruce_planks")
    for z in (7, 8, 9):
        b.set(3, 1, z, "spruce_planks")
        b.set(11, 1, z, "spruce_planks")
    b.set(5, 2, 6, "brewing_stand", has_bottle_0="false", has_bottle_1="false", has_bottle_2="false")
    b.set(9, 2, 6, "potted_red_tulip")
    b.set(10, 2, 6, "lantern", hanging="false", waterlogged="false")
    b.set(3, 2, 8, "potted_azalea_bush")
    # кухня у задней стены
    b.set(4, 1, 13, "smoker", facing="south", lit="false")
    b.set(5, 1, 13, "barrel", facing="up", open="false")
    b.set(6, 1, 13, "water_cauldron", level="3")
    for x in (8, 9, 10):
        b.set(x, 1, 13, "spruce_planks")
    b.set(8, 2, 13, "potted_fern")
    b.set(9, 2, 13, "barrel", facing="up", open="false")
    for y in (1, 2):
        b.set(12, y, 13, "bookshelf")
        b.set(13, y, 13, "bookshelf")
    b.set(13, 1, 10, "lectern", facing="west", has_book="false", powered="false")
    # мастерская масок у восточной стены, в зале для гостей
    b.set(13, 1, 6, "tokyoghoulrpg:mask_workshop")
    b.set(13, 2, 6, "potted_wither_rose")
    # два столика со стульями у витрин и скамья у боковой стены
    for tx in (3, 11):
        b.set(tx, 0, 3, "brown_wool")
        b.set(tx, 1, 3, "spruce_fence", north="false", east="false", south="false", west="false", waterlogged="false")
        b.set(tx, 2, 3, "spruce_pressure_plate", powered="false")
        b.set(tx - 1, 1, 3, "spruce_stairs", facing="east", half="bottom", shape="straight", waterlogged="false")
        b.set(tx + 1, 1, 3, "spruce_stairs", facing="west", half="bottom", shape="straight", waterlogged="false")
    for z in (9, 10, 11):
        b.set(1, 1, z, "spruce_stairs", facing="east", half="bottom", shape="straight", waterlogged="false")
    b.set(1, 1, 1, "potted_bamboo")
    b.set(W - 2, 1, 1, "potted_bamboo")
    return b


def rc_lab():
    """
    Заброшенная лаборатория RC 17 x 9 x 15. Вход с севера (z = 0). Бетонный корпус с выбитыми окнами
    и частично обвалившейся крышей. Вдоль восточной стены три камеры за решёткой (одна взломана),
    в зале столы с колбами и котлами, по центру две колбы-резервуара с бирюзовым светом,
    у задней стены архив: книжные шкафы и сундуки с добычей. Внутри трое подопытных.
    """
    W, H, D = 17, 9, 15
    b = Build(W, H, D, 8410)
    rng = b.rng
    for x in range(W):
        for z in range(D):
            b.set(x, 0, z, rng.choice(["light_gray_concrete", "light_gray_concrete", "gray_concrete",
                                       "cracked_stone_bricks", "andesite"]))
    # стены: белый бетон, низ серый; углы — колонны
    for y in range(1, 6):
        for x in range(W):
            for z in (0, D - 1):
                b.set(x, y, z, "gray_concrete" if y == 1 else "white_concrete")
        for z in range(D):
            for x in (0, W - 1):
                b.set(x, y, z, "gray_concrete" if y == 1 else "white_concrete")
    for x, z in ((0, 0), (W - 1, 0), (0, D - 1), (W - 1, D - 1)):
        for y in range(1, 7):
            b.set(x, y, z, "polished_andesite")
    # выбитые окна: где-то стекло, где-то пусто
    for x in (3, 4, 12, 13):
        for y in (2, 3):
            b.set(x, y, 0, "white_stained_glass_pane" if rng.random() < 0.4 else "air")
    for z in (4, 5, 9, 10):
        for y in (2, 3):
            b.set(0, y, z, "white_stained_glass_pane" if rng.random() < 0.4 else "air")
    # вход: двойная дверь выбита
    for x in (7, 8, 9):
        for y in (1, 2, 3):
            b.set(x, y, 0, "air")
    b.set(7, 4, 0, "iron_bars"); b.set(9, 4, 0, "iron_bars")
    # крыша: бетон с провалами
    for x in range(W):
        for z in range(D):
            if rng.random() < 0.22 and 2 < x < W - 3 and 2 < z < D - 3:
                continue
            b.set(x, 6, z, "light_gray_concrete")
    # камеры у восточной стены: решётки, внутри кровать-нары
    for cz in (2, 6, 10):
        for z in range(cz, cz + 3):
            b.set(12, 1, z, "iron_bars"); b.set(12, 2, z, "iron_bars"); b.set(12, 3, z, "iron_bars")
        for x in range(12, W - 1):
            for y in (1, 2, 3):
                b.set(x, y, cz + 3, "white_concrete")
        b.set(15, 1, cz + 1, "smooth_stone_slab", type="bottom", waterlogged="false")
        b.set(15, 1, cz, "chain", axis="y", waterlogged="false")
    for y in (1, 2):                                    # взломанная камера
        b.set(12, y, 7, "air")
    b.set(13, 1, 7, "iron_bars")
    # колбы-резервуары в центре
    for tx, tz in ((5, 6), (8, 6)):
        b.set(tx, 1, tz, "polished_andesite")
        b.set(tx, 2, tz, "cyan_stained_glass")
        b.set(tx, 3, tz, "sea_lantern" if tx == 5 else "cyan_stained_glass")
        b.set(tx, 4, tz, "polished_andesite")
    b.set(8, 2, 6, "air"); b.set(8, 1, 6, "cyan_carpet")    # разбитая колба
    # столы с колбами и котлами
    for x in range(2, 6):
        b.set(x, 1, 10, "white_concrete")
    b.set(2, 2, 10, "brewing_stand", has_bottle_0="true", has_bottle_1="false", has_bottle_2="true")
    b.set(4, 2, 10, "potted_wither_rose")
    b.set(5, 1, 3, "cauldron"); b.set(6, 1, 3, "water_cauldron", level="1")
    b.set(2, 1, 3, "lectern", facing="east", has_book="false", powered="false")
    # архив у задней стены
    for x in (2, 3, 4, 5):
        for y in (1, 2):
            b.set(x, y, D - 2, "bookshelf")
    b.chest(6, 1, D - 2, "north", "rc_lab")
    b.chest(10, 1, D - 2, "north", "rc_lab")
    # кровь, бирюзовые пятна RC, паутина
    for _ in range(10):
        x, z = rng.randint(1, 11), rng.randint(1, D - 3)
        if b.get(x, 1, z) == "minecraft:air":
            b.set(x, 1, z, rng.choice(["red_carpet", "cyan_carpet", "red_carpet"]))
    for _ in range(8):
        x, y, z = rng.randint(1, 11), rng.randint(3, 5), rng.randint(1, D - 2)
        if b.get(x, y, z) == "minecraft:air":
            b.set(x, y, z, "cobweb")
    # свет: редкие лампы
    for x, z in ((3, 7), (10, 4), (9, 11)):
        b.set(x, 5, z, "sea_lantern")
    # свободное место вокруг обитателей
    for mx, mz in ((7, 9), (10, 3), (13, 7)):
        for x in range(mx - 1, mx + 2):
            for z in range(mz - 1, mz + 2):
                if 0 < x < W - 1 and 0 < z < D - 1:
                    for y in range(1, 4):
                        if b.get(x, y, z) in ("minecraft:cobweb", "minecraft:red_carpet", "minecraft:cyan_carpet"):
                            b.set(x, y, z, "air")
    b.mob(7, 1, 9, "test_subject", 0)
    b.mob(10, 1, 3, "test_subject", 1)
    b.mob(13, 1, 7, "test_subject", 0)
    return b


def black_market():
    """
    Чёрный рынок 13 x 7 x 11. Открытый с севера навес на тёмных колоннах. Слева лавка Скупщика
    за прилавком, справа прилавок торговца, у задней стены мастерская масок и бочки с товаром.
    Над прилавками — полосатые навесы, свет — фонари.
    """
    W, H, D = 13, 7, 11
    b = Build(W, H, D, 9520)
    rng = b.rng
    for x in range(W):
        for z in range(D):
            b.set(x, 0, z, rng.choice(["cobblestone", "stone_bricks", "cracked_stone_bricks", "mossy_cobblestone",
                                       "stone_bricks"]))
    # задняя и боковые стены из чёрного камня, перед открыт
    for y in range(1, 5):
        for x in range(W):
            b.set(x, y, D - 1, "polished_blackstone_bricks")
        for z in range(D):
            b.set(0, y, z, "polished_blackstone_bricks")
            b.set(W - 1, y, z, "polished_blackstone_bricks")
    for x in (0, 4, 8, W - 1):
        for y in range(1, 5):
            b.set(x, y, 0, "polished_blackstone")
    # крыша
    for x in range(W):
        for z in range(D):
            b.set(x, 5, z, "dark_oak_planks")
    for x in range(W):
        b.set(x, 6, 0, "dark_oak_slab", type="bottom", waterlogged="false")
    # лавка Скупщика: прилавок слева
    for z in range(3, 8):
        b.set(3, 1, z, "spruce_planks")
    b.set(3, 2, 4, "lantern", hanging="false", waterlogged="false")
    b.set(3, 2, 6, "chest", facing="east", type="single", waterlogged="false")
    for z in range(2, 9):
        b.set(1, 4, z, "red_wool" if z % 2 == 0 else "black_wool")
        b.set(2, 4, z, "red_wool" if z % 2 == 0 else "black_wool")
    b.set(1, 1, 8, "barrel", facing="up", open="false")
    b.set(1, 1, 3, "barrel", facing="up", open="false")
    b.mob(2, 1, 5, "fence", 0)
    # прилавок торговца справа
    for z in range(3, 8):
        b.set(9, 1, z, "spruce_planks")
    b.set(9, 2, 3, "potted_dead_bush")
    b.set(9, 2, 7, "lantern", hanging="false", waterlogged="false")
    for z in range(2, 9):
        b.set(10, 4, z, "white_wool" if z % 2 == 0 else "black_wool")
        b.set(11, 4, z, "white_wool" if z % 2 == 0 else "black_wool")
    b.set(11, 1, 8, "barrel", facing="up", open="false")
    b.mob(10, 1, 5, "trader", 1)
    # задняя стена: мастерская масок, бочки, сундук с добычей
    b.set(6, 1, D - 2, "tokyoghoulrpg:mask_workshop")
    b.set(5, 1, D - 2, "barrel", facing="up", open="false")
    b.set(7, 1, D - 2, "barrel", facing="up", open="false")
    b.set(5, 2, D - 2, "barrel", facing="up", open="false")
    b.chest(7, 2, D - 2, "north", "black_market")
    for x, z in ((6, 3), (6, 7), (2, 1), (10, 1)):
        b.set(x, 4, z, "lantern", hanging="true", waterlogged="false")
    for x in (5, 6, 7):
        b.set(x, 1, 1, "brown_carpet")
    return b


def subway():
    """
    Заброшенное метро (катакомбы) 29 x 19 x 31. На поверхности — павильон входа (y 13-18),
    от него лестница в тоннеле уходит на 13 блоков вниз. Внизу лабиринт 7 x 6 клеток (коридоры 3 x 4)
    с рельсами, ловушками (нажимные плиты + раздатчики со стрелами, магма), паутиной и гулями.
    В дальнем углу — зал 11 x 12, где ждёт мини-босс «Хозяин тоннелей», и сундуки с добычей.
    Незаданные клетки не трогаются: вокруг остаётся камень мира.
    """
    W, H, D = 29, 19, 31
    b = Build(W, H, D, 5560)
    b.void_unset = True
    rng = b.rng
    wall = ["stone_bricks", "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "deepslate_tiles"]
    floor = ["polished_deepslate", "polished_deepslate", "cracked_deepslate_tiles", "gravel"]
    # оболочка лабиринта: z 5..30, y 0..5 — сплошная, потом вырезаем
    for x in range(W):
        for z in range(5, D):
            b.set(x, 0, z, rng.choice(floor))
            for y in range(1, 5):
                b.set(x, y, z, rng.choice(wall))
            b.set(x, 5, z, "deepslate_bricks")
    CX, CZ = 7, 6

    def cell(i, j):
        return 1 + 4 * i, 6 + 4 * j      # левый-верхний угол открытой части 3 x 3

    boss = {(i, j) for i in (4, 5, 6) for j in (3, 4, 5)}

    def carve(x0, z0, x1, z1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for y in range(1, 5):
                    b.set(x, y, z, "air")

    for i in range(CX):
        for j in range(CZ):
            x, z = cell(i, j)
            carve(x, z, x + 2, z + 2)
    # лабиринт: обход в глубину по клеткам вне зала
    seen = {(0, 0)}
    stack = [(0, 0)]
    links = set()
    while stack:
        i, j = stack[-1]
        nxt = [(i + di, j + dj) for di, dj in ((1, 0), (-1, 0), (0, 1), (0, -1))
               if 0 <= i + di < CX and 0 <= j + dj < CZ and (i + di, j + dj) not in seen and (i + di, j + dj) not in boss]
        if not nxt:
            stack.pop()
            continue
        n = rng.choice(nxt)
        seen.add(n)
        links.add(frozenset(((i, j), n)))
        stack.append(n)
    # пара лишних проходов, чтобы были петли
    for _ in range(4):
        i, j = rng.randrange(CX - 1), rng.randrange(CZ)
        if (i, j) not in boss and (i + 1, j) not in boss:
            links.add(frozenset(((i, j), (i + 1, j))))

    def open_between(a, c):
        (i1, j1), (i2, j2) = sorted((a, c))
        x, z = cell(i1, j1)
        if i2 > i1:
            carve(x + 3, z, x + 3, z + 2)
        else:
            carve(x, z + 3, x + 2, z + 3)

    for link in links:
        open_between(*tuple(link))
    # зал босса: всё внутри x 17..27, z 18..29
    carve(17, 18, 27, 29)
    for x in range(17, 28):
        for z in range(18, 30):
            b.set(x, 0, z, "polished_blackstone_bricks" if (x + z) % 2 else "polished_deepslate")
    open_between((3, 4), (4, 4))                 # единственная дверь в зал (с запада)
    b.set(16, 4, 22, "chain", axis="y", waterlogged="false")
    for px, pz in ((19, 20), (25, 20), (19, 27), (25, 27)):
        for y in range(1, 5):
            b.set(px, y, pz, "polished_basalt", axis="y")
    for x, z in ((22, 19), (22, 28)):
        b.set(x, 4, z, "lantern", hanging="true", waterlogged="false")
    b.chest(26, 1, 29, "west", "subway")
    b.chest(18, 1, 29, "east", "subway")
    for _ in range(8):
        x, z = rng.randint(17, 27), rng.randint(18, 29)
        if b.get(x, 1, z) == "minecraft:air" and (x, z) not in ((22, 24),):
            b.set(x, 1, z, "red_carpet")
    # рельсы вдоль коридоров и фонари
    for i in range(CX):
        for j in range(CZ):
            if (i, j) in boss:
                continue
            x, z = cell(i, j)
            r = rng.random()
            if r < 0.35:
                b.set(x + 1, 1, z + 1, "rail", shape=rng.choice(["north_south", "east_west"]), waterlogged="false")
            elif r < 0.5:
                b.set(x + rng.randint(0, 2), 3 + rng.randint(0, 1), z + rng.randint(0, 2), "cobweb")
            elif r < 0.6:
                b.set(x + 1, 0, z + 1, "magma_block")                # горячий пол
            if rng.random() < 0.3:
                b.set(x + 1, 4, z + 1, "lantern", hanging="true", waterlogged="false")
    # ловушки: плита у закрытой восточной стены, в стене — раздатчик со стрелами
    arrows = {"id": Tag(STRING, "minecraft:dispenser"),
              "Items": Tag(LIST, [Tag(COMPOUND, {"Slot": Tag(BYTE, 0), "id": Tag(STRING, "minecraft:arrow"),
                                                 "Count": Tag(BYTE, 16)})], item_type=COMPOUND)}
    traps = 0
    cells = [(i, j) for i in range(CX - 1) for j in range(CZ) if (i, j) not in boss and (i, j) != (0, 0)]
    rng.shuffle(cells)
    for i, j in cells:
        if traps >= 4:
            break
        if frozenset(((i, j), (i + 1, j))) in links or (i + 1, j) in boss:
            continue
        x, z = cell(i, j)
        b.set(x + 3, 1, z + 1, "dispenser", facing="west", triggered="false")
        b.block_nbt[(x + 3, 1, z + 1)] = dict(arrows)
        b.set(x + 2, 1, z + 1, "stone_pressure_plate", powered="false")
        traps += 1
    # сундук в тупике
    dead = [c for c in seen if c != (0, 0) and sum(1 for l in links if c in l) == 1]
    if dead:
        i, j = rng.choice(sorted(dead))
        x, z = cell(i, j)
        b.chest(x + 1, 1, z + 1, "north", "subway")
    # гули в коридорах
    spots = [c for c in sorted(seen) if c != (0, 0)]
    rng.shuffle(spots)
    for k, (i, j) in enumerate(spots[:5]):
        x, z = cell(i, j)
        if b.get(x + 1, 1, z + 1) in ("minecraft:air", "minecraft:rail"):
            b.set(x + 1, 1, z + 1, "air")
            b.mob(x + 1, 1, z + 1, "ghoul", k % 3)
    # мини-босс
    name = '{"text":"Хозяин тоннелей","color":"dark_red","bold":true}'
    b.mob(22, 1, 24, "ghoul", 2, extra={
        "CustomName": Tag(STRING, name),
        "CustomNameVisible": Tag(BYTE, 1),
        "Health": Tag(FLOAT, 160.0),
        "Attributes": Tag(LIST, [
            Tag(COMPOUND, {"Name": Tag(STRING, "minecraft:generic.max_health"), "Base": Tag(DOUBLE, 160.0)}),
            Tag(COMPOUND, {"Name": Tag(STRING, "minecraft:generic.attack_damage"), "Base": Tag(DOUBLE, 12.0)}),
            Tag(COMPOUND, {"Name": Tag(STRING, "minecraft:generic.armor"), "Base": Tag(DOUBLE, 10.0)}),
            Tag(COMPOUND, {"Name": Tag(STRING, "minecraft:generic.knockback_resistance"), "Base": Tag(DOUBLE, 0.6)}),
        ], item_type=COMPOUND),
        "Tags": Tag(LIST, [Tag(STRING, "tgrpg_subway_boss")], item_type=STRING),
    })
    # спуск: тоннель z 1..3 от x=1 (низ, y 0) до x=15 (верх, пол y 12), выход на поверхность x 16..18
    def floor_y(x):
        return 0 if x <= 3 else min(12, x - 3)
    for x in range(0, 19):
        f = floor_y(x) if x <= 15 else 12
        for z in range(0, 5):
            for y in range(max(0, f - 1), min(H, f + 6)):
                edge = z in (0, 4) or x == 0
                if y <= f or y == f + 5 or edge:
                    if y >= 13 and (z in (0, 4) or y == f + 5) and x >= 15:
                        continue                         # над землёй стен тоннеля нет
                    b.set(x, y, z, rng.choice(wall))
                else:
                    b.set(x, y, z, "air")
        if 4 <= x <= 15:
            for z in (1, 2, 3):
                b.set(x, f, z, "stone_brick_stairs", facing="east", half="bottom", shape="straight", waterlogged="false")
    for x in range(1, 4):                         # проход из тоннеля в лабиринт
        for z in (4, 5):
            for y in range(1, 5):
                b.set(x, y, z, "air")
    for x, y in ((6, 6), (11, 11)):
        b.set(x, y, 2, "lantern", hanging="true", waterlogged="false")
    # павильон входа на поверхности: x 14..20, z 0..4, пол y 12, стены y 13..16, крыша y 17
    for x in range(14, 21):
        for z in range(0, 5):
            if x >= 16 or z in (0, 4):
                b.set(x, 12, z, "smooth_stone")
            for y in range(13, 17):
                if x == 14 and z in (1, 2, 3):
                    b.set(x, y, z, "air" if y < 16 else "white_concrete")   # тоннель входит в павильон
                elif z in (0, 4) or x in (14, 20):
                    if x == 20 and z in (1, 2, 3) and y <= 15:
                        b.set(x, y, z, "air")            # вход с востока
                    elif y in (14, 15) and x not in (14, 20) and z in (0, 4):
                        b.set(x, y, z, "glass_pane", east="true", west="true", north="false", south="false",
                              waterlogged="false")
                    else:
                        b.set(x, y, z, "red_concrete" if y == 16 else "white_concrete")
                elif x >= 15:
                    b.set(x, y, z, "air")
            b.set(x, 17, z, "iron_block" if (x + z) % 2 == 0 else "light_gray_concrete")
    b.set(17, 16, 2, "lantern", hanging="true", waterlogged="false")
    return b


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    only = set(sys.argv[2:])
    for name, build in (("ccg_office", ccg_office), ("ghoul_hideout", ghoul_hideout), ("half_ghoul_arena", half_ghoul_arena), ("cafe", cafe), ("rc_lab", rc_lab), ("black_market", black_market), ("subway", subway)):
        if only and name not in only:
            continue
        info = build().save(os.path.join(out, name + ".nbt"))
        print(name, "palette/blocks/entities:", info)
