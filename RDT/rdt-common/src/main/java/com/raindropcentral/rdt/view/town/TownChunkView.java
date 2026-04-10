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
import com.raindropcentral.rdt.service.TownArmoryService;
import com.raindropcentral.rdt.service.TownFarmService;
import com.raindropcentral.rdt.service.TownMedicService;
import com.raindropcentral.rdt.service.TownRuntimeService;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Chunk detail view for one claimed chunk marker.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownChunkView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<String> worldName = initialState("world_name");
    private final State<Integer> chunkX = initialState("chunk_x");
    private final State<Integer> chunkZ = initialState("chunk_z");

    /**
     * Creates the chunk view.
     */
    public TownChunkView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_chunk_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "  t p i  ",
            "   u f x ",
            "   g a o ",
            "         ",
            "r        "
        };
    }

    /**
     * Renders the chunk summary and actions.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTownChunk townChunk = this.resolveChunk(render);
        if (townChunk == null) {
            render.slot(22).renderWith(() -> this.createMissingChunkItem(player));
            return;
        }

        final boolean securityChunk = supportsFuelFeatures(townChunk);
        final boolean bankChunk = supportsBankFeatures(townChunk);
        final boolean farmChunk = supportsFarmFeatures(townChunk);
        final boolean medicChunk = supportsMedicFeatures(townChunk);
        final boolean armoryChunk = supportsArmoryFeatures(townChunk);
        final boolean outpostChunk = townChunk.getChunkType() == ChunkType.OUTPOST;
        render.layoutSlot('s', this.createSummaryItem(render, townChunk));
        render.layoutSlot('t', this.createTypeItem(render, townChunk))
            .onClick(clickContext -> this.handleTypeClick(clickContext, townChunk));
        if (supportsChunkScopedProtections(townChunk)) {
            render.layoutSlot('p', this.createProtectionsItem(render, townChunk))
                .onClick(clickContext -> this.handleProtectionsClick(clickContext, townChunk));
        }
        render.layoutSlot('i', this.createInfoItem(render, townChunk));
        if (supportsTownRelationships(townChunk)) {
            render.layoutSlot('u', this.createRelationshipsItem(render, townChunk))
                .onClick(clickContext -> this.handleRelationshipsClick(clickContext, townChunk));
            render.layoutSlot('f', this.createNationItem(render, townChunk))
                .onClick(clickContext -> this.handleNationClick(clickContext, townChunk));
        } else if (supportsChunkProgression(townChunk)) {
            render.layoutSlot('u', this.createUpgradeItem(render, townChunk))
                .onClick(clickContext -> this.handleUpgradeClick(clickContext, townChunk));
        }
        if (securityChunk) {
            render.layoutSlot('f', this.createFuelItem(render, townChunk));
        }
        if (bankChunk) {
            render.layoutSlot('f', this.createBankAccessItem(render, townChunk))
                .onClick(clickContext -> this.handleBankAccessClick(clickContext, townChunk));
            render.layoutSlot('g', this.createBankCacheItem(render, townChunk));
        }
        if (farmChunk) {
            render.layoutSlot('f', this.createSeedBoxItem(render, townChunk));
            render.layoutSlot('g', this.createFarmGrowthItem(render, townChunk))
                .onClick(clickContext -> this.handleFarmGrowthClick(clickContext, townChunk));
            render.layoutSlot('a', this.createFarmAutoReplantItem(render, townChunk))
                .onClick(clickContext -> this.handleFarmAutoReplantClick(clickContext, townChunk));
            render.layoutSlot('o', this.createFarmPriorityItem(render, townChunk))
                .onClick(clickContext -> this.handleFarmPriorityClick(clickContext, townChunk));
        }
        if (medicChunk) {
            render.layoutSlot('f', this.createMedicFoodRegenItem(render, townChunk));
            render.layoutSlot('g', this.createMedicHealthRegenItem(render, townChunk));
            render.layoutSlot('a', this.createMedicCleanseItem(render, townChunk));
            render.layoutSlot('o', this.createMedicFortifiedRecoveryItem(render, townChunk));
            render.layoutSlot('x', this.createMedicEmergencyRefillItem(render, townChunk));
        }
        if (armoryChunk) {
            render.layoutSlot('f', this.createArmoryFreeRepairItem(render, townChunk))
                .onClick(clickContext -> this.handleArmoryFreeRepairClick(clickContext, townChunk));
            render.layoutSlot('g', this.createArmorySalvageBlockItem(render, townChunk));
            render.layoutSlot('a', this.createArmoryRepairBlockItem(render, townChunk));
            render.layoutSlot('o', this.createArmoryDoubleSmeltItem(render, townChunk))
                .onClick(clickContext -> this.handleArmoryDoubleSmeltClick(clickContext, townChunk));
        }
        if (outpostChunk) {
            render.layoutSlot('f', this.createOutpostShopItem(render, townChunk))
                .onClick(clickContext -> this.handleClaimTownShopClick(clickContext, townChunk));
        }
        if (securityChunk && townChunk.hasFuelTank()) {
            render.layoutSlot('x', this.createPickupFuelTankItem(render, townChunk))
                .onClick(clickContext -> this.handlePickupFuelTankClick(clickContext, townChunk));
        }
        if (farmChunk && townChunk.hasSeedBox()) {
            render.layoutSlot('x', this.createPickupSeedBoxItem(render, townChunk))
                .onClick(clickContext -> this.handlePickupSeedBoxClick(clickContext, townChunk));
        }
        if (bankChunk && townChunk.getTown().hasBankCacheLocation()) {
            render.layoutSlot('x', this.createPickupCacheItem(render, townChunk))
                .onClick(clickContext -> this.handlePickupCacheClick(clickContext, townChunk));
        }
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(clickContext -> clickContext.back());
    }

    /**
     * Cancels default inventory interaction for the menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleTypeClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        if (townChunk.getChunkType() == ChunkType.FOB) {
            new I18n.Builder(this.getKey() + ".type.fob_locked_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!this.plugin.get(clickContext).getTownRuntimeService().hasTownPermission(
            clickContext.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        )) {
            new I18n.Builder(this.getKey() + ".type.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        clickContext.openForPlayer(
            TownChunkTypeView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            )
        );
    }

    private void handleProtectionsClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        if (!supportsChunkScopedProtections(townChunk)) {
            new I18n.Builder(this.getKey() + ".protections.security_only", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!this.viewerHasProtectionPermission(clickContext, townChunk.getTown())) {
            new I18n.Builder(this.getKey() + ".protections.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        if (!TownProtectionEditSessionRegistry.canOpen(townChunk.getTown().getTownUUID(), clickContext.getPlayer().getUniqueId())) {
            new I18n.Builder("town_protection_shared.messages.in_use", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }
        clickContext.openForPlayer(
            TownProtectionsView.class,
            createProtectionNavigationData(this.plugin.get(clickContext), townChunk)
        );
    }

    private void handleUpgradeClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final LevelScope scope = LevelScope.fromChunkType(townChunk.getChunkType());
        if (scope == null) {
            new I18n.Builder(this.getKey() + ".upgrade.unavailable_message", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholder("chunk_type", townChunk.getChunkType().name())
                .build()
                .sendMessage();
            return;
        }

        final LevelProgressSnapshot snapshot = this.resolveChunkLevelSnapshot(clickContext, townChunk);
        final boolean canFinalize = snapshot != null
            && snapshot.readyToLevelUp()
            && this.viewerHasUpgradePermission(clickContext);
        if (snapshot != null && canFinalize) {
            final LevelUpResult result = this.plugin.get(clickContext).getTownRuntimeService().levelUpChunk(
                clickContext.getPlayer(),
                townChunk
            );
            this.sendLevelMessage(clickContext.getPlayer(), scope, result);
            TownLevelViewSupport.sendBankUnlockMessages(this.plugin.get(clickContext), clickContext.getPlayer(), scope, result);
            TownLevelViewSupport.sendFarmUnlockMessages(this.plugin.get(clickContext), clickContext.getPlayer(), scope, result);
            TownLevelViewSupport.sendMedicUnlockMessages(this.plugin.get(clickContext), clickContext.getPlayer(), scope, result);
            TownLevelViewSupport.sendArmoryUnlockMessages(this.plugin.get(clickContext), clickContext.getPlayer(), scope, result);
            clickContext.update();
            return;
        }

        clickContext.openForPlayer(
            TownLevelProgressView.class,
            TownLevelViewSupport.createChunkNavigationData(clickContext, townChunk)
        );
    }

    private void handlePickupFuelTankClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final var result = this.plugin.get(clickContext).getTownRuntimeService().pickupFuelTank(clickContext.getPlayer(), townChunk);
        final String messageKey = switch (result.status()) {
            case SUCCESS -> result.droppedFuel() ? "fuel_pickup.success_with_fuel" : "fuel_pickup.success";
            case NO_PERMISSION -> "fuel_pickup.no_permission";
            case INVALID_CHUNK, NO_TANK, FAILED -> "fuel_pickup.failed";
        };
        new I18n.Builder(this.getKey() + '.' + messageKey, clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
        clickContext.update();
    }

    private void handlePickupSeedBoxClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final var result = this.plugin.get(clickContext).getTownRuntimeService().pickupSeedBox(clickContext.getPlayer(), townChunk);
        final String messageKey = switch (result.status()) {
            case SUCCESS -> result.droppedSeeds() ? "farm.seed_pickup.success_with_seeds" : "farm.seed_pickup.success";
            case NO_PERMISSION -> "farm.seed_pickup.no_permission";
            case INVALID_CHUNK, NO_SEED_BOX, FAILED -> "farm.seed_pickup.failed";
        };
        new I18n.Builder(this.getKey() + '.' + messageKey, clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
        clickContext.update();
    }

    private void handleFarmGrowthClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(clickContext).getTownRuntimeService();
        if (runtimeService == null || !this.viewerCanManageFarmSettings(clickContext)) {
            this.sendFarmMessage(clickContext, "no_permission");
            return;
        }

        final var growthSettings = this.plugin.get(clickContext).getFarmConfig().getGrowth();
        if (!growthSettings.isUnlocked(townChunk.getChunkLevel())) {
            this.sendFarmMessage(
                clickContext,
                "growth.locked_message",
                Map.of("unlock_level", growthSettings.tierOneUnlockLevel())
            );
            return;
        }

        final boolean enabled = !townChunk.isFarmGrowthEnabled(growthSettings.enabledByDefault());
        if (!runtimeService.setFarmGrowthEnabled(townChunk, enabled)) {
            this.sendFarmMessage(clickContext, "update_failed");
            return;
        }

        this.sendFarmMessage(clickContext, enabled ? "growth.enabled" : "growth.disabled");
        clickContext.update();
    }

    private void handleFarmAutoReplantClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(clickContext).getTownRuntimeService();
        if (runtimeService == null || !this.viewerCanManageFarmSettings(clickContext)) {
            this.sendFarmMessage(clickContext, "no_permission");
            return;
        }

        final var replantSettings = this.plugin.get(clickContext).getFarmConfig().getReplant();
        if (!replantSettings.isUnlocked(townChunk.getChunkLevel())) {
            this.sendFarmMessage(
                clickContext,
                "replant.locked_message",
                Map.of("unlock_level", replantSettings.unlockLevel())
            );
            return;
        }

        final boolean enabled = !townChunk.isFarmAutoReplantEnabled(replantSettings.enabledByDefault());
        if (!runtimeService.setFarmAutoReplantEnabled(townChunk, enabled)) {
            this.sendFarmMessage(clickContext, "update_failed");
            return;
        }

        this.sendFarmMessage(clickContext, enabled ? "replant.enabled" : "replant.disabled");
        clickContext.update();
    }

    private void handleFarmPriorityClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(clickContext).getTownRuntimeService();
        if (runtimeService == null || !this.viewerCanManageFarmSettings(clickContext)) {
            this.sendFarmMessage(clickContext, "no_permission");
            return;
        }

        final var replantSettings = this.plugin.get(clickContext).getFarmConfig().getReplant();
        if (!replantSettings.isUnlocked(townChunk.getChunkLevel())) {
            this.sendFarmMessage(
                clickContext,
                "priority.locked_message",
                Map.of("unlock_level", replantSettings.unlockLevel())
            );
            return;
        }

        final FarmReplantPriority currentPriority = townChunk.getFarmReplantPriority(replantSettings.defaultSourcePriority());
        final FarmReplantPriority nextPriority = currentPriority == FarmReplantPriority.INVENTORY_FIRST
            ? FarmReplantPriority.SEED_BOX_FIRST
            : FarmReplantPriority.INVENTORY_FIRST;
        if (!runtimeService.setFarmReplantPriority(townChunk, nextPriority)) {
            this.sendFarmMessage(clickContext, "update_failed");
            return;
        }

        this.sendFarmMessage(
            clickContext,
            "priority.changed",
            Map.of("priority", this.formatReplantPriority(nextPriority))
        );
        clickContext.update();
    }

    private void handleBankAccessClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final var bankService = this.plugin.get(clickContext).getTownBankService();
        if (bankService == null || !bankService.canOpenBankChunkView(clickContext.getPlayer(), townChunk)) {
            new I18n.Builder(this.getKey() + ".bank.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        clickContext.openForPlayer(
            TownBankRootView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", townChunk.getTown().getTownUUID(),
                "world_name", townChunk.getWorldName(),
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            )
        );
    }

    private void handleClaimTownShopClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(clickContext).getTownRuntimeService();
        if (runtimeService == null) {
            return;
        }

        if (townChunk.getChunkLevel() < 3) {
            new I18n.Builder(this.getKey() + ".outpost_shop.locked_message", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholder("unlock_level", 3)
                .build()
                .sendMessage();
            return;
        }

        if (!runtimeService.hasTownPermission(clickContext.getPlayer(), TownPermissions.MANAGE_TOWN_SHOPS)) {
            new I18n.Builder(this.getKey() + ".outpost_shop.no_permission", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (!runtimeService.isRdsTownShopFeatureAvailable()) {
            new I18n.Builder(this.getKey() + ".outpost_shop.unavailable_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final boolean claimed = runtimeService.claimOutpostTownShopToken(clickContext.getPlayer(), townChunk);
        new I18n.Builder(
            this.getKey() + ".outpost_shop." + (claimed ? "success" : "failed"),
            clickContext.getPlayer()
        )
            .includePrefix()
            .withPlaceholders(Map.of(
                "chunk_x", townChunk.getX(),
                "chunk_z", townChunk.getZ()
            ))
            .build()
            .sendMessage();
    }

    private void handleRelationshipsClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTownChunk townChunk
    ) {
        clickContext.openForPlayer(
            TownRelationshipsView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", townChunk.getTown().getTownUUID()
            )
        );
    }

    private void handleNationClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RTownChunk townChunk
    ) {
        clickContext.openForPlayer(
            TownNationView.class,
            Map.of(
                "plugin", this.plugin.get(clickContext),
                "town_uuid", townChunk.getTown().getTownUUID()
            )
        );
    }

    private void handlePickupCacheClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final var bankService = this.plugin.get(clickContext).getTownBankService();
        if (bankService == null) {
            return;
        }

        final var result = bankService.pickupCacheChest(clickContext.getPlayer(), townChunk.getTown().getTownUUID());
        final String messageKey = switch (result.status()) {
            case SUCCESS -> "bank.cache_pickup.success";
            case NO_PERMISSION -> "bank.cache_pickup.no_permission";
            case NOT_PLACED -> "bank.cache_pickup.not_placed";
            case INVALID_TARGET, LOCKED, FAILED -> "bank.cache_pickup.failed";
        };
        new I18n.Builder(this.getKey() + '.' + messageKey, clickContext.getPlayer())
            .includePrefix()
            .build()
            .sendMessage();
        clickContext.update();
    }

    private void handleArmoryFreeRepairClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final TownArmoryService townArmoryService = this.plugin.get(clickContext).getTownArmoryService();
        if (townArmoryService == null) {
            this.sendArmoryMessage(clickContext, "free_repair.failed");
            return;
        }

        final TownArmoryService.FreeRepairResult result = townArmoryService.useFreeRepair(clickContext.getPlayer(), townChunk);
        final String key = switch (result.status()) {
            case SUCCESS -> "free_repair.success";
            case NO_PERMISSION -> "no_permission";
            case LOCKED -> "free_repair.locked_message";
            case COOLING_DOWN -> "free_repair.cooldown_message";
            case NOTHING_TO_REPAIR -> "free_repair.nothing_to_repair";
            case INVALID_CHUNK, FAILED -> "free_repair.failed";
        };
        this.sendArmoryMessage(
            clickContext,
            key,
            Map.of(
                "repaired_items", result.repairedItems(),
                "cooldown_remaining", TownOverviewView.formatDurationMillis(result.cooldownRemainingMillis())
            )
        );
        clickContext.update();
    }

    private void handleArmoryDoubleSmeltClick(final @NotNull SlotClickContext clickContext, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(clickContext).getTownRuntimeService();
        if (runtimeService == null) {
            this.sendArmoryMessage(clickContext, "double_smelt.update_failed");
            return;
        }
        if (!runtimeService.isPlayerAllowed(clickContext.getPlayer(), townChunk, TownProtections.ARMORY_FURNACE_TOGGLE)) {
            this.sendArmoryMessage(clickContext, "double_smelt.no_permission");
            return;
        }

        final var settings = this.plugin.get(clickContext).getArmoryConfig().getDoubleSmelt();
        if (!settings.isUnlocked(townChunk.getChunkLevel())) {
            this.sendArmoryMessage(
                clickContext,
                "double_smelt.locked_message",
                Map.of("unlock_level", settings.unlockLevel())
            );
            return;
        }

        final boolean enabled = !townChunk.isArmoryDoubleSmeltEnabled(settings.enabledByDefault());
        if (!runtimeService.setArmoryDoubleSmeltEnabled(townChunk, enabled)) {
            this.sendArmoryMessage(clickContext, "double_smelt.update_failed");
            return;
        }

        this.sendArmoryMessage(clickContext, enabled ? "double_smelt.enabled" : "double_smelt.disabled");
        clickContext.update();
    }

    static boolean supportsChunkScopedProtections(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.SECURITY || townChunk.getChunkType() == ChunkType.FOB;
    }

    static boolean supportsTownRelationships(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.NEXUS;
    }

    static boolean supportsTownNations(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.NEXUS;
    }

    static boolean supportsChunkProgression(final @NotNull RTownChunk townChunk) {
        return LevelScope.fromChunkType(townChunk.getChunkType()) != null;
    }

    static boolean supportsFuelFeatures(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.SECURITY;
    }

    static boolean supportsBankFeatures(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.BANK;
    }

    static boolean supportsFarmFeatures(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.FARM;
    }

    static boolean supportsMedicFeatures(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.MEDIC;
    }

    static boolean supportsArmoryFeatures(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.ARMORY;
    }

    static long estimateFuelDurationMillis(final double pooledFuelUnits, final double fuelPerHour) {
        if (pooledFuelUnits <= 0.0D || fuelPerHour <= 0.0D) {
            return 0L;
        }
        final double estimatedMillis = Math.ceil((pooledFuelUnits / fuelPerHour) * 3_600_000.0D);
        if (!Double.isFinite(estimatedMillis) || estimatedMillis <= 0.0D) {
            return 0L;
        }
        return estimatedMillis >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) estimatedMillis;
    }

    static @NotNull Map<String, Object> createProtectionNavigationData(
        final @NotNull RDT plugin,
        final @NotNull RTownChunk townChunk
    ) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("plugin", plugin);
        data.put("town_uuid", townChunk.getTown().getTownUUID());
        AbstractTownProtectionView.putOriginChunkTarget(data, townChunk);
        return data;
    }

    private boolean viewerHasProtectionPermission(final @NotNull Context context, final @NotNull RTown town) {
        final var townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        final var playerData = townRuntimeService == null
            ? null
            : townRuntimeService.getPlayerData(context.getPlayer().getUniqueId());
        return playerData != null
            && java.util.Objects.equals(playerData.getTownUUID(), town.getTownUUID())
            && playerData.hasTownPermission(TownPermissions.TOWN_PROTECTIONS);
    }

    private boolean viewerHasUpgradePermission(final @NotNull Context context) {
        final var townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        return townRuntimeService != null
            && townRuntimeService.hasTownPermission(context.getPlayer(), TownPermissions.UPGRADE_CHUNK);
    }

    private @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
        final UUID resolvedTownUuid = this.townUuid.get(context);
        final String resolvedWorldName = this.worldName.get(context);
        final Integer resolvedChunkX = this.chunkX.get(context);
        final Integer resolvedChunkZ = this.chunkZ.get(context);
        if (resolvedTownUuid == null
            || resolvedWorldName == null
            || resolvedChunkX == null
            || resolvedChunkZ == null
            || this.plugin.get(context).getTownRuntimeService() == null) {
            return null;
        }
        return this.plugin.get(context).getTownRuntimeService().getTownChunk(
            resolvedTownUuid,
            resolvedWorldName,
            resolvedChunkX,
            resolvedChunkZ
        );
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        return UnifiedBuilderFactory.item(Material.LODESTONE)
            .setName(this.i18n("summary.name", context.getPlayer()).build().component())
            .setLore(this.i18n("summary.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "chunk_x", townChunk.getX(),
                    "chunk_z", townChunk.getZ(),
                    "chunk_type", townChunk.getChunkType().name(),
                    "chunk_level", townChunk.getChunkLevel(),
                    "town_name", townChunk.getTown().getTownName()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTypeItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final boolean immutableFob = townChunk.getChunkType() == ChunkType.FOB;
        final boolean canChange = !immutableFob && this.plugin.get(context).getTownRuntimeService().hasTownPermission(
            context.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        );
        final Material material = immutableFob
            ? this.plugin.get(context).getChunkTypeDisplayMaterial(townChunk.getChunkType())
            : canChange
            ? this.plugin.get(context).getChunkTypeDisplayMaterial(townChunk.getChunkType())
            : Material.GRAY_DYE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("type.name", context.getPlayer()).build().component())
            .setLore(this.i18n((immutableFob ? "type.fob_locked" : canChange ? "type" : "type.locked") + ".lore", context.getPlayer())
                .withPlaceholder("chunk_type", townChunk.getChunkType().name())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createProtectionsItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final RTown town = townChunk.getTown();
        final boolean securityScoped = supportsChunkScopedProtections(townChunk);
        final boolean unlocked = securityScoped && this.viewerHasProtectionPermission(context, town);
        final String loreKey = unlocked
            ? "protections.lore"
            : securityScoped
                ? "protections.permission_locked.lore"
                : "protections.security_locked.lore";
        return UnifiedBuilderFactory.item(unlocked ? Material.IRON_SWORD : securityScoped ? Material.GRAY_DYE : Material.BARRIER)
            .setName(this.i18n("protections.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInfoItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("info.name", context.getPlayer()).build().component())
            .setLore(this.i18n("info.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "world_name", townChunk.getWorldName(),
                    "chunk_x", townChunk.getX(),
                    "chunk_z", townChunk.getZ(),
                    "chunk_level", townChunk.getChunkLevel()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createUpgradeItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final LevelScope scope = LevelScope.fromChunkType(townChunk.getChunkType());
        final LevelProgressSnapshot snapshot = scope == null ? null : this.resolveChunkLevelSnapshot(context, townChunk);
        final boolean canFinalize = snapshot != null
            && snapshot.readyToLevelUp()
            && this.viewerHasUpgradePermission(context);
        final Material material = scope == null
            ? Material.BARRIER
            : snapshot != null && snapshot.maxLevelReached()
                ? Material.NETHER_STAR
                : canFinalize
                    ? Material.EMERALD_BLOCK
                    : Material.EXPERIENCE_BOTTLE;
        final String loreKey = scope == null
            ? "upgrade.unavailable.lore"
            : snapshot != null && snapshot.maxLevelReached()
                ? "upgrade.max.lore"
                : canFinalize
                    ? "upgrade.ready.lore"
                    : "upgrade.progress.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("upgrade.name", context.getPlayer())
                .withPlaceholder("level_scope", scope == null ? townChunk.getChunkType().name() : scope.getDisplayName())
                .build()
                .component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "level_scope", scope == null ? townChunk.getChunkType().name() : scope.getDisplayName(),
                    "chunk_type", townChunk.getChunkType().name(),
                    "chunk_level", townChunk.getChunkLevel(),
                    "current_level", snapshot == null ? townChunk.getChunkLevel() : snapshot.currentLevel(),
                    "target_level", snapshot == null ? townChunk.getChunkLevel() : snapshot.displayLevel(),
                    "progress_percent", snapshot == null ? 0 : Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFuelItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final boolean offlineProtectionEnabled = this.plugin.get(context).getSecurityConfig().getFuel().isOfflineProtection();
        final var fuelService = this.plugin.get(context).getTownFuelService();
        final boolean powered = fuelService != null && fuelService.isTownPowered(townChunk.getTown());
        final double localFuelUnits = fuelService == null ? 0.0D : fuelService.getTankFuelUnits(townChunk);
        final double pooledFuelUnits = fuelService == null ? 0.0D : fuelService.getTotalFuelUnits(townChunk.getTown());
        final double fuelPerHour = fuelService == null ? 0.0D : fuelService.getFuelPerHour(townChunk.getTown());
        final int radius = this.plugin.get(context).getSecurityConfig().getFuel().getTankPlacementRadiusBlocks();
        final int intervalSeconds = this.plugin.get(context).getSecurityConfig().getFuel().getCalculationIntervalSeconds();
        final Material material = powered
            ? Material.REDSTONE_BLOCK
            : townChunk.hasFuelTank()
                ? Material.REDSTONE
                : Material.CHEST;
        final String loreKey = townChunk.hasFuelTank()
            ? offlineProtectionEnabled ? "fuel.offline.lore" : "fuel.lore"
            : offlineProtectionEnabled ? "fuel.no_tank.offline.lore" : "fuel.no_tank.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("fuel.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "local_fuel_fe", this.formatFuelAmount(localFuelUnits),
                    "pooled_fuel_fe", this.formatFuelAmount(pooledFuelUnits),
                    "fuel_per_hour", this.formatFuelAmount(fuelPerHour),
                    "estimated_empty", this.formatEstimatedFuelDuration(pooledFuelUnits, fuelPerHour),
                    "interval_seconds", intervalSeconds,
                    "radius_blocks", radius,
                    "power_state", powered ? "Powered" : "Unpowered"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSeedBoxItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final int unlockLevel = this.plugin.get(context).getFarmConfig().getSeedBox().unlockLevel();
        final int radius = this.plugin.get(context).getFarmConfig().getSeedBox().placementRadiusBlocks();
        final int storedSeedStacks = townChunk.getSeedBoxContents().size();
        final int storedSeedItems = townChunk.getSeedBoxContents().values().stream()
            .filter(Objects::nonNull)
            .mapToInt(ItemStack::getAmount)
            .sum();
        final boolean unlocked = townChunk.getChunkLevel() >= unlockLevel;
        final Material material = !unlocked
            ? Material.BARRIER
            : townChunk.hasSeedBox()
                ? Material.CHEST
                : Material.BARREL;
        final String loreKey = !unlocked
            ? "farm.seed_box.locked.lore"
            : townChunk.hasSeedBox()
                ? "farm.seed_box.placed.lore"
                : "farm.seed_box.unplaced.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("farm.seed_box.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", unlockLevel,
                    "radius_blocks", radius,
                    "stored_seed_stacks", storedSeedStacks,
                    "stored_seed_items", storedSeedItems
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFarmGrowthItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var growthSettings = this.plugin.get(context).getFarmConfig().getGrowth();
        final boolean unlocked = growthSettings.isUnlocked(townChunk.getChunkLevel());
        final boolean enabled = unlocked && townChunk.isFarmGrowthEnabled(growthSettings.enabledByDefault());
        final Material material = !unlocked ? Material.BARRIER : enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("farm.growth.name", context.getPlayer()).build().component())
            .setLore(this.i18n(
                unlocked ? "farm.growth.lore" : "farm.growth.locked.lore",
                context.getPlayer()
            )
                .withPlaceholders(Map.of(
                    "unlock_level", growthSettings.tierOneUnlockLevel(),
                    "growth_tier", this.resolveGrowthTierLabel(townChunk.getChunkLevel(), growthSettings),
                    "growth_speed_multiplier",
                    TownFarmService.formatGrowthSpeedMultiplier(
                        growthSettings.resolveGrowthSpeedMultiplier(townChunk.getChunkLevel())
                    ),
                    "growth_state", enabled ? "Enabled" : "Disabled"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFarmAutoReplantItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var replantSettings = this.plugin.get(context).getFarmConfig().getReplant();
        final boolean unlocked = replantSettings.isUnlocked(townChunk.getChunkLevel());
        final boolean enabled = unlocked && townChunk.isFarmAutoReplantEnabled(replantSettings.enabledByDefault());
        final Material material = !unlocked ? Material.BARRIER : enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("farm.replant.name", context.getPlayer()).build().component())
            .setLore(this.i18n(
                unlocked ? "farm.replant.lore" : "farm.replant.locked.lore",
                context.getPlayer()
            )
                .withPlaceholders(Map.of(
                    "unlock_level", replantSettings.unlockLevel(),
                    "replant_state", enabled ? "Enabled" : "Disabled"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createFarmPriorityItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var replantSettings = this.plugin.get(context).getFarmConfig().getReplant();
        final boolean unlocked = replantSettings.isUnlocked(townChunk.getChunkLevel());
        final FarmReplantPriority priority = townChunk.getFarmReplantPriority(replantSettings.defaultSourcePriority());
        final Material material = !unlocked
            ? Material.BARRIER
            : priority == FarmReplantPriority.INVENTORY_FIRST
                ? Material.HOPPER
                : Material.CHEST;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("farm.priority.name", context.getPlayer()).build().component())
            .setLore(this.i18n(
                unlocked ? "farm.priority.lore" : "farm.priority.locked.lore",
                context.getPlayer()
            )
                .withPlaceholders(Map.of(
                    "unlock_level", replantSettings.unlockLevel(),
                    "priority", this.formatReplantPriority(priority)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRelationshipsItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
        final RTown town = townChunk.getTown();
        final int unlockLevel = runtimeService == null ? 5 : runtimeService.getTownRelationshipUnlockLevel();
        final boolean unlocked = runtimeService != null && runtimeService.areTownRelationshipsUnlocked(town);
        final boolean canManage = runtimeService != null
            && runtimeService.hasTownPermission(context.getPlayer(), town, TownPermissions.MANAGE_RELATIONSHIPS);
        return UnifiedBuilderFactory.item(unlocked ? Material.FILLED_MAP : Material.GRAY_DYE)
            .setName(this.i18n("relationships.name", context.getPlayer()).build().component())
            .setLore(this.i18n((unlocked ? "relationships" : "relationships.locked") + ".lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "nexus_level", town.getNexusLevel(),
                    "unlock_level", unlockLevel,
                    "manage_state", canManage ? "Enabled" : "View Only"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createNationItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
        final RTown town = townChunk.getTown();
        final int unlockLevel = runtimeService == null ? 8 : runtimeService.getTownNationUnlockLevel();
        final int minimumTowns = runtimeService == null ? 2 : runtimeService.getTownNationMinTowns();
        final int eligibleTownCount = runtimeService == null ? 0 : runtimeService.getEligibleNationFormationTowns(town).size();
        final var activeNation = runtimeService == null ? null : runtimeService.getNationForTown(town);
        final var pendingNation = activeNation == null && runtimeService != null
            ? runtimeService.getPendingNationCreatedBy(town)
            : null;
        final var pendingInvite = activeNation == null && pendingNation == null && runtimeService != null
            ? runtimeService.getPendingNationInviteFor(town)
            : null;
        final Material material = activeNation != null
            ? activeNation.getCapitalTownUuid().equals(town.getTownUUID()) ? Material.EMERALD_BLOCK : Material.LIME_DYE
            : pendingInvite != null
                ? Material.BELL
                : pendingNation != null
                    ? Material.CLOCK
                    : runtimeService != null && runtimeService.getNationCreationProgress(context.getPlayer()).available()
                        ? Material.BEACON
                        : Material.GRAY_DYE;
        final String loreKey = activeNation != null
            ? "nation.active.lore"
            : pendingInvite != null
                ? "nation.invite.lore"
                : pendingNation != null
                    ? "nation.pending.lore"
                    : "nation.locked.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("nation.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "nexus_level", town.getNexusLevel(),
                    "unlock_level", unlockLevel,
                    "minimum_towns", minimumTowns,
                    "eligible_town_count", eligibleTownCount,
                    "nation_name", activeNation != null
                        ? activeNation.getNationName()
                        : pendingNation != null
                            ? pendingNation.getNationName()
                            : pendingInvite != null && runtimeService != null && runtimeService.getNation(pendingInvite.getNationUuid()) != null
                                ? runtimeService.getNation(pendingInvite.getNationUuid()).getNationName()
                                : "-",
                    "member_count", activeNation == null || runtimeService == null ? 0 : runtimeService.getNationMemberTowns(activeNation).size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createOutpostShopItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final TownRuntimeService runtimeService = this.plugin.get(context).getTownRuntimeService();
        final boolean unlocked = townChunk.getChunkLevel() >= 3;
        final boolean rdsAvailable = runtimeService != null && runtimeService.isRdsTownShopFeatureAvailable();
        final boolean canManage = runtimeService != null
            && runtimeService.hasTownPermission(context.getPlayer(), TownPermissions.MANAGE_TOWN_SHOPS);
        final int totalShops = this.resolveOutpostTownShopCapacity(townChunk.getChunkLevel());
        final Material material = !rdsAvailable
            ? Material.BARRIER
            : !unlocked || !canManage
                ? Material.GRAY_DYE
                : Material.CHEST;
        final String loreKey = !rdsAvailable
            ? "outpost_shop.unavailable.lore"
            : !unlocked
                ? "outpost_shop.locked.lore"
                : canManage
                    ? "outpost_shop.lore"
                    : "outpost_shop.permission_locked.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("outpost_shop.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "chunk_level", townChunk.getChunkLevel(),
                    "unlock_level", 3,
                    "total_shops", totalShops
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createBankAccessItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var bankService = this.plugin.get(context).getTownBankService();
        final boolean accessible = bankService != null && bankService.canOpenBankChunkView(context.getPlayer(), townChunk);
        final Material material = accessible ? Material.CHEST : Material.BARRIER;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("bank.access.name", context.getPlayer()).build().component())
            .setLore(this.i18n((accessible ? "bank.access" : "bank.access.locked") + ".lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "bank_level", townChunk.getChunkLevel(),
                    "shared_unlock_level", this.plugin.get(context).getBankConfig().getItemStorage().unlockLevel()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createBankCacheItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var bankService = this.plugin.get(context).getTownBankService();
        final boolean unlocked = bankService != null && bankService.isCacheUnlocked(townChunk.getTown());
        final boolean placed = bankService != null && bankService.hasPlacedCache(townChunk.getTown());
        final Material material = !unlocked ? Material.BARRIER : placed ? Material.CHEST : Material.TRAPPED_CHEST;
        final String loreKey = !unlocked ? "bank.cache.locked.lore" : placed ? "bank.cache.placed.lore" : "bank.cache.unplaced.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("bank.cache.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", this.plugin.get(context).getBankConfig().getCache().unlockLevel(),
                    "radius_blocks", this.plugin.get(context).getBankConfig().getCache().placementRadiusBlocks(),
                    "stored_slots", townChunk.getTown().getBankCacheContents().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMedicFoodRegenItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getMedicConfig().getFoodRegen();
        final var status = this.resolveMedicStatus(context, townChunk);
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final Material material = !unlocked
            ? Material.BARRIER
            : status.viewerEligible() && status.viewerInsideChunk()
                ? Material.BREAD
                : Material.HAY_BLOCK;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("medic.food_regen.name", context.getPlayer()).build().component())
            .setLore(this.i18n("medic.food_regen.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "viewer_status", this.resolveMedicViewerStatus(unlocked, status, false),
                    "interval_seconds", this.formatTicksAsSeconds(settings.intervalTicks()),
                    "food_points_per_pulse", settings.foodPointsPerPulse(),
                    "saturation_per_pulse", this.formatDecimal(settings.saturationPerPulse())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMedicHealthRegenItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getMedicConfig().getHealthRegen();
        final var status = this.resolveMedicStatus(context, townChunk);
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final Material material = !unlocked
            ? Material.BARRIER
            : status.viewerEligible() && status.viewerInsideChunk()
                ? Material.GLISTERING_MELON_SLICE
                : Material.MELON_SLICE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("medic.health_regen.name", context.getPlayer()).build().component())
            .setLore(this.i18n("medic.health_regen.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "viewer_status", this.resolveMedicViewerStatus(unlocked, status, false),
                    "interval_seconds", this.formatTicksAsSeconds(settings.intervalTicks()),
                    "health_points_per_pulse", this.formatDecimal(settings.healthPointsPerPulse())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMedicCleanseItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getMedicConfig().getCleanse();
        final var status = this.resolveMedicStatus(context, townChunk);
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final Material material = !unlocked
            ? Material.BARRIER
            : status.viewerEligible() && status.viewerInsideChunk()
                ? Material.MILK_BUCKET
                : Material.BUCKET;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("medic.cleanse.name", context.getPlayer()).build().component())
            .setLore(this.i18n("medic.cleanse.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "viewer_status", this.resolveMedicViewerStatus(unlocked, status, false),
                    "interval_seconds", this.formatTicksAsSeconds(settings.intervalTicks()),
                    "harmful_effects", this.formatHarmfulEffects(settings.harmfulEffects()),
                    "effect_count", settings.harmfulEffects().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMedicFortifiedRecoveryItem(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        final var settings = this.plugin.get(context).getMedicConfig().getFortifiedRecovery();
        final var status = this.resolveMedicStatus(context, townChunk);
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final Material material = !unlocked
            ? Material.BARRIER
            : status.fortifiedRecoveryActive()
                ? Material.ENCHANTED_GOLDEN_APPLE
                : Material.GOLDEN_APPLE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("medic.fortified_recovery.name", context.getPlayer()).build().component())
            .setLore(this.i18n("medic.fortified_recovery.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "viewer_status", this.resolveMedicViewerStatus(unlocked, status, true),
                    "duration", TownOverviewView.formatDurationMillis(settings.durationSeconds() * 1000L),
                    "remaining_duration", TownOverviewView.formatDurationMillis(status.fortifiedRecoveryRemainingMillis()),
                    "target_max_health", this.formatDecimal(settings.targetMaxHealth()),
                    "upkeep_interval_seconds", this.formatTicksAsSeconds(settings.upkeepIntervalTicks()),
                    "target_food_level", settings.targetFoodLevel(),
                    "target_saturation", this.formatDecimal(settings.targetSaturation())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMedicEmergencyRefillItem(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        final var settings = this.plugin.get(context).getMedicConfig().getEmergencyRefill();
        final var status = this.resolveMedicStatus(context, townChunk);
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final Material material = !unlocked
            ? Material.BARRIER
            : status.emergencyRefillOnCooldown()
                ? Material.CLOCK
                : Material.ENCHANTED_GOLDEN_APPLE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("medic.emergency_refill.name", context.getPlayer()).build().component())
            .setLore(this.i18n("medic.emergency_refill.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "viewer_status", this.resolveEmergencyRefillStatus(unlocked, status),
                    "cooldown_seconds", settings.cooldownSeconds(),
                    "cooldown_remaining", TownOverviewView.formatDurationMillis(
                        status.emergencyRefillCooldownRemainingMillis()
                    ),
                    "target_health_mode", this.formatTargetHealthMode(settings.targetHealthMode().name()),
                    "target_food_level", settings.targetFoodLevel(),
                    "target_saturation", this.formatDecimal(settings.targetSaturation())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createArmoryFreeRepairItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getArmoryConfig().getFreeRepair();
        final TownArmoryService townArmoryService = this.plugin.get(context).getTownArmoryService();
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final boolean allowed = this.viewerCanUseArmory(context, townChunk);
        final long cooldownRemainingMillis = unlocked && townArmoryService != null
            ? townArmoryService.getFreeRepairCooldownRemainingMillis(context.getPlayer(), townChunk)
            : 0L;
        final Material material = !unlocked
            ? Material.BARRIER
            : cooldownRemainingMillis > 0L
                ? Material.CLOCK
                : Material.ANVIL;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("armory.free_repair.name", context.getPlayer()).build().component())
            .setLore(this.i18n("armory.free_repair.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "viewer_status", this.resolveArmoryUseStatus(unlocked, allowed, cooldownRemainingMillis),
                    "cooldown", TownOverviewView.formatDurationMillis(settings.cooldownSeconds() * 1000L),
                    "cooldown_remaining", TownOverviewView.formatDurationMillis(cooldownRemainingMillis)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createArmorySalvageBlockItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getArmoryConfig().getSalvageBlock();
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final String loreKey = !unlocked
            ? "armory.salvage_block.locked.lore"
            : townChunk.hasSalvageBlock()
                ? "armory.salvage_block.placed.lore"
                : "armory.salvage_block.unplaced.lore";
        return UnifiedBuilderFactory.item(unlocked ? settings.blockMaterial() : Material.BARRIER)
            .setName(this.i18n("armory.salvage_block.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "radius_blocks", settings.placementRadiusBlocks()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createArmoryRepairBlockItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getArmoryConfig().getRepairBlock();
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final String loreKey = !unlocked
            ? "armory.repair_block.locked.lore"
            : townChunk.hasRepairBlock()
                ? "armory.repair_block.placed.lore"
                : "armory.repair_block.unplaced.lore";
        return UnifiedBuilderFactory.item(unlocked ? settings.blockMaterial() : Material.BARRIER)
            .setName(this.i18n("armory.repair_block.name", context.getPlayer()).build().component())
            .setLore(this.i18n(loreKey, context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "radius_blocks", settings.placementRadiusBlocks(),
                    "iron_material_cost", settings.iron().materialCost(),
                    "gold_material_cost", settings.gold().materialCost(),
                    "diamond_material_cost", settings.diamond().materialCost(),
                    "netherite_material_cost", settings.netherite().materialCost(),
                    "iron_repair_percent", this.formatDecimal(settings.iron().repairPercent()),
                    "gold_repair_percent", this.formatDecimal(settings.gold().repairPercent()),
                    "diamond_repair_percent", this.formatDecimal(settings.diamond().repairPercent()),
                    "netherite_repair_percent", this.formatDecimal(settings.netherite().repairPercent())
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createArmoryDoubleSmeltItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var settings = this.plugin.get(context).getArmoryConfig().getDoubleSmelt();
        final boolean unlocked = settings.isUnlocked(townChunk.getChunkLevel());
        final boolean enabled = unlocked && townChunk.isArmoryDoubleSmeltEnabled(settings.enabledByDefault());
        final boolean canToggle = this.viewerCanManageArmoryDoubleSmelt(context, townChunk);
        final Material material = !unlocked
            ? Material.BARRIER
            : enabled
                ? Material.BLAST_FURNACE
                : Material.FURNACE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("armory.double_smelt.name", context.getPlayer()).build().component())
            .setLore(this.i18n("armory.double_smelt.lore", context.getPlayer())
                .withPlaceholders(Map.of(
                    "unlock_level", settings.unlockLevel(),
                    "toggle_state", enabled ? "Enabled" : "Disabled",
                    "access_state", canToggle ? "Can Toggle" : "No Access",
                    "burn_faster_multiplier", this.formatDecimal(settings.burnFasterMultiplier()),
                    "extra_fuel_per_smelt_units", settings.extraFuelPerSmeltUnits()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPickupFuelTankItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final boolean canPickup = this.plugin.get(context).getTownRuntimeService().hasTownPermission(
            context.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        );
        return UnifiedBuilderFactory.item(canPickup ? Material.HOPPER : Material.GRAY_DYE)
            .setName(this.i18n("fuel_pickup.name", context.getPlayer()).build().component())
            .setLore(this.i18n((canPickup ? "fuel_pickup" : "fuel_pickup.locked") + ".lore", context.getPlayer())
                .withPlaceholder("chunk_type", townChunk.getChunkType().name())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPickupSeedBoxItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final boolean canPickup = this.plugin.get(context).getTownRuntimeService().hasTownPermission(
            context.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        );
        return UnifiedBuilderFactory.item(canPickup ? Material.HOPPER : Material.GRAY_DYE)
            .setName(this.i18n("farm.seed_pickup.name", context.getPlayer()).build().component())
            .setLore(this.i18n((canPickup ? "farm.seed_pickup" : "farm.seed_pickup.locked") + ".lore", context.getPlayer())
                .withPlaceholder("chunk_type", townChunk.getChunkType().name())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPickupCacheItem(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var bankService = this.plugin.get(context).getTownBankService();
        final boolean canPickup = bankService != null && bankService.canManageCachePlacement(context.getPlayer(), townChunk.getTown());
        return UnifiedBuilderFactory.item(canPickup ? Material.HOPPER : Material.GRAY_DYE)
            .setName(this.i18n("bank.cache_pickup.name", context.getPlayer()).build().component())
            .setLore(this.i18n((canPickup ? "bank.cache_pickup" : "bank.cache_pickup.locked") + ".lore", context.getPlayer())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingChunkItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @Nullable LevelProgressSnapshot resolveChunkLevelSnapshot(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        final var townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        return townRuntimeService == null ? null : townRuntimeService.getChunkLevelProgress(context.getPlayer(), townChunk);
    }

    private @NotNull String formatFuelAmount(final double amount) {
        return String.format(Locale.ROOT, "%.2f", Math.max(0.0D, amount));
    }

    private int resolveOutpostTownShopCapacity(final int chunkLevel) {
        return switch (Math.max(1, chunkLevel)) {
            case 1, 2 -> 0;
            case 3 -> 1;
            case 4 -> 3;
            default -> 5;
        };
    }

    private @NotNull String formatEstimatedFuelDuration(final double pooledFuelUnits, final double fuelPerHour) {
        return TownOverviewView.formatDurationMillis(estimateFuelDurationMillis(pooledFuelUnits, fuelPerHour));
    }

    private boolean viewerCanManageFarmSettings(final @NotNull Context context) {
        final var townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        return townRuntimeService != null
            && townRuntimeService.hasTownPermission(context.getPlayer(), TownPermissions.CHANGE_CHUNK_TYPE);
    }

    private boolean viewerCanUseArmory(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        return townRuntimeService != null
            && townRuntimeService.isPlayerAllowed(context.getPlayer(), townChunk, TownProtections.ARMORY_USE);
    }

    private boolean viewerCanManageArmoryDoubleSmelt(final @NotNull Context context, final @NotNull RTownChunk townChunk) {
        final var townRuntimeService = this.plugin.get(context).getTownRuntimeService();
        return townRuntimeService != null
            && townRuntimeService.isPlayerAllowed(context.getPlayer(), townChunk, TownProtections.ARMORY_FURNACE_TOGGLE);
    }

    private @NotNull String resolveGrowthTierLabel(
        final int farmLevel,
        final @NotNull com.raindropcentral.rdt.configs.FarmConfigSection.GrowthSettings growthSettings
    ) {
        return growthSettings.usesTierTwo(farmLevel) ? "II" : "I";
    }

    private @NotNull String formatReplantPriority(final @NotNull FarmReplantPriority priority) {
        return switch (priority) {
            case INVENTORY_FIRST -> "Inventory First";
            case SEED_BOX_FIRST -> "Seed Box First";
        };
    }

    private @NotNull TownMedicService.MedicChunkStatus resolveMedicStatus(
        final @NotNull Context context,
        final @NotNull RTownChunk townChunk
    ) {
        final TownMedicService townMedicService = this.plugin.get(context).getTownMedicService();
        return townMedicService == null
            ? new TownMedicService.MedicChunkStatus(true, false, false, 0L, 0L)
            : townMedicService.getChunkStatus(context.getPlayer(), townChunk);
    }

    private @NotNull String resolveMedicViewerStatus(
        final boolean unlocked,
        final @NotNull TownMedicService.MedicChunkStatus status,
        final boolean allowTimedActive
    ) {
        if (!status.viewerEligible()) {
            return "Town Members Only";
        }
        if (!unlocked) {
            return "Locked";
        }
        if (allowTimedActive && status.fortifiedRecoveryActive()) {
            return "Active";
        }
        return status.viewerInsideChunk() ? "Active" : "Ready";
    }

    private @NotNull String resolveEmergencyRefillStatus(
        final boolean unlocked,
        final @NotNull TownMedicService.MedicChunkStatus status
    ) {
        if (!status.viewerEligible()) {
            return "Town Members Only";
        }
        if (!unlocked) {
            return "Locked";
        }
        if (status.emergencyRefillOnCooldown()) {
            return "Cooling Down";
        }
        return status.viewerInsideChunk() ? "Ready" : "Waiting";
    }

    private @NotNull String resolveArmoryUseStatus(
        final boolean unlocked,
        final boolean allowed,
        final long cooldownRemainingMillis
    ) {
        if (!allowed) {
            return "No Access";
        }
        if (!unlocked) {
            return "Locked";
        }
        if (cooldownRemainingMillis > 0L) {
            return "Cooling Down";
        }
        return "Ready";
    }

    private @NotNull String formatTicksAsSeconds(final long ticks) {
        if (ticks % 20L == 0L) {
            return Long.toString(ticks / 20L);
        }
        return this.formatDecimal(ticks / 20.0D);
    }

    private @NotNull String formatDecimal(final double value) {
        final String formatted = String.format(Locale.ROOT, "%.2f", value);
        if (formatted.endsWith("00")) {
            return formatted.substring(0, formatted.length() - 3);
        }
        if (formatted.endsWith("0")) {
            return formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private @NotNull String formatHarmfulEffects(final @NotNull Set<String> harmfulEffects) {
        return harmfulEffects.stream()
            .filter(Objects::nonNull)
            .map(this::formatEffectName)
            .reduce((left, right) -> left + ", " + right)
            .orElse("None");
    }

    private @NotNull String formatEffectName(final @NotNull String rawName) {
        final String[] words = rawName.toLowerCase(Locale.ROOT).split("_");
        final StringBuilder formatted = new StringBuilder();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.length() == 0 ? rawName : formatted.toString();
    }

    private @NotNull String formatTargetHealthMode(final @NotNull String rawTargetHealthMode) {
        return switch (rawTargetHealthMode) {
            case "CURRENT_MAX" -> "Current Max";
            case "VANILLA_MAX" -> "Vanilla Max";
            default -> rawTargetHealthMode;
        };
    }

    private void sendFarmMessage(final @NotNull SlotClickContext clickContext, final @NotNull String key) {
        this.sendFarmMessage(clickContext, key, Map.of());
    }

    private void sendFarmMessage(
        final @NotNull SlotClickContext clickContext,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        new I18n.Builder(this.getKey() + ".farm." + key, clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(placeholders)
            .build()
            .sendMessage();
    }

    private void sendArmoryMessage(final @NotNull SlotClickContext clickContext, final @NotNull String key) {
        this.sendArmoryMessage(clickContext, key, Map.of());
    }

    private void sendArmoryMessage(
        final @NotNull SlotClickContext clickContext,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        new I18n.Builder(this.getKey() + ".armory." + key, clickContext.getPlayer())
            .includePrefix()
            .withPlaceholders(placeholders)
            .build()
            .sendMessage();
    }

    private void sendLevelMessage(
        final @NotNull Player player,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        final String key = switch (result.status()) {
            case SUCCESS -> "level_up_success";
            case NO_PERMISSION -> "no_permission";
            case NOT_READY -> "not_ready";
            case MAX_LEVEL -> "max_level";
            case INVALID_TARGET, FAILED -> "level_up_failed";
        };
        new I18n.Builder("town_level_shared.messages." + key, player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "level_scope", scope.getDisplayName(),
                "current_level", result.previousLevel(),
                "target_level", result.newLevel()
            ))
            .build()
            .sendMessage();
    }
}
