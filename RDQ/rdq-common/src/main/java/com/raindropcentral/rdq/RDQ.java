package com.raindropcentral.rdq;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.utility.BountyManager;
import com.raindropcentral.rdq.utility.rank.RankSystemFactory;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base contract shared by every RDQ edition, orchestrating the staged lifecycle described in
 * {@link com.raindropcentral.rdq package documentation}. Implementations bootstrap platform
 * services, bind GUIs, and register repositories while respecting synchronous Bukkit threading
 * boundaries. The lifecycle flows through asynchronous platform initialization, synchronous
 * component/view wiring, and repository registration, re-entering the main thread through
 * {@link #runSync(Runnable)} whenever Bukkit state is touched. Subclasses may extend the
 * lifecycle by chaining additional futures to {@link #onEnable()} while preserving the
 * sequencing guarantees captured here.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class RDQ {

    /**
     * Owning plugin instance responsible for delegating lifecycle hooks and exposing shared
     * Bukkit facilities. This reference is immutable after construction and is safe to access
     * from any lifecycle stage.
     */
    private final JavaPlugin plugin;

    /**
     * Edition-specific coordinator supplying quest, perk, and bounty controllers used during
     * component registration. The coordinator remains constant for the life of the plugin and
     * may expose cross-module state to RDQ integrations.
     */
    private final RDQManager manager;

    /**
     * Edition string resolved during plugin bootstrap to assist logging and factory selection.
     * Defaults to the constructor-provided value and may be reassigned by subclasses if the
     * edition is re-detected after start-up.
     */
    private String detectedEdition;

    /**
     * Primary asynchronous executor used for off-thread initialization and repository IO. The
     * executor is created eagerly using {@link #createExecutor()}, preferring virtual threads and
     * falling back to a fixed pool when unavailable.
     */
    private final ExecutorService executor = createExecutor();

    /**
     * Future representing the asynchronous enable sequence, allowing cancellation during disable.
     * Starts as {@code null} and is published as a {@code volatile} field once {@link #onEnable()}
     * kicks off the lifecycle.
     */
    private volatile CompletableFuture<Void> enableFuture;

    /**
     * Active platform bridge exposing shared services (ORM, metrics, sync helpers) to RDQ modules.
     * Instantiated during construction, backed by the {@code RPlatform} module, and initialised
     * asynchronously in {@link #initializePlatformAsync()}.
     */
    private RPlatform platform;

    /**
     * Root inventory-framework view frame responsible for RDQ GUI wiring. Assigned during
     * {@link #initializeViews()} once component wiring is complete and built against the
     * InventoryFramework dependency shared across modules.
     */
    private ViewFrame viewFrame;

    /**
     * Runtime bounty controller accessed by commands and views for bounty orchestration. Created
     * anew for each enable cycle and retained until disable, coordinating with RDQ view flows.
     */
    private BountyManager bountyManager;

    /**
     * Factory delegating rank system setup and asynchronous initialisation for the running edition.
     * Optional for editions that do not expose ranking features; otherwise populated by package
     * peers before {@link #onEnable()} completes so asynchronous initialisation can run.
     */
    private RankSystemFactory rankSystemFactory;

    /**
     * Command factory responsible for registering all RDQ commands and listeners. Created during
     * {@link #initializeComponents()}, backed by the JExCommand module, and released automatically
     * when the plugin disables.
     */
    private CommandFactory commandFactory;

    /**
     * Repository handling quest player persistence and transactional access. Lazily assigned in
     * {@link #initializeRepositories()} and executed on the shared {@link #executor}.
     */
    private RDQPlayerRepository playerRepository;

    /**
     * Repository providing persistence for player rank progression paths. Created during
     * {@link #initializeRepositories()} and shares the common executor.
     */
    private RPlayerRankPathRepository playerRankPathRepository;

    /**
     * Repository managing stored player rank entries. Initialised alongside other repositories and
     * scoped to the current enable cycle.
     */
    private RPlayerRankRepository playerRankRepository;

    /**
     * Repository coordinating perk unlock state for each player. Populated once the platform ORM is
     * ready.
     */
    private RPlayerPerkRepository playerPerkRepository;

    /**
     * Repository responsible for bounty definitions and completion progress. Built against the
     * current {@link EntityManagerFactory} each enable cycle.
     */
    private RBountyRepository bountyRepository;

    /**
     * Repository exposing configured ranks for quests and perks. Shares the lifecycle of the
     * surrounding plugin enablement.
     */
    private RRankRepository rankRepository;

    /**
     * Repository supplying perk metadata utilised by progression flows. Bound to the shared
     * executor to avoid blocking the main thread.
     */
    private RPerkRepository perkRepository;

    /**
     * Repository tracking rank upgrade progress for each player. Created lazily during enable and
     * re-used until disable.
     */
    private RPlayerRankUpgradeProgressRepository playerRankUpgradeProgressRepository;

    /**
     * Repository exposing rank tree structures underpinning quest gating. Uses the common executor
     * for persistence operations.
     */
    private RRankTreeRepository rankTreeRepository;

    /**
     * Repository providing requirement definitions for quests and perks. Instantiated during
     * {@link #initializeRepositories()} and cached for downstream services.
     */
    private RRequirementRepository requirementRepository;

    /**
     * Flag toggled when the plugin begins shutdown to guard against late lifecycle work. Defaults
     * to {@code false} and flips to {@code true} during {@link #onDisable()}.
     */
    private boolean isDisabling;

    /**
     * Constructs the base RDQ contract, assigning the running plugin, detected edition, and manager
     * while bootstrapping the shared platform bridge and central logging. Construction is invoked on
     * the main thread during {@link JavaPlugin#onLoad()}.
     *
     * @param plugin  backing plugin instance
     * @param edition textual identifier for the edition resolved during bootstrap
     * @param manager coordinator supplying edition-specific services
     */
    public RDQ(
            final @NotNull JavaPlugin plugin,
            final @NotNull String edition,
            final @NotNull RDQManager manager
    ) {
        this.plugin = plugin;
        this.detectedEdition = edition;
        this.manager = manager;
        this.platform = new RPlatform(plugin);
        CentralLogger.initialize(plugin);
    }

    /**
     * Invoked during the Bukkit {@code onLoad} lifecycle to perform lightweight logging. The call
     * occurs on the server main thread before any asynchronous work begins and intentionally
     * suppresses logging failures so bootstrap can continue.
     */
    public void onLoad() {
        try {
            plugin.getLogger().info("Loading RDQ " + detectedEdition + " Edition");
        } catch (final Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ", exception);
        }
    }

    /**
     * Begins the staged enable pipeline. Platform setup executes asynchronously before re-entering
     * the main thread to initialise components, views, repositories, and metrics. The returned
     * future is stored for cancellation during disable, and failures propagate through chained
     * {@link CompletionException CompletionExceptions} so the plugin can disable itself. Repeated
     * invocations while enable is still running short-circuit to avoid double registration.
     */
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            plugin.getLogger().warning("Enable sequence already in progress");
            return;
        }
        final Logger log = CentralLogger.getLogger(getClass().getName());
        this.enableFuture = initializePlatformAsync()
                .thenCompose(v -> runSync(() -> {
                    try {
                        initializeComponents();
                        initializeViews();
                        initializeRepositories();
                    } catch (Throwable t) {
                        throw new CompletionException(t);
                    }
                }))
                .thenCompose(v -> {
                    if (this.rankSystemFactory == null) return CompletableFuture.completedFuture(null);
                    try {
                        return this.rankSystemFactory.initializeAsync();
                    } catch (Throwable t) {
                        return CompletableFuture.failedFuture(t);
                    }
                })
                .thenCompose(v -> runSync(() -> {
                    try {
                        this.platform.initializeMetrics(getMetricsId());
                        registerServices();
                        plugin.getLogger().info("RDQ " + detectedEdition + " Edition enabled successfully!");
                    } catch (Throwable t) {
                        throw new CompletionException(t);
                    }
                }))
                .exceptionally(ex -> {
                    runSync(() -> {
                        log.log(Level.SEVERE, "Failed to enable RDQ (" + Optional.ofNullable(detectedEdition).orElse("?") + ")", ex);
                        plugin.getServer().getPluginManager().disablePlugin(plugin);
                    });
                    return null;
                });
    }

    /**
     * Cancels the enable pipeline, shuts down executors, and marks the plugin as disabling. Invoked
     * on the main thread during {@code JavaPlugin#onDisable()} to prevent further lifecycle work and
     * ensure asynchronous jobs terminate promptly.
     */
    public void onDisable() {
        this.isDisabling = true;
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            this.enableFuture.cancel(true);
        }
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }

    /**
     * Supplies the edition-specific startup message displayed prior to full enablement. Implementers
     * should avoid heavy computation as the value is logged synchronously from the main thread.
     *
     * @return message sent to the console during bootstrap
     */
    @NotNull
    protected abstract String getStartupMessage();

    /**
     * Provides the metrics identifier used when registering RDQ with the shared metrics subsystem.
     * Returning an incorrect identifier will prevent metrics registration during enable.
     *
     * @return metrics plugin id
     */
    protected abstract int getMetricsId();

    /**
     * Allows editions to register additional views before the base frame is committed. Invoked on
     * the main thread after default RDQ bounty views are configured, providing a hook for
     * thread-confined view mutations.
     *
     * @param viewFrame pre-configured frame containing default RDQ views
     * @return frame with any edition-specific views included
     */
    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    /**
     * Initializes command-bound components on the main thread, ensuring the bounty manager is
     * available prior to listener registration. No synchronization is required because execution
     * occurs inside the {@link #runSync(Runnable)} boundary, and command registration flows through
     * the shared JExCommand infrastructure.
     */
    private void initializeComponents() {
        // Initialize BountyManager BEFORE registering listeners so they can access it
        this.bountyManager = new BountyManager(this);

        // Now register commands and listeners - they can safely use getBountyManager()
        this.commandFactory = new CommandFactory(plugin, this);
        this.commandFactory.registerAllCommandsAndListeners();
    }

    /**
     * Registers the shared RDQ bounty view frame and allows subclasses to extend it before the
     * frame is committed. Execution occurs on the main thread and must remain lightweight to avoid
     * stalling the server tick while UI elements are wired.
     */
    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        ViewFrame viewFrame = ViewFrame
                .create(plugin)
                .with(
                        new BountyMainView(),
                        new BountyCreationView(),
                        new BountyOverviewView(),
                        new BountyRewardView(),
                        new BountyPlayerInfoView()
                )
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                    config.interactionDelay(Duration.ofMillis(100));
                });
        viewFrame = registerViews(viewFrame);
        this.viewFrame = viewFrame.register();
    }

    /**
     * Wires repository instances against the active {@link EntityManagerFactory}. Repositories run
     * asynchronous operations on the shared executor to avoid blocking the server thread and honour
     * Bukkit's threading rules, integrating RDQ persistence with the wider database layer.
     */
    private void initializeRepositories() {
        final EntityManagerFactory emf = this.platform.getEntityManagerFactory();
        this.playerRepository = new RDQPlayerRepository(this.executor, emf);
        this.bountyRepository = new RBountyRepository(this.executor, emf);
        this.rankRepository = new RRankRepository(this.executor, emf);
        this.perkRepository = new RPerkRepository(this.executor, emf);
        this.rankTreeRepository = new RRankTreeRepository(this.executor, emf);
        this.requirementRepository = new RRequirementRepository(this.executor, emf);
        this.playerRankUpgradeProgressRepository = new RPlayerRankUpgradeProgressRepository(this.executor, emf);
        this.playerRankPathRepository = new RPlayerRankPathRepository(this.executor, emf);
        this.playerRankRepository = new RPlayerRankRepository(this.executor, emf);
        this.playerPerkRepository = new RPlayerPerkRepository(this.executor, emf);
    }

    /**
     * Completes synchronous service wiring once repositories and components are ready. Logging is
     * performed under the central logger namespace, and any thrown exception aborts enablement.
     * This is the final synchronous step before the plugin is announced as enabled.
     */
    private void registerServices() {
        final Logger log = CentralLogger.getLogger(getClass().getName());
        try {
            // BountyManager is already initialized in initializeComponents()
            log.info("Bounty services wired for " + detectedEdition + ".");
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Service registration failed", t);
            throw t;
        }
    }

    /**
     * Initializes shared platform services asynchronously, deferring expensive work off the main
     * thread while propagating failures back into the enable pipeline. Delegates to
     * {@link RPlatform#initialize()} behind the scenes.
     *
     * @return future completing when the platform is initialised
     */
    private @NotNull CompletableFuture<Void> initializePlatformAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                this.platform.initialize();
            } catch (Throwable t) {
                throw new CompletionException(t);
            }
        }, this.executor);
    }

    /**
     * Schedules the provided runnable on the Bukkit main thread and returns a future completed when
     * the work finishes. This helper enforces synchronous lifecycle stages and propagates
     * exceptions back to asynchronous callers. All synchronous lifecycle blocks should invoke this
     * helper when entered from asynchronous execution.
     *
     * @param runnable action to execute on the main thread
     * @return future that completes once the runnable finishes executing
     */
    private @NotNull CompletableFuture<Void> runSync(final @NotNull Runnable runnable) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
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
     * Creates the shared executor, preferring a virtual-thread-per-task implementation and falling
     * back to a fixed thread pool when the runtime does not support virtual threads. The fallback
     * pool defaults to five threads, matching historical behaviour.
     *
     * @return executor used for asynchronous RDQ work
     */
    private ExecutorService createExecutor() {
        try {
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (Throwable ignored) {
            return Executors.newFixedThreadPool(5);
        }
    }

    /**
     * Exposes the running plugin instance for integration points needing direct Bukkit access.
     * Safe to call at any time because the instance is final.
     *
     * @return backing {@link JavaPlugin}
     */
    @NotNull
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Provides the resolved edition name for logging and conditional logic. The value may change if
     * subclasses re-detect the edition during runtime.
     *
     * @return edition string, or {@code null} if not yet determined
     */
    @Nullable
    public String getEdition() {
        return this.detectedEdition;
    }

    /**
     * Supplies the shared executor for background operations. The executor remains available until
     * {@link #onDisable()} requests shutdown.
     *
     * @return asynchronous executor
     */
    @NotNull
    public ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Retrieves the platform bridge used for metrics, ORM, and lifecycle helpers. Callers should
     * wait until the enable future completes to ensure the platform has been initialised.
     *
     * @return active platform instance
     */
    @NotNull
    public RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Returns the registered view frame encompassing RDQ GUI flows. Available after
     * {@link #initializeViews()} runs within the enable sequence.
     *
     * @return root view frame
     */
    @NotNull
    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    /**
     * Exposes the edition manager coordinating high-level gameplay components. The manager is
     * immutable and safe for concurrent reads.
     *
     * @return RDQ manager instance
     */
    @NotNull
    public RDQManager getManager() {
        return this.manager;
    }

    /**
     * Provides access to the bounty manager for command and view interactions. Non-null once
     * {@link #initializeComponents()} runs during enable.
     *
     * @return active bounty manager
     */
    @NotNull
    public BountyManager getBountyManager() {
        return this.bountyManager;
    }

    /**
     * Returns the rank system factory responsible for asynchronous rank initialisation. Editions
     * without ranking support should avoid calling this method, otherwise a
     * {@link NullPointerException} may occur.
     *
     * @return rank system factory
     */
    @NotNull
    public RankSystemFactory getRankSystemFactory() {
        return this.rankSystemFactory;
    }

    /**
     * Retrieves the repository handling player quest records. Populated during
     * {@link #initializeRepositories()} and safe to use once the enable future completes.
     *
     * @return player repository
     */
    @NotNull
    public RDQPlayerRepository getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Accesses the repository managing player rank paths. Ready after repository wiring finishes in
     * the enable sequence.
     *
     * @return rank path repository
     */
    @NotNull
    public RPlayerRankPathRepository getPlayerRankPathRepository() {
        return this.playerRankPathRepository;
    }

    /**
     * Accesses the repository storing player ranks. Available post-initialization and backed by the
     * shared executor to ensure non-blocking behaviour.
     *
     * @return player rank repository
     */
    @NotNull
    public RPlayerRankRepository getPlayerRankRepository() {
        return this.playerRankRepository;
    }

    /**
     * Retrieves the repository recording player perk unlocks. Initialised alongside other
     * repositories within the enable sequence.
     *
     * @return player perk repository
     */
    @NotNull
    public RPlayerPerkRepository getPlayerPerkRepository() {
        return this.playerPerkRepository;
    }

    /**
     * Returns the repository backing bounty persistence. Provides asynchronous persistence helpers
     * via the shared executor.
     *
     * @return bounty repository
     */
    @NotNull
    public RBountyRepository getBountyRepository() {
        return this.bountyRepository;
    }

    /**
     * Provides rank metadata storage. Consumers should wait for enable completion before invoking.
     *
     * @return rank repository
     */
    @NotNull
    public RRankRepository getRankRepository() {
        return this.rankRepository;
    }

    /**
     * Retrieves the repository containing perk definitions. Backed by the shared executor to prevent
     * main-thread blocking.
     *
     * @return perk repository
     */
    @NotNull
    public RPerkRepository getPerkRepository() {
        return this.perkRepository;
    }

    /**
     * Accesses the repository tracking upgrade progress toward next ranks. Available following the
     * repository wiring stage.
     *
     * @return rank upgrade progress repository
     */
    @NotNull
    public RPlayerRankUpgradeProgressRepository getPlayerRankUpgradeProgressRepository() {
        return this.playerRankUpgradeProgressRepository;
    }

    /**
     * Supplies the repository that resolves rank tree structures. The repository operates against
     * the shared executor for read/write operations.
     *
     * @return rank tree repository
     */
    @NotNull
    public RRankTreeRepository getRankTreeRepository() {
        return this.rankTreeRepository;
    }

    /**
     * Exposes requirement definitions used by quest and perk evaluation flows. Populated during
     * repository wiring and safe for use once enable completes.
     *
     * @return requirement repository
     */
    @NotNull
    public RRequirementRepository getRequirementRepository() {
        return this.requirementRepository;
    }

    /**
     * Indicates whether the plugin is currently shutting down, allowing callers to abort
     * long-lived operations. Set on the main thread during {@link #onDisable()} and visible to
     * background threads.
     *
     * @return {@code true} when disable has started
     */
    public boolean isDisabling() {
        return this.isDisabling;
    }

    /**
     * Exposes the enable future so callers can react to lifecycle completion or cancellation. The
     * returned future may be cancelled when disable begins.
     *
     * @return future representing enable completion, or {@code null} if enable has not started
     */
    public @Nullable CompletableFuture<Void> getEnableFuture() {
        return this.enableFuture;
    }

    /**
     * Delegates to {@link JavaPlugin#isEnabled()} for convenience. Useful for call sites operating
     * from asynchronous threads.
     *
     * @return {@code true} when Bukkit reports the plugin enabled
     */
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    /**
     * Accesses the plugin data folder for persistence operations. Direct file IO should still occur
     * off the main thread.
     *
     * @return plugin data folder
     */
    @NotNull
    public java.io.File getDataFolder() {
        return plugin.getDataFolder();
    }

    /**
     * Convenience getter for the plugin logger. Exposes the same logger returned by the Bukkit
     * plugin container.
     *
     * @return logger associated with the plugin
     */
    @NotNull
    public Logger getLogger() {
        return plugin.getLogger();
    }

    /**
     * Exposes the Bukkit server instance. Callers should respect Bukkit threading rules when
     * interacting with the returned server object.
     *
     * @return running server
     */
    @NotNull
    public org.bukkit.Server getServer() {
        return plugin.getServer();
    }

    /**
     * Provides the plugin configuration for callers that need to access RDQ configuration entries.
     * Mutations should be followed by {@link #saveConfig()} to persist changes.
     *
     * @return plugin configuration
     */
    @NotNull
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Persists the current configuration to disk through the plugin delegate. Long-running writes
     * should be executed asynchronously by the caller.
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Reloads the configuration from disk, delegating to the plugin implementation. Safe to call
     * from asynchronous contexts so long as Bukkit's configuration APIs allow it.
     */
    public void reloadConfig() {
        plugin.reloadConfig();
    }
}