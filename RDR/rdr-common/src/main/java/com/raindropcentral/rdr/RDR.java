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
import com.raindropcentral.rdr.requirement.RDRRequirementSetup;
import com.raindropcentral.rdr.service.StorageService;
import com.raindropcentral.rdr.service.scoreboard.StorageSidebarScoreboardService;
import com.raindropcentral.rdr.view.StorageHotkeyAnvilView;
import com.raindropcentral.rdr.view.StorageOverviewView;
import com.raindropcentral.rdr.view.StoragePlayerView;
import com.raindropcentral.rdr.view.StorageSettingsView;
import com.raindropcentral.rdr.view.StorageStoreRequirementsView;
import com.raindropcentral.rdr.view.StorageStoreView;
import com.raindropcentral.rdr.view.StorageTrustedView;
import com.raindropcentral.rdr.view.StorageView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
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

    private final JavaPlugin plugin;
    private final String edition;
    private final StorageService storageService;

    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;

    private Object economyInstance;
    private ViewFrame viewFrame;
    private StorageSidebarScoreboardService storageSidebarScoreboardService;

    private RRDRPlayer playerRepository;
    private RRStorage storageRepository;
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
        this.initializeCommands();
        this.initializeViews();
        this.initializeStorageSidebarScoreboards();
        this.getLogger().info("RDR (" + this.edition + ") Edition enabled successfully");
    }

    /**
     * Shuts down asynchronous services and closes the entity manager factory.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDR (" + this.edition + "): closing Hibernate");

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.storageSidebarScoreboardService != null) {
            this.storageSidebarScoreboardService.shutdown();
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
     * Returns the effective edition storage cap using the current configuration file.
     *
     * @return effective edition-aware storage cap
     */
    public int getMaximumStorages() {
        return this.getMaximumStorages(this.getDefaultConfig());
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
        this.getLogger().info("Registering Vault service");
        new ServiceRegistry().register(
            "net.milkbowl.vault.economy.Economy",
            "TheNewEconomy"
        ).optional().maxAttempts(30).retryDelay(1000).onSuccess(economy -> {
            this.getLogger().info("Vault service initialized");
            this.economyInstance = economy;
        }).onFailure(() -> this.getLogger().warning(
            "Vault service not present; initialization failed")
        ).load();
    }

    private void initializeCommands() {
        final var commandFactory = new CommandFactory(this.plugin, this);
        commandFactory.registerAllCommandsAndListeners();
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
                new StorageStoreRequirementsView(),
                new StorageHotkeyAnvilView(),
                new StorageView()
            )
            .disableMetrics();
        this.viewFrame = frame.register();
    }

    private void initializeStorageSidebarScoreboards() {
        this.storageSidebarScoreboardService = new StorageSidebarScoreboardService(this);
        this.storageSidebarScoreboardService.start();
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
