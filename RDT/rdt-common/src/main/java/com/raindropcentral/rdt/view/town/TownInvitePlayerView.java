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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Paginated town invite view that lists players without a town.
 *
 * <p>Entries show invite status per player. Clicking a non-invited player persists a town invite
 * when the viewer has {@link TownPermissions#TOWN_INVITE}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownInvitePlayerView extends APaginatedView<UUID> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the invite view and enables return navigation to main overview.
     */
    public TownInvitePlayerView() {
        super(com.raindropcentral.rdt.view.main.MainOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_invite_player_ui";
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
     * Loads online players without a town for pagination.
     *
     * @param context view context
     * @return future player UUID list
     */
    @Override
    protected @NotNull CompletableFuture<List<UUID>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Executor executor = plugin.getExecutor();
        if (executor == null) {
            return CompletableFuture.supplyAsync(() -> this.resolveInviteCandidates(context));
        }
        return CompletableFuture.supplyAsync(() -> this.resolveInviteCandidates(context), executor);
    }

    /**
     * Renders a player invite entry.
     *
     * @param context context
     * @param builder builder
     * @param index index
     * @param entry target player UUID
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull UUID entry
    ) {
        final Player viewer = context.getPlayer();
        final RTown town = this.resolveTown(context, viewer);
        final boolean invited = town != null && town.isPlayerInvited(entry);
        final Material material = invited ? Material.LIME_DYE : Material.NAME_TAG;
        final String invitedState = this.toPlain(
                viewer,
                invited ? "player.state.invited" : "player.state.not_invited"
        );

        builder.withItem(
                UnifiedBuilderFactory.item(material)
                        .setName(this.i18n("player.name", viewer)
                                .withPlaceholders(Map.of(
                                        "player_name", this.resolvePlayerName(entry),
                                        "player_uuid", entry,
                                        "invite_state", invitedState
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("player.lore", viewer)
                                .withPlaceholders(Map.of(
                                        "player_name", this.resolvePlayerName(entry),
                                        "player_uuid", entry,
                                        "invite_state", invitedState
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleInviteClick(click, entry));
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

    private @NotNull List<UUID> resolveInviteCandidates(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return List.of();
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return List.of();
        }

        final UUID viewerUuid = context.getPlayer().getUniqueId();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .filter(playerUuid -> !playerUuid.equals(viewerUuid))
                .filter(playerUuid -> this.isPlayerUnaffiliated(playerRepository, playerUuid))
                .sorted(Comparator.comparing(this::resolvePlayerName))
                .toList();
    }

    private void handleInviteClick(
            final @NotNull SlotClickContext click,
            final @NotNull UUID targetPlayerUuid
    ) {
        click.setCancelled(true);
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

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            this.i18n("error.system_unavailable", viewer).includePrefix().build().sendMessage();
            return;
        }

        final String targetName = this.resolvePlayerName(targetPlayerUuid);
        if (!this.isPlayerUnaffiliated(playerRepository, targetPlayerUuid)) {
            this.i18n("message.target_in_town", viewer)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "player_name", targetName,
                            "player_uuid", targetPlayerUuid
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.invitePlayer(targetPlayerUuid)) {
            this.i18n("message.already_invited", viewer)
                    .includePrefix()
                    .withPlaceholders(Map.of(
                            "player_name", targetName,
                            "player_uuid", targetPlayerUuid
                    ))
                    .build()
                    .sendMessage();
            return;
        }

        plugin.getTownRepository().update(town);
        this.i18n("message.invited", viewer)
                .includePrefix()
                .withPlaceholders(Map.of(
                        "player_name", targetName,
                        "player_uuid", targetPlayerUuid,
                        "town_name", town.getTownName()
                ))
                .build()
                .sendMessage();

        click.openForPlayer(
                TownInvitePlayerView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
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

    private boolean isPlayerUnaffiliated(
            final @NotNull RRDTPlayer playerRepository,
            final @NotNull UUID playerUuid
    ) {
        final RDTPlayer targetRecord = playerRepository.findByPlayer(playerUuid);
        return targetRecord == null || targetRecord.getTownUUID() == null;
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

    private @NotNull String toPlain(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return PlainTextComponentSerializer.plainText().serialize(this.i18n(key, player).build().component());
    }
}
