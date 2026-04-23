package de.jexcellence.quests;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry;
import de.jexcellence.jehibernate.core.JEHibernate;
import de.jexcellence.jexplatform.JExPlatform;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.logging.LogLevel;
import de.jexcellence.quests.api.JExQuestsAPI;
import de.jexcellence.quests.command.BountyHandler;
import de.jexcellence.quests.command.MachineHandler;
import de.jexcellence.quests.command.PerksHandler;
import de.jexcellence.quests.command.QuestHandler;
import de.jexcellence.quests.command.R18nCommandMessages;
import de.jexcellence.quests.command.RankHandler;
import de.jexcellence.quests.content.PerkDefinitionLoader;
import de.jexcellence.quests.content.QuestDefinitionLoader;
import de.jexcellence.quests.content.RankDefinitionLoader;
import de.jexcellence.quests.command.AdminHandler;
import de.jexcellence.quests.integration.StatisticsBridge;
import de.jexcellence.quests.machine.MachineDefinitionLoader;
import de.jexcellence.quests.machine.MachinePlacementListener;
import de.jexcellence.quests.machine.MachineRegistry;
import de.jexcellence.quests.machine.MachineStorageCloseListener;
import de.jexcellence.quests.machine.MachineTickScheduler;
import de.jexcellence.quests.perk.PerkRuntimeListener;
import de.jexcellence.quests.perk.PerkRuntimeService;
import de.jexcellence.quests.sidebar.QuestSidebarService;
import de.jexcellence.quests.view.BountyOverviewView;
import de.jexcellence.quests.view.MachineControllerView;
import de.jexcellence.quests.view.MachineTypeDetailView;
import de.jexcellence.quests.view.MachineTypeOverviewView;
import de.jexcellence.quests.view.PerkBrowserView;
import de.jexcellence.quests.view.PerkOverviewView;
import de.jexcellence.quests.view.QuestDetailView;
import de.jexcellence.quests.view.QuestOverviewView;
import de.jexcellence.quests.view.RankTreeDetailView;
import de.jexcellence.quests.view.RankTreeOverviewView;
import de.jexcellence.quests.database.repository.BountyClaimRepository;
import de.jexcellence.quests.database.repository.BountyRepository;
import de.jexcellence.quests.database.repository.MachineRepository;
import de.jexcellence.quests.database.repository.PerkRepository;
import de.jexcellence.quests.database.repository.PlayerPerkRepository;
import de.jexcellence.quests.database.repository.PlayerQuestProgressRepository;
import de.jexcellence.quests.database.repository.PlayerRankRepository;
import de.jexcellence.quests.database.repository.PlayerTaskProgressRepository;
import de.jexcellence.quests.database.repository.QuestRepository;
import de.jexcellence.quests.database.repository.QuestTaskRepository;
import de.jexcellence.quests.database.repository.QuestsPlayerRepository;
import de.jexcellence.quests.database.repository.RankRepository;
import de.jexcellence.quests.database.repository.RankTreeRepository;
import de.jexcellence.quests.service.BountyService;
import de.jexcellence.quests.service.MachineService;
import de.jexcellence.quests.service.PerkService;
import de.jexcellence.quests.service.QuestService;
import de.jexcellence.quests.service.QuestsPlayerService;
import de.jexcellence.quests.service.RankService;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Duration;

/**
 * Core orchestrator for JExQuests. Each edition subclass provides
 * {@link #metricsId()}; the lifecycle, JExPlatform bootstrap,
 * JEHibernate database, ViewFrame, services, and listener registration
 * are handled here.
 *
 * @since 1.0.0
 */
public abstract class JExQuests implements JExQuestsAPI {

    private static final String VERSION = "1.0.0";

    private final JavaPlugin plugin;
    private final String edition;

    private JExPlatform platform;
    private JEHibernate jeHibernate;
    private ViewFrame viewFrame;
    private JExLogger logger;

    private QuestsPlayerService questsPlayerService;
    private QuestService questService;
    private BountyService bountyService;
    private RankService rankService;
    private de.jexcellence.quests.service.RankPathService rankPathService;
    private PerkService perkService;
    private MachineService machineService;
    private MachineRegistry machineRegistry;
    private MachineTickScheduler machineTickScheduler;
    private QuestSidebarService questSidebarService;
    private PerkRuntimeService perkRuntime;

    protected JExQuests(@NotNull JavaPlugin plugin, @NotNull String edition) {
        this.plugin = plugin;
        this.edition = edition;
    }

