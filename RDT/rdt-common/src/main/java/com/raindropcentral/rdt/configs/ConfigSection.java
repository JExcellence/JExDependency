package com.raindropcentral.rdt.configs;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import com.raindropcentral.rdt.utils.ChunkType;

/**
 * Root configuration section for the RDT plugin.
 *
 * <p>This section exposes global runtime defaults used by shared town gameplay systems.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.6
 */
@CSAlways
@SuppressWarnings("unused")
public class ConfigSection extends AConfigSection {

    private static final int DEFAULT_GLOBAL_MAX_CHUNK_LIMIT = 64;
    private static final int DEFAULT_CHUNK_BLOCK_MIN_Y = -10;
    private static final int DEFAULT_CHUNK_BLOCK_MAX_Y = 10;
    private static final int DEFAULT_TOWN_SPAWN_TELEPORT_DELAY_SECONDS = 3;
    private static final Material DEFAULT_ICON_NEXUS = Material.REINFORCED_DEEPSLATE;
    private static final Material DEFAULT_ICON_DEFAULT = Material.OAK_PLANKS;
    private static final Material DEFAULT_ICON_BANK = Material.GOLD_BLOCK;
    private static final Material DEFAULT_ICON_FARM = Material.HAY_BLOCK;
    private static final Material DEFAULT_ICON_CLAIM_PENDING = Material.ORANGE_STAINED_GLASS;
    private static final Material DEFAULT_ICON_CHUNK_BLOCK = Material.OAK_PLANKS;
    private static final int DEFAULT_TOWN_LEVEL_COUNT = 10;
    private static final double DEFAULT_TOWN_LEVEL_BASE_REQUIREMENT = 2500.0D;
    private static final double DEFAULT_TOWN_LEVEL_REQUIREMENT_GROWTH = 1.2D;
    private static final double DEFAULT_TOWN_LEVEL_REWARD_MULTIPLIER = 0.2D;

    private Integer global_max_chunk_limit;
    private Integer chunk_block_min_y;
    private Integer chunk_block_max_y;
    private Integer town_spawn_teleport_delay_seconds;
    private String chunk_type_icon_nexus;
    private String chunk_type_icon_default;
    private String chunk_type_icon_bank;
    private String chunk_type_icon_farm;
    private String chunk_type_icon_claim_pending;
    private String chunk_type_icon_chunk_block;
    private Map<Integer, TownLevelSection> townLevels;

    /**
     * Creates a configuration section bound to the provided evaluation environment.
     *
     * @param baseEnvironment base environment used by the config mapper
     */
    public ConfigSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Returns the global cap for claimed chunks per town.
     *
     * @return positive chunk-claim limit
     */
    public int getGlobalMaxChunkLimit() {
        if (this.global_max_chunk_limit == null || this.global_max_chunk_limit < 1) {
            return DEFAULT_GLOBAL_MAX_CHUNK_LIMIT;
        }
        return this.global_max_chunk_limit;
    }

    /**
     * Returns the minimum Y offset from nexus allowed for Chunk Block placement.
     *
     * @return minimum valid Y offset
     */
    public int getChunkBlockMinY() {
        final int minY = this.getResolvedChunkBlockMinYRaw();
        final int maxY = this.getResolvedChunkBlockMaxYRaw();
        if (minY > maxY) {
            return DEFAULT_CHUNK_BLOCK_MIN_Y;
        }
        return minY;
    }

    /**
     * Returns the maximum Y offset from nexus allowed for Chunk Block placement.
     *
     * @return maximum valid Y offset
     */
    public int getChunkBlockMaxY() {
        final int minY = this.getResolvedChunkBlockMinYRaw();
        final int maxY = this.getResolvedChunkBlockMaxYRaw();
        if (minY > maxY) {
            return DEFAULT_CHUNK_BLOCK_MAX_Y;
        }
        return maxY;
    }

    /**
     * Returns the delay before executing a town-spawn teleport command.
     *
     * @return non-negative delay in seconds
     */
    public int getTownSpawnTeleportDelaySeconds() {
        if (this.town_spawn_teleport_delay_seconds == null || this.town_spawn_teleport_delay_seconds < 0) {
            return DEFAULT_TOWN_SPAWN_TELEPORT_DELAY_SECONDS;
        }
        return this.town_spawn_teleport_delay_seconds;
    }

