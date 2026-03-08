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

import java.util.HashMap;
import java.util.Map;

/**
 * Renders the purchase-limit window-minutes anvil editor for a shop item.
 *
 * <p>This view always accepts a positive minute count and preserves the purchase-limit amount. When
 * no purchase limit is currently configured, the amount defaults to {@code 1} to activate the
 * provided window.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopItemPurchaseLimitMinutesAnvilView extends AbstractAnvilView {

    private static final int DEFAULT_LIMIT_AMOUNT = 1;
    private static final int DEFAULT_WINDOW_MINUTES = 60;

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");

    /**
     * Creates a new shop item purchase-limit minutes anvil view.
     */
    public ShopItemPurchaseLimitMinutesAnvilView() {
        super(ShopItemEditView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_item_purchase_limit_minutes_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Integer parsedMinutes = parseLimitWindowMinutes(input);
        if (parsedMinutes == null) {
            return this.targetItem.get(context);
        }

        final ShopItem current = this.targetItem.get(context);
        final int limitAmount = current.hasPurchaseLimit()
                ? current.getPurchaseLimitAmount()
                : DEFAULT_LIMIT_AMOUNT;
        return current.withPurchaseLimit(limitAmount, parsedMinutes);
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        final int limitAmount = item.hasPurchaseLimit()
                ? item.getPurchaseLimitAmount()
                : DEFAULT_LIMIT_AMOUNT;
        final int windowMinutes = item.hasPurchaseLimit()
                ? item.getPurchaseLimitWindowMinutes()
                : DEFAULT_WINDOW_MINUTES;
        return Map.of(
                "item_type", item.getItem().getType().name(),
                "current_limit", this.getCurrentLimitLabel(context.getPlayer(), item),
                "limit_amount", limitAmount,
                "window_minutes", windowMinutes,
                "default_limit_amount", DEFAULT_LIMIT_AMOUNT
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        return String.valueOf(item.hasPurchaseLimit()
                ? item.getPurchaseLimitWindowMinutes()
                : DEFAULT_WINDOW_MINUTES);
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return parseLimitWindowMinutes(input) != null;
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final ShopItem item = this.targetItem.get(render);
        final int limitAmount = item.hasPurchaseLimit()
                ? item.getPurchaseLimitAmount()
                : DEFAULT_LIMIT_AMOUNT;
        final int windowMinutes = item.hasPurchaseLimit()
                ? item.getPurchaseLimitWindowMinutes()
                : DEFAULT_WINDOW_MINUTES;
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(Component.text(String.valueOf(windowMinutes)))
                .setLore(this.i18n("input.lore", player)
                        .withPlaceholders(Map.of(
                                "current_limit", this.getCurrentLimitLabel(player, item),
                                "limit_amount", limitAmount,
                                "window_minutes", windowMinutes,
                                "default_limit_amount", DEFAULT_LIMIT_AMOUNT
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
        this.i18n("error.invalid_number", context.getPlayer())
                .includePrefix()
                .withPlaceholder("input", input == null ? "" : input)
                .build()
                .sendMessage();
        this.reopenEditor(context);
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

    static @Nullable Integer parseLimitWindowMinutes(
            final @NotNull String input
    ) {
        final String trimmed = input.trim();
        try {
            final int parsed = Integer.parseInt(trimmed);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void reopenEditor(
            final @NotNull Context context
    ) {
        context.getPlayer().setItemOnCursor(null);
        context.openForPlayer(ShopItemPurchaseLimitMinutesAnvilView.class, this.createViewData(context));
    }

    private @NotNull Map<String, Object> createViewData(
            final @NotNull Context context
    ) {
        final Map<String, Object> viewData = new HashMap<>();
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> data) {
            for (final Map.Entry<?, ?> entry : data.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    viewData.put(key, entry.getValue());
                }
            }
        }

        viewData.put("plugin", this.rds.get(context));
        viewData.put("shopLocation", this.shopLocation.get(context));
        viewData.put("shopItem", this.targetItem.get(context));
        return viewData;
    }

    private @NotNull String getCurrentLimitLabel(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        if (!item.hasPurchaseLimit()) {
            return this.i18n("input.disabled", player)
                    .build()
                    .getI18nVersionWrapper()
                    .asPlaceholder();
        }

        return this.i18n("input.current", player)
                .withPlaceholders(Map.of(
                        "limit_amount", item.getPurchaseLimitAmount(),
                        "window_minutes", item.getPurchaseLimitWindowMinutes()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }
}
