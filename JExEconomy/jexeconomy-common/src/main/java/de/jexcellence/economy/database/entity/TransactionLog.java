package de.jexcellence.economy.database.entity;

import de.jexcellence.economy.api.ChangeType;
import de.jexcellence.economy.type.LogLevel;
import de.jexcellence.economy.type.LogType;
import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit record for a balance change or economy operation.
 *
 * @author JExcellence
 * @since 3.0.0
 */
@Entity
@Table(name = "economy_transaction_log", indexes = {
        @Index(name = "idx_txlog_timestamp", columnList = "timestamp"),
        @Index(name = "idx_txlog_player", columnList = "player_uuid"),
        @Index(name = "idx_txlog_currency", columnList = "currency_id"),
        @Index(name = "idx_txlog_type", columnList = "change_type")
})
public class TransactionLog extends LongIdEntity {

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "player_uuid")
    private UUID playerUuid;

    @Column(name = "player_name", length = 16)
    private String playerName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 16)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 16)
    private LogType logType;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 16)
    private LogLevel logLevel;

    @Column(name = "old_balance")
    private double oldBalance;

    @Column(name = "new_balance")
    private double newBalance;

    @Column(name = "amount")
    private double amount;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "initiator_uuid")
    private UUID initiatorUuid;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    /** JPA constructor. */
    protected TransactionLog() {
    }

    /**
     * Creates a transaction log entry.
     *
     * @param playerUuid    the player's UUID
     * @param playerName    the player's name
     * @param currency      the currency involved
     * @param changeType    the type of change
     * @param oldBalance    balance before
     * @param newBalance    balance after
     * @param amount        the amount changed
     * @param reason        optional reason
     * @param initiatorUuid optional initiator UUID
     * @param success       whether the operation succeeded
     * @param errorMessage  optional error message
     */
    @SuppressWarnings("ParameterNumber")
    public TransactionLog(@Nullable UUID playerUuid,
                          @Nullable String playerName,
                          @Nullable Currency currency,
                          @NotNull ChangeType changeType,
                          double oldBalance,
                          double newBalance,
                          double amount,
                          @Nullable String reason,
                          @Nullable UUID initiatorUuid,
                          boolean success,
                          @Nullable String errorMessage) {
        this.timestamp = Instant.now();
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.currency = currency;
        this.changeType = changeType;
        this.logType = LogType.TRANSACTION;
        this.logLevel = success ? LogLevel.INFO : LogLevel.ERROR;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.amount = amount;
        this.reason = reason;
        this.initiatorUuid = initiatorUuid;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    // ── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the timestamp.
     *
     * @return the timestamp
     */
    public @NotNull Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(@NotNull Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the player UUID.
     *
     * @return the player UUID, or null
     */
    public @Nullable UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Sets the player UUID.
     *
     * @param playerUuid the player UUID
     */
    public void setPlayerUuid(@Nullable UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    /**
     * Returns the player name.
     *
     * @return the player name, or null
     */
    public @Nullable String getPlayerName() {
        return playerName;
    }

    /**
     * Sets the player name.
     *
     * @param playerName the player name
     */
    public void setPlayerName(@Nullable String playerName) {
        this.playerName = playerName;
    }

    /**
     * Returns the currency.
     *
     * @return the currency, or null
     */
    public @Nullable Currency getCurrency() {
        return currency;
    }

    /**
     * Sets the currency.
     *
     * @param currency the currency
     */
    public void setCurrency(@Nullable Currency currency) {
        this.currency = currency;
    }

    /**
     * Returns the change type.
     *
     * @return the change type
     */
    public @NotNull ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Sets the change type.
     *
     * @param changeType the change type
     */
    public void setChangeType(@NotNull ChangeType changeType) {
        this.changeType = changeType;
    }

    /**
     * Returns the log type.
     *
     * @return the log type
     */
    public @NotNull LogType getLogType() {
        return logType;
    }

    /**
     * Sets the log type.
     *
     * @param logType the log type
     */
    public void setLogType(@NotNull LogType logType) {
        this.logType = logType;
    }

    /**
     * Returns the log level.
     *
     * @return the log level
     */
    public @NotNull LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the log level.
     *
     * @param logLevel the log level
     */
    public void setLogLevel(@NotNull LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Returns the balance before the operation.
     *
     * @return the old balance
     */
    public double getOldBalance() {
        return oldBalance;
    }

    /**
     * Sets the old balance.
     *
     * @param oldBalance the old balance
     */
    public void setOldBalance(double oldBalance) {
        this.oldBalance = oldBalance;
    }

    /**
     * Returns the balance after the operation.
     *
     * @return the new balance
     */
    public double getNewBalance() {
        return newBalance;
    }

    /**
     * Sets the new balance.
     *
     * @param newBalance the new balance
     */
    public void setNewBalance(double newBalance) {
        this.newBalance = newBalance;
    }

    /**
     * Returns the amount involved.
     *
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount the amount
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Returns the reason for the operation.
     *
     * @return the reason, or null
     */
    public @Nullable String getReason() {
        return reason;
    }

    /**
     * Sets the reason.
     *
     * @param reason the reason
     */
    public void setReason(@Nullable String reason) {
        this.reason = reason;
    }

    /**
     * Returns the initiator's UUID.
     *
     * @return the initiator UUID, or null
     */
    public @Nullable UUID getInitiatorUuid() {
        return initiatorUuid;
    }

    /**
     * Sets the initiator UUID.
     *
     * @param initiatorUuid the initiator UUID
     */
    public void setInitiatorUuid(@Nullable UUID initiatorUuid) {
        this.initiatorUuid = initiatorUuid;
    }

    /**
     * Returns whether the operation succeeded.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success flag.
     *
     * @param success the success flag
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Returns the error message, if any.
     *
     * @return the error message, or null
     */
    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage the error message
     */
    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the net balance change (new - old).
     *
     * @return the change amount
     */
    public double getChangeAmount() {
        return newBalance - oldBalance;
    }

    @Override
    public String toString() {
        return "TransactionLog[" + changeType + " " + amount + " " + playerName + "]";
    }
}
