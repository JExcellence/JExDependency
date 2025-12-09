package com.raindropcentral.rdq2.perk.runtime;

import com.raindropcentral.rdq2.type.EPerkType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public interface PerkRuntime {
    @NotNull String getId();
    @NotNull EPerkType getType();
    boolean canActivate(@NotNull Player player);
    boolean activate(@NotNull Player player);
    boolean deactivate(@NotNull Player player);
    void trigger(@NotNull Player player);
    
    default void trigger(@NotNull Player player, @NotNull String source) {
        trigger(player);
    }
    
    boolean isOnCooldown(@NotNull Player player);
    long getRemainingCooldown(@NotNull Player player);
    void setCooldown(@NotNull Player player, long seconds);
    boolean isActive(@NotNull Player player);
    
    default boolean supports(@NotNull Event event) {
        return true;
    }
}