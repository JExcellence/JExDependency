package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.database.entity.RRequirement;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.utility.RetryExecutor;
import com.raindropcentral.rdq.utility.requirement.RequirementFactory;
import com.raindropcentral.rplatform.logger.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Factory responsible for loading, constructing, and validating the rank system from configuration files.
 * <p>
 * This refactored version provides:
 * - Cleaner separation of concerns
 * - Elimination of duplicate code
 * - Better entity relationship management
 * - Support for default ranks without rank trees
 * - Improved error handling and logging
 * - Proper Hibernate session management
 * - Transaction-scoped operations
 * - Uses new BaseRequirementSection infrastructure
 * - Utilizes RequirementFactory for proper requirement creation
 * - Custom polymorphic requirement section resolution
 * - Proper cascade handling for foreign key constraints
 * - Fixed transaction management and entity persistence order
 * </p>
 *
 * @author JExcellence
 * @version 2.6.3
 * @since TBD
 */
public class RankSystemFactory {
	
	private static final Logger LOGGER = CentralLogger.getLogger(RankSystemFactory.class);
	
	private static final String       FILE_PATH      = "rank";
	private static final String       FILE_RANK_PATH = "paths";
	private static final String       FILE_NAME      = "rank-system.yml";
	private static final List<String> INITIAL_RANKS  = new ArrayList<>(List.of(
		"cleric.yml",
		"mage.yml",
		"merchant.yml",
		"ranger.yml",
		"rogue.yml",
		"warrior.yml"
	));
	
	private final RDQImpl                 rdq;
	private final ConfigurationLoader configLoader;
	private final EntityManager       entityManager;
	private final ValidationEngine    validator;
	private final RetryExecutor       retryExecutor;
	private final RequirementFactory  requirementFactory;
	
	private volatile boolean           isInitializing = false;
	private          RankSystemSection rankSystemSection;
	private          RankSystemData    systemData;
	
	private static volatile boolean resolverRegistered = false;
	
	/**
	 * Constructs a new RankSystemFactory with all necessary components.
	 */
	public RankSystemFactory(@NotNull RDQImpl rdq) {
		
		this.rdq = rdq;
		this.configLoader = new ConfigurationLoader();
		this.entityManager = new EntityManager();
		this.validator = new ValidationEngine();
		this.retryExecutor = new RetryExecutor();
		this.systemData = new RankSystemData();
		this.requirementFactory = new RequirementFactory(this.rdq);
	}
	
	/**
	 * Initializes the rank system with comprehensive error handling and validation.
	 */
	public void initialize() {
		
		if (isInitializing) {
			LOGGER.warning("Rank system initialization already in progress, skipping duplicate call");
			return;
		}
		
		isInitializing = true;
		
		try {
			LOGGER.info("Starting rank system initialization...");
			
			loadConfigurations();
			
			validator.validateConfigurations(systemData);
			
			executeInitializationPhases();
			
			validator.validateSystem(systemData);
			
			LOGGER.info("Rank system initialization completed successfully");
			logSystemSummary();
		} catch (Exception exception) {
			LOGGER.log(
				Level.SEVERE,
				"Failed to initialize rank system",
				exception
			);
			this.clearPartialData();
		} finally {
			isInitializing = false;
		}
	}
	
	/**
	 * Loads all configuration files and sections.
	 */
	private void loadConfigurations() {
		
		LOGGER.info("Loading rank system configurations...");
		
		systemData.rankSystemSection = configLoader.loadSystemConfiguration();
		
		rankSystemSection = systemData.rankSystemSection;
		
		systemData.rankTreeSections = configLoader.loadRankTreeConfigurations();
		systemData.rankSections = configLoader.loadRankConfigurations(systemData.rankTreeSections);
		
		LOGGER.log(
			Level.INFO,
			"Configuration loading completed - Trees: " + systemData.rankTreeSections.size() + ", Total Ranks: " +
			systemData.rankSections.values().stream().mapToInt(Map::size).sum()
		);
	}
	
	/**
	 * Executes the main initialization phases synchronously with proper transaction management.
	 */
	private void executeInitializationPhases() {
		
		LOGGER.info("Executing initialization phases...");
		
		createDefaultRank();
		createRankTrees();
		createRanks();
		establishConnections();
		
		LOGGER.info("All initialization phases completed successfully");
	}
	
	/**
	 * Creates the default rank if configured (without rank tree association).
	 */
	private void createDefaultRank() {
		
		if (systemData.rankSystemSection == null) {
			LOGGER.info("No rank system configuration found, skipping default rank creation");
			return;
		}
		
		String defaultRankId = systemData.rankSystemSection.getDefaultRank().getDefaultRankIdentifier();
		if (defaultRankId == null || defaultRankId.trim().isEmpty()) {
			LOGGER.info("No default rank configured, skipping default rank creation");
			return;
		}
		
		LOGGER.log(
			Level.INFO,
			"Creating default rank: " + defaultRankId
		);
		
		retryExecutor.executeWithRetry(
			() -> {
				systemData.defaultRank = entityManager.createOrUpdateDefaultRank(
					defaultRankId,
					systemData.rankSystemSection
				);
				LOGGER.log(
					Level.INFO,
					"Default rank created/updated successfully: " + defaultRankId
				);
				return null;
			},
			"create default rank " + defaultRankId
		);
	}
	
