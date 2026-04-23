package de.jexcellence.core;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry;
import de.jexcellence.core.api.JExCoreAPI;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.core.api.requirement.RequirementEvaluator;
import de.jexcellence.core.command.R18nCommandMessages;
import de.jexcellence.core.command.core.CoreHandler;
import de.jexcellence.core.database.repository.BossBarPreferenceRepository;
import de.jexcellence.core.database.repository.CentralServerRepository;
import de.jexcellence.core.database.repository.CorePlayerRepository;
import de.jexcellence.core.database.repository.PlayerInventoryRepository;
import de.jexcellence.core.database.repository.PlayerStatisticRepository;
import de.jexcellence.core.database.repository.StatisticRepository;
import de.jexcellence.core.service.BossBarService;
import de.jexcellence.core.service.CentralServerService;
import de.jexcellence.core.service.CorePlayerService;
import de.jexcellence.core.service.PlayerInventoryService;
import de.jexcellence.core.service.StatisticService;
import de.jexcellence.core.service.central.CentralService;
import de.jexcellence.core.service.central.ServerContext;
import de.jexcellence.core.service.requirement.DefaultRequirementEvaluator;
import de.jexcellence.core.service.reward.DefaultRewardExecutor;
import de.jexcellence.jehibernate.core.JEHibernate;
import de.jexcellence.jexplatform.JExPlatform;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.logging.LogLevel;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

/**
 * Core orchestrator for JExCore. Each edition subclass only provides
 * {@link #metricsId()}; lifecycle, JExPlatform bootstrap, JEHibernate
 * database, service registration, commands, listeners, and view frame are
 * handled here.
 */
public abstract class JExCore implements JExCoreAPI {

    private static final String VERSION = "1.0.0";

    private final JavaPlugin plugin;
    private final String edition;

    private JExPlatform platform;
    private JEHibernate jeHibernate;
    private JExLogger logger;
    private ViewFrame viewFrame;

    private CorePlayerService playerService;
    private CentralServerService centralServerService;
    private BossBarService bossBarService;
    private PlayerInventoryService inventoryService;
    private StatisticService statisticService;
    private CentralService centralService;
    private DefaultRewardExecutor rewardExecutor;
    private DefaultRequirementEvaluator requirementEvaluator;

    protected JExCore(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
    }

    public void onLoad() {
        // Drop default resources to the data folder BEFORE the platform bootstraps
        // its translation loader, so the first-run copy is guaranteed to exist.
        this.plugin.saveDefaultConfig();
        saveDefaultResource("translations/en_US.yml");

        this.platform = JExPlatform.builder(this.plugin)
                .withLogLevel(LogLevel.INFO)
                .enableTranslations("en_US")
                .enableMetrics(metricsId())
                .build();

        this.logger = this.platform.logger();
        this.logger.info("Loading JExCore {} Edition", this.edition);
    }

    public void onEnable() {
        this.logger.info("Enabling JExCore {} Edition...", this.edition);

        final ClassLoader dependencyClassLoader = Thread.currentThread().getContextClassLoader();

        this.platform.initialize()
                .thenRun(() -> {
                    Thread.currentThread().setContextClassLoader(dependencyClassLoader);
                    initializeDatabase();
                    initializeServices();
                    registerViews();
                    registerCommands();
                    startCentral();
                    onReady();
                    this.logger.info("JExCore {} Edition enabled", this.edition);
                })
                .exceptionally(ex -> {
                    this.logger.error("Failed to enable JExCore ({}): {}", this.edition, ex.getMessage());
                    this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                    return null;
                });
    }

    public void onDisable() {
        onShutdown();
        if (this.centralService != null) this.centralService.stop();
        unregisterServices();
        if (this.jeHibernate != null) this.jeHibernate.close();
        if (this.platform != null) this.platform.shutdown();
        if (this.logger != null) this.logger.info("JExCore {} Edition disabled", this.edition);
    }

    protected abstract int metricsId();

    /**
     * Subclass hook invoked after the full enable pipeline succeeds.
     * Edition delegates override this to start modules that live outside
     * {@code jexcore-common} (e.g. {@code jexcore-stats}).
     */
    protected void onReady() {
    }

    /**
     * Subclass hook invoked at the start of {@link #onDisable()}. Mirrors
     * {@link #onReady()} — tear down modules started there.
     */
    protected void onShutdown() {
    }

    @Override
    public @NotNull String edition() {
        return this.edition;
    }

    @Override
    public @NotNull String version() {
        return VERSION;
    }

    public @NotNull JavaPlugin getPlugin() {
        return this.plugin;
    }

    public @NotNull JExPlatform platform() {
        return this.platform;
    }

    public @NotNull JExLogger logger() {
        return this.logger;
    }

    public @NotNull ViewFrame viewFrame() {
        return this.viewFrame;
    }

    public @NotNull CorePlayerService playerService() {
        return this.playerService;
    }

    public @NotNull CentralServerService centralServerService() {
        return this.centralServerService;
    }

