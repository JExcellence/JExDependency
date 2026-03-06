package com.raindropcentral.rdr.view;

import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Administrative landing view for RDR controls.
 *
 * <p>This view centralizes privileged storage administration actions and currently
 * exposes player/group controls, config-editing, and plugin-integration entry points.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the storage admin landing view.
     */
    public StorageAdminView() {
        super(StorageOverviewView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage admin translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered layout with summary, config, and integration controls
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "  p g i  ",
            "         "
        };
    }

    /**
     * Renders the admin controls.
     *
     * @param render render context for this menu
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player));
        render.layoutSlot('p', this.createPlayerAdminButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminPlayerView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
        render.layoutSlot('g', this.createConfigButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageConfigView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
        render.layoutSlot('i', this.createPluginIntegrationsButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                PluginIntegrationManagementView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
    }

    /**
     * Cancels vanilla click handling so this menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(
        final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.COMMAND_BLOCK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPluginIntegrationsButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.ENDER_CHEST)
            .setName(this.i18n("actions.integrations.name", player).build().component())
            .setLore(this.i18n("actions.integrations.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPlayerAdminButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
            .setName(this.i18n("actions.players.name", player).build().component())
            .setLore(this.i18n("actions.players.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createConfigButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("actions.config.name", player).build().component())
            .setLore(this.i18n("actions.config.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.locked.name", player).build().component())
            .setLore(this.i18n("feedback.locked.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private boolean hasAdminAccess(
        final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }
}
