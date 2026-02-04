package com.raindropcentral.rplatform.requirement;

import com.raindropcentral.rplatform.requirement.event.RequirementCheckEvent;
import com.raindropcentral.rplatform.requirement.event.RequirementConsumeEvent;
import com.raindropcentral.rplatform.requirement.event.RequirementMetEvent;
import com.raindropcentral.rplatform.requirement.lifecycle.LifecycleRegistry;
import com.raindropcentral.rplatform.requirement.metrics.RequirementMetrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for checking, consuming, and calculating progress of requirements.
 * <p>
 * Provides caching, event firing, and lifecycle hook integration.
 * </p>
 */
public final class RequirementService {

    private static final Logger LOGGER = Logger.getLogger(RequirementService.class.getName());
    private static final RequirementService INSTANCE = new RequirementService();
    
    private static final long CACHE_EXPIRY_MS = TimeUnit.SECONDS.toMillis(30);

    private final Map<UUID, Map<String, CachedResult>> cache = new ConcurrentHashMap<>();
    private final LifecycleRegistry lifecycleRegistry = LifecycleRegistry.getInstance();
    private final RequirementMetrics metrics = RequirementMetrics.getInstance();

    private RequirementService() {}

    @NotNull
    public static RequirementService getInstance() {
        return INSTANCE;
    }

    // ==================== Core Operations ====================

    public boolean isMet(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        // Check cache first
        String cacheKey = getCacheKey(requirement);
        CachedResult cached = getCachedResult(player.getUniqueId(), cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            return cached.met;
        }
        
        // Fire check event
        RequirementCheckEvent event = new RequirementCheckEvent(player, requirement);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        
        // Execute lifecycle hooks
        if (!lifecycleRegistry.executeBeforeCheck(player, requirement)) {
            return false;
        }
        
        try {
            // Perform check
            long startTime = System.nanoTime();
            boolean met = requirement.isMet(player);
            double progress = requirement.calculateProgress(player);
            long duration = System.nanoTime() - startTime;
            
            // Record metrics
            metrics.recordCheck(requirement.getTypeId(), duration, met);
            
            // Cache result
            cacheResult(player.getUniqueId(), cacheKey, met, progress);
            
            // Execute after hooks
            lifecycleRegistry.executeAfterCheck(player, requirement, met, progress);
            
            // Fire met event if applicable
            if (met) {
                Bukkit.getPluginManager().callEvent(new RequirementMetEvent(player, requirement, progress));
            }
            
            return met;
        } catch (Exception e) {
            metrics.recordError(requirement.getTypeId());
            LOGGER.log(Level.SEVERE, "Error checking requirement: " + requirement.getTypeId(), e);
            lifecycleRegistry.executeOnError(player, requirement, e);
            return false;
        }
    }

    public double calculateProgress(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        String cacheKey = getCacheKey(requirement);
        CachedResult cached = getCachedResult(player.getUniqueId(), cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            return cached.progress;
        }
        
        try {
            double progress = requirement.calculateProgress(player);
            boolean met = progress >= 1.0 || requirement.isMet(player);
            cacheResult(player.getUniqueId(), cacheKey, met, progress);
            return progress;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating progress: " + requirement.getTypeId(), e);
            lifecycleRegistry.executeOnError(player, requirement, e);
            return 0.0;
        }
    }

    public void consume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        // Fire consume event
        RequirementConsumeEvent event = new RequirementConsumeEvent(player, requirement);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        
        // Execute lifecycle hooks
        if (!lifecycleRegistry.executeBeforeConsume(player, requirement)) {
            return;
        }
        
        try {
            long startTime = System.nanoTime();
            requirement.consume(player);
            long duration = System.nanoTime() - startTime;
            
            metrics.recordConsume(requirement.getTypeId(), duration);
            clearCache(player.getUniqueId(), getCacheKey(requirement));
            lifecycleRegistry.executeAfterConsume(player, requirement);
        } catch (Exception e) {
            metrics.recordError(requirement.getTypeId());
            LOGGER.log(Level.SEVERE, "Error consuming requirement: " + requirement.getTypeId(), e);
            lifecycleRegistry.executeOnError(player, requirement, e);
        }
    }

    // ==================== Batch Operations ====================

    public double calculateOverallProgress(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        if (requirements.isEmpty()) return 1.0;
        
        return requirements.stream()
            .mapToDouble(req -> calculateProgress(player, req))
            .average()
            .orElse(0.0);
    }

    public void consumeAll(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        requirements.forEach(req -> consume(player, req));
    }

    public boolean areAllMet(@NotNull Player player, @NotNull List<AbstractRequirement> requirements) {
        return requirements.stream().allMatch(req -> isMet(player, req));
    }

    // ==================== Cache Management ====================

    public void clearCache(@NotNull UUID playerId) {
        cache.remove(playerId);
    }

    public void clearCache(@NotNull UUID playerId, @NotNull String cacheKey) {
        Map<String, CachedResult> playerCache = cache.get(playerId);
        if (playerCache != null) {
            playerCache.remove(cacheKey);
        }
    }

    public void clearAllCache() {
        cache.clear();
    }

    public void cleanupExpiredCache() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(cacheEntry -> cacheEntry.getValue().isExpired(now));
            return entry.getValue().isEmpty();
        });
    }

    // ==================== Internal Methods ====================

    String getCacheKey(@NotNull AbstractRequirement requirement) {
        return requirement.getTypeId() + "_" + requirement.hashCode();
    }

    CachedResult getCachedResult(@NotNull UUID playerId, @NotNull String cacheKey) {
        Map<String, CachedResult> playerCache = cache.get(playerId);
        return playerCache != null ? playerCache.get(cacheKey) : null;
    }

    void cacheResult(@NotNull UUID playerId, @NotNull String cacheKey, boolean met, double progress) {
        cache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(cacheKey, new CachedResult(met, progress, System.currentTimeMillis()));
    }

    record CachedResult(boolean met, double progress, long timestamp) {
        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        boolean isExpired(long now) {
            return now - timestamp > CACHE_EXPIRY_MS;
        }
    }
}
