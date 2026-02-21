package de.jexcellence.economy;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.view.ConfirmationView;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JExEconomy {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExEconomy");

    private final JavaPlugin plugin;
    private final String edition;
    private final ExecutorService executor = createExecutor();
    private final RPlatform platform;

    private volatile CompletableFuture<Void> enableFuture;
    private boolean isDisabling;
    private boolean postEnableCompleted;

    private final Map<Long, Currency> currencyCache = new LinkedHashMap<>();

    private CurrencyAdapter currencyAdapter;
    private UserCurrencyRepository userCurrencyRepository;
    private CurrencyRepository currencyRepository;
    private UserRepository userRepository;
    private CurrencyLogRepository currencyLogRepository;
    private VaultMigrationManager vaultMigrationManager;
    private CurrencyLogService logService;
    private CommandFactory commandFactory;
    private ViewFrame viewFrame;

    public JExEconomy(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
        this.platform = new RPlatform(plugin);
    }

    public void onLoad() {
        try {
            LOGGER.info("Loading JExEconomy " + edition + " Edition");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load JExEconomy", e);
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
                        registerServices();
                        performPostEnableSync();
                    } catch (Throwable t) {
                        throw new CompletionException(t);
                    }
                }))
                .exceptionally(ex -> {
                    runSync(() -> {
                        LOGGER.log(Level.SEVERE, "Failed to enable JExEconomy (" + edition + ")", ex);
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
        unregisterServices();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        LOGGER.info("JExEconomy " + edition + " Edition disabled");
    }

    @NotNull
    protected abstract String getStartupMessage();

    protected abstract int getMetricsId();

    @NotNull
    protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

    private CompletableFuture<Void> performCoreEnableAsync() {
        return platform.initialize()
                .thenCompose(v -> runSync(() -> {
                    initializeRepositories();
                    currencyAdapter = new CurrencyAdapter(this);
                    vaultMigrationManager = new VaultMigrationManager(this);
                }))
                .thenRun(this::loadCurrenciesAsync);
    }

    private void registerServices() {
        if (currencyAdapter != null) {
            Bukkit.getServer().getServicesManager().register(
                    CurrencyAdapter.class,
                    currencyAdapter,
                    plugin,
                    ServicePriority.Normal
            );
            LOGGER.info("Registered CurrencyAdapter service");
        }
    }

    private void performPostEnableSync() {
        if (postEnableCompleted) {
            LOGGER.warning("Post-enable called more than once");
            return;
        }

        commandFactory = new CommandFactory(plugin, this);
        commandFactory.registerAllCommandsAndListeners();

        initializeViews();
        platform.initializeMetrics(getMetricsId());

        LOGGER.info(getStartupMessage());
        LOGGER.info("JExEconomy " + edition + " Edition enabled successfully!");
        postEnableCompleted = true;
    }

    private void initializeRepositories() {
        var emf = platform.getEntityManagerFactory();
        if (emf == null) {
            LOGGER.warning("EntityManagerFactory not initialized");
            onDisable();
            return;
        }

        userRepository = new UserRepository(executor, emf);
        currencyRepository = new CurrencyRepository(executor, emf);
        userCurrencyRepository = new UserCurrencyRepository(executor, emf);
        currencyLogRepository = new CurrencyLogRepository(executor, emf);
        logService = new CurrencyLogService(currencyLogRepository, executor);
    }

    private void loadCurrenciesAsync() {
        executor.submit(() -> {
            try {
                var currencies = currencyRepository.findAllAsync(0, 128).join();
                if (currencies != null) {
                    currencies.forEach(currency -> currencyCache.put(currency.getId(), currency));
                    LOGGER.info("Loaded " + currencies.size() + " currencies into cache");
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load currencies", ex);
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        ViewFrame frame = ViewFrame
                .create(plugin)
                .install(AnvilInputFeature.AnvilInput)
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
    }

    private void unregisterServices() {
        try {
            var registration = Bukkit.getServer().getServicesManager().getRegistration(CurrencyAdapter.class);
            if (registration != null && registration.getProvider() == currencyAdapter) {
                Bukkit.getServer().getServicesManager().unregister(CurrencyAdapter.class, currencyAdapter);
                LOGGER.info("Unregistered CurrencyAdapter service");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error unregistering services", ex);
        }
    }

    private @NotNull CompletableFuture<Void> runSync(final @NotNull Runnable runnable) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        if (isDisabling) {
            future.cancel(false);
            return future;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
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
    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Nullable
    public String getEdition() {
        return edition;
    }

    @NotNull
    public ExecutorService getExecutor() {
        return executor;
    }

    @NotNull
    public RPlatform getPlatform() {
        return platform;
    }

    @NotNull
    public ViewFrame getViewFrame() {
        return viewFrame;
    }

    @NotNull
    public CommandFactory getCommandFactory() {
        return commandFactory;
    }

    @NotNull
    public CurrencyAdapter getCurrencyAdapter() {
        return currencyAdapter;
    }

    @NotNull
    public CurrencyRepository getCurrencyRepository() {
        return currencyRepository;
    }

    @NotNull
    public UserRepository getUserRepository() {
        return userRepository;
    }

    @NotNull
    public UserCurrencyRepository getUserCurrencyRepository() {
        return userCurrencyRepository;
    }

    @NotNull
    public CurrencyLogRepository getCurrencyLogRepository() {
        return currencyLogRepository;
    }

    @NotNull
    public CurrencyLogService getLogService() {
        return logService;
    }

    @NotNull
    public VaultMigrationManager getVaultMigrationManager() {
        return vaultMigrationManager;
    }

    @NotNull
    public Map<Long, Currency> getCurrencies() {
        return currencyCache;
    }

    public boolean isDisabling() {
        return isDisabling;
    }

    public boolean isPostEnableCompleted() {
        return postEnableCompleted;
    }

    public @Nullable CompletableFuture<Void> getEnableFuture() {
        return enableFuture;
    }

    public boolean isEnabled() {
        return plugin.isEnabled();
    }
}
