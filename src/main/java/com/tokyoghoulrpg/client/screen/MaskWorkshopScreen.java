package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.MaskEffects;
import com.tokyoghoulrpg.item.MaskItem;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.MaskWorkshopC2S;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Мастерская масок: четыре маски, у каждой «Обычная» и «Работа мастера» с ценой в очках крови. */
public class MaskWorkshopScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int ROW = 40;
    private final BlockPos pos;

    public MaskWorkshopScreen(BlockPos pos) {
        super(Component.translatable("screen.tokyoghoulrpg.mask_workshop.title"));
        this.pos = pos;
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(6, (this.height - (ModItems.MASKS.size() * ROW + 70)) / 2);
    }

    @Override
    protected void init() {
        int left = left();
        int y = top() + 32;
        for (int i = 0; i < ModItems.MASKS.size(); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.mask_workshop.plain",
                                    Balance.MASK_PRICE),
                            b -> NetworkHandler.CHANNEL.sendToServer(new MaskWorkshopC2S(pos, index, false)))
                    .bounds(left + WIDTH - 196, y + 12, 90, 18).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.mask_workshop.master",
                                    Balance.MASK_MASTER_PRICE),
                            b -> NetworkHandler.CHANNEL.sendToServer(new MaskWorkshopC2S(pos, index, true)))
                    .bounds(left + WIDTH - 102, y + 12, 96, 18).build());
            y += ROW;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(this.width / 2 - 50, y + 16, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        int height = ModItems.MASKS.size() * ROW + 70;
        gfx.fill(left, top, left + WIDTH, top + height, 0xC0140808);
        gfx.fill(left, top, left + 2, top + height, 0xFFB01E1E);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top + 7, 0xC21B1B);
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        if (data != null) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.mask_workshop.blood",
                    data.getBloodPoints()), this.width / 2, top + 19, 0xFF5A78);
        }
        int y = top + 32;
        for (int i = 0; i < ModItems.MASKS.size(); i++) {
            ItemStack stack = new ItemStack(ModItems.MASKS.get(i).get());
            gfx.fill(left + 6, y, left + WIDTH - 6, y + ROW - 4, 0x60201010);
            gfx.renderItem(stack, left + 10, y + 10);
            gfx.drawString(this.font, stack.getHoverName(), left + 32, y + 5, 0xE6E6E6, false);
            if (stack.getItem() instanceof MaskItem mask) {
                MaskEffects.Kind kind = mask.kind();
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.mask_workshop.effect." + kind.id(),
                        MaskEffects.percent(kind, 0), MaskEffects.percent(kind, 1)), left + 32, y + 18, 0xC07070, false);
            }
            y += ROW;
        }
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.mask_workshop.note"),
                this.width / 2, y + 2, 0x7A7A7A);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
