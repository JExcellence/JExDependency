package com.raindropcentral.rdt.view.main;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.view.town.CreateTownAnvilView;
import com.raindropcentral.rdt.view.town.ServerTownsJoinView;
import com.raindropcentral.rdt.view.town.ServerTownsOverviewView;
import com.raindropcentral.rdt.view.town.TownInvitePlayerView;
import com.raindropcentral.rdt.view.town.TownOverviewView;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Main towns overview menu containing create/view, join, invite, and server-town actions.
 *
 * <p>The create/view button is dynamic: it shows create when the viewer is not in a town and
 * switches to view-town when they are already a member.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
public class MainOverviewView extends BaseView {

    private final State<RDT> rdt = initialState("plugin");

    @Override
    protected String getKey() {
        return "main_overview_ui";
    }

    @Override
    protected int getSize() {
        return 1;
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

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(render);
        if (plugin == null) {
            return;
        }

        final UUID playerTownUuid = this.resolvePlayerTownUuid(plugin, player);
        this.initializeCreateTownView(
                render,
                player,
                plugin,
                playerTownUuid
        );
        this.initializeJoinOrInviteView(render, player, plugin, playerTownUuid);

        this.initializeServerTownView(
                render,
                player
        );
    }

    private void initializeCreateTownView(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final @NotNull RDT plugin,
            final @Nullable UUID playerTownUuid
    ) {
        if (playerTownUuid != null) {
            this.initializeViewTownAction(context, player, plugin, playerTownUuid);
            return;
        }

        this.initializeCreateTownAction(context, player, plugin);
    }

    private void initializeJoinOrInviteView(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final @NotNull RDT plugin,
            final @Nullable UUID playerTownUuid
    ) {
        if (playerTownUuid == null) {
            this.initializeJoinTownAction(context, player, plugin);
            return;
        }

        this.initializeInvitePlayerAction(context, player, plugin, playerTownUuid);
    }

    private void initializeCreateTownAction(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        context
                .slot(
                        1,
                        2
                )
                .withItem(
                        UnifiedBuilderFactory
                                .item(
                                        Material.REINFORCED_DEEPSLATE
                                )
                                .setName(
                                        this.i18n(
                                                        "create_town.name",
                                                        player
                                                )
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n(
                                                        "create_town.lore",
                                                        player
                                                )
                                                .build()
                                                .children()
                                )
                                .build()
                )
                .displayIf(() -> player.hasPermission("raindroptowns.command.create"))
                .onClick(clickContext -> clickContext.openForPlayer(
                        CreateTownAnvilView.class,
                        Map.of(
                                "plugin",
                                plugin
                        )
                ));
    }

    private void initializeViewTownAction(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final @NotNull RDT plugin,
            final @NotNull UUID townUuid
    ) {
        context
                .slot(
                        1,
                        2
                )
                .withItem(
                        UnifiedBuilderFactory
                                .item(
                                        Material.WRITABLE_BOOK
                                )
                                .setName(
                                        this.i18n(
                                                        "view_town.name",
                                                        player
                                                )
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n(
                                                        "view_town.lore",
                                                        player
                                                )
                                                .build()
                                                .children()
                                )
                                .build()
                )
                .displayIf(() -> player.hasPermission("raindroptowns.command.town"))
                .onClick(clickContext -> clickContext.openForPlayer(
                        TownOverviewView.class,
                        Map.of(
                                "plugin",
                                plugin,
                                "town_uuid",
                                townUuid
                        )
                ));
    }

    private void initializeJoinTownAction(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        context
                .slot(
                        1,
                        3
                )
                .withItem(
                        UnifiedBuilderFactory
                                .item(
                                        Material.OAK_DOOR
                                )
                                .setName(
                                        this.i18n(
                                                        "join_town.name",
                                                        player
                                                )
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n(
                                                        "join_town.lore",
                                                        player
                                                )
                                                .build()
                                                .children()
                                )
                                .build()
                )
                .displayIf(() -> player.hasPermission("raindroptowns.command.towns"))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ServerTownsJoinView.class,
                        Map.of(
                                "plugin",
                                plugin
                        )
                ));
    }

    private void initializeInvitePlayerAction(
            final @NotNull RenderContext context,
            final @NotNull Player player,
            final @NotNull RDT plugin,
            final @NotNull UUID townUuid
    ) {
        context
                .slot(
                        1,
                        3
                )
                .withItem(
                        UnifiedBuilderFactory
                                .item(
                                        Material.NAME_TAG
                                )
                                .setName(
                                        this.i18n(
                                                        "invite_player.name",
                                                        player
                                                )
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n(
                                                        "invite_player.lore",
                                                        player
                                                )
                                                .build()
                                                .children()
                                )
                                .build()
                )
                .displayIf(() -> player.hasPermission("raindroptowns.command.town"))
                .onClick(click -> this.handleInvitePlayerClick(click, plugin, townUuid));
    }

    private void handleInvitePlayerClick(
            final @NotNull SlotClickContext click,
            final @NotNull RDT plugin,
            final @NotNull UUID townUuid
    ) {
        click.setCancelled(true);
        final Player player = click.getPlayer();

        if (plugin.getTownRepository() == null || plugin.getPlayerRepository() == null) {
            this.i18n("invite_player.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = plugin.getTownRepository().findByTownUUID(townUuid);
        if (town == null) {
            this.i18n("invite_player.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_INVITE)) {
            this.i18n("invite_player.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_INVITE.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                TownInvitePlayerView.class,
                Map.of(
                        "plugin",
                        plugin,
                        "town_uuid",
                        townUuid
                )
        );
    }

    private void initializeServerTownView(
            final @NotNull RenderContext context,
            final @NotNull Player player
    ) {
        context
                .slot(
                        1,
                        1
                )
                .withItem(
                        UnifiedBuilderFactory
                                .item(
                                        Material.DIAMOND
                                )
                                .setName(
                                        this.i18n(
                                                        "main_overview.name",
                                                        player
                                                )
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n(
                                                        "main_overview.lore",
                                                        player
                                                )
                                                .build()
                                                .children()
                                )
                                .build()
                )
                .displayIf(() -> player.hasPermission("raindroptowns.command.towns"))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ServerTownsOverviewView.class,
                        Map.of(
                                "plugin",
                                this.rdt.get(clickContext)
                        )
                ));
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private @Nullable UUID resolvePlayerTownUuid(
            final @NotNull RDT plugin,
            final @NotNull Player player
    ) {
        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (rdtPlayer == null) {
            return null;
        }
        return rdtPlayer.getTownUUID();
    }
}
