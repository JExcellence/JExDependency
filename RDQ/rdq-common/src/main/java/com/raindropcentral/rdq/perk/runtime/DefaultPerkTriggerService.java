package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of PerkTriggerService.
 *
 * @author JExcellence
 * @version 1.0.2
 * @since TBD
 */
public class DefaultPerkTriggerService implements PerkTriggerService, Listener {

    private static final Logger LOGGER = CentralLogger.getLogger(DefaultPerkTriggerService.class.getName());

    private final RDQ rdq;
    private final DefaultPerkRegistry perkRegistry;

    public DefaultPerkTriggerService(@NotNull RDQ rdq, @NotNull DefaultPerkRegistry perkRegistry) {
        this.rdq = rdq;
        this.perkRegistry = perkRegistry;
    }

    @Override
    public void handleEvent(@NotNull Event event, @NotNull Player player) {
        final String eventName = event.getClass().getSimpleName();
        for (PerkRuntime runtime : perkRegistry.getAllPerkRuntimes()) {
            if (!runtime.getType().isEventBased()) {
                continue;
            }
            if (!runtime.supports(event)) {
                LOGGER.log(Level.FINEST, "Skipping perk {0} for event {1} due to unsupported trigger", new Object[]{runtime.getId(), eventName});
                continue;
            }
            if (!runtime.canActivate(player) && !runtime.isActive(player)) {
                LOGGER.log(Level.FINEST, "Perk {0} not eligible for player {1} on event {2}", new Object[]{runtime.getId(), player.getUniqueId(), eventName});
                continue;
            }
            if (runtime.isOnCooldown(player)) {
                LOGGER.log(Level.FINER, "Perk {0} on cooldown for player {1}; skipping trigger", new Object[]{runtime.getId(), player.getUniqueId()});
                continue;
            }
            try {
                runtime.trigger(player);
                LOGGER.log(Level.INFO, "Triggered perk {0} for player {1} via event {2}", new Object[]{runtime.getId(), player.getUniqueId(), eventName});
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to trigger perk {0} for player {1} via event {2}", new Object[]{runtime.getId(), player.getUniqueId(), eventName});
                LOGGER.log(Level.FINER, "Trigger failure", exception);
            }
        }
    }

    @Override
    public void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(this, rdq.getPlugin());
        LOGGER.log(Level.FINE, "Registered perk trigger listeners");
    }

    @Override
    public void unregisterListeners() {
        LOGGER.log(Level.FINE, "Perk trigger listeners will be unregistered automatically on plugin shutdown");
    }
}