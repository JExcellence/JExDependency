package de.jexcellence.oneblock;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.RequirementRegistry;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.hibernate.repository.RepositoryManager;
import de.jexcellence.multiverse.api.IMultiverseAdapter;
import de.jexcellence.oneblock.adapter.MultiverseAdapter;
import de.jexcellence.oneblock.bonus.BonusManager;
import de.jexcellence.oneblock.bonus.EnhancedBonusSystem;
import de.jexcellence.oneblock.config.OneblockGameplaySection;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionBlock;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionEntity;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionItem;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure;
import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandBan;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockRegion;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockVisitorSettings;
import de.jexcellence.oneblock.database.repository.EvolutionBlockRepository;
import de.jexcellence.oneblock.database.repository.EvolutionEntityRepository;
import de.jexcellence.oneblock.database.repository.EvolutionItemRepository;
import de.jexcellence.oneblock.database.repository.IslandInfrastructureRepository;
import de.jexcellence.oneblock.database.repository.OneblockEvolutionRepository;
import de.jexcellence.oneblock.database.repository.OneblockIslandBanRepository;
import de.jexcellence.oneblock.database.repository.OneblockIslandMemberRepository;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import de.jexcellence.oneblock.database.repository.OneblockPlayerRepository;
import de.jexcellence.oneblock.database.repository.OneblockRegionRepository;
import de.jexcellence.oneblock.database.repository.OneblockVisitorSettingsRepository;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import de.jexcellence.oneblock.manager.IslandStorageManager;
import de.jexcellence.oneblock.manager.infrastructure.InfrastructureManager;
import de.jexcellence.oneblock.region.IslandRegionManager;
import de.jexcellence.oneblock.region.RegionBoundaryChecker;
import de.jexcellence.oneblock.region.SpiralIslandGenerator;
import de.jexcellence.oneblock.repository.GeneratorDesignRepository;
import de.jexcellence.oneblock.repository.PlayerGeneratorStructureRepository;
import de.jexcellence.oneblock.repository.IslandRegionRepository;
import de.jexcellence.oneblock.requirement.generator.OneBlockRequirement;
import de.jexcellence.oneblock.requirement.generator.OneBlockRequirementProvider;
import de.jexcellence.oneblock.service.BiomeService;
import de.jexcellence.oneblock.service.DynamicEvolutionService;
import de.jexcellence.oneblock.service.IInfrastructureService;
import de.jexcellence.oneblock.service.IOneblockService;
import de.jexcellence.oneblock.service.InfrastructureServiceImpl;
import de.jexcellence.oneblock.service.IslandBanService;
import de.jexcellence.oneblock.service.GeneratorStructureManager;
import de.jexcellence.oneblock.service.IslandCacheService;
import de.jexcellence.oneblock.service.IslandMemberService;
import de.jexcellence.oneblock.service.StartupInitializationService;
import de.jexcellence.oneblock.utility.workload.DistributedWorkloadRunnable;
import de.jexcellence.oneblock.utility.workload.biome.DistributedBiomeChanger;

