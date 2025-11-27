package com.raindropcentral.rdq;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.permissions.PermissionsService;
import com.raindropcentral.rdq.service.RCoreService;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rdq.utility.bounty.BountyManager;
import com.raindropcentral.rdq.utility.rank.RankSystemFactory;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.admin.AdminPermissionsView;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.perks.PerksOverviewView;
import com.raindropcentral.rdq.view.quests.QuestOverviewView;
import com.raindropcentral.rdq.view.ranks.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.aura.AuraService;
import com.raindropcentral.rplatform.api.eco.EcoJobsService;
import com.raindropcentral.rplatform.api.eco.EcoSkillsService;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.api.mcmmo.McMMOService;
import com.raindropcentral.rplatform.api.zrips.JobsRebornService;
import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.utility.PluginServiceRegistry;
import com.raindropcentral.rplatform.view.ConfirmationView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RDQImpl extends AbstractPluginDelegate<RDQ> {

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private LuckPermsService luckPermsService;

    private AuraService auraService;

    private EcoJobsService ecoJobsService;

    private EcoSkillsService ecoSkillsService;

    private JobsRebornService jobsRebornService;

    private McMMOService mcMMOService;

    private RankPathService rankPathService;

    private PermissionsService permissionsService;

    private RCoreService rCoreService;

    private ViewFrame viewFrame;

    private RDQPlayerRepository playerRepository;

    private RPlayerRankPathRepository playerRankPathRepository;

    private RPlayerRankRepository playerRankRepository;

    private RPlayerPerkRepository playerPerkRepository;

    private RBountyRepository bountyRepository;

    private RRankRepository rankRepository;

    private RPerkRepository perkRepository;

    private RPlayerRankUpgradeProgressRepository playerRankUpgradeProgressRepository;

    private RRankTreeRepository rankTreeRepository;

    private RRequirementRepository requirementRepository;

    private PluginServiceRegistry pluginServiceRegistry;

    private RankSystemFactory rankSystemFactory;

    private CommandFactory commandFactory;

    private RPlatform platform;

    private boolean isDisabling;

    private BountyManager bountyManager;

    public RDQImpl(final @NotNull RDQ rdq) {
        super(rdq);
    }

    private final static String STARTUP_MESSAGE =  """

    ===============================================================================================
                                         _____  _____   ____
                                        |  __ \\|  __ \\ / __ \\
                                        | |__) | |  | | |  | |
                                        |  _  /| |  | | |  | |
                                        | | \\ \\| |__| | |__| |
                                        |_|  \\_\\_____/ \\___\\_\\
                                        
                                      Product of Antimatter Zone LLC
                                          Powered by JExcellence
                                    
    ===============================================================================================
    Language System Initialized [Polyglot API]
    Product by: Antimatter Zone LLC
    Technology Partner: JExcellence
    Website: www.raindropcentral.com
    ===============================================================================================
    Modern i18n API: Polyglot v1.0
    Adventure Components: Enabled
    ===============================================================================================
""";

    @Override
    public void onLoad() {
        CentralLogger.initialize(this.getImpl());
        this.platform = new RPlatform(this.getImpl());
    }

    @Override
    public void onEnable() {
        this.platform.onEnable(STARTUP_MESSAGE);
        this.pluginServiceRegistry = new PluginServiceRegistry();
        this.bountyManager = new BountyManager(this);
        this.rankSystemFactory = new RankSystemFactory(this);
        this.initializeComponents();
        this.initializeViews();
        this.initializeRepositories();
        this.platform.initializeMetrics(16906);
        this.registerServices();
    }

    @Override
    public void onDisable() {
        this.isDisabling = true;
    }

    public RPlatform getPlatform() {
        return this.platform;
    }

    public RankSystemFactory getRankSystemFactory() {
        return this.rankSystemFactory;
    }

    public PluginServiceRegistry getPluginServiceRegistry() {
        return this.pluginServiceRegistry;
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public RDQPlayerRepository getPlayerRepository() {
        return this.playerRepository;
    }

    public BountyManager getBountyManager() {
        return this.bountyManager;
    }

    public RBountyRepository getBountyRepository() {
        return this.bountyRepository;
    }

    public RRankRepository getRankRepository() {
        return this.rankRepository;
    }

    public RRequirementRepository getRequirementRepository() {
        return this.requirementRepository;
    }

    public RPerkRepository getPerkRepository() {
        return this.perkRepository;
    }

    public RRankTreeRepository getRankTreeRepository() {
        return this.rankTreeRepository;
    }

    public ViewFrame getViewFrame() {
        return this.viewFrame;
    }

    public boolean isDisabling() {
        return this.isDisabling;
    }

    public RPlayerRankPathRepository getPlayerRankPathRepository() {
        return this.playerRankPathRepository;
    }

    public RPlayerRankRepository getPlayerRankRepository() {
        return this.playerRankRepository;
    }

    public LuckPermsService getLuckPermsService() {
        return this.luckPermsService;
    }

    public AuraService getAuraService() {
        return this.auraService;
    }

    public EcoJobsService getEcoJobsService() {
        return this.ecoJobsService;
    }

    public RCoreService getRCoreService() {
        return this.rCoreService;
    }

    private void initializeComponents() {
        this.commandFactory = new CommandFactory(this.getImpl());
        this.commandFactory.registerAllCommandsAndListeners();
        this.rankPathService = new RankPathService(this);
        this.permissionsService = new PermissionsService(this);
    }

    private void initializeRepositories() {
        this.playerRepository = new RDQPlayerRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.bountyRepository = new RBountyRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.rankRepository = new RRankRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.perkRepository = new RPerkRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.rankTreeRepository = new RRankTreeRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.requirementRepository = new RRequirementRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.playerRankUpgradeProgressRepository = new RPlayerRankUpgradeProgressRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.playerRankPathRepository = new RPlayerRankPathRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.playerRankRepository = new RPlayerRankRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
        this.playerPerkRepository = new RPlayerPerkRepository(
                this.executor,
                this.platform.getEntityManagerFactory()
        );
    }

    /**
     * Registers and wires external services using the ServiceWiring helper.
     * - Uses cross-classloader-safe bindings.
     * - Initializes RankSystemFactory when LuckPerms becomes available.
     * - All services are optional (same as previous behavior).
     */

    /**
     * Registers and wires external services using typed ExternalPluginService subclasses.
     * - Ensures classloader alignment (Option A).
     * - Binds RCoreAdapter typed and creates RCoreService with it.
     * - Binds LuckPerms typed and triggers rankSystemFactory.initialize() exactly once.
     */
    private void registerServices() {
        final Logger log = com.raindropcentral.rplatform.logger.CentralLogger.getLogger(getClass().getName());

        if (this.luckPermsService == null) {
            this.luckPermsService = new LuckPermsService(this.platform);
        }

        final Runnable initCallback = () -> {
            if (this.rankSystemFactory == null) {
                log.warning("rankSystemFactory is null; initialize() skipped");
                return;
            }
            try {
                this.rankSystemFactory.initialize();
                log.info("rankSystemFactory.initialize() executed");
            } catch (Throwable t) {
                log.log(Level.SEVERE, "rankSystemFactory.initialize() failed", t);
            }
        };

        com.raindropcentral.rplatform.utility.external.ServiceWiring.wireServices(
                this.pluginServiceRegistry,

                new com.raindropcentral.rdq.external.RCoreAdapterService("RCore", false, adapter -> {
                    try {
                        this.rCoreService = new com.raindropcentral.rdq.service.RCoreService(this.platform, adapter);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to construct RCoreService from adapter", t);
                    }
                }),

                new com.raindropcentral.rdq.external.LuckPermsInitGate(false, initCallback)

        );
    }


    @SuppressWarnings("UnstableApiUsage")
    private void initializeViews() {
        this.viewFrame = ViewFrame
                .create(this.getImpl())
                .defaultConfig(config -> {
                    config.cancelOnClick();
                    config.cancelOnDrag();
                    config.cancelOnDrop();
                    config.cancelOnPickup();
                    config.interactionDelay(Duration.ofMillis(100));
                })
                .with(
                        new ConfirmationView(),
                        new AdminOverviewView(),
                        new AdminPermissionsView(),
                        new BountyMainView(),
                        new BountyCreationView(),
                        new BountyOverviewView(),
                        new BountyRewardView(),
                        new BountyPlayerInfoView(),
                        new MainOverviewView(),
                        new QuestOverviewView(),
                        new PerksOverviewView(),
                        new RankMainView(),
                        new PaginatedPlayerView(),
                        new RankTreeOverviewView(),
                        new RankPathOverview(),
                        new RankPathRankRequirementOverview(),
                        new RankRequirementDetailView()
                )
                .register();
    }

    public RankPathService getRankPathService() {
        return this.rankPathService;
    }

    public PermissionsService getPermissionsService() {
        return this.permissionsService;
    }

    public RPlayerPerkRepository getPlayerPerkRepository() {
        return this.playerPerkRepository;
    }

    public RPlayerRankUpgradeProgressRepository getPlayerRankUpgradeProgressRepository() {
        return this.playerRankUpgradeProgressRepository;
    }
}