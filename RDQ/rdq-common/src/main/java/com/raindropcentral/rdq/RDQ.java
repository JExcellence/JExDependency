package com.raindropcentral.rdq;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdq.bounty.IBountyService;
import com.raindropcentral.rdq.bounty.utility.BountyFactory;
import com.raindropcentral.rdq.bounty.visual.VisualIndicatorManager;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.placeholders.RDQPlaceholderExpansion;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rdq.perk.PerkSystemFactory;
import com.raindropcentral.rdq.permissions.PermissionsService;
import com.raindropcentral.rdq.rank.IRankSystemService;
import com.raindropcentral.rdq.rank.RankSystemFactory;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rdq.service.scoreboard.PerkSidebarScoreboardService;
import com.raindropcentral.rdq.view.admin.AdminCurrencyView;
import com.raindropcentral.rdq.view.admin.AdminJobsView;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.admin.AdminPermissionsView;
import com.raindropcentral.rdq.view.admin.AdminSkillsView;
import com.raindropcentral.rdq.view.admin.PlaceholderAPIView;
import com.raindropcentral.rdq.view.admin.PluginIntegrationManagementView;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.perks.PerkDetailView;
import com.raindropcentral.rdq.view.perks.PerkOverviewView;
import com.raindropcentral.rdq.view.ranks.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.metrics.BStatsMetrics;
import com.raindropcentral.rplatform.placeholder.PlaceholderRegistry;

import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.raindropcentral.rplatform.view.ConfirmationView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;

