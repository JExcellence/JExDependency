package de.jexcellence.multiverse;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.multiverse.api.IMultiverseAdapter;
import de.jexcellence.multiverse.api.MultiverseAdapter;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.database.repository.MVWorldRepository;
import de.jexcellence.multiverse.factory.WorldFactory;
import de.jexcellence.multiverse.service.IMultiverseService;
import de.jexcellence.multiverse.view.MultiverseEditorView;
import lombok.Getter;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for JExMultiverse plugin following RDQ patterns.
 * <p>
 * This class provides the core infrastructure for the multiverse plugin including:
 * <ul>
 *   <li>Platform initialization via RPlatform</li>
 *   <li>Repository management for MVWorld entities</li>
 *   <li>World factory for world creation and management</li>
 *   <li>View frame for GUI components</li>
 *   <li>Service registration for external API access</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public abstract class JExMultiverse {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExMultiverse");

    private final JavaPlugin plugin;
    private final String edition;
    private final ExecutorService executor;
    private final RPlatform platform;

    private volatile CompletableFuture<Void> onEnableFuture;

    private boolean disabling;
    private boolean postEnableCompleted;

    private ViewFrame viewFrame;
    private WorldFactory worldFactory;
    private IMultiverseService multiverseService;
    private IMultiverseAdapter multiverseAdapter;

    private MVWorldRepository worldRepository;

    /**
     * Constructs a new JExMultiverse instance.
     *
     * @param plugin  the JavaPlugin instance
     * @param edition the edition name (Free or Premium)
     */
    public JExMultiverse(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
        this.platform = new RPlatform(plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Called when the plugin is loaded.
     */
    public void onLoad() {
        LOGGER.info("Loading JExMultiverse " + edition + " Edition");
    }

    /**
     * Called when the plugin is enabled.
     * <p>
     * Initializes all components in the correct order:
     * <ol>
     *   <li>Platform initialization</li>
     *   <li>Repository initialization</li>
     *   <li>Service initialization</li>
     *   <li>Factory initialization</li>
     *   <li>Component registration (commands, listeners)</li>
     *   <li>View initialization</li>
     *   <li>World loading</li>
     *   <li>API service registration</li>
     * </ol>
     * </p>
     */
    public void onEnable() {
        if (onEnableFuture != null && !onEnableFuture.isDone()) {
            LOGGER.log(Level.WARNING, "Enable sequence already in progress");
            return;
        }

        onEnableFuture = platform.initialize()
            .thenCompose(v -> {
                initializeRepositories();
                registerBukkitService();
                return CompletableFuture.completedFuture(null);
            })
            .thenRun(() -> {
                initializeFactory();
                initializeService();
                initializeComponents();
                initializeViews();
                platform.initializeMetrics(getMetricsId());
            })
            .thenCompose(v -> loadWorlds())
            .thenRun(() -> {
                LOGGER.info(getStartupMessage());
                LOGGER.info("JExMultiverse (" + edition + ") Edition enabled successfully!");
                postEnableCompleted = true;
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to initialize JExMultiverse", throwable);
                return null;
            });
    }


    /**
     * Called when the plugin is disabled.
     * <p>
     * Performs cleanup in the following order:
     * <ol>
     *   <li>Sets disabling flag</li>
     *   <li>Resets WorldFactory singleton</li>
     *   <li>Shuts down executor service</li>
     *   <li>Shuts down platform</li>
     * </ol>
     * </p>
     */
    public void onDisable() {
        disabling = true;
        WorldFactory.reset();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        platform.shutdown();
        LOGGER.info("JExMultiverse (" + edition + ") Edition disabled successfully!");
    }

    // ==================== Abstract Methods ====================

    /**
     * Gets the startup message to display when the plugin is enabled.
     *
     * @return the startup message
     */
    @NotNull
    protected abstract String getStartupMessage();

    /**
     * Gets the bStats metrics ID for this edition.
     *
     * @return the metrics ID
     */
    protected abstract int getMetricsId();

    /**
     * Registers edition-specific views with the ViewFrame.
     *
     * @param viewFrame the ViewFrame to register views with
     * @return the ViewFrame with registered views
     */
    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    /**
     * Creates the edition-specific multiverse service.
     *
     * @return the multiverse service implementation
     */
    @NotNull
    protected abstract IMultiverseService createMultiverseService();

    // ==================== Initialization Methods ====================

    /**
     * Initializes the repository layer.
     * <p>
     * Creates the MVWorldRepository with the entity manager factory from RPlatform.
     * </p>
     */
    private void initializeRepositories() {
        var emf = platform.getEntityManagerFactory();
        if (emf == null) {
            LOGGER.warning("EntityManagerFactory not initialized");
            return;
        }

        this.worldRepository = new MVWorldRepository(
            executor,
            emf,
            MVWorld.class,
            MVWorld::getIdentifier
        );

        LOGGER.info("MVWorldRepository initialized");
    }

    /**
     * Initializes the service layer.
     * <p>
     * Creates the edition-specific multiverse service.
     * </p>
     */
    private void initializeService() {
        this.multiverseService = createMultiverseService();
        LOGGER.info("Multiverse service initialized: " + (multiverseService.isPremium() ? "Premium" : "Free"));
    }

    /**
     * Initializes the factory layer.
     * <p>
     * Creates the WorldFactory singleton with the repository.
     * </p>
     */
    private void initializeFactory() {
        this.worldFactory = WorldFactory.initialize(plugin, worldRepository);
        LOGGER.info("WorldFactory initialized");
    }

    /**
     * Initializes commands and listeners.
     * <p>
     * Uses CommandFactory to register all commands and listeners from YAML configuration.
     * Also registers the SpawnListener for handling spawn events.
     * </p>
     */
    private void initializeComponents() {
        var commandFactory = new CommandFactory(plugin, this);
        commandFactory.registerAllCommandsAndListeners();

        LOGGER.info("Commands and listeners registered");
    }

    /**
     * Initializes the view layer.
     * <p>
     * Creates the ViewFrame with default configuration and registers views.
     * </p>
     */
    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        var frame = ViewFrame
            .create(plugin)
            .with(new ConfirmationView(), new MultiverseEditorView())
            .defaultConfig(config -> {
                config.cancelOnClick();
                config.cancelOnDrag();
                config.cancelOnDrop();
                config.cancelOnPickup();
                config.interactionDelay(Duration.ofMillis(100));
            })
            .disableMetrics();
        frame = registerViews(frame);
        this.viewFrame = frame.register();

        LOGGER.info("Views initialized");
    }

    /**
     * Loads all worlds from the database.
     *
     * @return a CompletableFuture that completes when all worlds are loaded
     */
    private CompletableFuture<Void> loadWorlds() {
        return worldFactory.loadAllWorlds()
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Error loading worlds", throwable);
                return null;
            });
    }

    /**
     * Registers the MultiverseAdapter as a Bukkit service.
     * <p>
     * This allows external plugins to access the multiverse API via:
     * <pre>{@code
     * RegisteredServiceProvider<IMultiverseAdapter> provider =
     *     Bukkit.getServicesManager().getRegistration(IMultiverseAdapter.class);
     * if (provider != null) {
     *     IMultiverseAdapter adapter = provider.getProvider();
     * }
     * }</pre>
     * </p>
     */
    private void registerBukkitService() {
        this.multiverseAdapter = new MultiverseAdapter(plugin, worldRepository, worldFactory);

        Bukkit.getServicesManager().register(
            IMultiverseAdapter.class,
            multiverseAdapter,
            plugin,
            ServicePriority.Normal
        );

        LOGGER.info("MultiverseAdapter registered as Bukkit service");
    }

    // ==================== Getters ====================

    /**
     * Gets the world repository.
     *
     * @return the world repository, or null if not initialized
     */
    public @Nullable MVWorldRepository getWorldRepository() {
        return worldRepository;
    }

    /**
     * Gets the multiverse service.
     *
     * @return the multiverse service
     */
    public @NotNull IMultiverseService getMultiverseService() {
        return multiverseService;
    }

    /**
     * Gets the multiverse adapter.
     *
     * @return the multiverse adapter
     */
    public @Nullable IMultiverseAdapter getMultiverseAdapter() {
        return multiverseAdapter;
    }

    /**
     * Gets the world factory.
     *
     * @return the world factory
     */
    public @Nullable WorldFactory getWorldFactory() {
        return worldFactory;
    }

    /**
     * Gets the view frame.
     *
     * @return the view frame
     */
    public @Nullable ViewFrame getViewFrame() {
        return viewFrame;
    }

    /**
     * Checks if the plugin is currently disabling.
     *
     * @return true if the plugin is disabling
     */
    public boolean isDisabling() {
        return disabling;
    }

    /**
     * Checks if the post-enable phase has completed.
     *
     * @return true if post-enable is complete
     */
    public boolean isPostEnableCompleted() {
        return postEnableCompleted;
    }
}
