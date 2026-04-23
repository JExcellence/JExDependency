package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit row for a resolved {@link Bounty}. Separate table so the
 * {@code bounty} row can be retired while the kill history remains
 * queryable.
 */
@Entity
@Table(
        name = "jexquests_bounty_claim",
        indexes = {
                @Index(name = "idx_jexquests_bounty_claim_bounty", columnList = "bounty_id"),
                @Index(name = "idx_jexquests_bounty_claim_killer", columnList = "killer_uuid")
        }
)
public class BountyClaim extends LongIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bounty_id", nullable = false)
    private Bounty bounty;

    @Column(name = "killer_uuid", nullable = false)
    private UUID killerUuid;

    @Column(name = "payout_amount", nullable = false)
    private double payoutAmount;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    protected BountyClaim() {
    }

    public BountyClaim(@NotNull Bounty bounty, @NotNull UUID killerUuid, double payoutAmount) {
        this.bounty = bounty;
        this.killerUuid = killerUuid;
        this.payoutAmount = payoutAmount;
        this.claimedAt = LocalDateTime.now();
    }

    public @NotNull Bounty getBounty() { return this.bounty; }
    public @NotNull UUID getKillerUuid() { return this.killerUuid; }
    public double getPayoutAmount() { return this.payoutAmount; }
    public @NotNull LocalDateTime getClaimedAt() { return this.claimedAt; }

    @Override
    public String toString() {
        return "BountyClaim[bounty=" + (this.bounty != null ? this.bounty.getId() : null)
                + " by " + this.killerUuid + " +" + this.payoutAmount + "]";
    }
}
