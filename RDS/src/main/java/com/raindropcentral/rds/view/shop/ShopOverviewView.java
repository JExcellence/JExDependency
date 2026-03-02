package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
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

import java.util.Map;

public class ShopOverviewView extends BaseView {

    private static final String ADMIN_SHOPS_PERMISSION = "raindropshops.admin.shops";

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    @Override
    protected String getKey() {
        return "shop_overview_ui";
    }

    @Override
    protected int getSize() {
        return 2;
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
        final boolean owner = shop != null && shop.isOwner(player.getUniqueId());
        final boolean canSupply = shop != null && shop.canSupply(player.getUniqueId());
        final boolean canManage = shop != null && shop.canManage(player.getUniqueId());

        if (shop == null) {
            render.slot(4).renderWith(() -> createMissingShopItem(player));
            return;
        }

        render.slot(2).renderWith(() -> createSummaryItem(shop, player));
        render.slot(3)
                .renderWith(() -> createCustomerViewItem(shop, player))
                .onClick(clickContext -> {
                    final Shop currentShop = this.getCurrentShop(clickContext);
                    if (currentShop == null) {
                        this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                                .includePrefix()
                                .build()
                                .sendMessage();
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopCustomerView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", currentShop.getShopLocation()
                            )
                    );
                });
        if (owner) {
            render.slot(1)
                    .renderWith(() -> createCloseShopItem(shop, player))
                    .onClick(this::handleCloseShopClick);

            render.slot(13)
                    .renderWith(() -> createTrustedPlayersItem(shop, player))
                    .onClick(clickContext -> clickContext.openForPlayer(
                            ShopTrustedView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", shop.getShopLocation()
                            )
                    ));
        }
        if (owner && player.hasPermission(ADMIN_SHOPS_PERMISSION)) {
            render.slot(0)
                    .renderWith(() -> createAdminToggleItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null) {
                            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        if (!currentShop.isOwner(clickContext.getPlayer().getUniqueId())) {
                            this.i18n("feedback.not_owner", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        if (!clickContext.getPlayer().hasPermission(ADMIN_SHOPS_PERMISSION)) {
                            this.i18n("feedback.no_admin_permission", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        final RDS plugin = this.rds.get(clickContext);
                        final RDSPlayer playerData = this.getOrCreatePlayer(plugin, currentShop.getOwner());
                        final boolean enablingAdmin = !currentShop.isAdminShop();

                        if (!enablingAdmin) {
                            final int maxShops = plugin.getDefaultConfig().getMaxShops();
                            if (maxShops > 0 && playerData.getShops() >= maxShops) {
                                this.i18n("feedback.admin_disable_limit_reached", clickContext.getPlayer())
                                        .withPlaceholders(Map.of(
                                                "owned_shops", playerData.getShops(),
                                                "max_shops", maxShops
                                        ))
                                        .includePrefix()
                                        .build()
                                        .sendMessage();
                                return;
                            }

                            playerData.addShop(1);
                        } else {
                            playerData.removeShop(1);
                        }

                        currentShop.setAdminShop(enablingAdmin);
                        plugin.getShopRepository().update(currentShop);
                        plugin.getPlayerRepository().update(playerData);

                        final String feedbackKey = currentShop.isAdminShop()
                                ? "feedback.admin_enabled"
                                : "feedback.admin_disabled";
                        this.i18n(feedbackKey, clickContext.getPlayer())
                                .withPlaceholder("owned_shops", playerData.getShops())
                                .includePrefix()
                                .build()
                                .sendMessage();

                        clickContext.openForPlayer(
                                ShopOverviewView.class,
                                Map.of(
                                        "plugin", this.rds.get(clickContext),
                                        "shopLocation", currentShop.getShopLocation()
                                )
                        );
                    });
        }
        if (canManage) {
            render.slot(4)
                    .renderWith(() -> createFinanceItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null) {
                            return;
                        }

                        if (!currentShop.canManage(clickContext.getPlayer().getUniqueId())) {
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
        }
        if (canSupply) {
            render.slot(6)
                    .renderWith(() -> createManageItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null || !currentShop.canSupply(clickContext.getPlayer().getUniqueId())) {
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
        }

        if (canManage) {
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

    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull RDSPlayer getOrCreatePlayer(
            final @NotNull RDS plugin,
            final @NotNull java.util.UUID playerId
    ) {
        final RDSPlayer existingPlayer = plugin.getPlayerRepository().findByPlayer(playerId);
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(playerId);
        plugin.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n(this.getSummaryLoreSuffix(shop), player)
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

    private @NotNull ItemStack createAdminToggleItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        final String suffix = shop.isAdminShop()
                ? "actions.admin_toggle.enabled"
                : "actions.admin_toggle.disabled";
        final Material material = shop.isAdminShop()
                ? Material.COMMAND_BLOCK
                : Material.LEVER;

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n(suffix + ".name", player).build().component())
                .setLore(this.i18n(suffix + ".lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCustomerViewItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.SPYGLASS)
                .setName(this.i18n("actions.customer_view.name", player).build().component())
                .setLore(this.i18n("actions.customer_view.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createTrustedPlayersItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                .setName(this.i18n("actions.trusted.name", player).build().component())
                .setLore(this.i18n("actions.trusted.lore", player)
                        .withPlaceholders(Map.of(
                                "associate_count", shop.getTrustedPlayerCount(com.raindropcentral.rds.database.entity.ShopTrustStatus.ASSOCIATE),
                                "trusted_count", shop.getTrustedPlayerCount(com.raindropcentral.rds.database.entity.ShopTrustStatus.TRUSTED)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void handleCloseShopClick(
            final @NotNull SlotClickContext clickContext
    ) {
        final Shop currentShop = this.getCurrentShop(clickContext);
        if (currentShop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!currentShop.isOwner(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (currentShop.getStoredItemCount() > 0) {
            this.i18n("feedback.close_not_empty", clickContext.getPlayer())
                    .withPlaceholder("item_count", currentShop.getStoredItemCount())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (currentShop.getBankCurrencyCount() > 0) {
            this.i18n("feedback.close_bank_not_empty", clickContext.getPlayer())
                    .withPlaceholder("bank_currency_count", currentShop.getBankCurrencyCount())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        final RDSPlayer playerData = this.getOrCreatePlayer(plugin, currentShop.getOwner());
        if (!currentShop.isAdminShop()) {
            playerData.removeShop(1);
            plugin.getPlayerRepository().update(playerData);
        }

        plugin.getShopRepository().deleteEntity(currentShop);
        final Location location = currentShop.getShopLocation();
        if (location.getWorld() != null) {
            location.getBlock().setType(Material.AIR);
        }

        clickContext.getPlayer().getInventory().addItem(ShopBlock.getShopBlock(plugin, clickContext.getPlayer()))
                .forEach((slot, item) -> clickContext.getPlayer().getWorld().dropItem(
                        clickContext.getPlayer().getLocation().clone().add(0, 0.5, 0),
                        item
                ));

        this.i18n("feedback.closed", clickContext.getPlayer())
                .withPlaceholder("owned_shops", playerData.getShops())
                .includePrefix()
                .build()
                .sendMessage();
        clickContext.getPlayer().closeInventory();
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
        return UnifiedBuilderFactory.item(Material.HOPPER)
                .setName(this.i18n("actions.manage.name", player).build().component())
                .setLore(this.i18n("actions.manage.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCloseShopItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("actions.close.name", player).build().component())
                .setLore(this.i18n("actions.close.lore", player)
                        .withPlaceholders(Map.of(
                                "item_count", shop.getStoredItemCount(),
                                "bank_currency_count", shop.getBankCurrencyCount()
                        ))
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

    private @NotNull String getSummaryLoreSuffix(
            final @NotNull Shop shop
    ) {
        return shop.isAdminShop()
                ? "summary.admin.lore"
                : "summary.player.lore";
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
