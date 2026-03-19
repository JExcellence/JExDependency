package com.raindropcentral.rdq.cache.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.repository.QuestCategoryRepository;
import com.raindropcentral.rdq.database.repository.QuestRepository;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Cache manager for quest definitions (categories, quests, tasks).
 * <p>
 * This cache loads all quest definitions from the database on startup and stores
 * them in memory for instant access. Quest definitions are read-only during gameplay
 * and only change when configurations are reloaded.
 * </p>
 * <h3>Design Philosophy</h3>
 * <ul>
 *   <li>Load all quest definitions on server startup</li>
 *   <li>Provide instant access to quest data without database queries</li>
 *   <li>Reload when quest configurations change</li>
 *   <li>Thread-safe for concurrent access</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCacheManager {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final RDQ plugin;
	private final QuestCategoryRepository categoryRepository;
	private final QuestRepository questRepository;
	
	/**
	 * Cache of quest categories: identifier -> QuestCategory
	 */
	private final Map<String, QuestCategory> categories;
	
	/**
	 * Cache of quests: identifier -> Quest
	 */
	private final Map<String, Quest> quests;
	
	/**
	 * Index of quests by category: categoryIdentifier -> List of Quest
	 */
	private final Map<String, List<Quest>> questsByCategory;
	
	/**
	 * Whether the cache has been initialized
	 */
	private volatile boolean initialized;
	
	/**
	 * Constructs a new quest cache manager.
	 *
	 * @param plugin the RDQ plugin instance
	 */
	public QuestCacheManager(@NotNull final RDQ plugin) {
		this.plugin = plugin;
		this.categoryRepository = plugin.getQuestCategoryRepository();
		this.questRepository = plugin.getQuestRepository();
		this.categories = new ConcurrentHashMap<>();
		this.quests = new ConcurrentHashMap<>();
		this.questsByCategory = new ConcurrentHashMap<>();
		this.initialized = false;
	}
	
	/**
	 * Initializes the cache by loading all quest definitions from the database.
	 * <p>
	 * This method should be called during server startup after the database
	 * has been initialized and quest definitions have been loaded.
	 * </p>
	 *
	 * @return a future completing when initialization is done
	 */
	@NotNull
	public CompletableFuture<Void> initialize() {
		LOGGER.info("Initializing quest definition cache...");
		
		return CompletableFuture.runAsync(() -> {
			try {
				// Load all categories
				loadCategories();
				
				// Load all quests
				loadQuests();
				
				// Build indexes
				buildIndexes();
				
				initialized = true;
				
				LOGGER.info("Quest definition cache initialized successfully");
				LOGGER.info("  Categories: " + categories.size());
				LOGGER.info("  Quests: " + quests.size());
				
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to initialize quest definition cache", e);
				initialized = false;
				throw new RuntimeException("Quest cache initialization failed", e);
			}
		});
	}
	
	/**
	 * Loads all quest categories from the database.
	 */
	private void loadCategories() {
		LOGGER.info("Loading quest categories from database...");
		
		final List<QuestCategory> categoryList = categoryRepository.findAllOrdered()
				.join();
		
		for (final QuestCategory category : categoryList) {
			categories.put(category.getIdentifier(), category);
			LOGGER.fine("Loaded category: " + category.getIdentifier());
		}
		
		LOGGER.info("Loaded " + categories.size() + " quest categories");
	}
	
	/**
	 * Loads all quests from the database.
	 */
	private void loadQuests() {
		LOGGER.info("Loading quests from database...");
		
		final List<Quest> questList = questRepository.findAllEnabled().join();
		
		for (final Quest quest : questList) {
			quests.put(quest.getIdentifier(), quest);
			LOGGER.fine("Loaded quest: " + quest.getIdentifier());
		}
		
		LOGGER.info("Loaded " + quests.size() + " quests");
	}
	
	/**
	 * Builds index structures for fast lookup.
	 */
	private void buildIndexes() {
		LOGGER.info("Building quest indexes...");
		
		// Build category -> quests index
		for (final Quest quest : quests.values()) {
			final String categoryId = quest.getCategory().getIdentifier();
			questsByCategory.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(quest);
		}
		
		// Sort quests within each category by display order (if available)
		for (final List<Quest> questList : questsByCategory.values()) {
			questList.sort(Comparator.comparing(Quest::getIdentifier));
		}
		
		LOGGER.info("Built indexes for " + questsByCategory.size() + " categories");
	}
	
	/**
	 * Gets a quest category by its identifier.
	 *
	 * @param identifier the category identifier
	 * @return the category if found
	 */
	@NotNull
	public Optional<QuestCategory> getCategory(@NotNull final String identifier) {
		return Optional.ofNullable(categories.get(identifier));
	}
	
	/**
	 * Gets all quest categories ordered by display order.
	 *
	 * @return the list of all categories
	 */
	@NotNull
	public List<QuestCategory> getAllCategories() {
		return categories.values().stream()
				.sorted(Comparator.comparingInt(QuestCategory::getDisplayOrder))
				.collect(Collectors.toList());
	}
	
	/**
	 * Gets a quest by its identifier.
	 *
	 * @param identifier the quest identifier
	 * @return the quest if found
	 */
	@NotNull
	public Optional<Quest> getQuest(@NotNull final String identifier) {
		return Optional.ofNullable(quests.get(identifier));
	}
	
	/**
	 * Gets all quests in a specific category.
	 *
	 * @param categoryIdentifier the category identifier
	 * @return the list of quests in the category
	 */
	@NotNull
	public List<Quest> getQuestsByCategory(@NotNull final String categoryIdentifier) {
		final List<Quest> questList = questsByCategory.get(categoryIdentifier);
		return questList != null ? new ArrayList<>(questList) : List.of();
	}
	
	/**
	 * Invalidates all cached data.
	 * <p>
	 * This clears all caches but does not reload data. Call {@link #reload()}
	 * to invalidate and reload in one operation.
	 * </p>
	 */
	public void invalidate() {
		LOGGER.info("Invalidating quest definition cache...");
		categories.clear();
		quests.clear();
		questsByCategory.clear();
		initialized = false;
		LOGGER.info("Quest definition cache invalidated");
	}
	
	/**
	 * Reloads all quest definitions from the database.
	 * <p>
	 * This invalidates the current cache and loads fresh data from the database.
	 * </p>
	 *
	 * @return a future completing when reload is done
	 */
	@NotNull
	public CompletableFuture<Void> reload() {
		LOGGER.info("Reloading quest definition cache...");
		invalidate();
		return initialize();
	}
	
	/**
	 * Checks if the cache has been initialized.
	 *
	 * @return true if initialized
	 */
	public boolean isInitialized() {
		return initialized;
	}
	
	/**
	 * Gets the number of cached categories.
	 *
	 * @return the category count
	 */
	public int getCategoryCount() {
		return categories.size();
	}
	
	/**
	 * Gets the number of cached quests.
	 *
	 * @return the quest count
	 */
	public int getQuestCount() {
		return quests.size();
	}
}
