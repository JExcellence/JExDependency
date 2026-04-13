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

import com.raindropcentral.rda.ManaDisplayMode;
import com.raindropcentral.rda.PlayerBuildSnapshot;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

/**
 * Settings root for mana HUD and active-trigger preferences.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class RaStatSettingsView extends BaseView {

    private final State<RDA> rda = initialState("plugin");

    /**
     * Creates the settings menu.
     */
    public RaStatSettingsView() {
        super();
    }

    @Override
    protected String getKey() {
        return "ra_settings_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "   I M   ",
            "         ",
            "   T     ",
            "         ",
            "X        "
        };
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDA plugin = this.rda.get(render);
        final PlayerBuildSnapshot buildSnapshot = plugin == null ? null : plugin.getBuildSnapshot(player);
        final ManaDisplayMode manaDisplayMode =
            buildSnapshot == null ? ManaDisplayMode.ACTION_BAR : buildSnapshot.manaDisplayMode();
        final String manaModeName = manaDisplayMode.getDisplayName(player);

        render.layoutSlot(
            'I',
            UnifiedBuilderFactory.item(Material.NETHER_STAR)
                .setName(this.i18n("info.name", player).build().component())
                .setLore(this.i18n("info.lore", player)
                    .withPlaceholders(Map.of(
                        "current_mana", buildSnapshot == null
                            ? "0.0"
                            : String.format(Locale.ROOT, "%.1f", buildSnapshot.currentMana()),
                        "max_mana", buildSnapshot == null
                            ? "0.0"
                            : String.format(Locale.ROOT, "%.1f", buildSnapshot.maxMana()),
                        "mana_mode", manaModeName
                    ))
                    .build()
                    .children())
                .build()
        );

        render.layoutSlot(
            'M',
            UnifiedBuilderFactory.item(Material.HEART_OF_THE_SEA)
                .setName(this.i18n("mana.name", player).build().component())
                .setLore(this.i18n("mana.lore", player)
                    .withPlaceholders(Map.of("mana_mode", manaModeName))
                    .build()
                    .children())
                .build()
        ).onClick(clickContext -> {
            if (plugin == null || plugin.getPlayerBuildService() == null) {
                return;
            }
            if (!plugin.openManaBossBarSettings(player)) {
                plugin.getPlayerBuildService().cycleManaDisplayMode(player);
                clickContext.update();
            }
        });

        render.layoutSlot(
            'T',
            UnifiedBuilderFactory.item(Material.REDSTONE_TORCH)
                .setName(this.i18n("triggers.name", player).build().component())
                .setLore(this.i18n("triggers.lore", player).build().children())
                .build()
        ).onClick(clickContext -> clickContext.openForPlayer(RaTriggerSettingsView.class, Map.of(
            "plugin", plugin
        )));
    }
}
