package de.jexcellence.economy.service;

import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service class for managing currency operation logging.
 * <p>
 * Provides high-level methods for creating, querying, and managing
 * currency log entries. Handles automatic log creation for common
 * operations and provides analytics capabilities.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class CurrencyLogService {
    
    private final CurrencyLogRepository logRepository;
    private final Executor executor;
    
    /**
     * Creates a new CurrencyLogService.
     *
     * @param logRepository the repository for log operations
     * @param executor the executor for async operations
     */
    public CurrencyLogService(
        @NotNull CurrencyLogRepository logRepository,
        @NotNull Executor executor
    ) {
        this.logRepository = logRepository;
        this.executor = executor;
    }
    
    /**
     * Logs a balance change operation.
     *
     * @param user the affected user
     * @param currency the currency involved
     * @param operationType the type of operation
     * @param oldBalance the balance before the change
     * @param newBalance the balance after the change
     * @param amount the amount involved in the operation
     * @param reason the reason for the change
     * @param initiator the player who initiated the change (null if system)
     * @param success whether the operation was successful
     * @param errorMessage error message if the operation failed
     * @return CompletableFuture that completes when the log is saved
     */
    @NotNull
    public CompletableFuture<CurrencyLog> logBalanceChange(
        @NotNull User user,
        @NotNull Currency currency,
        @NotNull EChangeType operationType,
        double oldBalance,
        double newBalance,
        double amount,
        @Nullable String reason,
        @Nullable Player initiator,
        boolean success,
        @Nullable String errorMessage
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String description = String.format(
                "%s %.2f %s %s %s account",
                success ? "Successfully" : "Failed to",
                amount,
                operationType.name().toLowerCase(),
                success ? "to/from" : "to/from",
                currency.getIdentifier()
            );
            
            CurrencyLog log = new CurrencyLog(
                user.getUniqueId(),
                user.getPlayerName(),
                currency,
                operationType,
                oldBalance,
                newBalance,
                amount,
                description,
                reason
            );
            
            log.setSuccess(success);
            if (errorMessage != null) {
                log.setErrorMessage(errorMessage);
                log.setLogLevel(ELogLevel.ERROR);
            }
            
            if (initiator != null) {
                log.setInitiatorUuid(initiator.getUniqueId());
                log.setInitiatorName(initiator.getName());
                log.setIpAddress(initiator.getAddress() != null ?
                                 initiator.getAddress().getAddress().getHostAddress() : null);
            }
            
            return logRepository.create(log);
        }, executor);
    }
    
    /**
     * Logs a currency management operation (creation, deletion, etc.).
     *
     * @param currency the currency involved
     * @param operation the operation performed
     * @param initiator the player who performed the operation (null if system)
     * @param success whether the operation was successful
     * @param details additional details about the operation
     * @param errorMessage error message if the operation failed
     * @return CompletableFuture that completes when the log is saved
     */
    @NotNull
    public CompletableFuture<CurrencyLog> logCurrencyManagement(
        @NotNull Currency currency,
        @NotNull String operation,
        @Nullable Player initiator,
        boolean success,
        @Nullable String details,
        @Nullable String errorMessage
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String description = String.format(
                "%s currency '%s'",
                success ? ("Successfully " + operation) : ("Failed to " + operation),
                currency.getIdentifier()
            );
            
            CurrencyLog log = new CurrencyLog(
                ELogType.MANAGEMENT,
                success ? ELogLevel.INFO : ELogLevel.ERROR,
                description
            );
            
            log.setCurrency(currency);
            log.setSuccess(success);
            log.setDetails(details);
            
            if (errorMessage != null) {
                log.setErrorMessage(errorMessage);
            }
            
            if (initiator != null) {
                log.setInitiatorUuid(initiator.getUniqueId());
                log.setInitiatorName(initiator.getName());
                log.setIpAddress(initiator.getAddress() != null ?
                                 initiator.getAddress().getAddress().getHostAddress() : null);
            }
            
            return logRepository.create(log);
        }, executor);
    }
    
    /**
     * Logs a system operation or event.
     *
     * @param description description of the system operation
     * @param level the severity level
     * @param details additional details
     * @param errorMessage error message if applicable
     * @return CompletableFuture that completes when the log is saved
     */
    @NotNull
    public CompletableFuture<CurrencyLog> logSystemOperation(
        @NotNull String description,
        @NotNull ELogLevel level,
        @Nullable String details,
        @Nullable String errorMessage
    ) {
        return CompletableFuture.supplyAsync(() -> {
            CurrencyLog log = new CurrencyLog(ELogType.SYSTEM, level, description);
            log.setDetails(details);
            log.setSuccess(errorMessage == null);
            
            if (errorMessage != null) {
                log.setErrorMessage(errorMessage);
            }
            
            return logRepository.create(log);
        }, executor);
    }
    
    /**
     * Logs an audit event (administrative action, permission check, etc.).
     *
     * @param player the player involved in the audit event
     * @param action the action performed
     * @param target the target of the action (player, currency, etc.)
     * @param success whether the action was successful
     * @param details additional details
     * @return CompletableFuture that completes when the log is saved
     */
    @NotNull
    public CompletableFuture<CurrencyLog> logAuditEvent(
        @NotNull Player player,
        @NotNull String action,
        @Nullable String target,
        boolean success,
        @Nullable String details
    ) {
        return CompletableFuture.supplyAsync(() -> {
            String description = String.format(
                "Player %s %s %s %s",
                player.getName(),
                success ? "successfully" : "failed to",
                action,
                target != null ? ("on " + target) : ""
            ).trim();
            
            CurrencyLog log = new CurrencyLog(
                ELogType.AUDIT,
                success ? ELogLevel.INFO : ELogLevel.WARNING,
                description
            );
            
            log.setPlayerUuid(player.getUniqueId());
            log.setPlayerName(player.getName());
            log.setIpAddress(player.getAddress() != null ?
                             player.getAddress().getAddress().getHostAddress() : null);
            log.setSuccess(success);
            log.setDetails(details);
            
            return logRepository.create(log);
        }, executor);
    }
    
    /**
     * Gets recent transaction history for a player.
     *
     * @param playerUuid the UUID of the player
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing the transaction history
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getPlayerTransactionHistory(
        @NotNull UUID playerUuid,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> logs = logRepository.findAllByAttributes(Map.of(
                "playerUuid", playerUuid,
                "logType", ELogType.TRANSACTION,
                "success", true
            ));
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets transaction history for a specific currency.
     *
     * @param currencyId the ID of the currency
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing the transaction history
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getCurrencyTransactionHistory(
        @NotNull Long currencyId,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> logs = logRepository.findAllByAttributes(Map.of(
                "currency.id", currencyId,
                "logType", ELogType.TRANSACTION,
                "success", true
            ));
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets recent error logs for monitoring and debugging.
     *
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing recent error logs
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getRecentErrors(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> errorLogs = logRepository.findAllByAttributes(Map.of(
                "logLevel", ELogLevel.ERROR
            ));
            
            List<CurrencyLog> criticalLogs = logRepository.findAllByAttributes(Map.of(
                "logLevel", ELogLevel.CRITICAL
            ));
            
            List<CurrencyLog> failedLogs = logRepository.findAllByAttributes(Map.of(
                "success", false
            ));
            
            return List.of(errorLogs, criticalLogs, failedLogs).stream()
                       .flatMap(List::stream)
                       .distinct()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets logs by multiple criteria.
     *
     * @param playerUuid the player UUID (null for any)
     * @param currencyId the currency ID (null for any)
     * @param logType the log type (null for any)
     * @param logLevel the log level (null for any)
     * @param success the success status (null for any)
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing matching logs
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getLogsByCriteria(
        @Nullable UUID playerUuid,
        @Nullable Long currencyId,
        @Nullable ELogType logType,
        @Nullable ELogLevel logLevel,
        @Nullable Boolean success,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> attributes = new java.util.HashMap<>();
            
            if (playerUuid != null) {
                attributes.put("playerUuid", playerUuid);
            }
            if (currencyId != null) {
                attributes.put("currency.id", currencyId);
            }
            if (logType != null) {
                attributes.put("logType", logType);
            }
            if (logLevel != null) {
                attributes.put("logLevel", logLevel);
            }
            if (success != null) {
                attributes.put("success", success);
            }
            
            List<CurrencyLog> logs = logRepository.findAllByAttributes(attributes);
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets logs by operation type.
     *
     * @param operationType the operation type to filter by
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing matching logs
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getLogsByOperationType(
        @NotNull EChangeType operationType,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> logs = logRepository.findAllByAttributes(Map.of(
                "operationType", operationType,
                "logType", ELogType.TRANSACTION
            ));
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets management logs for a specific currency.
     *
     * @param currencyId the ID of the currency
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing management logs
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getCurrencyManagementLogs(
        @NotNull Long currencyId,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> logs = logRepository.findAllByAttributes(Map.of(
                "currency.id", currencyId,
                "logType", ELogType.MANAGEMENT
            ));
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets audit logs for a specific player.
     *
     * @param playerUuid the UUID of the player
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing audit logs
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getPlayerAuditLogs(
        @NotNull UUID playerUuid,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> logs = logRepository.findAllByAttributes(Map.of(
                "playerUuid", playerUuid,
                "logType", ELogType.AUDIT
            ));
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Gets system logs.
     *
     * @param logLevel the minimum log level (null for all levels)
     * @param limit the maximum number of entries to return
     * @return CompletableFuture containing system logs
     */
    @NotNull
    public CompletableFuture<List<CurrencyLog>> getSystemLogs(
        @Nullable ELogLevel logLevel,
        int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> attributes = Map.of("logType", ELogType.SYSTEM);
            
            if (logLevel != null) {
                attributes = Map.of(
                    "logType", ELogType.SYSTEM,
                    "logLevel", logLevel
                );
            }
            
            List<CurrencyLog> logs = logRepository.findAllByAttributes(attributes);
            
            return logs.stream()
                       .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                       .limit(limit)
                       .toList();
        }, executor);
    }
    
    /**
     * Basic currency usage statistics using simple queries.
     *
     * @param currencyId the ID of the currency
     * @return CompletableFuture containing basic usage statistics
     */
    @NotNull
    public CompletableFuture<BasicCurrencyStats> getBasicCurrencyStats(@NotNull Long currencyId) {
        return CompletableFuture.supplyAsync(() -> {
            List<CurrencyLog> allTransactions = logRepository.findAllByAttributes(Map.of(
                "currency.id", currencyId,
                "logType", ELogType.TRANSACTION,
                "success", true
            ));
            
            List<CurrencyLog> deposits = allTransactions.stream()
                                                        .filter(log -> log.getOperationType() == EChangeType.DEPOSIT)
                                                        .toList();
            
            List<CurrencyLog> withdrawals = allTransactions.stream()
                                                           .filter(log -> log.getOperationType() == EChangeType.WITHDRAW)
                                                           .toList();
            
            long uniqueUsers = allTransactions.stream()
                                              .map(CurrencyLog::getPlayerUuid)
                                              .filter(uuid -> uuid != null)
                                              .distinct()
                                              .count();
            
            return new BasicCurrencyStats(
                currencyId,
                allTransactions.size(),
                deposits.size(),
                withdrawals.size(),
                (int) uniqueUsers
            );
        }, executor);
    }
    
    /**
     * Data class for basic currency statistics.
     */
    public static class BasicCurrencyStats {
        private final Long currencyId;
        private final int totalTransactions;
        private final int totalDeposits;
        private final int totalWithdrawals;
        private final int uniqueUsers;
        
        public BasicCurrencyStats(
            Long currencyId,
            int totalTransactions,
            int totalDeposits,
            int totalWithdrawals,
            int uniqueUsers
        ) {
            this.currencyId = currencyId;
            this.totalTransactions = totalTransactions;
            this.totalDeposits = totalDeposits;
            this.totalWithdrawals = totalWithdrawals;
            this.uniqueUsers = uniqueUsers;
        }
        
        public Long getCurrencyId() { return currencyId; }
        public int getTotalTransactions() { return totalTransactions; }
        public int getTotalDeposits() { return totalDeposits; }
        public int getTotalWithdrawals() { return totalWithdrawals; }
        public int getUniqueUsers() { return uniqueUsers; }
    }
}