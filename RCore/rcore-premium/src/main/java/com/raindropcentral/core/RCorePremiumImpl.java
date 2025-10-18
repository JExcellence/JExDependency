package com.raindropcentral.core;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.core.api.RCoreBackend;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.server.RServer;
import com.raindropcentral.core.database.repository.RPlayerRepository;
import com.raindropcentral.core.database.repository.RPlayerStatisticRepository;
import com.raindropcentral.core.database.repository.RServerRepository;
import com.raindropcentral.core.database.repository.RStatisticRepository;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Free variant runtime that wires repositories, platform, and exposes the shared RCoreService via Bukkit ServicesManager.
 * This class implements RCoreBackend so that the common RCoreAdapter can depend on the abstraction instead of variant classes.
 */
public class RCorePremiumImpl extends AbstractPluginDelegate<RCorePremium> implements RCoreBackend {

    private static final Logger LOGGER = CentralLogger.getLogger(RCorePremiumImpl.class);

    private final ExecutorService executor;
    private final Map<String, Boolean> enabledSupportedPlugins;

    private RServer rServer;
    private RPlayerStatisticRepository rPlayerStatisticRepository;
    private RStatisticRepository rStatisticRepository;
    private RPlayerRepository rPlayerRepository;
    private RServerRepository rServerRepository;
    private RCoreService rCoreService;
    private RPlatform platform;

    public RCorePremiumImpl(final @NotNull RCorePremium rCore) {
        super(rCore);
        this.executor = createExecutorService();
        this.enabledSupportedPlugins = new HashMap<>();
    }

    @Override
    public void onLoad() {
        CentralLogger.initialize(this.getPlugin());
        this.platform = new RPlatform(this.getPlugin());
        LOGGER.info("RCore (Free) loaded successfully");
    }

    @Override
    public void onEnable() {
        this.platform.initialize();

        initializeComponents();
        initializeRepositories();
        initializePlugins();
        initializeServer();

        this.rCoreService = new RCoreAdapter(this);
        registerService();

        this.platform.initializeMetrics(25809);
        LOGGER.info("RCore (Free) enabled successfully");
    }

    @Override
    public void onDisable() {
        unregisterService();
        shutdownExecutor();
        LOGGER.info("RCore (Free) disabled successfully");
    }

    // =====================================================================================
    // RCoreBackend implementation (consumed by the adapter)
    // =====================================================================================

    @Override
    public @NotNull ExecutorService getExecutor() {
        return this.executor;
    }

