package de.jexcellence.economy.api.event;

import de.jexcellence.economy.api.CurrencySnapshot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired <em>before</em> a currency is created. Cancellable.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull CurrencySnapshot currency;
    private final @Nullable UUID creatorUuid;

    private boolean cancelled;
    private @Nullable String cancellationReason;

    /**
     * Creates a new currency create event.
     *
     * @param currency    the currency being created
     * @param creatorUuid UUID of the player creating the currency, or null if server-initiated
     */
    public CurrencyCreateEvent(@NotNull CurrencySnapshot currency,
                               @Nullable UUID creatorUuid) {
        this.currency = currency;
        this.creatorUuid = creatorUuid;
    }

    /**
     * Returns the currency being created.
     *
     * @return the currency being created
     */
    public @NotNull CurrencySnapshot getCurrency() { return currency; }

    /**
     * Returns the UUID of the player creating the currency, or null.
     *
     * @return UUID of the player creating the currency, or null
     */
    public @Nullable UUID getCreatorUuid() { return creatorUuid; }

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
