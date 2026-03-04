/*
 * ShopItemEditView.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.AdminShopRestockMode;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.AdminShopStockSupport;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminResetTimerAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminStockLimitAnvilView;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders the shop item edit inventory view.
 */
public class ShopItemEditView extends BaseView {

    private static final DateTimeFormatter RESTOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> sourceItem = initialState("shopItem");
    private final MutableState<ShopItem> editedItem = mutableState((ShopItem) null);

    /**
     * Creates a new shop item edit view.
     */
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
                "  l r m  "
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
        final Shop currentShop = this.getCurrentShop(render);
        if (this.editedItem.get(render) == null) {
            this.restoreEditedItem(render);
        }
        if (this.editedItem.get(render) == null) {
            this.editedItem.set(this.sourceItem.get(render), render);
        }

        render.layoutSlot('s')
                .renderWith(() -> this.createSummaryItem(player, currentShop, this.getEditedItem(render)))
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

        if (currentShop != null && currentShop.isAdminShop()) {
            render.layoutSlot('l')
                    .renderWith(() -> this.createStockLimitItem(player, this.getEditedItem(render)))
                    .updateOnStateChange(this.editedItem)
                    .onClick(clickContext -> clickContext.openForPlayer(
                            ShopItemAdminStockLimitAnvilView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", this.shopLocation.get(clickContext),
                                    "shopItem", this.getEditedItem(clickContext)
                            )
                    ));

