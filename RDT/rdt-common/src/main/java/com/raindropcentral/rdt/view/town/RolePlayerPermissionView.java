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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated permission toggle view for a selected town member.
 *
 * <p>Each entry represents a {@link TownPermissions} flag and toggles that permission for the
 * selected member.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RolePlayerPermissionView extends APaginatedView<TownPermissions> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final State<UUID> targetPlayerUuid = initialState("target_player_uuid");

    /**
     * Creates the per-player permission toggle view and enables return navigation.
     */
    public RolePlayerPermissionView() {
        super(RolePermissionView.class);
    }

    /**
     * Returns the translation key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "role_player_permission_ui";
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
     * Loads all town permissions for pagination.
     *
     * @param context view context
     * @return future enum entries
     */
    @Override
    protected CompletableFuture<List<TownPermissions>> getAsyncPaginationSource(final @NotNull Context context) {
        final List<TownPermissions> permissions = Arrays.stream(TownPermissions.values())
                .sorted(Comparator.comparing(TownPermissions::name))
                .toList();
        return CompletableFuture.completedFuture(permissions);
    }

    /**
     * Renders an individual permission toggle entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry permission enum value
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TownPermissions entry
    ) {
        final Player viewer = context.getPlayer();
        final RDTPlayer target = this.resolveTargetTownMember(context, viewer);
        if (target == null) {
            builder.withItem(
                    UnifiedBuilderFactory.item(Material.BARRIER)
                            .setName(this.i18n("permission.unavailable.name", viewer).build().component())
                            .setLore(this.i18n("permission.unavailable.lore", viewer).build().children())
                            .build()
            );
            return;
        }

        final boolean enabled = target.hasTownPermission(entry);
        final Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        final String enabledState = this.toPlain(
                viewer,
                enabled ? "permission.state.enabled" : "permission.state.disabled"
        );

        builder.withItem(
                UnifiedBuilderFactory.item(material)
                        .setName(this.i18n("permission.name", viewer)
                                .withPlaceholders(Map.of(
                                        "permission", entry.getPermissionKey(),
                                        "state", enabledState
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("permission.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "permission", entry.getPermissionKey(),
                                        "default_role", entry.getDefaultRoleId(),
                                        "state", enabledState
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handlePermissionClick(click, entry));
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

    private void handlePermissionClick(
            final @NotNull SlotClickContext click,
            final @NotNull TownPermissions permission
    ) {
        click.setCancelled(true);
        final Player viewer = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getPlayerRepository() == null) {
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

        final RDTPlayer target = this.resolveTargetTownMember(click, viewer);
        if (target == null) {
            this.i18n("error.player_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final boolean enabled = target.toggleTownPermission(permission);
        plugin.getPlayerRepository().update(target);

        final String targetName = this.resolvePlayerName(target.getIdentifier());
        this.i18n("message.toggled", viewer)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "permission", permission.getPermissionKey(),
                        "player_name", targetName,
                        "state", this.toPlain(viewer, enabled ? "permission.state.enabled" : "permission.state.disabled")
                ))
                .build()
                .sendMessage();

        click.openForPlayer(
                RolePlayerPermissionView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "target_player_uuid", target.getIdentifier()
                )
        );
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

    private @NotNull String resolvePlayerName(final @NotNull UUID playerUuid) {
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        if (offlinePlayer.getName() == null || offlinePlayer.getName().isBlank()) {
            return playerUuid.toString();
        }
        return offlinePlayer.getName();
    }

    private @NotNull String toPlain(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return PlainTextComponentSerializer.plainText().serialize(this.i18n(key, player).build().component());
    }
}
