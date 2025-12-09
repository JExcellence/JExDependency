/*
package com.raindropcentral.rdq2.perk.listener;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.perk.effect.DeathPreventionHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class DeathPreventionListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(DeathPreventionListener.class.getName());

    private final DeathPreventionHandler handler;
    private final BiConsumer<org.bukkit.entity.Player, DeathPreventionHandler.DeathPreventionResult> notificationCallback;

    */
/**
     * Constructor for CommandFactory auto-registration.
     *//*

    public DeathPreventionListener(@NotNull RDQ core) {
        this.handler = new DeathPreventionHandler(core.getPerkRegistry());
        this.notificationCallback = (player, result) -> {
            // TODO: Implement when translation service is available
            // core.getTranslationService().sendMessage(player, "perk.death_prevented",
            //     java.util.Map.of("perk", result.perkNameKey(), "health", result.healthRestored()));
            player.sendMessage("§aDeath prevented by perk!");
        };
    }

    public DeathPreventionListener(
        @NotNull DeathPreventionHandler handler,
        @Nullable BiConsumer<org.bukkit.entity.Player, DeathPreventionHandler.DeathPreventionResult> notificationCallback
    ) {
        this.handler = handler;
        this.notificationCallback = notificationCallback;
    }

    public DeathPreventionListener(@NotNull DeathPreventionHandler handler) {
        this(handler, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) return;

        var finalDamage = event.getFinalDamage();
        var currentHealth = player.getHealth();

        if (finalDamage < currentHealth) return;

        if (!handler.hasActiveDeathPrevention(player.getUniqueId())) return;

        var result = handler.tryPreventDeath(player);
        if (result.isEmpty()) return;

        event.setCancelled(true);

        var prevention = result.get();
        LOGGER.info(() -> "Death prevented for " + player.getName() + " by perk " + prevention.perkId());

        if (notificationCallback != null) {
            notificationCallback.accept(player, prevention);
        }
    }
}
*/
