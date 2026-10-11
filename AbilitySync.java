package com.tokyoghoulrpg.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/** Тайминги способностей из ability_sync.json: моменты ударов и фазы захватов, переведённые в тики. */
public final class AbilitySync {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE = "/data/tokyoghoulrpg/ability_sync.json";
    private static final Timing FALLBACK = new Timing(20, new int[] {6}, 0, 0, 0, 0);
    private static Map<String, Timing> timings;

    /** Все значения в тиках. Если в файле поля нет, там 0. */
    public record Timing(int duration, int[] hits, int latch, int pullStart, int pullEnd, int release) {
    }

    private AbilitySync() {
    }

    public static synchronized Timing get(String animation) {
        if (timings == null) {
            timings = load();
        }
        return timings.getOrDefault(animation, FALLBACK);
    }

    private static Map<String, Timing> load() {
        Map<String, Timing> map = new HashMap<>();
        try (InputStream in = AbilitySync.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOGGER.warn("ability_sync.json не найден, используются запасные тайминги");
                return map;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    map.put(entry.getKey(), parse(entry.getValue().getAsJsonObject()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось прочитать ability_sync.json", e);
        }
        return map;
    }

    private static Timing parse(JsonObject o) {
        return new Timing(ticks(o, "duration"), hits(o), ticks(o, "latch"),
                ticks(o, "pull_start"), ticks(o, "pull_end"), ticks(o, "release"));
    }

    private static int ticks(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? (int) Math.round(e.getAsDouble() * 20.0) : 0;
    }

    private static int[] hits(JsonObject o) {
        if (o.has("hits") && o.get("hits").isJsonArray()) {
            JsonArray array = o.getAsJsonArray("hits");
            int[] result = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                JsonElement e = array.get(i);
                double seconds = e.isJsonObject() ? e.getAsJsonObject().get("t").getAsDouble() : e.getAsDouble();
                result[i] = (int) Math.round(seconds * 20.0);
            }
            Arrays.sort(result);
            return result;
        }
        if (o.has("hit")) {
            return new int[] {(int) Math.round(o.get("hit").getAsDouble() * 20.0)};
        }
        return new int[0];
    }
}
