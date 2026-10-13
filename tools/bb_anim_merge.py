"""
Добавляет анимации из файлов Blockbench (.bbmodel) в animations/kaneki_crimson_kagune.animation.json.
Перевод как при экспорте Blockbench в формат Bedrock/GeckoLib: поворот (-x, -y, z), сдвиг (-x, y, z),
размер как есть; ключевые кадры по времени. Режим повтора: loop -> true, hold -> hold_on_last_frame, once -> нет.
Запуск: python3 bb_anim_merge.py <animation.json> [--front] <файл.bbmodel> [...]

--front: отразить движение «спина <-> перед» (плоскость тела, ось Z). Нужно, если анимация в Blockbench
сделана так, что щупальца уходят назад, а в игре они должны быть перед игроком (так сделан блок).
Отражение: у поворота меняют знак X и Y, у сдвига — Z (в координатах Blockbench).
"""
import json
import sys


def num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return 0.0


def convert(an, front=False):
    bones = {}
    for animator in an.get("animators", {}).values():
        name = animator.get("name")
        if not name or animator.get("type", "bone") != "bone":
            continue
        channels = {}
        for kf in sorted(animator.get("keyframes", []), key=lambda k: k["time"]):
            ch = kf["channel"]
            if ch not in ("rotation", "position", "scale"):
                continue
            dp = kf["data_points"][0]
            x, y, z = num(dp.get("x")), num(dp.get("y")), num(dp.get("z"))
            if front:
                if ch == "rotation":
                    x, y = -x, -y
                elif ch == "position":
                    z = -z
            if ch == "rotation":
                x, y = -x, -y
            elif ch == "position":
                x = -x
            vec = [round(x, 5) + 0.0, round(y, 5) + 0.0, round(z, 5) + 0.0]
            key = repr(float(round(kf["time"], 4)))
            channels.setdefault(ch, {})[key] = {"vector": vec}
        if channels:
            bones[name] = channels
    out = {}
    loop = an.get("loop")
    if loop == "loop":
        out["loop"] = True
    elif loop == "hold":
        out["loop"] = "hold_on_last_frame"
    out["animation_length"] = an.get("length")
    out["bones"] = bones
    return out


if __name__ == "__main__":
    target = sys.argv[1]
    data = json.load(open(target))
    front = "--front" in sys.argv[2:]
    for path in [a for a in sys.argv[2:] if a != "--front"]:
        for an in json.load(open(path)).get("animations", []):
            data["animations"][an["name"]] = convert(an, front)
            print("добавлена", an["name"], an.get("length"), an.get("loop"))
    json.dump(data, open(target, "w"), indent="\t")