	/**
	 * Creates all rank trees from configuration.
	 */
	private void createRankTrees() {
		
		if (systemData.rankTreeSections.isEmpty()) {
			LOGGER.info("No rank tree configurations found, skipping rank tree creation");
			return;
		}
		
		LOGGER.log(
			Level.INFO,
			"Creating " + systemData.rankTreeSections.size() + " rank trees..."
		);
		
		for (Map.Entry<String, RankTreeSection> entry : systemData.rankTreeSections.entrySet()) {
			String          treeId = entry.getKey();
			RankTreeSection config = entry.getValue();
			
			//FIX, force owner to set pre-req rank tree in order to use this method...
			if (
				config.getMinimumRankTreesToBeDone() > 0 &&
				config.getPrerequisiteRankTrees().isEmpty()
			) {
				config.setMinimumRankTreesToBeDone(0);
				LOGGER.log(
					Level.INFO,
					"Forcing config value of key 'minimumRankTreesToBeDone' to be 0, prerequisiteRankTrees field can not be empty."
				);
			}
			
			retryExecutor.executeWithRetry(
				() -> {
					RRankTree rankTree = entityManager.createOrUpdateRankTree(
						treeId,
						config
					);
					
					if (
						rankTree == null
					) {
						return null;
					}
					
					systemData.rankTrees.put(
						treeId,
						rankTree
					);
					return null;
				},
				"create rank tree " + treeId
			);
		}
		
		LOGGER.log(
			Level.INFO,
			"Successfully created " + systemData.rankTrees.size() + " rank trees"
		);
	}
	
	/**
	 * Creates all ranks and associates them with their rank trees.
	 */
	private void createRanks() {
		
		if (systemData.rankSections.isEmpty()) {
			LOGGER.info("No rank configurations found, skipping rank creation");
			return;
		}
		
		LOGGER.log(
			Level.INFO,
			"Creating ranks for " + systemData.rankSections.size() + " trees..."
		);
		
		for (Map.Entry<String, Map<String, RankSection>> treeEntry : systemData.rankSections.entrySet()) {
			String                   treeId      = treeEntry.getKey();
			Map<String, RankSection> rankConfigs = treeEntry.getValue();
			
			RRankTree rankTree = systemData.rankTrees.get(treeId);
			if (rankTree == null) {
				LOGGER.log(
					Level.WARNING,
					"Rank tree not found for ID: " + treeId + ", skipping ranks"
				);
				continue;
			}
			
			Map<String, RRank> treeRanks = new HashMap<>();
			
			for (Map.Entry<String, RankSection> rankEntry : rankConfigs.entrySet()) {
				String      rankId = rankEntry.getKey();
				RankSection config = rankEntry.getValue();
				
				retryExecutor.executeWithRetry(
					() -> {
						RRank rank = entityManager.createOrUpdateRank(
							rankId,
							config,
							rankTree
						);
						treeRanks.put(
							rankId,
							rank
						);
						LOGGER.log(
							Level.INFO,
							"Created/updated rank: " + rankId + " in tree: " + treeId
						);
						return null;
					},
					"create rank " + rankId + " in tree " + treeId
				);
			}
			
			systemData.ranks.put(
				treeId,
				treeRanks
			);
		}
		
		int totalRanks = systemData.ranks.values().stream().mapToInt(Map::size).sum();
		LOGGER.log(
			Level.INFO,
			"Successfully created " + totalRanks + " ranks across " + systemData.ranks.size() + " trees"
		);
	}
	
	/**
	 * Establishes all connections between ranks and rank trees using batch operations.
	 */
	private void establishConnections() {
		
		LOGGER.info("Establishing rank and tree connections...");
		
		retryExecutor.executeWithRetry(
			() -> {
				establishRankConnectionsBatch();
				return null;
			},
			"establish rank connections"
		);
		
		retryExecutor.executeWithRetry(
			() -> {
				establishRankTreeConnectionsBatch();
				return null;
			},
			"establish rank tree connections"
		);
		
		LOGGER.info("Connection establishment completed");
	}
	
	/**
	 * Establishes connections between individual ranks in batches.
	 */
	private void establishRankConnectionsBatch() {
		
		LOGGER.info("Establishing rank-to-rank connections...");
		
		for (Map.Entry<String, Map<String, RankSection>> treeEntry : systemData.rankSections.entrySet()) {
			String                   treeId      = treeEntry.getKey();
			Map<String, RankSection> rankConfigs = treeEntry.getValue();
			Map<String, RRank>       treeRanks   = systemData.ranks.get(treeId);
			
			if (treeRanks == null) {
				LOGGER.log(
					Level.WARNING,
					"No ranks found for tree: " + treeId + ", skipping rank connections"
				);
				continue;
			}
			
			Set<String> validRankIds = treeRanks.keySet();
			
			for (Map.Entry<String, RankSection> rankEntry : rankConfigs.entrySet()) {
				String      rankId = rankEntry.getKey();
				RankSection config = rankEntry.getValue();
				RRank       rank   = treeRanks.get(rankId);
				
				if (rank != null) {
					retryExecutor.executeWithRetry(
						() -> {
							entityManager.updateRankConnections(
								rankId,
								config,
								validRankIds
							);
							LOGGER.log(
								Level.INFO,
								"Updated connections for rank: " + rankId
							);
							return null;
						},
						"update connections for rank " + rankId
					);
				}
			}
		}
		
		LOGGER.info("Rank-to-rank connections established");
	}
	
