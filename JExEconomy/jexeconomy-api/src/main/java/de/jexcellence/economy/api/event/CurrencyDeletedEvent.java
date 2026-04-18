package de.jexcellence.economy.api.event;

import de.jexcellence.economy.api.CurrencySnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Fired <em>after</em> a currency has been deleted successfully.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class CurrencyDeletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull CurrencySnapshot currency;
    private final @Nullable UUID deleterUuid;
    private final int affectedPlayers;
    private final double totalBalanceLost;
    private final @NotNull Instant deletionTime;

    /**
     * Creates a new currency deleted event.
     *
     * @param currency         the deleted currency
     * @param deleterUuid      UUID of the player who deleted the currency, or null if server-initiated
     * @param affectedPlayers  number of players who held this currency
     * @param totalBalanceLost total balance lost across all affected players
     */
    public CurrencyDeletedEvent(@NotNull CurrencySnapshot currency,
                                @Nullable UUID deleterUuid,
                                int affectedPlayers, double totalBalanceLost) {
        this.currency = currency;
        this.deleterUuid = deleterUuid;
        this.affectedPlayers = affectedPlayers;
        this.totalBalanceLost = totalBalanceLost;
        this.deletionTime = Instant.now();
    }

    /**
     * Returns the deleted currency.
     *
     * @return the deleted currency
     */
    public @NotNull CurrencySnapshot getCurrency() { return currency; }

    /**
     * Returns the UUID of the player who deleted the currency, or null.
     *
     * @return UUID of the player who deleted the currency, or null
     */
    public @Nullable UUID getDeleterUuid() { return deleterUuid; }

    /**
     * Returns the number of players who held this currency.
     *
     * @return number of players who held this currency
     */
    public int getAffectedPlayers() { return affectedPlayers; }

    /**
     * Returns the total balance lost across all affected players.
     *
     * @return total balance lost across all affected players
     */
    public double getTotalBalanceLost() { return totalBalanceLost; }

    /**
     * Returns the instant the currency was deleted.
     *
     * @return the instant the currency was deleted
     */
    public @NotNull Instant getDeletionTime() { return deletionTime; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /**
     * Returns the handler list for this event type.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
