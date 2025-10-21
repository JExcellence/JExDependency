package de.jexcellence.economy;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.currency.*;
import de.jexcellence.economy.currency.anvil.*;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import de.jexcellence.economy.migrate.VaultMigrationManager;
import de.jexcellence.economy.service.CurrencyLogService;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * JExEconomyImpl - Currency management implementation using modern startup style.
 *
 * @version 2.0.1
 * @author
 *     JExcellence
 */
public class JExEconomyImpl extends AbstractPluginDelegate<JExEconomy> {

    private static final String EDITION = "Standard";

    // Core
    private final ExecutorService asyncExecutorService = Executors.newFixedThreadPool(5);
    private RPlatform platformAbstraction;

    // Caches
    private final Map<Long, Currency> currencyCache = new LinkedHashMap<>();

    // External services/adapters
    private CurrencyAdapter externalCurrencyAdapter;

    // Persistence
    private UserCurrencyRepository playerCurrencyRepository;
    private CurrencyRepository currencyDataRepository;
    private UserRepository playerDataRepository;
    private CurrencyLogRepository currencyLogRepository;

    // Services
    private VaultMigrationManager vaultMigrationManager;
    private CurrencyLogService logService;

    // Commands/UI
    private CommandFactory commandRegistrationFactory;
    private ViewFrame inventoryViewFramework;

    public JExEconomyImpl(@NotNull JExEconomy plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            CentralLogger.initialize(this.getPlugin());
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO, getStartupMessage());

            // Platform
            this.platformAbstraction = new RPlatform(this.getPlugin());
            this.platformAbstraction.initialize();

            // Register adapter service (defer to onEnable finalization)
            this.externalCurrencyAdapter = new CurrencyAdapter(this);
            registerServices();

