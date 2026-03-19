package de.jexcellence.home;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.home.config.HomeSystemConfig;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.database.repository.HomeRepository;
import de.jexcellence.home.factory.HomeFactory;
import de.jexcellence.home.service.IHomeService;
import de.jexcellence.home.view.DeleteHomeView;
import de.jexcellence.home.view.HomeOverviewView;
import de.jexcellence.home.view.SetHomeAnvilView;
import de.jexcellence.home.view.ViewRouter;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.hibernate.repository.RepositoryManager;
import lombok.Getter;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
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
 * Abstract base class for JExHome plugin following RDQ patterns.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Getter
public abstract class JExHome {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExHome");

    private final JavaPlugin plugin;
    private final String edition;
    private final ExecutorService executor;
    private final RPlatform platform;

    private volatile CompletableFuture<Void> onEnableFuture;

    private boolean disabling;
    private boolean postEnableCompleted;

    private ViewFrame viewFrame;
    private ViewRouter viewRouter;
    private HomeSystemConfig homeConfig;
    private IHomeService homeService;

    @InjectRepository
    private HomeRepository homeRepository;

    public JExHome(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
        this.platform = new RPlatform(plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    public void onLoad() {
        LOGGER.info("Loading JExHome " + edition + " Edition");
    }

    public void onEnable() {
        if (onEnableFuture != null && !onEnableFuture.isDone()) {
            LOGGER.log(Level.WARNING, "Enable sequence already in progress");
            return;
        }

        onEnableFuture = platform.initialize()
            .thenCompose(v -> {
                initializeConfig();
                initializeRepositories();
                platform.initializeGeyser();
                return CompletableFuture.completedFuture(null);
            })
            .thenRun(() -> {
                initializeService();
                initializeFactory();
                initializeComponents();
                initializeViews();
                platform.initializeMetrics(getMetricsId());

                LOGGER.info(getStartupMessage());
                LOGGER.info("JExHome (" + edition + ") Edition enabled successfully!");
                postEnableCompleted = true;
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to initialize JExHome", throwable);
                return null;
            });
    }

    public void onDisable() {
        disabling = true;
        HomeFactory.reset();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        LOGGER.info("JExHome (" + edition + ") Edition disabled successfully!");
    }

    @NotNull
    protected abstract String getStartupMessage();

    protected abstract int getMetricsId();

    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    /**
     * Creates the edition-specific home service.
     *
     * @return the home service implementation
     */
    @NotNull
    protected abstract IHomeService createHomeService();

    private void initializeConfig() {
        try {
            var cfgManager = new ConfigManager(this.getPlugin(), "configs");
            var cfgKeeper = new ConfigKeeper<>(cfgManager, "home-system.yml", HomeSystemConfig.class);
            this.homeConfig = cfgKeeper.rootSection;
            LOGGER.log(Level.INFO, "Home configuration initialized");
        } catch (Exception exception) {
            this.homeConfig = new HomeSystemConfig(new EvaluationEnvironmentBuilder());
            LOGGER.log(Level.INFO, "Home configuration initialized with defaults", exception);
        }
    }

    private void initializeRepositories() {
        var emf = platform.getEntityManagerFactory();
        if (emf == null) {
            LOGGER.warning("EntityManagerFactory not initialized");
            return;
        }

        RepositoryManager.initialize(executor, emf);
        var repositoryManager = RepositoryManager.getInstance();
        repositoryManager.register(HomeRepository.class, Home.class, Home::getId);
        repositoryManager.injectInto(this);
    }

    private void initializeService() {
        this.homeService = createHomeService();
        LOGGER.info("Home service initialized: " + (homeService.isPremium() ? "Premium" : "Free"));
    }

    private void initializeFactory() {
        HomeFactory.initialize(homeService, homeConfig, plugin);
        LOGGER.info("HomeFactory initialized");
    }

    private void initializeComponents() {
        var commandFactory = new CommandFactory(plugin, this);
        commandFactory.registerAllCommandsAndListeners();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        var frame = ViewFrame
            .create(plugin)
            .install(AnvilInputFeature.AnvilInput)
            .with(new ConfirmationView(), new HomeOverviewView(), new SetHomeAnvilView(), new DeleteHomeView())
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
        this.viewRouter = new ViewRouter(this, this.viewFrame);
    }

    public @Nullable HomeRepository getHomeRepository() {
        return homeRepository;
    }

    public @NotNull HomeSystemConfig getHomeConfig() {
        if (homeConfig == null) {
            homeConfig = new HomeSystemConfig(new EvaluationEnvironmentBuilder());
        }
        return homeConfig;
    }

    public @NotNull IHomeService getHomeService() {
        return homeService;
    }

    public @NotNull ViewRouter getViewRouter() {
        return viewRouter;
    }
}
