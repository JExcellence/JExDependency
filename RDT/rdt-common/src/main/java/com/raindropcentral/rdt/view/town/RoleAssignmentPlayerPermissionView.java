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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Paginated role-assignment view for a selected town player.
 *
 * <p>Shows all assignable town roles except the built-in public and restricted roles. Clicking a role updates the
 * selected player's role ID and synchronizes the player's explicit permissions to that role.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RoleAssignmentPlayerPermissionView extends APaginatedView<TownRole> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<UUID> targetPlayerUuid = initialState("target_player_uuid");

    /**
     * Creates the role-assignment view and enables return navigation.
     */
    public RoleAssignmentPlayerPermissionView() {
        super(RolePermissionView.class);
    }

    /**
     * Returns the translation key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "role_assignment_player_permission_ui";
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
     * Verifies viewer and target-player context before rendering.
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
     * Loads assignable town roles asynchronously for pagination.
     *
     * @param context view context
     * @return future assignable role list
     */
    @Override
    protected CompletableFuture<List<TownRole>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Executor executor = plugin.getExecutor();
        if (executor == null) {
            return CompletableFuture.supplyAsync(() -> this.resolveAssignableRoles(context));
        }
        return CompletableFuture.supplyAsync(() -> this.resolveAssignableRoles(context), executor);
    }

    /**
     * Renders an individual assignable role entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry town role entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TownRole entry
    ) {
        final Player viewer = context.getPlayer();
        final RDTPlayer target = this.resolveTargetTownMember(context, viewer);
        final boolean selected = target != null
                && target.getTownRoleId() != null
                && RTown.normalizeRoleId(target.getTownRoleId()).equals(RTown.normalizeRoleId(entry.getRoleId()));

        final Material material = selected ? Material.LIME_DYE : Material.PAPER;
        final String selectedState = this.toPlain(
                viewer,
                selected ? "role.state.selected" : "role.state.not_selected"
        );

        builder.withItem(
                UnifiedBuilderFactory.item(material)
                        .setName(this.i18n("role.name", viewer)
                                .withPlaceholders(Map.of(
                                        "role_name", entry.getRoleName(),
                                        "role_id", entry.getRoleId(),
                                        "selected_state", selectedState
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("role.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "role_id", entry.getRoleId(),
                                        "role_name", entry.getRoleName(),
                                        "permission_count", entry.getPermissions().size(),
                                        "selected_state", selectedState
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleRoleAssignment(click, entry));
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

    private void handleRoleAssignment(
            final @NotNull SlotClickContext click,
            final @NotNull TownRole role
    ) {
        click.setCancelled(true);
        final Player viewer = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getPlayerRepository() == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, viewer);
        if (town == null) {
            this.i18n("error.town_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, viewer);
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_ROLES)) {
            this.i18n("error.no_permission", viewer)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        if (RTown.PUBLIC_ROLE_ID.equals(RTown.normalizeRoleId(role.getRoleId()))) {
            this.i18n("message.cannot_assign_public", viewer).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer target = this.resolveTargetTownMember(click, viewer);
        if (target == null) {
            this.i18n("error.player_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final String previousRoleId = target.getTownRoleId();
        target.setTownRoleId(role.getRoleId());
        target.syncTownPermissionsFromRole(role);
        plugin.getPlayerRepository().update(target);
        if (previousRoleId == null || !RTown.normalizeRoleId(previousRoleId).equals(RTown.normalizeRoleId(role.getRoleId()))) {
            town.recordPlayerRoleChanged(target.getIdentifier(), previousRoleId, role.getRoleId());
            plugin.getTownRepository().update(town);
        }

        this.i18n("message.assigned", viewer)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "role_id", role.getRoleId(),
                        "role_name", role.getRoleName(),
                        "player_uuid", target.getIdentifier()
                ))
                .build()
                .sendMessage();

        click.openForPlayer(
                RoleAssignmentPlayerPermissionView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "target_player_uuid", target.getIdentifier()
                )
        );
    }

    private @NotNull List<TownRole> resolveAssignableRoles(final @NotNull Context context) {
        final RTown town = this.resolveTown(context, context.getPlayer());
        if (town == null) {
            return List.of();
        }

        return town.getRoles().stream()
                .filter(role -> {
                    final String roleId = RTown.normalizeRoleId(role.getRoleId());
                    return !RTown.PUBLIC_ROLE_ID.equals(roleId)
                            && !RTown.RESTRICTED_ROLE_ID.equals(roleId);
                })
                .sorted(Comparator.comparing(TownRole::getRoleId))
                .toList();
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player viewer
    ) {
        final RTown town = this.resolveTown(context, viewer);
        if (town == null) {
            this.i18n("error.town_unavailable", viewer).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, viewer);
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_ROLES)) {
            this.i18n("error.no_permission", viewer)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            return false;
        }

        final RDTPlayer target = this.resolveTargetTownMember(context, viewer);
        if (target == null) {
            this.i18n("error.player_unavailable", viewer).includePrefix().build().sendMessage();
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

    private @Nullable UUID resolveTargetPlayerUuid(final @NotNull Context context) {
        try {
            return this.targetPlayerUuid.get(context);
        } catch (final Exception ignored) {
            return null;
        }
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

    private @Nullable RDTPlayer resolveTargetTownMember(
            final @NotNull Context context,
            final @NotNull Player viewer
    ) {
        final RTown town = this.resolveTown(context, viewer);
        if (town == null) {
            return null;
        }

        final UUID targetUuid = this.resolveTargetPlayerUuid(context);
        if (targetUuid == null) {
            return null;
        }

        for (final RDTPlayer member : town.getMembers()) {
            if (targetUuid.equals(member.getIdentifier())) {
                return member;
            }
        }
        return null;
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
