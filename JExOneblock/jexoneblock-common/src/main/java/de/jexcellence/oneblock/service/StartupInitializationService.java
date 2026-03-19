package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.repository.OneblockEvolutionRepository;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StartupInitializationService {

    private static final Logger LOGGER = Logger.getLogger(StartupInitializationService.class.getName());

    private final EvolutionInitializationService evolutionInitService;
    private volatile WorldManagementService worldManagementService;
    private final ExecutorService executorService;

    public StartupInitializationService(
            @NotNull EvolutionFactory evolutionFactory,
            @NotNull OneblockEvolutionRepository evolutionRepository,
            @NotNull ExecutorService executorService
    ) {
        this.evolutionInitService = new EvolutionInitializationService(
            evolutionFactory, evolutionRepository, executorService
        );
        this.worldManagementService = null;
        this.executorService = executorService;
    }

    public @NotNull CompletableFuture<StartupResult> performStartupInitialization() {
        LOGGER.info("Starting OneBlock plugin initialization...");

        return CompletableFuture
            .supplyAsync(() -> {
                LOGGER.info("Step 1: Initializing world management...");
                this.worldManagementService = new WorldManagementService(executorService, null);
                return worldManagementService.validateWorlds().join();
            }, executorService)
            .thenCompose(worldResult -> {
                if (!worldResult.isValid()) {
                    LOGGER.warning("World validation had issues: " + worldResult.message());
                }

                LOGGER.info("Step 2: Initializing evolutions...");
                return evolutionInitService.initializeEvolutions()
                    .thenApply(evolutionResult -> new StartupResult(true, "Partial initialization", 
                        worldResult, evolutionResult, null));
            })
            .thenApply(partialResult -> {
                LOGGER.info("✓ All initialization steps completed successfully");
                return new StartupResult(true, "Initialization completed successfully", partialResult.worldResult(), partialResult.evolutionResult(), partialResult.evolutionValidation());
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Critical error during startup initialization", throwable);
                return new StartupResult(false, "Critical error: " + throwable.getMessage(), null, null, null);
            });
    }

    public @NotNull EvolutionInitializationService getEvolutionInitializationService() {
        return evolutionInitService;
    }

    public @NotNull WorldManagementService getWorldManagementService() {
        return worldManagementService;
    }

    public record StartupResult(
        boolean success,
        String message,
        WorldManagementService.WorldValidationResult worldResult,
        EvolutionInitializationService.InitializationResult evolutionResult,
        EvolutionInitializationService.ValidationResult evolutionValidation
    ) {
        public boolean isReadyForIslandCreation() {
            return success && 
                   worldResult != null && worldResult.isValid() &&
                   evolutionValidation != null && evolutionValidation.isValid();
        }
    }

    public record HealthCheckResult(boolean healthy, String message) {}
}