import de.jexcellence.oneblock.view.generator.GeneratorBrowserView;
import de.jexcellence.oneblock.view.generator.GeneratorBuildProgressView;
import de.jexcellence.oneblock.view.generator.GeneratorDesignDetailView;
import de.jexcellence.oneblock.view.generator.GeneratorVisualization3DView;
import de.jexcellence.oneblock.view.infrastructure.AutomationView;
import de.jexcellence.oneblock.view.infrastructure.CraftingQueueView;
import de.jexcellence.oneblock.view.infrastructure.GeneratorUpgradeView;
import de.jexcellence.oneblock.view.infrastructure.GeneratorsView;
import de.jexcellence.oneblock.view.infrastructure.InfrastructureMainView;
import de.jexcellence.oneblock.view.infrastructure.InfrastructureStatsView;
import de.jexcellence.oneblock.view.infrastructure.ModuleCraftView;
import de.jexcellence.oneblock.view.infrastructure.ProcessorUpgradeView;
import de.jexcellence.oneblock.view.infrastructure.ProcessorsView;
import de.jexcellence.oneblock.view.infrastructure.StorageUpgradeView;
import de.jexcellence.oneblock.view.infrastructure.StorageView;
import de.jexcellence.oneblock.view.infrastructure.StorageWithdrawView;
import de.jexcellence.oneblock.view.island.BannedPlayersView;
import de.jexcellence.oneblock.view.island.BiomeSelectionView;
import de.jexcellence.oneblock.view.island.EvolutionBrowserView;
import de.jexcellence.oneblock.view.island.EvolutionDetailView;
import de.jexcellence.oneblock.view.island.IslandMainView;
import de.jexcellence.oneblock.view.island.IslandSettingsView;
import de.jexcellence.oneblock.view.island.MemberPermissionView;
import de.jexcellence.oneblock.view.island.MembersListView;
import de.jexcellence.oneblock.view.island.OneblockCoreView;
import de.jexcellence.oneblock.view.island.VisitorSettingsView;
import de.jexcellence.oneblock.view.storage.StorageCategoryView;
import de.jexcellence.oneblock.view.storage.StorageItemDetailView;
import lombok.Getter;
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

@Getter
public abstract class JExOneblock {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExOneblock");

    private final JavaPlugin plugin;
    private final String edition;
    private final ExecutorService executor;
    private final RPlatform platform;

    private volatile CompletableFuture<Void> onEnableFuture;
    private boolean disabling;

    private ViewFrame viewFrame;
    private IOneblockService oneblockService;
    private IInfrastructureService infrastructureService;
    private InfrastructureManager infrastructureManager;
    private IslandStorageManager islandStorageManager;
    private StartupInitializationService startupInitializationService;
    private BiomeService biomeService;
    private DistributedWorkloadRunnable workloadRunnable;
    private MultiverseAdapter multiverseAdapter;
    private IslandCacheService islandCacheService;
    private OneblockGameplaySection gameplayConfig;
    
    // Evolution System
    private DynamicEvolutionService dynamicEvolutionService;
    private EnhancedBonusSystem enhancedBonusSystem;
    
    // Region Management System
    private IslandRegionManager regionManager;
    private SpiralIslandGenerator spiralGenerator;
    private RegionBoundaryChecker boundaryChecker;
    
    // Generator Structure System
    private GeneratorStructureManager generatorStructureManager;

    @InjectRepository
    private OneblockIslandRepository oneblockIslandRepository;

    @InjectRepository
    private OneblockPlayerRepository oneblockPlayerRepository;

    @InjectRepository
    private OneblockIslandMemberRepository oneblockIslandMemberRepository;

    @InjectRepository
    private OneblockIslandBanRepository oneblockIslandBanRepository;

    @InjectRepository
    private OneblockRegionRepository oneblockRegionRepository;

    @InjectRepository
    private OneblockVisitorSettingsRepository oneblockVisitorSettingsRepository;

    @InjectRepository
    private OneblockEvolutionRepository oneblockEvolutionRepository;

    @InjectRepository
    private IslandInfrastructureRepository islandInfrastructureRepository;

    @InjectRepository
    private EvolutionBlockRepository evolutionBlockRepository;

    @InjectRepository
    private EvolutionEntityRepository evolutionEntityRepository;

    @InjectRepository
    private EvolutionItemRepository evolutionItemRepository;

    @InjectRepository
    private IslandRegionRepository islandRegionRepository;
    
    @InjectRepository
    private GeneratorDesignRepository generatorDesignRepository;
    
    @InjectRepository
    private PlayerGeneratorStructureRepository playerGeneratorStructureRepository;

