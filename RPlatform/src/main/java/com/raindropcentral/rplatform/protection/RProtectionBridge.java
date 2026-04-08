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

package com.raindropcentral.rplatform.protection;

import com.raindropcentral.rplatform.protection.impl.HuskTownProtectionBridge;
import com.raindropcentral.rplatform.protection.impl.RDTProtectionBridge;
import com.raindropcentral.rplatform.protection.impl.TownyProtectionBridge;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared protection-API bridge contract for town-oriented plugins.
 *
 * <p>The bridge intentionally uses runtime detection so dependent plugins can
 * integrate with Towny, RDT, or HuskTowns without a hard compile-time dependency.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public interface RProtectionBridge {

    /**
     * Default bridge discovery order.
     */
    List<RProtectionBridge> DEFAULT_BRIDGES = List.of(
            new TownyProtectionBridge(),
            new RDTProtectionBridge(),
            new HuskTownProtectionBridge()
    );

    /**
     * Gets the plugin name this bridge is responsible for.
     *
     * @return plugin name used for detection
     */
    @NotNull String getPluginName();

    /**
     * Checks if the protected plugin is currently installed, enabled, and ready.
     *
     * @return {@code true} when this bridge can serve requests
     */
    boolean isAvailable();

    /**
     * Checks whether a player belongs to any town managed by this plugin.
     *
     * @param player player to inspect
     * @return {@code true} if the player belongs to a town
     * @throws NullPointerException if {@code player} is {@code null}
     */
    boolean isPlayerInTown(@NotNull Player player);

    /**
     * Checks whether a player is standing inside land claimed by their own town.
     *
     * @param player player to inspect
     * @return {@code true} if the player is standing in their own town
     * @throws NullPointerException if {@code player} is {@code null}
     */
    boolean isPlayerStandingInOwnTown(@NotNull Player player);

    /**
     * Checks whether a player is the mayor/leader of their current town.
     *
     * @param player player to inspect
     * @return {@code true} when the player is recognized as the town mayor/leader
     * @throws NullPointerException if {@code player} is {@code null}
     */
    boolean isPlayerTownMayor(@NotNull Player player);

    /**
     * Resolves a stable identifier for the player's town.
     *
     * <p>The identifier should remain stable across restarts for the same town (for example a UUID),
     * and is used for plugin-owned tax-ledger persistence.</p>
     *
     * @param player player whose town identifier should be resolved
     * @return stable town identifier, or {@code null} when the player is not in a town
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Nullable String getPlayerTownIdentifier(@NotNull Player player);

    /**
     * Resolves a display name for the player's town.
     *
     * @param player player whose town display name should be resolved
     * @return town display name, or {@code null} when unavailable
     * @throws NullPointerException if {@code player} is {@code null}
     */
    @Nullable String getPlayerTownDisplayName(@NotNull Player player);

    /**
     * Deposits funds into the player's own town bank.
     *
     * @param player player whose town should receive the deposit
     * @param amount positive amount to deposit
     * @return {@code true} when the deposit operation succeeds
     * @throws NullPointerException if {@code player} is {@code null}
     */
    boolean depositToTownBank(@NotNull Player player, double amount);

    /**
     * Withdraws funds from the player's own town bank.
     *
     * @param player player whose town should be debited
     * @param amount positive amount to withdraw
     * @return {@code true} when the withdraw operation succeeds
     * @throws NullPointerException if {@code player} is {@code null}
     */
    boolean withdrawFromTownBank(@NotNull Player player, double amount);

    /**
     * Detects the first available protection bridge from the default bridge list.
     *
     * @return the first available bridge, or {@code null} when none are available
     */
    @Nullable
    static RProtectionBridge getBridge() {
        for (final RProtectionBridge bridge : DEFAULT_BRIDGES) {
            if (bridge.isAvailable()) {
                return bridge;
            }
        }
        return null;
    }

    /**
     * Returns the default bridge list in detection order.
     *
     * @return immutable default bridge list
     */
    @NotNull
    static List<RProtectionBridge> getDefaultBridges() {
        return DEFAULT_BRIDGES;
    }
}
