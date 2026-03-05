package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.AdminShopStockSupport;
import me.devnatan.inventoryframework.View;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Shared shop-browser helpers used by the public directory and material search views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ShopBrowserSupport {

    private ShopBrowserSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Resolves user input into a Bukkit material using case-insensitive, separator-tolerant matching.
     *
     * @param input raw player input
     * @return matched material, or {@code null} when the input does not resolve to a material
     */
    public static @Nullable Material resolveMaterialQuery(
            final @Nullable String input
    ) {
        if (input == null) {
            return null;
        }

        final String normalized = normalizeMaterialQuery(input);
        if (normalized.isBlank()) {
            return null;
        }

        final Material matched = Material.matchMaterial(normalized);
        if (matched != null) {
            return matched;
        }

        try {
            return Material.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Formats a material name into a player-friendly label.
     *
     * @param material material to format
     * @return human-readable material label
     */
    public static @NotNull String formatMaterialName(
            final @NotNull Material material
    ) {
        final String[] tokens = material.name().toLowerCase(Locale.ROOT).split("_");
        final StringBuilder builder = new StringBuilder();

        for (final String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                builder.append(token.substring(1));
            }
        }

        return builder.isEmpty() ? material.name() : builder.toString();
    }

    static @NotNull List<ShopBrowserEntry> loadEntries(
            final @NotNull RDS plugin,
            final @Nullable Material material
    ) {
        final List<ShopBrowserEntry> entries = new ArrayList<>();
        final var shopRepository = plugin.getShopRepository();
        if (shopRepository == null) {
            return entries;
        }

        final var adminShopRestockScheduler = plugin.getAdminShopRestockScheduler();

        for (final Shop shop : shopRepository.findAllShops()) {
            final Location shopLocation = shop.getShopLocation();
            if (shopLocation == null) {
                continue;
            }

            if (shop.isAdminShop() && adminShopRestockScheduler != null) {
                adminShopRestockScheduler.restockShop(shop);
            }

            final int matchingOfferCount = material == null
                    ? 0
                    : countMatchingOffers(shop, material);
            if (material != null && matchingOfferCount < 1) {
                continue;
            }

            entries.add(new ShopBrowserEntry(
                    shopLocation,
                    shop.getOwner(),
                    countAvailableItems(shop),
                    matchingOfferCount
            ));
        }

        entries.sort(Comparator.comparing(entry -> formatLocation(entry.shopLocation())));
        return entries;
    }

    static int countAvailableItems(
            final @NotNull Shop shop
    ) {
        if (shop.isAdminShop()) {
            int availableListings = 0;
            boolean hasUnlimitedListing = false;
            for (final AbstractItem item : shop.getItems()) {
                if (!(item instanceof ShopItem shopItem) || !shopItem.isAvailableNow()) {
                    continue;
                }

                availableListings++;
                if (AdminShopStockSupport.isUnlimitedAdminStock(shop, shopItem)) {
                    hasUnlimitedListing = true;
                }
            }
            if (hasUnlimitedListing) {
                return availableListings;
            }

            return AdminShopStockSupport.countVisibleStock(shop);
        }

        int availableCount = 0;
        for (final AbstractItem item : shop.getItems()) {
            if (item instanceof ShopItem shopItem
                    && shopItem.isAvailableNow()
                    && shopItem.getAmount() > 0) {
                availableCount += shopItem.getAmount();
            }
        }

        return availableCount;
    }

    static int countMatchingOffers(
            final @NotNull Shop shop,
            final @NotNull Material material
    ) {
        int matchingOfferCount = 0;

        for (final AbstractItem item : shop.getItems()) {
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (shopItem.getItem().getType() != material) {
                continue;
            }

            if (!shopItem.isAvailableNow()) {
                continue;
            }

            if (shop.isAdminShop() || shopItem.getAmount() > 0) {
                matchingOfferCount++;
            }
        }

        return matchingOfferCount;
    }

    static @NotNull String formatLocation(
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

    static @NotNull String getOwnerName(
            final @NotNull UUID ownerId
    ) {
        final String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        return ownerName == null ? ownerId.toString() : ownerName;
    }

    static @NotNull Class<? extends View> getTargetView(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return shop.canAccessOverview(player.getUniqueId())
                ? ShopOverviewView.class
                : ShopCustomerView.class;
    }

    private static @NotNull String normalizeMaterialQuery(
            final @NotNull String input
    ) {
        return input.trim()
                .replace("minecraft:", "")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    /**
     * Immutable shop browser entry used by paginated directory views.
     *
     * @param shopLocation shop block location
     * @param ownerId owner unique identifier
     * @param availableItemCount visible stock count used by the directory summary
     * @param matchingOfferCount number of visible listings matching the active material filter
     */
    record ShopBrowserEntry(
            @NotNull Location shopLocation,
            @NotNull UUID ownerId,
            int availableItemCount,
            int matchingOfferCount
    ) {
    }
}
