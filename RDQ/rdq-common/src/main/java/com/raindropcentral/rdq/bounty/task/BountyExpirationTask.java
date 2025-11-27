package com.raindropcentral.rdq.bounty.task;

import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.announcement.BountyAnnouncementService;
import com.raindropcentral.rdq.bounty.economy.EconomyService;
import com.raindropcentral.rdq.bounty.repository.BountyRepository;
import com.raindropcentral.rdq.bounty.service.DefaultFreeBountyService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BountyExpirationTask extends BukkitRunnable {

    private static final Logger LOGGER = Logger.getLogger(BountyExpirationTask.class.getName());
    private static final long CHECK_INTERVAL_TICKS = 20L * 60 * 5;

    private final BountyRepository bountyRepository;
    private final EconomyService economyService;
    private final BountyAnnouncementService announcementService;
    private final DefaultFreeBountyService bountyService;

    public BountyExpirationTask(
        @NotNull BountyRepository bountyRepository,
        @NotNull EconomyService economyService,
        @NotNull BountyAnnouncementService announcementService,
        @NotNull DefaultFreeBountyService bountyService
    ) {
        this.bountyRepository = bountyRepository;
        this.economyService = economyService;
        this.announcementService = announcementService;
        this.bountyService = bountyService;
    }

    public void start(@NotNull JavaPlugin plugin) {
        runTaskTimerAsynchronously(plugin, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
        LOGGER.info("Bounty expiration task started (interval: 5 minutes)");
    }

    @Override
    public void run() {
        processExpiredBounties();
    }


    private void processExpiredBounties() {
        bountyRepository.findExpiredAsync()
            .thenCompose(expired -> {
                if (expired.isEmpty()) {
                    return CompletableFuture.completedFuture(0);
                }

                LOGGER.info("Processing " + expired.size() + " expired bounties");

                var processFutures = expired.stream()
                    .map(bounty -> bountyRepository.findEntityByIdAsync(bounty.id())
                        .thenCompose(opt -> {
                            if (opt.isEmpty()) return CompletableFuture.completedFuture((Void) null);
                            var entity = opt.get();
                            entity.expire();
                            return bountyRepository.updateBountyAsync(entity)
                                .thenCompose(saved -> processExpiredBounty(bounty));
                        }))
                    .toList();

                return CompletableFuture.allOf(processFutures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> expired.size());
            })
            .thenAccept(count -> {
                if (count > 0) {
                    LOGGER.info("Expired " + count + " bounties");
                }
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to process expired bounties", ex);
                return null;
            });
    }

    private CompletableFuture<Void> processExpiredBounty(@NotNull Bounty bounty) {
        bountyService.invalidateCache(bounty.targetId());

        return economyService.deposit(bounty.placerId(), bounty.amount(), bounty.currency())
            .thenAccept(success -> {
                if (success) {
                    announcementService.announceExpiration(bounty);
                    LOGGER.fine("Refunded " + bounty.amount() + " to " + bounty.placerId() + " for expired bounty " + bounty.id());
                } else {
                    LOGGER.warning("Failed to refund " + bounty.amount() + " to " + bounty.placerId() + " for expired bounty " + bounty.id());
                }
            });
    }

    public void stop() {
        if (!isCancelled()) {
            cancel();
            LOGGER.info("Bounty expiration task stopped");
        }
    }
}
