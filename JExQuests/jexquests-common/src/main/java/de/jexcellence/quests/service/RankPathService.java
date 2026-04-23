package de.jexcellence.quests.service;

import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.core.api.reward.RewardContext;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import de.jexcellence.quests.database.repository.PlayerRankRepository;
import de.jexcellence.quests.database.repository.RankRepository;
import de.jexcellence.quests.database.repository.RankTreeRepository;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Tree-level selection service — the RDQ-parity layer for choosing,
 * switching, and gating active rank paths. The existing
 * {@link RankService} covers rank-within-a-tree promotion; this
 * service is strictly about which tree a player is progressing on.
 *
 * <p>Selection rules (mirrored from RDQ):
 * <ul>
 *   <li><b>Tree prerequisites</b> — a tree with non-empty
 *       {@link RankTree#prerequisiteTreeList()} is only selectable
 *       once the player has completed every listed tree.</li>
 *   <li><b>Minimum completed trees</b> — a tree with
 *       {@code minimumTreesToBeDone > 0} requires that many
 *       {@code treeCompleted=true} rows across the player's set.</li>
 *   <li><b>Switch cooldown</b> — switching away from an active tree
 *       is blocked until {@code switchCooldownSeconds} has elapsed
 *       since the last switch.</li>
 * </ul>
 */
public class RankPathService {

    private static final String SOURCE = "JExQuests";

    private final RankTreeRepository trees;
    private final RankRepository ranks;
    private final PlayerRankRepository playerRanks;
    private final QuestsPlayerService questsPlayers;
    private final JExLogger logger;

    public RankPathService(
            @NotNull RankTreeRepository trees,
            @NotNull RankRepository ranks,
            @NotNull PlayerRankRepository playerRanks,
            @NotNull QuestsPlayerService questsPlayers,
            @NotNull JExLogger logger
    ) {
        this.trees = trees;
        this.ranks = ranks;
        this.playerRanks = playerRanks;
        this.questsPlayers = questsPlayers;
        this.logger = logger;
    }

    /**
     * Select a tree as the player's active path. Creates the
     * {@link PlayerRank} row at the initial rank, sets
     * {@link QuestsPlayer#setActiveRankTree} and marks every other
     * row {@code active=false}. Idempotent — calling with the
     * already-active tree is a no-op success.
     */
    public @NotNull CompletableFuture<SelectResult> selectRankPath(
            @NotNull UUID playerUuid, @NotNull String treeIdentifier
    ) {
        return this.trees.findByIdentifierAsync(treeIdentifier).thenCompose(optTree -> {
            if (optTree.isEmpty()) return CompletableFuture.completedFuture(SelectResult.notFound());
            final RankTree tree = optTree.get();
            if (!tree.isEnabled()) return CompletableFuture.completedFuture(SelectResult.disabled());
            return this.playerRanks.findByPlayerAsync(playerUuid).thenCompose(allRows ->
                    gateAndSelect(playerUuid, tree, allRows));
        }).exceptionally(ex -> {
            this.logger.error("selectRankPath failed for {}/{}: {}", playerUuid, treeIdentifier, ex.getMessage());
            return SelectResult.error(ex.getMessage());
        });
    }

    private @NotNull CompletableFuture<SelectResult> gateAndSelect(
            @NotNull UUID playerUuid, @NotNull RankTree tree, @NotNull List<PlayerRank> allRows
    ) {
        final PlayerRank existing = findRow(allRows, tree.getIdentifier());
        if (existing != null && existing.isActive()) {
            return CompletableFuture.completedFuture(SelectResult.alreadyActive());
        }
        final int completedCount = (int) allRows.stream().filter(PlayerRank::isTreeCompleted).count();
        if (tree.getMinimumTreesToBeDone() > completedCount) {
            return CompletableFuture.completedFuture(
                    SelectResult.minimumNotMet(tree.getMinimumTreesToBeDone(), completedCount));
        }
        for (final String prereqId : tree.prerequisiteTreeList()) {
            final PlayerRank prereqRow = findRow(allRows, prereqId);
            if (prereqRow == null || !prereqRow.isTreeCompleted()) {
                return CompletableFuture.completedFuture(SelectResult.prerequisiteMissing(prereqId));
            }
        }
        return this.ranks.findByTreeAsync(tree).thenCompose(rankList -> {
            final Rank initial = resolveInitialRank(rankList);
            if (initial == null) return CompletableFuture.completedFuture(SelectResult.noInitialRank());
            return applySelection(playerUuid, tree, allRows, existing, initial);
        });
    }

    private @NotNull CompletableFuture<SelectResult> applySelection(
            @NotNull UUID playerUuid, @NotNull RankTree tree, @NotNull List<PlayerRank> allRows,
            PlayerRank existing, @NotNull Rank initial
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Deactivate every other tree for this player first.
            for (final PlayerRank row : allRows) {
                if (row.isActive()) {
                    row.setActive(false);
                    this.playerRanks.update(row);
                }
            }
            final boolean isFreshEnrolment;
            final PlayerRank target;
            if (existing != null) {
                existing.setActive(true);
                existing.setLastSwitchedAt(LocalDateTime.now());
                target = this.playerRanks.update(existing);
                isFreshEnrolment = false;
            } else {
                final PlayerRank fresh = new PlayerRank(playerUuid, tree.getIdentifier(), initial.getIdentifier());
                fresh.setActive(true);
                target = this.playerRanks.create(fresh);
                isFreshEnrolment = true;
            }
            this.questsPlayers.findAsync(playerUuid).thenAccept(optProfile ->
                    optProfile.ifPresent(profile -> {
                        profile.setActiveRankTree(tree.getIdentifier());
                        this.questsPlayers.repository().update(profile);
                    }));

            // Grant the initial rank's reward on a *fresh* enrolment only —
            // reactivating a previously-selected path must not double-grant
            // a reward the player already claimed on first selection.
            if (isFreshEnrolment) {
                grantReward(playerUuid, initial);
            }
            return SelectResult.selected(tree.getIdentifier(), target.getCurrentRankIdentifier());
        });
    }

    /**
     * Grants the JSON-encoded {@link Reward} stored on a rank, if any, via
     * the JExCore {@link RewardExecutor} service. Called on fresh path
     * enrolment so the starting rank's goodies actually reach the player
     * — otherwise the new enrollee would see nothing happen after clicking
     * "select path", which looks like a broken GUI.
     */
    private void grantReward(@NotNull UUID playerUuid, @NotNull Rank rank) {
        final Reward reward = RewardRequirementCodec.decodeReward(rank.getRewardData());
        if (reward == null) return;
        final RewardExecutor executor = RewardExecutor.get();
        if (executor == null) {
            this.logger.warn("RewardExecutor unavailable; initial-rank reward skipped for {}", playerUuid);
            return;
        }
        executor.grantSync(reward, new RewardContext(playerUuid, SOURCE, "rank-select-initial"));
    }

    /**
     * Switch the active tree from the player's current active one to
     * {@code newTreeIdentifier}. Honours the source tree's switch
     * cooldown + target tree's prerequisites.
     */
    public @NotNull CompletableFuture<SelectResult> switchRankPath(
            @NotNull UUID playerUuid, @NotNull String newTreeIdentifier
    ) {
        return this.playerRanks.findByPlayerAsync(playerUuid).thenCompose(allRows -> {
            final PlayerRank currentActive = allRows.stream()
                    .filter(PlayerRank::isActive)
                    .findFirst().orElse(null);
            if (currentActive == null) {
                // No active path — delegate to plain select.
                return selectRankPath(playerUuid, newTreeIdentifier);
            }
            if (currentActive.getTreeIdentifier().equals(newTreeIdentifier)) {
                return CompletableFuture.completedFuture(SelectResult.alreadyActive());
            }
            return this.trees.findByIdentifierAsync(currentActive.getTreeIdentifier()).thenCompose(optSrc -> {
                if (optSrc.isEmpty()) return selectRankPath(playerUuid, newTreeIdentifier);
                final RankTree source = optSrc.get();
                if (!source.isAllowSwitching()) {
                    return CompletableFuture.completedFuture(SelectResult.switchingLocked(source.getIdentifier()));
                }
                final long remaining = cooldownRemaining(currentActive, source);
                if (remaining > 0L) {
                    return CompletableFuture.completedFuture(SelectResult.onCooldown(remaining));
                }
                return selectRankPath(playerUuid, newTreeIdentifier);
            });
        }).exceptionally(ex -> {
            this.logger.error("switchRankPath failed: {}", ex.getMessage());
            return SelectResult.error(ex.getMessage());
        });
    }

    /**
     * Predicate — can the player currently select this tree? Doesn't
     * mutate state, just runs the same gate checks. Useful for view
     * classification.
     */
    public @NotNull CompletableFuture<Availability> availabilityAsync(
            @NotNull UUID playerUuid, @NotNull String treeIdentifier
    ) {
        return this.trees.findByIdentifierAsync(treeIdentifier).thenCombine(
                this.playerRanks.findByPlayerAsync(playerUuid),
                (optTree, allRows) -> {
                    if (optTree.isEmpty()) return Availability.NOT_FOUND;
                    final RankTree tree = optTree.get();
                    if (!tree.isEnabled()) return Availability.DISABLED;
                    final PlayerRank existing = findRow(allRows, tree.getIdentifier());
                    if (existing != null && existing.isTreeCompleted()) return Availability.COMPLETED;
                    if (existing != null && existing.isActive()) return Availability.CURRENTLY_ACTIVE;
                    if (existing != null) return Availability.PREVIOUSLY_SELECTED;

                    final int completedCount = (int) allRows.stream().filter(PlayerRank::isTreeCompleted).count();
                    if (tree.getMinimumTreesToBeDone() > completedCount) return Availability.LOCKED;
                    for (final String prereqId : tree.prerequisiteTreeList()) {
                        final PlayerRank prereqRow = findRow(allRows, prereqId);
                        if (prereqRow == null || !prereqRow.isTreeCompleted()) return Availability.LOCKED;
                    }
                    return Availability.AVAILABLE;
                }
        ).exceptionally(ex -> Availability.NOT_FOUND);
    }

    private static long cooldownRemaining(@NotNull PlayerRank row, @NotNull RankTree tree) {
        if (tree.getSwitchCooldownSeconds() <= 0L) return 0L;
        final LocalDateTime last = row.getLastSwitchedAt() != null
                ? row.getLastSwitchedAt() : row.getSelectedAt();
        final long elapsed = Duration.between(last, LocalDateTime.now()).toSeconds();
        return Math.max(0L, tree.getSwitchCooldownSeconds() - elapsed);
    }

    private static @NotNull Rank resolveInitialRank(@NotNull List<Rank> ordered) {
        return ordered.stream()
                .filter(Rank::isInitialRank)
                .findFirst()
                .orElseGet(() -> ordered.stream()
                        .min(Comparator.comparingInt(Rank::getOrderIndex))
                        .orElse(null));
    }

    private static PlayerRank findRow(@NotNull List<PlayerRank> rows, @NotNull String treeIdentifier) {
        for (final PlayerRank row : rows) if (row.getTreeIdentifier().equals(treeIdentifier)) return row;
        return null;
    }

    /** Five-way availability classification used by views. */
    public enum Availability {
        AVAILABLE,
        LOCKED,
        CURRENTLY_ACTIVE,
        PREVIOUSLY_SELECTED,
        COMPLETED,
        DISABLED,
        NOT_FOUND
    }

    public record SelectResult(
            @NotNull Status status,
            @org.jetbrains.annotations.Nullable String treeIdentifier,
            @org.jetbrains.annotations.Nullable String rankIdentifier,
            @org.jetbrains.annotations.Nullable String detail,
            long cooldownSecondsRemaining
    ) {
        public enum Status {
            SELECTED, ALREADY_ACTIVE, LOCKED_MINIMUM, LOCKED_PREREQUISITE,
            NO_INITIAL_RANK, SWITCHING_LOCKED, ON_COOLDOWN, DISABLED, NOT_FOUND, ERROR
        }

        public static @NotNull SelectResult selected(@NotNull String tree, @NotNull String rank) {
            return new SelectResult(Status.SELECTED, tree, rank, null, 0L);
        }
        public static @NotNull SelectResult alreadyActive() {
            return new SelectResult(Status.ALREADY_ACTIVE, null, null, null, 0L);
        }
        public static @NotNull SelectResult minimumNotMet(int required, int actual) {
            return new SelectResult(Status.LOCKED_MINIMUM, null, null,
                    "needs " + required + " completed trees, have " + actual, 0L);
        }
        public static @NotNull SelectResult prerequisiteMissing(@NotNull String treeId) {
            return new SelectResult(Status.LOCKED_PREREQUISITE, null, null,
                    "must complete tree '" + treeId + "' first", 0L);
        }
        public static @NotNull SelectResult noInitialRank() {
            return new SelectResult(Status.NO_INITIAL_RANK, null, null, "tree has no initial rank", 0L);
        }
        public static @NotNull SelectResult switchingLocked(@NotNull String treeId) {
            return new SelectResult(Status.SWITCHING_LOCKED, null, null,
                    "tree '" + treeId + "' disallows switching away", 0L);
        }
        public static @NotNull SelectResult onCooldown(long remaining) {
            return new SelectResult(Status.ON_COOLDOWN, null, null, null, remaining);
        }
        public static @NotNull SelectResult disabled() {
            return new SelectResult(Status.DISABLED, null, null, null, 0L);
        }
        public static @NotNull SelectResult notFound() {
            return new SelectResult(Status.NOT_FOUND, null, null, null, 0L);
        }
        public static @NotNull SelectResult error(@NotNull String detail) {
            return new SelectResult(Status.ERROR, null, null, detail, 0L);
        }
    }
}
