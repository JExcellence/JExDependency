package de.jexcellence.economy.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable snapshot of a player's account for a specific currency.
 *
 * <p>This is a lightweight, API-safe representation without
 * persistence annotations. Returned by {@link EconomyProvider}
 * for balance queries and leaderboards.
 *
 * @param playerUuid         the player's UUID
 * @param playerName         the player's current name
 * @param currencyIdentifier the currency identifier this account belongs to
 * @param balance            the current balance
 * @author JExcellence
 * @since 3.0.0
 */
public record AccountSnapshot(@NotNull UUID playerUuid,
                               @NotNull String playerName,
                               @NotNull String currencyIdentifier,
                               double balance) {
}
