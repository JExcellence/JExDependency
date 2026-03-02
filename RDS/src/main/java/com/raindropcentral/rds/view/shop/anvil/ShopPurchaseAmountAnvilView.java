package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.view.shop.ShopCustomerView;
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

public class ShopPurchaseAmountAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");

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
            return amount > 0 && (this.isAdminShop(context) || amount <= this.targetItem.get(context).getAmount());
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
                ? "input.admin.lore"
                : "input.player.lore";
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.PAPER)
                .setName(Component.text("1"))
                .setLore(this.i18n(loreKey, player)
                        .withPlaceholders(Map.of(
                                "max_amount", this.targetItem.get(render).getAmount(),
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
        final String errorKey = this.isAdminShop(context)
                ? "error.invalid_number_admin"
                : "error.invalid_number";
        this.i18n(errorKey, context.getPlayer())
                .withPlaceholders(Map.of(
                        "input", input == null ? "" : input,
                        "max_amount", this.targetItem.get(context).getAmount()
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
        final Shop shop = this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
        return shop != null && shop.isAdminShop();
    }
}
