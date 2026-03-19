package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.*;
import com.raindropcentral.rdq.database.repository.QuestRepository;
import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.model.quest.*;
import com.raindropcentral.rplatform.progression.ProgressionValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Cache-based implementation of {@link QuestService} providing comprehensive quest management functionality.
 * <p>
 * This service uses {@link PlayerQuestProgressCache} for instant access to player quest progress
 * without database queries. It integrates with the progression system to handle quest prerequisites,
 * manages quest caching for performance, and provides all quest-related operations.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Quest discovery and browsing with caching</li>
 *     <li>Prerequisite validation using ProgressionValidator</li>
 *     <li>Quest starting and abandoning with full validation</li>
 *     <li>Active quest tracking from cache (instant access)</li>
 *     <li>Automatic quest unlocking on completion</li>
 * </ul>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
public class QuestServiceImpl implements QuestService {
    
    private static final Logger LOGGER = Logger.getLogger(QuestServiceImpl.class.getName());
    
    private final RDQ plugin;
    private final QuestCacheManager questCacheManager;
    private final PlayerQuestProgressCache progressCache;
    private final QuestRepository questRepository;
    private final ProgressionValidator<Quest> progressionValidator;
    private final QuestProgressTracker progressTracker;
    
    /**
     * Constructs a new quest service implementation.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestServiceImpl(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        this.questCacheManager = plugin.getQuestCacheManager();
        this.progressCache = plugin.getPlayerQuestProgressCache();
        this.questRepository = plugin.getQuestRepository();
        this.progressionValidator = plugin.getQuestProgressionValidator();
        this.progressTracker = plugin.getQuestProgressTracker();
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<QuestCategory>> getCategories() {
        LOGGER.fine("Getting all quest categories");
        
        try {
            // Get categories from cache (instant access)
            final List<QuestCategory> categories = questCacheManager.getAllCategories();
            
            LOGGER.fine("Retrieved " + categories.size() + " quest categories");
            return CompletableFuture.completedFuture(categories);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get quest categories", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<Quest>> getQuestsByCategory(@NotNull final String categoryIdentifier) {
        LOGGER.fine("Getting quests for category: " + categoryIdentifier);
        
        try {
            // Get quests from cache (instant access)
            final List<Quest> quests = questCacheManager.getQuestsByCategory(categoryIdentifier);
            
            LOGGER.fine("Retrieved " + quests.size() + " quests for category: " + categoryIdentifier);
            return CompletableFuture.completedFuture(quests);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get quests for category: " + categoryIdentifier, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<Quest>> getQuest(@NotNull final String questIdentifier) {
        LOGGER.fine("Getting quest: " + questIdentifier);
        
        try {
            // Get quest from cache (instant access)
            final Optional<Quest> quest = questCacheManager.getQuest(questIdentifier);
            
            LOGGER.fine("Quest " + questIdentifier + " found: " + quest.isPresent());
            return CompletableFuture.completedFuture(quest);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get quest: " + questIdentifier, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestStartResult> startQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.fine("Starting quest " + questIdentifier + " for player " + playerId);
        
        return canStartQuest(playerId, questIdentifier)
                .thenCompose(validationResult -> {
                    // Check if validation failed
                    if (!validationResult.success()) {
                        return CompletableFuture.completedFuture(validationResult);
                    }
                    
                    // Get the quest
                    return getQuest(questIdentifier)
                            .thenCompose(questOpt -> {
                                if (questOpt.isEmpty()) {
                                    return CompletableFuture.completedFuture(
                                            QuestStartResult.failure("Quest not found: " + questIdentifier)
                                    );
                                }
                                
                                final Quest quest = questOpt.get();
                                
                                // Create PlayerQuestProgress record
                                final PlayerQuestProgress questProgress = new PlayerQuestProgress(playerId, quest);
                                
                                // Initialize task progress for all tasks
                                for (QuestTask task : quest.getTasks()) {
                                    PlayerTaskProgress taskProgress = new PlayerTaskProgress(questProgress, task);
                                    questProgress.addTaskProgress(taskProgress);
                                }
                                
                                // Add to cache (marks as dirty)
                                progressCache.updateProgress(playerId, questProgress);
                                
                                LOGGER.info("Successfully started quest " + questIdentifier + " for player " + playerId);
                                
                                return CompletableFuture.completedFuture(
                                        QuestStartResult.success(quest.getId())
                                );
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to start quest " + questIdentifier + " for player " + playerId, ex);
                    return QuestStartResult.failure("Quest not found: " + questIdentifier);
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestAbandonResult> abandonQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.fine("Abandoning quest " + questIdentifier + " for player " + playerId);
        
        try {
            // Check if quest is active
            return isQuestActive(playerId, questIdentifier)
                    .thenCompose(isActive -> {
                        if (!isActive) {
                            return CompletableFuture.completedFuture(
                                    new QuestAbandonResult.NotActive(questIdentifier)
                            );
                        }
                        
                        // Get quest for name
                        return getQuest(questIdentifier)
                                .thenCompose(questOpt -> {
                                    if (questOpt.isEmpty()) {
                                        return CompletableFuture.completedFuture(
                                                new QuestAbandonResult.QuestNotFound(questIdentifier)
                                        );
                                    }
                                    
                                    Quest quest = questOpt.get();
                                    
                                    // Find quest progress
                                    PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
                                    if (questProgress != null) {
                                        // Remove from cache (marks for deletion)
                                        progressCache.removeProgress(playerId, questProgress.getQuest().getId());
                                    }
                                    
                                    LOGGER.info("Successfully abandoned quest " + questIdentifier + " for player " + playerId);
                                    
                                    return CompletableFuture.completedFuture(
                                            new QuestAbandonResult.Success(questIdentifier, quest.getDisplayName())
                                    );
                                });
                    });
                    
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to abandon quest " + questIdentifier + " for player " + playerId, e);
            return CompletableFuture.completedFuture(
                    new QuestAbandonResult.QuestNotFound(questIdentifier)
            );
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<ActiveQuest>> getActiveQuests(@NotNull final UUID playerId) {
        LOGGER.fine("Getting active quests for player " + playerId);
        
        try {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                LOGGER.fine("Player cache not loaded for " + playerId + ", returning empty list");
                return CompletableFuture.completedFuture(List.of());
            }
            
            // Get active quests from cache (instant access)
            final List<PlayerQuestProgress> progressList = progressCache.getProgress(playerId);
            
            // Convert to ActiveQuest records (no async needed - all data in memory)
            final List<ActiveQuest> activeQuests = progressList.stream()
                    .map(this::convertToActiveQuest)
                    .collect(Collectors.toList());
            
            return CompletableFuture.completedFuture(activeQuests);
                            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get active quests for player " + playerId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<QuestProgress>> getProgress(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.fine("Getting progress for quest " + questIdentifier + " for player " + playerId);
        
        try {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            
            // Find quest progress from cache
            PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
            
            if (questProgress == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            
            // Convert to QuestProgress model
            QuestProgress progress = convertToQuestProgress(questProgress);
            return CompletableFuture.completedFuture(Optional.of(progress));
                    
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get progress for quest " + questIdentifier + " for player " + playerId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestStartResult> canStartQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.fine("Checking if player " + playerId + " can start quest " + questIdentifier);
        
        try {
            // Get the quest first
            return getQuest(questIdentifier)
                    .thenCompose(questOpt -> {
                        if (questOpt.isEmpty()) {
                            return CompletableFuture.completedFuture(
                                    QuestStartResult.failure("Quest not found: " + questIdentifier)
                            );
                        }
                        
                        final Quest quest = questOpt.get();
                        
                        // Check if already active
                        return isQuestActive(playerId, questIdentifier)
                                .thenCompose(isActive -> {
                                    if (isActive) {
                                        return CompletableFuture.completedFuture(
                                                QuestStartResult.failure("Quest is already active")
                                        );
                                    }
                                    
                                    // Check active quest limit
                                    return getActiveQuestCount(playerId)
                                            .thenCompose(activeCount -> {
                                                if (activeCount >= 1) { // Single quest limit
                                                    return CompletableFuture.completedFuture(
                                                            QuestStartResult.failure("Maximum active quests reached (" + activeCount + "/1)")
                                                    );
                                                }
                                                
                                                // Check prerequisites using ProgressionValidator
                                                return progressionValidator.getProgressionState(playerId, questIdentifier)
                                                        .thenApply(progressionState -> {
                                                            return switch (progressionState.status()) {
                                                                case AVAILABLE -> QuestStartResult.success(quest.getId());
                                                                
                                                                case LOCKED -> QuestStartResult.failure(
                                                                        "Prerequisites not met: " + String.join(", ", progressionState.missingPrerequisites())
                                                                );
                                                                
                                                                case COMPLETED -> QuestStartResult.failure("Quest already completed");
                                                                
                                                                case ACTIVE -> QuestStartResult.failure("Quest is already active");
                                                                
                                                                default -> QuestStartResult.failure("Quest not found: " + questIdentifier);
                                                            };
                                                        });
                                            });
                                });
                    })
                    .exceptionally(ex -> {
                        LOGGER.log(Level.SEVERE, "Failed to check if player " + playerId + " can start quest " + questIdentifier, ex);
                        return QuestStartResult.failure("Quest not found: " + questIdentifier);
                    });
                    
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check if player " + playerId + " can start quest " + questIdentifier, e);
            return CompletableFuture.completedFuture(QuestStartResult.failure("Quest not found: " + questIdentifier));
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<Integer> getActiveQuestCount(@NotNull final UUID playerId) {
        LOGGER.fine("Getting active quest count for player " + playerId);
        
        try {
            // Get count from cache (instant access)
            int count = progressCache.getActiveQuestCount(playerId);
            return CompletableFuture.completedFuture(count);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get active quest count for player " + playerId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> isQuestActive(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.fine("Checking if quest " + questIdentifier + " is active for player " + playerId);
        
        try {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                return CompletableFuture.completedFuture(false);
            }
            
            // Find quest progress from cache
            PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
            return CompletableFuture.completedFuture(questProgress != null);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check if quest " + questIdentifier + " is active for player " + playerId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public void invalidatePlayerCache(@NotNull final UUID playerId) {
        LOGGER.fine("Invalidating player cache for player " + playerId);
        
        // No-op - cache is managed by QuestProgressCacheListener lifecycle
        // Cache is loaded on join and saved on quit automatically
        LOGGER.fine("Player cache invalidation not needed - managed by lifecycle");
    }
    
    @Override
    public void invalidateQuestCache() {
        LOGGER.fine("Invalidating quest definition cache");
        
        try {
            questCacheManager.invalidate();
            LOGGER.fine("Successfully invalidated quest definition cache");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to invalidate quest definition cache", e);
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<Quest>> processQuestCompletion(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.fine("Processing quest completion for player " + playerId + ", quest: " + questIdentifier);
        
        try {
            // Use ProgressionValidator to unlock dependent quests
            return progressionValidator.processCompletion(playerId, questIdentifier)
                    .thenApply(unlockedNodes -> {
                        LOGGER.info("Quest completion processed for player " + playerId + 
                                   ", quest: " + questIdentifier + 
                                   ", unlocked: " + unlockedNodes.size() + " quests");
                        return unlockedNodes;
                    });
                    
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process quest completion for player " + playerId + ", quest: " + questIdentifier, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Converts a PlayerQuestProgress entity to an ActiveQuest model.
     * <p>
     * All data is already in memory from the cache, so this is instant (no async needed).
     * </p>
     *
     * @param questProgress the quest progress entity
     * @return the active quest model
     */
    @NotNull
    private ActiveQuest convertToActiveQuest(@NotNull final PlayerQuestProgress questProgress) {
        Quest quest = questProgress.getQuest();
        
        // Convert task progress (already in memory)
        List<TaskProgress> taskProgressList = questProgress.getTaskProgress().stream()
                .map(tp -> new TaskProgress(
                        tp.getId(),
                        tp.getTask().getIdentifier(),
                        tp.getTask().getDisplayName(),
                        (int) tp.getCurrentProgress(),
                        (int) tp.getRequiredProgress(),
                        tp.isCompleted(),
                        tp.getProgressPercentage()
                ))
                .collect(Collectors.toList());
        
        return new ActiveQuest(
                quest.getId(),
                quest.getIdentifier(),
                quest.getDisplayName(),
                questProgress.getPlayerId(),
                questProgress.getStartedAt(),
                (int) questProgress.getTaskProgress().stream().filter(tp -> tp.isCompleted()).count(),
                questProgress.getTaskProgress().size(),
                questProgress.getOverallProgress()
        );
    }
    