	/**
	 * Establishes connections between rank trees in batches.
	 */
	private void establishRankTreeConnectionsBatch() {
		
		LOGGER.info("Establishing rank tree connections...");
		
		for (Map.Entry<String, RRankTree> treeEntry : systemData.rankTrees.entrySet()) {
			String          treeId = treeEntry.getKey();
			RankTreeSection config = systemData.rankTreeSections.get(treeId);
			
			if (config == null) {
				LOGGER.log(
					Level.WARNING,
					"No configuration found for rank tree: " + treeId + ", skipping tree connections"
				);
				continue;
			}
			
			retryExecutor.executeWithRetry(
				() -> {
					entityManager.updateRankTreeConnections(
						treeId,
						config,
						systemData
					);
					LOGGER.log(
						Level.INFO,
						"Updated connections for rank tree: " + treeId
					);
					return null;
				},
				"update connections for rank tree " + treeId
			);
		}
		
		LOGGER.info("Rank tree connections established");
	}
	
	/**
	 * Clears all partially initialized data.
	 */
	private void clearPartialData() {
		
		LOGGER.info("Clearing partially initialized rank system data");
		systemData = new RankSystemData();
	}
	
	/**
	 * Logs a summary of the initialized system.
	 */
	private void logSystemSummary() {
		
		int totalRanks = systemData.ranks.values().stream().mapToInt(Map::size).sum();
		LOGGER.info("=== Rank System Summary ===");
		LOGGER.log(
			Level.INFO,
			"Rank Trees: " + systemData.rankTrees.size()
		);
		LOGGER.log(
			Level.INFO,
			"Total Ranks: " + totalRanks
		);
		LOGGER.log(
			Level.INFO,
			"Default Rank: " + (
				systemData.defaultRank != null ?
				systemData.defaultRank.getIdentifier() :
				"None"
			)
		);
		LOGGER.info("===========================");
	}
	
	public Map<String, RRankTree> getRankTrees() {
		
		return Map.copyOf(systemData.rankTrees);
	}
	
	public Map<String, Map<String, RRank>> getRanks() {
		
		return systemData.ranks.entrySet().stream()
		                       .collect(Collectors.toMap(
			                       Map.Entry::getKey,
			                       entry -> Map.copyOf(entry.getValue())
		                       ));
	}
	
	@Nullable
	public RRank getDefaultRank() {
		
		return systemData.defaultRank;
	}
	
	public boolean isInitialized() {
		
		return ! systemData.rankTrees.isEmpty() || systemData.defaultRank != null;
	}
	
	/**
	 * Data container for all rank system components.
	 */
	public static class RankSystemData {
		
		RankSystemSection                     rankSystemSection;
		Map<String, RankTreeSection>          rankTreeSections = new HashMap<>();
		Map<String, Map<String, RankSection>> rankSections     = new HashMap<>();
		Map<String, RRankTree>                rankTrees        = new HashMap<>();
		Map<String, Map<String, RRank>>       ranks            = new HashMap<>();
		RRank                                 defaultRank;
		
	}
	
	/**
	 * Handles all configuration loading operations with enhanced auto-key generation.
	 */
	private class ConfigurationLoader {
		
		RankSystemSection loadSystemConfiguration() {
			
			try {
				ConfigManager                   cfgManager = new ConfigManager(
					rdq.getImpl(),
					FILE_PATH
				);
				ConfigKeeper<RankSystemSection> cfgKeeper  = new ConfigKeeper<>(
					cfgManager,
					FILE_NAME,
					RankSystemSection.class
				);
				
				if (new File(rdq.getImpl().getDataFolder() + "/" + FILE_PATH + "/" + FILE_RANK_PATH).mkdir()) {
					LOGGER.log(
						Level.INFO,
						"Successfully created " + FILE_RANK_PATH + " folder."
					);
				}
				
				LOGGER.log(
					Level.INFO,
					"Successfully loaded rank system configuration"
				);
				return cfgKeeper.rootSection;
			} catch (Exception exception) {
				LOGGER.log(
					Level.WARNING,
					"Error loading rank system configuration, using fallback",
					exception
				);
				return new RankSystemSection(new EvaluationEnvironmentBuilder());
			}
		}
		
		Map<String, RankTreeSection> loadRankTreeConfigurations() {
			
			Map<String, RankTreeSection> sections = new HashMap<>();
			File                         folder   = new File(
				rdq.getImpl().getDataFolder(),
				FILE_PATH + "/" + FILE_RANK_PATH
			);
			
			if (! folder.exists() || ! folder.isDirectory()) {
				LOGGER.log(
					Level.INFO,
					"Rank tree directory not found: " + folder.getAbsolutePath()
				);
				return sections;
			}
			
			// Load initial ranks first
			for (String fileName : INITIAL_RANKS) {
				try {
					String          treeId  = convertIdentifier(fileName);
					RankTreeSection section = loadSingleRankTreeConfiguration(fileName);
					
					// Set the tree ID for auto-key generation
					section.setTreeId(treeId);
					
					// Trigger afterParsing to generate keys
					section.afterParsing(new ArrayList<>());
					
					sections.put(
						treeId,
						section
					);
					LOGGER.log(
						Level.INFO,
						"Loaded rank tree configuration: " + treeId
					);
				} catch (Exception exception) {
					LOGGER.log(
						Level.WARNING,
						"Failed to load rank tree configuration: " + fileName,
						exception
					);
				}
			}
			
			// Load additional files
			File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
			if (files == null || files.length == 0) {
				LOGGER.info("No additional rank tree configuration files found");
				return sections;
			}
			
			for (File file : Arrays.stream(files).filter(file -> ! INITIAL_RANKS.contains(file.getName().toLowerCase())).toList()) {
				try {
					String          treeId  = convertIdentifier(file.getName());
					RankTreeSection section = loadSingleRankTreeConfiguration(file.getName());
					
					// Set the tree ID for auto-key generation
					section.setTreeId(treeId);
					
					// Trigger afterParsing to generate keys
					section.afterParsing(new ArrayList<>());
					
					sections.put(
						treeId,
						section
					);
					LOGGER.log(
						Level.INFO,
						"Loaded rank tree configuration: " + treeId
					);
				} catch (Exception exception) {
					LOGGER.log(
						Level.WARNING,
						"Failed to load rank tree configuration: " + file.getName(),
						exception
					);
				}
			}
			
			LOGGER.log(
				Level.INFO,
				"Successfully loaded " + sections.size() + " rank tree configurations"
			);
			return sections;
		}
		
