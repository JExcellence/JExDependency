package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

final class PerkValidationService {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkValidationService.class.getName());

    CompletableFuture<Void> validateConfigurationsAsync(final @NotNull PerkSystemState state,
                                                        final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (state.perkSections().isEmpty()) return;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Perk configuration validation failed", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }

    CompletableFuture<Void> validateSystemAsync(final @NotNull PerkSystemState state,
                                                final @NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (state.perks().isEmpty()) return;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Perk system validation failed", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
}