            // Migration manager (constructed early to allow commands to use it later)
            this.vaultMigrationManager = new VaultMigrationManager(this);

            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO,
                    "Initialization started - core components loaded (" + EDITION + ")");
        } catch (final Exception ex) {
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.SEVERE,
                    "Failed to load JExEconomy (" + EDITION + ")", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onEnable() {
        try {
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO,
                    "onEnable() - starting systems (" + EDITION + ")");

            initializeCommandSystem();
            initializeDatabaseRepositories();
            initializeUserInterfaceFramework();
            loadCurrencyDataIntoCache();

            this.logService = new CurrencyLogService(
                    this.currencyLogRepository,
                    this.asyncExecutorService
            );

            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO,
                    "JExEconomy enabled successfully (" + EDITION + ")");
        } catch (final Exception ex) {
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.SEVERE,
                    "Error during onEnable (" + EDITION + ")", ex);
            // Fail-safe: disable plugin if startup failed
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
        }
    }

    @Override
    public void onDisable() {
        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO,
                "onDisable() - shutting down (" + EDITION + ")");

        try {
            unregisterServices();
        } catch (final Exception ex) {
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.WARNING,
                    "Error unregistering services during shutdown", ex);
        }

        if (!this.asyncExecutorService.isShutdown()) {
            this.asyncExecutorService.shutdown();
        }

        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO,
                "JExEconomy disabled (" + EDITION + ")");
    }

    // --- getters ---

    public @NotNull ExecutorService getExecutor() {
        return this.asyncExecutorService;
    }

    public @NotNull RPlatform getPlatform() {
        return this.platformAbstraction;
    }

    public @NotNull CommandFactory getCommandFactory() {
        return this.commandRegistrationFactory;
    }

    public @NotNull CurrencyRepository getCurrencyRepository() {
        return this.currencyDataRepository;
    }

    public @NotNull UserRepository getUserRepository() {
        return this.playerDataRepository;
    }

    public @NotNull UserCurrencyRepository getUserCurrencyRepository() {
        return this.playerCurrencyRepository;
    }

    public @NotNull Map<Long, Currency> getCurrencies() {
        return this.currencyCache;
    }

    public @NotNull ViewFrame getViewFrame() {
        return this.inventoryViewFramework;
    }

    public @NotNull CurrencyAdapter getCurrencyAdapter() {
        return this.externalCurrencyAdapter;
    }

    public CurrencyLogService getLogService() {
        return this.logService;
    }

    public CurrencyLogRepository getCurrencyLogRepository() {
        return this.currencyLogRepository;
    }

    public VaultMigrationManager getVaultMigrationManager() {
        return this.vaultMigrationManager;
    }

    // --- initialization stages ---

    private void initializeCommandSystem() {
        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO, "Initializing command system...");
        this.commandRegistrationFactory = new CommandFactory(this.getPlugin());
        this.commandRegistrationFactory.registerAllCommandsAndListeners();
    }

    private void initializeDatabaseRepositories() {
        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO, "Initializing database repositories...");

        this.playerDataRepository = new UserRepository(
                this.asyncExecutorService,
                this.platformAbstraction.getEntityManagerFactory()
        );

        this.currencyDataRepository = new CurrencyRepository(
                this.asyncExecutorService,
                this.platformAbstraction.getEntityManagerFactory()
        );

        this.playerCurrencyRepository = new UserCurrencyRepository(
                this.asyncExecutorService,
                this.platformAbstraction.getEntityManagerFactory()
        );

        this.currencyLogRepository = new CurrencyLogRepository(
                this.asyncExecutorService,
                this.platformAbstraction.getEntityManagerFactory()
        );
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initializeUserInterfaceFramework() {
        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO, "Initializing user interface framework...");

        this.inventoryViewFramework = ViewFrame
                .create(this.getPlugin())
                .install(AnvilInputFeature.AnvilInput)
                .defaultConfig(viewConfiguration -> {
                    viewConfiguration.cancelOnClick();
                    viewConfiguration.cancelOnDrag();
                    viewConfiguration.cancelOnDrop();
                    viewConfiguration.cancelOnPickup();
                    viewConfiguration.interactionDelay(Duration.ofMillis(100));
                })
                .with(
                        new ConfirmationView(),
                        new CurrenciesOverviewView(),
                        new CurrenciesCreatingView(),
                        new CurrencyIconAnvilView(),
                        new CurrencyDetailView(),
                        new CurrencyLeaderboardView(),
                        new CurrenciesActionOverviewView(),
                        new CurrencyIdentifierAnvilView(),
                        new CurrencySymbolAnvilView(),
                        new CurrencyPrefixAnvilView(),
                        new CurrencySuffixAnvilView(),
                        new CurrencyDeletionView(),
                        new CurrencyEditingView(),
                        new CurrencyPropertiesEditingView()
                )
                .disableMetrics()
                .register();
    }

    private void loadCurrencyDataIntoCache() {
        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(Level.INFO, "Loading currency data into cache...");

        this.currencyDataRepository.findAllAsync(0, 128)
                .thenAcceptAsync(this::populateCurrencyCache, this.asyncExecutorService)
                .exceptionally(this::handleCurrencyLoadingError);
    }

    private void populateCurrencyCache(final @Nullable List<Currency> loadedCurrencies) {
        if (loadedCurrencies == null) {
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(
                    Level.WARNING, "Currency loading returned null - cache remains empty"
            );
            return;
        }

        loadedCurrencies.forEach(currencyEntity ->
                this.currencyCache.put(currencyEntity.getId(), currencyEntity)
        );

        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(
                Level.INFO,
                String.format("Successfully loaded %d currencies into memory cache", loadedCurrencies.size())
        );
    }

    private @Nullable Void handleCurrencyLoadingError(final @NotNull Throwable cacheLoadingError) {
        CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(
                Level.SEVERE, "Failed to load currencies into cache during initialization", cacheLoadingError
        );
        return null;
    }

    // --- service registration ---

    private void registerServices() {
        if (this.externalCurrencyAdapter != null) {
            Bukkit.getServer().getServicesManager().register(
                    CurrencyAdapter.class,
                    this.externalCurrencyAdapter,
                    this.getPlugin(),
                    ServicePriority.Normal
            );
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(
                    Level.INFO, "Registered CurrencyAdapter provider (priority NORMAL)"
            );
        }
    }

    private void unregisterServices() {
        try {
            // Unregister only if our instance is the provider to avoid unregistering others
            final var registration = Bukkit.getServer().getServicesManager().getRegistration(CurrencyAdapter.class);
            if (registration != null && registration.getProvider() == this.externalCurrencyAdapter) {
                Bukkit.getServer().getServicesManager().unregister(CurrencyAdapter.class, this.externalCurrencyAdapter);
                CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(
                        Level.INFO, "Unregistered CurrencyAdapter provider"
                );
            }
        } catch (final Exception ex) {
            CentralLogger.getLogger(JExEconomyImpl.class.getName()).log(
                    Level.WARNING, "Error while unregistering CurrencyAdapter service", ex
            );
        }
    }

    // --- startup message (newer, concise language style) ---

    protected @NotNull String getStartupMessage() {
        return """
            ===============================================================================================
             __        ___      ______ _____ _   _  ______ _____ ___   _   _  _____ __  __  _   _  __  __
             \\ \\      / / |    |  ____/ ____| \\ | |/ __ \\_   _|__ \\ | \\ | |/ ____|  \\/  || \\ | ||  \\/  |
              \\ \\ /\\ / /| |    | |__ | |    |  \\| | |  | || |    ) ||  \\| | |    | \\  / ||  \\| || \\  / |
               \\ V  V / | |    |  __|| |    | . ` | |  | || |   / / | . ` | |    | |\\/| || . ` || |\\/| |
                \\_/\\_/  | |____| |___| |____| |\\  | |__| || |_ / /_ | |\\  | |____| |  | || |\\  || |  | |
                        |______|______\\_____|_| \\_|\\____/_____|____||_| \\_|\\_____|_|  |_||_| \\_||_|  |_|
            
                              JExEconomy - """ + EDITION + """
            ===============================================================================================
            Platform: RPlatform initialized
            Language: Polyglot i18n
            Adventure Components: Enabled
            Dependencies: Managed by JEDependency
            ===============================================================================================
            """;
    }
}