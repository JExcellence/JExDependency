package de.jexcellence.quests.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Durable, plugin-agnostic snapshot of a single player's JExQuests
 * state. Produced by {@code /jexquests export <player>} and consumed
 * by {@code /jexquests import <file>} for cross-server migration —
 * the schema is versioned so future format changes can be handled
 * backward-compatibly.
 *
 * @param schemaVersion bump when the record shape changes
 * @param exportedAt when the snapshot was produced
 * @param playerUuid subject player
 * @param playerName last-seen name for display purposes (may be stale)
 * @param sourcePlugin always {@code "JExQuests"}; keeps the envelope
 *                     self-identifying if multiple ecosystems share
 *                     an export format
 * @param profile per-player toggles + sidebar / rank state
 * @param questProgress every quest attempt (active / completed / abandoned)
 * @param taskProgress every seeded task row
 * @param ranks per-tree rank position
 * @param perks owned perk rows with toggle state + cooldown clocks
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PlayerSnapshot(
        int schemaVersion,
        @NotNull LocalDateTime exportedAt,
        @NotNull UUID playerUuid,
        @NotNull String playerName,
        @NotNull String sourcePlugin,
        @Nullable String sourceServer,
        @Nullable String integrityHash,
        @NotNull Profile profile,
        @NotNull List<QuestProgress> questProgress,
        @NotNull List<TaskProgress> taskProgress,
        @NotNull List<RankState> ranks,
        @NotNull List<PerkOwnership> perks
) {

    /**
     * Current schema version. Version 2 introduced
     * {@link #sourceServer} (shard self-identification) and
     * {@link #integrityHash} (tamper detection). Readers should accept
     * {@code schemaVersion <= CURRENT_SCHEMA} and treat missing fields
     * from older versions as {@code null}.
     */
    public static final int CURRENT_SCHEMA = 2;

    /** Returns a copy with {@link #integrityHash} replaced. */
    public @NotNull PlayerSnapshot withIntegrityHash(@Nullable String hash) {
        return new PlayerSnapshot(
                this.schemaVersion, this.exportedAt, this.playerUuid, this.playerName,
                this.sourcePlugin, this.sourceServer, hash,
                this.profile, this.questProgress, this.taskProgress, this.ranks, this.perks);
    }

    /**
     * Per-player toggles + sidebar / rank state from the
     * {@code jexquests_player} row.
     */
    public record Profile(
            boolean questSidebarEnabled,
            boolean perkSidebarEnabled,
            @Nullable String trackedQuestIdentifier,
            @Nullable String activeRankTree
    ) {
    }

    /**
     * One row in {@code jexquests_player_quest_progress}.
     */
    public record QuestProgress(
            @NotNull String questIdentifier,
            @NotNull String status,
            int currentTaskIndex,
            int completionCount,
            @Nullable LocalDateTime startedAt,
            @Nullable LocalDateTime completedAt,
            @Nullable LocalDateTime expiresAt,
            @NotNull LocalDateTime lastUpdatedAt
    ) {
    }

    /**
     * One row in {@code jexquests_player_task_progress}.
     */
    public record TaskProgress(
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier,
            long progress,
            long target,
            boolean completed,
            @NotNull LocalDateTime lastUpdatedAt
    ) {
    }

    /**
     * One row in {@code jexquests_player_rank}.
     */
    public record RankState(
            @NotNull String treeIdentifier,
            @NotNull String currentRankIdentifier,
            @NotNull LocalDateTime promotedAt,
            int progressionPercent,
            boolean treeCompleted,
            @Nullable LocalDateTime treeCompletedAt
    ) {
    }

    /**
     * One row in {@code jexquests_player_perk}.
     */
    public record PerkOwnership(
            @NotNull String perkIdentifier,
            boolean enabled,
            @NotNull LocalDateTime unlockedAt,
            @Nullable LocalDateTime lastActivatedAt,
            long activationCount
    ) {
    }
}
