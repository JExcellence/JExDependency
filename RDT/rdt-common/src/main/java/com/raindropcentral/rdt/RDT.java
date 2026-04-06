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

package com.raindropcentral.rdt;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.database.repository.RRTownChunk;
import com.raindropcentral.rdt.database.repository.RRTownInvite;
import com.raindropcentral.rdt.requirement.RDTRequirementProvider;
import com.raindropcentral.rdt.service.NexusAccessService;
import com.raindropcentral.rdt.service.TownBossBarService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.service.TownService;
import com.raindropcentral.rdt.service.TownSpawnService;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rdt.view.town.ClaimsView;
import com.raindropcentral.rdt.view.town.CreateTownColorAnvilView;
import com.raindropcentral.rdt.view.town.CreateTownConfirmView;
import com.raindropcentral.rdt.view.town.CreateTownNameAnvilView;
import com.raindropcentral.rdt.view.town.TownArchetypeView;
import com.raindropcentral.rdt.view.town.TownBankView;
import com.raindropcentral.rdt.view.town.TownChunkTypeView;
import com.raindropcentral.rdt.view.town.TownChunkView;
import com.raindropcentral.rdt.view.town.TownDirectoryView;
import com.raindropcentral.rdt.view.town.TownInvitesView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import com.raindropcentral.rdt.view.town.TownProtectionScopeView;
import com.raindropcentral.rdt.view.town.TownProtectionsView;
import com.raindropcentral.rdt.view.town.TownRenameAnvilView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.proxy.NoOpProxyService;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared runtime bootstrap for RDT editions.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class RDT {

    private static final String CONFIG_FOLDER_PATH = "config";
    private static final String CONFIG_FILE_NAME = "config.yml";

    private final JavaPlugin plugin;
    private final String edition;
    private final TownService townService;

    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private ProxyService proxyService;
    private volatile CompletableFuture<Void> enableFuture;

    private Object economyInstance;
    private ViewFrame viewFrame;

    private RRDTPlayer playerRepository;
    private RRTown townRepository;
    private RRTownChunk townChunkRepository;
    private RRTownInvite townInviteRepository;

    private TownRuntimeService townRuntimeService;
    private TownSpawnService townSpawnService;
    private TownBossBarService townBossBarService;
    private NexusAccessService nexusAccessService;
    private RDTRequirementProvider requirementProvider;

    /**
     * Creates a shared RDT runtime for a specific edition.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used in logs
     * @param townService edition-specific town rules
     */
    public RDT(
        final @NotNull JavaPlugin plugin,
        final @NotNull String edition,
        final @NotNull TownService townService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.townService = Objects.requireNonNull(townService, "townService");
    }

    /**
     * Allocates shared platform state before plugin enable.
     */
    public void onLoad() {
        this.getLogger().info("Loading RPlatform for RDT (" + this.edition + ")");
        this.platform = new RPlatform(this.plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes repositories, services, commands, listeners, and views.
     */
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            this.getLogger().warning("Enable sequence already in progress for RDT (" + this.edition + ")");
            return;
        }

        this.getLogger().info("Enabling RDT (" + this.edition + ") Edition");
        this.enableFuture = this.platform.initialize()
            .thenCompose(ignored -> this.runSync(() -> {
                this.entityManagerFactory = this.requireEntityManagerFactory();
                this.scheduler = this.platform.getScheduler();
                this.platformType = this.platform.getPlatformType();
                this.proxyService = this.platform.getProxyService();
                this.ensureDefaultConfigFile();
                this.initializeRepositories();
                this.initializeServices();
                this.initializeRequirementProvider();
                this.initializePlugins();
                this.initializeViews();
                this.initializeCommands();
                this.startRuntimeServices();
                this.getLogger().info("RDT (" + this.edition + ") Edition enabled successfully");
            }))
            .exceptionally(throwable -> {
                this.runSync(() -> {
                    this.getLogger().log(Level.SEVERE, "Failed to initialize RDT (" + this.edition + ")", throwable);
                    this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                });
                return null;
            });
    }

    /**
     * Shuts down runtime services and releases resources.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDT (" + this.edition + ")");

        if (this.townBossBarService != null) {
            this.townBossBarService.shutdown();
        }
        if (this.requirementProvider != null) {
            this.requirementProvider.unregister();
            this.requirementProvider = null;
        }
        this.proxyService = NoOpProxyService.createDefault();

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.entityManagerFactory != null) {
            try {
                this.entityManagerFactory.close();
            } catch (final Exception ignored) {
            }
        }
    }

    /**
     * Loads the effective plugin configuration.
     *
     * @return parsed plugin configuration
     */
    public @NotNull ConfigSection getDefaultConfig() {
        this.ensureDefaultConfigFile();
        try {
            if (!this.canChangeConfigs()) {
                final InputStream defaultStream = this.plugin.getResource(CONFIG_FOLDER_PATH + '/' + CONFIG_FILE_NAME);
                return defaultStream == null ? ConfigSection.createDefault() : ConfigSection.fromInputStream(defaultStream);
            }
            return ConfigSection.fromFile(this.getDefaultConfigFile());
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load RDT config: " + exception.getMessage());
            return ConfigSection.createDefault();
        }
    }

    /**
     * Returns whether the current edition may change config-driven behavior at runtime.
     *
     * @return {@code true} when config changes are permitted
     */
    public boolean canChangeConfigs() {
        return this.townService.canChangeConfigs();
    }

    /**
     * Returns whether the current edition is premium.
     *
     * @return {@code true} when premium features are available
     */
    public boolean isPremium() {
        return this.townService.isPremium();
    }

    private void initializeRepositories() {
        this.playerRepository = new RRDTPlayer(this.executor, this.entityManagerFactory, RDTPlayer.class, RDTPlayer::getIdentifier);
        this.townRepository = new RRTown(this.executor, this.entityManagerFactory, RTown.class, RTown::getTownUUID);
        this.townChunkRepository = new RRTownChunk(this.executor, this.entityManagerFactory);
        this.townInviteRepository = new RRTownInvite(this.executor, this.entityManagerFactory);
    }

    private void initializeServices() {
        this.nexusAccessService = new NexusAccessService(this);
        this.townRuntimeService = new TownRuntimeService(this);
        this.townSpawnService = new TownSpawnService(this);
        this.townBossBarService = new TownBossBarService(this);
    }

    private void initializeRequirementProvider() {
        this.requirementProvider = new RDTRequirementProvider(this);
        this.requirementProvider.register();
    }

    private void initializePlugins() {
        new com.raindropcentral.rplatform.service.ServiceRegistry().register(
            "net.milkbowl.vault.economy.Economy",
            "TheNewEconomy"
        ).optional().maxAttempts(30).retryDelay(1000).onSuccess(economy -> this.economyInstance = economy).load();
    }

    private void initializeCommands() {
        final CommandFactory commandFactory = new CommandFactory(this.plugin, this);
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeViews() {
        this.viewFrame = ViewFrame.create(this.plugin)
            .install(AnvilInputFeature.AnvilInput)
            .with(
                new TownHubView(),
                new CreateTownNameAnvilView(),
                new CreateTownColorAnvilView(),
                new CreateTownConfirmView(),
                new TownDirectoryView(),
                new TownInvitesView(),
                new TownOverviewView(),
                new TownArchetypeView(),
                new TownBankView(),
                new TownRenameAnvilView(),
                new ClaimsView(),
                new TownChunkView(),
                new TownChunkTypeView(),
                new TownProtectionScopeView(),
                new TownProtectionsView()
            )
            .disableMetrics()
            .register();
    }

    private void startRuntimeServices() {
        if (this.townBossBarService != null) {
            this.townBossBarService.start();
        }
    }

    private @NotNull CompletableFuture<Void> runSync(final @NotNull Runnable task) {
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

    private @NotNull File getDefaultConfigFile() {
        return new File(new File(this.plugin.getDataFolder(), CONFIG_FOLDER_PATH), CONFIG_FILE_NAME);
    }

    private void ensureDefaultConfigFile() {
        final File dataFolder = this.plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            this.getLogger().warning("Could not create plugin data folder for config extraction.");
            return;
        }

        final File configFolder = new File(dataFolder, CONFIG_FOLDER_PATH);
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            this.getLogger().warning("Could not create config folder for default config extraction.");
            return;
        }

        final File configFile = new File(configFolder, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            return;
        }

        try {
            this.plugin.saveResource(CONFIG_FOLDER_PATH + "/" + CONFIG_FILE_NAME, false);
        } catch (final IllegalArgumentException exception) {
            this.getLogger().warning("Bundled default config could not be extracted: " + exception.getMessage());
        }
    }

    private @NotNull EntityManagerFactory requireEntityManagerFactory() {
        final EntityManagerFactory factory = this.platform.getEntityManagerFactory();
        if (factory == null) {
            throw new IllegalStateException("RPlatform did not initialize the entity manager factory.");
        }
        return factory;
    }

    /**
     * Returns the active Vault economy instance when available.
     *
     * @return Vault economy, or {@code null} when unavailable
     */
    public @Nullable net.milkbowl.vault.economy.Economy getEco() {
        return this.economyInstance instanceof net.milkbowl.vault.economy.Economy economy ? economy : null;
    }

    /**
     * Returns the owning Bukkit plugin.
     *
     * @return owning Bukkit plugin
     */
    public @NotNull JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Returns the plugin logger.
     *
     * @return plugin logger
     */
    public @NotNull Logger getLogger() {
        return this.plugin.getLogger();
    }

    /**
     * Returns the Bukkit server instance.
     *
     * @return Bukkit server
     */
    public @NotNull Server getServer() {
        return this.plugin.getServer();
    }

    /**
     * Returns the async executor.
     *
     * @return async executor
     */
    public @Nullable ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Returns the shared RPlatform runtime.
     *
     * @return shared platform runtime
     */
    public @Nullable RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Returns the entity manager factory.
     *
     * @return entity manager factory
     */
    public @Nullable EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    /**
     * Returns the scheduler adapter.
     *
     * @return scheduler adapter
     */
    public @Nullable ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    /**
     * Returns the detected platform type.
     *
     * @return detected platform type
     */
    public @Nullable PlatformType getPlatformType() {
        return this.platformType;
    }

    /**
     * Returns the active edition label.
     *
     * @return edition label
     */
    public @NotNull String getEdition() {
        return this.edition;
    }

    /**
     * Returns the active view frame.
     *
     * @return registered view frame
     */
    public @Nullable ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    /**
     * Returns the active proxy bridge.
     *
     * @return proxy bridge
     */
    public @NotNull ProxyService getProxyService() {
        return this.proxyService == null ? NoOpProxyService.createDefault() : this.proxyService;
    }

    /**
     * Returns the player repository used by integrations and listeners.
     *
     * @return player repository
     */
    public @Nullable RRDTPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Returns the town repository used by integrations and listeners.
     *
     * @return town repository
     */
    public @Nullable RRTown getTownRepository() {
        return this.townRepository;
    }

    /**
     * Returns the claimed-chunk repository.
     *
     * @return claimed-chunk repository
     */
    public @Nullable RRTownChunk getTownChunkRepository() {
        return this.townChunkRepository;
    }

    /**
     * Returns the town-invite repository.
     *
     * @return town-invite repository
     */
    public @Nullable RRTownInvite getTownInviteRepository() {
        return this.townInviteRepository;
    }

    /**
     * Returns the central town runtime service.
     *
     * @return town runtime service
     */
    public @Nullable TownRuntimeService getTownRuntimeService() {
        return this.townRuntimeService;
    }

    /**
     * Returns the town-spawn service.
     *
     * @return town-spawn service
     */
    public @Nullable TownSpawnService getTownSpawnService() {
        return this.townSpawnService;
    }

    /**
     * Returns the boss-bar service.
     *
     * @return boss-bar service
     */
    public @Nullable TownBossBarService getTownBossBarService() {
        return this.townBossBarService;
    }

    /**
     * Returns the nexus access validator.
     *
     * @return nexus access validator
     */
    public @Nullable NexusAccessService getNexusAccessService() {
        return this.nexusAccessService;
    }

    /**
     * Returns the edition-specific town service.
     *
     * @return edition-specific town service
     */
    public @NotNull TownService getTownService() {
        return this.townService;
    }
}
