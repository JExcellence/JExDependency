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

/**
 * Service responsible for validating the structural integrity of configured perks
 * and the overall perk system.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
final class PerkValidationService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkValidationService.class.getName());

    /**
     * Validates the perk configuration asynchronously to ensure all declared perks are
     * internally consistent before the system is used.
     *
     * @param state    the current state containing perk configuration snapshots
     * @param executor the executor responsible for running the validation task
     * @return a future that completes when configuration validation succeeds
     * @throws IllegalStateException if the validation detects invalid configurations
     */
    CompletableFuture<Void> validateConfigurationsAsync(final @NotNull PerkSystemState state,
                                                        final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> validateConfigurations(state), executor);
    }

    /**
     * Validates the runtime perk system asynchronously to ensure that all perks have
     * valid settings and requirements.
     *
     * @param state    the current state containing perk configuration snapshots
     * @param executor the executor responsible for running the validation task
     * @return a future that completes when system validation succeeds
     * @throws IllegalStateException if validation detects issues with the perk system
     */
    CompletableFuture<Void> validateSystemAsync(final @NotNull PerkSystemState state,
                                                final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> validateSystem(state), executor);
    }

    /**
     * Performs configuration validation for all perks on the calling thread.
     *
     * @param state the current state containing perk configuration snapshots
     * @throws IllegalStateException if any perk configuration is invalid
     */
    private void validateConfigurations(final PerkSystemState state) {
        final List<String> errors = new ArrayList<>();

        if (state.perkSections().isEmpty()) {
            LOGGER.warning("No perk configurations found");
            return;
        }

        for (Map.Entry<String, PerkSection> entry : state.perkSections().entrySet()) {
            validatePerkConfiguration(entry.getKey(), entry.getValue(), errors);
        }

        if (!errors.isEmpty()) {
            errors.forEach(err -> LOGGER.log(Level.SEVERE, "Configuration validation error: {0}", err));
            throw new IllegalStateException("Perk configuration validation failed with " + errors.size() + " error(s)");
        }

        LOGGER.info("Perk configuration validation completed successfully");
    }

    /**
     * Performs perk system validation on the calling thread.
     *
     * @param state the current state containing perk entity snapshots
     * @throws IllegalStateException if the perk system has issues
     */
    private void validateSystem(final PerkSystemState state) {
        final List<String> errors = new ArrayList<>();

        if (state.perks().isEmpty()) {
            LOGGER.warning("No perks loaded into system");
            return;
        }

        // Validate that all configured perks were successfully created
        for (String perkId : state.perkSections().keySet()) {
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

    /**
     * Validates a single perk configuration.
     *
     * @param perkId the identifier of the perk being validated
     * @param config the configuration entry for the perk
     * @param errors the collection that receives validation error messages
     */
    private void validatePerkConfiguration(final String perkId,
                                          final PerkSection config,
                                          final List<String> errors) {
        if (config == null) {
            errors.add("Perk '" + perkId + "' has null configuration");
            return;
        }

        if (config.getPerkSettings() == null) {
            errors.add("Perk '" + perkId + "' has no perk settings section");
            return;
        }

        // Validate display name key
        if (config.getPerkSettings().getDisplayNameKey() == null || 
            config.getPerkSettings().getDisplayNameKey().isBlank()) {
            errors.add("Perk '" + perkId + "' has no display name key");
        }

        // Validate description key
        if (config.getPerkSettings().getDescriptionKey() == null || 
            config.getPerkSettings().getDescriptionKey().isBlank()) {
            errors.add("Perk '" + perkId + "' has no description key");
        }

        // Validate priority
        if (config.getPerkSettings().getPriority() < 0) {
            errors.add("Perk '" + perkId + "' has negative priority: " + config.getPerkSettings().getPriority());
        }

        // Validate max concurrent users
        if (config.getPerkSettings().getMaxConcurrentUsers() < -1) {
            errors.add("Perk '" + perkId + "' has invalid maxConcurrentUsers: " + config.getPerkSettings().getMaxConcurrentUsers());
        }
    }
}