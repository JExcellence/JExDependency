package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

final class PerkValidationService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkValidationService.class.getName());

    CompletableFuture<Void> validateConfigurationsAsync(@NotNull PerkSystemState state,
                                                        @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> validateConfigurations(state), executor);
    }

    CompletableFuture<Void> validateSystemAsync(@NotNull PerkSystemState state,
                                                @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> validateSystem(state), executor);
    }

    private void validateConfigurations(PerkSystemState state) {
        var errors = new ArrayList<String>();

        if (state.perkSections().isEmpty()) {
            LOGGER.warning("No perk configurations found");
            return;
        }

        for (var entry : state.perkSections().entrySet()) {
            validatePerkConfiguration(entry.getKey(), entry.getValue(), errors);
        }

        if (!errors.isEmpty()) {
            errors.forEach(err -> LOGGER.log(Level.SEVERE, "Configuration validation error: {0}", err));
            throw new IllegalStateException("Perk configuration validation failed with " + errors.size() + " error(s)");
        }

        LOGGER.info("Perk configuration validation completed successfully");
    }

    private void validateSystem(PerkSystemState state) {
        var errors = new ArrayList<String>();

        if (state.perks().isEmpty()) {
            LOGGER.warning("No perks loaded into system");
            return;
        }

        for (var perkId : state.perkSections().keySet()) {
            if (!state.perks().containsKey(perkId)) {
                errors.add("Perk '" + perkId + "' was configured but not created in database");
            }
        }

        if (!errors.isEmpty()) {
            errors.forEach(err -> LOGGER.log(Level.SEVERE, "System validation error: {0}", err));
            throw new IllegalStateException("Perk system validation failed with " + errors.size() + " error(s)");
        }

        LOGGER.log(Level.INFO, "Perk system validation completed successfully ({0} perks)", state.perks().size());
    }

    private void validatePerkConfiguration(String perkId,
                                          PerkSection config,
                                          List<String> errors) {
        if (config == null) {
            errors.add("Perk '" + perkId + "' has null configuration");
            return;
        }

        if (config.getPerkSettings() == null) {
            errors.add("Perk '" + perkId + "' has no perk settings section");
            return;
        }

        if (config.getPerkSettings().getDisplayNameKey() == null || 
            config.getPerkSettings().getDisplayNameKey().isBlank()) {
            errors.add("Perk '" + perkId + "' has no display name key");
        }

        if (config.getPerkSettings().getDescriptionKey() == null || 
            config.getPerkSettings().getDescriptionKey().isBlank()) {
            errors.add("Perk '" + perkId + "' has no description key");
        }

        if (config.getPerkSettings().getPriority() < 0) {
            errors.add("Perk '" + perkId + "' has negative priority: " + config.getPerkSettings().getPriority());
        }

        if (config.getPerkSettings().getMaxConcurrentUsers() < -1) {
            errors.add("Perk '" + perkId + "' has invalid maxConcurrentUsers: " + config.getPerkSettings().getMaxConcurrentUsers());
        }
    }
}