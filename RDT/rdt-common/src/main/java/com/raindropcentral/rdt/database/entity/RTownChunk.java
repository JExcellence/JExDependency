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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.database.converter.ItemStackMapConverter;
import com.raindropcentral.rplatform.database.converter.LocationConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent claimed chunk owned by a town.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_town_chunks")
public class RTownChunk extends BaseEntity {

    @Column(name = "chunk_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID chunkUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "world_name", nullable = false, length = 64)
    private String worldName;

    @Column(name = "x_loc", nullable = false)
    private int xLoc;

    @Column(name = "z_loc", nullable = false)
    private int zLoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false, length = 32)
    private ChunkType chunkType;

    @Column(name = "chunk_level", nullable = false)
    private int chunkLevel;

    @Convert(converter = LocationConverter.class)
    @Column(name = "chunk_block_location")
    private Location chunkBlockLocation;

    @Convert(converter = LocationConverter.class)
    @Column(name = "fuel_tank_location")
    private Location fuelTankLocation;

    @Convert(converter = LocationConverter.class)
    @Column(name = "seed_box_location")
    private Location seedBoxLocation;

    @Convert(converter = LocationConverter.class)
    @Column(name = "salvage_block_location")
    private Location salvageBlockLocation;

    @Convert(converter = LocationConverter.class)
    @Column(name = "repair_block_location")
    private Location repairBlockLocation;

    @Column(name = "farm_growth_enabled")
    private Boolean farmGrowthEnabled;

