"""
Достижения мода (data/tokyoghoulrpg/advancements). Запуск: python3 advancements.py <папка src/main/resources>
Заодно дописывает названия и описания в оба языковых файла.
Достижения с критерием «code» выдаёт сам мод (com.tokyoghoulrpg.util.Achievements.grant).
"""
import collections
import json
import os
import sys

NS = "tokyoghoulrpg"


def kill(entity):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": f"{NS}:{entity}"}}]}}


def visit(structure):
    return {"trigger": "minecraft:location", "conditions": {"player": [
        {"condition": "minecraft:entity_properties", "entity": "this",
         "predicate": {"location": {"structure": f"{NS}:{structure}"}}}]}}


CODE = {"trigger": "minecraft:impossible"}

# id, родитель, значок, рамка (task/goal/challenge), опыт, критерии, название, описание
ADV = [
    ("root", None, f"{NS}:mask_oni", "task", 0, {"tick": {"trigger": "minecraft:tick"}},
     "Tokyo Ghoul RPG", "Мир, где люди и гули делят один город"),
    ("choose_class", "root", f"{NS}:ghoul_injection", "task", 10, {"code": CODE},
     "Кем ты станешь?", "Выбери класс: гуль, полугуль, человек или агент CCG"),
    ("prestige", "choose_class", f"{NS}:rc_core_3", "goal", 100, {"code": CODE},
     "Начать заново, но сильнее", "Получи престиж"),
    # боссы мира
    ("kill_ghoul_boss", "root", f"{NS}:flesh", "task", 50, {"kill": kill("ghoul_boss")},
     "Хищник хищников", "Победи главаря гулей"),
    ("kill_ccg_boss", "root", f"{NS}:quinque_kura", "task", 50, {"kill": kill("ccg_boss")},
     "Следователю не повезло", "Победи старшего следователя CCG"),
    ("kill_half_ghoul_boss", "kill_ghoul_boss", f"{NS}:rare_kagune", "goal", 100, {"kill": kill("half_ghoul_boss")},
     "Кровь на асфальте", "Победи Кровавого полугуля"),
    ("kill_crimson_monarch", "kill_half_ghoul_boss", f"{NS}:crimson_crown", "challenge", 300,
     {"kill": kill("crimson_monarch")}, "Падение Монарха", "Победи Алого Монарха"),
    # стили боя
    ("find_dojo", "root", "minecraft:bamboo", "task", 10, {"visit": visit("dojo")},
     "Путь воина", "Найди додзё"),
    ("style_fighter", "find_dojo", f"{NS}:dojo_master_spawn_egg", "goal", 100, {"code": CODE},
     "Кулак и воля", "Победи Мастера додзё и открой стиль «Боец»"),
    ("style_shadow", "find_dojo", f"{NS}:clown_spawn_egg", "goal", 100, {"code": CODE},
     "Смех сквозь дым", "Победи Клоуна из Пьеро и открой стиль «Тень»"),
    ("style_reaper", "find_dojo", f"{NS}:white_reaper_spawn_egg", "goal", 100, {"code": CODE},
     "Жатва окончена", "Победи Белого жнеца и открой стиль «Жнец»"),
    ("style_all", "style_reaper", "minecraft:nether_star", "challenge", 250, {"code": CODE},
     "Три пути", "Открой все три стиля боя"),
    ("style_mastered", "style_fighter", f"{NS}:rc_core_2", "challenge", 200, {"code": CODE},
     "Совершенство стиля", "Прокачай все ветки одного стиля до конца"),
    # Глубины 24-го района
    ("enter_deep_ward", "root", "minecraft:sculk", "task", 20, {"visit": visit("deep_ward")},
     "Во тьму", "Спустись в Глубины 24-го района"),
    ("kill_deep_guardian_sting", "enter_deep_ward", f"{NS}:deep_guardian_sting_spawn_egg", "goal", 150,
     {"kill": kill("deep_guardian_sting")}, "Жало сломано", "Победи Стража глубин: Жало"),
    ("kill_deep_guardian_wing", "enter_deep_ward", f"{NS}:deep_guardian_wing_spawn_egg", "goal", 150,
     {"kill": kill("deep_guardian_wing")}, "Крылья подрезаны", "Победи Стража глубин: Крыло"),
    ("kill_ancient_ghoul", "kill_deep_guardian_sting", f"{NS}:ancient_ghoul_spawn_egg", "challenge", 500,
     {"kill": kill("ancient_ghoul")}, "Древнее зло", "Победи Древнего гуля в сердце Глубин"),
]


def main(res):
    folder = os.path.join(res, "data", NS, "advancements")
    os.makedirs(folder, exist_ok=True)
    lang = collections.OrderedDict()
    for aid, parent, icon, frame, xp, criteria, title, desc in ADV:
        display = {
            "icon": {"item": icon},
            "title": {"translate": f"advancements.{NS}.{aid}.title"},
            "description": {"translate": f"advancements.{NS}.{aid}.description"},
            "frame": frame,
            "show_toast": aid != "root",
            "announce_to_chat": aid != "root",
            "hidden": False,
        }
        if parent is None:
            display["background"] = "minecraft:textures/block/red_nether_bricks.png"
        data = {"display": display, "criteria": dict(criteria)}
        if "kill" in criteria:
            # убил сам — или был среди тех, кто бил (мод засчитывает участникам рейдовых боссов)
            data["criteria"]["code"] = CODE
            data["requirements"] = [["kill", "code"]]
        if parent:
            data["parent"] = f"{NS}:{parent}"
        if xp:
            data["rewards"] = {"experience": xp}
        with open(os.path.join(folder, aid + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
        lang[f"advancements.{NS}.{aid}.title"] = title
        lang[f"advancements.{NS}.{aid}.description"] = desc
    for name in ("ru_ru.json", "en_us.json"):
        path = os.path.join(res, "assets", NS, "lang", name)
        data = json.load(open(path, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
        data.update(lang)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print(len(ADV), "достижений")


if __name__ == "__main__":
    main(sys.argv[1])
