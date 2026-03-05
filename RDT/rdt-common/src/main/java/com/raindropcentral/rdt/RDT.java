package com.raindropcentral.rdt;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.factory.BossBarFactory;
import com.raindropcentral.rdt.factory.IRSFactory;
import com.raindropcentral.rdt.service.TownService;
import com.raindropcentral.rdt.view.main.MainOverviewView;
import com.raindropcentral.rdt.view.town.ServerTownsOverviewView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Shared runtime bootstrap for RDT editions.
 *
 * <p>The runtime initializes persistence, views, commands, and economy integration while
 * delegating edition-specific rules to {@link TownService}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class RDT {

    private static final String CONFIG_FOLDER_PATH = "config";
    private static final String CONFIG_FILE_NAME = "config.yml";

    private final JavaPlugin plugin;
    private final String edition;
    private final TownService townService;

    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private RRTown townRepository;
    private RRDTPlayer playerRepository;
    private BossBarFactory bossBarFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private IRSFactory irsFactory;

    private Object economyInstance;
    private ViewFrame viewFrame;

    /**
     * Creates a shared RDT runtime for a specific edition.
     *
     * @param plugin owning Bukkit plugin
     * @param edition edition label used in logs
     * @param townService edition-specific behavior service
     * @throws NullPointerException if any argument is {@code null}
     */
    public RDT(
            final @NotNull JavaPlugin plugin,
            final @NotNull String edition,
            final @NotNull TownService townService
    ) {
        this.plugin = plugin;
        this.edition = edition;
        this.townService = townService;
    }

    /**
     * Allocates shared platform state before plugin enable.
     */
    public void onLoad() {
        this.getLogger().info("Loading RPlatform for RDT (" + this.edition + ")");
        this.platform = new RPlatform(this.plugin);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Initializes repositories, views, and runtime services.
     */
    public void onEnable() {
        this.getLogger().info("Enabling RDT (" + this.edition + ") Edition");
        this.platform.initialize();
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.scheduler = this.platform.getScheduler();
        this.executor = Executors.newFixedThreadPool(4);
        this.ensureDefaultConfigFile();

        try {
            this.initializeHibernate();
            this.initializeRepositories();
            this.getLogger().info("Hibernate initialized");
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to initialize RDT persistence: " + exception.getMessage());
            this.onDisable();
            return;
        }

        this.getLogger().info("Connecting to economy");
        this.initializePlugins();
        this.initializeCommands();
        this.initializeViews();
        this.bossBarFactory = new BossBarFactory(this);
        // NO TAXATION WITHOUT REPRESENTATION
        this.irsFactory = new IRSFactory(this);
        // Initiate async tasks for each town since last taxation stored in town db
        this.irsFactory.runAll();
    }

    /**
     * Shuts down runtime services and releases resources.
     */
    public void onDisable() {
        this.getLogger().info("Disabling RDT (" + this.edition + "): closing Hibernate");

        if (this.executor != null) {
            this.executor.shutdownNow();
        }

        if (this.entityManagerFactory != null) {
            try {
                this.entityManagerFactory.close();
            } catch (final Exception ignored) {
            }
        }
    }

    /**
     * Loads the plugin configuration for the active edition.
     *
     * <p>Free editions always read the bundled JAR config to prevent runtime config overrides.
     * Premium editions read from the extracted data-folder config and fall back to bundled defaults
     * when the file cannot be parsed.</p>
     *
     * @return parsed configuration for the active edition
     */
    public @NotNull ConfigSection getDefaultConfig() {
        if (this.canChangeConfigs()) {
            this.ensureDefaultConfigFile();
            try {
                return ConfigSection.fromFile(this.getDefaultConfigFile());
            } catch (final Exception exception) {
                this.getLogger().warning(
                        "Failed to parse RDT config from " + this.getDefaultConfigFile().getAbsolutePath()
                                + ": " + exception.getMessage()
                );
                return this.loadBundledConfig();
            }
        }

        return this.loadBundledConfig();
    }

    /**
     * Returns whether the active edition allows runtime config changes.
     *
     * @return {@code true} when config edits are allowed
     */
    public boolean canChangeConfigs() {
        return this.townService.canChangeConfigs();
    }

    /**
     * Returns whether the active edition is premium.
     *
     * @return {@code true} when the runtime is running the premium edition
     */
    public boolean isPremium() {
        return this.townService.isPremium();
    }

    private @NotNull ConfigSection loadBundledConfig() {
        try (InputStream inputStream = this.plugin.getResource(CONFIG_FOLDER_PATH + "/" + CONFIG_FILE_NAME)) {
            if (inputStream == null) {
                this.getLogger().warning("Bundled config resource '" + CONFIG_FOLDER_PATH + "/" + CONFIG_FILE_NAME + "' was not found.");
                return ConfigSection.createDefault();
            }
            return ConfigSection.fromInputStream(inputStream);
        } catch (final Exception exception) {
            this.getLogger().warning("Failed to load bundled RDT config: " + exception.getMessage());
            return ConfigSection.createDefault();
        }
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

    private void initializeRepositories(){
        this.playerRepository = new RRDTPlayer(
                this.executor,
                this.entityManagerFactory,
                RDTPlayer.class,
                RDTPlayer::getIdentifier
        );
        this.townRepository = new RRTown(
                this.executor,
                this.entityManagerFactory,
                RTown.class,
                RTown::getIdentifier
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
        final File data = this.plugin.getDataFolder();
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
                        new MainOverviewView(),
                        new ServerTownsOverviewView(),
                        new TownOverviewView()
                )
                .disableMetrics();
        this.viewFrame = frame.register();
    }

    public @Nullable net.milkbowl.vault.economy.Economy getEco() {
        if (this.economyInstance == null) return null;
        return (net.milkbowl.vault.economy.Economy) this.economyInstance;
    }

    public @NotNull JavaPlugin getPlugin() {
        return this.plugin;
    }

    public @NotNull Logger getLogger() {
        return this.plugin.getLogger();
    }

    public @NotNull Server getServer() {
        return this.plugin.getServer();
    }

    public @Nullable ExecutorService getExecutor() {
        return this.executor;
    }

    public @Nullable RPlatform getPlatform() {
        return this.platform;
    }

    public @Nullable EntityManagerFactory getEntityManagerFactory() {
        return this.entityManagerFactory;
    }

    public @Nullable RRTown getTownRepository() {
        return this.townRepository;
    }

    public @Nullable RRDTPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    public @Nullable BossBarFactory getBossBarFactory() {
        return this.bossBarFactory;
    }

    public @Nullable ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    public @Nullable PlatformType getPlatformType() {
        return this.platformType;
    }

    public @Nullable IRSFactory getIrsFactory() {
        return this.irsFactory;
    }

    public @Nullable Object getEconomyInstance() {
        return this.economyInstance;
    }

    public @Nullable ViewFrame getViewFrame() {
        return this.viewFrame;
    }
}
