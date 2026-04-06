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

package com.raindropcentral.rdt.view.main;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.view.town.CreateTownColorAnvilView;
import com.raindropcentral.rdt.view.town.CreateTownConfirmView;
import com.raindropcentral.rdt.view.town.CreateTownNameAnvilView;
import com.raindropcentral.rdt.view.town.TownDirectoryView;
import com.raindropcentral.rdt.view.town.TownInvitesView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import com.raindropcentral.rdt.utils.TownOverviewAccessMode;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
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

import java.util.Map;

/**
 * Root GUI hub for all player-facing town actions launched from {@code /rt main}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownHubView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");

    /**
     * Creates the root town hub.
     */
    public TownHubView() {
        super();
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_hub_ui";
    }

    /**
     * Returns the menu layout.
     *
     * @return hub layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   m d   ",
            "   i c   ",
            "         ",
            "         ",
            "         "
        };
    }

    /**
     * Continues the town-creation flow when the player returns from an anvil step.
     *
     * @param origin previous context
     * @param target current hub context
     */
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> data = this.extractData(target) != null
            ? this.extractData(target)
            : this.extractData(origin);
        if (data == null) {
            target.update();
            return;
        }

        final Object townName = data.get("draftTownName");
        final Object townColor = data.get("draftTownColor");
        if (townName instanceof String draftTownName && !(townColor instanceof String)) {
            target.openForPlayer(
                CreateTownColorAnvilView.class,
                Map.of(
                    "plugin", this.plugin.get(target),
                    "draftTownName", draftTownName
                )
            );
            return;
        }

        if (townName instanceof String draftTownName && townColor instanceof String draftTownColor) {
            target.openForPlayer(
                CreateTownConfirmView.class,
                Map.of(
                    "plugin", this.plugin.get(target),
                    "draftTownName", draftTownName,
                    "draftTownColor", draftTownColor
                )
            );
            return;
        }

        target.update();
    }

    /**
     * Renders the root navigation options for town browsing and creation.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDT rdt = this.plugin.get(render);
        final RTown playerTown = rdt.getTownRuntimeService() == null
            ? null
            : rdt.getTownRuntimeService().getTownFor(player.getUniqueId());
        final int inviteCount = rdt.getTownRuntimeService() == null
            ? 0
            : rdt.getTownRuntimeService().getActiveInvites(player.getUniqueId()).size();

        render.layoutSlot('s', this.createSummaryItem(player, playerTown, inviteCount));
        render.layoutSlot('m', playerTown == null ? this.createLockedTownItem(player) : this.createMyTownItem(player, playerTown))
            .onClick(clickContext -> {
                if (playerTown == null) {
                    clickContext.setCancelled(true);
                    return;
                }
                clickContext.openForPlayer(
                    TownOverviewView.class,
                    Map.of(
                        "plugin", rdt,
                        "town_uuid", playerTown.getTownUUID(),
                        "access_mode", TownOverviewAccessMode.REMOTE
                    )
                );
            });
        render.layoutSlot('d', this.createDirectoryItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownDirectoryView.class,
                Map.of("plugin", rdt)
            ));
        render.layoutSlot('i', this.createInvitesItem(player, inviteCount))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownInvitesView.class,
                Map.of("plugin", rdt)
            ));
        render.layoutSlot('c', playerTown == null ? this.createCreateTownItem(player) : this.createCreationLockedItem(player))
            .onClick(clickContext -> {
                if (playerTown != null) {
                    clickContext.setCancelled(true);
                    return;
                }
                clickContext.openForPlayer(
                    CreateTownNameAnvilView.class,
                    Map.of("plugin", rdt)
                );
            });
    }

    /**
     * Cancels vanilla inventory interaction for the menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @Nullable Map<String, Object> extractData(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> rawMap)) {
            return null;
        }
        final java.util.Map<String, Object> copied = new java.util.LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @Nullable RTown playerTown,
        final int inviteCount
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", playerTown == null ? "none" : playerTown.getTownName(),
                    "invite_count", inviteCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMyTownItem(final @NotNull Player player, final @NotNull RTown town) {
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("my_town.name", player).build().component())
            .setLore(this.i18n("my_town.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "chunk_count", town.getChunks().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("my_town.locked.name", player).build().component())
            .setLore(this.i18n("my_town.locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createDirectoryItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.MAP)
            .setName(this.i18n("directory.name", player).build().component())
            .setLore(this.i18n("directory.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInvitesItem(final @NotNull Player player, final int inviteCount) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("invites.name", player).build().component())
            .setLore(this.i18n("invites.lore", player)
                .withPlaceholder("invite_count", inviteCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCreateTownItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.REINFORCED_DEEPSLATE)
            .setName(this.i18n("create.name", player).build().component())
            .setLore(this.i18n("create.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCreationLockedItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_DYE)
            .setName(this.i18n("create.locked.name", player).build().component())
            .setLore(this.i18n("create.locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
