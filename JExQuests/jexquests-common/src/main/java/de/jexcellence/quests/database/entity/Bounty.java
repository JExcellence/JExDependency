package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A player-placed bounty. {@code amount} + {@code currency} describe
 * the payout; the currency string maps to a Vault-registered currency
 * (or JExEconomy currency identifier when JExEconomy is installed).
 */
@Entity
@Table(
        name = "jexquests_bounty",
        indexes = {
                @Index(name = "idx_jexquests_bounty_target", columnList = "target_uuid"),
                @Index(name = "idx_jexquests_bounty_issuer", columnList = "issuer_uuid"),
                @Index(name = "idx_jexquests_bounty_status", columnList = "status")
        }
)
public class Bounty extends LongIdEntity {

    @Column(name = "target_uuid", nullable = false)
    private UUID targetUuid;

    @Column(name = "issuer_uuid", nullable = false)
    private UUID issuerUuid;

    @Column(name = "currency", nullable = false, length = 32)
    private String currency;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BountyStatus status = BountyStatus.ACTIVE;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "note", length = 256)
    private String note;

    protected Bounty() {
    }

    public Bounty(
            @NotNull UUID targetUuid,
            @NotNull UUID issuerUuid,
            @NotNull String currency,
            double amount
    ) {
        this.targetUuid = targetUuid;
        this.issuerUuid = issuerUuid;
        this.currency = currency;
        this.amount = amount;
        this.placedAt = LocalDateTime.now();
    }

    public @NotNull UUID getTargetUuid() { return this.targetUuid; }
    public @NotNull UUID getIssuerUuid() { return this.issuerUuid; }
    public @NotNull String getCurrency() { return this.currency; }
    public double getAmount() { return this.amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public @NotNull BountyStatus getStatus() { return this.status; }
    public void setStatus(@NotNull BountyStatus status) { this.status = status; }
    public @NotNull LocalDateTime getPlacedAt() { return this.placedAt; }
    public @Nullable LocalDateTime getExpiresAt() { return this.expiresAt; }
    public void setExpiresAt(@Nullable LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public @Nullable LocalDateTime getResolvedAt() { return this.resolvedAt; }
    public void setResolvedAt(@Nullable LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public @Nullable String getNote() { return this.note; }
    public void setNote(@Nullable String note) { this.note = note; }

    @Override
    public String toString() {
        return "Bounty[" + this.targetUuid + " by " + this.issuerUuid + " " + this.amount + " " + this.currency + " " + this.status + "]";
    }
}
