package com.raindropcentral.rdq.database.entity.player;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class BountyStatistics {

    @Column(name = "bounties_collected", nullable = false)
    private int bountiesCollected = 0;

    @Column(name = "biggest_bounty", nullable = false)
    private double biggestBounty = 0.0;

    @Column(name = "total_bounty_value_collected", nullable = false)
    private double totalBountyValueCollected = 0.0;

    @Column(name = "last_bounty_claimed_at")
    private LocalDateTime lastBountyClaimedAt;

    public BountyStatistics() {}

    public int getBountiesCollected() { return bountiesCollected; }
    public double getBiggestBounty() { return biggestBounty; }
    public double getTotalBountyValueCollected() { return totalBountyValueCollected; }
    public LocalDateTime getLastBountyClaimedAt() { return lastBountyClaimedAt; }

    public void recordBountyClaim(double bountyValue) {
        this.bountiesCollected++;
        this.totalBountyValueCollected += bountyValue;
        if (bountyValue > this.biggestBounty) {
            this.biggestBounty = bountyValue;
        }
        this.lastBountyClaimedAt = LocalDateTime.now();
    }
}
