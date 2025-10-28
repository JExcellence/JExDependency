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
 * @version 1.0.0
 * @since TBD
 */
public class DefaultPerkTriggerService implements PerkTriggerService, Listener {

    private final RDQ rdq;
    private final PerkRegistry perkRegistry;

    public DefaultPerkTriggerService(@NotNull RDQ rdq, @NotNull PerkRegistry perkRegistry) {
        this.rdq = rdq;
        this.perkRegistry = perkRegistry;
    }

    @Override
    public void handleEvent(@NotNull Event event, @NotNull Player player) {
        // TODO: Implement event handling logic
        // Check which perks are active for the player and trigger them if applicable
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