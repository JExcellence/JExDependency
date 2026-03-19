package de.jexcellence.oneblock.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced configuration section for biome management system.
 * Handles biome categories, permissions, costs, and advanced biome features.
 */
@CSAlways
@SuppressWarnings("ALL")
public class BiomeManagementSection extends AConfigSection {

    // ==================== BIOME CATEGORIES ====================
    private Map<String, Object> biomeCategories;
    private Map<String, String> biomeCategoryPermissions;
    private Map<String, Integer> biomeCategoryCosts;
    private Map<String, String> biomeCategoryDescriptions;

    // ==================== BIOME PERMISSIONS ====================
    private Map<String, String> individualBiomePermissions;
    private String defaultBiomePermission;
    private Boolean enablePermissionChecks;
    private Boolean inheritCategoryPermissions;

    // ==================== BIOME COSTS ====================
    private Map<String, Integer> individualBiomeCosts;
    private Integer defaultBiomeCost;
    private String costCurrency;
    private Boolean enableBiomeCosts;
    private Map<String, Double> costMultipliers;

    // ==================== BIOME EFFECTS ====================
    private Map<String, Object> biomeEffects;
    private Boolean enableBiomeEffects;
    private Map<String, Integer> effectDurations;
    private Map<String, Double> effectStrengths;

    // ==================== ADVANCED FEATURES ====================
    private Map<String, Object> customBiomeProperties;
    private Boolean enableWeatherControl;
    private Boolean enableTimeControl;
    private Map<String, String> biomeAliases;
    private List<String> restrictedBiomes;

    // ==================== UI SETTINGS ====================
    private Map<String, Object> categoryDisplaySettings;
    private Boolean enableBiomePreview;
    private Integer previewRadius;
    private Boolean enableBiomeSearch;
    private String defaultSortOrder;

    public BiomeManagementSection(@NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
        validateConfiguration();
    }

    /**
     * Validates and processes biome configuration.
     */
    private void validateConfiguration() {
        // Ensure default values are set
        if (this.defaultBiomeCost != null && this.defaultBiomeCost < 0) {
            this.defaultBiomeCost = 0;
        }

        if (this.previewRadius != null && this.previewRadius < 1) {
            this.previewRadius = 3;
        }

        // Validate biome names in categories
        if (this.biomeCategories != null) {
            validateBiomeNames();
        }
    }

    private void validateBiomeNames() {
        Set<String> validBiomes = Arrays.stream(Biome.values())
            .map(biome -> biome.name())
            .collect(Collectors.toSet());

        this.biomeCategories.forEach((category, biomesObj) -> {
            if (biomesObj instanceof List<?> biomesList) {
                List<String> validatedBiomes = biomesList.stream()
                    .map(Object::toString)
                    .map(String::toUpperCase)
                    .map(biome -> biome.replaceAll("[ -]", "_"))
                    .filter(validBiomes::contains)
                    .collect(Collectors.toList());

                this.biomeCategories.put(category, validatedBiomes);
            }
        });
    }

    // ==================== BIOME CATEGORIES GETTERS ====================

    public @NotNull Map<String, List<Biome>> getBiomeCategories() {
        if (this.biomeCategories == null) {
            return getDefaultBiomeCategories();
        }

        Map<String, List<Biome>> processedCategories = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : this.biomeCategories.entrySet()) {
            String category = entry.getKey();
            List<Biome> biomes = new ArrayList<>();

            if (entry.getValue() instanceof List<?> biomesList) {
                for (Object biomeObj : biomesList) {
                    try {
                        String biomeName = biomeObj.toString()
                            .replaceAll("[ -]", "_")
                            .toUpperCase();
                        Biome biome = Biome.valueOf(biomeName);
                        biomes.add(biome);
                    } catch (IllegalArgumentException ignored) {
                        // Invalid biome name, skip
                    }
                }
            }

            processedCategories.put(category, biomes);
        }

