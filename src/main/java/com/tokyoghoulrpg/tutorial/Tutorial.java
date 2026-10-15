package com.tokyoghoulrpg.tutorial;

import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.TutorialC2S;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Подсказки для новичков. Появляются справа на экране одна за другой, сразу после выбора класса.
 * Каждая видна несколько секунд (пока открыто меню, время подсказки стоит), потом следующая.
 * Набор подсказок свой у каждого класса. Прогресс хранится у игрока; в «Профиле» подсказки
 * можно пропустить или посмотреть заново.
 * <p>
 * В тексте подсказки вместо %s подставляются клавиши (по названию привязки), поэтому если
 * игрок переназначил клавишу, подсказка покажет новую.
 */
public final class Tutorial {
    /** Ключ текста и названия привязок клавиш для %s по порядку. */
    public record Hint(String key, String... keys) {
        public String translationKey() {
            return "tutorial.tokyoghoulrpg." + key;
        }
    }

    private static final String MOD = "key.tokyoghoulrpg.";

    private Tutorial() {
    }

    public static List<Hint> hints(PlayerClass cls) {
        List<Hint> list = new ArrayList<>();
        list.add(new Hint("welcome", MOD + "profile"));
        if (cls.hasKagune()) {
            list.add(new Hint("kagune", MOD + "toggle_kagune", "key.attack", "key.use"));
            list.add(new Hint("kagune_moves", MOD + "dash", MOD + "block", MOD + "barrage"));
            list.add(new Hint("kagune_grab", MOD + "grab", "key.attack", MOD + "grab", "key.use"));
            list.add(new Hint("ghoul_food", MOD + "bite"));
            list.add(new Hint("ghoul_rage", MOD + "rage", MOD + "kakuja"));
            list.add(new Hint("ghoul_type", MOD + "profile"));
        } else if (cls == PlayerClass.CCG_AGENT) {
            list.add(new Hint("ccg_quinque", "key.attack", "key.use", "key.sneak", "key.attack"));
            list.add(new Hint("ccg_moves", MOD + "barrage", MOD + "block", MOD + "rage"));
            list.add(new Hint("ccg_dash", MOD + "dash"));
        } else {
            list.add(new Hint("human_moves", MOD + "dash", "key.use"));
            list.add(new Hint("human_growth"));
        }
        list.add(new Hint("skills", MOD + "skill_tree"));
        list.add(new Hint("quests", MOD + "quests", MOD + "clan"));
        list.add(new Hint("world"));
        list.add(new Hint("end", MOD + "profile"));
        return list;
    }

    public static int count(PlayerClass cls) {
        return hints(cls).size();
    }

    /** Действие от клиента: следующая подсказка, начать заново, пропустить всё. */
    public static void action(ServerPlayer player, int action, int step) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        switch (action) {
            case TutorialC2S.NEXT -> {
                // клиент сообщает, какую подсказку показал: так повторный пакет не перескочит через одну
                if (data.isTutorialDone() || step != data.getTutorialStep()) {
                    return;
                }
                int next = step + 1;
                data.setTutorial(next, next >= count(data.getPlayerClass()));
            }
            case TutorialC2S.RESTART -> data.setTutorial(0, false);
            case TutorialC2S.SKIP -> data.setTutorial(0, true);
            default -> {
                return;
            }
        }
        NetworkHandler.syncTo(player);
    }
}
