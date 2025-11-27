package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.shared.Result;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class PerkRuntime {

    private static final Logger LOGGER = Logger.getLogger(PerkRuntime.class.getName());

    private final Perk perk;
    private final Plugin plugin;
    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> activeUsers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitRunnable> durationTasks = new ConcurrentHashMap<>();

    public PerkRuntime(@NotNull Perk perk, @NotNull Plugin plugin) {
        this.perk = perk;
        this.plugin = plugin;
    }

    @NotNull
    public Perk perk() {
        return perk;
    }

    @NotNull
    public Result<Void> activate(@NotNull Player player) {
        var playerId = player.getUniqueId();

        if (isOnCooldown(playerId)) {
            var remaining = getRemainingCooldown(playerId).orElse(Duration.ZERO);
            return new Result.Failure<>("perk.on_cooldown", Map.of(
                "remaining", formatDuration(remaining)
            ));
        }

        if (activeUsers.contains(playerId)) {
            return new Result.Failure<>("perk.already_active", Map.of("perk", perk.id()));
        }

        activeUsers.add(playerId);
        applyEffect(player);

        if (perk.hasDuration()) {
            scheduleDurationExpiry(player);
        }

        LOGGER.fine(() -> "Activated perk " + perk.id() + " for player " + player.getName());
        return new Result.Success<>(null);
    }

    public void deactivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        if (activeUsers.remove(playerId)) {
            removeEffect(player);
            cancelDurationTask(playerId);

            if (perk.hasCooldown()) {
                cooldowns.put(playerId, Instant.now().plusSeconds(perk.cooldownSeconds()));
            }

            LOGGER.fine(() -> "Deactivated perk " + perk.id() + " for player " + player.getName());
        }
    }

    public void forceDeactivate(@NotNull Player player) {
        var playerId = player.getUniqueId();
        activeUsers.remove(playerId);
        removeEffect(player);
        cancelDurationTask(playerId);
    }

    public boolean isActive(@NotNull UUID playerId) {
        return activeUsers.contains(playerId);
    }

    public boolean isOnCooldown(@NotNull UUID playerId) {
        var expiry = cooldowns.get(playerId);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            cooldowns.remove(playerId);
            return false;
        }
        return true;
    }

    @NotNull
    public Optional<Duration> getRemainingCooldown(@NotNull UUID playerId) {
        var expiry = cooldowns.get(playerId);
        if (expiry == null || Instant.now().isAfter(expiry)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(Instant.now(), expiry));
    }

    public void clearCooldown(@NotNull UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void cleanup(@NotNull UUID playerId) {
        activeUsers.remove(playerId);
        cooldowns.remove(playerId);
        cancelDurationTask(playerId);
    }

    @NotNull
    public Set<UUID> getActiveUsers() {
        return Set.copyOf(activeUsers);
    }

    private void applyEffect(@NotNull Player player) {
        switch (perk.effect()) {
            case PerkEffect.PotionEffect(var type, var amp) -> applyPotionEffect(player, type, amp);
            case PerkEffect.AttributeModifier(var attr, var val, var op) -> applyAttributeModifier(player, attr, val, op);
            case PerkEffect.Flight(var allowCombat) -> applyFlight(player, allowCombat);
            case PerkEffect.ExperienceMultiplier m -> {}
            case PerkEffect.DeathPrevention d -> {}
            case PerkEffect.Custom(var handler, var config) -> applyCustomEffect(player, handler, config);
        }
    }

    private void removeEffect(@NotNull Player player) {
        switch (perk.effect()) {
            case PerkEffect.PotionEffect(var type, var amp) -> removePotionEffect(player, type);
            case PerkEffect.AttributeModifier(var attr, var val, var op) -> removeAttributeModifier(player, attr);
            case PerkEffect.Flight f -> removeFlight(player);
            case PerkEffect.ExperienceMultiplier m -> {}
            case PerkEffect.DeathPrevention d -> {}
            case PerkEffect.Custom(var handler, var config) -> removeCustomEffect(player, handler, config);
        }
    }

    private void applyPotionEffect(@NotNull Player player, @NotNull String type, int amplifier) {
        var effectType = PotionEffectType.getByName(type);
        if (effectType == null) {
            LOGGER.warning("Unknown potion effect type: " + type);
            return;
        }

        int duration = perk.hasDuration() ? perk.durationSeconds() * 20 : Integer.MAX_VALUE;
        var effect = new PotionEffect(effectType, duration, amplifier, false, false, true);
        player.addPotionEffect(effect);
    }

    private void removePotionEffect(@NotNull Player player, @NotNull String type) {
        var effectType = PotionEffectType.getByName(type);
        if (effectType != null) {
            player.removePotionEffect(effectType);
        }
    }

    private void applyAttributeModifier(@NotNull Player player, @NotNull String attribute, double value, @NotNull String operation) {
        try {
            var attr = Attribute.valueOf(attribute.toUpperCase());
            var instance = player.getAttribute(attr);
            if (instance == null) return;

            var op = switch (operation.toUpperCase()) {
                case "ADD" -> AttributeModifier.Operation.ADD_NUMBER;
                case "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_SCALAR;
                case "MULTIPLY" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
                default -> AttributeModifier.Operation.ADD_NUMBER;
            };

            var modifier = new AttributeModifier(
                UUID.nameUUIDFromBytes(("rdq_perk_" + perk.id()).getBytes()),
                "rdq_perk_" + perk.id(),
                value,
                op
            );

            instance.removeModifier(modifier);
            instance.addModifier(modifier);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown attribute: " + attribute);
        }
    }

    private void removeAttributeModifier(@NotNull Player player, @NotNull String attribute) {
        try {
            var attr = Attribute.valueOf(attribute.toUpperCase());
            var instance = player.getAttribute(attr);
            if (instance == null) return;

            var modifierId = UUID.nameUUIDFromBytes(("rdq_perk_" + perk.id()).getBytes());
            instance.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(modifierId))
                .forEach(instance::removeModifier);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown attribute: " + attribute);
        }
    }

    private void applyFlight(@NotNull Player player, boolean allowInCombat) {
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    private void removeFlight(@NotNull Player player) {
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && 
            player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    private void applyCustomEffect(@NotNull Player player, @NotNull String handler, @NotNull Map<String, Object> config) {
        CustomPerkHandlers.get(handler).ifPresent(h -> h.apply(player, config));
    }

    private void removeCustomEffect(@NotNull Player player, @NotNull String handler, @NotNull Map<String, Object> config) {
        CustomPerkHandlers.get(handler).ifPresent(h -> h.remove(player, config));
    }

    private void scheduleDurationExpiry(@NotNull Player player) {
        var playerId = player.getUniqueId();
        cancelDurationTask(playerId);

        var task = new BukkitRunnable() {
            @Override
            public void run() {
                var p = plugin.getServer().getPlayer(playerId);
                if (p != null && p.isOnline()) {
                    deactivate(p);
                } else {
                    cleanup(playerId);
                }
                durationTasks.remove(playerId);
            }
        };

        task.runTaskLater(plugin, perk.durationSeconds() * 20L);
        durationTasks.put(playerId, task);
    }

    private void cancelDurationTask(@NotNull UUID playerId) {
        var task = durationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    @NotNull
    private String formatDuration(@NotNull Duration duration) {
        var seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + "s";
        }
        var minutes = seconds / 60;
        var remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        var hours = minutes / 60;
        var remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }
}
