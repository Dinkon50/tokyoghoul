"""
Добыча данжа «Глубины 24-го района»: сундуки трёх уровней, добыча с монстров, стражей и Древнего гуля,
и модификаторы, которые добавляют в сундуки данжа добычу из сундуков других установленных модов
(deep/ForeignLootModifier).
Запуск: python3 deep_loot.py <src/main/resources>
"""
import json
import os
import sys

M = "tokyoghoulrpg:"


def item(name, weight, lo=1, hi=1, extra=None):
    e = {"type": "minecraft:item", "name": name if ":" in name else "minecraft:" + name, "weight": weight}
    fns = []
    if (lo, hi) != (1, 1):
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if extra:
        fns.extend(extra)
    if fns:
        e["functions"] = fns
    return e


def mod(name, weight, lo=1, hi=1, extra=None):
    return item(M + name, weight, lo, hi, extra)


def enchanted(name, weight, levels, treasure=False):
    return item(name, weight, extra=[{"function": "minecraft:enchant_with_levels", "levels": levels,
                                      "treasure": treasure}])


def book(weight):
    return item("book", weight, extra=[{"function": "minecraft:enchant_randomly"}])


def table_ref(name, weight):
    return {"type": "minecraft:loot_table", "name": name, "weight": weight}


def pool(lo, hi, entries):
    return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "bonus_rolls": 0.0, "entries": entries}


def chest(*pools):
    return {"type": "minecraft:chest", "pools": list(pools)}


COMMON = chest(
    pool(5, 8, [
        item("iron_ingot", 14, 3, 8), item("gold_ingot", 10, 2, 6), item("diamond", 4, 1, 2), item("emerald", 6, 2, 5),
        item("bone", 10, 2, 6), item("arrow", 8, 6, 16), item("ender_pearl", 4, 1, 2), item("experience_bottle", 6, 2, 6),
        item("golden_apple", 3), item("cooked_beef", 6, 2, 5), item("redstone", 6, 4, 10), book(5),
        enchanted("iron_sword", 3, 20), enchanted("bow", 2, 20),
        mod("flesh", 12, 2, 5), mod("coffee", 6, 1, 3), mod("medkit", 6, 1, 2), mod("smoke_bomb", 5, 1, 3),
        mod("rc_core_1", 6, 1, 2), mod("raw_rc_steel", 6, 2, 5), mod("rc_steel_ingot", 4, 1, 3), mod("lab_notes", 3),
        mod("mask_bone", 1), mod("mask_crow", 1), mod("ghoul_coat", 1), mod("ghoul_boots", 1),
    ]),
    pool(1, 1, [table_ref("minecraft:chests/simple_dungeon", 3), table_ref("minecraft:chests/abandoned_mineshaft", 3),
                {"type": "minecraft:empty", "weight": 4}]),
)

VAULT = chest(
    pool(7, 10, [
        item("diamond", 10, 2, 5), item("emerald", 8, 4, 10), item("gold_block", 4, 1, 2), item("iron_block", 5, 1, 3),
        item("netherite_scrap", 4, 1, 2), item("golden_apple", 6, 1, 3), item("enchanted_golden_apple", 1),
        item("totem_of_undying", 1), item("experience_bottle", 6, 4, 10), book(6), item("ender_pearl", 4, 2, 4),
        enchanted("diamond_sword", 3, 30), enchanted("diamond_chestplate", 2, 30), enchanted("diamond_pickaxe", 2, 30),
        mod("rc_core_2", 6, 1, 2), mod("rc_core_1", 6, 1, 3), mod("rc_steel_ingot", 8, 3, 6), mod("lab_notes", 6, 1, 2),
        mod("medkit", 5, 2, 3), mod("quinque_kura", 2), mod("ccg_elite_helmet", 1), mod("ccg_elite_chestplate", 1),
        mod("ccg_elite_leggings", 1), mod("ccg_elite_boots", 1), mod("mask_fang", 2), mod("mask_oni", 2),
        mod("skill_reset_seal", 1), mod("rare_kagune", 1),
    ]),
    pool(1, 2, [table_ref("minecraft:chests/bastion_treasure", 3), table_ref("minecraft:chests/ancient_city", 3),
                table_ref("minecraft:chests/stronghold_corridor", 3), table_ref("minecraft:chests/end_city_treasure", 1)]),
)