    public JExOneblock(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
        this.platform = new RPlatform(plugin);
        this.executor = Executors.newFixedThreadPool(4);
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
                return initializeStartupServices();
            })
            .thenRun(() -> {
                initializeService();
                initializePlugins();
                initializeComponents();
                initializeViews();
                platform.initializeMetrics(getMetricsId());

                LOGGER.info(getStartupMessage());
                LOGGER.info("JExOneblock (" + edition + ") Edition enabled successfully!");
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Failed to initialize JExOneblock", throwable);
                return null;
            });
    }

    public void onDisable() {
        disabling = true;

        if (islandCacheService != null) {
            try {
                islandCacheService.saveAllCachedIslands().join();
                LOGGER.info("All cached islands saved successfully");
            } catch (Exception e) {
                LOGGER.severe("Failed to save cached islands during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (infrastructureService instanceof InfrastructureServiceImpl impl) {
            impl.shutdown();
        }
        
        if (generatorStructureManager != null) {
            generatorStructureManager.shutdown();
        }
        
        // Clear OneBlock requirement repository reference
        OneBlockRequirement.clearRepository();
        
        // Unregister OneBlock requirement provider
        RequirementRegistry.getInstance().unregisterProvider("jexoneblock");

        if (workloadRunnable != null && !workloadRunnable.isCancelled()) {
            workloadRunnable.cancel();
            LOGGER.info("Workload runnable shutdown");
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        LOGGER.info("JExOneblock (" + edition + ") Edition disabled successfully!");
    }

    @NotNull
    protected abstract String getStartupMessage();

    protected abstract int getMetricsId();

    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    @NotNull
    protected abstract IOneblockService createOneblockService();

    private void initializeConfig() {
        try {
            var cfgManager = new ConfigManager(this.getPlugin(), "configs");

            var gameplayCfgKeeper = new ConfigKeeper<>(cfgManager, "gameplay.yml", OneblockGameplaySection.class);
            this.gameplayConfig = gameplayCfgKeeper.rootSection;
            LOGGER.log(Level.INFO, "Oneblock gameplay configuration initialized");
            
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to load configurations, using defaults", exception);
            
            try {
                this.gameplayConfig = new OneblockGameplaySection(new EvaluationEnvironmentBuilder());
                LOGGER.log(Level.INFO, "Configurations initialized with defaults");
            } catch (Exception fallbackException) {
                LOGGER.log(Level.WARNING, "Failed to initialize default configurations", fallbackException);
                this.gameplayConfig = null;
            }
        }
    }

    private void initializeRepositories() {
        final var emf = this.platform.getEntityManagerFactory();

        if (emf == null) {
            LOGGER.warning("EntityManagerFactory not initialized");
            return;
        }

        // Register OneBlock requirement provider BEFORE repository initialization
        // This ensures the ObjectMapper is configured before any requirements are deserialized
        LOGGER.info("Registering OneBlock requirement provider...");
        RequirementRegistry.getInstance().registerProvider(new OneBlockRequirementProvider());
        
        LOGGER.info("Resetting RequirementParser mapper...");
        com.raindropcentral.rplatform.requirement.json.RequirementParser.resetMapper();
        
        LOGGER.info("OneBlock requirement provider registered and mapper reset");

        RepositoryManager.initialize(this.executor, emf);
        var repositoryManager = RepositoryManager.getInstance();

        repositoryManager.register(OneblockIslandRepository.class, OneblockIsland.class, OneblockIsland::getIdentifier);
        repositoryManager.register(OneblockPlayerRepository.class, OneblockPlayer.class, OneblockPlayer::getUniqueId);
        repositoryManager.register(OneblockIslandMemberRepository.class, OneblockIslandMember.class, OneblockIslandMember::getId);
        repositoryManager.register(OneblockIslandBanRepository.class, OneblockIslandBan.class, OneblockIslandBan::getId);
        repositoryManager.register(OneblockRegionRepository.class, OneblockRegion.class, OneblockRegion::getId);
        repositoryManager.register(OneblockVisitorSettingsRepository.class, OneblockVisitorSettings.class, OneblockVisitorSettings::getId);
        repositoryManager.register(OneblockEvolutionRepository.class, OneblockEvolution.class, OneblockEvolution::getEvolutionName);
        repositoryManager.register(IslandInfrastructureRepository.class, IslandInfrastructure.class, IslandInfrastructure::getId);
        repositoryManager.register(EvolutionBlockRepository.class, EvolutionBlock.class, EvolutionBlock::getId);
        repositoryManager.register(EvolutionEntityRepository.class, EvolutionEntity.class, EvolutionEntity::getId);
        repositoryManager.register(EvolutionItemRepository.class, EvolutionItem.class, EvolutionItem::getId);
        repositoryManager.register(IslandRegionRepository.class, IslandRegion.class, IslandRegion::getIslandId);
        repositoryManager.register(GeneratorDesignRepository.class, GeneratorDesign.class, GeneratorDesign::getDesignKey);
        repositoryManager.register(PlayerGeneratorStructureRepository.class, PlayerGeneratorStructure.class, PlayerGeneratorStructure::getId);

        repositoryManager.injectInto(this);
        LOGGER.log(Level.INFO, "Repositories initialized");
    }

    private CompletableFuture<Void> initializeStartupServices() {
        EvolutionFactory evolutionFactory = EvolutionFactory.getInstance();

        LOGGER.info("Starting evolution auto-registration...");
        int registeredCount = evolutionFactory.autoRegisterPredefinedEvolutions("de.jexcellence.oneblock.database.entity.evolution");
        LOGGER.info("Auto-registered " + registeredCount + " essential evolutions");

        var registeredNames = evolutionFactory.getRegisteredEvolutionNames();
        LOGGER.info("Registered evolution names: " + registeredNames);

        this.startupInitializationService = new StartupInitializationService(
            evolutionFactory,
            oneblockEvolutionRepository,
            executor
        );

        return startupInitializationService.performStartupInitialization()
            .thenAccept(result -> {
                if (result.success()) {
                    LOGGER.info("✓ Startup initialization completed successfully");
                    if (result.evolutionResult() != null && result.evolutionResult().hasChanges()) {
                        LOGGER.info("  - Evolution database updated with " + result.evolutionResult().created() + 
                            " new and " + result.evolutionResult().updated() + " updated entries");
                    }
                } else {
                    LOGGER.warning("⚠ Startup initialization completed with issues: " + result.message());
                    if (!result.isReadyForIslandCreation()) {
                        LOGGER.severe("Plugin is NOT ready for island creation - check configuration!");
                    }
                }
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Critical error during startup initialization", throwable);
                return null;
            });
    }

    private void initializeService() {
        this.oneblockService = createOneblockService();
        LOGGER.info("Oneblock service initialized: " + (oneblockService.isPremium() ? "Premium" : "Free"));

        if (islandInfrastructureRepository != null) {
            var infrastructureServiceImpl = new InfrastructureServiceImpl(
                plugin, 
                islandInfrastructureRepository
            );
            infrastructureServiceImpl.initialize();
            this.infrastructureService = infrastructureServiceImpl;
            this.infrastructureManager = infrastructureServiceImpl.getManager();
            LOGGER.info("Infrastructure service initialized");
        }

        this.workloadRunnable = new DistributedWorkloadRunnable(plugin);
        this.workloadRunnable.runTaskTimer(plugin, 1L, 1L);
        
        var biomeChanger = new DistributedBiomeChanger(workloadRunnable);
        this.biomeService = new BiomeService(biomeChanger);
        LOGGER.info("Biome service initialized with distributed processing");

        this.islandCacheService = new IslandCacheService(this.oneblockIslandRepository);
        LOGGER.info("Island cache service initialized");
        
        // Initialize Dynamic Evolution Service
        this.dynamicEvolutionService = new DynamicEvolutionService();
        LOGGER.info("Dynamic evolution service initialized");
        
        // Initialize Enhanced Bonus System
        BonusManager bonusManager = new BonusManager(EvolutionFactory.getInstance());
        this.enhancedBonusSystem = new EnhancedBonusSystem(dynamicEvolutionService, bonusManager);
        LOGGER.info("Enhanced bonus system initialized");
        
        this.islandStorageManager = new IslandStorageManager();
        LOGGER.info("Island storage manager initialized");
        
        // Initialize Region Management System
        initializeRegionManagement();
        
        // Initialize Generator Structure System
        initializeGeneratorStructureSystem();
    }
    
    private void initializeRegionManagement() {
        try {
            // Initialize spiral generator
            this.spiralGenerator = new SpiralIslandGenerator();
            LOGGER.info("Spiral island generator initialized");
            
            // Initialize boundary checker
            this.boundaryChecker = new RegionBoundaryChecker();
            LOGGER.info("Region boundary checker initialized");
            
            // Initialize region manager
            this.regionManager = new IslandRegionManager(spiralGenerator, boundaryChecker);
            
            // Set repository if available
            if (islandRegionRepository != null) {
                regionManager.setRepository(islandRegionRepository);
                LOGGER.info("Region manager initialized with database repository");
            } else {
                LOGGER.warning("Region manager initialized without database repository");
            }
            
            // Configure default worlds (this should be done when worlds are loaded)
            // For now, we'll configure them when needed
            
            LOGGER.info("✓ Region management system initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize region management system", e);
        }
    }
    
    private void initializeGeneratorStructureSystem() {
        try {
            if (generatorDesignRepository == null || playerGeneratorStructureRepository == null) {
                LOGGER.warning("Cannot initialize Generator Structure System - repositories not injected");
                return;
            }
            
            // Initialize the manager
            this.generatorStructureManager = new GeneratorStructureManager(
                plugin,
                generatorDesignRepository,
                playerGeneratorStructureRepository
            );
            
            // Initialize asynchronously
            generatorStructureManager.initialize()
                .thenRun(() -> LOGGER.info("✓ Generator Structure System initialized successfully"))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to initialize Generator Structure System", ex);
                    return null;
                });
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Generator Structure System", e);
        }
    }

    private void initializeComponents() {
        var command = new CommandFactory(plugin, this);
        command.registerAllCommandsAndListeners();
        
        // Inject repository into OneBlock requirements
        OneBlockRequirement.setIslandRepository(oneblockIslandRepository);
        
        LOGGER.info("Components initialized");
    }

    private void initializePlugins() {
        new ServiceRegistry().register(
                "de.jexcellence.multiverse.api.MultiverseAdapter",
                "JExMultiverse"
        ).optional().maxAttempts(30).retryDelay(500).onSuccess(multiverseService -> {
            this.multiverseAdapter = new MultiverseAdapter((IMultiverseAdapter) multiverseService);
        }).onFailure(() -> {
            LOGGER.log(Level.INFO, "Multiverse service initialization failed, not present.");
            this.multiverseAdapter = new MultiverseAdapter(null);
        }).load();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        try {
            ViewFrame frame = ViewFrame.create(plugin)
                    .with(
                            new IslandMainView(),
                            new EvolutionBrowserView(),
                            new EvolutionDetailView(),
                            new MembersListView(),
                            new IslandSettingsView(),
                            new BiomeSelectionView(),
                            new BannedPlayersView(),
                            new MemberPermissionView(),
                            new OneblockCoreView(),
                            new VisitorSettingsView(),
                            new InfrastructureMainView(),
                            new AutomationView(),
                            new CraftingQueueView(),
                            new GeneratorsView(),
                            new GeneratorUpgradeView(),
                            new GeneratorDesignDetailView(),
                            new GeneratorVisualization3DView(),
                            new GeneratorBrowserView(),
                            new GeneratorUpgradeView(),
                            new GeneratorBuildProgressView(),
                            new InfrastructureStatsView(),
                            new ModuleCraftView(),
                            new ProcessorsView(),
                            new ProcessorUpgradeView(),
                            new StorageView(),
                            new StorageUpgradeView(),
                            new StorageWithdrawView(),
                            new StorageCategoryView(),
                            new StorageItemDetailView()
                    )
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize views: " + e.getMessage());
        }
    }

    public @NotNull IOneblockService getOneblockService() {
        return oneblockService;
    }
    
    public @NotNull ViewFrame getViewFrame() {
        return viewFrame;
    }
    
    public @Nullable IInfrastructureService getInfrastructureService() {
        return infrastructureService;
    }
    
    public @Nullable InfrastructureManager getInfrastructureManager() {
        return infrastructureManager;
    }
    
    public @Nullable IslandStorageManager getIslandStorageManager() {
        return islandStorageManager;
    }
    
    protected void setInfrastructureService(@NotNull IInfrastructureService service) {
        this.infrastructureService = service;
    }
    
    protected void setInfrastructureManager(@NotNull InfrastructureManager manager) {
        this.infrastructureManager = manager;
    }
    
    public @NotNull StartupInitializationService getStartupInitializationService() {
        return startupInitializationService;
    }

    /**
     * Gets the island ban service.
     * @return the island ban service
     */
    @NotNull
    public IslandBanService getIslandBanService() {
        return new IslandBanService(oneblockIslandBanRepository);
    }

    /**
     * Gets the island member service.
     * @return the island member service
     */
    @NotNull
    public IslandMemberService getIslandMemberService() {
        return new IslandMemberService(oneblockIslandMemberRepository);
    }

    /**
     * Gets the biome service.
     * @return the biome service
     */
    @NotNull
    public BiomeService getBiomeService() {
        if (biomeService == null) {
            throw new IllegalStateException("BiomeService not initialized yet. Make sure the plugin is fully loaded.");
        }
        return biomeService;
    }

    /**
     * Gets the island cache service.
     * @return the island cache service
     */
    @NotNull
    public IslandCacheService getIslandCacheService() {
        if (islandCacheService == null) {
            throw new IllegalStateException("IslandCacheService not initialized yet. Make sure the plugin is fully loaded.");
        }
        return islandCacheService;
    }
    
    @NotNull
    public DynamicEvolutionService getDynamicEvolutionService() {
        if (dynamicEvolutionService == null) {
            throw new IllegalStateException("DynamicEvolutionService not initialized yet. Make sure the plugin is fully loaded.");
        }
        return dynamicEvolutionService;
    }
    
    @NotNull
    public EnhancedBonusSystem getEnhancedBonusSystem() {
        if (enhancedBonusSystem == null) {
            throw new IllegalStateException("EnhancedBonusSystem not initialized yet. Make sure the plugin is fully loaded.");
        }
        return enhancedBonusSystem;
    }

    /**
     * Gets the structure detection service.
     * @return the structure detection service
     */
    @NotNull
    public OneblockGameplaySection getGameplayConfig() {
        if (gameplayConfig != null) {
            return gameplayConfig;
        }

        try {
            return new OneblockGameplaySection(new EvaluationEnvironmentBuilder());
        } catch (Exception e) {
            LOGGER.warning("Failed to create fallback gameplay config: " + e.getMessage());
            return new OneblockGameplaySection(null);
        }
    }

    @NotNull
    public ExecutorService getExecutor() {
        return executor;
    }

    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Gets the island region manager.
     * @return the island region manager
     */
    @NotNull
    public IslandRegionManager getRegionManager() {
        if (regionManager == null) {
            throw new IllegalStateException("RegionManager not initialized yet. Make sure the plugin is fully loaded.");
        }
        return regionManager;
    }
    
    /**
     * Gets the spiral island generator.
     * @return the spiral island generator
     */
    @NotNull
    public SpiralIslandGenerator getSpiralGenerator() {
        if (spiralGenerator == null) {
            throw new IllegalStateException("SpiralGenerator not initialized yet. Make sure the plugin is fully loaded.");
        }
        return spiralGenerator;
    }
    
    /**
     * Gets the region boundary checker.
     * @return the region boundary checker
     */
    @NotNull
    public RegionBoundaryChecker getBoundaryChecker() {
        if (boundaryChecker == null) {
            throw new IllegalStateException("BoundaryChecker not initialized yet. Make sure the plugin is fully loaded.");
        }
        return boundaryChecker;
    }
    
    /**
     * Gets the generator structure manager.
     * @return the generator structure manager
     */
    @Nullable
    public GeneratorStructureManager getGeneratorStructureManager() {
        return generatorStructureManager;
    }
}
