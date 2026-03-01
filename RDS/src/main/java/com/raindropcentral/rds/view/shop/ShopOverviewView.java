package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ShopOverviewView extends BaseView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    @Override
    protected String getKey() {
        return "shop_overview_ui";
    }

    @Override
    protected int getSize() {
        return 1;
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final Shop shop = this.getCurrentShop(context);
        final Location location = this.shopLocation.get(context);

        return Map.of(
                "location", formatLocation(location),
                "owner", shop == null ? "Unknown" : getOwnerName(shop)
        );
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Shop shop = this.getCurrentShop(render);

        if (shop == null) {
            render.slot(4).renderWith(() -> createMissingShopItem(player));
            return;
        }

        render.slot(2).renderWith(() -> createSummaryItem(shop, player));
        render.slot(4)
                .renderWith(() -> createFinanceItem(shop, player))
                .onClick(clickContext -> {
                    final Shop currentShop = this.getCurrentShop(clickContext);
                    if (currentShop == null) {
                        return;
                    }

                    if (!currentShop.isOwner(clickContext.getPlayer().getUniqueId())) {
                        this.i18n("feedback.not_owner", clickContext.getPlayer())
                                .includePrefix()
                                .build()
                                .sendMessage();
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopBankView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", currentShop.getShopLocation()
                            )
                    );
                });
        render.slot(6)
                .renderWith(() -> createManageItem(shop, player))
                .onClick(clickContext -> {
                    final Shop currentShop = this.getCurrentShop(clickContext);
                    if (currentShop == null || !currentShop.isOwner(clickContext.getPlayer().getUniqueId())) {
                        this.i18n("feedback.not_owner", clickContext.getPlayer())
                                .includePrefix()
                                .build()
                                .sendMessage();
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopInputView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shop", currentShop,
                                    "shopLocation", currentShop.getShopLocation()
                            )
                    );
                });

        if (shop.isOwner(player.getUniqueId())) {
            render.slot(7)
                    .renderWith(() -> createEditItem(shop, player))
                    .onClick(clickContext -> clickContext.openForPlayer(
                            ShopEditView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", shop.getShopLocation()
                            )
                    ));

            render.slot(8)
                    .renderWith(() -> createStorageItem(shop, player))
                    .onClick(clickContext -> clickContext.openForPlayer(
                            ShopStorageView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", shop.getShopLocation()
                            )
                    ));
        }
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", getOwnerName(shop),
                                "location", formatLocation(shop.getShopLocation()),
                                "item_count", shop.getStoredItemCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createFinanceItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("finance.name", player).build().component())
                .setLore(this.i18n("finance.lore", player)
                        .withPlaceholders(Map.of(
                                "bank_currency_count", shop.getBankCurrencyCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createManageItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        final boolean owner = shop.isOwner(player.getUniqueId());
        final Material material = owner ? Material.HOPPER : Material.BARRIER;
        final String suffix = owner ? "manage" : "locked";

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("actions." + suffix + ".name", player).build().component())
                .setLore(this.i18n("actions." + suffix + ".lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEditItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("actions.edit.name", player).build().component())
                .setLore(this.i18n("actions.edit.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createStorageItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARREL)
                .setName(this.i18n("actions.storage.name", player).build().component())
                .setLore(this.i18n("actions.storage.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
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
