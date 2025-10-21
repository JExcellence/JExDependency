package de.jexcellence.economy.type;

import de.jexcellence.economy.type.EChangeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for managing currency log filtering criteria.
 * <p>
 * This class encapsulates all possible filter options for currency logs,
 * providing a clean interface for building database queries and displaying
 * active filter information to users.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class LogFilter {
    
    /**
     * Filter by specific player UUID.
     */
    public @Nullable UUID playerUuid;
    
    /**
     * Filter by specific currency ID.
     */
    public @Nullable Long currencyId;
    
    /**
     * Filter by log type (BALANCE_CHANGE, CURRENCY_MANAGEMENT, etc.).
     */
    public @Nullable ELogType logType;
    
    /**
     * Filter by log level (INFO, WARNING, ERROR, etc.).
     */
    public @Nullable ELogLevel logLevel;
    
    /**
     * Filter by operation type (DEPOSIT, WITHDRAW, etc.).
     */
    public @Nullable EChangeType operationType;
    
    /**
     * Filter by date range - start date.
     */
    public @Nullable LocalDateTime dateFrom;
    
    /**
     * Filter by date range - end date.
     */
    public @Nullable LocalDateTime dateTo;
    
    /**
     * Filter by success status.
     */
    public @Nullable Boolean successOnly;
    
    /**
     * Filter by initiator UUID.
     */
    public @Nullable UUID initiatorUuid;
    
    /**
     * Checks if any filters are currently active.
     *
     * @return true if at least one filter is set, false otherwise
     */
    public boolean hasActiveFilters() {
        return playerUuid != null ||
               currencyId != null ||
               logType != null ||
               logLevel != null ||
               operationType != null ||
               dateFrom != null ||
               dateTo != null ||
               successOnly != null ||
               initiatorUuid != null;
    }
    
    /**
     * Gets a human-readable description of active filters.
     *
     * @return formatted string describing active filters
     */
    public @NotNull String getFilterDescription() {
        if (!hasActiveFilters()) {
            return "<gradient:#868e96:#adb5bd>No filters active</gradient>";
        }
        
        List<String> activeFilters = new ArrayList<>();
        
        if (playerUuid != null) {
            activeFilters.add("<gradient:#74c0fc:#91a7ff>Player: " + playerUuid + "</gradient>");
        }
        
        if (currencyId != null) {
            activeFilters.add("<gradient:#ffd93d:#ffed4e>Currency ID: " + currencyId + "</gradient>");
        }
        
        if (logType != null) {
            activeFilters.add("<gradient:#51cf66:#8ce99a>Type: " + logType.name() + "</gradient>");
        }
        
        if (logLevel != null) {
            activeFilters.add("<gradient:#ff6b6b:#ffaaaa>Level: " + logLevel.name() + "</gradient>");
        }
        
        if (operationType != null) {
            activeFilters.add("<gradient:#845ef7:#9775fa>Operation: " + operationType.name() + "</gradient>");
        }
        
        if (dateFrom != null) {
            activeFilters.add("<gradient:#868e96:#adb5bd>From: " + dateFrom.toLocalDate() + "</gradient>");
        }
        
        if (dateTo != null) {
            activeFilters.add("<gradient:#868e96:#adb5bd>To: " + dateTo.toLocalDate() + "</gradient>");
        }
        
        if (successOnly != null) {
            activeFilters.add("<gradient:#51cf66:#8ce99a>Success Only: " + successOnly + "</gradient>");
        }
        
        if (initiatorUuid != null) {
            activeFilters.add("<gradient:#74c0fc:#91a7ff>Initiator: " + initiatorUuid + "</gradient>");
        }
        
        return String.join("<gradient:#868e96:#adb5bd>, </gradient>", activeFilters);
    }
    
    /**
     * Clears all active filters.
     */
    public void clearAll() {
        playerUuid = null;
        currencyId = null;
        logType = null;
        logLevel = null;
        operationType = null;
        dateFrom = null;
        dateTo = null;
        successOnly = null;
        initiatorUuid = null;
    }
}