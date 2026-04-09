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
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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
    private static final long DEFAULT_TOWN_RELATIONSHIP_CHANGE_COOLDOWN_SECONDS = 21_600L;
    private static final int DEFAULT_TOWN_RELATIONSHIP_UNLOCK_LEVEL = 5;
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
    private static final boolean DEFAULT_PROXY_ENABLED = false;
    private static final boolean DEFAULT_PROXY_TOWN_SPAWN_ENABLED = false;
    private static final boolean DEFAULT_CHUNK_TYPE_RESET_STATE_ON_CHANGE = true;
    private static final Set<Material> PROHIBITED_CHUNK_BLOCK_MATERIALS = EnumSet.of(
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.HOPPER
    );

    private Integer global_max_chunk_limit;
    private Integer chunk_block_min_y;
    private Integer chunk_block_max_y;
    private Integer town_spawn_teleport_delay_seconds;
    private Long town_archetype_change_cooldown_seconds;
    private Long town_relationship_change_cooldown_seconds;
    private Integer town_relationship_unlock_level;
    private Boolean exclude_corner_claim_adjacency;
    private Boolean proxy_enabled;
    private Boolean proxy_town_spawn_enabled;
    private Boolean chunk_type_reset_state_on_change;
    private String proxy_server_route_id;
    private String chunk_type_icon_nexus;
    private String chunk_type_icon_default;
    private String chunk_type_icon_bank;
    private String chunk_type_icon_farm;
    private String chunk_type_icon_claim_pending;
    private String chunk_type_icon_chunk_block;

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
     * Returns the configured cooldown in seconds between confirmed town relationship changes.
     *
     * @return relationship-change cooldown in seconds
     */
    public long getTownRelationshipChangeCooldownSeconds() {
        final Long configured = this.town_relationship_change_cooldown_seconds;
        return configured == null || configured < 0L ? DEFAULT_TOWN_RELATIONSHIP_CHANGE_COOLDOWN_SECONDS : configured;
    }

    /**
     * Returns the configured Nexus level required before diplomacy unlocks.
     *
     * @return required Nexus level for town relationships
     */
    public int getTownRelationshipUnlockLevel() {
        final Integer configured = this.town_relationship_unlock_level;
        return configured == null || configured <= 0 ? DEFAULT_TOWN_RELATIONSHIP_UNLOCK_LEVEL : configured;
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
     * Returns whether switching a chunk type should reset chunk-local state.
     *
     * @return {@code true} when chunk-local state resets after a type change
     */
    public boolean isChunkTypeResetOnChange() {
        return this.chunk_type_reset_state_on_change == null
            ? DEFAULT_CHUNK_TYPE_RESET_STATE_ON_CHANGE
            : this.chunk_type_reset_state_on_change;
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
     * Returns the configured default marker-block material used for claimed chunks.
     *
     * @return configured default marker-block material, or a safe fallback when invalid
     */
    public @NotNull Material getDefaultChunkBlockMaterial() {
        return this.resolveBlockMaterial(this.chunk_type_icon_chunk_block, DEFAULT_ICON_CHUNK_BLOCK);
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
            case DEFAULT, SECURITY, OUTPOST, MEDIC, ARMORY -> this.resolveMaterial(
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
        section.town_relationship_change_cooldown_seconds = configuration.getLong(
            "town.relationship_change_cooldown_seconds",
            DEFAULT_TOWN_RELATIONSHIP_CHANGE_COOLDOWN_SECONDS
        );
        section.town_relationship_unlock_level = configuration.getInt(
            "town.relationship_unlock_level",
            DEFAULT_TOWN_RELATIONSHIP_UNLOCK_LEVEL
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
        section.chunk_type_reset_state_on_change = configuration.getBoolean(
            "chunk_type.reset_state_on_change",
            DEFAULT_CHUNK_TYPE_RESET_STATE_ON_CHANGE
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

    private @NotNull Material resolveBlockMaterial(
        final @Nullable String materialName,
        final @NotNull Material fallback
    ) {
        if (materialName == null || materialName.isBlank()) {
            return fallback;
        }
        final Material material = Material.matchMaterial(materialName.trim().toUpperCase(Locale.ROOT));
        if (material == null
            || !LevelConfigSupport.isConfiguredBlockMaterial(material)
            || PROHIBITED_CHUNK_BLOCK_MATERIALS.contains(material)) {
            return fallback;
        }
        return material;
    }
}
