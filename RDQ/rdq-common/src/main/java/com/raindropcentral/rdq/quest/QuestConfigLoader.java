package com.raindropcentral.rdq.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.quest.QuestCategorySection;
import com.raindropcentral.rdq.config.quest.QuestSystemSection;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.repository.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.QuestRepository;
import com.raindropcentral.rdq.model.quest.QuestDifficulty;
import com.raindropcentral.rdq.model.quest.TaskDifficulty;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads quest configurations from YAML files and persists them to the database.
 * <p>
 * This loader reads the quest-system.yml file using ConfigManager/ConfigKeeper
 * and creates QuestCategory entities in the database. It should be run during
 * server startup before the quest cache is initialized.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 */
public class QuestConfigLoader {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	private static final String FILE_PATH = "quests";
	private static final String FILE_NAME = "quest-system.yml";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	private final RDQ plugin;
	private final QuestCategoryRepository categoryRepository;
	private final QuestRepository questRepository;
	
	/**
	 * Constructs a new quest configuration loader.
	 *
	 * @param plugin the RDQ plugin instance
	 */
	public QuestConfigLoader(@NotNull final RDQ plugin) {
		this.plugin = plugin;
		this.categoryRepository = plugin.getQuestCategoryRepository();
		this.questRepository = plugin.getQuestRepository();
	}
	
	/**
	 * Loads quest configurations from YAML files and persists to database.
	 * <p>
	 * This method:
	 * <ol>
	 *   <li>Loads quest-system.yml using ConfigManager/ConfigKeeper</li>
	 *   <li>Parses category definitions</li>
	 *   <li>Creates or updates QuestCategory entities</li>
	 *   <li>Loads quest definitions from definitions/{category}/*.yml</li>
	 *   <li>Saves to database</li>
	 * </ol>
	 * </p>
	 *
	 * @return a future completing when loading is done
	 */
	@NotNull
	public CompletableFuture<Void> loadConfigurations() {
		LOGGER.info("Loading quest configurations from YAML files...");
		
		return CompletableFuture.runAsync(() -> {
			try {
				// Load quest-system.yml using ConfigManager/ConfigKeeper
				final QuestSystemSection systemConfig = loadSystemConfig();
				
				// Load categories
				loadCategories(systemConfig);
				
				// Load quest definitions from YAML files
				loadQuestDefinitions();
				
				LOGGER.info("Quest configurations loaded successfully");
				
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to load quest configurations", e);
				throw new RuntimeException("Quest configuration loading failed", e);
			}
		});
	}
	
