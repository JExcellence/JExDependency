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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Paginated town-member permissions entry view.
 *
 * <p>This view lists all town members. Left click opens {@link RolePlayerPermissionView} and
 * right click opens {@link RoleAssignmentPlayerPermissionView} for the clicked player.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RolePermissionView extends APaginatedView<RDTPlayer> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the role-permission entry view and enables return navigation.
     */
    public RolePermissionView() {
        super(RolesOverviewView.class);
    }

    /**
     * Returns the translation key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "role_permission_ui";
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
     * Loads town members asynchronously for pagination.
     *
     * @param context view context
     * @return future member list
     */
    @Override
    protected CompletableFuture<List<RDTPlayer>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Executor executor = plugin.getExecutor();
        if (executor == null) {
            return CompletableFuture.supplyAsync(() -> this.resolveSortedMembers(context));
        }
        return CompletableFuture.supplyAsync(() -> this.resolveSortedMembers(context), executor);
    }

    /**
     * Renders a town member entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry member entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RDTPlayer entry
    ) {
        final Player viewer = context.getPlayer();
        final String memberName = this.resolvePlayerName(entry.getIdentifier());
        final String roleId = entry.getTownRoleId() == null ? "-" : entry.getTownRoleId();

        final ItemStack item = UnifiedBuilderFactory.item(Material.NAME_TAG)
                .setName(this.i18n("player.name", viewer)
                        .withPlaceholders(Map.of(
                                "player_name", memberName,
                                "player_uuid", entry.getIdentifier()
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("player.lore", viewer)
                        .withPlaceholders(Map.of(
                                "player_name", memberName,
                                "player_uuid", entry.getIdentifier(),
                                "role_id", roleId
                        ))
                        .build()
                        .children())
                .build();

        builder.withItem(item)
                .onClick(click -> this.handleMemberClick(click, entry));
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

    private @NotNull List<RDTPlayer> resolveSortedMembers(final @NotNull Context context) {
        final RTown town = this.resolveTown(context, context.getPlayer());
        if (town == null) {
            return List.of();
        }

        return town.getMembers().stream()
                .sorted(Comparator.comparing(member -> member.getIdentifier().toString()))
                .toList();
    }

    private void handleMemberClick(
            final @NotNull SlotClickContext click,
            final @NotNull RDTPlayer member
    ) {
        click.setCancelled(true);
        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_ROLES)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        if (click.getClickOrigin().isRightClick()) {
            click.openForPlayer(
                    RoleAssignmentPlayerPermissionView.class,
                    Map.of(
                            "plugin", plugin,
                            "town_uuid", town.getIdentifier(),
                            "target_player_uuid", member.getIdentifier()
                    )
            );
            return;
        }

        click.openForPlayer(
                RolePlayerPermissionView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "target_player_uuid", member.getIdentifier()
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
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_ROLES)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_ROLES.getPermissionKey())
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
