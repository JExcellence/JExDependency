/*
package de.jexcellence.oneblock.manager.permission;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.manager.base.BaseManager;
import de.jexcellence.oneblock.manager.base.ManagerException;
import de.jexcellence.oneblock.manager.config.ConfigurationManager;
import de.jexcellence.oneblock.database.repository.OneblockIslandMemberRepository;
import de.jexcellence.oneblock.database.repository.OneblockPlayerRepository;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

*/
/**
 * Manager for handling trust relationships between players and islands.
 * Provides comprehensive trust level management with async operations and caching.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

@Getter
public class IslandTrustManager extends BaseManager implements IIslandTrustManager {
    
    private final ConfigurationManager configurationManager;
    private final OneblockIslandMemberRepository memberRepository;
    private final OneblockPlayerRepository playerRepository;
    
    // Cache for trust levels to improve performance
    private final Map<String, TrustLevel> trustCache = new ConcurrentHashMap<>();
    private final Map<Long, Set<UUID>> islandMembersCache = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean cacheEnabled = true;
    private long cacheExpirationMs = 300_000; // 5 minutes
    private int maxCacheSize = 10_000;
    
    */
/**
     * Creates a new IslandTrustManager.
     * 
     * @param configurationManager the configuration manager
     * @param memberRepository the member repository
     * @param playerRepository the player repository
     * @param executorService the executor service for async operations
     *//*

    public IslandTrustManager(@NotNull ConfigurationManager configurationManager,
                             @NotNull OneblockIslandMemberRepository memberRepository,
                             @NotNull OneblockPlayerRepository playerRepository,
                             @NotNull ExecutorService executorService) {
        super("island-trust-manager", "Island Trust Manager", executorService);
        this.configurationManager = Objects.requireNonNull(configurationManager, "Configuration manager cannot be null");
        this.memberRepository = Objects.requireNonNull(memberRepository, "Member repository cannot be null");
        this.playerRepository = Objects.requireNonNull(playerRepository, "Player repository cannot be null");
    }
    
    @Override
    protected void validateDependencies() throws ManagerException {
        if (configurationManager == null) {
            throw new ManagerException("Configuration manager is required", managerId, 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "configurationManager");
        }
        if (memberRepository == null) {
            throw new ManagerException("Member repository is required", managerId, 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "memberRepository");
        }
        if (playerRepository == null) {
            throw new ManagerException("Player repository is required", managerId, 
                                     ManagerException.ErrorCode.DEPENDENCY_MISSING, "playerRepository");
        }
    }
    
    @Override
    protected void doInitialize() throws Exception {
        logger.info("Initializing Island Trust Manager...");
        
        // Load configuration
        loadConfiguration();
        
        // Initialize cache if enabled
        if (cacheEnabled) {
            logger.info("Trust cache enabled with expiration: {}ms, max size: {}", 
                       cacheExpirationMs, maxCacheSize);
        }
        
        logger.info("Island Trust Manager initialized successfully");
    }
    
    @Override
    protected void doStart() throws Exception {
        logger.info("Starting Island Trust Manager...");
        
        // Warm up cache if enabled
        if (cacheEnabled) {
            warmUpCache();
        }
        
        logger.info("Island Trust Manager started successfully");
    }
    
    @Override
    protected void doShutdown() throws Exception {
        logger.info("Shutting down Island Trust Manager...");
        
        // Clear caches
        trustCache.clear();
        islandMembersCache.clear();
        
        logger.info("Island Trust Manager shut down successfully");
    }
    
    @Override
    protected void doReload() throws Exception {
        logger.info("Reloading Island Trust Manager...");
        
        // Clear caches
        trustCache.clear();
        islandMembersCache.clear();
        
        // Reload configuration
        loadConfiguration();
        
        // Warm up cache again if enabled
        if (cacheEnabled) {
            warmUpCache();
        }
        
        logger.info("Island Trust Manager reloaded successfully");
    }
    
    @Override
    protected boolean doHealthCheck() {
        try {
            // Check if repositories are accessible
            memberRepository.count();
            playerRepository.count();
            return true;
        } catch (Exception e) {
            logger.warn("Health check failed for Island Trust Manager", e);
            return false;
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> setTrustLevel(@NotNull OneblockIsland island, 
                                                @NotNull OneblockPlayer player, 
                                                @NotNull TrustLevel trustLevel) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(trustLevel, "Trust level cannot be null");
        
        return executeOperation(() -> {
            // Check if player is the owner
            if (island.isOwner(player)) {
                logger.debug("Cannot set trust level for island owner: {}", player.getPlayerName());
                return null; // Owner always has OWNER trust level
            }
            
            // Find existing member relationship
            Optional<OneblockIslandMember> existingMember = memberRepository
                .findByIslandAndPlayer(island, player);
            
            if (trustLevel == TrustLevel.NONE) {
                // Remove trust by removing member relationship
                existingMember.ifPresent(member -> {
                    member.setActive(false);
                    memberRepository.create(member);
                });
            } else {
                OneblockIslandMember member;
                if (existingMember.isPresent()) {
                    // Update existing member
                    member = existingMember.get();
                    member.setRole(convertTrustLevelToMemberRole(trustLevel));
                    member.setActive(true);
                } else {
                    // Create new member relationship
                    member = OneblockIslandMember.builder()
                        .island(island)
                        .player(player)
                        .role(convertTrustLevelToMemberRole(trustLevel))
                        .invitedAt(LocalDateTime.now())
                        .joinedAt(LocalDateTime.now())
                        .isActive(true)
                        .invitedBy(island.getOwner())
                        .build();
                }
                
                memberRepository.create(member);
            }
            
            // Update cache
            if (cacheEnabled) {
                String cacheKey = getCacheKey(island.getId(), player.getId());
                if (trustLevel == TrustLevel.NONE) {
                    trustCache.remove(cacheKey);
                } else {
                    trustCache.put(cacheKey, trustLevel);
                }
                
                // Update members cache
                updateMembersCache(island.getId(), player.getId(), trustLevel != TrustLevel.NONE);
            }
            
            logger.debug("Set trust level {} for player {} on island {}", 
                        trustLevel, player.getPlayerName(), island.getIdentifier());
            
            return null;
        }, "setTrustLevel");
    }
    
    @Override
    @NotNull
    public CompletableFuture<TrustLevel> getTrustLevel(@NotNull OneblockIsland island, 
                                                      @NotNull OneblockPlayer player) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        
        return executeOperation(() -> {
            // Check if player is the owner
            if (island.isOwner(player)) {
                return TrustLevel.OWNER;
            }
            
            // Check cache first
            if (cacheEnabled) {
                String cacheKey = getCacheKey(island.getId(), player.getId());
                TrustLevel cachedLevel = trustCache.get(cacheKey);
                if (cachedLevel != null) {
                    logger.debug("Retrieved trust level from cache: {} for player {} on island {}", 
                                cachedLevel, player.getPlayerName(), island.getIdentifier());
                    return cachedLevel;
                }
            }
            
            // Query database
            Optional<OneblockIslandMember> member = memberRepository
                .findByIslandAndPlayerAndIsActive(island, player, true);
            
            TrustLevel trustLevel;
            if (member.isPresent()) {
                trustLevel = convertMemberRoleToTrustLevel(member.get().getRole());
            } else {
                trustLevel = TrustLevel.NONE;
            }
            
            // Update cache
            if (cacheEnabled) {
                String cacheKey = getCacheKey(island.getId(), player.getId());
                trustCache.put(cacheKey, trustLevel);
            }
            
            logger.debug("Retrieved trust level: {} for player {} on island {}", 
                        trustLevel, player.getPlayerName(), island.getIdentifier());
            
            return trustLevel;
        }, "getTrustLevel");
    }
    
    @Override
    @NotNull
    public CompletableFuture<TrustLevel> getTrustLevel(@NotNull OneblockIsland island, 
                                                      @NotNull UUID playerId) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        return executeOperation(() -> {
            // Check if player is the owner
            if (island.getOwner().getId().equals(playerId)) {
                return TrustLevel.OWNER;
            }
            
            // Check cache first
            if (cacheEnabled) {
                String cacheKey = getCacheKey(island.getId(), playerId);
                TrustLevel cachedLevel = trustCache.get(cacheKey);
                if (cachedLevel != null) {
                    logger.debug("Retrieved trust level from cache: {} for player {} on island {}", 
                                cachedLevel, playerId, island.getIdentifier());
                    return cachedLevel;
                }
            }
            
            // Find player and get trust level
            OneblockPlayer player = playerRepository.findByUuid(playerId);
            if (player == null) {
                logger.debug("Player not found: {}", playerId);
                return TrustLevel.NONE;
            }
            
            // Query database
            Optional<OneblockIslandMember> member = memberRepository
                .findByIslandAndPlayerAndIsActive(island, player, true);
            
            TrustLevel trustLevel;
            if (member.isPresent()) {
                trustLevel = convertMemberRoleToTrustLevel(member.get().getRole());
            } else {
                trustLevel = TrustLevel.NONE;
            }
            
            // Update cache
            if (cacheEnabled) {
                String cacheKey = getCacheKey(island.getId(), playerId);
                trustCache.put(cacheKey, trustLevel);
            }
            
            logger.debug("Retrieved trust level: {} for player {} on island {}", 
                        trustLevel, playerId, island.getIdentifier());
            
            return trustLevel;
        }, "getTrustLevelById");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> isTrusted(@NotNull OneblockIsland island, 
                                               @NotNull OneblockPlayer player) {
        return getTrustLevel(island, player)
            .thenApply(trustLevel -> trustLevel.isAtLeast(TrustLevel.BASIC));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> isTrusted(@NotNull OneblockIsland island, 
                                               @NotNull UUID playerId) {
        return getTrustLevel(island, playerId)
            .thenApply(trustLevel -> trustLevel.isAtLeast(TrustLevel.BASIC));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> canPerformAction(@NotNull OneblockIsland island, 
                                                      @NotNull OneblockPlayer player, 
                                                      @NotNull TrustLevel.TrustAction action) {
        return getTrustLevel(island, player)
            .thenApply(trustLevel -> trustLevel.allows(action));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> canPerformAction(@NotNull OneblockIsland island, 
                                                      @NotNull UUID playerId, 
                                                      @NotNull TrustLevel.TrustAction action) {
        return getTrustLevel(island, playerId)
            .thenApply(trustLevel -> trustLevel.allows(action));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> removeTrust(@NotNull OneblockIsland island, 
                                              @NotNull OneblockPlayer player) {
        return setTrustLevel(island, player, TrustLevel.NONE);
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<OneblockPlayer>> getTrustedPlayers(@NotNull OneblockIsland island) {
        return getTrustedPlayers(island, TrustLevel.BASIC);
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<OneblockPlayer>> getTrustedPlayers(@NotNull OneblockIsland island, 
                                                                    @NotNull TrustLevel minTrustLevel) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(minTrustLevel, "Minimum trust level cannot be null");
        
        return executeOperation(() -> {
            List<OneblockIslandMember> members = memberRepository
                .findByIslandAndIsActive(island, true);
            
            return members.stream()
                .filter(member -> {
                    TrustLevel trustLevel = convertMemberRoleToTrustLevel(member.getRole());
                    return trustLevel.isAtLeast(minTrustLevel);
                })
                .map(OneblockIslandMember::getPlayer)
                .collect(Collectors.toList());
        }, "getTrustedPlayers");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Integer> getTrustedPlayerCount(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            return (int) memberRepository.countByIslandAndIsActive(island, true);
        }, "getTrustedPlayerCount");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> clearAllTrust(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            List<OneblockIslandMember> members = memberRepository
                .findByIslandAndIsActive(island, true);
            
            for (OneblockIslandMember member : members) {
                member.setActive(false);
                memberRepository.create(member);
            }
            
            // Clear cache for this island
            if (cacheEnabled) {
                trustCache.entrySet().removeIf(entry -> 
                    entry.getKey().startsWith(island.getId().toString()));
                islandMembersCache.remove(island.getId());
            }
            
            logger.info("Cleared all trust relationships for island: {}", island.getIdentifier());
            return null;
        }, "clearAllTrust");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> transferOwnership(@NotNull OneblockIsland island, 
                                                    @NotNull OneblockPlayer newOwner) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(newOwner, "New owner cannot be null");
        
        return executeOperation(() -> {
            OneblockPlayer oldOwner = island.getOwner();
            
            // Set new owner
            island.setOwner(newOwner);
            
            // Remove new owner from members if they were a member
            Optional<OneblockIslandMember> newOwnerMember = memberRepository
                .findByIslandAndPlayer(island, newOwner);
            newOwnerMember.ifPresent(member -> {
                member.setActive(false);
                memberRepository.create(member);
            });
            
            // Add old owner as co-owner member
            OneblockIslandMember oldOwnerMember = OneblockIslandMember.builder()
                .island(island)
                .player(oldOwner)
                .role(OneblockIslandMember.MemberRole.CO_OWNER)
                .invitedAt(LocalDateTime.now())
                .joinedAt(LocalDateTime.now())
                .isActive(true)
                .invitedBy(newOwner)
                .build();
            
            memberRepository.create(oldOwnerMember);
            
            // Update cache
            if (cacheEnabled) {
                // Clear old owner cache entry
                String oldOwnerCacheKey = getCacheKey(island.getId(), oldOwner.getId());
                trustCache.put(oldOwnerCacheKey, TrustLevel.CO_OWNER);
                
                // Clear new owner cache entry (they're now owner)
                String newOwnerCacheKey = getCacheKey(island.getId(), newOwner.getId());
                trustCache.remove(newOwnerCacheKey);
                
                // Update members cache
                updateMembersCache(island.getId(), oldOwner.getId(), true);
                updateMembersCache(island.getId(), newOwner.getId(), false);
            }
            
            logger.info("Transferred ownership of island {} from {} to {}", 
                       island.getIdentifier(), oldOwner.getPlayerName(), newOwner.getPlayerName());
            
            return null;
        }, "transferOwnership");
    }
    
    // =====================================================
    // PRIVATE HELPER METHODS
    // =====================================================
    
    */