    public void onLoad() {
        this.plugin.saveDefaultConfig();
        mergeTranslationKeys("translations/en_US.yml");
        extractContentTree("quests/definitions");
        extractContentTree("ranks/paths");
        extractContentTree("perks");
        extractContentTree("machines");

        this.platform = JExPlatform.builder(this.plugin)
                .withLogLevel(LogLevel.INFO)
                .enableTranslations("en_US")
                .enableMetrics(metricsId())
                .build();

        this.logger = this.platform.logger();
        this.logger.info("Loading JExQuests {} Edition", this.edition);
    }

    public void onEnable() {
        this.logger.info("Enabling JExQuests {} Edition...", this.edition);
        // Wire up the thread-aware event dispatcher before any service
        // layer fires an event — otherwise services that fire from an
        // async chain hit "may only be triggered synchronously".
        de.jexcellence.quests.util.EventDispatch.install(this.plugin);

        final ClassLoader dependencyClassLoader = Thread.currentThread().getContextClassLoader();

        this.platform.initialize()
                .thenRun(() -> {
                    Thread.currentThread().setContextClassLoader(dependencyClassLoader);
                    initializeDatabase();
                    initializeServices();
                    registerServices();
                    loadContent();
                    registerViews();
                    registerListeners();
                    registerCommands();
                    registerIntegrations();
                    this.machineTickScheduler.start();
                    this.questSidebarService.start();
                    this.perkRuntime.start();
                    onReady();
                    this.logger.info("JExQuests {} Edition enabled", this.edition);
                })
                .exceptionally(ex -> {
                    this.logger.error("Failed to enable JExQuests ({}): {}", this.edition, ex.getMessage());
                    this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
                    return null;
                });
    }

    public void onDisable() {
        onShutdown();
        if (this.questSidebarService != null) this.questSidebarService.stop();
        if (this.machineTickScheduler != null) this.machineTickScheduler.stop();
        if (this.perkRuntime != null) this.perkRuntime.stop();
        unregisterServices();
        if (this.jeHibernate != null) this.jeHibernate.close();
        if (this.platform != null) this.platform.shutdown();
        if (this.logger != null) this.logger.info("JExQuests {} Edition disabled", this.edition);
    }

    protected abstract int metricsId();

    /** Subclass hook invoked after the enable pipeline completes. */
    protected void onReady() {
    }

    /** Subclass hook invoked at the start of {@link #onDisable()}. */
    protected void onShutdown() {
    }

    @Override public @NotNull String edition() { return this.edition; }
    @Override public @NotNull String version() { return VERSION; }

    public @NotNull JavaPlugin getPlugin() { return this.plugin; }
    public @NotNull JExPlatform platform() { return this.platform; }
    public @NotNull JExLogger logger() { return this.logger; }
    public @NotNull ViewFrame viewFrame() { return this.viewFrame; }

    public @NotNull QuestsPlayerService questsPlayerService() { return this.questsPlayerService; }
    public @NotNull QuestService questService() { return this.questService; }
    public @NotNull BountyService bountyService() { return this.bountyService; }
    public @NotNull RankService rankService() { return this.rankService; }
    public @NotNull de.jexcellence.quests.service.RankPathService rankPathService() { return this.rankPathService; }
    public @NotNull PerkService perkService() { return this.perkService; }
    public @NotNull MachineService machineService() { return this.machineService; }
    public @NotNull MachineRegistry machineRegistry() { return this.machineRegistry; }
    public @NotNull MachineTickScheduler machineTickScheduler() { return this.machineTickScheduler; }
    public @NotNull QuestSidebarService questSidebarService() { return this.questSidebarService; }
    public @NotNull PerkRuntimeService perkRuntime() { return this.perkRuntime; }

    /**
     * Re-runs every YAML content loader (quests, ranks, perks, machines).
     * Admin tools call this to apply on-disk edits without a restart.
     */
    public void reloadContent() {
        loadContent();
    }

    // ── Pipeline ────────────────────────────────────────────────────────────

    private void initializeDatabase() {
        saveDefaultResource("database/hibernate.properties");
        saveDefaultResource("database/log4j.properties");

        this.jeHibernate = JEHibernate.builder()
                .configuration(config -> config.fromProperties(
                        de.jexcellence.jehibernate.config.PropertyLoader.load(
                                this.plugin.getDataFolder(), "database", "hibernate.properties")))
                .scanPackages("de.jexcellence.quests.database")
                .build();

        this.logger.info("Database initialized");
    }

