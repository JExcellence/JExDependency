package com.raindropcentral.rds.view.shop;

import java.util.Map;

import com.raindropcentral.rds.RDS;
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
 * Landing view for RDS plugin integrations.
 *
 * <p>This view groups access to job and skill requirement integration tools and provides
 * PlaceholderAPI expansion management for privileged shop administrators.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class PluginIntegrationManagementView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the plugin integration management view.
     */
    public PluginIntegrationManagementView() {
        super(ShopAdminView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "plugin_integration_management_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                "    s    ",
                "  k p j  ",
                "         ",
                "         ",
                "         "
        };
    }

    /**
     * Renders plugin integration controls.
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
        render.layoutSlot('k', this.createSkillsButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopSkillsView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));
        render.layoutSlot('j', this.createJobsButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopJobsView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));
        render.layoutSlot('p', this.createPlaceholderApiButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        PlaceholderAPIView.class,
                        Map.of("plugin", this.rds.get(clickContext))
                ));
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
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
        return UnifiedBuilderFactory.item(Material.COMPASS)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createSkillsButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
                .setName(this.i18n("actions.skills.name", player).build().component())
                .setLore(this.i18n("actions.skills.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createJobsButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.DIAMOND_PICKAXE)
                .setName(this.i18n("actions.jobs.name", player).build().component())
                .setLore(this.i18n("actions.jobs.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createPlaceholderApiButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
                .setName(this.i18n("actions.placeholder_api.name", player).build().component())
                .setLore(this.i18n("actions.placeholder_api.lore", player).build().children())
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
