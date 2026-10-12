"""
Постройки мода в формате .nbt (как сохраняет структурный блок Minecraft 1.20.1, DataVersion 3465).
  ccg_office    — двухэтажный офис CCG: белые стены, кварцевые колонны, окна, кабинет босса на 2-м этаже.
  ghoul_hideout — разрушенное логово гулей: глубинносланцевые руины, паутина, кости, кровь на полу.
Запуск: python3 structures.py <папка data/tokyoghoulrpg/structures>
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

    def mob(self, x, y, z, entity, variant=0, hand=None, armor=(None, None, None, None), drop_head=0.085):
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


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, build in (("ccg_office", ccg_office), ("ghoul_hideout", ghoul_hideout)):
        info = build().save(os.path.join(out, name + ".nbt"))
        print(name, "palette/blocks/entities:", info)
