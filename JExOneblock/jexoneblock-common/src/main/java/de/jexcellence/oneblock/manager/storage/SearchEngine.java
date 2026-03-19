package de.jexcellence.oneblock.manager.storage;

import de.jexcellence.oneblock.database.entity.storage.StorageCategory;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Search Engine for Enhanced Storage System
 * 
 * Provides comprehensive search functionality with intelligent query processing,
 * result caching, and advanced filtering capabilities.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class SearchEngine {
    
    private final ItemIndexer itemIndexer;
    private final CategoryManager categoryManager;
    private final Map<String, CachedSearchResult> searchCache = new ConcurrentHashMap<>();
    private final Map<Long, SearchPreferences> userPreferences = new ConcurrentHashMap<>();
    
    private static final int CACHE_SIZE_LIMIT = 1000;
    private static final long CACHE_EXPIRY_MS = 300_000; // 5 minutes
    
    public SearchEngine(@NotNull ItemIndexer itemIndexer, @NotNull CategoryManager categoryManager) {
        this.itemIndexer = itemIndexer;
        this.categoryManager = categoryManager;
    }
    
    /**
     * Cached search result
     */
    private static class CachedSearchResult {
        private final List<ItemIndexer.SearchResult> results;
        private final long timestamp;
        private final String query;
        private final ItemIndexer.SearchOptions options;
        
        public CachedSearchResult(@NotNull List<ItemIndexer.SearchResult> results, @NotNull String query, 
                                @NotNull ItemIndexer.SearchOptions options) {
            this.results = new ArrayList<>(results);
            this.timestamp = System.currentTimeMillis();
            this.query = query;
            this.options = options;
        }
        
        public List<ItemIndexer.SearchResult> getResults() { return results; }
        public long getTimestamp() { return timestamp; }
        public String getQuery() { return query; }
        public ItemIndexer.SearchOptions getOptions() { return options; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
    
    /**
     * User search preferences
     */
    public static class SearchPreferences {
        private boolean enableFuzzySearch = true;
        private boolean includeAliases = true;
        private boolean includeTags = true;
        private int maxResults = 25;
        private double minRelevanceScore = 0.2;
        private Set<StorageCategory> preferredCategories = new HashSet<>();
        private SortPreference sortPreference = SortPreference.RELEVANCE;
        private boolean saveSearchHistory = true;
        
        public enum SortPreference {
            RELEVANCE, QUANTITY, RARITY, ALPHABETICAL, RECENT_ACCESS
        }
        
        // Getters and setters
        public boolean isEnableFuzzySearch() { return enableFuzzySearch; }
        public void setEnableFuzzySearch(boolean enableFuzzySearch) { this.enableFuzzySearch = enableFuzzySearch; }
        
        public boolean isIncludeAliases() { return includeAliases; }
        public void setIncludeAliases(boolean includeAliases) { this.includeAliases = includeAliases; }
        
        public boolean isIncludeTags() { return includeTags; }
        public void setIncludeTags(boolean includeTags) { this.includeTags = includeTags; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = Math.max(1, Math.min(100, maxResults)); }
        
        public double getMinRelevanceScore() { return minRelevanceScore; }
        public void setMinRelevanceScore(double minRelevanceScore) { this.minRelevanceScore = Math.max(0.0, Math.min(1.0, minRelevanceScore)); }
        
        public Set<StorageCategory> getPreferredCategories() { return preferredCategories; }
        public void setPreferredCategories(Set<StorageCategory> preferredCategories) { this.preferredCategories = preferredCategories; }
        
        public SortPreference getSortPreference() { return sortPreference; }
        public void setSortPreference(SortPreference sortPreference) { this.sortPreference = sortPreference; }
        
        public boolean isSaveSearchHistory() { return saveSearchHistory; }
        public void setSaveSearchHistory(boolean saveSearchHistory) { this.saveSearchHistory = saveSearchHistory; }
    }
    
    /**
     * Enhanced search result with additional metadata
     */
    public static class EnhancedSearchResult {
        private final ItemIndexer.SearchResult baseResult;
        private final boolean isRecommended;
        private final String searchContext;
        private final List<String> suggestions;
        
        public EnhancedSearchResult(@NotNull ItemIndexer.SearchResult baseResult, boolean isRecommended, 
                                  @Nullable String searchContext, @NotNull List<String> suggestions) {
            this.baseResult = baseResult;
            this.isRecommended = isRecommended;
            this.searchContext = searchContext;
            this.suggestions = suggestions;
        }
        
        public ItemIndexer.SearchResult getBaseResult() { return baseResult; }
        public boolean isRecommended() { return isRecommended; }
        public String getSearchContext() { return searchContext; }
        public List<String> getSuggestions() { return suggestions; }
        
        public Material getMaterial() { return baseResult.getMaterial(); }
        public ItemIndexer.ItemMetadata getMetadata() { return baseResult.getMetadata(); }
        public double getRelevanceScore() { return baseResult.getRelevanceScore(); }
        public Set<String> getMatchedTerms() { return baseResult.getMatchedTerms(); }
    }
    
    /**
     * Performs an enhanced search with intelligent processing
     * 
     * @param islandId the island ID
     * @param userId the user ID for preferences
     * @param query the search query
     * @return enhanced search results
     */
    @NotNull
    public CompletableFuture<List<EnhancedSearchResult>> searchAsync(@NotNull Long islandId, @NotNull Long userId, @NotNull String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performSearch(islandId, userId, query);
            } catch (Exception e) {
                // Log error and return empty results
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Performs a synchronous search
     * 
     * @param islandId the island ID
     * @param userId the user ID for preferences
     * @param query the search query
     * @return enhanced search results
     */
    @NotNull
    public List<EnhancedSearchResult> search(@NotNull Long islandId, @NotNull Long userId, @NotNull String query) {
        return performSearch(islandId, userId, query);
    }
    
    /**
     * Gets search suggestions with intelligent completion
     * 
     * @param islandId the island ID
     * @param userId the user ID for preferences
     * @param partialQuery the partial query
     * @return list of search suggestions
     */
    @NotNull
    public List<String> getSmartSuggestions(@NotNull Long islandId, @NotNull Long userId, @NotNull String partialQuery) {
        List<String> suggestions = new ArrayList<>();
        
        // Get basic suggestions from indexer
        suggestions.addAll(itemIndexer.getSearchSuggestions(islandId, partialQuery, 10));
        
        // Add category-based suggestions
        for (StorageCategory category : StorageCategory.values()) {
            String categoryName = category.getDisplayName().toLowerCase();
            if (categoryName.startsWith(partialQuery.toLowerCase())) {
                suggestions.add(category.getDisplayName());
            }
        }
        
        // Add popular search terms
        suggestions.addAll(itemIndexer.getPopularSearchTerms(islandId, 5));
        
        // Remove duplicates and sort by relevance
        return suggestions.stream()
            .distinct()
            .sorted((a, b) -> {
                // Prioritize exact prefix matches
                boolean aStartsWith = a.toLowerCase().startsWith(partialQuery.toLowerCase());
                boolean bStartsWith = b.toLowerCase().startsWith(partialQuery.toLowerCase());
                
                if (aStartsWith && !bStartsWith) return -1;
                if (!aStartsWith && bStartsWith) return 1;
                
                // Then by length (shorter first)
                int lengthDiff = a.length() - b.length();
                if (lengthDiff != 0) return lengthDiff;
                
                return a.compareTo(b);
            })
            .limit(15)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets recommended items based on user behavior
     * 
     * @param islandId the island ID
     * @param userId the user ID
     * @param limit the maximum number of recommendations
     * @return list of recommended materials
     */
    @NotNull
    public List<Material> getRecommendedItems(@NotNull Long islandId, @NotNull Long userId, int limit) {
        SearchPreferences preferences = getUserPreferences(userId);
        
        // Get frequently accessed categories
        List<StorageCategory> frequentCategories = categoryManager.getMostAccessedCategories(islandId, 5);
        
        // Get items from preferred and frequent categories
        Set<Material> recommendedMaterials = new HashSet<>();
        
        for (StorageCategory category : frequentCategories) {
            if (preferences.getPreferredCategories().isEmpty() || preferences.getPreferredCategories().contains(category)) {
                List<Material> categoryMaterials = category.getMaterials();
                recommendedMaterials.addAll(categoryMaterials.stream().limit(3).collect(Collectors.toList()));
            }
        }
        
        return recommendedMaterials.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Updates user search preferences
     * 
     * @param userId the user ID
     * @param preferences the new preferences
     */
    public void updateUserPreferences(@NotNull Long userId, @NotNull SearchPreferences preferences) {
        userPreferences.put(userId, preferences);
    }
    
    /**
     * Gets user search preferences
     * 
     * @param userId the user ID
     * @return user preferences
     */
    @NotNull
    public SearchPreferences getUserPreferences(@NotNull Long userId) {
        return userPreferences.computeIfAbsent(userId, k -> new SearchPreferences());
    }
    
    /**
     * Clears search cache
     */
    public void clearCache() {
        searchCache.clear();
    }
    
    /**
     * Clears expired cache entries
     */
    public void clearExpiredCache() {
        searchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Gets search analytics for an island
     * 
     * @param islandId the island ID
     * @return search analytics data
     */
    @NotNull
    public SearchAnalytics getSearchAnalytics(@NotNull Long islandId) {
        List<String> popularTerms = itemIndexer.getPopularSearchTerms(islandId, 10);
        List<StorageCategory> accessedCategories = categoryManager.getMostAccessedCategories(islandId, 10);
        
        return new SearchAnalytics(popularTerms, accessedCategories);
    }
    
    /**
     * Performs the actual search operation
     */
    @NotNull
    private List<EnhancedSearchResult> performSearch(@NotNull Long islandId, @NotNull Long userId, @NotNull String query) {
        // Normalize query
        String normalizedQuery = query.trim().toLowerCase();
        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Check cache first
        String cacheKey = islandId + ":" + userId + ":" + normalizedQuery;
        CachedSearchResult cached = searchCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return enhanceResults(cached.getResults(), normalizedQuery);
        }
        
        // Get user preferences
        SearchPreferences preferences = getUserPreferences(userId);
        
        // Create search options from preferences
        ItemIndexer.SearchOptions options = createSearchOptions(preferences);
        
        // Perform search
        List<ItemIndexer.SearchResult> results = itemIndexer.searchItems(islandId, normalizedQuery, options);
        
        // Cache results
        if (searchCache.size() < CACHE_SIZE_LIMIT) {
            searchCache.put(cacheKey, new CachedSearchResult(results, normalizedQuery, options));
        }
        
        // Record category access for accessed results
        for (ItemIndexer.SearchResult result : results) {
            categoryManager.recordCategoryAccess(islandId, result.getMetadata().getCategory());
        }
        
        return enhanceResults(results, normalizedQuery);
    }
    
    /**
     * Creates search options from user preferences
     */
    @NotNull
    private ItemIndexer.SearchOptions createSearchOptions(@NotNull SearchPreferences preferences) {
        ItemIndexer.SearchOptions options = new ItemIndexer.SearchOptions();
        
        options.setFuzzySearch(preferences.isEnableFuzzySearch());
        options.setIncludeAliases(preferences.isIncludeAliases());
        options.setIncludeTags(preferences.isIncludeTags());
        options.setMaxResults(preferences.getMaxResults());
        options.setMinRelevanceScore(preferences.getMinRelevanceScore());
        options.setCategoryFilter(preferences.getPreferredCategories());
        
        // Set sorting based on preference
        switch (preferences.getSortPreference()) {
            case RELEVANCE -> options.setSortByRelevance(true);
            case QUANTITY -> options.setSortByQuantity(true);
            case RARITY -> options.setSortByRarity(true);
            case ALPHABETICAL -> {
                options.setSortByRelevance(false);
                options.setSortByQuantity(false);
                options.setSortByRarity(false);
            }
            case RECENT_ACCESS -> {
                // This would require additional sorting logic
                options.setSortByRelevance(true);
            }
        }
        
        return options;
    }
    
    /**
     * Enhances search results with additional metadata
     */
    @NotNull
    private List<EnhancedSearchResult> enhanceResults(@NotNull List<ItemIndexer.SearchResult> results, @NotNull String query) {
        List<EnhancedSearchResult> enhanced = new ArrayList<>();
        
        for (ItemIndexer.SearchResult result : results) {
            boolean isRecommended = result.getRelevanceScore() > 0.8;
            String context = generateSearchContext(result, query);
            List<String> suggestions = generateSuggestions(result);
            
            enhanced.add(new EnhancedSearchResult(result, isRecommended, context, suggestions));
        }
        
        return enhanced;
    }
    
    /**
     * Generates search context for a result
     */
    @Nullable
    private String generateSearchContext(@NotNull ItemIndexer.SearchResult result, @NotNull String query) {
        ItemIndexer.ItemMetadata metadata = result.getMetadata();
        
        // Generate context based on what matched
        if (result.getMatchedTerms().contains(query)) {
            if (metadata.getTags().contains(query)) {
                return "Found by tag: " + query;
            } else if (metadata.getAliases().contains(query)) {
                return "Found by alias: " + query;
            } else {
                return "Exact name match";
            }
        } else {
            return "Similar to: " + query;
        }
    }
    
    /**
     * Generates suggestions for a search result
     */
    @NotNull
    private List<String> generateSuggestions(@NotNull ItemIndexer.SearchResult result) {
        List<String> suggestions = new ArrayList<>();
        ItemIndexer.ItemMetadata metadata = result.getMetadata();
        
        // Add related tags as suggestions
        suggestions.addAll(metadata.getTags().stream().limit(3).collect(Collectors.toList()));
        
        // Add category as suggestion
        suggestions.add(metadata.getCategory().getDisplayName());
        
        return suggestions;
    }
    
    /**
     * Search analytics data
     */
    public static class SearchAnalytics {
        private final List<String> popularSearchTerms;
        private final List<StorageCategory> mostAccessedCategories;
        
        public SearchAnalytics(@NotNull List<String> popularSearchTerms, @NotNull List<StorageCategory> mostAccessedCategories) {
            this.popularSearchTerms = popularSearchTerms;
            this.mostAccessedCategories = mostAccessedCategories;
        }
        
        public List<String> getPopularSearchTerms() { return popularSearchTerms; }
        public List<StorageCategory> getMostAccessedCategories() { return mostAccessedCategories; }
    }
}
