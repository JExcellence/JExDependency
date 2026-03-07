package com.raindropcentral.rdq.quest.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.database.repository.quest.QuestUserRepository;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requirement that checks if a player has completed a specific quest task.
 * <p>
 * This requirement integrates with the quest user progress system to verify
 * that a player has completed a specific task within an active quest.
 * </p>
 * <p>
 * Example JSON configuration:
 * <pre>
 * {
 *   "type": "QUEST_TASK_COMPLETION",
 *   "questIdentifier": "zombie_slayer",
 *   "taskIdentifier": "kill_zombies"
 * }
 * </pre>
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public final class QuestTaskCompletionRequirement extends AbstractRequirement {
    
    private static final Logger LOGGER = Logger.getLogger(QuestTaskCompletionRequirement.class.getName());
    
    @JsonProperty("questIdentifier")
    private final String questIdentifier;
    
    @JsonProperty("taskIdentifier")
    private final String taskIdentifier;
    
    @JsonIgnore
    private QuestUserRepository repository;
    
    /**
     * Constructor for quest task completion requirement.
     *
     * @param questIdentifier the quest identifier
     * @param taskIdentifier  the task identifier within the quest
     */
    @JsonCreator
    public QuestTaskCompletionRequirement(
            @JsonProperty("questIdentifier") @NotNull final String questIdentifier,
            @JsonProperty("taskIdentifier") @NotNull final String taskIdentifier
    ) {
        super("QUEST_TASK_COMPLETION", false);
        
        if (questIdentifier == null || questIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Quest identifier cannot be null or empty");
        }
        
        if (taskIdentifier == null || taskIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Task identifier cannot be null or empty");
        }
        
        this.questIdentifier = questIdentifier.toLowerCase();
        this.taskIdentifier = taskIdentifier.toLowerCase();
    }
    
    /**
     * Sets the repository for this requirement.
     * <p>
     * This is called during initialization by the quest system.
     * </p>
     *
     * @param repository the quest user repository
     */
    public void setRepository(@NotNull final QuestUserRepository repository) {
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
     * Gets the task identifier.
     *
     * @return the task identifier
     */
    @NotNull
    public String getTaskIdentifier() {
        return taskIdentifier;
    }
    
    @Override
    public boolean isMet(@NotNull final Player player) {
        if (repository == null) {
            LOGGER.log(Level.WARNING, "QuestUserRepository not set for requirement");
            return false;
        }
        
        try {
            final CompletableFuture<Optional<QuestUser>> future =
                    repository.findActiveByPlayerAndQuest(
                            player.getUniqueId(), 
                            questIdentifier
                    );
            
            final Optional<QuestUser> questUserOpt = future.join();
            
            if (questUserOpt.isEmpty()) {
                return false;
            }
            
            final QuestUser questUser = questUserOpt.get();
            
            // Check if the specific task is completed
            return questUser.getTaskProgress().stream()
                    .anyMatch(progress -> 
                            progress.getTaskIdentifier().equalsIgnoreCase(taskIdentifier) &&
                            progress.isCompleted()
                    );
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking quest task completion requirement for " + 
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
            final CompletableFuture<Optional<QuestUser>> future =
                    repository.findActiveByPlayerAndQuest(
                            player.getUniqueId(), 
                            questIdentifier
                    );
            
            final Optional<QuestUser> questUserOpt = future.join();
            
            if (questUserOpt.isEmpty()) {
                return 0.0;
            }
            
            final QuestUser questUser = questUserOpt.get();
            
            // Find the specific task progress
            return questUser.getTaskProgress().stream()
                    .filter(progress -> 
                            progress.getTaskIdentifier().equalsIgnoreCase(taskIdentifier))
                    .findFirst()
                    .map(progress -> {
                        if (progress.isCompleted()) {
                            return 1.0;
                        }
                        
                        final int required = progress.getRequiredProgress();
                        if (required <= 0) {
                            return 0.0;
                        }
                        
                        return Math.min(1.0, (double) progress.getCurrentProgress() / required);
                    })
                    .orElse(0.0);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating quest task completion progress for " + 
                    player.getName(), e);
            return 0.0;
        }
    }
    
    @Override
    public void consume(@NotNull final Player player) {
        // Quest task completions are not consumable
    }
    
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.quest_task_completion";
    }
    
    /**
     * Gets a detailed description with current/required progress.
     *
     * @param player the player to check
     * @return the detailed description
     */
    @JsonIgnore
    @NotNull
    public String getDetailedDescription(@NotNull final Player player) {
        if (repository == null) {
            return "Task not started";
        }
        
        try {
            final CompletableFuture<Optional<QuestUser>> future =
                    repository.findActiveByPlayerAndQuest(
                            player.getUniqueId(), 
                            questIdentifier
                    );
            
            final Optional<QuestUser> questUserOpt = future.join();
            
            if (questUserOpt.isEmpty()) {
                return "Quest not started";
            }
            
            final QuestUser questUser = questUserOpt.get();
            
            return questUser.getTaskProgress().stream()
                    .filter(progress -> 
                            progress.getTaskIdentifier().equalsIgnoreCase(taskIdentifier))
                    .findFirst()
                    .map(progress -> {
                        if (progress.isCompleted()) {
                            return "Task completed";
                        }
                        
                        return String.format("Progress: %d/%d", 
                                progress.getCurrentProgress(), 
                                progress.getRequiredProgress());
                    })
                    .orElse("Task not found");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting quest task description for " + 
                    player.getName(), e);
            return "Error loading task";
        }
    }
}
