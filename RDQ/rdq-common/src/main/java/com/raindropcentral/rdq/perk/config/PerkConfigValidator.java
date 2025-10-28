package com.raindropcentral.rdq.perk.config;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PerkConfigValidator {

    public static @NotNull ValidationResult validate(@NotNull PerkConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.id() == null || config.id().isBlank()) {
            errors.add("Perk id cannot be null or blank");
        }

        if (config.displayName() == null || config.displayName().isBlank()) {
            errors.add("Perk displayName cannot be null or blank");
        }

        if (config.perkType() == null) {
            errors.add("Perk perkType cannot be null");
        }

        if (config.category() == null) {
            errors.add("Perk category cannot be null");
        }

        if (config.iconMaterial() == null || config.iconMaterial().isBlank()) {
            errors.add("Perk iconMaterial cannot be null or blank");
        }

        if (config.priority() < 0) {
            errors.add("Perk priority cannot be negative");
        }

        if (config.cooldownSeconds() != null && config.cooldownSeconds() < 0) {
            errors.add("Perk cooldownSeconds cannot be negative");
        }

        if (config.durationSeconds() != null && config.durationSeconds() < 0) {
            errors.add("Perk durationSeconds cannot be negative");
        }

        if (config.requirements() == null) {
            errors.add("Perk requirements cannot be null");
        }

        if (config.rewards() == null) {
            errors.add("Perk rewards cannot be null");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public record ValidationResult(
        boolean valid,
        @NotNull List<String> errors
    ) {
    }
}
