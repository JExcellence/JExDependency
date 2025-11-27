package com.raindropcentral.rdq.perk.effect;

import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.perk.runtime.CustomPerkHandlers;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class PerkEffectApplicator {

    private static final Logger LOGGER = Logger.getLogger(PerkEffectApplicator.class.getName());
    private static final String MODIFIER_PREFIX = "rdq_perk_";

    private PerkEffectApplicator() {
    }

    public static void apply(@NotNull Player player, @NotNull Perk perk) {
        switch (perk.effect()) {
            case PerkEffect.PotionEffect(var type, var amp) -> applyPotionEffect(player, perk, type, amp);
            case PerkEffect.AttributeModifier(var attr, var val, var op) -> applyAttributeModifier(player, perk, attr, val, op);
            case PerkEffect.Flight(var allowCombat) -> applyFlight(player, allowCombat);
            case PerkEffect.ExperienceMultiplier m -> {}
            case PerkEffect.DeathPrevention d -> {}
            case PerkEffect.Custom(var handler, var config) -> applyCustom(player, handler, config);
        }
    }

    public static void remove(@NotNull Player player, @NotNull Perk perk) {
        switch (perk.effect()) {
            case PerkEffect.PotionEffect(var type, var amp) -> removePotionEffect(player, type);
            case PerkEffect.AttributeModifier(var attr, var val, var op) -> removeAttributeModifier(player, perk, attr);
            case PerkEffect.Flight f -> removeFlight(player);
            case PerkEffect.ExperienceMultiplier m -> {}
            case PerkEffect.DeathPrevention d -> {}
            case PerkEffect.Custom(var handler, var config) -> removeCustom(player, handler, config);
        }
    }

    private static void applyPotionEffect(@NotNull Player player, @NotNull Perk perk, @NotNull String type, int amplifier) {
        var effectType = PotionEffectType.getByName(type);
        if (effectType == null) {
            LOGGER.warning("Unknown potion effect type: " + type);
            return;
        }

        int duration = perk.hasDuration() ? perk.durationSeconds() * 20 : Integer.MAX_VALUE;
        var effect = new PotionEffect(effectType, duration, amplifier, false, false, true);
        player.addPotionEffect(effect);
    }

    private static void removePotionEffect(@NotNull Player player, @NotNull String type) {
        var effectType = PotionEffectType.getByName(type);
        if (effectType != null) {
            player.removePotionEffect(effectType);
        }
    }

    private static void applyAttributeModifier(
        @NotNull Player player,
        @NotNull Perk perk,
        @NotNull String attribute,
        double value,
        @NotNull String operation
    ) {
        try {
            var attr = Attribute.valueOf(attribute.toUpperCase());
            var instance = player.getAttribute(attr);
            if (instance == null) {
                LOGGER.warning("Player does not have attribute: " + attribute);
                return;
            }

            var op = parseOperation(operation);
            var modifierId = createModifierId(perk.id());
            var modifier = new AttributeModifier(
                modifierId,
                MODIFIER_PREFIX + perk.id(),
                value,
                op
            );

            instance.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(modifierId))
                .forEach(instance::removeModifier);

            instance.addModifier(modifier);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown attribute: " + attribute);
        }
    }

    private static void removeAttributeModifier(@NotNull Player player, @NotNull Perk perk, @NotNull String attribute) {
        try {
            var attr = Attribute.valueOf(attribute.toUpperCase());
            var instance = player.getAttribute(attr);
            if (instance == null) return;

            var modifierId = createModifierId(perk.id());
            instance.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(modifierId))
                .forEach(instance::removeModifier);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown attribute: " + attribute);
        }
    }

    private static void applyFlight(@NotNull Player player, boolean allowInCombat) {
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    private static void removeFlight(@NotNull Player player) {
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    private static void applyCustom(@NotNull Player player, @NotNull String handler, @NotNull Map<String, Object> config) {
        CustomPerkHandlers.get(handler).ifPresent(h -> h.apply(player, config));
    }

    private static void removeCustom(@NotNull Player player, @NotNull String handler, @NotNull Map<String, Object> config) {
        CustomPerkHandlers.get(handler).ifPresent(h -> h.remove(player, config));
    }

    @NotNull
    private static AttributeModifier.Operation parseOperation(@NotNull String operation) {
        return switch (operation.toUpperCase()) {
            case "ADD", "ADD_NUMBER" -> AttributeModifier.Operation.ADD_NUMBER;
            case "MULTIPLY_BASE", "ADD_SCALAR" -> AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY", "MULTIPLY_SCALAR_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> AttributeModifier.Operation.ADD_NUMBER;
        };
    }

    @NotNull
    private static UUID createModifierId(@NotNull String perkId) {
        return UUID.nameUUIDFromBytes((MODIFIER_PREFIX + perkId).getBytes());
    }
}
