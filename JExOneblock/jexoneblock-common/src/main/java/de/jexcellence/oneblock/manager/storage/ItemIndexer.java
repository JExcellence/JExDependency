package de.jexcellence.oneblock.manager.storage;

import de.jexcellence.oneblock.database.entity.storage.StorageCategory;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Item Indexer for Enhanced Storage Search
 * 
 * Provides advanced indexing and search capabilities for storage items,
 * including fuzzy search, tag-based search, and intelligent suggestions.
 * 
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class ItemIndexer {
    
    private final Map<Long, ItemIndex> islandIndexes = new ConcurrentHashMap<>();
    private final Map<Material, Set<String>> materialTags = new ConcurrentHashMap<>();
    private final Map<String, Set<Material>> tagMaterials = new ConcurrentHashMap<>();
    
    /**
     * Item index for a specific island
     */
    public static class ItemIndex {
        private final Map<String, Set<Material>> nameIndex = new ConcurrentHashMap<>();
        private final Map<String, Set<Material>> tagIndex = new ConcurrentHashMap<>();
        private final Map<StorageCategory, Set<Material>> categoryIndex = new ConcurrentHashMap<>();
        private final Map<Material, ItemMetadata> materialMetadata = new ConcurrentHashMap<>();
        private final Map<String, Integer> searchHistory = new ConcurrentHashMap<>();
        
        public Map<String, Set<Material>> getNameIndex() { return nameIndex; }
        public Map<String, Set<Material>> getTagIndex() { return tagIndex; }
        public Map<StorageCategory, Set<Material>> getCategoryIndex() { return categoryIndex; }
        public Map<Material, ItemMetadata> getMaterialMetadata() { return materialMetadata; }
        public Map<String, Integer> getSearchHistory() { return searchHistory; }
    }
    
    /**
     * Metadata for indexed items
     */
    public static class ItemMetadata {
        private final Material material;
        private final StorageCategory category;
        private final Set<String> tags;
        private final Set<String> aliases;
        private final String displayName;
        private final int rarityScore;
        private long quantity;
        private long lastAccessed;
        private int accessCount;
        
        public ItemMetadata(@NotNull Material material, @NotNull StorageCategory category, long quantity) {
            this.material = material;
            this.category = category;
            this.quantity = quantity;
            this.tags = new HashSet<>();
            this.aliases = new HashSet<>();
            this.displayName = formatMaterialName(material);
            this.rarityScore = calculateRarityScore(material);
            this.lastAccessed = System.currentTimeMillis();
            this.accessCount = 0;
            
            generateTags();
            generateAliases();
        }
        
        private void generateTags() {
            String materialName = material.name().toLowerCase();
            
            // Add material type tags
            if (materialName.contains("ore")) tags.add("ore");
            if (materialName.contains("ingot")) tags.add("ingot");
            if (materialName.contains("block")) tags.add("block");
            if (materialName.contains("tool")) tags.add("tool");
            if (materialName.contains("weapon")) tags.add("weapon");
            if (materialName.contains("armor")) tags.add("armor");
            if (materialName.contains("food")) tags.add("food");
            
            // Add color tags
            String[] colors = {"red", "blue", "green", "yellow", "orange", "purple", "pink", "black", "white", "gray", "brown"};
            for (String color : colors) {
                if (materialName.contains(color)) {
                    tags.add(color);
                    tags.add("colored");
                }
            }
            
            // Add material tags
            String[] materials = {"wood", "stone", "iron", "gold", "diamond", "netherite", "leather", "chain"};
            for (String mat : materials) {
                if (materialName.contains(mat)) {
                    tags.add(mat);
                }
            }
            
            // Add category-based tags
            tags.add(category.name().toLowerCase());
        }
        
        private void generateAliases() {
            String materialName = material.name().toLowerCase();
            
            // Common aliases
            Map<String, String[]> commonAliases = Map.of(
                "cobblestone", new String[]{"cobble", "stone"},
                "diamond_sword", new String[]{"dsword", "diamond sword"},
                "iron_ingot", new String[]{"iron", "iron bar"},
                "oak_log", new String[]{"wood", "log", "oak wood"},
                "redstone", new String[]{"dust", "red dust"},
                "ender_pearl", new String[]{"pearl", "enderpearl"},
                "golden_apple", new String[]{"gapple", "gold apple"}
            );
            
            String[] aliasArray = commonAliases.get(materialName);
            if (aliasArray != null) {
                aliases.addAll(Arrays.asList(aliasArray));
            }
            
            // Generate automatic aliases
            aliases.add(materialName.replace("_", " "));
            aliases.add(materialName.replace("_", ""));
            
            // Add shortened versions
            String[] parts = materialName.split("_");
            if (parts.length > 1) {
                aliases.add(parts[parts.length - 1]); // Last part
                aliases.add(parts[0]); // First part
                
                // Acronym
                StringBuilder acronym = new StringBuilder();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        acronym.append(part.charAt(0));
                    }
                }
                if (acronym.length() > 1) {
                    aliases.add(acronym.toString());
                }
            }
        }
        
        private int calculateRarityScore(@NotNull Material material) {
            // Calculate rarity based on material properties
            String name = material.name().toLowerCase();
            
            if (name.contains("netherite")) return 100;
            if (name.contains("dragon") || name.contains("elytra")) return 95;
            if (name.contains("diamond")) return 80;
            if (name.contains("emerald")) return 75;
            if (name.contains("gold")) return 60;
            if (name.contains("iron")) return 40;
            if (name.contains("stone") || name.contains("cobble")) return 10;
            if (name.contains("dirt") || name.contains("grass")) return 5;
            
            return 25; // Default rarity
        }
        
        public Material getMaterial() { return material; }
        public StorageCategory getCategory() { return category; }
        public Set<String> getTags() { return tags; }
        public Set<String> getAliases() { return aliases; }
        public String getDisplayName() { return displayName; }
        public int getRarityScore() { return rarityScore; }
        public long getQuantity() { return quantity; }
        public void setQuantity(long quantity) { this.quantity = quantity; }
        public long getLastAccessed() { return lastAccessed; }
        public int getAccessCount() { return accessCount; }
        
        public void recordAccess() {
            this.lastAccessed = System.currentTimeMillis();
            this.accessCount++;
        }
        
        private static String formatMaterialName(@NotNull Material material) {
            String name = material.name().toLowerCase().replace('_', ' ');
            StringBuilder formatted = new StringBuilder();
            
            boolean capitalizeNext = true;
            for (char c : name.toCharArray()) {
                if (c == ' ') {
                    formatted.append(' ');
                    capitalizeNext = true;
                } else if (capitalizeNext) {
                    formatted.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    formatted.append(c);
                }
            }
            
            return formatted.toString();
        }
    }
    
    /**
     * Search result with relevance scoring
     */
    public static class SearchResult {
        private final Material material;
        private final ItemMetadata metadata;
        private final double relevanceScore;
        private final Set<String> matchedTerms;
        
        public SearchResult(@NotNull Material material, @NotNull ItemMetadata metadata, 
                          double relevanceScore, @NotNull Set<String> matchedTerms) {
            this.material = material;
            this.metadata = metadata;
            this.relevanceScore = relevanceScore;
            this.matchedTerms = matchedTerms;
        }
        
        public Material getMaterial() { return material; }
        public ItemMetadata getMetadata() { return metadata; }
        public double getRelevanceScore() { return relevanceScore; }
        public Set<String> getMatchedTerms() { return matchedTerms; }
    }
    
    /**
     * Search options for customizing search behavior
     */
    public static class SearchOptions {
        private boolean fuzzySearch = true;
        private boolean includeAliases = true;
        private boolean includeTags = true;
        private boolean caseSensitive = false;
        private int maxResults = 50;
        private double minRelevanceScore = 0.1;
        private Set<StorageCategory> categoryFilter = new HashSet<>();
        private boolean sortByRelevance = true;
        private boolean sortByQuantity = false;
        private boolean sortByRarity = false;
        
        public boolean isFuzzySearch() { return fuzzySearch; }
        public void setFuzzySearch(boolean fuzzySearch) { this.fuzzySearch = fuzzySearch; }
        
        public boolean isIncludeAliases() { return includeAliases; }
        public void setIncludeAliases(boolean includeAliases) { this.includeAliases = includeAliases; }
        
        public boolean isIncludeTags() { return includeTags; }
        public void setIncludeTags(boolean includeTags) { this.includeTags = includeTags; }
        
        public boolean isCaseSensitive() { return caseSensitive; }
        public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
        
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
        
        public double getMinRelevanceScore() { return minRelevanceScore; }
        public void setMinRelevanceScore(double minRelevanceScore) { this.minRelevanceScore = minRelevanceScore; }
        
        public Set<StorageCategory> getCategoryFilter() { return categoryFilter; }
        public void setCategoryFilter(Set<StorageCategory> categoryFilter) { this.categoryFilter = categoryFilter; }
        
        public boolean isSortByRelevance() { return sortByRelevance; }
        public void setSortByRelevance(boolean sortByRelevance) { this.sortByRelevance = sortByRelevance; }
        
        public boolean isSortByQuantity() { return sortByQuantity; }
        public void setSortByQuantity(boolean sortByQuantity) { this.sortByQuantity = sortByQuantity; }
        
        public boolean isSortByRarity() { return sortByRarity; }
        public void setSortByRarity(boolean sortByRarity) { this.sortByRarity = sortByRarity; }
    }
    
    /**
     * Indexes items for an island
     * 
     * @param islandId the island ID
     * @param storedItems the items to index
     */
    public void indexItems(@NotNull Long islandId, @NotNull Map<Material, Long> storedItems) {
        ItemIndex index = islandIndexes.computeIfAbsent(islandId, k -> new ItemIndex());
        
        // Clear existing index
        index.getNameIndex().clear();
        index.getTagIndex().clear();
        index.getCategoryIndex().clear();
        index.getMaterialMetadata().clear();
        
        // Index each item
        for (Map.Entry<Material, Long> entry : storedItems.entrySet()) {
            Material material = entry.getKey();
            Long quantity = entry.getValue();
            
            if (quantity > 0) {
                indexItem(index, material, quantity);
            }
        }
    }    

    /**
     * Searches for items in an island's storage
     * 
     * @param islandId the island ID
     * @param query the search query
     * @param options the search options
     * @return list of search results
     */
    @NotNull
    public List<SearchResult> searchItems(@NotNull Long islandId, @NotNull String query, @NotNull SearchOptions options) {
        ItemIndex index = islandIndexes.get(islandId);
        if (index == null) {
            return new ArrayList<>();
        }
        
        // Record search in history
        String normalizedQuery = options.isCaseSensitive() ? query : query.toLowerCase();
        index.getSearchHistory().merge(normalizedQuery, 1, Integer::sum);
        
        List<SearchResult> results = new ArrayList<>();
        Set<Material> foundMaterials = new HashSet<>();
        
        // Search by exact name match
        searchByName(index, normalizedQuery, options, results, foundMaterials);
        
        // Search by aliases
        if (options.isIncludeAliases()) {
            searchByAliases(index, normalizedQuery, options, results, foundMaterials);
        }
        
        // Search by tags
        if (options.isIncludeTags()) {
            searchByTags(index, normalizedQuery, options, results, foundMaterials);
        }
        
        // Fuzzy search
        if (options.isFuzzySearch()) {
            fuzzySearch(index, normalizedQuery, options, results, foundMaterials);
        }
        
        // Filter by category
        if (!options.getCategoryFilter().isEmpty()) {
            results = results.stream()
                .filter(result -> options.getCategoryFilter().contains(result.getMetadata().getCategory()))
                .collect(Collectors.toList());
        }
        
        // Sort results
        sortResults(results, options);
        
        // Limit results
        if (results.size() > options.getMaxResults()) {
            results = results.subList(0, options.getMaxResults());
        }
        
        return results;
    }
    
    /**
     * Gets search suggestions based on query
     * 
     * @param islandId the island ID
     * @param partialQuery the partial query
     * @param limit the maximum number of suggestions
     * @return list of search suggestions
     */
    @NotNull
    public List<String> getSearchSuggestions(@NotNull Long islandId, @NotNull String partialQuery, int limit) {
        ItemIndex index = islandIndexes.get(islandId);
        if (index == null) {
            return new ArrayList<>();
        }
        
        Set<String> suggestions = new HashSet<>();
        String query = partialQuery.toLowerCase();
        
        // Suggest from material names
        for (ItemMetadata metadata : index.getMaterialMetadata().values()) {
            String displayName = metadata.getDisplayName().toLowerCase();
            if (displayName.startsWith(query)) {
                suggestions.add(metadata.getDisplayName());
            }
            
            // Suggest from aliases
            for (String alias : metadata.getAliases()) {
                if (alias.startsWith(query)) {
                    suggestions.add(alias);
                }
            }
            
            // Suggest from tags
            for (String tag : metadata.getTags()) {
                if (tag.startsWith(query)) {
                    suggestions.add(tag);
                }
            }
        }
        
        // Suggest from search history
        for (String historicalQuery : index.getSearchHistory().keySet()) {
            if (historicalQuery.startsWith(query)) {
                suggestions.add(historicalQuery);
            }
        }
        
        return suggestions.stream()
            .sorted((a, b) -> {
                // Prioritize exact matches and shorter suggestions
                int lengthDiff = a.length() - b.length();
                if (lengthDiff != 0) return lengthDiff;
                return a.compareTo(b);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets popular search terms for an island
     * 
     * @param islandId the island ID
     * @param limit the maximum number of terms
     * @return list of popular search terms
     */
    @NotNull
    public List<String> getPopularSearchTerms(@NotNull Long islandId, int limit) {
        ItemIndex index = islandIndexes.get(islandId);
        if (index == null) {
            return new ArrayList<>();
        }
        
        return index.getSearchHistory().entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Clears the index for an island
     * 
     * @param islandId the island ID
     */
    public void clearIndex(@NotNull Long islandId) {
        islandIndexes.remove(islandId);
    }
    
    /**
     * Clears all indexes
     */
    public void clearAllIndexes() {
        islandIndexes.clear();
        materialTags.clear();
        tagMaterials.clear();
    }
    
    /**
     * Indexes a single item
     */
    private void indexItem(@NotNull ItemIndex index, @NotNull Material material, @NotNull Long quantity) {
        StorageCategory category = StorageCategory.categorize(material);
        ItemMetadata metadata = new ItemMetadata(material, category, quantity);
        
        index.getMaterialMetadata().put(material, metadata);
        
        // Index by name
        String materialName = material.name().toLowerCase();
        index.getNameIndex().computeIfAbsent(materialName, k -> new HashSet<>()).add(material);
        
        // Index by display name
        String displayName = metadata.getDisplayName().toLowerCase();
        index.getNameIndex().computeIfAbsent(displayName, k -> new HashSet<>()).add(material);
        
        // Index by aliases
        for (String alias : metadata.getAliases()) {
            index.getNameIndex().computeIfAbsent(alias.toLowerCase(), k -> new HashSet<>()).add(material);
        }
        
        // Index by tags
        for (String tag : metadata.getTags()) {
            index.getTagIndex().computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(material);
        }
        
        // Index by category
        index.getCategoryIndex().computeIfAbsent(category, k -> new HashSet<>()).add(material);
    }
    
    /**
     * Searches by exact name match
     */
    private void searchByName(@NotNull ItemIndex index, @NotNull String query, @NotNull SearchOptions options,
                            @NotNull List<SearchResult> results, @NotNull Set<Material> foundMaterials) {
        Set<Material> matches = index.getNameIndex().get(query);
        if (matches != null) {
            for (Material material : matches) {
                if (!foundMaterials.contains(material)) {
                    ItemMetadata metadata = index.getMaterialMetadata().get(material);
                    if (metadata != null) {
                        double score = 1.0; // Exact match gets highest score
                        results.add(new SearchResult(material, metadata, score, Set.of(query)));
                        foundMaterials.add(material);
                    }
                }
            }
        }
    }
    
    /**
     * Searches by aliases
     */
    private void searchByAliases(@NotNull ItemIndex index, @NotNull String query, @NotNull SearchOptions options,
                               @NotNull List<SearchResult> results, @NotNull Set<Material> foundMaterials) {
        for (Map.Entry<Material, ItemMetadata> entry : index.getMaterialMetadata().entrySet()) {
            Material material = entry.getKey();
            ItemMetadata metadata = entry.getValue();
            
            if (!foundMaterials.contains(material)) {
                for (String alias : metadata.getAliases()) {
                    String aliasLower = options.isCaseSensitive() ? alias : alias.toLowerCase();
                    if (aliasLower.contains(query)) {
                        double score = calculateAliasScore(query, aliasLower);
                        if (score >= options.getMinRelevanceScore()) {
                            results.add(new SearchResult(material, metadata, score, Set.of(alias)));
                            foundMaterials.add(material);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Searches by tags
     */
    private void searchByTags(@NotNull ItemIndex index, @NotNull String query, @NotNull SearchOptions options,
                            @NotNull List<SearchResult> results, @NotNull Set<Material> foundMaterials) {
        Set<Material> matches = index.getTagIndex().get(query);
        if (matches != null) {
            for (Material material : matches) {
                if (!foundMaterials.contains(material)) {
                    ItemMetadata metadata = index.getMaterialMetadata().get(material);
                    if (metadata != null) {
                        double score = 0.8; // Tag match gets high score
                        results.add(new SearchResult(material, metadata, score, Set.of(query)));
                        foundMaterials.add(material);
                    }
                }
            }
        }
    }
    
    /**
     * Performs fuzzy search
     */
    private void fuzzySearch(@NotNull ItemIndex index, @NotNull String query, @NotNull SearchOptions options,
                           @NotNull List<SearchResult> results, @NotNull Set<Material> foundMaterials) {
        for (Map.Entry<Material, ItemMetadata> entry : index.getMaterialMetadata().entrySet()) {
            Material material = entry.getKey();
            ItemMetadata metadata = entry.getValue();
            
            if (!foundMaterials.contains(material)) {
                double score = calculateFuzzyScore(query, metadata.getDisplayName().toLowerCase());
                if (score >= options.getMinRelevanceScore()) {
                    results.add(new SearchResult(material, metadata, score, Set.of(query)));
                    foundMaterials.add(material);
                }
            }
        }
    }
    
    /**
     * Calculates alias match score
     */
    private double calculateAliasScore(@NotNull String query, @NotNull String alias) {
        if (alias.equals(query)) return 0.9;
        if (alias.startsWith(query)) return 0.8;
        if (alias.contains(query)) return 0.6;
        return 0.0;
    }
    
    /**
     * Calculates fuzzy match score using Levenshtein distance
     */
    private double calculateFuzzyScore(@NotNull String query, @NotNull String target) {
        if (query.equals(target)) return 1.0;
        if (target.contains(query)) return 0.7;
        
        int distance = levenshteinDistance(query, target);
        int maxLength = Math.max(query.length(), target.length());
        
        if (maxLength == 0) return 1.0;
        
        double similarity = 1.0 - ((double) distance / maxLength);
        return Math.max(0.0, similarity - 0.3); // Minimum threshold
    }
    
    /**
     * Calculates Levenshtein distance between two strings
     */
    private int levenshteinDistance(@NotNull String a, @NotNull String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return dp[a.length()][b.length()];
    }
    
    /**
     * Sorts search results based on options
     */
    private void sortResults(@NotNull List<SearchResult> results, @NotNull SearchOptions options) {
        results.sort((a, b) -> {
            if (options.isSortByRelevance()) {
                int relevanceCompare = Double.compare(b.getRelevanceScore(), a.getRelevanceScore());
                if (relevanceCompare != 0) return relevanceCompare;
            }
            
            if (options.isSortByQuantity()) {
                int quantityCompare = Long.compare(b.getMetadata().getQuantity(), a.getMetadata().getQuantity());
                if (quantityCompare != 0) return quantityCompare;
            }
            
            if (options.isSortByRarity()) {
                int rarityCompare = Integer.compare(b.getMetadata().getRarityScore(), a.getMetadata().getRarityScore());
                if (rarityCompare != 0) return rarityCompare;
            }
            
            // Default to alphabetical
            return a.getMetadata().getDisplayName().compareTo(b.getMetadata().getDisplayName());
        });
    }
}