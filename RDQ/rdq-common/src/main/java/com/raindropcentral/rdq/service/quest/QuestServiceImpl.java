package com.raindropcentral.rdq.service.quest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestUserRepository;
import com.raindropcentral.rdq.model.quest.ActiveQuest;
import com.raindropcentral.rdq.model.quest.QuestAbandonResult;
import com.raindropcentral.rdq.model.quest.QuestProgress;
import com.raindropcentral.rdq.model.quest.QuestStartResult;
import com.raindropcentral.rdq.model.quest.QuestState;
import com.raindropcentral.rdq.model.quest.QuestStateInfo;
import com.raindropcentral.rdq.model.quest.TaskProgress;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of the quest service.
 * <p>
 * This service manages all quest-related operations including quest discovery,
 * starting, abandoning, and progress tracking. It uses Caffeine caching for
 * improved performance on frequently accessed data.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestServiceImpl implements QuestService {
    
    private static final Logger LOGGER = Logger.getLogger(QuestServiceImpl.class.getName());
    
    private static final int MAX_ACTIVE_QUESTS_DEFAULT = 5;
    private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(30);
    
    private final RDQ plugin;
    private final QuestCategoryRepository categoryRepository;
    private final QuestRepository questRepository;
    private final QuestUserRepository questUserRepository;
    private final QuestCompletionHistoryRepository completionHistoryRepository;
    
    private final Cache<String, List<QuestCategory>> categoryCache;
    private final Cache<String, Quest> questCache;

    private final int maxActiveQuests;
    
    /**
     * Constructs a new quest service implementation.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestServiceImpl(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        
        // Get repositories from plugin
        this.categoryRepository = plugin.getQuestCategoryRepository();
        this.questRepository = plugin.getQuestRepository();
        this.questUserRepository = plugin.getQuestUserRepository();
        this.completionHistoryRepository = plugin.getQuestCompletionHistoryRepository();
        
        // Initialize caches
        this.categoryCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION)
                .maximumSize(100)
                .build();
        
        this.questCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION)
                .maximumSize(1000)
                .build();

        // TODO: Load from configuration
        this.maxActiveQuests = MAX_ACTIVE_QUESTS_DEFAULT;
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<QuestCategory>> getCategories() {
        // Check cache first
        final List<QuestCategory> cached = categoryCache.getIfPresent("all");
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Load from database
        return categoryRepository.findAllEnabled()
                .thenApply(categories -> {
                    categoryCache.put("all", categories);
                    return categories;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quest categories", ex);
                    return List.of();
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<Quest>> getQuestsByCategory(@NotNull final String categoryIdentifier) {
        // First get the category to get its ID
        return categoryRepository.findByIdentifier(categoryIdentifier)
                .thenCompose(categoryOpt -> {
                    if (categoryOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(List.of());
                    }
                    
                    final Long categoryId = categoryOpt.get().getId();
                    return questRepository.findByCategoryWithRewards(categoryId);
                })
                .thenApply(quests -> {
                    // Cache individual quests
                    quests.forEach(quest -> {
                        questCache.put(quest.getIdentifier(), quest);
                    });
                    return quests;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quests for category: " + categoryIdentifier, ex);
                    return List.of();
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<Quest>> getQuest(@NotNull final String questIdentifier) {
        // Check cache first
        final Quest cached = questCache.getIfPresent(questIdentifier);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        
        // Load from database with all collections eagerly fetched
        return questRepository.findByIdentifierWithCollections(questIdentifier)
                .thenApply(questOpt -> {
                    questOpt.ifPresent(quest -> questCache.put(questIdentifier, quest));
                    return questOpt;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quest: " + questIdentifier, ex);
                    return Optional.empty();
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestStartResult> startQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return canStartQuest(playerId, questIdentifier)
                .thenCompose(validationResult -> {
                    // If validation failed, return the result
                    if (!validationResult.success()) {
                        return CompletableFuture.completedFuture(validationResult);
                    }
                    
                    // Validation passed, actually start the quest
                    return questRepository.findByIdentifierWithCollections(questIdentifier)
                            .thenCompose(questOpt -> {
                                if (questOpt.isEmpty()) {
                                    return CompletableFuture.completedFuture(
                                            QuestStartResult.failure("Quest not found: " + questIdentifier)
                                    );
                                }

                                final Quest quest = questOpt.get();
                                
                                // Create quest user entity
                                final QuestUser questUser = QuestUser.start(playerId, quest);
                                
                                // Create task progress entries for each task
                                for (final com.raindropcentral.rdq.database.entity.quest.QuestTask task : quest.getTasks()) {
                                    final int required = parseRequiredAmount(task.getRequirementData());
                                    final com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress tp =
                                            com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress.create(
                                                    questUser, task.getTaskIdentifier(), required);
                                    questUser.addTaskProgress(tp);
                                }
                                
                                LOGGER.info("[QuestStart] Created QuestUser with " + questUser.getTaskProgress().size() + " task progress entries");
                                
                                // Save to database and add to cache
                                return questUserRepository.createAsync(questUser)
                                        .thenApply(saved -> {
                                            LOGGER.info("[QuestStart] Saved QuestUser to database, ID: " + saved.getId());
                                            LOGGER.info("[QuestStart] Task progress count: " + saved.getTaskProgress().size());
                                            
                                            // Add to in-memory cache immediately
                                            plugin.getQuestCacheManager().addPlayerQuest(playerId, saved);
                                            
                                            LOGGER.info("[QuestStart] Added to cache. Cache now has " + 
                                                    plugin.getQuestCacheManager().getPlayerQuests(playerId).size() + " quests");

                                            LOGGER.log(Level.INFO, "Player " + playerId +
                                                    " started quest: " + questIdentifier);

                                            return QuestStartResult.success(saved.getId());
                                        });
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error starting quest for player " + playerId, ex);
                    return QuestStartResult.failure("Error starting quest: " + ex.getMessage());
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestAbandonResult> abandonQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        // Try cache first
        final Optional<QuestUser> cachedUser = plugin.getQuestCacheManager()
                .getPlayerQuest(playerId, questIdentifier);

        final CompletableFuture<Optional<QuestUser>> questUserFuture = cachedUser.isPresent()
                ? CompletableFuture.completedFuture(cachedUser)
                : questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier);

        return questUserFuture.thenCompose(questUserOpt -> {
                    if (questUserOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new QuestAbandonResult.NotActive(questIdentifier)
                        );
                    }

                    final QuestUser questUser = questUserOpt.get();
                    final Quest quest = questUser.getQuest();

                    // Delete the quest user entity (cascades to task progress)
                    return questUserRepository.deleteAsync(questUser.getId())
                            .thenApply(v -> {
                                // Remove from in-memory cache
                                plugin.getQuestCacheManager().removePlayerQuest(playerId, questIdentifier);

                                LOGGER.log(Level.INFO, "Player " + playerId +
                                        " abandoned quest: " + questIdentifier);

                                return (QuestAbandonResult) new QuestAbandonResult.Success(
                                        quest.getIdentifier(),
                                        quest.getIcon().getDisplayNameKey()
                                );
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error abandoning quest for player " + playerId, ex);
                    return (QuestAbandonResult) new QuestAbandonResult.QuestNotFound(questIdentifier);
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<ActiveQuest>> getActiveQuests(@NotNull final UUID playerId) {
        // Serve from cache if loaded
        if (plugin.getQuestCacheManager().isLoaded(playerId)) {
            final List<ActiveQuest> result = plugin.getQuestCacheManager()
                    .getPlayerQuests(playerId)
                    .stream()
                    .map(this::convertToActiveQuest)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(result);
        }

        // Fall back to DB (e.g. offline player lookup)
        return questUserRepository.findActiveByPlayer(playerId)
                .thenApply(questUsers -> questUsers.stream()
                        .map(this::convertToActiveQuest)
                        .collect(Collectors.toList()))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading active quests for player " + playerId, ex);
                    return List.of();
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<QuestProgress>> getProgress(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        // Serve from cache if loaded
        if (plugin.getQuestCacheManager().isLoaded(playerId)) {
            final Optional<QuestProgress> result = plugin.getQuestCacheManager()
                    .getPlayerQuest(playerId, questIdentifier)
                    .map(this::convertToQuestProgress);
            return CompletableFuture.completedFuture(result);
        }
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenApply(questUserOpt -> questUserOpt.map(this::convertToQuestProgress))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quest progress for player " + playerId, ex);
                    return Optional.empty();
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestStartResult> canStartQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return getQuest(questIdentifier)
                .thenCompose(questOpt -> {
                    // Check if quest exists
                    if (questOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                QuestStartResult.failure("Quest not found: " + questIdentifier)
                        );
                    }
                    
                    final Quest quest = questOpt.get();
                    
                    // Check if quest is enabled
                    if (!quest.isEnabled()) {
                        return CompletableFuture.completedFuture(
                                QuestStartResult.failure("Quest is not enabled: " + questIdentifier)
                        );
                    }
                    
                    // Check max active quests
                    return getActiveQuestCount(playerId)
                            .thenCompose(activeCount -> {
                                if (activeCount >= maxActiveQuests) {
                                    return CompletableFuture.completedFuture(
                                            QuestStartResult.failure("Maximum active quests reached (" + 
                                                    activeCount + "/" + maxActiveQuests + ")")
                                    );
                                }
                                
                                // Check if already active
                                return isQuestActive(playerId, questIdentifier)
                                        .thenCompose(isActive -> {
                                            if (isActive) {
                                                return CompletableFuture.completedFuture(
                                                        QuestStartResult.failure("Quest is already active")
                                                );
                                            }
                                            
                                            // Check cooldown
                                            if (quest.isRepeatable()) {
                                                return checkCooldown(playerId, quest);
                                            }
                                            
                                            // Check requirements using RPlatform requirement system
                                            // TODO: Once Quest entity has requirements field, implement this
                                            // For now, requirements checking is not implemented
                                            // Future implementation will follow the pattern from PerkRequirementService:
                                            // 1. Get quest requirements from quest.getRequirements()
                                            // 2. Use RequirementService.getInstance().isMet(player, requirement)
                                            // 3. Return QuestStartResult.failure() if any requirement fails
                                            
                                            // All checks passed
                                            return CompletableFuture.completedFuture(
                                                    QuestStartResult.success(quest.getId())
                                            );
                                        });
                            });
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Integer> getActiveQuestCount(@NotNull final UUID playerId) {
        if (plugin.getQuestCacheManager().isLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                    plugin.getQuestCacheManager().getPlayerQuests(playerId).size());
        }
        return questUserRepository.countActiveByPlayer(playerId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error counting active quests for player " + playerId, ex);
                    return 0;
                });
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> isQuestActive(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        if (plugin.getQuestCacheManager().isLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                    plugin.getQuestCacheManager().getPlayerQuest(playerId, questIdentifier).isPresent());
        }
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenApply(Optional::isPresent)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error checking if quest is active for player " + playerId, ex);
                    return false;
                });
    }
    
    @Override
    public void invalidatePlayerCache(@NotNull final UUID playerId) {
        plugin.getQuestCacheManager().invalidate(playerId);
    }

    @Override
    public void invalidateQuestCache() {
        categoryCache.invalidateAll();
        questCache.invalidateAll();
    }

    /**
     * Checks if a quest is on cooldown for a player.
     *
     * @param playerId the player's unique identifier
     * @param quest    the quest to check
     * @return a future completing with the cooldown result
     */
    @NotNull
    private CompletableFuture<QuestStartResult> checkCooldown(
            @NotNull final UUID playerId,
            @NotNull final Quest quest
    ) {
        return completionHistoryRepository.findByPlayerAndQuest(playerId, quest.getIdentifier())
                .thenApply(historyOpt -> {
                    if (historyOpt.isEmpty()) {
                        // No history, can start
                        return QuestStartResult.success(quest.getId());
                    }
                    
                    final QuestCompletionHistory history = historyOpt.get();
                    
                    if (!history.canRepeat()) {
                        final Duration remaining = history.getRemainingCooldown();
                        
                        return QuestStartResult.failure(
                                "Quest is on cooldown. Time remaining: " + 
                                formatDuration(remaining)
                        );
                    }
                    
                    // Cooldown expired, can start
                    return QuestStartResult.success(quest.getId());
                });
    }
    
    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration the duration to format
     * @return the formatted string
     */
    @NotNull
    private String formatDuration(@NotNull final Duration duration) {
        final long hours = duration.toHours();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Converts a QuestUser entity to an ActiveQuest DTO.
     *
     * @param questUser the quest user entity
     * @return the active quest DTO
     */
    @NotNull
    private ActiveQuest convertToActiveQuest(@NotNull final QuestUser questUser) {
        final Quest quest = questUser.getQuest();
        
        // Count completed tasks
        final int completedTasks = (int) questUser.getTaskProgress().stream()
                .filter(tp -> tp.isCompleted())
                .count();
        
        final int totalTasks = questUser.getTaskProgress().size();
        
        // Calculate overall progress percentage (0-100)
        final double progressPercentage = totalTasks > 0 ?
                (completedTasks * 100.0 / totalTasks) : 0.0;
        
        return new ActiveQuest(
                quest.getId(),
                quest.getIdentifier(),
                quest.getIcon().getDisplayNameKey(),
                questUser.getPlayerId(),
                questUser.getStartedAt(),
                completedTasks,
                totalTasks,
                progressPercentage
        );
    }
    
    /**
     * Converts a QuestUser entity to a QuestProgress DTO.
     *
     * @param questUser the quest user entity
     * @return the quest progress DTO
     */
    @NotNull
    private QuestProgress convertToQuestProgress(@NotNull final QuestUser questUser) {
        final Quest quest = questUser.getQuest();
        
        // Convert task progress
        final List<TaskProgress> taskProgressList = questUser.getTaskProgress().stream()
                .map(tp -> new TaskProgress(
                        tp.getId(),
                        tp.getTaskIdentifier(),
                        tp.getTaskIdentifier(), // TODO: Get actual task name from quest definition
                        tp.getCurrentProgress(),
                        tp.getRequiredProgress(),
                        tp.isCompleted(),
                        tp.getRequiredProgress() > 0 ?
                                (double) tp.getCurrentProgress() / tp.getRequiredProgress() : 0.0
                ))
                .collect(Collectors.toList());
        
        // Calculate overall progress (0-100)
        final double overallProgress = taskProgressList.isEmpty() ? 0.0 :
                taskProgressList.stream()
                        .mapToDouble(TaskProgress::progressPercentage)
                        .average()
                        .orElse(0.0) * 100.0;
        
        return new QuestProgress(
                quest.getId(),
                quest.getIdentifier(),
                questUser.getPlayerId(),
                taskProgressList,
                questUser.isCompleted(),
                overallProgress
        );
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<Quest>> processQuestCompletion(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        LOGGER.log(Level.FINE, "Processing quest completion for player " + playerId + 
                ": " + questIdentifier);
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Loads the recorded completion history for a player's quest.
     *
     * @param playerId the player identifier
     * @param questIdentifier the quest identifier
     * @return a future completing with the stored completion history when present
     */
    @NotNull
    public CompletableFuture<Optional<QuestCompletionHistory>> getCompletionHistory(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return completionHistoryRepository.findByPlayerAndQuest(playerId, questIdentifier)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading completion history for player " + playerId, ex);
                    return Optional.empty();
                });
    }
    
    @Override
    @NotNull
    public CompletableFuture<QuestStateInfo> getQuestState(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return getQuest(questIdentifier)
                .thenCompose(questOpt -> {
                    if (questOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(
                                new QuestStateInfo(
                                        QuestState.LOCKED,
                                        null,
                                        null,
                                        0,
                                        List.of("Quest not found")
                                )
                        );
                    }
                    
                    final Quest quest = questOpt.get();
                    
                    // Check if quest is active
                    return isQuestActive(playerId, questIdentifier)
                            .thenCompose(isActive -> {
                                if (isActive) {
                                    // Quest is active - get progress
                                    return getActiveQuestUser(playerId, questIdentifier)
                                            .thenApply(questUser -> new QuestStateInfo(
                                                    QuestState.ACTIVE,
                                                    questUser.orElse(null),
                                                    null,
                                                    0,
                                                    List.of()
                                            ));
                                }
                                
                                // Check completion history
                                return getCompletionHistory(playerId, questIdentifier)
                                        .thenApply(historyOpt -> {
                                            if (historyOpt.isEmpty()) {
                                                // Never completed - check if available
                                                // TODO: Check requirements when implemented
                                                return new QuestStateInfo(
                                                        QuestState.AVAILABLE,
                                                        null,
                                                        null,
                                                        0,
                                                        List.of()
                                                );
                                            }
                                            
                                            final QuestCompletionHistory history = historyOpt.get();
                                            
                                            // Check if non-repeatable and completed
                                            if (!quest.isRepeatable()) {
                                                return new QuestStateInfo(
                                                        QuestState.FINISHED,
                                                        null,
                                                        history,
                                                        0,
                                                        List.of()
                                                );
                                            }
                                            
                                            // Check max completions
                                            if (quest.getMaxCompletions() > 0 && 
                                                history.getCompletionCount() >= quest.getMaxCompletions()) {
                                                return new QuestStateInfo(
                                                        QuestState.MAX_COMPLETIONS,
                                                        null,
                                                        history,
                                                        0,
                                                        List.of()
                                                );
                                            }
                                            
                                            // Check cooldown
                                            if (!history.canRepeat()) {
                                                final long remainingSeconds = history.getRemainingCooldown().getSeconds();
                                                return new QuestStateInfo(
                                                        QuestState.ON_COOLDOWN,
                                                        null,
                                                        history,
                                                        remainingSeconds,
                                                        List.of()
                                                );
                                            }
                                            
                                            // Cooldown expired - available to restart
                                            return new QuestStateInfo(
                                                    QuestState.AVAILABLE_TO_RESTART,
                                                    null,
                                                    history,
                                                    0,
                                                    List.of()
                                            );
                                        });
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error getting quest state for player " + playerId, ex);
                    return new QuestStateInfo(
                            QuestState.LOCKED,
                            null,
                            null,
                            0,
                            List.of("Error loading quest state")
                    );
                });
    }
    
    /**
     * Gets the active QuestUser entity for a player and quest.
     *
     * @param playerId the player's UUID
     * @param questIdentifier the quest identifier
     * @return the quest user, if active
     */
    @NotNull
    private CompletableFuture<Optional<QuestUser>> getActiveQuestUser(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        if (plugin.getQuestCacheManager().isLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                    plugin.getQuestCacheManager().getPlayerQuest(playerId, questIdentifier)
            );
        }
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier);
    }

    /**
     * Parses the required amount from a task's JSON requirement data.
     *
     * @param requirementData the JSON string
     * @return the required amount, defaulting to 1 if unparseable
     */
    private int parseRequiredAmount(final String requirementData) {
        if (requirementData == null || requirementData.isBlank()) {
            return 1;
        }
        try {
            final JsonObject json = JsonParser.parseString(requirementData).getAsJsonObject();
            return json.has("amount") ? json.get("amount").getAsInt() : 1;
        } catch (final Exception e) {
            return 1;
        }
    }
}
