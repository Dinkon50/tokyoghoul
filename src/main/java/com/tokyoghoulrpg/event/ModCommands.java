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
import com.tokyoghoulrpg.difficulty.DifficultyScaling;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.raid.CcgRaids;
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
    private static boolean SILENT = false;

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
                        .executes(ctx -> showInfo(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(OP))
                                .executes(ctx -> showInfo(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(amount("points", 100000, (op, n) -> (player, data) -> {
                    data.setSkillPoints(apply(op, data.getSkillPoints(), n));
                    return null;
                }))
                .then(amount("blood", 10000000, (op, n) -> (player, data) -> {
                    data.setBloodPoints(apply(op, data.getBloodPoints(), n));
                    return null;
                }))
                .then(Commands.literal("rage").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("value", IntegerArgumentType.integer(0, PlayerData.RAGE_MAX)),
                                ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    return (player, data) -> {
                                        data.setRage(value);
                                        return null;
                                    };
                                })))
                .then(Commands.literal("ragelevel").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.argument("level", IntegerArgumentType.integer(0, Balance.RAGE_LEVEL_MAX)),
                                ctx -> {
                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                    return (player, data) -> {
                                        data.setRageLevel(level);
                                        return null;
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
                                        return null;
                                    };
                                })))
                .then(Commands.literal("contract").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.literal("reset"), ctx -> (player, data) -> {
                            data.setContract(null, 0);
                            data.setContractDoneDay(-1L);
                            return null;
                        })))
                .then(Commands.literal("rep").requires(src -> src.hasPermission(OP))
                        .then(Commands.literal("ccg")
                                .then(withTarget(Commands.argument("value", IntegerArgumentType.integer(-100, 100)),
                                        ctx -> {
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            return (player, data) -> {
                                                data.setRepCcg(value);
                                                return null;
                                            };
                                        })))
                        .then(Commands.literal("ghouls")
                                .then(withTarget(Commands.argument("value", IntegerArgumentType.integer(-100, 100)),
                                        ctx -> {
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            return (player, data) -> {
                                                data.setRepGhouls(value);
                                                return null;
                                            };
                                        }))))
                .then(withTarget(Commands.literal("raid").requires(src -> src.hasPermission(OP)), ctx -> (player, data) -> {
                    // отряд приходит сразу, даже если игрока сейчас не ищут; следующий рейд по обычному расписанию
                    return CcgRaids.start(player) ? null : Component.translatable("command.tokyoghoulrpg.raid.no_place");
                }))
                .then(Commands.literal("cooldowns").requires(src -> src.hasPermission(OP))
                        .then(withTarget(Commands.literal("reset"), ctx -> (player, data) -> {
                            AbilityManager.resetCooldowns(player);
                            return null;
                        })))
                .then(withTarget(Commands.literal("reset").requires(src -> src.hasPermission(OP)), ctx -> (player, data) -> {
                    AbilityManager.resetAll(player);
                    data.reset();
                    AbilityManager.applyClassStats(player, data, false);
                    NetworkHandler.openClassSelection(player);
                    return null;
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
        // Команды работают молча: в чат ничего не пишется. Сообщение возвращают только ошибки
        // (неизвестный класс, нет задания и т. п.), их видит только тот, кто ввёл команду.
        Component error;
        SILENT = true;
        try {
            error = action.run(player, data);
        } finally {
            SILENT = false;
        }
        NetworkHandler.syncTo(player);
        if (error != null) {
            ctx.getSource().sendFailure(error);
            return 0;
        }
        return 1;
    }

    /** /tgrpg info: единственная команда (кроме help), которая что-то показывает, потому что это её смысл. */
    private static int showInfo(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return 0;
        }
        Component text = info(player, data);
        ctx.getSource().sendSuccess(() -> text, false);
        return 1;
    }

    /** Пока выполняется команда мода: сообщения о наградах и смене класса не пишутся в чат. */
    public static boolean isSilent() {
        return SILENT;
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
        for (int i = 0; i <= 18; i++) {
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
                        data.getSkills().size(), SkillTree.totalCost(data.getPlayerClass(), player.level().getDifficulty())))
                .append("\n").append(questLine)
                .append("\n").append(Component.translatable("command.tokyoghoulrpg.info.rep",
                        data.getRepCcg(), data.getRepGhouls()))
                .append("\n").append(Component.translatable("command.tokyoghoulrpg.info.difficulty",
                        player.level().getDifficulty().getDisplayName(),
                        DifficultyScaling.describe(player.level())));
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
        return null;
    }

    private static Component setVariant(PlayerData data, ServerPlayer player, KaguneVariant variant) {
        if (!data.getPlayerClass().hasKagune() || variant == KaguneVariant.NONE) {
            return Component.translatable("command.tokyoghoulrpg.variant.no");
        }
        data.setKaguneVariant(variant);
        return null;
    }

    /** Снять все навыки и вернуть потраченные очки. */
    private static Component resetSkills(ServerPlayer player, PlayerData data) {
        int refund = 0;
        for (String id : List.copyOf(data.getSkills())) {
            SkillTree.Skill skill = SkillTree.find(id);
            if (skill != null) {
                refund += data.paidFor(id, skill.cost());
            }
            data.removeSkill(id);
        }
        data.addSkillPoints(refund);
        data.setRageLevel(0);
        return null;
    }

    /** Открыть бесплатно все навыки дерева своего класса. */
    private static Component unlockAllSkills(ServerPlayer player, PlayerData data) {
        int opened = 0;
        for (SkillTree.Tab tab : SkillTree.tabs(data.getPlayerClass())) {
            for (SkillTree.Branch branch : SkillTree.branches(tab)) {
                for (SkillTree.Skill skill : SkillTree.branch(branch)) {
                    if (!data.hasSkill(skill.id())) {
                        data.addSkill(skill.id(), 0);   // бесплатно: при сбросе очки не вернутся
                        opened++;
                    }
                    if (branch == SkillTree.Branch.RAGE) {
                        data.setRageLevel(Math.max(data.getRageLevel(), skill.tier()));
                    }
                }
            }
        }
        return null;
    }

    private static Component completeQuest(ServerPlayer player, PlayerData data) {
        return QuestManager.completeCurrent(player) ? null : Component.translatable("command.tokyoghoulrpg.quests.none");
    }

    private static Component resetQuests(ServerPlayer player, PlayerData data) {
        data.setQuestIndex(0);
        data.setQuestProgress(0);
        data.setFinalQuinques(0);
        return null;
    }
}
