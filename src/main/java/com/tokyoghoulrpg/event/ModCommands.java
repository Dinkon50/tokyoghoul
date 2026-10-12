package com.tokyoghoulrpg.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.ClassChange;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Команды мода. Всё начинается с /tgrpg, полный список: /tgrpg help.
 * help и info о себе доступны всем, остальное только оператору (читы в одиночной игре).
 * У большинства команд в конце можно указать игрока, иначе команда действует на тебя.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class ModCommands {
    private static final int OP = 2;

    /** Что сделать с числом: добавить, задать или отнять. */
    private enum Op {
        ADD, SET, REMOVE
    }

    /** Команда над игроком: возвращает сообщение для того, кто её ввёл. */
    @FunctionalInterface
    private interface PlayerAction {
        Component run(ServerPlayer player, PlayerData data) throws CommandSyntaxException;
    }

    private ModCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tgrpg")
                .executes(ModCommands::help)
                .then(Commands.literal("help").executes(ModCommands::help))
                .then(Commands.literal("info")
                        .executes(ctx -> run(ctx, ctx.getSource().getPlayerOrException(), ModCommands::info))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(OP))
                                .executes(ctx -> run(ctx, EntityArgument.getPlayer(ctx, "player"), ModCommands::info))))
                .then(amount("points", 100000, (op, n) -> (player, data) -> {
                    data.setSkillPoints(apply(op, data.getSkillPoints(), n));
                    return Component.translatable("command.tokyoghoulrpg.points", player.getDisplayName(), data.getSkillPoints());
                }))
                .then(amount("blood", 10000000, (op, n) -> (player, data) -> {
                    data.setBloodPoints(apply(op, data.getBloodPoints(), n));
                    return Component.translatable("command.tokyoghoulrpg.blood", player.getDisplayName(), data.getBloodPoints());
                }))
                .then(Commands.literal("rage").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("value", IntegerArgumentType.integer(0, PlayerData.RAGE_MAX)),
                                ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    return (player, data) -> {
                                        data.setRage(value);
                                        return Component.translatable("command.tokyoghoulrpg.rage", player.getDisplayName(), value);
                                    };
                                })))
                .then(Commands.literal("ragelevel").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("level", IntegerArgumentType.integer(0, Balance.RAGE_LEVEL_MAX)),
                                ctx -> {
                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                    return (player, data) -> {
                                        data.setRageLevel(level);
                                        return Component.translatable("command.tokyoghoulrpg.ragelevel", player.getDisplayName(), level);
                                    };
                                })))
                .then(Commands.literal("class").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("class", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(classIds(), builder)),
                                ctx -> {
                                    PlayerClass target = PlayerClass.byId(StringArgumentType.getString(ctx, "class"));
                                    return (player, data) -> setClass(player, data, target);
                                })))
                .then(Commands.literal("variant").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("variant", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("kaneki", "author"), builder)),
                                ctx -> {
                                    KaguneVariant variant = KaguneVariant.byId(StringArgumentType.getString(ctx, "variant"));
                                    return (player, data) -> setVariant(data, player, variant);
                                })))
                .then(Commands.literal("skills").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.literal("reset"), ctx -> ModCommands::resetSkills))
                        .then(withTarget(Commands.literal("unlockall"), ctx -> ModCommands::unlockAllSkills)))
                .then(Commands.literal("quests").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.literal("complete"), ctx -> ModCommands::completeQuest))
                        .then(withTarget(Commands.literal("reset"), ctx -> ModCommands::resetQuests)))
                .then(Commands.literal("craving").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("value", IntegerArgumentType.integer(0, Balance.CRAVING_MAX)),
                                ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    return (player, data) -> {
                                        data.setCraving(value);
                                        return Component.translatable("command.tokyoghoulrpg.craving", player.getDisplayName(), value);
                                    };
                                })))
                .then(Commands.literal("contract").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.literal("reset"), ctx -> (player, data) -> {
                            data.setContract(null, 0);
                            data.setContractDoneDay(-1L);
                            return Component.translatable("command.tokyoghoulrpg.contract_reset", player.getDisplayName());
                        })))
                .then(Commands.literal("cooldowns").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.literal("reset"), ctx -> (player, data) -> {
                            AbilityManager.resetCooldowns(player);
                            return Component.translatable("command.tokyoghoulrpg.cooldowns", player.getDisplayName());
                        })))
                .then(withTarget(Commands.literal("reset").requires(src -> src.hasPermission(OP)), ctx -> (player, data) -> {
                    AbilityManager.resetAll(player);
                    data.reset();
                    AbilityManager.applyClassStats(player, data, false);
                    NetworkHandler.openClassSelection(player);
                    return Component.translatable("command.tokyoghoulrpg.reset", player.getDisplayName());
                })));
    }

    // ------------------------------------------------------------------
    // Построение команд
    // ------------------------------------------------------------------

    /**
     * Узел команды, после которого можно указать игрока: без игрока действует на того, кто ввёл.
     * action получает контекст команды и возвращает действие над игроком.
     */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withTarget(
            T node, java.util.function.Function<CommandContext<CommandSourceStack>, PlayerAction> action) {
        return node
                .executes(ctx -> run(ctx, ctx.getSource().getPlayerOrException(), action.apply(ctx)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> run(ctx, EntityArgument.getPlayer(ctx, "player"), action.apply(ctx))));
    }

    /** /tgrpg <имя> add|set|remove <число> [игрок] */
    private static ArgumentBuilder<CommandSourceStack, ?> amount(String name, int max,
                                                                 BiFunction<Op, Integer, PlayerAction> factory) {
        var root = Commands.literal(name).requires(src -> src.hasPermission(OP));
        for (Op op : Op.values()) {
            root.then(Commands.literal(op.name().toLowerCase())
                    .then(withTarget(Commands.argument("amount", IntegerArgumentType.integer(0, max)),
                            ctx -> factory.apply(op, IntegerArgumentType.getInteger(ctx, "amount")))));
        }
        return root;
    }

    private static int apply(Op op, int current, int amount) {
        return switch (op) {
            case ADD -> current + amount;
            case SET -> amount;
            case REMOVE -> Math.max(0, current - amount);
        };
    }

    private static int run(CommandContext<CommandSourceStack> ctx, ServerPlayer player, PlayerAction action)
            throws CommandSyntaxException {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return 0;
        }
        Component message = action.run(player, data);
        NetworkHandler.syncTo(player);
        if (message != null) {
            ctx.getSource().sendSuccess(() -> message, true);
        }
        return 1;
    }

    private static List<String> classIds() {
        return Arrays.stream(PlayerClass.values())
                .filter(c -> c != PlayerClass.NONE)
                .map(PlayerClass::id)
                .toList();
    }

    // ------------------------------------------------------------------
    // Действия
    // ------------------------------------------------------------------

    private static int help(CommandContext<CommandSourceStack> ctx) {
        for (int i = 0; i <= 16; i++) {
            final int line = i;
            ctx.getSource().sendSuccess(() -> Component.translatable("command.tokyoghoulrpg.help." + line), false);
        }
        return 1;
    }

    private static Component info(ServerPlayer player, PlayerData data) {
        Quest quest = Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
        Component questLine = quest == null
                ? Component.translatable("command.tokyoghoulrpg.info.no_quest")
                : Component.translatable("command.tokyoghoulrpg.info.quest", quest.title(),
                        Math.min(data.getQuestProgress(), quest.goal()), quest.goal());
        return Component.translatable("command.tokyoghoulrpg.info", player.getDisplayName())
                .append("\n").append(Component.translatable("command.tokyoghoulrpg.info.class",
                        Component.translatable(data.getPlayerClass().translationKey()),
                        Component.translatable(data.getKaguneVariant() == KaguneVariant.NONE
                                ? "command.tokyoghoulrpg.info.no_variant" : data.getKaguneVariant().translationKey())))
                .append("\n").append(Component.translatable("command.tokyoghoulrpg.info.points",
                        data.getSkillPoints(), data.getBloodPoints()))
                .append("\n").append(Component.translatable("command.tokyoghoulrpg.info.rage",
                        data.getRage(), PlayerData.RAGE_MAX, data.getRageLevel()))
                .append("\n").append(Component.translatable("command.tokyoghoulrpg.info.skills",
                        data.getSkills().size(), SkillTree.totalCost(data.getPlayerClass())))
                .append("\n").append(questLine);
    }

    private static Component setClass(ServerPlayer player, PlayerData data, PlayerClass target) {
        if (target == PlayerClass.NONE) {
            return Component.translatable("command.tokyoghoulrpg.class.unknown");
        }
        if (data.getPlayerClass() == target) {
            return Component.translatable("command.tokyoghoulrpg.class.same", player.getDisplayName());
        }
        if (!data.hasChosenClass()) {
            // Класса ещё не было: как выбор в меню
            data.setPlayerClass(target);
            SelectClassC2S.giveStartBlood(data);
            AbilityManager.applyClassStats(player, data, true);
            if (target.hasKagune()) {
                NetworkHandler.openVariantSelection(player);
            }
        } else {
            // Как инъекция: навыки чужого дерева возвращаются очками, задания своей цепочки сохраняются
            ClassChange.change(player, data, target);
        }
        return Component.translatable("command.tokyoghoulrpg.class", player.getDisplayName(),
                Component.translatable(target.translationKey()));
    }

    private static Component setVariant(PlayerData data, ServerPlayer player, KaguneVariant variant) {
        if (!data.getPlayerClass().hasKagune() || variant == KaguneVariant.NONE) {
            return Component.translatable("command.tokyoghoulrpg.variant.no");
        }
        data.setKaguneVariant(variant);
        return Component.translatable("command.tokyoghoulrpg.variant", player.getDisplayName(),
                Component.translatable(variant.translationKey()));
    }

    /** Снять все навыки и вернуть потраченные очки. */
    private static Component resetSkills(ServerPlayer player, PlayerData data) {
        int refund = 0;
        for (String id : List.copyOf(data.getSkills())) {
            SkillTree.Skill skill = SkillTree.find(id);
            if (skill != null) {
                refund += skill.cost();
            }
            data.removeSkill(id);
        }
        data.addSkillPoints(refund);
        data.setRageLevel(0);
        return Component.translatable("command.tokyoghoulrpg.skills.reset", player.getDisplayName(), refund);
    }

    /** Открыть бесплатно все навыки дерева своего класса. */
    private static Component unlockAllSkills(ServerPlayer player, PlayerData data) {
        int opened = 0;
        for (SkillTree.Tab tab : SkillTree.tabs(data.getPlayerClass())) {
            for (SkillTree.Branch branch : SkillTree.branches(tab)) {
                for (SkillTree.Skill skill : SkillTree.branch(branch)) {
                    if (!data.hasSkill(skill.id())) {
                        data.addSkill(skill.id());
                        opened++;
                    }
                    if (branch == SkillTree.Branch.RAGE) {
                        data.setRageLevel(Math.max(data.getRageLevel(), skill.tier()));
                    }
                }
            }
        }
        return Component.translatable("command.tokyoghoulrpg.skills.unlockall", player.getDisplayName(), opened);
    }

    private static Component completeQuest(ServerPlayer player, PlayerData data) {
        return QuestManager.completeCurrent(player)
                ? Component.translatable("command.tokyoghoulrpg.quests.complete", player.getDisplayName())
                : Component.translatable("command.tokyoghoulrpg.quests.none");
    }

    private static Component resetQuests(ServerPlayer player, PlayerData data) {
        data.setQuestIndex(0);
        data.setQuestProgress(0);
        return Component.translatable("command.tokyoghoulrpg.quests.reset", player.getDisplayName());
    }
}
