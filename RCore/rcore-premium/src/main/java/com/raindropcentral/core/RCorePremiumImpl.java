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
import java.util.logging.Logger;

/**
 * Edition delegate for the premium RCore distribution.
 *
 * <p>Premium extends the baseline orchestration provided by the free variant so enhanced features
 * can layer on top of the shared {@link RCoreBackend} contract. The delegate owns lifecycle hooks,
 * repository wiring, optional integration detection, and service registration while deferring API
 * exposure to {@link RCoreAdapter}.</p>
 *
 * <p>Lifecycle summary:</p>
 * <ul>
 *     <li>{@link #onLoad()} prepares logging and platform infrastructure so premium can initialise
 *     without immediately exposing services.</li>
 *     <li>{@link #onEnable()} runs synchronous initialization stages, wires repositories, registers
 *     optional integrations, and only then creates the adapter-backed {@link RCoreService} so
 *     premium-specific overrides can be in place before other plugins resolve the provider.</li>
 *     <li>{@link #onDisable()} unregisters services and tears down executors to avoid leaking
 *     resources between reloads.</li>
 * </ul>
 *
 * <p>All backend API methods delegate to asynchronous repositories backed by a dedicated executor to
 * avoid blocking the Bukkit main thread.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCorePremiumImpl extends AbstractPluginDelegate<RCorePremium> implements RCoreBackend {

    /**
     * Edition-scoped logger emitting lifecycle diagnostics via the shared central logger.
     */
    private static final Logger LOGGER = CentralLogger.getLogger(RCorePremiumImpl.class);

    /**
     * Executor powering asynchronous repository calls and background tasks.
     */
    private final ExecutorService executor;

    /**
     * Records the enabled state of optional supported plugins detected during boot.
     */
    private final Map<String, Boolean> enabledSupportedPlugins;

    /**
     * Cached server entity once registered with persistence.
     */
    private RServer rServer;

    /**
     * Repository for player statistic persistence.
     */
    private RPlayerStatisticRepository rPlayerStatisticRepository;

    /**
     * Repository for statistic metadata operations.
     */
    private RStatisticRepository rStatisticRepository;

    /**
     * Repository for player account persistence.
     */
    private RPlayerRepository rPlayerRepository;

    /**
     * Repository for server registrations.
     */
    private RServerRepository rServerRepository;

    /**
     * Adapter-backed service provider registered with Bukkit once initialization completes.
     */
    private RCoreService rCoreService;

    /**
     * Shared platform handle exposing metrics, dependency injection, and persistence utilities.
     */
    private RPlatform platform;

    /**
     * Creates the premium delegate and provisions the executor supporting asynchronous operations.
     *
     * @param rCore plugin context that owns the lifecycle
     */
    public RCorePremiumImpl(final @NotNull RCorePremium rCore) {
        super(rCore);
        this.executor = createExecutorService();
        this.enabledSupportedPlugins = new HashMap<>();
    }

    /**
     * Initializes logging facilities and constructs the shared platform.
     *
     * <p>Premium delays service registration until {@link #onEnable()} to guarantee optional
     * integrations are available before other plugins consume the provider.</p>
     */
    @Override
    public void onLoad() {
        CentralLogger.initialize(this.getPlugin());
        this.platform = new RPlatform(this.getPlugin());
        LOGGER.info("RCore (Free) loaded successfully");
    }

    /**
     * Performs synchronous startup, wires repositories, registers integrations, and publishes the
     * {@link RCoreService} to Bukkit.
     *
     * <p>The method intentionally executes on the main thread. While repository creation uses the
     * asynchronous executor, Bukkit services and command registration require primary-thread
     * execution to remain safe. Service registration currently uses {@link ServicePriority#Normal};
     * premium installations may raise this priority when overriding other providers.</p>
     */
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

    /**
     * Removes the service provider and stops the executor during shutdown.
     */
    @Override
    public void onDisable() {
        unregisterService();
        shutdownExecutor();
        LOGGER.info("RCore (Free) disabled successfully");
    }

    // =====================================================================================
    // RCoreBackend implementation (consumed by the adapter)
    // =====================================================================================

    /**
     * Exposes the dedicated premium executor created during construction.
     *
     * <p>The premium bootstrap sequence constructs the executor ahead of repository wiring so
     * asynchronous lookups are always backed by an isolated pool. Returning the same
     * {@link ExecutorService} ensures downstream adapters use the premium-managed virtual threads
     * instead of falling back to the common fork-join pool.</p>
     *
     * @return the executor orchestrating premium asynchronous work
     */
    @Override
    public @NotNull ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Resolves a player entity by UUID using the premium asynchronous pipeline.
     *
     * <p>Premium defers repository creation until {@link #onEnable()} completes; this lookup first
     * calls {@link #ensureReposReady()} so early invocations surface clear state errors. Once the
     * premium repositories are online the query executes on the dedicated executor, preserving the
     * main-thread semantics expected by Bukkit.</p>
     *
     * @param uniqueId player identifier to resolve
     * @return future yielding the resolved player when present
     */
    @Override
    public @NotNull CompletableFuture<java.util.Optional<RPlayer>> findByUuidAsync(final @NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.findByUuidAsync(uniqueId);
    }

    /**
     * Resolves a player entity by their last known username using premium repositories.
     *
     * <p>The premium lifecycle mirrors the free distribution yet inserts optional integration
     * detection before repository wiring. This method therefore validates initialization order with
     * {@link #ensureReposReady()} and schedules the query on the premium executor so username lookups
     * never block the server thread.</p>
     *
     * @param playerName username to resolve
     * @return future yielding the resolved player when present
     */
    @Override
    public @NotNull CompletableFuture<java.util.Optional<RPlayer>> findByNameAsync(final @NotNull String playerName) {
        Objects.requireNonNull(playerName, "playerName cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.findByNameAsync(playerName);
    }

    /**
     * Persists a newly created player using premium persistence extensions.
     *
     * <p>Premium stores additional metrics and integration flags relative to the free edition. The
     * method enforces repository initialization before dispatching the create task on the dedicated
     * executor, guaranteeing premium deltas are processed without blocking the Bukkit lifecycle.</p>
     *
     * @param player aggregate to persist
     * @return future completing with the saved player snapshot
     */
    @Override
    public @NotNull CompletableFuture<RPlayer> createAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.createAsync(player);
    }

    /**
     * Updates an existing player record while maintaining premium-specific metadata.
     *
     * <p>Premium propagates delta computations for integrations such as RDR and Towny. The update
     * is queued on the premium executor after {@link #ensureReposReady()} verifies initialization,
     * protecting the main thread and ensuring repository guards remain active.</p>
     *
     * @param player aggregate to update
     * @return future completing with the updated entity
     */
    @Override
    public @NotNull CompletableFuture<RPlayer> updateAsync(final @NotNull RPlayer player) {
        Objects.requireNonNull(player, "player cannot be null");
        ensureReposReady();
        return this.rPlayerRepository.updateAsync(player);
    }

    // =====================================================================================
    // Internal getters / wiring
    // =====================================================================================

    /**
     * Returns the player statistic repository once initialization completes.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories have not yet been created
     */
    public @NotNull RPlayerStatisticRepository getRPlayerStatisticRepository() {
        ensureReposReady();
        return this.rPlayerStatisticRepository;
    }

    /**
     * Returns the statistic metadata repository once initialization completes.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories have not yet been created
     */
    public @NotNull RStatisticRepository getRStatisticRepository() {
        ensureReposReady();
        return this.rStatisticRepository;
    }

    /**
     * Returns the server repository once initialization completes.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories have not yet been created
     */
    public @NotNull RServerRepository getRServerRepository() {
        ensureReposReady();
        return this.rServerRepository;
    }

    /**
     * Returns the player repository once initialization completes.
     *
     * @return the initialized repository
     * @throws IllegalStateException when repositories have not yet been created
     */
    public @NotNull RPlayerRepository getRPlayerRepository() {
        ensureReposReady();
        return this.rPlayerRepository;
    }

    /**
     * Exposes the shared platform handle for integrations requiring metrics or persistence access.
     *
     * @return the initialized platform
     */
    public @NotNull RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Provides an immutable snapshot of optional integration detection for diagnostics.
     *
     * @return immutable view of detected plugin states
     */
    public @NotNull Map<String, Boolean> getEnabledSupportedPlugins() {
        return Map.copyOf(this.enabledSupportedPlugins);
    }

    /**
     * Creates the dedicated executor backing asynchronous operations.
     *
     * @return per-task virtual-thread executor suitable for blocking persistence work
     */
    private ExecutorService createExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Registers the {@link RCoreService} provider with Bukkit after initialization succeeds.
     *
     * <p>The premium jar currently registers with {@link ServicePriority#Normal}. Deployments that
     * override the free edition may raise this to {@link ServicePriority#Highest} to ensure premium
     * answers lookups first.</p>
     */
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

    /**
     * Unregisters the {@link RCoreService} provider or clears all plugin-owned services when no
     * provider was registered.
     */
    private void unregisterService() {
        if (this.rCoreService != null) {
            Bukkit.getServer().getServicesManager().unregister(RCoreService.class, this.rCoreService);
            LOGGER.info("Unregistered RCoreService provider (Free)");
        } else {
            // Ensure no stale registrations remain
            Bukkit.getServer().getServicesManager().unregisterAll(this.getPlugin());
        }
    }

    /**
     * Attempts a graceful executor shutdown and logs whenever work requires interruption.
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
     * Registers all premium commands and listeners using the shared command factory.
     */
    private void initializeComponents() {
        final CommandFactory commandFactory = new CommandFactory(this.getPlugin());
        commandFactory.registerAllCommandsAndListeners();
    }

    /**
     * Wires repositories once the platform exposes an {@link EntityManagerFactory}.
     *
     * <p>Failure to create the factory is logged and leaves repositories unset so guard checks can
     * surface the issue when backend calls arrive.</p>
     */
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

    /**
     * Detects supported optional plugins so premium features can react accordingly.
     *
     * <p>Premium performs this scan before repository-dependent tasks to inform downstream deltas
     * such as asynchronous sync jobs or statistic enrichments. The resulting map drives premium-only
     * behaviour toggles exposed via {@link #getEnabledSupportedPlugins()}.</p>
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
     * Registers the running server entity asynchronously and logs the result.
     *
     * <p>Premium records additional server metadata compared to the free release, so the registration
     * fires after repositories initialize but before metrics activation. Failures are logged and
     * suppressed because premium enablement continues even when the persistence layer rejects the
     * registration. Future iterations may tighten this behaviour by disabling the plugin on
     * failure.</p>
     */
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

    /**
     * Checks whether a plugin with the provided name is currently enabled.
     *
     * @param pluginName plugin identifier to test
     * @return {@code true} if enabled
     */
    private boolean isPluginEnabled(final @NotNull String pluginName) {
        return Bukkit.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    /**
     * Ensures repositories are wired before servicing backend API calls.
     *
     * <p>The premium lifecycle invokes multiple initialization stages (components, repositories,
     * integrations, server registration). This guard protects asynchronous helpers from executing
     * before the repository stage completes, ensuring premium deltas—such as integration metadata
     * columns—are available.</p>
     *
     * @throws IllegalStateException when repositories remain uninitialized
     */
    private void ensureReposReady() {
        if (this.rPlayerRepository == null) {
            throw new IllegalStateException("Repositories are not initialized yet");
        }
    }

    /**
     * ASCII-art banner emitted during successful startup.
     *
     * <p>The banner highlights premium branding and companion technologies so console operators can
     * immediately distinguish the premium distribution from the free variant during boot logs.</p>
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
