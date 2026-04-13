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

package com.raindropcentral.rda;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rda.database.entity.*;
import com.raindropcentral.rda.database.repository.*;
import com.raindropcentral.rda.view.RaMainView;
import com.raindropcentral.rda.view.RaSkillView;
import com.raindropcentral.rda.view.RaSkillsView;
import com.raindropcentral.rda.view.RaStatSettingsView;
import com.raindropcentral.rda.view.RaStatView;
import com.raindropcentral.rda.view.RaTriggerSettingsView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.view.ConfirmationView;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared runtime bootstrap for RDA editions.
 *
 * <p>The runtime initializes the shared platform, repositories, generic skill configurations,
 * listeners, commands, and inventory views used by both plugin editions.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.1.0
 */
@SuppressWarnings("FieldCanBeLocal")
public final class RDA {

    public static final String MANA_BOSS_BAR_PROVIDER_KEY = "rda.mana";

    private final JavaPlugin plugin;
    private final String edition;
    private final EnumMap<SkillType, SkillRuntime> skillRuntimes = new EnumMap<>(SkillType.class);
    private final EnumMap<Material, SkillType> blockBreakMaterialOwners = new EnumMap<>(Material.class);
    private final EnumSet<SkillType> naturalBlockSuppressedSkills = EnumSet.of(
        SkillType.MINING,
        SkillType.WOODCUTTING,
        SkillType.EXCAVATION
    );

    private @Nullable ExecutorService executor;
    private @Nullable RPlatform platform;
    private @Nullable EntityManagerFactory entityManagerFactory;
    private @Nullable ViewFrame viewFrame;
    private @Nullable RRDAPlayer playerRepository;
    private @Nullable RRDASkillState skillStateRepository;
    private @Nullable RRDAPlayerBuild playerBuildRepository;
    private @Nullable RRDAStatAllocation statAllocationRepository;
    private @Nullable RRDASkillPreference skillPreferenceRepository;
    private @Nullable RRDAAgilityVisitedChunk agilityVisitedChunkRepository;
    private @Nullable RRDAPlacedSkillBlock placedSkillBlockRepository;
    private @Nullable RRDAParty partyRepository;
    private @Nullable RRDAPartyMember partyMemberRepository;
    private @Nullable RRDAPartyInvite partyInviteRepository;
    private @Nullable AgilityChunkVisitService agilityChunkVisitService;
    private @Nullable StatsConfig statsConfig;
    private @Nullable PartyConfig partyConfig;
    private @Nullable PlayerBuildService playerBuildService;
    private @Nullable ManaBossBarIntegration manaBossBarIntegration;
    private @Nullable PartyService partyService;
    private volatile @Nullable CompletableFuture<Void> enableFuture;

