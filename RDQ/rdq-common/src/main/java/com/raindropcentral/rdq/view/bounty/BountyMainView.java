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

package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Main entry point for the Bounty UI.
 * Compact 3-row layout with view and create buttons.
 *
 * @author JExcellence
 * @version 1.1.0
 */
public class BountyMainView extends BaseView {

    private final State<RDQ> rdq = initialState("plugin");

    @Override
    protected String getKey() {
        return "bounty_overview_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "   V C   "
        };
    }

    @Override
    protected int getSize() {
        return 1;
    }

    /**
     * Executes onFirstRender.
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        renderViewBountiesButton(render, player);
        renderCreateBountyButton(render, player);
    }

    private void renderViewBountiesButton(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        render.layoutSlot('V', UnifiedBuilderFactory
            .item(Material.DIAMOND_SWORD)
            .setName(this.i18n("view_bounties.name", player).build().component())
            .setLore(this.i18n("view_bounties.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(
            BountyOverviewView.class,
            Map.of("plugin", this.rdq.get(ctx))
        ));
    }

    private void renderCreateBountyButton(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        render.layoutSlot('C', UnifiedBuilderFactory
            .item(Material.EMERALD)
            .setName(this.i18n("create_bounty.name", player).build().component())
            .setLore(this.i18n("create_bounty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(
            BountyCreationView.class,
            Map.of(
                "plugin", this.rdq.get(render),
                "target", Optional.empty(),
                "rewards", new ArrayList<>(),
                "bounty", Optional.empty(),
                "insertedItems", new HashMap<>()
            )
        ));
    }
}
