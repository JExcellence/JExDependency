package com.raindropcentral.rds;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ServerBank;
import com.raindropcentral.rds.database.repository.RRDSPlayer;
import com.raindropcentral.rds.database.repository.RServerBank;
import com.raindropcentral.rds.database.repository.RShop;
import com.raindropcentral.rds.service.ShopService;
import com.raindropcentral.rds.service.scoreboard.ShopSidebarScoreboardService;
import com.raindropcentral.rds.service.bank.AdminShopServerBankScheduler;
import com.raindropcentral.rds.service.shop.AdminShopRestockScheduler;
import com.raindropcentral.rds.service.shop.ShopBossBarService;
import com.raindropcentral.rds.service.tax.ShopTaxScheduler;
import com.raindropcentral.rds.view.shop.AdminCurrencyView;
import com.raindropcentral.rds.view.shop.ShopBankView;
import com.raindropcentral.rds.view.shop.ShopCustomerView;
import com.raindropcentral.rds.view.shop.ShopEditView;
import com.raindropcentral.rds.view.shop.ShopInputView;
import com.raindropcentral.rds.view.shop.ServerBankView;
import com.raindropcentral.rds.view.shop.ShopAdminView;
import com.raindropcentral.rds.view.shop.ShopConfigView;
import com.raindropcentral.rds.view.shop.ShopItemAdminCommandView;
import com.raindropcentral.rds.view.shop.ShopItemEditView;
import com.raindropcentral.rds.view.shop.ShopLedgerView;
import com.raindropcentral.rds.view.shop.ShopListView;
import com.raindropcentral.rds.view.shop.ShopOverviewView;
import com.raindropcentral.rds.view.shop.ShopResultsView;
import com.raindropcentral.rds.view.shop.ShopSearchView;
import com.raindropcentral.rds.view.shop.ShopStorageView;
import com.raindropcentral.rds.view.shop.ShopStoreCostView;
import com.raindropcentral.rds.view.shop.ShopStoreView;
import com.raindropcentral.rds.view.shop.ShopTrustedView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAvailabilityMinutesAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminResetTimerAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminCommandAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminCommandDelayAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminStockLimitAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemCurrencyTypeAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemValueAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopMaterialSearchAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopPurchaseAmountAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopConfigValueAnvilView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Shared runtime bootstrap for RDS editions.
 *
 * <p>The runtime initializes persistence, views, commands, and economy integration while
 * delegating edition-specific limits to {@link ShopService}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class RDS {

    private static final String VAULT_ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";
    private static final String CONFIG_FOLDER_PATH = "config";
    private static final String CONFIG_FILE_NAME = "config.yml";

    private final JavaPlugin plugin;
    private final String edition;
    private final ShopService shopService;

    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private Object economyInstance;
    private ViewFrame viewFrame;
    private ShopTaxScheduler shopTaxScheduler;
    private ShopBossBarService shopBossBarService;
    private AdminShopRestockScheduler adminShopRestockScheduler;
    private AdminShopServerBankScheduler adminShopServerBankScheduler;
    private ShopSidebarScoreboardService shopSidebarScoreboardService;
    private RRDSPlayer playerRepository;
    private RShop shopRepository;
    private RServerBank serverBankRepository;

    /**
     * Creates a new shared RDS runtime.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used for logs
     * @param shopService edition-specific shop rules
     * @throws NullPointerException if any argument is {@code null}
     */
    public RDS(
        final @NotNull JavaPlugin plugin,
        final @NotNull String edition,
        final @NotNull ShopService shopService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.shopService = Objects.requireNonNull(shopService, "shopService");
    }

    /**
     * Allocates shared platform state before plugin enable.
     */
    public void onLoad() {
        this.getLogger().info("Loading RPlatform for RDS (" + this.edition + ")");
        this.platform = new RPlatform(this.plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes repositories, views, and runtime services.
     */
    public void onEnable() {
        this.getLogger().info("Enabling RDS (" + this.edition + ") Edition");

        this.platform.initialize();
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.scheduler = this.platform.getScheduler();
        this.executor = Executors.newFixedThreadPool(4);
        this.ensureDefaultConfigFile();
        this.getDefaultConfig().logMissingRequirementWarnings(this.getLogger());

        try {
            this.initializeHibernate();
            this.initializeRepositories();
            this.getLogger().info("Hibernate initialized");
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to initialize RDS persistence: " + exception.getMessage());
            this.onDisable();
            return;
        }

        this.initializePlugins();
        this.initializeCommands();
        this.initializeViews();
        this.initializeTaxes();
        this.initializeAdminShopRestocking();
        this.initializeAdminShopServerBankTransfers();
        this.initializeShopBossBar();
        this.initializeShopSidebarScoreboards();

        if (!this.hasValidEconomyAndCurrency()) {
            this.getLogger().warning(
                "No Vault provider or registered JExEconomy currencies are currently available. "
                    + "RDS will remain enabled and currency-backed features will become available when a provider registers."
            );
        }
    }

    /**
     * Shuts down runtime services and releases resources.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDS (" + this.edition + "): closing Hibernate");

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.shopBossBarService != null) {
            this.shopBossBarService.shutdown();
        }

        if (this.shopSidebarScoreboardService != null) {
            this.shopSidebarScoreboardService.shutdown();
        }

        if (this.entityManagerFactory != null) {
            try {
                this.entityManagerFactory.close();
            } catch (final Exception ignored) {
            }
        }
    }

    /**
     * Loads the plugin configuration from disk.
     *
     * @return parsed plugin configuration
     */
    public @NotNull ConfigSection getDefaultConfig() {
        this.ensureDefaultConfigFile();
        try {
            return ConfigSection.fromFile(this.getDefaultConfigFile());
        } catch (final Exception exception) {
            this.getLogger().warning(
                "Failed to parse RDS config from " + this.getDefaultConfigFile().getAbsolutePath() + ": " + exception.getMessage()
            );
            return ConfigSection.fromFile(this.getDefaultConfigFile());
        }
    }

    /**
     * Returns the effective maximum placed shops per player for the active edition.
     *
     * @param config configuration snapshot to evaluate
     * @return edition-aware player shop cap
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public int getMaximumShops(final @NotNull ConfigSection config) {
        return this.shopService.getMaximumShops(config);
    }

    /**
     * Returns the effective maximum placed shops per player using the current config.
     *
     * @return edition-aware player shop cap
     */
    public int getMaximumShops() {
        return this.getMaximumShops(this.getDefaultConfig());
    }

    /**
     * Returns whether the active edition enforces a finite shop cap.
     *
     * @param config configuration snapshot to evaluate
     * @return {@code true} when a finite player shop cap applies
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public boolean hasShopLimit(final @NotNull ConfigSection config) {
        return this.getMaximumShops(config) > 0;
    }

    /**
     * Returns whether the active edition enforces a finite shop cap.
     *
     * @return {@code true} when a finite player shop cap applies
     */
    public boolean hasShopLimit() {
        return this.hasShopLimit(this.getDefaultConfig());
    }

    /**
     * Returns the maximum number of admin shops allowed on the server for the active edition.
     *
     * @return edition-aware admin shop cap, or {@code -1} when unlimited
     */
    public int getMaximumAdminShops() {
        return this.shopService.getMaximumAdminShops();
    }

    /**
     * Returns whether the active edition enforces a finite admin shop cap.
     *
     * @return {@code true} when a finite admin shop cap applies
     */
    public boolean hasAdminShopLimit() {
        return this.getMaximumAdminShops() > 0;
    }

    /**
     * Returns whether the active edition may change plugin-wide RDS config values.
     *
     * @return {@code true} when config changes are allowed
     */
    public boolean canChangeConfigs() {
        return this.shopService.canChangeConfigs();
    }

    /**
     * Returns the edition-specific shop service.
     *
     * @return active shop service
     */
    public @NotNull ShopService getShopService() {
        return this.shopService;
    }

    /**
     * Returns the human-readable edition label used by this runtime.
     *
     * @return edition label
     */
    public @NotNull String getEdition() {
        return this.edition;
    }

    /**
     * Returns the owning Bukkit plugin instance.
     *
     * @return active Bukkit plugin
     */
    public @NotNull JavaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Returns the runtime logger exposed by the owning Bukkit plugin.
     *
     * @return plugin logger
     */
    public @NotNull Logger getLogger() {
        return this.plugin.getLogger();
    }

    /**
     * Returns the plugin data folder.
     *
     * @return plugin data folder
     */
    public @NotNull File getDataFolder() {
        return this.plugin.getDataFolder();
    }

    private @NotNull File getDefaultConfigFile() {
        return new File(new File(this.getDataFolder(), CONFIG_FOLDER_PATH), CONFIG_FILE_NAME);
    }

    private void ensureDefaultConfigFile() {
        final File dataFolder = this.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            this.getLogger().warning("Could not create plugin data folder for config extraction.");
            return;
        }

        final File configFolder = new File(dataFolder, CONFIG_FOLDER_PATH);
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            this.getLogger().warning("Could not create config folder for default config extraction.");
            return;
        }

        final File configFile = new File(configFolder, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            return;
        }

        try {
            this.plugin.saveResource(CONFIG_FOLDER_PATH + "/" + CONFIG_FILE_NAME, false);
        } catch (final IllegalArgumentException exception) {
            this.getLogger().warning("Bundled default config could not be extracted: " + exception.getMessage());
        }
    }

    private void initializePlugins() {
        this.getLogger().info("Registering Vault service");
        new ServiceRegistry().register(
                "net.milkbowl.vault.economy.Economy",
                "TheNewEconomy"
        ).optional().maxAttempts(30).retryDelay(1000).onSuccess(economy -> {
            this.getLogger().info("Vault service initialized");
            this.economyInstance = economy;
        }).onFailure(() -> this.getLogger().info(
                "Vault service not present; continuing without Vault integration")
        ).load();
    }

    private void initializeCommands() {
        final var commandFactory = new CommandFactory(this.plugin, this);
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeRepositories() {
        this.playerRepository = new RRDSPlayer(
                this.executor,
                this.entityManagerFactory,
                RDSPlayer.class,
                RDSPlayer::getIdentifier
        );

        this.shopRepository = new RShop(
                this.executor,
                this.entityManagerFactory,
                Shop.class,
                Shop::getShopLocation
        );

        this.serverBankRepository = new RServerBank(
                this.executor,
                this.entityManagerFactory,
                ServerBank.class,
                ServerBank::getCurrencyType
        );
    }

    @SuppressWarnings("resource")
    private void initializeHibernate() throws IOException {
        final File file = this.getHibernateFile();

        if (!file.exists()) {
            try (InputStream in = this.plugin.getResource("database/hibernate.properties")) {
                if (in == null) {
                    throw new IOException("Missing resource com.raindropcentral.rdt.database/hibernate.properties");
                }
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        this.entityManagerFactory =
                new JEHibernate(file.getAbsolutePath())
                        .getEntityManagerFactory();
    }

    @Contract(" -> new")
    private @NonNull File getHibernateFile() throws IOException {
        final File data = this.getDataFolder();
        if (!data.exists() && !data.mkdirs()) {
            throw new IOException("Could not create data folder");
        }

        final File db = new File(data, "database");
        if (!db.exists() && !db.mkdirs()) {
            throw new IOException("Could not create com.raindropcentral.rdt.database folder");
        }

        return new File(db, "hibernate.properties");
    }

    private void initializeViews() {
        final ViewFrame frame = ViewFrame
                .create(this.plugin)
                .install(AnvilInputFeature.AnvilInput)
                .with(
                    new ShopOverviewView(),
                    new ShopAdminView(),
                    new AdminCurrencyView(),
                    new ShopConfigView(),
                    new ShopBankView(),
                    new ServerBankView(),
                    new ShopSearchView(),
                    new ShopListView(),
                    new ShopResultsView(),
                    new ShopStoreView(),
                    new ShopStoreCostView(),
                    new ShopCustomerView(),
                    new ShopInputView(),
                    new ShopStorageView(),
                    new ShopEditView(),
                    new ShopLedgerView(),
                    new ShopItemEditView(),
                    new ShopItemAdminCommandView(),
                    new ShopItemAvailabilityMinutesAnvilView(),
                    new ShopItemAdminCommandDelayAnvilView(),
                    new ShopItemAdminCommandAnvilView(),
                    new ShopItemAdminStockLimitAnvilView(),
                    new ShopItemAdminResetTimerAnvilView(),
                    new ShopItemCurrencyTypeAnvilView(),
                    new ShopItemValueAnvilView(),
                    new ShopConfigValueAnvilView(),
                    new ShopMaterialSearchAnvilView(),
                    new ShopPurchaseAmountAnvilView(),
                    new ShopTrustedView()
                )
                .disableMetrics();
        this.viewFrame = frame.register();
    }

    private void initializeTaxes() {
        this.shopTaxScheduler = new ShopTaxScheduler(this);
        this.shopTaxScheduler.start();
    }

    private void initializeShopBossBar() {
        this.shopBossBarService = new ShopBossBarService(this);
        this.shopBossBarService.start();
    }

    private void initializeShopSidebarScoreboards() {
        this.shopSidebarScoreboardService = new ShopSidebarScoreboardService(this);
        this.shopSidebarScoreboardService.start();
    }

    private void initializeAdminShopRestocking() {
        this.adminShopRestockScheduler = new AdminShopRestockScheduler(this);
        this.adminShopRestockScheduler.start();
    }

    private void initializeAdminShopServerBankTransfers() {
        this.adminShopServerBankScheduler = new AdminShopServerBankScheduler(this);
        this.adminShopServerBankScheduler.start();
    }

    private boolean hasValidEconomyAndCurrency() {
        final Object vaultEconomy = this.resolveVaultEconomy();
        final boolean vaultAvailable = vaultEconomy != null;
        final boolean customCurrencyAvailable = this.hasRegisteredCustomCurrency();

        if (vaultAvailable || customCurrencyAvailable) {
            if (vaultAvailable) {
                this.economyInstance = vaultEconomy;
            }
            return true;
        }

        this.getLogger().warning(
            "RDS requires a valid Vault economy provider or at least one registered JExEconomy currency. "
                + "Currency-backed features will remain unavailable until a provider registers."
        );
        return false;
    }

    private @Nullable Object resolveVaultEconomy() {
        if (this.isVaultEconomyInstance(this.economyInstance)) {
            return this.economyInstance;
        }

        try {
            final Class<?> economyClass = Class.forName(VAULT_ECONOMY_CLASS);
            final Server server = this.plugin.getServer();
            final Object registration = server.getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                return null;
            }

            final Method getProviderMethod = registration.getClass().getMethod("getProvider");
            final Object provider = getProviderMethod.invoke(registration);
            if (this.isVaultEconomyInstance(provider)) {
                this.economyInstance = provider;
                return provider;
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean hasRegisteredCustomCurrency() {
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null) {
            return false;
        }

        try {
            final Field adapterField = JExEconomyBridge.class.getDeclaredField("adapter");
            final Field adapterClassField = JExEconomyBridge.class.getDeclaredField("adapterClass");
            adapterField.setAccessible(true);
            adapterClassField.setAccessible(true);

            final Object adapter = adapterField.get(bridge);
            final Class<?> adapterClass = (Class<?>) adapterClassField.get(bridge);
            final Method getAllCurrenciesMethod = adapterClass.getMethod("getAllCurrencies");
            final Object currenciesObject = getAllCurrenciesMethod.invoke(adapter);

            return currenciesObject instanceof Map<?, ?> currencies && !currencies.isEmpty();
        } catch (ReflectiveOperationException exception) {
            this.getLogger().fine("Failed to validate JExEconomy currencies during startup.");
            return false;
        }
    }

    /**
     * Indicates whether vault economy is available.
     *
     * @return {@code true} if vault economy; otherwise {@code false}
     */
    public boolean hasVaultEconomy() {
        return this.resolveVaultEconomy() != null;
    }

    /**
     * Returns whether the player currently has at least the requested Vault balance.
     *
     * @param player player to check
     * @param amount required balance
     * @return {@code true} when the player has sufficient Vault funds
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public boolean hasVaultFunds(
        final @NotNull OfflinePlayer player,
        final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        final Object vaultEconomy = this.resolveVaultEconomy();
        if (vaultEconomy == null) {
            return false;
        }

        final Object result = this.invokeVaultMethod(
                vaultEconomy,
                "has",
                new Class<?>[]{OfflinePlayer.class, double.class},
                player,
                amount
        );
        return Boolean.TRUE.equals(result);
    }

    /**
     * Withdraws Vault currency from the supplied player.
     *
     * @param player player to charge
     * @param amount amount to withdraw
     * @return {@code true} when the Vault transaction succeeded
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public boolean withdrawVault(
        final @NotNull OfflinePlayer player,
        final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        final Object vaultEconomy = this.resolveVaultEconomy();
        return vaultEconomy != null
            && this.invokeVaultTransaction(vaultEconomy, "withdrawPlayer", player, amount);
    }

    /**
     * Deposits Vault currency to the supplied player.
     *
     * @param player player to credit
     * @param amount amount to deposit
     * @return {@code true} when the Vault transaction succeeded
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public boolean depositVault(
        final @NotNull OfflinePlayer player,
        final double amount
    ) {
        if (amount <= 0D) {
            return true;
        }

        final Object vaultEconomy = this.resolveVaultEconomy();
        return vaultEconomy != null
            && this.invokeVaultTransaction(vaultEconomy, "depositPlayer", player, amount);
    }

    /**
     * Formats a Vault currency amount using the active provider when one is available.
     *
     * @param amount amount to format
     * @return formatted Vault amount string
     */
    public @NotNull String formatVaultCurrency(final double amount) {
        final Object vaultEconomy = this.resolveVaultEconomy();
        if (vaultEconomy == null) {
            return this.formatAmount(amount);
        }

        final Object formatted = this.invokeVaultMethod(
                vaultEconomy,
                "format",
                new Class<?>[]{double.class},
                amount
        );
        return formatted instanceof String value ? value : this.formatAmount(amount);
    }

    private boolean isVaultEconomyInstance(final @Nullable Object candidate) {
        if (candidate == null) {
            return false;
        }

        try {
            return Class.forName(VAULT_ECONOMY_CLASS).isInstance(candidate);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private @Nullable Object invokeVaultMethod(
        final @NotNull Object vaultEconomy,
        final @NotNull String methodName,
        final @NotNull Class<?>[] parameterTypes,
        final Object... arguments
    ) {
        try {
            return vaultEconomy.getClass()
                .getMethod(methodName, parameterTypes)
                .invoke(vaultEconomy, arguments);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private boolean invokeVaultTransaction(
        final @NotNull Object vaultEconomy,
        final @NotNull String methodName,
        final @NotNull OfflinePlayer player,
        final double amount
    ) {
        final Object response = this.invokeVaultMethod(
            vaultEconomy,
            methodName,
            new Class<?>[]{OfflinePlayer.class, double.class},
            player,
            amount
        );
        if (response == null) {
            return false;
        }

        try {
            final Method successMethod = response.getClass().getMethod("transactionSuccess");
            return Boolean.TRUE.equals(successMethod.invoke(response));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private @NotNull String formatAmount(final double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    /**
     * Returns the shared executor used for asynchronous persistence operations.
     *
     * @return plugin executor service, or {@code null} before load completes
     */
    public @Nullable ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Returns the shared platform abstraction instance.
     *
     * @return active platform facade, or {@code null} before load completes
     */
    public @Nullable RPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Returns the JPA entity manager factory used by repositories.
     *
     * @return entity manager factory, or {@code null} before initialization completes
     */
    public @Nullable EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    /**
     * Returns the raw economy service instance registered by the service registry.
     *
     * @return raw service instance, or {@code null} when no provider was resolved
     */
    public @Nullable Object getEconomyInstance() {
        return this.economyInstance;
    }

    /**
     * Returns the registered Inventory Framework view frame.
     *
     * @return registered view frame, or {@code null} before views are initialized
     */
    public @Nullable ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    /**
     * Returns the service that schedules recurring shop tax processing.
     *
     * @return shop tax scheduler, or {@code null} before enable completes
     */
    public @Nullable ShopTaxScheduler getShopTaxScheduler() {
        return this.shopTaxScheduler;
    }

    /**
     * Returns the service that manages the optional shop boss bar overlay.
     *
     * @return shop boss bar service, or {@code null} before enable completes
     */
    public @Nullable ShopBossBarService getShopBossBarService() {
        return this.shopBossBarService;
    }

    /**
     * Returns the scheduler that restocks admin shops automatically.
     *
     * @return admin shop restock scheduler, or {@code null} before enable completes
     */
    public @Nullable AdminShopRestockScheduler getAdminShopRestockScheduler() {
        return this.adminShopRestockScheduler;
    }

    /**
     * Returns the scheduler that periodically transfers admin shop bank balances into the server bank.
     *
     * @return admin-shop server bank scheduler, or {@code null} before enable completes
     */
    public @Nullable AdminShopServerBankScheduler getAdminShopServerBankScheduler() {
        return this.adminShopServerBankScheduler;
    }

    /**
     * Returns the service that manages the optional shop sidebar scoreboard.
     *
     * @return shop sidebar scoreboard service, or {@code null} before enable completes
     */
    public @Nullable ShopSidebarScoreboardService getShopSidebarScoreboardService() {
        return this.shopSidebarScoreboardService;
    }

    /**
     * Returns the scheduler adapter detected for the current server platform.
     *
     * @return scheduler adapter, or {@code null} before enable completes
     */
    public @Nullable ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    /**
     * Returns the detected platform type.
     *
     * @return current platform type, or {@code null} before enable completes
     */
    public @Nullable PlatformType getPlatformType() {
        return this.platformType;
    }

    /**
     * Returns the repository used for persisted player shop profiles.
     *
     * @return player repository, or {@code null} before repository initialization completes
     */
    public @Nullable RRDSPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Returns the repository used for placed-shop persistence and lookups.
     *
     * @return shop repository, or {@code null} before repository initialization completes
     */
    public @Nullable RShop getShopRepository() {
        return this.shopRepository;
    }

    /**
     * Returns the repository used for server-bank balances.
     *
     * @return server-bank repository, or {@code null} before repository initialization completes
     */
    public @Nullable RServerBank getServerBankRepository() {
        return this.serverBankRepository;
    }
}
