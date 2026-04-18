package de.jexcellence.jexplatform;

import de.jexcellence.jexplatform.database.DatabaseBridge;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.logging.LogLevel;
import de.jexcellence.jexplatform.metrics.MetricsBridge;
import de.jexcellence.jexplatform.requirement.CoreRequirementTypes;
import de.jexcellence.jexplatform.requirement.RequirementRegistry;
import de.jexcellence.jexplatform.requirement.RequirementService;
import de.jexcellence.jexplatform.requirement.lifecycle.LifecycleRegistry;
import de.jexcellence.jexplatform.requirement.metrics.RequirementMetrics;
import de.jexcellence.jexplatform.reward.CoreRewardTypes;
import de.jexcellence.jexplatform.reward.RewardRegistry;
import de.jexcellence.jexplatform.reward.RewardService;
import de.jexcellence.jexplatform.reward.lifecycle.RewardLifecycleRegistry;
import de.jexcellence.jexplatform.reward.metrics.RewardMetrics;
import de.jexcellence.jexplatform.scheduler.PlatformScheduler;
import de.jexcellence.jexplatform.server.PlatformApi;
import de.jexcellence.jexplatform.server.ServerDetector;
import de.jexcellence.jexplatform.server.ServerType;
import de.jexcellence.jexplatform.service.ServiceRegistry;
import de.jexcellence.jexplatform.translation.TranslationBridge;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for JExPlatform — lean plugin infrastructure for
 * Spigot, Paper, and Folia.
 *
 * <p>No static singleton. Each plugin holds its own instance:
 *
 * <pre>{@code
 * var platform = JExPlatform.builder(plugin)
 *     .withLogLevel(LogLevel.INFO)
 *     .enableFileLogging()
 *     .enableDatabase()
 *     .enableTranslations("en_US", "de_DE", "es_ES")
 *     .enableMetrics(12345)
 *     .build();
 *
 * platform.initialize().thenRun(() -> log.info("Ready"));
 *
 * // Access
 * platform.api().sendMessage(player, component);
 * platform.scheduler().runAsync(() -> heavyWork());
 * platform.services().bind(MyService.class, myService);
 * platform.logger().info("Loaded {} currencies in {}ms", count, elapsed);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class JExPlatform {

    private final PlatformContext context;

    // Optional bridges (null when disabled)
    private final @Nullable TranslationBridge translations;
    private final @Nullable MetricsBridge metrics;
    private final boolean databaseEnabled;

    // Requirement/reward systems (null when disabled)
    private final @Nullable RequirementRegistry requirementRegistry;
    private final @Nullable RequirementService requirementService;
    private final @Nullable RewardRegistry rewardRegistry;
    private final @Nullable RewardService rewardService;

    // Mutable async state
    private volatile @Nullable EntityManagerFactory entityManagerFactory;

    private JExPlatform(
            @NotNull PlatformContext context,
            @Nullable TranslationBridge translations,
            @Nullable MetricsBridge metrics,
            boolean databaseEnabled,
            @Nullable RequirementRegistry requirementRegistry,
            @Nullable RequirementService requirementService,
            @Nullable RewardRegistry rewardRegistry,
            @Nullable RewardService rewardService
    ) {
        this.context = context;
        this.translations = translations;
        this.metrics = metrics;
        this.databaseEnabled = databaseEnabled;
        this.requirementRegistry = requirementRegistry;
        this.requirementService = requirementService;
        this.rewardRegistry = rewardRegistry;
        this.rewardService = rewardService;
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    /**
     * Initializes async components (database, translations).
     *
     * <p>Synchronous components (logger, scheduler, API, services, metrics)
     * are already available after {@link Builder#build()}.
     *
     * @return future that completes when all async initialization finishes
     */
    public @NotNull CompletableFuture<Void> initialize() {
        var log = context.logger();
        var futures = new ArrayList<CompletableFuture<?>>();

        if (databaseEnabled) {
            futures.add(DatabaseBridge.initialize(context.plugin(), log)
                    .thenAccept(emf -> {
                        this.entityManagerFactory = emf;
                        log.info("Database initialized");
                    }));
        }

        if (translations != null) {
            futures.add(translations.initialize());
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * Shuts down all platform components.
     *
     * <p>Closes the entity manager factory, translation system, and all loggers.
     * Call this from {@code onDisable()}.
     */
    public void shutdown() {
        var log = context.logger();
        log.info("Shutting down...");

        if (translations != null) {
            translations.shutdown();
        }

        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
            log.debug("EntityManagerFactory closed");
        }

        context.services().clear();
        JExLogger.closeAll();
    }

    // ── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the immutable platform context containing core components.
     *
     * @return the platform context
     */
    public @NotNull PlatformContext context() {
        return context;
    }

    /**
     * Returns the platform-agnostic messaging and item API.
     *
     * @return the platform API
     */
    public @NotNull PlatformApi api() {
        return context.api();
    }

    /**
     * Returns the cross-platform task scheduler.
     *
     * @return the platform scheduler
     */
    public @NotNull PlatformScheduler scheduler() {
        return context.scheduler();
    }

    /**
     * Returns the SLF4J-style logger.
     *
     * @return the plugin logger
     */
    public @NotNull JExLogger logger() {
        return context.logger();
    }

    /**
     * Returns the thread-safe service registry.
     *
     * @return the service registry
     */
    public @NotNull ServiceRegistry services() {
        return context.services();
    }

    /**
     * Returns the detected server type.
     *
     * @return the server type
     */
    public @NotNull ServerType serverType() {
        return context.serverType();
    }

    /**
     * Returns the entity manager factory if database was enabled and initialized.
     *
     * @return the EMF, or empty if database is disabled or not yet initialized
     */
    public @NotNull Optional<EntityManagerFactory> entityManagerFactory() {
        return Optional.ofNullable(entityManagerFactory);
    }

    /**
     * Returns the translation bridge if translations were enabled.
     *
     * @return the translation bridge, or empty
     */
    public @NotNull Optional<TranslationBridge> translations() {
        return Optional.ofNullable(translations);
    }

    /**
     * Returns the metrics bridge if bStats was available and enabled.
     *
     * @return the metrics bridge, or empty
     */
    public @NotNull Optional<MetricsBridge> metrics() {
        return Optional.ofNullable(metrics);
    }

    /**
     * Returns the requirement registry if requirements were enabled.
     *
     * @return the requirement registry, or empty
     */
    public @NotNull Optional<RequirementRegistry> requirementRegistry() {
        return Optional.ofNullable(requirementRegistry);
    }

    /**
     * Returns the requirement service if requirements were enabled.
     *
     * @return the requirement service, or empty
     */
    public @NotNull Optional<RequirementService> requirementService() {
        return Optional.ofNullable(requirementService);
    }

    /**
     * Returns the reward registry if rewards were enabled.
     *
     * @return the reward registry, or empty
     */
    public @NotNull Optional<RewardRegistry> rewardRegistry() {
        return Optional.ofNullable(rewardRegistry);
    }

    /**
     * Returns the reward service if rewards were enabled.
     *
     * @return the reward service, or empty
     */
    public @NotNull Optional<RewardService> rewardService() {
        return Optional.ofNullable(rewardService);
    }

    // ── Builder ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new builder for the given plugin.
     *
     * @param plugin the owning Bukkit plugin
     * @return a new builder instance
     */
    public static @NotNull Builder builder(@NotNull JavaPlugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Fluent builder for configuring and constructing a {@link JExPlatform} instance.
     */
    public static final class Builder {

        private final JavaPlugin plugin;
        private LogLevel logLevel = LogLevel.INFO;
        private boolean fileLogging;
        private boolean database;
        private boolean translationsEnabled;
        private String defaultLocale = "en_US";
        private String[] extraLocales = new String[0];
        private int metricsServiceId = -1;
        private boolean requirementsEnabled;
        private boolean rewardsEnabled;

        private Builder(@NotNull JavaPlugin plugin) {
            this.plugin = plugin;
        }

        /**
         * Sets the minimum console log level.
         *
         * @param level the minimum log level (default {@link LogLevel#INFO})
         * @return this builder
         */
        public @NotNull Builder withLogLevel(@NotNull LogLevel level) {
            this.logLevel = level;
            return this;
        }

        /**
         * Enables rotating file logging in the plugin's data folder.
         *
         * @return this builder
         */
        public @NotNull Builder enableFileLogging() {
            this.fileLogging = true;
            return this;
        }

        /**
         * Enables JEHibernate database initialization.
         *
         * @return this builder
         */
        public @NotNull Builder enableDatabase() {
            this.database = true;
            return this;
        }

        /**
         * Enables JExTranslate internationalization.
         *
         * @param defaultLocale the fallback locale (e.g. {@code "en_US"})
         * @param extraLocales  additional supported locales
         * @return this builder
         */
        public @NotNull Builder enableTranslations(@NotNull String defaultLocale,
                                                   @NotNull String... extraLocales) {
            this.translationsEnabled = true;
            this.defaultLocale = defaultLocale;
            this.extraLocales = extraLocales;
            return this;
        }

        /**
         * Enables bStats metrics with the given service ID.
         *
         * @param serviceId bStats service ID from the dashboard
         * @return this builder
         */
        public @NotNull Builder enableMetrics(int serviceId) {
            this.metricsServiceId = serviceId;
            return this;
        }

        /**
         * Enables the requirement system with all core requirement types.
         *
         * @return this builder
         */
        public @NotNull Builder enableRequirements() {
            this.requirementsEnabled = true;
            return this;
        }

        /**
         * Enables the reward system with all core reward types.
         *
         * @return this builder
         */
        public @NotNull Builder enableRewards() {
            this.rewardsEnabled = true;
            return this;
        }

        /**
         * Builds the platform instance, creating all synchronous components.
         *
         * <p>After calling this, the logger, scheduler, API, services, and metrics
         * are immediately available. Call {@link JExPlatform#initialize()} to start
         * async components (database, translations).
         *
         * @return a configured but not yet fully initialized platform
         */
        public @NotNull JExPlatform build() {
            var name = plugin.getName();

            // Logger
            var log = JExLogger.of(name, logLevel, fileLogging,
                    plugin.getDataFolder().toPath());
            log.info("Initializing {} platform...", name);

            // Server detection
            var serverType = ServerDetector.detect();
            log.info("Detected server: {}", serverType.name());

            // Core components
            var scheduler = PlatformScheduler.create(plugin, serverType);
            var api = PlatformApi.create(plugin, serverType);
            var services = new ServiceRegistry(plugin);

            var context = new PlatformContext(plugin, serverType, api, scheduler, log, services);

            // Optional: translations
            TranslationBridge translations = null;
            if (translationsEnabled) {
                translations = TranslationBridge.create(plugin, log, defaultLocale, extraLocales);
            }

            // Optional: metrics
            MetricsBridge metrics = null;
            if (metricsServiceId > 0) {
                metrics = MetricsBridge.create(plugin, metricsServiceId, serverType, log)
                        .orElse(null);
            }

            // Optional: requirements
            RequirementRegistry reqRegistry = null;
            RequirementService reqService = null;
            if (requirementsEnabled) {
                reqRegistry = new RequirementRegistry(log);
                CoreRequirementTypes.registerAll(reqRegistry);
                var reqLifecycle = new LifecycleRegistry();
                var reqMetrics = new RequirementMetrics();
                reqService = new RequirementService(reqRegistry, log, reqLifecycle, reqMetrics);
                log.info("Requirement system enabled ({} types)", reqRegistry.size());
            }

            // Optional: rewards
            RewardRegistry rwdRegistry = null;
            RewardService rwdService = null;
            if (rewardsEnabled) {
                rwdRegistry = new RewardRegistry(log);
                CoreRewardTypes.registerAll(rwdRegistry);
                var rwdLifecycle = new RewardLifecycleRegistry();
                var rwdMetrics = new RewardMetrics();
                rwdService = new RewardService(log, rwdLifecycle, rwdMetrics);
                log.info("Reward system enabled ({} types)", rwdRegistry.size());
            }

            return new JExPlatform(context, translations, metrics, database,
                    reqRegistry, reqService, rwdRegistry, rwdService);
        }
    }
}
