package de.jexcellence.economy.api.event;

import de.jexcellence.economy.api.ChangeType;
import de.jexcellence.economy.api.CurrencySnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Fired <em>after</em> a player's balance has changed successfully.
 *
 * <p>This event is informational and cannot be cancelled. Use
 * {@link BalanceChangeEvent} to intercept changes before they happen.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class BalanceChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull UUID playerUuid;
    private final @NotNull String playerName;
    private final @NotNull CurrencySnapshot currency;
    private final double oldBalance;
    private final double newBalance;
    private final @NotNull ChangeType changeType;
    private final @Nullable String reason;
    private final @Nullable UUID initiatorUuid;
    private final @NotNull Instant changeTime;

    /**
     * Creates a new balance changed event.
     *
     * @param playerUuid    UUID of the affected player
     * @param playerName    current name of the affected player
     * @param currency      the currency involved
     * @param oldBalance    balance before the change
     * @param newBalance    balance after the change
     * @param changeType    type of balance change
     * @param reason        optional human-readable reason
     * @param initiatorUuid optional UUID of the player who initiated the change
     */
    public BalanceChangedEvent(@NotNull UUID playerUuid,
                               @NotNull String playerName,
                               @NotNull CurrencySnapshot currency,
                               double oldBalance, double newBalance,
                               @NotNull ChangeType changeType,
                               @Nullable String reason,
                               @Nullable UUID initiatorUuid) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.currency = currency;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.changeType = changeType;
        this.reason = reason;
        this.initiatorUuid = initiatorUuid;
        this.changeTime = Instant.now();
    }

    /**
     * Returns the UUID of the affected player.
     *
     * @return UUID of the affected player
     */
    public @NotNull UUID getPlayerUuid() { return playerUuid; }

    /**
     * Returns the current name of the affected player.
     *
     * @return current name of the affected player
     */
    public @NotNull String getPlayerName() { return playerName; }

    /**
     * Returns the currency involved.
     *
     * @return the currency involved
     */
    public @NotNull CurrencySnapshot getCurrency() { return currency; }

    /**
     * Returns the balance before the change.
     *
     * @return balance before the change
     */
    public double getOldBalance() { return oldBalance; }

    /**
     * Returns the balance after the change.
     *
     * @return balance after the change
     */
    public double getNewBalance() { return newBalance; }

    /**
     * Returns the signed difference between new and old balance.
     *
     * @return the signed difference between new and old balance
     */
    public double getChangeAmount() { return newBalance - oldBalance; }

    /**
     * Returns the type of balance change.
     *
     * @return type of balance change
     */
    public @NotNull ChangeType getChangeType() { return changeType; }

    /**
     * Returns the optional human-readable reason.
     *
     * @return optional human-readable reason
     */
    public @Nullable String getReason() { return reason; }

    /**
     * Returns the optional UUID of the initiator.
     *
     * @return optional UUID of the initiator
     */
    public @Nullable UUID getInitiatorUuid() { return initiatorUuid; }

    /**
     * Returns the instant the balance change occurred.
     *
     * @return the instant the balance change occurred
     */
    public @NotNull Instant getChangeTime() { return changeTime; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /**
     * Returns the handler list for this event type.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
