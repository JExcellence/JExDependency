package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates perk system configurations and state consistency.
 *
 * <p>This service ensures that all loaded perk configurations are valid and that the system
 * state is consistent before gameplay systems access the perks.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
final class PerkValidationService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkValidationService.class.getName());

    /**
     * Validates all perk configurations asynchronously.
     *
     * @param state    the aggregated perk system state to validate
     * @param executor the executor used for asynchronous validation work
     * @return a future that completes when validation finishes
     */
    CompletableFuture<Void> validateConfigurationsAsync(
            final @NotNull PerkSystemState state,
            final @NotNull Executor executor
    ) {
        return CompletableFuture.runAsync(() -> {
            if (state.perkSections().isEmpty()) {
                LOGGER.info("No perk configurations to validate");
                return;
            }

            int validCount = 0;
            int invalidCount = 0;

            for (String perkId : state.perkSections().keySet()) {
                try {
                    if (validatePerkConfiguration(perkId, state)) {
                        validCount++;
                    } else {
                        invalidCount++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Validation error for perk: " + perkId, e);
                    invalidCount++;
                }
            }

            LOGGER.log(Level.INFO, "Perk configuration validation: {0} valid, {1} invalid",
                    new Object[]{validCount, invalidCount});
        }, executor);
    }

    /**
     * Validates the entire perk system state for consistency.
     *
     * @param state    the aggregated perk system state to validate
     * @param executor the executor used for asynchronous validation work
     * @return a future that completes when system validation finishes
     */
    CompletableFuture<Void> validateSystemAsync(final @NotNull PerkSystemState state,
                                                final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            if (state.perks().isEmpty()) {
                LOGGER.warning("Perk system has no loaded perks");
                return;
            }

            LOGGER.log(Level.INFO, "Perk system validation completed with {0} perks",
                    state.perks().size());
        }, executor);
    }

    /**
     * Validates a single perk configuration.
     *
     * @param perkId the perk identifier to validate
     * @param state  the perk system state containing the configuration
     * @return {@code true} if the configuration is valid, {@code false} otherwise
     */
    private boolean validatePerkConfiguration(final @NotNull String perkId,
                                              final @NotNull PerkSystemState state) {
        final var perkSection = state.perkSections().get(perkId);
        if (perkSection == null) {
            LOGGER.log(Level.WARNING, "Perk configuration not found: {0}", perkId);
            return false;
        }

        final var settings = perkSection.getPerkSettings();
        if (settings == null) {
            LOGGER.log(Level.WARNING, "Perk settings missing for: {0}", perkId);
            return false;
        }

        if (!settings.getEnabled()) {
            LOGGER.log(Level.FINE, "Perk is disabled: {0}", perkId);
            return true;
        }

        return true;
    }
}