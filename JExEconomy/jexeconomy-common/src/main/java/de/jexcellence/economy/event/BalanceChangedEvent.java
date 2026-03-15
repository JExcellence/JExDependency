package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.type.EChangeType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired after a player's balance has been successfully changed.
 * This event is not cancellable as the change has already occurred.
 */
public class BalanceChangedEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final User                          user;
    private final Currency                      currency;
    private final double                        oldBalance;
    private final double      newBalance;
    private final EChangeType changeType;
    private final String      reason;
    private final Player                        initiator;
    private final long                          changeTime;
    
    /**
     * Creates a new balance changed event.
     *
     * @param user       the user whose balance changed
     * @param currency   the currency that was changed
     * @param oldBalance the balance before the change
     * @param newBalance the balance after the change
     * @param changeType the type of change that occurred
     * @param reason     the reason for the balance change
     * @param initiator  the player who initiated this change (null if system/console)
     */
    public BalanceChangedEvent(
        final @NotNull User user,
        final @NotNull Currency currency,
        final double oldBalance,
        final double newBalance,
        final @NotNull EChangeType changeType,
        final @Nullable String reason,
        final @Nullable Player initiator
    ) {
        
        this.user = user;
        this.currency = currency;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.changeType = changeType;
        this.reason = reason;
        this.initiator = initiator;
        this.changeTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the user whose balance changed.
     *
     * @return the user
     */
    @NotNull
    public User getUser() {
        
        return this.user;
    }
    
    /**
     * Gets the currency that was changed.
     *
     * @return the currency
     */
    @NotNull
    public Currency getCurrency() {
        
        return this.currency;
    }
    
    /**
     * Gets the balance before the change.
     *
     * @return the old balance
     */
    public double getOldBalance() {
        
        return this.oldBalance;
    }
    
    /**
     * Gets the balance after the change.
     *
     * @return the new balance
     */
    public double getNewBalance() {
        
        return this.newBalance;
    }
    
    /**
     * Gets the amount that was changed (positive for increase, negative for decrease).
     *
     * @return the change amount
     */
    public double getChangeAmount() {
        
        return this.newBalance - this.oldBalance;
    }
    
    /**
     * Gets the absolute amount that was changed.
     *
     * @return the absolute change amount
     */
    public double getAbsoluteChangeAmount() {
        
        return Math.abs(this.getChangeAmount());
    }
    
    /**
     * Gets the type of balance change that occurred.
     *
     * @return the change type
     */
    @NotNull
    public EChangeType getChangeType() {
        
        return this.changeType;
    }
    
    /**
     * Gets the reason for the balance change.
     *
     * @return the reason, or null if no reason provided
     */
    @Nullable
    public String getReason() {
        
        return this.reason;
    }
    
    /**
     * Gets the player who initiated this balance change.
     *
     * @return the initiator, or null if system/console initiated
     */
    @Nullable
    public Player getInitiator() {
        
        return this.initiator;
    }
    
    /**
     * Gets the timestamp when the balance change occurred.
     *
     * @return the change timestamp in milliseconds
     */
    public long getChangeTime() {
        
        return this.changeTime;
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