/**
     * Loads configuration from the configuration manager.
     *//*

    private void loadConfiguration() {
        try {
            // Load trust manager specific configuration
            // This would typically come from a YAML configuration file
            cacheEnabled = true; // Default value, could be loaded from config
            cacheExpirationMs = 300_000; // 5 minutes
            maxCacheSize = 10_000;
            
            logger.debug("Loaded trust manager configuration: cache={}, expiration={}ms, maxSize={}", 
                        cacheEnabled, cacheExpirationMs, maxCacheSize);
        } catch (Exception e) {
            logger.warn("Failed to load trust manager configuration, using defaults", e);
        }
    }
    
    */
/**
     * Warms up the cache by loading frequently accessed trust relationships.
     *//*

    private void warmUpCache() {
        try {
            // This could be implemented to pre-load frequently accessed trust relationships
            logger.debug("Cache warm-up completed");
        } catch (Exception e) {
            logger.warn("Failed to warm up trust cache", e);
        }
    }
    
    */
/**
     * Generates a cache key for trust level caching.
     * 
     * @param islandId the island ID
     * @param playerId the player ID
     * @return the cache key
     *//*

    @NotNull
    private String getCacheKey(@NotNull Long islandId, @NotNull Long playerId) {
        return islandId.toString() + ":" + playerId.toString();
    }
    
    */
