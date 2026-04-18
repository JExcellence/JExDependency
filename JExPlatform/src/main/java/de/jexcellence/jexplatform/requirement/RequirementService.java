package de.jexcellence.jexplatform.requirement;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.requirement.event.RequirementCheckEvent;
import de.jexcellence.jexplatform.requirement.event.RequirementConsumeEvent;
import de.jexcellence.jexplatform.requirement.event.RequirementMetEvent;
import de.jexcellence.jexplatform.requirement.lifecycle.LifecycleRegistry;
import de.jexcellence.jexplatform.requirement.metrics.RequirementMetrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for checking, consuming, and calculating progress of requirements.
 *
 * <p>Provides caching (30 s TTL), event firing, lifecycle hook integration,
 * and metrics recording. Not a singleton — create one per platform instance.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RequirementService {

    private static final long CACHE_EXPIRY_MS = TimeUnit.SECONDS.toMillis(30);

    private final RequirementRegistry registry;
    private final JExLogger logger;
    private final LifecycleRegistry lifecycleRegistry;
    private final RequirementMetrics metrics;
    private final Map<UUID, Map<String, CachedResult>> cache = new ConcurrentHashMap<>();

    /**
     * Creates a new requirement service.
     *
     * @param registry          the requirement registry
     * @param logger            the platform logger
     * @param lifecycleRegistry the lifecycle hook registry
     * @param metrics           the metrics collector
     */
    public RequirementService(@NotNull RequirementRegistry registry,
                              @NotNull JExLogger logger,
                              @NotNull LifecycleRegistry lifecycleRegistry,
                              @NotNull RequirementMetrics metrics) {
        this.registry = registry;
        this.logger = logger;
        this.lifecycleRegistry = lifecycleRegistry;
        this.metrics = metrics;
    }

    // ── Core operations ───────────────────────────────────────────────────────

    /**
     * Checks whether the player meets the given requirement.
     *
     * @param player      the player to evaluate
     * @param requirement the requirement to check
     * @return {@code true} when the requirement is met
     */
    public boolean isMet(@NotNull Player player,
                         @NotNull AbstractRequirement requirement) {
        var cacheKey = getCacheKey(requirement);
        var cached = getCachedResult(player.getUniqueId(), cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.met;
        }

        var event = new RequirementCheckEvent(player, requirement);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        if (!lifecycleRegistry.executeBeforeCheck(player, requirement)) {
            return false;
        }

        try {
            var startTime = System.nanoTime();
            var met = requirement.isMet(player);
            var progress = requirement.calculateProgress(player);
            var duration = System.nanoTime() - startTime;

            metrics.recordCheck(requirement.typeId(), duration, met);
            cacheResult(player.getUniqueId(), cacheKey, met, progress);
            lifecycleRegistry.executeAfterCheck(player, requirement, met, progress);

            if (met) {
                Bukkit.getPluginManager().callEvent(
                        new RequirementMetEvent(player, requirement, progress));
            }

            return met;
        } catch (Exception e) {
            metrics.recordError(requirement.typeId());
            logger.error("Error checking requirement: {}", requirement.typeId(), e);
            lifecycleRegistry.executeOnError(player, requirement, e);
            return false;
        }
    }

    /**
     * Calculates the player's progress towards meeting a requirement.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return progress between {@code 0.0} and {@code 1.0}
     */
    public double calculateProgress(@NotNull Player player,
                                    @NotNull AbstractRequirement requirement) {
        var cacheKey = getCacheKey(requirement);
        var cached = getCachedResult(player.getUniqueId(), cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.progress;
        }

        try {
            var progress = requirement.calculateProgress(player);
            var met = progress >= 1.0 || requirement.isMet(player);
            cacheResult(player.getUniqueId(), cacheKey, met, progress);
            return progress;
        } catch (Exception e) {
            logger.error("Error calculating progress: {}", requirement.typeId(), e);
            lifecycleRegistry.executeOnError(player, requirement, e);
            return 0.0;
        }
    }

    /**
     * Consumes the resources required by the given requirement.
     *
     * @param player      the player
     * @param requirement the requirement
     */
    public void consume(@NotNull Player player,
                        @NotNull AbstractRequirement requirement) {
        var event = new RequirementConsumeEvent(player, requirement);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        if (!lifecycleRegistry.executeBeforeConsume(player, requirement)) {
            return;
        }

        try {
            var startTime = System.nanoTime();
            requirement.consume(player);
            var duration = System.nanoTime() - startTime;

            metrics.recordConsume(requirement.typeId(), duration);
            clearCache(player.getUniqueId(), getCacheKey(requirement));
            lifecycleRegistry.executeAfterConsume(player, requirement);
        } catch (Exception e) {
            metrics.recordError(requirement.typeId());
            logger.error("Error consuming requirement: {}", requirement.typeId(), e);
            lifecycleRegistry.executeOnError(player, requirement, e);
        }
    }

    // ── Batch operations ──────────────────────────────────────────────────────

    /**
     * Checks whether all requirements are met.
     *
     * @param player       the player
     * @param requirements the requirements to check
     * @return {@code true} when all are met
     */
    public boolean areAllMet(@NotNull Player player,
                             @NotNull List<AbstractRequirement> requirements) {
        return requirements.stream().allMatch(req -> isMet(player, req));
    }

    /**
     * Consumes all requirements.
     *
     * @param player       the player
     * @param requirements the requirements to consume
     */
    public void consumeAll(@NotNull Player player,
                           @NotNull List<AbstractRequirement> requirements) {
        requirements.forEach(req -> consume(player, req));
    }

    /**
     * Calculates the average progress across all requirements.
     *
     * @param player       the player
     * @param requirements the requirements
     * @return average progress
     */
    public double calculateOverallProgress(@NotNull Player player,
                                           @NotNull List<AbstractRequirement> requirements) {
        if (requirements.isEmpty()) {
            return 1.0;
        }
        return requirements.stream()
                .mapToDouble(req -> calculateProgress(player, req))
                .average()
                .orElse(0.0);
    }

    // ── Cache management ──────────────────────────────────────────────────────

    /**
     * Clears all cached results for a player.
     *
     * @param playerId the player UUID
     */
    public void clearCache(@NotNull UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Clears all cached results.
     */
    public void clearAllCache() {
        cache.clear();
    }

    /**
     * Removes expired cache entries.
     */
    public void cleanupExpiredCache() {
        var now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(
                    cacheEntry -> cacheEntry.getValue().isExpired(now));
            return entry.getValue().isEmpty();
        });
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private String getCacheKey(@NotNull AbstractRequirement requirement) {
        return requirement.typeId() + "_" + requirement.hashCode();
    }

    private CachedResult getCachedResult(@NotNull UUID playerId, @NotNull String key) {
        var playerCache = cache.get(playerId);
        return playerCache != null ? playerCache.get(key) : null;
    }

    private void cacheResult(@NotNull UUID playerId, @NotNull String key,
                             boolean met, double progress) {
        cache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(key, new CachedResult(met, progress, System.currentTimeMillis()));
    }

    private void clearCache(@NotNull UUID playerId, @NotNull String key) {
        var playerCache = cache.get(playerId);
        if (playerCache != null) {
            playerCache.remove(key);
        }
    }

    private record CachedResult(boolean met, double progress, long timestamp) {
        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long now) {
            return now - timestamp > CACHE_EXPIRY_MS;
        }
    }
}
