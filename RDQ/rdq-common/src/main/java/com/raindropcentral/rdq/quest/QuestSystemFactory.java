package com.raindropcentral.rdq.quest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.quest.PerformanceSection;
import com.raindropcentral.rdq.config.quest.QuestCategoriesSection;
import com.raindropcentral.rdq.config.quest.QuestCategorySection;
import com.raindropcentral.rdq.config.quest.QuestSection;
import com.raindropcentral.rdq.config.quest.QuestSystemSection;
import com.raindropcentral.rdq.config.quest.QuestTaskSection;
import com.raindropcentral.rdq.config.quest.RewardsSection;
import com.raindropcentral.rdq.config.quest.TaskHandlersSection;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.entity.quest.QuestTaskReward;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRewardRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestTaskRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestTaskRewardRepository;
import com.raindropcentral.rdq.model.quest.QuestDifficulty;
import com.raindropcentral.rdq.model.quest.TaskDifficulty;
import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory class responsible for loading and initializing the quest system.
 * <p>
 * This class loads quest categories and quest definitions from YAML files,
 * parses them using ConfigKeeper, and persists them to the database.
 * Quest definitions are organized in category subdirectories under
 * {@code quests/definitions/<category>/}.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final Gson GSON = new Gson();

    private static final String FILE_PATH = "quests";
    private static final String CATEGORIES_FILE = "categories.yml";
    private static final String SYSTEM_FILE = "quest-system.yml";
    private static final String DEFINITIONS_DIR = "definitions";

    /** JAR path prefix used when scanning bundled quest definitions. */
    private static final String JAR_DEFINITIONS_PREFIX = FILE_PATH + "/" + DEFINITIONS_DIR + "/";

    private final RDQ plugin;
    private final QuestCategoryRepository categoryRepository;
    private final QuestRepository questRepository;
    private final QuestTaskRepository questTaskRepository;
    private final QuestRewardRepository questRewardRepository;
    private final QuestTaskRewardRepository questTaskRewardRepository;
    private final com.raindropcentral.rdq.quest.reward.QuestRewardFactory rewardFactory;
    private QuestSystemSection systemConfig;
    private TaskHandlersSection taskHandlersConfig;
    private RewardsSection rewardsConfig;
    private PerformanceSection performanceConfig;

    /**
     * Constructs a new QuestSystemFactory.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestSystemFactory(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        this.categoryRepository = plugin.getQuestCategoryRepository();
        this.questRepository = plugin.getQuestRepository();
        this.questTaskRepository = plugin.getQuestTaskRepository();
        this.questRewardRepository = plugin.getQuestRewardRepository();
        this.questTaskRewardRepository = plugin.getQuestTaskRewardRepository();
        this.rewardFactory = new com.raindropcentral.rdq.quest.reward.QuestRewardFactory(plugin);
    }

    /**
     * Initializes the quest system by loading all configurations.
     *
     * @return a CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Void> initialize() {
        LOGGER.info("Initializing Quest System...");

        return CompletableFuture.runAsync(() -> {
            try {
                loadSystemConfig();

                if (!systemConfig.getEnabled()) {
                    LOGGER.info("Quest System is disabled in configuration");
                    return;
                }

                loadCategories();
                loadQuestDefinitions();

                LOGGER.info("Quest System initialized successfully!");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize Quest System", e);
            }
        });
    }

    /**
     * Loads the main quest system configuration.
     */
    private void loadSystemConfig() {
        try {
            final ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), FILE_PATH);
            final ConfigKeeper<QuestSystemSection> cfgKeeper = new ConfigKeeper<>(
                cfgManager,
                SYSTEM_FILE,
                QuestSystemSection.class
            );
            systemConfig = cfgKeeper.rootSection;
            LOGGER.info("Loaded quest system configuration");
            
            // Load task handlers configuration
            try {
                final ConfigKeeper<TaskHandlersSection> taskHandlersKeeper = new ConfigKeeper<>(
                    cfgManager,
                    SYSTEM_FILE,
                    TaskHandlersSection.class
                );
                taskHandlersConfig = taskHandlersKeeper.rootSection;
                LOGGER.info("Loaded task handlers configuration");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load task handlers configuration, using defaults", e);
                taskHandlersConfig = new TaskHandlersSection(new EvaluationEnvironmentBuilder());
            }
            
            // Load rewards configuration
            try {
                final ConfigKeeper<RewardsSection> rewardsKeeper = new ConfigKeeper<>(
                    cfgManager,
                    SYSTEM_FILE,
                    RewardsSection.class
                );
                rewardsConfig = rewardsKeeper.rootSection;
                LOGGER.info("Loaded rewards configuration");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load rewards configuration, using defaults", e);
                rewardsConfig = new RewardsSection(new EvaluationEnvironmentBuilder());
            }
            
            // Load performance configuration
            try {
                final ConfigKeeper<PerformanceSection> performanceKeeper = new ConfigKeeper<>(
                    cfgManager,
                    SYSTEM_FILE,
                    PerformanceSection.class
                );
                performanceConfig = performanceKeeper.rootSection;
                LOGGER.info("Loaded performance configuration");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load performance configuration, using defaults", e);
                performanceConfig = new PerformanceSection(new EvaluationEnvironmentBuilder());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load quest system configuration, using defaults", e);
            systemConfig = new QuestSystemSection(new EvaluationEnvironmentBuilder());
            taskHandlersConfig = new TaskHandlersSection(new EvaluationEnvironmentBuilder());
            rewardsConfig = new RewardsSection(new EvaluationEnvironmentBuilder());
            performanceConfig = new PerformanceSection(new EvaluationEnvironmentBuilder());
        }
    }

    /**
     * Scans the plugin JAR for bundled quest definition YAMLs and copies any that
     * are missing from the data folder into their respective category subdirectory.
     * <p>
     * Entry names under {@code quests/definitions/<category>/<file>.yml} are
     * enumerated directly from the JAR so no hardcoded file list is needed.
     *
     * @param definitionsDir the on-disk {@code quests/definitions/} directory
     */
    private void copyDefaultQuestFiles(@NotNull final File definitionsDir) {
        final URI jarUri;
        try {
            jarUri = plugin.getPlugin().getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI();
        } catch (final URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Could not resolve plugin JAR URI — skipping default quest copy", e);
            return;
        }

        final File jarFile = new File(jarUri);
        if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
            // Running in a dev environment (exploded classpath) — nothing to copy
            return;
        }

        // Build a map of category → list of file names from the JAR
        final Map<String, List<String>> jarEntries = new HashMap<>();
        try (final JarFile jar = new JarFile(jarFile)) {
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final String entryName = entries.nextElement().getName();
                if (!entryName.startsWith(JAR_DEFINITIONS_PREFIX) || !entryName.endsWith(".yml")) {
                    continue;
                }
                // entryName looks like: quests/definitions/<category>/<file>.yml
                final String relative = entryName.substring(JAR_DEFINITIONS_PREFIX.length()); // <category>/<file>.yml
                final int slash = relative.indexOf('/');
                if (slash < 1) {
                    continue; // flat file at definitions root — skip
                }
                final String category = relative.substring(0, slash);
                final String fileName = relative.substring(slash + 1);
                jarEntries.computeIfAbsent(category, k -> new ArrayList<>()).add(fileName);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to scan JAR for default quest files", e);
            return;
        }

        // Copy missing files to disk
        for (final Map.Entry<String, List<String>> entry : jarEntries.entrySet()) {
            final String category = entry.getKey();
            final File categoryDir = new File(definitionsDir, category);
            if (!categoryDir.exists()) {
                categoryDir.mkdirs();
            }
            for (final String fileName : entry.getValue()) {
                final File target = new File(categoryDir, fileName);
                if (!target.exists()) {
                    final String resourcePath = JAR_DEFINITIONS_PREFIX + category + "/" + fileName;
                    try {
                        plugin.getPlugin().saveResource(resourcePath, false);
                        LOGGER.info("Copied default quest definition: " + category + "/" + fileName);
                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to copy default quest: " + category + "/" + fileName, e);
                    }
                }
            }
        }

        LOGGER.info("Default quest file check complete (" + jarEntries.values().stream()
            .mapToInt(List::size).sum() + " bundled definition(s) across "
            + jarEntries.size() + " categories)");
    }

    /**
     * Loads quest categories from the categories.yml file.
     */
    private void loadCategories() {
        try {
            final ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), FILE_PATH);
            final ConfigKeeper<QuestCategoriesSection> cfgKeeper = new ConfigKeeper<>(
                cfgManager,
                CATEGORIES_FILE,
                QuestCategoriesSection.class
            );

            final Map<String, QuestCategorySection> categoryConfigs = cfgKeeper.rootSection.getCategories();

            if (categoryConfigs.isEmpty()) {
                LOGGER.warning("No categories found in categories.yml");
                return;
            }

            LOGGER.info("Loading quest categories from: " + CATEGORIES_FILE);

            final List<QuestCategory> categories = new ArrayList<>();

            for (Map.Entry<String, QuestCategorySection> entry : categoryConfigs.entrySet()) {
                final String categoryId = entry.getKey();
                final QuestCategorySection config = entry.getValue();

                config.setCategoryId(categoryId);

                try {
                    final var existingOpt = categoryRepository.findByIdentifier(config.getIdentifier()).join();
                    QuestCategory category = existingOpt.orElseGet(() -> new QuestCategory(config.getIdentifier(), config.getIcon()));

                    category.setDisplayOrder(config.getDisplayOrder());
                    category.setEnabled(config.getEnabled());

                    if (category.getId() == null) {
                        category = categoryRepository.create(category);
                    } else {
                        category = categoryRepository.update(category);
                    }

                    categories.add(category);

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to create/update category: " + categoryId, e);
                }
            }

            LOGGER.info("Loaded " + categories.size() + " quest categories");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load quest categories", e);
        }
    }

    /**
     * Loads quest definitions from category subdirectories under
     * {@code quests/definitions/<category>/}.
     * <p>
     * Default quest files bundled in the JAR are copied to their respective
     * category subfolder if they do not yet exist on disk.
     */
    private void loadQuestDefinitions() {
        try {
            final File definitionsDir = new File(plugin.getPlugin().getDataFolder(), FILE_PATH + "/" + DEFINITIONS_DIR);
            if (!definitionsDir.exists()) {
                definitionsDir.mkdirs();
                LOGGER.info("Created quest definitions directory: " + definitionsDir.getAbsolutePath());
            }

            // Copy default quest files from JAR into their category subfolders
            copyDefaultQuestFiles(definitionsDir);

            // Warn about any leftover flat YML files in the root definitions dir
            final File[] flatFiles = definitionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (flatFiles != null && flatFiles.length > 0) {
                LOGGER.warning(flatFiles.length + " quest YAML file(s) found directly in definitions/ — "
                    + "please move them into a category subfolder (e.g. definitions/combat/my_quest.yml).");
            }

            // Scan all category subdirectories
            final File[] categoryDirs = definitionsDir.listFiles(File::isDirectory);
            if (categoryDirs == null || categoryDirs.length == 0) {
                LOGGER.warning("No category subdirectories found in: " + definitionsDir.getPath());
                return;
            }

            LOGGER.info("Scanning quest definitions in: " + definitionsDir.getPath());

            int totalLoaded = 0;

            // Process quests sequentially to avoid OptimisticLockException
            for (final File categoryDir : categoryDirs) {
                final File[] questFiles = categoryDir.listFiles((dir, name) -> name.endsWith(".yml"));
                if (questFiles == null || questFiles.length == 0) {
                    continue;
                }

                final ConfigManager cfgManager = new ConfigManager(
                    plugin.getPlugin(),
                    FILE_PATH + "/" + DEFINITIONS_DIR + "/" + categoryDir.getName()
                );

                for (final File questFile : questFiles) {
                    try {
                        final String questId = questFile.getName().replace(".yml", "");

                        final ConfigKeeper<QuestSection> cfgKeeper = new ConfigKeeper<>(
                            cfgManager,
                            questFile.getName(),
                            QuestSection.class
                        );

                        final QuestSection questConfig = cfgKeeper.rootSection;
                        questConfig.setQuestId(questId);

                        // Create quest and tasks in separate transactions
                        final Quest quest = createQuestFromConfig(questConfig);
                        if (quest != null) {
                            createTasksFromConfig(quest, questConfig);
                            totalLoaded++;
                            LOGGER.fine("Loaded quest: " + categoryDir.getName() + "/" + questId);
                        }

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to load quest from: "
                            + categoryDir.getName() + "/" + questFile.getName(), e);
                    }
                }
            }

            LOGGER.info("Loaded " + totalLoaded + " quest definitions");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load quest definitions", e);
        }
    }

    /**
     * Creates or updates a {@link Quest} entity from a parsed {@link QuestSection}.
     * Includes retry logic for OptimisticLockException.
     *
     * @param config the quest configuration
     * @return the persisted Quest entity, or {@code null} if creation failed
     */
    @Nullable
    private Quest createQuestFromConfig(@NotNull final QuestSection config) {
        final int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                return createQuestFromConfigInternal(config);
            } catch (jakarta.persistence.OptimisticLockException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    LOGGER.log(Level.WARNING, "Failed to create quest from config after " + maxRetries 
                        + " attempts: " + config.getIdentifier(), e);
                    return null;
                }
                
                // Exponential backoff: 50ms, 100ms, 200ms
                long backoffMs = 50L * (1L << (attempt - 1));
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.warning("Interrupted while retrying quest creation: " + config.getIdentifier());
                    return null;
                }
                
                LOGGER.fine("Retrying quest creation (attempt " + (attempt + 1) + "/" + maxRetries 
                    + ") for: " + config.getIdentifier());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to create quest from config: " + config.getIdentifier(), e);
                return null;
            }
        }
        
        return null;
    }

    /**
     * Internal method that performs the actual quest creation/update.
     * <p>
     * This method creates or updates ONLY the Quest entity itself, without
     * touching any child entities (tasks, rewards, requirements). Child entities
     * are handled separately to avoid OptimisticLockException from cascading.
     * </p>
     *
     * @param config the quest configuration
     * @return the persisted Quest entity
     */
    @Nullable
    private Quest createQuestFromConfigInternal(@NotNull final QuestSection config) {
        final var categoryOpt = categoryRepository.findByIdentifier(config.getCategory()).join();
        if (categoryOpt.isEmpty()) {
            LOGGER.warning("Category not found for quest: " + config.getIdentifier()
                + " (category: " + config.getCategory() + ")");
            return null;
        }
        final QuestCategory category = categoryOpt.get();

        final var existingOpt = questRepository.findByIdentifier(config.getIdentifier()).join();
        Quest quest;

        if (existingOpt.isPresent()) {
            quest = existingOpt.get();
            // Clear collections to prevent cascade operations
            quest.getTasks().clear();
            quest.getRewards().clear();
            quest.getRequirements().clear();
        } else {
            final QuestDifficulty difficulty = parseQuestDifficulty(config.getDifficulty());
            quest = new Quest(
                config.getIdentifier(),
                category,
                config.getIcon(),
                difficulty
            );
        }

        quest.setRepeatable(config.getRepeatable());
        quest.setMaxCompletions(config.getMaxCompletions());
        quest.setCooldownSeconds(config.getCooldownSeconds());
        quest.setTimeLimitSeconds(config.getTimeLimitSeconds());
        quest.setEnabled(config.getEnabled());

        // Persist rewards and requirements as JSON for display and validation
        if (!config.getRewards().isEmpty()) {
            quest.setRewardData(GSON.toJson(config.getRewards()));
        }
        if (!config.getRequirements().isEmpty()) {
            quest.setRequirementData(GSON.toJson(config.getRequirements()));
        }

        if (quest.getId() == null) {
            quest = questRepository.create(quest);
        } else {
            quest = questRepository.update(quest);
        }

        return quest;
    }

    /**
     * Creates or updates {@link QuestTask} entities for all tasks defined in a
     * {@link QuestSection}.
     *
     * @param quest  the parent quest entity (must already be persisted)
     * @param config the quest configuration containing task definitions
     */
    private void createTasksFromConfig(@NotNull final Quest quest, @NotNull final QuestSection config) {
        final Map<String, QuestTaskSection> tasks = config.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, QuestTaskSection> entry : tasks.entrySet()) {
            final String taskKey = entry.getKey();
            final QuestTaskSection taskSection = entry.getValue();

            // Propagate IDs so afterParsing can generate i18n keys
            taskSection.setQuestId(config.getQuestId());
            taskSection.setTaskId(taskKey);

            // Retry logic for OptimisticLockException
            final int maxRetries = 3;
            int attempt = 0;
            boolean success = false;
            
            while (attempt < maxRetries && !success) {
                try {
                    createTaskFromConfigInternal(quest, taskSection);
                    success = true;
                } catch (jakarta.persistence.OptimisticLockException e) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        LOGGER.log(Level.WARNING, "Failed to persist task '" + taskKey 
                            + "' for quest '" + config.getIdentifier() + "' after " + maxRetries + " attempts", e);
                        break;
                    }
                    
                    // Exponential backoff: 50ms, 100ms, 200ms
                    long backoffMs = 50L * (1L << (attempt - 1));
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.warning("Interrupted while retrying task creation: " + taskKey);
                        break;
                    }
                    
                    LOGGER.fine("Retrying task creation (attempt " + (attempt + 1) + "/" + maxRetries 
                        + ") for: " + taskKey);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                        "Failed to persist task '" + taskKey + "' for quest '" + config.getIdentifier() + "'", e);
                    break;
                }
            }
        }
    }

    /**
     * Internal method that creates or updates a single quest task.
     * <p>
     * This method loads the quest fresh from the database to ensure we have
     * the latest version and avoid OptimisticLockException.
     * </p>
     *
     * @param quest the parent quest entity (used for ID reference only)
     * @param taskSection the task configuration
     */
    private void createTaskFromConfigInternal(@NotNull final Quest quest, @NotNull final QuestTaskSection taskSection) {
        // Get the icon from the task section (already configured with i18n keys)
        final IconSection taskIcon = taskSection.getIcon();

        // Check whether this task already exists to avoid duplicates
        final var existingOpt = questTaskRepository
            .findByQuestAndIdentifier(quest.getId(), taskSection.getIdentifier())
            .join();

        final QuestTask task;
        if (existingOpt.isPresent()) {
            task = existingOpt.get();
            // Refresh mutable fields (icon, order, difficulty, sequential, data)
            task.setIcon(taskIcon);
            task.setOrderIndex(taskSection.getOrderIndex());
            // Clear collections to prevent cascade operations
            task.getRewards().clear();
            task.getRequirements().clear();
        } else {
            // Load quest fresh from database to get latest version
            final var freshQuestOpt = questRepository.findById(quest.getId());
            if (freshQuestOpt.isEmpty()) {
                LOGGER.warning("Quest not found when creating task: " + quest.getIdentifier());
                return;
            }
            task = new QuestTask(freshQuestOpt.get(), taskSection.getIdentifier(), taskIcon, taskSection.getOrderIndex());
        }

        task.setDifficulty(parseTaskDifficulty(taskSection.getDifficulty()));
        task.setSequential(taskSection.getSequential());

        if (taskSection.getRequirement() != null) {
            task.setRequirementData(GSON.toJson(taskSection.getRequirement()));
        }
        if (taskSection.getReward() != null && taskSection.getReward().getType() != null) {
            task.setRewardData(GSON.toJson(taskSection.getReward()));
        }

        if (task.getId() == null) {
            questTaskRepository.create(task);
        } else {
            questTaskRepository.update(task);
        }
    }

    /**
     * Converts a difficulty string from YAML to {@link TaskDifficulty}.
     * Maps {@code "NORMAL"} → {@code MEDIUM} for backwards compatibility
     * with YAML files that pre-date the enum rename.
     *
     * @param value the raw difficulty string from YAML
     * @return the matching TaskDifficulty, or {@code EASY} as a safe fallback
     */
    @NotNull
    private static TaskDifficulty parseTaskDifficulty(@NotNull final String value) {
        final String upper = value.toUpperCase();
        if ("NORMAL".equals(upper)) {
            return TaskDifficulty.MEDIUM;
        }
        try {
            return TaskDifficulty.valueOf(upper);
        } catch (final IllegalArgumentException e) {
            LOGGER.warning("Unknown task difficulty '" + value + "', defaulting to EASY");
            return TaskDifficulty.EASY;
        }
    }

    /**
     * Converts a difficulty string from YAML to {@link QuestDifficulty}.
     * Maps {@code "NORMAL"} → {@code MEDIUM} for backwards compatibility
     * with YAML files that pre-date the enum rename.
     *
     * @param value the raw difficulty string from YAML
     * @return the matching QuestDifficulty, or {@code MEDIUM} as a safe fallback
     */
    @NotNull
    private static QuestDifficulty parseQuestDifficulty(@NotNull final String value) {
        final String upper = value.toUpperCase();
        if ("NORMAL".equals(upper)) {
            return QuestDifficulty.MEDIUM;
        }
        try {
            return QuestDifficulty.valueOf(upper);
        } catch (final IllegalArgumentException e) {
            LOGGER.warning("Unknown quest difficulty '" + value + "', defaulting to MEDIUM");
            return QuestDifficulty.MEDIUM;
        }
    }

    /**
     * Reloads the quest system configuration.
     *
     * @return a CompletableFuture that completes when reload is done
     */
    public CompletableFuture<Void> reload() {
        LOGGER.info("Reloading Quest System...");

        return CompletableFuture.runAsync(() -> {
            try {
                loadSystemConfig();
                loadCategories();
                loadQuestDefinitions();
                LOGGER.info("Quest System reloaded successfully!");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reload Quest System", e);
            }
        });
    }
    
    /**
     * Gets the quest system configuration section.
     *
     * @return the system configuration
     */
    @NotNull
    public QuestSystemSection getSystemConfig() {
        return systemConfig;
    }
    
    /**
     * Gets the task handlers configuration section.
     *
     * @return the task handlers configuration
     */
    @Nullable
    public TaskHandlersSection getTaskHandlersConfig() {
        return taskHandlersConfig;
    }
    
    /**
     * Gets the rewards configuration section.
     *
     * @return the rewards configuration
     */
    @Nullable
    public RewardsSection getRewardsConfig() {
        return rewardsConfig;
    }
    
    /**
     * Gets the performance configuration section.
     *
     * @return the performance configuration
     */
    @Nullable
    public PerformanceSection getPerformanceConfig() {
        return performanceConfig;
    }
    
    /**
     * Converts a RewardSection to a Map for reward factory processing.
     *
     * @param section the reward section to convert
     * @return a map representation of the reward section
     */
    @NotNull
    private static Map<String, Object> rewardSectionToMap(@NotNull final com.raindropcentral.rdq.config.utility.RewardSection section) {
        final Map<String, Object> map = new HashMap<>();
        
        // Required fields
        if (section.getType() != null) {
            map.put("type", section.getType());
        }
        
        // Optional fields - only add if not null
        if (section.getIcon() != null) {
            map.put("icon", section.getIcon());
        }
        if (section.getDisplayOrder() != null) {
            map.put("displayOrder", section.getDisplayOrder());
        }
        if (section.getItem() != null) {
            map.put("item", section.getItem());
        }
        if (section.getCurrencyId() != null) {
            map.put("currencyId", section.getCurrencyId());
        }
        if (section.getAmount() != null) {
            map.put("amount", section.getAmount());
        }
        if (section.getExperienceAmount() != null) {
            map.put("experienceAmount", section.getExperienceAmount());
        }
        if (section.getExperienceType() != null) {
            map.put("experienceType", section.getExperienceType());
        }
        if (section.getCommand() != null) {
            map.put("command", section.getCommand());
        }
        if (section.getExecuteAsPlayer() != null) {
            map.put("executeAsPlayer", section.getExecuteAsPlayer());
        }
        if (section.getDelayTicks() != null) {
            map.put("delayTicks", section.getDelayTicks());
        }
        if (section.getPermissions() != null) {
            map.put("permissions", section.getPermissions());
        }
        if (section.getDurationSeconds() != null) {
            map.put("durationSeconds", section.getDurationSeconds());
        }
        if (section.getTemporary() != null) {
            map.put("temporary", section.getTemporary());
        }
        if (section.getRewards() != null) {
            map.put("rewards", section.getRewards());
        }
        if (section.getChoices() != null) {
            map.put("choices", section.getChoices());
        }
        if (section.getContinueOnError() != null) {
            map.put("continueOnError", section.getContinueOnError());
        }
        if (section.getMinimumRequired() != null) {
            map.put("minimumRequired", section.getMinimumRequired());
        }
        if (section.getMaximumRequired() != null) {
            map.put("maximumRequired", section.getMaximumRequired());
        }
        if (section.getAllowMultipleSelections() != null) {
            map.put("allowMultipleSelections", section.getAllowMultipleSelections());
        }
        if (section.getPerkIdentifier() != null) {
            map.put("perkIdentifier", section.getPerkIdentifier());
        }
        if (section.getAutoEnable() != null) {
            map.put("autoEnable", section.getAutoEnable());
        }
        
        return map;
    }
    
    /**
     * Creates QuestReward entities from the quest's reward JSON data.
     * <p>
     * This method parses the reward JSON, creates BaseReward entities using the
     * QuestRewardFactory, and links them to the quest via QuestReward entities.
     *
     * @param quest the quest to create rewards for (must be persisted with an ID)
     */
    private void createRewardsFromJson(@NotNull final Quest quest) {
        final String rewardDataJson = quest.getRewardData();
        if (rewardDataJson == null || rewardDataJson.isBlank()) {
            return;
        }
        
        try {
            // Parse JSON to Map<String, Map<String, Object>>
            final java.lang.reflect.Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
            final Map<String, Map<String, Object>> rewardsMap = GSON.fromJson(rewardDataJson, type);
            
            if (rewardsMap == null || rewardsMap.isEmpty()) {
                return;
            }
            
            // Create BaseReward and QuestReward for each entry
            for (final Map.Entry<String, Map<String, Object>> entry : rewardsMap.entrySet()) {
                final String rewardKey = entry.getKey();
                final Map<String, Object> rewardData = new HashMap<>(entry.getValue());
                
                // Remove icon from reward data if present (we create it separately)
                rewardData.remove("icon");
                
                try {
                    // Use QuestRewardFactory to create BaseReward
                    final BaseReward baseReward = rewardFactory.createFromMap(rewardKey, rewardData);
                    
                    if (baseReward != null) {
                        // Create default icon for the reward
                        final IconSection icon = createDefaultRewardIcon(
                                baseReward.getReward().getTypeId(), 
                                rewardData
                        );
                        
                        // Create QuestReward entity linking quest to reward
                        final QuestReward questReward = new QuestReward(quest, baseReward, icon);
                        questRewardRepository.create(questReward);
                        LOGGER.fine("Created quest reward: " + rewardKey + " for quest: " + quest.getIdentifier());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to create reward '" + rewardKey + 
                            "' for quest '" + quest.getIdentifier() + "'", e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse rewards for quest: " + quest.getIdentifier(), e);
        }
    }
    
    /**
     * Creates QuestTaskReward entities from the task's reward JSON data.
     * <p>
     * This method parses the reward JSON, creates a BaseReward entity using the
     * QuestRewardFactory, and links it to the task via a QuestTaskReward entity.
     *
     * @param task the task to create rewards for (must be persisted with an ID)
     */
    private void createTaskRewardsFromJson(@NotNull final QuestTask task) {
        final String rewardDataJson = task.getRewardData();
        if (rewardDataJson == null || rewardDataJson.isBlank()) {
            return;
        }
        
        try {
            // Parse JSON to Map<String, Object> (single reward for tasks)
            final java.lang.reflect.Type type = new TypeToken<Map<String, Object>>(){}.getType();
            final Map<String, Object> rewardData = new HashMap<>(GSON.fromJson(rewardDataJson, type));
            
            if (rewardData == null || rewardData.isEmpty()) {
                return;
            }
            
            // Remove icon from reward data if present (we create it separately)
            rewardData.remove("icon");
            
            // Use task identifier as reward key
            final String rewardKey = task.getTaskIdentifier() + "_reward";
            
            try {
                // Use QuestRewardFactory to create BaseReward
                final BaseReward baseReward = rewardFactory.createFromMap(rewardKey, rewardData);
                
                if (baseReward != null) {
                    // Create default icon for the reward
                    final IconSection icon = createDefaultRewardIcon(
                            baseReward.getReward().getTypeId(), 
                            rewardData
                    );
                    
                    // Create QuestTaskReward entity linking task to reward
                    final QuestTaskReward taskReward = new QuestTaskReward(task, baseReward, icon);
                    questTaskRewardRepository.create(taskReward);
                    LOGGER.fine("Created task reward for task: " + task.getTaskIdentifier());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to create reward for task '" + 
                        task.getTaskIdentifier() + "'", e);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse reward for task: " + task.getTaskIdentifier(), e);
        }
    }
    
    /**
     * Creates a default IconSection for a reward based on its type.
     * <p>
     * This method generates a simple icon with appropriate material and i18n keys
     * based on the reward type.
     *
     * @param type       the reward type (e.g., "CURRENCY", "ITEM", "EXPERIENCE")
     * @param rewardData the reward data map
     * @return a default IconSection for the reward
     */
    @NotNull
    private IconSection createDefaultRewardIcon(
            @NotNull final String type,
            @NotNull final Map<String, Object> rewardData
    ) {
        final de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder envBuilder =
                new de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder();
        
        final IconSection icon = new IconSection(envBuilder);
        
        // Set material based on reward type
        final Material material = switch (type.toUpperCase()) {
            case "CURRENCY" -> Material.GOLD_INGOT;
            case "ITEM" -> {
                if (rewardData.containsKey("material")) {
                    try {
                        yield Material.valueOf(rewardData.get("material").toString().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        yield Material.CHEST;
                    }
                }
                yield Material.CHEST;
            }
            case "EXPERIENCE" -> Material.EXPERIENCE_BOTTLE;
            case "COMMAND" -> Material.COMMAND_BLOCK;
            case "TITLE" -> Material.NAME_TAG;
            default -> Material.PAPER;
        };
        
        icon.setMaterial(material.name());
        icon.setDisplayNameKey("reward." + type.toLowerCase() + ".name");
        icon.setDescriptionKey("reward." + type.toLowerCase() + ".description");
        
        return icon;
    }
}