/**
     * Generates a cache key for trust level caching.
     * 
     * @param islandId the island ID
     * @param playerId the player UUID
     * @return the cache key
     *//*

    @NotNull
    private String getCacheKey(@NotNull Long islandId, @NotNull UUID playerId) {
        return islandId.toString() + ":" + playerId.toString();
    }
    
    */
/**
     * Updates the members cache for an island.
     * 
     * @param islandId the island ID
     * @param playerId the player ID
     * @param isMember true if player is a member
     *//*

    private void updateMembersCache(@NotNull Long islandId, @NotNull Long playerId, boolean isMember) {
        Set<UUID> members = islandMembersCache.computeIfAbsent(islandId, k -> ConcurrentHashMap.newKeySet());
        // Note: This is a simplified implementation - in production you'd want to convert Long to UUID properly
    }
    
    */
/**
     * Updates the members cache for an island.
     * 
     * @param islandId the island ID
     * @param playerId the player UUID
     * @param isMember true if player is a member
     *//*

    private void updateMembersCache(@NotNull Long islandId, @NotNull UUID playerId, boolean isMember) {
        Set<UUID> members = islandMembersCache.computeIfAbsent(islandId, k -> ConcurrentHashMap.newKeySet());
        if (isMember) {
            members.add(playerId);
        } else {
            members.remove(playerId);
        }
    }
    
    */
