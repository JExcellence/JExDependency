/*


package com.raindropcentral.rdq.placeholder;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.Common.Quest;
import com.raindropcentral.rdq.Common.RAdvancement;
import com.raindropcentral.rdq.Common.Rank;
import com.raindropcentral.rdq.Hibernate.Core.RStats;
import com.raindropcentral.rdq.PluginData;
import com.raindropcentral.rcore.RCoreImpl;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;

*/
/**
 * Utility class for handling RDQImpl-related placeholders.
 * Similar in structure to CurrencyPlaceholderUtil.
 *//*

public class RDQPlaceholderUtil {

    private final RDQImpl plugin;

    public RDQPlaceholderUtil(@NotNull RDQImpl plugin) {
        this.plugin = plugin;
    }

    */
/**
 * Processes a placeholder for a specific player.
 *
 * @param player The player (may be null)
 * @param params The placeholder parameters
 * @return The processed placeholder value or null if not applicable
 *//*

    public @Nullable String processPlaceholder(@Nullable Player player, @NotNull String params) {
        if (params.startsWith("achievements_")) {
            String[] parts = params.split("_");
            if (parts.length == 2) {
                // e.g., "achievements_achievementId"
                return getAchievementInfo(parts[1], "id");
            } else if (parts.length == 3 && parts[2].equalsIgnoreCase("title")) {
                // e.g., "achievements_achievementId_title"
                return getAchievementInfo(parts[1], "title");
            }
        }
        if (params.startsWith("advancement_")) {
            String[] parts = params.split("_");
            if (parts.length >= 3) {
                return getAdvancementInfo(parts[1], parts[2]);
            }
        }
        if (params.startsWith("quest_")) {
            String[] parts = params.split("_");
            if (parts.length == 3) {
                return getQuestInfo(parts[1], parts[2]);
            }
        }
        if (params.startsWith("rank_")) {
            String[] parts = params.split("_");
            if (parts.length == 3) {
                return getRankInfo(parts[1], parts[2]);
            }
        }
        if (params.startsWith("skill")) {
            if (RCoreImpl.skills && player != null) {
                return String.valueOf(RCoreImpl.getInstance().getSkillsAPI().getTotalSkillLevel(player));
            }
        }
        if (params.startsWith("job")) {
            if (RCoreImpl.jobs && player != null) {
                return String.valueOf(RCoreImpl.getInstance().getJobsAPI().getTotalJobLevel(player));
            }
        }
        if (params.startsWith("stat_")) {
            String[] parts = params.split("_");
            if (parts.length >= 3 && player != null) {
                return getStatInfo(player, parts);
            }
        }
        return null;
    }

    private @Nullable String getAchievementInfo(@NotNull String achievementId, @NotNull String infoType) {
        // Extend this based on how achievements are structured in RDQImpl/PluginData.
        // Assuming achievementId maps to some Advancement for demonstration.
        RAdvancement adv = PluginData.getAdvancements().get(achievementId);
        if (adv == null) return null;

        return switch (infoType.toLowerCase()) {
            case "title" -> adv.getTitle();
            case "id" -> adv.getIdentifier();
            default -> null;
        };
    }

    private @Nullable String getAdvancementInfo(@NotNull String advancementId, @NotNull String infoType) {
        RAdvancement adv = PluginData.getAdvancements().get(advancementId);
        if (adv == null) return null;
        return switch (infoType.toLowerCase()) {
            case "title" -> adv.getTitle();
            case "lore" -> adv.getLore();
            default -> null;
        };
    }

    private @Nullable String getQuestInfo(@NotNull String questId, @NotNull String infoType) {
        Quest quest = PluginData.getQuests().get(questId);
        if (quest == null) return null;
        return switch (infoType.toLowerCase()) {
            case "title" -> quest.getTitle();
            // Add more fields if available on Quest
            default -> null;
        };
    }

    private @Nullable String getRankInfo(@NotNull String rankId, @NotNull String infoType) {
        Rank rank = PluginData.getRanks().get(rankId);
        if (rank == null) return null;
        return switch (infoType.toLowerCase()) {
            case "title" -> rank.getTitle();
            // Add more as needed
            default -> null;
        };
    }

    private @Nullable String getStatInfo(@NotNull Player player, @NotNull String[] parts) {
        RStats rStats = PluginData.getRStatsDao().getPlayerStats(player);
        if (rStats == null) return null;
        // Example: stat_bounties_total
        if (parts.length >= 3) {
            String mainType = parts[1].toLowerCase();
            String subType = parts[2].toLowerCase();
            String statKey = switch (mainType) {
                case "bounty", "bounties" -> subType.equals("total") ? "bounties_total" : null;
                case "longest" -> "longest";
                case "perks" -> subType.equals("total") ? "perks_total" : parts[2];
                case "quest", "quests" -> switch (subType) {
                    case "total" -> "quests_total";
                    case "daily" -> "quests_daily_total";
                    case "weekly" -> "quests_weekly_total";
                    default -> parts[2];
                };
                case "machine", "machines" -> subType.equals("total") ? "machines_total" : null;
                case "fab", "fabricator", "fabricators" -> subType.equals("total") ? "fabricators_total" : null;
                case "collector", "collectors" -> subType.equals("total") ? "collectors_total" : null;
                case "raindrop", "raindrops" -> subType.equals("earned") ? "raindrops_earned" : null;
                case "water" -> subType.equals("gathered") ? "water_gathered" : null;
                case "lava" -> switch (subType) {
                    case "gathered" -> "lava_gathered";
                    case "deaths" -> "lava_deaths";
                    default -> null;
                };
                default -> null;
            };
            if (statKey != null) {
                return BigDecimal.valueOf(rStats.getStatValue(statKey)).stripTrailingZeros().toPlainString();
            }
        }
        return null;
    }
}
*/
