package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all currency-related events.
 * Provides common functionality and structure for currency events.
 */
public abstract class CurrencyEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    protected final Currency currency;
    protected final Player   player;
    
    /**
     * Creates a new currency event.
     *
     * @param currency the currency involved in this event
     * @param player   the player involved in this event (can be null for system events)
     */
    public CurrencyEvent(
        final @NotNull Currency currency,
        final @Nullable Player player
    ) {
        
        this.currency = currency;
        this.player = player;
    }
    
    /**
     * Gets the currency involved in this event.
     *
     * @return the currency
     */
    @NotNull
    public Currency getCurrency() {
        
        return this.currency;
    }
    
    /**
     * Gets the player involved in this event.
     *
     * @return the player, or null if this is a system event
     */
    @Nullable
    public Player getPlayer() {
        
        return this.player;
    }
    
    /**
     * Checks if this event involves a player.
     *
     * @return true if a player is involved, false otherwise
     */
    public boolean hasPlayer() {
        
        return this.player != null;
    }
    
    /**
     * Gets handlers.
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        
        return HANDLERS;
    }
    
    /**
     * Gets handlerList.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        
        return HANDLERS;
    }
}
