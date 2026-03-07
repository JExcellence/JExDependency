package com.raindropcentral.rdq.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.quest.QuestCategoriesSection;
import com.raindropcentral.rdq.config.quest.QuestCategorySection;
import com.raindropcentral.rdq.config.quest.QuestSection;
import com.raindropcentral.rdq.config.quest.QuestSystemSection;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.repository.quest.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.quest.model.QuestDifficulty;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory class responsible for loading and initializing the quest system.
 * <p>
 * This class loads quest categories and quest definitions from YAML files,
 * parses them using ConfigKeeper, and persists them to the database.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestSystemFactory {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private static final String FILE_PATH = "quests";
    private static final String CATEGORIES_FILE = "categories.yml";
    private static final String SYSTEM_FILE = "quest.yml";
    
    private final RDQ plugin;
    private final QuestCategoryRepository categoryRepository;
    private final QuestRepository questRepository;
    private QuestSystemSection systemConfig;
    
    /**
     * Constructs a new QuestSystemFactory.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestSystemFactory(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        this.categoryRepository = plugin.getQuestCategoryRepository();
        this.questRepository = plugin.getQuestRepository();
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
                // Load system configuration
                loadSystemConfig();
                
                if (!systemConfig.getEnabled()) {
                    LOGGER.info("Quest System is disabled in configuration");
                    return;
                }
                
                // Load categories first
                loadCategories();
                
                // Then load quest definitions
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
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load quest system configuration, using defaults", e);
            systemConfig = new QuestSystemSection(new EvaluationEnvironmentBuilder());
        }
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
            
            // Get the categories map from the root config
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
                
                // Set the category ID for proper key generation
                config.setCategoryId(categoryId);
                
                try {
                    // Check if category already exists
                    final var existingOpt = categoryRepository.findByIdentifier(config.getIdentifier()).join();
                    QuestCategory category;

                    // Create new category
                    category = existingOpt.orElseGet(() -> new QuestCategory(config.getIdentifier(), config.getIcon()));
                    
                    // Update properties
                    category.setDisplayOrder(config.getDisplayOrder());
                    category.setEnabled(config.getEnabled());
                    
                    // Save or update
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
     * Loads quest definitions from the quests/definitions/ directory.
     * Uses ConfigKeeper which automatically copies files from JAR resources if they don't exist.
     */
    private void loadQuestDefinitions() {
        try {
            // Create definitions directory if it doesn't exist
            final File definitionsDir = new File(plugin.getPlugin().getDataFolder(), FILE_PATH + "/definitions");
            if (!definitionsDir.exists()) {
                definitionsDir.mkdirs();
                LOGGER.info("Created quest definitions directory: " + definitionsDir.getAbsolutePath());
            }
            
            // Manually copy quest definition files from JAR if they don't exist
            final String[] questFiles = {
                "zombie_slayer.yml",
                "zombie_slayer_2.yml",
                "zombie_slayer_3.yml",
                "master_miner.yml",
                "builders_dream.yml"
            };
            
            for (String fileName : questFiles) {
                final File questFile = new File(definitionsDir, fileName);
                if (!questFile.exists()) {
                    try {
                        plugin.getPlugin().saveResource(FILE_PATH + "/definitions/" + fileName, false);
                        LOGGER.info("Copied quest definition: " + fileName);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to copy quest definition: " + fileName, e);
                    }
                }
            }
            
            // ConfigManager will load the files
            final ConfigManager cfgManager = new ConfigManager(plugin.getPlugin(), FILE_PATH + "/definitions");
            
            // Get list of quest definition files
            final File[] loadedQuestFiles = definitionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (loadedQuestFiles == null || loadedQuestFiles.length == 0) {
                LOGGER.warning("No quest definition files found in: " + definitionsDir.getPath());
                LOGGER.info("Quest definition files should be placed in: " + definitionsDir.getAbsolutePath());
                return;
            }
            
            LOGGER.info("Loading quest definitions from: " + definitionsDir.getPath());
            
            final List<Quest> quests = new ArrayList<>();
            
            for (File questFile : loadedQuestFiles) {
                try {
                    final String questId = questFile.getName().replace(".yml", "");
                    
                    // ConfigKeeper will load the file (and copy from JAR if needed)
                    final ConfigKeeper<QuestSection> cfgKeeper = new ConfigKeeper<>(
                        cfgManager,
                        questFile.getName(),
                        QuestSection.class
                    );
                    
                    final QuestSection questConfig = cfgKeeper.rootSection;
                    questConfig.setQuestId(questId);
                    
                    // Create Quest entity
                    final Quest quest = createQuestFromConfig(questConfig);
                    if (quest != null) {
                        quests.add(quest);
                        LOGGER.fine("Loaded quest: " + questId);
                    }
                    
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load quest from file: " + questFile.getName(), e);
                }
            }
            
            LOGGER.info("Loaded " + quests.size() + " quest definitions");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load quest definitions", e);
        }
    }
    
    /**
     * Creates a Quest entity from a QuestSection configuration.
     *
     * @param config the quest configuration
     * @return the created Quest entity, or null if creation failed
     */
    private Quest createQuestFromConfig(@NotNull final QuestSection config) {
        try {
            // Find the category
            final var categoryOpt = categoryRepository.findByIdentifier(config.getCategory()).join();
            if (categoryOpt.isEmpty()) {
                LOGGER.warning("Category not found for quest: " + config.getIdentifier() + " (category: " + config.getCategory() + ")");
                return null;
            }
            final QuestCategory category = categoryOpt.get();
            
            // Check if quest already exists
            final var existingOpt = questRepository.findByIdentifier(config.getIdentifier()).join();
            Quest quest;
            
            if (existingOpt.isPresent()) {
                quest = existingOpt.get();
            } else {
                // Create new quest using public constructor
                quest = new Quest(
                    config.getIdentifier(),
                    category,
                    config.getIcon(),
                    QuestDifficulty.valueOf(config.getDifficulty().toUpperCase())
                );
            }
            
            // Update properties
            quest.setRepeatable(config.getRepeatable());
            quest.setMaxCompletions(config.getMaxCompletions());
            quest.setCooldownSeconds(config.getCooldownSeconds());
            quest.setTimeLimitSeconds(config.getTimeLimitSeconds());
            quest.setEnabled(config.getEnabled());
            
            // Save or update
            if (quest.getId() == null) {
                quest = questRepository.create(quest);
            } else {
                quest = questRepository.update(quest);
            }
            
            // Note: Task creation and requirements/rewards processing would be implemented here
            // using RPlatform's requirement and reward factories
            
            return quest;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create quest from config: " + config.getIdentifier(), e);
            return null;
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
                // Reload everything
                loadSystemConfig();
                loadCategories();
                loadQuestDefinitions();
                
                LOGGER.info("Quest System reloaded successfully!");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reload Quest System", e);
            }
        });
    }

}
