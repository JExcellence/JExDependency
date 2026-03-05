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
 * exposes the currency-management entry point.</p>
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
     * @return rendered layout with summary and currency controls
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "    s    ",
            "    c    ",
            "         ",
            "         ",
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
        render.layoutSlot('c', this.createCurrencyButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                AdminCurrencyView.class,
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

    private @NotNull ItemStack createCurrencyButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PRISMARINE_CRYSTALS)
            .setName(this.i18n("actions.currency.name", player).build().component())
            .setLore(this.i18n("actions.currency.lore", player).build().children())
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
