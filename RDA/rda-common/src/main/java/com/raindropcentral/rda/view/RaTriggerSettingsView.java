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

import com.raindropcentral.rda.ActivationMode;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated trigger-preference menu for skill actives.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class RaTriggerSettingsView extends APaginatedView<SkillType> {

    private final State<RDA> rda = initialState("plugin");

    /**
     * Creates the trigger settings menu.
     */
    public RaTriggerSettingsView() {
        super(RaStatSettingsView.class);
    }

    @Override
    protected String getKey() {
        return "ra_trigger_settings_ui";
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
        if (plugin == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.completedFuture(
            plugin.getEnabledSkills().stream()
                .filter(skillType -> {
                    final SkillConfig skillConfig = plugin.getSkillConfig(skillType);
                    return skillConfig != null && skillConfig.getActiveAbility() != null;
                })
                .toList()
        );
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull SkillType entry
    ) {
        final Player player = context.getPlayer();
        final RDA plugin = this.rda.get(context);
        final SkillConfig skillConfig = plugin == null ? null : plugin.getSkillConfig(entry);
        final ActivationMode activationMode = plugin == null || plugin.getPlayerBuildService() == null
            ? ActivationMode.COMMAND
            : plugin.getPlayerBuildService().getActivationMode(player, entry);
        final List<ActivationMode> allowedModes = plugin == null || plugin.getPlayerBuildService() == null
            ? List.of(ActivationMode.COMMAND)
            : plugin.getPlayerBuildService().getAllowedActivationModesForSkill(entry);
        final String allowedModeText = allowedModes.stream()
            .map(mode -> mode.getDisplayName(player))
            .reduce((left, right) -> left + ", " + right)
            .orElse("-");

        builder.withItem(
            UnifiedBuilderFactory.item(skillConfig == null ? entry.getFallbackIcon() : skillConfig.getDisplayIcon())
                .setName(this.i18n("skill.name", player)
                    .withPlaceholders(entry.getPlaceholders(player))
                    .build()
                    .component())
                .setLore(this.i18n("skill.lore", player)
                    .withPlaceholders(Map.of(
                        "skill_name", entry.getDisplayName(player),
                        "active_name", skillConfig == null || skillConfig.getActiveAbility() == null
                            ? "-"
                            : skillConfig.getActiveAbility().name(),
                        "activation_mode", activationMode.getDisplayName(player),
                        "allowed_modes", allowedModeText
                    ))
                    .build()
                    .children())
                .build()
        ).onClick(clickContext -> {
            if (plugin == null || plugin.getPlayerBuildService() == null) {
                return;
            }
            plugin.getPlayerBuildService().cycleActivationMode(player, entry);
            clickContext.update();
        });
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        render.layoutSlot(
            'I',
            UnifiedBuilderFactory.item(Material.REDSTONE)
                .setName(this.i18n("info.name", player).build().component())
                .setLore(this.i18n("info.lore", player).build().children())
                .build()
        );
    }
}
