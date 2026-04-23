package de.jexcellence.quests.integration;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.core.api.requirement.RequirementContext;
import de.jexcellence.core.api.requirement.RequirementEvaluator;
import de.jexcellence.core.api.requirement.RequirementHandler;
import de.jexcellence.core.api.requirement.RequirementResult;
import de.jexcellence.core.service.requirement.DefaultRequirementEvaluator;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.QuestStatus;
import de.jexcellence.quests.database.entity.Rank;
import de.jexcellence.quests.database.entity.RankTree;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Registers JExQuests-owned requirement providers with the JExCore
 * {@link RequirementEvaluator}. Without this the default evaluator
 * returns {@link RequirementResult#error} for {@code quest-completed}
 * and {@code rank} predicates — every rank tree or quest gate that
 * references those types fails as "not met" regardless of the
 * player's real progress.
 *
 * <p>Handlers run <em>synchronously on the Bukkit main thread</em>
 * per the {@link RequirementHandler} contract. The JExQuests
 * repositories expose async-only queries, so each handler blocks on
 * a 2-second timeout — indexed lookups typically resolve in under a
 * millisecond. A timeout surfaces as {@link RequirementResult#error}
 * so the caller sees the stall rather than a spurious "not met".
 */
public final class QuestRequirementProvider {

    private final JExQuests quests;
    private final JExLogger logger;

    public QuestRequirementProvider(@NotNull JExQuests quests) {
        this.quests = quests;
        this.logger = quests.logger();
    }

    /**
     * Installs both handlers onto the singleton evaluator. Must run
     * after {@link RequirementEvaluator#get()} is non-null (i.e. after
     * JExCore has registered its {@link DefaultRequirementEvaluator}
     * on the ServicesManager).
     */
    public void install() {
        final RequirementEvaluator evaluator = RequirementEvaluator.get();
        if (!(evaluator instanceof DefaultRequirementEvaluator def)) {
            this.logger.warn("RequirementEvaluator missing or not DefaultRequirementEvaluator — "
                    + "quest-completed / rank gates will always fail");
            return;
        }
        def.registerHandler("quest-completed", this::evaluateQuestCompleted);
        def.registerHandler("rank", this::evaluateRank);
        this.logger.info("Registered requirement handlers: quest-completed, rank");
    }

    /**
     * {@code quest-completed(questIdentifier, minCompletions)} — passes
     * when the player has a COMPLETED {@link PlayerQuestProgress} row
     * with {@code completionCount >= minCompletions}.
     */
    private @NotNull RequirementResult evaluateQuestCompleted(
            @NotNull Requirement requirement, @NotNull RequirementContext ctx
    ) {
        if (!(requirement instanceof Requirement.QuestCompleted q)) {
            return RequirementResult.error("handler got wrong type: " + requirement.getClass());
        }
        try {
            final Optional<PlayerQuestProgress> opt = this.quests.questService().questProgress()
                    .findAsync(ctx.playerUuid(), q.questIdentifier())
                    .get(2, TimeUnit.SECONDS);
            if (opt.isEmpty()) {
                return RequirementResult.notMet("quest " + q.questIdentifier() + " not yet started");
            }
            final PlayerQuestProgress row = opt.get();
            if (row.getStatus() != QuestStatus.COMPLETED) {
                return RequirementResult.notMet("quest " + q.questIdentifier() + " not completed");
            }
            if (row.getCompletionCount() < q.minCompletions()) {
                return RequirementResult.notMet("quest " + q.questIdentifier()
                        + " needs " + q.minCompletions() + " completions, has " + row.getCompletionCount());
            }
            return RequirementResult.met();
        } catch (final TimeoutException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            return RequirementResult.error("quest lookup failed: " + ex.getMessage());
        }
    }

    /**
     * {@code rank(tree, minRankIdentifier)} — passes when the player
     * holds a rank at or above {@code minRankIdentifier} on the given
     * tree. Ordering is by {@link Rank#getOrderIndex()}; a linear tree
     * of ascending indices degenerates to "has at least this rank".
     */
    private @NotNull RequirementResult evaluateRank(
            @NotNull Requirement requirement, @NotNull RequirementContext ctx
    ) {
        if (!(requirement instanceof Requirement.Rank r)) {
            return RequirementResult.error("handler got wrong type: " + requirement.getClass());
        }
        try {
            final Optional<PlayerRank> playerOpt = this.quests.rankService().playerRankRepository()
                    .findAsync(ctx.playerUuid(), r.tree())
                    .get(2, TimeUnit.SECONDS);
            if (playerOpt.isEmpty()) {
                return RequirementResult.notMet("not enrolled on tree " + r.tree());
            }
            final Optional<RankTree> treeOpt = this.quests.rankService().trees()
                    .findByIdentifierAsync(r.tree())
                    .get(2, TimeUnit.SECONDS);
            if (treeOpt.isEmpty()) {
                return RequirementResult.error("tree " + r.tree() + " does not exist");
            }
            final List<Rank> ranks = this.quests.rankService().ranks()
                    .findByTreeAsync(treeOpt.get())
                    .get(2, TimeUnit.SECONDS);
            final List<Rank> ordered = ranks.stream()
                    .sorted(Comparator.comparingInt(Rank::getOrderIndex))
                    .toList();
            final int currentIdx = indexOf(ordered, playerOpt.get().getCurrentRankIdentifier());
            final int requiredIdx = indexOf(ordered, r.minRankIdentifier());
            if (requiredIdx < 0) {
                return RequirementResult.error("required rank " + r.minRankIdentifier() + " does not exist on tree");
            }
            if (currentIdx < requiredIdx) {
                return RequirementResult.notMet("need " + r.minRankIdentifier() + " on " + r.tree()
                        + ", have " + playerOpt.get().getCurrentRankIdentifier());
            }
            return RequirementResult.met();
        } catch (final TimeoutException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            return RequirementResult.error("rank lookup failed: " + ex.getMessage());
        }
    }

    private static int indexOf(@NotNull List<Rank> ordered, @NotNull String identifier) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getIdentifier().equals(identifier)) return i;
        }
        return -1;
    }
}