BOSS = chest(
    pool(7, 9, [
        item("netherite_ingot", 6, 1, 2), item("diamond", 10, 4, 8), item("enchanted_golden_apple", 4),
        item("totem_of_undying", 5), item("emerald_block", 4, 1, 3), item("experience_bottle", 6, 8, 16),
        enchanted("netherite_sword", 3, 39, True), enchanted("netherite_chestplate", 2, 39, True),
        enchanted("netherite_helmet", 2, 39, True), enchanted("bow", 2, 39, True), book(5),
        mod("rc_core_3", 3), mod("rc_core_2", 8, 1, 2), mod("rare_kagune", 6), mod("skill_reset_seal", 6),
        mod("lab_notes", 6, 2, 4), mod("quinque_narukami", 1), mod("quinque_ixa", 1), mod("quinque_jason", 1),
        mod("crimson_crown", 1), mod("rc_steel_ingot", 6, 4, 8),
    ]),
    pool(2, 2, [table_ref("minecraft:chests/ancient_city", 3), table_ref("minecraft:chests/bastion_treasure", 3),
                table_ref("minecraft:chests/end_city_treasure", 2)]),
)


def entity(*pools):
    return {"type": "minecraft:entity", "pools": list(pools)}


def drop(name, lo, hi, looting=True, chance=None):
    e = {"type": "minecraft:item", "name": name if ":" in name else "minecraft:" + name, "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    if looting:
        e["functions"].append({"function": "minecraft:looting_enchant",
                               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    p = {"rolls": 1, "entries": [e]}
    if chance is not None:
        p["conditions"] = [{"condition": "minecraft:killed_by_player"},
                           {"condition": "minecraft:random_chance_with_looting", "chance": chance,
                            "looting_multiplier": chance / 2}]
    return p


ENTITIES = {
    "deep_ghoul": entity(drop(M + "flesh", 0, 2), drop("bone", 0, 2), drop(M + "rc_core_1", 1, 1, False, 0.03)),
    "deep_spitter": entity(drop(M + "flesh", 0, 2), drop("amethyst_shard", 0, 3), drop(M + "rc_core_1", 1, 1, False, 0.05)),
    "deep_brute": entity(drop(M + "flesh", 1, 3), drop("bone", 1, 4), drop(M + "rc_core_1", 1, 1, False, 0.12),
                         drop("diamond", 1, 1, False, 0.08)),
    "deep_guardian_sting": entity(drop(M + "rc_core_2", 1, 2, False), drop("diamond", 3, 6), drop("netherite_scrap", 1, 3),
                                  drop("totem_of_undying", 1, 1, False, 0.5), drop(M + "rc_steel_ingot", 3, 6)),
    "deep_guardian_wing": entity(drop(M + "rc_core_2", 1, 2, False), drop("diamond", 3, 6), drop("netherite_scrap", 1, 3),
                                 drop("totem_of_undying", 1, 1, False, 0.5), drop(M + "rc_steel_ingot", 3, 6)),
    "ancient_ghoul": entity(drop(M + "rare_kagune", 1, 1, False), drop(M + "rc_core_3", 1, 2, False),
                            drop("netherite_ingot", 2, 4), drop("enchanted_golden_apple", 1, 2, False),
                            drop(M + "skill_reset_seal", 1, 1, False), drop("nether_star", 1, 1, False, 0.35)),
}

# сколько раз и с каким шансом добавить добычу из сундука другого мода (если модов нет — ничего)
# и щедрость (luck): во сколько раз чаще редкие и эпические вещи модов
FOREIGN = {"deep_common": (1, 0.35, 1.0), "deep_vault": (2, 0.85, 2.0), "deep_boss": (3, 1.0, 4.0)}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main(res):
    d = os.path.join(res, "data", "tokyoghoulrpg")
    write(os.path.join(d, "loot_tables", "chests", "deep_common.json"), COMMON)
    write(os.path.join(d, "loot_tables", "chests", "deep_vault.json"), VAULT)
    write(os.path.join(d, "loot_tables", "chests", "deep_boss.json"), BOSS)
    for name, data in ENTITIES.items():
        write(os.path.join(d, "loot_tables", "entities", name + ".json"), data)
    entries = []
    for table, (rolls, chance, luck) in FOREIGN.items():
        name = "foreign_" + table
        entries.append("tokyoghoulrpg:" + name)
        write(os.path.join(d, "loot_modifiers", name + ".json"), {
            "type": "tokyoghoulrpg:foreign_loot",
            "conditions": [{"condition": "forge:loot_table_id", "loot_table_id": "tokyoghoulrpg:chests/" + table}],
            "rolls": rolls, "chance": chance, "luck": luck})
    # все остальные сундуки мода (кофейня, склады, лаборатория, метро, арена и будущие постройки)
    entries.append("tokyoghoulrpg:foreign_mod_chests")
    write(os.path.join(d, "loot_modifiers", "foreign_mod_chests.json"), {
        "type": "tokyoghoulrpg:foreign_loot", "conditions": [], "rolls": 1, "chance": 0.6, "all_mod_chests": True, "luck": 1.0})
    write(os.path.join(res, "data", "forge", "loot_modifiers", "global_loot_modifiers.json"),
          {"replace": False, "entries": entries})
    print("ok")


if __name__ == "__main__":
    main(sys.argv[1])
