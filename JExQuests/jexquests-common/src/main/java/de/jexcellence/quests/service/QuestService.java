package de.jexcellence.quests.service;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.core.api.requirement.RequirementContext;
import de.jexcellence.core.api.requirement.RequirementEvaluator;
import de.jexcellence.core.api.requirement.RequirementResult;
import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.core.api.reward.RewardContext;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.core.api.reward.RewardResult;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.api.QuestSnapshot;
import de.jexcellence.quests.api.event.QuestAcceptedEvent;
import de.jexcellence.quests.api.event.QuestCompletedEvent;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.database.entity.QuestStatus;
import de.jexcellence.quests.database.entity.QuestTask;
import de.jexcellence.quests.database.repository.PlayerQuestProgressRepository;
import de.jexcellence.quests.database.repository.PlayerTaskProgressRepository;
import de.jexcellence.quests.database.repository.QuestRepository;
import de.jexcellence.quests.database.repository.QuestTaskRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Quest lifecycle: accept, abandon, progress (increment task counters),
 * complete (grant rewards). Every reward/requirement check runs through
 * the JExCore-shared {@link RewardExecutor} and {@link RequirementEvaluator}
 * services — plugin-agnostic primitives across the ecosystem.
 */
public class QuestService {

    private static final String SOURCE = "JExQuests";

    private final QuestRepository quests;
    private final QuestTaskRepository tasks;
    private final PlayerQuestProgressRepository questProgress;
    private final PlayerTaskProgressRepository taskProgress;
    private final JExLogger logger;

    public QuestService(
            @NotNull QuestRepository quests,
            @NotNull QuestTaskRepository tasks,
            @NotNull PlayerQuestProgressRepository questProgress,
            @NotNull PlayerTaskProgressRepository taskProgress,
            @NotNull JExLogger logger
    ) {
        this.quests = quests;
        this.tasks = tasks;
        this.questProgress = questProgress;
        this.taskProgress = taskProgress;
        this.logger = logger;
    }

    /** Accept a quest — checks prerequisite requirement then creates ACTIVE progress rows. */
    public @NotNull CompletableFuture<AcceptResult> acceptAsync(@NotNull UUID playerUuid, @NotNull String questIdentifier) {
        return this.quests.findByIdentifierAsync(questIdentifier).thenCompose(optQuest -> {
            if (optQuest.isEmpty()) return CompletableFuture.completedFuture(AcceptResult.NOT_FOUND);
            final Quest quest = optQuest.get();
            if (!quest.isEnabled()) return CompletableFuture.completedFuture(AcceptResult.DISABLED);

            return this.questProgress.findAsync(playerUuid, questIdentifier)
                    .thenCompose(existing -> gateAndAccept(playerUuid, quest, existing));
        }).exceptionally(ex -> {
            this.logger.error("accept failed for {}/{}: {}", playerUuid, questIdentifier, ex.getMessage());
            return AcceptResult.ERROR;
        });
    }

    private @NotNull CompletableFuture<AcceptResult> gateAndAccept(
            @NotNull UUID playerUuid, @NotNull Quest quest, @NotNull Optional<PlayerQuestProgress> existing
    ) {
        if (existing.isPresent()) {
            final QuestStatus status = existing.get().getStatus();
            if (status == QuestStatus.ACTIVE) return CompletableFuture.completedFuture(AcceptResult.ALREADY_ACTIVE);
            if (status == QuestStatus.COMPLETED && !quest.isRepeatable())
                return CompletableFuture.completedFuture(AcceptResult.ALREADY_COMPLETED);
        }

        final Requirement requirement = RewardRequirementCodec.decodeRequirement(quest.getRequirementData());
        final CompletableFuture<Boolean> gate = requirement == null
                ? CompletableFuture.completedFuture(true)
                : evaluateRequirement(requirement, playerUuid);

        return gate.thenCompose(passed -> {
            if (!passed) return CompletableFuture.completedFuture(AcceptResult.REQUIREMENTS_NOT_MET);
            return createOrReactivateProgress(playerUuid, quest, existing);
        });
    }

    private @NotNull CompletableFuture<Boolean> evaluateRequirement(@NotNull Requirement requirement, @NotNull UUID playerUuid) {
        final RequirementEvaluator evaluator = RequirementEvaluator.get();
        if (evaluator == null) {
            this.logger.warn("RequirementEvaluator unavailable; skipping gate");
            return CompletableFuture.completedFuture(true);
        }
        return evaluator.evaluate(requirement, new RequirementContext(playerUuid, SOURCE, "quest-accept"))
                .thenApply(RequirementResult::isMet);
    }

