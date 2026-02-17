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
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.database.repository.*;
import com.raindropcentral.rdq.economy.EconomyService;
import com.raindropcentral.rdq.economy.EconomyServiceHolder;
import com.raindropcentral.rdq.economy.JExEconomyService;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rdq.perk.PerkManagementService;
import com.raindropcentral.rdq.perk.PerkRequirementService;
import com.raindropcentral.rdq.perk.PerkSystemFactory;
import com.raindropcentral.rdq.permissions.PermissionsService;
import com.raindropcentral.rdq.rank.IRankSystemService;
import com.raindropcentral.rdq.rank.RankSystemFactory;
import com.raindropcentral.rdq.service.RankPathService;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.admin.AdminPermissionsView;
import com.raindropcentral.rdq.view.bounty.*;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.perks.PerkDetailView;
import com.raindropcentral.rdq.view.perks.PerkOverviewView;
import com.raindropcentral.rdq.view.ranks.*;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.service.ServiceRegistry;
import com.raindropcentral.rplatform.view.ConfirmationView;
import com.raindropcentral.rplatform.view.PaginatedPlayerView;
import de.jexcellence.hibernate.repository.InjectRepository;
import de.jexcellence.hibernate.repository.RepositoryManager;
import lombok.Getter;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public abstract class RDQ {

	private static final Logger LOGGER = CentralLogger.getLogger("RDQ");

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

	private LuckPermsService luckPermsService;
	private IBountyService bountyService;
	private IRankSystemService rankSystemService;
	private BountyFactory bountyFactory;
	private VisualIndicatorManager visualIndicatorManager;
	private Object currencyAdapter; // JExEconomy CurrencyAdapter
	private com.raindropcentral.rdq.economy.EconomyService economyService;
	
	// Perk system components
	private PerkSystemFactory perkSystemFactory;
	private PerkManagementService perkManagementService;
	private PerkActivationService perkActivationService;
	private PerkRequirementService perkRequirementService;

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
			LOGGER.log(Level.WARNING, "Enable sequence already in progress");
			return;
		}

		onEnableFuture = platform.initialize()
				.thenCompose(v -> {
					initializeRepositories();
					return CompletableFuture.completedFuture(null);
				})
				.thenRun(() -> {
					initializeComponents();
					initializePlugins();
					initializeViews();
					platform.initializeMetrics(getMetricsId());

					// Initialize RDQ requirement system integration
					com.raindropcentral.rdq.requirement.RDQRequirementSetup.initialize();
					
					// Initialize RDQ reward system integration
					com.raindropcentral.rdq.reward.RDQRewardSetup.initialize();

					rankPathService = new RankPathService(this);
					permissionsService = new PermissionsService(this);

					bountyService = createBountyService();
					bountyFactory = new BountyFactory(this, bountyService);

					rankSystemService = createRankSystemService();
					rankSystemFactory = new RankSystemFactory(this);
					this.rankSystemFactory.initialize();

					// Initialize perk system
					initializePerkSystem();

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

		repositoryManager.injectInto(this);
	}

	private void initializeComponents() {
		var commandFactory = new CommandFactory(plugin, this);
		commandFactory.registerAllCommandsAndListeners();
	}

	private void initializePlugins() {
		ServiceRegistry registry = new ServiceRegistry();

		registry.register(
				"net.luckperms.api.LuckPerms",
				"LuckPerms"
		).optional().maxAttempts(30).retryDelay(500).onSuccess(luckPerms -> {
			luckPermsService = new LuckPermsService(platform);
			LOGGER.log(Level.INFO, "LuckPerms service initialized");
		}).onFailure(() -> {
			LOGGER.log(Level.INFO, "LuckPerms service initialization failed, not present.");
		}).load();

		registry.register(
				"de.jexcellence.economy.adapter.CurrencyAdapter",
				"JExEconomy"
		).optional().maxAttempts(30).retryDelay(500).onSuccess(adapter -> {
			currencyAdapter = adapter;
			economyService = new JExEconomyService(adapter);
			EconomyServiceHolder.setInstance(economyService);
			
			// Register our wrapper service so RPlatform can find it
			plugin.getServer().getServicesManager().register(
					EconomyService.class,
					economyService,
					plugin,
					org.bukkit.plugin.ServicePriority.Normal
			);
			
			LOGGER.log(Level.INFO, "JExEconomy currency adapter initialized");
		}).onFailure(() -> {
			LOGGER.log(Level.INFO, "JExEconomy not found, currency requirements will be unavailable");
		}).load();
	}

	private void initializeViews() {
		ViewFrame frame = ViewFrame
				.create(plugin)
				.install(AnvilInputFeature.AnvilInput)
				.with(
						new ConfirmationView(),
						new AdminOverviewView(),
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
						new PerkDetailView()
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
	 * Registers the perk event listener and special perk handler.
	 */
	private void initializePerkSystem() {
		try {
			LOGGER.info("Initializing perk system...");
			
			// Create perk system factory and initialize
			perkSystemFactory = new PerkSystemFactory(this);
			perkSystemFactory.initialize();
			
			// Get system configuration
			PerkSystemSection systemConfig = perkSystemFactory.getPerkSystemSection();
			
			// Create perk management service
			perkManagementService = new PerkManagementService(
					perkRepository,
					playerPerkRepository,
					systemConfig.getMaxEnabledPerksPerPlayer()
			);
			
			// Create perk requirement service
			perkRequirementService = new PerkRequirementService(perkManagementService);
			
			// Create perk activation service
			perkActivationService = new PerkActivationService(
					this,
					playerPerkRepository,
					perkManagementService,
					systemConfig.getCooldownMultiplier()
			);
			
			// Register special perk handler (it implements Listener for death/damage events)
			plugin.getServer().getPluginManager().registerEvents(
					perkActivationService.getSpecialPerkHandler(), 
					plugin
			);
			LOGGER.info("Registered SpecialPerkHandler");
			
			// Start scheduled tasks (potion effect refresh, etc.)
			perkActivationService.startScheduledTasks();
			
			LOGGER.info("Perk system initialized successfully!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to initialize perk system", e);
		}
	}

	/**
	 * Called when the plugin is being disabled.
	 * Shuts down the visual indicator manager and other resources.
	 */
	public void onDisable() {
		disabling = true;
		
		// Shutdown perk system
		if (perkActivationService != null) {
			try {
				perkActivationService.stopScheduledTasks();
				perkActivationService.handleServerShutdown().join();
				LOGGER.info("Perk system shut down successfully");
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error shutting down perk system", e);
			}
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
