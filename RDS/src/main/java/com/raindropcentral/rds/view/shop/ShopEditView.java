package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ShopEditView extends APaginatedView<ShopItem> {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

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
    protected CompletableFuture<List<ShopItem>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);
        if (shop == null || !shop.isOwner(context.getPlayer().getUniqueId())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<ShopItem> items = new ArrayList<>();
        for (AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem) {
                items.add(shopItem);
            }
        }

        return CompletableFuture.completedFuture(items);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ShopItem entry
    ) {
        final Player player = context.getPlayer();
        final ItemStack displayItem = this.createDisplayItem(entry);

        builder.withItem(
                UnifiedBuilderFactory.item(displayItem)
                        .setLore(this.buildEntryLore(player, entry, displayItem))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(clickContext -> clickContext.openForPlayer(
                ShopItemEditView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "shopLocation", this.shopLocation.get(clickContext),
                        "shopItem", entry
                )
        ));
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
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
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
            final @NotNull ShopItem item,
            final @NotNull ItemStack displayItem
    ) {
        final List<Component> lore = new ArrayList<>(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                        "amount", item.getAmount(),
                        "item_type", displayItem.getType().name(),
                        "currency_type", item.getCurrencyType(),
                        "value", item.getValue()
                ))
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
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
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
}
