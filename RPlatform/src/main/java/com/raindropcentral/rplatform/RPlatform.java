/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform;

import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.integration.geyser.GeyserService;
import com.raindropcentral.rplatform.localization.TranslationManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.logging.PluginLogger;
import com.raindropcentral.rplatform.metrics.BStatsMetrics;
import com.raindropcentral.rplatform.metrics.MetricsManager;
import com.raindropcentral.rplatform.placeholder.PlaceholderManager;
import com.raindropcentral.rplatform.proxy.NoOpProxyService;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.requirement.BuiltInRequirementProvider;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.evaluable.CommandUpdater;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Primary orchestrator for the shared Raindrop platform runtime that binds plugin lifecycle.
 * components, manages async initialization, and exposes integrations such as metrics,
 * placeholders, and database resources.
 *
 * <p>The class coordinates scheduler selection, service registry creation, and platform API
 * detection during construction. {@link #initialize()} must be invoked to asynchronously prepare the
 * translation manager, command updater, and database resources before any dependent feature is
 * consumed.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RPlatform {

    /**
     * Static instance holder for accessing the platform from static contexts.
     * This is set during construction and cleared during shutdown.
     */
    private static RPlatform instance;

    /**
     * Hosting {@link JavaPlugin} providing lifecycle hooks, configuration paths, and scheduler.
     * access for the platform.
     */
    private final JavaPlugin plugin;

    /**
     * Platform type resolved from the running environment used to tailor integrations such as.
     * scheduler adapters and metrics exporters.
     */
    private final PlatformType platformType;

    /**
     * Abstraction over Bukkit/Folia APIs enabling shared command registration, task scheduling, and.
     * shutdown behaviour.
     */
    private final PlatformAPI platformAPI;

    /**
     * Scheduler adapter that runs asynchronous tasks using an implementation suitable for the.
     * detected {@link PlatformType}.
     */
    private final ISchedulerAdapter scheduler;

    /**
     * Registry tracking singleton services that the platform exposes to downstream modules.
     */
    private final ServiceRegistry serviceRegistry;

    /**
     * Platform-aware logger emitting lifecycle and diagnostic messages for both initialization and.
     * shutdown sequences.
     */
    private final PluginLogger logger;
    /**
     * Proxy bridge used by network-aware modules when a proxy coordinator is available.
     */
    private ProxyService proxyService;

    /**
     * Handles command updates for JEx command framework integrations once initialization completes.
     */
    private CommandUpdater commandUpdater;

    /**
     * Provides translation bundles and localization support after asynchronous initialization.
     */
    private TranslationManager translationManager;

    /**
     * Lazily created {@link EntityManagerFactory} used for persistence across platform modules.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Metrics manager created when a valid bStats service identifier is supplied.
     */
    private MetricsManager metricsManager;

    /**
     * Wrapper around PlaceholderAPI for registering and unregistering platform placeholders.
     */
    private PlaceholderManager placeholderManager;

    /**
     * Service for Geyser/Floodgate integration and Bedrock player detection.
     */
    private GeyserService geyserService;

    /**
     * Flag indicating whether premium-only resources were detected on the classpath.
     */
    private boolean premiumVersion;

    /**
     * Flag indicating whether the asynchronous initialization routine completed successfully.
     */
    private boolean initialized;

    /**
     * Creates a new platform orchestrator bound to the provided plugin and resolves the environment.
     * specific adapters required for initialization.
     *
     * @param plugin plugin instance that owns the platform runtime and supplies configuration paths
     *               alongside Bukkit lifecycle callbacks
     */
    public RPlatform(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.platformAPI = PlatformAPIFactory.create(plugin);
        this.scheduler = ISchedulerAdapter.create(plugin, platformType);
        this.serviceRegistry = new ServiceRegistry();
        this.logger = CentralLogger.getLogger(plugin);
        this.proxyService = NoOpProxyService.createDefault();
        this.premiumVersion = false;
        this.initialized = false;
        
        // Set static instance for global access
        instance = this;
    }

    /**
     * Asynchronously initializes translation, command, and database resources required by the.
     * platform. Subsequent invocations no-op once initialization finishes.
     *
     * <p>Lightweight registry and helper setup runs immediately during plugin enable while blocking
     * database bootstrap is dispatched through the scheduler's asynchronous executor. Translation
     * resources are then loaded via their own asynchronous pipeline so Folia region threads never
     * perform JDBC startup work.</p>
     *
     * @return a future that completes when asynchronous resource setup finishes or immediately when
     *         initialization has already been performed
     */
    public @NotNull CompletableFuture<Void> initialize() {
        if (initialized) {
            return CompletableFuture.completedFuture(null);
        }
        logger.info("Preparing platform initialization task");
        logger.info("Initializing RPlatform for " + platformType.name());

        // Initialize requirement system first
        logger.info("Initializing requirement system...");
        BuiltInRequirementProvider.initialize();
        logger.info("Requirement system initialized");

        translationManager = TranslationManager.builder(plugin)
                .defaultLocale("en_US").supportedLocales("de_DE", "en_US")
                .enableMetrics(true)
                .build();

        commandUpdater = new CommandUpdater(plugin);

        return this.scheduler.runAsyncFuture(() -> {
            this.initializeDatabaseResources();

            logger.info("RPlatform initialized successfully");
            initialized = true;
        }).thenCompose(v -> {
            // Initialize translations after platform setup - must be awaited
            logger.info("Initializing translation system...");
            return translationManager.initialize().thenRun(() -> {
                logger.info("Translation system initialized with " + 
                    translationManager.getKeyCount() + " keys in " + 
                    translationManager.getLocaleCount() + " locales");
                
                // Cleanup unsupported files after initialization (if needed)
                // TODO: add actual support of the file deny progress..
                // translationManager.cleanupUnsupportedFiles();
            });
        });
    }

    /**
     * Initializes Geyser/Floodgate integration for Bedrock player detection.
 *
 * <p>This method should be called during plugin initialization if Bedrock support is desired.
     * The service will gracefully handle missing Floodgate installations.
     */
    public void initializeGeyser() {
        if (geyserService == null) {
            geyserService = new GeyserService();
            if (geyserService.isFloodgateAvailable()) {
                logger.info("Geyser/Floodgate integration initialized");
            }
        }
    }

    /**
     * Initializes metrics collection through the {@link MetricsManager} when provided a valid.
     * service identifier.
     *
     * @param serviceId bStats service identifier; values less than or equal to zero are ignored to
     *                  avoid erroneous registrations
     */
    public void initializeMetrics(final int serviceId) {
        if (serviceId > 0 && metricsManager == null) {
            metricsManager = new MetricsManager(plugin, serviceId, platformType);
            logger.info("Metrics initialized with service ID: " + serviceId);
        }
    }

    /**
     * Registers a custom bStats chart when metrics have been initialized.
     *
     * @param chart custom chart to include in future metrics payloads
     */
    public void addMetricsChart(final @NotNull BStatsMetrics.CustomChart chart) {
        if (this.metricsManager != null) {
            this.metricsManager.addCustomChart(chart);
        }
    }

    /**
     * Sets up PlaceholderAPI integration by registering the platform's placeholders under the given.
     * identifier. Subsequent calls are ignored once registration occurs.
     *
     * @param identifier PlaceholderAPI identifier namespace used when registering expansions
     */
    public void initializePlaceholders(final @NotNull String identifier) {
        if (placeholderManager == null) {
            placeholderManager = new PlaceholderManager(plugin, identifier);
            placeholderManager.register();
            logger.info("PlaceholderAPI integration initialized");
        }
    }

    /**
     * Detects whether the premium platform build is available by checking for a marker resource on.
     * the supplied class loader.
     *
     * @param resourceClass class whose class loader should contain the premium marker resource
     * @param resourcePath  path to the premium marker resource to verify
     */
    public void detectPremiumVersion(final @NotNull Class<?> resourceClass, final @NotNull String resourcePath) {
        if (resourceClass.getClassLoader().getResource(resourcePath) != null) {
            premiumVersion = true;
            logger.info("Premium version detected");
        }
    }

    /**
     * Shuts down platform integrations by unregistering placeholders, closing API adapters, and.
     * releasing logging resources.
     */
    public void shutdown() {
        logger.info("Shutting down RPlatform");

        if (placeholderManager != null) {
            placeholderManager.unregister();
        }

        platformAPI.close();
        logger.close();
        
        // Clear static instance
        instance = null;
    }

    /**
     * Gets the global RPlatform instance.
     * 
     * @return the platform instance, or null if not initialized
     */
    public static @Nullable RPlatform getInstance() {
        return instance;
    }

    /**
     * Provides the plugin that owns this platform runtime.
     *
     * @return the hosting plugin instance
     */
    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Retrieves the detected platform type for the running server implementation.
     *
     * @return detected platform type
     */
    public @NotNull PlatformType getPlatformType() {
        return platformType;
    }

    /**
     * Exposes the platform API wrapper used for command and scheduler interactions.
     *
     * @return platform API abstraction instance
     */
    public @NotNull PlatformAPI getPlatformAPI() {
        return platformAPI;
    }

    /**
     * Returns the scheduler adapter responsible for executing asynchronous workloads.
     *
     * @return scheduler adapter resolved for the detected platform type
     */
    public @NotNull ISchedulerAdapter getScheduler() {
        return scheduler;
    }

    /**
     * Supplies the service registry for registering and retrieving shared platform services.
     *
     * @return mutable registry that downstream components can use to store services
     */
    public @NotNull ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Provides the platform-aware logger for emitting diagnostic messages.
     *
     * @return logger backing initialization and shutdown logging
     */
    public @NotNull PluginLogger getLogger() {
        return logger;
    }

    /**
     * Returns the currently configured proxy bridge.
     *
     * @return active proxy bridge implementation
     */
    public @NotNull ProxyService getProxyService() {
        return this.proxyService;
    }

    /**
     * Replaces the active proxy bridge implementation.
     *
     * <p>Passing {@code null} restores the local no-op bridge so standalone Paper installs remain
     * functional.</p>
     *
     * @param proxyService replacement proxy bridge, or {@code null} for no-op fallback
     */
    public void setProxyService(final @Nullable ProxyService proxyService) {
        this.proxyService = proxyService == null ? NoOpProxyService.createDefault() : proxyService;
    }

    /**
     * Gives access to the command updater once initialization completes.
     *
     * @return command updater used for managing JEx command refreshes
     */
    public @NotNull CommandUpdater getCommandUpdater() {
        return commandUpdater;
    }

    /**
     * Retrieves the translation manager created during initialization.
     *
     * @return translation manager enabling localized messaging
     */
    public @NotNull TranslationManager getTranslationManager() {
        return translationManager;
    }

    /**
     * Retrieves the Geyser service for Bedrock player detection.
     *
     * @return the Geyser service, or null if not initialized
     */
    public @Nullable GeyserService getGeyserService() {
        return geyserService;
    }

    /**
     * Indicates whether a premium build marker was found on the classpath.
     *
     * @return {@code true} when premium resources are present; {@code false} otherwise
     */
    public boolean isPremiumVersion() {
        return premiumVersion;
    }

    /**
     * Reports whether the asynchronous initialization routine has completed.
     *
     * @return {@code true} if initialization succeeded previously; {@code false} when initialization
     *         has not yet run
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Provides access to the lazily created {@link EntityManagerFactory} instance.
     *
     * @return entity manager factory used for persistence interactions, or {@code null} when
     *         initialization has not provisioned the resource
     */
    public @Nullable EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    /**
     * Creates the database directory, copies the bundled Hibernate configuration, and builds the.
     * {@link EntityManagerFactory} used by platform modules.
     *
     * <p>The method writes files within the plugin data folder and may throw unchecked exceptions
     * propagated from filesystem operations or {@link JEHibernate} construction. Failures are logged
     * via the {@link CentralLogger} for visibility.</p>
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

            try {
                this.entityManagerFactory = new JEHibernate(hiberateFile.getPath()).getEntityManagerFactory();

                logger.info("Database resources initialized successfully.");
            } catch (Exception exception) {
                this.entityManagerFactory = null;

                logger.severe("Database resources initialization failed.", exception);
            }
        }
    }
}