    private void initializeServices() {
        final var repos = this.jeHibernate.repositories();

        final QuestRepository questRepo = repos.get(QuestRepository.class);
        final QuestTaskRepository taskRepo = repos.get(QuestTaskRepository.class);
        final QuestsPlayerRepository questsPlayerRepo = repos.get(QuestsPlayerRepository.class);
        final PlayerQuestProgressRepository questProgressRepo = repos.get(PlayerQuestProgressRepository.class);
        final PlayerTaskProgressRepository taskProgressRepo = repos.get(PlayerTaskProgressRepository.class);
        final RankTreeRepository treeRepo = repos.get(RankTreeRepository.class);
        final RankRepository rankRepo = repos.get(RankRepository.class);
        final PlayerRankRepository playerRankRepo = repos.get(PlayerRankRepository.class);
        final BountyRepository bountyRepo = repos.get(BountyRepository.class);
        final BountyClaimRepository bountyClaimRepo = repos.get(BountyClaimRepository.class);
        final PerkRepository perkRepo = repos.get(PerkRepository.class);
        final PlayerPerkRepository playerPerkRepo = repos.get(PlayerPerkRepository.class);
        final MachineRepository machineRepo = repos.get(MachineRepository.class);

        this.questsPlayerService = new QuestsPlayerService(questsPlayerRepo, this.logger);
        this.questService = new QuestService(questRepo, taskRepo, questProgressRepo, taskProgressRepo, this.logger);
        this.rankService = new RankService(treeRepo, rankRepo, playerRankRepo, this.logger);
        this.rankPathService = new de.jexcellence.quests.service.RankPathService(
                treeRepo, rankRepo, playerRankRepo, this.questsPlayerService, this.logger);
        this.bountyService = new BountyService(bountyRepo, bountyClaimRepo, this.logger);
        this.perkService = new PerkService(perkRepo, playerPerkRepo, this.logger);
        this.machineService = new MachineService(machineRepo, this.logger);
        this.machineRegistry = new MachineRegistry(this.logger);
        this.machineTickScheduler = new MachineTickScheduler(this);
        this.questSidebarService = new QuestSidebarService(this);
        this.perkRuntime = new PerkRuntimeService(this);
    }

    private void registerServices() {
        final var manager = Bukkit.getServicesManager();
        manager.register(JExQuestsAPI.class, this, this.plugin, ServicePriority.Normal);
        manager.register(QuestsPlayerService.class, this.questsPlayerService, this.plugin, ServicePriority.Normal);
        manager.register(QuestService.class, this.questService, this.plugin, ServicePriority.Normal);
        manager.register(RankService.class, this.rankService, this.plugin, ServicePriority.Normal);
        manager.register(BountyService.class, this.bountyService, this.plugin, ServicePriority.Normal);
        manager.register(PerkService.class, this.perkService, this.plugin, ServicePriority.Normal);
        manager.register(MachineService.class, this.machineService, this.plugin, ServicePriority.Normal);

        this.logger.info("Services registered on ServicesManager");
    }

    private void unregisterServices() {
        final var manager = Bukkit.getServicesManager();
        if (this.questsPlayerService != null) manager.unregister(QuestsPlayerService.class, this.questsPlayerService);
        if (this.questService != null) manager.unregister(QuestService.class, this.questService);
        if (this.rankService != null) manager.unregister(RankService.class, this.rankService);
        if (this.bountyService != null) manager.unregister(BountyService.class, this.bountyService);
        if (this.perkService != null) manager.unregister(PerkService.class, this.perkService);
        if (this.machineService != null) manager.unregister(MachineService.class, this.machineService);
        manager.unregister(JExQuestsAPI.class, this);
    }

    private void registerListeners() {
        this.plugin.getServer().getPluginManager().registerEvents(
                new de.jexcellence.quests.listener.PlayerLifecycleListener(this), this.plugin);
        this.plugin.getServer().getPluginManager().registerEvents(
                new de.jexcellence.quests.listener.QuestProgressionListener(this), this.plugin);
        this.plugin.getServer().getPluginManager().registerEvents(
                new de.jexcellence.quests.listener.FeedbackListener(this), this.plugin);
        this.plugin.getServer().getPluginManager().registerEvents(
                new MachinePlacementListener(this), this.plugin);
        this.plugin.getServer().getPluginManager().registerEvents(
                new MachineStorageCloseListener(this), this.plugin);
        this.plugin.getServer().getPluginManager().registerEvents(
                new PerkRuntimeListener(this), this.plugin);
    }

