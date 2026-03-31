package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a currency is about to be created.
 * This event is cancellable, allowing other plugins to prevent currency creation.
 */
public class CurrencyCreateEvent extends CurrencyEvent implements Cancellable {
    
    private static final HandlerList HANDLERS  = new HandlerList();
    private              boolean     cancelled = false;
    private              String      cancelReason;
    
    /**
     * Creates a new currency creation event.
     *
     * @param currency the currency being created
     * @param player   the player creating the currency (null if created by system/console)
     */
    public CurrencyCreateEvent(
        final @NotNull Currency currency,
        final @Nullable Player player
    ) {
        
        super(
            currency,
            player
        );
    }
    
    /**
     * Gets the reason why this event was cancelled.
     *
     * @return the cancellation reason, or null if not cancelled
     */
    @Nullable
    public String getCancelReason() {
        
        return this.cancelReason;
    }
    
    /**
     * Sets the cancellation reason.
     *
     * @param reason the reason for cancellation
     */
    public void setCancelReason(@Nullable String reason) {
        
        this.cancelReason = reason;
    }
    
    /**
     * Returns whether cancelled.
     */
    @Override
    public boolean isCancelled() {
        
        return this.cancelled;
    }
    
    /**
     * Sets cancelled.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        
        this.cancelled = cancelled;
    }
    
    /**
     * Cancels this event with a specific reason.
     *
     * @param reason the reason for cancellation
     */
    public void setCancelled(@NotNull String reason) {
        
        this.cancelled = true;
        this.cancelReason = reason;
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
