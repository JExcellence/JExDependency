package com.raindropcentral.rdt;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.factory.BossBarFactory;
import com.raindropcentral.rdt.factory.IRSFactory;
import com.raindropcentral.rdt.view.main.MainOverviewView;
import com.raindropcentral.rdt.view.town.ServerTownsOverviewView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class RDT extends JavaPlugin {

    private JavaPlugin plugin;
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

    @Override
    public void onLoad() {
        CentralLogger.initialize(this);

        this.plugin = this;
        this.getLogger().info("Loading RPlatform for RDT");
        this.platform = new RPlatform(plugin);
        this.platform.initialize();
        this.executor = Executors.newFixedThreadPool(4);
    }

    @Override
    public void onEnable() {
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.scheduler = this.platform.getScheduler();
        this.executor = Executors.newFixedThreadPool(4);

        try {
            initializeHibernate();
            initializeRepositories();
            this.getLogger().info("Hibernate initialized");
        } catch (Exception e) {
            onDisable();
            return;
        }
        this.getLogger().info("Connecting to economy");
        initializePlugins();
        initializeCommands();
        initializeViews();
        this.bossBarFactory = new BossBarFactory(this);
        // NO TAXATION WITHOUT REPRESENTATION
        this.irsFactory = new IRSFactory(this);
        // Initiate async tasks for each town since last taxation stored in town db
        irsFactory.runAll();
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Disabling RDT: closing Hibernate");

        if (entityManagerFactory != null) {
            try {
                entityManagerFactory.close();
            } catch (Exception ignored) {}
        }
    }

    public ConfigSection getDefaultConfig() {
        try {
            var cfgManager = new ConfigManager(this, "config");
            var cfgKeeper = new ConfigKeeper<>(cfgManager, "config.yml", ConfigSection.class);
            return cfgKeeper.rootSection;
        } catch (Exception e) {
            return new ConfigSection(new EvaluationEnvironmentBuilder());
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
        var commandFactory = new CommandFactory(this);
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
                .create(plugin)
                .install(AnvilInputFeature.AnvilInput)
                .with(
                        new MainOverviewView(),
                        new ServerTownsOverviewView(),
                        new TownOverviewView()
                )
                .disableMetrics();
        this.viewFrame = frame.register();
    }

    public net.milkbowl.vault.economy.Economy getEco() {
        if (this.economyInstance == null) return null;
        return (net.milkbowl.vault.economy.Economy) this.economyInstance;
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
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

    public RRTown getTownRepository() {
        return this.townRepository;
    }

    public RRDTPlayer getPlayerRepository() {
        return this.playerRepository;
    }

    public BossBarFactory getBossBarFactory() {
        return this.bossBarFactory;
    }

    public ISchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    public PlatformType getPlatformType() {
        return this.platformType;
    }

    public IRSFactory getIrsFactory() {
        return this.irsFactory;
    }

    public Object getEconomyInstance() {
        return this.economyInstance;
    }

    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }
}
