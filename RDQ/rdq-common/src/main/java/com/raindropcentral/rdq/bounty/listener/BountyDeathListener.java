package com.raindropcentral.rdq.bounty.listener;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.api.BountyService;
import com.raindropcentral.rdq.bounty.announcement.BountyAnnouncementService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class BountyDeathListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(BountyDeathListener.class.getName());

    private final @Nullable RDQCore core;
    private final @Nullable BountyService bountyService;
    private final @Nullable DamageTracker damageTracker;
    private final @Nullable BountyAnnouncementService announcementService;

    /**
     * Constructor for CommandFactory auto-registration.
     * Stores RDQCore reference for lazy service access.
     */
    public BountyDeathListener(@NotNull RDQCore core) {
        this.core = core;
        this.bountyService = null;
        this.damageTracker = null;
        this.announcementService = null;
    }

    public BountyDeathListener(
        @NotNull BountyService bountyService,
        @NotNull DamageTracker damageTracker,
        @NotNull BountyAnnouncementService announcementService
    ) {
        this.core = null;
        this.bountyService = bountyService;
        this.damageTracker = damageTracker;
        this.announcementService = announcementService;
    }

    private @Nullable BountyService getBountyService() {
        if (bountyService != null) return bountyService;
        if (core != null) {
            try {
                return core.getBountyService();
            } catch (IllegalStateException e) {
                return null;
            }
        }
        return null;
    }

    private @Nullable DamageTracker getDamageTracker() {
        if (damageTracker != null) return damageTracker;
        if (core != null) return core.getDamageTracker();
        return null;
    }

    private @Nullable BountyAnnouncementService getAnnouncementService() {
        if (announcementService != null) return announcementService;
        if (core != null) return core.getBountyAnnouncementService();
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        var service = getBountyService();
        var tracker = getDamageTracker();
        if (service == null || tracker == null) return;

        var victim = event.getEntity();
        var killer = victim.getKiller();

        if (killer != null) {
            handleBountyClaim(killer, victim, service, tracker);
            return;
        }

        tracker.getLastAttacker(victim.getUniqueId())
            .ifPresent(record -> {
                var attacker = victim.getServer().getPlayer(record.attackerId());
                if (attacker != null && attacker.isOnline()) {
                    handleBountyClaim(attacker, victim, service, tracker);
                }
            });
    }


    private void handleBountyClaim(@NotNull Player hunter, @NotNull Player target,
                                   @NotNull BountyService service, @NotNull DamageTracker tracker) {
        if (hunter.equals(target)) {
            return;
        }

        var announcement = getAnnouncementService();
        service.getBountiesOnPlayer(target.getUniqueId())
            .thenCompose(bounties -> {
                if (bounties.isEmpty()) {
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }

                return service.claimBounty(hunter.getUniqueId(), target.getUniqueId())
                    .thenAccept(result -> {
                        if (announcement != null) {
                            announcement.announceClaim(hunter, target, result);
                        }
                        tracker.clearRecord(target.getUniqueId());
                        LOGGER.info(hunter.getName() + " claimed bounty on " + target.getName() + " for " + result.reward());
                    });
            })
            .exceptionally(ex -> {
                LOGGER.log(Level.WARNING, "Failed to process bounty claim for " + hunter.getName() + " killing " + target.getName(), ex);
                return null;
            });
    }
}
