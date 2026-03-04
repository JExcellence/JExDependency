/*
 * AdminShopPurchaseCommandSupport.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Executes configured admin-shop purchase commands for {@link ShopItem} entries.
 */
public final class AdminShopPurchaseCommandSupport {

    private AdminShopPurchaseCommandSupport() {
    }

    /**
     * Executes all configured command actions for an admin-shop item purchase.
     *
     * @param plugin owning plugin
     * @param customer purchasing player
     * @param shop source shop
     * @param item purchased shop item
     * @param purchasedAmount purchased amount
     * @param totalPrice total purchase price
     */
    public static void executePurchaseCommands(
            final @NotNull RDS plugin,
            final @NotNull Player customer,
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final int purchasedAmount,
            final double totalPrice
    ) {
        if (!shop.isAdminShop() || !item.hasAdminPurchaseCommands()) {
            return;
        }

        for (final ShopItem.AdminPurchaseCommand action : item.getAdminPurchaseCommands()) {
            final Runnable task = () -> executeSingleCommand(
                    plugin,
                    customer,
                    shop,
                    item,
                    action,
                    purchasedAmount,
                    totalPrice
            );
            if (action.delayTicks() > 0L) {
                plugin.getScheduler().runDelayed(task, action.delayTicks());
            } else {
                plugin.getScheduler().runSync(task);
            }
        }
    }

    private static void executeSingleCommand(
            final @NotNull RDS plugin,
            final @NotNull Player customer,
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull ShopItem.AdminPurchaseCommand action,
            final int purchasedAmount,
            final double totalPrice
    ) {
        final String resolvedCommand = resolveCommandText(
                plugin,
                customer,
                shop,
                item,
                action.command(),
                purchasedAmount,
                totalPrice
        );
        final String runnableCommand = stripLeadingSlash(resolvedCommand);
        if (runnableCommand.isBlank()) {
            return;
        }

        if (action.executionMode() == ShopItem.CommandExecutionMode.PLAYER) {
            if (!customer.isOnline()) {
                return;
            }
            customer.performCommand(runnableCommand);
            return;
        }

        final ConsoleCommandSender console = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(console, runnableCommand);
    }

    private static @NotNull String resolveCommandText(
            final @NotNull RDS plugin,
            final @NotNull Player customer,
            final @NotNull Shop shop,
            final @NotNull ShopItem item,
            final @NotNull String rawCommand,
            final int purchasedAmount,
            final double totalPrice
    ) {
        final Map<String, String> placeholders = collectBasicPlaceholders(shop, customer, item, purchasedAmount, totalPrice);
        String resolved = rawCommand;
        for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return applyPlaceholderApi(plugin.getLogger(), customer, resolved);
    }

    private static @NotNull Map<String, String> collectBasicPlaceholders(
            final @NotNull Shop shop,
            final @NotNull Player customer,
            final @NotNull ShopItem item,
            final int purchasedAmount,
            final double totalPrice
    ) {
        final Map<String, String> placeholders = new LinkedHashMap<>();
        final UUID ownerId = shop.getOwner();
        final OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        final Location location = shop.getShopLocation();

        placeholders.put("{player}", customer.getName());
        placeholders.put("{player_uuid}", customer.getUniqueId().toString());
        placeholders.put("{amount}", String.valueOf(Math.max(0, purchasedAmount)));
        placeholders.put("{item_type}", item.getItem().getType().name());
        placeholders.put("{currency_type}", item.getCurrencyType());
        placeholders.put("{price_each}", String.format(java.util.Locale.US, "%.2f", item.getValue()));
        placeholders.put("{total_price}", String.format(java.util.Locale.US, "%.2f", Math.max(0D, totalPrice)));
        placeholders.put("{shop_owner}", owner.getName() == null ? ownerId.toString() : owner.getName());
        placeholders.put("{shop_owner_uuid}", ownerId.toString());
        placeholders.put("{shop_world}", location.getWorld() == null ? "unknown" : location.getWorld().getName());
        placeholders.put("{shop_x}", String.valueOf(location.getBlockX()));
        placeholders.put("{shop_y}", String.valueOf(location.getBlockY()));
        placeholders.put("{shop_z}", String.valueOf(location.getBlockZ()));
        return placeholders;
    }

    private static @NotNull String stripLeadingSlash(
            final @NotNull String command
    ) {
        String trimmed = command.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }

    private static @NotNull String applyPlaceholderApi(
            final @NotNull Logger logger,
            final @NotNull Player customer,
            final @NotNull String input
    ) {
        if (input.isBlank() || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return input;
        }

        try {
            final Class<?> placeholderApiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            final Method setPlaceholdersMethod = placeholderApiClass.getMethod(
                    "setPlaceholders",
                    OfflinePlayer.class,
                    String.class
            );
            final Object parsed = setPlaceholdersMethod.invoke(null, customer, input);
            return parsed instanceof String parsedString ? parsedString : input;
        } catch (ReflectiveOperationException exception) {
            logger.fine("Failed to resolve PlaceholderAPI command placeholders: " + exception.getMessage());
            return input;
        }
    }
}
