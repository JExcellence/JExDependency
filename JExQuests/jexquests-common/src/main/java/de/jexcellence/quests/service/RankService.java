package de.jexcellence.quests.service;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.core.api.requirement.RequirementContext;
import de.jexcellence.core.api.requirement.RequirementEvaluator;
import de.jexcellence.core.api.requirement.RequirementResult;
import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.core.api.reward.RewardContext;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.api.RankSnapshot;
import de.jexcellence.quests.api.event.RankPromotedEvent;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import de.jexcellence.quests.database.repository.PlayerRankRepository;
import de.jexcellence.quests.database.repository.RankRepository;
import de.jexcellence.quests.database.repository.RankTreeRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Rank lifecycle: join a tree, check promotion eligibility, promote,
 * grant rewards. Gating uses JExCore's shared
 * {@link RequirementEvaluator}; rewards go through {@link RewardExecutor}.
 */
public class RankService {

    private static final String SOURCE = "JExQuests";

    private final RankTreeRepository trees;
    private final RankRepository ranks;
    private final PlayerRankRepository playerRanks;
    private final JExLogger logger;

    public RankService(
            @NotNull RankTreeRepository trees,
            @NotNull RankRepository ranks,
            @NotNull PlayerRankRepository playerRanks,
            @NotNull JExLogger logger
    ) {
        this.trees = trees;
        this.ranks = ranks;
        this.playerRanks = playerRanks;
        this.logger = logger;
    }

    /** Lookup / initialise a player's rank row in the given tree. Joins at the lowest rank. */
    public @NotNull CompletableFuture<PlayerRank> enrollAsync(@NotNull UUID playerUuid, @NotNull String treeIdentifier) {
        return this.trees.findByIdentifierAsync(treeIdentifier).thenCompose(optTree -> {
            if (optTree.isEmpty()) return CompletableFuture.failedFuture(new IllegalArgumentException("no tree: " + treeIdentifier));
            return this.playerRanks.findAsync(playerUuid, treeIdentifier).thenCompose(existing -> {
                if (existing.isPresent()) return CompletableFuture.completedFuture(existing.get());
                return this.ranks.findByTreeAsync(optTree.get()).thenApply(rankList -> {
                    if (rankList.isEmpty()) return null;
                    final Rank initial = rankList.stream()
                            .min(Comparator.comparingInt(Rank::getOrderIndex))
                            .orElseThrow();
                    return this.playerRanks.create(new PlayerRank(playerUuid, treeIdentifier, initial.getIdentifier()));
                });
            });
        }).exceptionally(ex -> {
            this.logger.error("enrol failed: {}", ex.getMessage());
            return null;
        });
    }

    /** Attempt to promote the player on the given tree. */
    public @NotNull CompletableFuture<PromotionResult> promoteAsync(@NotNull UUID playerUuid, @NotNull String treeIdentifier) {
        return this.trees.findByIdentifierAsync(treeIdentifier).thenCombine(
                this.playerRanks.findAsync(playerUuid, treeIdentifier),
                (optTree, optPr) -> new PromotionAttempt(optTree, optPr)
        ).thenCompose(attempt -> attempt.resolve(playerUuid));
    }

    /**
     * Explicit-target promotion — the branching-aware variant. The
     * {@link #promoteAsync} method above picks the next rank by
     * {@code orderIndex + 1}, which is wrong for branching trees where
     * the player needs to pick <em>which</em> sibling to advance into.
     * This overload accepts the clicked rank and validates it:
     * <ul>
     *   <li>target must list the player's current rank in its
     *       {@link Rank#previousRankList()} (direct descendant only)</li>
     *   <li>target's requirement gate must pass</li>
     * </ul>
     */
    public @NotNull CompletableFuture<PromotionResult> promoteToAsync(
            @NotNull UUID playerUuid, @NotNull String treeIdentifier, @NotNull String targetRankIdentifier
    ) {
        return this.trees.findByIdentifierAsync(treeIdentifier).thenCombine(
                this.playerRanks.findAsync(playerUuid, treeIdentifier),
                (optTree, optPr) -> new Object[]{optTree, optPr}
        ).thenCompose(pair -> {
            @SuppressWarnings("unchecked") final Optional<RankTree> optTree = (Optional<RankTree>) pair[0];
            @SuppressWarnings("unchecked") final Optional<PlayerRank> optPr = (Optional<PlayerRank>) pair[1];
            if (optTree.isEmpty()) return CompletableFuture.completedFuture(PromotionResult.notFound());
            if (optPr.isEmpty()) return CompletableFuture.completedFuture(PromotionResult.notEnrolled());

            final RankTree tree = optTree.get();
            final PlayerRank playerRank = optPr.get();
            final String currentId = playerRank.getCurrentRankIdentifier();

            return this.ranks.findByTreeAsync(tree).thenCompose(all -> {
                final Rank target = all.stream()
                        .filter(r -> r.getIdentifier().equals(targetRankIdentifier))
                        .findFirst().orElse(null);
                if (target == null) return CompletableFuture.completedFuture(PromotionResult.notFound());
                // Target must be reachable in one hop from the current rank.
                if (!target.previousRankList().contains(currentId)) {
                    return CompletableFuture.completedFuture(PromotionResult.error(
                            "rank " + targetRankIdentifier + " is not a direct child of your current rank"));
                }
                final Requirement requirement = RewardRequirementCodec.decodeRequirement(target.getRequirementData());
                return gate(requirement, playerUuid).thenApply(passed -> {
                    if (!passed) return PromotionResult.requirementsNotMet();
                    return applyPromotion(playerRank, target, playerUuid);
                });
            });
        }).exceptionally(ex -> {
            this.logger.error("promoteToAsync failed: {}", ex.getMessage());
            return PromotionResult.error(ex.getMessage());
        });
    }