            render.layoutSlot('r')
                    .renderWith(() -> this.createResetTimerItem(player, this.getEditedItem(render)))
                    .updateOnStateChange(this.editedItem)
                    .onClick(clickContext -> clickContext.openForPlayer(
                            ShopItemAdminResetTimerAnvilView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", this.shopLocation.get(clickContext),
                                    "shopItem", this.getEditedItem(clickContext)
                            )
                    ));

            render.layoutSlot('m')
                    .renderWith(() -> this.createRestockModeItem(player))
                    .updateOnStateChange(this.editedItem)
                    .onClick(this::handleRestockModeClick);
        }
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
            items.set(matchingIndex, this.mergeEditedItem(matchingItem, edited));
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

    private void handleRestockModeClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

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

        final RDS plugin = this.rds.get(clickContext);
        final AdminShopRestockMode updatedMode = this.toggleAdminRestockMode(plugin);
        if (updatedMode == null) {
            this.i18n("feedback.restock_mode_save_failed", clickContext.getPlayer())
                    .build()
                    .sendMessage();
            return;
        }

        final long updatedReferenceTime = AdminShopStockSupport.resolveReferenceTimeForMode(
                plugin.getDefaultConfig().getAdminShops(),
                updatedMode
        );
        this.rebaseAllAdminShopReferenceTimes(plugin, updatedReferenceTime);
        this.editedItem.set(
                this.rebaseEditedItemReferenceTime(this.getEditedItem(clickContext), updatedReferenceTime),
                clickContext
        );

        clickContext.update();

        this.i18n("feedback.restock_mode_updated", clickContext.getPlayer())
                .withPlaceholder("restock_mode", this.getAdminRestockModeLabel(clickContext.getPlayer()))
                .build()
                .sendMessage();
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        final RDS plugin = this.rds.get(context);
        final Shop shop = plugin.getShopRepository().findByLocation(this.shopLocation.get(context));
        if (shop != null && shop.isAdminShop()) {
            plugin.getAdminShopRestockScheduler().restockShop(shop);
        }
        return shop;
    }

    private @Nullable AdminShopRestockMode toggleAdminRestockMode(
            final @NotNull RDS plugin
    ) {
        this.syncAllAdminShops(plugin);

        final AdminShopRestockMode currentMode = plugin.getDefaultConfig().getAdminShops().getRestockMode();
        final AdminShopRestockMode updatedMode = currentMode == AdminShopRestockMode.FULL_AT_TIME
                ? AdminShopRestockMode.GRADUAL
                : AdminShopRestockMode.FULL_AT_TIME;
        final File configFile = new File(new File(plugin.getDataFolder(), "config"), "config.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        configuration.set(
                "admin_shops.restock_mode",
                updatedMode == AdminShopRestockMode.FULL_AT_TIME ? "full_at_time" : "gradual"
        );

        try {
            configuration.save(configFile);
            return updatedMode;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save admin restock mode toggle: " + exception.getMessage());
            return null;
        }
    }

    private void syncAllAdminShops(
            final @NotNull RDS plugin
    ) {
        for (final Shop adminShop : plugin.getShopRepository().findAllShops()) {
            if (adminShop.isAdminShop()) {
                plugin.getAdminShopRestockScheduler().restockShop(adminShop);
            }
        }
    }

    private void rebaseAllAdminShopReferenceTimes(
            final @NotNull RDS plugin,
            final long updatedReferenceTime
    ) {
        for (final Shop adminShop : plugin.getShopRepository().findAllShops()) {
            if (!adminShop.isAdminShop()) {
                continue;
            }

            boolean changed = false;
            final List<AbstractItem> updatedItems = new ArrayList<>(adminShop.getItems().size());
            for (final AbstractItem item : adminShop.getItems()) {
                if (item instanceof ShopItem shopItem && shopItem.hasAdminStockLimit()) {
                    final ShopItem updatedItem = this.rebaseItemReferenceTime(shopItem, updatedReferenceTime);
                    if (updatedItem.getAdminStockReferenceTime() != shopItem.getAdminStockReferenceTime()) {
                        changed = true;
                    }
                    updatedItems.add(updatedItem);
                    continue;
                }

                updatedItems.add(item);
            }

            if (changed) {
                adminShop.setItems(updatedItems);
                plugin.getShopRepository().update(adminShop);
            }
        }
    }

    private @NotNull ShopItem rebaseEditedItemReferenceTime(
            final @NotNull ShopItem item,
            final long updatedReferenceTime
    ) {
        if (!item.hasAdminStockLimit()) {
            return item;
        }
        return this.rebaseItemReferenceTime(item, updatedReferenceTime);
    }

    private @NotNull ShopItem rebaseItemReferenceTime(
            final @NotNull ShopItem item,
            final long updatedReferenceTime
    ) {
        return item.withAdminStockSettings(
                item.hasAdminStockLimit() ? item.getAdminStockLimit() : null,
                this.getStoredRestockInterval(item),
                updatedReferenceTime
        );
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
            final Shop currentShop,
            final @NotNull ShopItem item
    ) {
        final ItemStack displayItem = item.getItem();
        displayItem.setAmount(1);

        final String loreKey;
        final Map<String, Object> placeholders;
        if (currentShop != null && currentShop.isAdminShop() && item.hasAdminStockLimit()) {
            loreKey = "summary.admin_limited.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "stock_limit", item.getAdminStockLimit(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue(),
                    "restock_mode", this.getAdminRestockModeLabel(player),
                    "restock_schedule", this.getAdminRestockSchedule(player, item)
            );
        } else if (currentShop != null && currentShop.isAdminShop()) {
            loreKey = "summary.admin_unlimited.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue(),
                    "restock_mode", this.getAdminRestockModeLabel(player)
            );
        } else {
            loreKey = "summary.player.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue()
            );
        }

        final List<Component> lore = new ArrayList<>(this.i18n(loreKey, player)
                .withPlaceholders(placeholders)
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

    private @NotNull ItemStack createStockLimitItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final String stockLimit = item.hasAdminStockLimit()
                ? String.valueOf(item.getAdminStockLimit())
                : this.i18n("stock_limit.unlimited", player)
                        .build()
                        .getI18nVersionWrapper()
                        .asPlaceholder();
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("stock_limit.name", player).build().component())
                .setLore(this.i18n("stock_limit.lore", player)
                        .withPlaceholders(Map.of(
                                "stock_limit", stockLimit,
                                "current_stock", item.getAmount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createResetTimerItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final long resetTimerTicks = item.getAdminRestockIntervalTicks() > 0L
                ? item.getAdminRestockIntervalTicks()
                : JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops().getDefaultResetTimerTicks();
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("reset_timer.name", player).build().component())
                .setLore(this.i18n("reset_timer.lore", player)
                        .withPlaceholder("reset_timer_ticks", resetTimerTicks)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createRestockModeItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.REPEATER)
                .setName(this.i18n("restock_mode.name", player).build().component())
                .setLore(this.i18n("restock_mode.lore", player)
                        .withPlaceholders(Map.of(
                                "restock_mode", this.getAdminRestockModeLabel(player),
                                "restock_schedule", this.getAdminSummaryRestockSchedule(player)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ShopItem mergeEditedItem(
            final @NotNull ShopItem liveItem,
            final @NotNull ShopItem edited
    ) {
        final Integer stockLimit = edited.hasAdminStockLimit() ? edited.getAdminStockLimit() : null;
        final Long restockInterval = this.getStoredRestockInterval(edited);
        final Long referenceTime = edited.getAdminStockReferenceTime() >= 0L
                ? edited.getAdminStockReferenceTime()
                : null;
        final int mergedAmount = stockLimit == null
                ? liveItem.getAmount()
                : Math.min(stockLimit, Math.max(0, edited.getAmount()));

        return new ShopItem(
                liveItem.getEntryId(),
                liveItem.getItem(),
                mergedAmount,
                edited.getCurrencyType(),
                edited.getValue(),
                stockLimit,
                restockInterval,
                referenceTime
        );
    }

    private Long getStoredRestockInterval(
            final @NotNull ShopItem item
    ) {
        return item.getAdminRestockIntervalTicks() > 0L
                ? item.getAdminRestockIntervalTicks()
                : null;
    }

    private @NotNull String getAdminRestockModeLabel(
            final @NotNull Player player
    ) {
        final var adminConfig = JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "restock_mode.full"
                : "restock_mode.gradual";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getAdminRestockSchedule(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final var adminConfig = JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "restock_schedule.full"
                : "restock_schedule.gradual";
        return this.i18n(key, player)
                .withPlaceholders(Map.of(
                        "reset_timer_ticks", item.getAdminRestockIntervalTicks() > 0L
                                ? item.getAdminRestockIntervalTicks()
                                : adminConfig.getDefaultResetTimerTicks(),
                        "restock_time", adminConfig.getFullRestockTime().format(RESTOCK_TIME_FORMAT),
                        "time_zone", adminConfig.getTimeZoneId().getId()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getAdminSummaryRestockSchedule(
            final @NotNull Player player
    ) {
        final var adminConfig = JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "restock_schedule.full"
                : "restock_schedule.gradual";
        return this.i18n(key, player)
                .withPlaceholders(Map.of(
                        "reset_timer_ticks", adminConfig.getDefaultResetTimerTicks(),
                        "restock_time", adminConfig.getFullRestockTime().format(RESTOCK_TIME_FORMAT),
                        "time_zone", adminConfig.getTimeZoneId().getId()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }
}