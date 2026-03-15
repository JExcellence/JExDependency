package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerEntry;
import com.raindropcentral.rds.database.entity.ShopLedgerType;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Renders the shop ledger inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopLedgerView extends APaginatedView<ShopLedgerEntry> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    /**
     * Creates a new shop ledger view.
     */
    public ShopLedgerView() {
        super(ShopOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "shop_ledger_ui";
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
    protected CompletableFuture<List<ShopLedgerEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null || !this.canManage(context, shop)) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<ShopLedgerEntry> entries = new ArrayList<>(shop.getLedgerEntries());
        entries.sort(Comparator
                .comparing(ShopLedgerEntry::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(ShopLedgerEntry::getId, Comparator.nullsLast(Long::compareTo))
                .reversed());
        return CompletableFuture.completedFuture(entries);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ShopLedgerEntry entry
    ) {
        builder.withItem(this.createEntryItem(context.getPlayer(), entry));
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

        if (!this.canManage(render, shop)) {
            render.slot(4).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot(
                's',
                this.createSummaryItem(player, shop)
        );

        if (shop.getLedgerEntries().isEmpty()) {
            render.slot(4).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Executes onClick.
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private Shop getCurrentShop(
            final @NotNull Context context
    ) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(shop),
                                "location", this.formatLocation(shop.getShopLocation()),
                                "ledger_count", shop.getLedgerEntryCount(),
                                "purchase_count", shop.getLedgerEntryCount(ShopLedgerType.PURCHASE),
                                "tax_count", shop.getLedgerEntryCount(ShopLedgerType.TAXATION)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Player player,
            final @NotNull ShopLedgerEntry entry
    ) {
        final String typeKey = entry.getEntryType().name().toLowerCase(Locale.ROOT);
        final String currencyName = this.getCurrencyDisplayName(entry.getCurrencyType());
        return UnifiedBuilderFactory.item(this.resolveMaterial(entry.getEntryType()))
                .setName(this.i18n("entry." + typeKey + ".name", player)
                        .withPlaceholders(this.createEntryPlaceholders(entry, currencyName))
                        .build()
                        .component())
                .setLore(this.i18n("entry." + typeKey + ".lore", player)
                        .withPlaceholders(this.createEntryPlaceholders(entry, currencyName))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull Map<String, Object> createEntryPlaceholders(
            final @NotNull ShopLedgerEntry entry,
            final @NotNull String currencyName
    ) {
        return Map.of(
                "actor", entry.getActorName(),
                "amount", this.formatAmount(entry.getAmount()),
                "currency_type", entry.getCurrencyType(),
                "currency_name", currencyName,
                "item_type", entry.getItemType() == null ? "Unknown" : entry.getItemType(),
                "item_amount", entry.getItemAmount() == null ? 0 : entry.getItemAmount(),
                "counted_shops", entry.getCountedShops() == null ? 0 : entry.getCountedShops(),
                "created_at", this.formatDate(entry.getCreatedAt())
        );
    }

    private @NotNull Material resolveMaterial(
            final @NotNull ShopLedgerType ledgerType
    ) {
        return switch (ledgerType) {
            case PURCHASE -> Material.EMERALD;
            case TAXATION -> Material.CLOCK;
        };
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

    private @NotNull String getCurrencyDisplayName(
            final @NotNull String currencyType
    ) {
        if ("vault".equalsIgnoreCase(currencyType)) {
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

    private @NotNull String formatDate(
            final LocalDateTime dateTime
    ) {
        return dateTime == null ? "Unknown" : dateTime.format(DATE_FORMAT);
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

    private boolean canManage(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        return shop.canManage(context.getPlayer().getUniqueId()) || ShopAdminAccessSupport.hasOwnerOverride(context);
    }
}
