package com.raindropcentral.rdq.perk.event;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PerkEventListener {

    void onPerkActivated(@NotNull Player player, @NotNull String perkId);

    void onPerkDeactivated(@NotNull Player player, @NotNull String perkId);

    void onPerkTriggered(@NotNull Player player, @NotNull String perkId);

    void onPerkCooldownStart(@NotNull Player player, @NotNull String perkId, long durationSeconds);

    void onPerkCooldownEnd(@NotNull Player player, @NotNull String perkId);
}