    @Override
    public @NotNull CompletableFuture<java.util.Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.findByUuidAsync(uniqueId);
    }

    @Override
    public @NotNull CompletableFuture<java.util.Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.findByNameAsync(playerName);
    }

    @Override
    public @NotNull CompletableFuture<RPlayer> createAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.createAsync(player);
    }

    @Override
    public @NotNull CompletableFuture<RPlayer> updateAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.updateAsync(player);
    }

    // =====================================================================================
    // Internal getters / wiring
    // =====================================================================================

    public @NotNull RPlayerStatisticRepository getRPlayerStatisticRepository() {
        ensureReposReady();
        return this.rPlayerStatisticRepository;
    }

    public @NotNull RStatisticRepository getRStatisticRepository() {
        ensureReposReady();
        return this.rStatisticRepository;
    }

    public @NotNull RServerRepository getRServerRepository() {
        ensureReposReady();
        return this.rServerRepository;
    }

    public @NotNull RPlayerRepository getRPlayerRepository() {
        ensureReposReady();
        return this.rPlayerRepository;
    }

    public @NotNull RPlatform getPlatform() {
        return this.platform;
    }

    public @NotNull Map<String, Boolean> getEnabledSupportedPlugins() {
        return Map.copyOf(this.enabledSupportedPlugins);
    }

    private ExecutorService createExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    private void registerService() {
        if (this.rCoreService == null) {
            throw new IllegalStateException("rCoreService not initialized");
        }
        Bukkit.getServer().getServicesManager().register(
                RCoreService.class, // register by the API interface
                this.rCoreService,
                this.getPlugin(),
                ServicePriority.Normal // Premium should use Highest
        );
        LOGGER.info("Registered RCoreService provider (Free) with priority NORMAL");
    }

    private void unregisterService() {
        if (this.rCoreService != null) {
            Bukkit.getServer().getServicesManager().unregister(RCoreService.class, this.rCoreService);
            LOGGER.info("Unregistered RCoreService provider (Free)");
        } else {
            // Ensure no stale registrations remain
            Bukkit.getServer().getServicesManager().unregisterAll(this.getPlugin());
        }
    }

    private void shutdownExecutor() {
        if (!this.executor.isShutdown()) {
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                    LOGGER.warning("Executor did not terminate gracefully, forced shutdown");
                }
            } catch (InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
                LOGGER.severe("Executor shutdown interrupted");
            }
        }
    }

    private void initializeComponents() {
        final CommandFactory commandFactory = new CommandFactory(this.getPlugin());
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeRepositories() {
        final EntityManagerFactory emf = this.platform.getEntityManagerFactory();

        if (emf == null) {
            CentralLogger.getLogger(RCorePremiumImpl.class).warning("EntityManagerFactory not initialized");
            return;
        }

        this.rPlayerRepository = new RPlayerRepository(this.executor, emf);
        this.rPlayerStatisticRepository = new RPlayerStatisticRepository(this.executor, emf);
        this.rStatisticRepository = new RStatisticRepository(this.executor, emf);
        this.rServerRepository = new RServerRepository(this.executor, emf);
    }

    private void initializePlugins() {
        final List<String> supportedPlugins = List.of(
                "Aura", "ChestSort", "CMI", "DiscordSRV", "EcoJobs",
                "EssentialsChat", "EssentialsDiscord", "EssentialsSpawn",
                "Jobs", "mcMMO", "MysticMobs", "ProtocolLib", "RDR",
                "Towny", "TownyChat"
        );

        supportedPlugins.forEach(plugin ->
                this.enabledSupportedPlugins.put(plugin, isPluginEnabled(plugin))
        );

        final long enabledCount = this.enabledSupportedPlugins.values().stream()
                .filter(Boolean::booleanValue)
                .count();
        LOGGER.info("Detected %d/%d supported plugins".formatted(enabledCount, supportedPlugins.size()));
    }

    private void initializeServer() {
        this.rServer = new RServer(UUID.randomUUID(), Bukkit.getServer().getName());

        this.rServerRepository.createAsync(rServer)
                .thenAccept(server -> {
                    this.rServer = server;
                    LOGGER.info("Server registered: %s (%s)"
                            .formatted(server.getServerName(), server.getUniqueId()));
                })
                .exceptionally(throwable -> {
                    LOGGER.severe("Failed to register server: %s"
                            .formatted(throwable.getMessage()));
                    return null;
                });
    }

    private boolean isPluginEnabled(final @NotNull String pluginName) {
        return Bukkit.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    private void ensureReposReady() {
        if (this.rPlayerRepository == null) {
            throw new IllegalStateException("Repositories are not initialized yet");
        }
    }

    private static final String STARTUP_MESSAGE = """
        ===============================================================================================
                    ____     ____     ___    ____    _____
                   |  _ \\   / ___|   / _ \\  |  _ \\  | ____|
                   | |_) | | |      | | | | | |_) | |  _|
                   |  _ <  | |___   | |_| | |  _ <  | |___
                   |_| \\_\\  \\____|   \\___/  |_| \\_\\ |_____|
        
                   Product of Antimatter Zone LLC
                   Powered by JExcellence
        ===============================================================================================
        Language System Initialized [Polyglot API]
        Product by: Antimatter Zone LLC
        Technology Partner: JExcellence
        Website: www.raindropcentral.com
        ===============================================================================================
        Modern i18n API: Polyglot v1.0
        Adventure Components: Enabled
        ===============================================================================================
        """;
}