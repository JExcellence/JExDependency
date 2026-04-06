/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.configs;

import com.raindropcentral.rdt.utils.ChunkType;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed configuration snapshot for the RDT runtime.
 *
 * <p>The section keeps the public API lightweight while supporting direct YAML parsing for tests
 * and runtime bootstrap. Values fall back to safe defaults when missing or invalid.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ConfigSection extends AConfigSection {

    private static final long DEFAULT_TOWN_ARCHETYPE_CHANGE_COOLDOWN_SECONDS = 86_400L;
    private static final int DEFAULT_GLOBAL_MAX_CHUNK_LIMIT = 64;
    private static final int DEFAULT_CHUNK_BLOCK_MIN_Y = -10;
    private static final int DEFAULT_CHUNK_BLOCK_MAX_Y = 10;
    private static final int DEFAULT_TOWN_SPAWN_TELEPORT_DELAY_SECONDS = 3;
    private static final boolean DEFAULT_EXCLUDE_CORNER_CLAIM_ADJACENCY = true;
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
    private static final boolean DEFAULT_PROXY_ENABLED = false;
    private static final boolean DEFAULT_PROXY_TOWN_SPAWN_ENABLED = false;

    private Integer global_max_chunk_limit;
    private Integer chunk_block_min_y;
    private Integer chunk_block_max_y;
    private Integer town_spawn_teleport_delay_seconds;
    private Long town_archetype_change_cooldown_seconds;
    private Boolean exclude_corner_claim_adjacency;
    private Boolean proxy_enabled;
    private Boolean proxy_town_spawn_enabled;
    private String proxy_server_route_id;
    private String chunk_type_icon_nexus;
    private String chunk_type_icon_default;
    private String chunk_type_icon_bank;
    private String chunk_type_icon_farm;
    private String chunk_type_icon_claim_pending;
    private String chunk_type_icon_chunk_block;
    private Map<Integer, TownLevelSection> townLevels;

    /**
     * Creates an empty config section backed by the supplied evaluation environment.
     *
     * @param environmentBuilder expression environment used by the underlying config mapper
     */
    public ConfigSection(final @NotNull EvaluationEnvironmentBuilder environmentBuilder) {
        super(environmentBuilder);
    }

    /**
     * Returns the configured global chunk limit for towns.
     *
     * @return global town chunk cap
     */
    public int getGlobalMaxChunkLimit() {
        final Integer configured = this.global_max_chunk_limit;
        return configured == null || configured <= 0 ? DEFAULT_GLOBAL_MAX_CHUNK_LIMIT : configured;
    }

    /**
     * Returns the minimum allowed Y offset for placing chunk blocks relative to the nexus.
     *
     * @return minimum allowed chunk-block Y offset
     */
    public int getChunkBlockMinY() {
        return this.getResolvedChunkBlockMinYRaw();
    }

    /**
     * Returns the maximum allowed Y offset for placing chunk blocks relative to the nexus.
     *
     * @return maximum allowed chunk-block Y offset
     */
    public int getChunkBlockMaxY() {
        return this.getResolvedChunkBlockMaxYRaw();
    }

    /**
     * Returns the configured town-spawn delay in seconds.
     *
     * @return teleport delay in seconds
     */
    public int getTownSpawnTeleportDelaySeconds() {
        final Integer configured = this.town_spawn_teleport_delay_seconds;
        return configured == null || configured < 0 ? DEFAULT_TOWN_SPAWN_TELEPORT_DELAY_SECONDS : configured;
    }

    /**
     * Returns the configured cooldown in seconds between town archetype changes.
     *
     * @return archetype change cooldown in seconds
     */
    public long getTownArchetypeChangeCooldownSeconds() {
        final Long configured = this.town_archetype_change_cooldown_seconds;
        return configured == null || configured < 0L ? DEFAULT_TOWN_ARCHETYPE_CHANGE_COOLDOWN_SECONDS : configured;
    }

    /**
     * Returns whether diagonal chunk claims should be excluded from adjacency checks.
     *
     * @return {@code true} when only cardinal neighbors count as adjacent
     */
    public boolean isCornerClaimAdjacencyExcluded() {
        return this.exclude_corner_claim_adjacency == null
            ? DEFAULT_EXCLUDE_CORNER_CLAIM_ADJACENCY
            : this.exclude_corner_claim_adjacency;
    }

    /**
     * Returns whether proxy-aware routing is enabled.
     *
     * @return {@code true} when proxy routing is enabled
     */
    public boolean isProxyEnabled() {
        return Boolean.TRUE.equals(this.proxy_enabled);
    }

    /**
     * Returns whether proxy-backed town-spawn transfers are enabled.
     *
     * @return {@code true} when proxy town spawn routing is enabled
     */
    public boolean isProxyTownSpawnEnabled() {
        return Boolean.TRUE.equals(this.proxy_town_spawn_enabled);
    }

    /**
     * Returns the configured proxy route identifier for the local server.
     *
     * @return normalized proxy route identifier, or an empty string when none is configured
     */
    public @NotNull String getProxyServerRouteId() {
        return this.proxy_server_route_id == null ? "" : this.proxy_server_route_id.trim();
    }

    /**
     * Returns the configured icon material for a chunk type.
     *
     * @param chunkType chunk type to resolve
     * @return configured icon material, or a safe fallback when no explicit mapping exists
     */
    public @NotNull Material getChunkTypeIconMaterial(final @Nullable ChunkType chunkType) {
        if (chunkType == null) {
            return DEFAULT_ICON_DEFAULT;
        }

        return switch (chunkType) {
            case NEXUS -> this.resolveMaterial(this.chunk_type_icon_nexus, DEFAULT_ICON_NEXUS);
            case DEFAULT, SECURITY, OUTPOST, MEDIC -> this.resolveMaterial(
                this.chunk_type_icon_default,
                DEFAULT_ICON_DEFAULT
            );
            case BANK -> this.resolveMaterial(this.chunk_type_icon_bank, DEFAULT_ICON_BANK);
            case FARM -> this.resolveMaterial(this.chunk_type_icon_farm, DEFAULT_ICON_FARM);
            case CLAIM_PENDING -> this.resolveMaterial(
                this.chunk_type_icon_claim_pending,
                DEFAULT_ICON_CLAIM_PENDING
            );
        };
    }

    /**
     * Returns a defensive copy of the configured town levels.
     *
     * @return configured town levels keyed by target level
     */
    public @NotNull Map<Integer, TownLevelSection> getTownLevels() {
        return Map.copyOf(this.townLevels == null ? createDefaultTownLevels() : this.townLevels);
    }

    /**
     * Returns a configured town level section.
     *
     * @param level target level to resolve
     * @return configured level section, or {@code null} when no section exists
     */
    public @Nullable TownLevelSection getTownLevelSection(final int level) {
        return this.getTownLevels().get(level);
    }

    /**
     * Returns the highest configured town level.
     *
     * @return highest configured level
     */
    public int getHighestConfiguredTownLevel() {
        return this.getTownLevels().keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /**
     * Returns the next configured town level after the supplied current level.
     *
     * @param currentLevel current town level
     * @return next configured level, or {@code null} when already at the highest level
     */
    public @Nullable Integer getNextTownLevel(final int currentLevel) {
        return this.getTownLevels().keySet().stream()
            .filter(level -> level > currentLevel)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    /**
     * Parses a config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config section
     */
    public static @NotNull ConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read config stream", exception);
        }
    }

    /**
     * Returns a config section populated with built-in defaults.
     *
     * @return default config section
     */
    public static @NotNull ConfigSection createDefault() {
        return fromConfiguration(new YamlConfiguration());
    }

    private static @NotNull ConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        final ConfigSection section = new ConfigSection(new EvaluationEnvironmentBuilder());
        section.global_max_chunk_limit = configuration.getInt("global_max_chunk_limit", DEFAULT_GLOBAL_MAX_CHUNK_LIMIT);
        section.chunk_block_min_y = configuration.getInt("chunk_block_min_y", DEFAULT_CHUNK_BLOCK_MIN_Y);
        section.chunk_block_max_y = configuration.getInt("chunk_block_max_y", DEFAULT_CHUNK_BLOCK_MAX_Y);
        section.town_spawn_teleport_delay_seconds = configuration.getInt(
            "town_spawn_teleport_delay_seconds",
            DEFAULT_TOWN_SPAWN_TELEPORT_DELAY_SECONDS
        );
        section.town_archetype_change_cooldown_seconds = configuration.getLong(
            "town.archetype_change_cooldown_seconds",
            DEFAULT_TOWN_ARCHETYPE_CHANGE_COOLDOWN_SECONDS
        );
        section.exclude_corner_claim_adjacency = configuration.getBoolean(
            "exclude_corner_claim_adjacency",
            DEFAULT_EXCLUDE_CORNER_CLAIM_ADJACENCY
        );
        section.proxy_enabled = configuration.getBoolean("proxy.enabled", DEFAULT_PROXY_ENABLED);
        section.proxy_town_spawn_enabled = configuration.getBoolean(
            "proxy.town_spawn_enabled",
            DEFAULT_PROXY_TOWN_SPAWN_ENABLED
        );
        section.proxy_server_route_id = configuration.getString("proxy.server_route_id", "");
        section.chunk_type_icon_nexus = configuration.getString("chunk_type_icon_nexus", DEFAULT_ICON_NEXUS.name());
        section.chunk_type_icon_default = configuration.getString("chunk_type_icon_default", DEFAULT_ICON_DEFAULT.name());
        section.chunk_type_icon_bank = configuration.getString("chunk_type_icon_bank", DEFAULT_ICON_BANK.name());
        section.chunk_type_icon_farm = configuration.getString("chunk_type_icon_farm", DEFAULT_ICON_FARM.name());
        section.chunk_type_icon_claim_pending = configuration.getString(
            "chunk_type_icon_claim_pending",
            DEFAULT_ICON_CLAIM_PENDING.name()
        );
        section.chunk_type_icon_chunk_block = configuration.getString(
            "chunk_type_icon_chunk_block",
            DEFAULT_ICON_CHUNK_BLOCK.name()
        );
        section.townLevels = parseTownLevels(configuration.getConfigurationSection("town.levels"));
        return section;
    }

    private int getResolvedChunkBlockMinYRaw() {
        final Integer min = this.chunk_block_min_y;
        final Integer max = this.chunk_block_max_y;
        if (min == null || max == null || min > max) {
            return DEFAULT_CHUNK_BLOCK_MIN_Y;
        }
        return min;
    }

    private int getResolvedChunkBlockMaxYRaw() {
        final Integer min = this.chunk_block_min_y;
        final Integer max = this.chunk_block_max_y;
        if (min == null || max == null || min > max) {
            return DEFAULT_CHUNK_BLOCK_MAX_Y;
        }
        return max;
    }

    private @NotNull Material resolveMaterial(
        final @Nullable String materialName,
        final @NotNull Material fallback
    ) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }
        final Material material = Material.matchMaterial(materialName.trim().toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private static @NotNull Map<Integer, TownLevelSection> parseTownLevels(
        final @Nullable ConfigurationSection levelsSection
    ) {
        if (levelsSection == null) {
            return createDefaultTownLevels();
        }

        final Map<Integer, TownLevelSection> townLevels = new LinkedHashMap<>();
        for (final String key : levelsSection.getKeys(false)) {
            final Integer parsedLevel = parsePositiveLevel(key);
            if (parsedLevel == null) {
                continue;
            }

            final ConfigurationSection levelSection = levelsSection.getConfigurationSection(key);
            if (levelSection == null) {
                continue;
            }

            final Map<String, Map<String, Object>> requirements = parseDefinitionEntries(
                levelSection.getConfigurationSection("requirements")
            );
            final Map<String, Map<String, Object>> rewards = parseDefinitionEntries(
                levelSection.getConfigurationSection("rewards")
            );
            townLevels.put(parsedLevel, new TownLevelSection(parsedLevel, requirements, rewards));
        }

        return townLevels.isEmpty() ? createDefaultTownLevels() : townLevels;
    }

    private static @NotNull Map<String, Map<String, Object>> parseDefinitionEntries(
        final @Nullable ConfigurationSection definitionsSection
    ) {
        if (definitionsSection == null) {
            return Map.of();
        }

        final Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
        for (final String key : definitionsSection.getKeys(false)) {
            final String normalizedKey = normalizeDefinitionKey(key);
            if (normalizedKey.isEmpty()) {
                continue;
            }

            final ConfigurationSection definitionSection = definitionsSection.getConfigurationSection(key);
            if (definitionSection != null) {
                definitions.put(normalizedKey, convertSection(definitionSection));
                continue;
            }

            final Object rawValue = definitionsSection.get(key);
            if (rawValue instanceof Map<?, ?> rawMap) {
                final Map<String, Object> converted = new LinkedHashMap<>();
                for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    converted.put(
                        normalizeNestedKey(String.valueOf(entry.getKey())),
                        normalizeValue(entry.getValue())
                    );
                }
                definitions.put(normalizedKey, converted);
            }
        }
        return definitions;
    }

    private static @NotNull Map<String, Object> convertSection(final @NotNull ConfigurationSection section) {
        final Map<String, Object> converted = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            converted.put(normalizeNestedKey(key), normalizeValue(section.get(key)));
        }
        return converted;
    }

    private static @Nullable Object normalizeValue(final @Nullable Object rawValue) {
        if (rawValue instanceof ConfigurationSection nestedSection) {
            return convertSection(nestedSection);
        }
        if (rawValue instanceof Map<?, ?> nestedMap) {
            final Map<String, Object> converted = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : nestedMap.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                converted.put(
                    normalizeNestedKey(String.valueOf(entry.getKey())),
                    normalizeValue(entry.getValue())
                );
            }
            return converted;
        }
        if (rawValue instanceof List<?> list) {
            final List<Object> converted = new ArrayList<>();
            for (final Object value : list) {
                converted.add(normalizeValue(value));
            }
            return converted;
        }
        return rawValue;
    }

    private static @Nullable Integer parsePositiveLevel(final @Nullable String rawLevel) {
        if (rawLevel == null || rawLevel.isBlank()) {
            return null;
        }
        try {
            final int parsed = Integer.parseInt(rawLevel.trim());
            return parsed > 0 ? parsed : null;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static @NotNull String normalizeDefinitionKey(final @Nullable String rawKey) {
        if (rawKey == null) {
            return "";
        }
        return rawKey.trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull String normalizeNestedKey(final @Nullable String rawKey) {
        return rawKey == null ? "" : rawKey.trim();
    }

    private static @NotNull Map<Integer, TownLevelSection> createDefaultTownLevels() {
        final Map<Integer, TownLevelSection> defaults = new LinkedHashMap<>();
        for (int level = 1; level <= DEFAULT_TOWN_LEVEL_COUNT; level++) {
            if (level == 1) {
                defaults.put(level, new TownLevelSection(level, Map.of(), Map.of()));
                continue;
            }

            final double requirementAmount = Math.round(
                DEFAULT_TOWN_LEVEL_BASE_REQUIREMENT
                    * Math.pow(DEFAULT_TOWN_LEVEL_REQUIREMENT_GROWTH, level - 2)
                    * 100.0D
            ) / 100.0D;
            final double rewardAmount = Math.round(
                requirementAmount * DEFAULT_TOWN_LEVEL_REWARD_MULTIPLIER * 100.0D
            ) / 100.0D;
            defaults.put(
                level,
                new TownLevelSection(
                    level,
                    Map.of(
                        "vault_upgrade",
                        Map.of(
                            "type", "CURRENCY",
                            "currency", "vault",
                            "amount", requirementAmount,
                            "consumable", true,
                            "description", "Vault funding contribution"
                        )
                    ),
                    Map.of(
                        "vault_bonus",
                        Map.of(
                            "type", "CURRENCY",
                            "currencyId", "vault",
                            "amount", rewardAmount,
                            "description", "Town vault bonus"
                        )
                    )
                )
            );
        }
        return defaults;
    }

    /**
     * Immutable config snapshot for a single town level.
     *
     * @param level target level reached after completing the requirements
     * @param requirements normalized requirement definitions keyed by identifier
     * @param rewards normalized reward definitions keyed by identifier
     */
    public record TownLevelSection(
        int level,
        @NotNull Map<String, Map<String, Object>> requirements,
        @NotNull Map<String, Map<String, Object>> rewards
    ) {

        /**
         * Creates an immutable town level section snapshot.
         *
         * @param level target level reached after leveling up
         * @param requirements normalized requirement definitions
         * @param rewards normalized reward definitions
         */
        public TownLevelSection {
            level = Math.max(1, level);
            requirements = deepCopyDefinitionMap(requirements);
            rewards = deepCopyDefinitionMap(rewards);
        }

        /**
         * Returns a defensive deep copy of the configured requirement definitions.
         *
         * @return copied requirement definitions
         */
        public @NotNull Map<String, Map<String, Object>> getRequirements() {
            return deepCopyDefinitionMap(this.requirements);
        }

        /**
         * Returns a defensive deep copy of the configured reward definitions.
         *
         * @return copied reward definitions
         */
        public @NotNull Map<String, Map<String, Object>> getRewards() {
            return deepCopyDefinitionMap(this.rewards);
        }

        private static @NotNull Map<String, Map<String, Object>> deepCopyDefinitionMap(
            final @Nullable Map<String, Map<String, Object>> definitions
        ) {
            if (definitions == null || definitions.isEmpty()) {
                return Map.of();
            }

            final Map<String, Map<String, Object>> copy = new LinkedHashMap<>();
            for (final Map.Entry<String, Map<String, Object>> entry : definitions.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }

                final Map<String, Object> nestedCopy = new LinkedHashMap<>();
                if (entry.getValue() != null) {
                    for (final Map.Entry<String, Object> nestedEntry : entry.getValue().entrySet()) {
                        nestedCopy.put(nestedEntry.getKey(), deepCopyValue(nestedEntry.getValue()));
                    }
                }
                copy.put(entry.getKey(), nestedCopy);
            }
            return copy;
        }

        private static @Nullable Object deepCopyValue(final @Nullable Object value) {
            if (value instanceof Map<?, ?> map) {
                final Map<String, Object> copiedMap = new LinkedHashMap<>();
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    copiedMap.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
                }
                return copiedMap;
            }
            if (value instanceof List<?> list) {
                final List<Object> copiedList = new ArrayList<>();
                for (final Object entry : list) {
                    copiedList.add(deepCopyValue(entry));
                }
                return copiedList;
            }
            return value;
        }
    }
}
