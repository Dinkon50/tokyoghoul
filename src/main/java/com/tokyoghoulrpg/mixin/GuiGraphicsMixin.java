package com.tokyoghoulrpg.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Окна с ячейками (инвентарь, сундуки, печи...) у мода тёмные, а игра пишет их подписи тёмно-серым (0x404040).
 * В обычных окнах Minecraft такая подпись рисуется светлой, с бордовой тенью — как заголовки в меню мода.
 * То же в окне достижений. Цифра уровня опыта — красная, под цвет полосы опыта.
 * Подпись «Инвентарь» над ячейками игрока не рисуется: вместо неё остаётся красная линия-разделитель.
 * Окна других модов не трогаем: у них свои светлые текстуры.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private static final int TOKYOGHOULRPG_VANILLA_LABEL = 0x404040;
    @Unique
    private static final int TOKYOGHOULRPG_LABEL = 0xF2E2E2;
    @Unique
    private static final int TOKYOGHOULRPG_LABEL_SHADOW = 0x5A0E18;

    @Unique
    private static boolean tokyoghoulrpg$isLabel(int color) {
        if ((color & 0xFFFFFF) != TOKYOGHOULRPG_VANILLA_LABEL) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.screen instanceof AbstractContainerScreen<?> screen
                && screen.getClass().getName().startsWith("net.minecraft.")
                || mc.screen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
    }

    /** Зелёная цифра уровня опыта над полосой — кровавая, как сама полоса (в окнах с ячейками не трогаем). */
    @Unique
    private static int tokyoghoulrpg$xpColor(int color) {
        if ((color & 0xFFFFFF) == 0x80FF20
                && !(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) {
            return (color & 0xFF000000) | 0xFF4A56;
        }
        return color;
    }

    /** Подпись «Инвентарь» (на языке игры). */
    @Unique
    private static boolean tokyoghoulrpg$isInventoryTitle(String text) {
        return text != null && text.equals(
                net.minecraft.network.chat.Component.translatable("container.inventory").getString());
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void tokyoghoulrpg$labelString(Font font, String text, int x, int y, int color, boolean shadow,
            CallbackInfoReturnable<Integer> cir) {
        int xp = tokyoghoulrpg$xpColor(color);
        if (xp != color && text != null) {
            cir.setReturnValue(((GuiGraphics) (Object) this).drawString(font, text, x, y, xp, shadow));
            return;
        }
        if (text != null && tokyoghoulrpg$isLabel(color)) {
            if (tokyoghoulrpg$isInventoryTitle(text)) {
                cir.setReturnValue(x);
                return;
            }
            GuiGraphics self = (GuiGraphics) (Object) this;
            int alpha = color & 0xFF000000;
            self.drawString(font, text, x + 1, y + 1, alpha | TOKYOGHOULRPG_LABEL_SHADOW, false);
            cir.setReturnValue(self.drawString(font, text, x, y, alpha | TOKYOGHOULRPG_LABEL, false));
        }
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void tokyoghoulrpg$labelSequence(Font font, FormattedCharSequence text, int x, int y, int color,
            boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (tokyoghoulrpg$isLabel(color)) {
            StringBuilder plain = new StringBuilder();
            text.accept((index, style, codePoint) -> {
                plain.appendCodePoint(codePoint);
                return true;
            });
            if (tokyoghoulrpg$isInventoryTitle(plain.toString())) {
                cir.setReturnValue(x);
                return;
            }
            GuiGraphics self = (GuiGraphics) (Object) this;
            int alpha = color & 0xFF000000;
            self.drawString(font, text, x + 1, y + 1, alpha | TOKYOGHOULRPG_LABEL_SHADOW, false);
            cir.setReturnValue(self.drawString(font, text, x, y, alpha | TOKYOGHOULRPG_LABEL, false));
        }
    }
}
