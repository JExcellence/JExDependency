/*
package com.raindropcentral.rdq2.perk.listener;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.perk.effect.ExperienceMultiplierHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class ExperienceMultiplierListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(ExperienceMultiplierListener.class.getName());

    private final ExperienceMultiplierHandler handler;

    */
/**
     * Constructor for CommandFactory auto-registration.
     *//*

    public ExperienceMultiplierListener(@NotNull RDQ core) {
        this.handler = new ExperienceMultiplierHandler(core.getPerkRegistry());
    }

    public ExperienceMultiplierListener(@NotNull ExperienceMultiplierHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExperienceChange(@NotNull PlayerExpChangeEvent event) {
        var player = event.getPlayer();
        var originalXp = event.getAmount();

        if (originalXp <= 0) return;

        if (!handler.hasActiveMultiplier(player.getUniqueId())) return;

        var multipliedXp = handler.applyMultiplier(player, originalXp);
        event.setAmount(multipliedXp);

        LOGGER.fine(() -> "Applied XP multiplier for " + player.getName() + ": " + originalXp + " -> " + multipliedXp);
    }
}
*/
