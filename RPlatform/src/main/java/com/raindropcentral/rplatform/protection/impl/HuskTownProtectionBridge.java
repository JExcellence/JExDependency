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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HuskTowns implementation of {@link com.raindropcentral.rplatform.protection.RProtectionBridge}.
 *
 * <p>This bridge supports multiple HuskTowns API entrypoints by probing reflective
 * method signatures at runtime.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class HuskTownProtectionBridge extends AbstractReflectionProtectionBridge {

    private static final Logger LOGGER = Logger.getLogger(HuskTownProtectionBridge.class.getName());
    private static final String PLUGIN_NAME = "HuskTowns";
    private static final String[] API_CLASSES = {
            "net.william278.husktowns.api.HuskTownsAPI",
            "net.william278.husktowns.api.BukkitHuskTownsAPI"
    };

    private @Nullable Object huskTownsApi;

    /**
     * Creates a HuskTowns protection bridge.
     */
    public HuskTownProtectionBridge() {
    }

    /**
     * Gets the plugin name used for availability checks.
     *
     * @return {@code "HuskTowns"}
     */
    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Checks whether HuskTowns is installed, enabled, and exposes an API singleton.
     *
     * @return {@code true} if this bridge can query HuskTowns
     */
    @Override
    public boolean isAvailable() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            this.huskTownsApi = null;
            return false;
        }

        if (this.huskTownsApi != null) {
            return true;
        }

        this.huskTownsApi = resolveApi(plugin);
        return this.huskTownsApi != null;
    }

    /**
     * Checks whether a player belongs to a HuskTowns town.
     *
     * @param player player to inspect
     * @return {@code true} if HuskTowns resolves a town for the player
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerInTown(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            return resolvePlayerTown(player) != null;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve HuskTowns membership for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Checks whether a player is standing in territory that belongs to their own HuskTowns town.
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
            final Object playerTown = resolvePlayerTown(player);
            if (playerTown == null) {
                return false;
            }

            final Object locationTown = resolveLocationTown(player);
            return sameTown(playerTown, locationTown);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve HuskTowns location ownership for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Checks whether a player is the mayor/leader of their HuskTowns town.
     *
     * @param player player to inspect
     * @return {@code true} when the player resolves to a mayor/leader role
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerTownMayor(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object town = resolvePlayerTown(player);
            if (town == null) {
                return false;
            }

            final Object onlineUser = resolveOnlineUser(player);
            final Object member = onlineUser == null ? null : invokeOptional(this.huskTownsApi, "getUserTown", onlineUser);
            final Object role = firstNonNull(
                member == null ? null : invokeOptional(member, "role"),
                member == null ? null : invokeOptional(member, "getRole")
            );
            if (isMayorRole(role)) {
                return true;
            }

            final Object mayorIdentity = firstNonNull(
                invokeOptional(town, "getMayor"),
                invokeOptional(town, "mayor"),
                invokeOptional(town, "getOwner"),
                invokeOptional(town, "owner"),
                invokeOptional(town, "getLeader"),
                invokeOptional(town, "leader")
            );
            return isMayorIdentityMatch(player, mayorIdentity);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve HuskTowns mayor check for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Resolves a stable town identifier for a player's HuskTowns town.
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
            return resolveTownIdentifier(resolvePlayerTown(player));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve HuskTowns town identifier for " + player.getName(), exception);
            return null;
        }
    }

    /**
     * Resolves a display name for a player's HuskTowns town.
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
            return resolveTownDisplayName(resolvePlayerTown(player));
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve HuskTowns town display name for " + player.getName(), exception);
            return null;
        }
    }

    /**
     * Resolves the HuskTowns level for the player's town.
     *
     * @param player player to inspect
     * @return HuskTowns level, or {@code 0} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public double getPlayerTownLevel(@NotNull Player player) {
        if (!isAvailable()) {
            return 0.0D;
        }

        try {
            final Object town = resolvePlayerTown(player);
            if (town == null) {
                return 0.0D;
            }

            final Double level = firstNonNullDouble(
                invokeOptional(town, "getLevel"),
                invokeOptional(town, "level"),
                invokeOptional(town, "getTownLevel")
            );
            return level == null ? 1.0D : Math.max(0.0D, level);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve HuskTowns town level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    /**
     * Deposits funds into the player's HuskTowns town bank.
     *
     * @param player player whose town receives the deposit
     * @param amount positive amount to deposit
     * @return {@code true} when a supported API path accepts the deposit
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean depositToTownBank(@NotNull Player player, double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final Object town = resolvePlayerTown(player);
            if (town == null) {
                return false;
            }
            return runDeposit(player, town, amount);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to deposit into HuskTowns bank for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Withdraws funds from the player's HuskTowns town bank.
     *
     * @param player player whose town is debited
     * @param amount positive amount to withdraw
     * @return {@code true} when a supported API path accepts the withdraw
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean withdrawFromTownBank(@NotNull Player player, double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final Object town = resolvePlayerTown(player);
            if (town == null) {
                return false;
            }
            return runWithdraw(player, town, amount);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to withdraw from HuskTowns bank for " + player.getName(), exception);
            return false;
        }
    }

    @Nullable
    private Object resolveApi(@NotNull Plugin plugin) {
        for (final String apiClassName : API_CLASSES) {
            final Class<?> apiClass = loadApiClass(plugin, apiClassName);
            if (apiClass == null) {
                continue;
            }

            final Object api = firstNonNull(
                    invokeStaticOptional(apiClass, "getBukkitInstance"),
                    invokeStaticOptional(apiClass, "getInstance"),
                    invokeStaticOptional(apiClass, "getAPI")
            );
            if (api != null) {
                return api;
            }
        }

        return firstNonNull(
                invokeOptional(plugin, "getAPI"),
                invokeOptional(plugin, "getApi"),
                invokeOptional(plugin, "getHuskTownsAPI"),
                readFieldOptional(plugin, "api"),
                readFieldOptional(plugin, "huskTownsAPI"),
                readFieldOptional(plugin, "huskTownsApi")
        );
    }

    @Nullable
    private Object resolvePlayerTown(@NotNull Player player) {
        if (this.huskTownsApi == null) {
            return null;
        }

        final Object onlineUser = resolveOnlineUser(player);
        final Object townFromUserTown = resolveTownFromUserTown(onlineUser);
        if (townFromUserTown != null) {
            return townFromUserTown;
        }

        final Object userObject = firstNonNull(
                onlineUser,
                invokeOptional(this.huskTownsApi, "getUser", player),
                invokeOptional(this.huskTownsApi, "adapt", player),
                invokeOptional(this.huskTownsApi, "getUser", player.getUniqueId())
        );

        final Object townFromUuid = firstNonNull(
                invokeOptional(this.huskTownsApi, "getPlayerTown", player.getUniqueId()),
                invokeOptional(this.huskTownsApi, "getResidentTown", player.getUniqueId()),
                invokeOptional(this.huskTownsApi, "getTownOf", player.getUniqueId())
        );
        if (townFromUuid != null) {
            return townFromUuid;
        }

        final Object townFromPlayer = firstNonNull(
                invokeOptional(this.huskTownsApi, "getPlayerTown", player),
                invokeOptional(this.huskTownsApi, "getResidentTown", player),
                invokeOptional(this.huskTownsApi, "getTownOf", player)
        );
        if (townFromPlayer != null) {
            return townFromPlayer;
        }

        if (userObject == null) {
            return null;
        }

        return firstNonNull(
                invokeOptional(this.huskTownsApi, "getPlayerTown", userObject),
                invokeOptional(this.huskTownsApi, "getResidentTown", userObject),
                invokeOptional(this.huskTownsApi, "getTownOf", userObject)
        );
    }

    @Nullable
    private Object resolveLocationTown(@NotNull Player player) {
        if (this.huskTownsApi == null) {
            return null;
        }

        final Object onlineUser = resolveOnlineUser(player);
        final Object position = firstNonNull(
                invokeOptional(this.huskTownsApi, "getPosition", player.getLocation()),
                onlineUser == null ? null : invokeOptional(onlineUser, "getPosition")
        );
        if (position != null) {
            final Object claimAtPosition = firstNonNull(
                    invokeOptional(this.huskTownsApi, "getClaimAt", position),
                    invokeOptional(this.huskTownsApi, "getClaim", position)
            );
            final Object townFromClaimAtPosition = resolveTownFromClaim(claimAtPosition);
            if (townFromClaimAtPosition != null) {
                return townFromClaimAtPosition;
            }
        }

        final Object townDirect = firstNonNull(
                invokeOptional(this.huskTownsApi, "getTownAt", player.getLocation()),
                invokeOptional(this.huskTownsApi, "getTown", player.getLocation())
        );
        if (townDirect != null) {
            return townDirect;
        }

        final Object claim = firstNonNull(
                invokeOptional(this.huskTownsApi, "getClaimAt", player.getLocation()),
                invokeOptional(this.huskTownsApi, "getClaim", player.getLocation()),
                invokeOptional(this.huskTownsApi, "getClaimAt", player.getLocation().getWorld().getName(), player.getChunk().getX(), player.getChunk().getZ()),
                invokeOptional(this.huskTownsApi, "getClaim", player.getLocation().getWorld().getName(), player.getChunk().getX(), player.getChunk().getZ())
        );
        return resolveTownFromClaim(claim);
    }

    @Nullable
    private Object resolveOnlineUser(@NotNull Player player) {
        if (this.huskTownsApi == null) {
            return null;
        }
        return firstNonNull(
                invokeOptional(this.huskTownsApi, "getOnlineUser", player),
                invokeOptional(this.huskTownsApi, "getUser", player),
                invokeOptional(this.huskTownsApi, "adapt", player)
        );
    }

    @Nullable
    private Object resolveTownFromUserTown(@Nullable Object user) {
        if (this.huskTownsApi == null || user == null) {
            return null;
        }

        final Object member = invokeOptional(this.huskTownsApi, "getUserTown", user);
        final Object townFromMember = firstNonNull(
                member == null ? null : invokeOptional(member, "town"),
                member == null ? null : invokeOptional(member, "getTown")
        );
        if (townFromMember != null) {
            return townFromMember;
        }

        return firstNonNull(
                invokeOptional(this.huskTownsApi, "getTownOf", user),
                invokeOptional(this.huskTownsApi, "getPlayerTown", user),
                invokeOptional(this.huskTownsApi, "getResidentTown", user)
        );
    }

    @Nullable
    private Object resolveTownFromClaim(@Nullable Object claim) {
        if (claim == null) {
            return null;
        }

        return firstNonNull(
                invokeOptional(claim, "town"),
                invokeOptional(claim, "getTown"),
                invokeOptional(claim, "getOwner"),
                invokeOptional(claim, "getHolder")
        );
    }

    @Nullable
    private Class<?> loadApiClass(@NotNull Plugin plugin, @NotNull String apiClassName) {
        try {
            return Class.forName(apiClassName, true, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException pluginLoaderException) {
            try {
                return Class.forName(apiClassName);
            } catch (ClassNotFoundException fallbackException) {
                LOGGER.log(Level.FINE, "HuskTowns API class is not present: " + apiClassName, fallbackException);
                return null;
            }
        }
    }

    private boolean runDeposit(@NotNull Player player, @NotNull Object town, double amount) {
        final BigDecimal decimalAmount = BigDecimal.valueOf(amount);
        if (tryInvoke(town, "deposit", amount)
                || tryInvoke(town, "deposit", decimalAmount)
                || tryInvoke(town, "addMoney", amount)
                || tryInvoke(town, "addToBank", amount)) {
            return true;
        }

        if (this.huskTownsApi == null) {
            return false;
        }

        return tryInvoke(this.huskTownsApi, "depositTownBank", town, amount)
                || tryInvoke(this.huskTownsApi, "depositTownBank", town, decimalAmount)
                || tryInvoke(this.huskTownsApi, "depositToTown", town, amount)
                || tryInvoke(this.huskTownsApi, "depositToTown", player, town, amount)
                || tryInvoke(this.huskTownsApi, "depositTown", player.getUniqueId(), town, amount)
                || tryInvoke(this.huskTownsApi, "addTownMoney", town, amount);
    }

    private boolean runWithdraw(@NotNull Player player, @NotNull Object town, double amount) {
        final BigDecimal decimalAmount = BigDecimal.valueOf(amount);
        if (tryInvoke(town, "withdraw", amount)
                || tryInvoke(town, "withdraw", decimalAmount)
                || tryInvoke(town, "removeMoney", amount)
                || tryInvoke(town, "removeFromBank", amount)) {
            return true;
        }

        if (this.huskTownsApi == null) {
            return false;
        }

        return tryInvoke(this.huskTownsApi, "withdrawTownBank", town, amount)
                || tryInvoke(this.huskTownsApi, "withdrawTownBank", town, decimalAmount)
                || tryInvoke(this.huskTownsApi, "withdrawFromTown", town, amount)
                || tryInvoke(this.huskTownsApi, "withdrawFromTown", player, town, amount)
                || tryInvoke(this.huskTownsApi, "withdrawTown", player.getUniqueId(), town, amount)
                || tryInvoke(this.huskTownsApi, "removeTownMoney", town, amount);
    }

    private boolean isMayorRole(@Nullable Object roleObject) {
        if (roleObject == null) {
            return false;
        }

        if (roleObject instanceof String roleText) {
            return isMayorRoleName(roleText);
        }

        final Object roleName = firstNonNull(
            invokeOptional(roleObject, "name"),
            invokeOptional(roleObject, "getName")
        );
        if (roleName instanceof String roleText) {
            return isMayorRoleName(roleText);
        }

        return false;
    }

    private boolean isMayorRoleName(@NotNull String roleText) {
        final String normalized = roleText.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("mayor")
            || normalized.contains("leader")
            || normalized.contains("owner")
            || normalized.contains("founder");
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
            invokeOptional(value, "getLevel"),
            invokeOptional(value, "level"),
            invokeOptional(value, "getValue"),
            invokeOptional(value, "getIndex")
        );
    }
}
