package com.tokyoghoulrpg.client.quinque;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.kagune.KaguneAnimatable;
import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.QuinqueItem;
import com.tokyoghoulrpg.item.QuinqueType;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Анимации квинке: удары серии, сильный удар (у Narukami — разряд), выпад, шквал, «Освобождение» и стойка блока.
 * Видны от первого и от третьего лица, и другим игрокам тоже: сервер рассылает, какой приём начался
 * (тем же пакетом, что и анимации кагуне, имя начинается с quinque_).
 * <p>
 * Сами движения лежат в assets/tokyoghoulrpg/quinque_animations.json (ключевые кадры по тикам), здесь только
 * их проигрывание. Три манеры: клинок (Kura, Narukami, учебная), коса 13's Jason и копьё IXA (две руки).
 * <ul>
 *   <li>arm — рука с квинке: поворот вбок (+ наружу), подъём (0 — опущена, -90 — вперёд, -180 — вверх), скрутка;</li>
 *   <li>body — поворот корпуса (+ в сторону руки с квинке); legs — шаг ног (главная сторона, вторая);</li>
 *   <li>wrist — поворот квинке в кисти (x, y — вокруг древка, z — наклон острия);</li>
 *   <li>fp_pos, fp_rot — сдвиг и поворот квинке от первого лица;</li>
 *   <li>grip — у оружия для двух рук вторая рука сама находит древко (участок древка в блоках).</li>
 * </ul>
 * Между кадрами — плавная кривая без перелётов (монотонный кубический сплайн), приёмы плавно перетекают
 * друг в друга. Предпросмотр всех приёмов делался на точной копии модели игрока, той же математикой, что здесь.
 */
