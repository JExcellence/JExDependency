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
import com.raindropcentral.rdt.service.LevelProgressSnapshot;
import com.raindropcentral.rdt.service.LevelScope;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared roadmap browser for configured Nexus and chunk levels.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownLevelRoadmapView extends BaseView {

    private static final List<Integer> ROADMAP_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    );
    private static final int PAGE_SIZE = ROADMAP_SLOTS.size();
    private static final String PAGE_KEY = "level_page";

    /**
     * Creates the shared roadmap browser.
     */
    public TownLevelRoadmapView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "town_level_roadmap_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "p   h   n",
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

        final List<Integer> configuredLevels = this.resolveConfiguredLevels(render);
        final int maxPage = Math.max(0, (configuredLevels.size() - 1) / PAGE_SIZE);
        final int page = Math.max(0, Math.min(this.resolvePage(render), maxPage));
        final int startIndex = page * PAGE_SIZE;
        final int endIndex = Math.min(configuredLevels.size(), startIndex + PAGE_SIZE);

        render.layoutSlot('s', this.createSummaryItem(player, snapshot, page + 1, maxPage + 1));
        render.layoutSlot('p', this.createPageItem(player, "previous", page > 0))
            .onClick(clickContext -> {
                if (page > 0) {
                    clickContext.openForPlayer(TownLevelRoadmapView.class, this.createPageNavigationData(clickContext, page - 1));
                }
            });
        render.layoutSlot('n', this.createPageItem(player, "next", page < maxPage))
            .onClick(clickContext -> {
                if (page < maxPage) {
                    clickContext.openForPlayer(TownLevelRoadmapView.class, this.createPageNavigationData(clickContext, page + 1));
                }
            });
        render.layoutSlot('h', this.createHubShortcut(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownLevelProgressView.class,
                this.createHubNavigationData(clickContext)
            ));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(SlotClickContext::back);

        for (int index = 0; index < ROADMAP_SLOTS.size(); index++) {
            final int slot = ROADMAP_SLOTS.get(index);
            final int levelIndex = startIndex + index;
            if (levelIndex >= endIndex) {
                render.slot(slot).renderWith(() -> this.createEmptyItem(player));
                continue;
            }

            final int level = configuredLevels.get(levelIndex);
            final LevelProgressSnapshot levelSnapshot = this.resolveLevelSnapshot(render, snapshot, level);
            render.slot(slot).renderWith(() -> this.createLevelItem(player, levelSnapshot))
                .onClick(clickContext -> clickContext.openForPlayer(
                    TownLevelRequirementsView.class,
                    this.createPreviewNavigationData(clickContext, level)
                ));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull List<Integer> resolveConfiguredLevels(final @NotNull Context context) {
        final RDT plugin = TownLevelViewSupport.plugin(context);
        if (plugin == null) {
            return List.of();
        }

        final LevelScope resolvedScope = this.resolveScope(context);
        final List<Integer> levels = new ArrayList<>(switch (resolvedScope) {
            case NEXUS -> plugin.getNexusConfig().getLevels().keySet();
            case SECURITY -> plugin.getSecurityConfig().getLevels().keySet();
            case BANK -> plugin.getBankConfig().getLevels().keySet();
            case FARM -> plugin.getFarmConfig().getLevels().keySet();
            case OUTPOST -> plugin.getOutpostConfig().getLevels().keySet();
        });
        levels.sort(Comparator.naturalOrder());
        return List.copyOf(levels);
    }

    private int resolvePage(final @NotNull Context context) {
        final Map<String, Object> data = TownLevelViewSupport.copyInitialData(context);
        if (data == null) {
            return 0;
        }
        final Object rawPage = data.get(PAGE_KEY);
        if (rawPage instanceof Number pageNumber) {
            return Math.max(0, pageNumber.intValue());
        }
        if (rawPage instanceof String serializedPage) {
            try {
                return Math.max(0, Integer.parseInt(serializedPage.trim()));
            } catch (final NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private @NotNull LevelProgressSnapshot resolveLevelSnapshot(
        final @NotNull Context context,
        final @NotNull LevelProgressSnapshot fallbackSnapshot,
        final int level
    ) {
        final RDT plugin = TownLevelViewSupport.plugin(context);
        final var town = TownLevelViewSupport.resolveTown(context);
        if (plugin == null || plugin.getTownRuntimeService() == null || town == null) {
            return fallbackSnapshot;
        }

        return switch (this.resolveScope(context)) {
            case NEXUS -> plugin.getTownRuntimeService().getNexusLevelProgress(context.getPlayer(), town, level);
            case SECURITY, BANK, FARM, OUTPOST -> {
                final var chunk = TownLevelViewSupport.resolveChunk(context);
                yield chunk == null
                    ? fallbackSnapshot
                    : plugin.getTownRuntimeService().getChunkLevelProgress(context.getPlayer(), chunk, level);
            }
        };
    }

    private @NotNull LevelScope resolveScope(final @NotNull Context context) {
        final LevelScope requestedScope = TownLevelViewSupport.scope(context);
        if (!requestedScope.isChunkScope()) {
            return requestedScope;
        }

        final var chunk = TownLevelViewSupport.resolveChunk(context);
        final LevelScope liveScope = chunk == null ? null : LevelScope.fromChunkType(chunk.getChunkType());
        return liveScope == null ? requestedScope : liveScope;
    }

    private @NotNull Map<String, Object> createPageNavigationData(final @NotNull Context context, final int page) {
        final Map<String, Object> data = this.createNavigationData(context);
        data.put(PAGE_KEY, page);
        return data;
    }

    private @NotNull Map<String, Object> createPreviewNavigationData(final @NotNull Context context, final int previewLevel) {
        final Map<String, Object> data = this.createNavigationData(context);
        data.put(TownLevelViewSupport.PREVIEW_LEVEL_KEY, previewLevel);
        return data;
    }

    private @NotNull Map<String, Object> createNavigationData(final @NotNull Context context) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownLevelViewSupport.copyInitialData(context);
        if (copiedData != null) {
            data.putAll(TownLevelViewSupport.stripTransientData(copiedData));
        }
        return data;
    }

    private @NotNull Map<String, Object> createHubNavigationData(final @NotNull Context context) {
        final Map<String, Object> data = this.createNavigationData(context);
        data.remove(PAGE_KEY);
        data.remove(TownLevelViewSupport.PREVIEW_LEVEL_KEY);
        return data;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull LevelProgressSnapshot snapshot,
        final int page,
        final int pageCount
    ) {
        return UnifiedBuilderFactory.item(Material.CARTOGRAPHY_TABLE)
            .setName(this.i18n("summary.name", player)
                .withPlaceholder("level_scope", snapshot.scope().getDisplayName())
                .build()
                .component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "level_scope", snapshot.scope().getDisplayName(),
                    "current_level", snapshot.currentLevel(),
                    "max_level", snapshot.maxLevel(),
                    "page", page,
                    "page_count", pageCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLevelItem(final @NotNull Player player, final @NotNull LevelProgressSnapshot snapshot) {
        final boolean currentLevel = snapshot.displayLevel() == snapshot.currentLevel();
        final Material material = currentLevel
            ? Material.BEACON
            : snapshot.completedLevel()
            ? Material.LIME_STAINED_GLASS_PANE
            : snapshot.readyToLevelUp()
                ? Material.EMERALD_BLOCK
                : Material.EXPERIENCE_BOTTLE;
        final String loreKey = currentLevel
            ? "entry.current.lore"
            : snapshot.completedLevel()
            ? "entry.completed.lore"
            : snapshot.readyToLevelUp()
                ? "entry.ready.lore"
                : "entry.upcoming.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "level_scope", snapshot.scope().getDisplayName(),
                    "target_level", snapshot.displayLevel()
                ))
                .build()
                .component())
            .setLore(this.i18n(loreKey, player)
                .withPlaceholders(Map.of(
                    "target_level", snapshot.displayLevel(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D),
                    "requirement_count", snapshot.requirements().size(),
                    "reward_count", snapshot.rewards().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPageItem(
        final @NotNull Player player,
        final @NotNull String direction,
        final boolean enabled
    ) {
        final Material material = enabled ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE;
        final String loreKey = "page." + direction + (enabled ? ".lore" : ".locked.lore");
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("page." + direction + ".name", player).build().component())
            .setLore(this.i18n(loreKey, player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createHubShortcut(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("hub.name", player).build().component())
            .setLore(this.i18n("hub.lore", player).build().children())
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

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
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
