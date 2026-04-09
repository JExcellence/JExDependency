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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed configuration snapshot for Bank chunk level progression.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class BankConfigSection {

    private static final Material DEFAULT_BLOCK_MATERIAL = Material.GOLD_BLOCK;
    private static final int DEFAULT_CURRENCY_STORAGE_UNLOCK_LEVEL = 1;
    private static final List<String> DEFAULT_CURRENCY_IDS = List.of("vault", "raindrops");
    private static final int DEFAULT_ITEM_STORAGE_UNLOCK_LEVEL = 2;
    private static final int DEFAULT_ITEM_STORAGE_ROWS = 6;
    private static final int DEFAULT_REMOTE_ACCESS_UNLOCK_LEVEL = 3;
    private static final boolean DEFAULT_REMOTE_ACCESS_REQUIRE_OWN_CLAIM = true;
    private static final int DEFAULT_REMOTE_CACHE_DEPOSIT_UNLOCK_LEVEL = 5;
    private static final int DEFAULT_CACHE_UNLOCK_LEVEL = 4;
    private static final int DEFAULT_CACHE_ROWS = 3;
    private static final int DEFAULT_CACHE_PLACEMENT_RADIUS_BLOCKS = 10;
    private static final Material DEFAULT_CACHE_ITEM_MATERIAL = Material.CHEST;
    private static final boolean DEFAULT_SINGLE_VIEWER_LOCKING = true;

    private final Map<Integer, LevelDefinition> levels;
    private final Material blockMaterial;
    private final CurrencyStorageSettings currencyStorage;
    private final ItemStorageSettings itemStorage;
    private final RemoteAccessSettings remoteAccess;
    private final CacheSettings cache;
    private final LockingSettings locking;

    private BankConfigSection(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final @NotNull Material blockMaterial,
        final @NotNull CurrencyStorageSettings currencyStorage,
        final @NotNull ItemStorageSettings itemStorage,
        final @NotNull RemoteAccessSettings remoteAccess,
        final @NotNull CacheSettings cache,
        final @NotNull LockingSettings locking
    ) {
        this.levels = LevelConfigSupport.copyLevels(levels);
        this.blockMaterial = Objects.requireNonNull(blockMaterial, "blockMaterial");
        this.currencyStorage = Objects.requireNonNull(currencyStorage, "currencyStorage");
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage");
        this.remoteAccess = Objects.requireNonNull(remoteAccess, "remoteAccess");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.locking = Objects.requireNonNull(locking, "locking");
    }

    /**
     * Returns a defensive copy of configured Bank levels.
     *
     * @return configured levels keyed by target level
     */
    public @NotNull Map<Integer, LevelDefinition> getLevels() {
        return LevelConfigSupport.copyLevels(this.levels);
    }

    /**
     * Returns the configured Bank level definition.
     *
     * @param level target level to resolve
     * @return matching definition, or {@code null} when absent
     */
    public @Nullable LevelDefinition getLevelDefinition(final int level) {
        return this.levels.get(level);
    }

    /**
     * Returns the configured marker-block material for Bank chunks.
     *
     * @return configured marker-block material
     */
    public @NotNull Material getBlockMaterial() {
        return this.blockMaterial;
    }

    /**
     * Returns the parsed bank currency-storage settings.
     *
     * @return currency-storage settings snapshot
     */
    public @NotNull CurrencyStorageSettings getCurrencyStorage() {
        return this.currencyStorage;
    }

    /**
     * Returns the parsed bank item-storage settings.
     *
     * @return item-storage settings snapshot
     */
    public @NotNull ItemStorageSettings getItemStorage() {
        return this.itemStorage;
    }

    /**
     * Returns the parsed bank remote-access settings.
     *
     * @return remote-access settings snapshot
     */
    public @NotNull RemoteAccessSettings getRemoteAccess() {
        return this.remoteAccess;
    }

    /**
     * Returns the parsed bank cache settings.
     *
     * @return cache settings snapshot
     */
    public @NotNull CacheSettings getCache() {
        return this.cache;
    }

    /**
     * Returns the parsed bank locking settings.
     *
     * @return locking settings snapshot
     */
    public @NotNull LockingSettings getLocking() {
        return this.locking;
    }

    /**
     * Returns the highest configured Bank level.
     *
     * @return highest configured level
     */
    public int getHighestConfiguredLevel() {
        return this.levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /**
     * Returns the next configured level after the supplied current level.
     *
     * @param currentLevel current level
     * @return next configured level, or {@code null} when already at max
     */
    public @Nullable Integer getNextLevel(final int currentLevel) {
        return this.levels.keySet().stream()
            .filter(level -> level > currentLevel)
            .sorted()
            .findFirst()
            .orElse(null);
    }

    /**
     * Parses a Bank config section from a YAML file.
     *
     * @param file source config file
     * @return parsed config snapshot
     */
    public static @NotNull BankConfigSection fromFile(final @NotNull File file) {
        Objects.requireNonNull(file, "file");
        return fromConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Parses a Bank config section from a UTF-8 YAML stream.
     *
     * @param inputStream source YAML stream
     * @return parsed config snapshot
     */
    public static @NotNull BankConfigSection fromInputStream(final @NotNull InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return fromConfiguration(YamlConfiguration.loadConfiguration(reader));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to read bank config stream", exception);
        }
    }

    /**
     * Returns a snapshot populated with built-in defaults.
     *
     * @return default Bank config
     */
    public static @NotNull BankConfigSection createDefault() {
        return new BankConfigSection(
            LevelConfigSupport.createDefaultBankLevels(),
            DEFAULT_BLOCK_MATERIAL,
            CurrencyStorageSettings.createDefault(),
            ItemStorageSettings.createDefault(),
            RemoteAccessSettings.createDefault(),
            CacheSettings.createDefault(),
            LockingSettings.createDefault()
        );
    }

    private static @NotNull BankConfigSection fromConfiguration(final @NotNull YamlConfiguration configuration) {
        return new BankConfigSection(
            LevelConfigSupport.parseLevels(
                configuration.getConfigurationSection("levels"),
                LevelConfigSupport.createDefaultBankLevels()
            ),
            LevelConfigSupport.resolveConfiguredBlockMaterial(
                configuration.getString("block_material"),
                DEFAULT_BLOCK_MATERIAL
            ),
            CurrencyStorageSettings.fromConfiguration(configuration.getConfigurationSection("currency_storage")),
            ItemStorageSettings.fromConfiguration(configuration.getConfigurationSection("item_storage")),
            RemoteAccessSettings.fromConfiguration(configuration.getConfigurationSection("remote_access")),
            CacheSettings.fromConfiguration(configuration.getConfigurationSection("cache")),
            LockingSettings.fromConfiguration(configuration.getConfigurationSection("locking"))
        );
    }

    /**
     * Immutable bank currency-storage settings parsed from {@code bank.yml}.
     *
     * @param unlockLevel Bank level that unlocks currency storage
     * @param currencies configured supported currency identifiers
     */
    public record CurrencyStorageSettings(
        int unlockLevel,
        @NotNull List<String> currencies
    ) {

        /**
         * Creates one immutable bank currency-storage settings snapshot.
         *
         * @param unlockLevel Bank level that unlocks currency storage
         * @param currencies configured supported currency identifiers
         */
        public CurrencyStorageSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_CURRENCY_STORAGE_UNLOCK_LEVEL);
            currencies = copyCurrencyIds(currencies);
        }

        /**
         * Returns the configured currency identifiers in display order.
         *
         * @return copied configured currency identifiers
         */
        @Override
        public @NotNull List<String> currencies() {
            return List.copyOf(this.currencies);
        }

        /**
         * Returns whether currency storage is unlocked at the supplied bank level.
         *
         * @param bankLevel Bank chunk level
         * @return {@code true} when currency storage is unlocked
         */
        public boolean isUnlocked(final int bankLevel) {
            return bankLevel >= this.unlockLevel;
        }

        private static @NotNull CurrencyStorageSettings createDefault() {
            return new CurrencyStorageSettings(DEFAULT_CURRENCY_STORAGE_UNLOCK_LEVEL, DEFAULT_CURRENCY_IDS);
        }

        private static @NotNull CurrencyStorageSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new CurrencyStorageSettings(
                section.getInt("unlock_level", DEFAULT_CURRENCY_STORAGE_UNLOCK_LEVEL),
                section.getStringList("currencies")
            );
        }
    }

    /**
     * Immutable bank item-storage settings parsed from {@code bank.yml}.
     *
     * @param unlockLevel Bank level that unlocks shared item storage
     * @param rows configured shared item-storage row count
     */
    public record ItemStorageSettings(
        int unlockLevel,
        int rows
    ) {

        /**
         * Creates one immutable bank item-storage settings snapshot.
         *
         * @param unlockLevel Bank level that unlocks shared item storage
         * @param rows configured shared item-storage row count
         */
        public ItemStorageSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_ITEM_STORAGE_UNLOCK_LEVEL);
            rows = sanitizeInventoryRows(rows, DEFAULT_ITEM_STORAGE_ROWS);
        }

        /**
         * Returns whether shared item storage is unlocked at the supplied bank level.
         *
         * @param bankLevel Bank chunk level
         * @return {@code true} when shared item storage is unlocked
         */
        public boolean isUnlocked(final int bankLevel) {
            return bankLevel >= this.unlockLevel;
        }

        /**
         * Returns the configured inventory size for shared item storage.
         *
         * @return shared item-storage slot count
         */
        public int inventorySize() {
            return this.rows * 9;
        }

        private static @NotNull ItemStorageSettings createDefault() {
            return new ItemStorageSettings(DEFAULT_ITEM_STORAGE_UNLOCK_LEVEL, DEFAULT_ITEM_STORAGE_ROWS);
        }

        private static @NotNull ItemStorageSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new ItemStorageSettings(
                section.getInt("unlock_level", DEFAULT_ITEM_STORAGE_UNLOCK_LEVEL),
                section.getInt("rows", DEFAULT_ITEM_STORAGE_ROWS)
            );
        }
    }

    /**
     * Immutable bank remote-access settings parsed from {@code bank.yml}.
     *
     * @param unlockLevel Bank level that unlocks {@code /rt bank}
     * @param requireOwnClaim whether level 3 access requires standing in one own claim
     * @param crossClusterCacheDepositUnlockLevel Bank level that unlocks remote cache deposit
     */
    public record RemoteAccessSettings(
        int unlockLevel,
        boolean requireOwnClaim,
        int crossClusterCacheDepositUnlockLevel
    ) {

        /**
         * Creates one immutable bank remote-access settings snapshot.
         *
         * @param unlockLevel Bank level that unlocks {@code /rt bank}
         * @param requireOwnClaim whether level 3 access requires standing in one own claim
         * @param crossClusterCacheDepositUnlockLevel Bank level that unlocks remote cache deposit
         */
        public RemoteAccessSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_REMOTE_ACCESS_UNLOCK_LEVEL);
            crossClusterCacheDepositUnlockLevel = Math.max(
                unlockLevel,
                sanitizePositiveInt(
                    crossClusterCacheDepositUnlockLevel,
                    DEFAULT_REMOTE_CACHE_DEPOSIT_UNLOCK_LEVEL
                )
            );
        }

        /**
         * Returns whether remote bank access is unlocked at the supplied bank level.
         *
         * @param bankLevel Bank chunk level
         * @return {@code true} when {@code /rt bank} is unlocked
         */
        public boolean isUnlocked(final int bankLevel) {
            return bankLevel >= this.unlockLevel;
        }

        /**
         * Returns whether cross-cluster cache deposit is unlocked at the supplied bank level.
         *
         * @param bankLevel Bank chunk level
         * @return {@code true} when remote cache deposit is unlocked
         */
        public boolean supportsCrossClusterCacheDeposit(final int bankLevel) {
            return bankLevel >= this.crossClusterCacheDepositUnlockLevel;
        }

        private static @NotNull RemoteAccessSettings createDefault() {
            return new RemoteAccessSettings(
                DEFAULT_REMOTE_ACCESS_UNLOCK_LEVEL,
                DEFAULT_REMOTE_ACCESS_REQUIRE_OWN_CLAIM,
                DEFAULT_REMOTE_CACHE_DEPOSIT_UNLOCK_LEVEL
            );
        }

        private static @NotNull RemoteAccessSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new RemoteAccessSettings(
                section.getInt("unlock_level", DEFAULT_REMOTE_ACCESS_UNLOCK_LEVEL),
                section.getBoolean("require_own_claim", DEFAULT_REMOTE_ACCESS_REQUIRE_OWN_CLAIM),
                section.getInt(
                    "cross_cluster_cache_deposit_unlock_level",
                    DEFAULT_REMOTE_CACHE_DEPOSIT_UNLOCK_LEVEL
                )
            );
        }
    }

    /**
     * Immutable bank cache settings parsed from {@code bank.yml}.
     *
     * @param unlockLevel Bank level that unlocks the cache chest
     * @param rows configured cache row count
     * @param placementRadiusBlocks maximum radius from the host Bank marker block
     * @param itemMaterial configured bound cache item material
     */
    public record CacheSettings(
        int unlockLevel,
        int rows,
        int placementRadiusBlocks,
        @NotNull Material itemMaterial
    ) {

        /**
         * Creates one immutable bank cache settings snapshot.
         *
         * @param unlockLevel Bank level that unlocks the cache chest
         * @param rows configured cache row count
         * @param placementRadiusBlocks maximum radius from the host Bank marker block
         * @param itemMaterial configured bound cache item material
         */
        public CacheSettings {
            unlockLevel = sanitizePositiveInt(unlockLevel, DEFAULT_CACHE_UNLOCK_LEVEL);
            rows = sanitizeInventoryRows(rows, DEFAULT_CACHE_ROWS);
            placementRadiusBlocks = sanitizePositiveInt(
                placementRadiusBlocks,
                DEFAULT_CACHE_PLACEMENT_RADIUS_BLOCKS
            );
            itemMaterial = itemMaterial == null || !isSupportedCacheItemMaterial(itemMaterial)
                ? DEFAULT_CACHE_ITEM_MATERIAL
                : itemMaterial;
        }

        /**
         * Returns whether the cache chest is unlocked at the supplied bank level.
         *
         * @param bankLevel Bank chunk level
         * @return {@code true} when the cache chest is unlocked
         */
        public boolean isUnlocked(final int bankLevel) {
            return bankLevel >= this.unlockLevel;
        }

        /**
         * Returns the configured cache inventory size.
         *
         * @return cache slot count
         */
        public int inventorySize() {
            return this.rows * 9;
        }

        private static @NotNull CacheSettings createDefault() {
            return new CacheSettings(
                DEFAULT_CACHE_UNLOCK_LEVEL,
                DEFAULT_CACHE_ROWS,
                DEFAULT_CACHE_PLACEMENT_RADIUS_BLOCKS,
                DEFAULT_CACHE_ITEM_MATERIAL
            );
        }

        private static @NotNull CacheSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new CacheSettings(
                section.getInt("unlock_level", DEFAULT_CACHE_UNLOCK_LEVEL),
                section.getInt("rows", DEFAULT_CACHE_ROWS),
                section.getInt("placement_radius_blocks", DEFAULT_CACHE_PLACEMENT_RADIUS_BLOCKS),
                resolveConfiguredCacheItemMaterial(section.getString("item_material"))
            );
        }

        private static @NotNull Material resolveConfiguredCacheItemMaterial(final @Nullable String rawMaterialName) {
            if (rawMaterialName == null || rawMaterialName.isBlank()) {
                return DEFAULT_CACHE_ITEM_MATERIAL;
            }

            final Material material = Material.matchMaterial(rawMaterialName.trim().toUpperCase(Locale.ROOT));
            if (material == null) {
                return DEFAULT_CACHE_ITEM_MATERIAL;
            }
            if (isSupportedCacheItemMaterial(material)) {
                return material;
            }
            return DEFAULT_CACHE_ITEM_MATERIAL;
        }

        private static boolean isSupportedCacheItemMaterial(final @NotNull Material material) {
            return material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.BARREL;
        }
    }

    /**
     * Immutable bank locking settings parsed from {@code bank.yml}.
     *
     * @param singleViewer whether bank access is limited to one viewer per town
     */
    public record LockingSettings(boolean singleViewer) {

        private static @NotNull LockingSettings createDefault() {
            return new LockingSettings(DEFAULT_SINGLE_VIEWER_LOCKING);
        }

        private static @NotNull LockingSettings fromConfiguration(final @Nullable ConfigurationSection section) {
            if (section == null) {
                return createDefault();
            }
            return new LockingSettings(section.getBoolean("single_viewer", DEFAULT_SINGLE_VIEWER_LOCKING));
        }
    }

    private static int sanitizePositiveInt(final int value, final int fallback) {
        return value > 0 ? value : fallback;
    }

    private static int sanitizeInventoryRows(final int rows, final int fallback) {
        return Math.max(1, Math.min(6, rows > 0 ? rows : fallback));
    }

    private static @NotNull List<String> copyCurrencyIds(final @Nullable Iterable<String> currencies) {
        final LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (currencies != null) {
            for (final String currencyId : currencies) {
                if (currencyId == null || currencyId.isBlank()) {
                    continue;
                }
                normalized.add(currencyId.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized.isEmpty() ? List.copyOf(DEFAULT_CURRENCY_IDS) : List.copyOf(normalized);
    }
}
