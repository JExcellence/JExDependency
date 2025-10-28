package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of PerkTriggerService.
 *
 * @author qodo
 * @version 1.0.1
 * @since TBD
 */
public class DefaultPerkTriggerService implements PerkTriggerService, Listener {

    private final RDQ rdq;
    private final DefaultPerkRegistry perkRegistry;

    public DefaultPerkTriggerService(@NotNull RDQ rdq, @NotNull DefaultPerkRegistry perkRegistry) {
        this.rdq = rdq;
        this.perkRegistry = perkRegistry;
    }

    @Override
    public void handleEvent(@NotNull Event event, @NotNull Player player) {
        for (PerkRuntime runtime : perkRegistry.getAllPerkRuntimes()) {
            if (!runtime.getType().isEventBased()) {
                continue;
            }
            if (!runtime.canActivate(player) && !runtime.isActive(player)) {
                continue;
            }
            if (runtime.isOnCooldown(player)) {
                continue;
            }
            runtime.trigger(player);
        }
    }

    @Override
    public void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(this, rdq.getPlugin());
    }

    @Override
    public void unregisterListeners() {
        // Event listeners are automatically unregistered when plugin disables
    }
}