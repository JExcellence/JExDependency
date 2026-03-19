package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.repository.OneblockEvolutionRepository;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EvolutionInitializationService {

    private static final Logger LOGGER = Logger.getLogger(EvolutionInitializationService.class.getName());

    private final EvolutionFactory evolutionFactory;
    private final OneblockEvolutionRepository evolutionRepository;
    private final ExecutorService executorService;

    public EvolutionInitializationService(
            @NotNull EvolutionFactory evolutionFactory,
            @NotNull OneblockEvolutionRepository evolutionRepository,
            @NotNull ExecutorService executorService
    ) {
        this.evolutionFactory = evolutionFactory;
        this.evolutionRepository = evolutionRepository;
        this.executorService = executorService;
    }

    public @NotNull CompletableFuture<InitializationResult> initializeEvolutions() {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Starting evolution initialization...");
            
            try {
                var registeredEvolutions = evolutionFactory.getRegisteredEvolutionNames();
                LOGGER.info("Found " + registeredEvolutions.size() + " registered evolutions");

                int created = 0;
                int updated = 0;
                int errors = 0;

                for (var evolutionName : registeredEvolutions) {
                    try {
                        var status = initializeEvolution(evolutionName);
                        switch (status) {
                            case CREATED -> created++;
                            case UPDATED -> updated++;
                            case ERROR -> errors++;
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to initialize evolution: " + evolutionName, e);
                        errors++;
                    }
                }

                var result = new InitializationResult(
                    registeredEvolutions.size(), created, updated, errors
                );

                LOGGER.info("Evolution initialization completed: " + result.total() + " total, " + result.created() + " created, " + result.updated() + " updated, " + result.errors() + " errors");

                return result;

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Critical error during evolution initialization", e);
                return new InitializationResult(0, 0, 0, 1);
            }
        }, executorService);
    }

    private @NotNull InitializationStatus initializeEvolution(@NotNull String evolutionName) {
        try {
            var existingEvolution = evolutionRepository.findByName(evolutionName);
            
            var factoryEvolution = evolutionFactory.createEvolution(evolutionName);
            if (factoryEvolution == null) {
                LOGGER.warning("Factory returned null for evolution: " + evolutionName);
                return InitializationStatus.ERROR;
            }

            if (existingEvolution == null) {
                var savedEvolution = evolutionRepository.create(factoryEvolution);
                return InitializationStatus.CREATED;
            } else {
                if (needsUpdate(existingEvolution, factoryEvolution)) {
                    updateEvolution(existingEvolution, factoryEvolution);
                    var updatedEvolution = evolutionRepository.create(existingEvolution);
                    return InitializationStatus.UPDATED;
                } else {
                    return InitializationStatus.EXISTING;
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing evolution: " + evolutionName, e);
            return InitializationStatus.ERROR;
        }
    }

    private boolean needsUpdate(@NotNull OneblockEvolution existing, @NotNull OneblockEvolution factory) {
        return existing.getLevel() != factory.getLevel() ||
               existing.getExperienceToPass() != factory.getExperienceToPass() ||
               !existing.getShowcase().equals(factory.getShowcase()) ||
               !existing.getDescription().equals(factory.getDescription()) ||
               existing.isDisabled() != factory.isDisabled();
    }

    private void updateEvolution(@NotNull OneblockEvolution existing, @NotNull OneblockEvolution factory) {
        existing.setLevel(factory.getLevel());
        existing.setExperienceToPass(factory.getExperienceToPass());
        existing.setShowcase(factory.getShowcase());
        existing.setDescription(factory.getDescription());
        existing.setDisabled(factory.isDisabled());
    }

    public record InitializationResult(int total, int created, int updated, int errors) {
        public boolean isSuccessful() {
            return errors == 0;
        }
        
        public boolean hasChanges() {
            return created > 0 || updated > 0;
        }
    }

    public record ValidationResult(boolean isValid, String message) {}

    private enum InitializationStatus {
        CREATED, UPDATED, EXISTING, ERROR
    }
}