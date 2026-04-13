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

import com.raindropcentral.rda.CoreStatSnapshot;
import com.raindropcentral.rda.CoreStatType;
import com.raindropcentral.rda.PlayerBuildSnapshot;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.StatsConfig;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Main 54-slot navigation menu for Raindrop Abilities.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class RaMainView extends BaseView {

    private final State<RDA> rda = initialState("plugin");

    /**
     * Creates the main abilities menu.
     */
    public RaMainView() {
        super();
    }

    @Override
    protected String getKey() {
        return "ra_main_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            " V S A D ",
            " I P L C ",
            "         ",
            "   M K   ",
            "         ",
            "X        "
        };
    }

    /**
     * Renders the stats-first overview layout.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDA plugin = this.rda.get(render);
        final PlayerBuildSnapshot buildSnapshot = plugin == null ? null : plugin.getBuildSnapshot(player);
        final long totalPower = plugin == null ? 0L : plugin.getTotalPower(player);
        final int enabledSkills = plugin == null ? 0 : plugin.getEnabledSkills().size();
        final StatsConfig statsConfig = plugin == null ? null : plugin.getStatsConfig();
        final EnumMap<CoreStatType, Character> slots = new EnumMap<>(CoreStatType.class);
        slots.put(CoreStatType.VIT, 'V');
        slots.put(CoreStatType.STR, 'S');
        slots.put(CoreStatType.AGI, 'A');
        slots.put(CoreStatType.DEX, 'D');
        slots.put(CoreStatType.INT, 'I');
        slots.put(CoreStatType.SPI, 'P');
        slots.put(CoreStatType.LUK, 'L');
        slots.put(CoreStatType.CHA, 'C');

        for (final Map.Entry<CoreStatType, Character> entry : slots.entrySet()) {
            final CoreStatType coreStatType = entry.getKey();
            final CoreStatSnapshot statSnapshot =
                buildSnapshot == null ? null : buildSnapshot.statSnapshots().get(coreStatType);
            final Material icon = statsConfig == null
                ? coreStatType.getFallbackIcon()
                : statsConfig.getStatDefinition(coreStatType).icon();
            render.layoutSlot(
                entry.getValue(),
                UnifiedBuilderFactory.item(icon)
                    .setName(this.i18n("stat.name", player)
                        .withPlaceholders(coreStatType.getPlaceholders(player))
                        .build()
                        .component())
                    .setLore(this.i18n("stat.lore", player)
                        .withPlaceholders(Map.of(
                            "stat_name", coreStatType.getDisplayName(player),
                            "allocated_points", statSnapshot == null ? 0 : statSnapshot.allocatedPoints(),
                            "passive_label", statSnapshot == null ? "-" : statSnapshot.passiveLabel(),
                            "passive_value", statSnapshot == null
                                ? "0.00"
                                : String.format(Locale.ROOT, "%.2f", statSnapshot.passiveValue()),
                            "passive_unit", statSnapshot == null ? "" : statSnapshot.passiveUnit()
                        ))
                        .build()
                        .children())
                    .build()
            ).onClick(clickContext -> clickContext.openForPlayer(RaStatView.class, Map.of(
                "plugin", plugin,
                "statType", coreStatType
            )));
        }

        render.layoutSlot(
            'M',
            UnifiedBuilderFactory.item(Material.NETHER_STAR)
                .setName(this.i18n("info.name", player).build().component())
                .setLore(this.i18n("info.lore", player)
                    .withPlaceholders(Map.of(
                        "total_power", totalPower,
                        "enabled_skills", enabledSkills,
                        "earned_points", buildSnapshot == null ? 0 : buildSnapshot.earnedPoints(),
                        "unspent_points", buildSnapshot == null ? 0 : buildSnapshot.unspentPoints(),
                        "current_mana", buildSnapshot == null ? 0 : buildSnapshot.currentMana(),
                        "max_mana", buildSnapshot == null ? 0 : buildSnapshot.maxMana()
                    ))
                    .build()
                    .children())
                .build()
        );

        render.layoutSlot(
            'K',
            UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("skills.name", player).build().component())
                .setLore(this.i18n("skills.lore", player)
                    .withPlaceholders(Map.of("enabled_skills", enabledSkills))
                    .build()
                    .children())
                .build()
        ).onClick(clickContext -> clickContext.openForPlayer(RaSkillsView.class, Map.of("plugin", plugin)));

        render.layoutSlot(
            'P',
            UnifiedBuilderFactory.item(Material.COMPARATOR)
                .setName(this.i18n("settings.name", player).build().component())
                .setLore(this.i18n("settings.lore", player).build().children())
                .build()
        ).onClick(clickContext -> clickContext.openForPlayer(RaStatSettingsView.class, Map.of("plugin", plugin)));
    }
}
