package com.raindropcentral.rdq.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.model.quest.QuestDifficulty;
import com.raindropcentral.rdq.model.quest.TaskDifficulty;
import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
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
 *
 * @author JExcellence
 * @version 2.0.0
 */
public class QuestConfigLoader {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
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
	 * Categories are inferred from the directory structure under
	 * {@code quests/definitions/<category>/*.yml}. A {@link QuestCategory} entity is
	 * created automatically for each directory that does not yet exist in the database.
	 *
	 * @return a future completing when loading is done
	 */
	@NotNull
	public CompletableFuture<Void> loadConfigurations() {
		LOGGER.info("Loading quest configurations from YAML files...");
		return CompletableFuture.runAsync(() -> {
			try {
				// Load quest definitions from YAML files (categories are created automatically)
				loadQuestDefinitions();
				LOGGER.info("Quest configurations loaded successfully");
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to load quest configurations", e);
				throw new RuntimeException("Quest configuration loading failed", e);
			}
		});
	}

	
	/**
	 * Extracts quest definition YAML files bundled in the plugin JAR to the server's
	 * data folder so that {@link #loadQuestDefinitions()} can read them at runtime.
	 * <p>
	 * Files are only written when they do not already exist on disk, preserving any
	 * server-side customisations made by administrators.
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
				// Always overwrite with the bundled version so plugin updates (e.g. difficulty
				// value fixes) are applied automatically on the next server start.
				try {
					plugin.getPlugin().saveResource(name, true);
					LOGGER.fine("Extracted quest definition: " + name);
					extracted++;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to extract quest definition: " + name, e);
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

			// Find or auto-create the category entity
			final Optional<QuestCategory> existingCategoryOpt = categoryRepository.findByIdentifier(categoryId).join();
			final QuestCategory category;
			if (existingCategoryOpt.isPresent()) {
				category = existingCategoryOpt.get();
				LOGGER.fine("Found existing category: " + category.getIdentifier() + " (ID: " + category.getId() + ")");
			} else {
				// Build IconSection with i18n keys (null builder is fine for programmatic creation)
				final IconSection categoryIcon = new IconSection(null);
				categoryIcon.setMaterial("BOOK");
				categoryIcon.setDisplayNameKey("quest.category." + categoryId + ".name");
				categoryIcon.setDescriptionKey("quest.category." + categoryId + ".description");
				final QuestCategory newCategory = QuestCategory.create(categoryId, categoryIcon);
				newCategory.setEnabled(true);
				categoryRepository.create(newCategory);
				
				// Re-fetch the category to get the generated ID
				category = categoryRepository.findByIdentifier(categoryId).join()
					.orElseThrow(() -> new IllegalStateException("Failed to create category: " + categoryId));
				LOGGER.info("Created new category: " + categoryId + " (ID: " + category.getId() + ")");
			}
			
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

		// Parse difficulty
		final String difficultyStr = config.getString("difficulty", "MEDIUM");
		QuestDifficulty difficulty;
		try {
			difficulty = QuestDifficulty.valueOf(difficultyStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			difficulty = QuestDifficulty.MEDIUM;
			LOGGER.warning("Invalid difficulty for quest " + identifier + ": " + difficultyStr + ", defaulting to MEDIUM");
		}

		// Parse icon section
		final ConfigurationSection iconSection = config.getConfigurationSection("icon");
		final IconSection icon = new IconSection(null);
		if (iconSection != null) {
			icon.setMaterial(iconSection.getString("material", "PAPER"));
			icon.setDisplayNameKey(iconSection.getString("displayNameKey", "quest." + identifier + ".name"));
			icon.setDescriptionKey(iconSection.getString("descriptionKey", "quest." + identifier + ".description"));
		} else {
			icon.setMaterial("PAPER");
			icon.setDisplayNameKey("quest." + identifier + ".name");
			icon.setDescriptionKey("quest." + identifier + ".description");
		}

		// Check if quest already exists
		final Optional<Quest> existingOpt = questRepository.findByIdentifier(identifier).join();
		final Quest quest;

		if (existingOpt.isPresent()) {
			quest = existingOpt.get();
			quest.setIcon(icon);
			quest.setDifficulty(difficulty);
			quest.setCategory(category);
			LOGGER.fine("Updating existing quest: " + identifier + " with category: " + category.getIdentifier() + " (ID: " + category.getId() + ")");
		} else {
			quest = new Quest(identifier, category, icon, difficulty);
			LOGGER.info("Creating new quest: " + identifier + " with category: " + category.getIdentifier() + " (ID: " + category.getId() + ")");
		}

		// Parse attributes — flat fields in YAML (no nested 'attributes:' section)
		quest.setRepeatable(config.getBoolean("repeatable", false));
		quest.setTimeLimitSeconds(config.getLong("timeLimitSeconds", 0));
		quest.setCooldownSeconds(config.getLong("cooldownSeconds", 0));
		quest.setMaxCompletions(config.getInt("maxCompletions", 0));
		quest.setEnabled(config.getBoolean("enabled", true));

		// Parse tasks — YAML uses a map keyed by task identifier
		quest.setTasks(new java.util.ArrayList<>());
		final ConfigurationSection tasksSection = config.getConfigurationSection("tasks");
		if (tasksSection != null && !tasksSection.getKeys(false).isEmpty()) {
			LOGGER.fine("Parsing " + tasksSection.getKeys(false).size() + " tasks for quest: " + identifier);
			int taskOrder = 0;
			for (final String taskId : tasksSection.getKeys(false)) {
				final ConfigurationSection taskSection = tasksSection.getConfigurationSection(taskId);
				if (taskSection == null) continue;
				try {
					final QuestTask task = parseTaskSection(taskId, taskSection, quest, taskOrder++);
					if (task != null) {
						quest.addTask(task);
						LOGGER.fine("Added task: " + task.getTaskIdentifier());
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to parse task " + taskId + " in quest " + identifier, e);
				}
			}
		} else {
			LOGGER.warning("Quest " + identifier + " has no tasks defined");
		}

		// Save to database
		try {
			if (existingOpt.isPresent()) {
				questRepository.update(quest);
				org.bukkit.Bukkit.getLogger().info("[QuestLoader] Updated quest '" + identifier + "' with " + quest.getTasks().size() + " tasks");
			} else {
				questRepository.create(quest);
				org.bukkit.Bukkit.getLogger().info("[QuestLoader] Created quest '" + identifier + "' with " + quest.getTasks().size() + " tasks");
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to save quest to database: " + identifier, e);
			throw e;
		}
	}

	/**
	 * Parses a quest task from a {@link ConfigurationSection} (map-keyed YAML format).
	 *
	 * @param taskId      the task identifier (map key in YAML)
	 * @param section     the task configuration section
	 * @param quest       the parent quest
	 * @param sortOrder   the task sort order
	 * @return the parsed quest task, or null if parsing failed
	 */
	@Nullable
	private QuestTask parseTaskSection(
		@NotNull final String taskId,
		@NotNull final ConfigurationSection section,
		@NotNull final Quest quest,
		final int sortOrder
	) {
		// Build icon from the icon sub-section
		final ConfigurationSection iconSection = section.getConfigurationSection("icon");
		final IconSection taskIcon = new IconSection(null);
		if (iconSection != null) {
			taskIcon.setMaterial(iconSection.getString("material", "PAPER"));
			taskIcon.setDisplayNameKey(iconSection.getString("displayNameKey",
				"quest." + quest.getIdentifier() + ".task." + taskId + ".name"));
			taskIcon.setDescriptionKey(iconSection.getString("descriptionKey",
				"quest." + quest.getIdentifier() + ".task." + taskId + ".description"));
		} else {
			taskIcon.setMaterial("PAPER");
			taskIcon.setDisplayNameKey("quest." + quest.getIdentifier() + ".task." + taskId + ".name");
			taskIcon.setDescriptionKey("quest." + quest.getIdentifier() + ".task." + taskId + ".description");
		}

		final QuestTask task = new QuestTask(quest, taskId, taskIcon, sortOrder);

		// Parse difficulty
		final String difficultyStr = section.getString("difficulty", "MEDIUM");
		try {
			task.setDifficulty(TaskDifficulty.valueOf(difficultyStr.toUpperCase()));
		} catch (IllegalArgumentException e) {
			task.setDifficulty(TaskDifficulty.MEDIUM);
			LOGGER.warning("Invalid difficulty for task " + taskId + ": " + difficultyStr);
		}

		task.setSequential(section.getBoolean("sequential", false));

		// Build requirement data as JSON from the 'requirement' sub-section
		final ConfigurationSection reqSection = section.getConfigurationSection("requirement");
		final java.util.Map<String, Object> reqData = new java.util.LinkedHashMap<>();
		if (reqSection != null) {
			reqData.put("type", reqSection.getString("type", "UNKNOWN"));
			reqData.put("target", reqSection.getString("target", ""));
			reqData.put("amount", reqSection.getInt("amount", 1));
			reqData.put("consume", reqSection.getBoolean("consume", false));
		} else {
			reqData.put("type", "UNKNOWN");
			reqData.put("target", "");
			reqData.put("amount", 1);
			reqData.put("consume", false);
		}
		task.setRequirementData(GSON.toJson(reqData));

		org.bukkit.Bukkit.getLogger().info("[QuestLoader] task=" + taskId + " reqData=" + task.getRequirementData());

		return task;
	}

}
