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

package com.raindropcentral.rds.service.shop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.ShopAdminGroupSetting;
import com.raindropcentral.rds.database.entity.ShopAdminPlayerSetting;
import com.raindropcentral.rds.database.repository.RShopAdminGroupSetting;
import com.raindropcentral.rds.database.repository.RShopAdminPlayerSetting;
import com.raindropcentral.rplatform.api.luckperms.LuckPermsService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists and resolves player/group-specific admin overrides for shop limits and discounts.
 *
 * <p>Overrides are stored in the RDS database tables:
 * <ul>
 *     <li>{@code shop_admin_player_settings}</li>
 *     <li>{@code shop_admin_group_settings}</li>
 * </ul>
 *
 * <p>Legacy flatfile data from {@code config/admin-player-settings.yml} is imported automatically
 * once on load when present, then archived.</p>
 *
 * <p>Resolution order prefers player values first, then matching group values, then edition/config
 * defaults provided by the caller.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminPlayerSettingsService {

    private static final String PLAYERS_SECTION = "players";
    private static final String GROUPS_SECTION = "groups";
    private static final String NAME_PATH = "name";
    private static final String MAX_SHOPS_PATH = "max_shops";
    private static final String DISCOUNT_PERCENT_PATH = "discount_percent";
    private static final String LEGACY_ADMIN_SETTINGS_FILE = "admin-player-settings.yml";

    private final RDS plugin;

    private final Map<UUID, PlayerOverride> playerOverrides = new LinkedHashMap<>();
    private final Map<String, GroupOverride> groupOverrides = new LinkedHashMap<>();

    /**
     * Creates a new player/group admin override resolver.
     *
     * @param plugin active plugin runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public ShopAdminPlayerSettingsService(final @NotNull RDS plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Loads persisted override data from the database.
     */
    public synchronized void load() {
        this.playerOverrides.clear();
        this.groupOverrides.clear();

        this.importLegacyFlatfileIfPresent();
        this.loadPlayerOverridesFromDatabase();
        this.loadGroupOverridesFromDatabase();
    }

    /**
     * Persists all currently loaded overrides to the database.
     *
     * <p>Overrides are already persisted on each write operation. This method is retained for
     * lifecycle compatibility and ensures the in-memory snapshot is fully synchronized.</p>
     *
     * @throws IllegalStateException when repository services are unavailable
     */
    public synchronized void save() {
        final RShopAdminPlayerSetting playerRepository = this.requirePlayerRepository();
        final RShopAdminGroupSetting groupRepository = this.requireGroupRepository();

        for (final Map.Entry<UUID, PlayerOverride> entry : this.playerOverrides.entrySet()) {
            final PlayerOverride override = entry.getValue();
            if (override == null || (override.maximumShops() == null && override.discountPercent() == null)) {
                playerRepository.deleteByPlayerId(entry.getKey());
                continue;
            }

            playerRepository.upsert(
                    entry.getKey(),
                    override.playerName(),
                    override.maximumShops(),
                    override.discountPercent()
            );
        }

        for (final ShopAdminPlayerSetting storedSetting : playerRepository.findAllEntries()) {
            if (this.playerOverrides.containsKey(storedSetting.getPlayerId())) {
                continue;
            }
            playerRepository.deleteByPlayerId(storedSetting.getPlayerId());
        }

        for (final Map.Entry<String, GroupOverride> entry : this.groupOverrides.entrySet()) {
            final GroupOverride override = entry.getValue();
            if (override == null || (override.maximumShops() == null && override.discountPercent() == null)) {
                groupRepository.deleteByGroupName(entry.getKey());
                continue;
            }

            groupRepository.upsert(
                    entry.getKey(),
                    override.maximumShops(),
                    override.discountPercent()
            );
        }

        for (final ShopAdminGroupSetting storedSetting : groupRepository.findAllEntries()) {
            if (this.groupOverrides.containsKey(storedSetting.getGroupName())) {
                continue;
            }
            groupRepository.deleteByGroupName(storedSetting.getGroupName());
        }
    }

    /**
     * Resolves the effective max shops for a player.
     *
     * @param player target player
     * @param defaultMaximumShops fallback default maximum shops
     * @return resolved max shops ({@code -1} means unlimited)
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public int resolveMaximumShops(
            final @NotNull Player player,
            final int defaultMaximumShops
    ) {
        Objects.requireNonNull(player, "player");
        final PlayerOverride playerOverride = this.getPlayerOverride(player.getUniqueId());
        if (playerOverride != null && playerOverride.maximumShops() != null) {
            return playerOverride.maximumShops();
        }

        final Integer groupMaximum = this.resolveGroupMaximumShops(player);
        return groupMaximum == null ? defaultMaximumShops : groupMaximum;
    }

    /**
     * Resolves the effective max shops for an offline player identifier.
     *
     * <p>This overload only considers explicit player overrides and does not evaluate group
     * membership.</p>
     *
     * @param playerId target player UUID
     * @param defaultMaximumShops fallback default maximum shops
     * @return resolved max shops ({@code -1} means unlimited)
     * @throws NullPointerException if {@code playerId} is {@code null}
     */
    public int resolveMaximumShops(
            final @NotNull UUID playerId,
            final int defaultMaximumShops
    ) {
        Objects.requireNonNull(playerId, "playerId");
        final PlayerOverride playerOverride = this.getPlayerOverride(playerId);
        return playerOverride != null && playerOverride.maximumShops() != null
                ? playerOverride.maximumShops()
                : defaultMaximumShops;
    }

    /**
     * Resolves the effective purchase discount percent for a player.
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
                this.plugin.getLogger().warning("Failed to read LuckPerms groups for " + onlinePlayer.getName() + ": " + exception.getMessage());
            }
        }

        for (final String configuredGroup : this.getGroupOverrides().keySet()) {
            try {
                if (luckPermsService.groupExists(configuredGroup).join()) {
                    discoveredGroups.add(configuredGroup);
                }
            } catch (Exception exception) {
                this.plugin.getLogger().warning("Failed to verify LuckPerms group " + configuredGroup + ": " + exception.getMessage());
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
     * Updates the player max-shops override.
     *
     * @param playerId target player UUID
     * @param playerName optional player display name
     * @param maximumShops override value ({@code -1} for unlimited, or positive)
     * @throws NullPointerException if {@code playerId} is {@code null}
     * @throws IllegalArgumentException when {@code maximumShops} is neither {@code -1} nor positive
     */
    public synchronized void setPlayerMaximumShops(
            final @NotNull UUID playerId,
            final @Nullable String playerName,
            final int maximumShops
    ) {
        validateMaximumShops(maximumShops);
        final UUID validatedPlayerId = Objects.requireNonNull(playerId, "playerId");
        final PlayerOverride current = this.playerOverrides.get(validatedPlayerId);
        final PlayerOverride updated = new PlayerOverride(
                normalizeOptionalName(playerName == null && current != null ? current.playerName() : playerName),
                maximumShops,
                current == null ? null : current.discountPercent()
        );

        this.persistPlayerOverride(validatedPlayerId, updated);
        this.playerOverrides.put(validatedPlayerId, updated);
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
        validateDiscountPercent(discountPercent);
        final UUID validatedPlayerId = Objects.requireNonNull(playerId, "playerId");
        final PlayerOverride current = this.playerOverrides.get(validatedPlayerId);
        final PlayerOverride updated = new PlayerOverride(
                normalizeOptionalName(playerName == null && current != null ? current.playerName() : playerName),
                current == null ? null : current.maximumShops(),
                clampDiscountPercent(discountPercent)
        );

        this.persistPlayerOverride(validatedPlayerId, updated);
        this.playerOverrides.put(validatedPlayerId, updated);
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
        final UUID validatedPlayerId = Objects.requireNonNull(playerId, "playerId");
        this.requirePlayerRepository().deleteByPlayerId(validatedPlayerId);
        this.playerOverrides.remove(validatedPlayerId);
    }

    /**
     * Updates the group max-shops override.
     *
     * @param groupName group identifier
     * @param maximumShops override value ({@code -1} for unlimited, or positive)
     * @throws NullPointerException if {@code groupName} is {@code null}
     * @throws IllegalArgumentException when inputs are invalid
     */
    public synchronized void setGroupMaximumShops(
            final @NotNull String groupName,
            final int maximumShops
    ) {
        validateMaximumShops(maximumShops);
        final String normalizedGroup = normalizeGroupName(groupName);
        final GroupOverride current = this.groupOverrides.get(normalizedGroup);
        final GroupOverride updated = new GroupOverride(
                normalizedGroup,
                maximumShops,
                current == null ? null : current.discountPercent()
        );

        this.persistGroupOverride(normalizedGroup, updated);
        this.groupOverrides.put(normalizedGroup, updated);
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
                current == null ? null : current.maximumShops(),
                clampDiscountPercent(discountPercent)
        );

        this.persistGroupOverride(normalizedGroup, updated);
        this.groupOverrides.put(normalizedGroup, updated);
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
        final String normalizedGroup = normalizeGroupName(groupName);
        this.requireGroupRepository().deleteByGroupName(normalizedGroup);
        this.groupOverrides.remove(normalizedGroup);
    }

    private void loadPlayerOverridesFromDatabase() {
        final RShopAdminPlayerSetting playerRepository = this.requirePlayerRepository();
        for (final ShopAdminPlayerSetting setting : playerRepository.findAllEntries()) {
            final Integer maximumShops = setting.getMaximumShops();
            final Double discountPercent = setting.getDiscountPercent();
            if (maximumShops == null && discountPercent == null) {
                continue;
            }

            this.playerOverrides.put(
                    setting.getPlayerId(),
                    new PlayerOverride(
                            normalizeOptionalName(setting.getPlayerName()),
                            maximumShops,
                            discountPercent
                    )
            );
        }
    }

    private void loadGroupOverridesFromDatabase() {
        final RShopAdminGroupSetting groupRepository = this.requireGroupRepository();
        for (final ShopAdminGroupSetting setting : groupRepository.findAllEntries()) {
            final Integer maximumShops = setting.getMaximumShops();
            final Double discountPercent = setting.getDiscountPercent();
            if (maximumShops == null && discountPercent == null) {
                continue;
            }

            final String normalizedGroupName = normalizeGroupName(setting.getGroupName());
            this.groupOverrides.put(
                    normalizedGroupName,
                    new GroupOverride(
                            normalizedGroupName,
                            maximumShops,
                            discountPercent
                    )
            );
        }
    }

    private void importLegacyFlatfileIfPresent() {
        final File legacyFile = this.getLegacySettingsFile();
        if (!legacyFile.exists()) {
            return;
        }

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(legacyFile);
        final Map<UUID, PlayerOverride> legacyPlayerOverrides = this.parseLegacyPlayerOverrides(configuration);
        final Map<String, GroupOverride> legacyGroupOverrides = this.parseLegacyGroupOverrides(configuration);
        if (legacyPlayerOverrides.isEmpty() && legacyGroupOverrides.isEmpty()) {
            this.archiveLegacySettingsFile(legacyFile);
            return;
        }

        final RShopAdminPlayerSetting playerRepository = this.requirePlayerRepository();
        final RShopAdminGroupSetting groupRepository = this.requireGroupRepository();
        for (final Map.Entry<UUID, PlayerOverride> entry : legacyPlayerOverrides.entrySet()) {
            final PlayerOverride override = entry.getValue();
            playerRepository.upsert(
                    entry.getKey(),
                    override.playerName(),
                    override.maximumShops(),
                    override.discountPercent()
            );
        }

        for (final Map.Entry<String, GroupOverride> entry : legacyGroupOverrides.entrySet()) {
            final GroupOverride override = entry.getValue();
            groupRepository.upsert(
                    entry.getKey(),
                    override.maximumShops(),
                    override.discountPercent()
            );
        }

        this.archiveLegacySettingsFile(legacyFile);
        this.plugin.getLogger().info(
                "Imported legacy admin player settings from "
                        + legacyFile.getName()
                        + " into the database."
        );
    }

    private @NotNull Map<UUID, PlayerOverride> parseLegacyPlayerOverrides(
            final @NotNull YamlConfiguration configuration
    ) {
        final Map<UUID, PlayerOverride> parsedOverrides = new LinkedHashMap<>();
        final var playersSection = configuration.getConfigurationSection(PLAYERS_SECTION);
        if (playersSection == null) {
            return parsedOverrides;
        }

        for (final String playerKey : playersSection.getKeys(false)) {
            final UUID playerId;
            try {
                playerId = UUID.fromString(playerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            final String path = PLAYERS_SECTION + "." + playerKey;
            final Integer maximumShops = readMaximumShops(configuration.get(path + "." + MAX_SHOPS_PATH));
            final Double discountPercent = readDiscountPercent(configuration.get(path + "." + DISCOUNT_PERCENT_PATH));
            if (maximumShops == null && discountPercent == null) {
                continue;
            }

            final String playerName = normalizeOptionalName(configuration.getString(path + "." + NAME_PATH));
            parsedOverrides.put(playerId, new PlayerOverride(playerName, maximumShops, discountPercent));
        }

        return parsedOverrides;
    }

    private @NotNull Map<String, GroupOverride> parseLegacyGroupOverrides(
            final @NotNull YamlConfiguration configuration
    ) {
        final Map<String, GroupOverride> parsedOverrides = new LinkedHashMap<>();
        final var groupsSection = configuration.getConfigurationSection(GROUPS_SECTION);
        if (groupsSection == null) {
            return parsedOverrides;
        }

        for (final String groupKey : groupsSection.getKeys(false)) {
            final String normalizedGroup = normalizeGroupName(groupKey);
            final String path = GROUPS_SECTION + "." + groupKey;
            final Integer maximumShops = readMaximumShops(configuration.get(path + "." + MAX_SHOPS_PATH));
            final Double discountPercent = readDiscountPercent(configuration.get(path + "." + DISCOUNT_PERCENT_PATH));
            if (maximumShops == null && discountPercent == null) {
                continue;
            }

            parsedOverrides.put(normalizedGroup, new GroupOverride(normalizedGroup, maximumShops, discountPercent));
        }

        return parsedOverrides;
    }

    private void archiveLegacySettingsFile(
            final @NotNull File legacyFile
    ) {
        final File migratedFile = this.buildLegacyArchiveFile(legacyFile);
        try {
            Files.move(
                    legacyFile.toPath(),
                    migratedFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException moveException) {
            this.plugin.getLogger().warning(
                    "Failed to archive legacy admin settings file " + legacyFile.getAbsolutePath() + ": " + moveException.getMessage()
            );
        }
    }

    private @NotNull File buildLegacyArchiveFile(
            final @NotNull File legacyFile
    ) {
        final String baseName = legacyFile.getName() + ".migrated";
        File candidate = new File(legacyFile.getParentFile(), baseName);
        int suffix = 1;
        while (candidate.exists()) {
            candidate = new File(legacyFile.getParentFile(), baseName + "." + suffix);
            suffix++;
        }
        return candidate;
    }

    private @Nullable Integer resolveGroupMaximumShops(
            final @NotNull Player player
    ) {
        Integer resolvedMaximum = null;
        for (final GroupOverride groupOverride : this.groupOverrides.values()) {
            if (groupOverride.maximumShops() == null || !this.isPlayerInGroup(player, groupOverride.groupName())) {
                continue;
            }

            if (groupOverride.maximumShops() == -1) {
                return -1;
            }

            if (resolvedMaximum == null || groupOverride.maximumShops() > resolvedMaximum) {
                resolvedMaximum = groupOverride.maximumShops();
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
                || player.hasPermission("raindropshops.group." + normalizedGroupName);
    }

    private void persistPlayerOverride(
            final @NotNull UUID playerId,
            final @NotNull PlayerOverride override
    ) {
        final RShopAdminPlayerSetting playerRepository = this.requirePlayerRepository();
        if (override.maximumShops() == null && override.discountPercent() == null) {
            playerRepository.deleteByPlayerId(playerId);
            return;
        }

        playerRepository.upsert(
                playerId,
                override.playerName(),
                override.maximumShops(),
                override.discountPercent()
        );
    }

    private void persistGroupOverride(
            final @NotNull String groupName,
            final @NotNull GroupOverride override
    ) {
        final RShopAdminGroupSetting groupRepository = this.requireGroupRepository();
        if (override.maximumShops() == null && override.discountPercent() == null) {
            groupRepository.deleteByGroupName(groupName);
            return;
        }

        groupRepository.upsert(
                groupName,
                override.maximumShops(),
                override.discountPercent()
        );
    }

    private @NotNull RShopAdminPlayerSetting requirePlayerRepository() {
        final RShopAdminPlayerSetting repository = this.plugin.getShopAdminPlayerSettingRepository();
        if (repository != null) {
            return repository;
        }
        throw new IllegalStateException("Admin player settings repository is unavailable.");
    }

    private @NotNull RShopAdminGroupSetting requireGroupRepository() {
        final RShopAdminGroupSetting repository = this.plugin.getShopAdminGroupSettingRepository();
        if (repository != null) {
            return repository;
        }
        throw new IllegalStateException("Admin group settings repository is unavailable.");
    }

    private @NotNull File getLegacySettingsFile() {
        return new File(new File(this.plugin.getDataFolder(), "config"), LEGACY_ADMIN_SETTINGS_FILE);
    }

    private static @Nullable Integer readMaximumShops(
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

    private static void validateMaximumShops(
            final int maximumShops
    ) {
        if (maximumShops != -1 && maximumShops < 1) {
            throw new IllegalArgumentException("Maximum shops must be -1 or greater than 0");
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
     * @param maximumShops optional max shops override ({@code -1} means unlimited)
     * @param discountPercent optional purchase discount percent
     * @author ItsRainingHP
     * @version 1.0.0
     */
    public record PlayerOverride(
            @Nullable String playerName,
            @Nullable Integer maximumShops,
            @Nullable Double discountPercent
    ) {
    }

    /**
     * Immutable group-specific admin override.
     *
     * @param groupName normalized group identifier
     * @param maximumShops optional max shops override ({@code -1} means unlimited)
     * @param discountPercent optional purchase discount percent
     * @author ItsRainingHP
     * @version 1.0.0
     */
    public record GroupOverride(
            @NotNull String groupName,
            @Nullable Integer maximumShops,
            @Nullable Double discountPercent
    ) {
    }
}
