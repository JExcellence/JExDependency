package de.jexcellence.glow;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.glow.database.entity.PlayerGlow;
import de.jexcellence.glow.database.repository.GlowRepository;
import de.jexcellence.glow.factory.GlowFactory;
import de.jexcellence.glow.placeholder.GlowPlaceholder;
import de.jexcellence.glow.service.GlowService;
import de.jexcellence.glow.service.IGlowService;
import de.jexcellence.hibernate.repository.RepositoryManager;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for JExGlow.
 * <p>
 * The delegate bridges Bukkit lifecycle callbacks with the glow system backend.
 * It wires platform services, boots repositories, manages asynchronous initialization,
 * and handles graceful shutdown.
 * </p>
 *
 * <p>Lifecycle overview:</p>
 * <ul>
 *     <li>{@link #onLoad()} instantiates logging and the shared {@link RPlatform}.</li>
 *     <li>{@link #onEnable()} orchestrates asynchronous startup by chaining futures across
 *     platform initialization, repository wiring, service creation, command registration,
 *     and listener setup, handling failure by disabling the plugin on the main thread.</li>
 *     <li>{@link #onDisable()} resets factories and gracefully shuts down the executor
 *     to ensure repository operations complete before the JVM stops.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class JExGlowImpl extends AbstractPluginDelegate<JExGlow> {

    /**
     * Logger emitting lifecycle and repository wiring information through the shared
     * {@link CentralLogger} infrastructure.
     */
    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExGlow");

    /**
     * Executor backing asynchronous repository access and other background tasks. Created
     * during construction and shut down in {@link #onDisable()} to avoid leaking virtual threads.
     */
    private final ExecutorService executor;

    /**
     * Guard future tracking the asynchronous enable pipeline so duplicate invocations can be
     * rejected and so failure paths can cancel pending stages.
     */
    private volatile CompletableFuture<Void> enableFuture;

    /**
     * Shared platform bundle that configures dependency injection, metrics, and persistence.
     */
    private RPlatform platform;

    /**
     * Repository for managing player glow state persistence.
     */
    private GlowRepository glowRepository;

    /**
     * Service for managing glow operations.
     */
    private IGlowService glowService;

    /**
     * Creates the delegate and provisions the executor used across asynchronous operations.
     *
     * @param plugin the owning plugin instance that supplies Bukkit context
     */
    public JExGlowImpl(final @NotNull JExGlow plugin) {
        super(plugin);
        this.executor = createExecutorService();
    }

    /**
     * Initializes logging and constructs the shared platform.
     * <p>
     * The platform is created eagerly so other components can access it during enable.
     * </p>
     */
    @Override
    public void onLoad() {
        this.platform = new RPlatform(this.getPlugin());

        LOGGER.info("JExGlow loaded successfully");
    }

    /**
     * Orchestrates asynchronous startup across platform, repository, service, and integration phases.
     * <p>
     * The method first ensures only a single enable chain runs at a time by tracking the returned
     * {@link CompletableFuture}. It then executes platform initialization and repository wiring on
     * the main thread using {@link #runSync(Runnable)} to preserve Bukkit thread-safety guarantees.
     * Any failure disables the plugin on the main thread to avoid running without persistence guarantees.
     * </p>
     */
    @Override
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            LOGGER.warning("Enable sequence already in progress");
            return;
        }

        this.enableFuture = this.platform.initialize()
            .thenCompose(v -> runSync(this::initializeRepositories))
            .thenCompose(v -> runSync(this::initializeServices))
            .thenCompose(v -> runSync(this::initializeCommands))
            .thenCompose(v -> runSync(this::initializePlaceholderAPI))
            .thenCompose(v -> runSync(() -> {
                this.platform.initializeMetrics(24680); // Replace with actual bStats plugin ID
                LOGGER.info(STARTUP_MESSAGE);
                LOGGER.info("JExGlow enabled successfully");
            }))
            .exceptionally(ex -> {
                runSync(() -> {
                    LOGGER.log(Level.SEVERE, "Failed to enable JExGlow", ex);
                    this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
                });
                return null;
            });
    }

    /**
     * Resets factories and shuts down asynchronous infrastructure.
     * <p>
     * The executor is given a grace period to complete in-flight repository calls before being
     * interrupted. This ensures outstanding persistence tasks flush correctly during server
     * shutdown.
     * </p>
     */
    @Override
    public void onDisable() {
        // Reset factory
        GlowFactory.reset();

        // Shutdown executor
        shutdownExecutor();

        LOGGER.info("JExGlow disabled successfully");
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
     * Returns the executor service used for asynchronous operations.
     *
     * @return the executor service
     */
    public @NotNull ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Returns the glow repository instance.
     *
     * @return the glow repository
     */
    public @NotNull GlowRepository getGlowRepository() {
        return this.glowRepository;
    }

    /**
     * Returns the glow service instance.
     *
     * @return the glow service
     */
    public @NotNull IGlowService getGlowService() {
        return this.glowService;
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
     * Attempts a graceful executor shutdown from {@link #onDisable()} before interrupting outstanding
     * tasks.
     * <p>
     * The method waits up to ten seconds for tasks to finish and logs whenever a forced shutdown
     * or interruption occurs so operators can diagnose long-running jobs.
     * </p>
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
     * Lazily wires repositories once the platform exposes an {@link EntityManagerFactory}.
     * <p>
     * Uses RepositoryManager to register and inject repositories.
     * </p>
     */
    private void initializeRepositories() {
        final var emf = this.platform.getEntityManagerFactory();

        if (emf == null) {
            throw new IllegalStateException("EntityManagerFactory not initialized");
        }

        RepositoryManager.initialize(this.executor, emf);
        var repositoryManager = RepositoryManager.getInstance();

        // Register GlowRepository
        repositoryManager.register(
            GlowRepository.class,
            PlayerGlow.class,
            PlayerGlow::getPlayerUuid
        );

        // Get repository instance
        this.glowRepository = repositoryManager.getRepository(GlowRepository.class);

        LOGGER.info("Repositories initialized");
    }

    /**
     * Initializes services and the factory.
     */
    private void initializeServices() {
        // Create glow service
        this.glowService = new GlowService(glowRepository, this.getPlugin());

        // Initialize factory
        GlowFactory.initialize(glowService, glowRepository);

        LOGGER.info("Services initialized");
    }

    /**
     * Initializes commands through the shared command factory.
     */
    private void initializeCommands() {
        try {
            // Create command factory with auto-registration
            var commandFactory = new CommandFactory(this.getPlugin(), this);
            commandFactory.registerAllCommandsAndListeners();

            LOGGER.info("Commands registered");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register commands", e);
            throw new RuntimeException("Command registration failed", e);
        }
    }

    /**
     * Initializes PlaceholderAPI integration if available.
     */
    private void initializePlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new GlowPlaceholder().register();
                LOGGER.info("PlaceholderAPI integration enabled");
            } catch (NoClassDefFoundError e) {
                LOGGER.log(Level.WARNING, "PlaceholderAPI plugin found but classes not available", e);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to register PlaceholderAPI expansion", e);
            }
        } else {
            LOGGER.info("PlaceholderAPI not found, placeholder support disabled");
        }
    }

    /**
     * Schedules the provided runnable on the Bukkit main thread and exposes its completion as a
     * future.
     * <p>
     * Errors thrown by the runnable complete the returned future exceptionally so the enable
     * pipeline can abort and trigger shutdown logic. The helper centralizes thread marshaling for
     * stages that must interact with Bukkit APIs.
     * </p>
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
                      _____  ______        _____  _
                     |_   _||  ____|      / ____|| |
                       | |  | |__  __  __| |  __ | |  ___ __      __
                       | |  |  __| \\ \\/ /| | |_ || | / _ \\\\ \\ /\\ / /
                      _| |_ | |____ >  < | |__| || || (_) |\\ V  V /
                     |_____||______/_/\\_\\ \\_____||_| \\___/  \\_/\\_/
        
                     Product of JExcellence
                     Powered by RPlatform
        ===============================================================================================
        Persistent Player Glow Effects System
        Technology Partner: JExcellence
        ===============================================================================================
        """;
}