    private final class PromotionAttempt {
        final Optional<RankTree> tree;
        final Optional<PlayerRank> current;

        PromotionAttempt(Optional<RankTree> tree, Optional<PlayerRank> current) {
            this.tree = tree;
            this.current = current;
        }

        CompletableFuture<PromotionResult> resolve(@NotNull UUID playerUuid) {
            if (this.tree.isEmpty()) return CompletableFuture.completedFuture(PromotionResult.notFound());
            if (this.current.isEmpty()) return CompletableFuture.completedFuture(PromotionResult.notEnrolled());

            return RankService.this.ranks.findByTreeAsync(this.tree.get()).thenCompose(all -> {
                final List<Rank> ordered = all.stream()
                        .sorted(Comparator.comparingInt(Rank::getOrderIndex))
                        .toList();
                final int currentIdx = indexOf(ordered, this.current.get().getCurrentRankIdentifier());
                if (currentIdx < 0) return CompletableFuture.completedFuture(PromotionResult.error("stale player rank"));
                if (currentIdx >= ordered.size() - 1) {
                    markTreeCompleted(this.current.get());
                    return CompletableFuture.completedFuture(PromotionResult.treeCompleted());
                }
                final Rank next = ordered.get(currentIdx + 1);
                final Requirement requirement = RewardRequirementCodec.decodeRequirement(next.getRequirementData());
                return gate(requirement, playerUuid).thenApply(passed -> {
                    if (!passed) return PromotionResult.requirementsNotMet();
                    return applyPromotion(this.current.get(), next, playerUuid);
                });
            });
        }
    }

    private @NotNull CompletableFuture<Boolean> gate(Requirement requirement, @NotNull UUID playerUuid) {
        if (requirement == null) return CompletableFuture.completedFuture(true);
        final RequirementEvaluator evaluator = RequirementEvaluator.get();
        if (evaluator == null) return CompletableFuture.completedFuture(true);
        return evaluator.evaluate(requirement, new RequirementContext(playerUuid, SOURCE, "rank-promote"))
                .thenApply(RequirementResult::isMet);
    }

    private @NotNull PromotionResult applyPromotion(@NotNull PlayerRank current, @NotNull Rank next, @NotNull UUID playerUuid) {
        final String previousRank = current.getCurrentRankIdentifier();
        current.setCurrentRankIdentifier(next.getIdentifier());
        current.setPromotedAt(LocalDateTime.now());
        current.setProgressionPercent(0);
        this.playerRanks.update(current);

        final Reward reward = RewardRequirementCodec.decodeReward(next.getRewardData());
        if (reward != null) {
            final RewardExecutor executor = RewardExecutor.get();
            if (executor != null) {
                executor.grantSync(reward, new RewardContext(playerUuid, SOURCE, "rank-promote"));
            }
        }

        de.jexcellence.quests.util.EventDispatch.fire(new RankPromotedEvent(
                new RankSnapshot(
                        playerUuid,
                        current.getTreeIdentifier(),
                        next.getIdentifier(),
                        next.getOrderIndex(),
                        current.getProgressionPercent(),
                        current.isTreeCompleted()
                ),
                previousRank
        ));

        return PromotionResult.promoted(next.getIdentifier());
    }

    private void markTreeCompleted(@NotNull PlayerRank current) {
        if (current.isTreeCompleted()) return;
        current.setTreeCompleted(true);
        current.setTreeCompletedAt(LocalDateTime.now());
        this.playerRanks.update(current);
    }

    private static int indexOf(@NotNull List<Rank> ordered, @NotNull String identifier) {
        for (int i = 0; i < ordered.size(); i++) if (ordered.get(i).getIdentifier().equals(identifier)) return i;
        return -1;
    }

    /** Raw access to the rank-tree repository — used by views for bulk listing. */
    public @NotNull RankTreeRepository trees() {
        return this.trees;
    }

    /** Raw access to the rank repository — used by views for per-tree listing. */
    public @NotNull RankRepository ranks() {
        return this.ranks;
    }

