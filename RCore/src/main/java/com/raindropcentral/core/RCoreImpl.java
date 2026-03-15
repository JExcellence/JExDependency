package com.raindropcentral.core;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.entity.inventory.RPlayerInventory;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.database.repository.*;
import com.raindropcentral.core.service.RCoreService;
import com.raindropcentral.core.service.central.RCentralService;
import com.raindropcentral.core.service.statistics.StatisticsDeliveryService;
import com.raindropcentral.core.service.statistics.StatisticsDeliveryServiceFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.metrics.BStatsMetrics;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.hibernate.repository.RepositoryManager;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for RCore.
 *
 * <p>The delegate bridges Bukkit lifecycle callbacks with the shared {@code rcore-common} backend.
 * It wires platform services, boots repositories, manages asynchronous initialization, and
 * registers the {@link RCoreService} with Bukkit's {@code ServicesManager}.</p>
 *
 * <p>Lifecycle overview:</p>
 * <ul>
 *     <li>{@link #onLoad()} instantiates logging, the shared {@link RPlatform}, and publishes the
 *     adapter-driven service provider.</li>
 *     <li>{@link #onEnable()} orchestrates asynchronous startup by chaining futures across
 *     platform initialization, repository wiring, optional integrations, and server registration,
 *     handling failure by disabling the plugin on the main thread.</li>
 *     <li>{@link #onDisable()} unregisters services and gracefully shuts down the executor
 *     to ensure repository operations complete before the JVM stops.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public class RCoreImpl extends AbstractPluginDelegate<RCore> {

    private static final int METRICS_SERVICE_ID = 25809;

    /**
     * Logger emitting lifecycle and repository wiring information through the shared.
     * {@link CentralLogger} infrastructure.
     */
    private static Logger LOGGER;

    /**
     * Executor backing asynchronous repository access and other background tasks. Created.
     * during construction and shut down in {@link #onDisable()} to avoid leaking virtual threads.
     */
    private final ExecutorService executor;

    /**
     * Snapshot of optional plugin integrations detected during {@link #initializePlugins()}. The.
     * map is mutated only on the main thread while bootstrapping and exposed as an immutable copy
     * for diagnostics.
     */
    private final Map<String, Boolean> enabledSupportedPlugins;

    /**
     * Guard future tracking the asynchronous enable pipeline so duplicate invocations can be.
     * rejected and so failure paths can cancel pending stages.
     */
    private volatile CompletableFuture<Void> enableFuture;

    /**
     * Adapter-backed handle published to Bukkit's service registry to expose RCore APIs.
     */
    private RCoreService rCoreService;

    /**
     * Shared platform bundle that configures dependency injection, metrics, and persistence.
     */
    private RPlatform platform;

    /**
     * RaindropCentral connection service for managing platform integration.
     */
    private RCentralService rCentralService;

    /**
     * Statistics delivery service for transmitting player statistics to RaindropCentral.
     */
    private StatisticsDeliveryService statisticsDeliveryService;
    private BStatsMetrics metrics;

    private RCoreAdapter rCoreAdapter;

    @InjectRepository
    private RPlayerRepository playerRepository;

    /**
     * Creates the delegate and provisions the executor used across asynchronous operations.
     *
     * @param rCore the owning plugin instance that supplies Bukkit context
     */
    public RCoreImpl(final @NotNull RCore rCore) {
        super(rCore);
        this.executor = createExecutorService();
        this.enabledSupportedPlugins = new HashMap<>();
    }

    /**
     * Initializes logging, constructs the shared platform, and registers the core service.
     *
     * <p>The adapter is created eagerly so other plugins querying {@link RCoreService} during enable
     * can obtain the handle immediately. Service registration occurs synchronously to ensure Bukkit
     * consumers see the provider before their own enable hooks continue.</p>
     */
    @Override
    public void onLoad() {
        // Initialize logger for RCore
        LOGGER = CentralLogger.getLoggerByName(this.getPlugin().getName());
        this.platform = new RPlatform(this.getPlugin());

        LOGGER.info("RCore loaded successfully");
    }

    /**
     * Orchestrates asynchronous startup across platform, repository, and integration phases.
     *
     * <p>The method first ensures only a single enable chain runs at a time by tracking the returned
     * {@link CompletableFuture}. It then executes platform initialization and repository wiring on
     * the main thread using {@link #runSync(Runnable)} to preserve Bukkit thread-safety guarantees.
     * Server registration occurs asynchronously; any failure disables the plugin on the main thread
     * to avoid running without persistence guarantees.</p>
     */
    @Override
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            LOGGER.warning("Enable sequence already in progress");
            return;
        }

        this.enableFuture = this.platform.initialize()
                .thenCompose(v -> runSync(() -> {
                    rCoreAdapter = new RCoreAdapter(this);
                    registerServices();

                    initializeRepositories();
                    initializeComponents();
                    initializePlugins();
                }))
                .thenCompose(v -> runSync(() -> {
                    this.initializeMetrics();

                    LOGGER.info(STARTUP_MESSAGE);
                    LOGGER.info("RCore enabled successfully");
                }))
                .exceptionally(ex -> {
                    runSync(() -> {
                        LOGGER.log(Level.SEVERE, "Failed to enable RCore", ex);
                        this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
                    });
                    return null;
                });
    }

    /**
     * Unregisters the service provider and shuts down asynchronous infrastructure.
     *
     * <p>The executor is given a grace period to complete in-flight repository calls before being
     * interrupted. This ensures outstanding persistence tasks flush correctly during server
     * shutdown.</p>
     */
    @Override
    public void onDisable() {
        // Shutdown statistics delivery service first to flush pending statistics
        StatisticsDeliveryServiceFactory.shutdown(this.statisticsDeliveryService);

        // Notify RCentral of shutdown
        if (this.rCentralService != null) {
            this.rCentralService.notifyShutdown().join();
        }

        shutdownExecutor();
        LOGGER.info("RCore disabled successfully");
    }

    /**
     * Returns the shared platform facade which exposes dependency injection, metrics, and.
     * persistence utilities.
     *
     * @return the initialized platform instance
     */
    public @NotNull RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Provides a read-only view of optional plugin integrations detected during startup.
     *
     * @return immutable copy of plugin detection results
     */
    public @NotNull Map<String, Boolean> getEnabledSupportedPlugins() {
        return Map.copyOf(this.enabledSupportedPlugins);
    }

    /**
     * Creates the executor used for asynchronous repository and initialization tasks.
     *
     * @return a per-task virtual-thread executor tuned for blocking database interactions
     */
    private ExecutorService createExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Attempts a graceful executor shutdown from {@link #onDisable()} before interrupting outstanding.
     * tasks.
     *
     * <p>The method waits up to ten seconds for tasks to finish and logs whenever a forced shutdown
     * or interruption occurs so operators can diagnose long-running jobs.</p>
     */
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

    private void registerServices() {
        if (rCoreAdapter != null) {
            Bukkit.getServer().getServicesManager().register(
                    RCoreAdapter.class,
                    rCoreAdapter,
                    getPlugin(),
                    ServicePriority.Normal
            );
            LOGGER.info("Registered CurrencyAdapter service");
        }
    }

    /**
     * Initializes RCentralService and registers commands/listeners through the shared command factory.
 *
 * <p>Creates the RCentralService first so it can be injected into commands that need it.
     * Passes this implementation instance as context so commands can access services through
     * dependency injection.
     * </p>
     */
    private void initializeComponents() {
        this.rCentralService = new RCentralService(this.getPlugin(), this.platform);

        // Initialize statistics delivery service
        this.statisticsDeliveryService = StatisticsDeliveryServiceFactory.create(
            this.getPlugin(), this.rCentralService
        );
        StatisticsDeliveryServiceFactory.initialize(this.statisticsDeliveryService);

        var commandFactory = new CommandFactory(this.getPlugin(), this);
        commandFactory.registerAllCommandsAndListeners();
    }
    
    /**
     * Provides access to the RCentralService for commands and other components.
     *
     * @return the RCentralService instance
     */
    public @NotNull RCentralService getRCentralService() {
        return this.rCentralService;
    }

    /**
     * Lazily wires repositories once the platform exposes an {@link EntityManagerFactory}.
     *
     * <p>Uses RepositoryManager to register and inject repositories.</p>
     */
    private void initializeRepositories() {
        final var emf = this.platform.getEntityManagerFactory();

        if (emf == null) {
            LOGGER.warning("EntityManagerFactory not initialized");
            return;
        }

        RepositoryManager.initialize(this.executor, emf);
        var repositoryManager = RepositoryManager.getInstance();

        // Register all repositories
        repositoryManager.register(RPlayerRepository.class, RPlayer.class, RPlayer::getUniqueId);
        repositoryManager.register(RPlayerStatisticRepository.class, RPlayerStatistic.class, RPlayerStatistic::getId);
        repositoryManager.register(RStatisticRepository.class, RAbstractStatistic.class, RAbstractStatistic::getId);
        repositoryManager.register(RPlayerInventoryRepository.class, RPlayerInventory.class, RPlayerInventory::getId);
        repositoryManager.register(RCentralServerRepository.class, RCentralServer.class, RCentralServer::getId);

        repositoryManager.injectInto(this);
    }
    


    /**
     * Records the presence of optional companion plugins for diagnostics.
     *
     * <p>Detection runs on the main thread to align with Bukkit API expectations and feeds the
     * {@link #enabledSupportedPlugins} map used for status reporting.</p>
     */
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

    private void initializeMetrics() {
        if (this.metrics != null) {
            return;
        }

        this.metrics = new BStatsMetrics(
                this.getPlugin(),
                METRICS_SERVICE_ID,
                this.platform.getPlatformType() == PlatformType.FOLIA
        );

        final String pluginName = this.getPlugin().getPluginMeta().getName().toLowerCase(Locale.ROOT);
        final boolean premiumEdition = this.platform.isPremiumVersion() || !pluginName.contains("free");
        this.metrics.addCustomChart(new BStatsMetrics.SingleLineChart("free", () -> premiumEdition ? 0 : 1));
        this.metrics.addCustomChart(new BStatsMetrics.SingleLineChart("premium", () -> premiumEdition ? 1 : 0));
    }

    /**
     * Checks whether a plugin is currently enabled through Bukkit's plugin manager.
     *
     * @param pluginName plugin identifier to inspect
     * @return {@code true} when enabled, {@code false} otherwise
     */
    private boolean isPluginEnabled(final @NotNull String pluginName) {
        return Bukkit.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    /**
     * Schedules the provided runnable on the Bukkit main thread and exposes its completion as a.
     * future.
     *
     * <p>Errors thrown by the runnable complete the returned future exceptionally so the enable
     * pipeline can abort and trigger shutdown logic. The helper centralizes thread marshaling for
     * stages that must interact with Bukkit APIs.</p>
     *
     * @param runnable work to execute on the main server thread
     * @return future completing when the runnable finishes
     */
    private CompletableFuture<Void> runSync(final Runnable runnable) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.platform.getScheduler().runSync(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * ASCII-art banner displayed in the console once enablement succeeds.
     */
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
        Language System Initialized [JExTranslate API]
        Product by: Antimatter Zone LLC
        Technology Partner: JExcellence
        Website: www.raindropcentral.com
        ===============================================================================================
        Modern i18n API: JExTranslate v3.0
        Adventure Components: Enabled
        ===============================================================================================
        """;

    /**
     * Gets executor.
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    public CompletableFuture<Void> getEnableFuture() {
        return enableFuture;
    }

    /**
     * Performs getrCoreService.
     */
    public RCoreService getrCoreService() {
        return rCoreService;
    }

    /**
     * Gets rCentralService.
     */
    public RCentralService getrCentralService() {
        return rCentralService;
    }

    /**
     * Gets rCoreAdapter.
     */
    public RCoreAdapter getrCoreAdapter() {
        return rCoreAdapter;
    }

    /**
     * Gets playerRepository.
     */
    public RPlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    /**
     * Provides access to the StatisticsDeliveryService for components that need it.
     *
     * @return the StatisticsDeliveryService instance, or null if disabled
     */
    public StatisticsDeliveryService getStatisticsDeliveryService() {
        return statisticsDeliveryService;
    }
}