		/**
		 * Loads a single rank tree configuration with fallback handling for requirement sections.
		 */
		private RankTreeSection loadSingleRankTreeConfiguration(String fileName) throws Exception {
			
			try {
				ConfigManager                 cfgManager = new ConfigManager(
					rdq.getImpl(),
					FILE_PATH + "/" + FILE_RANK_PATH
				);
				ConfigKeeper<RankTreeSection> cfgKeeper  = new ConfigKeeper<>(
					cfgManager,
					fileName,
					RankTreeSection.class
				);
				return cfgKeeper.rootSection;
			} catch (Exception e) {
				if (e.getMessage() != null && e.getMessage().contains("InstantiationException")) {
					LOGGER.log(
						Level.WARNING,
						"Polymorphic requirement loading failed for " + fileName + ", attempting fallback approach"
					);
					return loadRankTreeWithFallback(fileName);
				}
				throw e;
			}
		}
		
		/**
		 * Fallback method to load rank tree configuration without polymorphic requirements.
		 * This temporarily removes requirements from the configuration to allow loading.
		 */
		private RankTreeSection loadRankTreeWithFallback(String fileName) throws Exception {
			
			LOGGER.log(
				Level.INFO,
				"Using fallback configuration loading for: " + fileName
			);
			
			ConfigManager cfgManager = new ConfigManager(
				rdq.getImpl(),
				FILE_PATH + "/" + FILE_RANK_PATH
			);
			
			try {
				ConfigKeeper<RankTreeSection> cfgKeeper = new ConfigKeeper<>(
					cfgManager,
					fileName,
					RankTreeSection.class
				);
				return cfgKeeper.rootSection;
			} catch (Exception fallbackException) {
				LOGGER.log(
					Level.SEVERE,
					"Even fallback loading failed for: " + fileName,
					fallbackException
				);
				throw fallbackException;
			}
		}
		
		Map<String, Map<String, RankSection>> loadRankConfigurations(Map<String, RankTreeSection> treeSections) {
			
			Map<String, Map<String, RankSection>> allRanks = new HashMap<>();
			
			treeSections.forEach((treeId, treeSection) -> {
				Map<String, RankSection> ranks = treeSection.getRanks();
				if (ranks != null && ! ranks.isEmpty()) {
					ranks.forEach((rankId, rankSection) -> {
						// Set context for the rank
						rankSection.setRankTreeName(treeId);
						rankSection.setRankName(rankId);
						
						// Process requirements and set their context
						processRankRequirements(
							rankSection,
							treeId,
							rankId
						);
						
						// Trigger afterParsing to generate keys (this was already done in RankTreeSection.afterParsing)
						// but we'll do it again to ensure requirements are processed
						try {
							rankSection.afterParsing(new ArrayList<>());
						} catch (Exception e) {
							LOGGER.log(
								Level.WARNING,
								"Failed to process rank " + rankId + " in tree " + treeId + ": " + e.getMessage()
							);
						}
					});
					
					allRanks.put(
						treeId,
						ranks
					);
					LOGGER.log(
						Level.INFO,
						"Loaded " + ranks.size() + " rank configurations for tree: " + treeId
					);
				}
			});
			
			return allRanks;
		}
		
		/**
		 * Processes requirements for a rank and sets their context for auto-key generation.
		 */
		private void processRankRequirements(
			RankSection rankSection,
			String treeId,
			String rankId
		) {
			
			Map<String, BaseRequirementSection> requirements = rankSection.getRequirements();
			if (requirements != null && ! requirements.isEmpty()) {
				requirements.forEach((requirementKey, baseRequirementSection) -> {
					if (baseRequirementSection != null) {
						// Set context for auto-key generation
						baseRequirementSection.setContext(
							treeId,
							rankId,
							requirementKey
						);
						
						// Trigger afterParsing to generate keys
						try {
							baseRequirementSection.afterParsing(new ArrayList<>());
						} catch (Exception e) {
							LOGGER.log(
								Level.WARNING,
								"Failed to process requirement " + requirementKey + " for rank " + rankId + " in tree " + treeId + ": " + e.getMessage()
							);
						}
					}
				});
			}
		}
		
		private String convertIdentifier(String identifier) {
			
			return identifier.replace(
				                 ".yml",
				                 ""
			                 )
			                 .replace(
				                 " ",
				                 ""
			                 )
			                 .replace(
				                 "-",
				                 "_"
			                 )
			                 .toLowerCase();
		}
		
	}
	
