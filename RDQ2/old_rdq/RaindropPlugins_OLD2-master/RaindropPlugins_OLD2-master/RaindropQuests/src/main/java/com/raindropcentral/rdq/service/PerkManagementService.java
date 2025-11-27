package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkState;
import com.raindropcentral.rdq.view.perks.PerkDisplayData;
import com.raindropcentral.rplatform.logger.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive service for managing perk operations, caching, and state management.
 * <p>
 * This service provides a high-level interface for all perk-related operations including:
 * <ul>
 *   <li>Loading and caching perk data with player-specific states</li>
 *   <li>Managing perk activation, deactivation, and cooldowns</li>
 *   <li>Handling perk prerequisites and conflict resolution</li>
 *   <li>Providing efficient data access for UI components</li>
 *   <li>Managing database operations with proper error handling</li>
 * </ul>
 * </p>
 *
 * <p>
 * The service implements a multi-level caching strategy:
 * <ul>
 *   <li>Global perk cache for all available perks</li>
 *   <li>Player-specific perk state cache</li>
 *   <li>Cooldown tracking with automatic expiry</li>
 *   <li>Category-based filtering cache</li>
 * </ul>
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class PerkManagementService {
    
    private static final Logger LOGGER = CentralLogger.getLogger(PerkManagementService.class.getName());
    
    private final RDQImpl plugin;
    
    // Global caches
    private final Map<String, RPerk> globalPerkCache = new ConcurrentHashMap<>();
    private final Map<EPerkCategory, List<String>> categoryCache = new ConcurrentHashMap<>();
    private volatile long globalCacheLastUpdate = 0L;
    private static final long GLOBAL_CACHE_TTL = 300_000L; // 5 minutes
    
    // Player-specific caches
    private final Map<UUID, PlayerPerkCache> playerCaches = new ConcurrentHashMap<>();
    private static final long PLAYER_CACHE_TTL = 60_000L; // 1 minute
    
    // Cooldown tracking
    private final Map<String, LocalDateTime> activeCooldowns = new ConcurrentHashMap<>();
    
    /**
     * Constructs a new PerkManagementService with the specified plugin instance.
     *
     * @param plugin the main plugin instance
     */
    public PerkManagementService(final @NotNull RDQImpl plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        
        // Initialize global cache on startup
        this.refreshGlobalCache();
        
        LOGGER.log(Level.INFO, "PerkManagementService initialized successfully");
    }
    
    /**
     * Loads all perk display data for a specific player asynchronously.
     *
     * @param player the player to load perks for
     * @return CompletableFuture containing the list of perk display data
     */
    public @NotNull CompletableFuture<List<PerkDisplayData>> loadPlayerPerks(final @NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RDQPlayer rdqPlayer = this.getRDQPlayerEntity(player);
                if (rdqPlayer == null) {
                    LOGGER.log(Level.WARNING, "RDQPlayer entity not found for player: " + player.getName());
                    return new ArrayList<>();
                }
                
                // Ensure global cache is fresh
                this.ensureGlobalCacheValid();
                
                // Get or create player cache
                final PlayerPerkCache playerCache = this.getOrCreatePlayerCache(player.getUniqueId(), rdqPlayer);
                
                // Build display data
                final List<PerkDisplayData> displayData = new ArrayList<>();
                for (final RPerk perk : this.globalPerkCache.values()) {
                    if (!perk.isEnabled()) {
                        continue; // Skip globally disabled perks
                    }
                    
                    final PerkDisplayData perkDisplay = this.createPerkDisplayData(perk, player, rdqPlayer, playerCache);
                    displayData.add(perkDisplay);
                }
                
                // Sort by priority and state
                displayData.sort(this::comparePerkDisplayData);
                
                LOGGER.log(Level.FINE, "Loaded {} perks for player {}", new Object[]{displayData.size(), player.getName()});
                return displayData;
                
            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading player perks for " + player.getName(), e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Loads perks filtered by category for a specific player.
     *
     * @param player   the player to load perks for
     * @param category the category to filter by (null for all categories)
     * @return CompletableFuture containing the filtered list of perk display data
     */
    public @NotNull CompletableFuture<List<PerkDisplayData>> loadPlayerPerksByCategory(
        final @NotNull Player player,
        final @Nullable EPerkCategory category
    ) {
        if (category == null) {
            return this.loadPlayerPerks(player);
        }
        
        return this.loadPlayerPerks(player).thenApply(allPerks ->
            allPerks.stream()
                .filter(perk -> perk.getCategory() == category)
                .collect(Collectors.toList())
        );
    }
    
    /**
     * Activates a perk for a player.
     *
     * @param player     the player
     * @param perkId     the perk identifier
     * @param forceBypass whether to bypass normal checks (admin override)
     * @return CompletableFuture containing the activation result
     */
    public @NotNull CompletableFuture<PerkActivationResult> activatePerk(
        final @NotNull Player player,
        final @NotNull String perkId,
        final boolean forceBypass
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RPerk perk = this.globalPerkCache.get(perkId);
                if (perk == null) {
                    return new PerkActivationResult(false, "Perk not found: " + perkId);
                }
                
                final RDQPlayer rdqPlayer = this.getRDQPlayerEntity(player);
                if (rdqPlayer == null) {
                    return new PerkActivationResult(false, "Player entity not found");
                }
                
                if (!forceBypass) {
                    // Perform validation checks
                    final PerkActivationResult validationResult = this.validatePerkActivation(player, perk, rdqPlayer);
                    if (!validationResult.isSuccess()) {
                        return validationResult;
                    }
                }
                
                // Perform the activation
                final boolean activationSuccess = this.performPerkActivation(player, perk, rdqPlayer);
                if (!activationSuccess) {
                    return new PerkActivationResult(false, "Perk activation failed");
                }
                
                // Update caches
                this.invalidatePlayerCache(player.getUniqueId());
                
                LOGGER.log(Level.INFO, "Successfully activated perk {} for player {}", new Object[]{perkId, player.getName()});
                return new PerkActivationResult(true, "Perk activated successfully");
                
            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error activating perk " + perkId + " for player " + player.getName(), e);
                return new PerkActivationResult(false, "Internal error during activation");
            }
        });
    }
    
    /**
     * Deactivates a perk for a player.
     *
     * @param player the player
     * @param perkId the perk identifier
     * @return CompletableFuture containing the deactivation result
     */
    public @NotNull CompletableFuture<PerkActivationResult> deactivatePerk(
        final @NotNull Player player,
        final @NotNull String perkId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RPerk perk = this.globalPerkCache.get(perkId);
                if (perk == null) {
                    return new PerkActivationResult(false, "Perk not found: " + perkId);
                }
                
                final RDQPlayer rdqPlayer = this.getRDQPlayerEntity(player);
                if (rdqPlayer == null) {
                    return new PerkActivationResult(false, "Player entity not found");
                }
                
                // Perform the deactivation
                final boolean deactivationSuccess = this.performPerkDeactivation(player, perk, rdqPlayer);
                if (!deactivationSuccess) {
                    return new PerkActivationResult(false, "Perk deactivation failed");
                }
                
                // Update caches
                this.invalidatePlayerCache(player.getUniqueId());
                
                LOGGER.log(Level.INFO, "Successfully deactivated perk {} for player {}", new Object[]{perkId, player.getName()});
                return new PerkActivationResult(true, "Perk deactivated successfully");
                
            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error deactivating perk " + perkId + " for player " + player.getName(), e);
                return new PerkActivationResult(false, "Internal error during deactivation");
            }
        });
    }
    
    /**
     * Clears the cache for a specific player.
     *
     * @param playerId the player's UUID
     */
    public void cleaRDQPlayerCache(final @NotNull UUID playerId) {
        this.playerCaches.remove(playerId);
        LOGGER.log(Level.FINE, "Cleared cache for player: {}", playerId);
    }
    
    /**
     * Refreshes the global perk cache from the database.
     */
    public void refreshGlobalCache() {
        try {
            LOGGER.log(Level.FINE, "Refreshing global perk cache...");
            
            final List<RPerk> allPerks = this.plugin.getPerkRepository().findAll(0, 1000);
            
            this.globalPerkCache.clear();
            this.categoryCache.clear();
            
            for (final RPerk perk : allPerks) {
                this.globalPerkCache.put(perk.getIdentifier(), perk);
                
                // Update category cache
                final EPerkCategory category = this.determinePerkCategory(perk);
                this.categoryCache.computeIfAbsent(category, k -> new ArrayList<>()).add(perk.getIdentifier());
            }
            
            this.globalCacheLastUpdate = System.currentTimeMillis();
            
            LOGGER.log(Level.INFO, "Global perk cache refreshed with {} perks", allPerks.size());
            
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error refreshing global perk cache", e);
        }
    }
    
    // ========== Private Helper Methods ==========
    
    private void ensureGlobalCacheValid() {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - this.globalCacheLastUpdate > GLOBAL_CACHE_TTL) {
            this.refreshGlobalCache();
        }
    }
    
    private @Nullable RDQPlayer getRDQPlayerEntity(final @NotNull Player player) {
        try {
            return this.plugin.getPlayerRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId()));
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error retrieving RDQPlayer entity for " + player.getName(), e);
            return null;
        }
    }
    
    private @NotNull PlayerPerkCache getOrCreatePlayerCache(final @NotNull UUID playerId, final @NotNull RDQPlayer rdqPlayer) {
        return this.playerCaches.computeIfAbsent(playerId, k -> {
            try {
                return this.loadPlayerPerkCache(rdqPlayer);
            } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Error loading player perk cache for " + playerId, e);
                return new PlayerPerkCache();
            }
        });
    }
    
    private @NotNull PlayerPerkCache loadPlayerPerkCache(final @NotNull RDQPlayer rdqPlayer) {
        final PlayerPerkCache cache = new PlayerPerkCache();
        
        try {
            // Load player-perk relationships
            final List<RPlayerPerk> playerPerks = this.plugin.getPlayerPerkRepository()
                .findListByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));
            
            for (final RPlayerPerk playerPerk : playerPerks) {
                final String perkId = playerPerk.getPerk().getIdentifier();
                cache.ownedPerks.add(perkId);
                
                if (playerPerk.isActive()) {
                    cache.activePerks.add(perkId);
                }
                
                if (playerPerk.getCooldownExpiry() != null && playerPerk.getCooldownExpiry().isAfter(LocalDateTime.now())) {
                    cache.cooldownExpiries.put(perkId, playerPerk.getCooldownExpiry());
                }
            }
            
            cache.lastUpdate = System.currentTimeMillis();
            
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error loading player perk cache", e);
        }
        
        return cache;
    }
    
    private @NotNull PerkDisplayData createPerkDisplayData(
        final @NotNull RPerk perk,
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull PlayerPerkCache playerCache
    ) {
        final String perkId = perk.getIdentifier();
        
        // Determine perk state
        final EPerkState state = this.calculatePerkState(perk, playerCache);
        
        // Determine perk category
        final EPerkCategory category = this.determinePerkCategory(perk);
        
        // Calculate cooldown information
        final LocalDateTime cooldownExpiry = playerCache.cooldownExpiries.get(perkId);
        final long remainingCooldown = this.calculateRemainingCooldown(cooldownExpiry);
        
        // Determine interaction capabilities
        final boolean canToggle = this.canTogglePerk(perk, player, state);
        final boolean canActivate = this.canActivatePerk(perk, player, state);
        
        return new PerkDisplayData(
            perk,
            state,
            category,
            cooldownExpiry,
            remainingCooldown,
            canToggle,
            canActivate
        );
    }
    
    private @NotNull EPerkState calculatePerkState(final @NotNull RPerk perk, final @NotNull PlayerPerkCache playerCache) {
        final String perkId = perk.getIdentifier();
        
        // Check if perk is globally disabled
        if (!perk.isEnabled()) {
            return EPerkState.DISABLED;
        }
        
        // Check if player has unlocked the perk
        if (!playerCache.ownedPerks.contains(perkId)) {
            return EPerkState.LOCKED;
        }
        
        // Check if perk is on cooldown
        final LocalDateTime cooldownExpiry = playerCache.cooldownExpiries.get(perkId);
        if (cooldownExpiry != null && cooldownExpiry.isAfter(LocalDateTime.now())) {
            return EPerkState.COOLDOWN;
        }
        
        // Check if perk is currently active
        if (playerCache.activePerks.contains(perkId)) {
            return EPerkState.ACTIVE;
        }
        
        // Perk is available for use
        return EPerkState.AVAILABLE;
    }
    
    private @NotNull EPerkCategory determinePerkCategory(final @NotNull RPerk perk) {
        // TODO: This should be configurable or stored in the database
        // For now, use simple pattern matching based on identifier
        final String identifier = perk.getIdentifier().toLowerCase();
        
        if (identifier.contains("combat") || identifier.contains("damage") || identifier.contains("attack")) {
            return EPerkCategory.COMBAT;
        } else if (identifier.contains("speed") || identifier.contains("flight") || identifier.contains("teleport")) {
            return EPerkCategory.MOVEMENT;
        } else if (identifier.contains("vision") || identifier.contains("tool") || identifier.contains("utility")) {
            return EPerkCategory.UTILITY;
        } else if (identifier.contains("health") || identifier.contains("hunger") || identifier.contains("survival")) {
            return EPerkCategory.SURVIVAL;
        } else if (identifier.contains("money") || identifier.contains("economy") || identifier.contains("shop")) {
            return EPerkCategory.ECONOMY;
        } else if (identifier.contains("chat") || identifier.contains("social") || identifier.contains("friend")) {
            return EPerkCategory.SOCIAL;
        } else if (identifier.contains("particle") || identifier.contains("cosmetic") || identifier.contains("trail")) {
            return EPerkCategory.COSMETIC;
        } else {
            return EPerkCategory.SPECIAL;
        }
    }
    
    private long calculateRemainingCooldown(final @Nullable LocalDateTime cooldownExpiry) {
        if (cooldownExpiry == null) {
            return 0;
        }
        
        final LocalDateTime now = LocalDateTime.now();
        if (cooldownExpiry.isBefore(now) || cooldownExpiry.isEqual(now)) {
            return 0;
        }
        
        return ChronoUnit.SECONDS.between(now, cooldownExpiry);
    }
    
    private boolean canTogglePerk(final @NotNull RPerk perk, final @NotNull Player player, final @NotNull EPerkState state) {
        // Only toggleable perks can be toggled
        if (!perk.getPerkType().isToggleable()) {
            return false;
        }
        
        // Must be available or active to toggle
        return state == EPerkState.AVAILABLE || state == EPerkState.ACTIVE;
    }
    
    private boolean canActivatePerk(final @NotNull RPerk perk, final @NotNull Player player, final @NotNull EPerkState state) {
        // Must be available to activate
        if (state != EPerkState.AVAILABLE) {
            return false;
        }
        
        // Check permissions if required
        final String requiredPermission = perk.getRequiredPermission();
        if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
            return false;
        }
        
        return true;
    }
    
    private @NotNull PerkActivationResult validatePerkActivation(
        final @NotNull Player player,
        final @NotNull RPerk perk,
        final @NotNull RDQPlayer rdqPlayer
    ) {
        // Check if perk can be activated
        if (!perk.canPerformActivation()) {
            return new PerkActivationResult(false, "Perk cannot be activated at this time");
        }
        
        // Check permissions
        final String requiredPermission = perk.getRequiredPermission();
        if (requiredPermission != null && !player.hasPermission(requiredPermission)) {
            return new PerkActivationResult(false, "Insufficient permissions");
        }
        
        // Check concurrent user limit
        final Integer maxConcurrentUsers = perk.getMaxConcurrentUsers();
        if (maxConcurrentUsers != null) {
            final long currentActiveUsers = this.countActiveUsers(perk.getIdentifier());
            if (currentActiveUsers >= maxConcurrentUsers) {
                return new PerkActivationResult(false, "Maximum concurrent users reached");
            }
        }
        
        return new PerkActivationResult(true, "Validation passed");
    }
    
    private boolean performPerkActivation(final @NotNull Player player, final @NotNull RPerk perk, final @NotNull RDQPlayer rdqPlayer) {
        try {
            // Get or create player-perk relationship
            RPlayerPerk playerPerk = this.getOrCreatePlayerPerk(rdqPlayer, perk);
            
            // Perform perk-specific activation
            final boolean perkActivationSuccess = perk.performActivation();
            if (!perkActivationSuccess) {
                return false;
            }
            
            // Update database state
            playerPerk.setActive(true);
            playerPerk.setLastActivated(LocalDateTime.now());
            
            // Set cooldown if applicable
            if (perk.getPerkType().hasCooldown()) {
                // TODO: Get cooldown duration from perk configuration
                final LocalDateTime cooldownExpiry = LocalDateTime.now().plusMinutes(5); // Placeholder
                playerPerk.setCooldownExpiry(cooldownExpiry);
            }
            
            this.plugin.getPlayerPerkRepository().update(playerPerk);
            
            return true;
            
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error performing perk activation", e);
            return false;
        }
    }
    
    private boolean performPerkDeactivation(final @NotNull Player player, final @NotNull RPerk perk, final @NotNull RDQPlayer rdqPlayer) {
        try {
            // Find existing player-perk relationship
            final List<RPlayerPerk> playerPerks = this.plugin.getPlayerPerkRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "perk.identifier", perk.getIdentifier()
                ));
            
            if (playerPerks.isEmpty()) {
                return false;
            }
            
            final RPlayerPerk playerPerk = playerPerks.get(0);
            
            // Perform perk-specific deactivation
            final boolean perkDeactivationSuccess = perk.performDeactivation();
            if (!perkDeactivationSuccess) {
                return false;
            }
            
            // Update database state
            playerPerk.setActive(false);
            playerPerk.setLastDeactivated(LocalDateTime.now());
            
            this.plugin.getPlayerPerkRepository().update(playerPerk);
            
            return true;
            
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error performing perk deactivation", e);
            return false;
        }
    }
    
    private @NotNull RPlayerPerk getOrCreatePlayerPerk(final @NotNull RDQPlayer rdqPlayer, final @NotNull RPerk perk) {
        try {
            final List<RPlayerPerk> existing = this.plugin.getPlayerPerkRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "perk.identifier", perk.getIdentifier()
                ));
            
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
            
            // Create new relationship
            final RPlayerPerk newPlayerPerk = new RPlayerPerk(rdqPlayer, perk);
            this.plugin.getPlayerPerkRepository().create(newPlayerPerk);
            return newPlayerPerk;
            
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting or creating player perk", e);
            throw new RuntimeException("Failed to get or create player perk", e);
        }
    }
    
    private long countActiveUsers(final @NotNull String perkId) {
        try {
            return this.plugin.getPlayerPerkRepository()
                .findListByAttributes(Map.of(
                    "perk.identifier", perkId,
                    "active", true
                )).size();
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error counting active users for perk " + perkId, e);
            return 0;
        }
    }
    
    private void invalidatePlayerCache(final @NotNull UUID playerId) {
        this.playerCaches.remove(playerId);
    }
    
    private int comparePerkDisplayData(final @NotNull PerkDisplayData a, final @NotNull PerkDisplayData b) {
        // Sort by state priority first
        final int stateComparison = this.compareStates(a.getState(), b.getState());
        if (stateComparison != 0) {
            return stateComparison;
        }
        
        // Then by perk priority
        final int priorityComparison = Integer.compare(b.getPerk().getPriority(), a.getPerk().getPriority());
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        
        // Finally by identifier for consistent ordering
        return a.getIdentifier().compareTo(b.getIdentifier());
    }
    
    private int compareStates(final @NotNull EPerkState a, final @NotNull EPerkState b) {
        final int[] statePriority = {
            1, // LOCKED
            3, // AVAILABLE
            4, // ACTIVE
            2, // COOLDOWN
            0  // DISABLED
        };
        
        return Integer.compare(statePriority[b.ordinal()], statePriority[a.ordinal()]);
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Cache for player-specific perk data.
     */
    private static class PlayerPerkCache {
        final Set<String> ownedPerks = new HashSet<>();
        final Set<String> activePerks = new HashSet<>();
        final Map<String, LocalDateTime> cooldownExpiries = new HashMap<>();
        long lastUpdate = 0L;
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastUpdate > PLAYER_CACHE_TTL;
        }
    }
    
    /**
     * Result of a perk activation/deactivation operation.
     */
    public static class PerkActivationResult {
        private final boolean success;
        private final String message;
        
        public PerkActivationResult(final boolean success, final @NotNull String message) {
            this.success = success;
            this.message = Objects.requireNonNull(message, "Message cannot be null");
        }
        
        public boolean isSuccess() {
            return this.success;
        }
        
        public @NotNull String getMessage() {
            return this.message;
        }
    }
}