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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.service.LevelProgressSnapshot;
import com.raindropcentral.rdt.service.LevelScope;
import com.raindropcentral.rdt.service.LevelUpResult;
import com.raindropcentral.rdt.service.TownFarmService;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared initial-data and progression-resolution helpers for level views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class TownLevelViewSupport {

    static final String SCOPE_KEY = "level_scope";
    static final String PREVIEW_LEVEL_KEY = "preview_level";
    static final String ENTRY_KEY = "level_entry_key";
    static final String CONTRIBUTION_STATUS_KEY = "level_contribution_status";
    static final String CONTRIBUTION_AMOUNT_KEY = "level_contribution_amount";
    static final String CONTRIBUTION_COMPLETED_KEY = "level_requirement_completed";
    static final String LEVEL_READY_KEY = "level_ready";
    static final String LEVEL_UP_STATUS_KEY = "level_up_status";
    static final String LEVEL_UP_NEW_LEVEL_KEY = "level_up_new_level";
    static final String LEVEL_UP_PREVIOUS_LEVEL_KEY = "level_up_previous_level";

    private TownLevelViewSupport() {
    }

    static @Nullable Map<String, Object> copyInitialData(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> rawMap)) {
            return null;
        }

        final Map<String, Object> copied = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    static @NotNull Map<String, Object> mergeInitialData(
        final @NotNull Context context,
        final @NotNull Map<String, Object> extraData
    ) {
        final Map<String, Object> copiedData = copyInitialData(context);
        final Map<String, Object> mergedData = copiedData == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(copiedData);
        mergedData.putAll(extraData);
        return mergedData;
    }

    static @NotNull Map<String, Object> stripTransientData(final @NotNull Map<String, Object> data) {
        final Map<String, Object> sanitizedData = new LinkedHashMap<>(data);
        sanitizedData.remove(ENTRY_KEY);
        sanitizedData.remove(CONTRIBUTION_STATUS_KEY);
        sanitizedData.remove(CONTRIBUTION_AMOUNT_KEY);
        sanitizedData.remove(CONTRIBUTION_COMPLETED_KEY);
        sanitizedData.remove(LEVEL_READY_KEY);
        sanitizedData.remove(LEVEL_UP_STATUS_KEY);
        sanitizedData.remove(LEVEL_UP_NEW_LEVEL_KEY);
        sanitizedData.remove(LEVEL_UP_PREVIOUS_LEVEL_KEY);
        return sanitizedData;
    }

    static @NotNull Map<String, Object> createNexusNavigationData(
        final @NotNull Context context,
        final @NotNull RTown town
    ) {
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", town.getTownUUID(),
                SCOPE_KEY, LevelScope.NEXUS
            )
        );
    }

    static @NotNull Map<String, Object> createNationNavigationData(
        final @NotNull Context context,
        final @NotNull RTown town
    ) {
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", town.getTownUUID(),
                SCOPE_KEY, LevelScope.NATION
            )
        );
    }

    static @NotNull Map<String, Object> createSecurityNavigationData(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        return createChunkNavigationData(context, townChunk);
    }

    static @NotNull Map<String, Object> createChunkNavigationData(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        final LevelScope scope = LevelScope.fromChunkType(townChunk.getChunkType());
        if (scope == null) {
            throw new IllegalArgumentException("Chunk type " + townChunk.getChunkType() + " has no progression path");
        }
        return mergeInitialData(
            context,
            Map.of(
                "plugin", plugin(context),
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ(),
                SCOPE_KEY, scope
            )
        );
    }

    static @Nullable RDT plugin(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        return data != null && data.get("plugin") instanceof RDT plugin ? plugin : null;
    }

    static @NotNull LevelScope scope(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        if (data == null) {
            return LevelScope.NEXUS;
        }
        final Object rawScope = data.get(SCOPE_KEY);
        if (rawScope instanceof LevelScope scope) {
            return scope;
        }
        if (rawScope instanceof String serializedScope) {
            try {
                return LevelScope.valueOf(serializedScope.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (final IllegalArgumentException ignored) {
                return LevelScope.NEXUS;
            }
        }
        return LevelScope.NEXUS;
    }

    static @Nullable RTown resolveTown(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }
        final Map<String, Object> data = copyInitialData(context);
        if (data == null || !(data.get("town_uuid") instanceof UUID townUuid)) {
            return null;
        }
        return plugin.getTownRuntimeService().getTown(townUuid);
    }

    static @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }
        final Map<String, Object> data = copyInitialData(context);
        if (data == null
            || !(data.get("town_uuid") instanceof UUID townUuid)
            || !(data.get("world_name") instanceof String worldName)
            || !(data.get("chunk_x") instanceof Number chunkX)
            || !(data.get("chunk_z") instanceof Number chunkZ)) {
            return null;
        }
        return plugin.getTownRuntimeService().getTownChunk(townUuid, worldName, chunkX.intValue(), chunkZ.intValue());
    }

    static @Nullable Integer previewLevel(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        if (data == null) {
            return null;
        }
        final Object rawPreviewLevel = data.get(PREVIEW_LEVEL_KEY);
        if (rawPreviewLevel instanceof Number number) {
            return number.intValue();
        }
        if (rawPreviewLevel instanceof String serializedPreviewLevel) {
            try {
                return Integer.parseInt(serializedPreviewLevel.trim());
            } catch (final NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static @Nullable LevelProgressSnapshot resolveSnapshot(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        final RTown town = resolveTown(context);
        if (plugin == null || plugin.getTownRuntimeService() == null || town == null) {
            return null;
        }

        return switch (scope(context)) {
            case NEXUS -> previewLevel(context) == null
                ? plugin.getTownRuntimeService().getNexusLevelProgress(context.getPlayer(), town)
                : plugin.getTownRuntimeService().getNexusLevelProgress(context.getPlayer(), town, previewLevel(context));
            case NATION_FORMATION -> null;
            case NATION -> previewLevel(context) == null
                ? plugin.getTownRuntimeService().getNationLevelProgress(context.getPlayer(), town)
                : plugin.getTownRuntimeService().getNationLevelProgress(context.getPlayer(), town, previewLevel(context));
            case SECURITY, BANK, FARM, FOB, OUTPOST, MEDIC, ARMORY -> {
                final RTownChunk townChunk = resolveChunk(context);
                if (townChunk == null) {
                    yield null;
                }
                if (LevelScope.fromChunkType(townChunk.getChunkType()) == null) {
                    yield null;
                }
                yield previewLevel(context) == null
                    ? plugin.getTownRuntimeService().getChunkLevelProgress(context.getPlayer(), townChunk)
                    : plugin.getTownRuntimeService().getChunkLevelProgress(context.getPlayer(), townChunk, previewLevel(context));
            }
        };
    }

    static void sendFarmUnlockMessages(
        final @Nullable RDT plugin,
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        if (plugin == null || scope != LevelScope.FARM || result.status() != com.raindropcentral.rdt.service.LevelUpStatus.SUCCESS) {
            return;
        }

        final var growthSettings = plugin.getFarmConfig().getGrowth();
        final var seedBoxSettings = plugin.getFarmConfig().getSeedBox();
        final var replantSettings = plugin.getFarmConfig().getReplant();
        final var doubleHarvestSettings = plugin.getFarmConfig().getDoubleHarvest();

        if (result.previousLevel() < growthSettings.tierOneUnlockLevel()
            && result.newLevel() >= growthSettings.tierOneUnlockLevel()) {
            new I18n.Builder("town_farm_shared.messages.growth_tier_1_unlocked", player)
                .includePrefix()
                .withPlaceholder(
                    "growth_speed_multiplier",
                    TownFarmService.formatGrowthSpeedMultiplier(growthSettings.tierOneGrowthSpeedMultiplier())
                )
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < seedBoxSettings.unlockLevel() && result.newLevel() >= seedBoxSettings.unlockLevel()) {
            new I18n.Builder("town_farm_shared.messages.seed_box_unlocked", player)
                .includePrefix()
                .withPlaceholder("radius_blocks", seedBoxSettings.placementRadiusBlocks())
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < replantSettings.unlockLevel() && result.newLevel() >= replantSettings.unlockLevel()) {
            new I18n.Builder("town_farm_shared.messages.replant_unlocked", player)
                .includePrefix()
                .withPlaceholder("priority", formatReplantPriority(replantSettings.defaultSourcePriority().name()))
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < growthSettings.tierTwoUnlockLevel()
            && result.newLevel() >= growthSettings.tierTwoUnlockLevel()) {
            new I18n.Builder("town_farm_shared.messages.growth_tier_2_unlocked", player)
                .includePrefix()
                .withPlaceholder(
                    "growth_speed_multiplier",
                    TownFarmService.formatGrowthSpeedMultiplier(growthSettings.tierTwoGrowthSpeedMultiplier())
                )
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < doubleHarvestSettings.unlockLevel()
            && result.newLevel() >= doubleHarvestSettings.unlockLevel()) {
            new I18n.Builder("town_farm_shared.messages.double_harvest_unlocked", player)
                .includePrefix()
                .withPlaceholder("multiplier", doubleHarvestSettings.multiplier())
                .build()
                .sendMessage();
        }
    }

    static void sendMedicUnlockMessages(
        final @Nullable RDT plugin,
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        if (plugin == null || scope != LevelScope.MEDIC || result.status() != com.raindropcentral.rdt.service.LevelUpStatus.SUCCESS) {
            return;
        }

        final var foodRegenSettings = plugin.getMedicConfig().getFoodRegen();
        final var healthRegenSettings = plugin.getMedicConfig().getHealthRegen();
        final var cleanseSettings = plugin.getMedicConfig().getCleanse();
        final var fortifiedRecoverySettings = plugin.getMedicConfig().getFortifiedRecovery();
        final var emergencyRefillSettings = plugin.getMedicConfig().getEmergencyRefill();

        if (result.previousLevel() < foodRegenSettings.unlockLevel() && result.newLevel() >= foodRegenSettings.unlockLevel()) {
            new I18n.Builder("town_medic_shared.messages.food_regen_unlocked", player)
                .includePrefix()
                .withPlaceholder("food_points_per_pulse", foodRegenSettings.foodPointsPerPulse())
                .withPlaceholder("interval_seconds", foodRegenSettings.intervalTicks() / 20.0D)
                .withPlaceholder("saturation_per_pulse", foodRegenSettings.saturationPerPulse())
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < healthRegenSettings.unlockLevel()
            && result.newLevel() >= healthRegenSettings.unlockLevel()) {
            new I18n.Builder("town_medic_shared.messages.health_regen_unlocked", player)
                .includePrefix()
                .withPlaceholder("health_points_per_pulse", healthRegenSettings.healthPointsPerPulse())
                .withPlaceholder("interval_seconds", healthRegenSettings.intervalTicks() / 20.0D)
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < cleanseSettings.unlockLevel() && result.newLevel() >= cleanseSettings.unlockLevel()) {
            new I18n.Builder("town_medic_shared.messages.cleanse_unlocked", player)
                .includePrefix()
                .withPlaceholder("effect_count", cleanseSettings.harmfulEffects().size())
                .withPlaceholder("interval_seconds", cleanseSettings.intervalTicks() / 20.0D)
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < fortifiedRecoverySettings.unlockLevel()
            && result.newLevel() >= fortifiedRecoverySettings.unlockLevel()) {
            new I18n.Builder("town_medic_shared.messages.fortified_recovery_unlocked", player)
                .includePrefix()
                .withPlaceholder("duration_seconds", fortifiedRecoverySettings.durationSeconds())
                .withPlaceholder("target_max_health", fortifiedRecoverySettings.targetMaxHealth())
                .withPlaceholder("target_food_level", fortifiedRecoverySettings.targetFoodLevel())
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < emergencyRefillSettings.unlockLevel()
            && result.newLevel() >= emergencyRefillSettings.unlockLevel()) {
            new I18n.Builder("town_medic_shared.messages.emergency_refill_unlocked", player)
                .includePrefix()
                .withPlaceholder("cooldown_seconds", emergencyRefillSettings.cooldownSeconds())
                .withPlaceholder("target_health_mode", formatTargetHealthMode(emergencyRefillSettings.targetHealthMode().name()))
                .build()
                .sendMessage();
        }
    }

    static void sendBankUnlockMessages(
        final @Nullable RDT plugin,
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        if (plugin == null || scope != LevelScope.BANK || result.status() != com.raindropcentral.rdt.service.LevelUpStatus.SUCCESS) {
            return;
        }

        final var itemStorageSettings = plugin.getBankConfig().getItemStorage();
        final var remoteAccessSettings = plugin.getBankConfig().getRemoteAccess();
        final var cacheSettings = plugin.getBankConfig().getCache();
        if (result.previousLevel() < itemStorageSettings.unlockLevel() && result.newLevel() >= itemStorageSettings.unlockLevel()) {
            new I18n.Builder("town_bank_shared.messages.item_storage_unlocked", player)
                .includePrefix()
                .withPlaceholder("storage_rows", itemStorageSettings.rows())
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < remoteAccessSettings.unlockLevel() && result.newLevel() >= remoteAccessSettings.unlockLevel()) {
            new I18n.Builder("town_bank_shared.messages.remote_command_unlocked", player)
                .includePrefix()
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < cacheSettings.unlockLevel() && result.newLevel() >= cacheSettings.unlockLevel()) {
            new I18n.Builder("town_bank_shared.messages.cache_unlocked", player)
                .includePrefix()
                .withPlaceholder("radius_blocks", cacheSettings.placementRadiusBlocks())
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < remoteAccessSettings.crossClusterCacheDepositUnlockLevel()
            && result.newLevel() >= remoteAccessSettings.crossClusterCacheDepositUnlockLevel()) {
            new I18n.Builder("town_bank_shared.messages.remote_cache_unlocked", player)
                .includePrefix()
                .build()
                .sendMessage();
        }
    }

    static void sendArmoryUnlockMessages(
        final @Nullable RDT plugin,
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        if (plugin == null || scope != LevelScope.ARMORY || result.status() != com.raindropcentral.rdt.service.LevelUpStatus.SUCCESS) {
            return;
        }

        final var freeRepairSettings = plugin.getArmoryConfig().getFreeRepair();
        final var salvageBlockSettings = plugin.getArmoryConfig().getSalvageBlock();
        final var repairBlockSettings = plugin.getArmoryConfig().getRepairBlock();
        final var doubleSmeltSettings = plugin.getArmoryConfig().getDoubleSmelt();

        if (result.previousLevel() < freeRepairSettings.unlockLevel() && result.newLevel() >= freeRepairSettings.unlockLevel()) {
            new I18n.Builder("town_armory_shared.messages.free_repair_unlocked", player)
                .includePrefix()
                .withPlaceholder("cooldown", TownOverviewView.formatDurationMillis(freeRepairSettings.cooldownSeconds() * 1000L))
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < salvageBlockSettings.unlockLevel()
            && result.newLevel() >= salvageBlockSettings.unlockLevel()) {
            new I18n.Builder("town_armory_shared.messages.salvage_block_unlocked", player)
                .includePrefix()
                .withPlaceholder("radius_blocks", salvageBlockSettings.placementRadiusBlocks())
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < repairBlockSettings.unlockLevel()
            && result.newLevel() >= repairBlockSettings.unlockLevel()) {
            new I18n.Builder("town_armory_shared.messages.repair_block_unlocked", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "radius_blocks", repairBlockSettings.placementRadiusBlocks(),
                    "iron_material_cost", repairBlockSettings.iron().materialCost(),
                    "gold_material_cost", repairBlockSettings.gold().materialCost(),
                    "diamond_material_cost", repairBlockSettings.diamond().materialCost(),
                    "netherite_material_cost", repairBlockSettings.netherite().materialCost()
                ))
                .build()
                .sendMessage();
        }
        if (result.previousLevel() < doubleSmeltSettings.unlockLevel()
            && result.newLevel() >= doubleSmeltSettings.unlockLevel()) {
            new I18n.Builder("town_armory_shared.messages.double_smelt_unlocked", player)
                .includePrefix()
                .withPlaceholders(Map.of(
                    "burn_faster_multiplier", formatDecimal(doubleSmeltSettings.burnFasterMultiplier()),
                    "extra_fuel_per_smelt_units", doubleSmeltSettings.extraFuelPerSmeltUnits()
                ))
                .build()
                .sendMessage();
        }
    }

    private static @NotNull String formatReplantPriority(final @NotNull String rawPriority) {
        return switch (rawPriority) {
            case "INVENTORY_FIRST" -> "Inventory First";
            case "SEED_BOX_FIRST" -> "Seed Box First";
            default -> rawPriority;
        };
    }

    private static @NotNull String formatTargetHealthMode(final @NotNull String rawTargetHealthMode) {
        return switch (rawTargetHealthMode) {
            case "CURRENT_MAX" -> "Current Max";
            case "VANILLA_MAX" -> "Vanilla Max";
            default -> rawTargetHealthMode;
        };
    }

    private static @NotNull String formatDecimal(final double value) {
        final String formatted = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (formatted.endsWith("00")) {
            return formatted.substring(0, formatted.length() - 3);
        }
        if (formatted.endsWith("0")) {
            return formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }
}
