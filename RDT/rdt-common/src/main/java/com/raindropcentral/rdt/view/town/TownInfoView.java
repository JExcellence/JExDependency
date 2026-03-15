package com.raindropcentral.rdt.view.town;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;

/**
 * Lightweight town information menu opened from right-clicking a placed Nexus block.
 *
 * <p>The first action slot shows basic town information. The second slot opens the full
 * {@link TownOverviewView} when the viewer has {@link TownPermissions#VIEW_TOWN}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
public final class TownInfoView extends BaseView {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return town info key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_info_ui";
    }

    /**
     * Returns the inventory size in rows.
     *
     * @return inventory row count
     */
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

    /**
     * Renders town info and town overview navigation buttons.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RTown town = this.resolveTown(render, player);

        render.slot(1, 4).withItem(this.buildInfoItem(player, town));
        render.slot(1, 5)
                .withItem(this.buildProtectionsItem(player, town))
                .onClick(this::handleOpenProtectionsClick);
        render.slot(1, 6)
                .withItem(this.buildOverviewItem(player, town))
                .onClick(this::handleOpenOverviewClick);
    }

    private @NotNull ItemStack buildInfoItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("info.unavailable.name", player).build().component())
                    .setLore(this.i18n("info.unavailable.lore", player).build().children())
                    .build();
        }

        final String mayorName = this.resolveMayorName(town);
        final Location nexusLocation = town.getNexusLocation();
        final String nexusChunkX = nexusLocation == null || nexusLocation.getWorld() == null
                ? "-"
                : String.valueOf(nexusLocation.getChunk().getX());
        final String nexusChunkZ = nexusLocation == null || nexusLocation.getWorld() == null
                ? "-"
                : String.valueOf(nexusLocation.getChunk().getZ());

        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("info.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("info.lore", player)
                        .withPlaceholders(Map.of(
                                "mayor_name", mayorName,
                                "member_count", town.getMembers().size(),
                                "claimed_chunks", town.getChunks().size(),
                                "nexus_chunk_x", nexusChunkX,
                                "nexus_chunk_z", nexusChunkZ,
                                "town_level", town.getTownLevel(),
                                "active_state", this.resolveActiveState(player, town.getActive())
                        ))
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildOverviewItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("overview.unavailable.name", player).build().component())
                    .setLore(this.i18n("overview.unavailable.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("overview.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("overview.lore", player)
                        .withPlaceholder("permission", TownPermissions.VIEW_TOWN.getPermissionKey())
                        .build()
                        .children())
                .build();
    }

    private @NotNull ItemStack buildProtectionsItem(
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        if (town == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("protections.unavailable.name", player).build().component())
                    .setLore(this.i18n("protections.unavailable.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.SHIELD)
                .setName(this.i18n("protections.name", player)
                        .withPlaceholder("town_name", town.getTownName())
                        .build()
                        .component())
                .setLore(this.i18n("protections.lore", player)
                        .withPlaceholders(Map.of(
                                "permission", TownPermissions.TOWN_PROTECTIONS.getPermissionKey(),
                                "scope", this.resolveProtectionScopeState(player, "protections.scope.town")
                        ))
                        .build()
                        .children())
                .build();
    }

    private void handleOpenOverviewClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("overview.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("overview.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_TOWN)) {
            this.i18n("overview.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_TOWN.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                TownOverviewView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleOpenProtectionsClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null) {
            this.i18n("protections.error.system_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("protections.error.town_unavailable", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_PROTECTIONS)) {
            this.i18n("protections.error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_PROTECTIONS.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                TownProtectionsView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier(),
                        "protection_scope", "town"
                )
        );
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

        final UUID targetTownUuid = this.resolveTownUuid(context, player, plugin);
        if (targetTownUuid == null) {
            return null;
        }
        return townRepository.findByTownUUID(targetTownUuid);
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

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        if (rdtPlayer == null) {
            return null;
        }
        return rdtPlayer.getTownUUID();
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

    private @NotNull String resolveMayorName(final @NotNull RTown town) {
        final @Nullable String mayorName = Bukkit.getOfflinePlayer(town.getMayor()).getName();
        if (mayorName == null || mayorName.isBlank()) {
            return town.getMayor().toString();
        }
        return mayorName;
    }

    private @NotNull String resolveActiveState(
            final @NotNull Player player,
            final boolean active
    ) {
        final String key = active ? "info.state.active" : "info.state.inactive";
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }

    private @NotNull String resolveProtectionScopeState(
            final @NotNull Player player,
            final @NotNull String key
    ) {
        return PlainTextComponentSerializer.plainText().serialize(
                this.i18n(key, player).build().component()
        );
    }
}
