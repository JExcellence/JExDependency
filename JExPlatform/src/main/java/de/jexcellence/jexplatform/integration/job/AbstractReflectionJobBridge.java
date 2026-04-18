package de.jexcellence.jexplatform.integration.job;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared reflection helpers for job bridges.
 *
 * @author JExcellence
 * @since 1.0.0
 */
abstract class AbstractReflectionJobBridge {

    /** The platform logger. */
    protected final JExLogger logger;

    /**
     * Creates the reflection bridge.
     *
     * @param logger the platform logger
     */
    protected AbstractReflectionJobBridge(@NotNull JExLogger logger) {
        this.logger = logger;
    }

    /**
     * Loads a class by name, returning {@code null} if not found.
     *
     * @param className the fully qualified class name
     * @return the class, or {@code null}
     */
    @Nullable
    protected static Class<?> findClass(@NotNull String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
