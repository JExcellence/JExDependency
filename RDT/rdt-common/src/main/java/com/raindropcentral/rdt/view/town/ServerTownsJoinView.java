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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated town-join request view for unaffiliated players.
 *
 * <p>Clicking a town creates a persistent join request. Requests are auto-declined when the town's
 * public role does not include {@link TownPermissions#JOIN_TOWN}, unless the player has an invite
 * from that town.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ServerTownsJoinView extends APaginatedView<RTown> {

    private final State<RDT> rdt = initialState("plugin");

    /**
     * Creates the town-join view and enables return navigation to main overview.
     */
    public ServerTownsJoinView() {
        super(com.raindropcentral.rdt.view.main.MainOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "server_towns_join_ui";
    }

    /**
     * Cancels default inventory movement behavior in this view.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Verifies that the viewer is not currently in a town before rendering.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        if (!this.verifyViewerAccess(render, player)) {
            player.closeInventory();
            return;
        }
        super.onFirstRender(render, player);
    }

    /**
     * Loads active towns asynchronously for pagination.
     *
     * @param context view context
     * @return future town list
     */
    @Override
    protected @NotNull CompletableFuture<List<RTown>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getTownRepository() == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        return plugin.getTownRepository()
                .findAllByAttributesAsync(Map.of("active", true))
                .thenApply(towns -> towns.stream()
                        .sorted(Comparator.comparing(RTown::getTownName))
                        .toList());
    }

    /**
     * Renders a join-request town entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry town entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RTown entry
    ) {
        final Player viewer = context.getPlayer();
        final boolean invited = entry.isPlayerInvited(viewer.getUniqueId());
        final boolean publicJoinEnabled = entry.hasRolePermission(RTown.PUBLIC_ROLE_ID, TownPermissions.JOIN_TOWN);
        final boolean requestAllowed = invited || publicJoinEnabled;
        final String stateKey;
        if (invited) {
            stateKey = "town.state.invited";
        } else if (requestAllowed) {
            stateKey = "town.state.open";
        } else {
            stateKey = "town.state.closed";
        }

        final Material material;
        if (invited) {
            material = Material.GOLD_INGOT;
        } else if (requestAllowed) {
            material = Material.LIME_DYE;
        } else {
            material = Material.BARRIER;
        }

        builder.withItem(
                UnifiedBuilderFactory.item(material)
                        .setName(this.i18n("town.name", viewer)
                                .withPlaceholders(Map.of(
                                        "town_name", entry.getTownName(),
                                        "state", this.toPlain(viewer, stateKey)
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("town.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "town_name", entry.getTownName(),
                                        "state", this.toPlain(viewer, stateKey),
                                        "permission", TownPermissions.JOIN_TOWN.getPermissionKey()
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleJoinRequestClick(click, entry));
    }

    /**
     * Renders additional fixed controls for this paginated view.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
    }

    private void handleJoinRequestClick(
            final @NotNull SlotClickContext click,
            final @NotNull RTown entry
    ) {
        click.setCancelled(true);
        final Player viewer = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null || plugin.getPlayerRepository() == null) {
            this.i18n("error.system_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        if (!this.isViewerUnaffiliated(plugin.getPlayerRepository(), viewer)) {
            this.i18n("error.already_in_town", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = plugin.getTownRepository().findByTownUUID(entry.getIdentifier());
        if (town == null) {
            this.i18n("error.town_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        if (town.hasPendingJoinRequest(viewer.getUniqueId())) {
            this.i18n("message.already_pending", viewer)
                    .includePrefix()
                    .withPlaceholder("town_name", town.getTownName())
                    .build()
                    .sendMessage();
            return;
        }

        final boolean requestAllowed = town.hasRolePermission(RTown.PUBLIC_ROLE_ID, TownPermissions.JOIN_TOWN)
                || town.isPlayerInvited(viewer.getUniqueId());
        if (!requestAllowed) {
            this.i18n("message.auto_declined", viewer)
                    .includePrefix()
                    .withPlaceholder("town_name", town.getTownName())
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.addPendingJoinRequest(viewer.getUniqueId())) {
            this.i18n("message.already_pending", viewer)
                    .includePrefix()
                    .withPlaceholder("town_name", town.getTownName())
                    .build()
                    .sendMessage();
            return;
        }

        plugin.getTownRepository().update(town);
        this.i18n("message.requested", viewer)
                .includePrefix()
                .withPlaceholder("town_name", town.getTownName())
                .build()
                .sendMessage();
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        if (!this.isViewerUnaffiliated(plugin.getPlayerRepository(), player)) {
            this.i18n("error.already_in_town", player).includePrefix().build().sendMessage();
            return false;
        }
        return true;
    }

    private boolean isViewerUnaffiliated(
            final @NotNull RRDTPlayer playerRepository,
            final @NotNull Player player
    ) {
        final RDTPlayer playerRecord = playerRepository.findByPlayer(player.getUniqueId());
        return playerRecord == null || playerRecord.getTownUUID() == null;
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull String toPlain(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return PlainTextComponentSerializer.plainText().serialize(this.i18n(key, player).build().component());
    }
}
