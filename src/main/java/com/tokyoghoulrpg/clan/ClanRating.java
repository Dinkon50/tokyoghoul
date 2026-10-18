package com.tokyoghoulrpg.clan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.MinecraftServer;

/**
 * Рейтинг кланов сервера. Очки: уровень x1000, победа в войне 300, клановый босс 400,
 * задание клана 100, участник 50. Клан на первом месте (если кланов хотя бы два) получает
 * +5% очков крови за убийства для всех участников, пока держится наверху.
 * Очки за участников небольшие, а за дела — большие: маленький активный клан может обогнать большой.
 */
public final class ClanRating {
    public static final double TOP_BONUS = 0.05;

    private ClanRating() {
    }

    public static int score(ClanData.Clan clan) {
        long s = clan.level * 1000L + clan.warWins * 300L + clan.bossKills * 400L + clan.questsDone * 100L
                + clan.members.size() * 50L;
        return (int) Math.min(Integer.MAX_VALUE, s);
    }

    /** Кланы по убыванию очков. */
    public static List<ClanData.Clan> ranked(MinecraftServer server) {
        List<ClanData.Clan> list = new ArrayList<>(ClanData.get(server).all());
        list.sort(Comparator.comparingInt(ClanRating::score).reversed());
        return list;
    }

    /** Этот клан сейчас первый (и кланов больше одного). */
    public static boolean isTop(MinecraftServer server, ClanData.Clan clan) {
        List<ClanData.Clan> list = ranked(server);
        return list.size() >= 2 && list.get(0) == clan && score(clan) > score(list.get(1));
    }
}
