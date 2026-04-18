package de.jexcellence.economy.api.event;

import de.jexcellence.economy.api.CurrencySnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Fired <em>after</em> a currency has been created successfully.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull CurrencySnapshot currency;
    private final @Nullable UUID creatorUuid;
    private final @NotNull Instant creationTime;

    /**
     * Creates a new currency created event.
     *
     * @param currency    the newly created currency
     * @param creatorUuid UUID of the player who created the currency, or null if server-initiated
     */
    public CurrencyCreatedEvent(@NotNull CurrencySnapshot currency,
                                @Nullable UUID creatorUuid) {
        this.currency = currency;
        this.creatorUuid = creatorUuid;
        this.creationTime = Instant.now();
    }

    /**
     * Returns the newly created currency.
     *
     * @return the newly created currency
     */
    public @NotNull CurrencySnapshot getCurrency() { return currency; }

    /**
     * Returns the UUID of the player who created the currency, or null.
     *
     * @return UUID of the player who created the currency, or null
     */
    public @Nullable UUID getCreatorUuid() { return creatorUuid; }

    /**
     * Returns the instant the currency was created.
     *
     * @return the instant the currency was created
     */
    public @NotNull Instant getCreationTime() { return creationTime; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /**
     * Returns the handler list for this event type.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
