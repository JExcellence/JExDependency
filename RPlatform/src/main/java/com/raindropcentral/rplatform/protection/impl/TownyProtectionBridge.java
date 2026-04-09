/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.protection.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Towny implementation of {@link com.raindropcentral.rplatform.protection.RProtectionBridge}.
 *
 * <p>The implementation relies on reflection so {@code RPlatform} can query Towny
 * membership and ownership checks without linking against Towny at compile time.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownyProtectionBridge extends AbstractReflectionProtectionBridge {

    private static final Logger LOGGER = Logger.getLogger(TownyProtectionBridge.class.getName());
    private static final String PLUGIN_NAME = "Towny";
    private static final String[] API_CLASSES = {
            "com.palmergames.bukkit.towny.TownyAPI",
            "com.palmergames.bukkit.towny.api.TownyAPI",
            "com.palmergames.bukkit.towny.TownyUniverse"
    };

    private @Nullable Object townyApi;

    /**
     * Creates a Towny protection bridge.
     */
    public TownyProtectionBridge() {
    }

    /**
     * Gets the plugin name used for availability checks.
     *
     * @return {@code "Towny"}
     */
    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Checks whether Towny is installed, enabled, and its API can be reached.
     *
     * @return {@code true} if the Towny bridge can execute lookups
     */
    @Override
    public boolean isAvailable() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            this.townyApi = null;
            return false;
        }

        if (this.townyApi != null) {
            return true;
        }

        this.townyApi = this.resolveApi(plugin);

        return this.townyApi != null;
    }

    /**
     * Checks whether a player has any Towny town membership.
     *
     * @param player player to inspect
     * @return {@code true} when Towny resolves a town for the player
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerInTown(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            return resolveResidentTown(player) != null;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve Towny membership for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Checks whether a player is currently standing in land claimed by their Towny town.
     *
     * @param player player to inspect
     * @return {@code true} if the location town matches the player's own town
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerStandingInOwnTown(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object residentTown = resolveResidentTown(player);
            if (residentTown == null) {
                return false;
            }

            final Object locationTown = resolveLocationTown(player);
            return sameTown(residentTown, locationTown);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve Towny location ownership for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Checks whether a player is the mayor (or equivalent leader) of their Towny town.
     *
     * @param player player to inspect
     * @return {@code true} when Towny resolves the player as mayor/leader
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerTownMayor(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object residentTown = resolveResidentTown(player);
            if (residentTown == null) {
                return false;
            }

            final Object resident = resolveResident(player);
            final Object residentMayorFlag = firstNonNull(
                resident == null ? null : invokeOptional(resident, "isMayor"),
                resident == null ? null : invokeOptional(resident, "isKing")
            );
            if (residentMayorFlag instanceof Boolean mayorFlag && mayorFlag) {
                return true;
            }

            final Object mayor = firstNonNull(
                invokeOptional(residentTown, "getMayor"),
                invokeOptional(residentTown, "getMayorResident"),
                invokeOptional(residentTown, "getKing"),
                invokeOptional(residentTown, "getLeader")
            );
            return isMayorIdentityMatch(player, mayor);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve Towny mayor check for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Resolves a stable town identifier for a player's Towny town.
     *
     * @param player player to inspect
     * @return stable town identifier, or {@code null} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public @Nullable String getPlayerTownIdentifier(@NotNull Player player) {
        if (!isAvailable()) {
            return null;
        }

        try {
            return resolveTownIdentifier(resolveResidentTown(player));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve Towny town identifier for " + player.getName(), exception);
            return null;
        }
    }

    /**
     * Resolves a display name for a player's Towny town.
     *
     * @param player player to inspect
     * @return town display name, or {@code null} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public @Nullable String getPlayerTownDisplayName(@NotNull Player player) {
        if (!isAvailable()) {
            return null;
        }

        try {
            return resolveTownDisplayName(resolveResidentTown(player));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve Towny town display name for " + player.getName(), exception);
            return null;
        }
    }

    /**
     * Resolves the Towny level number for the player's town.
     *
     * @param player player to inspect
     * @return Towny level number, or {@code 0} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public double getPlayerTownLevel(@NotNull Player player) {
        if (!isAvailable()) {
            return 0.0D;
        }

        try {
            final Object town = resolveResidentTown(player);
            if (town == null) {
                return 0.0D;
            }

            final Double level = resolveTownLevelNumber(town);
            return level == null ? 1.0D : Math.max(0.0D, level);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve Towny town level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    /**
     * Deposits funds into the player's Towny town bank.
     *
     * @param player player whose town receives the deposit
     * @param amount positive amount to deposit
     * @return {@code true} when Towny confirms the deposit call
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean depositToTownBank(@NotNull Player player, double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final Object town = resolveResidentTown(player);
            if (town == null) {
                return false;
            }
            return runDeposit(town, amount);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to deposit into Towny bank for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Withdraws funds from the player's Towny town bank.
     *
     * @param player player whose town is debited
     * @param amount positive amount to withdraw
     * @return {@code true} when Towny confirms the withdraw call
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean withdrawFromTownBank(@NotNull Player player, double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final Object town = resolveResidentTown(player);
            if (town == null) {
                return false;
            }
            return runWithdraw(town, amount);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to withdraw from Towny bank for " + player.getName(), exception);
            return false;
        }
    }

    @Nullable
    private Object resolveResidentTown(@NotNull Player player) {
        final Object directTown = invokeOptional(this.townyApi, "getTown", player);
        if (directTown != null) {
            return directTown;
        }

        final Object resident = resolveResident(player);
        if (resident == null) {
            return null;
        }

        final Object hasTown = invokeOptional(resident, "hasTown");
        if (hasTown instanceof Boolean hasTownFlag && !hasTownFlag) {
            return null;
        }

        return firstNonNull(
                invokeOptional(resident, "getTownOrNull"),
                invokeOptional(resident, "getTown")
        );
    }

    @Nullable
    private Object resolveResident(@NotNull Player player) {
        if (this.townyApi == null) {
            return null;
        }

        return firstNonNull(
                invokeOptional(this.townyApi, "getResident", player),
                invokeOptional(this.townyApi, "getResident", player.getUniqueId()),
                invokeOptional(this.townyApi, "getResident", player.getName())
        );
    }

    @Nullable
    private Object resolveLocationTown(@NotNull Player player) {
        if (this.townyApi == null) {
            return null;
        }

        final Object locationTown = firstNonNull(
                invokeOptional(this.townyApi, "getTown", player.getLocation()),
                invokeOptional(this.townyApi, "getTownOrNull", player.getLocation())
        );
        if (locationTown != null) {
            return locationTown;
        }

        final Object townBlock = invokeOptional(this.townyApi, "getTownBlock", player.getLocation());
        if (townBlock == null) {
            return null;
        }

        final Object townFromBlock = firstNonNull(
                invokeOptional(townBlock, "getTownOrNull"),
                invokeOptional(townBlock, "getTown")
        );
        if (townFromBlock != null) {
            return townFromBlock;
        }

        return null;
    }

    private boolean runDeposit(@NotNull Object town, double amount) {
        final Object account = firstNonNull(
                invokeOptional(town, "getAccount"),
                invokeOptional(town, "getAccountOrNull")
        );
        final BigDecimal decimalAmount = BigDecimal.valueOf(amount);

        final boolean success = tryInvoke(town, "deposit", amount)
                || tryInvoke(town, "deposit", amount, "RPlatform bank deposit")
                || tryInvoke(town, "deposit", decimalAmount)
                || tryInvoke(town, "deposit", decimalAmount, "RPlatform bank deposit")
                || tryInvoke(town, "depositMoney", amount)
                || tryInvoke(town, "addToBank", amount)
                || tryInvoke(town, "addBankBalance", amount)
                || (account != null && (
                    tryInvoke(account, "deposit", amount)
                            || tryInvoke(account, "deposit", amount, "RPlatform bank deposit")
                            || tryInvoke(account, "deposit", decimalAmount)
                            || tryInvoke(account, "deposit", decimalAmount, "RPlatform bank deposit")
                            || tryInvoke(account, "add", amount)
                            || tryInvoke(account, "add", decimalAmount)
                ));

        if (success) {
            tryInvoke(town, "save");
        }

        return success;
    }

    private boolean runWithdraw(@NotNull Object town, double amount) {
        final Object account = firstNonNull(
                invokeOptional(town, "getAccount"),
                invokeOptional(town, "getAccountOrNull")
        );
        final BigDecimal decimalAmount = BigDecimal.valueOf(amount);

        final boolean success = tryInvoke(town, "withdraw", amount)
                || tryInvoke(town, "withdraw", amount, "RPlatform bank withdrawal")
                || tryInvoke(town, "withdraw", decimalAmount)
                || tryInvoke(town, "withdraw", decimalAmount, "RPlatform bank withdrawal")
                || tryInvoke(town, "withdrawMoney", amount)
                || tryInvoke(town, "removeFromBank", amount)
                || tryInvoke(town, "subtractBankBalance", amount)
                || (account != null && (
                    tryInvoke(account, "withdraw", amount)
                            || tryInvoke(account, "withdraw", amount, "RPlatform bank withdrawal")
                            || tryInvoke(account, "withdraw", decimalAmount)
                            || tryInvoke(account, "withdraw", decimalAmount, "RPlatform bank withdrawal")
                            || tryInvoke(account, "subtract", amount)
                            || tryInvoke(account, "subtract", decimalAmount)
                ));

        if (success) {
            tryInvoke(town, "save");
        }

        return success;
    }

    @Nullable
    private Object resolveApi(
            final @NotNull Plugin plugin
    ) {
        for (final String apiClassName : API_CLASSES) {
            final Class<?> apiClass = this.loadApiClass(plugin, apiClassName);
            if (apiClass == null) {
                continue;
            }

            final Object api = firstNonNull(
                    invokeStaticOptional(apiClass, "getInstance"),
                    invokeStaticOptional(apiClass, "getAPI"),
                    invokeStaticOptional(apiClass, "getSingleton")
            );
            if (api != null) {
                return api;
            }
        }

        return firstNonNull(
                invokeOptional(plugin, "getTownyAPI"),
                invokeOptional(plugin, "getAPI"),
                invokeOptional(plugin, "getTownyUniverse"),
                readFieldOptional(plugin, "api"),
                readFieldOptional(plugin, "townyAPI"),
                readFieldOptional(plugin, "townyApi")
        );
    }

    @Nullable
    private Class<?> loadApiClass(
            final @NotNull Plugin plugin,
            final @NotNull String apiClassName
    ) {
        try {
            return Class.forName(apiClassName, true, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException pluginLoaderException) {
            try {
                return Class.forName(apiClassName);
            } catch (ClassNotFoundException fallbackException) {
                LOGGER.log(Level.FINE, "Towny API class is not present: " + apiClassName, fallbackException);
                return null;
            }
        }
    }

    @Nullable
    private Double resolveTownLevelNumber(@NotNull Object town) {
        return firstNonNullDouble(
            invokeOptional(town, "getLevelNumber"),
            invokeOptional(town, "getTownLevel"),
            invokeOptional(town, "getLevel")
        );
    }

    @Nullable
    private Double firstNonNullDouble(@Nullable Object... values) {
        for (final Object value : values) {
            final Double numericValue = toNumericLevel(value);
            if (numericValue != null) {
                return numericValue;
            }
        }
        return null;
    }

    @Nullable
    private Double toNumericLevel(@Nullable Object value) {
        final Double directValue = asDouble(value);
        if (directValue != null) {
            return directValue;
        }
        if (value == null) {
            return null;
        }

        return firstNonNullDouble(
            invokeOptional(value, "getLevelNumber"),
            invokeOptional(value, "getLevel"),
            invokeOptional(value, "getValue"),
            invokeOptional(value, "getIndex")
        );
    }
}
