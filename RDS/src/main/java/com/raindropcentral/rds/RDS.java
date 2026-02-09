package com.raindropcentral.rds;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.repository.RRDSPlayer;
import com.raindropcentral.rds.database.repository.RShop;
import com.raindropcentral.rds.view.shop.ShopOverviewView;
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

@SuppressWarnings("unused")
public class RDS extends JavaPlugin {

    private JavaPlugin rds;
    private ExecutorService executor;
    private RPlatform platform;
    private EntityManagerFactory entityManagerFactory;
    private ISchedulerAdapter scheduler;
    private PlatformType platformType;
    private Object economyInstance;
    private ViewFrame viewFrame;

    //Repositories
    private RRDSPlayer playerRepository;
    private RShop shopRepository;

    @Override
    public void onLoad() {
        this.rds = this;
        this.getLogger().info("Loading RPlatform for RDS");
        this.platform = new RPlatform(rds);
        this.executor = Executors.newFixedThreadPool(4);
    }


    @Override
    public void onEnable() {
        this.platformType = PlatformAPIFactory.detectPlatformType();
        this.scheduler = ISchedulerAdapter.create(this, this.platformType);
        this.executor = Executors.newFixedThreadPool(4);

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
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Disabling RDS: closing Hibernate");

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
                Shop::getShopByLocation
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
                    new ShopOverviewView()
                )
                .disableMetrics();
        this.viewFrame = frame.register();
    }

    public net.milkbowl.vault.economy.Economy getEco() {
        if (this.economyInstance == null) return null;
        return (net.milkbowl.vault.economy.Economy) this.economyInstance;
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