    /**
     * Returns the configured icon material for a chunk type.
     *
     * @param chunkType chunk type
     * @return material used for this type
     */
    public @NotNull Material getChunkTypeIconMaterial(final @NotNull ChunkType chunkType) {
        return switch (chunkType) {
            case NEXUS -> this.resolveMaterial(this.chunk_type_icon_nexus, DEFAULT_ICON_NEXUS);
            case DEFAULT -> this.resolveMaterial(this.chunk_type_icon_default, DEFAULT_ICON_DEFAULT);
            case BANK -> this.resolveMaterial(this.chunk_type_icon_bank, DEFAULT_ICON_BANK);
            case FARM -> this.resolveMaterial(this.chunk_type_icon_farm, DEFAULT_ICON_FARM);
            case CLAIM_PENDING -> this.resolveMaterial(this.chunk_type_icon_claim_pending, DEFAULT_ICON_CLAIM_PENDING);
            case CHUNK_BLOCK -> this.resolveMaterial(this.chunk_type_icon_chunk_block, DEFAULT_ICON_CHUNK_BLOCK);
        };
    }

    /**
     * Returns all configured town levels keyed by numeric level.
     *
     * @return copied ordered map of configured town levels
     */
    public @NotNull Map<Integer, TownLevelSection> getTownLevels() {
        final Map<Integer, TownLevelSection> configuredLevels = this.townLevels;
        if (configuredLevels == null || configuredLevels.isEmpty()) {
            return new LinkedHashMap<>(createDefaultTownLevels());
        }
        return new LinkedHashMap<>(configuredLevels);
    }

    /**
     * Returns configuration for one specific town level.
     *
     * @param level target level
     * @return level config or {@code null} when missing
     */
    public @Nullable TownLevelSection getTownLevelSection(final int level) {
        if (level < 1) {
            return null;
        }
        return this.getTownLevels().get(level);
    }

    /**
     * Returns the highest configured town level number.
     *
     * @return highest configured level, or {@code 0} when no levels are configured
     */
    public int getHighestConfiguredTownLevel() {
        int highest = 0;
        for (final Integer configuredLevel : this.getTownLevels().keySet()) {
            if (configuredLevel != null && configuredLevel > highest) {
                highest = configuredLevel;
            }
        }
        return highest;
    }

    /**
     * Returns the next configured town level above the provided current level.
     *
     * @param currentLevel current town level
     * @return next configured level, or {@code null} when already at max level
     */
    public @Nullable Integer getNextTownLevel(final int currentLevel) {
        final int normalizedCurrentLevel = Math.max(1, currentLevel);
        for (final Integer configuredLevel : this.getTownLevels().keySet()) {
            if (configuredLevel != null && configuredLevel > normalizedCurrentLevel) {
                return configuredLevel;
            }
        }
        return null;
    }

    /**
     * Loads the RDT root config from disk.
     *
     * @param configFile config file to parse
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromFile(final @NotNull File configFile) {
        return fromConfiguration(YamlConfiguration.loadConfiguration(configFile));
    }

    /**
     * Loads the RDT root config from a bundled resource stream.
     *
     * @param inputStream config resource stream
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
    }

    /**
     * Creates a default config section with field-level fallback values.
     *
     * @return default config section
     */
    public static @NotNull ConfigSection createDefault() {
        return new ConfigSection(new EvaluationEnvironmentBuilder());
    }

