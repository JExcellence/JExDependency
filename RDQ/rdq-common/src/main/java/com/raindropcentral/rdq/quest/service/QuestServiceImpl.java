/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.quest.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestUserRepository;
import com.raindropcentral.rdq.quest.model.*;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of the quest service.
 *
 * <p>This service manages all quest-related operations including quest discovery,
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
    private final Cache<UUID, List<ActiveQuest>> activeQuestCache;
    
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
        
        this.activeQuestCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .build();
        
        // TODO: Load from configuration
        this.maxActiveQuests = MAX_ACTIVE_QUESTS_DEFAULT;
    }
    
    /**
     * Gets categories.
     */
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
    
    /**
     * Gets questsByCategory.
     */
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
                    return questRepository.findByCategory(categoryId);
                })
                .thenApply(quests -> {
                    // Cache individual quests
                    quests.forEach(quest -> questCache.put(quest.getIdentifier(), quest));
                    return quests;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quests for category: " + categoryIdentifier, ex);
                    return List.of();
                });
    }
    
    /**
     * Gets quest.
     */
    @Override
    @NotNull
    public CompletableFuture<Optional<Quest>> getQuest(@NotNull final String questIdentifier) {
        // Check cache first
        final Quest cached = questCache.getIfPresent(questIdentifier);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        
        // Load from database
        return questRepository.findByIdentifier(questIdentifier)
                .thenApply(questOpt -> {
                    questOpt.ifPresent(quest -> questCache.put(questIdentifier, quest));
                    return questOpt;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quest: " + questIdentifier, ex);
                    return Optional.empty();
                });
    }
    
    /**
     * Executes startQuest.
     */
    @Override
    @NotNull
    public CompletableFuture<QuestStartResult> startQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return canStartQuest(playerId, questIdentifier)
                .thenCompose(validationResult -> {
                    // If validation failed, return the result
                    if (!(validationResult instanceof QuestStartResult.Success)) {
                        return CompletableFuture.completedFuture(validationResult);
                    }
                    
                    // Validation passed, actually start the quest
                    return getQuest(questIdentifier)
                            .thenCompose(questOpt -> {
                                if (questOpt.isEmpty()) {
                                    return CompletableFuture.completedFuture(
                                            new QuestStartResult.QuestNotFound(questIdentifier)
                                    );
                                }
                                
                                final Quest quest = questOpt.get();
                                
                                // Create quest user entity
                                final QuestUser questUser = QuestUser.start(playerId, quest);
                                
                                // Save to database
                                return questUserRepository.createAsync(questUser)
                                        .thenApply(saved -> {
                                            // Invalidate cache
                                            activeQuestCache.invalidate(playerId);
                                            
                                            LOGGER.log(Level.INFO, "Player " + playerId + 
                                                    " started quest: " + questIdentifier);
                                            
                                            return new QuestStartResult.Success(
                                                    quest.getIdentifier(),
                                                    quest.getIcon().getDisplayNameKey()
                                            );
                                        });
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error starting quest for player " + playerId, ex);
                    return new QuestStartResult.QuestNotFound(questIdentifier);
                });
    }
    
    /**
     * Executes abandonQuest.
     */
    @Override
    @NotNull
    public CompletableFuture<QuestAbandonResult> abandonQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenCompose(questUserOpt -> {
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
                                // Invalidate cache
                                activeQuestCache.invalidate(playerId);
                                
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
                    return new QuestAbandonResult.QuestNotFound(questIdentifier);
                });
    }
    
    /**
     * Gets activeQuests.
     */
    @Override
    @NotNull
    public CompletableFuture<List<ActiveQuest>> getActiveQuests(@NotNull final UUID playerId) {
        // Check cache first
        final List<ActiveQuest> cached = activeQuestCache.getIfPresent(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Load from database
        return questUserRepository.findActiveByPlayer(playerId)
                .thenApply(questUsers -> {
                    final List<ActiveQuest> activeQuests = questUsers.stream()
                            .map(this::convertToActiveQuest)
                            .collect(Collectors.toList());
                    
                    // Cache the result
                    activeQuestCache.put(playerId, activeQuests);
                    
                    return activeQuests;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading active quests for player " + playerId, ex);
                    return List.of();
                });
    }
    
    /**
     * Gets progress.
     */
    @Override
    @NotNull
    public CompletableFuture<Optional<QuestProgress>> getProgress(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenApply(questUserOpt -> questUserOpt.map(this::convertToQuestProgress))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error loading quest progress for player " + playerId, ex);
                    return Optional.empty();
                });
    }
    
    /**
     * Executes canStartQuest.
     */
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
                                new QuestStartResult.QuestNotFound(questIdentifier)
                        );
                    }
                    
                    final Quest quest = questOpt.get();
                    
                    // Check if quest is enabled
                    if (!quest.isEnabled()) {
                        return CompletableFuture.completedFuture(
                                new QuestStartResult.QuestNotFound(questIdentifier)
                        );
                    }
                    
                    // Check max active quests
                    return getActiveQuestCount(playerId)
                            .thenCompose(activeCount -> {
                                if (activeCount >= maxActiveQuests) {
                                    return CompletableFuture.completedFuture(
                                            new QuestStartResult.MaxActiveReached(activeCount, maxActiveQuests)
                                    );
                                }
                                
                                // Check if already active
                                return isQuestActive(playerId, questIdentifier)
                                        .thenCompose(isActive -> {
                                            if (isActive) {
                                                return questUserRepository.findActiveByPlayerAndQuest(
                                                        playerId, questIdentifier
                                                ).thenApply(questUserOpt -> {
                                                    final Instant startedAt = questUserOpt
                                                            .map(QuestUser::getStartedAt)
                                                            .orElse(Instant.now());
                                                    
                                                    return new QuestStartResult.AlreadyActive(
                                                            quest.getIdentifier(),
                                                            quest.getIcon().getDisplayNameKey(),
                                                            startedAt
                                                    );
                                                });
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
                                            // 3. Return QuestStartResult.RequirementsNotMet if any requirement fails
                                            
                                            // All checks passed
                                            return CompletableFuture.completedFuture(
                                                    new QuestStartResult.Success(
                                                            quest.getIdentifier(),
                                                            quest.getIcon().getDisplayNameKey()
                                                    )
                                            );
                                        });
                            });
                });
    }
    
    /**
     * Gets activeQuestCount.
     */
    @Override
    @NotNull
    public CompletableFuture<Integer> getActiveQuestCount(@NotNull final UUID playerId) {
        return questUserRepository.countActiveByPlayer(playerId)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error counting active quests for player " + playerId, ex);
                    return 0;
                });
    }
    
    /**
     * Returns whether questActive.
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> isQuestActive(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenApply(Optional::isPresent)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error checking if quest is active for player " + playerId, ex);
                    return false;
                });
    }
    
    /**
     * Executes invalidatePlayerCache.
     */
    @Override
    public void invalidatePlayerCache(@NotNull final UUID playerId) {
        activeQuestCache.invalidate(playerId);
    }
    
    /**
     * Executes invalidateQuestCache.
     */
    @Override
    public void invalidateQuestCache() {
        categoryCache.invalidateAll();
        questCache.invalidateAll();
        activeQuestCache.invalidateAll();
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
                        return new QuestStartResult.Success(
                                quest.getIdentifier(),
                                quest.getIcon().getDisplayNameKey()
                        );
                    }
                    
                    final QuestCompletionHistory history = historyOpt.get();
                    
                    if (!history.canRepeat()) {
                        final Duration remaining = history.getRemainingCooldown();
                        final Instant nextAvailable = history.getNextAvailableAt();
                        
                        return new QuestStartResult.OnCooldown(
                                quest.getIdentifier(),
                                quest.getIcon().getDisplayNameKey(),
                                remaining,
                                nextAvailable
                        );
                    }
                    
                    // Cooldown expired, can start
                    return new QuestStartResult.Success(
                            quest.getIdentifier(),
                            quest.getIcon().getDisplayNameKey()
                    );
                });
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
        
        // Convert task progress
        final List<TaskProgress> taskProgress = questUser.getTaskProgress().stream()
                .map(tp -> new TaskProgress(
                        tp.getTaskIdentifier(),
                        tp.getTaskIdentifier(), // TODO: Get actual task name from quest
                        tp.getCurrentProgress(),
                        tp.getRequiredProgress(),
                        tp.isCompleted()
                ))
                .collect(Collectors.toList());
        
        // Calculate overall progress
        final double overallProgress = taskProgress.isEmpty() ? 0.0 :
                taskProgress.stream()
                        .mapToDouble(TaskProgress::getPercentage)
                        .average()
                        .orElse(0.0);
        
        // Calculate remaining time
        final Duration timeLimit = quest.hasTimeLimit() ? 
                Duration.ofSeconds(quest.getTimeLimitSeconds()) : null;
        
        final Duration remainingTime = timeLimit != null ?
                Duration.between(Instant.now(), questUser.getStartedAt().plus(timeLimit)) : null;
        
        return new ActiveQuest(
                quest.getIdentifier(),
                quest.getIcon().getDisplayNameKey(),
                quest.getDifficulty(),
                questUser.getStartedAt(),
                timeLimit,
                remainingTime,
                taskProgress,
                overallProgress
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
        
        // Convert task progress to map
        final var taskProgressMap = questUser.getTaskProgress().stream()
                .collect(Collectors.toMap(
                        tp -> tp.getTaskIdentifier(),
                        tp -> new TaskProgress(
                                tp.getTaskIdentifier(),
                                tp.getTaskIdentifier(), // TODO: Get actual task name
                                tp.getCurrentProgress(),
                                tp.getRequiredProgress(),
                                tp.isCompleted()
                        )
                ));
        
        // Count completed tasks
        final int completedTasks = (int) questUser.getTaskProgress().stream()
                .filter(tp -> tp.isCompleted())
                .count();
        
        final int totalTasks = questUser.getTaskProgress().size();
        
        return new QuestProgress(
                quest.getIdentifier(),
                completedTasks,
                totalTasks,
                taskProgressMap
        );
    }
}
