/*
package com.raindropcentral.rdq2.perk;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

*/
/**
 * Sealed interface representing perk effects.
 *
 * <p>Effects are applied when a perk is activated. Use pattern matching
 * to handle different effect types:
 *
 * <pre>{@code
 * switch (perk.effect()) {
 *     case PotionEffect(var type, var amp) -> applyPotion(player, type, amp);
 *     case Flight(var combat) -> player.setAllowFlight(true);
 *     // ...
 * }
 * }</pre>
 *
 * @see Perk
 *//*

public sealed interface PerkEffect {

    record PotionEffect(
        @NotNull String potionType,
        int amplifier
    ) implements PerkEffect {
        public PotionEffect {
            Objects.requireNonNull(potionType, "potionType");
            if (amplifier < 0) throw new IllegalArgumentException("amplifier must be non-negative");
        }
    }

    record AttributeModifier(
        @NotNull String attribute,
        double value,
        @NotNull String operation
    ) implements PerkEffect {
        public AttributeModifier {
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(operation, "operation");
        }
    }

    record Flight(boolean allowInCombat) implements PerkEffect {
    }

    record ExperienceMultiplier(double multiplier) implements PerkEffect {
        public ExperienceMultiplier {
            if (multiplier <= 0) throw new IllegalArgumentException("multiplier must be positive");
        }
    }

    record DeathPrevention(int healthOnSave) implements PerkEffect {
        public DeathPrevention {
            if (healthOnSave < 1) throw new IllegalArgumentException("healthOnSave must be at least 1");
        }
    }

    record Custom(
        @NotNull String handler,
        @NotNull Map<String, Object> config
    ) implements PerkEffect {
        public Custom {
            Objects.requireNonNull(handler, "handler");
            config = config != null ? Map.copyOf(config) : Map.of();
        }
    }
}
*/
