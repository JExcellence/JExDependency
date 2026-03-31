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
import com.raindropcentral.rdt.database.entity.TownRole;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated pending join-request management view for a town.
 *
 * <p>Left-click accepts a join request and assigns the Member role. Right-click declines and
 * removes the pending request.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownPendingJoinView extends APaginatedView<UUID> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the pending-join view and enables return navigation to town overview.
     */
    public TownPendingJoinView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_pending_join_ui";
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
     * Verifies viewer permissions before rendering pagination components.
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
     * Loads pending town join request UUIDs for pagination.
     *
     * @param context view context
     * @return future pending UUID list
     */
    @Override
    protected @NotNull CompletableFuture<List<UUID>> getAsyncPaginationSource(final @NotNull Context context) {
        final RTown town = this.resolveTown(context, context.getPlayer());
        if (town == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<UUID> requests = town.getPendingJoinRequests().stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        return CompletableFuture.completedFuture(requests);
    }

    /**
     * Renders a pending join-request entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry requesting player UUID
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull UUID entry
    ) {
        final Player viewer = context.getPlayer();
        final String targetName = this.resolvePlayerName(entry);
        builder.withItem(
                UnifiedBuilderFactory.item(Material.CLOCK)
                        .setName(this.i18n("request.name", viewer)
                                .withPlaceholders(Map.of(
                                        "player_name", targetName,
                                        "player_uuid", entry
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("request.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "player_name", targetName,
                                        "player_uuid", entry
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleRequestClick(click, entry));
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

    private void handleRequestClick(
            final @NotNull SlotClickContext click,
            final @NotNull UUID targetPlayerUuid
    ) {
        click.setCancelled(true);
        if (click.getClickOrigin().isRightClick()) {
            this.declineJoinRequest(click, targetPlayerUuid);
            return;
        }
        this.acceptJoinRequest(click, targetPlayerUuid);
    }

    private void acceptJoinRequest(
            final @NotNull SlotClickContext click,
            final @NotNull UUID targetPlayerUuid
    ) {
        final Player viewer = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null || plugin.getPlayerRepository() == null) {
            this.i18n("error.system_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, viewer);
        if (town == null) {
            this.i18n("error.town_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, viewer);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_INVITE)) {
            this.i18n("error.no_permission", viewer)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_INVITE.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.hasPendingJoinRequest(targetPlayerUuid)) {
            this.i18n("message.not_pending", viewer)
                    .includePrefix()
                    .withPlaceholder("player_uuid", targetPlayerUuid)
                    .build()
                    .sendMessage();
            return;
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            this.i18n("error.system_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final String targetName = this.resolvePlayerName(targetPlayerUuid);
        RDTPlayer targetPlayer = playerRepository.findByPlayer(targetPlayerUuid);
        if (targetPlayer != null && targetPlayer.getTownUUID() != null) {
            town.removePendingJoinRequest(targetPlayerUuid);
            town.uninvitePlayer(targetPlayerUuid);
            plugin.getTownRepository().update(town);
            this.i18n("message.target_in_town", viewer)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "player_name", targetName,
                            "player_uuid", targetPlayerUuid
                    ))
                    .build()
                    .sendMessage();
            this.reopen(click, plugin, town.getIdentifier());
            return;
        }

        final TownRole memberRole = town.findRoleById(RTown.MEMBER_ROLE_ID);
        if (targetPlayer == null) {
            targetPlayer = new RDTPlayer(targetPlayerUuid, town.getIdentifier(), RTown.MEMBER_ROLE_ID);
            if (memberRole != null) {
                targetPlayer.syncTownPermissionsFromRole(memberRole);
            }
            playerRepository.create(targetPlayer);
        } else {
            targetPlayer.setTownUUID(town.getIdentifier());
            targetPlayer.setTownRoleId(RTown.MEMBER_ROLE_ID);
            if (memberRole != null) {
                targetPlayer.syncTownPermissionsFromRole(memberRole);
            }
            playerRepository.update(targetPlayer);
        }

        town.addMember(targetPlayer);
        town.removePendingJoinRequest(targetPlayerUuid);
        town.uninvitePlayer(targetPlayerUuid);
        town.recordPlayerJoined(targetPlayerUuid, RTown.MEMBER_ROLE_ID);
        plugin.getTownRepository().update(town);

        this.i18n("message.accepted", viewer)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "player_name", targetName,
                        "player_uuid", targetPlayerUuid,
                        "town_name", town.getTownName()
                ))
                .build()
                .sendMessage();

        final Player targetOnline = Bukkit.getPlayer(targetPlayerUuid);
        if (targetOnline != null) {
            this.i18n("message.accepted_target", targetOnline)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "town_name", town.getTownName()
                    ))
                    .build()
                    .sendMessage();
        }

        this.reopen(click, plugin, town.getIdentifier());
    }

    private void declineJoinRequest(
            final @NotNull SlotClickContext click,
            final @NotNull UUID targetPlayerUuid
    ) {
        final Player viewer = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, viewer);
        if (town == null) {
            this.i18n("error.town_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, viewer);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_INVITE)) {
            this.i18n("error.no_permission", viewer)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_INVITE.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.removePendingJoinRequest(targetPlayerUuid)) {
            this.i18n("message.not_pending", viewer)
                    .includePrefix()
                    .withPlaceholder("player_uuid", targetPlayerUuid)
                    .build()
                    .sendMessage();
            return;
        }

        plugin.getTownRepository().update(town);
        this.i18n("message.declined", viewer)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "player_name", this.resolvePlayerName(targetPlayerUuid),
                        "player_uuid", targetPlayerUuid
                ))
                .build()
                .sendMessage();
        this.reopen(click, plugin, town.getIdentifier());
    }

    private void reopen(
            final @NotNull SlotClickContext click,
            final @NotNull RDT plugin,
            final @NotNull UUID townUuid
    ) {
        click.openForPlayer(
                TownPendingJoinView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", townUuid
                )
        );
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RTown town = this.resolveTown(context, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_INVITE)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_INVITE.getPermissionKey())
                    .build()
                    .sendMessage();
            return false;
        }
        return true;
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return null;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }
        return townRepository.findByTownUUID(resolvedTownUuid);
    }

    private @Nullable UUID resolveTownUuid(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        try {
            final UUID explicitTownUuid = this.townUuid.get(context);
            if (explicitTownUuid != null) {
                return explicitTownUuid;
            }
        } catch (final Exception ignored) {
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer townPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (townPlayer == null) {
            return null;
        }
        return townPlayer.getTownUUID();
    }

    private @Nullable RDTPlayer resolveTownPlayer(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return null;
        }
        return plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @NotNull String resolvePlayerName(final @NotNull UUID playerUuid) {
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        if (offlinePlayer.getName() == null || offlinePlayer.getName().isBlank()) {
            return playerUuid.toString();
        }
        return offlinePlayer.getName();
    }
}
