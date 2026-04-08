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
import com.raindropcentral.rdt.database.entity.TownInvite;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated inbox for town invites sent to the current player.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownInvitesView extends APaginatedView<TownInvite> {

    private final State<RDT> plugin = initialState("plugin");

    /**
     * Creates the invite inbox view.
     */
    public TownInvitesView() {
        super(TownHubView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_invites_ui";
    }

    /**
     * Loads active invites for the viewing player.
     *
     * @param context current view context
     * @return async invite list
     */
    @Override
    protected @NotNull CompletableFuture<List<TownInvite>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT rdt = this.plugin.get(context);
        return CompletableFuture.completedFuture(
            rdt.getTownRuntimeService() == null
                ? List.of()
                : rdt.getTownRuntimeService().getActiveInvites(context.getPlayer().getUniqueId())
        );
    }

    /**
     * Renders one invite inbox card.
     *
     * @param context current context
     * @param builder item builder
     * @param index entry index
     * @param entry invite entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull TownInvite entry
    ) {
        builder.withItem(this.createInviteItem(context.getPlayer(), entry))
            .onClick(clickContext -> this.handleInviteClick(clickContext, entry));
    }

    /**
     * Renders the inbox summary item.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final int inviteCount = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        render.slot(4).renderWith(() -> this.createSummaryItem(player, inviteCount));
        if (inviteCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
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

    private void handleInviteClick(final @NotNull SlotClickContext clickContext, final @NotNull TownInvite invite) {
        final RDT rdt = this.plugin.get(clickContext);
        final boolean accepted = rdt.getTownRuntimeService() != null && rdt.getTownRuntimeService().acceptInvite(clickContext.getPlayer(), invite);
        if (!accepted) {
            new I18n.Builder("town_invites_ui.accept_failed", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
        } else {
            new I18n.Builder("town_invites_ui.accepted", clickContext.getPlayer())
                .includePrefix()
                .withPlaceholder("town_name", invite.getTown().getTownName())
                .build()
                .sendMessage();
        }
        clickContext.openForPlayer(TownInvitesView.class, Map.of("plugin", rdt));
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final int inviteCount) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("invite_count", inviteCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInviteItem(final @NotNull Player player, final @NotNull TownInvite invite) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("town_name", invite.getTown().getTownName())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", invite.getTown().getTownName(),
                    "mayor_uuid", invite.getTown().getMayorUUID().toString()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
