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
import me.devnatan.inventoryframework.state.MutableState;
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
 * Paginated role-management view for a town.
 *
 * <p>Shows all town roles, supports selecting a role entry, and provides controls to create and
 * delete roles.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RolesOverviewView extends APaginatedView<TownRole> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");
    private final MutableState<String> selectedRoleId = mutableState("");

    /**
     * Creates the roles overview and configures return navigation back to town overview.
     */
    public RolesOverviewView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected String getKey() {
        return "roles_overview_ui";
    }

    /**
     * Returns the paginated layout including add/delete controls.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "XXXXXXXXX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "XOOOOOOOX",
                "XXXXXXXXX",
                "  <p>mda "
        };
    }

    /**
     * Loads town roles asynchronously for pagination.
     *
     * @param context view context
     * @return future role list
     */
    @Override
    protected CompletableFuture<List<TownRole>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Executor executor = plugin.getExecutor();
        if (executor == null) {
            return CompletableFuture.supplyAsync(() -> this.resolveSortedRoles(context));
        }
        return CompletableFuture.supplyAsync(() -> this.resolveSortedRoles(context), executor);
    }

    /**
     * Renders an individual role entry.
     *
     * @param context context
     * @param builder builder
     * @param index zero-based index
     * @param entry role entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TownRole entry
    ) {
        final Player player = context.getPlayer();
        final boolean isDefaultRole = RTown.isDefaultRoleId(entry.getRoleId());
        final boolean selected = this.isSelected(context, entry.getRoleId());

        final Material material;
        if (isDefaultRole) {
            material = Material.NETHER_STAR;
        } else if (selected) {
            material = Material.LIME_DYE;
        } else {
            material = Material.PAPER;
        }

        builder.withItem(
                UnifiedBuilderFactory.item(material)
                        .setName(this.i18n("role.name", player)
                                .withPlaceholders(Map.of(
                                        "role_name", entry.getRoleName(),
                                        "role_id", entry.getRoleId()
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("role.lore", player)
                                .withPlaceholders(Map.of(
                                        "role_id", entry.getRoleId(),
                                        "role_name", entry.getRoleName(),
                                        "permission_count", entry.getPermissions().size(),
                                        "default_state", this.toPlain(player, isDefaultRole ? "role.state.default" : "role.state.custom"),
                                        "selected_state", this.toPlain(player, selected ? "role.state.selected" : "role.state.not_selected")
                                ))
                                .build()
                                .children())
                        .build()
        ).onClick(click -> this.handleRoleSelection(click, entry));
    }

    /**
     * Renders add/delete controls after pagination controls are in place.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render, player);

        render.layoutSlot('m', this.buildRolePermissionItem(player))
                .onClick(this::handleRolePermissionClick);

        render.layoutSlot('a', this.buildAddRoleItem(player))
                .onClick(this::handleAddRoleClick);

        render.layoutSlot('d', this.buildDeleteRoleItem(render, player, town))
                .updateOnStateChange(this.selectedRoleId)
                .onClick(this::handleDeleteRoleClick);
    }

    /**
     * Verifies viewer permissions before rendering pagination components.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RTown town = this.resolveTown(render, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(render, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.VIEW_ROLES)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.VIEW_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            player.closeInventory();
            return;
        }

        super.onFirstRender(render, player);
    }

    /**
     * Cancels default item movement for this inventory.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull List<TownRole> resolveSortedRoles(final @NotNull Context context) {
        final RTown town = this.resolveTown(context, context.getPlayer());
        if (town == null) {
            return List.of();
        }

        return town.getRoles().stream()
                .sorted(Comparator.comparing(TownRole::getRoleId))
                .toList();
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

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private void handleRoleSelection(
            final @NotNull SlotClickContext click,
            final @NotNull TownRole entry
    ) {
        click.setCancelled(true);
        this.selectedRoleId.set(entry.getRoleId(), click);
        this.i18n("message.selected", click.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "role_id", entry.getRoleId(),
                        "role_name", entry.getRoleName()
                ))
                .build()
                .sendMessage();
    }

    private @NotNull org.bukkit.inventory.ItemStack buildAddRoleItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ANVIL)
                .setName(this.i18n("add.name", player).build().component())
                .setLore(this.i18n("add.lore", player).build().children())
                .build();
    }

    private @NotNull org.bukkit.inventory.ItemStack buildRolePermissionItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.COMPARATOR)
                .setName(this.i18n("permissions.name", player).build().component())
                .setLore(this.i18n("permissions.lore", player).build().children())
                .build();
    }

    private @NotNull org.bukkit.inventory.ItemStack buildDeleteRoleItem(
            final @NotNull Context context,
            final @NotNull Player player,
            final @Nullable RTown town
    ) {
        final String selected = this.resolveSelectedRoleId(context);
        if (selected == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("delete.disabled.name", player).build().component())
                    .setLore(this.i18n("delete.disabled.lore", player).build().children())
                    .build();
        }

        final TownRole selectedRole = town == null ? null : town.findRoleById(selected);
        if (selectedRole == null) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("delete.disabled.name", player).build().component())
                    .setLore(this.i18n("delete.disabled.lore", player).build().children())
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.LAVA_BUCKET)
                .setName(this.i18n("delete.name", player)
                        .withPlaceholder("role_name", selectedRole.getRoleName())
                        .build()
                        .component())
                .setLore(this.i18n("delete.lore", player)
                        .withPlaceholders(Map.of(
                                "role_id", selectedRole.getRoleId(),
                                "role_name", selectedRole.getRoleName()
                        ))
                        .build()
                        .children())
                .build();
    }

    private void handleAddRoleClick(final @NotNull SlotClickContext click) {
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
        if (!town.hasTownPermission(townPlayer, TownPermissions.CREATE_ROLES)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.CREATE_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        click.openForPlayer(
                RoleCreateAnvilView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleDeleteRoleClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("error.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("error.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(click, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.DELETE_ROLES)) {
            this.i18n("error.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.DELETE_ROLES.getPermissionKey())
                    .build()
                    .sendMessage();
            return;
        }

        final String selected = this.resolveSelectedRoleId(click);
        if (selected == null) {
            this.i18n("delete.error.no_selection", player).includePrefix().build().sendMessage();
            return;
        }

        if (RTown.isDefaultRoleId(selected)) {
            this.i18n("delete.error.default_role", player)
                    .includePrefix()
                    .withPlaceholder("role_id", selected)
                    .build()
                    .sendMessage();
            return;
        }

        if (this.isRoleAssignedToMembers(town, selected)) {
            this.i18n("delete.error.role_in_use", player)
                    .includePrefix()
                    .withPlaceholder("role_id", selected)
                    .build()
                    .sendMessage();
            return;
        }

        if (!town.removeRoleById(selected)) {
            this.i18n("delete.error.not_found", player)
                    .includePrefix()
                    .withPlaceholder("role_id", selected)
                    .build()
                    .sendMessage();
            return;
        }

        plugin.getTownRepository().update(town);
        this.selectedRoleId.set("", click);
        this.i18n("delete.success", player)
                .includePrefix()
                .withPlaceholder("role_id", selected)
                .build()
                .sendMessage();

        click.openForPlayer(
                RolesOverviewView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private void handleRolePermissionClick(final @NotNull SlotClickContext click) {
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

        click.openForPlayer(
                RolePermissionView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", town.getIdentifier()
                )
        );
    }

    private boolean isRoleAssignedToMembers(
            final @NotNull RTown town,
            final @NotNull String roleId
    ) {
        final String normalizedRoleId = RTown.normalizeRoleId(roleId);
        for (final RDTPlayer member : town.getMembers()) {
            final String memberRoleId = member.getTownRoleId();
            if (memberRoleId != null && RTown.normalizeRoleId(memberRoleId).equals(normalizedRoleId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSelected(
            final @NotNull Context context,
            final @NotNull String roleId
    ) {
        final String selected = this.resolveSelectedRoleId(context);
        if (selected == null) {
            return false;
        }
        return RTown.normalizeRoleId(selected).equals(RTown.normalizeRoleId(roleId));
    }

    private @Nullable String resolveSelectedRoleId(final @NotNull Context context) {
        try {
            final String selected = this.selectedRoleId.get(context);
            if (selected == null || selected.isBlank()) {
                return null;
            }
            return selected;
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
