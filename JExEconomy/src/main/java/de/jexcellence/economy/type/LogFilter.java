package de.jexcellence.economy.type;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 * @version 1.0.1
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
     * Creates a new builder for constructing {@link LogFilter} instances.
     *
     * @return a fresh {@link Builder}
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link LogFilter} instances.
     */
    public static final class Builder {

        private final LogFilter filter;

        private Builder() {
            this.filter = new LogFilter();
        }

        public @NotNull Builder withPlayerUuid(@Nullable UUID playerUuid) {
            this.filter.playerUuid = playerUuid;
            return this;
        }

        public @NotNull Builder withCurrencyId(@Nullable Long currencyId) {
            this.filter.currencyId = currencyId;
            return this;
        }

        public @NotNull Builder withLogType(@Nullable ELogType logType) {
            this.filter.logType = logType;
            return this;
        }

        public @NotNull Builder withLogLevel(@Nullable ELogLevel logLevel) {
            this.filter.logLevel = logLevel;
            return this;
        }

        public @NotNull Builder withOperationType(@Nullable EChangeType operationType) {
            this.filter.operationType = operationType;
            return this;
        }

        public @NotNull Builder withDateFrom(@Nullable LocalDateTime dateFrom) {
            this.filter.dateFrom = dateFrom;
            return this;
        }

        public @NotNull Builder withDateTo(@Nullable LocalDateTime dateTo) {
            this.filter.dateTo = dateTo;
            return this;
        }

        public @NotNull Builder withSuccessOnly(@Nullable Boolean successOnly) {
            this.filter.successOnly = successOnly;
            return this;
        }

        public @NotNull Builder withInitiatorUuid(@Nullable UUID initiatorUuid) {
            this.filter.initiatorUuid = initiatorUuid;
            return this;
        }

        public @NotNull LogFilter build() {
            LogFilter result = new LogFilter();
            result.playerUuid = this.filter.playerUuid;
            result.currencyId = this.filter.currencyId;
            result.logType = this.filter.logType;
            result.logLevel = this.filter.logLevel;
            result.operationType = this.filter.operationType;
            result.dateFrom = this.filter.dateFrom;
            result.dateTo = this.filter.dateTo;
            result.successOnly = this.filter.successOnly;
            result.initiatorUuid = this.filter.initiatorUuid;
            return result;
        }
    }

    public @NotNull LogFilter setPlayerUuid(@Nullable UUID playerUuid) {
        this.playerUuid = playerUuid;
        return this;
    }

    public @NotNull LogFilter setCurrencyId(@Nullable Long currencyId) {
        this.currencyId = currencyId;
        return this;
    }

    public @NotNull LogFilter setLogType(@Nullable ELogType logType) {
        this.logType = logType;
        return this;
    }

    public @NotNull LogFilter setLogLevel(@Nullable ELogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public @NotNull LogFilter setOperationType(@Nullable EChangeType operationType) {
        this.operationType = operationType;
        return this;
    }

    public @NotNull LogFilter setDateFrom(@Nullable LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
        return this;
    }

    public @NotNull LogFilter setDateTo(@Nullable LocalDateTime dateTo) {
        this.dateTo = dateTo;
        return this;
    }

    public @NotNull LogFilter setSuccessOnly(@Nullable Boolean successOnly) {
        this.successOnly = successOnly;
        return this;
    }

    public @NotNull LogFilter setInitiatorUuid(@Nullable UUID initiatorUuid) {
        this.initiatorUuid = initiatorUuid;
        return this;
    }
    
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

    /**
     * Determines whether the provided log entry satisfies all configured criteria.
     *
     * @param log the log entry to evaluate
     * @return {@code true} when the log matches every active criterion
     */
    public boolean matches(final @NotNull CurrencyLog log) {
        if (logType != null && !Objects.equals(logType, log.getLogType())) {
            return false;
        }

        if (logLevel != null && !Objects.equals(logLevel, log.getLogLevel())) {
            return false;
        }

        if (operationType != null && !Objects.equals(operationType, log.getOperationType())) {
            return false;
        }

        if (playerUuid != null && !Objects.equals(playerUuid, log.getPlayerUuid())) {
            return false;
        }

        if (initiatorUuid != null && !Objects.equals(initiatorUuid, log.getInitiatorUuid())) {
            return false;
        }

        if (currencyId != null) {
            Currency currency = log.getCurrency();
            Long logCurrencyId = currency != null ? currency.getId() : null;
            if (!Objects.equals(currencyId, logCurrencyId)) {
                return false;
            }
        }

        LocalDateTime timestamp = log.getTimestamp();
        if (dateFrom != null && timestamp.isBefore(dateFrom)) {
            return false;
        }

        if (dateTo != null && timestamp.isAfter(dateTo)) {
            return false;
        }

        if (successOnly != null && log.wasSuccessful() != successOnly) {
            return false;
        }

        return true;
    }
}