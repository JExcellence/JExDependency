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
import com.raindropcentral.rdq.cache.quest.QuestProgressAutoSaveTask;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.quest.*;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.database.repository.quest.PlayerQuestProgressRepository;
import com.raindropcentral.rdq.database.repository.quest.PlayerTaskProgressRepository;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rdq.perk.PerkSystemFactory;
import com.raindropcentral.rdq.perk.cache.SimplePerkCache;
import com.raindropcentral.rdq.permissions.PermissionsService;
import com.raindropcentral.rdq.placeholders.RDQPlaceholderExpansion;
import com.raindropcentral.rdq.quest.QuestConfigLoader;
import com.raindropcentral.rdq.quest.progression.QuestCompletionTracker;
import com.raindropcentral.rdq.rank.IRankSystemService;
import com.raindropcentral.rdq.rank.RankSystemFactory;
import com.raindropcentral.rdq.rank.progression.RankCompletionTracker;
import com.raindropcentral.rdq.requirement.RDQRequirementSetup;
import com.raindropcentral.rdq.reward.RDQRewardSetup;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rdq.service.RankUpgradeService;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import com.raindropcentral.rdq.service.quest.QuestProgressTrackerImpl;
import com.raindropcentral.rdq.service.quest.QuestService;
import com.raindropcentral.rdq.service.quest.QuestServiceImpl;
import com.raindropcentral.rdq.service.scoreboard.PerkSidebarScoreboardService;
import com.raindropcentral.rdq.view.admin.*;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.perks.PerkDetailView;
import com.raindropcentral.rdq.view.perks.PerkOverviewView;
import com.raindropcentral.rdq.view.quest.QuestCategoryView;
import com.raindropcentral.rdq.view.quest.QuestDetailView;
import com.raindropcentral.rdq.view.quest.QuestListView;
import com.raindropcentral.rdq.view.ranks.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.metrics.BStatsMetrics;
import com.raindropcentral.rplatform.placeholder.PlaceholderRegistry;
import com.raindropcentral.rplatform.progression.ProgressionValidator;
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
import java.util.ArrayList;
import java.util.List;
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
	private RankUpgradeService rankUpgradeService;

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
	private QuestCategoryRepository questCategoryRepository;

	@InjectRepository
	private QuestRepository questRepository;

	@InjectRepository
	private PlayerQuestProgressRepository playerQuestProgressRepository;

	@InjectRepository
	private PlayerTaskProgressRepository playerTaskProgressRepository;

	@InjectRepository
	private QuestCompletionHistoryRepository questCompletionHistoryRepository;

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
	private SimplePerkCache playerPerkCache;
    private PerkSidebarScoreboardService perkSidebarScoreboardService;
	private BStatsMetrics metrics;
	private @Nullable PlaceholderRegistry placeholderRegistry;

	// Quest system components
	private QuestService questService;
	private QuestProgressTracker questProgressTracker;
	private QuestCacheManager questCacheManager;
	private PlayerQuestProgressCache playerQuestProgressCache;
	private QuestProgressAutoSaveTask questProgressAutoSaveTask;
	private ProgressionValidator<Quest> questProgressionValidator;

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

					RDQRequirementSetup.initialize();

					RDQRewardSetup.initialize();

					// Initialize rank system with progression support
					initializeRankSystem();
					
					permissionsService = new PermissionsService(this);

					bountyService = createBountyService();
					bountyFactory = new BountyFactory(this, bountyService);

					rankSystemService = createRankSystemService();

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
			this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
			this.onDisable();
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
		repositoryManager.register(QuestCategoryRepository.class, 
			QuestCategory.class, 
			QuestCategory::getIdentifier);
		repositoryManager.register(QuestRepository.class, 
			Quest.class, 
			Quest::getIdentifier);
		repositoryManager.register(QuestCompletionHistoryRepository.class, 
			QuestCompletionHistory.class,
			QuestCompletionHistory::getId);
		repositoryManager.register(PlayerQuestProgressRepository.class,
			PlayerQuestProgress.class,
			PlayerQuestProgress::getPlayerId);
		repositoryManager.register(PlayerTaskProgressRepository.class,
			PlayerTaskProgress.class,
			PlayerTaskProgress::getId);

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
						new QuestCategoryView(),
						new QuestListView(),
						new QuestDetailView()
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
	 * Initializes the rank system components.
	 * Creates the rank factory, path service, and upgrade service with progression support.
	 */
	private void initializeRankSystem() {
		try {
			LOGGER.info("Initializing rank system...");

			// Initialize rank system factory and load rank definitions
			rankSystemFactory = new RankSystemFactory(this);
			rankSystemFactory.initialize();

			// Create completion tracker for rank progression
			RankCompletionTracker completionTracker = 
				new RankCompletionTracker(
					playerRankRepository,
					rankRepository
				);

			// Load all ranks for progression validator
			List<RRank> allRanks =
				rankRepository.findAllByAttributes(java.util.Map.of());

			// Create progression validator
			ProgressionValidator<RRank> progressionValidator = 
				new ProgressionValidator<>(
					completionTracker,
					allRanks
				);

			// Initialize rank path service with progression support
			rankPathService = new RankPathService(this, progressionValidator, completionTracker);

			// Initialize rank upgrade service
			rankUpgradeService = new RankUpgradeService(
				this,
				progressionValidator,
				completionTracker
			);

			LOGGER.info("Rank system initialized successfully!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize rank system", e);
		}
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
	 * Creates the quest factory, service, progress tracker, and cache managers.
	 */
	private void initializeQuestSystem() {
		try {
			LOGGER.info("Initializing quest system...");

			// Load quest configurations from YAML and persist to database
			final QuestConfigLoader configLoader = new QuestConfigLoader(this);
			configLoader.loadConfigurations().join();
			
			// Initialize quest definition cache manager
			questCacheManager = new QuestCacheManager(this);
			
			// Initialize player quest progress cache
			playerQuestProgressCache = new PlayerQuestProgressCache(
				playerQuestProgressRepository,
				false // Set to true for performance logging
			);
			
			// Initialize quest definition cache from database
			questCacheManager.initialize().join();
			
			// Load all quests for progression validator
			List<Quest> allQuests = new ArrayList<>(questCacheManager.getAllCategories().stream()
				.flatMap(cat -> questCacheManager.getQuestsByCategory(cat.getIdentifier()).stream())
				.toList());
			
			// Create quest completion tracker
			final QuestCompletionTracker questCompletionTracker = new QuestCompletionTracker(
				questCompletionHistoryRepository,
				playerQuestProgressCache
			);
			
			// Create quest progression validator
			questProgressionValidator = new ProgressionValidator<>(
				questCompletionTracker,
				allQuests
			);
			
			// Initialize quest service (uses cache managers and progression components)
			questService = new QuestServiceImpl(this);
			questProgressTracker = new QuestProgressTrackerImpl(this);
			
			// Start quest progress auto-save task (every 5 minutes)
			questProgressAutoSaveTask = new QuestProgressAutoSaveTask(playerQuestProgressCache);
			questProgressAutoSaveTask.runTaskTimerAsynchronously(
				plugin,
				20 * 60 * 5,  // Initial delay: 5 minutes
				20 * 60 * 5   // Repeat: every 5 minutes
			);

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

	public RankPathService getRankPathService() {
		return rankPathService;
	}

	public RankUpgradeService getRankUpgradeService() {
		return rankUpgradeService;
	}

	public RankSystemFactory getRankSystemFactory() {
		return rankSystemFactory;
	}

	/**
	 * Gets the hosting plugin instance.
	 *
	 * @return the plugin instance
	 */
	@NotNull
	public JavaPlugin getPlugin() {
		return plugin;
	}

	/**
	 * Gets the LuckPerms service.
	 *
	 * @return the LuckPerms service, or null if not available
	 */
	@Nullable
	public LuckPermsService getLuckPermsService() {
		return luckPermsService;
	}

	/**
	 * Gets the executor service.
	 *
	 * @return the executor service
	 */
	@NotNull
	public ExecutorService getExecutor() {
		return executor;
	}

	/**
	 * Gets the platform instance.
	 *
	 * @return the platform instance
	 */
	@NotNull
	public RPlatform getPlatform() {
		return platform;
	}

	/**
	 * Gets the player repository.
	 *
	 * @return the player repository
	 */
	@NotNull
	public RDQPlayerRepository getPlayerRepository() {
		return playerRepository;
	}

	/**
	 * Gets the rank repository.
	 *
	 * @return the rank repository
	 */
	@NotNull
	public RRankRepository getRankRepository() {
		return rankRepository;
	}

	/**
	 * Gets the rank tree repository.
	 *
	 * @return the rank tree repository
	 */
	@NotNull
	public RRankTreeRepository getRankTreeRepository() {
		return rankTreeRepository;
	}

	/**
	 * Gets the player rank repository.
	 *
	 * @return the player rank repository
	 */
	@NotNull
	public RPlayerRankRepository getPlayerRankRepository() {
		return playerRankRepository;
	}

	/**
	 * Gets the player rank path repository.
	 *
	 * @return the player rank path repository
	 */
	@NotNull
	public RPlayerRankPathRepository getPlayerRankPathRepository() {
		return playerRankPathRepository;
	}

	/**
	 * Gets the player rank upgrade progress repository.
	 *
	 * @return the player rank upgrade progress repository
	 */
	@NotNull
	public RPlayerRankUpgradeProgressRepository getPlayerRankUpgradeProgressRepository() {
		return playerRankUpgradeProgressRepository;
	}

	/**
	 * Gets the requirement repository.
	 *
	 * @return the requirement repository
	 */
	@NotNull
	public RRequirementRepository getRequirementRepository() {
		return requirementRepository;
	}

	/**
	 * Gets the reward repository.
	 *
	 * @return the reward repository
	 */
	@NotNull
	public RRewardRepository getRewardRepository() {
		return rewardRepository;
	}

	/**
	 * Gets the rank reward repository.
	 *
	 * @return the rank reward repository
	 */
	@NotNull
	public RRankRewardRepository getRankRewardRepository() {
		return rankRewardRepository;
	}

	/**
	 * Gets the perk repository.
	 *
	 * @return the perk repository
	 */
	@NotNull
	public PerkRepository getPerkRepository() {
		return perkRepository;
	}

	/**
	 * Gets the quest category repository.
	 *
	 * @return the quest category repository
	 */
	@NotNull
	public QuestCategoryRepository getQuestCategoryRepository() {
		return questCategoryRepository;
	}

	/**
	 * Gets the quest repository.
	 *
	 * @return the quest repository
	 */
	@NotNull
	public QuestRepository getQuestRepository() {
		return questRepository;
	}

	/**
	 * Gets the player quest progress repository.
	 *
	 * @return the player quest progress repository
	 */
	@NotNull
	public PlayerQuestProgressRepository getPlayerQuestProgressRepository() {
		return playerQuestProgressRepository;
	}

	/**
	 * Gets the player task progress repository.
	 *
	 * @return the player task progress repository
	 */
	@NotNull
	public PlayerTaskProgressRepository getPlayerTaskProgressRepository() {
		return playerTaskProgressRepository;
	}

	/**
	 * Gets the quest service.
	 *
	 * @return the quest service
	 */
	@NotNull
	public QuestService getQuestService() {
		return questService;
	}

	/**
	 * Gets the quest progress tracker.
	 *
	 * @return the quest progress tracker
	 */
	@NotNull
	public QuestProgressTracker getQuestProgressTracker() {
		return questProgressTracker;
	}

	/**
	 * Gets the quest cache manager.
	 *
	 * @return the quest cache manager
	 */
	@NotNull
	public QuestCacheManager getQuestCacheManager() {
		return questCacheManager;
	}

	/**
	 * Gets the player quest progress cache.
	 *
	 * @return the player quest progress cache
	 */
	@NotNull
	public PlayerQuestProgressCache getPlayerQuestProgressCache() {
		return playerQuestProgressCache;
	}

	/**
	 * Gets the quest progression validator.
	 *
	 * @return the quest progression validator
	 */
	@NotNull
	public ProgressionValidator<Quest> getQuestProgressionValidator() {
		return questProgressionValidator;
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

		// Shutdown quest progress auto-save task
		if (questProgressAutoSaveTask != null) {
			try {
				questProgressAutoSaveTask.cancel();
				LOGGER.info("Quest progress auto-save task cancelled");
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error cancelling quest progress auto-save task", e);
			}
		}

		// Save all dirty quest progress caches
		if (playerQuestProgressCache != null) {
			try {
				LOGGER.info("Saving all dirty quest progress caches before shutdown...");
				int savedCount = playerQuestProgressCache.autoSaveAll();
				LOGGER.info("Saved " + savedCount + " player quest progress caches successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to save quest progress caches during shutdown", e);
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