/**
     * Converts a TrustLevel to a MemberRole.
     * 
     * @param trustLevel the trust level
     * @return the corresponding member role
     *//*

    @NotNull
    private OneblockIslandMember.MemberRole convertTrustLevelToMemberRole(@NotNull TrustLevel trustLevel) {
        return switch (trustLevel) {
            case VISITOR -> OneblockIslandMember.MemberRole.VISITOR;
            case BASIC, MEMBER -> OneblockIslandMember.MemberRole.MEMBER;
            case TRUSTED -> OneblockIslandMember.MemberRole.TRUSTED;
            case MODERATOR -> OneblockIslandMember.MemberRole.MODERATOR;
            case CO_OWNER -> OneblockIslandMember.MemberRole.CO_OWNER;
            case OWNER -> OneblockIslandMember.MemberRole.CO_OWNER; // Owner is handled separately
            default -> OneblockIslandMember.MemberRole.VISITOR;
        };
    }
    
    */
/**
     * Converts a MemberRole to a TrustLevel.
     * 
     * @param memberRole the member role
     * @return the corresponding trust level
     *//*

    @NotNull
    private TrustLevel convertMemberRoleToTrustLevel(@NotNull OneblockIslandMember.MemberRole memberRole) {
        return switch (memberRole) {
            case VISITOR -> TrustLevel.VISITOR;
            case MEMBER -> TrustLevel.MEMBER;
            case TRUSTED -> TrustLevel.TRUSTED;
            case MODERATOR -> TrustLevel.MODERATOR;
            case CO_OWNER -> TrustLevel.CO_OWNER;
        };
    }
}*/
