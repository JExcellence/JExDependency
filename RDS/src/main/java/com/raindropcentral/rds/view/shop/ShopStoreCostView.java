package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopStoreCostView extends APaginatedView<ShopStorePricingSupport.StoreCurrencyCost> {

    private final State<RDS> rds = initialState("plugin");

    public ShopStoreCostView() {
        super(ShopStoreView.class);
    }

    @Override
    protected String getKey() {
        return "shop_store_cost_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "  < p >  "
        };
    }

    @Override
    protected CompletableFuture<List<ShopStorePricingSupport.StoreCurrencyCost>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDS plugin = this.rds.get(context);
        final ConfigSection config = plugin.getDefaultConfig();
        final RDSPlayer playerData = this.getOrCreatePlayer(context);
        return CompletableFuture.completedFuture(
                ShopStorePricingSupport.getAvailableStoreCosts(plugin, config, playerData.getShops())
        );
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ShopStorePricingSupport.StoreCurrencyCost entry
    ) {
        final Player player = context.getPlayer();
        builder.withItem(
                UnifiedBuilderFactory.item(this.resolveMaterial(entry.currencyType()))
                        .setName(this.i18n("entry.name", player)
                                .withPlaceholders(Map.of(
                                        "currency_name", entry.currencyName(),
                                        "currency_type", entry.currencyType()
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("entry.lore", player)
                                .withPlaceholders(Map.of(
                                        "currency_name", entry.currencyName(),
                                        "currency_type", entry.currencyType(),
                                        "initial_cost", ShopStorePricingSupport.formatCurrency(
                                                this.rds.get(context),
                                                entry.currencyType(),
                                                entry.initialCost()
                                        ),
                                        "growth_rate", String.format(java.util.Locale.US, "%.3f", entry.growthRate()),
                                        "current_cost", ShopStorePricingSupport.formatCurrency(
                                                this.rds.get(context),
                                                entry.currencyType(),
                                                entry.currentCost()
                                        )
                                ))
                                .build()
                                .children())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        );
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final RDSPlayer playerData = this.getOrCreatePlayer(render);
        final List<ShopStorePricingSupport.StoreCurrencyCost> costs = ShopStorePricingSupport
                .getAvailableStoreCosts(plugin, config, playerData.getShops());
        final boolean limited = config.hasShopLimit();
        final String maxShopsDisplay = limited ? Integer.toString(config.getMaxShops()) : "No limit";

        render.layoutSlot('s')
                .renderWith(() -> this.createSummaryItem(player, playerData.getShops(), maxShopsDisplay, costs.size()))
                .onClick(clickContext -> this.handlePurchaseClick(clickContext, playerData, config, maxShopsDisplay, costs));
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handlePurchaseClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull RDSPlayer playerData,
            final @NotNull ConfigSection config,
            final @NotNull String maxShopsDisplay,
            final @NotNull List<ShopStorePricingSupport.StoreCurrencyCost> costs
    ) {
        final Player player = clickContext.getPlayer();
        final RDS plugin = this.rds.get(clickContext);

        if (config.hasShopLimit() && playerData.getShops() >= config.getMaxShops()) {
            this.i18n("feedback.limit_reached", player)
                    .withPlaceholders(Map.of(
                            "owned_shops", playerData.getShops(),
                            "max_shops", maxShopsDisplay
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (costs.isEmpty()) {
            this.i18n("feedback.no_currencies", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final ShopStorePricingSupport.PurchaseResult purchaseResult = ShopStorePricingSupport.purchaseShop(
                clickContext,
                plugin,
                playerData,
                config
        );

        if (!purchaseResult.success()) {
            this.i18n(purchaseResult.failureKey(), player)
                    .withPlaceholders(Map.of(
                            "currency_type", purchaseResult.currencyType(),
                            "currency_name", purchaseResult.currencyName(),
                            "cost", purchaseResult.formattedCost(),
                            "costs", purchaseResult.costSummary(),
                            "owned_shops", playerData.getShops(),
                            "max_shops", maxShopsDisplay
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        player.getInventory().addItem(ShopBlock.getShopBlock(plugin, player))
                .forEach((slot, item) -> player.getWorld().dropItem(
                        player.getLocation().clone().add(0, 0.5, 0),
                        item
                ));

        this.i18n("feedback.purchased", player)
                .withPlaceholders(Map.of(
                        "costs", purchaseResult.costSummary(),
                        "owned_shops", playerData.getShops(),
                        "max_shops", maxShopsDisplay
                ))
                .includePrefix()
                .build()
                .sendMessage();

        clickContext.update();
    }

    private @NotNull RDSPlayer getOrCreatePlayer(
            final @NotNull Context context
    ) {
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

    private @NotNull org.bukkit.inventory.ItemStack createSummaryItem(
            final @NotNull Player player,
            final int ownedShops,
            final @NotNull String maxShopsDisplay,
            final int currencyCount
    ) {
        final Material material = currencyCount == 0 ? Material.BARRIER : Material.CHEST;
        final String suffix = currencyCount == 0 ? "empty" : "summary";

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n(suffix + ".name", player).build().component())
                .setLore(this.i18n(suffix + ".lore", player)
                        .withPlaceholders(Map.of(
                                "owned_shops", ownedShops,
                                "max_shops", maxShopsDisplay,
                                "currency_count", currencyCount
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull Material resolveMaterial(
            final @NotNull String currencyType
    ) {
        return "vault".equalsIgnoreCase(currencyType)
                ? Material.GOLD_INGOT
                : Material.EMERALD;
    }
}