public final class QuinqueAnim {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation FILE = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "quinque_animations.json");
    private static final float DEG = (float) Math.PI / 180.0F;

    private QuinqueAnim() {
    }

    /** Своя поза руки с квинке от третьего лица (Forge позволяет добавить её к обычным позам). */
    public static final HumanoidModel.ArmPose ARM_POSE =
            HumanoidModel.ArmPose.create("TOKYOGHOULRPG_QUINQUE", false, QuinqueAnim::applyThirdPerson);

    // ------------------------------------------------------------------ данные

    /** Каналы позы и их размер: arm 3, body 1, wrist 3, legs 2, fp_pos 3, fp_rot 3. */
    private static final String[] CHANNELS = {"arm", "body", "wrist", "legs", "fp_pos", "fp_rot"};
    private static final int[] DIMS = {3, 1, 3, 2, 3, 3};
    private static final int[] OFFSET = {0, 3, 4, 7, 9, 12};
    private static final int N = 15;
    private static final int ARM = 0, BODY = 3, WRIST = 4, LEGS = 7, FP_POS = 9, FP_ROT = 12;
    /** Обычная поза «держит предмет»: рука чуть вперёд. */
    private static final float[] REST = {0, -18, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static final class Move {
        float length;
        boolean loop;
        float fadeIn = 2.0F;
        float fadeOut = 3.0F;
        float[] grip;
        float anchor;
        float align;
        float[] times;
        String[] ease;
        /** Значения [канал][ключ] и наклоны кривой в ключах. */
        float[][] values;
        float[][] slopes;
    }

    private static Map<String, Move> moves;

    /** Ресурсы перезагружены (F3 + T): прочитаем файл заново. */
    public static void reload() {
        moves = null;
    }

    private static Map<String, Move> moves() {
        if (moves == null) {
            moves = load();
        }
        return moves;
    }

    private static Map<String, Move> load() {
        Map<String, Move> map = new HashMap<>();
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILE);
        if (resource.isEmpty()) {
            LOGGER.error("Нет файла анимаций квинке {}", FILE);
            return map;
        }
        try (Reader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            JsonObject styles = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("styles");
            for (Map.Entry<String, JsonElement> style : styles.entrySet()) {
                for (Map.Entry<String, JsonElement> move : style.getValue().getAsJsonObject().entrySet()) {
                    map.put(style.getKey() + ":" + move.getKey(), parse(move.getValue().getAsJsonObject()));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось прочитать анимации квинке", e);
        }
        return map;
    }

    private static float f(JsonObject o, String key, float fallback) {
        return o.has(key) ? o.get(key).getAsFloat() : fallback;
    }

    private static Move parse(JsonObject o) {
        Move m = new Move();
        m.length = f(o, "length", 1.0F);
        m.loop = o.has("loop") && o.get("loop").getAsBoolean();
        m.fadeIn = f(o, "fade_in", 2.0F);
        m.fadeOut = f(o, "fade_out", 3.0F);
        m.anchor = f(o, "anchor", 0.0F);
        m.align = f(o, "align", 0.0F);
        if (o.has("grip")) {
            JsonArray g = o.getAsJsonArray("grip");
            m.grip = new float[] {g.get(0).getAsFloat(), g.get(1).getAsFloat()};
        }
        JsonArray keys = o.getAsJsonArray("keys");
        int n = keys.size();
        m.times = new float[n];
        m.ease = new String[n];
        m.values = new float[N][n];
        float[] last = REST.clone();
        for (int i = 0; i < n; i++) {
            JsonObject k = keys.get(i).getAsJsonObject();
            m.times[i] = k.get("t").getAsFloat();
            m.ease[i] = k.has("ease") ? k.get("ease").getAsString() : "smooth";
            for (int c = 0; c < CHANNELS.length; c++) {
                JsonElement e = k.get(CHANNELS[c]);
                if (e == null) {
                    continue;
                }
                if (e.isJsonArray()) {
                    JsonArray a = e.getAsJsonArray();
                    for (int d = 0; d < DIMS[c]; d++) {
                        last[OFFSET[c] + d] = a.get(d).getAsFloat();
                    }
                } else {
                    last[OFFSET[c]] = e.getAsFloat();
                }
            }
            for (int j = 0; j < N; j++) {
                m.values[j][i] = last[j];
            }
        }
        m.slopes = new float[N][];
        for (int j = 0; j < N; j++) {
            m.slopes[j] = slopes(m.times, m.values[j], m.loop, m.length);
        }
        return m;
    }

    /** Наклоны кривой в ключах (метод Штеффена): плавно, без перелётов, на крайних точках — остановка. */
    private static float[] slopes(float[] ts, float[] ys, boolean loop, float period) {
        int n = ts.length;
        float[] m = new float[n];
        for (int i = 0; i < n; i++) {
            float t0, t1, y0, y1;
            if (i == 0) {
                if (!loop || n < 3) {
                    continue;
                }
                t0 = ts[n - 2] - period;
                y0 = ys[n - 2];
            } else {
                t0 = ts[i - 1];
                y0 = ys[i - 1];
            }
            if (i == n - 1) {
                if (!loop || n < 3) {
                    continue;
                }
                t1 = ts[1] + period;
                y1 = ys[1];
            } else {
                t1 = ts[i + 1];
                y1 = ys[i + 1];
            }
            float h0 = ts[i] - t0;
            float h1 = t1 - ts[i];
            if (h0 <= 0 || h1 <= 0) {
                continue;
            }
            float d0 = (ys[i] - y0) / h0;
            float d1 = (y1 - ys[i]) / h1;
            float p = (d0 * h1 + d1 * h0) / (h0 + h1);
            m[i] = (Math.signum(d0) + Math.signum(d1))
                    * Math.min(Math.min(Math.abs(d0), Math.abs(d1)), 0.5F * Math.abs(p));
        }
        return m;
    }

    private static float curve(Move m, int j, float t) {
        float[] ts = m.times;
        float[] ys = m.values[j];
        int n = ts.length;
        if (t <= ts[0]) {
            return ys[0];
        }
        if (t >= ts[n - 1]) {
            return ys[n - 1];
        }
        int i = 0;
        while (i < n - 2 && t > ts[i + 1]) {
            i++;
        }
        float h = ts[i + 1] - ts[i];
        float u = (t - ts[i]) / h;
        float y0 = ys[i];
        float y1 = ys[i + 1];
        switch (m.ease[i]) {
            case "linear":
                return y0 + (y1 - y0) * u;
            case "fast": {
                float f = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
                return y0 + (y1 - y0) * f;
            }
            case "hold":
                return y0;
            default: {
                float u2 = u * u;
                float u3 = u2 * u;
                return (2 * u3 - 3 * u2 + 1) * y0 + (u3 - 2 * u2 + u) * h * m.slopes[j][i]
                        + (-2 * u3 + 3 * u2) * y1 + (u3 - u2) * h * m.slopes[j][i + 1];
            }
        }
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    // ------------------------------------------------------------------ кто что играет

    private enum Style {
        BLADE("blade"), SCYTHE("scythe"), SPEAR("spear");

        final String id;

        Style(String id) {
            this.id = id;
        }

        static Style of(QuinqueType type) {
            return switch (type) {
                case JASON -> SCYTHE;
                case IXA -> SPEAR;
                default -> BLADE;
            };
        }
    }

    /** Приём, который играет игрок: имя, тик начала, длительность (у шквала и выпада) и прошлый приём. */
    private record Playing(String move, float start, float duration, Playing previous) {
    }

    private static final Map<Integer, Playing> PLAYING = new HashMap<>();
    /** Плавный вход в стойку блока и выход из неё (вес 0..1 и время прошлого кадра) по каждому игроку. */
    private static final Map<Integer, float[]> GUARD = new HashMap<>();
    /** Поворот квинке в кисти, посчитанный для текущего кадра отрисовки (рендерер квинке берёт его отсюда). */
    private static int wristEntity = Integer.MIN_VALUE;
    private static float[] wrist;

    /**
     * Пакет с сервера: имя вида quinque_attack_1 или quinque_flurry:30 (после двоеточия длительность в тиках).
     * Пустое имя — прервать приём.
     */
    public static void play(int entityId, String animation) {
        Playing old = PLAYING.get(entityId);
        if (animation == null || animation.isEmpty()) {
            PLAYING.remove(entityId);
            return;
        }
        String move = animation.substring("quinque_".length());
        float duration = -1.0F;
        int colon = move.indexOf(':');
        if (colon >= 0) {
            try {
                duration = Float.parseFloat(move.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                // без длительности — по ключевым кадрам
            }
            move = move.substring(0, colon);
        }
        // прошлый приём нужен только для плавного перехода, цепочку дальше не храним
        Playing previous = old == null ? null : new Playing(old.move, old.start, old.duration, null);
        PLAYING.put(entityId, new Playing(move, now(), duration, previous));
    }

    public static void clear() {
        PLAYING.clear();
        GUARD.clear();
        wrist = null;
        wristEntity = Integer.MIN_VALUE;
    }

    private static float now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() + mc.getFrameTime() : 0.0F;
    }

    private static boolean isBlocking(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player) {
            PlayerData data = PlayerData.get(player);
            return data != null && data.isBlocking();
        }
        KaguneAnimatable state = KaguneClientState.get(player.getId());
        return state != null && state.isBlocking();
    }

    /** Поза (значения каналов) и её вес: 0 — обычная поза, 1 — полностью анимация. */
    private record Sample(float[] v, float weight, float[] grip) {
    }

    private static Move moveFor(Style style, QuinqueType type, String name) {
        Map<String, Move> all = moves();
        if ("strong".equals(name) && type.ranged() && all.containsKey(style.id + ":strong_bolt")) {
            name = "strong_bolt";
        }
        return all.get(style.id + ":" + name);
    }

    /** Поза приёма в момент t (тиков от начала). withFadeIn = false — без плавного входа (его делает переход). */
    private static Sample sampleMove(Move m, Playing p, QuinqueType type, float t, boolean withFadeIn) {
        if (m == null) {
            return null;
        }
        float length = p.duration > 0 ? p.duration : m.length;
        if (t < 0 || t >= length) {
            return null;
        }
        float local;
        if (m.loop) {
            float rate = "flurry".equals(p.move) ? m.length / (2.0F * Math.max(1, type.flurryInterval())) : 1.0F;
            local = ((t - m.align) * rate + m.anchor) % m.length;
            if (local < 0) {
                local += m.length;
            }
        } else {
            local = Math.min(t, m.length);
        }
        float[] v = new float[N];
        for (int j = 0; j < N; j++) {
            v[j] = curve(m, j, local);
        }
        float in = withFadeIn ? smooth(m.fadeIn > 0 ? t / m.fadeIn : 1.0F) : 1.0F;
        float out = smooth(m.fadeOut > 0 ? (length - t) / m.fadeOut : 1.0F);
        return new Sample(v, in * out, m.grip);
    }

    private static Sample sample(LivingEntity entity, QuinqueType type) {
        Style style = Style.of(type);
        float time = now();
        Playing playing = PLAYING.get(entity.getId());
        if (playing != null) {
            Move m = moveFor(style, type, playing.move);
            float t = time - playing.start;
            Playing prev = playing.previous;
            Move pm = prev != null ? moveFor(style, type, prev.move) : null;
            boolean blending = pm != null && m != null && t < m.fadeIn;
            Sample s = sampleMove(m, playing, type, t, !blending);
            if (s == null) {
                PLAYING.remove(entity.getId());
            } else if (blending) {
                // плавный переход из прошлого приёма в новый (например, между ударами серии)
                Sample ps = sampleMove(pm, prev, type, time - prev.start, true);
                float f = smooth(t / m.fadeIn);
                float wp = ps != null ? ps.weight : 0.0F;
                float w = Mth.lerp(f, wp, s.weight);
                if (w < 1.0E-4F) {
                    return null;
                }
                float[] v = new float[N];
                for (int j = 0; j < N; j++) {
                    float a = ps != null ? ps.v[j] * wp : 0.0F;
                    v[j] = Mth.lerp(f, a, s.v[j] * s.weight) / w;
                }
                return new Sample(v, w, s.grip);
            } else {
                return s;
            }
        }
        // стойка блока: плавно встаём и выходим (примерно за 3 тика)
        float[] guard = GUARD.computeIfAbsent(entity.getId(), id -> new float[] {0.0F, time});
        float target = isBlocking(entity) ? 1.0F : 0.0F;
        float dt = Mth.clamp(time - guard[1], 0.0F, 5.0F);
        guard[1] = time;
        guard[0] = target > guard[0] ? Math.min(target, guard[0] + dt / 3.0F) : Math.max(target, guard[0] - dt / 3.0F);
        if (guard[0] > 0.001F) {
            Move m = moves().get(style.id + ":guard");
            if (m != null) {
                float[] v = new float[N];
                for (int j = 0; j < N; j++) {
                    v[j] = m.values[j][0];
                }
                return new Sample(v, smooth(guard[0]), m.grip);
            }
        }
        return null;
    }

    private static QuinqueType typeOf(ItemStack stack) {
        return stack.getItem() instanceof QuinqueItem item ? item.type() : null;
    }

    // ------------------------------------------------------------------ матрицы 4 x 4 (по столбцам, как PoseStack)

    private static float[] mul(float[] a, float[] b) {
        float[] o = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                float s = 0;
                for (int k = 0; k < 4; k++) {
                    s += a[k * 4 + r] * b[c * 4 + k];
                }
                o[c * 4 + r] = s;
            }
        }
        return o;
    }

    private static float[] translate(float x, float y, float z) {
        return new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1};
    }

    private static float[] scale(float s) {
        return new float[] {s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1};
    }

    private static float[] rotX(float a) {
        float c = Mth.cos(a), s = Mth.sin(a);
        return new float[] {1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0, 0, 0, 0, 1};
    }

    private static float[] rotY(float a) {
        float c = Mth.cos(a), s = Mth.sin(a);
        return new float[] {c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1};
    }

    private static float[] rotZ(float a) {
        float c = Mth.cos(a), s = Mth.sin(a);
        return new float[] {c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static float[] point(float[] m, float x, float y, float z) {
        return new float[] {m[0] * x + m[4] * y + m[8] * z + m[12], m[1] * x + m[5] * y + m[9] * z + m[13],
                m[2] * x + m[6] * y + m[10] * z + m[14]};
    }

    /** Поворот как у ModelPart (Rz * Ry * Rx): из матрицы обратно в xRot, yRot, zRot. */
    private static void setRotation(ModelPart part, float[] m) {
        part.xRot = (float) Math.atan2(m[6], m[10]);
        part.yRot = (float) Math.asin(Mth.clamp(-m[2], -1.0F, 1.0F));
        part.zRot = (float) Math.atan2(m[1], m[0]);
    }

    /**
     * Цепочка от руки до квинке в пространстве руки (как ItemInHandLayer + модель предмета + QuinqueRenderer,
     * правая рука, третье лицо). Нужна, чтобы вторая рука нашла древко.
     */
    private static float[] handChain(QuinqueType type, float[] wristDeg) {
        float[] m = mul(rotX(-90 * DEG), rotY(180 * DEG));
        m = mul(m, translate(1.0F / 16.0F, 0.125F, -0.625F));
        // thirdperson_righthand: translation [0, 4, 0.5] / 16, rotation [0, -90, 55] (XYZ), scale 0.85
        m = mul(m, translate(0.0F, 4.0F / 16.0F, 0.5F / 16.0F));
        m = mul(m, mul(rotX(0), mul(rotY(-90 * DEG), rotZ(55 * DEG))));
        m = mul(m, scale(0.85F));
        m = mul(m, translate(-0.5F, -0.5F, -0.5F));
        m = mul(m, translate(QuinqueRenderer.GRIP, QuinqueRenderer.GRIP, 0.5F));
        m = mul(m, rotZ(-45 * DEG));
        m = mul(m, mul(rotZ(wristDeg[2] * DEG), mul(rotY(wristDeg[1] * DEG), rotX(wristDeg[0] * DEG))));
        m = mul(m, scale(QuinqueRenderer.THIRD_PERSON_SCALE));
        if (type.flipInHand()) {
            m = mul(m, rotY((float) Math.PI));
        }
        return m;
    }

    // ------------------------------------------------------------------ третье лицо

    /** Нужна ли сейчас своя поза руки (иначе обычная поза «держит предмет»). */
    public static boolean animating(LivingEntity entity, ItemStack stack) {
        QuinqueType type = typeOf(stack);
        boolean on = type != null && !entity.isUsingItem() && !com.tokyoghoulrpg.client.vr.VrClient.isVrPlayer(entity)
                && sample(entity, type) != null;
        if (!on && entity.getId() == wristEntity) {
            // приём закончился: поворот в кисти из прошлого кадра больше не нужен
            wristEntity = Integer.MIN_VALUE;
            wrist = null;
        }
        return on;
    }

    /** Поворот квинке в кисти для этого существа в текущем кадре (или null). */
    public static float[] wrist(LivingEntity entity) {
        return entity != null && entity.getId() == wristEntity ? wrist : null;
    }

    /** Поза рук, корпуса и ног с квинке от третьего лица. */
    public static void applyThirdPerson(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        wristEntity = Integer.MIN_VALUE;
        wrist = null;
        QuinqueType type = typeOf(entity.getMainHandItem());
        Sample s = type != null ? sample(entity, type) : null;
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart main = right ? model.rightArm : model.leftArm;
        ModelPart other = right ? model.leftArm : model.rightArm;
        ModelPart mainLeg = right ? model.rightLeg : model.leftLeg;
        ModelPart otherLeg = right ? model.leftLeg : model.rightLeg;
        // обычная поза «держит предмет» — от неё и к ней плавный переход
        float baseX = main.xRot * 0.5F - (float) Math.PI / 10.0F;
        if (s == null) {
            main.xRot = baseX;
            main.yRot = 0.0F;
            return;
        }
        float w = s.weight;
        float side = right ? 1.0F : -1.0F;
        float[] v = s.v;
        // свой взмах вместо обычного взмаха рукой
        model.attackTime = 0.0F;

        float yaw = Mth.lerp(w, 0.0F, v[ARM]);
        float pitch = Mth.lerp(w, baseX / DEG, v[ARM + 1]);
        float twist = Mth.lerp(w, 0.0F, v[ARM + 2]);
        float body = v[BODY] * w * DEG * side;
        model.body.yRot = body;
        // плечи поворачиваются вместе с корпусом (как в обычной анимации удара)
        float sin = Mth.sin(body);
        float cos = Mth.cos(body);
        model.rightArm.z = sin * 5.0F;
        model.rightArm.x = -cos * 5.0F;
        model.leftArm.z = -sin * 5.0F;
        model.leftArm.x = cos * 5.0F;

        float[] r = mul(rotY(body), mul(rotY(yaw * DEG * side), mul(rotX(pitch * DEG), rotY(twist * DEG * side))));
        setRotation(main, r);
        mainLeg.xRot = Mth.lerp(w, mainLeg.xRot, v[LEGS] * DEG);
        otherLeg.xRot = Mth.lerp(w, otherLeg.xRot, v[LEGS + 1] * DEG);

        float[] wr = {v[WRIST] * w, v[WRIST + 1] * w, v[WRIST + 2] * w};
        wristEntity = entity.getId();
        wrist = wr;

        if (s.grip != null && right) {
            // вторая рука держит древко: ищем на древке точку, до которой она достаёт
            float armY = entity.isCrouching() ? 5.0F : 2.0F;
            float[] hand = mul(mul(translate(main.x / 16.0F, armY / 16.0F, main.z / 16.0F), r), handChain(type, wr));
            float sx = other.x, sy = armY, sz = other.z;
            float bestErr = Float.MAX_VALUE;
            float[] best = null;
            for (int i = 0; i <= 24; i++) {
                float d = s.grip[0] + (s.grip[1] - s.grip[0]) * i / 24.0F;
                float[] p = point(hand, 0.0F, d, 0.0F);
                float px = p[0] * 16.0F - sx, py = p[1] * 16.0F - sy, pz = p[2] * 16.0F - sz;
                float err = Math.abs(Mth.sqrt(px * px + py * py + pz * pz) - 9.5F);
                if (err < bestErr) {
                    bestErr = err;
                    best = new float[] {px, py, pz};
                }
            }
            if (best != null) {
                float len = Mth.sqrt(best[0] * best[0] + best[1] * best[1] + best[2] * best[2]);
                if (len > 1.0E-4F) {
                    float dx = best[0] / len, dy = best[1] / len, dz = best[2] / len;
                    float offPitch = -(float) Math.acos(Mth.clamp(dy, -1.0F, 1.0F));
                    float offYaw = (float) Math.atan2(-dx, -dz);
                    float[] ro = mul(rotY(offYaw), rotX(offPitch));
                    float ox = (float) Math.atan2(ro[6], ro[10]);
                    float oy = (float) Math.asin(Mth.clamp(-ro[2], -1.0F, 1.0F));
                    float oz = (float) Math.atan2(ro[1], ro[0]);
                    other.xRot = Mth.lerp(w, other.xRot, ox);
                    other.yRot = Mth.lerp(w, other.yRot, oy);
                    other.zRot = Mth.lerp(w, other.zRot, oz);
                }
            }
        }
    }

    // ------------------------------------------------------------------ первое лицо

    /**
     * Своя квинке от первого лица. Возвращает false, если анимации нет (тогда как обычно).
     * В VR руки рисует Vivecraft по контроллерам — там анимации от первого лица не нужны.
     */
    public static boolean applyFirstPerson(PoseStack poseStack, LivingEntity player, HumanoidArm arm, ItemStack stack,
                                           float equipProcess) {
        QuinqueType type = typeOf(stack);
        if (type == null || com.tokyoghoulrpg.client.vr.VrClient.active() || stack != player.getMainHandItem()) {
            return false;
        }
        Sample s = sample(player, type);
        if (s == null) {
            return false;
        }
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float w = s.weight;
        float[] v = s.v;
        // обычное место предмета в руке (как ItemInHandRenderer.applyItemArmTransform)
        poseStack.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        poseStack.translate(v[FP_POS] * side * w, v[FP_POS + 1] * w, v[FP_POS + 2] * w);
        poseStack.mulPose(Axis.YP.rotationDegrees(v[FP_ROT + 1] * side * w));
        poseStack.mulPose(Axis.XP.rotationDegrees(v[FP_ROT] * w));
        poseStack.mulPose(Axis.ZP.rotationDegrees(v[FP_ROT + 2] * side * w));
        return true;
    }
}
