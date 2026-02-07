package com.raindropcentral.rdt;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.factory.BossBarFactory;
import com.raindropcentral.rdt.factory.IRSFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPIFactory;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.hibernate.JEHibernate;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RDT extends JavaPlugin {

    private final JavaPlugin plugin;
    private final String edition;
    private ExecutorService executor;
    private final RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private RRTown townRepository;
    private RRDTPlayer playerRepository;
    private BossBarFactory bossBarFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private IRSFactory irsFactory;

    private Object economyInstance;

    public RDT(
            @NotNull JavaPlugin plugin,
            @NotNull String edition
    ) {
        this.plugin = plugin;
        this.edition = edition;
        this.platform = new RPlatform(plugin);
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

    public net.milkbowl.vault.economy.Economy getEco() {
        if (economyInstance == null) return null;
        return (net.milkbowl.vault.economy.Economy) economyInstance;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getEdition() {
        return edition;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public RPlatform getPlatform() {
        return platform;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public RRTown getTownRepository() {
        return townRepository;
    }

    public RRDTPlayer getPlayerRepository() {
        return playerRepository;
    }

    public BossBarFactory getBossBarFactory() {
        return bossBarFactory;
    }

    public ISchedulerAdapter getScheduler() {
        return scheduler;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public IRSFactory getIrsFactory() {
        return irsFactory;
    }

    public Object getEconomyInstance() {
        return economyInstance;
    }
}