    /**
     * Converts PlayerQuestProgress entity to QuestProgress model.
     *
     * @param questProgress the entity
     * @return the model
     */
    @NotNull
    private QuestProgress convertToQuestProgress(@NotNull final PlayerQuestProgress questProgress) {
        List<TaskProgress> taskProgressList = questProgress.getTaskProgress().stream()
                .map(tp -> new TaskProgress(
                        tp.getId(),
                        tp.getTask().getIdentifier(),
                        tp.getTask().getDisplayName(),
                        (int) tp.getCurrentProgress(),
                        (int) tp.getRequiredProgress(),
                        tp.isCompleted(),
                        tp.getProgressPercentage()
                ))
                .collect(Collectors.toList());
        
        return new QuestProgress(
                questProgress.getQuest().getId(),
                questProgress.getQuest().getIdentifier(),
                questProgress.getPlayerId(),
                taskProgressList,
                questProgress.isCompleted(),
                questProgress.getOverallProgress()
        );
    }
    
    /**
     * Finds quest progress by quest identifier from cache.
     *
     * @param playerId        the player's UUID
     * @param questIdentifier the quest identifier
     * @return the quest progress, or null if not found
     */
    @Nullable
    private PlayerQuestProgress findQuestProgressByIdentifier(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return progressCache.getProgress(playerId).stream()
                .filter(qp -> qp.getQuest().getIdentifier().equalsIgnoreCase(questIdentifier))
                .findFirst()
                .orElse(null);
    }
}
