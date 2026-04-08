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
import com.raindropcentral.rdt.service.LevelUpStatus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared progression hub for Nexus and chunk levels.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownLevelProgressView extends BaseView {

    /**
     * Creates the shared level progression hub.
     */
    public TownLevelProgressView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "town_level_progress_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   q w   ",
            "   m u   ",
            "         ",
            "         ",
            "r        "
        };
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final LevelProgressSnapshot snapshot = TownLevelViewSupport.resolveSnapshot(render);
        if (snapshot == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, snapshot));
        render.layoutSlot('q', this.createRequirementsItem(player, snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownLevelRequirementsView.class,
                this.createBaseNavigationData(clickContext)
            ));
        render.layoutSlot('w', this.createRewardsItem(player, snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownLevelRewardsView.class,
                this.createBaseNavigationData(clickContext)
            ));
        render.layoutSlot('m', this.createRoadmapItem(player, snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownLevelRoadmapView.class,
                this.createBaseNavigationData(clickContext)
            ));
        render.layoutSlot('u', this.createUpgradeItem(player, snapshot))
            .onClick(clickContext -> this.handleUpgradeClick(clickContext, snapshot));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(SlotClickContext::back);
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleUpgradeClick(final @NotNull SlotClickContext clickContext, final @NotNull LevelProgressSnapshot snapshot) {
        if (!snapshot.available()) {
            this.sendSharedMessage(clickContext.getPlayer(), "invalid_target", Map.of("level_scope", snapshot.scope().getDisplayName()));
            return;
        }
        if (snapshot.maxLevelReached()) {
            this.sendSharedMessage(clickContext.getPlayer(), "max_level", Map.of("level_scope", snapshot.scope().getDisplayName()));
            return;
        }
        if (!snapshot.readyToLevelUp()) {
            clickContext.openForPlayer(TownLevelRequirementsView.class, this.createBaseNavigationData(clickContext));
            return;
        }

        final RDT plugin = TownLevelViewSupport.plugin(clickContext);
        final RTown town = TownLevelViewSupport.resolveTown(clickContext);
        if (plugin == null || plugin.getTownRuntimeService() == null || town == null) {
            this.sendSharedMessage(clickContext.getPlayer(), "invalid_target", Map.of("level_scope", snapshot.scope().getDisplayName()));
            return;
        }

        final LevelUpResult result = switch (snapshot.scope()) {
            case NEXUS -> plugin.getTownRuntimeService().levelUpNexus(clickContext.getPlayer(), town);
            case SECURITY, BANK, FARM, OUTPOST -> {
                final RTownChunk townChunk = TownLevelViewSupport.resolveChunk(clickContext);
                yield townChunk == null
                    ? new LevelUpResult(LevelUpStatus.INVALID_TARGET, snapshot.currentLevel(), snapshot.currentLevel())
                    : plugin.getTownRuntimeService().levelUpChunk(clickContext.getPlayer(), townChunk);
            }
        };
        this.handleLevelUpResult(clickContext, snapshot.scope(), result);
    }

    private void handleLevelUpResult(
        final @NotNull SlotClickContext clickContext,
        final @NotNull LevelScope scope,
        final @NotNull LevelUpResult result
    ) {
        final Map<String, Object> placeholders = Map.of(
            "level_scope", scope.getDisplayName(),
            "current_level", result.previousLevel(),
            "target_level", result.newLevel()
        );

        switch (result.status()) {
            case SUCCESS -> {
                this.sendSharedMessage(clickContext.getPlayer(), "level_up_success", placeholders);
                clickContext.openForPlayer(TownLevelProgressView.class, this.createBaseNavigationData(clickContext));
            }
            case NO_PERMISSION -> this.sendSharedMessage(clickContext.getPlayer(), "no_permission", placeholders);
            case NOT_READY -> this.sendSharedMessage(clickContext.getPlayer(), "not_ready", placeholders);
            case MAX_LEVEL -> this.sendSharedMessage(clickContext.getPlayer(), "max_level", placeholders);
            case INVALID_TARGET, FAILED -> this.sendSharedMessage(clickContext.getPlayer(), "level_up_failed", placeholders);
        }
    }

    private @NotNull Map<String, Object> createBaseNavigationData(final @NotNull Context context) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownLevelViewSupport.copyInitialData(context);
        if (copiedData != null) {
            data.putAll(TownLevelViewSupport.stripTransientData(copiedData));
        }
        data.remove(TownLevelViewSupport.PREVIEW_LEVEL_KEY);
        return data;
    }

    private void sendSharedMessage(
        final @NotNull Player player,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        new I18n.Builder("town_level_shared.messages." + key, player)
            .includePrefix()
            .withPlaceholders(placeholders)
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final @NotNull LevelProgressSnapshot snapshot) {
        return UnifiedBuilderFactory.item(snapshot.scope() == LevelScope.NEXUS ? Material.BEACON : Material.SHIELD)
            .setName(this.i18n("summary.name", player)
                .withPlaceholder("level_scope", snapshot.scope().getDisplayName())
                .build()
                .component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "level_scope", snapshot.scope().getDisplayName(),
                    "town_name", snapshot.townName(),
                    "current_level", snapshot.currentLevel(),
                    "target_level", snapshot.displayLevel(),
                    "max_level", snapshot.maxLevel(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D),
                    "location", snapshot.scope().isChunkScope()
                        ? snapshot.chunkX() + ", " + snapshot.chunkZ()
                        : "Nexus"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementsItem(final @NotNull Player player, final @NotNull LevelProgressSnapshot snapshot) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("requirements.name", player).build().component())
            .setLore(this.i18n("requirements.lore", player)
                .withPlaceholders(Map.of(
                    "requirement_count", snapshot.requirements().size(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRewardsItem(final @NotNull Player player, final @NotNull LevelProgressSnapshot snapshot) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("rewards.name", player).build().component())
            .setLore(this.i18n("rewards.lore", player)
                .withPlaceholder("reward_count", snapshot.rewards().size())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRoadmapItem(final @NotNull Player player, final @NotNull LevelProgressSnapshot snapshot) {
        return UnifiedBuilderFactory.item(Material.CARTOGRAPHY_TABLE)
            .setName(this.i18n("roadmap.name", player).build().component())
            .setLore(this.i18n("roadmap.lore", player)
                .withPlaceholders(Map.of(
                    "current_level", snapshot.currentLevel(),
                    "max_level", snapshot.maxLevel()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createUpgradeItem(final @NotNull Player player, final @NotNull LevelProgressSnapshot snapshot) {
        final Material material = !snapshot.available()
            ? Material.BARRIER
            : snapshot.maxLevelReached()
                ? Material.NETHER_STAR
                : snapshot.readyToLevelUp()
                    ? Material.EMERALD_BLOCK
                    : Material.EXPERIENCE_BOTTLE;
        final String loreKey = !snapshot.available()
            ? "upgrade.unavailable.lore"
            : snapshot.maxLevelReached()
                ? "upgrade.max.lore"
                : snapshot.readyToLevelUp()
                    ? "upgrade.ready.lore"
                    : "upgrade.progress.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("upgrade.name", player)
                .withPlaceholder("level_scope", snapshot.scope().getDisplayName())
                .build()
                .component())
            .setLore(this.i18n(loreKey, player)
                .withPlaceholders(Map.of(
                    "level_scope", snapshot.scope().getDisplayName(),
                    "target_level", snapshot.displayLevel(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
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

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
