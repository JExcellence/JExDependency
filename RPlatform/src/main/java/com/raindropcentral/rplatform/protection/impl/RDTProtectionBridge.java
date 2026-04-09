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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RDT implementation of {@link com.raindropcentral.rplatform.protection.RProtectionBridge}.
 *
 * <p>The bridge accesses RDT runtime repositories by reflection so this module
 * stays decoupled from the concrete RDT artifacts.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDTProtectionBridge extends AbstractReflectionProtectionBridge {

    private static final Logger LOGGER = Logger.getLogger(RDTProtectionBridge.class.getName());
    private static final String PLUGIN_NAME = "RDT";

    private @Nullable Object rdtRuntime;

    /**
     * Creates an RDT protection bridge.
     */
    public RDTProtectionBridge() {
    }

    /**
     * Gets the plugin name used for availability checks.
     *
     * @return {@code "RDT"}
     */
    @Override
    public @NotNull String getPluginName() {
        return PLUGIN_NAME;
    }

    /**
     * Checks whether RDT is installed, enabled, and exposes a usable runtime object.
     *
     * @return {@code true} if this bridge can query RDT repositories
     */
    @Override
    public boolean isAvailable() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            this.rdtRuntime = null;
            return false;
        }

        if (this.rdtRuntime != null) {
            return true;
        }

        this.rdtRuntime = resolveRuntime(plugin);
        return this.rdtRuntime != null;
    }

    /**
     * Checks whether a player belongs to a town in RDT.
     *
     * @param player player to inspect
     * @return {@code true} if the player's stored town UUID is present
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerInTown(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return false;
            }
            return resolveTownUuid(playerRecord) != null;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT membership for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Checks whether a player is currently standing in a chunk claimed by their own RDT town.
     *
     * @param player player to inspect
     * @return {@code true} if the player's current chunk is in their town claim list
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerStandingInOwnTown(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return false;
            }

            final UUID townUuid = resolveTownUuid(playerRecord);
            if (townUuid == null) {
                return false;
            }

            final Object town = resolveTown(townUuid);
            if (town == null) {
                return false;
            }

            return isChunkClaimedByTown(town, player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT location ownership for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Checks whether a player is the mayor/leader of their RDT town.
     *
     * @param player player to inspect
     * @return {@code true} when the resolved town mayor identity matches the player
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean isPlayerTownMayor(@NotNull Player player) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return false;
            }

            final Object directMayorFlag = firstNonNull(
                invokeOptional(playerRecord, "isMayor"),
                invokeOptional(playerRecord, "isTownMayor"),
                invokeOptional(playerRecord, "isLeader")
            );
            if (directMayorFlag instanceof Boolean mayorFlag && mayorFlag) {
                return true;
            }

            final UUID townUuid = resolveTownUuid(playerRecord);
            if (townUuid == null) {
                return false;
            }

            final Object town = resolveTown(townUuid);
            if (town == null) {
                return false;
            }

            final Object mayorIdentity = firstNonNull(
                invokeOptional(town, "getMayorUUID"),
                invokeOptional(town, "getMayorUuid"),
                invokeOptional(town, "getOwnerUUID"),
                invokeOptional(town, "getOwnerUuid"),
                invokeOptional(town, "getLeaderUUID"),
                invokeOptional(town, "getLeaderUuid"),
                invokeOptional(town, "getMayor"),
                invokeOptional(town, "getOwner"),
                invokeOptional(town, "getLeader")
            );
            return isMayorIdentityMatch(player, mayorIdentity);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT mayor check for " + player.getName(), exception);
            return false;
        }
    }

    /**
     * Resolves a stable town identifier for a player's RDT town.
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
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return null;
            }

            final UUID townUuid = resolveTownUuid(playerRecord);
            if (townUuid == null) {
                return null;
            }

            final Object town = resolveTown(townUuid);
            final String identifier = resolveTownIdentifier(town);
            return identifier == null ? townUuid.toString().toLowerCase(Locale.ROOT) : identifier;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT town identifier for " + player.getName(), exception);
            return null;
        }
    }

    /**
     * Resolves a display name for a player's RDT town.
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
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return null;
            }

            final UUID townUuid = resolveTownUuid(playerRecord);
            if (townUuid == null) {
                return null;
            }

            final Object town = resolveTown(townUuid);
            final String displayName = resolveTownDisplayName(town);
            return displayName == null ? townUuid.toString() : displayName;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT town display name for " + player.getName(), exception);
            return null;
        }
    }

    /**
     * Resolves the composite RDT town level for the player's town.
     *
     * @param player player to inspect
     * @return RDT composite town level, or {@code 0} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public double getPlayerTownLevel(@NotNull Player player) {
        if (!isAvailable()) {
            return 0.0D;
        }

        try {
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return 0.0D;
            }

            final UUID townUuid = resolveTownUuid(playerRecord);
            if (townUuid == null) {
                return 0.0D;
            }

            final Object town = resolveTown(townUuid);
            final Double townLevel = asDouble(invokeOptional(town, "getTownLevel"));
            return townLevel == null ? 0.0D : Math.max(0.0D, townLevel);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT town level for " + player.getName(), exception);
            return 0.0D;
        }
    }

    /**
     * Deposits funds into the player's RDT town bank.
     *
     * @param player player whose town receives the deposit
     * @param amount positive amount to deposit
     * @return {@code true} when the town balance update succeeds
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean depositToTownBank(@NotNull Player player, double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final String townIdentifier = getPlayerTownIdentifier(player);
            return townIdentifier != null && this.depositToTownBank(townIdentifier, "vault", amount);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to deposit into RDT bank for " + player.getName(), exception);
            return false;
        }
    }

    @Override
    public boolean depositToTownBank(
        final @NotNull String townIdentifier,
        final @NotNull String currencyType,
        final double amount
    ) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final Object town = resolveTownByIdentifier(townIdentifier);
            if (town == null) {
                return false;
            }

            final BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            final boolean success = tryInvoke(town, "depositBank", currencyType, amount)
                || tryInvoke(town, "depositBank", currencyType, decimalAmount)
                || tryInvoke(town, "deposit", amount)
                || tryInvoke(town, "deposit", decimalAmount)
                || tryInvoke(town, "depositMoney", amount)
                || tryInvoke(town, "addToBank", amount);
            if (!success) {
                return false;
            }

            persistTown(town);
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to deposit into RDT bank for town " + townIdentifier, exception);
            return false;
        }
    }

    /**
     * Withdraws funds from the player's RDT town bank.
     *
     * @param player player whose town is debited
     * @param amount positive amount to withdraw
     * @return {@code true} when the town balance update succeeds
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Override
    public boolean withdrawFromTownBank(@NotNull Player player, double amount) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final String townIdentifier = getPlayerTownIdentifier(player);
            return townIdentifier != null && this.withdrawFromTownBank(townIdentifier, "vault", amount);
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to withdraw from RDT bank for " + player.getName(), exception);
            return false;
        }
    }

    @Override
    public boolean withdrawFromTownBank(
        final @NotNull String townIdentifier,
        final @NotNull String currencyType,
        final double amount
    ) {
        if (!isAvailable() || amount <= 0.0D) {
            return false;
        }

        try {
            final Object town = resolveTownByIdentifier(townIdentifier);
            if (town == null) {
                return false;
            }

            final Double currentBank = asDouble(firstNonNull(
                invokeOptional(town, "getBankAmount", currencyType),
                invokeOptional(town, "getBank")
            ));
            if (currentBank != null && currentBank + 1.0E-6D < amount) {
                return false;
            }

            final BigDecimal decimalAmount = BigDecimal.valueOf(amount);
            final boolean success = tryInvoke(town, "withdrawBank", currencyType, amount)
                || tryInvoke(town, "withdrawBank", currencyType, decimalAmount)
                || tryInvoke(town, "withdraw", amount)
                || tryInvoke(town, "withdraw", decimalAmount)
                || tryInvoke(town, "withdrawMoney", amount)
                || tryInvoke(town, "removeFromBank", amount);
            if (!success) {
                return false;
            }

            persistTown(town);
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to withdraw from RDT bank for town " + townIdentifier, exception);
            return false;
        }
    }

    @Override
    public boolean hasTownPermission(
        final @NotNull Player player,
        final @NotNull String permissionKey
    ) {
        if (!isAvailable()) {
            return false;
        }

        try {
            final Object playerRecord = resolvePlayerRecord(player);
            if (playerRecord == null) {
                return false;
            }

            final Object directResult = firstNonNull(
                invokeOptional(playerRecord, "hasTownPermission", permissionKey),
                invokeOptional(playerRecord, "hasPermission", permissionKey)
            );
            if (directResult instanceof Boolean granted) {
                return granted;
            }

            final UUID townUuid = resolveTownUuid(playerRecord);
            if (townUuid == null) {
                return false;
            }

            final Object town = resolveTown(townUuid);
            final Object townResult = town == null
                ? null
                : firstNonNull(
                    invokeOptional(town, "hasTownPermission", playerRecord, permissionKey),
                    invokeOptional(town, "hasRolePermission", invokeOptional(playerRecord, "getTownRoleId"), permissionKey)
                );
            return townResult instanceof Boolean granted && granted;
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Failed to resolve RDT permission " + permissionKey + " for " + player.getName(), exception);
            return false;
        }
    }

    @Nullable
    private Object resolveRuntime(@NotNull Plugin plugin) {
        if (hasMethod(plugin.getClass(), "getPlayerRepository", 0)
                && hasMethod(plugin.getClass(), "getTownRepository", 0)) {
            return plugin;
        }

        final Object directRuntime = firstNonNull(
                invokeOptional(plugin, "getRdt"),
                readFieldOptional(plugin, "rdt")
        );
        if (isRuntimeObject(directRuntime)) {
            return directRuntime;
        }

        final Object delegate = firstNonNull(
                readFieldOptional(plugin, "impl"),
                readFieldOptional(plugin, "delegate")
        );
        if (delegate == null) {
            return null;
        }

        final Object delegatedRuntime = firstNonNull(
                invokeOptional(delegate, "getRdt"),
                readFieldOptional(delegate, "rdt")
        );
        if (isRuntimeObject(delegatedRuntime)) {
            return delegatedRuntime;
        }

        return null;
    }

    private boolean isRuntimeObject(@Nullable Object runtime) {
        if (runtime == null) {
            return false;
        }
        return hasMethod(runtime.getClass(), "getPlayerRepository", 0)
                && hasMethod(runtime.getClass(), "getTownRepository", 0);
    }

    @Nullable
    private Object resolvePlayerRecord(@NotNull Player player) {
        if (this.rdtRuntime == null) {
            return null;
        }

        final Object playerRepository = invokeOptional(this.rdtRuntime, "getPlayerRepository");
        if (playerRepository == null) {
            return null;
        }

        return firstNonNull(
                invokeOptional(playerRepository, "findByPlayer", player.getUniqueId()),
                invokeOptional(playerRepository, "findByIdentifier", player.getUniqueId())
        );
    }

    @Nullable
    private UUID resolveTownUuid(@NotNull Object playerRecord) {
        final Object townUuid = firstNonNull(
                invokeOptional(playerRecord, "getTownUUID"),
                invokeOptional(playerRecord, "getTownUuid")
        );
        return asUuid(townUuid);
    }

    @Nullable
    private Object resolveTown(@NotNull UUID townUuid) {
        if (this.rdtRuntime == null) {
            return null;
        }

        final Object townRepository = invokeOptional(this.rdtRuntime, "getTownRepository");
        if (townRepository == null) {
            return null;
        }

        return firstNonNull(
                invokeOptional(townRepository, "findByTownUUID", townUuid),
                invokeOptional(townRepository, "findByTownUuid", townUuid),
                invokeOptional(townRepository, "findByIdentifier", townUuid),
                invokeOptional(townRepository, "findById", townUuid)
        );
    }

    @Nullable
    private Object resolveTownByIdentifier(@NotNull String townIdentifier) {
        final String normalizedIdentifier = townIdentifier.trim();
        if (normalizedIdentifier.isEmpty()) {
            return null;
        }

        try {
            return this.resolveTown(UUID.fromString(normalizedIdentifier));
        } catch (IllegalArgumentException ignored) {
            if (this.rdtRuntime == null) {
                return null;
            }

            final Object townRepository = invokeOptional(this.rdtRuntime, "getTownRepository");
            if (townRepository == null) {
                return null;
            }

            final Object towns = invokeOptional(townRepository, "findAll");
            if (!(towns instanceof Iterable<?> iterable)) {
                return null;
            }

            for (final Object town : iterable) {
                final String identifier = resolveTownIdentifier(town);
                if (identifier != null && identifier.equalsIgnoreCase(normalizedIdentifier)) {
                    return town;
                }
            }
            return null;
        }
    }

    private boolean isChunkClaimedByTown(@NotNull Object town, int chunkX, int chunkZ) {
        final Object chunks = invokeOptional(town, "getChunks");
        if (!(chunks instanceof Iterable<?> iterable)) {
            return false;
        }

        for (final Object chunk : iterable) {
            final Integer claimedX = asInteger(firstNonNull(
                    invokeOptional(chunk, "getX_loc"),
                    invokeOptional(chunk, "getXLoc"),
                    invokeOptional(chunk, "getX")
            ));
            final Integer claimedZ = asInteger(firstNonNull(
                    invokeOptional(chunk, "getZ_loc"),
                    invokeOptional(chunk, "getZLoc"),
                    invokeOptional(chunk, "getZ")
            ));

            if (claimedX != null && claimedZ != null && claimedX == chunkX && claimedZ == chunkZ) {
                return true;
            }
        }

        return false;
    }

    private void persistTown(@NotNull Object town) {
        if (this.rdtRuntime == null) {
            return;
        }

        final Object townRepository = invokeOptional(this.rdtRuntime, "getTownRepository");
        if (townRepository == null) {
            return;
        }

        if (!tryInvoke(townRepository, "update", town)) {
            tryInvoke(townRepository, "updateAsync", town);
        }
    }
}