import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.hibernate.repository.RepositoryManager;
import lombok.Getter;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public abstract class RDQ {

	private static final Logger LOGGER = Logger.getLogger("RDQ");

	private final JavaPlugin plugin;
	private final String edition;
	private final ExecutorService executor;
	private final RPlatform platform;

	private volatile CompletableFuture<Void> onEnableFuture;

	private boolean disabling;
	private boolean postEnableCompleted;

	private ViewFrame viewFrame;
	private PermissionsService permissionsService;
	private RankSystemFactory rankSystemFactory;
	private RankPathService rankPathService;

	@InjectRepository
	private RDQPlayerRepository playerRepository;

	@InjectRepository
	private BountyRepository bountyRepository;

	@InjectRepository
	private BountyHunterRepository bountyHunterRepository;

	@InjectRepository
	private RPlayerRankPathRepository playerRankPathRepository;

	@InjectRepository
	private RPlayerRankRepository playerRankRepository;

	@InjectRepository
	private RPlayerRankUpgradeProgressRepository playerRankUpgradeProgressRepository;

	@InjectRepository
	private RRankRepository rankRepository;

	@InjectRepository
	private RRankTreeRepository rankTreeRepository;
	
	@InjectRepository
	private RRankRewardRepository rankRewardRepository;

	@InjectRepository
	private RRequirementRepository requirementRepository;

	@InjectRepository
	private RRewardRepository rewardRepository;

	@InjectRepository
	private PerkRepository perkRepository;

	@InjectRepository
	private PlayerPerkRepository playerPerkRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository questCategoryRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestRepository questRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestUserRepository questUserRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository questCompletionHistoryRepository;

	private LuckPermsService luckPermsService;
	private IBountyService bountyService;
	private IRankSystemService rankSystemService;
	private BountyFactory bountyFactory;
	private VisualIndicatorManager visualIndicatorManager;
	private Object currencyAdapter;

	private PerkSystemFactory perkSystemFactory;
	private PerkManagementService perkManagementService;
	private PerkActivationService perkActivationService;
	private PerkRequirementService perkRequirementService;
	private com.raindropcentral.rdq.perk.cache.SimplePerkCache playerPerkCache;
    private PerkSidebarScoreboardService perkSidebarScoreboardService;
	private BStatsMetrics metrics;
	private @Nullable PlaceholderRegistry placeholderRegistry;

	// Quest system components
	private com.raindropcentral.rdq.quest.QuestSystemFactory questSystemFactory;
	private com.raindropcentral.rdq.quest.service.QuestService questService;
	private com.raindropcentral.rdq.quest.service.QuestProgressTracker questProgressTracker;
	private com.raindropcentral.rdq.quest.cache.QuestCacheManager questCacheManager;

	public RDQ(
			@NotNull JavaPlugin plugin,
			@NotNull String edition
	) {
		this.plugin = plugin;
		this.edition = edition;
		this.platform = new RPlatform(plugin);
		this.executor = Executors.newFixedThreadPool(4);
	}

	public void onEnable() {
		if (
				onEnableFuture != null && !onEnableFuture.isDone()
		) {
			LOGGER.warning("Enable sequence already in progress");
			return;
		}

		onEnableFuture = platform.initialize()
				.thenCompose(v -> {
					initializeRepositories();
					return CompletableFuture.completedFuture(null);
				})
				.thenRun(() -> {
					initializePlugins();
					initializeViews();
					initializeMetrics();

					com.raindropcentral.rdq.requirement.RDQRequirementSetup.initialize();

					com.raindropcentral.rdq.reward.RDQRewardSetup.initialize();

					rankPathService = new RankPathService(this);
					permissionsService = new PermissionsService(this);

					bountyService = createBountyService();
					bountyFactory = new BountyFactory(this, bountyService);

					rankSystemService = createRankSystemService();
					rankSystemFactory = new RankSystemFactory(this);
					this.rankSystemFactory.initialize();

					initializePerkSystem();
					perkSidebarScoreboardService = new PerkSidebarScoreboardService(this);
					perkSidebarScoreboardService.start();

					initializeQuestSystem();

					initializeComponents();
					initializePlaceholderExpansion();

					visualIndicatorManager = new VisualIndicatorManager(this);

					LOGGER.info(getStartupMessage());
					LOGGER.info("RDQ (" + edition + ") Edition enabled successfully!");
				})
				.exceptionally(throwable -> {
					LOGGER.log(Level.SEVERE, "Failed to initialize RDQ", throwable);
					return null;
				});
	}

	@NotNull
	protected abstract String getStartupMessage();

	protected abstract int getMetricsId();

	@NotNull
	protected abstract ViewFrame registerViews(@NotNull ViewFrame viewFrame);

	@NotNull
	protected abstract IBountyService createBountyService();

	@NotNull
	protected abstract IRankSystemService createRankSystemService();

	private void initializeMetrics() {
		final int metricsId = getMetricsId();
		if (metricsId <= 0 || this.metrics != null) {
			return;
		}

		this.metrics = new BStatsMetrics(
				plugin,
				metricsId,
				platform.getPlatformType() == PlatformType.FOLIA
		);

		final boolean premiumEdition = "premium".equalsIgnoreCase(edition);
		this.metrics.addCustomChart(new BStatsMetrics.SingleLineChart("free", () -> premiumEdition ? 0 : 1));
		this.metrics.addCustomChart(new BStatsMetrics.SingleLineChart("premium", () -> premiumEdition ? 1 : 0));
	}

	private void initializeRepositories() {
		final var emf = this.platform.getEntityManagerFactory();

		if (emf == null) {
			LOGGER.warning("EntityManagerFactory not initialized");
			return;
		}

		RepositoryManager.initialize(this.executor, emf);
		var repositoryManager = RepositoryManager.getInstance();

		repositoryManager.register(RDQPlayerRepository.class, RDQPlayer.class, RDQPlayer::getUniqueId);
		repositoryManager.register(BountyRepository.class, Bounty.class, Bounty::getId);
		repositoryManager.register(BountyHunterRepository.class, BountyHunter.class, BountyHunter::getId);
		repositoryManager.register(RPlayerRankPathRepository.class, RPlayerRankPath.class, RPlayerRankPath::getId);
		repositoryManager.register(RPlayerRankRepository.class, RPlayerRank.class, RPlayerRank::getId);
		repositoryManager.register(RPlayerRankUpgradeProgressRepository.class, RPlayerRankUpgradeProgress.class, RPlayerRankUpgradeProgress::getId);
		repositoryManager.register(RRankRepository.class, RRank.class, RRank::getIdentifier);
		repositoryManager.register(RRankTreeRepository.class, RRankTree.class, RRankTree::getIdentifier);
		repositoryManager.register(RRankRewardRepository.class, RRankReward.class, RRankReward::getId);
		repositoryManager.register(RRequirementRepository.class, BaseRequirement.class, BaseRequirement::getId);
		repositoryManager.register(RRewardRepository.class, BaseReward.class, BaseReward::getId);
		repositoryManager.register(PerkRepository.class, Perk.class, Perk::getIdentifier);
		repositoryManager.register(PlayerPerkRepository.class, PlayerPerk.class, PlayerPerk::getId);
		
		// Quest system repositories
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository.class, 
			com.raindropcentral.rdq.database.entity.quest.QuestCategory.class, 
			com.raindropcentral.rdq.database.entity.quest.QuestCategory::getIdentifier);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestRepository.class, 
			com.raindropcentral.rdq.database.entity.quest.Quest.class, 
			com.raindropcentral.rdq.database.entity.quest.Quest::getIdentifier);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestUserRepository.class, 
			com.raindropcentral.rdq.database.entity.quest.QuestUser.class, 
			com.raindropcentral.rdq.database.entity.quest.QuestUser::getId);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository.class, 
			com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory.class,
			qch -> qch.getPlayerId().toString() + ":" + qch.getQuestIdentifier());

		repositoryManager.injectInto(this);
	}

	private void initializeComponents() {
		var commandFactory = new CommandFactory(plugin, this);
		commandFactory.registerAllCommandsAndListeners();
	}

	private void initializePlugins() {
		ServiceRegistry registry = platform.getServiceRegistry();

		registry.register(
				"net.luckperms.api.LuckPerms",
				"LuckPerms"
		).optional().maxAttempts(30).retryDelay(500).onSuccess(luckPerms -> {
			luckPermsService = new LuckPermsService(platform);
			LOGGER.info("LuckPerms service initialized");
		}).onFailure(() -> {
			LOGGER.info("LuckPerms service initialization failed, not present.");
		}).load();
	}

	private void initializeViews() {
		ViewFrame frame = ViewFrame
				.create(plugin)
				.install(AnvilInputFeature.AnvilInput)
				.with(
						new ConfirmationView(),
						new AdminOverviewView(),
						new PluginIntegrationManagementView(),
						new AdminCurrencyView(),
						new AdminSkillsView(),
						new AdminJobsView(),
						new PlaceholderAPIView(),
						new AdminPermissionsView(),
						new BountyMainView(),
						new BountyRewardView(),
						new BountyCreationView(),
						new BountyPlayerInfoView(),
						new BountyOverviewView(),
						new PaginatedPlayerView(),
						new RankMainView(),
						new RankTreeOverviewView(),
						new RankRequirementDetailView(),
						new RankPathOverview(),
						new RankPathRankRequirementOverview(),
						new RankRequirementsJourneyView(),
						new MainOverviewView(),
						new RankRewardsDetailView(),
						new PerkOverviewView(),
						new PerkDetailView(),
						new com.raindropcentral.rdq.view.quest.QuestCategoryView(),
						new com.raindropcentral.rdq.view.quest.QuestListView(),
						new com.raindropcentral.rdq.view.quest.QuestDetailView()
				)
				.defaultConfig(config -> {
					config.cancelOnClick();
					config.cancelOnDrag();
					config.cancelOnDrop();
					config.cancelOnPickup();
					config.interactionDelay(Duration.ofMillis(100));
				})
				.disableMetrics();
		frame = registerViews(frame);
		this.viewFrame = frame.register();
	}

	/**
	 * Initializes the perk system components.
	 * Creates the perk factory, management service, and activation service.
	 * Registers the special perk handler.
	 */
	private void initializePerkSystem() {
		try {
			LOGGER.info("Initializing perk system...");

			perkSystemFactory = new PerkSystemFactory(this);
			perkSystemFactory.initialize();

			PerkSystemSection systemConfig = perkSystemFactory.getPerkSystemSection();

			// Initialize simple perk cache
			playerPerkCache = new com.raindropcentral.rdq.perk.cache.SimplePerkCache(
					playerPerkRepository,
					systemConfig.getCacheLogPerformance()
			);

			perkManagementService = new PerkManagementService(
					perkRepository,
					playerPerkRepository,
					systemConfig.getMaxEnabledPerksPerPlayer()
			);
			
			// Inject cache into management service
			perkManagementService.setCache(playerPerkCache);

			perkRequirementService = new PerkRequirementService(perkManagementService);

			perkActivationService = new PerkActivationService(
					this,
					playerPerkRepository,
					perkManagementService,
					systemConfig.getCooldownMultiplier()
			);
			
			// Inject cache into activation service
			perkActivationService.setCache(playerPerkCache);

			perkActivationService.startScheduledTasks();
			
			LOGGER.info("Perk system initialized successfully!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize perk system", e);
		}
	}

	/**
	 * Initializes the quest system components.
	 * Creates the quest factory, service, progress tracker, and cache manager.
	 */
	private void initializeQuestSystem() {
		try {
			LOGGER.info("Initializing quest system...");

			questService = new com.raindropcentral.rdq.quest.service.QuestServiceImpl(this);
			questProgressTracker = new com.raindropcentral.rdq.quest.service.QuestProgressTrackerImpl(this);
			questCacheManager = new com.raindropcentral.rdq.quest.cache.QuestCacheManager(this, false);
			questSystemFactory = new com.raindropcentral.rdq.quest.QuestSystemFactory(this);
			
			questSystemFactory.initialize().join();

			LOGGER.info("Quest system initialized successfully!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize quest system", e);
		}
	}

	/**
	 * Registers RDQ internal PlaceholderAPI placeholders when PlaceholderAPI is available.
	 */
	private void initializePlaceholderExpansion() {
		if (!this.isPlaceholderApiAvailable()) {
			LOGGER.info("PlaceholderAPI not detected; skipping RDQ placeholder expansion registration.");
			return;
		}

		this.placeholderRegistry = new PlaceholderRegistry(
				this.plugin,
				new RDQPlaceholderExpansion(this)
		);
		this.placeholderRegistry.register();
	}

	private boolean isPlaceholderApiAvailable() {
		return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
	}

	/**
	 * Called when the plugin is being disabled.
	 * Shuts down the visual indicator manager and other resources.
	 */
	public void onDisable() {
		disabling = true;

		if (placeholderRegistry != null) {
			placeholderRegistry.unregister();
			placeholderRegistry = null;
		}

		if (questCacheManager != null) {
			try {
				LOGGER.info("Saving all quest caches before shutdown...");
				questCacheManager.autoSaveAll();
				LOGGER.info("Quest caches saved successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to save quest caches during shutdown", e);
			}
		}

		if (playerPerkCache != null) {
			try {
				LOGGER.info("Saving all dirty perk caches before shutdown...");
				int savedCount = playerPerkCache.autoSaveAll();
				LOGGER.info("Saved " + savedCount + " player perk caches successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to save perk caches during shutdown", e);
			}
		}

		if (perkActivationService != null) {
			try {
				perkActivationService.stopScheduledTasks();
				perkActivationService.handleServerShutdown().join();
				LOGGER.info("Perk system shut down successfully");
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error shutting down perk system", e);
			}
		}

        if (perkSidebarScoreboardService != null) {
            perkSidebarScoreboardService.shutdown();
        }
		
		if (visualIndicatorManager != null) {
			visualIndicatorManager.shutdown();
		}
		
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
		}
		
		LOGGER.info("RDQ (" + edition + ") Edition disabled successfully!");
	}
}
