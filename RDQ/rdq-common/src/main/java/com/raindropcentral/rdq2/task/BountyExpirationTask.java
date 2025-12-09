/*
package com.raindropcentral.rdq2.task;

import com.raindropcentral.rdq2.service.bounty.BountyServiceProvider;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public final class BountyExpirationTask extends BukkitRunnable {

    @Override
    public void run() {
        var service = BountyServiceProvider.getInstance();
        if (service == null) return;

        service.getAllBounties(0, 1000).thenAccept(bounties -> {
            bounties.stream()
                    .filter(bounty -> bounty.isActive() && bounty.isExpired())
                    .forEach(bounty -> {
                        bounty.expire();
                        service.updateBounty(bounty).thenAccept(updated -> {
                            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("RDQ"), () -> {
                                var target = Bukkit.getOfflinePlayer(bounty.getTargetUniqueId());
                                var commissioner = Bukkit.getOfflinePlayer(bounty.getCommissionerUniqueId());
                                
                                Bukkit.broadcastMessage("§c§l[BOUNTY] §7The bounty on §c" + 
                                        (target.getName() != null ? target.getName() : "Unknown") + 
                                        " §7has expired!");
                                
                                if (commissioner.isOnline()) {
                                    commissioner.getPlayer().sendMessage("§eYour bounty on " + 
                                            (target.getName() != null ? target.getName() : "Unknown") + 
                                            " has expired.");
                                }
                            });
                        });
                    });
        });
    }
}
*/