	/**
	 * Manages all entity creation and update operations with proper session handling.
	 */
	private class EntityManager {
		
		RRank createOrUpdateDefaultRank(
			String rankId,
			RankSystemSection systemConfig
		) {
			
			try {
				RRank existingRank = rdq.getRankRepository().findByAttributes(Map.of(
					"identifier",
					rankId
				));
				
				if (existingRank != null) {
					LOGGER.log(
						Level.INFO,
						"Found existing default rank: " + rankId
					);
					return existingRank;
				}
				
				RRank defaultRank = new RRank(
					rankId,
					systemConfig.getDefaultRank().getDisplayNameKey(),
					systemConfig.getDefaultRank().getDescriptionKey(),
					systemConfig.getDefaultRank().getLuckPermsGroup(),
					systemConfig.getDefaultRank().getPrefixKey(),
					systemConfig.getDefaultRank().getSuffixKey(),
					systemConfig.getDefaultRank().getIcon(),
					true,
					systemConfig.getDefaultRank().getTier(),
					systemConfig.getDefaultRank().getWeight()
				);
				
				rdq.getRankRepository().create(defaultRank);
				LOGGER.log(
					Level.INFO,
					"Created new default rank: " + rankId
				);
				return defaultRank;
			} catch (Exception e) {
				LOGGER.log(
					Level.SEVERE,
					"Failed to create/update default rank: " + rankId,
					e
				);
				throw new RuntimeException(
					"Failed to create default rank",
					e
				);
			}
		}
		
		@Nullable RRankTree createOrUpdateRankTree(
			String treeId,
			RankTreeSection config
		) {
			
			try {
				RRankTree existingTree = rdq.getRankTreeRepository().findByAttributes(Map.of(
					"identifier",
					treeId
				));
				
				if (existingTree != null) {
					existingTree.setDisplayOrder(config.getDisplayOrder());
					existingTree.setMinimumRankTreesToBeDone(config.getMinimumRankTreesToBeDone());
					existingTree.setFinalRankTree(config.getFinalRankTree());
					
					rdq.getRankTreeRepository().update(existingTree);
					LOGGER.log(
						Level.INFO,
						"Updated existing rank tree: " + treeId
					);
					return existingTree;
				}
				
				RRankTree rankTree = new RRankTree(
					treeId,
					config.getDisplayNameKey(),
					config.getDescriptionKey(),
					config.getIcon(),
					config.getDisplayOrder(),
					config.getMinimumRankTreesToBeDone(),
					config.getEnabled(),
					config.getFinalRankTree()
				);
				
				rdq.getRankTreeRepository().create(rankTree);
				LOGGER.log(
					Level.INFO,
					"Created new rank tree: " + treeId
				);
				return rankTree;
			} catch (
				final Exception exception) {
				LOGGER.log(
					Level.WARNING,
					"Failed to create/update rank tree: " + treeId,
					exception
				);
			}
			
			return null;
		}
		
		RRank createOrUpdateRank(
			String rankId,
			RankSection config,
			RRankTree rankTree
		) {
			
			try {
				RRank existingRank = rdq.getRankRepository().findByAttributes(Map.of(
					"identifier",
					rankId
				));
				
				if (existingRank != null) {
					boolean needsUpdate = false;
					
					if (existingRank.getRankTree() == null && rankTree != null) {
						needsUpdate = true;
					} else if (existingRank.getRankTree() != null && rankTree == null) {
						needsUpdate = true;
					} else if (existingRank.getRankTree() != null && rankTree != null) {
						Long existingTreeId = existingRank.getRankTree().getId();
						Long newTreeId      = rankTree.getId();
						needsUpdate = ! Objects.equals(
							existingTreeId,
							newTreeId
						);
					}
					
					if (needsUpdate) {
						existingRank.setRankTree(rankTree);
						rdq.getRankRepository().update(existingRank);
						LOGGER.log(
							Level.INFO,
							"Updated rank tree association for rank: " + rankId
						);
					}
					
					try {
						updateRankRequirements(
							existingRank,
							config
						);
						LOGGER.log(
							Level.INFO,
							"Updated requirements for existing rank: " + rankId
						);
					} catch (Exception reqException) {
						LOGGER.log(
							Level.WARNING,
							"Failed to update requirements for existing rank: " + rankId,
							reqException
						);
					}
					
					LOGGER.log(
						Level.INFO,
						"Found existing rank: " + rankId
					);
					return existingRank;
				}
				
				RRank rank = new RRank(
					rankId,
					config.getDisplayNameKey(),
					config.getDescriptionKey(),
					config.getLuckPermsGroup(),
					config.getPrefixKey(),
					config.getSuffixKey(),
					config.getIcon(),
					config.getInitialRank(),
					config.getTier(),
					config.getWeight(),
					rankTree
				);
				
				rdq.getRankRepository().create(rank);
				LOGGER.log(
					Level.INFO,
					"Created new rank: " + rankId
				);
				
				try {
					updateRankRequirements(
						rank,
						config
					);
					LOGGER.log(
						Level.INFO,
						"Successfully parsed and assigned requirements for rank: " + rankId
					);
				} catch (Exception reqException) {
					LOGGER.log(
						Level.WARNING,
						"Failed to parse requirements for rank: " + rankId + ", continuing without requirements",
						reqException
					);
				}
				
				return rank;
			} catch (Exception e) {
				LOGGER.log(
					Level.SEVERE,
					"Failed to create/update rank: " + rankId,
					e
				);
				throw new RuntimeException(
					"Failed to create rank",
					e
				);
			}
		}
		
