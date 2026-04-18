package de.jexcellence.economy;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry;
import de.jexcellence.economy.api.EconomyProvider;
import de.jexcellence.economy.command.CurrencyArgumentType;
import de.jexcellence.economy.command.R18nCommandMessages;
import de.jexcellence.economy.command.admin.economy.EconomyHandler;
import de.jexcellence.economy.command.player.currencies.CurrenciesHandler;
import de.jexcellence.economy.command.player.currency.BalanceHandler;
import de.jexcellence.economy.command.player.currencylog.CurrencyLogHandler;
import de.jexcellence.economy.command.player.pay.PayHandler;
import de.jexcellence.economy.database.repository.AccountRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.PlayerRepository;
import de.jexcellence.economy.database.repository.TransactionLogRepository;
import de.jexcellence.economy.service.EconomyService;
import de.jexcellence.economy.service.TransactionLogger;
import de.jexcellence.economy.vault.VaultProvider;
import de.jexcellence.economy.view.CurrencyCreateView;
import de.jexcellence.economy.view.CurrencyDeleteView;
import de.jexcellence.economy.view.CurrencyDetailView;
import de.jexcellence.economy.view.CurrencyEditView;
import de.jexcellence.economy.view.CurrencyFieldInputView;
import de.jexcellence.economy.view.CurrencyOverviewView;
import de.jexcellence.economy.view.LeaderboardView;
import de.jexcellence.jehibernate.core.JEHibernate;
import de.jexcellence.jexplatform.JExPlatform;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.logging.LogLevel;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Core API entry point for JExEconomy. Manages plugin lifecycle, database,
 * currency caching, and service registration across Free and Premium editions.
 *
 * <p>Each edition subclass only provides {@link #metricsId()}. All wiring
 * is handled here: JExPlatform for infrastructure, JEHibernate for the
 * data layer, and EconomyService for the public API.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public abstract class JExEconomy {

    private final JavaPlugin plugin;
    private final String edition;

    private JExPlatform platform;
    private JEHibernate jeHibernate;
    private EconomyService economyService;
    private TransactionLogger transactionLogger;
    private TransactionLogRepository transactionLogRepo;
    private VaultProvider vaultProvider;
    private ViewFrame viewFrame;
    private JExLogger logger;

    protected JExEconomy(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Called during server load phase. Builds the platform (synchronous components
     * become available immediately).
     */
    public void onLoad() {
        platform = JExPlatform.builder(plugin)
                .withLogLevel(LogLevel.INFO)
                .enableTranslations("en_US")
                .enableMetrics(metricsId())
                .build();

        logger = platform.logger();
        logger.info("Loading JExEconomy {} Edition", edition);
    }

    /**
     * Enables the economy plugin: initializes database, repositories, services,
     * Vault integration, and auto-discovers commands and listeners.
     */
    public void onEnable() {
        logger.info("Enabling JExEconomy {} Edition...", edition);

        // Capture the main-thread context classloader NOW.
        // JEDependency.initializeWithRemapping() sets the dependency classloader as the
        // context classloader of the main thread during onLoad(). ForkJoinPool worker
        // threads inherit the JVM default (system classloader) and cannot see injected
        // libraries. Propagating the captured loader fixes that.
        final ClassLoader dependencyClassLoader = Thread.currentThread().getContextClassLoader();

        platform.initialize()
                .thenRun(() -> {
                    Thread.currentThread().setContextClassLoader(dependencyClassLoader);
                    initializeDatabase();
                    initializeServices();
                    registerIntegrations();
                    registerViews();
                    registerCommands();
                    logger.info("JExEconomy {} Edition enabled", edition);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to enable JExEconomy ({}): {}", edition, ex.getMessage());
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    return null;
                });
    }

    /**
     * Disables the economy plugin, unregistering services and releasing resources.
     */
    public void onDisable() {
        unregisterIntegrations();
        if (jeHibernate != null) jeHibernate.close();
        if (platform != null) platform.shutdown();
        logger.info("JExEconomy {} Edition disabled", edition);
    }

    /**
     * Returns the bStats service ID for this edition.
     *
     * @return the metrics service ID, or {@code 0} if metrics are disabled
     */
    protected abstract int metricsId();

    // ── Initialization pipeline ─────────────────────────────────────────────────

    private void initializeDatabase() {
        // Copy default config files to the data folder if they don't already exist
        saveDefaultResource("database/hibernate.properties");
        saveDefaultResource("database/log4j.properties");

        jeHibernate = JEHibernate.builder()
                .configuration(config -> config.fromProperties(
                        de.jexcellence.jehibernate.config.PropertyLoader.load(
                                plugin.getDataFolder(), "database", "hibernate.properties")))
                .scanPackages("de.jexcellence.economy.database")
                .build();

        logger.info("Database initialized");
    }

    /**
     * Saves a resource from the JAR to the plugin data folder if it does not already exist.
     *
     * @param resourcePath the resource path relative to the JAR root (use forward slashes)
     */
    private void saveDefaultResource(@NotNull String resourcePath) {
        var target = new java.io.File(plugin.getDataFolder(), resourcePath.replace('/', java.io.File.separatorChar));
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private void initializeServices() {
        var repos = jeHibernate.repositories();
        var currencyRepo = repos.get(CurrencyRepository.class);
        var playerRepo = repos.get(PlayerRepository.class);
        var accountRepo = repos.get(AccountRepository.class);
        transactionLogRepo = repos.get(TransactionLogRepository.class);

        transactionLogger = new TransactionLogger(transactionLogRepo, logger);
        economyService = new EconomyService(
                currencyRepo, playerRepo, accountRepo,
                transactionLogger, logger, platform.scheduler(), plugin);

        economyService.loadCurrencies().join();
        economyService.seedDefaultCurrencyIfEmpty();
        logger.info("Economy service ready — {} currencies loaded",
                economyService.getAllCurrencies().size());

        Bukkit.getServicesManager().register(
                EconomyProvider.class, economyService, plugin, ServicePriority.Normal);
        Bukkit.getServicesManager().register(
                EconomyService.class, economyService, plugin, ServicePriority.Normal);
    }

    private void registerIntegrations() {
        // Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultProvider = new VaultProvider(economyService);
            vaultProvider.register(plugin);
            logger.info("Registered as Vault economy provider");
        } else {
            logger.info("Vault not found — skipping provider registration");
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerViews() {
        viewFrame = ViewFrame
                .create(plugin)
                .install(AnvilInputFeature.AnvilInput)
                .with(
                        new CurrencyOverviewView(),
                        new CurrencyDetailView(),
                        new CurrencyCreateView(),
                        new CurrencyEditView(),
                        new CurrencyDeleteView(),
                        new CurrencyFieldInputView(),
                        new LeaderboardView()
                )
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                    config.interactionDelay(Duration.ofMillis(100));
                })
                .disableMetrics()
                .register();
        logger.info("ViewFrame registered with {} views", 7);
    }

    private void registerCommands() {
        var factory = new CommandFactory(plugin, this);

        // Shared argument type registry — defaults + the plugin-specific "currency" type.
        var registry = ArgumentTypeRegistry.defaults()
                .register(CurrencyArgumentType.of(economyService));

        // Shared i18n bridge — all framework & plugin keys route through R18nManager.
        var messages = new R18nCommandMessages();

        // Register each YAML tree against its handler map.
        factory.registerTree("commands/pay.yml",
                new PayHandler(economyService).handlerMap(), messages, registry);
        factory.registerTree("commands/balance.yml",
                new BalanceHandler(economyService).handlerMap(), messages, registry);
        factory.registerTree("commands/currencies.yml",
                new CurrenciesHandler(this).handlerMap(), messages, registry);
        factory.registerTree("commands/currencylog.yml",
                new CurrencyLogHandler(this).handlerMap(), messages, registry);
        factory.registerTree("commands/economy.yml",
                new EconomyHandler(economyService).handlerMap(), messages, registry);

        // Still let JExCommand auto-register any listener classes under the plugin package.
        factory.registerAllCommandsAndListeners();
    }

    private void unregisterIntegrations() {
        if (vaultProvider != null) {
            Bukkit.getServicesManager().unregister(Economy.class, vaultProvider);
            vaultProvider = null;
        }
        if (economyService != null) {
            Bukkit.getServicesManager().unregister(EconomyProvider.class, economyService);
            Bukkit.getServicesManager().unregister(EconomyService.class, economyService);
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────────

    /** Returns the owning Bukkit plugin. */
    public @NotNull JavaPlugin getPlugin() { return plugin; }

    /** Returns the edition name (Free or Premium). */
    public @NotNull String getEdition() { return edition; }

    /** Returns the platform instance for scheduling, logging, and translations. */
    public @NotNull JExPlatform platform() { return platform; }

    /** Returns the economy service. */
    public @NotNull EconomyService economyService() { return economyService; }

    /** Returns the transaction logger. */
    public @NotNull TransactionLogger transactionLogger() { return transactionLogger; }

    /** Returns the transaction log repository. */
    public @NotNull TransactionLogRepository transactionLogRepo() { return transactionLogRepo; }

    /** Returns the platform logger. */
    public @NotNull JExLogger logger() { return logger; }

    /** Returns the view frame for opening GUI views. */
    public @NotNull ViewFrame viewFrame() { return viewFrame; }
}
