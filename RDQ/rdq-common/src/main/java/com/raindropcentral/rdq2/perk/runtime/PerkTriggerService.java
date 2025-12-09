package com.raindropcentral.rdq2.perk.runtime;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public interface PerkTriggerService {

    void handleEvent(@NotNull Event event, @NotNull Player player);

    void registerListeners();

    void unregisterListeners();
}