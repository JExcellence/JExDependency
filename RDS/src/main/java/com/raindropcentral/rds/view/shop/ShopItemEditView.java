package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.view.shop.anvil.ShopItemCurrencyTypeAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemValueAnvilView;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopItemEditView extends BaseView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> sourceItem = initialState("shopItem");
    private final MutableState<ShopItem> editedItem = mutableState((ShopItem) null);

    public ShopItemEditView() {
        super(ShopEditView.class);
    }

    @Override
    protected String getKey() {
        return "shop_item_edit_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                "  t v c  ",
                "         "
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.getEditedItem(context);
        return Map.of(
                "item_type", item.getItem().getType().name(),
                "currency_type", item.getCurrencyType(),
                "value", item.getValue()
        );
    }

    @Override
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        this.restoreEditedItem(target);
        if (this.editedItem.get(target) == null) {
            this.restoreEditedItem(origin, target);
        }

        target.update();
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (this.editedItem.get(render) == null) {
            this.restoreEditedItem(render);
        }
        if (this.editedItem.get(render) == null) {
            this.editedItem.set(this.sourceItem.get(render), render);
        }

        render.layoutSlot('s')
                .renderWith(() -> this.createSummaryItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem);

        render.layoutSlot('t')
                .renderWith(() -> this.createCurrencyTypeItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopItemCurrencyTypeAnvilView.class,
                        Map.of(
                                "plugin", this.rds.get(clickContext),
                                "shopLocation", this.shopLocation.get(clickContext),
                                "shopItem", this.getEditedItem(clickContext)
                        )
                ));

        render.layoutSlot('v')
                .renderWith(() -> this.createValueItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopItemValueAnvilView.class,
                        Map.of(
                                "plugin", this.rds.get(clickContext),
                                "shopLocation", this.shopLocation.get(clickContext),
                                "shopItem", this.getEditedItem(clickContext)
                        )
                ));

        render.layoutSlot(
                'c',
                UnifiedBuilderFactory.item(new Proceed().getHead(player))
                        .setName(this.i18n("confirm.name", player).build().component())
                        .setLore(this.i18n("confirm.lore", player).build().children())
                        .build()
        ).onClick(clickContext -> this.handleConfirm(clickContext));
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleConfirm(final @NotNull SlotClickContext clickContext) {
        clickContext.setCancelled(true);
        clickContext.getPlayer().setItemOnCursor(null);

        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!shop.canManage(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final ShopItem edited = this.getEditedItem(clickContext);
        final ShopItem original = this.sourceItem.get(clickContext);
        final List<AbstractItem> items = new ArrayList<>(shop.getItems());
        final int matchingIndex = this.findMatchingItemIndex(items, original, edited);

        if (matchingIndex >= 0) {
            final ShopItem matchingItem = (ShopItem) items.get(matchingIndex);
            items.set(matchingIndex, matchingItem.withPricing(edited.getCurrencyType(), edited.getValue()));
            shop.setItems(items);
            this.rds.get(clickContext).getShopRepository().update(shop);

            this.i18n("feedback.saved", clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "currency_type", edited.getCurrencyType(),
                            "value", edited.getValue()
                    ))
                    .build()
                    .sendMessage();

            clickContext.openForPlayer(
                    ShopEditView.class,
                    Map.of(
                            "plugin", this.rds.get(clickContext),
                            "shopLocation", this.shopLocation.get(clickContext)
                    )
            );
            return;
        }

        this.i18n("feedback.item_missing", clickContext.getPlayer())
                .build()
                .sendMessage();
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull ShopItem getEditedItem(final @NotNull Context context) {
        final ShopItem current = this.editedItem.get(context);
        return current == null ? this.sourceItem.get(context) : current;
    }

    private void restoreEditedItem(final @NotNull Context context) {
        this.restoreEditedItem(context, context);
    }

    private void restoreEditedItem(
            final @NotNull Context dataContext,
            final @NotNull Context stateContext
    ) {
        final Object initialData = dataContext.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return;
        }

        final Object updatedItem = data.get("shopItem");
        if (updatedItem instanceof ShopItem shopItem) {
            this.editedItem.set(shopItem, stateContext);
        }
    }

    private int findMatchingItemIndex(
            final @NotNull List<AbstractItem> items,
            final @NotNull ShopItem original,
            final @NotNull ShopItem edited
    ) {
        for (int index = 0; index < items.size(); index++) {
            final AbstractItem item = items.get(index);
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (shopItem.getEntryId().equals(edited.getEntryId())) {
                return index;
            }
        }

        for (int index = 0; index < items.size(); index++) {
            final AbstractItem item = items.get(index);
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (this.isLegacyMatch(shopItem, original)) {
                return index;
            }
        }

        return -1;
    }

    private boolean isLegacyMatch(
            final @NotNull ShopItem candidate,
            final @NotNull ShopItem original
    ) {
        return candidate.getAmount() == original.getAmount()
                && candidate.getItem().isSimilar(original.getItem());
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final ItemStack displayItem = item.getItem();
        displayItem.setAmount(1);

        final List<Component> lore = new ArrayList<>(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                        "amount", item.getAmount(),
                        "currency_type", item.getCurrencyType(),
                        "value", item.getValue()
                ))
                .build()
                .children());

        final var itemLore = displayItem.lore();
        if (itemLore != null && !itemLore.isEmpty()) {
            lore.add(Component.empty());
            lore.addAll(itemLore);
        }

        return UnifiedBuilderFactory.item(displayItem)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCurrencyTypeItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.EMERALD)
                .setName(this.i18n("currency_type.name", player).build().component())
                .setLore(this.i18n("currency_type.lore", player)
                        .withPlaceholder("currency_type", item.getCurrencyType())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createValueItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("value.name", player).build().component())
                .setLore(this.i18n("value.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_type", item.getCurrencyType(),
                                "value", item.getValue()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
