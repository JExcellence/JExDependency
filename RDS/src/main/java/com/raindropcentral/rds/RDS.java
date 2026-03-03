package com.raindropcentral.rds;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.configs.TaxSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.repository.RRDSPlayer;
import com.raindropcentral.rds.database.repository.RShop;
import com.raindropcentral.rds.service.shop.AdminShopRestockScheduler;
import com.raindropcentral.rds.service.shop.ShopBossBarService;
import com.raindropcentral.rds.service.tax.ShopTaxScheduler;
import com.raindropcentral.rds.view.shop.*;
import com.raindropcentral.rds.view.shop.anvil.ShopItemCurrencyTypeAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminResetTimerAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminStockLimitAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemValueAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopPurchaseAmountAnvilView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class RDS extends JavaPlugin {

    private static final String VAULT_ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";

    private JavaPlugin rds;
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

    //Repositories
    private RRDSPlayer playerRepository;
    private RShop shopRepository;
    
    private final static String FOLDER_PATH = "config";
    private final static String FILE_NAME   = "config.yml";

    @Override
    public void onLoad() {
        this.rds = this;
        this.getLogger().info("Loading RPlatform for RDS");
        this.platform = new RPlatform(rds);
        this.executor = Executors.newFixedThreadPool(4);
    }


    @Override
    public void onEnable() {
        this.platform.initialize();
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.scheduler = this.platform.getScheduler();
        this.executor = Executors.newFixedThreadPool(4);
        this.ensureDefaultConfigFile();

        try {
            initializeHibernate();
            initializeRepositories();
            this.getLogger().info("Hibernate initialized");
        } catch (Exception e) {
            onDisable();
            return;
        }
        initializePlugins();
        initializeCommands();
        initializeViews();
        initializeTaxes();
        initializeAdminShopRestocking();
        initializeShopBossBar();

        if (!this.hasValidEconomyAndCurrency()) {
            this.getLogger().warning(
                    "No Vault provider or registered JExEconomy currencies are currently available. " +
                    "RDS will remain enabled and currency-backed features will become available when a provider registers."
            );
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Disabling RDS: closing Hibernate");

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.shopBossBarService != null) {
            this.shopBossBarService.shutdown();
        }

        if (entityManagerFactory != null) {
            try {
                entityManagerFactory.close();
            } catch (Exception ignored) {}
        }
    }

    public ConfigSection getDefaultConfig() {
        this.ensureDefaultConfigFile();
        try {
            var cfgManager = new ConfigManager(this, "config");
            var cfgKeeper = new ConfigKeeper<>(cfgManager, "config.yml", ConfigSection.class);
            final ConfigSection config = cfgKeeper.rootSection;
            config.setTaxes(
                    TaxSection.fromFile(
                            this.getDefaultConfigFile(),
                            config.getDefaultCurrencyType()
                    )
            );
            return config;
        } catch (Exception e) {
            return new ConfigSection(new EvaluationEnvironmentBuilder());
        }
    }

    private @NotNull File getDefaultConfigFile() {
        return new File(new File(this.getDataFolder(), FOLDER_PATH), FILE_NAME);
    }

    private void ensureDefaultConfigFile() {
        final File dataFolder = this.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            this.getLogger().warning("Could not create plugin data folder for config extraction.");
            return;
        }

        final File configFolder = new File(dataFolder, FOLDER_PATH);
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            this.getLogger().warning("Could not create config folder for default config extraction.");
            return;
        }

        final File configFile = new File(configFolder, FILE_NAME);
        if (configFile.exists()) {
            return;
        }

        try {
            this.saveResource(FOLDER_PATH + "/" + FILE_NAME, false);
        } catch (IllegalArgumentException exception) {
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
        var commandFactory = new CommandFactory(this);
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeRepositories(){
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
    }

    @SuppressWarnings("resource")
    private void initializeHibernate() throws IOException {
        File file = getHibernateFile();

        if (!file.exists()) {
            try (InputStream in = getResource("database/hibernate.properties")) {
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
        File data = getDataFolder();
        if (!data.exists() && !data.mkdirs()) {
            throw new IOException("Could not create data folder");
        }

        File db = new File(data, "database");
        if (!db.exists() && !db.mkdirs()) {
            throw new IOException("Could not create com.raindropcentral.rdt.database folder");
        }

        return new File(db, "hibernate.properties");
    }

    private void initializeViews() {
        ViewFrame frame = ViewFrame
                .create(this.rds)
                .install(AnvilInputFeature.AnvilInput)
                .with(
                    new ShopOverviewView(),
                    new ShopBankView(),
                    new ShopSearchView(),
                    new ShopStoreView(),
                    new ShopStoreCostView(),
                    new ShopCustomerView(),
                    new ShopInputView(),
                    new ShopStorageView(),
                    new ShopEditView(),
                    new ShopLedgerView(),
                    new ShopItemEditView(),
                    new ShopItemAdminStockLimitAnvilView(),
                    new ShopItemAdminResetTimerAnvilView(),
                    new ShopItemCurrencyTypeAnvilView(),
                    new ShopItemValueAnvilView(),
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

    private void initializeAdminShopRestocking() {
        this.adminShopRestockScheduler = new AdminShopRestockScheduler(this);
        this.adminShopRestockScheduler.start();
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

        this.getLogger().warning("RDS requires a valid Vault economy provider or at least one registered JExEconomy currency. Disabling plugin.");
        return false;
    }

    private @Nullable Object resolveVaultEconomy() {
        if (this.isVaultEconomyInstance(this.economyInstance)) {
            return this.economyInstance;
        }

        try {
            final Class<?> economyClass = Class.forName(VAULT_ECONOMY_CLASS);
            final Object registration = Bukkit.getServicesManager().getRegistration(economyClass);
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

    public boolean hasVaultEconomy() {
        return this.resolveVaultEconomy() != null;
    }

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

    public @NotNull String formatVaultCurrency(
            final double amount
    ) {
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

    private boolean isVaultEconomyInstance(
            final @Nullable Object candidate
    ) {
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

    private @NotNull String formatAmount(
            final double amount
    ) {
        return String.format(Locale.US, "%.2f", amount);
    }

    public JavaPlugin getPlugin() {
        return this.rds;
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public RPlatform getPlatform() {
        return this.platform;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    public Object getEconomyInstance() {
        return this.economyInstance;
    }

    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    public ShopTaxScheduler getShopTaxScheduler() {
        return this.shopTaxScheduler;
    }

    public ShopBossBarService getShopBossBarService() {
        return this.shopBossBarService;
    }

    public AdminShopRestockScheduler getAdminShopRestockScheduler() {
        return this.adminShopRestockScheduler;
    }

    public ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    public PlatformType getPlatformType() {
        return this.platformType;
    }

    public RRDSPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    public RShop getShopRepository() { return this.shopRepository; }
}
