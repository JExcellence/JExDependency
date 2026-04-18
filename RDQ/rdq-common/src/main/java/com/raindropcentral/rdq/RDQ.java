/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rdq.bounty.IBountyService;
import com.raindropcentral.rdq.bounty.utility.BountyFactory;
import com.raindropcentral.rdq.bounty.visual.VisualIndicatorManager;
import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.machine.Machine;
import com.raindropcentral.rdq.database.entity.machine.MachineStorage;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.quest.*;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.database.repository.quest.PlayerQuestProgressRepository;
import com.raindropcentral.rdq.machine.MachineFactory;
import com.raindropcentral.rdq.machine.MachineManager;
import com.raindropcentral.rdq.machine.MachineRegistry;
import com.raindropcentral.rdq.machine.MachineService;
import com.raindropcentral.rdq.machine.config.FabricatorSection;
import com.raindropcentral.rdq.machine.config.MachineSystemSection;
import com.raindropcentral.rdq.machine.item.MachineItemFactory;
import com.raindropcentral.rdq.machine.repository.MachineCache;
import com.raindropcentral.rdq.machine.repository.MachineRepository;
import com.raindropcentral.rdq.machine.repository.MachineStorageRepository;
import com.raindropcentral.rdq.machine.structure.MultiBlockStructure;
import com.raindropcentral.rdq.machine.structure.StructureDetector;
import com.raindropcentral.rdq.machine.task.MachineAutoSaveTask;
import com.raindropcentral.rdq.machine.view.*;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rdq.perk.PerkSystemFactory;
import com.raindropcentral.rdq.perk.cache.SimplePerkCache;
import com.raindropcentral.rdq.permissions.PermissionsService;
import com.raindropcentral.rdq.placeholders.RDQPlaceholderExpansion;
import com.raindropcentral.rdq.quest.QuestSystemFactory;
import com.raindropcentral.rdq.quest.handler.TaskHandlerManager;
import com.raindropcentral.rdq.quest.sidebar.QuestProgressSidebarService;
import com.raindropcentral.rdq.rank.IRankSystemService;
import com.raindropcentral.rdq.rank.RankSystemFactory;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rdq.service.quest.QuestProgressTrackerImpl;
import com.raindropcentral.rdq.service.quest.QuestRewardDistributor;
import com.raindropcentral.rdq.service.quest.QuestServiceImpl;
import com.raindropcentral.rdq.service.scoreboard.PerkSidebarScoreboardService;
import com.raindropcentral.rdq.view.admin.*;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.perks.PerkDetailView;
import com.raindropcentral.rdq.view.perks.PerkOverviewView;
import com.raindropcentral.rdq.view.quest.QuestAbandonConfirmationView;
import com.raindropcentral.rdq.view.quest.QuestCategoryView;
import com.raindropcentral.rdq.view.quest.QuestDetailView;
import com.raindropcentral.rdq.view.quest.QuestListView;
import com.raindropcentral.rdq.view.ranks.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.metrics.BStatsMetrics;
import com.raindropcentral.rplatform.placeholder.PlaceholderRegistry;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.raindropcentral.rplatform.view.ConfirmationView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
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

