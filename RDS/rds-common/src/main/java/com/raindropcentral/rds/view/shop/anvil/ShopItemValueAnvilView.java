package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.view.shop.ShopItemEditView;
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

import java.util.Locale;
import java.util.Map;

/**
 * Renders the shop item value anvil inventory view.
 */
public class ShopItemValueAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");

    /**
     * Creates a new shop item value anvil view.
     */
    public ShopItemValueAnvilView() {
        super(ShopItemEditView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_item_value_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return this.targetItem.get(context).withValue(Double.parseDouble(input.trim()));
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        return Map.of(
                "currency_type", item.getCurrencyType(),
                "value", item.getValue()
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        return String.format(Locale.US, "%.2f", this.targetItem.get(context).getValue());
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        try {
            return Double.parseDouble(input.trim()) >= 0D;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final String currentValue = String.format(Locale.US, "%.2f", this.targetItem.get(render).getValue());
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(Component.text(currentValue))
                .setLore(this.i18n("input.lore", player).build().children())
                .build();

        render.firstSlot(inputSlotItem);
    }

    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        this.i18n("error.invalid_number", context.getPlayer())
                .includePrefix()
                .withPlaceholder("input", input == null ? "" : input)
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
        result.put("shopItem", processingResult);
        return result;
    }
}