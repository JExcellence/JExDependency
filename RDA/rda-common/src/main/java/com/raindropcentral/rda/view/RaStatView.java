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

package com.raindropcentral.rda.view;

import com.raindropcentral.rda.AbilitySnapshot;
import com.raindropcentral.rda.CoreStatSnapshot;
import com.raindropcentral.rda.CoreStatType;
import com.raindropcentral.rda.PlayerBuildSnapshot;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.StatsConfig;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated stat detail menu that shows point spending and linked skill abilities.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class RaStatView extends APaginatedView<AbilitySnapshot> {

    private final State<RDA> rda = initialState("plugin");
    private final State<CoreStatType> statType = initialState("statType");

    /**
     * Creates the stat detail menu.
     */
    public RaStatView() {
        super(RaMainView.class);
    }

    @Override
    protected String getKey() {
        return "ra_stat_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    I    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  <p+>   "
        };
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return this.getStatType(open).getPlaceholders(open.getPlayer());
    }

    @Override
    protected CompletableFuture<List<AbilitySnapshot>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDA plugin = this.rda.get(context);
        if (plugin == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.completedFuture(
            plugin.getAbilitySnapshotsForStat(context.getPlayer(), this.getStatType(context))
        );
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull AbilitySnapshot entry
    ) {
        final Player player = context.getPlayer();
        final RDA plugin = this.rda.get(context);
        final boolean active = entry.abilityDefinition().active();
        final long cooldownSeconds = plugin == null || plugin.getPlayerBuildService() == null
            ? 0L
            : plugin.getPlayerBuildService().getRemainingCooldownSeconds(player, entry.skillType());
        final boolean running = plugin != null
            && plugin.getPlayerBuildService() != null
            && plugin.getPlayerBuildService().isSkillActive(player, entry.skillType());
        final LinkedHashMap<String, Object> placeholders = new LinkedHashMap<>();
        placeholders.put("ability_name", entry.abilityDefinition().name());
        placeholders.put("ability_description", entry.abilityDefinition().resolveDescription(player));
        placeholders.put("skill_name", entry.skillType().getDisplayName(player));
        placeholders.put("ability_kind", active ? "Active" : "Passive");
        placeholders.put("ability_tier", entry.displayTier());
        placeholders.put("ability_potency", String.format(Locale.ROOT, "%.2f", entry.potency()));
        placeholders.put("ability_state", entry.unlocked() ? "Unlocked" : "Locked");
        placeholders.put("cooldown_seconds", active ? cooldownSeconds : "-");
        placeholders.put("active_state", active ? (running ? "Running" : (cooldownSeconds > 0 ? "Cooling Down" : "Ready")) : "-");
        placeholders.put("next_skill_level", entry.nextRequiredSkillLevel());
        placeholders.put("next_stat_points", entry.nextRequiredStatPoints());
        builder.withItem(
            UnifiedBuilderFactory.item(entry.abilityDefinition().icon())
                .setName(this.i18n("ability.name", player)
                    .withPlaceholders(this.mergePlaceholders(player, context, placeholders))
                    .build()
                    .component())
                .setLore(this.i18n("ability.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, context, placeholders))
                    .build()
                    .children())
                .build()
        );
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDA plugin = this.rda.get(render);
        if (plugin == null) {
            return;
        }

        final PlayerBuildSnapshot buildSnapshot = plugin.getBuildSnapshot(player);
        final CoreStatType resolvedStatType = this.getStatType(render);
        final CoreStatSnapshot statSnapshot = buildSnapshot.statSnapshots().get(resolvedStatType);
        final StatsConfig statsConfig = plugin.getStatsConfig();
        final Material icon = statsConfig == null
            ? resolvedStatType.getFallbackIcon()
            : statsConfig.getStatDefinition(resolvedStatType).icon();

        render.layoutSlot(
            'I',
            UnifiedBuilderFactory.item(icon)
                .setName(this.i18n("info.name", player)
                    .withPlaceholders(resolvedStatType.getPlaceholders(player))
                    .build()
                    .component())
                .setLore(this.i18n("info.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, render, Map.of(
                        "allocated_points", statSnapshot == null ? 0 : statSnapshot.allocatedPoints(),
                        "unspent_points", buildSnapshot.unspentPoints(),
                        "passive_label", statSnapshot == null ? "-" : statSnapshot.passiveLabel(),
                        "passive_value", statSnapshot == null
                            ? "0"
                            : String.format(Locale.ROOT, "%.2f", statSnapshot.passiveValue()),
                        "passive_unit", statSnapshot == null ? "" : statSnapshot.passiveUnit(),
                        "stat_description", statSnapshot == null
                            ? ""
                            : statsConfig.getStatDefinition(resolvedStatType).resolveLoreDescription(player)
                    )))
                    .build()
                    .children())
                .build()
        );

        render.layoutSlot(
            '+',
            UnifiedBuilderFactory.item(Material.EMERALD)
                .setName(this.i18n("spend.name", player)
                    .withPlaceholders(resolvedStatType.getPlaceholders(player))
                    .build()
                    .component())
                .setLore(this.i18n("spend.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, render, Map.of(
                        "unspent_points", buildSnapshot.unspentPoints(),
                        "allocated_points", statSnapshot == null ? 0 : statSnapshot.allocatedPoints()
                    )))
                    .build()
                    .children())
                .build()
        ).onClick(clickContext -> {
            if (plugin.getPlayerBuildService() == null) {
                return;
            }
            if (plugin.getPlayerBuildService().spendPoint(player, resolvedStatType)) {
                clickContext.update();
            }
        });
    }

    private @NotNull CoreStatType getStatType(final @NotNull Context context) {
        final CoreStatType resolved = this.statType.get(context);
        return resolved == null ? CoreStatType.STR : resolved;
    }

    private @NotNull Map<String, Object> mergePlaceholders(
        final @NotNull Player player,
        final @NotNull Context context,
        final @NotNull Map<String, Object> extraPlaceholders
    ) {
        final LinkedHashMap<String, Object> placeholders =
            new LinkedHashMap<>(this.getStatType(context).getPlaceholders(player));
        placeholders.putAll(extraPlaceholders);
        return placeholders;
    }
}
