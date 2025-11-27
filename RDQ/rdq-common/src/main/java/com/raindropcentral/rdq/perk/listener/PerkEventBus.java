package com.raindropcentral.rdq.perk.listener;

import com.raindropcentral.rdq.perk.Perk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class PerkEventBus {

    private static final Logger LOGGER = Logger.getLogger(PerkEventBus.class.getName());

    private final List<Consumer<PerkActivatedEvent>> activationListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PerkDeactivatedEvent>> deactivationListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PerkUnlockedEvent>> unlockListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PerkCooldownStartedEvent>> cooldownListeners = new CopyOnWriteArrayList<>();

    public void onActivation(@NotNull Consumer<PerkActivatedEvent> listener) {
        activationListeners.add(listener);
    }

    public void onDeactivation(@NotNull Consumer<PerkDeactivatedEvent> listener) {
        deactivationListeners.add(listener);
    }

    public void onUnlock(@NotNull Consumer<PerkUnlockedEvent> listener) {
        unlockListeners.add(listener);
    }

    public void onCooldownStarted(@NotNull Consumer<PerkCooldownStartedEvent> listener) {
        cooldownListeners.add(listener);
    }

    public void fireActivation(@NotNull Player player, @NotNull Perk perk) {
        var event = new PerkActivatedEvent(player.getUniqueId(), player.getName(), perk);
        activationListeners.forEach(l -> {
            try {
                l.accept(event);
            } catch (Exception e) {
                LOGGER.warning("Error in perk activation listener: " + e.getMessage());
            }
        });
    }

    public void fireDeactivation(@NotNull Player player, @NotNull Perk perk, @NotNull DeactivationReason reason) {
        var event = new PerkDeactivatedEvent(player.getUniqueId(), player.getName(), perk, reason);
        deactivationListeners.forEach(l -> {
            try {
                l.accept(event);
            } catch (Exception e) {
                LOGGER.warning("Error in perk deactivation listener: " + e.getMessage());
            }
        });
    }

    public void fireUnlock(@NotNull Player player, @NotNull Perk perk) {
        var event = new PerkUnlockedEvent(player.getUniqueId(), player.getName(), perk);
        unlockListeners.forEach(l -> {
            try {
                l.accept(event);
            } catch (Exception e) {
                LOGGER.warning("Error in perk unlock listener: " + e.getMessage());
            }
        });
    }

    public void fireCooldownStarted(@NotNull UUID playerId, @NotNull Perk perk, int cooldownSeconds) {
        var event = new PerkCooldownStartedEvent(playerId, perk, cooldownSeconds);
        cooldownListeners.forEach(l -> {
            try {
                l.accept(event);
            } catch (Exception e) {
                LOGGER.warning("Error in perk cooldown listener: " + e.getMessage());
            }
        });
    }

    public void clear() {
        activationListeners.clear();
        deactivationListeners.clear();
        unlockListeners.clear();
        cooldownListeners.clear();
    }

    public record PerkActivatedEvent(
        @NotNull UUID playerId,
        @NotNull String playerName,
        @NotNull Perk perk
    ) {}

    public record PerkDeactivatedEvent(
        @NotNull UUID playerId,
        @NotNull String playerName,
        @NotNull Perk perk,
        @NotNull DeactivationReason reason
    ) {}

    public record PerkUnlockedEvent(
        @NotNull UUID playerId,
        @NotNull String playerName,
        @NotNull Perk perk
    ) {}

    public record PerkCooldownStartedEvent(
        @NotNull UUID playerId,
        @NotNull Perk perk,
        int cooldownSeconds
    ) {}

    public enum DeactivationReason {
        MANUAL,
        DURATION_EXPIRED,
        COMBAT,
        DISCONNECT,
        ADMIN,
        DEATH
    }
}
