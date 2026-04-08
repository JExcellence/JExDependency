package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.evolution.CustomEvolution;
import de.jexcellence.oneblock.database.entity.evolution.CustomEvolutionBuilder;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.factory.EvolutionFactory;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@Slf4j
public class EvolutionIntegrationService {
    
    private final EvolutionFactory evolutionFactory;
    private final Map<String, ValidationResult> validationCache = new HashMap<>();
    private final Set<String> registeredBuilders = new HashSet<>();

    public EvolutionIntegrationService(@NotNull EvolutionFactory evolutionFactory) {
        this.evolutionFactory = evolutionFactory;
    }

    public EvolutionIntegrationService() {
        this.evolutionFactory = EvolutionFactory.getInstance();
    }

    public boolean isAvailable() {
        return evolutionFactory != null;
    }

    public boolean registerCustomEvolutionBuilder(@NotNull String name, @NotNull CustomEvolutionBuilder builder) {
        try {
            var validation = validateCustomEvolutionBuilder(builder);
            if (!validation.isValid()) {
                log.error("Custom evolution builder '{}' failed validation: {}", name, validation.getErrorMessage());
                return false;
            }
            
            var supplier = builder.build();
            boolean success = evolutionFactory.registerEvolution(name, supplier);
            
            if (success) {
                registeredBuilders.add(name);
                validationCache.put(name, validation);
                log.info("Successfully registered custom evolution builder: {}", name);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to register custom evolution builder '{}': {}", name, e.getMessage(), e);
            return false;
        }
    }

    public boolean registerPredefinedEvolution(@NotNull PredefinedEvolution evolution) {
        try {
            var validation = validatePredefinedEvolution(evolution);
            if (!validation.isValid()) {
                log.error("Predefined evolution '{}' failed validation: {}", evolution.getEvolutionName(), validation.getErrorMessage());
                return false;
            }
            
            boolean success = evolutionFactory.registerPredefinedEvolution(evolution);
            
            if (success) {
                validationCache.put(evolution.getEvolutionName(), validation);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to register predefined evolution '{}': {}", evolution.getEvolutionName(), e.getMessage(), e);
            return false;
        }
    }

    public boolean registerCustomEvolution(@NotNull CustomEvolution evolution) {
        try {
            var validation = validateCustomEvolution(evolution);
            if (!validation.isValid()) {
                log.error("Custom evolution '{}' failed validation: {}", evolution.getEvolutionName(), validation.getErrorMessage());
                return false;
            }
            
            boolean success = evolutionFactory.registerCustomEvolution(evolution);
            
            if (success) {
                validationCache.put(evolution.getEvolutionName(), validation);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to register custom evolution '{}': {}", evolution.getEvolutionName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validates a custom evolution builder configuration
     * @param builder the builder to validate
     * @return validation result with errors if any
     */
    public @NotNull ValidationResult validateCustomEvolutionBuilder(@NotNull CustomEvolutionBuilder builder) {
        List<String> errors = new ArrayList<>();
        
        try {
            // Test build the evolution to check for issues
            OneblockEvolution evolution = builder.build().get();
            
            // Validate the built evolution
            ValidationResult evolutionValidation = validateEvolution(evolution);
            if (!evolutionValidation.isValid()) {
                errors.addAll(evolutionValidation.getErrors());
            }
            
        } catch (IllegalArgumentException e) {
            errors.add("Builder configuration error: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Unexpected error during builder validation: " + e.getMessage());
        }
        
        return new ValidationResult(errors);
    }
    
    /**
     * Validates a predefined evolution configuration
     * @param evolution the evolution to validate
     * @return validation result with errors if any
     */
    public @NotNull ValidationResult validatePredefinedEvolution(@NotNull PredefinedEvolution evolution) {
        List<String> errors = new ArrayList<>();
        
        // Use the evolution's built-in validation
        errors.addAll(evolution.validateConfiguration());
        
        // Additional integration-specific validation
        if (evolutionFactory.isEvolutionRegistered(evolution.getEvolutionName())) {
            errors.add("Evolution name '" + evolution.getEvolutionName() + "' is already registered");
        }
        
        return new ValidationResult(errors);
    }
    
    /**
     * Validates a custom evolution configuration
     * @param evolution the evolution to validate
     * @return validation result with errors if any
     */
    public @NotNull ValidationResult validateCustomEvolution(@NotNull CustomEvolution evolution) {
        List<String> errors = new ArrayList<>();
        
        // Use the evolution's built-in validation
        errors.addAll(evolution.validateConfiguration());
        
        // Additional integration-specific validation
        if (evolutionFactory.isEvolutionRegistered(evolution.getEvolutionName())) {
            errors.add("Evolution name '" + evolution.getEvolutionName() + "' is already registered");
        }
        
        return new ValidationResult(errors);
    }
    
    /**
     * Validates any evolution configuration
     * @param evolution the evolution to validate
     * @return validation result with errors if any
     */
    public @NotNull ValidationResult validateEvolution(@NotNull OneblockEvolution evolution) {
        List<String> errors = new ArrayList<>();
        
        // Basic validation
        if (evolution.getEvolutionName() == null || evolution.getEvolutionName().trim().isEmpty()) {
            errors.add("Evolution name cannot be null or empty");
        }
        
        if (evolution.getLevel() < 0) {
            errors.add("Evolution level cannot be negative");
        }
        
        if (evolution.getExperienceToPass() <= 0) {
            errors.add("Experience to pass must be greater than 0");
        }
        
        if (!evolution.isReady()) {
            errors.add("Evolution must have at least one valid content type");
        }
        
        // Content validation
        boolean hasValidContent = false;
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            if (evolution.hasContentForRarity(rarity)) {
                hasValidContent = true;
                break;
            }
        }
        
        if (!hasValidContent) {
            errors.add("Evolution must have content for at least one rarity level");
        }
        
        // Type-specific validation
        if (evolution instanceof CustomEvolution) {
            errors.addAll(((CustomEvolution) evolution).validateConfiguration());
        } else if (evolution instanceof PredefinedEvolution) {
            errors.addAll(((PredefinedEvolution) evolution).validateConfiguration());
        }
        
        return new ValidationResult(errors);
    }
    
    /**
     * Validates all registered evolutions
     * @return map of evolution names to their validation results
     */
    public @NotNull Map<String, ValidationResult> validateAllEvolutions() {
        Map<String, ValidationResult> results = new HashMap<>();
        
        for (String evolutionName : evolutionFactory.getRegisteredEvolutionNames()) {
            try {
                OneblockEvolution evolution = evolutionFactory.createEvolution(evolutionName);
                if (evolution != null) {
                    ValidationResult result = validateEvolution(evolution);
                    results.put(evolutionName, result);
                    validationCache.put(evolutionName, result);
                } else {
                    results.put(evolutionName, new ValidationResult(List.of("Failed to create evolution instance")));
                }
            } catch (Exception e) {
                results.put(evolutionName, new ValidationResult(List.of("Validation error: " + e.getMessage())));
            }
        }
        
        return results;
    }
    
    // ========== Type Conversion Utilities ==========
    
    /**
     * Converts a PredefinedEvolution to a CustomEvolution
     * @param predefined the predefined evolution to convert
     * @param newCreator the creator name for the custom evolution
     * @return the converted custom evolution
     */
    public @NotNull CustomEvolution convertToCustomEvolution(@NotNull PredefinedEvolution predefined, @NotNull String newCreator) {
        CustomEvolution custom = new CustomEvolution();
        custom.setEvolutionName(predefined.getEvolutionName() + "_Custom");
        custom.setLevel(predefined.getLevel());
        custom.setExperienceToPass(predefined.getExperienceToPass());
        custom.setShowcase(predefined.getShowcase());
        custom.setDescription(predefined.getDescription());
        custom.setCreatorName(newCreator);
        custom.setTemplate(false);
        custom.setDisabled(predefined.isDisabled());
        return custom;
    }
    
    /**
     * Converts a CustomEvolution to a template
     * @param custom the custom evolution to convert
     * @return the template evolution
     */
    public @NotNull CustomEvolution convertToTemplate(@NotNull CustomEvolution custom) {
        CustomEvolution template = new CustomEvolution();
        template.setEvolutionName(custom.getEvolutionName() + "_Template");
        template.setLevel(custom.getLevel());
        template.setExperienceToPass(custom.getExperienceToPass());
        template.setShowcase(custom.getShowcase());
        template.setDescription(custom.getDescription());
        template.setCreatorName(custom.getCreatorName());
        template.setTemplate(true);
        template.setDisabled(false);
        
        // Copy content from original
        custom.getBlocks().forEach(template::addBlock);
        custom.getEntities().forEach(template::addEntity);
        custom.getItems().forEach(template::addItem);
        
        return template;
    }
    
    /**
     * Creates a CustomEvolutionBuilder from an existing evolution
     * @param evolution the evolution to convert
     * @return a new builder configured with the evolution's settings
     */
    public @NotNull CustomEvolutionBuilder createBuilderFromEvolution(@NotNull OneblockEvolution evolution) {
        CustomEvolutionBuilder builder = CustomEvolutionBuilder.create()
            .evolutionName(evolution.getEvolutionName())
            .level(evolution.getLevel())
            .experienceRequired(evolution.getExperienceToPass())
            .description(evolution.getDescription())
            .showcase(evolution.getShowcase())
            .disabled(evolution.isDisabled());
        
        // Add content from evolution
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            if (evolution.hasContentForRarity(rarity)) {
                builder.addBlocks(rarity, evolution.getBlocksByRarity(rarity));
                builder.addEntities(rarity, evolution.getEntitiesByRarity(rarity));
                evolution.getItemsByRarity(rarity).forEach(item -> builder.addItemStack(rarity, item));
            }
        }
        
        return builder;
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Gets the validation result for a registered evolution
     * @param evolutionName the name of the evolution
     * @return the cached validation result, or null if not found
     */
    public @Nullable ValidationResult getValidationResult(@NotNull String evolutionName) {
        return validationCache.get(evolutionName);
    }
    
    /**
     * Clears the validation cache
     */
    public void clearValidationCache() {
        validationCache.clear();
    }
    
    /**
     * Gets all registered builder names
     * @return set of registered builder names
     */
    public @NotNull Set<String> getRegisteredBuilders() {
        return new HashSet<>(registeredBuilders);
    }
    
    /**
     * Checks if a builder is registered
     * @param name the builder name to check
     * @return true if the builder is registered
     */
    public boolean isBuilderRegistered(@NotNull String name) {
        return registeredBuilders.contains(name);
    }
    
    /**
     * Checks if an evolution exists in the factory
     * @param evolutionName the name of the evolution to check
     * @return true if the evolution exists
     */
    public boolean evolutionExists(@NotNull String evolutionName) {
        return evolutionFactory.isEvolutionRegistered(evolutionName);
    }
    
    /**
     * Gets a random block material from the specified evolution and level
     * @param evolutionName the name of the evolution
     * @param evolutionLevel the evolution level
     * @return Optional containing the random material, or empty if not found
     */
    public @NotNull Optional<org.bukkit.Material> getRandomBlockMaterial(@NotNull String evolutionName, int evolutionLevel) {
        try {
            OneblockEvolution evolution = evolutionFactory.createEvolution(evolutionName);
            if (evolution == null) {
                return Optional.empty();
            }
            return evolution.getRandomBlock();
        } catch (Exception e) {
            log.warn("Failed to get random block material for evolution '{}' level {}: {}", 
                    evolutionName, evolutionLevel, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Unregisters a builder and removes it from the factory
     * @param name the name of the builder to unregister
     * @return true if the builder was unregistered
     */
    public boolean unregisterBuilder(@NotNull String name) {
        boolean wasRegistered = registeredBuilders.remove(name);
        validationCache.remove(name);
        
        if (wasRegistered) {
            evolutionFactory.unregisterEvolution(name);
            log.info("Unregistered evolution builder: {}", name);
        }
        
        return wasRegistered;
    }
    
    /**
     * Gets integration statistics
     * @return map containing integration statistics
     */
    public @NotNull Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredBuilders", registeredBuilders.size());
        stats.put("validationCacheSize", validationCache.size());
        stats.put("factoryStatistics", evolutionFactory.getStatistics());
        
        // Validation statistics
        long validEvolutions = validationCache.values().stream()
            .mapToLong(result -> result.isValid() ? 1 : 0)
            .sum();
        stats.put("validEvolutions", validEvolutions);
        stats.put("invalidEvolutions", validationCache.size() - validEvolutions);
        
        return stats;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Represents the result of a validation operation
     */
    public static class ValidationResult {
        private final List<String> errors;
        private final boolean valid;
        
        public ValidationResult(@NotNull List<String> errors) {
            this.errors = new ArrayList<>(errors);
            this.valid = errors.isEmpty();
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public @NotNull List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public @NotNull String getErrorMessage() {
            return errors.isEmpty() ? "No errors" : String.join("; ", errors);
        }
        
        public int getErrorCount() {
            return errors.size();
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", errorCount=" + errors.size() +
                    ", errors=" + errors +
                    '}';
        }
    }
}