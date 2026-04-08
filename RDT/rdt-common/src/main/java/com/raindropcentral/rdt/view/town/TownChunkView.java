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
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownPermissions;
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
            "   t p   ",
            "   i u   ",
            "   f x   ",
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
        render.layoutSlot('s', this.createSummaryItem(render, townChunk));
        render.layoutSlot('t', this.createTypeItem(render, townChunk))
            .onClick(clickContext -> this.handleTypeClick(clickContext, townChunk));
        if (supportsChunkScopedProtections(townChunk)) {
            render.layoutSlot('p', this.createProtectionsItem(render, townChunk))
                .onClick(clickContext -> this.handleProtectionsClick(clickContext, townChunk));
        }
        render.layoutSlot('i', this.createInfoItem(render, townChunk));
        if (supportsChunkProgression(townChunk)) {
            render.layoutSlot('u', this.createUpgradeItem(render, townChunk))
                .onClick(clickContext -> this.handleUpgradeClick(clickContext, townChunk));
        }
        if (securityChunk) {
            render.layoutSlot('f', this.createFuelItem(render, townChunk));
        }
        if (securityChunk && townChunk.hasFuelTank()) {
            render.layoutSlot('x', this.createPickupFuelTankItem(render, townChunk))
                .onClick(clickContext -> this.handlePickupFuelTankClick(clickContext, townChunk));
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

    static boolean supportsChunkScopedProtections(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.SECURITY;
    }

    static boolean supportsChunkProgression(final @NotNull RTownChunk townChunk) {
        return LevelScope.fromChunkType(townChunk.getChunkType()) != null;
    }

    static boolean supportsFuelFeatures(final @NotNull RTownChunk townChunk) {
        return townChunk.getChunkType() == ChunkType.SECURITY;
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
        final boolean canChange = this.plugin.get(context).getTownRuntimeService().hasTownPermission(
            context.getPlayer(),
            TownPermissions.CHANGE_CHUNK_TYPE
        );
        final Material material = canChange
            ? this.plugin.get(context).getDefaultConfig().getChunkTypeIconMaterial(townChunk.getChunkType())
            : Material.GRAY_DYE;
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("type.name", context.getPlayer()).build().component())
            .setLore(this.i18n((canChange ? "type" : "type.locked") + ".lore", context.getPlayer())
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

    private @NotNull String formatEstimatedFuelDuration(final double pooledFuelUnits, final double fuelPerHour) {
        return TownOverviewView.formatDurationMillis(estimateFuelDurationMillis(pooledFuelUnits, fuelPerHour));
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