		/**
		 * Updates rank requirements with proper transaction management and entity persistence order.
		 * This method ensures that RRequirement entities are saved before RRankUpgradeRequirement entities
		 * and handles all operations within a single transaction scope.
		 */
		private void updateRankRequirements(
			RRank rank,
			RankSection config
		) {
			try {
				LOGGER.log(Level.INFO, "Starting requirement update for rank: " + rank.getIdentifier());
				
				// Step 1: Clean up existing player progress to prevent foreign key violations
				cleanupPlayerProgressForRank(rank);
				
				// Step 2: Parse new requirements from configuration
				List<RRankUpgradeRequirement> upgradeRequirements = requirementFactory.parseRequirements(
					rank,
					config.getRequirements()
				);
				
				if (upgradeRequirements.isEmpty()) {
					LOGGER.log(Level.INFO, "No requirements to process for rank: " + rank.getIdentifier());
					
					// Clear existing requirements and update rank
					rank.getUpgradeRequirements().clear();
					rdq.getRankRepository().update(rank);
					return;
				}
				
				// Step 3: Process requirements in a single transaction-like approach
				List<RRankUpgradeRequirement> processedRequirements = new ArrayList<>();
				
				for (RRankUpgradeRequirement upgradeReq : upgradeRequirements) {
					try {
						RRequirement requirement = upgradeReq.getRequirement();
						
						// Save the requirement first if it's not persisted
						if (requirement.getId() == null) {
							LOGGER.log(Level.FINE, "Saving requirement entity for rank: " + rank.getIdentifier());
							requirement = rdq.getRequirementRepository().create(requirement);
							LOGGER.log(Level.FINE, "Saved requirement entity with ID: " + requirement.getId());
						}
						
						// Create a new upgrade requirement with the persisted requirement
						// Don't set the rank yet to avoid circular reference issues
						RRankUpgradeRequirement persistedUpgradeReq = new RRankUpgradeRequirement(
							null, // Don't set rank yet
							requirement,
							upgradeReq.getIcon()
						);
						persistedUpgradeReq.setDisplayOrder(upgradeReq.getDisplayOrder());
						
						processedRequirements.add(persistedUpgradeReq);
						
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Failed to process individual requirement for rank: " + rank.getIdentifier(), e);
						// Continue with other requirements instead of failing completely
					}
				}
				
				// Step 4: Clear existing requirements and add new ones
				rank.getUpgradeRequirements().clear();
				
				// Step 5: Add all processed requirements to the rank
				for (RRankUpgradeRequirement processedReq : processedRequirements) {
					boolean added = rank.addUpgradeRequirement(processedReq);
					if (added) {
						LOGGER.log(Level.FINE, "Successfully added upgrade requirement to rank: " + rank.getIdentifier());
					} else {
						LOGGER.log(Level.WARNING, "Failed to add upgrade requirement to rank: " + rank.getIdentifier());
					}
				}
				
				LOGGER.log(Level.INFO, "Rank " + rank.getIdentifier() + " now has " + rank.getUpgradeRequirements().size() + " upgrade requirements");
				
				// Step 6: Update the rank with all requirements in a single transaction
				// Use a fresh entity manager session to avoid closed connection issues
				try {
					rdq.getRankRepository().update(rank);
					LOGGER.log(Level.INFO, "Successfully updated rank with requirements: " + rank.getIdentifier());
				} catch (Exception updateException) {
					LOGGER.log(Level.SEVERE, "Failed to update rank in database: " + rank.getIdentifier(), updateException);
					
					// Try to refresh the rank and retry the update
					try {
						RRank freshRank = rdq.getRankRepository().findByAttributes(Map.of("identifier", rank.getIdentifier()));
						if (freshRank != null) {
							// Clear and re-add requirements to fresh entity
							freshRank.getUpgradeRequirements().clear();
							for (RRankUpgradeRequirement processedReq : processedRequirements) {
								freshRank.addUpgradeRequirement(processedReq);
							}
							rdq.getRankRepository().update(freshRank);
							LOGGER.log(Level.INFO, "Successfully updated rank with fresh entity: " + rank.getIdentifier());
						} else {
							throw new RuntimeException("Could not find rank for refresh: " + rank.getIdentifier());
						}
					} catch (Exception retryException) {
						LOGGER.log(Level.SEVERE, "Failed to update rank even with fresh entity: " + rank.getIdentifier(), retryException);
						throw retryException;
					}
				}
				
			} catch (Exception exception) {
				LOGGER.log(Level.SEVERE, "Failed to update requirements for rank: " + rank.getIdentifier(), exception);
				throw new RuntimeException("Failed to update rank requirements", exception);
			}
		}
		
