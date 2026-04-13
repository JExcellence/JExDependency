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

import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProfileSnapshot;
import com.raindropcentral.rda.SkillType;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated skill overview menu for Raindrop Abilities.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class RaSkillsView extends APaginatedView<SkillType> {

    private final State<RDA> rda = initialState("plugin");

    /**
     * Creates the skills overview menu.
     */
    public RaSkillsView() {
        super(RaMainView.class);
    }

    @Override
    protected String getKey() {
        return "ra_skills_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    I    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  <p>    "
        };
    }

    @Override
    protected CompletableFuture<List<SkillType>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDA plugin = this.rda.get(context);
        return CompletableFuture.completedFuture(plugin == null ? List.of() : plugin.getEnabledSkills());
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull SkillType skillType
    ) {
        final Player player = context.getPlayer();
        final RDA plugin = this.rda.get(context);
        final SkillProfileSnapshot snapshot = this.getSnapshot(plugin, player, skillType);
        final Material icon = this.getDisplayIcon(plugin, skillType);
        final SkillConfig skillConfig = plugin == null ? null : plugin.getSkillConfig(skillType);

        builder.withItem(
            UnifiedBuilderFactory.item(icon)
                .setName(this.i18n("skill.name", player)
                    .withPlaceholders(skillType.getPlaceholders(player))
                    .build()
                    .component())
                .setLore(this.i18n("skill.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, skillType, Map.of(
                        "level", snapshot.displayLevelText(),
                        "prestige", snapshot.prestige(),
                        "prestige_symbols", snapshot.prestigeSymbols(),
                        "current_xp", snapshot.currentLevelXp(),
                        "xp_to_next", snapshot.xpToNextLevel(),
                        "total_power", snapshot.totalPower(),
                        "rate_count", skillConfig == null ? 0 : skillConfig.getRates().size()
                    )))
                    .build()
                    .children())
                .build()
        ).onClick(clickContext -> clickContext.openForPlayer(RaSkillView.class, Map.of(
            "plugin", plugin,
            "skillType", skillType
        )));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDA plugin = this.rda.get(render);
        final long totalPower = plugin == null ? 0L : plugin.getTotalPower(player);
        final int enabledSkills = plugin == null ? 0 : plugin.getEnabledSkills().size();

        render.layoutSlot(
            'I',
            UnifiedBuilderFactory.item(Material.COMPASS)
                .setName(this.i18n("info.name", player).build().component())
                .setLore(this.i18n("info.lore", player)
                    .withPlaceholders(Map.of(
                        "total_power", totalPower,
                        "enabled_skills", enabledSkills
                    ))
                    .build()
                    .children())
                .build()
        );
    }

    private @NotNull Material getDisplayIcon(final RDA plugin, final @NotNull SkillType skillType) {
        final SkillConfig skillConfig = plugin == null ? null : plugin.getSkillConfig(skillType);
        return skillConfig == null ? skillType.getFallbackIcon() : skillConfig.getDisplayIcon();
    }

    private @NotNull SkillProfileSnapshot getSnapshot(
        final RDA plugin,
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        return plugin == null ? SkillProfileSnapshot.empty(skillType) : plugin.getSkillSnapshot(skillType, player);
    }

    private @NotNull Map<String, Object> mergePlaceholders(
        final @NotNull Player player,
        final @NotNull SkillType skillType,
        final @NotNull Map<String, Object> extraPlaceholders
    ) {
        final LinkedHashMap<String, Object> placeholders = new LinkedHashMap<>(skillType.getPlaceholders(player));
        placeholders.putAll(extraPlaceholders);
        return placeholders;
    }
}