/**
 * Represents the RDQ API type.
 */
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
	private com.raindropcentral.rdq.database.repository.quest.QuestRepository questRepository;
	
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
	private com.raindropcentral.rdq.database.repository.quest.QuestRewardRepository questRewardRepository;
	
	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestTaskRewardRepository questTaskRewardRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestUserRepository questUserRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository questCompletionHistoryRepository;

	@InjectRepository
	private com.raindropcentral.rdq.database.repository.quest.QuestTaskRepository questTaskRepository;

	private com.raindropcentral.rdq.database.repository.quest.PlayerQuestProgressRepository playerQuestProgressRepository;

	@InjectRepository
	private MachineRepository machineRepository;

	@InjectRepository
	private MachineStorageRepository machineStorageRepository;

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
	private com.raindropcentral.rdq.service.quest.QuestService questService;
	private com.raindropcentral.rdq.service.quest.QuestProgressTracker questProgressTracker;
	private com.raindropcentral.rdq.service.quest.RewardDistributor rewardDistributor;
	private com.raindropcentral.rdq.cache.quest.QuestCacheManager questCacheManager; // Quest cache manager
	private com.raindropcentral.rdq.quest.sidebar.QuestProgressSidebarService questProgressSidebarService;
	private com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache playerQuestProgressCache;
	private com.raindropcentral.rplatform.progression.ProgressionValidator<com.raindropcentral.rdq.database.entity.quest.Quest> questProgressionValidator;
	private com.raindropcentral.rdq.quest.handler.TaskHandlerManager taskHandlerManager;

	// Machine system components
	private MachineManager machineManager;
	private MachineCache machineCache;
	private MachineAutoSaveTask machineAutoSaveTask;
	private MachineService machineService;
	private StructureDetector structureDetector;
	private MachineItemFactory machineItemFactory;
	private MachineSystemSection machineSystemConfig;
	private FabricatorSection fabricatorConfig;

	/**
	 * Executes RDQ.
	 */
	public RDQ(
			@NotNull JavaPlugin plugin,
			@NotNull String edition
	) {
		this.plugin = plugin;
		this.edition = edition;
		this.platform = new RPlatform(plugin);
		this.executor = Executors.newFixedThreadPool(4);
	}

	/**
	 * Executes onEnable.
	 */
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

					initializeMachineSystem();

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
			QuestCategory.class,
			QuestCategory::getIdentifier);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestRepository.class, 
			Quest.class,
			Quest::getIdentifier);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestUserRepository.class, 
			QuestUser.class,
			QuestUser::getId);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository.class,
			QuestCompletionHistory.class,
			qch -> qch.getPlayerId().toString() + ":" + qch.getQuestIdentifier());
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestTaskRepository.class,
			com.raindropcentral.rdq.database.entity.quest.QuestTask.class,
			com.raindropcentral.rdq.database.entity.quest.QuestTask::getId);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestRewardRepository.class,
			com.raindropcentral.rdq.database.entity.quest.QuestReward.class,
			com.raindropcentral.rdq.database.entity.quest.QuestReward::getId);
		repositoryManager.register(com.raindropcentral.rdq.database.repository.quest.QuestTaskRewardRepository.class,
			com.raindropcentral.rdq.database.entity.quest.QuestTaskReward.class,
			com.raindropcentral.rdq.database.entity.quest.QuestTaskReward::getId);

		// Machine system repositories
		repositoryManager.register(MachineRepository.class, Machine.class, Machine::getId);
		repositoryManager.register(MachineStorageRepository.class, MachineStorage.class, MachineStorage::getId);

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
		
		// Initialize JExEconomy bridge for currency rewards
		com.raindropcentral.rplatform.economy.JExEconomyBridge bridge = 
				com.raindropcentral.rplatform.economy.JExEconomyBridge.getBridge();
		if (bridge != null) {
			com.raindropcentral.rplatform.reward.impl.CurrencyReward.setEconomyBridge(bridge);
			LOGGER.info("JExEconomy bridge initialized for currency rewards");
		} else {
			LOGGER.info("JExEconomy not available, currency rewards will use Vault only");
		}
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
						new QuestCategoryView(),
						new QuestListView(),
						new QuestDetailView(),
						new QuestAbandonConfirmationView(),
						new MachineRecipeView(),
						new MachineStorageView(),
						new MachineMainView(),
						new MachineUpgradeView(),
						new MachineTrustView()
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
			playerPerkCache = new SimplePerkCache(
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

			perkRequirementService = new PerkRequirementService(this, perkManagementService);

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

			// Initialize reward distributor FIRST (before progress tracker needs it)
			rewardDistributor = new QuestRewardDistributor(this);
			LOGGER.info("Reward distributor initialized");
			
			// Initialize cache manager BEFORE progress tracker (progress tracker needs it)
			questCacheManager = new QuestCacheManager(this, false);
			LOGGER.info("Quest cache manager initialized");
			
			// Manually instantiate PlayerQuestProgressRepository
			playerQuestProgressRepository = new PlayerQuestProgressRepository(
				executor,
				platform.getEntityManagerFactory(),
				 PlayerQuestProgress.class,
				PlayerQuestProgress::getPlayerId
			);
			
			playerQuestProgressCache = new PlayerQuestProgressCache(
				playerQuestProgressRepository,
				false
			);
			LOGGER.info("Player quest progress cache initialized");
			
			// Now initialize services that depend on cache manager
			questService = new QuestServiceImpl(this);
			LOGGER.info("Quest service initialized");
			
			questProgressTracker = new QuestProgressTrackerImpl(this);
			questProgressTracker.start();  // CRITICAL: Start the progress tracker!
			LOGGER.info("Quest progress tracker initialized and started");
			
			questSystemFactory = new QuestSystemFactory(this);
			
			// Initialize quest system factory (loads categories and quests from YAML to database)
			questSystemFactory.initialize().join();

			// Initialize and register task handlers
			taskHandlerManager = new TaskHandlerManager(
				this,
				questProgressTracker,
				questCacheManager,
				playerQuestProgressCache,
				null  // TODO: Load from configuration
			);
			taskHandlerManager.registerHandlers();

			questProgressSidebarService = new QuestProgressSidebarService(this);
			questProgressSidebarService.start();

			LOGGER.info("Quest system initialized successfully!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize quest system", e);
		}
	}

	/**
	 * Initializes the machine fabrication system components.
	 * Creates the machine manager, cache, and starts the auto-save task.
	 */
	private void initializeMachineSystem() {
		try {
			LOGGER.info("Initializing machine system...");

			// Load machine configurations using ConfigManager/ConfigKeeper
			ConfigManager machinesConfigManager = new ConfigManager(plugin, "machines");
			ConfigManager fabricatorConfigManager = new ConfigManager(plugin, "machines");
			
			ConfigKeeper<MachineSystemSection> systemConfigKeeper = 
				new ConfigKeeper<>(machinesConfigManager, "machines.yml", MachineSystemSection.class);
			ConfigKeeper<FabricatorSection> fabricatorConfigKeeper = 
				new ConfigKeeper<>(fabricatorConfigManager, "fabricator.yml", FabricatorSection.class);
			
			this.machineSystemConfig = systemConfigKeeper.rootSection;
			this.fabricatorConfig = fabricatorConfigKeeper.rootSection;
			
			LOGGER.info("Machine configurations loaded from files");

			// Initialize machine cache
			machineCache = new MachineCache(executor, platform.getEntityManagerFactory(), false);
			LOGGER.info("Machine cache initialized");

			// Initialize machine registry
			MachineRegistry registry = new MachineRegistry();
			LOGGER.info("Machine registry initialized");

			// Initialize machine factory
			MachineFactory factory = new MachineFactory(
				machineRepository,
				fabricatorConfig
			);
			LOGGER.info("Machine factory initialized");

			// Initialize machine manager
			machineManager = new MachineManager(
				plugin,
				registry,
				machineCache,
				factory,
				fabricatorConfig
			);
			LOGGER.info("Machine manager initialized");

			// Initialize machine service
			this.machineService = new MachineService(
				machineManager,
				factory,
				machineCache,
				machineRepository
			);
			LOGGER.info("Machine service initialized");

			// Initialize structure detector
			this.structureDetector = new StructureDetector(
				type -> new MultiBlockStructure(
					fabricatorConfig.getStructure()
				)
			);
			LOGGER.info("Structure detector initialized");

			// Initialize machine item factory
			this.machineItemFactory = new MachineItemFactory(this.plugin);
			LOGGER.info("Machine item factory initialized");

			// Start auto-save task (every 5 minutes)
			machineAutoSaveTask = new MachineAutoSaveTask(machineCache, false);
			machineAutoSaveTask.runTaskTimerAsynchronously(
				plugin,
				20L * 60 * 5,  // Initial delay: 5 minutes
				20L * 60 * 5   // Repeat: every 5 minutes
			);
			LOGGER.info("Machine auto-save task started");

			LOGGER.info("Machine system initialized successfully!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize machine system", e);
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

		if (machineAutoSaveTask != null) {
			try {
				LOGGER.info("Cancelling machine auto-save task...");
				machineAutoSaveTask.cancel();
				LOGGER.info("Machine auto-save task cancelled");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to cancel machine auto-save task", e);
			}
		}

		if (machineManager != null) {
			try {
				LOGGER.info("Shutting down machine manager...");
				machineManager.shutdown();
				LOGGER.info("Machine manager shut down successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to shutdown machine manager", e);
			}
		}

		if (machineCache != null) {
			try {
				LOGGER.info("Saving all machine caches before shutdown...");
				machineCache.autoSaveAll().thenAccept(savedCount -> {
					LOGGER.info("Saved " + savedCount + " machine caches successfully");
				}).join();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to save machine caches during shutdown", e);
			}
		}

		if (taskHandlerManager != null) {
			try {
				LOGGER.info("Unregistering quest task handlers...");
				taskHandlerManager.unregisterHandlers();
				LOGGER.info("Quest task handlers unregistered successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to unregister quest task handlers", e);
			}
		}

		if (questProgressTracker != null) {
			try {
				LOGGER.info("Shutting down quest progress tracker...");
				questProgressTracker.shutdown().join();
				LOGGER.info("Quest progress tracker shut down successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to shutdown quest progress tracker", e);
			}
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

		if (questProgressSidebarService != null) {
			questProgressSidebarService.shutdown();
		}
		
		if (visualIndicatorManager != null) {
			visualIndicatorManager.shutdown();
		}
		
		if (executor != null && !executor.isShutdown()) {
			executor.shutdown();
		}
		
		LOGGER.info("RDQ (" + edition + ") Edition disabled successfully!");
	}
	
	/**
	 * Gets the quest cache manager.
	 *
	 * @return the quest cache manager
	 */
	@NotNull
	public com.raindropcentral.rdq.cache.quest.QuestCacheManager getQuestCacheManager() {
		return questCacheManager;
	}

	/**
	 * Gets the machine manager.
	 *
	 * @return the machine manager
	 */
	@NotNull
	public MachineManager getMachineManager() {
		return machineManager;
	}

	/**
	 * Gets the machine cache.
	 *
	 * @return the machine cache
	 */
	@NotNull
	public MachineCache getMachineCache() {
		return machineCache;
	}

	/**
	 * Gets the machine repository.
	 *
	 * @return the machine repository
	 */
	@NotNull
	public MachineRepository getMachineRepository() {
		return machineRepository;
	}

	/**
	 * Gets the machine storage repository.
	 *
	 * @return the machine storage repository
	 */
	@NotNull
	public MachineStorageRepository getMachineStorageRepository() {
		return machineStorageRepository;
	}
}
