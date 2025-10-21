package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired after a currency has been successfully created.
 * This event is not cancellable as the creation has already occurred.
 */
public class CurrencyCreatedEvent extends CurrencyEvent {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private final        long        creationTime;
    
    /**
     * Creates a new currency created event.
     *
     * @param currency the currency that was created
     * @param player   the player who created the currency (null if created by system/console)
     */
    public CurrencyCreatedEvent(
        final @NotNull Currency currency,
        final @Nullable Player player
    ) {
        
        super(
            currency,
            player
        );
        this.creationTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the timestamp when the currency was created.
     *
     * @return the creation timestamp in milliseconds
     */
    public long getCreationTime() {
        
        return this.creationTime;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        
        return HANDLERS;
    }
    
}