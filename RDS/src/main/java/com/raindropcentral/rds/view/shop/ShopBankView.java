package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Bank;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopBankView extends APaginatedView<ShopBankView.BankViewEntry> {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    public ShopBankView() {
        super(ShopOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "shop_bank_ui";
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
    protected CompletableFuture<List<BankViewEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null || !shop.isOwner(context.getPlayer().getUniqueId())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<BankViewEntry> entries = new ArrayList<>();
        for (final Bank bankEntry : shop.getBankEntries()) {
            if (bankEntry.getAmount() > 0D) {
                entries.add(new BankViewEntry(
                        bankEntry.getId(),
                        bankEntry.getCurrencyType(),
                        bankEntry.getAmount()
                ));
            }
        }

        entries.sort(Comparator.comparing(BankViewEntry::currencyType, String.CASE_INSENSITIVE_ORDER));
        return CompletableFuture.completedFuture(entries);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull BankViewEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry))
                .onClick(clickContext -> this.handleWithdrawClick(clickContext, entry));
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

        if (!shop.isOwner(player.getUniqueId())) {
            render.slot(4).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot(
                's',
                this.createSummaryItem(player, shop)
        );

        if (shop.getBankEntries().isEmpty()) {
            render.slot(4).renderWith(() -> this.createEmptyItem(player));
        }
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleWithdrawClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull BankViewEntry entry
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

        if (!shop.isOwner(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final Bank currentEntry = this.findMatchingEntry(shop, entry);
        if (currentEntry == null || currentEntry.getAmount() <= 0D) {
            this.i18n("feedback.entry_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final String currencyType = currentEntry.getCurrencyType();
        final String currencyName = this.getCurrencyDisplayName(currencyType);
        final double withdrawAmount = currentEntry.getAmount();
        final WithdrawalResult withdrawalResult = this.tryWithdrawToOwner(
                clickContext,
                currencyType,
                withdrawAmount
        );

        if (!withdrawalResult.success()) {
            this.i18n(withdrawalResult.failureKey(), clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "amount", this.formatAmount(withdrawAmount),
                            "currency_type", currencyType,
                            "currency_name", currencyName
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (!shop.withdrawBank(currencyType, withdrawAmount)) {
            this.i18n("feedback.entry_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        this.rds.get(clickContext).getShopRepository().update(shop);

        this.i18n("feedback.withdrawn", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "amount", this.formatAmount(withdrawAmount),
                        "remaining_amount", this.formatAmount(shop.getBankAmount(currencyType)),
                        "currency_type", currencyType,
                        "currency_name", currencyName,
                        "bank_currency_count", shop.getBankCurrencyCount()
                ))
                .includePrefix()
                .build()
                .sendMessage();

        this.openFreshView(clickContext);
    }

    private @Nullable Bank findMatchingEntry(
            final @NotNull Shop shop,
            final @NotNull BankViewEntry entry
    ) {
        for (final Bank bankEntry : shop.getBankEntries()) {
            if (entry.entryId() != null && entry.entryId().equals(bankEntry.getId())) {
                return bankEntry;
            }

            if (bankEntry.getCurrencyType().equalsIgnoreCase(entry.currencyType())) {
                return bankEntry;
            }
        }

        return null;
    }

    private @NotNull WithdrawalResult tryWithdrawToOwner(
            final @NotNull Context context,
            final @NotNull String currencyType,
            final double amount
    ) {
        if (amount <= 0D) {
            return WithdrawalResult.successful();
        }

        final Player player = context.getPlayer();
        if (this.usesVaultCurrency(currencyType)) {
            final RDS plugin = this.rds.get(context);
            if (!plugin.hasVaultEconomy()) {
                return WithdrawalResult.failure("feedback.currency_unavailable");
            }

            return plugin.depositVault(player, amount)
                    ? WithdrawalResult.successful()
                    : WithdrawalResult.failure("feedback.withdraw_failed");
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null || !this.hasCustomCurrency(bridge, currencyType, context)) {
            return WithdrawalResult.failure("feedback.currency_unavailable");
        }

        return this.depositCustomCurrency(bridge, player, currencyType, amount)
                ? WithdrawalResult.successful()
                : WithdrawalResult.failure("feedback.withdraw_failed");
    }

    private boolean hasCustomCurrency(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull String currencyType,
            final @NotNull Context context
    ) {
        try {
            final Method hasCurrencyMethod = JExEconomyBridge.class.getMethod("hasCurrency", String.class);
            return Boolean.TRUE.equals(hasCurrencyMethod.invoke(bridge, currencyType));
        } catch (ReflectiveOperationException ignored) {
            try {
                final Method findCurrencyMethod = JExEconomyBridge.class.getDeclaredMethod("findCurrency", String.class);
                findCurrencyMethod.setAccessible(true);
                return findCurrencyMethod.invoke(bridge, currencyType) != null;
            } catch (ReflectiveOperationException exception) {
                this.rds.get(context).getLogger().fine("Failed to resolve custom currency for shop bank withdrawal: " + currencyType);
                return false;
            }
        }
    }

    private boolean depositCustomCurrency(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull OfflinePlayer player,
            final @NotNull String currencyType,
            final double amount
    ) {
        try {
            final Method depositMethod = JExEconomyBridge.class.getMethod(
                    "deposit",
                    OfflinePlayer.class,
                    String.class,
                    double.class
            );
            final Object futureObject = depositMethod.invoke(bridge, player, currencyType, amount);
            if (futureObject instanceof CompletableFuture<?> future) {
                return Boolean.TRUE.equals(future.join());
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return this.invokeBridgeAdapterDeposit(bridge, player, currencyType, amount);
    }

    private boolean invokeBridgeAdapterDeposit(
            final @NotNull JExEconomyBridge bridge,
            final @NotNull OfflinePlayer player,
            final @NotNull String currencyType,
            final double amount
    ) {
        try {
            final Method findCurrencyMethod = JExEconomyBridge.class.getDeclaredMethod("findCurrency", String.class);
            findCurrencyMethod.setAccessible(true);
            final Object currency = findCurrencyMethod.invoke(bridge, currencyType);
            if (currency == null) {
                return false;
            }

            final Field adapterField = JExEconomyBridge.class.getDeclaredField("adapter");
            final Field adapterClassField = JExEconomyBridge.class.getDeclaredField("adapterClass");
            final Field responseClassField = JExEconomyBridge.class.getDeclaredField("responseClass");
            final Field currencyClassField = JExEconomyBridge.class.getDeclaredField("currencyClass");

            adapterField.setAccessible(true);
            adapterClassField.setAccessible(true);
            responseClassField.setAccessible(true);
            currencyClassField.setAccessible(true);

            final Object adapter = adapterField.get(bridge);
            final Class<?> adapterClass = (Class<?>) adapterClassField.get(bridge);
            final Class<?> responseClass = (Class<?>) responseClassField.get(bridge);
            final Class<?> currencyClass = (Class<?>) currencyClassField.get(bridge);

            final Method depositMethod = adapterClass.getMethod(
                    "deposit",
                    OfflinePlayer.class,
                    currencyClass,
                    double.class
            );
            final Object futureObject = depositMethod.invoke(adapter, player, currency, amount);
            if (!(futureObject instanceof CompletableFuture<?> future)) {
                return false;
            }

            final Object response = future.join();
            final Method isSuccessMethod = responseClass.getMethod("isSuccess");
            return Boolean.TRUE.equals(isSuccessMethod.invoke(response));
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private void openFreshView(
            final @NotNull Context context
    ) {
        context.openForPlayer(
                ShopBankView.class,
                Map.of(
                        "plugin", this.rds.get(context),
                        "shopLocation", this.shopLocation.get(context)
                )
        );
    }

    private Shop getCurrentShop(
            final @NotNull Context context
    ) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Player player,
            final @NotNull BankViewEntry entry
    ) {
        final String currencyName = this.getCurrencyDisplayName(entry.currencyType());
        final Material material = this.usesVaultCurrency(entry.currencyType())
                ? Material.GOLD_INGOT
                : Material.EMERALD;

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("entry.name", player)
                        .withPlaceholders(Map.of(
                                "currency_name", currencyName
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "amount", this.formatAmount(entry.amount()),
                                "currency_type", entry.currencyType(),
                                "currency_name", currencyName
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(shop),
                                "location", this.formatLocation(shop.getShopLocation()),
                                "bank_currency_count", shop.getBankCurrencyCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
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

    private boolean usesVaultCurrency(
            final @NotNull String currencyType
    ) {
        return "vault".equalsIgnoreCase(currencyType);
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

    private @NotNull String formatAmount(
            final double amount
    ) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private @NotNull String formatLocation(
            final @NotNull Location location
    ) {
        return "("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private @NotNull String getOwnerName(
            final @NotNull Shop shop
    ) {
        final String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        return ownerName == null ? shop.getOwner().toString() : ownerName;
    }

    public record BankViewEntry(
            @Nullable Long entryId,
            @NotNull String currencyType,
            double amount
    ) {
    }

    private record WithdrawalResult(
            boolean success,
            @NotNull String failureKey
    ) {
        private static @NotNull WithdrawalResult successful() {
            return new WithdrawalResult(true, "");
        }

        private static @NotNull WithdrawalResult failure(
                final @NotNull String failureKey
        ) {
            return new WithdrawalResult(false, failureKey);
        }
    }
}