    /**
     * Creates a new shared RDA runtime.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used for logs
     */
    public RDA(final @NotNull JavaPlugin plugin, final @NotNull String edition) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.edition = Objects.requireNonNull(edition, "edition");
    }

    /**
     * Allocates shared platform state before plugin enable.
     */
    public void onLoad() {
        this.getLogger().info("Loading RPlatform for RDA (" + this.edition + ")");
        this.platform = new RPlatform(this.plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes persistence, skill systems, commands, listeners, and views.
     */
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            this.getLogger().warning("Enable sequence already in progress for RDA (" + this.edition + ")");
            return;
        }

        if (this.platform == null || this.executor == null) {
            throw new IllegalStateException("RDA must be loaded before it can be enabled.");
        }

        this.getLogger().info("Enabling RDA (" + this.edition + ") Edition");
        this.enableFuture = this.platform.initialize()
            .thenCompose(ignored -> this.runSync(() -> {
                this.entityManagerFactory = this.requireEntityManagerFactory();
                this.initializeRepositories();
                this.initializeSkillSystems();
                this.initializeBossBarIntegration();
                this.initializeViews();
                this.initializeComponents();
                this.getLogger().info("RDA (" + this.edition + ") Edition enabled successfully");
            }))
            .exceptionally(throwable -> {
                this.runSync(() -> {
                    this.getLogger().log(Level.SEVERE, "Failed to initialize RDA (" + this.edition + ")", throwable);
                    this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                });
                return null;
            });
    }

    /**
     * Shuts down asynchronous services and closes the entity manager factory.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDA (" + this.edition + ")");
        this.skillRuntimes.clear();
        this.blockBreakMaterialOwners.clear();

        if (this.manaBossBarIntegration != null) {
            this.manaBossBarIntegration.unregister();
            this.manaBossBarIntegration = null;
        }

        if (this.playerBuildService != null) {
            this.playerBuildService.shutdown();
        }

        if (this.entityManagerFactory != null) {
            try {
                this.entityManagerFactory.close();
            } catch (final Exception exception) {
                this.getLogger().warning("Failed to close the RDA entity manager factory: " + exception.getMessage());
            }
        }

        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
        }

        if (this.platform != null) {
            this.platform.shutdown();
        }
    }

    /**
     * Returns the runtime logger exposed by the owning Bukkit plugin.
     *
     * @return plugin logger
     */
    public @NotNull Logger getLogger() {
        return this.plugin.getLogger();
    }

    /**
     * Returns the active plugin edition label.
     *
     * @return edition label
     */
    public @NotNull String getEdition() {
        return this.edition;
    }

    /**
     * Returns the shared platform runtime once loaded.
     *
     * @return platform runtime, or {@code null} before load completes
     */
    public @Nullable RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Returns the shared executor once loaded.
     *
     * @return shared executor, or {@code null} before load completes
     */
    public @Nullable ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Returns the registered Inventory Framework view frame.
     *
     * @return view frame, or {@code null} before enable completes
     */
    public @Nullable ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    /**
     * Returns the player repository once RDA has been enabled.
     *
     * @return player repository, or {@code null} before enable completes
     */
    public @Nullable RRDAPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Returns the shared build service once RDA has been enabled.
     *
     * @return build service, or {@code null} before enable completes
     */
    public @Nullable PlayerBuildService getPlayerBuildService() {
        return this.playerBuildService;
    }

    /**
     * Returns the optional RCore-backed mana boss bar integration bridge.
     *
     * @return optional mana boss bar integration bridge
     */
    public @Nullable ManaBossBarIntegration getManaBossBarIntegration() {
        return this.manaBossBarIntegration;
    }

    /**
     * Returns the shared party service once RDA has been enabled.
     *
     * @return party service, or {@code null} before enable completes
     */
    public @Nullable PartyService getPartyService() {
        return this.partyService;
    }

    /**
     * Returns the loaded stats configuration once enable completes.
     *
     * @return stats configuration, or {@code null} before enable completes
     */
    public @Nullable StatsConfig getStatsConfig() {
        return this.statsConfig;
    }

    /**
     * Returns the loaded skill configuration for the supplied skill.
     *
     * @param skillType skill to resolve
     * @return skill configuration, or {@code null} before enable completes
     */
    public @Nullable SkillConfig getSkillConfig(final @NotNull SkillType skillType) {
        final SkillRuntime runtime = this.skillRuntimes.get(Objects.requireNonNull(skillType, "skillType"));
        return runtime == null ? null : runtime.config();
    }

    /**
     * Returns the progression service for the supplied skill.
     *
     * @param skillType skill to resolve
     * @return progression service, or {@code null} before enable completes
     */
    public @Nullable SkillProgressionService getSkillProgressionService(final @NotNull SkillType skillType) {
        final SkillRuntime runtime = this.skillRuntimes.get(Objects.requireNonNull(skillType, "skillType"));
        return runtime == null ? null : runtime.progressionService();
    }

    /**
     * Returns the placed-block suppression service for the supplied skill.
     *
     * @param skillType skill to resolve
     * @return placed-block suppression service, or {@code null} before enable completes
     */
    public @Nullable PlacedTrackedBlockService<?> getPlacedTrackedBlockService(final @NotNull SkillType skillType) {
        final SkillRuntime runtime = this.skillRuntimes.get(Objects.requireNonNull(skillType, "skillType"));
        return runtime == null ? null : runtime.placedBlockService();
    }

    /**
     * Returns the profile snapshot for the supplied player and skill.
     *
     * @param skillType skill to resolve
     * @param player player whose snapshot should be resolved
     * @return skill snapshot, or an empty snapshot when the runtime is unavailable
     */
    public @NotNull SkillProfileSnapshot getSkillSnapshot(
        final @NotNull SkillType skillType,
        final @NotNull Player player
    ) {
        final SkillProgressionService service = this.getSkillProgressionService(skillType);
        return service == null ? SkillProfileSnapshot.empty(skillType) : service.getSnapshot(player);
    }

    /**
     * Returns the current build snapshot for the supplied player.
     *
     * @param player player whose build should be resolved
     * @return build snapshot, or an empty baseline snapshot when unavailable
     */
    public @NotNull PlayerBuildSnapshot getBuildSnapshot(final @NotNull Player player) {
        final PlayerBuildService buildService = this.playerBuildService;
        if (buildService == null) {
            return new PlayerBuildSnapshot(
                0,
                0,
                0,
                0.0D,
                0.0D,
                0.0D,
                ManaDisplayMode.ACTION_BAR,
                Map.of()
            );
        }
        return buildService.getBuildSnapshot(player);
    }

    /**
     * Returns every ability snapshot for the supplied player and skill.
     *
     * @param player player whose ability tiers should be resolved
     * @param skillType skill whose abilities should be resolved
     * @return resolved ability snapshots
     */
    public @NotNull List<AbilitySnapshot> getAbilitySnapshots(
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        final PlayerBuildService buildService = this.playerBuildService;
        return buildService == null ? List.of() : buildService.getAbilitySnapshots(player, skillType);
    }

    /**
     * Returns every ability snapshot associated with the supplied stat.
     *
     * @param player player whose build should be resolved
     * @param coreStatType stat whose abilities should be resolved
     * @return resolved stat-linked abilities
     */
    public @NotNull List<AbilitySnapshot> getAbilitySnapshotsForStat(
        final @NotNull Player player,
        final @NotNull CoreStatType coreStatType
    ) {
        final PlayerBuildService buildService = this.playerBuildService;
        return buildService == null ? List.of() : buildService.getAbilitySnapshotsForStat(player, coreStatType);
    }

    /**
     * Previews the build adjustments caused by a skill prestige.
     *
     * @param player player previewing the prestige
     * @param skillType skill being prestiged
     * @return preview of the resulting build adjustments, or {@code null} when unavailable
     */
    public @Nullable PrestigeAdjustmentPreview previewPrestigeAdjustment(
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        final PlayerBuildService buildService = this.playerBuildService;
        return buildService == null ? null : buildService.previewPrestigeAdjustment(player, skillType);
    }

    /**
     * Returns the currently enabled skills in enum order.
     *
     * @return enabled skill types
     */
    public @NotNull List<SkillType> getEnabledSkills() {
        return this.skillRuntimes.entrySet().stream()
            .filter(entry -> entry.getValue().config().isEnabled())
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Reports whether the supplied skill is enabled.
     *
     * @param skillType skill to probe
     * @return {@code true} when the skill is enabled
     */
    public boolean isSkillEnabled(final @NotNull SkillType skillType) {
        final SkillConfig config = this.getSkillConfig(skillType);
        return config != null && config.isEnabled();
    }

    /**
     * Returns the total enabled skill power for the supplied player.
     *
     * @param player player whose total power should be resolved
     * @return total power contribution across enabled skills
     */
    public long getTotalPower(final @NotNull Player player) {
        Objects.requireNonNull(player, "player");
        long totalPower = 0L;
        for (final SkillType skillType : this.getEnabledSkills()) {
            totalPower += this.getSkillSnapshot(skillType, player).totalPower();
        }
        return totalPower;
    }

    /**
     * Returns the owning enabled block-break skill for the supplied material.
     *
     * @param material material to resolve
     * @return owning skill type, or {@code null} when the material is untracked
     */
    public @Nullable SkillType getBlockBreakSkillOwner(final @NotNull Material material) {
        return this.blockBreakMaterialOwners.get(Objects.requireNonNull(material, "material"));
    }

    /**
     * Reports whether the supplied skill currently uses natural-block suppression.
     *
     * @param skillType skill to probe
     * @return {@code true} when player-placed blocks should be suppressed for the skill
     */
    public boolean usesNaturalBlockSuppression(final @NotNull SkillType skillType) {
        return this.naturalBlockSuppressedSkills.contains(Objects.requireNonNull(skillType, "skillType"));
    }

    /**
     * Returns the agility chunk visit service.
     *
     * @return agility chunk visit service, or {@code null} before enable completes
     */
    public @Nullable AgilityChunkVisitService getAgilityChunkVisitService() {
        return this.agilityChunkVisitService;
    }

    /**
     * Ensures the player root profile and per-skill state rows exist.
     *
     * @param player player whose profile should be provisioned
     */
    public void ensurePlayerProfile(final @NotNull Player player) {
        Objects.requireNonNull(player, "player");
        if (this.executor == null || this.playerRepository == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            final RDAPlayer playerProfile = this.getOrCreatePlayerProfile(player.getUniqueId());
            for (final SkillRuntime runtime : this.skillRuntimes.values()) {
                runtime.progressionService().ensureState(playerProfile);
            }
            if (this.playerBuildService != null) {
                this.playerBuildService.ensureStoredState(player.getUniqueId());
            }
        }, this.executor).exceptionally(throwable -> {
            this.getLogger().warning(
                "Failed to provision RDA skill rows for " + player.getUniqueId() + ": " + throwable.getMessage()
            );
            return null;
        });

        if (this.playerBuildService != null) {
            this.playerBuildService.refreshPassiveAttributes(player);
        }
    }

    private void initializeRepositories() {
        if (this.executor == null || this.entityManagerFactory == null) {
            throw new IllegalStateException("RDA repositories cannot be initialized before enable completes.");
        }

        this.playerRepository = new RRDAPlayer(
            this.executor,
            this.entityManagerFactory,
            RDAPlayer.class,
            RDAPlayer::getIdentifier
        );
        this.skillStateRepository = new RRDASkillState(
            this.executor,
            this.entityManagerFactory,
            RDASkillState.class,
            RDASkillState::getCacheKey
        );
        this.playerBuildRepository = new RRDAPlayerBuild(
            this.executor,
            this.entityManagerFactory,
            RDAPlayerBuild.class,
            RDAPlayerBuild::getCacheKey
        );
        this.statAllocationRepository = new RRDAStatAllocation(
            this.executor,
            this.entityManagerFactory,
            RDAStatAllocation.class,
            RDAStatAllocation::getCacheKey
        );
        this.skillPreferenceRepository = new RRDASkillPreference(
            this.executor,
            this.entityManagerFactory,
            RDASkillPreference.class,
            RDASkillPreference::getCacheKey
        );
        this.agilityVisitedChunkRepository = new RRDAAgilityVisitedChunk(
            this.executor,
            this.entityManagerFactory,
            RDAAgilityVisitedChunk.class,
            RDAAgilityVisitedChunk::getChunkKey
        );
        this.placedSkillBlockRepository = new RRDAPlacedSkillBlock(
            this.executor,
            this.entityManagerFactory,
            RDAPlacedSkillBlock.class,
            RDAPlacedSkillBlock::getLocationKey
        );
        this.partyRepository = new RRDAParty(
            this.executor,
            this.entityManagerFactory,
            RDAParty.class,
            RDAParty::getPartyUuid
        );
        this.partyMemberRepository = new RRDAPartyMember(
            this.executor,
            this.entityManagerFactory,
            RDAPartyMember.class,
            RDAPartyMember::getCacheKey
        );
        this.partyInviteRepository = new RRDAPartyInvite(
            this.executor,
            this.entityManagerFactory,
            RDAPartyInvite.class,
            RDAPartyInvite::getInviteUuid
        );
    }

    private void initializeSkillSystems() {
        if (this.playerRepository == null
            || this.skillStateRepository == null
            || this.playerBuildRepository == null
            || this.statAllocationRepository == null
            || this.skillPreferenceRepository == null
            || this.agilityVisitedChunkRepository == null
            || this.placedSkillBlockRepository == null
            || this.partyRepository == null
            || this.partyMemberRepository == null
            || this.partyInviteRepository == null) {
            throw new IllegalStateException("RDA repositories must be initialized before skills.");
        }

        this.plugin.saveResource("stats.yml", false);
        this.plugin.saveResource("party.yml", false);
        this.statsConfig = new StatsConfigLoader(this.plugin).load();
        this.partyConfig = new PartyConfigLoader(this.plugin).load();
        final EnumMap<SkillType, SkillConfig> configurations = new EnumMap<>(SkillType.class);
        for (final SkillType skillType : SkillType.values()) {
            this.migrateLegacySkillConfig(skillType);
            this.plugin.saveResource(skillType.getResourcePath(), false);
            configurations.put(skillType, new SkillConfigLoader(this.plugin, skillType).load());
        }

        SkillConfigLoader.validateTrackedMaterialUniqueness(configurations);
        this.skillRuntimes.clear();
        this.blockBreakMaterialOwners.clear();

        this.playerBuildService = new PlayerBuildService(
            this.plugin,
            this,
            this.playerRepository,
            this.skillStateRepository,
            this.playerBuildRepository,
            this.statAllocationRepository,
            this.skillPreferenceRepository,
            Objects.requireNonNull(this.statsConfig, "statsConfig")
        );
        this.playerBuildService.initialize();

        this.agilityChunkVisitService = new AgilityChunkVisitService(
            this.playerRepository,
            this.agilityVisitedChunkRepository,
            this.getLogger()
        );
        this.agilityChunkVisitService.initialize();
        this.partyService = new PartyService(
            this.plugin,
            this.getLogger(),
            Objects.requireNonNull(this.partyConfig, "partyConfig"),
            this.playerRepository,
            this.partyRepository,
            this.partyMemberRepository,
            this.partyInviteRepository
        );
        this.partyService.initialize();

        for (final SkillType skillType : SkillType.values()) {
            final SkillConfig configuration = configurations.get(skillType);
            if (configuration == null) {
                continue;
            }

            final PlacedTrackedBlockService<RDAPlacedSkillBlock> placedBlockService =
                this.createPlacedBlockService(skillType, configuration);
            if (placedBlockService != null) {
                placedBlockService.initialize();
            }

            this.skillRuntimes.put(
                skillType,
                new SkillRuntime(
                    configuration,
                    new SkillProgressionService(
                        this,
                        this.playerRepository,
                        this.skillStateRepository,
                        skillType,
                        configuration
                    ),
                    placedBlockService
                )
            );

            if (configuration.isEnabled()) {
                for (final Material material : configuration.getTrackedMaterials()) {
                    this.blockBreakMaterialOwners.put(material, skillType);
                }
            }
        }
    }

    private void migrateLegacySkillConfig(final @NotNull SkillType skillType) {
        final File currentConfigFile = new File(this.plugin.getDataFolder(), skillType.getResourcePath());
        final File legacyConfigFile = new File(this.plugin.getDataFolder(), skillType.getLegacyResourcePath());
        if (currentConfigFile.exists()) {
            if (legacyConfigFile.isFile()) {
                this.getLogger().warning(
                    "Found both current and legacy skill config files for "
                        + skillType.getId()
                        + ". Keeping "
                        + currentConfigFile.getPath()
                        + " and leaving "
                        + legacyConfigFile.getPath()
                        + " untouched."
                );
            }
            return;
        }

        if (!legacyConfigFile.isFile()) {
            return;
        }

        final File parentDirectory = currentConfigFile.getParentFile();
        if (parentDirectory != null
            && !parentDirectory.exists()
            && !parentDirectory.mkdirs()
            && !parentDirectory.isDirectory()) {
            throw new IllegalStateException(
                "Unable to create the RDA skill config directory at " + parentDirectory.getPath()
            );
        }

        try {
            Files.move(legacyConfigFile.toPath(), currentConfigFile.toPath());
            this.getLogger().info(
                "Migrated legacy skill config for "
                    + skillType.getId()
                    + " from "
                    + legacyConfigFile.getPath()
                    + " to "
                    + currentConfigFile.getPath()
            );
            this.deleteDirectoryIfEmpty(legacyConfigFile.getParentFile());
        } catch (final IOException exception) {
            throw new IllegalStateException(
                "Unable to migrate legacy RDA skill config from "
                    + legacyConfigFile.getPath()
                    + " to "
                    + currentConfigFile.getPath(),
                exception
            );
        }
    }

    private void deleteDirectoryIfEmpty(final @Nullable File directory) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }

        final String[] contents = directory.list();
        if (contents != null && contents.length == 0 && !directory.delete()) {
            this.getLogger().fine("Unable to delete empty legacy skill config directory " + directory.getPath());
        }
    }

    private @Nullable PlacedTrackedBlockService<RDAPlacedSkillBlock> createPlacedBlockService(
        final @NotNull SkillType skillType,
        final @NotNull SkillConfig configuration
    ) {
        if (!this.usesNaturalBlockSuppression(skillType)) {
            return null;
        }

        return new PlacedTrackedBlockService<>(
            Objects.requireNonNull(this.placedSkillBlockRepository, "placedSkillBlockRepository"),
            configuration.getTrackedMaterials(),
            this.getLogger(),
            block -> new RDAPlacedSkillBlock(skillType, block),
            RDAPlacedSkillBlock::getLocationKey
        );
    }

    private void initializeViews() {
        final ViewFrame frame = ViewFrame
            .create(this.plugin)
            .with(
                new ConfirmationView(),
                new RaMainView(),
                new RaSkillsView(),
                new RaSkillView(),
                new RaStatView(),
                new RaStatSettingsView(),
                new RaTriggerSettingsView()
            )
            .defaultConfig(config -> {
                config.cancelOnClick();
                config.cancelOnDrag();
                config.cancelOnDrop();
                config.cancelOnPickup();
                config.interactionDelay(Duration.ofMillis(100));
            })
            .disableMetrics();
        this.viewFrame = frame.register();
    }

    private void initializeBossBarIntegration() {
        if (!this.plugin.getServer().getPluginManager().isPluginEnabled("RCore")) {
            this.manaBossBarIntegration = null;
            return;
        }

        if (this.playerBuildRepository == null || this.playerBuildService == null || this.statsConfig == null) {
            throw new IllegalStateException("Mana boss-bar integration requires initialized RDA build services.");
        }

        try {
            final ManaBossBarIntegration integration = new ManaBossBarIntegration(
                this,
                this.playerBuildRepository,
                this.playerBuildService,
                this.statsConfig
            );
            if (integration.register()) {
                this.manaBossBarIntegration = integration;
                this.getLogger().info("Registered RDA mana HUD with RCore");
                return;
            }
        } catch (final Throwable throwable) {
            this.getLogger().log(Level.WARNING, "Failed to initialize the optional RCore boss-bar bridge", throwable);
        }

        this.manaBossBarIntegration = null;
    }

    private void initializeComponents() {
        new CommandFactory(this.plugin, this).registerAllCommandsAndListeners();
    }

    private @NotNull EntityManagerFactory requireEntityManagerFactory() {
        if (this.platform == null) {
            throw new IllegalStateException("RPlatform was not initialized.");
        }

        final EntityManagerFactory factory = this.platform.getEntityManagerFactory();
        if (factory == null) {
            throw new IllegalStateException("RPlatform did not initialize the entity manager factory.");
        }
        return factory;
    }

    private @NotNull CompletableFuture<Void> runSync(final @NotNull Runnable task) {
        if (this.platform == null) {
            throw new IllegalStateException("RPlatform was not initialized.");
        }

        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.platform.getScheduler().runSync(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (final Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private @NotNull RDAPlayer getOrCreatePlayerProfile(final @NotNull UUID playerUuid) {
        final RRDAPlayer repository = Objects.requireNonNull(this.playerRepository, "playerRepository");
        return repository.findOrCreateByPlayer(playerUuid);
    }

    private record SkillRuntime(
        @NotNull SkillConfig config,
        @NotNull SkillProgressionService progressionService,
        @Nullable PlacedTrackedBlockService<?> placedBlockService
    ) {
    }

    /**
     * Opens the centralized RCore boss-bar settings view for the RDA mana provider when
     * available.
     *
     * @param player player opening the settings view
     * @return {@code true} when the RCore-managed settings view was opened
     */
    public boolean openManaBossBarSettings(final @NotNull Player player) {
        if (this.manaBossBarIntegration == null) {
            return false;
        }

        this.manaBossBarIntegration.openSettings(player);
        return true;
    }
}
