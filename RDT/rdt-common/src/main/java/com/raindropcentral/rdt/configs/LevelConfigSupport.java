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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared YAML parsing and default-definition support for level-config snapshots.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class LevelConfigSupport {

    private static final int DEFAULT_NEXUS_LEVEL_COUNT = 10;
    private static final double DEFAULT_NEXUS_LEVEL_BASE_REQUIREMENT = 2500.0D;
    private static final double DEFAULT_NEXUS_LEVEL_REQUIREMENT_GROWTH = 1.2D;
    private static final double DEFAULT_NEXUS_LEVEL_REWARD_MULTIPLIER = 0.2D;
    private static final double[] DEFAULT_CHUNK_LEVEL_REQUIREMENTS = {1_000.0D, 1_750.0D, 2_750.0D, 4_000.0D};
    private static final double[] DEFAULT_CHUNK_LEVEL_REWARDS = {150.0D, 250.0D, 400.0D, 600.0D};

    private LevelConfigSupport() {
    }

    static @NotNull Map<Integer, LevelDefinition> parseLevels(
        final @Nullable ConfigurationSection levelsSection,
        final @NotNull Map<Integer, LevelDefinition> fallback
    ) {
        if (levelsSection == null) {
            return copyLevels(fallback);
        }

        final Map<Integer, LevelDefinition> levels = new LinkedHashMap<>();
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
            levels.put(parsedLevel, new LevelDefinition(parsedLevel, requirements, rewards));
        }

        return levels.isEmpty() ? copyLevels(fallback) : Map.copyOf(levels);
    }

    static @NotNull Map<Integer, NexusCombatStats> parseNexusCombatStats(
        final @Nullable ConfigurationSection levelsSection,
        final @NotNull Collection<Integer> resolvedLevels
    ) {
        final Map<Integer, NexusCombatStats> combatStats = new LinkedHashMap<>();
        for (final Integer level : resolvedLevels) {
            if (level == null) {
                continue;
            }
            final ConfigurationSection levelSection = levelsSection == null
                ? null
                : levelsSection.getConfigurationSection(String.valueOf(level));
            combatStats.put(level, parseNexusCombatStats(level, levelSection == null ? null : levelSection.getConfigurationSection("combat")));
        }
        return Map.copyOf(combatStats);
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultNexusLevels() {
        final Map<Integer, LevelDefinition> defaults = new LinkedHashMap<>();
        for (int level = 1; level <= DEFAULT_NEXUS_LEVEL_COUNT; level++) {
            if (level == 1) {
                defaults.put(
                    level,
                    new LevelDefinition(
                        level,
                        Map.of(
                            "town_charter",
                            createCurrencyRequirement(1_000.0D, "Town charter funding")
                        ),
                        Map.of(
                            "town_broadcast",
                            createCommandReward(
                                "rt broadcast {town_uuid} <gradient:#22c55e:#84cc16>{town_name}</gradient> "
                                    + "<yellow>has been founded and claimed its first Nexus!</yellow>"
                            )
                        )
                    )
                );
                continue;
            }

            final double requirementAmount = roundCurrency(
                DEFAULT_NEXUS_LEVEL_BASE_REQUIREMENT
                    * Math.pow(DEFAULT_NEXUS_LEVEL_REQUIREMENT_GROWTH, level - 2)
            );
            final double rewardAmount = roundCurrency(requirementAmount * DEFAULT_NEXUS_LEVEL_REWARD_MULTIPLIER);
            defaults.put(
                level,
                new LevelDefinition(
                    level,
                    Map.of(
                        "vault_upgrade",
                        createCurrencyRequirement(requirementAmount, "Vault funding contribution")
                    ),
                    Map.of(
                        "vault_bonus",
                        createCurrencyReward(rewardAmount, "Town vault bonus"),
                        "town_broadcast",
                        createCommandReward(
                            "rt broadcast {town_uuid} <gradient:#22c55e:#84cc16>{town_name}</gradient> "
                                + "<yellow>advanced its Nexus to level <white>{target_level}</white><yellow>!</yellow>"
                        )
                    )
                )
            );
        }
        return Map.copyOf(defaults);
    }

    static @NotNull Map<Integer, NexusCombatStats> createDefaultNexusCombatStats() {
        final Map<Integer, NexusCombatStats> defaults = new LinkedHashMap<>();
        for (int level = 1; level <= DEFAULT_NEXUS_LEVEL_COUNT; level++) {
            defaults.put(level, NexusCombatStats.createDefault(level));
        }
        return Map.copyOf(defaults);
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultSecurityLevels() {
        final Map<Integer, LevelDefinition> defaults = new LinkedHashMap<>();
        defaults.put(1, new LevelDefinition(1, Map.of(), Map.of()));
        defaults.put(
            2,
            new LevelDefinition(
                2,
                Map.of(
                    "vault_upgrade", createCurrencyRequirement(1_000.0D, "Security funding contribution"),
                    "reinforcement_materials", createItemRequirement("IRON_INGOT", 32, "Security reinforcement materials")
                ),
                Map.of(
                    "vault_bonus", createCurrencyReward(150.0D, "Security treasury rebate"),
                    "town_broadcast", createCommandReward(
                        "rt broadcast {town_uuid} <aqua>{town_name}</aqua> <yellow>upgraded Security chunk "
                            + "<white>{chunk_x}, {chunk_z}</white> to level <white>{target_level}</white><yellow>!</yellow>"
                    )
                )
            )
        );
        defaults.put(
            3,
            new LevelDefinition(
                3,
                Map.of(
                    "vault_upgrade", createCurrencyRequirement(1_750.0D, "Security funding contribution"),
                    "signal_materials", createItemRequirement("REDSTONE", 48, "Security signal materials")
                ),
                Map.of(
                    "vault_bonus", createCurrencyReward(250.0D, "Security treasury rebate"),
                    "town_broadcast", createCommandReward(
                        "rt broadcast {town_uuid} <aqua>{town_name}</aqua> <yellow>upgraded Security chunk "
                            + "<white>{chunk_x}, {chunk_z}</white> to level <white>{target_level}</white><yellow>!</yellow>"
                    )
                )
            )
        );
        defaults.put(
            4,
            new LevelDefinition(
                4,
                Map.of(
                    "vault_upgrade", createCurrencyRequirement(2_750.0D, "Security funding contribution"),
                    "barrier_materials", createItemRequirement("OBSIDIAN", 24, "Security barrier materials")
                ),
                Map.of(
                    "vault_bonus", createCurrencyReward(400.0D, "Security treasury rebate"),
                    "town_broadcast", createCommandReward(
                        "rt broadcast {town_uuid} <aqua>{town_name}</aqua> <yellow>upgraded Security chunk "
                            + "<white>{chunk_x}, {chunk_z}</white> to level <white>{target_level}</white><yellow>!</yellow>"
                    )
                )
            )
        );
        defaults.put(
            5,
            new LevelDefinition(
                5,
                Map.of(
                    "vault_upgrade", createCurrencyRequirement(4_000.0D, "Security funding contribution"),
                    "precision_materials", createItemRequirement("DIAMOND", 8, "Security precision materials")
                ),
                Map.of(
                    "vault_bonus", createCurrencyReward(600.0D, "Security treasury rebate"),
                    "town_broadcast", createCommandReward(
                        "rt broadcast {town_uuid} <aqua>{town_name}</aqua> <yellow>upgraded Security chunk "
                            + "<white>{chunk_x}, {chunk_z}</white> to level <white>{target_level}</white><yellow>!</yellow>"
                    )
                )
            )
        );
        return Map.copyOf(defaults);
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultBankLevels() {
        return createDefaultChunkLevels(
            "Bank",
            new ChunkLevelItemDefinition[] {
                new ChunkLevelItemDefinition("reserve_materials", "GOLD_INGOT", 32, "Bank reserve materials"),
                new ChunkLevelItemDefinition("trade_materials", "EMERALD", 24, "Bank trade materials"),
                new ChunkLevelItemDefinition("vault_materials", "GOLD_BLOCK", 16, "Bank vault materials"),
                new ChunkLevelItemDefinition("precision_materials", "DIAMOND", 8, "Bank precision materials")
            }
        );
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultFarmLevels() {
        return createDefaultChunkLevels(
            "Farm",
            new ChunkLevelItemDefinition[] {
                new ChunkLevelItemDefinition("harvest_materials", "WHEAT", 64, "Farm harvest materials"),
                new ChunkLevelItemDefinition("carrot_supplies", "CARROT", 64, "Farm carrot supplies"),
                new ChunkLevelItemDefinition("potato_supplies", "POTATO", 64, "Farm potato supplies"),
                new ChunkLevelItemDefinition("pumpkin_supplies", "PUMPKIN", 32, "Farm pumpkin supplies")
            }
        );
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultOutpostLevels() {
        final Map<Integer, LevelDefinition> defaults = new LinkedHashMap<>(createDefaultChunkLevels(
            "Outpost",
            new ChunkLevelItemDefinition[] {
                new ChunkLevelItemDefinition("navigation_tools", "COMPASS", 8, "Outpost navigation tools"),
                new ChunkLevelItemDefinition("travel_supplies", "ENDER_PEARL", 16, "Outpost travel supplies"),
                new ChunkLevelItemDefinition("fortification_materials", "OBSIDIAN", 24, "Outpost fortification materials"),
                new ChunkLevelItemDefinition("relay_materials", "ENDER_EYE", 8, "Outpost relay materials")
            }
        ));
        addDefaultOutpostTownShopReward(defaults, 3);
        addDefaultOutpostTownShopReward(defaults, 4);
        addDefaultOutpostTownShopReward(defaults, 5);
        return Map.copyOf(defaults);
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultMedicLevels() {
        return createDefaultChunkLevels(
            "Medic",
            new ChunkLevelItemDefinition[] {
                new ChunkLevelItemDefinition(
                    "clinic_supplies",
                    "GLISTERING_MELON_SLICE",
                    24,
                    "Medic clinic supplies"
                ),
                new ChunkLevelItemDefinition("triage_rations", "GOLDEN_CARROT", 32, "Medic triage rations"),
                new ChunkLevelItemDefinition("restoratives", "GHAST_TEAR", 8, "Medic restorative supplies"),
                new ChunkLevelItemDefinition(
                    "revival_supplies",
                    "TOTEM_OF_UNDYING",
                    1,
                    "Medic revival supplies"
                )
            },
            new ChunkLevelRewardItemDefinition[] {
                new ChunkLevelRewardItemDefinition("field_supplies", "HONEY_BOTTLE", 8, "Medic field supplies"),
                new ChunkLevelRewardItemDefinition("combat_rations", "GOLDEN_CARROT", 16, "Medic combat rations"),
                new ChunkLevelRewardItemDefinition("recovery_cache", "GOLDEN_APPLE", 4, "Medic recovery cache"),
                new ChunkLevelRewardItemDefinition("emergency_cache", "GOLDEN_APPLE", 8, "Medic emergency cache")
            }
        );
    }

    static @NotNull Map<Integer, LevelDefinition> createDefaultArmoryLevels() {
        return createDefaultChunkLevels(
            "Armory",
            new ChunkLevelItemDefinition[] {
                new ChunkLevelItemDefinition("forging_materials", "IRON_INGOT", 32, "Armory forging materials"),
                new ChunkLevelItemDefinition("defense_kits", "SHIELD", 6, "Armory defense kits"),
                new ChunkLevelItemDefinition("ranged_hardware", "CROSSBOW", 4, "Armory ranged hardware"),
                new ChunkLevelItemDefinition("elite_forging", "NETHERITE_SCRAP", 4, "Armory elite forging")
            },
            new ChunkLevelRewardItemDefinition[] {
                new ChunkLevelRewardItemDefinition("munition_cache", "ARROW", 64, "Armory munition cache"),
                new ChunkLevelRewardItemDefinition(
                    "precision_munitions",
                    "SPECTRAL_ARROW",
                    24,
                    "Armory precision munitions"
                ),
                new ChunkLevelRewardItemDefinition("spare_materials", "IRON_INGOT", 24, "Armory spare materials"),
                new ChunkLevelRewardItemDefinition("elite_cache", "DIAMOND", 8, "Armory elite cache")
            }
        );
    }

    static @NotNull Map<String, Map<String, Object>> parseDefinitionEntries(
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
        return Map.copyOf(definitions);
    }

    static @NotNull Map<String, Object> convertSection(final @NotNull ConfigurationSection section) {
        final Map<String, Object> converted = new LinkedHashMap<>();
        for (final String key : section.getKeys(false)) {
            converted.put(normalizeNestedKey(key), normalizeValue(section.get(key)));
        }
        return converted;
    }

    static @Nullable Object normalizeValue(final @Nullable Object rawValue) {
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

    static @NotNull Map<String, Map<String, Object>> deepCopyDefinitionMap(
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
        return Map.copyOf(copy);
    }

    static @Nullable Object deepCopyValue(final @Nullable Object value) {
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

    static @Nullable Integer parsePositiveLevel(final @Nullable String rawLevel) {
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

    static @NotNull String normalizeDefinitionKey(final @Nullable String rawKey) {
        if (rawKey == null) {
            return "";
        }
        return rawKey.trim().toLowerCase(Locale.ROOT);
    }

    static @NotNull String normalizeNestedKey(final @Nullable String rawKey) {
        return rawKey == null ? "" : rawKey.trim();
    }

    static @NotNull Map<Integer, LevelDefinition> copyLevels(final @NotNull Map<Integer, LevelDefinition> source) {
        final Map<Integer, LevelDefinition> copy = new LinkedHashMap<>();
        for (final Map.Entry<Integer, LevelDefinition> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copy.put(entry.getKey(), new LevelDefinition(
                entry.getValue().level(),
                entry.getValue().getRequirements(),
                entry.getValue().getRewards()
            ));
        }
        return Map.copyOf(copy);
    }

    static @NotNull Map<Integer, NexusCombatStats> copyNexusCombatStats(final @NotNull Map<Integer, NexusCombatStats> source) {
        final Map<Integer, NexusCombatStats> copy = new LinkedHashMap<>();
        for (final Map.Entry<Integer, NexusCombatStats> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            copy.put(entry.getKey(), new NexusCombatStats(
                entry.getValue().level(),
                entry.getValue().maxHealth(),
                entry.getValue().defense()
            ));
        }
        return Map.copyOf(copy);
    }

    static @NotNull Material resolveConfiguredBlockMaterial(
        final @Nullable String rawMaterialName,
        final @NotNull Material fallback
    ) {
        if (rawMaterialName == null || rawMaterialName.isBlank()) {
            return fallback;
        }
        final Material material = Material.matchMaterial(rawMaterialName.trim().toUpperCase(Locale.ROOT));
        if (material == null || !isConfiguredBlockMaterial(material)) {
            return fallback;
        }
        return switch (material) {
            case CHEST, TRAPPED_CHEST, HOPPER -> fallback;
            default -> material;
        };
    }

    static boolean isConfiguredBlockMaterial(final @NotNull Material material) {
        try {
            return material.isBlock();
        } catch (final ExceptionInInitializerError | NoClassDefFoundError | IllegalStateException exception) {
            return switch (material) {
                case AIR, CAVE_AIR, VOID_AIR, GOLD_INGOT, DIAMOND, ARROW, HONEY_BOTTLE, TOTEM_OF_UNDYING,
                     SPECTRAL_ARROW, CROSSBOW, SHIELD, NETHERITE_SCRAP -> false;
                default -> material.name().endsWith("_BLOCK")
                    || material.name().endsWith("_PLANKS")
                    || material.name().endsWith("_GLASS")
                    || material.name().endsWith("_WOOL")
                    || material.name().endsWith("_TERRACOTTA")
                    || material.name().endsWith("_CONCRETE")
                    || material.name().endsWith("_STAIRS")
                    || material.name().endsWith("_SLAB")
                    || material.name().endsWith("_WALL")
                    || material.name().endsWith("_DOOR")
                    || material.name().endsWith("_TRAPDOOR")
                    || material.name().endsWith("_FENCE")
                    || material.name().endsWith("_FENCE_GATE")
                    || material.name().endsWith("_LOG")
                    || material.name().endsWith("_WOOD")
                    || material.name().endsWith("_LEAVES")
                    || material == Material.LODESTONE
                    || material == Material.SEA_LANTERN
                    || material == Material.CRYING_OBSIDIAN
                    || material == Material.GLOWSTONE
                    || material == Material.REINFORCED_DEEPSLATE
                    || material == Material.ORANGE_STAINED_GLASS;
            };
        }
    }

    private static @NotNull Map<String, Object> createCurrencyRequirement(
        final double amount,
        final @NotNull String description
    ) {
        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("type", "CURRENCY");
        definition.put("currency", "vault");
        definition.put("amount", roundCurrency(amount));
        definition.put("consumable", true);
        definition.put("description", description);
        return Map.copyOf(definition);
    }

    private static @NotNull NexusCombatStats parseNexusCombatStats(
        final int level,
        final @Nullable ConfigurationSection combatSection
    ) {
        final NexusCombatStats defaults = NexusCombatStats.createDefault(level);
        if (combatSection == null) {
            return defaults;
        }
        return new NexusCombatStats(
            level,
            combatSection.getDouble("max_health", defaults.maxHealth()),
            combatSection.getDouble("defense", defaults.defense())
        );
    }

    private static @NotNull Map<String, Object> createItemRequirement(
        final @NotNull String material,
        final int amount,
        final @NotNull String description
    ) {
        final Map<String, Object> itemDefinition = new LinkedHashMap<>();
        itemDefinition.put("type", material);
        itemDefinition.put("amount", Math.max(1, amount));

        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("type", "ITEM");
        definition.put("requiredItems", List.of(itemDefinition));
        definition.put("consumeOnComplete", true);
        definition.put("exactMatch", false);
        definition.put("description", description);
        return Map.copyOf(definition);
    }

    private static @NotNull Map<String, Object> createCurrencyReward(
        final double amount,
        final @NotNull String description
    ) {
        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("type", "CURRENCY");
        definition.put("currencyId", "vault");
        definition.put("amount", roundCurrency(amount));
        definition.put("description", description);
        return Map.copyOf(definition);
    }

    private static @NotNull Map<String, Object> createCommandReward(final @NotNull String command) {
        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("type", "COMMAND");
        definition.put("command", command);
        definition.put("executeAsPlayer", false);
        return Map.copyOf(definition);
    }

    private static @NotNull Map<String, Object> createItemReward(
        final @NotNull String material,
        final int amount,
        final @NotNull String description
    ) {
        final Map<String, Object> item = new LinkedHashMap<>();
        item.put("material", material);
        item.put("amount", Math.max(1, amount));

        final Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("type", "ITEM");
        definition.put("item", Map.copyOf(item));
        definition.put("description", description);
        return Map.copyOf(definition);
    }

    private static @NotNull Map<Integer, LevelDefinition> createDefaultChunkLevels(
        final @NotNull String scopeName,
        final @NotNull ChunkLevelItemDefinition[] itemDefinitions
    ) {
        return createDefaultChunkLevels(scopeName, itemDefinitions, new ChunkLevelRewardItemDefinition[0]);
    }

    private static @NotNull Map<Integer, LevelDefinition> createDefaultChunkLevels(
        final @NotNull String scopeName,
        final @NotNull ChunkLevelItemDefinition[] itemDefinitions,
        final @NotNull ChunkLevelRewardItemDefinition[] rewardDefinitions
    ) {
        final Map<Integer, LevelDefinition> defaults = new LinkedHashMap<>();
        defaults.put(1, new LevelDefinition(1, Map.of(), Map.of()));
        for (int index = 0; index < itemDefinitions.length; index++) {
            final int level = index + 2;
            final ChunkLevelItemDefinition itemDefinition = itemDefinitions[index];
            final Map<String, Map<String, Object>> rewards = new LinkedHashMap<>();
            rewards.put(
                "vault_bonus",
                createCurrencyReward(
                    DEFAULT_CHUNK_LEVEL_REWARDS[index],
                    scopeName + " treasury rebate"
                )
            );
            if (index < rewardDefinitions.length) {
                final ChunkLevelRewardItemDefinition rewardDefinition = rewardDefinitions[index];
                rewards.put(
                    rewardDefinition.key(),
                    createItemReward(
                        rewardDefinition.material(),
                        rewardDefinition.amount(),
                        rewardDefinition.description()
                    )
                );
            }
            rewards.put(
                "town_broadcast",
                createCommandReward(
                    "rt broadcast {town_uuid} <aqua>{town_name}</aqua> <yellow>upgraded "
                        + scopeName + " chunk <white>{chunk_x}, {chunk_z}</white> to level "
                        + "<white>{target_level}</white><yellow>!</yellow>"
                )
            );
            defaults.put(
                level,
                new LevelDefinition(
                    level,
                    Map.of(
                        "vault_upgrade",
                        createCurrencyRequirement(
                            DEFAULT_CHUNK_LEVEL_REQUIREMENTS[index],
                            scopeName + " funding contribution"
                        ),
                        itemDefinition.key(),
                        createItemRequirement(
                            itemDefinition.material(),
                            itemDefinition.amount(),
                            itemDefinition.description()
                        )
                    ),
                    Map.copyOf(rewards)
                )
            );
        }
        return Map.copyOf(defaults);
    }

    private static double roundCurrency(final double amount) {
        return Math.round(amount * 100.0D) / 100.0D;
    }

    private static void addDefaultOutpostTownShopReward(
        final @NotNull Map<Integer, LevelDefinition> levels,
        final int level
    ) {
        final LevelDefinition existingDefinition = levels.get(level);
        if (existingDefinition == null) {
            return;
        }

        final Map<String, Map<String, Object>> rewards = new LinkedHashMap<>(existingDefinition.getRewards());
        rewards.put(
            "town_shop_reward",
            createCommandReward(
                "rs internal reward-town-shop {player} RDT {town_uuid} {town_name_base64} {chunk_uuid} "
                    + "{world_name} {chunk_x} {chunk_z} {target_level}"
            )
        );
        levels.put(
            level,
            new LevelDefinition(
                existingDefinition.level(),
                existingDefinition.getRequirements(),
                Map.copyOf(rewards)
            )
        );
    }

    private record ChunkLevelItemDefinition(
        @NotNull String key,
        @NotNull String material,
        int amount,
        @NotNull String description
    ) {
    }

    private record ChunkLevelRewardItemDefinition(
        @NotNull String key,
        @NotNull String material,
        int amount,
        @NotNull String description
    ) {
    }
}
