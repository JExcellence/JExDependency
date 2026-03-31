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
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated server-town listing view.
 *
 * <p>Each town entry is rendered as open or closed based on whether the built-in public role has
 * {@link TownPermissions#TOWN_INFO}. Entries marked as closed represent closed borders for
 * non-members.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
public final class ServerTownsOverviewView extends APaginatedView<RTown> {

    private final State<RDT> rdt = initialState("plugin");

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "server_town_overview";
    }

    /**
     * Returns the inventory row count.
     *
     * @return row count
     */
    @Override
    protected int getSize() {
        return 6;
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
     * Loads active towns asynchronously for pagination.
     *
     * @param context view context
     * @return future list of active towns
     */
    @Override
    protected @NotNull CompletableFuture<List<RTown>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getTownRepository() == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return plugin.getTownRepository().findAllByAttributesAsync(Map.of("active", true));
    }

    /**
     * Renders a town entry with open/closed borders state.
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
            final @NonNull RTown entry
    ) {
        final Player viewer = context.getPlayer();
        final RDT plugin = this.resolvePlugin(context);
        final @Nullable RDTPlayer viewerTownRecord = this.resolveTownPlayer(context, viewer);

        final boolean publicInfoEnabled = entry.hasRolePermission(RTown.PUBLIC_ROLE_ID, TownPermissions.TOWN_INFO);
        final boolean viewerCanOpen = publicInfoEnabled || entry.hasTownPermission(viewerTownRecord, TownPermissions.TOWN_INFO);

        final Material material = publicInfoEnabled ? Material.EMERALD : Material.BARRIER;
        final String stateKey = publicInfoEnabled ? "town.open.state" : "town.closed.state";
        final String nameKey = publicInfoEnabled ? "town.open.name" : "town.closed.name";
        final String loreKey = publicInfoEnabled ? "town.open.lore" : "town.closed.lore";

        builder.withItem(
                UnifiedBuilderFactory.item(material)
                        .setName(this.i18n(nameKey, viewer)
                                .withPlaceholders(Map.of(
                                        "town_name", entry.getTownName(),
                                        "state", this.toPlain(viewer, stateKey)
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n(loreKey, viewer)
                                .withPlaceholders(Map.of(
                                        "town_name", entry.getTownName(),
                                        "state", this.toPlain(viewer, stateKey),
                                        "permission", TownPermissions.TOWN_INFO.getPermissionKey()
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> {
            click.setCancelled(true);
            if (plugin == null) {
                this.i18n("error.system_unavailable", viewer)
                        .includePrefix()
                        .build()
                        .sendMessage();
                return;
            }
            if (!viewerCanOpen) {
                this.i18n("message.closed_borders", viewer)
                        .includePrefix()
                        .withPlaceholder("town_name", entry.getTownName())
                        .build()
                        .sendMessage();
                return;
            }
            click.openForPlayer(
                    TownOverviewView.class,
                    Map.of(
                            "plugin", plugin,
                            "town_uuid", entry.getIdentifier()
                    )
            );
        });
    }

    /**
     * Renders additional fixed controls for this paginated view.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
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

    private @NotNull String toPlain(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(this.i18n(key, player).build().component());
    }
}
