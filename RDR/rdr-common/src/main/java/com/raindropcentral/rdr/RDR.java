package com.raindropcentral.rdr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.repository.RRDRPlayer;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rdr.database.repository.RRTownStorageBank;
import com.raindropcentral.rdr.requirement.RDRRequirementSetup;
import com.raindropcentral.rdr.service.StorageAdminPlayerSettingsService;
import com.raindropcentral.rdr.service.StorageFilledTaxScheduler;
import com.raindropcentral.rdr.service.StorageService;
import com.raindropcentral.rdr.service.scoreboard.StorageSidebarScoreboardService;
import com.raindropcentral.rdr.view.AdminCurrencyView;
import com.raindropcentral.rdr.view.PlaceholderAPIView;
import com.raindropcentral.rdr.view.PluginIntegrationManagementView;
import com.raindropcentral.rdr.view.StorageAdminGroupEditView;
import com.raindropcentral.rdr.view.StorageAdminOverrideValueAnvilView;
import com.raindropcentral.rdr.view.StorageAdminPlayerEditView;
import com.raindropcentral.rdr.view.StorageAdminPlayerSelectView;
import com.raindropcentral.rdr.view.StorageAdminPlayerView;
import com.raindropcentral.rdr.view.StorageAdminStorageControlView;
import com.raindropcentral.rdr.view.StorageAdminView;
import com.raindropcentral.rdr.view.StorageConfigValueAnvilView;
import com.raindropcentral.rdr.view.StorageConfigView;
import com.raindropcentral.rdr.view.StorageFrozenStorageView;
import com.raindropcentral.rdr.view.StorageHotkeyAnvilView;
import com.raindropcentral.rdr.view.StorageJobsView;
import com.raindropcentral.rdr.view.StorageOverviewView;
import com.raindropcentral.rdr.view.StoragePlayerView;
import com.raindropcentral.rdr.view.StorageSettingsView;
import com.raindropcentral.rdr.view.StorageSkillsView;
import com.raindropcentral.rdr.view.StorageStoreRequirementsView;
import com.raindropcentral.rdr.view.StorageStoreView;
import com.raindropcentral.rdr.view.StorageTaxTownBankView;
import com.raindropcentral.rdr.view.StorageTaxView;
import com.raindropcentral.rdr.view.StorageTrustedView;
import com.raindropcentral.rdr.view.StorageView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.metrics.BStatsMetrics;
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

