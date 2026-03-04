/*
 * ShopEditView.java
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
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Renders the shop edit inventory view.
 */
public class ShopEditView extends APaginatedView<ShopEditView.EditableShopEntry> {

    private static final DateTimeFormatter RESTOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    /**
     * Creates a new shop edit view.
     */
    public ShopEditView() {
        super(ShopOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "shop_edit_ui";
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
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final Shop shop = this.getCurrentShop(context);
        final Location location = this.shopLocation.get(context);

        return Map.of(
                "location", this.formatLocation(location),
                "owner", shop == null ? "Unknown" : this.getOwnerName(shop)
        );
    }

    @Override
    protected CompletableFuture<List<EditableShopEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null || !shop.canManage(context.getPlayer().getUniqueId())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<EditableShopEntry> items = new ArrayList<>();
        final List<AbstractItem> shopItems = shop.getItems();
        for (int index = 0; index < shopItems.size(); index++) {
            final AbstractItem item = shopItems.get(index);
            if (item instanceof ShopItem shopItem) {
                items.add(new EditableShopEntry(index, shopItem, shopItem.getEntryId()));
            }
        }

        return CompletableFuture.completedFuture(items);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull EditableShopEntry entry
    ) {
        final Player player = context.getPlayer();
        final Shop shop = this.getCurrentShop(context);
        final ItemStack displayItem = this.createDisplayItem(entry.item());

        builder.withItem(
                UnifiedBuilderFactory.item(displayItem)
                        .setLore(this.buildEntryLore(player, shop, entry.item(), displayItem))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(clickContext -> this.handleEntryClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Shop shop = this.getCurrentShop(render);

        if (shop == null) {
            render.slot(4).renderWith(() -> this.createMissingShopItem(player));
            return;
        }

        if (!shop.canManage(player.getUniqueId())) {
            render.slot(4).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot(
                's',
                this.createSummaryItem(player, shop)
        );
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        final RDS plugin = this.rds.get(context);
        final Shop shop = plugin.getShopRepository().findByLocation(this.shopLocation.get(context));
        if (shop != null && shop.isAdminShop()) {
            plugin.getAdminShopRestockScheduler().restockShop(shop);
        }
        return shop;
    }

    private void handleEntryClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull EditableShopEntry entry
    ) {
        if (clickContext.isRightClick()) {
            this.handleRemoveClick(clickContext, entry);
            return;
        }

        clickContext.openForPlayer(
                ShopItemEditView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "shopLocation", this.shopLocation.get(clickContext),
                        "shopItem", entry.item()
                )
        );
    }

    private void handleRemoveClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull EditableShopEntry entry
    ) {
        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (!shop.canManage(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final List<AbstractItem> items = new ArrayList<>(shop.getItems());
        final int removalIndex = this.findRemovalIndex(items, entry);
        if (removalIndex < 0 || removalIndex >= items.size() || !(items.get(removalIndex) instanceof ShopItem removedItem)) {
            this.i18n("feedback.item_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        items.remove(removalIndex);
        shop.setItems(items);
        this.rds.get(clickContext).getShopRepository().update(shop);
        this.grantItem(clickContext.getPlayer(), removedItem);

        this.i18n("feedback.removed", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "amount", removedItem.getAmount(),
                        "item_type", removedItem.getItem().getType().name(),
                        "currency_type", removedItem.getCurrencyType(),
                        "value", removedItem.getValue(),
                        "stored_count", shop.getStoredItemCount()
                ))
                .includePrefix()
                .build()
                .sendMessage();

        this.openFreshView(clickContext);
    }

    private int findRemovalIndex(
            final @NotNull List<AbstractItem> items,
            final @NotNull EditableShopEntry entry
    ) {
        if (entry.originalIndex() >= 0 && entry.originalIndex() < items.size()) {
            final AbstractItem indexed = items.get(entry.originalIndex());
            if (indexed instanceof ShopItem shopItem && shopItem.getEntryId().equals(entry.entryId())) {
                return entry.originalIndex();
            }
        }

        for (int index = 0; index < items.size(); index++) {
            final AbstractItem candidate = items.get(index);
            if (candidate instanceof ShopItem shopItem && shopItem.getEntryId().equals(entry.entryId())) {
                return index;
            }
        }

        return -1;
    }

    private @NotNull ItemStack createDisplayItem(
            final @NotNull ShopItem item
    ) {
        final ItemStack displayItem = item.getItem();
        displayItem.setAmount(1);
        return displayItem;
    }

    private @NotNull List<Component> buildEntryLore(
            final @NotNull Player player,
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull ItemStack displayItem
    ) {
        final String loreKey;
        final Map<String, Object> placeholders;
        if (!shop.isAdminShop()) {
            loreKey = "entry.player.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "item_type", displayItem.getType().name(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue()
            );
        } else if (AdminShopStockSupport.usesLimitedAdminStock(shop, item)) {
            loreKey = "entry.admin_limited.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "stock_limit", item.getAdminStockLimit(),
                    "item_type", displayItem.getType().name(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue(),
                    "restock_mode", this.getAdminRestockModeLabel(player),
                    "restock_schedule", this.getAdminRestockSchedule(player, item)
            );
        } else {
            loreKey = "entry.admin_unlimited.lore";
            placeholders = Map.of(
                    "item_type", displayItem.getType().name(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue(),
                    "restock_mode", this.getAdminRestockModeLabel(player)
            );
        }

        final List<Component> lore = new ArrayList<>(this.i18n(loreKey, player)
                .withPlaceholders(placeholders)
                .build()
                .children());

        final var originalLore = displayItem.lore();
        if (originalLore != null && !originalLore.isEmpty()) {
            lore.add(Component.empty());
            lore.addAll(originalLore);
        }

        return lore;
    }

    private void grantItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        int remaining = item.getAmount();
        final ItemStack template = item.getItem();
        final int maxStack = template.getMaxStackSize();
        final List<ItemStack> stacks = new ArrayList<>();

        while (remaining > 0) {
            final int stackAmount = Math.min(remaining, maxStack);
            final ItemStack stack = template.clone();
            stack.setAmount(stackAmount);
            stacks.add(stack);
            remaining -= stackAmount;
        }

        this.giveOrDrop(player, stacks);
    }

    private void giveOrDrop(
            final @NotNull Player player,
            final @NotNull Collection<ItemStack> stacks
    ) {
        player.getInventory().addItem(stacks.toArray(new ItemStack[0]))
                .forEach((slot, item) -> player.getWorld().dropItem(
                        player.getLocation().clone().add(0, 0.5, 0),
                        item
                ));
    }

    private void openFreshView(
            final @NotNull Context context
    ) {
        context.openForPlayer(
                ShopEditView.class,
                Map.of(
                        "plugin", this.rds.get(context),
                        "shopLocation", this.shopLocation.get(context)
                )
        );
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        final String loreKey = shop.isAdminShop()
                ? "summary.admin.lore"
                : "summary.player.lore";
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n(loreKey, player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(shop),
                                "location", this.formatLocation(shop.getShopLocation()),
                                "stored_count", shop.getStoredItemCount(),
                                "restock_mode", this.getAdminRestockModeLabel(player),
                                "restock_schedule", this.getAdminSummaryRestockSchedule(player)
                        ))
                        .build()
                        .children())
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

    private @NotNull ItemStack createMissingShopItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.shop_missing.name", player).build().component())
                .setLore(this.i18n("feedback.shop_missing.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull String formatLocation(final @NotNull Location location) {
        return "("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private @NotNull String getOwnerName(final @NotNull Shop shop) {
        final String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        return ownerName == null ? shop.getOwner().toString() : ownerName;
    }

    private @NotNull String getAdminRestockModeLabel(
            final @NotNull Player player
    ) {
        final var adminConfig = JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "entry.admin_mode.full"
                : "entry.admin_mode.gradual";
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
                ? "entry.restock_schedule.full"
                : "entry.restock_schedule.gradual";
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
                ? "entry.restock_schedule.full"
                : "summary.restock_schedule.gradual";
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

    /**
     * Represents editable shop entry.
     *
     * @param originalIndex original index
     * @param item target item payload
     * @param entryId entry id
     */
    public record EditableShopEntry(
            int originalIndex,
            @NotNull ShopItem item,
            @NotNull UUID entryId
    ) {
    }
}