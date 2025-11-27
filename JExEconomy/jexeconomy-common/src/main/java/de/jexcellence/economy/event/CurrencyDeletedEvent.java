package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired after a currency has been successfully deleted.
 * This event is not cancellable as the deletion has already occurred.
 */
public class CurrencyDeletedEvent extends CurrencyEvent {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private final        long        deletionTime;
    private final        int         affectedPlayers;
    private final        double      totalBalanceLost;
    
    /**
     * Creates a new currency deleted event.
     *
     * @param currency         the currency that was deleted
     * @param player           the player who deleted the currency (null if deleted by system/console)
     * @param affectedPlayers  the number of players who had this currency
     * @param totalBalanceLost the total balance that was lost when this currency was deleted
     */
    public CurrencyDeletedEvent(
        final @NotNull Currency currency,
        final @Nullable Player player,
        final int affectedPlayers,
        final double totalBalanceLost
    ) {
        
        super(
            currency,
            player
        );
        this.deletionTime = System.currentTimeMillis();
        this.affectedPlayers = affectedPlayers;
        this.totalBalanceLost = totalBalanceLost;
    }
    
    /**
     * Gets the timestamp when the currency was deleted.
     *
     * @return the deletion timestamp in milliseconds
     */
    public long getDeletionTime() {
        
        return this.deletionTime;
    }
    
    /**
     * Gets the number of players who were affected by this deletion.
     *
     * @return the number of affected players
     */
    public int getAffectedPlayers() {
        
        return this.affectedPlayers;
    }
    
    /**
     * Gets the total balance that was lost when this currency was deleted.
     *
     * @return the total balance that was lost
     */
    public double getTotalBalanceLost() {
        
        return this.totalBalanceLost;
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