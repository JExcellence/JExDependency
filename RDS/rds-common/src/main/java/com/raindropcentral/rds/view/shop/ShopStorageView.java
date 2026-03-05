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
import me.devnatan.inventoryframework.context.SlotClickContext;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Renders the shop storage inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopStorageView extends APaginatedView<ShopStorageView.StoredShopEntry> {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    /**
     * Creates a new shop storage view.
     */
    public ShopStorageView() {
        super(ShopOverviewView.class);
    }

    @Override
    protected String getKey() {
        return "shop_storage_ui";
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
    protected CompletableFuture<List<StoredShopEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final Shop shop = this.getCurrentShop(context);

        if (shop == null || !shop.canManage(context.getPlayer().getUniqueId())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<StoredShopEntry> entries = new ArrayList<>();
        final List<AbstractItem> items = shop.getItems();
        for (int index = 0; index < items.size(); index++) {
            final AbstractItem item = items.get(index);
            if (item instanceof ShopItem shopItem) {
                entries.add(new StoredShopEntry(
                        index,
                        shopItem,
                        shopItem.getEntryId()
                ));
            }
        }

        return CompletableFuture.completedFuture(entries);
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull StoredShopEntry entry
    ) {
        final Player player = context.getPlayer();
        final ItemStack displayItem = this.createDisplayItem(entry.item());

        builder.withItem(
                UnifiedBuilderFactory.item(displayItem)
                        .setLore(this.buildEntryLore(player, entry.item(), displayItem))
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(clickContext -> this.handleExtractClick(clickContext, entry));
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

    private void handleExtractClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull StoredShopEntry entry
    ) {
        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
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

        final List<AbstractItem> items = new ArrayList<>(shop.getItems());
        final int removalIndex = this.findRemovalIndex(items, entry);

        if (removalIndex < 0 || removalIndex >= items.size() || !(items.get(removalIndex) instanceof ShopItem extracted)) {
            this.i18n("feedback.item_missing", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            clickContext.openForPlayer(
                    ShopStorageView.class,
                    Map.of(
                            "plugin", this.rds.get(clickContext),
                            "shopLocation", this.shopLocation.get(clickContext)
                    )
            );
            return;
        }

        items.remove(removalIndex);
        shop.setItems(items);
        this.rds.get(clickContext).getShopRepository().update(shop);
        this.grantItem(clickContext.getPlayer(), extracted);

        this.i18n("feedback.extracted", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "amount", extracted.getAmount(),
                        "item_type", extracted.getItem().getType().name(),
                        "currency_type", extracted.getCurrencyType(),
                        "value", extracted.getValue(),
                        "stored_count", shop.getStoredItemCount()
                ))
                .includePrefix()
                .build()
                .sendMessage();

        clickContext.openForPlayer(
                ShopStorageView.class,
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "shopLocation", this.shopLocation.get(clickContext)
                )
        );
    }

    private int findRemovalIndex(
            final @NotNull List<AbstractItem> items,
            final @NotNull StoredShopEntry entry
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

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        return UnifiedBuilderFactory.item(Material.BARREL)
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

    /**
     * Represents stored shop entry.
     *
     * @param originalIndex original index
     * @param item target item payload
     * @param entryId entry id
     */
    public record StoredShopEntry(
            int originalIndex,
            @NotNull ShopItem item,
            @NotNull UUID entryId
    ) {
    }
}
