package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.AdminShopRestockMode;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.AdminShopStockSupport;
import com.raindropcentral.rds.view.shop.anvil.ShopPurchaseAmountAnvilView;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.time.format.DateTimeFormatter;

public class ShopCustomerView extends APaginatedView<ShopCustomerView.CustomerShopEntry> {

    private static final DateTimeFormatter RESTOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    @Override
    protected String getKey() {
        return "shop_customer_ui";
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
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        final PurchaseRequest request = this.extractPurchaseRequest(target) != null
                ? this.extractPurchaseRequest(target)
                : this.extractPurchaseRequest(origin);

        if (request == null) {
            target.update();
            return;
        }

        this.handlePurchaseRequest(
                target,
                null,
                request.item(),
                request.amount()
        );
    }

    @Override
    protected CompletableFuture<List<CustomerShopEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<CustomerShopEntry> entries = new ArrayList<>();
        final List<AbstractItem> items = shop.getItems();
        for (int index = 0; index < items.size(); index++) {
            final AbstractItem item = items.get(index);
            if (item instanceof ShopItem shopItem && (shop.isAdminShop() || shopItem.getAmount() > 0)) {
                entries.add(new CustomerShopEntry(index, shopItem, shopItem.getEntryId()));
            }
        }

        return CompletableFuture.completedFuture(entries);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull CustomerShopEntry entry
    ) {
        final Player player = context.getPlayer();
        final Shop shop = this.getCurrentShop(context);
        if (shop == null) {
            return;
        }
        final ItemStack displayItem = this.createDisplayItem(entry.item());

        builder.withItem(
                UnifiedBuilderFactory.item(displayItem)
                        .setLore(this.buildEntryLore(player, shop, entry.item(), displayItem))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(clickContext -> {
            if (clickContext.isRightClick()) {
                clickContext.openForPlayer(
                        ShopPurchaseAmountAnvilView.class,
                        Map.of(
                                "plugin", this.rds.get(clickContext),
                                "shopLocation", this.shopLocation.get(clickContext),
                                "shopItem", entry.item()
                        )
                );
                return;
            }

            if (clickContext.isLeftClick()) {
                this.handlePurchaseRequest(clickContext, entry, entry.item(), 1);
            }
        });
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

        render.layoutSlot(
                's',
                this.createSummaryItem(player, shop)
        );

        if (!this.hasVisibleEntries(shop)) {
            render.slot(4).renderWith(() -> this.createEmptyShopItem(player));
        }
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handlePurchaseRequest(
            final @NotNull Context context,
            final @Nullable CustomerShopEntry entry,
            final @NotNull ShopItem referenceItem,
            final int desiredAmount
    ) {
        if (desiredAmount < 1) {
            this.i18n("feedback.invalid_amount", context.getPlayer())
                    .build()
                    .sendMessage();
            this.openFreshView(context);
            return;
        }

        final Shop shop = this.getCurrentShop(context);
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", context.getPlayer())
                    .build()
                    .sendMessage();
            this.openFreshView(context);
            return;
        }

        final List<AbstractItem> items = new ArrayList<>(shop.getItems());
        final int matchingIndex = this.findMatchingItemIndex(items, entry, referenceItem);
        if (matchingIndex < 0 || !(items.get(matchingIndex) instanceof ShopItem currentItem)) {
            this.i18n("feedback.item_missing", context.getPlayer())
                    .build()
                    .sendMessage();
            this.openFreshView(context);
            return;
        }

        final boolean limitedAdminStock = AdminShopStockSupport.usesLimitedAdminStock(shop, currentItem);
        if ((!shop.isAdminShop() || limitedAdminStock) && desiredAmount > currentItem.getAmount()) {
            this.i18n("feedback.insufficient_stock", context.getPlayer())
                    .withPlaceholders(Map.of(
                            "amount", desiredAmount,
                            "available_amount", currentItem.getAmount(),
                            "item_type", currentItem.getItem().getType().name()
                    ))
                    .build()
                    .sendMessage();
            this.openFreshView(context);
            return;
        }

        final double totalPrice = currentItem.getValue() * desiredAmount;
        final String currencyName = this.getCurrencyDisplayName(currentItem.getCurrencyType());
        final PaymentResult paymentResult = this.tryChargePlayer(
                context,
                currentItem.getCurrencyType(),
                totalPrice
        );

        if (!paymentResult.success()) {
            this.i18n(paymentResult.failureKey(), context.getPlayer())
                    .withPlaceholders(Map.of(
                            "amount", desiredAmount,
                            "item_type", currentItem.getItem().getType().name(),
                            "currency_type", currentItem.getCurrencyType(),
                            "currency_name", currencyName,
                            "total_price", this.formatAmount(totalPrice)
                    ))
                    .build()
                    .sendMessage();
            this.openFreshView(context);
            return;
        }

        final boolean adminShop = shop.isAdminShop();
        final boolean unlimitedAdminStock = AdminShopStockSupport.isUnlimitedAdminStock(shop, currentItem);
        final int remainingAmount = (adminShop && unlimitedAdminStock)
                ? currentItem.getAmount()
                : currentItem.getAmount() - desiredAmount;
        if (!adminShop) {
            if (remainingAmount > 0) {
                items.set(matchingIndex, currentItem.withAmount(remainingAmount));
            } else {
                items.remove(matchingIndex);
            }

            shop.setItems(items);
        } else if (limitedAdminStock) {
            items.set(
                    matchingIndex,
                    AdminShopStockSupport.consumeLimitedStock(
                            this.rds.get(context),
                            currentItem,
                            remainingAmount
                    )
            );
            shop.setItems(items);
        }

        shop.addBank(currentItem.getCurrencyType(), totalPrice);
        shop.addLedgerEntry(
                ShopLedgerEntry.purchase(
                        shop,
                        context.getPlayer().getUniqueId(),
                        context.getPlayer().getName(),
                        currentItem.getCurrencyType(),
                        totalPrice,
                        currentItem.getItem().getType().name(),
                        desiredAmount
                )
        );
        this.rds.get(context).getShopRepository().update(shop);
        this.grantPurchasedItems(context.getPlayer(), currentItem, desiredAmount);

        final String feedbackKey;
        if (!adminShop) {
            feedbackKey = "feedback.purchased_player";
        } else if (limitedAdminStock) {
            feedbackKey = "feedback.purchased_admin_limited";
        } else {
            feedbackKey = "feedback.purchased_admin";
        }
        this.i18n(feedbackKey, context.getPlayer())
                .withPlaceholders(Map.of(
                        "amount", desiredAmount,
                        "remaining_amount", Math.max(remainingAmount, 0),
                        "item_type", currentItem.getItem().getType().name(),
                        "currency_type", currentItem.getCurrencyType(),
                        "currency_name", currencyName,
                        "price_each", this.formatAmount(currentItem.getValue()),
                        "total_price", this.formatAmount(totalPrice)
                ))
                .build()
                .sendMessage();

        this.openFreshView(context);
    }

    private @Nullable PurchaseRequest extractPurchaseRequest(
            final @NotNull Context context
    ) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return null;
        }

        final Object itemObject = data.get("shopItem");
        final Object amountObject = data.get("purchaseAmount");
        if (!(itemObject instanceof ShopItem shopItem) || !(amountObject instanceof Number amount)) {
            return null;
        }

        return new PurchaseRequest(shopItem, amount.intValue());
    }

    private int findMatchingItemIndex(
            final @NotNull List<AbstractItem> items,
            final @Nullable CustomerShopEntry entry,
            final @NotNull ShopItem referenceItem
    ) {
        if (entry != null && entry.originalIndex() >= 0 && entry.originalIndex() < items.size()) {
            final AbstractItem indexed = items.get(entry.originalIndex());
            if (indexed instanceof ShopItem shopItem && shopItem.getEntryId().equals(entry.entryId())) {
                return entry.originalIndex();
            }
        }

        for (int index = 0; index < items.size(); index++) {
            final AbstractItem candidate = items.get(index);
            if (candidate instanceof ShopItem shopItem && shopItem.getEntryId().equals(referenceItem.getEntryId())) {
                return index;
            }
        }

        for (int index = 0; index < items.size(); index++) {
            final AbstractItem candidate = items.get(index);
            if (!(candidate instanceof ShopItem shopItem)) {
                continue;
            }

            if (this.isLegacyMatch(shopItem, referenceItem)) {
                return index;
            }
        }

        return -1;
    }

    private boolean isLegacyMatch(
            final @NotNull ShopItem candidate,
            final @NotNull ShopItem referenceItem
    ) {
        return candidate.getItem().isSimilar(referenceItem.getItem())
                && candidate.getCurrencyType().equalsIgnoreCase(referenceItem.getCurrencyType())
                && Double.compare(candidate.getValue(), referenceItem.getValue()) == 0;
    }

    private @NotNull PaymentResult tryChargePlayer(
            final @NotNull Context context,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return PaymentResult.successful();
        }

        if (this.usesVaultCurrency(currencyType)) {
            final RDS plugin = this.rds.get(context);
            if (!plugin.hasVaultEconomy()) {
                return PaymentResult.failure("feedback.currency_unavailable");
            }

            if (!plugin.hasVaultFunds(context.getPlayer(), amount)) {
                return PaymentResult.failure("feedback.insufficient_funds");
            }

            return plugin.withdrawVault(context.getPlayer(), amount)
                    ? PaymentResult.successful()
                    : PaymentResult.failure("feedback.insufficient_funds");
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null) {
            return PaymentResult.failure("feedback.currency_unavailable");
        }

        if (!bridge.has(context.getPlayer(), currencyType, amount)) {
            return PaymentResult.failure("feedback.insufficient_funds");
        }

        return bridge.withdraw(context.getPlayer(), currencyType, amount).join()
                ? PaymentResult.successful()
                : PaymentResult.failure("feedback.insufficient_funds");
    }

    private boolean usesVaultCurrency(
            final @NotNull String currencyType
    ) {
        return "vault".equalsIgnoreCase(currencyType);
    }

    private void grantPurchasedItems(
            final @NotNull Player player,
            final @NotNull ShopItem item,
            final int amount
    ) {
        int remaining = amount;
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
                ShopCustomerView.class,
                Map.of(
                        "plugin", this.rds.get(context),
                        "shopLocation", this.shopLocation.get(context)
                )
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

    private @NotNull ItemStack createDisplayItem(
            final @NotNull ShopItem item
    ) {
        final ItemStack displayItem = item.getItem().clone();
        displayItem.setAmount(1);
        return displayItem;
    }

    private @NotNull List<Component> buildEntryLore(
            final @NotNull Player player,
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull ItemStack displayItem
    ) {
        final double totalPrice = item.getValue() * item.getAmount();
        final String loreKey;
        final Map<String, Object> placeholders;
        if (!shop.isAdminShop()) {
            loreKey = "entry.player.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "item_type", displayItem.getType().name(),
                    "currency_type", item.getCurrencyType(),
                    "currency_name", this.getCurrencyDisplayName(item.getCurrencyType()),
                    "price_each", this.formatAmount(item.getValue()),
                    "total_price", this.formatAmount(totalPrice)
            );
        } else if (AdminShopStockSupport.usesLimitedAdminStock(shop, item)) {
            loreKey = "entry.admin_limited.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "stock_limit", item.getAdminStockLimit(),
                    "item_type", displayItem.getType().name(),
                    "currency_type", item.getCurrencyType(),
                    "currency_name", this.getCurrencyDisplayName(item.getCurrencyType()),
                    "price_each", this.formatAmount(item.getValue()),
                    "restock_mode", this.getAdminRestockModeLabel(player),
                    "restock_schedule", this.getAdminRestockSchedule(player, item)
            );
        } else {
            loreKey = "entry.admin.lore";
            placeholders = Map.of(
                    "item_type", displayItem.getType().name(),
                    "currency_type", item.getCurrencyType(),
                    "currency_name", this.getCurrencyDisplayName(item.getCurrencyType()),
                    "price_each", this.formatAmount(item.getValue())
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

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        final String loreKey = shop.isAdminShop()
                ? "summary.admin.lore"
                : "summary.player.lore";
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n(loreKey, player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(shop),
                                "location", this.formatLocation(shop.getShopLocation()),
                                "stored_count", shop.getStoredItemCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private boolean hasVisibleEntries(
            final @NotNull Shop shop
    ) {
        for (final AbstractItem item : shop.getItems()) {
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (shop.isAdminShop() || shopItem.getAmount() > 0) {
                return true;
            }
        }

        return false;
    }

    private @NotNull ItemStack createEmptyShopItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty_shop.name", player).build().component())
                .setLore(this.i18n("feedback.empty_shop.lore", player).build().children())
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

    private @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if (this.usesVaultCurrency(currencyType)) {
            return "Vault";
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge == null ? currencyType : bridge.getCurrencyDisplayName(currencyType);
    }

    private @NotNull String formatAmount(final double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private @NotNull String getAdminRestockModeLabel(
            final @NotNull Player player
    ) {
        final String key = JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops().getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
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
        final var adminShopConfig = JavaPlugin.getPlugin(RDS.class).getDefaultConfig().getAdminShops();
        final String key = adminShopConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "entry.restock_schedule.full"
                : "entry.restock_schedule.gradual";
        return this.i18n(key, player)
                .withPlaceholders(Map.of(
                        "reset_timer_ticks", item.getAdminRestockIntervalTicks() > 0L
                                ? item.getAdminRestockIntervalTicks()
                                : adminShopConfig.getDefaultResetTimerTicks(),
                        "restock_time", adminShopConfig.getFullRestockTime().format(RESTOCK_TIME_FORMAT),
                        "time_zone", adminShopConfig.getTimeZoneId().getId()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    public record CustomerShopEntry(
            int originalIndex,
            @NotNull ShopItem item,
            @NotNull UUID entryId
    ) {
    }

    private record PurchaseRequest(
            @NotNull ShopItem item,
            int amount
    ) {
    }

    private record PaymentResult(
            boolean success,
            @NotNull String failureKey
    ) {
        private static @NotNull PaymentResult successful() {
            return new PaymentResult(true, "");
        }

        private static @NotNull PaymentResult failure(final @NotNull String failureKey) {
            return new PaymentResult(false, failureKey);
        }
    }
}
