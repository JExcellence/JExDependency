package de.jexcellence.economy.event;


import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.service.CurrencyLogService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener responsible for handling currency-related events and logging them to the database.
 * <p>
 * This listener maintains the separation of concerns by handling all database logging operations
 * for currency events fired by the CurrencyAdapter. It ensures that all significant currency
 * operations are properly logged for auditing, monitoring, and debugging purposes.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class CurrencyLogEventListener implements Listener {
    
    private static final Logger LISTENER_LOGGER = CentralLogger.getLogger(CurrencyLogEventListener.class.getName());
    
    private final CurrencyLogService logService;
    
    /**
     * Constructs a new CurrencyEventListener with the specified logging service.
     *
     * @param jexEconomyImpl the currency service for database operations, must not be null
     * @throws IllegalArgumentException if logService is null
     */
    public CurrencyLogEventListener(
        final @NotNull JExEconomyImpl jexEconomyImpl
    ) {
        this.logService = jexEconomyImpl.getLogService();
    }
    
    /**
     * Handles balance change events (before the change occurs).
     * <p>
     * This method logs attempted balance changes, including cancelled operations.
     * It provides a complete audit trail of all balance change attempts.
     * </p>
     *
     * @param event the balance change event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBalanceChange(final @NotNull BalanceChangeEvent event) {
        try {
            final boolean success = !event.isCancelled();
            final String errorMessage = event.isCancelled() ? event.getCancelReason() : null;
            
            this.logService.logBalanceChange(
                event.getUser(),
                event.getCurrency(),
                event.getChangeType(),
                event.getOldBalance(),
                event.getNewBalance(),
                event.getAbsoluteChangeAmount(),
                event.getReason(),
                event.getInitiator(),
                success,
                errorMessage
            ).exceptionally(throwable -> {
                LISTENER_LOGGER.log(Level.WARNING, "Failed to log balance change event", throwable);
                return null;
            });
            
        } catch (final Exception e) {
            LISTENER_LOGGER.log(Level.WARNING, "Error handling balance change event", e);
        }
    }
    
    /**
     * Handles balance changed events (after the change has occurred).
     * <p>
     * This method logs successful balance changes with the final balance amounts.
     * It provides confirmation that the database operations completed successfully.
     * </p>
     *
     * @param event the balance changed event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBalanceChanged(final @NotNull BalanceChangedEvent event) {
        try {
            this.logService.logBalanceChange(
                event.getUser(),
                event.getCurrency(),
                event.getChangeType(),
                event.getOldBalance(),
                event.getNewBalance(),
                event.getAbsoluteChangeAmount(),
                event.getReason(),
                event.getInitiator(),
                true,
                null
            ).exceptionally(throwable -> {
                LISTENER_LOGGER.log(Level.WARNING, "Failed to log balance changed event", throwable);
                return null;
            });
            
        } catch (final Exception e) {
            LISTENER_LOGGER.log(Level.WARNING, "Error handling balance changed event", e);
        }
    }
    
    /**
     * Handles currency creation events (before creation).
     *
     * @param event the currency create event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCurrencyCreate(final @NotNull CurrencyCreateEvent event) {
        try {
            final boolean success = !event.isCancelled();
            final String errorMessage = event.isCancelled() ? event.getCancelReason() : null;
            
            this.logService.logCurrencyManagement(
                event.getCurrency(),
                "CREATE_ATTEMPT",
                event.getPlayer(),
                success,
                "Currency creation attempt: " + event.getCurrency().getIdentifier(),
                errorMessage
            ).exceptionally(throwable -> {
                LISTENER_LOGGER.log(Level.WARNING, "Failed to log currency create event", throwable);
                return null;
            });
            
        } catch (final Exception e) {
            LISTENER_LOGGER.log(Level.WARNING, "Error handling currency create event", e);
        }
    }
    
    /**
     * Handles currency created events (after successful creation).
     *
     * @param event the currency created event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCurrencyCreated(final @NotNull CurrencyCreatedEvent event) {
        try {
            this.logService.logCurrencyManagement(
                event.getCurrency(),
                "CREATE_SUCCESS",
                event.getPlayer(),
                true,
                "Currency created successfully: " + event.getCurrency().getIdentifier(),
                null
            ).exceptionally(throwable -> {
                LISTENER_LOGGER.log(Level.WARNING, "Failed to log currency created event", throwable);
                return null;
            });
            
        } catch (final Exception e) {
            LISTENER_LOGGER.log(Level.WARNING, "Error handling currency created event", e);
        }
    }
    
    /**
     * Handles currency deletion events (before deletion).
     *
     * @param event the currency delete event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCurrencyDelete(final @NotNull CurrencyDeleteEvent event) {
        try {
            final boolean success = !event.isCancelled();
            final String errorMessage = event.isCancelled() ? event.getCancelReason() : null;
            final String details = String.format(
                "Deletion attempt - Affected players: %d, Total balance: %.2f",
                event.getAffectedPlayers(),
                event.getTotalBalance()
            );
            
            this.logService.logCurrencyManagement(
                event.getCurrency(),
                "DELETE_ATTEMPT",
                event.getPlayer(),
                success,
                details,
                errorMessage
            ).exceptionally(throwable -> {
                LISTENER_LOGGER.log(Level.WARNING, "Failed to log currency delete event", throwable);
                return null;
            });
            
        } catch (final Exception e) {
            LISTENER_LOGGER.log(Level.WARNING, "Error handling currency delete event", e);
        }
    }
    
    /**
     * Handles currency deleted events (after successful deletion).
     *
     * @param event the currency deleted event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCurrencyDeleted(final @NotNull CurrencyDeletedEvent event) {
        try {
            final String details = String.format(
                "Currency deleted successfully - Affected players: %d, Total balance lost: %.2f",
                event.getAffectedPlayers(),
                event.getTotalBalanceLost()
            );
            
            this.logService.logCurrencyManagement(
                event.getCurrency(),
                "DELETE_SUCCESS",
                event.getPlayer(),
                true,
                details,
                null
            ).exceptionally(throwable -> {
                LISTENER_LOGGER.log(Level.WARNING, "Failed to log currency deleted event", throwable);
                return null;
            });
            
        } catch (final Exception e) {
            LISTENER_LOGGER.log(Level.WARNING, "Error handling currency deleted event", e);
        }
    }
}