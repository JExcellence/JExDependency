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
import com.raindropcentral.rdt.factory.BossBarFactory;
import com.raindropcentral.rdt.service.TownService;
import com.raindropcentral.rdt.service.TownSpawnService;
import com.raindropcentral.rdt.view.main.MainOverviewView;
import com.raindropcentral.rdt.view.town.ChunkClaimView;
import com.raindropcentral.rdt.view.town.CreateTownAnvilView;
import com.raindropcentral.rdt.view.town.RoleAssignmentPlayerPermissionView;
import com.raindropcentral.rdt.view.town.RoleCreateNameAnvilView;
import com.raindropcentral.rdt.view.town.RoleCreateAnvilView;
import com.raindropcentral.rdt.view.town.RolePermissionView;
import com.raindropcentral.rdt.view.town.RolePlayerPermissionView;
import com.raindropcentral.rdt.view.town.RolesOverviewView;
import com.raindropcentral.rdt.view.town.ServerTownsJoinView;
import com.raindropcentral.rdt.view.town.ServerTownsOverviewView;
import com.raindropcentral.rdt.view.town.TownBankView;
import com.raindropcentral.rdt.view.town.TownChunkTypeView;
import com.raindropcentral.rdt.view.town.TownChunkView;
import com.raindropcentral.rdt.view.town.TownInfoView;
import com.raindropcentral.rdt.view.town.TownInvitePlayerView;
import com.raindropcentral.rdt.view.town.TownLevelUpRequirementsView;
import com.raindropcentral.rdt.view.town.TownLevelUpRewardsView;
import com.raindropcentral.rdt.view.town.TownLevelUpView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import com.raindropcentral.rdt.view.town.TownPendingJoinView;
import com.raindropcentral.rdt.view.town.TownProtectionsView;
import com.raindropcentral.rplatform.proxy.NoOpProxyService;
import com.raindropcentral.rplatform.proxy.ProxyActionResult;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared runtime bootstrap for RDT editions.
 *
 * <p>The runtime initializes persistence, views, commands, and economy integration while
 * delegating edition-specific rules to {@link TownService}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.10
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
    private RRTown townRepository;
    private RRDTPlayer playerRepository;
    private BossBarFactory bossBarFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private ProxyService proxyService;
    private TownSpawnService townSpawnService;
    private volatile CompletableFuture<Void> enableFuture;

    private Object economyInstance;
    private ViewFrame viewFrame;

    /**
     * Creates a shared RDT runtime for a specific edition.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used in logs
     * @param townService edition-specific behavior service
     * @throws NullPointerException if any argument is {@code null}
     */
    public RDT(
            final @NotNull JavaPlugin plugin,
            final @NotNull String edition,
            final @NotNull TownService townService
    ) {
        this.plugin = plugin;
        this.edition = edition;
        this.townService = townService;
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
     * Initializes repositories, views, and runtime services.
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
                    this.platformType = this.platform.getPlatformType();
                    this.scheduler = this.platform.getScheduler();
                    this.initializeProxyService();
                    this.townSpawnService = new TownSpawnService(this);
                    this.ensureDefaultConfigFile();
                    this.initializeRepositories();
                    this.getLogger().info("Persistence initialized via RPlatform");
                    this.getLogger().info("Connecting to economy");
                    this.initializePlugins();
                    this.initializeCommands();
                    this.initializeViews();
                    this.bossBarFactory = new BossBarFactory(this);
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
        this.getLogger().info("Disabling RDT (" + this.edition + "): closing Hibernate");
        if (this.proxyService != null) {
            this.proxyService.unregisterActionHandler("rdt", "town_spawn_arrival");
        }
        this.townSpawnService = null;

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
     * Loads the plugin configuration for the active edition.
     *
     * <p>Free editions always read the bundled JAR config to prevent runtime config overrides.
     * Premium editions read from the extracted data-folder config and fall back to bundled defaults
     * when the file cannot be parsed.</p>
     *
     * @return parsed configuration for the active edition
     */
    public @NotNull ConfigSection getDefaultConfig() {
        if (this.canChangeConfigs()) {
            this.ensureDefaultConfigFile();
            try {
                return ConfigSection.fromFile(this.getDefaultConfigFile());
            } catch (final Exception exception) {
                this.getLogger().warning(
                        "Failed to parse RDT config from " + this.getDefaultConfigFile().getAbsolutePath()
                                + ": " + exception.getMessage()
                );
                return this.loadBundledConfig();
            }
        }

        return this.loadBundledConfig();
    }

    /**
     * Returns whether the active edition allows runtime config changes.
     *
     * @return {@code true} when config edits are allowed
     */
    public boolean canChangeConfigs() {
        return this.townService.canChangeConfigs();
    }

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} when the runtime is running the premium edition
     */
    public boolean isPremium() {
        return this.townService.isPremium();
    }

    private @NotNull ConfigSection loadBundledConfig() {
        try (InputStream inputStream = this.plugin.getResource(CONFIG_FOLDER_PATH + "/" + CONFIG_FILE_NAME)) {
            if (inputStream == null) {
                this.getLogger().warning("Bundled config resource '" + CONFIG_FOLDER_PATH + "/" + CONFIG_FILE_NAME + "' was not found.");
                return ConfigSection.createDefault();
            }
            return ConfigSection.fromInputStream(inputStream);
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load bundled RDT config: " + exception.getMessage());
            return ConfigSection.createDefault();
        }
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

    private void initializePlugins() {
        this.getLogger().info("Registering Vault service");
        new ServiceRegistry().register(
                "net.milkbowl.vault.economy.Economy",
                "TheNewEconomy"
        ).optional().maxAttempts(30).retryDelay(1000).onSuccess(economy -> {
            this.getLogger().info("Vault service initialized");
            this.economyInstance = economy;
        }).onFailure(() -> this.getLogger().warning(
                "Vault service not present; initialization failed")
        ).load();
    }

    private void initializeCommands() {
        final var commandFactory = new CommandFactory(this.plugin, this);
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeRepositories(){
        this.playerRepository = new RRDTPlayer(
                this.executor,
                this.entityManagerFactory,
                RDTPlayer.class,
                RDTPlayer::getIdentifier
        );
        this.townRepository = new RRTown(
                this.executor,
                this.entityManagerFactory,
                RTown.class,
                RTown::getIdentifier,
                this.getServerRouteId()
        );
    }

    private @NotNull EntityManagerFactory requireEntityManagerFactory() {
        final EntityManagerFactory factory = this.platform.getEntityManagerFactory();
        if (factory == null) {
            throw new IllegalStateException("RPlatform did not initialize the entity manager factory.");
        }
        return factory;
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

    private void initializeViews() {
        final ViewFrame frame = ViewFrame
                .create(this.plugin)
                .install(AnvilInputFeature.AnvilInput)
                .with(
                        new MainOverviewView(),
                        new CreateTownAnvilView(),
                        new RoleCreateAnvilView(),
                        new RoleCreateNameAnvilView(),
                        new RolePermissionView(),
                        new RoleAssignmentPlayerPermissionView(),
                        new RolePlayerPermissionView(),
                        new RolesOverviewView(),
                        new ServerTownsJoinView(),
                        new ServerTownsOverviewView(),
                        new ChunkClaimView(),
                        new TownInvitePlayerView(),
                        new TownPendingJoinView(),
                        new TownInfoView(),
                        new TownChunkView(),
                        new TownChunkTypeView(),
                        new TownProtectionsView(),
                        new TownBankView(),
                        new TownLevelUpView(),
                        new TownLevelUpRequirementsView(),
                        new TownLevelUpRewardsView(),
                        new TownOverviewView()
                )
                .disableMetrics();
        this.viewFrame = frame.register();
    }

    
    /**
     * Gets eco. Executes this member.
     */
    public @Nullable net.milkbowl.vault.economy.Economy getEco() {
        if (this.economyInstance == null) return null;
        return (net.milkbowl.vault.economy.Economy) this.economyInstance;
    }

    /**
     * Gets plugin.
     */
    public @NotNull JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Gets logger.
     */
    public @NotNull Logger getLogger() {
        return this.plugin.getLogger();
    }

    /**
     * Gets server.
     */
    public @NotNull Server getServer() {
        return this.plugin.getServer();
    }

    /**
     * Gets executor.
     */
    public @Nullable ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Gets platform.
     */
    public @Nullable RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Gets entityManagerFactory.
     */
    public @Nullable EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    /**
     * Gets townRepository.
     */
    public @Nullable RRTown getTownRepository() {
        return this.townRepository;
    }

    /**
     * Gets playerRepository.
     */
    public @Nullable RRDTPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Gets bossBarFactory.
     */
    public @Nullable BossBarFactory getBossBarFactory() {
        return this.bossBarFactory;
    }

    /**
     * Gets scheduler.
     */
    public @Nullable ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    /**
     * Gets platformType.
     */
    public @Nullable PlatformType getPlatformType() {
        return this.platformType;
    }

    /**
     * Gets economyInstance.
     */
    public @Nullable Object getEconomyInstance() {
        return this.economyInstance;
    }

    /**
     * Gets viewFrame.
     */
    public @Nullable ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    /**
     * Returns the active proxy service bridge.
     *
     * @return proxy service bridge
     */
    public @NotNull ProxyService getProxyService() {
        if (this.proxyService == null) {
            this.proxyService = NoOpProxyService.createDefault();
        }
        return this.proxyService;
    }

    /**
     * Returns the town-spawn flow service.
     *
     * @return town-spawn service, or {@code null} before enable completes
     */
    public @Nullable TownSpawnService getTownSpawnService() {
        return this.townSpawnService;
    }

    /**
     * Returns the configured authoritative route ID for this server.
     *
     * @return server route identifier
     */
    public @NotNull String getServerRouteId() {
        final String configuredRouteId = this.getDefaultConfig().getProxyServerRouteId();
        if (!configuredRouteId.isBlank()) {
            return configuredRouteId;
        }
        final String serverName = this.getServer().getName();
        if (serverName == null || serverName.isBlank()) {
            return "server";
        }
        return serverName.trim().toLowerCase(Locale.ROOT);
    }

    private void initializeProxyService() {
        final ProxyService registeredProxyService =
                this.getServer().getServicesManager().load(ProxyService.class);
        if (registeredProxyService != null) {
            this.proxyService = registeredProxyService;
            this.getLogger().info("ProxyService bridge detected via Bukkit ServicesManager.");
            return;
        }

        if (this.platform != null && this.platform.getProxyService() != null) {
            this.proxyService = this.platform.getProxyService();
        } else {
            this.proxyService = NoOpProxyService.createDefault();
        }

        this.proxyService.registerActionHandler(
            TownSpawnService.MODULE_ID,
            TownSpawnService.TOWN_SPAWN_ARRIVAL_ACTION,
            envelope -> CompletableFuture.completedFuture(
                ProxyActionResult.success("Town-spawn arrival handler registered.")
            )
        );
    }
}
