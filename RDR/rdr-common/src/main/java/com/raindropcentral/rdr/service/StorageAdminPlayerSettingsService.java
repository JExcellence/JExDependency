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

package com.raindropcentral.rdr.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists and resolves player/group-specific admin overrides for storage limits and discounts.
 *
 * <p>Overrides are stored in {@code config/admin-player-settings.yml} and can define:
 * <ul>
 *     <li>per-player max storage limits and storage-store discounts</li>
 *     <li>per-group max storage limits and storage-store discounts</li>
 * </ul>
 *
 * <p>Resolution order prefers player values first, then matching group values, then edition/config
 * defaults provided by the caller.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminPlayerSettingsService {

    private static final String PLAYERS_SECTION = "players";
    private static final String GROUPS_SECTION = "groups";
    private static final String NAME_PATH = "name";
    private static final String MAX_STORAGES_PATH = "max_storages";
    private static final String DISCOUNT_PERCENT_PATH = "discount_percent";
    private static final String ADMIN_SETTINGS_FILE = "admin-player-settings.yml";

    private final RDR plugin;

    private final Map<UUID, PlayerOverride> playerOverrides = new LinkedHashMap<>();
    private final Map<String, GroupOverride> groupOverrides = new LinkedHashMap<>();

    /**
     * Creates a new player/group admin override resolver.
     *
     * @param plugin active plugin runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public StorageAdminPlayerSettingsService(final @NotNull RDR plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Loads persisted override data from disk.
     */
    public synchronized void load() {
        this.playerOverrides.clear();
        this.groupOverrides.clear();

        final File settingsFile = this.getSettingsFile();
        if (!settingsFile.exists()) {
            return;
        }

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(settingsFile);
        this.loadPlayerOverrides(configuration);
        this.loadGroupOverrides(configuration);
    }

    /**
     * Saves all currently loaded overrides to disk.
     *
     * @throws IllegalStateException when the settings file could not be written
     */
    public synchronized void save() {
        final File settingsFile = this.getSettingsFile();
        final File parent = settingsFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create settings directory: " + parent.getAbsolutePath());
        }

        final YamlConfiguration configuration = new YamlConfiguration();
        for (final Map.Entry<UUID, PlayerOverride> entry : this.playerOverrides.entrySet()) {
            final String rootPath = PLAYERS_SECTION + "." + entry.getKey();
            final PlayerOverride override = entry.getValue();

            if (override.playerName() != null && !override.playerName().isBlank()) {
                configuration.set(rootPath + "." + NAME_PATH, override.playerName());
            }
            if (override.maximumStorages() != null) {
                configuration.set(rootPath + "." + MAX_STORAGES_PATH, override.maximumStorages());
            }
            if (override.discountPercent() != null) {
                configuration.set(rootPath + "." + DISCOUNT_PERCENT_PATH, override.discountPercent());
            }
        }

        for (final Map.Entry<String, GroupOverride> entry : this.groupOverrides.entrySet()) {
            final String rootPath = GROUPS_SECTION + "." + entry.getKey();
            final GroupOverride override = entry.getValue();
            if (override.maximumStorages() != null) {
                configuration.set(rootPath + "." + MAX_STORAGES_PATH, override.maximumStorages());
            }
            if (override.discountPercent() != null) {
                configuration.set(rootPath + "." + DISCOUNT_PERCENT_PATH, override.discountPercent());
            }
        }

        try {
            configuration.save(settingsFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save admin player settings.", exception);
        }
    }

    /**
     * Resolves the effective max storages for a player.
     *
     * @param player target player
     * @param defaultMaximumStorages fallback default max storages
     * @return resolved max storages ({@code -1} means unlimited)
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public int resolveMaximumStorages(
        final @NotNull Player player,
        final int defaultMaximumStorages
    ) {
        Objects.requireNonNull(player, "player");
        final PlayerOverride playerOverride = this.getPlayerOverride(player.getUniqueId());
        if (playerOverride != null && playerOverride.maximumStorages() != null) {
            return playerOverride.maximumStorages();
        }

        final Integer groupMaximum = this.resolveGroupMaximumStorages(player);
        return groupMaximum == null ? defaultMaximumStorages : groupMaximum;
    }

    /**
     * Resolves the effective max storages for an offline player identifier.
     *
     * <p>This overload only considers explicit player overrides and does not evaluate group
     * membership.</p>
     *
     * @param playerId target player UUID
     * @param defaultMaximumStorages fallback default max storages
     * @return resolved max storages ({@code -1} means unlimited)
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public int resolveMaximumStorages(
        final @NotNull UUID playerId,
        final int defaultMaximumStorages
    ) {
        Objects.requireNonNull(playerId, "playerId");
        final PlayerOverride playerOverride = this.getPlayerOverride(playerId);
        return playerOverride != null && playerOverride.maximumStorages() != null
            ? playerOverride.maximumStorages()
            : defaultMaximumStorages;
    }

    /**
     * Resolves the effective storage-store discount percent for a player.
     *
     * @param player target player
     * @return resolved discount percent in the range {@code 0.0} to {@code 100.0}
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public double resolveDiscountPercent(
        final @NotNull Player player
    ) {
        Objects.requireNonNull(player, "player");
        final PlayerOverride playerOverride = this.getPlayerOverride(player.getUniqueId());
        if (playerOverride != null && playerOverride.discountPercent() != null) {
            return clampDiscountPercent(playerOverride.discountPercent());
        }

        final Double groupDiscount = this.resolveGroupDiscountPercent(player);
        return groupDiscount == null ? 0.0D : clampDiscountPercent(groupDiscount);
    }

    /**
     * Returns a snapshot of configured player overrides.
     *
     * @return copied player override map
     */
    public synchronized @NotNull Map<UUID, PlayerOverride> getPlayerOverrides() {
        return new LinkedHashMap<>(this.playerOverrides);
    }

    /**
     * Returns a snapshot of configured group overrides.
     *
     * @return copied group override map
     */
    public synchronized @NotNull Map<String, GroupOverride> getGroupOverrides() {
        return new LinkedHashMap<>(this.groupOverrides);
    }

    /**
     * Returns currently loaded LuckPerms group names.
     *
     * <p>Group names are normalized to lowercase and sorted case-insensitively.</p>
     *
     * @return loaded LuckPerms group names, or an empty list when LuckPerms is unavailable
     */
    public @NotNull List<String> getLuckPermsGroupNames() {
        final LuckPermsService luckPermsService = this.plugin.getLuckPermsService();
        if (luckPermsService == null) {
            return List.of();
        }

        final Set<String> discoveredGroups = new java.util.LinkedHashSet<>();
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            try {
                discoveredGroups.addAll(luckPermsService.getPlayerGroups(onlinePlayer));
            } catch (Exception exception) {
                this.plugin.getLogger().warning(
                    "Failed to read LuckPerms groups for " + onlinePlayer.getName() + ": " + exception.getMessage()
                );
            }
        }

        for (final String configuredGroup : this.getGroupOverrides().keySet()) {
            try {
                if (luckPermsService.groupExists(configuredGroup).join()) {
                    discoveredGroups.add(configuredGroup);
                }
            } catch (Exception exception) {
                this.plugin.getLogger().warning(
                    "Failed to verify LuckPerms group " + configuredGroup + ": " + exception.getMessage()
                );
            }
        }

        final List<String> groups = new ArrayList<>(discoveredGroups);
        groups.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(groups);
    }

    /**
     * Returns one player override.
     *
     * @param playerId target player UUID
     * @return override entry, or {@code null} when none exists
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public synchronized @Nullable PlayerOverride getPlayerOverride(
        final @NotNull UUID playerId
    ) {
        Objects.requireNonNull(playerId, "playerId");
        return this.playerOverrides.get(playerId);
    }

    /**
     * Returns one group override.
     *
     * @param groupName group identifier
     * @return override entry, or {@code null} when none exists
     * @throws NullPointerException if {@code groupName} is {@code null}
     */
    public synchronized @Nullable GroupOverride getGroupOverride(
        final @NotNull String groupName
    ) {
        Objects.requireNonNull(groupName, "groupName");
        return this.groupOverrides.get(normalizeGroupName(groupName));
    }

    /**
     * Updates the player max-storages override.
     *
     * @param playerId target player UUID
     * @param playerName optional player display name
     * @param maximumStorages override value ({@code -1} for unlimited, or positive)
     * @throws NullPointerException if {@code playerId} is {@code null}
     * @throws IllegalArgumentException when {@code maximumStorages} is neither {@code -1} nor positive
     */
    public synchronized void setPlayerMaximumStorages(
        final @NotNull UUID playerId,
        final @Nullable String playerName,
        final int maximumStorages
    ) {
        Objects.requireNonNull(playerId, "playerId");
        validateMaximumStorages(maximumStorages);
        final PlayerOverride current = this.playerOverrides.get(playerId);
        final PlayerOverride updated = new PlayerOverride(
            normalizeOptionalName(playerName == null && current != null ? current.playerName() : playerName),
            maximumStorages,
            current == null ? null : current.discountPercent()
        );
        this.playerOverrides.put(playerId, updated);
        this.save();
    }

    /**
     * Updates the player discount override.
     *
     * @param playerId target player UUID
     * @param playerName optional player display name
     * @param discountPercent discount percent between {@code 0.0} and {@code 100.0}
     * @throws NullPointerException if {@code playerId} is {@code null}
     * @throws IllegalArgumentException when {@code discountPercent} is outside valid range
     */
    public synchronized void setPlayerDiscountPercent(
        final @NotNull UUID playerId,
        final @Nullable String playerName,
        final double discountPercent
    ) {
        Objects.requireNonNull(playerId, "playerId");
        validateDiscountPercent(discountPercent);
        final PlayerOverride current = this.playerOverrides.get(playerId);
        final PlayerOverride updated = new PlayerOverride(
            normalizeOptionalName(playerName == null && current != null ? current.playerName() : playerName),
            current == null ? null : current.maximumStorages(),
            clampDiscountPercent(discountPercent)
        );
        this.playerOverrides.put(playerId, updated);
        this.save();
    }

    /**
     * Removes all configured overrides for a player.
     *
     * @param playerId target player UUID
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public synchronized void clearPlayerOverrides(
        final @NotNull UUID playerId
    ) {
        Objects.requireNonNull(playerId, "playerId");
        this.playerOverrides.remove(playerId);
        this.save();
    }

    /**
     * Updates the group max-storages override.
     *
     * @param groupName group identifier
     * @param maximumStorages override value ({@code -1} for unlimited, or positive)
     * @throws NullPointerException if {@code groupName} is {@code null}
     * @throws IllegalArgumentException when inputs are invalid
     */
    public synchronized void setGroupMaximumStorages(
        final @NotNull String groupName,
        final int maximumStorages
    ) {
        validateMaximumStorages(maximumStorages);
        final String normalizedGroup = normalizeGroupName(groupName);
        final GroupOverride current = this.groupOverrides.get(normalizedGroup);
        final GroupOverride updated = new GroupOverride(
            normalizedGroup,
            maximumStorages,
            current == null ? null : current.discountPercent()
        );
        this.groupOverrides.put(normalizedGroup, updated);
        this.save();
    }

    /**
     * Updates the group discount override.
     *
     * @param groupName group identifier
     * @param discountPercent discount percent between {@code 0.0} and {@code 100.0}
     * @throws NullPointerException if {@code groupName} is {@code null}
     * @throws IllegalArgumentException when inputs are invalid
     */
    public synchronized void setGroupDiscountPercent(
        final @NotNull String groupName,
        final double discountPercent
    ) {
        validateDiscountPercent(discountPercent);
        final String normalizedGroup = normalizeGroupName(groupName);
        final GroupOverride current = this.groupOverrides.get(normalizedGroup);
        final GroupOverride updated = new GroupOverride(
            normalizedGroup,
            current == null ? null : current.maximumStorages(),
            clampDiscountPercent(discountPercent)
        );
        this.groupOverrides.put(normalizedGroup, updated);
        this.save();
    }

    /**
     * Removes all configured overrides for a group.
     *
     * @param groupName group identifier
     * @throws NullPointerException if {@code groupName} is {@code null}
     */
    public synchronized void clearGroupOverrides(
        final @NotNull String groupName
    ) {
        Objects.requireNonNull(groupName, "groupName");
        this.groupOverrides.remove(normalizeGroupName(groupName));
        this.save();
    }

    private void loadPlayerOverrides(
        final @NotNull YamlConfiguration configuration
    ) {
        final var playersSection = configuration.getConfigurationSection(PLAYERS_SECTION);
        if (playersSection == null) {
            return;
        }

        for (final String playerKey : playersSection.getKeys(false)) {
            final UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            final String path = PLAYERS_SECTION + "." + playerKey;
            final Integer maximumStorages = readMaximumStorages(configuration.get(path + "." + MAX_STORAGES_PATH));
            final Double discountPercent = readDiscountPercent(configuration.get(path + "." + DISCOUNT_PERCENT_PATH));
            if (maximumStorages == null && discountPercent == null) {
                continue;
            }

            final String playerName = normalizeOptionalName(configuration.getString(path + "." + NAME_PATH));
            this.playerOverrides.put(playerId, new PlayerOverride(playerName, maximumStorages, discountPercent));
        }
    }

    private void loadGroupOverrides(
        final @NotNull YamlConfiguration configuration
    ) {
        final var groupsSection = configuration.getConfigurationSection(GROUPS_SECTION);
        if (groupsSection == null) {
            return;
        }

        for (final String groupKey : groupsSection.getKeys(false)) {
            final String normalizedGroup = normalizeGroupName(groupKey);
            final String path = GROUPS_SECTION + "." + groupKey;
            final Integer maximumStorages = readMaximumStorages(configuration.get(path + "." + MAX_STORAGES_PATH));
            final Double discountPercent = readDiscountPercent(configuration.get(path + "." + DISCOUNT_PERCENT_PATH));
            if (maximumStorages == null && discountPercent == null) {
                continue;
            }

            this.groupOverrides.put(normalizedGroup, new GroupOverride(normalizedGroup, maximumStorages, discountPercent));
        }
    }

    private @Nullable Integer resolveGroupMaximumStorages(
        final @NotNull Player player
    ) {
        Integer resolvedMaximum = null;
        for (final GroupOverride groupOverride : this.groupOverrides.values()) {
            if (groupOverride.maximumStorages() == null || !this.isPlayerInGroup(player, groupOverride.groupName())) {
                continue;
            }

            if (groupOverride.maximumStorages() == -1) {
                return -1;
            }

            if (resolvedMaximum == null || groupOverride.maximumStorages() > resolvedMaximum) {
                resolvedMaximum = groupOverride.maximumStorages();
            }
        }
        return resolvedMaximum;
    }

    private @Nullable Double resolveGroupDiscountPercent(
        final @NotNull Player player
    ) {
        Double resolvedDiscount = null;
        for (final GroupOverride groupOverride : this.groupOverrides.values()) {
            if (groupOverride.discountPercent() == null || !this.isPlayerInGroup(player, groupOverride.groupName())) {
                continue;
            }

            if (resolvedDiscount == null || groupOverride.discountPercent() > resolvedDiscount) {
                resolvedDiscount = groupOverride.discountPercent();
            }
        }
        return resolvedDiscount;
    }

    private boolean isPlayerInGroup(
        final @NotNull Player player,
        final @NotNull String groupName
    ) {
        final String normalizedGroupName = normalizeGroupName(groupName);
        final LuckPermsService luckPermsService = this.plugin.getLuckPermsService();
        if (luckPermsService != null) {
            try {
                return luckPermsService.hasGroup(player, normalizedGroupName);
            } catch (Exception exception) {
                this.plugin.getLogger().warning("Failed to resolve LuckPerms group membership: " + exception.getMessage());
                return false;
            }
        }

        return player.hasPermission("group." + normalizedGroupName)
            || player.hasPermission("raindroprdr.group." + normalizedGroupName);
    }

    private @NotNull File getSettingsFile() {
        return new File(new File(this.plugin.getPlugin().getDataFolder(), "config"), ADMIN_SETTINGS_FILE);
    }

    private static @Nullable Integer readMaximumStorages(
        final @Nullable Object value
    ) {
        if (!(value instanceof Number number)) {
            return null;
        }

        final int parsed = number.intValue();
        if (parsed == -1 || parsed > 0) {
            return parsed;
        }
        return null;
    }

    private static @Nullable Double readDiscountPercent(
        final @Nullable Object value
    ) {
        if (!(value instanceof Number number)) {
            return null;
        }

        final double parsed = number.doubleValue();
        return parsed >= 0.0D && parsed <= 100.0D ? parsed : null;
    }

    private static void validateMaximumStorages(
        final int maximumStorages
    ) {
        if (maximumStorages != -1 && maximumStorages < 1) {
            throw new IllegalArgumentException("Maximum storages must be -1 or greater than 0");
        }
    }

    private static void validateDiscountPercent(
        final double discountPercent
    ) {
        if (discountPercent < 0.0D || discountPercent > 100.0D) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 100");
        }
    }

    private static double clampDiscountPercent(
        final double discountPercent
    ) {
        return Math.max(0.0D, Math.min(100.0D, discountPercent));
    }

    private static @Nullable String normalizeOptionalName(
        final @Nullable String rawValue
    ) {
        if (rawValue == null) {
            return null;
        }

        final String normalizedValue = rawValue.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private static @NotNull String normalizeGroupName(
        final @NotNull String groupName
    ) {
        final String normalized = Objects.requireNonNull(groupName, "groupName")
            .trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be blank");
        }
        return normalized;
    }

    /**
     * Immutable player-specific admin override.
     *
     * @param playerName optional cached player display name
     * @param maximumStorages optional max storages override ({@code -1} means unlimited)
     * @param discountPercent optional storage-store discount percent
     * @author ItsRainingHP
     * @version 5.0.0
     */
    public record PlayerOverride(
        @Nullable String playerName,
        @Nullable Integer maximumStorages,
        @Nullable Double discountPercent
    ) {
    }

    /**
     * Immutable group-specific admin override.
     *
     * @param groupName normalized group identifier
     * @param maximumStorages optional max storages override ({@code -1} means unlimited)
     * @param discountPercent optional storage-store discount percent
     * @author ItsRainingHP
     * @version 5.0.0
     */
    public record GroupOverride(
        @NotNull String groupName,
        @Nullable Integer maximumStorages,
        @Nullable Double discountPercent
    ) {
    }
}
