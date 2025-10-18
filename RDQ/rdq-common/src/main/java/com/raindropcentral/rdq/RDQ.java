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

public abstract class RDQ {

    private final JavaPlugin plugin;
    private final RDQManager manager;
    private String detectedEdition;
    private final ExecutorService executor = createExecutor();
    private volatile CompletableFuture<Void> enableFuture;
    private RPlatform platform;
    private ViewFrame viewFrame;
    private BountyManager bountyManager;
    private RankSystemFactory rankSystemFactory;
    private CommandFactory commandFactory;
    private RDQPlayerRepository playerRepository;
    private RPlayerRankPathRepository playerRankPathRepository;
    private RPlayerRankRepository playerRankRepository;
    private RPlayerPerkRepository playerPerkRepository;
    private RBountyRepository bountyRepository;
    private RRankRepository rankRepository;
    private RPerkRepository perkRepository;
    private RPlayerRankUpgradeProgressRepository playerRankUpgradeProgressRepository;
    private RRankTreeRepository rankTreeRepository;
    private RRequirementRepository requirementRepository;
    private boolean isDisabling;

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

    public void onLoad() {
        try {
            plugin.getLogger().info("Loading RDQ " + detectedEdition + " Edition");
        } catch (final Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ", exception);
        }
    }

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

    public void onDisable() {
        this.isDisabling = true;
        if (this.enableFuture != null && !this.enableFuture.isDone()) {
            this.enableFuture.cancel(true);
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

    private void initializeComponents() {
        // Initialize BountyManager BEFORE registering listeners so they can access it
        this.bountyManager = new BountyManager(this);
        
        // Now register commands and listeners - they can safely use getBountyManager()
        this.commandFactory = new CommandFactory(plugin, this);
        this.commandFactory.registerAllCommandsAndListeners();
    }

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

    private @NotNull CompletableFuture<Void> initializePlatformAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                this.platform.initialize();
            } catch (Throwable t) {
                throw new CompletionException(t);
            }
        }, this.executor);
    }

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

    private ExecutorService createExecutor() {
        try {
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (Throwable ignored) {
            return Executors.newFixedThreadPool(5);
        }
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
    public RPlatform getPlatform() {
        return this.platform;
    }

    @NotNull
    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    @NotNull
    public RDQManager getManager() {
        return this.manager;
    }

    @NotNull
    public BountyManager getBountyManager() {
        return this.bountyManager;
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
    public RRankTreeRepository getRankTreeRepository() {
        return this.rankTreeRepository;
    }

    @NotNull
    public RRequirementRepository getRequirementRepository() {
        return this.requirementRepository;
    }

    public boolean isDisabling() {
        return this.isDisabling;
    }

    public @Nullable CompletableFuture<Void> getEnableFuture() {
        return this.enableFuture;
    }

    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @NotNull
    public java.io.File getDataFolder() {
        return plugin.getDataFolder();
    }

    @NotNull
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @NotNull
    public org.bukkit.Server getServer() {
        return plugin.getServer();
    }

    @NotNull
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void saveConfig() {
        plugin.saveConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }
}