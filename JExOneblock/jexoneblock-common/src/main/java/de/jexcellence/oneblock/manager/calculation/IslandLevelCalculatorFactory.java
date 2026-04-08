package de.jexcellence.oneblock.manager.calculation;

import de.jexcellence.oneblock.manager.config.CalculationConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

/**
 * Factory for creating IslandLevelCalculator instances with proper dependencies.
 * Provides convenient methods for creating calculators with different configurations.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class IslandLevelCalculatorFactory {
    
    /**
     * Creates a new IslandLevelCalculator with the provided dependencies.
     * 
     * @param configuration the calculation configuration
     * @param blockValueProvider the block value provider
     * @param executorService the executor service for async operations
     * @return new IslandLevelCalculator instance
     */
    @NotNull
    public static IslandLevelCalculator create(@NotNull CalculationConfiguration configuration,
                                              @NotNull IBlockValueProvider blockValueProvider,
                                              @NotNull ExecutorService executorService) {
        return new IslandLevelCalculator(configuration, blockValueProvider, executorService);
    }
    
    /**
     * Creates a new IslandLevelCalculator with default BlockValueProvider.
     * 
     * @param configuration the calculation configuration
     * @param executorService the executor service for async operations
     * @return new IslandLevelCalculator instance
     */
    @NotNull
    public static IslandLevelCalculator createWithDefaultProvider(@NotNull CalculationConfiguration configuration,
                                                                 @NotNull ExecutorService executorService) {
        IBlockValueProvider blockValueProvider = new BlockValueProvider(configuration);
        return new IslandLevelCalculator(configuration, blockValueProvider, executorService);
    }
    
    /**
     * Creates a builder for configuring IslandLevelCalculator creation.
     * 
     * @return new builder instance
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating IslandLevelCalculator instances.
     */
    public static class Builder {
        private CalculationConfiguration configuration;
        private IBlockValueProvider blockValueProvider;
        private ExecutorService executorService;
        
        private Builder() {}
        
        /**
         * Sets the calculation configuration.
         * 
         * @param configuration the calculation configuration
         * @return this builder
         */
        @NotNull
        public Builder configuration(@NotNull CalculationConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }
        
        /**
         * Sets the block value provider.
         * 
         * @param blockValueProvider the block value provider
         * @return this builder
         */
        @NotNull
        public Builder blockValueProvider(@NotNull IBlockValueProvider blockValueProvider) {
            this.blockValueProvider = blockValueProvider;
            return this;
        }
        
        /**
         * Sets the executor service.
         * 
         * @param executorService the executor service
         * @return this builder
         */
        @NotNull
        public Builder executorService(@NotNull ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }
        
        /**
         * Uses the default block value provider with the current configuration.
         * 
         * @return this builder
         */
        @NotNull
        public Builder useDefaultBlockValueProvider() {
            if (configuration == null) {
                throw new IllegalStateException("Configuration must be set before using default block value provider");
            }
            this.blockValueProvider = new BlockValueProvider(configuration);
            return this;
        }
        
        /**
         * Builds the IslandLevelCalculator instance.
         * 
         * @return new IslandLevelCalculator instance
         * @throws IllegalStateException if required dependencies are not set
         */
        @NotNull
        public IslandLevelCalculator build() {
            if (configuration == null) {
                throw new IllegalStateException("Configuration is required");
            }
            if (blockValueProvider == null) {
                throw new IllegalStateException("Block value provider is required");
            }
            if (executorService == null) {
                throw new IllegalStateException("Executor service is required");
            }
            
            return new IslandLevelCalculator(configuration, blockValueProvider, executorService);
        }
    }
}