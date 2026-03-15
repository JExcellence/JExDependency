package com.raindropcentral.rds.view.shop;

import java.util.List;
import java.util.Map;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Root RDS store view used to inspect the next shop purchase tier.
 *
 * <p>This view shows the player's current placed-shop count, configured cap, and the availability
 * of the next purchase's requirement set before opening the detailed browser.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopStoreView extends BaseView {

    private final State<RDS> rds = initialState("plugin");

    @Override
    protected String getKey() {
        return "shop_store_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "  o m c  ",
            "         "
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    /**
     * Executes renderNavigationButtons.
     */
    @Override
    public void renderNavigationButtons(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        // Root store view does not use a return button.
    }

    /**
     * Executes onFirstRender.
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final RDSPlayer rdsPlayer = this.getOrCreatePlayer(render);
        final int ownedShops = rdsPlayer.getShops();
        final int purchaseNumber = ShopStoreSupport.getNextPurchaseNumber(ownedShops);
        final List<ShopStorePricingSupport.ResolvedStoreRequirement> requirements =
            ShopStorePricingSupport.getConfiguredStoreRequirements(plugin, config, player, purchaseNumber);
        final ShopStorePricingSupport.RequirementAvailability availability =
            ShopStorePricingSupport.resolveAvailability(player, requirements, rdsPlayer);

        render.layoutSlot('o')
            .renderWith(() -> this.createOwnedShopsItem(player, ownedShops))
            .updateOnStateChange(this.rds);

        render.layoutSlot('m')
            .renderWith(() -> this.createMaxShopsItem(player, plugin, config, ownedShops))
            .updateOnStateChange(this.rds);

        render.layoutSlot('c')
            .renderWith(() -> this.createRequirementsItem(player, requirements.size(), availability))
            .onClick(clickContext -> clickContext.openForPlayer(
                ShopStoreCostView.class,
                Map.of("plugin", this.rds.get(clickContext))
            ));
    }

    /**
     * Cancels direct inventory clicks while this view is open.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull RDSPlayer getOrCreatePlayer(final @NotNull Context context) {
        final Player player = context.getPlayer();
        final RDS plugin = this.rds.get(context);
        final RDSPlayer existingPlayer = plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(player.getUniqueId());
        plugin.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    private @NotNull ItemStack createOwnedShopsItem(
        final @NotNull Player player,
        final int ownedShops
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("owned_shops.name", player).build().component())
            .setLore(this.i18n("owned_shops.lore", player)
                .withPlaceholder("owned_shops", ownedShops)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMaxShopsItem(
        final @NotNull Player player,
        final @NotNull RDS plugin,
        final @NotNull ConfigSection config,
        final int ownedShops
    ) {
        final int maxShops = plugin.getMaximumShops(player, config);
        final boolean limited = maxShops > 0;
        final String maxShopsDisplay = limited ? Integer.toString(maxShops) : "No limit";

        return UnifiedBuilderFactory.item(limited ? Material.BEACON : Material.LIME_STAINED_GLASS_PANE)
            .setName(this.i18n("max_shops.name", player).build().component())
            .setLore(this.i18n("max_shops.lore", player)
                .withPlaceholders(Map.of(
                    "owned_shops", ownedShops,
                    "max_shops", maxShopsDisplay
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementsItem(
        final @NotNull Player player,
        final int requirementCount,
        final @NotNull ShopStorePricingSupport.RequirementAvailability availability
    ) {
        final String availabilityPlaceholder = switch (availability) {
            case READY -> this.i18n("costs.availability.available", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
            case PENDING -> this.i18n("costs.availability.pending", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
            case UNAVAILABLE -> this.i18n("costs.availability.unavailable", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
        };

        final Material material = switch (availability) {
            case READY -> Material.GOLD_INGOT;
            case PENDING -> Material.CLOCK;
            case UNAVAILABLE -> Material.REDSTONE;
        };

        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("costs.name", player).build().component())
            .setLore(this.i18n("costs.lore", player)
                .withPlaceholders(Map.of(
                    "requirement_count", requirementCount,
                    "requirements", requirementCount,
                    "availability", availabilityPlaceholder
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
