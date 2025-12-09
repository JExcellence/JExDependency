/*
package com.raindropcentral.rdq2;

import com.raindropcentral.rdq2.bounty.config.BountyConfig;
import com.raindropcentral.rdq2.bounty.config.BountyConfigLoader;
import com.raindropcentral.rdq2.bounty.tracking.DamageTracker;
import com.raindropcentral.rdq2.database.repository.*;
import com.raindropcentral.rdq2.perk.event.PerkEventBus;
import com.raindropcentral.rdq2.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq2.perk.runtime.PerkTypeRegistry;
import com.raindropcentral.rdq2.shared.AsyncExecutor;
import com.raindropcentral.rdq2.shared.CacheManager;
import com.raindropcentral.rdq2.shared.edition.EditionFeatures;
import com.raindropcentral.rplatform.RPlatform;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Core initialization and service management for RDQ.
 * Handles plugin lifecycle, service registration, and provides access to all RDQ subsystems.
 *//*

public class RDQCore {
    
    private static final Logger LOGGER = Logger.getLogger(RDQCore.class.getName());
    
    private final JavaPlugin plugin;
    private final String edition;
    private final EditionFeatures editionFeatures;
    private final RPlatform platform;
    private final AsyncExecutor executor;
    private final CacheManager cacheManager;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Repositories
    private @Nullable RRankTreeRepository rankTreeRepository;
    private @Nullable RRankRepository rankRepository;
    private @Nullable RPlayerRankRepository playerRankRepository;
    private @Nullable RPlayerRankPathRepository playerRankPathRepository;
    private @Nullable RPerkRepository perkRepository;
    private @Nullable RPlayerPerkRepository playerPerkRepository;
    private @Nullable RBountyRepository bountyRepository;
    private @Nullable BountyHunterStatsRepository hunterStatsRepository;
    
    // Core components
    private @Nullable PerkRegistry perkRegistry;
    private @Nullable PerkEventBus perkEventBus;
    private @Nullable DamageTracker damageTracker;
    private @Nullable BountyConfig bountyConfig;
    
    // Services (registered by edition implementations)
    private @Nullable com.raindropcentral.rdq2.api.RankService rankService;
    private @Nullable com.raindropcentral.rdq2.api.PerkService perkService;
    private @Nullable com.raindropcentral.rdq2.api.BountyService bountyService;
    
    public RDQCore(@NotNull JavaPlugin plugin, @NotNull String edition, @NotNull EditionFeatures editionFeatures) {
        this.plugin = plugin;
        this.edition = edition;
        this.editionFeatures = editionFeatures;
        this.platform = new RPlatform(plugin);
        this.executor = new AsyncExecutor();
        this.cacheManager = new CacheManager();
    }
    
    public CompletableFuture<Void> initialize() {
        if (initialized.get()) {
            LOGGER.warning("RDQCore already initialized");
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.info("Initializing RDQ " + edition + " Edition v6.0.0");
        
        return platform.initialize()
            .thenCompose(v -> initializeRepositories())
            .thenCompose(v -> initializeSharedComponents())
            .thenRun(() -> {
                initialized.set(true);
                LOGGER.info("RDQ " + edition + " Edition initialized successfully!");
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to initialize RDQ", ex);
                return null;
            });
    }
    
    private CompletableFuture<Void> initializeRepositories() {
        return executor.runAsync(() -> {
            LOGGER.info("Initializing repositories...");
            
            var entityManagerFactory = platform.getEntityManagerFactory();
            if (entityManagerFactory == null) {
                LOGGER.warning("EntityManagerFactory not available - database operations will fail");
                return;
            }
            
            var executorService = executor.getExecutor();
            
            // Initialize rank repositories
            rankTreeRepository = new RRankTreeRepository(executorService, entityManagerFactory);
            rankRepository = new RRankRepository(executorService, entityManagerFactory);
            playerRankRepository = new RPlayerRankRepository(executorService, entityManagerFactory);
            playerRankPathRepository = new RPlayerRankPathRepository(executorService, entityManagerFactory);
            
            // Initialize perk repositories
            perkRepository = new RPerkRepository(executorService, entityManagerFactory);
            playerPerkRepository = new RPlayerPerkRepository(executorService, entityManagerFactory);
            
            // Initialize bounty repositories
            bountyRepository = new RBountyRepository(executorService, entityManagerFactory);
            hunterStatsRepository = new BountyHunterStatsRepository(executorService, entityManagerFactory);
            
            LOGGER.info("All repositories initialized");
        });
    }
    
    private CompletableFuture<Void> initializeSharedComponents() {
        return executor.runSync(plugin, () -> {
            LOGGER.info("Initializing shared components...");
            
            // Initialize perk system
            var perkTypeRegistry = new PerkTypeRegistry();
            perkRegistry = new PerkRegistry(perkTypeRegistry);
            perkEventBus = new PerkEventBus();
            
            // Initialize bounty system (30 second tracking window)
            damageTracker = new DamageTracker(30000L);
            
            // Load bounty configuration
            var bountyConfigLoader = new BountyConfigLoader(plugin);
            bountyConfig = bountyConfigLoader.loadConfig();
            LOGGER.info("Bounty config loaded - selfTargetAllowed: " + bountyConfig.selfTargetAllowed());
            
            LOGGER.info("Shared components initialized");
        });
    }
    
    public void shutdown() {
        LOGGER.info("Shutting down RDQ " + edition + " Edition");
        
        if (executor != null) {
            executor.shutdown();
        }
        
        initialized.set(false);
    }
    
    // Getters
    public Plugin getPlugin() {
        return plugin;
    }
    
    public String getEdition() {
        return edition;
    }
    
    public EditionFeatures getFeatures() {
        return editionFeatures;
    }
    
    public RPlatform getPlatform() {
        return platform;
    }
    
    public AsyncExecutor getExecutor() {
        return executor;
    }
    
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    public PerkRegistry getPerkRegistry() {
        return perkRegistry;
    }
    
    public PerkEventBus getPerkEventBus() {
        return perkEventBus;
    }
    
    public DamageTracker getDamageTracker() {
        return damageTracker;
    }
    
    public BountyConfig getBountyConfig() {
        return bountyConfig;
    }
    
    // Service registration
    public void registerRankService(com.raindropcentral.rdq2.api.RankService service) {
        LOGGER.info("Registering Rank Service: " + service.getClass().getSimpleName());
        this.rankService = service;
    }
    
    public void registerPerkService(com.raindropcentral.rdq2.api.PerkService service) {
        LOGGER.info("Registering Perk Service: " + service.getClass().getSimpleName());
        this.perkService = service;
    }
    
    public void registerBountyService(com.raindropcentral.rdq2.api.BountyService service) {
        LOGGER.info("Registering Bounty Service: " + service.getClass().getSimpleName());
        this.bountyService = service;
    }
    
    public void registerServiceDependentViews() {
        LOGGER.info("Registering service-dependent views");
        // Views will be registered after services are available
        // This is called by edition implementations after service registration
    }
    
    // Service creation methods for Free edition
    public com.raindropcentral.rdq2.api.FreeRankService createFreeRankService() {
        LOGGER.info("Creating Free Rank Service");
        if (playerRankRepository == null || rankRepository == null || rankTreeRepository == null) {
            LOGGER.warning("Repositories not initialized, service will not be available");
            return null;
        }
        return new com.raindropcentral.rdq2.rank.service.DefaultFreeRankService(
            playerRankRepository,
            rankRepository,
            rankTreeRepository
        );
    }
    
    public com.raindropcentral.rdq2.api.FreePerkService createFreePerkService() {
        LOGGER.info("Creating Free Perk Service");
        if (perkRegistry == null || playerPerkRepository == null) {
            LOGGER.warning("Components not initialized, service will not be available");
            return null;
        }
        // Note: PerkRepository in perk.repository is different from RPerkRepository in database.repository
        // The service needs the in-memory PerkRepository which is managed by PerkRegistry
        // For now, return null until the architecture is properly aligned
        LOGGER.warning("Perk service creation needs architectural alignment - returning null");
        return null;
    }
    
    public com.raindropcentral.rdq2.api.FreeBountyService createFreeBountyService() {
        LOGGER.info("Creating Free Bounty Service");
        if (bountyRepository == null || hunterStatsRepository == null || damageTracker == null || bountyConfig == null) {
            LOGGER.warning("Components not initialized, service will not be available");
            return null;
        }
        // Note: BountyRepository types need alignment
        // For now, return null until the architecture is properly aligned
        LOGGER.warning("Bounty service creation needs architectural alignment - returning null");
        return null;
    }
    
    // Service creation methods for Premium edition
    public com.raindropcentral.rdq2.api.PremiumRankService createPremiumRankService() {
        LOGGER.info("Creating Premium Rank Service");
        if (playerRankRepository == null || rankRepository == null || rankTreeRepository == null) {
            LOGGER.warning("Repositories not initialized, service will not be available");
            return null;
        }
        return new com.raindropcentral.rdq2.rank.service.DefaultPremiumRankService(
            playerRankRepository,
            rankRepository,
            rankTreeRepository
        );
    }
    
    public com.raindropcentral.rdq2.api.PremiumPerkService createPremiumPerkService() {
        LOGGER.info("Creating Premium Perk Service");
        LOGGER.warning("Premium perk service creation needs architectural alignment - returning null");
        return null;
    }
    
    public com.raindropcentral.rdq2.api.PremiumBountyService createPremiumBountyService() {
        LOGGER.info("Creating Premium Bounty Service");
        LOGGER.warning("Premium bounty service creation needs architectural alignment - returning null");
        return null;
    }
}
*/
