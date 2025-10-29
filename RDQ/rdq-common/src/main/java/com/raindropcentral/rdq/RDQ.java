package com.raindropcentral.rdq;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.manager.perk.PerkInitializationManager;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.runtime.CooldownService;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkTypeRegistry;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rdq.utility.rank.RankSystemFactory;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.perks.PerkListViewFrame;
import com.raindropcentral.rdq.view.rank.view.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
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
 * The core abstract class for RaindropQuests.
 * <p>
 * This class manages the entire plugin lifecycle, including a robust asynchronous
 * startup sequence, dependency management, and access to core components like
 * repositories, managers, and the platform API.
 * </p>
 *
 * @author JExcellence
 * @version 3.2.0
 */
public abstract class RDQ {

    private final Logger LOGGER = CentralLogger.getLogger(RDQ.class);

    private final JavaPlugin plugin;
    private final String detectedEdition;
    private final ExecutorService executor = createExecutor();
    private final RPlatform platform;

    private volatile CompletableFuture<Void> enableFuture;
    private boolean isDisabling;
    private boolean postEnableCompleted = false;

    private RDQManager manager;
    private ViewFrame viewFrame;
    private RankSystemFactory rankSystemFactory;
    private RankPathService rankPathService;

    // RDQ repositories
    private RDQPlayerRepository playerRepository;
    private RPlayerRankPathRepository playerRankPathRepository;
    private RPlayerRankRepository playerRankRepository;
    private RPlayerPerkRepository playerPerkRepository;
    private RBountyRepository bountyRepository;
    private RRankRepository rankRepository;
    private RPerkRepository perkRepository;
    private PlayerPerkRequirementProgressRepository playerPerkRequirementProgressRepository;
    private RPlayerRankUpgradeProgressRepository playerRankUpgradeProgressRepository;
    private RRankTreeRepository rankTreeRepository;
    private RRequirementRepository requirementRepository;

    // Perk initialization manager
    private PerkInitializationManager perkInitializationManager;

    private @Nullable LuckPermsService luckPermsService;
    private @Nullable RCoreAdapter rCoreAdapter;

    public RDQ(final @NotNull JavaPlugin plugin, final @NotNull String edition) {
        this.plugin = plugin;
        this.detectedEdition = edition;
        this.platform = new RPlatform(plugin);
        CentralLogger.initialize(plugin);
    }

    public void onLoad() {
        try {
            LOGGER.info("Loading RDQ " + detectedEdition + " Edition");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDQ", exception);
        }
    }

    /**
     * Orchestrates the entire asynchronous startup sequence for the plugin.
     * This method is the single entry point for enabling the plugin.
     */
    public void onEnable() {
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            LOGGER.warning("Enable sequence already in progress");
            return;
        }