    @Column(name = "farm_auto_replant_enabled")
    private Boolean farmAutoReplantEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "farm_replant_priority", length = 32)
    private FarmReplantPriority farmReplantPriority;

    @Column(name = "armory_double_smelt_enabled")
    private Boolean armoryDoubleSmeltEnabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_chunk_protections", joinColumns = @JoinColumn(name = "chunk_id_fk"))
    @Column(name = "required_role_id", nullable = false, length = 64)
    private Map<String, String> protectionOverrides = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_chunk_allied_protections", joinColumns = @JoinColumn(name = "chunk_id_fk"))
    @Column(name = "access_state", nullable = false, length = 32)
    private Map<String, String> alliedProtectionOverrides = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rdt_chunk_level_currency_progress", joinColumns = @JoinColumn(name = "chunk_id_fk"))
    @Column(name = "amount", nullable = false)
    private Map<String, Double> levelCurrencyProgress = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "level_item_progress", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> levelItemProgress = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "seed_box_contents", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> seedBoxContents = new LinkedHashMap<>();

    @Convert(converter = ItemStackMapConverter.class)
    @Column(name = "fuel_tank_contents", columnDefinition = "LONGTEXT")
    private Map<String, ItemStack> fuelTankContents = new LinkedHashMap<>();

    /**
     * Creates a persisted town chunk.
     *
     * @param town owning town
     * @param worldName world name
     * @param xLoc chunk X
     * @param zLoc chunk Z
     * @param chunkType initial chunk type
     */
    public RTownChunk(
        final @NotNull RTown town,
        final @NotNull String worldName,
        final int xLoc,
        final int zLoc,
        final @NotNull ChunkType chunkType
    ) {
        this.chunkUuid = UUID.randomUUID();
        this.town = Objects.requireNonNull(town, "town");
        this.worldName = normalizeWorldName(worldName);
        this.xLoc = xLoc;
        this.zLoc = zLoc;
        this.chunkType = Objects.requireNonNull(chunkType, "chunkType");
        this.chunkLevel = 1;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RTownChunk() {
    }

    /**
     * Returns the stable chunk identifier.
     *
     * @return chunk UUID
     */
    public @NotNull UUID getIdentifier() {
        return this.chunkUuid;
    }

    /**
     * Returns the owning town.
     *
     * @return owning town
     */
    public @NotNull RTown getTown() {
        return this.town;
    }

    /**
     * Returns the claimed world name.
     *
     * @return claimed world name
     */
    public @NotNull String getWorldName() {
        return this.worldName;
    }

    /**
     * Returns the claimed chunk X coordinate.
     *
     * @return claimed chunk X coordinate
     */
    public int getX() {
        return this.xLoc;
    }

    /**
     * Returns the claimed chunk X coordinate.
     *
     * @return claimed chunk X coordinate
     */
    public int getXLoc() {
        return this.xLoc;
    }

    /**
     * Returns the claimed chunk X coordinate using the legacy name.
     *
     * @return claimed chunk X coordinate
     */
    public int getX_loc() {
        return this.xLoc;
    }

    /**
     * Returns the claimed chunk Z coordinate.
     *
     * @return claimed chunk Z coordinate
     */
    public int getZ() {
        return this.zLoc;
    }

    /**
     * Returns the claimed chunk Z coordinate.
     *
     * @return claimed chunk Z coordinate
     */
    public int getZLoc() {
        return this.zLoc;
    }

    /**
     * Returns the claimed chunk Z coordinate using the legacy name.
     *
     * @return claimed chunk Z coordinate
     */
    public int getZ_loc() {
        return this.zLoc;
    }

    /**
     * Returns the assigned chunk type.
     *
     * @return assigned chunk type
     */
    public @NotNull ChunkType getChunkType() {
        return this.chunkType;
    }

    /**
     * Returns the assigned chunk type using the legacy name.
     *
     * @return assigned chunk type
     */
    public @NotNull ChunkType getType() {
        return this.chunkType;
    }

    /**
     * Replaces the assigned chunk type.
     *
     * @param chunkType replacement chunk type
     */
    public void setChunkType(final @NotNull ChunkType chunkType) {
        final ChunkType validatedType = Objects.requireNonNull(chunkType, "chunkType");
        if (this.chunkType != validatedType) {
            this.chunkType = validatedType;
            this.town.recalculateTownLevel();
        }
    }

    /**
     * Resets chunk-local state after a chunk type change.
     */
    public void resetChunkTypeState() {
        this.chunkLevel = 1;
        this.protectionOverrides.clear();
        this.alliedProtectionOverrides.clear();
        this.levelCurrencyProgress.clear();
        this.levelItemProgress.clear();
        this.clearFarmState();
        this.clearFuelTankState();
        this.clearArmoryState();
        this.town.recalculateTownLevel();
    }

    /**
     * Returns the chunk upgrade level.
     *
     * @return chunk upgrade level
     */
    public int getChunkLevel() {
        return this.chunkLevel;
    }

    /**
     * Replaces the chunk upgrade level.
     *
     * @param chunkLevel replacement chunk level
     */
    public void setChunkLevel(final int chunkLevel) {
        this.chunkLevel = Math.max(1, chunkLevel);
        this.town.recalculateTownLevel();
    }

    /**
     * Returns the stored marker block location for this chunk.
     *
     * @return marker block location, or {@code null} when not placed
     */
    public @Nullable Location getChunkBlockLocation() {
        return this.chunkBlockLocation == null ? null : this.chunkBlockLocation.clone();
    }

    /**
     * Replaces the marker block location for this chunk.
     *
     * @param chunkBlockLocation replacement marker location
     */
    public void setChunkBlockLocation(final @Nullable Location chunkBlockLocation) {
        this.chunkBlockLocation = chunkBlockLocation == null ? null : chunkBlockLocation.clone();
    }

    /**
     * Returns the placed fuel tank location for this Security chunk.
     *
     * @return placed fuel tank location, or {@code null} when no tank is placed
     */
    public @Nullable Location getFuelTankLocation() {
        return this.fuelTankLocation == null ? null : this.fuelTankLocation.clone();
    }

    /**
     * Replaces the placed fuel tank location for this Security chunk.
     *
     * @param fuelTankLocation replacement fuel tank location
     */
    public void setFuelTankLocation(final @Nullable Location fuelTankLocation) {
        this.fuelTankLocation = fuelTankLocation == null ? null : fuelTankLocation.clone();
    }

    /**
     * Returns whether a fuel tank is currently tracked for this chunk.
     *
     * @return {@code true} when a fuel tank location is stored
     */
    public boolean hasFuelTank() {
        return this.fuelTankLocation != null;
    }

    /**
     * Returns the placed seed-box location for this Farm chunk.
     *
     * @return placed seed-box location, or {@code null} when no box is placed
     */
    public @Nullable Location getSeedBoxLocation() {
        return this.seedBoxLocation == null ? null : this.seedBoxLocation.clone();
    }

    /**
     * Replaces the placed seed-box location for this Farm chunk.
     *
     * @param seedBoxLocation replacement seed-box location
     */
    public void setSeedBoxLocation(final @Nullable Location seedBoxLocation) {
        this.seedBoxLocation = seedBoxLocation == null ? null : seedBoxLocation.clone();
    }

    /**
     * Returns whether a seed box is currently tracked for this chunk.
     *
     * @return {@code true} when a seed-box location is stored
     */
    public boolean hasSeedBox() {
        return this.seedBoxLocation != null;
    }

    /**
     * Returns the placed salvage-block location for this Armory chunk.
     *
     * @return placed salvage-block location, or {@code null} when none is tracked
     */
    public @Nullable Location getSalvageBlockLocation() {
        return this.salvageBlockLocation == null ? null : this.salvageBlockLocation.clone();
    }

    /**
     * Replaces the placed salvage-block location for this Armory chunk.
     *
     * @param salvageBlockLocation replacement salvage-block location
     */
    public void setSalvageBlockLocation(final @Nullable Location salvageBlockLocation) {
        this.salvageBlockLocation = salvageBlockLocation == null ? null : salvageBlockLocation.clone();
    }

    /**
     * Returns whether a salvage block is currently tracked for this chunk.
     *
     * @return {@code true} when a salvage-block location is stored
     */
    public boolean hasSalvageBlock() {
        return this.salvageBlockLocation != null;
    }

    /**
     * Returns the placed repair-block location for this Armory chunk.
     *
     * @return placed repair-block location, or {@code null} when none is tracked
     */
    public @Nullable Location getRepairBlockLocation() {
        return this.repairBlockLocation == null ? null : this.repairBlockLocation.clone();
    }

    /**
     * Replaces the placed repair-block location for this Armory chunk.
     *
     * @param repairBlockLocation replacement repair-block location
     */
    public void setRepairBlockLocation(final @Nullable Location repairBlockLocation) {
        this.repairBlockLocation = repairBlockLocation == null ? null : repairBlockLocation.clone();
    }

    /**
     * Returns whether a repair block is currently tracked for this chunk.
     *
     * @return {@code true} when a repair-block location is stored
     */
    public boolean hasRepairBlock() {
        return this.repairBlockLocation != null;
    }

    /**
     * Returns a defensive copy of the chunk protection overrides.
     *
     * @return configured protection overrides
     */
    public @NotNull Map<String, String> getProtectionRoleIds() {
        return new LinkedHashMap<>(this.protectionOverrides);
    }

    /**
     * Returns the configured role override for a protection.
     *
     * @param protection protection to resolve
     * @return configured role override, or {@code null} when the chunk inherits the town setting
     */
    public @Nullable String getProtectionRoleId(final @Nullable TownProtections protection) {
        if (protection == null) {
            return null;
        }
        return protection.normalizeOverrideRoleId(this.protectionOverrides.get(protection.getProtectionKey()));
    }

    /**
     * Stores or clears the chunk-level override for a protection.
     *
     * @param protection protection to update
     * @param roleId replacement required role, or {@code null} to clear the override
     */
    public void setProtectionRoleId(
        final @NotNull TownProtections protection,
        final @Nullable String roleId
    ) {
        Objects.requireNonNull(protection, "protection");
        if (roleId == null || roleId.isBlank()) {
            this.protectionOverrides.remove(protection.getProtectionKey());
            return;
        }
        this.protectionOverrides.put(
            protection.getProtectionKey(),
            protection.normalizeConfiguredRoleId(roleId)
        );
    }

    /**
     * Returns whether the chunk overrides the supplied protection.
     *
     * @param protection protection to resolve
     * @return {@code true} when the chunk has an explicit override
     */
    public boolean overridesProtection(final @Nullable TownProtections protection) {
        return protection != null && this.protectionOverrides.containsKey(protection.getProtectionKey());
    }

    /**
     * Returns a defensive copy of configured allied-access overrides.
     *
     * @return copied allied-access overrides
     */
    public @NotNull Map<String, String> getAlliedProtectionStates() {
        return new LinkedHashMap<>(this.alliedProtectionOverrides);
    }

    /**
     * Returns the configured allied-access override for one protection.
     *
     * @param protection protection to resolve
     * @return {@code true} when allies are explicitly allowed, {@code false} when restricted, or
     *     {@code null} when the chunk inherits the town-global setting
     */
    public @Nullable Boolean getConfiguredAlliedProtectionAllowed(final @Nullable TownProtections protection) {
        if (protection == null) {
            return null;
        }
        return parseAlliedProtectionState(this.alliedProtectionOverrides.get(protection.getProtectionKey()));
    }

    /**
     * Stores or clears the chunk-level allied-access override for one protection.
     *
     * @param protection protection to update
     * @param allowed replacement allied-access override, or {@code null} to inherit the town value
     */
    public void setAlliedProtectionAllowed(
        final @NotNull TownProtections protection,
        final @Nullable Boolean allowed
    ) {
        Objects.requireNonNull(protection, "protection");
        if (allowed == null) {
            this.alliedProtectionOverrides.remove(protection.getProtectionKey());
            return;
        }
        this.alliedProtectionOverrides.put(protection.getProtectionKey(), formatAlliedProtectionState(allowed));
    }

    /**
     * Returns whether the chunk overrides allied access for one protection.
     *
     * @param protection protection to inspect
     * @return {@code true} when an explicit allied override is stored
     */
    public boolean overridesAlliedProtection(final @Nullable TownProtections protection) {
        return protection != null && this.alliedProtectionOverrides.containsKey(protection.getProtectionKey());
    }

    /**
     * Returns a defensive copy of the farm seed-box contents.
     *
     * @return copied seed-box contents
     */
    public @NotNull Map<String, ItemStack> getSeedBoxContents() {
        return new LinkedHashMap<>(this.seedBoxContents);
    }

    /**
     * Replaces the farm seed-box contents.
     *
     * @param seedBoxContents replacement seed-box contents
     */
    public void setSeedBoxContents(final @NotNull Map<String, ItemStack> seedBoxContents) {
        this.seedBoxContents = new LinkedHashMap<>(Objects.requireNonNull(seedBoxContents, "seedBoxContents"));
    }

    /**
     * Returns the stored Farm growth-toggle state, falling back when the state has not been initialized yet.
     *
     * @param fallback fallback enabled value
     * @return effective growth-toggle state
     */
    public boolean isFarmGrowthEnabled(final boolean fallback) {
        return this.farmGrowthEnabled == null ? fallback : this.farmGrowthEnabled;
    }

    /**
     * Returns the raw stored Farm growth-toggle state.
     *
     * @return stored growth-toggle state, or {@code null} when not initialized
     */
    public @Nullable Boolean getFarmGrowthEnabledValue() {
        return this.farmGrowthEnabled;
    }

    /**
     * Replaces the stored Farm growth-toggle state.
     *
     * @param farmGrowthEnabled replacement growth-toggle state, or {@code null} to clear it
     */
    public void setFarmGrowthEnabled(final @Nullable Boolean farmGrowthEnabled) {
        this.farmGrowthEnabled = farmGrowthEnabled;
    }

    /**
     * Returns the stored Farm auto-replant state, falling back when the state has not been initialized yet.
     *
     * @param fallback fallback enabled value
     * @return effective auto-replant state
     */
    public boolean isFarmAutoReplantEnabled(final boolean fallback) {
        return this.farmAutoReplantEnabled == null ? fallback : this.farmAutoReplantEnabled;
    }

    /**
     * Returns the raw stored Farm auto-replant state.
     *
     * @return stored auto-replant state, or {@code null} when not initialized
     */
    public @Nullable Boolean getFarmAutoReplantEnabledValue() {
        return this.farmAutoReplantEnabled;
    }

    /**
     * Replaces the stored Farm auto-replant state.
     *
     * @param farmAutoReplantEnabled replacement auto-replant state, or {@code null} to clear it
     */
    public void setFarmAutoReplantEnabled(final @Nullable Boolean farmAutoReplantEnabled) {
        this.farmAutoReplantEnabled = farmAutoReplantEnabled;
    }

    /**
     * Returns the stored Farm replant-source priority, falling back when the state has not been initialized yet.
     *
     * @param fallback fallback replant-source priority
     * @return effective replant-source priority
     */
    public @NotNull FarmReplantPriority getFarmReplantPriority(final @NotNull FarmReplantPriority fallback) {
        return this.farmReplantPriority == null ? Objects.requireNonNull(fallback, "fallback") : this.farmReplantPriority;
    }

    /**
     * Returns the raw stored Farm replant-source priority.
     *
     * @return stored replant-source priority, or {@code null} when not initialized
     */
    public @Nullable FarmReplantPriority getFarmReplantPriorityValue() {
        return this.farmReplantPriority;
    }

    /**
     * Replaces the stored Farm replant-source priority.
     *
     * @param farmReplantPriority replacement replant-source priority, or {@code null} to clear it
     */
    public void setFarmReplantPriority(final @Nullable FarmReplantPriority farmReplantPriority) {
        this.farmReplantPriority = farmReplantPriority;
    }

    /**
     * Returns the stored Armory double-smelt state, falling back when the state has not been initialized yet.
     *
     * @param fallback fallback enabled value
     * @return effective double-smelt state
     */
    public boolean isArmoryDoubleSmeltEnabled(final boolean fallback) {
        return this.armoryDoubleSmeltEnabled == null ? fallback : this.armoryDoubleSmeltEnabled;
    }

    /**
     * Returns the raw stored Armory double-smelt state.
     *
     * @return stored double-smelt state, or {@code null} when not initialized
     */
    public @Nullable Boolean getArmoryDoubleSmeltEnabledValue() {
        return this.armoryDoubleSmeltEnabled;
    }

    /**
     * Replaces the stored Armory double-smelt state.
     *
     * @param armoryDoubleSmeltEnabled replacement double-smelt state, or {@code null} to clear it
     */
    public void setArmoryDoubleSmeltEnabled(final @Nullable Boolean armoryDoubleSmeltEnabled) {
        this.armoryDoubleSmeltEnabled = armoryDoubleSmeltEnabled;
    }

    /**
     * Returns a defensive copy of the stored fuel tank contents.
     *
     * @return copied stored fuel tank contents
     */
    public @NotNull Map<String, ItemStack> getFuelTankContents() {
        return new LinkedHashMap<>(this.fuelTankContents);
    }

    /**
     * Replaces the stored fuel tank contents.
     *
     * @param fuelTankContents replacement fuel tank contents
     */
    public void setFuelTankContents(final @NotNull Map<String, ItemStack> fuelTankContents) {
        this.fuelTankContents = new LinkedHashMap<>(Objects.requireNonNull(fuelTankContents, "fuelTankContents"));
    }

    /**
     * Clears the placed fuel tank location and its persisted contents.
     */
    public void clearFuelTankState() {
        this.fuelTankLocation = null;
        this.fuelTankContents.clear();
    }

    /**
     * Clears the placed seed-box location and its persisted contents.
     */
    public void clearSeedBoxState() {
        this.seedBoxLocation = null;
        this.seedBoxContents.clear();
    }

    /**
     * Clears all Farm-specific persisted state for this chunk.
     */
    public void clearFarmState() {
        this.clearSeedBoxState();
        this.farmGrowthEnabled = null;
        this.farmAutoReplantEnabled = null;
        this.farmReplantPriority = null;
    }

    /**
     * Clears the placed Armory salvage block, repair block, and double-smelt state.
     */
    public void clearArmoryState() {
        this.salvageBlockLocation = null;
        this.repairBlockLocation = null;
        this.armoryDoubleSmeltEnabled = null;
    }

    /**
     * Returns stored level-item progress.
     *
     * @return copied stored item progress
     */
    public @NotNull Map<String, ItemStack> getLevelItemProgress() {
        return new LinkedHashMap<>(this.levelItemProgress);
    }

    /**
     * Returns stored level-item progress for a key.
     *
     * @param progressKey progress key to resolve
     * @return stored progress item, or {@code null} when absent
     */
    public @Nullable ItemStack getLevelItemProgress(final @NotNull String progressKey) {
        return this.levelItemProgress.get(normalizeProgressKey(progressKey));
    }

    /**
     * Replaces stored level-item progress for a key.
     *
     * @param progressKey progress key to update
     * @param itemStack replacement stored item, or {@code null} to clear it
     */
    public void setLevelItemProgress(
        final @NotNull String progressKey,
        final @Nullable ItemStack itemStack
    ) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (itemStack == null || itemStack.isEmpty()) {
            this.levelItemProgress.remove(normalizedProgressKey);
            return;
        }
        this.levelItemProgress.put(normalizedProgressKey, itemStack.clone());
    }

    /**
     * Returns stored level-currency progress for a key.
     *
     * @param progressKey progress key to resolve
     * @return stored currency amount
     */
    public double getLevelCurrencyProgress(final @NotNull String progressKey) {
        return this.levelCurrencyProgress.getOrDefault(normalizeProgressKey(progressKey), 0.0D);
    }

    /**
     * Returns all stored level-currency progress.
     *
     * @return copied stored currency progress
     */
    public @NotNull Map<String, Double> getLevelCurrencyProgress() {
        return new LinkedHashMap<>(this.levelCurrencyProgress);
    }

    /**
     * Replaces stored level-currency progress for a key.
     *
     * @param progressKey progress key to update
     * @param amount replacement amount
     */
    public void setLevelCurrencyProgress(final @NotNull String progressKey, final double amount) {
        final String normalizedProgressKey = normalizeProgressKey(progressKey);
        if (amount <= 0.0D) {
            this.levelCurrencyProgress.remove(normalizedProgressKey);
            return;
        }
        this.levelCurrencyProgress.put(normalizedProgressKey, amount);
    }

    /**
     * Clears stored level progress entries using a stable key prefix.
     *
     * @param progressKeyPrefix shared prefix for one level target
     */
    public void clearLevelRequirementProgress(final @NotNull String progressKeyPrefix) {
        final String normalizedPrefix = normalizeProgressKey(progressKeyPrefix);
        this.levelItemProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
        this.levelCurrencyProgress.keySet().removeIf(key -> key.startsWith(normalizedPrefix));
    }

    private static @NotNull String normalizeWorldName(final @NotNull String worldName) {
        final String normalized = Objects.requireNonNull(worldName, "worldName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("worldName cannot be blank");
        }
        return normalized;
    }

    private static @Nullable Boolean parseAlliedProtectionState(final @Nullable String rawState) {
        if (rawState == null || rawState.isBlank()) {
            return null;
        }
        return Objects.equals(rawState.trim().toUpperCase(Locale.ROOT), "ALLOWED");
    }

    private static @NotNull String formatAlliedProtectionState(final boolean allowed) {
        return allowed ? "ALLOWED" : "RESTRICTED";
    }

    private static @NotNull String normalizeProgressKey(final @NotNull String progressKey) {
        final String normalized = Objects.requireNonNull(progressKey, "progressKey").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("progressKey cannot be blank");
        }
        return normalized;
    }
}