	/**
	 * Loads the quest system configuration using ConfigManager/ConfigKeeper.
	 *
	 * @return the quest system configuration section
	 */
	@NotNull
	private QuestSystemSection loadSystemConfig() {
		try {
			ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), FILE_PATH);
			ConfigKeeper<QuestSystemSection> cfgKeeper = new ConfigKeeper<>(
				cfgManager, 
				FILE_NAME, 
				QuestSystemSection.class
			);
			return cfgKeeper.rootSection;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error loading quest system config, using defaults", e);
			return new QuestSystemSection(new EvaluationEnvironmentBuilder());
		}
	}
	
	/**
	 * Loads quest categories from the configuration.
	 *
	 * @param systemConfig the quest system configuration
	 */
	private void loadCategories(@NotNull final QuestSystemSection systemConfig) {
		final Map<String, QuestCategorySection> categoriesMap = systemConfig.getCategories();
		if (categoriesMap == null || categoriesMap.isEmpty()) {
			LOGGER.warning("No categories found in quest-system.yml");
			return;
		}
		
		int loadedCount = 0;
		
		for (final Map.Entry<String, QuestCategorySection> entry : categoriesMap.entrySet()) {
			final String categoryId = entry.getKey();
			final QuestCategorySection categorySection = entry.getValue();
			
			if (categorySection == null) {
				continue;
			}
			
			try {
				// Check if category already exists
				final Optional<QuestCategory> existingOpt = categoryRepository.findByIdentifier(categoryId).join();
				final QuestCategory category;
				
				if (existingOpt.isPresent()) {
					// Update existing category
					category = existingOpt.get();
					LOGGER.fine("Updating existing category: " + categoryId);
				} else {
					// Create new category
					category = new QuestCategory();
					category.setIdentifier(categoryId);
					LOGGER.info("Creating new category: " + categoryId);
				}
				
				// Set properties from config
				category.setSortOrder(categorySection.getDisplayOrder());
				category.setEnabled(categorySection.getEnabled());
				
				// Set icon properties
				if (categorySection.getIcon() != null) {
					category.setIconMaterial(categorySection.getIcon().getMaterial());
					// Store the i18n keys as display name and description
					// The actual translation will happen in the view layer
					final String displayNameKey = categorySection.getIcon().getDisplayNameKey();
					final String descriptionKey = categorySection.getIcon().getDescriptionKey();
					category.setDisplayName(displayNameKey != null ? displayNameKey : categoryId);
					category.setDescription(descriptionKey != null ? descriptionKey : "");
				}
				
				// Save to database using repository methods
				if (existingOpt.isPresent()) {
					categoryRepository.update(category);
				} else {
					categoryRepository.create(category);
				}
				loadedCount++;
				
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to load category: " + categoryId, e);
			}
		}
		
		LOGGER.info("Loaded " + loadedCount + " quest categories");
	}

	
	/**
	 * Extracts quest definition YAML files bundled in the plugin JAR to the server's
	 * data folder so that {@link #loadQuestDefinitions()} can read them at runtime.
	 * <p>
	 * Files are only written when they do not already exist on disk, preserving any
	 * server-side customisations made by administrators.
	 * </p>
	 */
	private void extractQuestDefinitions() {
		final java.net.URL jarUrl = plugin.getPlugin().getClass()
			.getProtectionDomain().getCodeSource().getLocation();
		if (jarUrl == null) {
			LOGGER.warning("Cannot locate plugin JAR – skipping quest definition extraction");
			return;
		}

		final File jarFile;
		try {
			// Use toURI() so percent-encoded characters (spaces, etc.) in the path are decoded correctly
			jarFile = new File(jarUrl.toURI());
		} catch (URISyntaxException e) {
			LOGGER.log(Level.WARNING, "Invalid JAR URL – skipping quest definition extraction", e);
			return;
		}

		if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
			LOGGER.fine("Not running from a JAR (IDE mode?) – skipping quest definition extraction");
			return;
		}

		int extracted = 0;
		try (JarFile jar = new JarFile(jarFile)) {
			final Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				final String name = entry.getName();
				// Only extract quest definition YAML files from the definitions sub-tree
				if (!name.startsWith("quests/definitions/") || !name.endsWith(".yml") || entry.isDirectory()) {
					continue;
				}
				// saveResource respects the full sub-path and will NOT overwrite existing files
				// when replace=false, so operator customisations are preserved.
				final File target = new File(plugin.getPlugin().getDataFolder(), name);
				if (!target.exists()) {
					try {
						plugin.getPlugin().saveResource(name, false);
						LOGGER.fine("Extracted quest definition: " + name);
						extracted++;
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to extract quest definition: " + name, e);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to open plugin JAR for quest definition extraction", e);
		}

		if (extracted > 0) {
			LOGGER.info("Extracted " + extracted + " default quest definition(s) to data folder");
		}
	}

	/**
	 * Loads quest definitions from YAML files in the definitions directory.
	 */
	private void loadQuestDefinitions() {
		// Ensure the bundled default quest definitions are present in the data folder
		extractQuestDefinitions();

		final File definitionsDir = new File(plugin.getPlugin().getDataFolder(), "quests/definitions");
		LOGGER.info("Looking for quest definitions in: " + definitionsDir.getAbsolutePath());

		if (!definitionsDir.exists() || !definitionsDir.isDirectory()) {
			LOGGER.warning("Quest definitions directory not found: " + definitionsDir.getPath());
			return;
		}
		
		int loadedCount = 0;
		int totalFiles = 0;
		
		// Iterate through category directories
		final File[] categoryDirs = definitionsDir.listFiles(File::isDirectory);
		if (categoryDirs == null) {
			LOGGER.warning("No category directories found in definitions folder");
			return;
		}
		
		LOGGER.info("Found " + categoryDirs.length + " category directories");
		
		for (final File categoryDir : categoryDirs) {
			final String categoryId = categoryDir.getName();
			LOGGER.info("Processing category directory: " + categoryId);
			
			// Find category entity
			final Optional<QuestCategory> categoryOpt = categoryRepository.findByIdentifier(categoryId).join();
			if (categoryOpt.isEmpty()) {
				LOGGER.warning("Category not found in database for quest definitions: " + categoryId);
				continue;
			}
			
			final QuestCategory category = categoryOpt.get();
			LOGGER.fine("Found category entity: " + category.getIdentifier());
			
			// Load quest definition files
			final File[] questFiles = categoryDir.listFiles((dir, name) -> name.endsWith(".yml"));
			if (questFiles == null || questFiles.length == 0) {
				LOGGER.warning("No quest files found in category: " + categoryId);
				continue;
			}
			
			LOGGER.info("Found " + questFiles.length + " quest files in category: " + categoryId);
			totalFiles += questFiles.length;
			
			for (final File questFile : questFiles) {
				try {
					LOGGER.fine("Loading quest file: " + questFile.getName());
					loadQuestDefinition(questFile, category);
					loadedCount++;
					LOGGER.fine("Successfully loaded quest from: " + questFile.getName());
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to load quest definition: " + questFile.getName(), e);
				}
			}
		}
		
		LOGGER.info("Quest loading complete: " + loadedCount + "/" + totalFiles + " quests loaded successfully");
	}
	
	/**
	 * Loads a single quest definition from a YAML file.
	 *
	 * @param questFile the quest definition file
	 * @param category the quest category
	 */
	private void loadQuestDefinition(@NotNull final File questFile, @NotNull final QuestCategory category) {
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(questFile);
		
		// Parse quest metadata (flat structure, no nested 'quest:' section)
		final String identifier = config.getString("identifier");
		if (identifier == null || identifier.isEmpty()) {
			LOGGER.warning("Quest definition missing identifier: " + questFile.getName());
			return;
		}
		
		LOGGER.fine("Parsing quest: " + identifier);
		
		// Check if quest already exists
		final Optional<Quest> existingOpt = questRepository.findByIdentifier(identifier).join();
		final Quest quest;
		
		if (existingOpt.isPresent()) {
			quest = existingOpt.get();
			LOGGER.fine("Updating existing quest: " + identifier);
		} else {
			quest = new Quest();
			quest.setIdentifier(identifier);
			LOGGER.info("Creating new quest: " + identifier);
		}
		
		// Set basic properties
		quest.setCategory(category);
		
		// Parse difficulty
		final String difficultyStr = config.getString("difficulty", "MEDIUM");
		try {
			quest.setDifficulty(QuestDifficulty.valueOf(difficultyStr.toUpperCase()));
		} catch (IllegalArgumentException e) {
			quest.setDifficulty(QuestDifficulty.MEDIUM);
			LOGGER.warning("Invalid difficulty for quest " + identifier + ": " + difficultyStr);
		}
		
		// Parse icon section
		final ConfigurationSection iconSection = config.getConfigurationSection("icon");
		if (iconSection != null) {
			quest.setDisplayName(iconSection.getString("display_name_key", identifier));
			quest.setDescription(iconSection.getString("description_key", ""));
		} else {
			quest.setDisplayName(identifier);
			quest.setDescription("");
		}
		
		// Parse attributes
		final ConfigurationSection attributesSection = config.getConfigurationSection("attributes");
		if (attributesSection != null) {
			quest.setRepeatable(attributesSection.getBoolean("repeatable", false));
			quest.setTimeLimitMinutes(attributesSection.getInt("time_limit", 0));
			quest.setMaxConcurrentTasks(attributesSection.getInt("max_concurrent", 1));
			quest.setHidden(attributesSection.getBoolean("hidden", false));
			quest.setAutoStart(attributesSection.getBoolean("auto_start", false));
			quest.setShowInLog(attributesSection.getBoolean("show_in_log", true));
			quest.setQuestType(attributesSection.getString("quest_type", "MAIN"));
			quest.setChainId(attributesSection.getString("chain_id"));
			quest.setChainOrder(attributesSection.getInt("chain_order", 0));
		} else {
			quest.setRepeatable(false);
			quest.setTimeLimitMinutes(0);
			quest.setMaxConcurrentTasks(1);
			quest.setHidden(false);
			quest.setAutoStart(false);
			quest.setShowInLog(true);
		}
		
		quest.setEnabled(true);
		quest.setSortOrder(0);
		
		// Parse effects
		final ConfigurationSection effectsSection = config.getConfigurationSection("effects");
		if (effectsSection != null) {
			parseEffects(effectsSection, quest);
		}
		
		// Parse metadata
		final ConfigurationSection metadataSection = config.getConfigurationSection("metadata");
		if (metadataSection != null) {
			parseMetadata(metadataSection, quest);
		}
		
		// Parse failure conditions
		final ConfigurationSection failureSection = config.getConfigurationSection("failure_conditions");
		if (failureSection != null) {
			parseFailureConditions(failureSection, quest);
		}

		// Reset tasks to a new list to avoid touching the lazy-loaded proxy on existing quests.
		// orphanRemoval = true on Quest.tasks means Hibernate will delete the old task rows
		// and cascade-insert the new ones when the quest is saved.
		final List<QuestTask> parsedTasks = new java.util.ArrayList<>();

		// Parse tasks (list structure in YAML: each task is a "- identifier: ..." item)
		final List<?> tasksList = config.getList("tasks");
		if (tasksList != null && !tasksList.isEmpty()) {
			LOGGER.fine("Parsing " + tasksList.size() + " tasks for quest: " + identifier);
			int taskOrder = 0;
			for (final Object taskObj : tasksList) {
				if (taskObj instanceof Map) {
					try {
						@SuppressWarnings("unchecked")
						final Map<String, Object> taskMap = (Map<String, Object>) taskObj;
						final QuestTask task = parseTask(taskMap, quest, taskOrder++);
						if (task != null) {
							parsedTasks.add(task);
							LOGGER.fine("Added task: " + task.getIdentifier() + " (type: " + task.getTaskType() + ")");
						}
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to parse task in quest " + identifier, e);
					}
				}
			}
		} else {
			LOGGER.warning("Quest " + identifier + " has no tasks defined");
		}

		// Replace the tasks collection without touching the lazy proxy
		quest.setTasks(parsedTasks);
		
		// Parse prerequisites
		final List<String> prerequisites = config.getStringList("prerequisites");
		quest.setPrerequisiteQuestIds(prerequisites != null ? prerequisites : List.of());
		
		// Parse requirements
		// TODO: Implement requirement loading - requires BaseRequirement entity integration
		// loadQuestRequirements(config, quest);
		
		// Parse rewards
		// TODO: Implement reward loading - requires BaseReward entity integration
		// loadQuestRewards(config, quest);
		
		// Save to database
		try {
			if (existingOpt.isPresent()) {
				questRepository.update(quest);
				LOGGER.fine("Updated quest in database: " + identifier);
			} else {
				questRepository.create(quest);
				LOGGER.fine("Created quest in database: " + identifier);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to save quest to database: " + identifier, e);
			throw e;
		}
	}

	/**
	 * Converts a ConfigurationSection to a Map<String, Object> for task parsing.
	 * <p>
	 * This is necessary because YAML tasks are stored as ConfigurationSections
	 * (maps in YAML), not as lists. This method recursively converts the section
	 * and its children to native Java objects.
	 * </p>
	 *
	 * @param section the configuration section to convert
	 * @return a map representation of the section
	 */
	@NotNull
	private Map<String, Object> convertSectionToMap(@NotNull final ConfigurationSection section) {
		final Map<String, Object> map = new java.util.HashMap<>();
		for (final String key : section.getKeys(false)) {
			final Object value = section.get(key);
			if (value instanceof ConfigurationSection) {
				map.put(key, convertSectionToMap((ConfigurationSection) value));
			} else {
				map.put(key, value);
			}
		}
		return map;
	}

	/**
	 * Parses a quest task from a configuration map.
	 *
	 * @param taskMap the task configuration map
	 * @param quest the parent quest
	 * @param sortOrder the task sort order
	 * @return the parsed quest task, or null if parsing failed
	 */
	@Nullable
	private QuestTask parseTask(
		@NotNull final Map<String, Object> taskMap,
		@NotNull final Quest quest,
		final int sortOrder
	) {
		final String identifier = (String) taskMap.get("identifier");
		if (identifier == null || identifier.isEmpty()) {
			LOGGER.warning("Task missing identifier in quest: " + quest.getIdentifier());
			return null;
		}
		
		final QuestTask task = new QuestTask();
		task.setQuest(quest);
		task.setIdentifier(identifier);
		task.setSortOrder(sortOrder);
		
		// Parse task type (KILL_MOBS, CRAFT_ITEMS, etc.)
		final String taskType = (String) taskMap.get("type");
		if (taskType != null && !taskType.isEmpty()) {
			task.setTaskType(taskType);
		} else {
			task.setTaskType("UNKNOWN");
			LOGGER.warning("Task " + identifier + " missing type field");
		}
		
		// Parse task target (entity/item/block type)
		final String target = (String) taskMap.get("target");
		if (target != null && !target.isEmpty()) {
			task.setTarget(target);
		}
		
		// Parse task amount (required count)
		final Object amountObj = taskMap.get("amount");
		if (amountObj instanceof Number) {
			task.setAmount(((Number) amountObj).intValue());
		} else if (amountObj instanceof String) {
			try {
				task.setAmount(Integer.parseInt((String) amountObj));
			} catch (NumberFormatException e) {
				LOGGER.warning("Invalid amount for task " + identifier + ": " + amountObj);
				task.setAmount(1);
			}
		} else {
			task.setAmount(1);
		}
		
		// Parse optional flag
		final Object optionalObj = taskMap.get("optional");
		if (optionalObj instanceof Boolean) {
			task.setOptional((Boolean) optionalObj);
		} else {
			task.setOptional(false);
		}
		
		// Parse difficulty
		final String difficultyStr = (String) taskMap.get("difficulty");
		if (difficultyStr != null && !difficultyStr.isEmpty()) {
			try {
				task.setDifficulty(TaskDifficulty.valueOf(difficultyStr.toUpperCase()));
			} catch (IllegalArgumentException e) {
				task.setDifficulty(TaskDifficulty.MEDIUM);
				LOGGER.warning("Invalid difficulty for task " + identifier + ": " + difficultyStr);
			}
		} else {
			task.setDifficulty(TaskDifficulty.MEDIUM);
		}
		
		// Parse icon section for display name
		@SuppressWarnings("unchecked")
		final Map<String, Object> iconMap = (Map<String, Object>) taskMap.get("icon");
		if (iconMap != null) {
			task.setDisplayName((String) iconMap.getOrDefault("display_name_key", identifier));
			task.setDescription((String) iconMap.getOrDefault("description_key", ""));
		} else {
			task.setDisplayName(identifier);
			task.setDescription("");
		}
		
		// Parse task requirements
		// TODO: Implement task requirement loading
		// loadTaskRequirements(taskMap, task);
		
		// Parse task rewards
		// TODO: Implement task reward loading
		// loadTaskRewards(taskMap, task);
		
		return task;
	}

	// REMOVED: First parseEffects method (duplicate/obsolete)

	// REMOVED: parseQuestReward method
	// TODO: Implement using BaseReward entity integration with proper ConfigManager/ConfigKeeper pattern
	// Reference: QuestSystemFactory.java for proper implementation
	
	// REMOVED: loadTaskRewards and parseTaskReward methods
	// TODO: Implement using BaseReward entity integration with proper ConfigManager/ConfigKeeper pattern
	// Reference: QuestSystemFactory.java for proper implementation

	// REMOVED: loadQuestRequirements and parseQuestRequirement methods
	// TODO: Implement using BaseRequirement entity integration with proper ConfigManager/ConfigKeeper pattern
	// Reference: QuestSystemFactory.java for proper implementation
	
	// REMOVED: loadTaskRequirements and parseTaskRequirement methods
	// TODO: Implement using BaseRequirement entity integration with proper ConfigManager/ConfigKeeper pattern
	// Reference: QuestSystemFactory.java for proper implementation

	/**
	 * Parses effects from configuration and stores as JSON.
	 *
	 * @param effectsSection the effects configuration section
	 * @param quest the quest entity
	 */
	private void parseEffects(@NotNull final ConfigurationSection effectsSection, @NotNull final Quest quest) {
		final Map<String, Object> effectsData = new java.util.HashMap<>();
		
		// Parse particle effects
		if (effectsSection.contains("start_particle")) {
			effectsData.put("start_particle", effectsSection.getString("start_particle"));
		}
		if (effectsSection.contains("complete_particle")) {
			effectsData.put("complete_particle", effectsSection.getString("complete_particle"));
		}
		
		// Parse sound effects
		if (effectsSection.contains("start_sound")) {
			effectsData.put("start_sound", effectsSection.getString("start_sound"));
		}
		if (effectsSection.contains("complete_sound")) {
			effectsData.put("complete_sound", effectsSection.getString("complete_sound"));
		}
		
		// Parse title effects
		final ConfigurationSection startTitleSection = effectsSection.getConfigurationSection("start_title");
		if (startTitleSection != null) {
			final Map<String, Object> startTitle = new java.util.HashMap<>();
			startTitle.put("title_key", startTitleSection.getString("title_key"));
			startTitle.put("subtitle_key", startTitleSection.getString("subtitle_key"));
			startTitle.put("fade_in", startTitleSection.getInt("fade_in", 10));
			startTitle.put("stay", startTitleSection.getInt("stay", 70));
			startTitle.put("fade_out", startTitleSection.getInt("fade_out", 20));
			effectsData.put("start_title", startTitle);
		}
		
		final ConfigurationSection completeTitleSection = effectsSection.getConfigurationSection("complete_title");
		if (completeTitleSection != null) {
			final Map<String, Object> completeTitle = new java.util.HashMap<>();
			completeTitle.put("title_key", completeTitleSection.getString("title_key"));
			completeTitle.put("subtitle_key", completeTitleSection.getString("subtitle_key"));
			completeTitle.put("fade_in", completeTitleSection.getInt("fade_in", 10));
			completeTitle.put("stay", completeTitleSection.getInt("stay", 70));
			completeTitle.put("fade_out", completeTitleSection.getInt("fade_out", 20));
			effectsData.put("complete_title", completeTitle);
		}
		
		// Serialize to JSON
		try {
			quest.setEffectsJson(GSON.toJson(effectsData));
			LOGGER.fine("Parsed effects for quest: " + quest.getIdentifier());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to serialize effects data for quest: " + quest.getIdentifier(), e);
			quest.setEffectsJson("{}");
		}
	}
	
	/**
	 * Parses metadata from configuration and stores as JSON.
	 *
	 * @param metadataSection the metadata configuration section
	 * @param quest the quest entity
	 */
	private void parseMetadata(@NotNull final ConfigurationSection metadataSection, @NotNull final Quest quest) {
		final Map<String, Object> metadataData = new java.util.HashMap<>();
		
		// Parse metadata fields
		if (metadataSection.contains("author")) {
			metadataData.put("author", metadataSection.getString("author"));
		}
		if (metadataSection.contains("created")) {
			metadataData.put("created", metadataSection.getString("created"));
		}
		if (metadataSection.contains("modified")) {
			metadataData.put("modified", metadataSection.getString("modified"));
		}
		if (metadataSection.contains("version")) {
			metadataData.put("version", metadataSection.getString("version"));
		}
		if (metadataSection.contains("tags")) {
			metadataData.put("tags", metadataSection.getStringList("tags"));
		}
		
		// Serialize to JSON
		try {
			quest.setMetadataJson(GSON.toJson(metadataData));
			LOGGER.fine("Parsed metadata for quest: " + quest.getIdentifier());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to serialize metadata for quest: " + quest.getIdentifier(), e);
			quest.setMetadataJson("{}");
		}
	}
	
	/**
	 * Parses failure conditions from configuration and stores as JSON.
	 *
	 * @param failureSection the failure conditions configuration section
	 * @param quest the quest entity
	 */
	private void parseFailureConditions(@NotNull final ConfigurationSection failureSection, @NotNull final Quest quest) {
		final Map<String, Object> failureData = new java.util.HashMap<>();
		
		// Parse failure condition flags
		failureData.put("fail_on_death", failureSection.getBoolean("fail_on_death", false));
		failureData.put("fail_on_logout", failureSection.getBoolean("fail_on_logout", false));
		failureData.put("fail_on_timeout", failureSection.getBoolean("fail_on_timeout", false));
		
		// Serialize to JSON
		try {
			quest.setFailureConditionsJson(GSON.toJson(failureData));
			LOGGER.fine("Parsed failure conditions for quest: " + quest.getIdentifier());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to serialize failure conditions for quest: " + quest.getIdentifier(), e);
			quest.setFailureConditionsJson("{}");
		}
	}
}
