package de.jexcellence.economy.event;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.type.EChangeType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player's balance is about to change.
 * This event is cancellable, allowing other plugins to prevent balance changes.
 */
public class BalanceChangeEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final User       user;
    private final Currency   currency;
    private final double     oldBalance;
    private final double     newBalance;
    private final EChangeType changeType;
    private final String     reason;
    private final Player     initiator;
    
    private boolean cancelled = false;
    private String  cancelReason;
    
    /**
     * Creates a new balance change event.
     *
     * @param user       the user whose balance is changing
     * @param currency   the currency being changed
     * @param oldBalance the current balance
     * @param newBalance the new balance after the change
     * @param changeType the type of change (DEPOSIT, WITHDRAW, SET)
     * @param reason     the reason for the balance change
     * @param initiator  the player who initiated this change (null if system/console)
     */
    public BalanceChangeEvent(
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
    }
    
    /**
     * Gets the user whose balance is changing.
     *
     * @return the user
     */
    @NotNull
    public User getUser() {
        
        return this.user;
    }
    
    /**
     * Gets the currency being changed.
     *
     * @return the currency
     */
    @NotNull
    public Currency getCurrency() {
        
        return this.currency;
    }
    
    /**
     * Gets the current balance before the change.
     *
     * @return the old balance
     */
    public double getOldBalance() {
        
        return this.oldBalance;
    }
    
    /**
     * Gets the new balance after the change.
     *
     * @return the new balance
     */
    public double getNewBalance() {
        
        return this.newBalance;
    }
    
    /**
     * Gets the amount being changed (positive for increase, negative for decrease).
     *
     * @return the change amount
     */
    public double getChangeAmount() {
        
        return this.newBalance - this.oldBalance;
    }
    
    /**
     * Gets the absolute amount being changed.
     *
     * @return the absolute change amount
     */
    public double getAbsoluteChangeAmount() {
        
        return Math.abs(this.getChangeAmount());
    }
    
    /**
     * Gets the type of balance change.
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
     * Checks if this change would result in a negative balance.
     *
     * @return true if the new balance would be negative
     */
    public boolean wouldResultInNegativeBalance() {
        
        return this.newBalance < 0;
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
