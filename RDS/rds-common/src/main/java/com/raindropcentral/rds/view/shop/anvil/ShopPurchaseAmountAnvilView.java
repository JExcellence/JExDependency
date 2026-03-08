package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.AdminShopStockSupport;
import com.raindropcentral.rds.view.shop.ShopCustomerView;
import com.raindropcentral.rds.view.shop.ShopPurchaseLimitSupport;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Renders the shop purchase amount anvil inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopPurchaseAmountAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");

    /**
     * Creates a new shop purchase amount anvil view.
     */
    public ShopPurchaseAmountAnvilView() {
        super(ShopCustomerView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_purchase_amount_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return Integer.parseInt(input.trim());
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        return Map.of(
                "item_type", item.getItem().getType().name()
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        return "1";
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        try {
            final int amount = Integer.parseInt(input.trim());
            return amount > 0 && amount <= this.getMaxPurchasableAmount(context);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final String loreKey = this.isAdminShop(render)
                ? this.isUnlimitedAdminStock(render)
                    ? "input.admin.lore"
                    : "input.admin_limited.lore"
                : "input.player.lore";
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.PAPER)
                .setName(Component.text("1"))
                .setLore(this.i18n(loreKey, player)
                        .withPlaceholders(Map.of(
                                "max_amount", this.getMaxPurchasableAmount(render),
                                "item_type", this.targetItem.get(render).getItem().getType().name()
                        ))
                        .build()
                        .children())
                .build();

        render.firstSlot(inputSlotItem);
    }

    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        final boolean unlimitedAdminStock = this.isAdminShop(context)
                && this.isUnlimitedAdminStock(context)
                && this.getMaxPurchasableAmount(context) == Integer.MAX_VALUE;
        final String errorKey = unlimitedAdminStock
                ? "error.invalid_number_admin"
                : "error.invalid_number";
        this.i18n(errorKey, context.getPlayer())
                .withPlaceholders(Map.of(
                        "input", input == null ? "" : input,
                        "max_amount", this.getMaxPurchasableAmount(context)
                ))
                .includePrefix()
                .build()
                .sendMessage();
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
            final @Nullable Object processingResult,
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Map<String, Object> result = super.prepareResultData(processingResult, input, context);
        result.put("plugin", this.rds.get(context));
        result.put("shopLocation", this.shopLocation.get(context));
        result.put("shopItem", this.targetItem.get(context));
        result.put("purchaseAmount", processingResult);
        return result;
    }

    private boolean isAdminShop(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        return shop != null && shop.isAdminShop();
    }

    private boolean isUnlimitedAdminStock(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        final ShopItem item = this.getCurrentTargetItem(context);
        return shop != null
                && item != null
                && AdminShopStockSupport.isUnlimitedAdminStock(shop, item);
    }

    private int getMaxPurchasableAmount(
            final @NotNull Context context
    ) {
        final ShopItem item = this.getCurrentTargetItem(context);
        if (item == null) {
            return Math.max(0, this.targetItem.get(context).getAmount());
        }

        final Shop shop = this.getCurrentShop(context);
        final int stockMaximum = this.resolveStockMaximum(shop, item);
        if (shop == null) {
            return stockMaximum == Integer.MAX_VALUE ? Math.max(0, item.getAmount()) : stockMaximum;
        }

        final int limitMaximum = ShopPurchaseLimitSupport.getRemainingQuota(
                shop,
                item,
                context.getPlayer().getUniqueId()
        );
        if (stockMaximum == Integer.MAX_VALUE) {
            return Math.max(0, limitMaximum);
        }
        if (limitMaximum == Integer.MAX_VALUE) {
            return stockMaximum;
        }

        return Math.max(0, Math.min(stockMaximum, limitMaximum));
    }

    private int resolveStockMaximum(
            final @Nullable Shop shop,
            final @NotNull ShopItem item
    ) {
        if (shop != null && shop.isAdminShop() && AdminShopStockSupport.isUnlimitedAdminStock(shop, item)) {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, item.getAmount());
    }

    private Shop getCurrentShop(
            final @NotNull Context context
    ) {
        final RDS plugin = this.rds.get(context);
        final Shop shop = plugin.getShopRepository().findByLocation(this.shopLocation.get(context));
        if (shop != null && shop.isAdminShop()) {
            plugin.getAdminShopRestockScheduler().restockShop(shop);
        }
        return shop;
    }

    private ShopItem getCurrentTargetItem(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null) {
            return this.targetItem.get(context);
        }

        for (final var item : shop.getItems()) {
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (shopItem.getEntryId().equals(this.targetItem.get(context).getEntryId())) {
                return shopItem;
            }
        }

        return this.targetItem.get(context);
    }
}
