package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public interface PerkServiceListener {
    
    void onActivate(@NotNull Player player);
    
    void onDeactivate(@NotNull Player player);
    
    void onTrigger(@NotNull Player player);
    
    void registerEventHandlers(@NotNull PluginManager manager);
    
    @NotNull String getPerkId();
}
