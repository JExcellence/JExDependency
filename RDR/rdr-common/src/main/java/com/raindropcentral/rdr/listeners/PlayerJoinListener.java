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

package com.raindropcentral.rdr.listeners;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.repository.RRDRPlayer;
import com.raindropcentral.rdr.service.scoreboard.StorageSidebarScoreboardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;

/**
 * Player join listener that provisions persistent RDR storage profiles.
 *
 * <p>When a player joins for the first time, the listener creates the backing {@link RDRPlayer} entity and
 * provisions the configured number of initial {@link RStorage} rows.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@SuppressWarnings("unused")
public class PlayerJoinListener implements Listener {

    private final Supplier<PlayerProfileRepository> repositorySupplier;
    private final Supplier<ConfigSection> configSupplier;
    private final Supplier<StorageSidebarScoreboardService> scoreboardServiceSupplier;
    private final ToIntFunction<ConfigSection> initialStorageResolver;
    private final Logger logger;

    /**
     * Creates a listener bound to the active plugin instance.
     *
     * @param rdr plugin instance used to resolve configuration and repositories
     * @throws NullPointerException if {@code rdr} is {@code null}
     */
    public PlayerJoinListener(final @NotNull RDR rdr) {
        this(
            () -> RepositoryBackedPlayerProfileRepository.from(rdr.getPlayerRepository()),
            rdr::getDefaultConfig,
            rdr::getStorageSidebarScoreboardService,
            rdr::getInitialProvisionedStorages,
            rdr.getLogger()
        );
    }

    /**
     * Creates a listener with explicit collaborators.
     *
     * @param repositorySupplier repository supplier used to resolve persistent player profiles
     * @param configSupplier configuration supplier used to resolve storage defaults
     * @param logger logger used for provisioning failures
     * @throws NullPointerException if any non-null collaborator is {@code null}
     */
    PlayerJoinListener(
        final @NotNull Supplier<PlayerProfileRepository> repositorySupplier,
        final @NotNull Supplier<ConfigSection> configSupplier,
        final @NotNull Logger logger
    ) {
        this(repositorySupplier, configSupplier, () -> null, ConfigSection::getInitialProvisionedStorages, logger);
    }

    /**
     * Creates a listener with explicit collaborators.
     *
     * @param repositorySupplier repository supplier used to resolve persistent player profiles
     * @param configSupplier configuration supplier used to resolve storage defaults
     * @param scoreboardServiceSupplier supplier used to resolve the optional storage scoreboard service
     * @param logger logger used for provisioning failures
     * @throws NullPointerException if any non-null collaborator is {@code null}
     */
    PlayerJoinListener(
        final @NotNull Supplier<PlayerProfileRepository> repositorySupplier,
        final @NotNull Supplier<ConfigSection> configSupplier,
        final @NotNull Supplier<StorageSidebarScoreboardService> scoreboardServiceSupplier,
        final @NotNull Logger logger
    ) {
        this(
            repositorySupplier,
            configSupplier,
            scoreboardServiceSupplier,
            ConfigSection::getInitialProvisionedStorages,
            logger
        );
    }

    /**
     * Creates a listener with explicit collaborators.
     *
     * @param repositorySupplier repository supplier used to resolve persistent player profiles
     * @param configSupplier configuration supplier used to resolve storage defaults
     * @param scoreboardServiceSupplier supplier used to resolve the optional storage scoreboard service
     * @param initialStorageResolver resolver used to calculate first-join storage provisioning
     * @param logger logger used for provisioning failures
     * @throws NullPointerException if any non-null collaborator is {@code null}
     */
    PlayerJoinListener(
        final @NotNull Supplier<PlayerProfileRepository> repositorySupplier,
        final @NotNull Supplier<ConfigSection> configSupplier,
        final @NotNull Supplier<StorageSidebarScoreboardService> scoreboardServiceSupplier,
        final @NotNull ToIntFunction<ConfigSection> initialStorageResolver,
        final @NotNull Logger logger
    ) {
        this.repositorySupplier = Objects.requireNonNull(repositorySupplier, "repositorySupplier");
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
        this.scoreboardServiceSupplier = Objects.requireNonNull(scoreboardServiceSupplier, "scoreboardServiceSupplier");
        this.initialStorageResolver = Objects.requireNonNull(initialStorageResolver, "initialStorageResolver");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Creates a new storage profile for first-time players when no persisted profile exists yet.
     *
     * @param event Bukkit player join event
     */
    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final PlayerProfileRepository repository = this.repositorySupplier.get();
        if (repository == null) {
            this.logger.warning("Player repository was unavailable during join handling.");
            return;
        }

        final Player player = event.getPlayer();
        final RDRPlayer existingPlayer = repository.findByPlayer(player.getUniqueId());
        if (existingPlayer != null) {
            this.restoreSidebarScoreboard(player, existingPlayer);
            return;
        }

        final ConfigSection config = this.configSupplier.get();
        final RDRPlayer newPlayer = new RDRPlayer(player.getUniqueId());
        final int initialStorages = this.initialStorageResolver.applyAsInt(config);
        for (int index = 1; index <= initialStorages; index++) {
            new RStorage(newPlayer, this.buildDefaultStorageKey(index), 54);
        }

        repository.createAsync(newPlayer).exceptionally(throwable -> {
            this.logger.warning(
                "Failed to create RDR player profile for " + player.getUniqueId() + ": " + throwable.getMessage()
            );
            return null;
        });
    }

    /**
     * Clears any active sidebar scoreboard for players leaving the server.
     *
     * @param event Bukkit player quit event
     */
    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        final StorageSidebarScoreboardService scoreboardService = this.scoreboardServiceSupplier.get();
        if (scoreboardService == null) {
            return;
        }
        scoreboardService.disable(event.getPlayer());
    }

    private void restoreSidebarScoreboard(
        final @NotNull Player player,
        final @NotNull RDRPlayer playerData
    ) {
        if (!playerData.isSidebarScoreboardEnabled()) {
            return;
        }

        final StorageSidebarScoreboardService scoreboardService = this.scoreboardServiceSupplier.get();
        if (scoreboardService == null) {
            return;
        }
        scoreboardService.enable(player);
    }

    private @NotNull String buildDefaultStorageKey(final int index) {
        return "storage-" + index;
    }
}

