package com.tokyoghoulrpg.client.quinque;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.kagune.KaguneAnimatable;
import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.QuinqueItem;
import com.tokyoghoulrpg.item.QuinqueType;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/**
 * Рисует 3D-модель квинке в руке (от первого и третьего лица). В инвентаре, на земле и в рамке
 * показывается обычная иконка (так задано в модели предмета), поэтому здесь только руки.
 * Квинке держится как меч: модель повёрнута по диагонали плоскости предмета, рукоять у нижнего левого
 * угла, остриём вверх-вперёд. От третьего лица — в настоящем размере (1 пиксель Blockbench =
 * 1/16 блока), от первого лица — уменьшенная, чтобы не закрывать экран.
 */
public class QuinqueRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/quinque/palette.png");
    /** Точка хвата в плоскости предмета (в пикселях из 16): как у рукояти меча. */
    private static final float GRIP = 3.5F / 16.0F;
    private static final float THIRD_PERSON_SCALE = 1.0F;
    private static final float FIRST_PERSON_SCALE = 0.5F;
    private static final float OTHER_SCALE = 0.35F;

    private static QuinqueRenderer instance;
    private static final Map<String, Optional<QuinqueMesh>> MESHES = new HashMap<>();

    /** Кто сейчас держит рисуемую квинке (нужно, чтобы показать раскрытую форму в «Освобождении»). */
    private static LivingEntity holder;

    public QuinqueRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static QuinqueRenderer instance() {
        if (instance == null) {
            instance = new QuinqueRenderer();
        }
        return instance;
    }

    public static void setHolder(LivingEntity entity) {
        holder = entity;
    }

    /** Ресурсы перезагружены (F3+T): сетки прочитаем заново. */
    public static void clearCache() {
        MESHES.clear();
    }

    private static QuinqueMesh mesh(String name) {
        return MESHES.computeIfAbsent(name, n -> {
            ResourceLocation location = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "quinque/" + n + ".tgqm");
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                LOGGER.error("Нет модели квинке {}", location);
                return Optional.empty();
            }
            try (InputStream stream = resource.get().open()) {
                return Optional.of(QuinqueMesh.read(stream));
            } catch (Exception e) {
                LOGGER.error("Не удалось прочитать модель квинке {}", location, e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** Держит ли holder квинке в «Освобождении»: свой игрок — по своим данным, чужие — по рассылке сервера. */
    private static boolean released() {
        if (!(holder instanceof Player player)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player) {
            PlayerData data = PlayerData.get(player);
            return data != null && data.isRageActive();
        }
        KaguneAnimatable state = KaguneClientState.get(player.getId());
        return state != null && state.isRage();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        if (!(stack.getItem() instanceof QuinqueItem item) || !item.type().has3dModel()) {
            return;
        }
        QuinqueType type = item.type();
        String name = type.openMesh() != null && released() ? type.openMesh() : type.mesh();
        QuinqueMesh mesh = mesh(name);
        if (mesh == null) {
            return;
        }
        float scale = switch (context) {
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> THIRD_PERSON_SCALE;
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> FIRST_PERSON_SCALE;
            default -> OTHER_SCALE;
        };
        poseStack.pushPose();
        // рукоять в нижнем левом углу плоскости предмета, модель (+Y) по диагонали вправо-вверх, как клинок меча
        poseStack.translate(GRIP, GRIP, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.scale(scale, scale, scale);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        for (QuinqueMesh.Part part : mesh.parts()) {
            float[] q = part.quads();
            for (int i = 0; i < part.quadCount(); i++) {
                int base = i * 23;
                float nx = q[base + 20];
                float ny = q[base + 21];
                float nz = q[base + 22];
                for (int v = 0; v < 4; v++) {
                    int o = base + v * 5;
                    consumer.vertex(matrix, q[o], q[o + 1], q[o + 2])
                            .color(255, 255, 255, 255)
                            .uv(q[o + 3], q[o + 4])
                            .overlayCoords(overlay)
                            .uv2(light)
                            .normal(normal, nx, ny, nz)
                            .endVertex();
                }
            }
        }
        poseStack.popPose();
    }
}