/**
 * Shared runtime bootstrap for RDR editions.
 *
 * <p>The runtime bootstraps shared platform services, persistence, commands, listeners, and
 * player-facing views while delegating edition-specific feature limits to {@link StorageService}.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class RDR {

    private static final String VAULT_ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";
    private static final String CONFIG_FOLDER_PATH = "config";
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String SERVER_UUID_FILE_NAME = "server.uuid";
    private static final int METRICS_SERVICE_ID = 22905;

    private final JavaPlugin plugin;
    private final String edition;
    private final StorageService storageService;

    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;

    private Object economyInstance;
    private LuckPermsService luckPermsService;
    private ViewFrame viewFrame;
    private StorageSidebarScoreboardService storageSidebarScoreboardService;
    private StorageFilledTaxScheduler storageFilledTaxScheduler;
    private StorageAdminPlayerSettingsService storageAdminPlayerSettingsService;
    private BStatsMetrics metrics;

    private RRDRPlayer playerRepository;
    private RRStorage storageRepository;
    private RRTownStorageBank townStorageBankRepository;
    private UUID serverUuid;

    /**
     * Creates a new shared RDR runtime.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used for logging
     * @param storageService edition-specific storage service
     * @throws NullPointerException if any argument is {@code null}
     */
    public RDR(
        final @NotNull JavaPlugin plugin,
        final @NotNull String edition,
        final @NotNull StorageService storageService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.edition = Objects.requireNonNull(edition, "edition");
        this.storageService = Objects.requireNonNull(storageService, "storageService");
    }

    /**
     * Allocates shared platform state before plugin enable.
     */
    public void onLoad() {
        this.platform = new RPlatform(this.plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes persistence, commands, listeners, and views for the running edition.
     */
    public void onEnable() {
        this.getLogger().info("Enabling RDR (" + this.edition + ") Edition");

        this.platform.initialize();
        this.initializeMetrics();
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.scheduler = this.platform.getScheduler();
        this.executor = Executors.newFixedThreadPool(4);
        this.ensureDefaultConfigFile();
        this.getDefaultConfig().logMissingRequirementWarnings(this.getLogger());
        RDRRequirementSetup.initialize(this);
        this.serverUuid = this.loadOrCreateServerUuid();

        try {
            this.initializeHibernate();
            this.initializeRepositories();
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to initialize RDR persistence: " + exception.getMessage());
            this.onDisable();
            return;
        }

        this.initializePlugins();
        this.initializeAdminPlayerSettings();
        this.initializeCommands();
        this.initializeViews();
        this.initializeStorageSidebarScoreboards();
        this.initializeStorageFilledTaxScheduler();
        this.getLogger().info("RDR (" + this.edition + ") Edition enabled successfully");
    }

    /**
     * Shuts down asynchronous services and closes the entity manager factory.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDR (" + this.edition + "): closing Hibernate");

        if (this.storageAdminPlayerSettingsService != null) {
            try {
                this.storageAdminPlayerSettingsService.save();
            } catch (Exception exception) {
                this.getLogger().warning("Failed to save storage admin player settings: " + exception.getMessage());
            }
        }

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.storageSidebarScoreboardService != null) {
            this.storageSidebarScoreboardService.shutdown();
        }
        if (this.storageFilledTaxScheduler != null) {
            this.storageFilledTaxScheduler.shutdown();
        }

        if (this.entityManagerFactory != null) {
            try {
                this.entityManagerFactory.close();
            } catch (final Exception ignored) {
            }
        }

        RDRRequirementSetup.shutdown();
    }

    /**
     * Loads the plugin configuration from disk, returning fallback defaults when loading fails.
     *
     * @return parsed plugin configuration
     */
    public @NotNull ConfigSection getDefaultConfig() {
        this.ensureDefaultConfigFile();
        try {
            return ConfigSection.fromFile(this.getDefaultConfigFile());
        } catch (final Exception exception) {
            this.getLogger().warning(
                "Failed to load RDR config from " + this.getDefaultConfigFile().getAbsolutePath() + ": " + exception.getMessage()
            );
            return ConfigSection.createDefault();
        }
    }

    /**
     * Returns the effective edition storage cap for the supplied configuration snapshot.
     *
     * @param config configuration snapshot to evaluate
     * @return effective edition-aware storage cap
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public int getMaximumStorages(final @NotNull ConfigSection config) {
        return this.storageService.getMaximumStorages(config);
    }

    /**
     * Returns the effective maximum storages for a player after admin overrides are applied.
     *
     * @param player target player
     * @param config configuration snapshot to evaluate
     * @return effective max storages for the player ({@code -1} means unlimited)
     * @throws NullPointerException if {@code player} or {@code config} is {@code null}
     */
    public int getMaximumStorages(
        final @NotNull org.bukkit.entity.Player player,
        final @NotNull ConfigSection config
    ) {
        final int defaultMaximum = this.getMaximumStorages(config);
        return this.storageAdminPlayerSettingsService == null
            ? defaultMaximum
            : this.storageAdminPlayerSettingsService.resolveMaximumStorages(player, defaultMaximum);
    }

    /**
     * Returns the effective maximum storages for a player using the current config.
     *
     * @param player target player
     * @return effective max storages ({@code -1} means unlimited)
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public int getMaximumStorages(final @NotNull org.bukkit.entity.Player player) {
        return this.getMaximumStorages(player, this.getDefaultConfig());
    }

    /**
     * Returns the effective maximum storages for a player UUID after player overrides are applied.
     *
     * <p>This overload does not evaluate group membership and therefore only uses explicit player
     * overrides plus edition defaults.</p>
     *
     * @param playerId target player identifier
     * @param config configuration snapshot to evaluate
     * @return effective max storages ({@code -1} means unlimited)
     * @throws NullPointerException if {@code playerId} or {@code config} is {@code null}
     */
    public int getMaximumStorages(
        final @NotNull UUID playerId,
        final @NotNull ConfigSection config
    ) {
        final int defaultMaximum = this.getMaximumStorages(config);
        return this.storageAdminPlayerSettingsService == null
            ? defaultMaximum
            : this.storageAdminPlayerSettingsService.resolveMaximumStorages(playerId, defaultMaximum);
    }

    /**
     * Returns the effective edition storage cap using the current configuration file.
     *
     * @return effective edition-aware storage cap
     */
    public int getMaximumStorages() {
        return this.getMaximumStorages(this.getDefaultConfig());
    }

    /**
     * Returns the effective storage-store discount percent for a player.
     *
     * @param player target player
     * @return discount percent in range {@code 0.0} to {@code 100.0}
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public double getStorageDiscountPercent(final @NotNull org.bukkit.entity.Player player) {
        return this.storageAdminPlayerSettingsService == null
            ? 0.0D
            : this.storageAdminPlayerSettingsService.resolveDiscountPercent(player);
    }

    /**
     * Applies the resolved player storage discount to a base amount.
     *
     * @param player target player
     * @param baseAmount amount before discount
     * @return discounted amount
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public double applyStorageDiscount(
        final @NotNull org.bukkit.entity.Player player,
        final double baseAmount
    ) {
        if (baseAmount <= 0.0D) {
            return 0.0D;
        }

        final double discountPercent = this.getStorageDiscountPercent(player);
        return baseAmount * (1.0D - (Math.max(0.0D, Math.min(100.0D, discountPercent)) / 100.0D));
    }

    /**
     * Returns the edition-aware number of storages that should be provisioned for new players.
     *
     * @param config configuration snapshot to evaluate
     * @return effective first-join storage count
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public int getInitialProvisionedStorages(final @NotNull ConfigSection config) {
        return this.storageService.getInitialProvisionedStorages(config);
    }

    /**
     * Returns whether the active edition allows storage settings to be changed.
     *
     * @return {@code true} when hotkeys and trusted-access settings may be edited
     */
    public boolean canChangeStorageSettings() {
        return this.storageService.canChangeStorageSettings();
    }

    /**
     * Returns the edition-specific storage service.
     *
     * @return active storage service
     */
    public @NotNull StorageService getStorageService() {
        return this.storageService;
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
     * Returns the shared executor used for asynchronous persistence operations.
     *
     * @return plugin executor service
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
     * Returns the service that manages the optional storage sidebar scoreboard.
     *
     * @return storage sidebar scoreboard service
     */
    public @Nullable StorageSidebarScoreboardService getStorageSidebarScoreboardService() {
        return this.storageSidebarScoreboardService;
    }

    /**
     * Returns the service managing per-player/group admin overrides for storage limits and discounts.
     *
     * @return admin override settings service, or {@code null} before enable completes
     */
    public @Nullable StorageAdminPlayerSettingsService getStorageAdminPlayerSettingsService() {
        return this.storageAdminPlayerSettingsService;
    }

    /**
     * Returns the optional LuckPerms integration wrapper.
     *
     * @return LuckPerms service wrapper, or {@code null} if LuckPerms is unavailable
     */
    public @Nullable LuckPermsService getLuckPermsService() {
        return this.luckPermsService;
    }

    /**
     * Returns the recurring scheduler responsible for non-empty storage taxes.
     *
     * @return filled-storage tax scheduler, or {@code null} before initialization completes
     */
    public @Nullable StorageFilledTaxScheduler getStorageFilledTaxScheduler() {
        return this.storageFilledTaxScheduler;
    }

    /**
     * Returns the repository used for persisted player storage profiles.
     *
     * @return player repository, or {@code null} before repository initialization completes
     */
    public @Nullable RRDRPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    /**
     * Returns the repository used for direct storage persistence and lease management.
     *
     * @return storage repository, or {@code null} before repository initialization completes
     */
    public @Nullable RRStorage getStorageRepository() {
        return this.storageRepository;
    }

    /**
     * Returns the repository used for persisted town storage-tax bank ledgers.
     *
     * @return town storage-tax repository, or {@code null} before repository initialization completes
     */
    public @Nullable RRTownStorageBank getTownStorageBankRepository() {
        return this.townStorageBankRepository;
    }

    /**
     * Returns the persisted UUID that identifies this server instance for storage leases.
     *
     * @return stable server UUID used for cross-server storage locking
     */
    public @NotNull UUID getServerUuid() {
        return this.serverUuid;
    }

    /**
     * Returns the currently registered Vault economy provider when one is available.
     *
     * @return active Vault economy provider, or {@code null} when Vault is unavailable
     */
    public @Nullable net.milkbowl.vault.economy.Economy getEco() {
        if (this.economyInstance == null) {
            return null;
        }
        return (net.milkbowl.vault.economy.Economy) this.economyInstance;
    }

    /**
     * Returns whether a valid Vault economy provider is currently available.
     *
     * @return {@code true} when Vault economy access is available
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

    private @NotNull File getDefaultConfigFile() {
        return new File(new File(this.plugin.getDataFolder(), CONFIG_FOLDER_PATH), CONFIG_FILE_NAME);
    }

    private void ensureDefaultConfigFile() {
        final File dataFolder = this.plugin.getDataFolder();
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
        final ServiceRegistry registry = this.platform == null
            ? new ServiceRegistry()
            : this.platform.getServiceRegistry();

        this.getLogger().info("Registering Vault service");
        registry.register(
            "net.milkbowl.vault.economy.Economy",
            "TheNewEconomy"
        ).optional().maxAttempts(30).retryDelay(1000).onSuccess(economy -> {
            this.getLogger().info("Vault service initialized");
            this.economyInstance = economy;
        }).onFailure(() -> this.getLogger().warning(
            "Vault service not present; initialization failed")
        ).load();

        this.getLogger().info("Registering LuckPerms service");
        registry.register(
            "net.luckperms.api.LuckPerms",
            "LuckPerms"
        ).optional().maxAttempts(30).retryDelay(500).onSuccess(luckPerms -> {
            if (this.platform == null) {
                this.getLogger().warning("LuckPerms detected but RPlatform is unavailable.");
                return;
            }

            this.luckPermsService = new LuckPermsService(this.platform);
            this.getLogger().info("LuckPerms service initialized");
        }).onFailure(() -> {
            this.luckPermsService = null;
            this.getLogger().info("LuckPerms service not present; continuing without LuckPerms integration");
        }).load();
    }

    private void initializeCommands() {
        final var commandFactory = new CommandFactory(this.plugin, this);
        commandFactory.registerAllCommandsAndListeners();
    }

    private void initializeMetrics() {
        if (this.platform == null || this.metrics != null) {
            return;
        }

        this.metrics = new BStatsMetrics(
            this.plugin,
            METRICS_SERVICE_ID,
            this.platform.getPlatformType() == PlatformType.FOLIA
        );
        final boolean premiumEdition = this.storageService.isPremium();
        this.metrics.addCustomChart(new BStatsMetrics.SingleLineChart("free", () -> premiumEdition ? 0 : 1));
        this.metrics.addCustomChart(new BStatsMetrics.SingleLineChart("premium", () -> premiumEdition ? 1 : 0));
    }

    private void initializeAdminPlayerSettings() {
        this.storageAdminPlayerSettingsService = new StorageAdminPlayerSettingsService(this);
        this.storageAdminPlayerSettingsService.load();
    }

    private void initializeRepositories() {
        this.playerRepository = new RRDRPlayer(
            this.executor,
            this.entityManagerFactory,
            RDRPlayer.class,
            RDRPlayer::getIdentifier
        );
        this.storageRepository = new RRStorage(
            this.executor,
            this.entityManagerFactory
        );
        this.townStorageBankRepository = new RRTownStorageBank(
            this.executor,
            this.entityManagerFactory
        );
    }

    @SuppressWarnings("resource")
    private void initializeHibernate() throws IOException {
        final File file = this.getHibernateFile();

        if (!file.exists()) {
            try (InputStream in = this.plugin.getResource("database/hibernate.properties")) {
                if (in == null) {
                    throw new IOException("Missing resource com.raindropcentral.rdr.database/hibernate.properties");
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
        final File data = this.plugin.getDataFolder();
        if (!data.exists() && !data.mkdirs()) {
            throw new IOException("Could not create data folder");
        }

        final File db = new File(data, "database");
        if (!db.exists() && !db.mkdirs()) {
            throw new IOException("Could not create com.raindropcentral.rdr.database folder");
        }

        return new File(db, "hibernate.properties");
    }

    private void initializeViews() {
        final ViewFrame frame = ViewFrame
            .create(this.plugin)
            .install(AnvilInputFeature.AnvilInput)
            .with(
                new StorageOverviewView(),
                new StoragePlayerView(),
                new StorageSettingsView(),
                new StorageTrustedView(),
                new StorageStoreView(),
                new StorageAdminView(),
                new StorageAdminPlayerView(),
                new StorageAdminPlayerSelectView(),
                new StorageAdminPlayerEditView(),
                new StorageAdminGroupEditView(),
                new StorageAdminStorageControlView(),
                new StorageConfigView(),
                new PluginIntegrationManagementView(),
                new AdminCurrencyView(),
                new StorageSkillsView(),
                new StorageJobsView(),
                new PlaceholderAPIView(),
                new StorageStoreRequirementsView(),
                new StorageHotkeyAnvilView(),
                new StorageConfigValueAnvilView(),
                new StorageAdminOverrideValueAnvilView(),
                new StorageTaxView(),
                new StorageFrozenStorageView(),
                new StorageTaxTownBankView(),
                new StorageView()
            )
            .disableMetrics();
        this.viewFrame = frame.register();
    }

    private void initializeStorageSidebarScoreboards() {
        this.storageSidebarScoreboardService = new StorageSidebarScoreboardService(this);
        this.storageSidebarScoreboardService.start();
    }

    private void initializeStorageFilledTaxScheduler() {
        this.storageFilledTaxScheduler = new StorageFilledTaxScheduler(this);
        this.storageFilledTaxScheduler.start();
    }

    private @NotNull UUID loadOrCreateServerUuid() {
        final File dataFolder = this.plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            this.getLogger().warning("Could not create plugin data folder for server identity.");
            return UUID.randomUUID();
        }

        final File serverUuidFile = new File(dataFolder, SERVER_UUID_FILE_NAME);
        try {
            if (serverUuidFile.exists()) {
                return UUID.fromString(Files.readString(serverUuidFile.toPath()).trim());
            }

            final UUID generatedUuid = UUID.randomUUID();
            Files.writeString(
                serverUuidFile.toPath(),
                generatedUuid.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            return generatedUuid;
        } catch (final IOException | IllegalArgumentException exception) {
            this.getLogger().warning("Failed to load persisted RDR server UUID: " + exception.getMessage());
            return UUID.randomUUID();
        }
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
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private boolean isVaultEconomyInstance(final @Nullable Object candidate) {
        if (candidate == null) {
            return false;
        }

        try {
            return Class.forName(VAULT_ECONOMY_CLASS).isInstance(candidate);
        } catch (final Throwable ignored) {
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
        } catch (final ReflectiveOperationException exception) {
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
        } catch (final ReflectiveOperationException exception) {
            return false;
        }
    }

    private @NotNull String formatAmount(final double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
