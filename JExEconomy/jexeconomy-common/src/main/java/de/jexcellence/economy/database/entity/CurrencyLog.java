package de.jexcellence.economy.database.entity;

import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a log entry for currency-related operations and events.
 * <p>
 * This entity provides comprehensive logging capabilities for all currency operations,
 * including balance changes, currency management, system events, and error tracking.
 * It serves as an audit trail and analytics foundation for the currency system.
 * </p>
 *
 * <h3>Log Categories:</h3>
 * <ul>
 *   <li><strong>Transaction Logs:</strong> Balance changes, deposits, withdrawals, transfers</li>
 *   <li><strong>Management Logs:</strong> Currency creation, deletion, configuration changes</li>
 *   <li><strong>System Logs:</strong> Plugin events, cache operations, database operations</li>
 *   <li><strong>Error Logs:</strong> Failed operations, validation errors, system exceptions</li>
 *   <li><strong>Audit Logs:</strong> Administrative actions, permission checks, security events</li>
 * </ul>
 *
 * <h3>Database Mapping:</h3>
 * <p>
 * This entity is mapped to the {@code p_currency_log} table with appropriate
 * indexes for efficient querying by timestamp, player, currency, and log type.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
@Table(
    name = "p_currency_log",
    indexes = {
        @Index(name = "idx_currency_log_timestamp", columnList = "log_timestamp"),
        @Index(name = "idx_currency_log_player", columnList = "player_uuid"),
        @Index(name = "idx_currency_log_currency", columnList = "currency_id"),
        @Index(name = "idx_currency_log_type", columnList = "log_type"),
        @Index(name = "idx_currency_log_level", columnList = "log_level"),
        @Index(name = "idx_currency_log_operation", columnList = "operation_type")
    }
)
@Entity
public class CurrencyLog extends BaseEntity {
    
