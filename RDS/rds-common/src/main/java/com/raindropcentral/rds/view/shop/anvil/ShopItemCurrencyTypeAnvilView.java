package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.view.shop.ShopItemEditView;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Renders the shop item currency type anvil inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopItemCurrencyTypeAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");

    /**
     * Creates a new shop item currency type anvil view.
     */
    public ShopItemCurrencyTypeAnvilView() {
        super(ShopItemEditView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_item_currency_type_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final String normalizedCurrencyType = input.trim();
        return this.targetItem.get(context).withCurrencyType(normalizedCurrencyType);
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        return Map.of(
                "currency_type", item.getCurrencyType(),
                "item_type", item.getItem().getType().name()
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        return this.targetItem.get(context).getCurrencyType();
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return this.isAvailableCurrencyType(input, context);
    }

    /**
     * Cancels raw inventory interaction to prevent the anvil slot icon from being moved to player inventory.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final String currentCurrencyType = this.targetItem.get(render).getCurrencyType();
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.EMERALD)
                .setName(Component.text(currentCurrencyType))
                .setLore(this.i18n("input.lore", player).build().children())
                .build();

        render.firstSlot(inputSlotItem);
    }

    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        final String normalizedInput = input == null ? "" : input.trim();
        final String errorKey = normalizedInput.isEmpty()
                ? "error.empty"
                : "error.invalid_currency";

        this.i18n(errorKey, context.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "input", normalizedInput,
                        "currency_type", normalizedInput
                ))
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

    private boolean isAvailableCurrencyType(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final String normalizedInput = input.trim();
        if (normalizedInput.isEmpty()) {
            return false;
        }

        if ("vault".equalsIgnoreCase(normalizedInput)) {
            return true;
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null && this.hasCustomCurrency(bridge, normalizedInput, context);
    }

    private boolean hasCustomCurrency(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull String currencyType,
            final @NotNull Context context
    ) {
        try {
            final Method findCurrencyMethod = JExEconomyBridge.class.getDeclaredMethod("findCurrency", String.class);
            findCurrencyMethod.setAccessible(true);
            return findCurrencyMethod.invoke(bridge, currencyType) != null;
        } catch (ReflectiveOperationException exception) {
            this.rds.get(context).getLogger().fine("Failed to validate currency type through JExEconomyBridge: " + currencyType);
            return false;
        }
    }
}
