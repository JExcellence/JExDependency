package com.raindropcentral.rdq2.perk.event;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PerkEventBus {

    private final List<PerkEventListener> listeners;

    public PerkEventBus() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void register(@NotNull PerkEventListener listener) {
        listeners.add(listener);
    }

    public void unregister(@NotNull PerkEventListener listener) {
        listeners.remove(listener);
    }

    public void fireActivated(@NotNull Player player, @NotNull String perkId) {
        for (PerkEventListener listener : listeners) {
            try {
                listener.onPerkActivated(player, perkId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireDeactivated(@NotNull Player player, @NotNull String perkId) {
        for (PerkEventListener listener : listeners) {
            try {
                listener.onPerkDeactivated(player, perkId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireTriggered(@NotNull Player player, @NotNull String perkId) {
        for (PerkEventListener listener : listeners) {
            try {
                listener.onPerkTriggered(player, perkId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireCooldownStart(@NotNull Player player, @NotNull String perkId, long durationSeconds) {
        for (PerkEventListener listener : listeners) {
            try {
                listener.onPerkCooldownStart(player, perkId, durationSeconds);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireCooldownEnd(@NotNull Player player, @NotNull String perkId) {
        for (PerkEventListener listener : listeners) {
            try {
                listener.onPerkCooldownEnd(player, perkId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearListeners() {
        listeners.clear();
    }
}
