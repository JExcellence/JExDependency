/*
package de.jexcellence.oneblock.manager.permission;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.manager.base.BaseManager;
import de.jexcellence.oneblock.manager.base.ManagerException;
import de.jexcellence.oneblock.manager.config.ConfigurationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

*/
/**
 * Manager for handling permissions on islands.
 * Provides comprehensive permission management with caching, custom permissions,
 * and builder permission support.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 *//*

@Getter
public class IslandPermissionManager implements IIslandPermissionManager {

    private final IIslandTrustManager trustManager;
    
    // Permission caches for improved performance
    private final Map<String, PermissionLevel> permissionLevelCache = new ConcurrentHashMap<>();
    private final Map<String, Map<PermissionType, Boolean>> customPermissionsCache = new ConcurrentHashMap<>();
    private final Map<Long, PermissionLevel> defaultVisitorPermissionsCache = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean cacheEnabled = true;
    private long cacheExpirationMs = 300_000; // 5 minutes
    private int maxCacheSize = 10_000;
    private PermissionLevel defaultVisitorPermission = PermissionLevel.VISITOR;
    private boolean strictPermissionValidation = true;
    
    */
/**
     * Creates a new IslandPermissionManager.
     * 
     * @param configurationManager the configuration manager
     * @param trustManager the trust manager
     * @param executorService the executor service for async operations
     *//*

    public IslandPermissionManager(
                                  @NotNull IIslandTrustManager trustManager,
                                  @NotNull ExecutorService executorService) {
        super("island-permission-manager", "Island Permission Manager", executorService);

        this.trustManager = Objects.requireNonNull(trustManager, "Trust manager cannot be null");
    }

    @Override
    protected void doInitialize() throws Exception {
        logger.info("Initializing Island Permission Manager...");
        
        // Load configuration
        loadConfiguration();
        
        // Initialize caches if enabled
        if (cacheEnabled) {
            logger.info("Permission cache enabled with expiration: {}ms, max size: {}", 
                       cacheExpirationMs, maxCacheSize);
        }
        
        logger.info("Island Permission Manager initialized successfully");
    }
    
    @Override
    protected void doStart() throws Exception {
        logger.info("Starting Island Permission Manager...");
        
        // Warm up cache if enabled
        if (cacheEnabled) {
            warmUpCache();
        }
        
        logger.info("Island Permission Manager started successfully");
    }
    
    @Override
    protected void doShutdown() throws Exception {
        logger.info("Shutting down Island Permission Manager...");
        
        // Clear caches
        permissionLevelCache.clear();
        customPermissionsCache.clear();
        defaultVisitorPermissionsCache.clear();
        
        logger.info("Island Permission Manager shut down successfully");
    }
    
    @Override
    protected void doReload() throws Exception {
        logger.info("Reloading Island Permission Manager...");
        
        // Clear caches
        permissionLevelCache.clear();
        customPermissionsCache.clear();
        defaultVisitorPermissionsCache.clear();
        
        // Reload configuration
        loadConfiguration();
        
        // Warm up cache again if enabled
        if (cacheEnabled) {
            warmUpCache();
        }
        
        logger.info("Island Permission Manager reloaded successfully");
    }
    
    @Override
    protected boolean doHealthCheck() {
        try {
            // Check if trust manager is healthy
            return trustManager.isHealthy();
        } catch (Exception e) {
            logger.warn("Health check failed for Island Permission Manager", e);
            return false;
        }
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasPermission(@NotNull OneblockIsland island, 
                                                   @NotNull OneblockPlayer player, 
                                                   @NotNull PermissionType permissionType) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(permissionType, "Permission type cannot be null");
        
        return executeOperation(() -> {
            // Check for custom permission first
            String customPermKey = getCustomPermissionKey(island.getId(), player.getId());
            if (cacheEnabled && customPermissionsCache.containsKey(customPermKey)) {
                Map<PermissionType, Boolean> customPerms = customPermissionsCache.get(customPermKey);
                if (customPerms.containsKey(permissionType)) {
                    boolean allowed = customPerms.get(permissionType);
                    logger.debug("Custom permission found for player {} on island {}: {} = {}", 
                                player.getPlayerName(), island.getIdentifier(), permissionType, allowed);
                    return allowed;
                }
            }
            
            // Get player's permission level
            PermissionLevel playerLevel = getPermissionLevel(island, player).join();
            
            // Check if permission level allows this permission type
            boolean allowed = permissionType.isAllowedBy(playerLevel);
            
            logger.debug("Permission check for player {} on island {}: {} = {} (level: {})", 
                        player.getPlayerName(), island.getIdentifier(), permissionType, allowed, playerLevel);
            
            return allowed;
        }, "hasPermission");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasPermission(@NotNull OneblockIsland island, 
                                                   @NotNull UUID playerId, 
                                                   @NotNull PermissionType permissionType) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(permissionType, "Permission type cannot be null");
        
        return executeOperation(() -> {
            // Check for custom permission first
            String customPermKey = getCustomPermissionKey(island.getId(), playerId);
            if (cacheEnabled && customPermissionsCache.containsKey(customPermKey)) {
                Map<PermissionType, Boolean> customPerms = customPermissionsCache.get(customPermKey);
                if (customPerms.containsKey(permissionType)) {
                    boolean allowed = customPerms.get(permissionType);
                    logger.debug("Custom permission found for player {} on island {}: {} = {}", 
                                playerId, island.getIdentifier(), permissionType, allowed);
                    return allowed;
                }
            }
            
            // Get player's permission level
            PermissionLevel playerLevel = getPermissionLevel(island, playerId).join();
            
            // Check if permission level allows this permission type
            boolean allowed = permissionType.isAllowedBy(playerLevel);
            
            logger.debug("Permission check for player {} on island {}: {} = {} (level: {})", 
                        playerId, island.getIdentifier(), permissionType, allowed, playerLevel);
            
            return allowed;
        }, "hasPermissionById");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasPermissionLevel(@NotNull OneblockIsland island, 
                                                        @NotNull OneblockPlayer player, 
                                                        @NotNull PermissionLevel permissionLevel) {
        return getPermissionLevel(island, player)
            .thenApply(playerLevel -> playerLevel.isAtLeast(permissionLevel));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasPermissionLevel(@NotNull OneblockIsland island, 
                                                        @NotNull UUID playerId, 
                                                        @NotNull PermissionLevel permissionLevel) {
        return getPermissionLevel(island, playerId)
            .thenApply(playerLevel -> playerLevel.isAtLeast(permissionLevel));
    }
    
    @Override
    @NotNull
    public CompletableFuture<PermissionLevel> getPermissionLevel(@NotNull OneblockIsland island, 
                                                                @NotNull OneblockPlayer player) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        
        return executeOperation(() -> {
            // Check cache first
            if (cacheEnabled) {
                String cacheKey = getPermissionLevelCacheKey(island.getId(), player.getId());
                PermissionLevel cachedLevel = permissionLevelCache.get(cacheKey);
                if (cachedLevel != null) {
                    logger.debug("Retrieved permission level from cache: {} for player {} on island {}", 
                                cachedLevel, player.getPlayerName(), island.getIdentifier());
                    return cachedLevel;
                }
            }
            
            // Get trust level from trust manager
            TrustLevel trustLevel = trustManager.getTrustLevel(island, player).join();
            
            // Convert trust level to permission level
            PermissionLevel permissionLevel;
            if (trustLevel == TrustLevel.NONE) {
                // Use default visitor permission for non-trusted players
                permissionLevel = getDefaultVisitorPermission(island).join();
            } else {
                permissionLevel = PermissionLevel.fromTrustLevel(trustLevel);
            }
            
            // Update cache
            if (cacheEnabled) {
                String cacheKey = getPermissionLevelCacheKey(island.getId(), player.getId());
                permissionLevelCache.put(cacheKey, permissionLevel);
            }
            
            logger.debug("Retrieved permission level: {} for player {} on island {} (trust: {})", 
                        permissionLevel, player.getPlayerName(), island.getIdentifier(), trustLevel);
            
            return permissionLevel;
        }, "getPermissionLevel");
    }
    
    @Override
    @NotNull
    public CompletableFuture<PermissionLevel> getPermissionLevel(@NotNull OneblockIsland island, 
                                                                @NotNull UUID playerId) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        
        return executeOperation(() -> {
            // Check cache first
            if (cacheEnabled) {
                String cacheKey = getPermissionLevelCacheKey(island.getId(), playerId);
                PermissionLevel cachedLevel = permissionLevelCache.get(cacheKey);
                if (cachedLevel != null) {
                    logger.debug("Retrieved permission level from cache: {} for player {} on island {}", 
                                cachedLevel, playerId, island.getIdentifier());
                    return cachedLevel;
                }
            }
            
            // Get trust level from trust manager
            TrustLevel trustLevel = trustManager.getTrustLevel(island, playerId).join();
            
            // Convert trust level to permission level
            PermissionLevel permissionLevel;
            if (trustLevel == TrustLevel.NONE) {
                // Use default visitor permission for non-trusted players
                permissionLevel = getDefaultVisitorPermission(island).join();
            } else {
                permissionLevel = PermissionLevel.fromTrustLevel(trustLevel);
            }
            
            // Update cache
            if (cacheEnabled) {
                String cacheKey = getPermissionLevelCacheKey(island.getId(), playerId);
                permissionLevelCache.put(cacheKey, permissionLevel);
            }
            
            logger.debug("Retrieved permission level: {} for player {} on island {} (trust: {})", 
                        permissionLevel, playerId, island.getIdentifier(), trustLevel);
            
            return permissionLevel;
        }, "getPermissionLevelById");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Set<PermissionType>> getPlayerPermissions(@NotNull OneblockIsland island, 
                                                                      @NotNull OneblockPlayer player) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        
        return executeOperation(() -> {
            PermissionLevel playerLevel = getPermissionLevel(island, player).join();
            Map<PermissionType, Boolean> customPerms = getCustomPermissions(island, player).join();
            
            Set<PermissionType> permissions = new HashSet<>();
            
            // Add all permissions allowed by the player's level
            for (PermissionType permType : PermissionType.values()) {
                boolean allowed = permType.isAllowedBy(playerLevel);
                
                // Override with custom permission if exists
                if (customPerms.containsKey(permType)) {
                    allowed = customPerms.get(permType);
                }
                
                if (allowed) {
                    permissions.add(permType);
                }
            }
            
            logger.debug("Retrieved {} permissions for player {} on island {}", 
                        permissions.size(), player.getPlayerName(), island.getIdentifier());
            
            return permissions;
        }, "getPlayerPermissions");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> setCustomPermission(@NotNull OneblockIsland island, 
                                                      @NotNull OneblockPlayer player, 
                                                      @NotNull PermissionType permissionType, 
                                                      boolean allowed) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(permissionType, "Permission type cannot be null");
        
        return executeOperation(() -> {
            String customPermKey = getCustomPermissionKey(island.getId(), player.getId());
            
            // Get or create custom permissions map
            Map<PermissionType, Boolean> customPerms = customPermissionsCache
                .computeIfAbsent(customPermKey, k -> new ConcurrentHashMap<>());
            
            // Set the custom permission
            customPerms.put(permissionType, allowed);
            
            // TODO: Persist to database if needed
            
            logger.debug("Set custom permission for player {} on island {}: {} = {}", 
                        player.getPlayerName(), island.getIdentifier(), permissionType, allowed);
            
            return null;
        }, "setCustomPermission");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> removeCustomPermission(@NotNull OneblockIsland island, 
                                                         @NotNull OneblockPlayer player, 
                                                         @NotNull PermissionType permissionType) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(permissionType, "Permission type cannot be null");
        
        return executeOperation(() -> {
            String customPermKey = getCustomPermissionKey(island.getId(), player.getId());
            
            Map<PermissionType, Boolean> customPerms = customPermissionsCache.get(customPermKey);
            if (customPerms != null) {
                customPerms.remove(permissionType);
                
                // Remove the entire map if it's empty
                if (customPerms.isEmpty()) {
                    customPermissionsCache.remove(customPermKey);
                }
            }
            
            // TODO: Remove from database if needed
            
            logger.debug("Removed custom permission for player {} on island {}: {}", 
                        player.getPlayerName(), island.getIdentifier(), permissionType);
            
            return null;
        }, "removeCustomPermission");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Map<PermissionType, Boolean>> getCustomPermissions(@NotNull OneblockIsland island, 
                                                                               @NotNull OneblockPlayer player) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        
        return executeOperation(() -> {
            String customPermKey = getCustomPermissionKey(island.getId(), player.getId());
            
            Map<PermissionType, Boolean> customPerms = customPermissionsCache.get(customPermKey);
            if (customPerms == null) {
                // TODO: Load from database if needed
                customPerms = new HashMap<>();
            }
            
            logger.debug("Retrieved {} custom permissions for player {} on island {}", 
                        customPerms.size(), player.getPlayerName(), island.getIdentifier());
            
            return new HashMap<>(customPerms); // Return copy to prevent external modification
        }, "getCustomPermissions");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> clearCustomPermissions(@NotNull OneblockIsland island, 
                                                         @NotNull OneblockPlayer player) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        
        return executeOperation(() -> {
            String customPermKey = getCustomPermissionKey(island.getId(), player.getId());
            customPermissionsCache.remove(customPermKey);
            
            // TODO: Remove from database if needed
            
            logger.debug("Cleared all custom permissions for player {} on island {}", 
                        player.getPlayerName(), island.getIdentifier());
            
            return null;
        }, "clearCustomPermissions");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> clearAllCustomPermissions(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            String islandPrefix = island.getId().toString() + ":";
            
            // Remove all custom permissions for this island
            customPermissionsCache.entrySet().removeIf(entry -> 
                entry.getKey().startsWith(islandPrefix));
            
            // TODO: Remove from database if needed
            
            logger.info("Cleared all custom permissions for island: {}", island.getIdentifier());
            
            return null;
        }, "clearAllCustomPermissions");
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> setDefaultVisitorPermission(@NotNull OneblockIsland island, 
                                                              @NotNull PermissionLevel permissionLevel) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(permissionLevel, "Permission level cannot be null");
        
        return executeOperation(() -> {
            defaultVisitorPermissionsCache.put(island.getId(), permissionLevel);
            
            // Clear permission level cache for this island since default changed
            if (cacheEnabled) {
                String islandPrefix = island.getId().toString() + ":";
                permissionLevelCache.entrySet().removeIf(entry -> 
                    entry.getKey().startsWith(islandPrefix));
            }
            
            // TODO: Persist to database if needed
            
            logger.debug("Set default visitor permission for island {}: {}", 
                        island.getIdentifier(), permissionLevel);
            
            return null;
        }, "setDefaultVisitorPermission");
    }
    
    @Override
    @NotNull
    public CompletableFuture<PermissionLevel> getDefaultVisitorPermission(@NotNull OneblockIsland island) {
        Objects.requireNonNull(island, "Island cannot be null");
        
        return executeOperation(() -> {
            PermissionLevel level = defaultVisitorPermissionsCache.get(island.getId());
            if (level == null) {
                // TODO: Load from database if needed
                level = defaultVisitorPermission; // Use global default
            }
            
            logger.debug("Retrieved default visitor permission for island {}: {}", 
                        island.getIdentifier(), level);
            
            return level;
        }, "getDefaultVisitorPermission");
    }
    
    @Override
    @NotNull
    public CompletableFuture<PermissionValidationResult> validatePermission(@NotNull OneblockIsland island, 
                                                                           @NotNull OneblockPlayer player, 
                                                                           @NotNull PermissionType permissionType) {
        Objects.requireNonNull(island, "Island cannot be null");
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(permissionType, "Permission type cannot be null");
        
        return executeOperation(() -> {
            // Get player's trust level and permission level
            TrustLevel trustLevel = trustManager.getTrustLevel(island, player).join();
            PermissionLevel playerLevel = getPermissionLevel(island, player).join();
            PermissionLevel requiredLevel = permissionType.getRequiredLevel();
            
            // Check for custom permission
            Map<PermissionType, Boolean> customPerms = getCustomPermissions(island, player).join();
            boolean hasCustomPermission = customPerms.containsKey(permissionType);
            
            // Determine if allowed
            boolean allowed;
            String reason;
            
            if (hasCustomPermission) {
                allowed = customPerms.get(permissionType);
                reason = allowed ? "Custom permission granted" : "Custom permission denied";
            } else {
                allowed = permissionType.isAllowedBy(playerLevel);
                if (allowed) {
                    reason = "Permission level sufficient";
                } else {
                    reason = String.format("Permission level insufficient (has: %s, required: %s)", 
                                          playerLevel, requiredLevel);
                }
            }
            
            return new PermissionValidationResult(
                allowed,
                playerLevel,
                requiredLevel,
                trustLevel,
                hasCustomPermission,
                reason
            );
        }, "validatePermission");
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
            // Load permission manager specific configuration
            // This would typically come from a YAML configuration file
            cacheEnabled = true; // Default value, could be loaded from config
            cacheExpirationMs = 300_000; // 5 minutes
            maxCacheSize = 10_000;
            defaultVisitorPermission = PermissionLevel.VISITOR;
            strictPermissionValidation = true;
            
            logger.debug("Loaded permission manager configuration: cache={}, expiration={}ms, maxSize={}, defaultVisitor={}", 
                        cacheEnabled, cacheExpirationMs, maxCacheSize, defaultVisitorPermission);
        } catch (Exception e) {
            logger.warn("Failed to load permission manager configuration, using defaults", e);
        }
    }
    
    */
