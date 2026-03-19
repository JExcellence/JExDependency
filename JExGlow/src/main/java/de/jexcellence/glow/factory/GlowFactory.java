package de.jexcellence.glow.factory;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.glow.database.repository.GlowRepository;
import de.jexcellence.glow.service.GlowService;
import de.jexcellence.glow.service.IGlowService;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Factory class for centralized access to glow service and repository instances.
 * <p>
 * Provides a singleton pattern for accessing the glow service and repository,
 * following the HomeFactory pattern from JExHome.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class GlowFactory {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("GlowFactory");

    private static GlowFactory instance;

    private final IGlowService glowService;
    private final GlowRepository glowRepository;

    private GlowFactory(@NotNull IGlowService glowService, @NotNull GlowRepository glowRepository) {
        this.glowService = glowService;
        this.glowRepository = glowRepository;
    }

    /**
     * Initializes the GlowFactory singleton.
     *
     * @param glowService    the glow service implementation
     * @param glowRepository the glow repository
     * @return the initialized factory instance
     */
    public static GlowFactory initialize(@NotNull IGlowService glowService, @NotNull GlowRepository glowRepository) {
        if (instance == null) {
            instance = new GlowFactory(glowService, glowRepository);
            LOGGER.info("GlowFactory initialized");
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     *
     * @return the factory instance
     * @throws IllegalStateException if not initialized
     */
    public static GlowFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GlowFactory not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Gets the glow service instance.
     *
     * @return the glow service
     */
    public static IGlowService getService() {
        return getInstance().glowService;
    }

    /**
     * Gets the glow repository instance.
     *
     * @return the glow repository
     */
    public static GlowRepository getRepository() {
        return getInstance().glowRepository;
    }

    /**
     * Resets the singleton instance (for testing).
     */
    public static void reset() {
        if (instance != null) {
            LOGGER.info("GlowFactory reset");
            instance = null;
        }
    }
}
