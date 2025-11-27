package com.raindropcentral.rdq;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.core.api.RCoreAdapter;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.manager.perk.PerkInitializationManager;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.runtime.CooldownService;
import com.raindropcentral.rdq.perk.runtime.DefaultPerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.runtime.PerkTypeRegistry;
import com.raindropcentral.rdq.service.rank.RankPathService;
import com.raindropcentral.rdq.utility.perk.PerkSystemFactory;
import com.raindropcentral.rdq.utility.rank.RankSystemFactory;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.perks.*;
import com.raindropcentral.rdq.view.rank.view.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class RDQ {

    private static final Logger LOGGER = CentralLogger.getLogger(RDQ.class);

    private final JavaPlugin plugin;
    private final String detectedEdition;
    private final ExecutorService executor = createExecutor();
    private final RPlatform platform;

    private volatile CompletableFuture<Void> enableFuture;
    private boolean isDisabling;
    private boolean postEnableCompleted;

    private RDQManager manager;
    private ViewFrame viewFrame;
    private RankSystemFactory rankSystemFactory;
    private PerkSystemFactory perkSystemFactory;
    private RankPathService rankPathService;
    private com.raindropcentral.rdq.bounty.service.BountyService bountyService;
    private com.raindropcentral.rdq.config.bounty.BountyConfig bountyConfig;

    private RDQPlayerRepository playerRepository;
    private RPlayerRankPathRepository playerRankPathRepository;
    private RPlayerRankRepository playerRankRepository;
    private RPlayerPerkRepository playerPerkRepository;
    private RBountyRepository bountyRepository;
    private BountyHunterStatsRepository bountyHunterStatsRepository;
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

    public RDQ(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.detectedEdition = edition;
        this.platform = new RPlatform(plugin);
        CentralLogger.initialize(plugin);
    }

    public void onLoad() {
        try {
            LOGGER.info("Loading RDQ " + detectedEdition + " Edition");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load RDQ", e);
        }
    }

    public void onEnable() {
        if (enableFuture != null && !enableFuture.isDone()) {
            LOGGER.warning("Enable sequence already in progress");
            return;
        }

        enableFuture = performCoreEnableAsync()
                .thenCompose(v -> runSync(() -> {
                    try {
                        manager = initializeManager(this);
                        registerServices();
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
        isDisabling = true;
        if (enableFuture != null && !enableFuture.isDone()) {
            enableFuture.cancel(true);
        }
        if (perkInitializationManager != null) {
            perkInitializationManager.shutdown();
        }
        if (manager != null) {
            manager.shutdown();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @NotNull
    protected abstract String getStartupMessage();

    protected abstract int getMetricsId();

    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    @NotNull
    protected abstract RDQManager initializeManager(@NotNull RDQ rdq);
    
    /**
     * Sets the BountyService instance. This should be called by edition-specific implementations
     * during initialization.
     *
     * @param bountyService the bounty service instance
     */
    protected void setBountyService(@NotNull com.raindropcentral.rdq.bounty.service.BountyService bountyService) {
        this.bountyService = bountyService;
    }

    private CompletableFuture<Void> performCoreEnableAsync() {
        return platform.initialize()
                .thenCompose(v -> runSync(() -> {
                    initializeRepositories();
                }))
                .thenCompose(v -> CompletableFuture.allOf(
                        rankSystemFactory.initializeAsync().exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "Rank system initialization failed", ex);
                            return null;
                        }),
                        perkSystemFactory.initializeAsync().exceptionally(ex -> {
                            LOGGER.log(Level.SEVERE, "Perk system initialization failed", ex);
                            return null;
                        })
                ))
                .thenCompose(v -> loadRankSystemAsync())
                .thenRun(this::loadPerksIntoRegistryAsync);
    }

    private void registerServices() {
        new ServiceRegistry().register("net.luckperms.api.LuckPerms", "LuckPerms")
                .optional()
                .maxAttempts(20)
                .retryDelay(500)
                .onSuccess(lp -> {
                    luckPermsService = new LuckPermsService(platform);
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
                        rCoreAdapter = (RCoreAdapter) rc;
                        LOGGER.info("RCoreAdapter detected; statistics repository bound");
                    } catch (Throwable t) {
                        LOGGER.log(Level.WARNING, "RCoreAdapter detected but failed to bind repositories: " + t.getMessage(), t);
                    }
                })
                .onFailure(() -> LOGGER.info("RCoreAdapter not present; core statistics integration disabled"))
                .load();
    }

    private void performPostEnableSync() {
        if (postEnableCompleted) {
            plugin.getLogger().warning("Post-enable called more than once.");
            return;
        }

        var commandFactory = new CommandFactory(plugin, this);
        commandFactory.registerAllCommandsAndListeners();

        rankPathService = new RankPathService(this);
        initializeViews();
        startBountyExpirationTask();
        platform.initializeMetrics(getMetricsId());

        plugin.getLogger().info(getStartupMessage());
        plugin.getLogger().info("RDQ " + detectedEdition + " Edition enabled successfully!");
        postEnableCompleted = true;
    }

    private void startBountyExpirationTask() {
        new com.raindropcentral.rdq.task.BountyExpirationTask().runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60 * 5);
        LOGGER.info("Bounty expiration task started (checks every 5 minutes)");
    }

    private void initializeRepositories() {
        var emf = platform.getEntityManagerFactory();
        if (emf == null) {
            plugin.getLogger().warning("EntityManagerFactory not initialized");
            onDisable();
            return;
        }

        // Initialize configuration
        bountyConfig = new com.raindropcentral.rdq.config.bounty.BountyConfig(this);

        playerRepository = new RDQPlayerRepository(executor, emf);
        bountyRepository = new RBountyRepository(executor, emf);
        bountyHunterStatsRepository = new BountyHunterStatsRepository(executor, emf);
        rankRepository = new RRankRepository(executor, emf);
        perkRepository = new RPerkRepository(executor, emf);
        rankTreeRepository = new RRankTreeRepository(executor, emf);
        requirementRepository = new RRequirementRepository(executor, emf);
        playerPerkRequirementProgressRepository = new PlayerPerkRequirementProgressRepository(executor, emf);
        playerRankUpgradeProgressRepository = new RPlayerRankUpgradeProgressRepository(executor, emf);
        playerRankPathRepository = new RPlayerRankPathRepository(executor, emf);
        playerRankRepository = new RPlayerRankRepository(executor, emf);
        playerPerkRepository = new RPlayerPerkRepository(executor, emf);

        rankSystemFactory = new RankSystemFactory(this);
        perkSystemFactory = new PerkSystemFactory(this);

        initializePerkServices();
    }

    private void initializePerkServices() {
        perkInitializationManager = new PerkInitializationManager(this);
        perkInitializationManager.initializePerkServices();
        perkInitializationManager.registerPerkServices();
    }

    private void loadPerksIntoRegistryAsync() {
        executor.submit(() -> {
            try {
                var all = perkRepository.findListByAttributes(Map.of());
                var byId = all.stream()
                    .filter(perk -> perk != null && !perk.getIdentifier().isBlank())
                    .collect(Collectors.toMap(
                        perk -> perk.getIdentifier().trim().toLowerCase(),
                        perk -> perk,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                    ));
                
                var registry = perkInitializationManager.getPerkRegistry();
                if (registry instanceof DefaultPerkRegistry factory) {
                    factory.getAllPerkRuntimes().forEach(runtime -> factory.unregisterPerkRuntime(runtime.getId()));
                    
                    var loaded = byId.values().stream()
                        .mapToInt(perk -> {
                            try {
                                factory.buildPerkRuntime(perk, perk.getPerkSection());
                                return 1;
                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING, "Failed to build runtime for perk " + perk.getIdentifier(), ex);
                                return 0;
                            }
                        })
                        .sum();
                    
                    LOGGER.info("Loaded " + loaded + " perks from database into runtime registry");
                } else {
                    LOGGER.warning("Perk registry is not a DefaultPerkRegistry; skipping runtime reload");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to reload perks into registry", ex);
            }
        });
    }

    private CompletableFuture<Void> loadRankSystemAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Loading rank system...");
                rankSystemFactory.loadAndPersistRankSystem();
                LOGGER.info("Rank system loaded successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load rank system", e);
            }
        }, executor);
    }

    public CompletableFuture<Void> reloadRankSystem() {
        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Reloading rank system...");
                rankSystemFactory.loadAndPersistRankSystem();
                LOGGER.info("Rank system reloaded successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reload rank system", e);
            }
        }, executor);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        ViewFrame frame = ViewFrame
                .create(plugin)
                .with(
                        // New bounty system views
                        new BountyMainView(),
                        new BountyCreationView(),
                        new BountyListView(),
                        new BountyDetailView(),
                        new BountyLeaderboardView(),
                        new MyBountiesView(),
                        new BountyRewardView(),
                        new BountyCurrencySelectionView(),
                        // Utility views
                        new PaginatedPlayerView(),
                        // Rank views
                        new RankMainView(),
                        new RankPathOverview(),
                        new RankRequirementDetailView(),
                        new RankTreeOverviewView(),
                        new RankPathRankRequirementOverview(),
                        // Perk views
                        new PerkListViewFrame(),
                        new PerkDetailView(),
                        new PerkMainView(),
                        new PerkRequirementView(),
                        new PerkAdminView(),
                        new PerkUnlockView()
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
    public BountyHunterStatsRepository getBountyHunterStatsRepository() {
        return this.bountyHunterStatsRepository;
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

    /**
     * Returns the bound RCoreAdapter if present. This adapter provides access to core repositories
     * such as player statistics that live outside RDQ.
     *
     * @return the adapter instance or null if not available
     */
    public @Nullable RCoreAdapter getRCoreAdapter() {
        return this.rCoreAdapter;
    }

    public String getDetectedEdition() {
        return detectedEdition;
    }

    public boolean isPostEnableCompleted() {
        return postEnableCompleted;
    }

    public PerkSystemFactory getPerkSystemFactory() {
        return perkSystemFactory;
    }

    @NotNull
    public com.raindropcentral.rdq.bounty.service.BountyService getBountyService() {
        return bountyService;
    }

    @NotNull
    public com.raindropcentral.rdq.config.bounty.BountyConfig getBountyConfig() {
        return bountyConfig;
    }

    public @Nullable RCoreAdapter getrCoreAdapter() {
        return rCoreAdapter;
    }
}