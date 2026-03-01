package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShopSearchView extends APaginatedView<ShopSearchView.ShopSearchEntry> {

    private final State<RDS> rds = initialState("plugin");

    @Override
    protected String getKey() {
        return "shop_search_ui";
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
        return Map.of();
    }

    @Override
    protected CompletableFuture<List<ShopSearchEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDS plugin = this.rds.get(context);

        return CompletableFuture.supplyAsync(() -> {
            final List<ShopSearchEntry> entries = new ArrayList<>();

            for (final Shop shop : plugin.getShopRepository().findAllShops()) {
                final Location shopLocation = shop.getShopLocation();
                if (shopLocation == null) {
                    continue;
                }

                entries.add(new ShopSearchEntry(
                        shopLocation,
                        shop.getOwner(),
                        this.countAvailableItems(shop)
                ));
            }

            entries.sort(Comparator.comparing(entry -> this.formatLocation(entry.shopLocation())));
            return entries;
        }, plugin.getExecutor());
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ShopSearchEntry entry
    ) {
        final Player player = context.getPlayer();

        builder.withItem(
                UnifiedBuilderFactory.item(Material.CHEST)
                        .setName(this.i18n("entry.name", player)
                                .withPlaceholders(Map.of(
                                        "owner", this.getOwnerName(entry.ownerId())
                                ))
                                .build()
                                .component())
                        .setLore(this.i18n("entry.lore", player)
                                .withPlaceholders(Map.of(
                                        "owner", this.getOwnerName(entry.ownerId()),
                                        "location", this.formatLocation(entry.shopLocation()),
                                        "available_count", entry.availableItemCount()
                                ))
                                .build()
                                .children())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(clickContext -> this.handleShopClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final State<Pagination> paginationState = this.findPaginationState();
        if (paginationState == null) {
            render.slot(4).renderWith(() -> this.createHeaderItem(render, player));
            return;
        }

        render.slot(4)
                .renderWith(() -> this.createHeaderItem(render, player))
                .updateOnStateChange(paginationState);
    }

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleShopClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull ShopSearchEntry entry
    ) {
        final Shop shop = this.rds.get(clickContext).getShopRepository().findByLocation(entry.shopLocation());
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .build()
                    .sendMessage();
            clickContext.openForPlayer(
                    ShopSearchView.class,
                    Map.of("plugin", this.rds.get(clickContext))
            );
            return;
        }

        clickContext.openForPlayer(
                this.getTargetView(shop, clickContext.getPlayer()),
                Map.of(
                        "plugin", this.rds.get(clickContext),
                        "shopLocation", shop.getShopLocation()
                )
        );
    }

    private int countAvailableItems(
            final @NotNull Shop shop
    ) {
        int availableCount = 0;

        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem && shopItem.getAmount() > 0) {
                availableCount += shopItem.getAmount();
            }
        }

        return availableCount;
    }

    private @NotNull Class<? extends View> getTargetView(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return shop.isOwner(player.getUniqueId())
                ? ShopOverviewView.class
                : ShopCustomerView.class;
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull List<ShopSearchEntry> entries
    ) {
        int totalAvailableItems = 0;
        for (final ShopSearchEntry entry : entries) {
            totalAvailableItems += entry.availableItemCount();
        }

        return UnifiedBuilderFactory.item(Material.COMPASS)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "shop_count", entries.size(),
                                "available_count", totalAvailableItems
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createHeaderItem(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Pagination pagination = this.getPagination(render);
        final List<ShopSearchEntry> entries = this.getEntries(pagination);

        if (pagination.source() != null && entries.isEmpty()) {
            return this.createEmptyItem(player);
        }

        return this.createSummaryItem(player, entries);
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

    private @NotNull String formatLocation(
            final @NotNull Location location
    ) {
        final @Nullable String worldName = location.getWorld() == null
                ? null
                : location.getWorld().getName();

        return (worldName == null ? "unknown_world" : worldName)
                + " ("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private @NotNull List<ShopSearchEntry> getEntries(
            final @NotNull Pagination pagination
    ) {
        final List<ShopSearchEntry> entries = new ArrayList<>();
        if (pagination.source() == null) {
            return entries;
        }

        for (final Object sourceEntry : pagination.source()) {
            if (sourceEntry instanceof ShopSearchEntry shopSearchEntry) {
                entries.add(shopSearchEntry);
            }
        }

        return entries;
    }

    @SuppressWarnings("unchecked")
    private @Nullable State<Pagination> findPaginationState() {
        try {
            final Field paginationField = APaginatedView.class.getDeclaredField("pagination");
            paginationField.setAccessible(true);

            final Object paginationState = paginationField.get(this);
            return paginationState instanceof State<?> state
                    ? (State<Pagination>) state
                    : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @NotNull String getOwnerName(
            final @NotNull UUID ownerId
    ) {
        final String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        return ownerName == null ? ownerId.toString() : ownerName;
    }

    public record ShopSearchEntry(
            @NotNull Location shopLocation,
            @NotNull UUID ownerId,
            int availableItemCount
    ) {
    }
}
