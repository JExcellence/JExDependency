/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.service.central.cookie;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.database.entity.statistic.RStringStatistic;
import com.raindropcentral.core.service.RPlayerStatisticService;
import com.raindropcentral.rplatform.cookie.CookieBoostLookup;
import com.raindropcentral.rplatform.cookie.CookieBoostType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks active timed droplet cookie boosts and persists them through player statistics.
 */
public final class ActiveCookieBoostService implements CookieBoostLookup {

    private static final long CLEANUP_INTERVAL_TICKS = 20L * 60L;

    private final RCoreImpl plugin;
    private final Logger logger;
    private final String statisticPlugin;
    private final Map<UUID, Map<String, ActiveCookieBoost>> boostsByPlayer = new ConcurrentHashMap<>();

    /**
     * Creates the runtime service and starts periodic cleanup of expired online-player boosts.
     *
     * @param plugin active RCore implementation providing repositories and scheduler access
     */
    public ActiveCookieBoostService(final @NotNull RCoreImpl plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPlugin().getLogger();
        this.statisticPlugin = plugin.getPlugin().getName();
        this.plugin.getPlatform().getScheduler().runRepeating(this::cleanupOnlinePlayers, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
    }

    /** {@inheritDoc} */
    @Override
    public double getMultiplier(
            final @NotNull UUID playerId,
            final @NotNull CookieBoostType boostType,
            final @Nullable String integrationId,
            final @Nullable String targetId
    ) {
        final ActiveCookieBoost boost = this.findActiveBoost(playerId, boostType, integrationId, targetId);
        return boost == null ? 1.0D : boost.multiplier();
    }

    /**
     * Activates or refreshes a timed boost for the supplied player and target.
     *
     * @param player player receiving the boost
     * @param definition droplet cookie definition being redeemed
     * @param integrationId optional integration identifier for scoped boosts
     * @param targetId optional skill or job identifier for scoped boosts
     * @return future describing whether persistence succeeded and whether an earlier boost was replaced
     */
    public @NotNull CompletableFuture<BoostActivationResult> activateBoost(
            final @NotNull Player player,
            final @NotNull DropletCookieDefinition definition,
            final @Nullable String integrationId,
            final @Nullable String targetId
    ) {
        if (!definition.isTimedBoost() || definition.effectType().boostType() == null) {
            return CompletableFuture.completedFuture(new BoostActivationResult(false, false));
        }

        final long expiresAt = System.currentTimeMillis() + (definition.durationSeconds() * 1000L);
        final ActiveCookieBoost boost = new ActiveCookieBoost(
                player.getUniqueId(),
                definition.effectType().boostType(),
                integrationId,
                targetId,
                definition.itemCode(),
                definition.rateBonus(),
                expiresAt
        );

        final Map<String, ActiveCookieBoost> playerBoosts = this.boostsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>());
        final ActiveCookieBoost previous = playerBoosts.put(boost.cacheKey(), boost);
        final boolean replaced = previous != null;

        return this.persistBoost(player, boost)
                .thenApply(success -> {
                    if (success) {
                        return new BoostActivationResult(true, replaced);
                    }

                    this.restorePreviousBoost(player.getUniqueId(), boost.cacheKey(), previous);
                    return new BoostActivationResult(false, replaced);
                });
    }

    /**
     * Loads persisted boosts for the supplied player into the in-memory runtime cache.
     *
     * @param player player whose boost statistics should be hydrated
     */
    public void hydratePlayer(final @NotNull OfflinePlayer player) {
        this.plugin.getPlayerRepository().findByUuidAsync(player.getUniqueId())
                .thenCompose(optionalPlayer -> optionalPlayer
                        .map(rPlayer -> this.applyHydratedBoosts(player.getUniqueId(), rPlayer))
                        .orElseGet(() -> CompletableFuture.completedFuture(null)))
                .exceptionally(throwable -> {
                    this.logger.log(Level.WARNING, "Failed to hydrate active droplet boosts for " + player.getUniqueId(), throwable);
                    return null;
                });
    }

