package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.type.EPerkState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPerkState {

    private final UUID playerId;
    private final Map<String, EPerkState> perkStates;
    private final Map<String, Long> activationTimes;

    public PlayerPerkState(@NotNull Player player) {
        this.playerId = player.getUniqueId();
        this.perkStates = new HashMap<>();
        this.activationTimes = new HashMap<>();
    }

    public @NotNull UUID getPlayerId() {
        return playerId;
    }

    public void setState(@NotNull String perkId, @NotNull EPerkState state) {
        perkStates.put(perkId, state);
    }

    public @NotNull EPerkState getState(@NotNull String perkId) {
        return perkStates.getOrDefault(perkId, EPerkState.LOCKED);
    }

    public void setActivationTime(@NotNull String perkId, long timestamp) {
        activationTimes.put(perkId, timestamp);
    }

    public long getActivationTime(@NotNull String perkId) {
        return activationTimes.getOrDefault(perkId, 0L);
    }

    public boolean isActive(@NotNull String perkId) {
        return getState(perkId) == EPerkState.ACTIVE;
    }

    public void clear() {
        perkStates.clear();
        activationTimes.clear();
    }
}
