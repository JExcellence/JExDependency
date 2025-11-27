package com.raindropcentral.rdq;

import com.raindropcentral.rdq.api.*;
import com.raindropcentral.rdq.bounty.announcement.BountyAnnouncementService;
import com.raindropcentral.rdq.bounty.config.BountyConfig;
import com.raindropcentral.rdq.bounty.config.BountyConfigLoader;
import com.raindropcentral.rdq.bounty.listener.BountyDeathListener;
import com.raindropcentral.rdq.bounty.listener.DamageTracker;
import com.raindropcentral.rdq.bounty.repository.BountyRepository;
import com.raindropcentral.rdq.bounty.repository.HunterStatsRepository;
import com.raindropcentral.rdq.bounty.view.*;
import com.raindropcentral.rdq.perk.effect.DeathPreventionHandler;
import com.raindropcentral.rdq.perk.effect.ExperienceMultiplierHandler;
import com.raindropcentral.rdq.perk.effect.FlightHandler;
import com.raindropcentral.rdq.perk.listener.*;
import com.raindropcentral.rdq.perk.repository.PerkRepository;
import com.raindropcentral.rdq.perk.repository.PlayerPerkRepository;
import com.raindropcentral.rdq.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq.perk.service.DefaultFreePerkService;
import com.raindropcentral.rdq.perk.service.DefaultPremiumPerkService;
import com.raindropcentral.rdq.perk.service.PerkRequirementChecker;
import com.raindropcentral.rdq.perk.view.*;
import com.raindropcentral.rdq.player.PlayerDataService;
import com.raindropcentral.rdq.rank.repository.PlayerRankPathRepository;
import com.raindropcentral.rdq.rank.repository.PlayerRankRepository;
import com.raindropcentral.rdq.rank.repository.RankRepository;
import com.raindropcentral.rdq.rank.repository.RankTreeRepository;
import com.raindropcentral.rdq.rank.service.DefaultFreeRankService;
import com.raindropcentral.rdq.rank.service.DefaultPremiumRankService;
import com.raindropcentral.rdq.rank.service.RankRequirementChecker;
import com.raindropcentral.rdq.rank.view.RankMainView;
import com.raindropcentral.rdq.rank.view.RankProgressView;
import com.raindropcentral.rdq.shared.AsyncExecutor;
import com.raindropcentral.rdq.shared.CacheManager;
import com.raindropcentral.rdq.shared.edition.EditionFeatures;
import com.raindropcentral.rdq.shared.edition.FreeEditionFeatures;
import com.raindropcentral.rdq.shared.translation.RDQTranslationService;
import com.raindropcentral.rdq.shared.translation.RDQTranslationServiceBuilder;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.api.LocaleResolver;
import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.MissingKeyTracker;
import de.jexcellence.jextranslate.api.TranslationRepository;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core initialization and service management for RDQ.
 *
 * <p>RDQCore handles plugin lifecycle, service registration, and provides
 * access to all RDQ subsystems. It is instantiated by the edition-specific
 * entry points ({@code RDQFree} or {@code RDQPremium}).
 *
 * <p>Initialization is asynchronous and follows this sequence:
 * <ol>
 *   <li>Platform initialization (RPlatform)</li>
 *   <li>Translation service setup</li>
 *   <li>View frame registration</li>
 *   <li>Event listener registration</li>
 *   <li>Command registration</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * var core = new RDQCore(plugin, "Free", new FreeEditionFeatures());
 * core.initialize()
 *     .thenRun(() -> {
 *         var rankService = core.createFreeRankService();
 *         core.registerRankService(rankService);
 *     });
 * }</pre>
 *
 * @see com.raindropcentral.rdq.shared.edition.EditionFeatures
 */
public final class RDQCore {

    private static final Logger LOGGER = CentralLogger.getLogger(RDQCore.class);
    private static final Locale DEFAULT_LOCALE = Locale.US;

    private final JavaPlugin plugin;
    private final String edition;
    private final EditionFeatures editionFeatures;
    private final RPlatform platform;
    private final AsyncExecutor executor;
    private final CacheManager cacheManager;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private @Nullable ViewFrame viewFrame;
    private @Nullable RDQTranslationService translationService;
    private @Nullable PlayerDataService playerDataService;
    private @Nullable RankService rankService;
    private @Nullable BountyService bountyService;
    private @Nullable PerkService perkService;