/**
     * Warms up the cache by loading frequently accessed permissions.
     *//*

    private void warmUpCache() {
        try {
            // This could be implemented to pre-load frequently accessed permissions
            logger.debug("Permission cache warm-up completed");
        } catch (Exception e) {
            logger.warn("Failed to warm up permission cache", e);
        }
    }
    
    */
/**
     * Generates a cache key for permission level caching.
     * 
     * @param islandId the island ID
     * @param playerId the player ID
     * @return the cache key
     *//*

    @NotNull
    private String getPermissionLevelCacheKey(@NotNull Long islandId, @NotNull Long playerId) {
        return islandId.toString() + ":" + playerId.toString() + ":level";
    }
    
    */
/**
     * Generates a cache key for permission level caching.
     * 
     * @param islandId the island ID
     * @param playerId the player UUID
     * @return the cache key
     *//*

    @NotNull
    private String getPermissionLevelCacheKey(@NotNull Long islandId, @NotNull UUID playerId) {
        return islandId.toString() + ":" + playerId.toString() + ":level";
    }
    
    */
/**
     * Generates a cache key for custom permissions.
     * 
     * @param islandId the island ID
     * @param playerId the player ID
     * @return the cache key
     *//*

    @NotNull
    private String getCustomPermissionKey(@NotNull Long islandId, @NotNull Long playerId) {
        return islandId.toString() + ":" + playerId.toString();
    }
    
    */
/**
     * Generates a cache key for custom permissions.
     * 
     * @param islandId the island ID
     * @param playerId the player UUID
     * @return the cache key
     *//*

    @NotNull
    private String getCustomPermissionKey(@NotNull Long islandId, @NotNull UUID playerId) {
        return islandId.toString() + ":" + playerId.toString();
    }
}*/
