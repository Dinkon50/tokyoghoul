package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.contract.Contract;
import com.tokyoghoulrpg.contract.ContractManager;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ContractActionC2S;
import com.tokyoghoulrpg.rep.Reputation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Окно информатора: контракт на сегодня или текущий контракт с прогрессом. */
public class ContractScreen extends Screen {
    private static final int WIDTH = 280;
    private static final int HEIGHT = 120;

    private final int informantId;
    private final Contract offer;
    private final long day;
    private Button take;
    private Button turnIn;
    private Button abandon;

    public ContractScreen(int informantId, String offerId, long day) {
        super(Component.translatable("screen.tokyoghoulrpg.contract.title"));
        this.informantId = informantId;
        this.offer = Contract.byId(offerId);
        this.day = day;
    }

    private PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return (this.height - HEIGHT) / 2;
    }

    private void send(int action) {
        NetworkHandler.CHANNEL.sendToServer(new ContractActionC2S(informantId, action));
    }

    @Override
    protected void init() {
        int y = top() + HEIGHT - 26;
        int w = (WIDTH - 24) / 3;
        take = addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.contract.take"),
                b -> send(ContractManager.ACCEPT)).bounds(left() + 6, y, w, 20).build());
        turnIn = addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.contract.turn_in"),
                b -> send(ContractManager.TURN_IN)).bounds(left() + 12 + w, y, w, 20).build());
        abandon = addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.contract.abandon"),
                b -> send(ContractManager.ABANDON)).bounds(left() + 18 + 2 * w, y, w, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + HEIGHT, 0xC0140808);
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFB01E1E);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top + 6, 0xC21B1B);

        PlayerData data = data();
        Contract active = data == null || data.getContractId() == null ? null : Contract.byId(data.getContractId());
        boolean doneToday = data != null && data.getContractDoneDay() == day;
        int y = top + 24;

        if (active != null) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.contract.active"), left + 10, y, 0x9A9A9A, false);
            gfx.drawString(this.font, active.title(), left + 10, y + 12, 0xFFFFFF, false);
            reward(gfx, active, left + 10, y + 26);
            if (active.kind() != Contract.Kind.DELIVER) {
                int barW = WIDTH - 70;
                float fraction = Mth.clamp(data.getContractProgress() / (float) active.goal(), 0.0F, 1.0F);
                gfx.fill(left + 10, y + 42, left + 10 + barW, y + 46, 0xFF2A0A14);
                gfx.fill(left + 10, y + 42, left + 10 + Math.round(barW * fraction), y + 46, 0xFFD01F4A);
                gfx.drawString(this.font, Math.min(data.getContractProgress(), active.goal()) + "/" + active.goal(),
                        left + 16 + barW, y + 40, 0xC9C9C9, false);
            } else {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.contract.deliver_hint"),
                        left + 10, y + 40, 0x9A9A9A, false);
            }
        } else if (data != null && !Reputation.informantTrusts(data)) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.contract.distrust"),
                    this.width / 2, y + 20, 0xC04050);
        } else if (doneToday || offer == null) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.contract.tomorrow"),
                    this.width / 2, y + 20, 0x9A9A9A);
        } else {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.contract.offer"), left + 10, y, 0x9A9A9A, false);
            gfx.drawString(this.font, offer.title(), left + 10, y + 12, 0xFFFFFF, false);
            reward(gfx, offer, left + 10, y + 26);
        }
        take.active = active == null && !doneToday && offer != null && data != null && Reputation.informantTrusts(data);
        turnIn.active = active != null;
        abandon.active = active != null;
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void reward(GuiGraphics gfx, Contract contract, int x, int y) {
        Component text = contract.points() > 0
                ? Component.translatable("screen.tokyoghoulrpg.contract.reward_points", contract.bloodFor(data()), contract.points())
                : Component.translatable("screen.tokyoghoulrpg.contract.reward", contract.bloodFor(data()));
        gfx.drawString(this.font, text, x, y, 0xFF5A78, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
