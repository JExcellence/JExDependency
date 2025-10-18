package com.raindropcentral.rplatform;

import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.logging.PlatformLogger;
import com.raindropcentral.rplatform.metrics.MetricsManager;
import com.raindropcentral.rplatform.placeholder.PlaceholderManager;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.raindropcentral.rplatform.localization.TranslationManager;
import de.jexcellence.evaluable.CommandUpdater;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class RPlatform {

    private final JavaPlugin plugin;
    private final PlatformType platformType;
    private final PlatformAPI platformAPI;
    private final ISchedulerAdapter scheduler;
    private final ServiceRegistry serviceRegistry;
    private final PlatformLogger logger;
    
    private CommandUpdater commandUpdater;
    private TranslationManager translationManager;
    private EntityManagerFactory entityManagerFactory;
    private MetricsManager metricsManager;
    private PlaceholderManager placeholderManager;
    
    private boolean premiumVersion;
    private boolean initialized;

    public RPlatform(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.platformAPI = PlatformAPIFactory.create(plugin);
        this.scheduler = ISchedulerAdapter.create(plugin, platformType);
        this.serviceRegistry = new ServiceRegistry();
        this.logger = PlatformLogger.create(plugin);
        this.premiumVersion = false;
        this.initialized = false;
    }

    public @NotNull CompletableFuture<Void> initialize() {
        if (initialized) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            logger.info("Initializing RPlatform for " + platformType.name());

            this.initializeDatabaseResources();

            translationManager = new TranslationManager(plugin);
            translationManager.initialize();
            
            commandUpdater = new CommandUpdater(plugin);
            
            logger.info("RPlatform initialized successfully");
            initialized = true;
        }, scheduler::runAsync);
    }

    public void initializeMetrics(final int serviceId) {
        if (serviceId > 0 && metricsManager == null) {
            metricsManager = new MetricsManager(plugin, serviceId, platformType);
            logger.info("Metrics initialized with service ID: " + serviceId);
        }
    }

    public void initializePlaceholders(final @NotNull String identifier) {
        if (placeholderManager == null) {
            placeholderManager = new PlaceholderManager(plugin, identifier);
            placeholderManager.register();
            logger.info("PlaceholderAPI integration initialized");
        }
    }

    public void detectPremiumVersion(final @NotNull Class<?> resourceClass, final @NotNull String resourcePath) {
        if (resourceClass.getClassLoader().getResource(resourcePath) != null) {
            premiumVersion = true;
            logger.info("Premium version detected");
        }
    }

    public void shutdown() {
        logger.info("Shutting down RPlatform");
        
        if (placeholderManager != null) {
            placeholderManager.unregister();
        }
        
        platformAPI.close();
        logger.close();
    }

    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }

    public @NotNull PlatformType getPlatformType() {
        return platformType;
    }

    public @NotNull PlatformAPI getPlatformAPI() {
        return platformAPI;
    }

    public @NotNull ISchedulerAdapter getScheduler() {
        return scheduler;
    }

    public @NotNull ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public @NotNull PlatformLogger getLogger() {
        return logger;
    }

    public @NotNull CommandUpdater getCommandUpdater() {
        return commandUpdater;
    }

    public @NotNull TranslationManager getTranslationManager() {
        return translationManager;
    }

    public boolean isPremiumVersion() {
        return premiumVersion;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    /**
     * Initializes database resources for the platform.
     * <p>
     * Creates the database folder and copies the Hibernate properties file if necessary.
     * Initializes the JPA {@link EntityManagerFactory} and logs the result.
     * </p>
     */
    private void initializeDatabaseResources() {

        File databaseFolder = new File(this.plugin.getDataFolder(), "database");
        if (
                databaseFolder.exists() || databaseFolder.mkdirs()
        ) {
            this.plugin.saveResource(
                    "database/hibernate.properties",
                    false
            );
            File hiberateFile = new File(databaseFolder + "/hibernate.properties");
            
            this.entityManagerFactory = new JEHibernate(hiberateFile.getPath()).getEntityManagerFactory();

            CentralLogger.getLogger(RPlatform.class.getName()).log(
                    Level.INFO,
                    "Database resources initialized successfully."
            );
        }
    }
}