    private @NotNull CompletableFuture<AcceptResult> createOrReactivateProgress(
            @NotNull UUID playerUuid, @NotNull Quest quest, @NotNull Optional<PlayerQuestProgress> existing
    ) {
        final PlayerQuestProgress progress = existing.orElseGet(() ->
                new PlayerQuestProgress(playerUuid, quest.getIdentifier()));
        progress.setStatus(QuestStatus.ACTIVE);
        progress.setCurrentTaskIndex(0);
        progress.setStartedAt(LocalDateTime.now());
        progress.setCompletedAt(null);
        progress.setLastUpdatedAt(LocalDateTime.now());
        if (quest.getTimeLimitSeconds() > 0) {
            progress.setExpiresAt(LocalDateTime.now().plusSeconds(quest.getTimeLimitSeconds()));
        }

        return CompletableFuture.runAsync(() -> {
            if (existing.isEmpty()) this.questProgress.create(progress);
            else this.questProgress.update(progress);
            final List<String> taskIds = seedTaskRows(playerUuid, quest);
            de.jexcellence.quests.util.EventDispatch.fire(new QuestAcceptedEvent(playerUuid, snapshot(quest, taskIds)));
        }).thenApply(v -> AcceptResult.ACCEPTED);
    }

    private @NotNull List<String> seedTaskRows(@NotNull UUID playerUuid, @NotNull Quest quest) {
        final List<String> seeded = new ArrayList<>();
        try {
            final List<QuestTask> taskList = this.tasks.findByQuestAsync(quest).get();
            this.logger.info("[quest] seeding {} task rows for {}/{}",
                    taskList.size(), playerUuid, quest.getIdentifier());
            for (final QuestTask task : taskList) {
                // Upsert — repeatable quests will re-enter this code path on
                // every re-accept and naive create() fails the unique
                // (player_uuid, quest_identifier, task_identifier) constraint.
                // Reset progress/completion on re-accept so the player starts
                // the quest fresh.
                final var existing = this.taskProgress.findAsync(
                        playerUuid, quest.getIdentifier(), task.getTaskIdentifier()).get();
                final long target = resolveObjectiveTarget(task);
                if (existing.isPresent()) {
                    final PlayerTaskProgress row = existing.get();
                    row.setProgress(0L);
                    row.setTarget(target);
                    row.setCompleted(false);
                    row.setLastUpdatedAt(LocalDateTime.now());
                    this.taskProgress.update(row);
                } else {
                    this.taskProgress.create(new PlayerTaskProgress(
                            playerUuid, quest.getIdentifier(), task.getTaskIdentifier(), target));
                }
                seeded.add(task.getTaskIdentifier());
            }
        } catch (final Exception ex) {
            this.logger.error("seed task rows failed for {}/{}: {}",
                    playerUuid, quest.getIdentifier(), ex.toString());
        }
        return seeded;
    }

    /**
     * Reads the task's objective JSON (if any) and returns its
     * declared target. Falls back to {@code 1L} for legacy tasks that
     * don't declare an objective — those remain unchanged from the
     * pre-objective behaviour.
     */
    private long resolveObjectiveTarget(@NotNull QuestTask task) {
        final String json = task.getObjectiveData();
        if (json == null || json.isBlank()) return 1L;
        try {
            final var objective = QuestObjectiveCodec.decode(json);
            return objective != null ? objective.target() : 1L;
        } catch (final RuntimeException ex) {
            this.logger.warn("invalid objective on task {} — defaulting to target=1: {}",
                    task.getTaskIdentifier(), ex.getMessage());
            return 1L;
        }
    }

    static @NotNull QuestSnapshot snapshot(@NotNull Quest quest, @NotNull List<String> taskIdentifiers) {
        return new QuestSnapshot(
                quest.getIdentifier(),
                quest.getCategory(),
                quest.getDisplayName(),
                quest.getDifficulty().name(),
                quest.isRepeatable(),
                quest.getMaxCompletions(),
                quest.getCooldownSeconds(),
                quest.getTimeLimitSeconds(),
                quest.isEnabled(),
                taskIdentifiers
        );
    }

