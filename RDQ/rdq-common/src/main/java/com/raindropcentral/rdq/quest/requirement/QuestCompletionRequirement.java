package com.raindropcentral.rdq.quest.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requirement that checks if a player has completed a specific quest.
 * <p>
 * This requirement integrates with the quest completion history system to verify
 * that a player has completed a quest at least once (or a minimum number of times).
 * </p>
 * <p>
 * Example JSON configuration:
 * <pre>
 * {
 *   "type": "QUEST_COMPLETION",
 *   "questIdentifier": "zombie_slayer",
 *   "minCompletions": 1
 * }
 * </pre>
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public final class QuestCompletionRequirement extends AbstractRequirement {
    
    private static final Logger LOGGER = Logger.getLogger(QuestCompletionRequirement.class.getName());
    
    @JsonProperty("questIdentifier")
    private final String questIdentifier;
    
    @JsonProperty("minCompletions")
    private final int minCompletions;
    
    @JsonIgnore
    private QuestCompletionHistoryRepository repository;
    
    /**
     * Simple constructor requiring at least one completion.
     *
     * @param questIdentifier the quest identifier to check
     */
    public QuestCompletionRequirement(@NotNull final String questIdentifier) {
        this(questIdentifier, 1);
    }
    
    /**
     * Full constructor with minimum completion count.
     *
     * @param questIdentifier the quest identifier to check
     * @param minCompletions  the minimum number of completions required
     */
    @JsonCreator
    public QuestCompletionRequirement(
            @JsonProperty("questIdentifier") @NotNull final String questIdentifier,
            @JsonProperty("minCompletions") @Nullable final Integer minCompletions
    ) {
        super("QUEST_COMPLETION", false);
        
        if (questIdentifier == null || questIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Quest identifier cannot be null or empty");
        }
        
        this.questIdentifier = questIdentifier.toLowerCase();
        this.minCompletions = minCompletions != null && minCompletions > 0 ? minCompletions : 1;
    }
    
    /**
     * Sets the repository for this requirement.
     * <p>
     * This is called during initialization by the quest system.
     * </p>
     *
     * @param repository the completion history repository
     */
    public void setRepository(@NotNull final QuestCompletionHistoryRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Gets the quest identifier.
     *
     * @return the quest identifier
     */
    @NotNull
    public String getQuestIdentifier() {
        return questIdentifier;
    }
    
    /**
     * Gets the minimum number of completions required.
     *
     * @return the minimum completions
     */
    public int getMinCompletions() {
        return minCompletions;
    }
    
    @Override
    public boolean isMet(@NotNull final Player player) {
        if (repository == null) {
            LOGGER.log(Level.WARNING, "QuestCompletionHistoryRepository not set for requirement");
            return false;
        }
        
        try {
            final CompletableFuture<Optional<QuestCompletionHistory>> future =
                    repository.findByPlayerAndQuest(player.getUniqueId(), questIdentifier);
            
            final Optional<QuestCompletionHistory> historyOpt = future.join();
            
            if (historyOpt.isEmpty()) {
                return false;
            }
            
            final QuestCompletionHistory history = historyOpt.get();
            return history.getCompletionCount() >= minCompletions;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking quest completion requirement for " + 
                    player.getName(), e);
            return false;
        }
    }
    
    @Override
    public double calculateProgress(@NotNull final Player player) {
        if (repository == null) {
            return 0.0;
        }
        
        try {
            final CompletableFuture<Optional<QuestCompletionHistory>> future =
                    repository.findByPlayerAndQuest(player.getUniqueId(), questIdentifier);
            
            final Optional<QuestCompletionHistory> historyOpt = future.join();
            
            if (historyOpt.isEmpty()) {
                return 0.0;
            }
            
            final QuestCompletionHistory history = historyOpt.get();
            final int completions = history.getCompletionCount();
            
            return Math.min(1.0, (double) completions / minCompletions);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating quest completion progress for " + 
                    player.getName(), e);
            return 0.0;
        }
    }
    
    @Override
    public void consume(@NotNull final Player player) {
        // Quest completions are not consumable - they remain in history
    }
    
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.quest_completion";
    }
    
    /**
     * Gets the current completion count for the player.
     *
     * @param player the player to check
     * @return the completion count, or 0 if not found
     */
    @JsonIgnore
    public int getCurrentCompletions(@NotNull final Player player) {
        if (repository == null) {
            return 0;
        }
        
        try {
            final CompletableFuture<Optional<QuestCompletionHistory>> future =
                    repository.findByPlayerAndQuest(player.getUniqueId(), questIdentifier);
            
            final Optional<QuestCompletionHistory> historyOpt = future.join();
            
            return historyOpt.map(QuestCompletionHistory::getCompletionCount).orElse(0);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting quest completion count for " + 
                    player.getName(), e);
            return 0;
        }
    }
    
    /**
     * Gets a detailed description with current/required completions.
     *
     * @param player the player to check
     * @return the detailed description
     */
    @JsonIgnore
    @NotNull
    public String getDetailedDescription(@NotNull final Player player) {
        final int current = getCurrentCompletions(player);
        
        if (current >= minCompletions) {
            return String.format("Completed %d/%d times", current, minCompletions);
        } else {
            return String.format("Need %d completions", minCompletions);
        }
    }
}
