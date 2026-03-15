package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.BossBarSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents shop boss bar service.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopBossBarService {

    private final RDS plugin;
    private final long updatePeriodTicks;
    private final int maxViewDistance;
    private final Map<UUID, BossBar> activeBars = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> enabledStates = new ConcurrentHashMap<>();

    /**
     * Creates a new shop boss bar service.
     *
     * @param plugin plugin instance
     */
    public ShopBossBarService(
            final @NotNull RDS plugin
    ) {
        this.plugin = plugin;
        final BossBarSection bossBarSection = this.plugin.getDefaultConfig().getBossBar();
        this.updatePeriodTicks = bossBarSection.getUpdatePeriodTicks();
        this.maxViewDistance = bossBarSection.getViewDistance();
    }

    /**
     * Starts shop boss bar service processing.
     */
    public void start() {
        this.plugin.getScheduler().runRepeating(
                this::refreshOnlinePlayers,
                this.updatePeriodTicks,
                this.updatePeriodTicks
        );
    }

    /**
     * Shuts down shop boss bar service processing.
     */
    public void shutdown() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.clearPlayer(player);
        }
        this.activeBars.clear();
        this.enabledStates.clear();
    }

    /**
     * Executes toggleFor.
     */
    public boolean toggleFor(
            final @NotNull Player player
    ) {
        RDSPlayer playerData = this.plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        final boolean created = playerData == null;

        if (playerData == null) {
            playerData = new RDSPlayer(player.getUniqueId());
        }

        final boolean enabled = playerData.toggleShopBar();
        if (created) {
            this.plugin.getPlayerRepository().create(playerData);
        } else {
            this.plugin.getPlayerRepository().update(playerData);
        }

        this.enabledStates.put(player.getUniqueId(), enabled);
        if (enabled) {
            this.refreshPlayer(player);
        } else {
            this.hideDisplayedBar(player);
        }

        return enabled;
    }

    /**
     * Executes clearPlayer.
     */
    public void clearPlayer(
            final @NotNull Player player
    ) {
        this.enabledStates.remove(player.getUniqueId());
        this.hideDisplayedBar(player);
    }

    /**
     * Returns whether enabled.
     */
    public boolean isEnabled(
            final @NotNull Player player
    ) {
        return this.enabledStates.computeIfAbsent(
                player.getUniqueId(),
                this::loadEnabledState
        );
    }

    /**
     * Executes refreshPlayer.
     */
    public void refreshPlayer(
            final @NotNull Player player
    ) {
        if (!player.isOnline() || !this.isEnabled(player)) {
            this.hideDisplayedBar(player);
            return;
        }

        final Block targetBlock = player.getTargetBlockExact(this.maxViewDistance);
        if (targetBlock == null) {
            this.hideDisplayedBar(player);
            return;
        }

        final Shop shop = this.plugin.getShopRepository().findByLocation(targetBlock.getLocation());
        if (shop == null) {
            this.hideDisplayedBar(player);
            return;
        }

        if (shop.isAdminShop()) {
            this.plugin.getAdminShopRestockScheduler().restockShop(shop);
        }

        this.showBossBar(player, shop);
    }

    private void refreshOnlinePlayers() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.refreshPlayer(player);
        }
    }

    private boolean loadEnabledState(
            final @NotNull UUID playerId
    ) {
        final RDSPlayer playerData = this.plugin.getPlayerRepository().findByPlayer(playerId);
        return playerData != null && playerData.isShopBarEnabled();
    }

    private void showBossBar(
            final @NotNull Player player,
            final @NotNull Shop shop
    ) {
        final BossBar.Color color = shop.isAdminShop()
                ? BossBar.Color.RED
                : BossBar.Color.BLUE;
        final Component title = new I18n.Builder("shop_boss_bar.title", player)
                .withPlaceholders(Map.of(
                        "shop_type", this.getShopTypeLabel(player, shop),
                        "owner", this.getOwnerLabel(player, shop),
                        "available_items", this.getAvailableItemsLabel(player, shop),
                        "location", this.formatLocation(shop.getShopLocation())
                ))
                .build()
                .component();

        this.activeBars.compute(player.getUniqueId(), (playerId, existingBar) -> {
            if (existingBar == null) {
                final BossBar bossBar = BossBar.bossBar(
                        title,
                        1.0f,
                        color,
                        BossBar.Overlay.PROGRESS
                );
                player.showBossBar(bossBar);
                return bossBar;
            }

            existingBar.name(title);
            existingBar.color(color);
            existingBar.progress(1.0f);
            player.showBossBar(existingBar);
            return existingBar;
        });
    }

    private void hideDisplayedBar(
            final @NotNull Player player
    ) {
        final BossBar bossBar = this.activeBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private @NotNull String getShopTypeLabel(
            final @NotNull Player viewer,
            final @NotNull Shop shop
    ) {
        final String key = shop.isAdminShop()
                ? "shop_boss_bar.type.admin"
                : "shop_boss_bar.type.player";
        return this.toPlainString(key, viewer, Map.of());
    }

    private @NotNull String getOwnerLabel(
            final @NotNull Player viewer,
            final @NotNull Shop shop
    ) {
        if (shop.isAdminShop()) {
            return this.toPlainString("shop_boss_bar.owner.admin", viewer, Map.of());
        }

        final String ownerName = Bukkit.getOfflinePlayer(shop.getOwner()).getName();
        return ownerName == null ? shop.getOwner().toString() : ownerName;
    }

    private @NotNull String getAvailableItemsLabel(
            final @NotNull Player viewer,
            final @NotNull Shop shop
    ) {
        if (shop.isAdminShop()) {
            return switch (this.resolveAdminAvailableItemsMode(shop)) {
                case LIMITED -> this.toPlainString(
                        "shop_boss_bar.available_items.limited",
                        viewer,
                        Map.of("item_count", AdminShopStockSupport.countVisibleStock(shop))
                );
                case INFINITE -> this.toPlainString(
                        "shop_boss_bar.available_items.infinite",
                        viewer,
                        Map.of("listing_count", this.countVisibleListings(shop))
                );
                case VARIOUS -> this.toPlainString(
                        "shop_boss_bar.available_items.various",
                        viewer,
                        Map.of(
                                "limited_listing_count", this.countLimitedAdminListings(shop),
                                "infinite_listing_count", this.countUnlimitedAdminListings(shop)
                        )
                );
            };
        }

        return this.toPlainString(
                "shop_boss_bar.available_items.finite",
                viewer,
                Map.of("item_count", this.countAvailableItems(shop))
        );
    }

    private int countAvailableItems(
            final @NotNull Shop shop
    ) {
        int availableItems = 0;

        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem
                    && shopItem.isAvailableNow()
                    && shopItem.getAmount() > 0) {
                availableItems += shopItem.getAmount();
            }
        }

        return availableItems;
    }

    private int countVisibleListings(
            final @NotNull Shop shop
    ) {
        int listingCount = 0;

        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem && shopItem.isAvailableNow()) {
                listingCount++;
            }
        }

        return listingCount;
    }

    private @NotNull AdminAvailableItemsMode resolveAdminAvailableItemsMode(
            final @NotNull Shop shop
    ) {
        final int unlimitedListingCount = this.countUnlimitedAdminListings(shop);
        if (unlimitedListingCount == 0) {
            return AdminAvailableItemsMode.LIMITED;
        }

        if (this.countLimitedAdminListings(shop) == 0) {
            return AdminAvailableItemsMode.INFINITE;
        }

        return AdminAvailableItemsMode.VARIOUS;
    }

    private int countUnlimitedAdminListings(
            final @NotNull Shop shop
    ) {
        int listingCount = 0;

        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem
                    && shopItem.isAvailableNow()
                    && AdminShopStockSupport.isUnlimitedAdminStock(shop, shopItem)) {
                listingCount++;
            }
        }

        return listingCount;
    }

    private int countLimitedAdminListings(
            final @NotNull Shop shop
    ) {
        int listingCount = 0;

        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem
                    && shopItem.isAvailableNow()
                    && AdminShopStockSupport.usesLimitedAdminStock(shop, shopItem)) {
                listingCount++;
            }
        }

        return listingCount;
    }

    private @NotNull String formatLocation(
            final @Nullable Location location
    ) {
        if (location == null) {
            return "unknown";
        }

        final String worldName = location.getWorld() == null
                ? "unknown_world"
                : location.getWorld().getName();

        return worldName
                + " ("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private @NotNull String toPlainString(
            final @NotNull String key,
            final @NotNull Player player,
            final @NotNull Map<String, Object> placeholders
    ) {
        return new I18n.Builder(key, player)
                .withPlaceholders(placeholders)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private enum AdminAvailableItemsMode {
        LIMITED,
        INFINITE,
        VARIOUS
    }
}