/**
 * Represents player profile repository.
 */
interface PlayerProfileRepository {

    /**
     * Finds by player.
     *
     * @param playerUuid player identifier to look up
     * @return the matched by player, or {@code null} when none exists
     */
    RDRPlayer findByPlayer(@NotNull UUID playerUuid);

    /**
     * Creates async.
     *
     * @param player target player
     * @return the create async result
     */
    CompletableFuture<RDRPlayer> createAsync(@NotNull RDRPlayer player);
}

/**
 * Represents repository backed player profile repository.
 */
final class RepositoryBackedPlayerProfileRepository implements PlayerProfileRepository {

    private final RRDRPlayer repository;

    private RepositoryBackedPlayerProfileRepository(final @NotNull RRDRPlayer repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    static PlayerProfileRepository from(final RRDRPlayer repository) {
        return repository == null ? null : new RepositoryBackedPlayerProfileRepository(repository);
    }

    /**
     * Finds by player.
     *
     * @param playerUuid player identifier to look up
     * @return the matched by player, or {@code null} when none exists
     */
    @Override
    public RDRPlayer findByPlayer(final @NotNull UUID playerUuid) {
        return this.repository.findByPlayer(playerUuid);
    }

    /**
     * Creates async.
     *
     * @param player target player
     * @return the create async result
     */
    @Override
    public CompletableFuture<RDRPlayer> createAsync(final @NotNull RDRPlayer player) {
        return this.repository.createAsync(player);
    }
}