    private @Nullable RankTreeRepository rankTreeRepository;
    private @Nullable RankRepository rankRepository;
    private @Nullable PlayerRankRepository playerRankRepository;
    private @Nullable PlayerRankPathRepository playerRankPathRepository;
    private @Nullable PerkRepository perkRepository;
    private @Nullable PlayerPerkRepository playerPerkRepository;
    private @Nullable BountyRepository bountyRepository;
    private @Nullable HunterStatsRepository hunterStatsRepository;

    private @Nullable PerkRegistry perkRegistry;
    private @Nullable PerkEventBus perkEventBus;
    private @Nullable DamageTracker damageTracker;
    private @Nullable BountyConfig bountyConfig;
    private @Nullable BountyAnnouncementService bountyAnnouncementService;

    private @Nullable com.raindropcentral.rdq.shared.edition.FeatureGate featureGate;

    private @Nullable CompletableFuture<Void> initializationFuture;

    public RDQCore(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this(plugin, edition, new FreeEditionFeatures());
    }

    public RDQCore(@NotNull JavaPlugin plugin, @NotNull String edition, @NotNull EditionFeatures editionFeatures) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.editionFeatures = Objects.requireNonNull(editionFeatures, "editionFeatures");
        this.platform = new RPlatform(plugin);
        this.executor = new AsyncExecutor();
        this.cacheManager = new CacheManager();
        CentralLogger.initialize(plugin);
    }

    public CompletableFuture<Void> initialize() {
        if (initialized.get()) {
            LOGGER.warning("RDQCore already initialized");
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("Initializing RDQ " + edition + " Edition v6.0.0");

        initializationFuture = platform.initialize()
            .thenCompose(v -> initializeTranslations())
            .thenCompose(v -> initializeRepositories())
            .thenCompose(v -> initializeSharedComponents())
            .thenCompose(v -> initializeViews())
            .thenCompose(v -> registerListeners())
            .thenCompose(v -> registerCommands())
            .thenRun(() -> {
                initialized.set(true);
                LOGGER.info("RDQ " + edition + " Edition initialized successfully!");
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to initialize RDQ", ex);
                return null;
            });

        return initializationFuture;
    }

    private CompletableFuture<Void> initializeRepositories() {
        return executor.runAsync(() -> {
            LOGGER.info("Initializing repositories...");

            var entityManagerFactory = platform.getEntityManagerFactory();
            if (entityManagerFactory == null) {
                LOGGER.warning("EntityManagerFactory not available - database operations will fail");
            }

            rankTreeRepository = new RankTreeRepository();
            rankRepository = new RankRepository();
            playerRankRepository = new PlayerRankRepository(executor.getExecutor(), entityManagerFactory);
            playerRankPathRepository = new PlayerRankPathRepository(executor.getExecutor(), entityManagerFactory);

            perkRepository = new PerkRepository();
            playerPerkRepository = new PlayerPerkRepository(executor.getExecutor(), entityManagerFactory);

            bountyRepository = new BountyRepository(executor.getExecutor(), entityManagerFactory);
            hunterStatsRepository = new HunterStatsRepository(executor.getExecutor(), entityManagerFactory);

            LOGGER.info("All repositories initialized");
            
            loadRankSystem();
        });
    }

    private void loadRankSystem() {
        try {
            LOGGER.info("Loading rank system...");
            
            var pathsDirectory = new java.io.File(plugin.getDataFolder(), "rank/paths");
            
            // Copy default rank files from resources if directory doesn't exist
            if (!pathsDirectory.exists()) {
                copyDefaultRankFiles(pathsDirectory);
            }
            
            var rankTreeLoader = new com.raindropcentral.rdq.rank.config.RankTreeLoader(
                pathsDirectory,
                rankTreeRepository,
                rankRepository
            );
            
            rankTreeLoader.load();
            
            LOGGER.info("Rank system loaded successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load rank system", e);
        }
    }

    public CompletableFuture<Void> reloadRankSystem() {
        return executor.runAsync(() -> {
            try {
                LOGGER.info("Reloading rank system...");
                
                var pathsDirectory = new java.io.File(plugin.getDataFolder(), "rank/paths");
                
                // Copy default rank files from resources if directory doesn't exist
                if (!pathsDirectory.exists()) {
                    copyDefaultRankFiles(pathsDirectory);
                }
                
                var rankTreeLoader = new com.raindropcentral.rdq.rank.config.RankTreeLoader(
                    pathsDirectory,
                    rankTreeRepository,
                    rankRepository
                );
                
                rankTreeLoader.reload();
                
                LOGGER.info("Rank system reloaded successfully");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reload rank system", e);
                throw new RuntimeException("Rank system reload failed", e);
            }
        });
    }

    private void copyDefaultRankFiles(java.io.File pathsDirectory) {
        try {
            pathsDirectory.mkdirs();
            
            // Copy rank-system.yml
            var rankSystemFile = new java.io.File(plugin.getDataFolder(), "rank/rank-system.yml");
            if (!rankSystemFile.exists()) {
                plugin.saveResource("rank/rank-system.yml", false);
                LOGGER.info("Copied default rank-system.yml");
            }
            
            // Copy rank path files
            String[] defaultPaths = {"warrior.yml", "mage.yml", "rogue.yml", "cleric.yml", "ranger.yml", "merchant.yml"};
            for (String pathFile : defaultPaths) {
                var targetFile = new java.io.File(pathsDirectory, pathFile);
                if (!targetFile.exists()) {
                    plugin.saveResource("rank/paths/" + pathFile, false);
                    LOGGER.info("Copied default rank path: " + pathFile);
                }
            }
            
            LOGGER.info("Default rank files copied successfully");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to copy default rank files", e);
        }
    }

    private CompletableFuture<Void> initializeSharedComponents() {
        return executor.runSync(plugin, () -> {
            LOGGER.info("Initializing shared components...");

            perkRegistry = new PerkRegistry(plugin);
            perkEventBus = new PerkEventBus();
            damageTracker = new DamageTracker();
            
            // Load bounty configuration from file
            var bountyConfigLoader = new BountyConfigLoader(plugin);
            bountyConfig = bountyConfigLoader.loadConfig();
            LOGGER.info("Bounty config loaded - selfTargetAllowed: " + bountyConfig.selfTargetAllowed());
            
            bountyAnnouncementService = new BountyAnnouncementService(bountyConfig);

            if (perkRepository != null) {
                var perks = perkRepository.findEnabled();
                perkRegistry.registerAll(perks);
                LOGGER.info("Registered " + perks.size() + " perks in PerkRegistry");
            }

            LOGGER.info("Shared components initialized");
        });
    }

    private CompletableFuture<Void> initializeTranslations() {
        return executor.runAsync(() -> {
            LOGGER.info("Initializing translations...");

            translationService = RDQTranslationServiceBuilder.create(plugin)
                .defaultLocale(DEFAULT_LOCALE)
                .build();

            LOGGER.info("Translations initialized with " + translationService.getAvailableLocales().size() + " locales");
        });
    }

    private CompletableFuture<Void> initializeViews() {
        return executor.runSync(plugin, () -> {
            LOGGER.info("ViewFrame will be initialized after services are registered...");
        });
    }

    /**
     * Registers all views and initializes the ViewFrame. Call this after services are registered.
     */
    public void registerServiceDependentViews() {
        executor.runSync(plugin, () -> {
            LOGGER.info("Initializing ViewFrame with all views...");

            var frameBuilder = ViewFrame.create(plugin)
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                });

            // Register perk views (don't require services in constructor)
            frameBuilder.with(new PerkMainView());
            frameBuilder.with(new PerkListView());
            frameBuilder.with(new PerkDetailView());
            frameBuilder.with(new PerkAdminView());
            frameBuilder.with(new PerkUnlockView());
            LOGGER.info("Registered perk views");

            // Register rank views if service is available
            if (rankService != null) {
                frameBuilder.with(new RankMainView(rankService));
                frameBuilder.with(new RankProgressView(rankService));
                LOGGER.info("Registered rank views (main, progress)");
            } else {
                LOGGER.warning("RankService not available - rank views not registered");
            }

            // Register bounty views (they use state for service access)
            frameBuilder.with(new BountyMainView());
            frameBuilder.with(new BountyListView());
            frameBuilder.with(new BountyLeaderboardView());
            frameBuilder.with(new MyBountiesView());
            frameBuilder.with(new BountyCreationView());
            frameBuilder.with(new BountyRewardView());
            LOGGER.info("Registered bounty views");

            // Register utility views from RPlatform
            frameBuilder.with(new com.raindropcentral.rplatform.view.PaginatedPlayerView());
            LOGGER.info("Registered utility views");

            // Now register the ViewFrame with all views
            viewFrame = frameBuilder.register();
            LOGGER.info("ViewFrame initialized with all views");
        });
    }

    private CompletableFuture<Void> registerListeners() {
        return executor.runSync(plugin, () -> {
            LOGGER.info("Registering listeners...");
            var pluginManager = plugin.getServer().getPluginManager();

            if (perkRegistry != null && perkEventBus != null) {
                var cleanupListener = new PerkCleanupListener(perkRegistry, perkEventBus);
                pluginManager.registerEvents(cleanupListener, plugin);
                LOGGER.info("Registered PerkCleanupListener");

                var xpHandler = new ExperienceMultiplierHandler(perkRegistry);
                var xpListener = new ExperienceMultiplierListener(xpHandler);
                pluginManager.registerEvents(xpListener, plugin);
                LOGGER.info("Registered ExperienceMultiplierListener");

                var deathHandler = new DeathPreventionHandler(perkRegistry);
                var deathListener = new DeathPreventionListener(deathHandler, (player, result) -> {
                    if (translationService != null) {
                        translationService.sendMessage(player, "perk.death_prevented",
                            java.util.Map.of("perk", result.perkNameKey(), "health", result.healthRestored()));
                    }
                });
                pluginManager.registerEvents(deathListener, plugin);
                LOGGER.info("Registered DeathPreventionListener");

                var flightHandler = new FlightHandler(perkRegistry);
                var flightListener = new FlightCombatListener(flightHandler, perkEventBus);
                pluginManager.registerEvents(flightListener, plugin);
                LOGGER.info("Registered FlightCombatListener");
            }

            if (damageTracker != null) {
                pluginManager.registerEvents(damageTracker, plugin);
                LOGGER.info("Registered DamageTracker");
            }

            if (bountyService != null && damageTracker != null && bountyAnnouncementService != null) {
                var bountyDeathListener = new BountyDeathListener(bountyService, damageTracker, bountyAnnouncementService);
                pluginManager.registerEvents(bountyDeathListener, plugin);
                LOGGER.info("Registered BountyDeathListener");
            }

            LOGGER.info("All listeners registered");
        });
    }

    private CompletableFuture<Void> registerCommands() {
        return executor.runSync(plugin, () -> {
            LOGGER.info("Registering commands via CommandFactory...");

            try {
                var commandFactory = new com.raindropcentral.commands.CommandFactory(plugin, this);
                commandFactory.registerAllCommandsAndListeners();
                LOGGER.info("All commands and listeners registered via CommandFactory");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to register commands via CommandFactory", e);
            }
        });
    }

    public void shutdown() {
        if (shuttingDown.getAndSet(true)) {
            return;
        }

        LOGGER.info("Shutting down RDQ " + edition + " Edition...");

        if (initializationFuture != null && !initializationFuture.isDone()) {
            initializationFuture.cancel(true);
        }

        if (perkRegistry != null) {
            perkRegistry.clear();
            LOGGER.info("Cleared PerkRegistry");
        }

        if (perkEventBus != null) {
            perkEventBus.clear();
            LOGGER.info("Cleared PerkEventBus");
        }

        if (damageTracker != null) {
            damageTracker.clearAll();
            LOGGER.info("Cleared DamageTracker");
        }

        cacheManager.invalidateAll();
        executor.shutdown();

        LOGGER.info("RDQ " + edition + " Edition shut down successfully");
    }

    public void registerRankService(@NotNull RankService service) {
        this.rankService = Objects.requireNonNull(service, "rankService");
        LOGGER.info("Registered RankService: " + service.getClass().getSimpleName());
    }

    public void registerBountyService(@NotNull BountyService service) {
        this.bountyService = Objects.requireNonNull(service, "bountyService");
        LOGGER.info("Registered BountyService: " + service.getClass().getSimpleName());
    }

    public void registerPerkService(@NotNull PerkService service) {
        this.perkService = Objects.requireNonNull(service, "perkService");
        LOGGER.info("Registered PerkService: " + service.getClass().getSimpleName());
    }

    public void registerPlayerDataService(@NotNull PlayerDataService service) {
        this.playerDataService = Objects.requireNonNull(service, "playerDataService");
        LOGGER.info("Registered PlayerDataService: " + service.getClass().getSimpleName());
    }

    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    public String getEdition() {
        return edition;
    }

    @NotNull
    public RPlatform getPlatform() {
        return platform;
    }

    @NotNull
    public AsyncExecutor getExecutor() {
        return executor;
    }

    @NotNull
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @NotNull
    public ViewFrame getViewFrame() {
        if (viewFrame == null) {
            throw new IllegalStateException("ViewFrame not initialized");
        }
        return viewFrame;
    }

    @NotNull
    public RDQTranslationService getTranslationService() {
        if (translationService == null) {
            throw new IllegalStateException("TranslationService not initialized");
        }
        return translationService;
    }

    @NotNull
    public TranslationRepository getTranslationRepository() {
        return getTranslationService().getRepository();
    }

    @NotNull
    public LocaleResolver getLocaleResolver() {
        return getTranslationService().getLocaleResolver();
    }

    @NotNull
    public MessageFormatter getMessageFormatter() {
        return getTranslationService().getFormatter();
    }

    @NotNull
    public MissingKeyTracker getMissingKeyTracker() {
        return getTranslationService().getMissingKeyTracker();
    }

    @NotNull
    public PlayerDataService getPlayerDataService() {
        if (playerDataService == null) {
            throw new IllegalStateException("PlayerDataService not registered");
        }
        return playerDataService;
    }

    @NotNull
    public RankService getRankService() {
        if (rankService == null) {
            throw new IllegalStateException("RankService not registered");
        }
        return rankService;
    }

    @NotNull
    public BountyService getBountyService() {
        if (bountyService == null) {
            throw new IllegalStateException("BountyService not registered");
        }
        return bountyService;
    }

    @NotNull
    public PerkService getPerkService() {
        if (perkService == null) {
            throw new IllegalStateException("PerkService not registered");
        }
        return perkService;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    @NotNull
    public EditionFeatures getEditionFeatures() {
        return editionFeatures;
    }

    @NotNull
    public com.raindropcentral.rdq.shared.edition.FeatureGate getFeatureGate() {
        if (featureGate == null) {
            featureGate = new com.raindropcentral.rdq.shared.edition.FeatureGate(editionFeatures, translationService);
        }
        return featureGate;
    }

    @Nullable
    public FreeRankService createFreeRankService() {
        initializeRankRepositories();
        if (rankTreeRepository == null || rankRepository == null || 
            playerRankRepository == null || playerRankPathRepository == null) {
            LOGGER.warning("Cannot create FreeRankService: repositories not initialized");
            return null;
        }
        var requirementChecker = new RankRequirementChecker(playerRankRepository);
        return new DefaultFreeRankService(
            this,
            rankTreeRepository,
            rankRepository,
            playerRankRepository,
            playerRankPathRepository,
            requirementChecker
        );
    }

    @Nullable
    public PremiumRankService createPremiumRankService() {
        initializeRankRepositories();
        if (rankTreeRepository == null || rankRepository == null || 
            playerRankRepository == null || playerRankPathRepository == null) {
            LOGGER.warning("Cannot create PremiumRankService: repositories not initialized");
            return null;
        }
        var requirementChecker = new RankRequirementChecker(playerRankRepository);
        return new DefaultPremiumRankService(
            this,
            rankTreeRepository,
            rankRepository,
            playerRankRepository,
            playerRankPathRepository,
            requirementChecker,
            editionFeatures.getMaxActiveRankTrees()
        );
    }

    @Nullable
    public FreePerkService createFreePerkService() {
        initializePerkRepositories();
        if (perkRepository == null || playerPerkRepository == null || playerRankRepository == null) {
            LOGGER.warning("Cannot create FreePerkService: repositories not initialized");
            return null;
        }
        var requirementChecker = new PerkRequirementChecker(playerRankRepository);
        return new DefaultFreePerkService(
            perkRepository,
            playerPerkRepository,
            requirementChecker
        );
    }

    @Nullable
    public PremiumPerkService createPremiumPerkService() {
        initializePerkRepositories();
        if (perkRepository == null || playerPerkRepository == null || playerRankRepository == null) {
            LOGGER.warning("Cannot create PremiumPerkService: repositories not initialized");
            return null;
        }
        var requirementChecker = new PerkRequirementChecker(playerRankRepository);
        return new DefaultPremiumPerkService(
            perkRepository,
            playerPerkRepository,
            requirementChecker
        );
    }

    @Nullable
    private com.raindropcentral.rdq.bounty.economy.EconomyService createEconomyService() {
        // TODO: Add JExEconomy support for multi-currency bounties
        // Currently using Vault (single currency only)
        LOGGER.info("Using Vault for bounty economy");
        return new com.raindropcentral.rdq.bounty.economy.VaultEconomyAdapter(executor.getExecutor());
    }

    @Nullable
    public FreeBountyService createFreeBountyService() {
        initializeBountyRepositories();
        if (bountyRepository == null || hunterStatsRepository == null) {
            LOGGER.warning("Cannot create FreeBountyService: repositories not initialized");
            return null;
        }
        var economyService = createEconomyService();
        var config = bountyConfig != null ? bountyConfig : BountyConfig.defaults();
        return new com.raindropcentral.rdq.bounty.service.DefaultFreeBountyService(
            bountyRepository,
            hunterStatsRepository,
            economyService,
            config
        );
    }

    @Nullable
    public PremiumBountyService createPremiumBountyService() {
        initializeBountyRepositories();
        if (bountyRepository == null || hunterStatsRepository == null) {
            LOGGER.warning("Cannot create PremiumBountyService: repositories not initialized");
            return null;
        }
        var economyService = createEconomyService();
        var config = bountyConfig != null ? bountyConfig : BountyConfig.defaults();
        return new com.raindropcentral.rdq.bounty.service.DefaultPremiumBountyService(
            bountyRepository,
            hunterStatsRepository,
            economyService,
            config
        );
    }

    private void initializeRankRepositories() {
        var entityManagerFactory = platform.getEntityManagerFactory();
        if (entityManagerFactory == null) {
            LOGGER.warning("EntityManagerFactory not available - database operations will fail");
        }
        if (rankTreeRepository == null) {
            rankTreeRepository = new RankTreeRepository();
        }
        if (rankRepository == null) {
            rankRepository = new RankRepository();
        }
        if (playerRankRepository == null) {
            playerRankRepository = new PlayerRankRepository(executor.getExecutor(), entityManagerFactory);
        }
        if (playerRankPathRepository == null) {
            playerRankPathRepository = new PlayerRankPathRepository(executor.getExecutor(), entityManagerFactory);
        }
    }

    private void initializePerkRepositories() {
        initializeRankRepositories();
        var entityManagerFactory = platform.getEntityManagerFactory();
        if (perkRepository == null) {
            perkRepository = new PerkRepository();
        }
        if (playerPerkRepository == null) {
            playerPerkRepository = new PlayerPerkRepository(executor.getExecutor(), entityManagerFactory);
        }
    }

    private void initializeBountyRepositories() {
        var entityManagerFactory = platform.getEntityManagerFactory();
        if (entityManagerFactory == null) {
            LOGGER.warning("EntityManagerFactory not available - database operations will fail");
        }
        if (bountyRepository == null) {
            bountyRepository = new BountyRepository(executor.getExecutor(), entityManagerFactory);
        }
        if (hunterStatsRepository == null) {
            hunterStatsRepository = new HunterStatsRepository(executor.getExecutor(), entityManagerFactory);
        }
    }

    @NotNull
    public PerkRegistry getPerkRegistry() {
        if (perkRegistry == null) {
            throw new IllegalStateException("PerkRegistry not initialized");
        }
        return perkRegistry;
    }

    @NotNull
    public PerkEventBus getPerkEventBus() {
        if (perkEventBus == null) {
            throw new IllegalStateException("PerkEventBus not initialized");
        }
        return perkEventBus;
    }

    @NotNull
    public DamageTracker getDamageTracker() {
        if (damageTracker == null) {
            throw new IllegalStateException("DamageTracker not initialized");
        }
        return damageTracker;
    }

    @NotNull
    public BountyConfig getBountyConfig() {
        if (bountyConfig == null) {
            return BountyConfig.defaults();
        }
        return bountyConfig;
    }

    /**
     * Reloads the bounty configuration from file.
     * 
     * @return the reloaded BountyConfig
     */
    @NotNull
    public BountyConfig reloadBountyConfig() {
        var loader = new BountyConfigLoader(plugin);
        bountyConfig = loader.loadConfig();
        
        // Update announcement service with new config
        if (bountyAnnouncementService != null) {
            bountyAnnouncementService = new BountyAnnouncementService(bountyConfig);
        }
        
        LOGGER.info("Bounty configuration reloaded - selfTargetAllowed: " + bountyConfig.selfTargetAllowed());
        return bountyConfig;
    }

    @NotNull
    public BountyAnnouncementService getBountyAnnouncementService() {
        if (bountyAnnouncementService == null) {
            throw new IllegalStateException("BountyAnnouncementService not initialized");
        }
        return bountyAnnouncementService;
    }

    @Nullable
    public PerkRepository getPerkRepository() {
        return perkRepository;
    }

    @Nullable
    public RankTreeRepository getRankTreeRepository() {
        return rankTreeRepository;
    }

    @Nullable
    public RankRepository getRankRepositoryInstance() {
        return rankRepository;
    }
}