    public @NotNull BossBarService bossBarService() {
        return this.bossBarService;
    }

    public @NotNull PlayerInventoryService inventoryService() {
        return this.inventoryService;
    }

    public @NotNull StatisticService statisticService() {
        return this.statisticService;
    }

    // ── Pipeline ────────────────────────────────────────────────────────────

    private void initializeDatabase() {
        saveDefaultResource("database/hibernate.properties");
        saveDefaultResource("database/log4j.properties");

        this.jeHibernate = JEHibernate.builder()
                .configuration(config -> config.fromProperties(
                        de.jexcellence.jehibernate.config.PropertyLoader.load(
                                this.plugin.getDataFolder(), "database", "hibernate.properties")))
                .scanPackages("de.jexcellence.core.database")
                .build();

        this.logger.info("Database initialized");
    }

    private void saveDefaultResource(@NotNull String resourcePath) {
        final File target = new File(this.plugin.getDataFolder(), resourcePath.replace('/', File.separatorChar));
        if (!target.exists()) this.plugin.saveResource(resourcePath, false);
    }

    private void initializeServices() {
        final var repos = this.jeHibernate.repositories();

        final CorePlayerRepository playerRepo = repos.get(CorePlayerRepository.class);
        final CentralServerRepository serverRepo = repos.get(CentralServerRepository.class);
        final BossBarPreferenceRepository bossBarRepo = repos.get(BossBarPreferenceRepository.class);
        final PlayerInventoryRepository inventoryRepo = repos.get(PlayerInventoryRepository.class);
        final PlayerStatisticRepository playerStatRepo = repos.get(PlayerStatisticRepository.class);
        final StatisticRepository statRepo = repos.get(StatisticRepository.class);

        this.playerService = new CorePlayerService(playerRepo, this.logger);
        this.centralServerService = new CentralServerService(serverRepo, this.logger);
        this.bossBarService = new BossBarService(bossBarRepo, this.logger);
        this.inventoryService = new PlayerInventoryService(inventoryRepo, this.logger);
        this.statisticService = new StatisticService(playerStatRepo, statRepo, this.logger);
        this.rewardExecutor = new DefaultRewardExecutor(this.plugin, this.logger);
        this.requirementEvaluator = new DefaultRequirementEvaluator(this.plugin, this.logger);

        final var manager = Bukkit.getServicesManager();
        manager.register(JExCoreAPI.class, this, this.plugin, ServicePriority.Normal);
        manager.register(CorePlayerService.class, this.playerService, this.plugin, ServicePriority.Normal);
        manager.register(CentralServerService.class, this.centralServerService, this.plugin, ServicePriority.Normal);
        manager.register(BossBarService.class, this.bossBarService, this.plugin, ServicePriority.Normal);
        manager.register(PlayerInventoryService.class, this.inventoryService, this.plugin, ServicePriority.Normal);
        manager.register(StatisticService.class, this.statisticService, this.plugin, ServicePriority.Normal);
        manager.register(RewardExecutor.class, this.rewardExecutor, this.plugin, ServicePriority.Normal);
        manager.register(RequirementEvaluator.class, this.requirementEvaluator, this.plugin, ServicePriority.Normal);

        this.logger.info("Services registered on ServicesManager");
    }

    private void unregisterServices() {
        final var manager = Bukkit.getServicesManager();
        if (this.playerService != null) manager.unregister(CorePlayerService.class, this.playerService);
        if (this.centralServerService != null) manager.unregister(CentralServerService.class, this.centralServerService);
        if (this.bossBarService != null) manager.unregister(BossBarService.class, this.bossBarService);
        if (this.inventoryService != null) manager.unregister(PlayerInventoryService.class, this.inventoryService);
        if (this.statisticService != null) manager.unregister(StatisticService.class, this.statisticService);
        if (this.rewardExecutor != null) manager.unregister(RewardExecutor.class, this.rewardExecutor);
        if (this.requirementEvaluator != null) manager.unregister(RequirementEvaluator.class, this.requirementEvaluator);
        manager.unregister(JExCoreAPI.class, this);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerViews() {
        this.viewFrame = ViewFrame
                .create(this.plugin)
                .install(AnvilInputFeature.AnvilInput)
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                    config.interactionDelay(Duration.ofMillis(100));
                })
                .disableMetrics()
                .register();
    }

    private void registerCommands() {
        final var factory = new CommandFactory(this.plugin, this);
        final var registry = ArgumentTypeRegistry.defaults();
        final var messages = new R18nCommandMessages();

        factory.registerTree("commands/jexcore.yml",
                new CoreHandler(this, this.playerService).handlerMap(), messages, registry);
        factory.registerAllCommandsAndListeners();
    }

    private void startCentral() {
        final ServerContext context = new ServerContext(
                UUID.nameUUIDFromBytes(("jexcore:" + Bukkit.getServer().getName()).getBytes()),
                Bukkit.getServer().getVersion(),
                VERSION
        );
        this.centralService = new CentralService(this.plugin, this.centralServerService, this.logger, context);
        this.centralService.start();
    }
}