        return sortBiomeCategories(processedCategories);
    }

    private @NotNull Map<String, List<Biome>> getDefaultBiomeCategories() {
        Map<String, List<Biome>> defaultCategories = new LinkedHashMap<>();

        defaultCategories.put("OVERWORLD_COMMON", List.of(
            Biome.PLAINS, Biome.FOREST, Biome.BIRCH_FOREST, Biome.FOREST,
            Biome.DARK_FOREST, Biome.TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA, Biome.OLD_GROWTH_PINE_TAIGA,
            Biome.DESERT, Biome.SAVANNA, Biome.SWAMP, Biome.WINDSWEPT_GRAVELLY_HILLS
        ));

        defaultCategories.put("OVERWORLD_RARE", List.of(
            Biome.JUNGLE, Biome.BAMBOO_JUNGLE, Biome.MUSHROOM_FIELDS,
            Biome.ICE_SPIKES, Biome.FLOWER_FOREST, Biome.SUNFLOWER_PLAINS,
            Biome.BADLANDS, Biome.PALE_GARDEN
        ));

        defaultCategories.put("OCEAN", List.of(
            Biome.OCEAN, Biome.DEEP_OCEAN, Biome.WARM_OCEAN, Biome.LUKEWARM_OCEAN,
            Biome.COLD_OCEAN, Biome.FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN
        ));

        defaultCategories.put("NETHER", List.of(
            Biome.NETHER_WASTES, Biome.CRIMSON_FOREST, Biome.WARPED_FOREST,
            Biome.SOUL_SAND_VALLEY, Biome.BASALT_DELTAS
        ));

        defaultCategories.put("END", List.of(
            Biome.THE_END, Biome.END_HIGHLANDS, Biome.END_MIDLANDS,
            Biome.END_BARRENS, Biome.SMALL_END_ISLANDS
        ));

        return defaultCategories;
    }

    private @NotNull Map<String, List<Biome>> sortBiomeCategories(@NotNull Map<String, List<Biome>> categories) {
        // Define category priority order
        List<String> categoryOrder = List.of(
            "OVERWORLD_COMMON", "OVERWORLD_RARE", "OCEAN", "NETHER", "END"
        );

        Map<String, List<Biome>> sortedCategories = new LinkedHashMap<>();

        // Add categories in priority order
        for (String category : categoryOrder) {
            if (categories.containsKey(category)) {
                sortedCategories.put(category, categories.get(category));
            }
        }

        // Add remaining categories
        categories.entrySet().stream()
            .filter(entry -> !categoryOrder.contains(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> sortedCategories.put(entry.getKey(), entry.getValue()));

        return sortedCategories;
    }

    public @NotNull Map<String, String> getBiomeCategoryPermissions() {
        return this.biomeCategoryPermissions != null ? this.biomeCategoryPermissions : getDefaultCategoryPermissions();
    }

    private @NotNull Map<String, String> getDefaultCategoryPermissions() {
        return Map.of(
            "OVERWORLD_COMMON", "jeoneblock.biome.overworld.common",
            "OVERWORLD_RARE", "jeoneblock.biome.overworld.rare",
            "OCEAN", "jeoneblock.biome.ocean",
            "NETHER", "jeoneblock.biome.nether",
            "END", "jeoneblock.biome.end"
        );
    }

    public @NotNull Map<String, Integer> getBiomeCategoryCosts() {
        return this.biomeCategoryCosts != null ? this.biomeCategoryCosts : getDefaultCategoryCosts();
    }

    private @NotNull Map<String, Integer> getDefaultCategoryCosts() {
        return Map.of(
            "OVERWORLD_COMMON", 1000,
            "OVERWORLD_RARE", 2500,
            "OCEAN", 1500,
            "NETHER", 5000,
            "END", 10000
        );
    }

    public @NotNull Map<String, String> getBiomeCategoryDescriptions() {
        return this.biomeCategoryDescriptions != null ? this.biomeCategoryDescriptions : getDefaultCategoryDescriptions();
    }

    private @NotNull Map<String, String> getDefaultCategoryDescriptions() {
        return Map.of(
            "OVERWORLD_COMMON", "Common overworld biomes with standard features",
            "OVERWORLD_RARE", "Rare and unique overworld biomes with special properties",
            "OCEAN", "Various ocean biomes with aquatic themes",
            "NETHER", "Dangerous nether biomes with unique challenges",
            "END", "Mysterious end dimension biomes"
        );
    }

    // ==================== BIOME PERMISSIONS GETTERS ====================

    public @NotNull Map<String, String> getIndividualBiomePermissions() {
        return this.individualBiomePermissions != null ? this.individualBiomePermissions : new HashMap<>();
    }

    public @NotNull String getDefaultBiomePermission() {
        return this.defaultBiomePermission != null ? this.defaultBiomePermission : "jeoneblock.biome.use";
    }

    public boolean arePermissionChecksEnabled() {
        return this.enablePermissionChecks != null ? this.enablePermissionChecks : true;
    }

    public boolean shouldInheritCategoryPermissions() {
        return this.inheritCategoryPermissions != null ? this.inheritCategoryPermissions : true;
    }

    // ==================== BIOME COSTS GETTERS ====================

    public @NotNull Map<String, Integer> getIndividualBiomeCosts() {
        return this.individualBiomeCosts != null ? this.individualBiomeCosts : new HashMap<>();
    }

    public @NotNull Integer getDefaultBiomeCost() {
        return this.defaultBiomeCost != null ? this.defaultBiomeCost : 1000;
    }

    public @NotNull String getCostCurrency() {
        return this.costCurrency != null ? this.costCurrency : "coins";
    }

    public boolean areBiomeCostsEnabled() {
        return this.enableBiomeCosts != null ? this.enableBiomeCosts : true;
    }

    public @NotNull Map<String, Double> getCostMultipliers() {
        return this.costMultipliers != null ? this.costMultipliers : getDefaultCostMultipliers();
    }

    private @NotNull Map<String, Double> getDefaultCostMultipliers() {
        return Map.of(
            "VIP", 0.8,
            "PREMIUM", 0.6,
            "ELITE", 0.4,
            "LEGENDARY", 0.2
        );
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Gets the permission required for a specific biome.
     */
    public @NotNull String getBiomePermission(@NotNull Biome biome) {
        if (!arePermissionChecksEnabled()) {
            return "";
        }

        String biomeName = biome.name();

        // Check individual biome permissions first
        String individualPermission = getIndividualBiomePermissions().get(biomeName);
        if (individualPermission != null) {
            return individualPermission;
        }

        // Check category permissions if inheritance is enabled
        if (shouldInheritCategoryPermissions()) {
            String categoryPermission = getCategoryPermissionForBiome(biome);
            if (categoryPermission != null) {
                return categoryPermission;
            }
        }

        return getDefaultBiomePermission();
    }

    private String getCategoryPermissionForBiome(@NotNull Biome biome) {
        for (Map.Entry<String, List<Biome>> entry : getBiomeCategories().entrySet()) {
            if (entry.getValue().contains(biome)) {
                return getBiomeCategoryPermissions().get(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Gets the cost for a specific biome.
     */
    public int getBiomeCost(@NotNull Biome biome) {
        if (!areBiomeCostsEnabled()) {
            return 0;
        }

        String biomeName = biome.name();

        // Check individual biome costs first
        Integer individualCost = getIndividualBiomeCosts().get(biomeName);
        if (individualCost != null) {
            return individualCost;
        }

        // Check category costs
        Integer categoryCost = getCategoryCostForBiome(biome);
        if (categoryCost != null) {
            return categoryCost;
        }

        return getDefaultBiomeCost();
    }

    private Integer getCategoryCostForBiome(@NotNull Biome biome) {
        for (Map.Entry<String, List<Biome>> entry : getBiomeCategories().entrySet()) {
            if (entry.getValue().contains(biome)) {
                return getBiomeCategoryCosts().get(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Calculates the final cost with multipliers applied.
     */
    public int getCalculatedBiomeCost(@NotNull Biome biome, @NotNull String playerRank) {
        int baseCost = getBiomeCost(biome);
        double multiplier = getCostMultipliers().getOrDefault(playerRank, 1.0);
        return (int) Math.ceil(baseCost * multiplier);
    }

    /**
     * Gets the category that contains the specified biome.
     */
    public @NotNull Optional<String> getBiomeCategory(@NotNull Biome biome) {
        for (Map.Entry<String, List<Biome>> entry : getBiomeCategories().entrySet()) {
            if (entry.getValue().contains(biome)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a biome is restricted.
     */
    public boolean isBiomeRestricted(@NotNull Biome biome) {
        List<String> restricted = getRestrictedBiomes();
        return restricted != null && restricted.contains(biome.name());
    }

    public @NotNull List<String> getRestrictedBiomes() {
        return this.restrictedBiomes != null ? this.restrictedBiomes : List.of();
    }

    /**
     * Gets a biome by its alias.
     */
    public @NotNull Optional<Biome> getBiomeByAlias(@NotNull String alias) {
        String biomeName = getBiomeAliases().get(alias.toLowerCase());
        if (biomeName != null) {
            try {
                return Optional.of(Biome.valueOf(biomeName));
            } catch (IllegalArgumentException ignored) {
                // Invalid biome name
            }
        }
        return Optional.empty();
    }

    public @NotNull Map<String, String> getBiomeAliases() {
        return this.biomeAliases != null ? this.biomeAliases : getDefaultBiomeAliases();
    }

    private @NotNull Map<String, String> getDefaultBiomeAliases() {
        return Map.of(
            "grass", "PLAINS",
            "trees", "FOREST",
            "sand", "DESERT",
            "snow", "SNOWY_TUNDRA",
            "water", "OCEAN",
            "hell", "NETHER_WASTES",
            "void", "THE_END"
        );
    }
}