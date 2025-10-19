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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edition delegate for the free RCore distribution.
 *
 * <p>The delegate bridges Bukkit lifecycle callbacks with the shared {@code rcore-common} backend.
 * It wires platform services, boots repositories, manages asynchronous initialization, and
 * registers the {@link RCoreService} with Bukkit's {@code ServicesManager}. The free and premium
 * implementations follow identical orchestration, but the free variant sticks to baseline
 * integrations and service priorities while still satisfying the {@link RCoreBackend} contract.</p>
 *
 * <p>Lifecycle overview:</p>
 * <ul>
 *     <li>{@link #onLoad()} instantiates logging, the shared {@link RPlatform}, and publishes the
 *     adapter-driven service provider.</li>
 *     <li>{@link #onEnable()} orchestrates asynchronous startup by chaining futures across
 *     platform initialization, repository wiring, optional integrations, and server registration,
 *     handling failure by disabling the plugin on the main thread.</li>
 *     <li>{@link #onDisable()} unregisters services and gracefully shuts down the variant executor
 *     to ensure repository operations complete before the JVM stops.</li>
 * </ul>
 *
 * <p>All public operations guard against null dependencies and rely on the dedicated executor
 * returned by {@link #getExecutor()} to keep persistence calls off the main server thread.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCoreFreeImpl extends AbstractPluginDelegate<RCoreFree> implements RCoreBackend {

    /**
     * Edition-scoped logger emitting lifecycle and repository wiring information through the shared
     * {@link CentralLogger} infrastructure.
     */
    private static final Logger LOGGER = CentralLogger.getLogger(RCoreFreeImpl.class);

    /**
     * Variant executor backing asynchronous repository access and other background tasks. Created
     * during construction and shut down in {@link #onDisable()} to avoid leaking virtual threads.
     */
    private final ExecutorService executor;

    /**
     * Snapshot of optional plugin integrations detected during {@link #initializePlugins()}. The
     * map is mutated only on the main thread while bootstrapping and exposed as an immutable copy
     * for diagnostics.
     */
    private final Map<String, Boolean> enabledSupportedPlugins;

    /**
     * Guard future tracking the asynchronous enable pipeline so duplicate invocations can be
     * rejected and so failure paths can cancel pending stages.
     */
    private volatile CompletableFuture<Void> enableFuture;

    /**
     * Entity representing the running server once registered with the persistence layer.
     */
    private RServer rServer;

    /**
     * Repository responsible for player statistic persistence once the platform initializes JPA.
     */
    private RPlayerStatisticRepository rPlayerStatisticRepository;

    /**
     * Repository exposing stat template operations for asynchronous callers.
     */
    private RStatisticRepository rStatisticRepository;

    /**
     * Repository used by adapter-backed service calls for player CRUD operations.
     */
    private RPlayerRepository rPlayerRepository;

    /**
     * Repository handling server registration and metadata updates for analytics workflows.
     */
    private RServerRepository rServerRepository;

    /**
     * Adapter-backed handle published to Bukkit's service registry to expose RCore APIs.
     */
    private RCoreService rCoreService;

    /**
     * Shared platform bundle that configures dependency injection, metrics, and persistence.
     */
    private RPlatform platform;

    /**
     * Creates the delegate and provisions the executor used across asynchronous operations.
     *
     * @param rCore the owning plugin instance that supplies Bukkit context
     */
    public RCoreFreeImpl(final @NotNull RCoreFree rCore) {
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
        CentralLogger.initialize(this.getPlugin());
        this.platform = new RPlatform(this.getPlugin());

        this.rCoreService = new RCoreAdapter(this);
        registerService();

        LOGGER.info("RCore (Free) loaded successfully");
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
                    initializeComponents();
                    initializeRepositories();
                    initializePlugins();
                }))
                .thenCompose(v -> initializeServerAsync())
                .thenCompose(v -> runSync(() -> {
                    this.platform.initializeMetrics(25809);

                    LOGGER.info(STARTUP_MESSAGE);
                    LOGGER.info("RCore (Free) enabled successfully");
                }))
                .exceptionally(ex -> {
                    runSync(() -> {
                        LOGGER.log(Level.SEVERE, "Failed to enable RCore (Free)", ex);
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
        unregisterService();
        shutdownExecutor();
        LOGGER.info("RCore (Free) disabled successfully");
    }


    /**
     * Exposes the variant-specific executor that backs all asynchronous persistence work.
     *
     * <p>The returned executor is created during construction and shut down inside
     * {@link #onDisable()} so callers can safely reuse it without leaking threads.</p>
     *
     * @return executor dedicated to repository and background operations
     */
    @Override
    public @NotNull ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Finds a player by unique identifier while enforcing null guards and repository readiness.
     *
     * <p>The method validates the supplied identifier, ensures repositories finished wiring through
     * {@link #ensureReposReady()}, and delegates execution to the asynchronous executor provided by
     * {@link #getExecutor()}.</p>
     *
     * @param uniqueId player UUID to lookup
     * @return future completing with the matching player when present
     */
    @Override
    public @NotNull CompletableFuture<java.util.Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.findByUuidAsync(uniqueId);
    }

    /**
     * Retrieves a player snapshot by name once repository wiring succeeds.
     *
     * <p>Null player names are rejected immediately and a readiness check guards against early
     * invocation before {@link #initializeRepositories()} completes. The lookup then executes on the
     * asynchronous executor returned by {@link #getExecutor()}.</p>
     *
     * @param playerName profile name to search for
     * @return future yielding the player when the repository contains a match
     */
    @Override
    public @NotNull CompletableFuture<java.util.Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.findByNameAsync(playerName);
    }

    /**
     * Persists a new player aggregate while honoring lifecycle and executor guarantees.
     *
     * <p>The supplied aggregate must be non-null. Repository readiness is enforced via
     * {@link #ensureReposReady()} before delegating to the asynchronous repository which uses the
     * executor from {@link #getExecutor()}.</p>
     *
     * @param player aggregate to store in the persistence layer
     * @return future resolving to the stored aggregate
     */
    @Override
    public @NotNull CompletableFuture<RPlayer> createAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.createAsync(player);
    }

    /**
     * Updates an existing player aggregate once the repositories finish initialization.
     *
     * <p>The method rejects null aggregates, checks repository readiness, and performs the update on
     * the dedicated executor returned by {@link #getExecutor()}.</p>
     *
     * @param player aggregate to update in persistent storage
     * @return future containing the updated aggregate
     */
    @Override
    public @NotNull CompletableFuture<RPlayer> updateAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.updateAsync(player);
    }


    /**
     * Provides access to the player statistic repository once initialization completes.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories are not yet wired
     */
    public @NotNull RPlayerStatisticRepository getRPlayerStatisticRepository() {
        ensureReposReady();
        return this.rPlayerStatisticRepository;
    }

    /**
     * Provides access to the statistic template repository after repository wiring succeeds.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories are not yet wired
     */
    public @NotNull RStatisticRepository getRStatisticRepository() {
        ensureReposReady();
        return this.rStatisticRepository;
    }

    /**
     * Provides access to the server repository after repository wiring succeeds.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories are not yet wired
     */
    public @NotNull RServerRepository getRServerRepository() {
        ensureReposReady();
        return this.rServerRepository;
    }

    /**
     * Provides access to the player repository after repository wiring succeeds.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories are not yet wired
     */
    public @NotNull RPlayerRepository getRPlayerRepository() {
        ensureReposReady();
        return this.rPlayerRepository;
    }

    /**
     * Returns the shared platform facade which exposes dependency injection, metrics, and
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
     * Registers the {@link RCoreService} provider with Bukkit during {@link #onLoad()} so downstream
     * plugins can access the adapter-backed API throughout the server lifecycle.
     *
     * <p>Registration occurs synchronously on the main thread and uses a normal priority because the
     * free variant does not override any other provider. A missing service instance indicates a
     * programming error and results in an {@link IllegalStateException}.</p>
     */
    private void registerService() {
        if (this.rCoreService == null) {
            throw new IllegalStateException("rCoreService not initialized");
        }
        Bukkit.getServer().getServicesManager().register(
                RCoreService.class,
                this.rCoreService,
                this.getPlugin(),
                ServicePriority.Normal
        );
        LOGGER.info("Registered RCoreService provider (Free) with priority NORMAL");
    }

    /**
     * Removes the service provider from Bukkit's registry when {@link #onDisable()} executes.
     *
     * <p>If the adapter never initialized the method falls back to unregistering every service owned
     * by this plugin to guarantee a clean shutdown state.</p>
     */
    private void unregisterService() {
        if (this.rCoreService != null) {
            Bukkit.getServer().getServicesManager().unregister(RCoreService.class, this.rCoreService);
            LOGGER.info("Unregistered RCoreService provider (Free)");
        } else {
            Bukkit.getServer().getServicesManager().unregisterAll(this.getPlugin());
        }
    }

    /**
     * Attempts a graceful executor shutdown from {@link #onDisable()} before interrupting outstanding
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

    /**
     * Registers commands and listeners through the shared command factory.
     */
    private void initializeComponents() {
        final CommandFactory commandFactory = new CommandFactory(this.getPlugin());
        commandFactory.registerAllCommandsAndListeners();
    }

    /**
     * Lazily wires repositories once the platform exposes an {@link EntityManagerFactory}.
     *
     * <p>If JPA failed to initialize the method logs a warning and leaves repositories unset so
     * subsequent calls trigger {@link IllegalStateException} through {@link #ensureReposReady()}.</p>
     */
    private void initializeRepositories() {
        final EntityManagerFactory emf = this.platform.getEntityManagerFactory();

        if (emf == null) {
            CentralLogger.getLogger(RCoreFreeImpl.class).warning("EntityManagerFactory not initialized");
            return;
        }

        this.rPlayerRepository = new RPlayerRepository(this.executor, emf);
        this.rPlayerStatisticRepository = new RPlayerStatisticRepository(this.executor, emf);
        this.rStatisticRepository = new RStatisticRepository(this.executor, emf);
        this.rServerRepository = new RServerRepository(this.executor, emf);
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

    /**
     * Asynchronously registers the running server entity in the persistence layer.
     *
     * <p>Completion updates the cached {@link #rServer} reference, while failures propagate through
     * the returned future so {@link #onEnable()} can handle errors and disable the plugin.</p>
     *
     * @return future completing when the server registration sequence finishes
     */
    private CompletableFuture<Void> initializeServerAsync() {
        this.rServer = new RServer(UUID.randomUUID(), Bukkit.getServer().getName());
        return this.rServerRepository.createAsync(rServer)
                .thenAccept(server -> {
                    this.rServer = server;
                    LOGGER.info("Server registered: %s (%s)"
                            .formatted(server.getServerName(), server.getUniqueId()));
                });
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
     * Ensures repositories are initialized before servicing asynchronous requests.
     *
     * @throws IllegalStateException when repository wiring has not completed
     */
    private void ensureReposReady() {
        if (this.rPlayerRepository == null) {
            throw new IllegalStateException("Repositories are not initialized yet");
        }
    }

    /**
     * Schedules the provided runnable on the Bukkit main thread and exposes its completion as a
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
        Bukkit.getScheduler().runTask(this.getPlugin(), () -> {
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
}
