package com.raindropcentral.rdq.quest.progression;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository;
import com.raindropcentral.rplatform.progression.ICompletionTracker;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Tracks quest completion status for players.
 * <p>
 * This implementation uses the quest completion history repository and player quest progress cache
 * to efficiently track which quests players have completed and which are currently active.
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuestCompletionTracker implements ICompletionTracker<Quest> {
    
    private final QuestCompletionHistoryRepository completionRepository;
    private final PlayerQuestProgressCache progressCache;
    
    /**
     * Creates a new quest completion tracker.
     *
     * @param completionRepository Repository for quest completion history
     * @param progressCache Cache for player quest progress
     */
    public QuestCompletionTracker(
        @NotNull final QuestCompletionHistoryRepository completionRepository,
        @NotNull final PlayerQuestProgressCache progressCache
    ) {
        this.completionRepository = completionRepository;
        this.progressCache = progressCache;
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasCompleted(@NotNull final UUID playerId, @NotNull final String nodeIdentifier) {
        // Check completion history - if any completion exists, the quest has been completed
        return completionRepository.findByPlayer(playerId)
            .thenApply(history -> history.stream()
                .anyMatch(h -> h.getQuestIdentifier().equals(nodeIdentifier)));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> isActive(@NotNull final UUID playerId, @NotNull final String nodeIdentifier) {
        return CompletableFuture.completedFuture(
            progressCache.getProgress(playerId).stream()
                .anyMatch(progress -> progress.getQuest().getIdentifier().equals(nodeIdentifier))
        );
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<String>> getCompletedNodes(@NotNull final UUID playerId) {
        return completionRepository.findByPlayer(playerId)
            .thenApply(history -> history.stream()
                .map(h -> h.getQuestIdentifier())
                .distinct()
                .collect(Collectors.toList()));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> markCompleted(@NotNull final UUID playerId, @NotNull final String nodeIdentifier) {
        // Completion is handled by QuestService, so this is a no-op
        // The completion history is already recorded when a quest is completed
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void invalidateCache(@NotNull final UUID playerId) {
        // Save and unload the player's quest progress cache
        progressCache.savePlayer(playerId);
    }
}