    private void loadContent() {
        final var repos = this.jeHibernate.repositories();
        new QuestDefinitionLoader(
                this.plugin,
                repos.get(QuestRepository.class),
                repos.get(QuestTaskRepository.class),
                this.logger
        ).load();
        new RankDefinitionLoader(
                this.plugin,
                repos.get(RankTreeRepository.class),
                repos.get(RankRepository.class),
                this.logger
        ).load();
        new PerkDefinitionLoader(
                this.plugin,
                repos.get(PerkRepository.class),
                this.logger
        ).load();
        new MachineDefinitionLoader(
                this.plugin,
                this.machineRegistry,
                this.logger
        ).load();
    }

    private void registerIntegrations() {
        new StatisticsBridge(this.plugin, this.logger).install();
        new de.jexcellence.quests.integration.QuestRequirementProvider(this).install();

        // PlaceholderAPI is optional — but the expansion class *extends*
        // me.clip.placeholderapi.expansion.PlaceholderExpansion, so the
        // JVM resolves that superclass the moment we instantiate our
        // subclass. That throws NoClassDefFoundError on servers without
        // PAPI. Gate the `new` on the plugin-presence check so the
        // class is only ever loaded when the PAPI runtime is reachable.
        if (this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            installPlaceholderExpansion();
        } else {
            this.logger.info("PlaceholderAPI not installed; placeholders disabled");
        }
    }

    /**
     * Isolated so the bytecode-level reference to
     * {@code QuestsPlaceholderExpansion} is only linked when this
     * method actually runs — which only happens after the PAPI
     * presence check passes. Keeps the plugin enableable on servers
     * without PlaceholderAPI.
     */
    private void installPlaceholderExpansion() {
        new de.jexcellence.quests.integration.QuestsPlaceholderExpansion(this).install();
    }

