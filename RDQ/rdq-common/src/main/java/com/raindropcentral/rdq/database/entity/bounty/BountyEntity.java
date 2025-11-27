package com.raindropcentral.rdq.database.entity.bounty;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a bounty placed on a player.
 *
 * <p>A bounty tracks the placer, target, amount, and lifecycle status.
 * Bounties can be active, claimed, expired, or cancelled.
 *
 * @author JExcellence
 * @since 6.0.0
 */
@Entity
@Table(
    name = "rdq_bounties",
    indexes = {
        @Index(name = "idx_bounty_target", columnList = "target_id"),
        @Index(name = "idx_bounty_placer", columnList = "placer_id"),
        @Index(name = "idx_bounty_status", columnList = "status"),
        @Index(name = "idx_bounty_expires", columnList = "expires_at")
    }
)
public class BountyEntity extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "placer_id", nullable = false)
    private UUID placerId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 32)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BountyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "claimed_by")
    private UUID claimedBy;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected BountyEntity() {
    }

    public BountyEntity(
        @NotNull UUID placerId,
        @NotNull UUID targetId,
        @NotNull BigDecimal amount,
        @NotNull String currency,
        @NotNull BountyStatus status,
        @NotNull Instant createdAt,
        @Nullable Instant expiresAt,
        @Nullable UUID claimedBy,
        @Nullable Instant claimedAt
    ) {
        this.placerId = Objects.requireNonNull(placerId, "placerId");
        this.targetId = Objects.requireNonNull(targetId, "targetId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = expiresAt;
        this.claimedBy = claimedBy;
        this.claimedAt = claimedAt;
    }

    public static BountyEntity create(
        @NotNull UUID placerId,
        @NotNull UUID targetId,
        @NotNull BigDecimal amount,
        @NotNull String currency,
        @Nullable Instant expiresAt
    ) {
        return new BountyEntity(
            placerId,
            targetId,
            amount,
            currency,
            BountyStatus.ACTIVE,
            Instant.now(),
            expiresAt,
            null,
            null
        );
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return status == BountyStatus.ACTIVE && !isExpired();
    }

    public void claim(@NotNull UUID hunterId) {
        if (claimedBy != null) {
            throw new IllegalStateException("Bounty is already claimed");
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot claim expired bounty");
        }
        this.claimedBy = Objects.requireNonNull(hunterId, "hunterId");
        this.claimedAt = Instant.now();
        this.status = BountyStatus.CLAIMED;
    }

    public void expire() {
        if (claimedBy != null) {
            throw new IllegalStateException("Cannot expire a claimed bounty");
        }
        this.status = BountyStatus.EXPIRED;
    }

    public void cancel() {
        this.status = BountyStatus.CANCELLED;
    }

    /**
     * Converts this entity to the Bounty record for API use.
     */
    public com.raindropcentral.rdq.bounty.Bounty toRecord() {
        return new com.raindropcentral.rdq.bounty.Bounty(
            getId(),
            placerId,
            targetId,
            amount,
            currency,
            com.raindropcentral.rdq.bounty.BountyStatus.valueOf(status.name()),
            createdAt,
            expiresAt,
            claimedBy,
            claimedAt
        );
    }

    @NotNull
    public UUID placerId() {
        return placerId;
    }

    @NotNull
    public UUID targetId() {
        return targetId;
    }

    @NotNull
    public BigDecimal amount() {
        return amount;
    }

    @NotNull
    public String currency() {
        return currency;
    }

    @NotNull
    public BountyStatus status() {
        return status;
    }

    @NotNull
    public Instant createdAt() {
        return createdAt;
    }

    @Nullable
    public Instant expiresAt() {
        return expiresAt;
    }

    @Nullable
    public UUID claimedBy() {
        return claimedBy;
    }

    @Nullable
    public Instant claimedAt() {
        return claimedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BountyEntity bounty)) return false;
        return Objects.equals(getId(), bounty.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "BountyEntity[id=" + getId() + ", target=" + targetId + ", amount=" + amount + ", status=" + status + "]";
    }
}
