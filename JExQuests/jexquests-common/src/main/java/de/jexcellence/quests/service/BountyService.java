package de.jexcellence.quests.service;

import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.core.api.reward.RewardContext;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.api.BountySnapshot;
import de.jexcellence.quests.api.event.BountyClaimedEvent;
import de.jexcellence.quests.database.entity.Bounty;
import de.jexcellence.quests.database.entity.BountyClaim;
import de.jexcellence.quests.database.entity.BountyStatus;
import de.jexcellence.quests.database.repository.BountyClaimRepository;
import de.jexcellence.quests.database.repository.BountyRepository;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Bounty lifecycle: place, claim (on kill), cancel (issuer refund).
 * Currency payouts go through {@link RewardExecutor#grantSync} with a
 * {@link Reward.Currency} — one abstraction for every plugin.
 */
public class BountyService {

    private static final String SOURCE = "JExQuests";

    private final BountyRepository bounties;
    private final BountyClaimRepository claims;
    private final JExLogger logger;

    public BountyService(
            @NotNull BountyRepository bounties,
            @NotNull BountyClaimRepository claims,
            @NotNull JExLogger logger
    ) {
        this.bounties = bounties;
        this.claims = claims;
        this.logger = logger;
    }

    /** Place a bounty. Returns the persisted row. */
    public @NotNull CompletableFuture<Bounty> placeAsync(
            @NotNull UUID target, @NotNull UUID issuer, @NotNull String currency, double amount
    ) {
        final Bounty bounty = new Bounty(target, issuer, currency, amount);
        return CompletableFuture.supplyAsync(() -> this.bounties.create(bounty)).exceptionally(ex -> {
            this.logger.error("bounty place failed: {}", ex.getMessage());
            return null;
        });
    }

    /** Mark the active bounty on {@code target} as claimed by {@code killer}. Grants the currency reward. */
    public @NotNull CompletableFuture<ClaimResult> claimAsync(@NotNull UUID target, @NotNull UUID killer) {
        return this.bounties.findActiveByTargetAsync(target).thenApply(opt -> {
            if (opt.isEmpty()) return ClaimResult.NOT_FOUND;
            final Bounty bounty = opt.get();
            bounty.setStatus(BountyStatus.CLAIMED);
            bounty.setResolvedAt(LocalDateTime.now());
            this.bounties.update(bounty);

            this.claims.create(new BountyClaim(bounty, killer, bounty.getAmount()));

            final RewardExecutor executor = RewardExecutor.get();
            if (executor != null) {
                executor.grantSync(
                        new Reward.Currency(bounty.getCurrency(), bounty.getAmount()),
                        new RewardContext(killer, SOURCE, "bounty-claim")
                );
            }

            de.jexcellence.quests.util.EventDispatch.fire(new BountyClaimedEvent(
                    new BountySnapshot(
                            bounty.getId() != null ? bounty.getId() : 0L,
                            bounty.getTargetUuid(),
                            bounty.getIssuerUuid(),
                            bounty.getCurrency(),
                            bounty.getAmount(),
                            bounty.getPlacedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                    ),
                    killer
            ));
            return ClaimResult.CLAIMED;
        }).exceptionally(ex -> {
            this.logger.error("claim failed: {}", ex.getMessage());
            return ClaimResult.ERROR;
        });
    }

    /** Cancel an active bounty. Refunds the issuer. */
    public @NotNull CompletableFuture<Boolean> cancelAsync(@NotNull UUID target) {
        return this.bounties.findActiveByTargetAsync(target).thenApply(opt -> {
            if (opt.isEmpty()) return false;
            final Bounty bounty = opt.get();
            bounty.setStatus(BountyStatus.CANCELLED);
            bounty.setResolvedAt(LocalDateTime.now());
            this.bounties.update(bounty);

            final RewardExecutor executor = RewardExecutor.get();
            if (executor != null) {
                executor.grantSync(
                        new Reward.Currency(bounty.getCurrency(), bounty.getAmount()),
                        new RewardContext(bounty.getIssuerUuid(), SOURCE, "bounty-refund")
                );
            }
            return true;
        }).exceptionally(ex -> {
            this.logger.error("cancel failed: {}", ex.getMessage());
            return false;
        });
    }

    public @NotNull CompletableFuture<List<Bounty>> activeAsync() {
        return this.bounties.findActiveAsync().exceptionally(ex -> {
            this.logger.error("active listing failed: {}", ex.getMessage());
            return List.of();
        });
    }

    public enum ClaimResult { CLAIMED, NOT_FOUND, ERROR }
}