    private static @NotNull ConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        final ConfigSection section = createDefault();
        if (configuration.contains("global_max_chunk_limit")) {
            section.global_max_chunk_limit = configuration.getInt(
                    "global_max_chunk_limit",
                    DEFAULT_GLOBAL_MAX_CHUNK_LIMIT
            );
        }
        if (configuration.contains("chunk_block_min_y")) {
            section.chunk_block_min_y = configuration.getInt(
                    "chunk_block_min_y",
                    DEFAULT_CHUNK_BLOCK_MIN_Y
            );
        }
        if (configuration.contains("chunk_block_max_y")) {
            section.chunk_block_max_y = configuration.getInt(
                    "chunk_block_max_y",
                    DEFAULT_CHUNK_BLOCK_MAX_Y
            );
        }
        if (configuration.contains("town_spawn_teleport_delay_seconds")) {
            section.town_spawn_teleport_delay_seconds = configuration.getInt(
                    "town_spawn_teleport_delay_seconds",
                    DEFAULT_TOWN_SPAWN_TELEPORT_DELAY_SECONDS
            );
        }
        if (configuration.contains("chunk_type_icon_nexus")) {
            section.chunk_type_icon_nexus = configuration.getString("chunk_type_icon_nexus");
        }
        if (configuration.contains("chunk_type_icon_default")) {
            section.chunk_type_icon_default = configuration.getString("chunk_type_icon_default");
        }
        if (configuration.contains("chunk_type_icon_bank")) {
            section.chunk_type_icon_bank = configuration.getString("chunk_type_icon_bank");
        }
        if (configuration.contains("chunk_type_icon_farm")) {
            section.chunk_type_icon_farm = configuration.getString("chunk_type_icon_farm");
        }
        if (configuration.contains("chunk_type_icon_claim_pending")) {
            section.chunk_type_icon_claim_pending = configuration.getString("chunk_type_icon_claim_pending");
        }
        if (configuration.contains("chunk_type_icon_chunk_block")) {
            section.chunk_type_icon_chunk_block = configuration.getString("chunk_type_icon_chunk_block");
        }
        section.townLevels = parseTownLevels(configuration.getConfigurationSection("town.levels"));
        return section;
    }

    private int getResolvedChunkBlockMinYRaw() {
        if (this.chunk_block_min_y == null) {
            return DEFAULT_CHUNK_BLOCK_MIN_Y;
        }
        return this.chunk_block_min_y;
    }

    private int getResolvedChunkBlockMaxYRaw() {
        if (this.chunk_block_max_y == null) {
            return DEFAULT_CHUNK_BLOCK_MAX_Y;
        }
        return this.chunk_block_max_y;
    }

    private @NotNull Material resolveMaterial(
            final @Nullable String rawMaterialName,
            final @NotNull Material fallback
    ) {
        if (rawMaterialName == null || rawMaterialName.isBlank()) {
            return fallback;
        }

        final @Nullable Material resolved = Material.matchMaterial(
                rawMaterialName.trim().toUpperCase(Locale.ROOT)
        );
        if (resolved == null) {
            return fallback;
        }
        return resolved;
    }

    private static @NotNull Map<Integer, TownLevelSection> parseTownLevels(
            final @Nullable ConfigurationSection levelsSection
    ) {
        if (levelsSection == null) {
            return createDefaultTownLevels();
        }

        final Map<Integer, TownLevelSection> parsedLevels = new LinkedHashMap<>();
        for (final String levelKey : levelsSection.getKeys(false)) {
            final Integer levelNumber = parsePositiveLevel(levelKey);
            if (levelNumber == null) {
                continue;
            }

            final ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
            if (levelSection == null) {
                continue;
            }

            final Map<String, Map<String, Object>> requirements = parseDefinitionEntries(
                    levelSection.getConfigurationSection("requirements")
            );
            final Map<String, Map<String, Object>> rewards = parseDefinitionEntries(
                    levelSection.getConfigurationSection("rewards")
            );
            parsedLevels.put(levelNumber, new TownLevelSection(levelNumber, requirements, rewards));
        }

        if (parsedLevels.isEmpty()) {
            return createDefaultTownLevels();
        }

        final List<Integer> sortedLevels = new ArrayList<>(parsedLevels.keySet());
        sortedLevels.sort(Comparator.naturalOrder());
        final Map<Integer, TownLevelSection> orderedLevels = new LinkedHashMap<>();
        for (final Integer sortedLevel : sortedLevels) {
            final TownLevelSection levelSection = parsedLevels.get(sortedLevel);
            if (sortedLevel == null || levelSection == null) {
                continue;
            }
            orderedLevels.put(sortedLevel, levelSection);
        }
        return orderedLevels;
    }

    private static @NotNull Map<String, Map<String, Object>> parseDefinitionEntries(
            final @Nullable ConfigurationSection section
    ) {
        final Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        if (section == null) {
            return entries;
        }

        for (final String key : section.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!section.isConfigurationSection(key)) {
                continue;
            }

            final ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) {
                continue;
            }
            entries.put(normalizeDefinitionKey(key), convertSection(entrySection));
        }

        return entries;
    }

    private static @NotNull Map<String, Object> convertSection(final @NotNull ConfigurationSection section) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            values.put(key, normalizeValue(section.get(key)));
        }
        return values;
    }

    private static @Nullable Object normalizeValue(final @Nullable Object value) {
        if (value instanceof ConfigurationSection nestedSection) {
            return convertSection(nestedSection);
        }

        if (value instanceof Map<?, ?> valueMap) {
            final Map<String, Object> normalized = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : valueMap.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                normalized.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
            }
            return normalized;
        }

        if (value instanceof List<?> valueList) {
            final List<Object> normalized = new ArrayList<>(valueList.size());
            for (final Object entry : valueList) {
                normalized.add(normalizeValue(entry));
            }
            return normalized;
        }

        return value;
    }

    private static @Nullable Integer parsePositiveLevel(final @Nullable String levelKey) {
        if (levelKey == null || levelKey.isBlank()) {
            return null;
        }
        try {
            final int parsedLevel = Integer.parseInt(levelKey.trim());
            return parsedLevel < 1 ? null : parsedLevel;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static @NotNull String normalizeDefinitionKey(final @NotNull String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull Map<Integer, TownLevelSection> createDefaultTownLevels() {
        final Map<Integer, TownLevelSection> defaults = new LinkedHashMap<>();
        for (int level = 1; level <= DEFAULT_TOWN_LEVEL_COUNT; level++) {
            final Map<String, Map<String, Object>> requirements = new LinkedHashMap<>();
            final Map<String, Map<String, Object>> rewards = new LinkedHashMap<>();
            if (level > 1) {
                final double requirementAmount = Math.round(
                        DEFAULT_TOWN_LEVEL_BASE_REQUIREMENT
                                * Math.pow(DEFAULT_TOWN_LEVEL_REQUIREMENT_GROWTH, level - 2)
                                * 100.0D
                ) / 100.0D;
                final double rewardAmount = Math.round(requirementAmount * DEFAULT_TOWN_LEVEL_REWARD_MULTIPLIER * 100.0D) / 100.0D;

                requirements.put("vault_upgrade", Map.of(
                        "type", "CURRENCY",
                        "currency", "vault",
                        "amount", requirementAmount,
                        "consumable", true,
                        "description", "Vault funding contribution"
                ));
                rewards.put("vault_bonus", Map.of(
                        "type", "CURRENCY",
                        "currencyId", "vault",
                        "amount", rewardAmount,
                        "description", "Town vault bonus"
                ));
            }
            defaults.put(level, new TownLevelSection(level, requirements, rewards));
        }
        return defaults;
    }

    /**
     * Configuration wrapper for a single town level definition.
     *
     * @param level configured numeric level
     * @param requirements requirement definitions keyed by requirement key
     * @param rewards reward definitions keyed by reward key
     */
    public record TownLevelSection(
            int level,
            @NotNull Map<String, Map<String, Object>> requirements,
            @NotNull Map<String, Map<String, Object>> rewards
    ) {

        /**
         * Creates a defensive town-level config record.
         *
         * @param level configured numeric level
         * @param requirements requirement definitions keyed by requirement key
         * @param rewards reward definitions keyed by reward key
         */
        public TownLevelSection {
            final Map<String, Map<String, Object>> copiedRequirements = new LinkedHashMap<>();
            for (final Map.Entry<String, Map<String, Object>> entry : Objects.requireNonNull(
                    requirements,
                    "requirements"
            ).entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                copiedRequirements.put(entry.getKey(), deepCopyMap(entry.getValue()));
            }

            final Map<String, Map<String, Object>> copiedRewards = new LinkedHashMap<>();
            for (final Map.Entry<String, Map<String, Object>> entry : Objects.requireNonNull(
                    rewards,
                    "rewards"
            ).entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                copiedRewards.put(entry.getKey(), deepCopyMap(entry.getValue()));
            }

            level = Math.max(1, level);
            requirements = copiedRequirements;
            rewards = copiedRewards;
        }

        /**
         * Returns copied requirement definitions.
         *
         * @return copied requirement definitions
         */
        public @NotNull Map<String, Map<String, Object>> getRequirements() {
            final Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            for (final Map.Entry<String, Map<String, Object>> entry : this.requirements.entrySet()) {
                copy.put(entry.getKey(), deepCopyMap(entry.getValue()));
            }
            return copy;
        }

        /**
         * Returns copied reward definitions.
         *
         * @return copied reward definitions
         */
        public @NotNull Map<String, Map<String, Object>> getRewards() {
            final Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            for (final Map.Entry<String, Map<String, Object>> entry : this.rewards.entrySet()) {
                copy.put(entry.getKey(), deepCopyMap(entry.getValue()));
            }
            return copy;
        }

        private static @NotNull Map<String, Object> deepCopyMap(final @NotNull Map<String, Object> source) {
            final Map<String, Object> copy = new LinkedHashMap<>();
            for (final Map.Entry<String, Object> entry : source.entrySet()) {
                copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
            }
            return copy;
        }

        private static @Nullable Object deepCopyValue(final @Nullable Object value) {
            if (value instanceof Map<?, ?> valueMap) {
                final Map<String, Object> nested = new LinkedHashMap<>();
                for (final Map.Entry<?, ?> entry : valueMap.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    nested.put(entry.getKey().toString(), deepCopyValue(entry.getValue()));
                }
                return nested;
            }

            if (value instanceof List<?> valueList) {
                final List<Object> nested = new ArrayList<>(valueList.size());
                for (final Object entry : valueList) {
                    nested.add(deepCopyValue(entry));
                }
                return nested;
            }

            return value;
        }
    }
}