    private void registerCommands() {
        final CommandFactory factory = new CommandFactory(this.plugin, this);
        final ArgumentTypeRegistry args = ArgumentTypeRegistry.defaults()
                .register(de.jexcellence.quests.command.argument.QuestArgumentType.of(this.questService))
                .register(de.jexcellence.quests.command.argument.ActiveQuestArgumentType.of(this.questService))
                .register(de.jexcellence.quests.command.argument.PerkArgumentType.of(this.perkService))
                .register(de.jexcellence.quests.command.argument.MachineTypeArgumentType.of(this.machineRegistry))
                .register(de.jexcellence.quests.command.argument.RankTreeArgumentType.of(this.rankService));
        final R18nCommandMessages messages = new R18nCommandMessages();

        factory.registerTree("commands/quest.yml",   new QuestHandler(this).handlerMap(),   messages, args);
        factory.registerTree("commands/rank.yml",    new RankHandler(this).handlerMap(),    messages, args);
        factory.registerTree("commands/bounty.yml",  new BountyHandler(this).handlerMap(),  messages, args);
        factory.registerTree("commands/perks.yml",   new PerksHandler(this).handlerMap(),   messages, args);
        factory.registerTree("commands/machine.yml", new MachineHandler(this).handlerMap(), messages, args);
        factory.registerTree("commands/jexquests.yml", new AdminHandler(this).handlerMap(), messages, args);

        this.logger.info("Registered 6 command trees");
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerViews() {
        this.viewFrame = ViewFrame
                .create(this.plugin)
                .install(AnvilInputFeature.AnvilInput)
                .with(
                        new de.jexcellence.quests.view.QuestMainView(),
                        new QuestOverviewView(),
                        new QuestDetailView(),
                        new de.jexcellence.quests.view.RewardListView(),
                        new de.jexcellence.quests.view.RequirementListView(),
                        new BountyOverviewView(),
                        new de.jexcellence.quests.view.BountyCreationView(),
                        new PerkOverviewView(),
                        new PerkBrowserView(),
                        new de.jexcellence.quests.view.RankMainView(),
                        new RankTreeOverviewView(),
                        new RankTreeDetailView(),
                        new de.jexcellence.quests.view.RankPathOverview(),
                        new de.jexcellence.quests.view.RankPathGridView(),
                        new de.jexcellence.quests.view.RankDetailView(),
                        new de.jexcellence.quests.view.RankTopView(),
                        new de.jexcellence.quests.view.QuestObjectiveListView(),
                        new MachineTypeOverviewView(),
                        new MachineTypeDetailView(),
                        new MachineControllerView()
                )
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                    config.interactionDelay(Duration.ofMillis(100));
                })
                .disableMetrics()
                .register();
        this.logger.info("ViewFrame registered with 20 views");
    }

    private void saveDefaultResource(@NotNull String resourcePath) {
        final File target = new File(this.plugin.getDataFolder(), resourcePath.replace('/', File.separatorChar));
        if (!target.exists()) {
            this.plugin.saveResource(resourcePath, false);
        }
    }

    /**
     * Merge any <b>missing</b> keys from the shipped classpath copy of a
     * YAML translation file into the on-disk version. Existing keys —
     * including ones the admin has customised — are left untouched; only
     * net-new paths from a plugin upgrade get added. Without this, every
     * release that introduces new i18n keys (a new view, a new status
     * message) would render "Message key '…' is missing!" in chat and the
     * GUI until an admin manually deletes the translation file.
     *
     * <p>Uses Bukkit's {@link org.bukkit.configuration.file.YamlConfiguration}
     * — {@code getKeys(true)} gives a deep walk of every leaf path and
     * {@link org.bukkit.configuration.ConfigurationSection#contains}
     * tells us whether the on-disk version already has it. Only leaves
     * are copied (we skip intermediate sections) so structural nesting
     * stays correct.
     */
    private void mergeTranslationKeys(@NotNull String resourcePath) {
        final File target = new File(this.plugin.getDataFolder(), resourcePath.replace('/', File.separatorChar));
        if (!target.exists()) {
            this.plugin.saveResource(resourcePath, false);
            return;
        }
        try (var shippedIn = this.plugin.getResource(resourcePath)) {
            if (shippedIn == null) return;
            try (var reader = new java.io.InputStreamReader(shippedIn, java.nio.charset.StandardCharsets.UTF_8)) {
                final var shipped = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(reader);
                final var onDisk = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(target);
                int added = 0;
                for (final String key : shipped.getKeys(true)) {
                    if (shipped.isConfigurationSection(key)) continue;
                    if (onDisk.contains(key)) continue;
                    onDisk.set(key, shipped.get(key));
                    added++;
                }
                if (added > 0) {
                    onDisk.save(target);
                    this.logger.info("Merged {} new translation keys into {}", added, resourcePath);
                }
            }
        } catch (final java.io.IOException ex) {
            this.logger.warn("Failed to merge translation keys for {}: {}", resourcePath, ex.getMessage());
        }
    }

    /**
     * On first run, unpack every classpath resource under {@code root/}
     * into {@code plugins/JExQuests/root/}. Used to seed quest / rank /
     * perk definitions from the shipped jar so fresh installs have a
     * working content pack without the admin having to write YAML from
     * scratch. Files that already exist on disk are left untouched —
     * operators' edits win every reload.
     */
    private void extractContentTree(@NotNull String root) {
        try {
            final java.net.URL url = this.plugin.getClass().getClassLoader().getResource(root);
            if (url == null) return;
            final java.net.URI uri = url.toURI();
            if (!"jar".equals(uri.getScheme())) {
                // Dev / test classpath: resources live as plain files.
                extractFromDirectory(java.nio.file.Paths.get(uri), root);
                return;
            }
            try (java.nio.file.FileSystem fs = java.nio.file.FileSystems.newFileSystem(uri, java.util.Map.of())) {
                extractFromDirectory(fs.getPath(root), root);
            }
        } catch (final java.io.IOException | java.net.URISyntaxException ex) {
            this.logger.warn("content extraction for {} failed: {}", root, ex.getMessage());
        }
    }

    private void extractFromDirectory(@NotNull java.nio.file.Path source, @NotNull String root) throws java.io.IOException {
        final java.nio.file.Path dataRoot = this.plugin.getDataFolder().toPath().resolve(root);
        try (var stream = java.nio.file.Files.walk(source)) {
            stream.filter(java.nio.file.Files::isRegularFile).forEach(src -> {
                try {
                    final String relative = source.relativize(src).toString().replace('\\', '/');
                    final java.nio.file.Path target = dataRoot.resolve(relative);
                    if (java.nio.file.Files.exists(target)) return;
                    java.nio.file.Files.createDirectories(target.getParent());
                    java.nio.file.Files.copy(src, target);
                } catch (final java.io.IOException ex) {
                    this.logger.warn("failed to extract {}: {}", src, ex.getMessage());
                }
            });
        }
    }
}