		/**
		 * Alternative method that uses a more conservative approach with individual requirement persistence.
		 * This method can be used as a fallback if the main method still has issues.
		 */
		private void updateRankRequirementsConservative(
			RRank rank,
			RankSection config
		) {
			try {
				LOGGER.log(Level.INFO, "Starting conservative requirement update for rank: " + rank.getIdentifier());
				
				// Step 1: Clean up existing player progress
				cleanupPlayerProgressForRank(rank);
				
				// Step 2: Clear existing requirements
				rank.getUpgradeRequirements().clear();
				rdq.getRankRepository().update(rank);
				
				// Step 3: Parse new requirements
				List<RRankUpgradeRequirement> upgradeRequirements = requirementFactory.parseRequirements(
					rank,
					config.getRequirements()
				);
				
				if (upgradeRequirements.isEmpty()) {
					LOGGER.log(Level.INFO, "No requirements to process for rank: " + rank.getIdentifier());
					return;
				}
				
				// Step 4: Process each requirement individually
				for (RRankUpgradeRequirement upgradeReq : upgradeRequirements) {
					try {
						// Save requirement first
						RRequirement requirement = upgradeReq.getRequirement();
						if (requirement.getId() == null) {
							requirement = rdq.getRequirementRepository().create(requirement);
						}
						
						// Create upgrade requirement with persisted requirement
						RRankUpgradeRequirement persistedUpgradeReq = new RRankUpgradeRequirement(
							rank,
							requirement,
							upgradeReq.getIcon()
						);
						persistedUpgradeReq.setDisplayOrder(upgradeReq.getDisplayOrder());
						
						// The constructor should handle adding to rank automatically
						LOGGER.log(Level.FINE, "Added requirement to rank: " + rank.getIdentifier());
						
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to process individual requirement for rank: " + rank.getIdentifier(), e);
					}
				}
				
				// Step 5: Final update
				rdq.getRankRepository().update(rank);
				LOGGER.log(Level.INFO, "Conservative update completed for rank: " + rank.getIdentifier());
				
			} catch (Exception exception) {
				LOGGER.log(Level.SEVERE, "Conservative requirement update failed for rank: " + rank.getIdentifier(), exception);
				throw new RuntimeException("Failed to update rank requirements conservatively", exception);
			}
		}
		
		/**
		 * Cleans up player progress records that reference upgrade requirements for the given rank.
		 * This prevents foreign key constraint violations when clearing upgrade requirements.
		 */
		private void cleanupPlayerProgressForRank(RRank rank) {
			
			try {
				Set<RRankUpgradeRequirement> upgradeRequirements = rank.getUpgradeRequirements();
				
				if (upgradeRequirements.isEmpty()) {
					return;
				}
				
				LOGGER.log(
					Level.INFO,
					"Cleaning up player progress for " + upgradeRequirements.size() + " upgrade requirements in rank: " + rank.getIdentifier()
				);
				
				for (RRankUpgradeRequirement upgradeReq : upgradeRequirements) {
					List<RPlayerRankUpgradeProgress> progressRecords = rdq.getPlayerRankUpgradeProgressRepository().findListByAttributes(Map.of(
						"upgradeRequirement",
						upgradeReq
					));
					
					if (! progressRecords.isEmpty()) {
						LOGGER.log(
							Level.INFO,
							"Deleting " + progressRecords.size() + " player progress records for upgrade requirement ID: " + upgradeReq.getId()
						);
						
						for (RPlayerRankUpgradeProgress progress : progressRecords) {
							rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId());
						}
					}
				}
				
				LOGGER.log(
					Level.INFO,
					"Player progress cleanup completed for rank: " + rank.getIdentifier()
				);
			} catch (Exception e) {
				LOGGER.log(
					Level.WARNING,
					"Failed to cleanup player progress for rank: " + rank.getIdentifier(),
					e
				);
			}
		}
		
		/**
		 * Updates rank connections by fetching fresh entity from database to avoid session issues.
		 */
		void updateRankConnections(
			String rankId,
			RankSection config,
			Set<String> validRankIds
		) {
			
			try {
				RRank rank = rdq.getRankRepository().findByAttributes(Map.of(
					"identifier",
					rankId
				));
				
				if (rank == null) {
					LOGGER.log(
						Level.WARNING,
						"Rank not found for connection update: " + rankId
					);
					return;
				}
				
				List<String> filteredPrevious = config.getPreviousRanks().stream()
				                                      .filter(validRankIds::contains)
				                                      .collect(Collectors.toList());
				rank.setPreviousRanks(filteredPrevious);
				
				List<String> filteredNext = config.getNextRanks().stream()
				                                  .filter(validRankIds::contains)
				                                  .collect(Collectors.toList());
				rank.setNextRanks(filteredNext);
				
				rdq.getRankRepository().update(rank);
			} catch (Exception e) {
				LOGGER.log(
					Level.SEVERE,
					"Failed to update rank connections for: " + rankId,
					e
				);
				throw new RuntimeException(
					"Failed to update rank connections",
					e
				);
			}
		}
		
