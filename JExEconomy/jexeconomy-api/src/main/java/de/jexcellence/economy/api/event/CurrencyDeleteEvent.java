package de.jexcellence.economy.api.event;

import de.jexcellence.economy.api.CurrencySnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired <em>before</em> a currency is deleted. Cancellable.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyDeleteEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull CurrencySnapshot currency;
    private final @Nullable UUID deleterUuid;
    private final int affectedPlayers;
    private final double totalBalance;

    private boolean cancelled;
    private @Nullable String cancellationReason;

    /**
     * Creates a new currency delete event.
     *
     * @param currency        the currency being deleted
     * @param deleterUuid     UUID of the player deleting the currency, or null if server-initiated
     * @param affectedPlayers number of players holding this currency
     * @param totalBalance    total balance across all affected players
     */
    public CurrencyDeleteEvent(@NotNull CurrencySnapshot currency,
                               @Nullable UUID deleterUuid,
                               int affectedPlayers, double totalBalance) {
        this.currency = currency;
        this.deleterUuid = deleterUuid;
        this.affectedPlayers = affectedPlayers;
        this.totalBalance = totalBalance;
    }

    /**
     * Returns the currency being deleted.
     *
     * @return the currency being deleted
     */
    public @NotNull CurrencySnapshot getCurrency() { return currency; }

    /**
     * Returns the UUID of the player deleting the currency, or null.
     *
     * @return UUID of the player deleting the currency, or null
     */
    public @Nullable UUID getDeleterUuid() { return deleterUuid; }

    /**
     * Returns the number of players holding this currency.
     *
     * @return number of players holding this currency
     */
    public int getAffectedPlayers() { return affectedPlayers; }

    /**
     * Returns the total balance across all affected players.
     *
     * @return total balance across all affected players
     */
    public double getTotalBalance() { return totalBalance; }

    /**
     * Returns the optional reason why this event was cancelled.
     *
     * @return optional reason why this event was cancelled
     */
    public @Nullable String getCancellationReason() { return cancellationReason; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    /**
     * Cancels this event with a specific reason.
     *
     * @param reason the cancellation reason
     */
    public void setCancelled(@NotNull String reason) {
        this.cancelled = true;
        this.cancellationReason = reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /**
     * Returns the handler list for this event type.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