        this.enableFuture = performCoreEnableAsync()
                .thenCompose(v -> runSync(() -> {
                    try {
                        this.manager = initializeManager(this);
                        this.registerServices();
                        performPostEnableSync();
                    } catch (Throwable t) {
                        throw new CompletionException(t);
                    }
                }))
                .exceptionally(ex -> {
                    runSync(() -> {
                        LOGGER.log(Level.SEVERE, "Failed to enable RDQ (" + Optional.ofNullable(detectedEdition).orElse("?") + ")", ex);
                        plugin.getServer().getPluginManager().disablePlugin(plugin);
                    });
                    return null;
                });
    }

    public void onDisable() {
        this.isDisabling = true;
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            this.enableFuture.cancel(true);
        }
        if (this.perkInitializationManager != null) {
            this.perkInitializationManager.shutdown();
        }
        if (this.manager != null) {
            this.manager.shutdown();
        }
        if (this.executor != null && !this.executor.isShutdown()) {
            this.executor.shutdown();
        }
    }

    @NotNull
    protected abstract String getStartupMessage();

    protected abstract int getMetricsId();

    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    /**
     * Implemented by concrete classes (Free/Premium) to create and return
     * their specific RDQManager instance. This is called after core systems are ready.
     *
     * @param rdq the RDQ instance
     * @return The initialized RDQManager.
     */
    @NotNull
    protected abstract RDQManager initializeManager(@NotNull RDQ rdq);

    /**
     * Performs core asynchronous setup (Platform, Database, Repositories, Ranks).
     * This must complete before any managers or services can be initialized.
     */
    private CompletableFuture<Void> performCoreEnableAsync() {
        return this.platform.initialize()
                .thenCompose(v -> runSync(() -> {
                    initializeRepositories();
                    this.rankSystemFactory = new RankSystemFactory(this);
                }))
                .thenCompose(v -> this.rankSystemFactory.initializeAsync());
    }

    private void registerServices() {
        new ServiceRegistry().register("net.luckperms.api.LuckPerms", "LuckPerms")
                .optional()
                .maxAttempts(20)
                .retryDelay(500)
                .onSuccess(lp -> {
                    this.luckPermsService = new LuckPermsService(platform);
                    LOGGER.info("LuckPerms detected; LP wrapper registered and bound");
                })
                .onFailure(() -> LOGGER.info("LuckPerms not present; LP wrapper not registered"))
                .load();

        new ServiceRegistry().register("com.raindropcentral.core.adapter.RCoreAdapter", "RCore")
                .optional()
                .maxAttempts(20)
                .retryDelay(500)
                .onSuccess(rc -> {
                    try {
                        this.rCoreAdapter = (RCoreAdapter) rc;
                        LOGGER.info("RCoreAdapter detected; statistics repository bound");
                    } catch (Throwable t) {
                        LOGGER.log(Level.WARNING, "RCoreAdapter detected but failed to bind repositories: " + t.getMessage(), t);
                    }
                })
                .onFailure(() -> LOGGER.info("RCoreAdapter not present; core statistics integration disabled"))
                .load();
    }

    /**
     * Performs the final synchronous setup after all async tasks are complete.
     * This initializes and registers all user-facing components like commands and views.
     */
    private void performPostEnableSync() {
        if (postEnableCompleted) {
            plugin.getLogger().warning("Post-enable called more than once.");
            return;
        }

        CommandFactory commandFactory = new CommandFactory(plugin, this);
        commandFactory.registerAllCommandsAndListeners();

        this.rankPathService = new RankPathService(this);

        initializeViews();

        this.platform.initializeMetrics(getMetricsId());

        plugin.getLogger().info(getStartupMessage());
        plugin.getLogger().info("RDQ " + detectedEdition + " Edition enabled successfully!");
        postEnableCompleted = true;
    }

    private void initializeRepositories() {
        final EntityManagerFactory emf = this.platform.getEntityManagerFactory();

        if (emf == null) {
            plugin.getLogger().warning("EntityManagerFactory not initialized");
            this.onDisable();
            return;
        }

        this.playerRepository = new RDQPlayerRepository(this.executor, emf);
        this.bountyRepository = new RBountyRepository(this.executor, emf);
        this.rankRepository = new RRankRepository(this.executor, emf);
        this.perkRepository = new RPerkRepository(this.executor, emf);
        this.rankTreeRepository = new RRankTreeRepository(this.executor, emf);
        this.requirementRepository = new RRequirementRepository(this.executor, emf);
        this.playerPerkRequirementProgressRepository = new PlayerPerkRequirementProgressRepository(this.executor, emf);
        this.playerRankUpgradeProgressRepository = new RPlayerRankUpgradeProgressRepository(this.executor, emf);
        this.playerRankPathRepository = new RPlayerRankPathRepository(this.executor, emf);
        this.playerRankRepository = new RPlayerRankRepository(this.executor, emf);
        this.playerPerkRepository = new RPlayerPerkRepository(this.executor, emf);

        // Initialize perk services
        initializePerkServices();
    }

    private void initializePerkServices() {
        this.perkInitializationManager = new PerkInitializationManager(this);
        this.perkInitializationManager.initializePerkServices();
        this.perkInitializationManager.registerPerkServices();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        ViewFrame frame = ViewFrame
                .create(plugin)
                .with(
                        new BountyMainView(),
                        new BountyCreationView(),
                        new BountyOverviewView(),
                        new BountyRewardView(),
                        new BountyPlayerInfoView(),
                        new PaginatedPlayerView(),
                        new RankMainView(),
                        new RankPathOverview(),
                        new RankRequirementDetailView(),
                        new RankTreeOverviewView(),
                        new RankTreeOverviewView(),
                        new RankPathRankRequirementOverview(),
                        new PerkListViewFrame()


                )
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                    config.interactionDelay(Duration.ofMillis(100));
                });
        frame = registerViews(frame);
        this.viewFrame = frame.register();
    }

    private @NotNull CompletableFuture<Void> runSync(final @NotNull Runnable runnable) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        if (isDisabling) {
            future.cancel(false);
            return future;
        }
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

    private ExecutorService createExecutor() {
        try {
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (Throwable ignored) {
            return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
    }

    @NotNull
    public RDQManager getManager() {
        if (this.manager == null) {
            throw new IllegalStateException("RDQManager has not been initialized yet. It is only available after the enable sequence is complete.");
        }
        return this.manager;
    }

    @NotNull
    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    @Nullable
    public String getEdition() {
        return this.detectedEdition;
    }

    @NotNull
    public ExecutorService getExecutor() {
        return this.executor;
    }

    @NotNull
    public RankPathService getRankPathService() {
        return rankPathService;
    }

    @NotNull
    public RPlatform getPlatform() {
        return this.platform;
    }

    @NotNull
    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    @NotNull
    public RankSystemFactory getRankSystemFactory() {
        return this.rankSystemFactory;
    }

    @NotNull
    public RDQPlayerRepository getPlayerRepository() {
        return this.playerRepository;
    }

    @NotNull
    public RPlayerRankPathRepository getPlayerRankPathRepository() {
        return this.playerRankPathRepository;
    }

    @NotNull
    public RPlayerRankRepository getPlayerRankRepository() {
        return this.playerRankRepository;
    }

    @NotNull
    public RPlayerPerkRepository getPlayerPerkRepository() {
        return this.playerPerkRepository;
    }

    @NotNull
    public RBountyRepository getBountyRepository() {
        return this.bountyRepository;
    }

    @NotNull
    public RRankRepository getRankRepository() {
        return this.rankRepository;
    }

    @NotNull
    public RPerkRepository getPerkRepository() {
        return this.perkRepository;
    }

    @NotNull
    public RPlayerRankUpgradeProgressRepository getPlayerRankUpgradeProgressRepository() {
        return this.playerRankUpgradeProgressRepository;
    }

    @NotNull
    public PlayerPerkRequirementProgressRepository getPlayerPerkRequirementProgressRepository() {
        return this.playerPerkRequirementProgressRepository;
    }

    @NotNull
    public RRankTreeRepository getRankTreeRepository() {
        return this.rankTreeRepository;
    }

    @NotNull
    public RRequirementRepository getRequirementRepository() {
        return this.requirementRepository;
    }

    @NotNull
    public PerkTypeRegistry getPerkTypeRegistry() {
        return this.perkInitializationManager.getPerkTypeRegistry();
    }

    @NotNull
    public PerkRegistry getPerkRegistry() {
        return this.perkInitializationManager.getPerkRegistry();
    }

    @NotNull
    public CooldownService getCooldownService() {
        return this.perkInitializationManager.getCooldownService();
    }

    @NotNull
    public PerkEventBus getPerkEventBus() {
        return this.perkInitializationManager.getPerkEventBus();
    }

    @NotNull
    public PerkInitializationManager getPerkInitializationManager() {
        return this.perkInitializationManager;
    }

    public boolean isDisabling() {
        return this.isDisabling;
    }

    public @Nullable CompletableFuture<Void> getEnableFuture() {
        return this.enableFuture;
    }

    public @Nullable LuckPermsService getLuckPermsService() {
        return luckPermsService;
    }

    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    // RCoreAdapter integration getters

    /**
     * Returns the bound RCoreAdapter if present. This adapter provides access to core repositories
     * such as player statistics that live outside RDQ.
     *
     * @return the adapter instance or null if not available
     */
    public @Nullable RCoreAdapter getRCoreAdapter() {
        return this.rCoreAdapter;
    }
}