		/**
		 * Updates rank tree connections by fetching fresh entity from database and handling lazy collections properly.
		 */
		void updateRankTreeConnections(
			String treeId,
			RankTreeSection config,
			RankSystemData systemData
		) {
			
			try {
				RRankTree rankTree = rdq.getRankTreeRepository().findByAttributes(Map.of(
					"identifier",
					treeId
				));
				
				if (rankTree == null) {
					LOGGER.log(
						Level.WARNING,
						"Rank tree not found for connection update: " + treeId
					);
					return;
				}
				
				List<RRankTree> preReqTrees = config.getPrerequisiteRankTrees().stream()
				                                    .map(preReqId -> rdq.getRankTreeRepository().findByAttributes(Map.of(
					                                    "identifier",
					                                    preReqId
				                                    )))
				                                    .filter(Objects::nonNull)
				                                    .collect(Collectors.toList());
				rankTree.setPrerequisiteRankTrees(preReqTrees);
				
				List<RRankTree> unlockedTrees = config.getUnlockedRankTrees().stream()
				                                      .map(unlockedId -> rdq.getRankTreeRepository().findByAttributes(Map.of(
					                                      "identifier",
					                                      unlockedId
				                                      )))
				                                      .filter(Objects::nonNull)
				                                      .collect(Collectors.toList());
				rankTree.setUnlockedRankTrees(unlockedTrees);
				
				List<RRankTree> connectedTrees = config.getConnectedRankTrees().stream()
				                                       .map(connectedId -> rdq.getRankTreeRepository().findByAttributes(Map.of(
					                                       "identifier",
					                                       connectedId
				                                       )))
				                                       .filter(Objects::nonNull)
				                                       .collect(Collectors.toList());
				rankTree.setConnectedRankTrees(connectedTrees);
				
				rdq.getRankTreeRepository().update(rankTree);
			} catch (Exception e) {
				LOGGER.log(
					Level.SEVERE,
					"Failed to update rank tree connections for: " + treeId,
					e
				);
				throw new RuntimeException(
					"Failed to update rank tree connections",
					e
				);
			}
		}
		
	}
	
	/**
	 * Handles all validation operations.
	 */
	private static class ValidationEngine {
		
		void validateConfigurations(RankSystemData data) {
			
			List<String> errors = new ArrayList<>();
			
			for (Map.Entry<String, RankTreeSection> entry : data.rankTreeSections.entrySet()) {
				String          treeId = entry.getKey();
				RankTreeSection config = entry.getValue();
				
				validateTreeReferences(
					treeId,
					config,
					data.rankTreeSections.keySet(),
					errors
				);
			}
			
			for (Map.Entry<String, Map<String, RankSection>> treeEntry : data.rankSections.entrySet()) {
				String                   treeId = treeEntry.getKey();
				Map<String, RankSection> ranks  = treeEntry.getValue();
				
				validateRankReferences(
					treeId,
					ranks,
					errors
				);
			}
			
			if (! errors.isEmpty()) {
				errors.forEach(error -> LOGGER.log(
					Level.SEVERE,
					"Configuration validation error: " + error
				));
				throw new IllegalStateException("Configuration validation failed. See logs for details.");
			}
			
			LOGGER.info("Configuration validation completed successfully");
		}
		
		void validateSystem(RankSystemData data) {
			
			List<String> errors = new ArrayList<>();
			
			for (String treeId : data.rankTreeSections.keySet()) {
				if (hasCycleInPrerequisites(
					treeId,
					data.rankTreeSections,
					new HashSet<>(),
					new HashSet<>()
				)) {
					errors.add("Cycle detected in prerequisites starting at tree: " + treeId);
				}
			}
			
			if (! errors.isEmpty()) {
				errors.forEach(error -> LOGGER.log(
					Level.SEVERE,
					"System validation error: " + error
				));
				throw new IllegalStateException("System validation failed. See logs for details.");
			}
			
			LOGGER.info("System validation completed successfully");
		}
		
		private void validateTreeReferences(
			String treeId,
			RankTreeSection config,
			Set<String> validTreeIds,
			List<String> errors
		) {
			
			for (String preReqId : config.getPrerequisiteRankTrees()) {
				if (! validTreeIds.contains(preReqId)) {
					errors.add("Tree " + treeId + " has missing prerequisite: " + preReqId);
				}
			}
			
			for (String unlockedId : config.getUnlockedRankTrees()) {
				if (! validTreeIds.contains(unlockedId)) {
					errors.add("Tree " + treeId + " has missing unlocked tree: " + unlockedId);
				}
			}
			
			for (String connectedId : config.getConnectedRankTrees()) {
				if (! validTreeIds.contains(connectedId)) {
					errors.add("Tree " + treeId + " has missing connected tree: " + connectedId);
				}
			}
		}
		
		private void validateRankReferences(
			String treeId,
			Map<String, RankSection> ranks,
			List<String> errors
		) {
			
			Set<String> validRankIds = ranks.keySet();
			
			for (Map.Entry<String, RankSection> entry : ranks.entrySet()) {
				String      rankId = entry.getKey();
				RankSection config = entry.getValue();
				
				for (String prevId : config.getPreviousRanks()) {
					if (! validRankIds.contains(prevId)) {
						errors.add("Rank " + rankId + " in tree " + treeId + " has missing previous rank: " + prevId);
					}
				}
				
				for (String nextId : config.getNextRanks()) {
					if (! validRankIds.contains(nextId)) {
						errors.add("Rank " + rankId + " in tree " + treeId + " has missing next rank: " + nextId);
					}
				}
			}
		}
		
		private boolean hasCycleInPrerequisites(
			String treeId,
			Map<String, RankTreeSection> treeSections,
			Set<String> visited,
			Set<String> stack
		) {
			
			if (stack.contains(treeId))
				return true;
			if (visited.contains(treeId))
				return false;
			
			visited.add(treeId);
			stack.add(treeId);
			
			RankTreeSection section = treeSections.get(treeId);
			if (section != null) {
				for (String preReqId : section.getPrerequisiteRankTrees()) {
					if (hasCycleInPrerequisites(
						preReqId,
						treeSections,
						visited,
						stack
					)) {
						return true;
					}
				}
			}
			
			stack.remove(treeId);
			return false;
		}
		
	}
	
	public RankSystemSection getRankSystemSection() {
		
		return this.rankSystemSection;
	}
	
}