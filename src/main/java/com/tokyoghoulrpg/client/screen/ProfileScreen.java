package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.QuinqueUpgrade;
import com.tokyoghoulrpg.rank.Ranks;
import com.tokyoghoulrpg.rep.Standing;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * «Профиль» (клавиша O): всё о персонаже на одном экране.
 * Слева — класс, ранг, здоровье, очки, все репутации, клан, квинке в руке, Какудзя.
 * Справа — откаты способностей твоего класса (идут в реальном времени).
 */
public class ProfileScreen extends Screen {
    private static final int WIDTH = 340;
    private static final int HEIGHT = 262;
    private static final int LINE = 12;

    /** Первый клик по «Престиж» — просьба подтвердить, второй — престиж. */
    private boolean prestigeConfirm = false;

    public ProfileScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.profile.title"));
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - HEIGHT) / 2);
    }

    @Override
    protected void init() {
        int by = top() + HEIGHT - 24;
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.profile.achievements"),
                b -> Minecraft.getInstance().setScreen(new AchievementsScreen())).bounds(left() + WIDTH - 94, top() + 4, 88, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(left() + WIDTH / 2 - 50, by, 100, 18).build());
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        // престиж: только на высшем ранге; нужно нажать дважды (подтверждение)
        if (data != null && Ranks.ladder(data.getPlayerClass()) != Ranks.NONE) {
            String block = Ranks.prestigeBlock(data);
            Button prestige = Button.builder(Component.translatable(prestigeConfirm
                            ? "screen.tokyoghoulrpg.profile.prestige_confirm" : "screen.tokyoghoulrpg.profile.prestige"),
                    b -> {
                        if (!prestigeConfirm) {
                            prestigeConfirm = true;
                            rebuildWidgets();
                            return;
                        }
                        prestigeConfirm = false;
                        com.tokyoghoulrpg.network.NetworkHandler.CHANNEL.sendToServer(
                                new com.tokyoghoulrpg.network.packet.PrestigeC2S());
                        onClose();
                    })
                    .bounds(left() + 6, top() + 4, 88, 14)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(block == null
                            ? Component.translatable("screen.tokyoghoulrpg.profile.prestige_tip", Ranks.prestige(data) + 1,
                            Balance.PRESTIGE_REWARD_BLOOD * (Ranks.prestige(data) + 1), Balance.PRESTIGE_REWARD_POINTS)
                            : Component.translatable("message.tokyoghoulrpg." + block)))
                    .build();
            prestige.active = block == null;
            addRenderableWidget(prestige);
        }
        // тип кагуне: выбрать или сменить
        if (data != null && data.getPlayerClass().hasKagune()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.profile.type_button"),
                    b -> Minecraft.getInstance().setScreen(new KaguneTypeScreen())).bounds(left() + 8, by, 100, 18).build());
        }
        // подсказки для новичков: пропустить, если идут, или посмотреть заново
        if (data != null) {
            boolean running = !data.isTutorialDone();
            addRenderableWidget(Button.builder(Component.translatable(running
                            ? "screen.tokyoghoulrpg.profile.tutorial_skip" : "screen.tokyoghoulrpg.profile.tutorial_restart"),
                    b -> {
                        com.tokyoghoulrpg.network.NetworkHandler.CHANNEL.sendToServer(
                                new com.tokyoghoulrpg.network.packet.TutorialC2S(running
                                        ? com.tokyoghoulrpg.network.packet.TutorialC2S.SKIP
                                        : com.tokyoghoulrpg.network.packet.TutorialC2S.RESTART, 0));
                        onClose();
                    }).bounds(left() + WIDTH - 108, by, 100, 18).build());
        }
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + HEIGHT, 0xC0140808);
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFB01E1E);
        gfx.fill(left + 178, top + 22, left + 179, top + HEIGHT - 30, 0x40FFFFFF);

        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        if (mc.player == null || data == null || mc.level == null) {
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        long now = mc.level.getGameTime();
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.profile.header",
                mc.player.getGameProfile().getName()), this.width / 2, top + 7, 0xC21B1B);

        // ---------------- слева: персонаж
        int x = left + 10;
        int y = top + 24;
        PlayerClass cls = data.getPlayerClass();
        Component classLine = Component.translatable(cls.translationKey());
        if (cls.hasKagune() && data.getKaguneVariant() != KaguneVariant.NONE) {
            classLine = Component.empty().append(classLine).append(", ")
                    .append(Component.translatable(data.getKaguneVariant().translationKey()));
        }
        y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.class", classLine), 0xE6E6E6);
        if (!data.getTitle().isEmpty()) {
            y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.title_line",
                    Component.translatable("achievement.tokyoghoulrpg." + data.getTitle() + ".title")), 0xFFD700);
        }
        if (cls.hasKagune()) {
            com.tokyoghoulrpg.data.KaguneType type = data.getKaguneType();
            y = line(gfx, x + 8, y, type == com.tokyoghoulrpg.data.KaguneType.NONE
                    ? Component.translatable("screen.tokyoghoulrpg.profile.type_none")
                    : Component.translatable("screen.tokyoghoulrpg.profile.type", Component.translatable(type.translationKey()),
                    Component.translatable(type.translationKey() + ".short")), 0xFF8A8A);
        }

        int ladder = Ranks.ladder(cls);
        if (ladder == Ranks.NONE) {
            y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.rank_none"), 0x9A9A9A);
        } else {
            int points = Ranks.points(data);
            int index = Ranks.index(points);
            int next = Ranks.nextThreshold(index);
            y = line(gfx, x, y, Component.translatable(ladder == Ranks.CCG ? "screen.tokyoghoulrpg.profile.rank_ccg"
                    : "screen.tokyoghoulrpg.profile.rank_ghoul", Ranks.title(ladder, index)), 0xE6E6E6);
            y = line(gfx, x + 8, y, next < 0
                    ? Component.translatable("screen.tokyoghoulrpg.profile.rank_max", points)
                    : Component.translatable("screen.tokyoghoulrpg.profile.rank_next", points, next), 0x9A9A9A);
            y = line(gfx, x + 8, y, Component.translatable("screen.tokyoghoulrpg.profile.rank_bonus",
                    Math.round(Ranks.damageBonus(data) * 100), Math.round(Ranks.bloodBonus(data) * 100)), 0x9A9A9A);
            int prestige = Ranks.prestige(data);
            if (prestige > 0) {
                y = line(gfx, x + 8, y, Component.translatable("screen.tokyoghoulrpg.profile.prestige_line",
                        Ranks.stars(prestige), Balance.PRESTIGE_MAX, Math.round(Balance.PRESTIGE_DEFENSE * prestige * 100)),
                        0xFFD27A);
            }
        }
        y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.health",
                Math.round(mc.player.getHealth()), Math.round(mc.player.getMaxHealth())), 0xFF6A6A);
        y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.points",
                data.getSkillPoints(), data.getBloodPoints()), 0xFF5A78);
        y += 4;

        y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.rep_title"), 0xE0A050);
        y = line(gfx, x + 8, y, Component.translatable("screen.tokyoghoulrpg.profile.rep_own",
                Standing.colored(data.getStanding())), 0xC9C9C9);
        y = line(gfx, x + 8, y, Component.translatable("screen.tokyoghoulrpg.profile.rep_sides",
                signed(data.getRepCcg()), signed(data.getRepGhouls())), 0xC9C9C9);
        Component clan = data.getClanNameCache().isEmpty()
                ? Component.translatable("screen.tokyoghoulrpg.profile.no_clan")
                : Component.translatable("screen.tokyoghoulrpg.profile.clan", data.getClanNameCache(),
                Standing.colored(data.getClanRepCache()));
        y = line(gfx, x + 8, y, clan, 0xC9C9C9);
        y += 4;

        ItemStack hand = mc.player.getMainHandItem();
        Component quinque;
        if (QuinqueUpgrade.upgradable(hand)) {
            int level = QuinqueUpgrade.level(hand);
            quinque = Component.translatable("screen.tokyoghoulrpg.profile.quinque", hand.getHoverName(),
                    level, Math.round(Balance.QUINQUE_LEVEL_BONUS[level] * 100));
        } else {
            quinque = Component.translatable("screen.tokyoghoulrpg.profile.no_quinque");
        }
        y = line(gfx, x, y, quinque, 0x7FD8E6);
        // маска на лице
        ItemStack head = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (head.getItem() instanceof com.tokyoghoulrpg.item.MaskItem mask) {
            int tier = com.tokyoghoulrpg.item.MaskEffects.isMaster(head) ? 1 : 0;
            Component effect = Component.translatable("item.tokyoghoulrpg.mask." + mask.kind().id() + ".effect",
                    com.tokyoghoulrpg.item.MaskEffects.percent(mask.kind(), tier));
            y = line(gfx, x, y, Component.translatable("screen.tokyoghoulrpg.profile.mask", head.getHoverName()), 0xD08A6A);
            y = line(gfx, x + 8, y, cls.hasKagune() ? effect
                    : Component.translatable("screen.tokyoghoulrpg.profile.mask_off"), 0x9A9A9A);
        }

        if (cls.hasKagune()) {
            Component kakuja;
            if (data.isKakujaActive(now)) {
                kakuja = Component.translatable("screen.tokyoghoulrpg.profile.kakuja_active",
                        (data.getKakujaEndTick() - now + 19) / 20);
            } else {
                int stage = com.tokyoghoulrpg.combat.Kakuja.stage(data.getKakujaPoints());
                int next = com.tokyoghoulrpg.combat.Kakuja.nextThreshold(stage);
                Component stageText = Component.literal(stage == 0 ? "-" : com.tokyoghoulrpg.skill.SkillTree.roman(stage));
                kakuja = next < 0
                        ? Component.translatable("screen.tokyoghoulrpg.profile.kakuja_stage_max", stageText, data.getKakujaPoints(),
                        Math.round(stage * Balance.KAKUJA_STAGE_DAMAGE * 100))
                        : Component.translatable("screen.tokyoghoulrpg.profile.kakuja_stage", stageText,
                        data.getKakujaPoints(), next, Math.round(stage * Balance.KAKUJA_STAGE_DAMAGE * 100));
            }
            line(gfx, x, y, kakuja, 0xE0203A);
        }

        // ---------------- справа: откаты
        int rx = left + 188;
        int ry = top + 24;
        ry = line(gfx, rx, ry, Component.translatable("screen.tokyoghoulrpg.profile.cooldowns"), 0xE0A050);
        for (String[] entry : cooldownEntries(cls)) {
            long ready = data.getCooldownsCache().getOrDefault(entry[0], 0L);
            long left2 = ready - now;
            Component name = Component.translatable(entry[1]);
            Component state = left2 > 0
                    ? Component.translatable("screen.tokyoghoulrpg.profile.cd_wait", (left2 + 19) / 20)
                    : Component.translatable("screen.tokyoghoulrpg.profile.cd_ready");
            gfx.drawString(this.font, name, rx, ry, 0xC9C9C9, false);
            gfx.drawString(this.font, state, left + WIDTH - 10 - this.font.width(state), ry,
                    left2 > 0 ? 0xFF6A6A : 0x6EE07A, false);
            ry += LINE;
        }
        // «Ночь охоты»
        ry += 4;
        int days = com.tokyoghoulrpg.world.HuntNight.daysUntil(mc.level);
        Component hunt = com.tokyoghoulrpg.world.HuntNight.isActive(mc.level)
                ? Component.translatable("screen.tokyoghoulrpg.profile.hunt_now")
                : days == 0 ? Component.translatable("screen.tokyoghoulrpg.profile.hunt_tonight")
                : Component.translatable("screen.tokyoghoulrpg.profile.hunt_days", days);
        gfx.drawString(this.font, hunt, rx, ry, 0xE05050, false);
        ry += LINE;
        if (data.getDailyStreak() > 0) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.profile.daily", data.getDailyStreak(),
                    com.tokyoghoulrpg.rep.DailyReward.cycleDay(data.getDailyStreak())), rx, ry, 0xE0C060, false);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private int line(GuiGraphics gfx, int x, int y, Component text, int color) {
        gfx.drawString(this.font, text, x, y, color, false);
        return y + LINE;
    }

    /** Какие откаты показывать: ключ в данных и название способности. */
    private static List<String[]> cooldownEntries(PlayerClass cls) {
        List<String[]> list = new ArrayList<>();
        if (cls.hasKagune()) {
            list.add(new String[] {"bite", "ability.tokyoghoulrpg.bite"});
            list.add(new String[] {"dash", "ability.tokyoghoulrpg.dash"});
            list.add(new String[] {"barrage", "ability.tokyoghoulrpg.barrage"});
            list.add(new String[] {"block", "ability.tokyoghoulrpg.block"});
            list.add(new String[] {"strong", "ability.tokyoghoulrpg.strong_attack"});
            list.add(new String[] {"pull", "ability.tokyoghoulrpg.pull_target"});
            list.add(new String[] {"grapple", "ability.tokyoghoulrpg.grapple_self"});
            list.add(new String[] {"kakuja", "ability.tokyoghoulrpg.kakuja"});
        } else if (cls == PlayerClass.CCG_AGENT) {
            list.add(new String[] {"dash", "ability.tokyoghoulrpg.dash"});
            list.add(new String[] {"strong", "ability.tokyoghoulrpg.quinque_strong"});
            list.add(new String[] {"barrage", "ability.tokyoghoulrpg.quinque_flurry"});
            list.add(new String[] {"lunge", "ability.tokyoghoulrpg.quinque_lunge"});
            list.add(new String[] {"block", "ability.tokyoghoulrpg.block"});
        } else if (cls == PlayerClass.HUMAN) {
            list.add(new String[] {"dash", "ability.tokyoghoulrpg.dash"});
            list.add(new String[] {"human_strong", "ability.tokyoghoulrpg.human_strong_attack"});
        }
        list.add(new String[] {"retribution", "ability.tokyoghoulrpg.retribution"});
        list.add(new String[] {"charisma", "ability.tokyoghoulrpg.charisma"});
        list.add(new String[] {"fortune", "ability.tokyoghoulrpg.fortune"});
        list.add(new String[] {"bond", "screen.tokyoghoulrpg.profile.bond"});
        return list;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.PROFILE.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
