package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a currency is about to be deleted.
 * This event is cancellable, allowing other plugins to prevent currency deletion.
 */
public class CurrencyDeleteEvent extends CurrencyEvent implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private              boolean     cancelled = false;
    private              String      cancelReason;
    private final        int         affectedPlayers;
    private final        double      totalBalance;
    
    /**
     * Creates a new currency deletion event.
     *
     * @param currency        the currency being deleted
     * @param player          the player deleting the currency (null if deleted by system/console)
     * @param affectedPlayers the number of players who have this currency
     * @param totalBalance    the total balance across all players for this currency
     */
    public CurrencyDeleteEvent(
        final @NotNull Currency currency,
        final @Nullable Player player,
        final int affectedPlayers,
        final double totalBalance
    ) {
        
        super(
            currency,
            player
        );
        
        this.affectedPlayers = affectedPlayers;
        this.totalBalance = totalBalance;
    }
    
    /**
     * Gets the number of players who will be affected by this deletion.
     *
     * @return the number of affected players
     */
    public int getAffectedPlayers() {
        
        return this.affectedPlayers;
    }
    
    /**
     * Gets the total balance that will be lost when this currency is deleted.
     *
     * @return the total balance across all players
     */
    public double getTotalBalance() {
        
        return this.totalBalance;
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
    public void setCancelReason(
        @Nullable String reason
    ) {
        
        this.cancelReason = reason;
    }
    
    /**
     * Returns whether cancelled.
     */
    @Override
    public boolean isCancelled() {
        
        return cancelled;
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