    /**
     * The timestamp when this log entry was created.
     */
    @Column(
        name = "log_timestamp",
        nullable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime timestamp;
    
    /**
     * The type of log entry (TRANSACTION, MANAGEMENT, SYSTEM, ERROR, AUDIT).
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name = "log_type",
        nullable = false,
        length = 20
    )
    private ELogType logType;
    
    /**
     * The severity level of this log entry (INFO, WARNING, ERROR, DEBUG).
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name = "log_level",
        nullable = false,
        length = 10
    )
    private ELogLevel logLevel;
    
    /**
     * The UUID of the player involved in this operation (null for system operations).
     */
    @Column(
        name = "player_uuid"
    )
    private UUID playerUuid;
    
    /**
     * The name of the player at the time of the operation (for easier reading).
     */
    @Column(
        name = "player_name",
        length = 16
    )
    private String playerName;
    
    /**
     * The UUID of the player who initiated this operation (null if same as affected player).
     */
    @Column(
        name = "initiator_uuid"
    )
    private UUID initiatorUuid;
    
    /**
     * The name of the initiator at the time of the operation.
     */
    @Column(
        name = "initiator_name",
        length = 16
    )
    private String initiatorName;
    
    /**
     * The currency involved in this operation (null for non-currency-specific logs).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "currency_id"
    )
    private Currency currency;
    
    /**
     * The type of operation performed (DEPOSIT, WITHDRAW, SET, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(
        name = "operation_type",
        length = 20
    )
    private EChangeType operationType;
    
    /**
     * The balance before the operation (null for non-balance operations).
     */
    @Column(
        name = "old_balance",
        columnDefinition = "DECIMAL(19,2) DEFAULT 0.00"
    )
    private Double oldBalance;
    
    /**
     * The balance after the operation (null for non-balance operations).
     */
    @Column(
        name = "new_balance",
        columnDefinition = "DECIMAL(19,2) DEFAULT 0.00"
    )
    private Double newBalance;
    
    /**
     * The amount involved in the operation (null for non-amount operations).
     */
    @Column(
        name = "amount",
        columnDefinition = "DECIMAL(19,2) DEFAULT 0.00"
    )
    private Double amount;
    
    /**
     * A brief description of the operation or event.
     */
    @Column(
        name = "operation_description",
        nullable = false
    )
    private String description;
    
    /**
     * Detailed information about the operation (JSON, stack trace, etc.).
     */
    @Column(
        name = "details",
        columnDefinition = "TEXT"
    )
    private String details;
    
    /**
     * The reason provided for the operation (if any).
     */
    @Column(
        name = "reason"
    )
    private String reason;
    
    /**
     * Whether the operation was successful.
     */
    @Column(
        name = "success",
        nullable = false
    )
    private boolean success;
    
    /**
     * Error message if the operation failed.
     */
    @Column(
        name = "error_message",
        length = 500
    )
    private String errorMessage;
    
    /**
     * The IP address of the player (if available and relevant).
     */
    @Column(
        name = "ip_address",
        length = 45
    )
    private String ipAddress;
    
    /**
     * Additional metadata as JSON string.
     */
    @Column(
        name = "metadata",
        columnDefinition = "TEXT"
    )
    private String metadata;
    
    /**
     * Protected no-args constructor for JPA/Hibernate.
     */
    protected CurrencyLog() {
        this.timestamp = LocalDateTime.now();
        this.success = true; // Default value handled in constructor
    }
    
    /**
     * Creates a new currency log entry.
     *
     * @param logType the type of log entry
     * @param logLevel the severity level
     * @param description brief description of the operation
     */
    public CurrencyLog(
        @NotNull ELogType logType,
        @NotNull ELogLevel logLevel,
        @NotNull String description
    ) {
        this();
        this.logType = logType;
        this.logLevel = logLevel;
        this.description = description;
        this.success = true;
    }
    
    /**
     * Creates a new transaction log entry.
     *
     * @param playerUuid the UUID of the affected player
     * @param playerName the name of the affected player
     * @param currency the currency involved
     * @param operationType the type of operation
     * @param oldBalance the balance before the operation
     * @param newBalance the balance after the operation
     * @param amount the amount involved
     * @param description brief description
     * @param reason the reason for the operation
     */
    public CurrencyLog(
        @NotNull UUID playerUuid,
        @NotNull String playerName,
        @NotNull Currency currency,
        @NotNull EChangeType operationType,
        double oldBalance,
        double newBalance,
        double amount,
        @NotNull String description,
        @Nullable String reason
    ) {
        this(ELogType.TRANSACTION, ELogLevel.INFO, description);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.currency = currency;
        this.operationType = operationType;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.amount = amount;
        this.reason = reason;
    }
    
    @NotNull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(@NotNull LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @NotNull
    public ELogType getLogType() {
        return logType;
    }
    
    public void setLogType(@NotNull ELogType logType) {
        this.logType = logType;
    }
    
    @NotNull
    public ELogLevel getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(@NotNull ELogLevel logLevel) {
        this.logLevel = logLevel;
    }
    
    @Nullable
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(@Nullable UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    @Nullable
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(@Nullable String playerName) {
        this.playerName = playerName;
    }
    
    @Nullable
    public UUID getInitiatorUuid() {
        return initiatorUuid;
    }
    
    public void setInitiatorUuid(@Nullable UUID initiatorUuid) {
        this.initiatorUuid = initiatorUuid;
    }
    
    @Nullable
    public String getInitiatorName() {
        return initiatorName;
    }
    
    public void setInitiatorName(@Nullable String initiatorName) {
        this.initiatorName = initiatorName;
    }
    
    @Nullable
    public Currency getCurrency() {
        return currency;
    }
    
    public void setCurrency(@Nullable Currency currency) {
        this.currency = currency;
    }
    
    @Nullable
    public EChangeType getOperationType() {
        return operationType;
    }
    
    public void setOperationType(@Nullable EChangeType operationType) {
        this.operationType = operationType;
    }
    
    @Nullable
    public Double getOldBalance() {
        return oldBalance;
    }
    
    public void setOldBalance(@Nullable Double oldBalance) {
        this.oldBalance = oldBalance;
    }
    
    @Nullable
    public Double getNewBalance() {
        return newBalance;
    }
    
    public void setNewBalance(@Nullable Double newBalance) {
        this.newBalance = newBalance;
    }
    
    @Nullable
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(@Nullable Double amount) {
        this.amount = amount;
    }
    
    @NotNull
    public String getDescription() {
        return description;
    }
    
    public void setDescription(@NotNull String description) {
        this.description = description;
    }
    
    @Nullable
    public String getDetails() {
        return details;
    }
    
    public void setDetails(@Nullable String details) {
        this.details = details;
    }
    
    @Nullable
    public String getReason() {
        return reason;
    }
    
    public void setReason(@Nullable String reason) {
        this.reason = reason;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            this.success = false;
        }
    }
    
    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    @Nullable
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(@Nullable String metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Calculates the change amount for balance operations.
     *
     * @return the change amount, or null if not a balance operation
     */
    @Nullable
    public Double getChangeAmount() {
        if (oldBalance != null && newBalance != null) {
            return newBalance - oldBalance;
        }
        return null;
    }
    
    /**
     * Checks if this log entry represents a successful operation.
     *
     * @return true if the operation was successful
     */
    public boolean wasSuccessful() {
        return success && errorMessage == null;
    }
    
    /**
     * Checks if this log entry involves a specific player.
     *
     * @param uuid the player UUID to check
     * @return true if this log involves the specified player
     */
    public boolean involvesPlayer(@NotNull UUID uuid) {
        return Objects.equals(playerUuid, uuid) || Objects.equals(initiatorUuid, uuid);
    }
    
    /**
     * Checks if this log entry involves a specific currency.
     *
     * @param currencyId the currency ID to check
     * @return true if this log involves the specified currency
     */
    public boolean involvesCurrency(@NotNull Long currencyId) {
        return currency != null && Objects.equals(currency.getId(), currencyId);
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CurrencyLog that = (CurrencyLog) obj;
        return Objects.equals(getId(), that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    @Override
    public @NotNull String toString() {
        return String.format(
            "CurrencyLog{id=%d, timestamp=%s, type=%s, level=%s, player=%s, currency=%s, operation=%s, description='%s', success=%s}",
            getId(),
            timestamp,
            logType,
            logLevel,
            playerName != null ? playerName : "SYSTEM",
            currency != null ? currency.getIdentifier() : "N/A",
            operationType != null ? operationType : "N/A",
            description,
            success
        );
    }
}