    /**
     * Hydrates boost state for every currently connected player.
     */
    public void hydrateOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(this::hydratePlayer);
    }

    /**
     * Removes all cached boosts for the supplied player.
     *
     * @param playerId player whose runtime boost cache should be cleared
     */
    public void removePlayerCache(final @NotNull UUID playerId) {
        this.boostsByPlayer.remove(playerId);
    }

    private @Nullable ActiveCookieBoost findActiveBoost(
            final @NotNull UUID playerId,
            final @NotNull CookieBoostType boostType,
            final @Nullable String integrationId,
            final @Nullable String targetId
    ) {
        final Map<String, ActiveCookieBoost> playerBoosts = this.boostsByPlayer.get(playerId);
        if (playerBoosts == null || playerBoosts.isEmpty()) {
            return null;
        }

        final String cacheKey = boostType.name() + "|" + normalize(integrationId) + "|" + normalize(targetId);
        final ActiveCookieBoost boost = playerBoosts.get(cacheKey);
        if (boost == null) {
            return null;
        }
        if (!boost.isExpired(System.currentTimeMillis())) {
            return boost;
        }

        playerBoosts.remove(cacheKey);
        this.removePersistedBoostAsync(playerId, boost.statisticIdentifier());
        return null;
    }

    private @NotNull CompletableFuture<Boolean> persistBoost(
            final @NotNull Player player,
            final @NotNull ActiveCookieBoost boost
    ) {
        return this.plugin.getPlayerRepository().findByUuidAsync(player.getUniqueId())
                .thenCompose(optionalPlayer -> {
                    final RPlayer rPlayer = optionalPlayer.orElseGet(() -> this.createPlayerAggregate(player));
                    if (rPlayer.getPlayerStatistic() == null) {
                        rPlayer.setPlayerStatistic(new RPlayerStatistic(rPlayer));
                    }

                    RPlayerStatisticService.addOrUpdateStatistic(
                            rPlayer.getPlayerStatistic(),
                            boost.statisticIdentifier(),
                            this.statisticPlugin,
                            boost.serialize()
                    );

                    return this.plugin.getPlayerRepository().createOrUpdateAsync(rPlayer).thenApply(saved -> true);
                })
                .exceptionally(throwable -> {
                    this.logger.log(Level.WARNING, "Failed to persist active droplet boost " + boost.statisticIdentifier(), throwable);
                    return false;
                });
    }

    private @NotNull CompletableFuture<Void> applyHydratedBoosts(
            final @NotNull UUID playerId,
            final @NotNull RPlayer rPlayer
    ) {
        final RPlayerStatistic statistic = rPlayer.getPlayerStatistic();
        if (statistic == null) {
            this.boostsByPlayer.remove(playerId);
            return CompletableFuture.completedFuture(null);
        }

        final long now = System.currentTimeMillis();
        final Map<String, ActiveCookieBoost> hydrated = new ConcurrentHashMap<>();
        final List<String> expiredIdentifiers = new ArrayList<>();

        for (final RAbstractStatistic entry : statistic.getStatistics()) {
            if (!(entry instanceof RStringStatistic stringStatistic)) {
                continue;
            }

            final Optional<ActiveCookieBoost> boost = ActiveCookieBoost.deserialize(playerId, entry.getIdentifier(), stringStatistic.getValue());
            if (boost.isEmpty()) {
                continue;
            }

            if (boost.get().isExpired(now)) {
                expiredIdentifiers.add(boost.get().statisticIdentifier());
                continue;
            }
            hydrated.put(boost.get().cacheKey(), boost.get());
        }

        if (hydrated.isEmpty()) {
            this.boostsByPlayer.remove(playerId);
        } else {
            this.boostsByPlayer.put(playerId, hydrated);
        }

        if (expiredIdentifiers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        expiredIdentifiers.forEach(identifier -> RPlayerStatisticService.removeStatistic(statistic, identifier, this.statisticPlugin));
        return this.plugin.getPlayerRepository().createOrUpdateAsync(rPlayer).thenApply(saved -> null);
    }

    private void cleanupOnlinePlayers() {
        final long now = System.currentTimeMillis();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final Map<String, ActiveCookieBoost> playerBoosts = this.boostsByPlayer.get(player.getUniqueId());
            if (playerBoosts == null || playerBoosts.isEmpty()) {
                continue;
            }

            final List<ActiveCookieBoost> expired = playerBoosts.values().stream()
                    .filter(boost -> boost.isExpired(now))
                    .toList();
            if (expired.isEmpty()) {
                continue;
            }

            expired.forEach(boost -> playerBoosts.remove(boost.cacheKey()));
            expired.forEach(boost -> this.removePersistedBoostAsync(player.getUniqueId(), boost.statisticIdentifier()));
            if (playerBoosts.isEmpty()) {
                this.boostsByPlayer.remove(player.getUniqueId());
            }
        }
    }

    private void removePersistedBoostAsync(final @NotNull UUID playerId, final @NotNull String statisticIdentifier) {
        this.plugin.getPlayerRepository().findByUuidAsync(playerId)
                .thenCompose(optionalPlayer -> {
                    if (optionalPlayer.isEmpty() || optionalPlayer.get().getPlayerStatistic() == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    final RPlayer rPlayer = optionalPlayer.get();
                    RPlayerStatisticService.removeStatistic(rPlayer.getPlayerStatistic(), statisticIdentifier, this.statisticPlugin);
                    return this.plugin.getPlayerRepository().createOrUpdateAsync(rPlayer).thenApply(saved -> null);
                })
                .exceptionally(throwable -> {
                    this.logger.log(Level.FINE, "Failed to remove expired droplet boost " + statisticIdentifier, throwable);
                    return null;
                });
    }

    private void restorePreviousBoost(
            final @NotNull UUID playerId,
            final @NotNull String cacheKey,
            final @Nullable ActiveCookieBoost previous
    ) {
        final Map<String, ActiveCookieBoost> playerBoosts = this.boostsByPlayer.get(playerId);
        if (playerBoosts == null) {
            return;
        }

        if (previous == null) {
            playerBoosts.remove(cacheKey);
        } else {
            playerBoosts.put(cacheKey, previous);
        }

        if (playerBoosts.isEmpty()) {
            this.boostsByPlayer.remove(playerId);
        }
    }

    private @NotNull RPlayer createPlayerAggregate(final @NotNull Player player) {
        final RPlayer rPlayer = new RPlayer(player);
        rPlayer.setPlayerStatistic(new RPlayerStatistic(rPlayer));
        return rPlayer;
    }

    private static @NotNull String normalize(final @Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * Result of attempting to activate a persisted droplet boost.
     *
     * @param success whether the boost was stored successfully
     * @param replaced whether an earlier boost with the same scope was replaced
     */
    public record BoostActivationResult(boolean success, boolean replaced) {
    }
}