    /** Raw access to the player-rank repository — used by migration tooling. */
    public @NotNull PlayerRankRepository playerRankRepository() {
        return this.playerRanks;
    }

    /** Returns every {@link PlayerRank} row owned by the player across all trees. */
    public @NotNull CompletableFuture<List<PlayerRank>> playerRanks(@NotNull UUID playerUuid) {
        return this.playerRanks.findByPlayerAsync(playerUuid).exceptionally(ex -> {
            this.logger.error("playerRanks failed for {}: {}", playerUuid, ex.getMessage());
            return List.of();
        });
    }

    /**
     * Builds the top-N leaderboard for a tree. Ordering: rank ordinal
     * descending (higher rank wins) with earliest {@code promotedAt}
     * breaking ties. Player names are resolved via
     * {@link org.bukkit.Bukkit#getOfflinePlayer(java.util.UUID)} —
     * offline players get their last-seen name; null names fall back
     * to the UUID string.
     */
    public @NotNull CompletableFuture<List<de.jexcellence.quests.api.RankLeaderboardEntry>> topAsync(
            @NotNull String treeIdentifier, int limit
    ) {
        return this.trees.findByIdentifierAsync(treeIdentifier).thenCompose(optTree -> {
            if (optTree.isEmpty()) return CompletableFuture.completedFuture(List.<de.jexcellence.quests.api.RankLeaderboardEntry>of());
            final var tree = optTree.get();
            return this.ranks.findByTreeAsync(tree).thenCombine(
                    this.playerRanks.findAllByTreeAsync(treeIdentifier),
                    (rankList, playerRanks) -> buildLeaderboard(treeIdentifier, rankList, playerRanks, limit)
            );
        }).exceptionally(ex -> {
            this.logger.error("top leaderboard failed for {}: {}", treeIdentifier, ex.getMessage());
            return List.of();
        });
    }

    private @NotNull List<de.jexcellence.quests.api.RankLeaderboardEntry> buildLeaderboard(
            @NotNull String treeIdentifier,
            @NotNull List<Rank> rankList,
            @NotNull List<de.jexcellence.quests.database.entity.PlayerRank> playerRanks,
            int limit
    ) {
        final java.util.Map<String, Integer> ordinalByIdentifier = new java.util.HashMap<>();
        for (final Rank r : rankList) ordinalByIdentifier.put(r.getIdentifier(), r.getOrderIndex());

        return playerRanks.stream()
                .sorted(Comparator
                        .<de.jexcellence.quests.database.entity.PlayerRank>comparingInt(pr ->
                                ordinalByIdentifier.getOrDefault(pr.getCurrentRankIdentifier(), -1))
                        .reversed()
                        .thenComparing(de.jexcellence.quests.database.entity.PlayerRank::getPromotedAt))
                .limit(Math.max(1, limit))
                .map(new java.util.function.Function<de.jexcellence.quests.database.entity.PlayerRank, de.jexcellence.quests.api.RankLeaderboardEntry>() {
                    int position = 1;
                    @Override
                    public de.jexcellence.quests.api.RankLeaderboardEntry apply(de.jexcellence.quests.database.entity.PlayerRank pr) {
                        final var offline = org.bukkit.Bukkit.getOfflinePlayer(pr.getPlayerUuid());
                        final String name = offline.getName() != null ? offline.getName() : pr.getPlayerUuid().toString();
                        return new de.jexcellence.quests.api.RankLeaderboardEntry(
                                this.position++,
                                pr.getPlayerUuid(),
                                name,
                                treeIdentifier,
                                pr.getCurrentRankIdentifier(),
                                ordinalByIdentifier.getOrDefault(pr.getCurrentRankIdentifier(), -1),
                                pr.getPromotedAt(),
                                pr.isTreeCompleted()
                        );
                    }
                })
                .toList();
    }

    public record PromotionResult(@NotNull Status status, String rank, String error) {
        public enum Status { PROMOTED, REQUIREMENTS_NOT_MET, TREE_COMPLETED, NOT_ENROLLED, NOT_FOUND, ERROR }
        public static @NotNull PromotionResult promoted(@NotNull String r) { return new PromotionResult(Status.PROMOTED, r, null); }
        public static @NotNull PromotionResult requirementsNotMet() { return new PromotionResult(Status.REQUIREMENTS_NOT_MET, null, null); }
        public static @NotNull PromotionResult treeCompleted() { return new PromotionResult(Status.TREE_COMPLETED, null, null); }
        public static @NotNull PromotionResult notEnrolled() { return new PromotionResult(Status.NOT_ENROLLED, null, null); }
        public static @NotNull PromotionResult notFound() { return new PromotionResult(Status.NOT_FOUND, null, null); }
        public static @NotNull PromotionResult error(String msg) { return new PromotionResult(Status.ERROR, null, msg); }
    }
}