    /** Abandon — marks the progress row ABANDONED and orphan-deletes task rows. */
    public @NotNull CompletableFuture<Boolean> abandonAsync(@NotNull UUID playerUuid, @NotNull String questIdentifier) {
        return this.questProgress.findAsync(playerUuid, questIdentifier).thenApply(opt -> {
            if (opt.isEmpty()) return false;
            final PlayerQuestProgress row = opt.get();
            if (row.getStatus() != QuestStatus.ACTIVE) return false;
            row.setStatus(QuestStatus.ABANDONED);
            row.setLastUpdatedAt(LocalDateTime.now());
            this.questProgress.update(row);
            return true;
        }).exceptionally(ex -> {
            this.logger.error("abandon failed: {}", ex.getMessage());
            return false;
        });
    }

    /**
     * Increment progress on one task; flips to completed when the task
     * hits its target; auto-finalises the quest when <em>all</em>
     * tasks for that quest are completed.
     *
     * @return {@code true} when this call caused the task to complete
     */
    public @NotNull CompletableFuture<Boolean> incrementTaskAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier, @NotNull String taskIdentifier, long delta
    ) {
        return this.taskProgress.findAsync(playerUuid, questIdentifier, taskIdentifier).thenCompose(opt -> {
            if (opt.isPresent()) return applyIncrement(opt.get(), playerUuid, questIdentifier, delta);
            // Self-heal: the row should have been seeded at accept-time but
            // wasn't — lazily create it from the QuestTask definition so a
            // missed seed doesn't permanently block progression.
            this.logger.warn("[quest] no task-progress row for {}/{}/{} — seeding lazily",
                    playerUuid, questIdentifier, taskIdentifier);
            return seedTaskRowAsync(playerUuid, questIdentifier, taskIdentifier)
                    .thenCompose(seeded -> {
                        if (seeded == null) return CompletableFuture.completedFuture(false);
                        return applyIncrement(seeded, playerUuid, questIdentifier, delta);
                    });
        }).exceptionally(ex -> {
            this.logger.error("increment failed for {}/{}/{}: {}",
                    playerUuid, questIdentifier, taskIdentifier, ex.toString());
            return false;
        });
    }

    /** Applies the delta and fires the auto-complete check. Extracted so the lazy-seed branch can reuse it. */
    private @NotNull CompletableFuture<Boolean> applyIncrement(
            @NotNull PlayerTaskProgress row, @NotNull UUID playerUuid,
            @NotNull String questIdentifier, long delta
    ) {
        if (row.isCompleted()) return CompletableFuture.completedFuture(false);
        row.setProgress(Math.min(row.getTarget(), row.getProgress() + delta));
        row.setLastUpdatedAt(LocalDateTime.now());
        final boolean justCompleted = row.getProgress() >= row.getTarget();
        if (justCompleted) row.setCompleted(true);
        this.taskProgress.update(row);
        if (!justCompleted) return CompletableFuture.completedFuture(false);
        return maybeAutoComplete(playerUuid, questIdentifier).thenApply(ignored -> true);
    }

    /**
     * Recovery path for when an active quest has no {@link PlayerTaskProgress}
     * row for a given task — looks up the task definition, computes its
     * objective target, creates the row, and returns it. Used by
     * {@link #incrementTaskAsync} so a missed seed at accept time
     * self-heals on the next event instead of silently dropping progress.
     */
    private @NotNull CompletableFuture<PlayerTaskProgress> seedTaskRowAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier, @NotNull String taskIdentifier
    ) {
        return this.quests.findByIdentifierAsync(questIdentifier).thenCompose(optQuest -> {
            if (optQuest.isEmpty()) return CompletableFuture.completedFuture(null);
            return this.tasks.findByQuestAsync(optQuest.get()).thenApply(taskList -> {
                final QuestTask task = taskList.stream()
                        .filter(t -> t.getTaskIdentifier().equals(taskIdentifier))
                        .findFirst().orElse(null);
                if (task == null) return null;
                final long target = resolveObjectiveTarget(task);
                final PlayerTaskProgress fresh = new PlayerTaskProgress(
                        playerUuid, questIdentifier, taskIdentifier, target);
                return this.taskProgress.create(fresh);
            });
        });
    }

    /**
     * After a task flips to completed, check whether every other task
     * for the same quest is also completed; if so, call
     * {@link #completeAsync} so the player doesn't need to run a
     * manual finish command.
     */
    private @NotNull CompletableFuture<?> maybeAutoComplete(@NotNull UUID playerUuid, @NotNull String questIdentifier) {
        return this.taskProgress.findByPlayerAndQuestAsync(playerUuid, questIdentifier).thenCompose(rows -> {
            if (rows.isEmpty()) return CompletableFuture.completedFuture(null);
            final boolean allDone = rows.stream().allMatch(PlayerTaskProgress::isCompleted);
            if (!allDone) return CompletableFuture.completedFuture(null);
            return completeAsync(playerUuid, questIdentifier);
        }).exceptionally(ex -> {
            this.logger.error("auto-complete check failed for {}/{}: {}", playerUuid, questIdentifier, ex.getMessage());
            return null;
        });
    }

    /** Finalize a completed quest — grants the quest-level reward and bumps completion count. */
    public @NotNull CompletableFuture<CompletionResult> completeAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier
    ) {
        return this.quests.findByIdentifierAsync(questIdentifier).thenCombine(
                this.questProgress.findAsync(playerUuid, questIdentifier),
                (optQuest, optProgress) -> {
                    if (optQuest.isEmpty() || optProgress.isEmpty()) return CompletionResult.notFound();
                    final PlayerQuestProgress progress = optProgress.get();
                    if (progress.getStatus() != QuestStatus.ACTIVE) return CompletionResult.notActive();

                    progress.setStatus(QuestStatus.COMPLETED);
                    progress.setCompletedAt(LocalDateTime.now());
                    progress.setLastUpdatedAt(LocalDateTime.now());
                    progress.setCompletionCount(progress.getCompletionCount() + 1);
                    this.questProgress.update(progress);

                    final Quest quest = optQuest.get();
                    de.jexcellence.quests.util.EventDispatch.fire(new QuestCompletedEvent(
                            playerUuid, snapshot(quest, List.of()), progress.getCompletionCount()));

                    final Reward reward = RewardRequirementCodec.decodeReward(quest.getRewardData());
                    if (reward == null) return CompletionResult.completed(null);
                    final RewardExecutor executor = RewardExecutor.get();
                    if (executor == null) {
                        this.logger.warn("RewardExecutor unavailable; reward skipped");
                        return CompletionResult.completed(null);
                    }
                    final RewardResult result = executor.grantSync(
                            reward, new RewardContext(playerUuid, SOURCE, "quest-complete"));
                    return CompletionResult.completed(result);
                }
        ).exceptionally(ex -> {
            this.logger.error("complete failed: {}", ex.getMessage());
            return CompletionResult.error(ex.getMessage());
        });
    }

    /** Returns the player's active quest rows. */
    public @NotNull CompletableFuture<List<PlayerQuestProgress>> activeForPlayerAsync(@NotNull UUID playerUuid) {
        return this.questProgress.findActiveForPlayerAsync(playerUuid).exceptionally(ex -> {
            this.logger.error("activeForPlayer failed: {}", ex.getMessage());
            return List.of();
        });
    }

    /** Returns task rows ordered by the parent quest's {@link QuestTask#getOrderIndex()}. */
    public @NotNull CompletableFuture<List<PlayerTaskProgress>> taskProgressAsync(
            @NotNull UUID playerUuid, @NotNull String questIdentifier
    ) {
        return this.taskProgress.findByPlayerAndQuestAsync(playerUuid, questIdentifier)
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparing(PlayerTaskProgress::getTaskIdentifier))
                        .toList());
    }

    /** Raw access to the quest repository — used by views for bulk listing. */
    public @NotNull de.jexcellence.quests.database.repository.QuestRepository quests() {
        return this.quests;
    }

    /** Raw access to the task repository — used by the progression listener. */
    public @NotNull de.jexcellence.quests.database.repository.QuestTaskRepository tasks() {
        return this.tasks;
    }

    /** Raw access to the player-quest-progress repository — used by migration tooling. */
    public @NotNull PlayerQuestProgressRepository questProgress() {
        return this.questProgress;
    }

    /** Raw access to the player-task-progress repository — used by migration tooling. */
    public @NotNull PlayerTaskProgressRepository taskProgress() {
        return this.taskProgress;
    }

    public enum AcceptResult {
        ACCEPTED, NOT_FOUND, DISABLED, ALREADY_ACTIVE, ALREADY_COMPLETED, REQUIREMENTS_NOT_MET, ERROR
    }

    public record CompletionResult(@NotNull Status status, RewardResult rewardResult, String error) {
        public enum Status { COMPLETED, NOT_FOUND, NOT_ACTIVE, ERROR }
        public static @NotNull CompletionResult completed(RewardResult r) { return new CompletionResult(Status.COMPLETED, r, null); }
        public static @NotNull CompletionResult notFound() { return new CompletionResult(Status.NOT_FOUND, null, null); }
        public static @NotNull CompletionResult notActive() { return new CompletionResult(Status.NOT_ACTIVE, null, null); }
        public static @NotNull CompletionResult error(String msg) { return new CompletionResult(Status.ERROR, null, msg); }
    }
}
