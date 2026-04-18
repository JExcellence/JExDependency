package de.jexcellence.jexplatform.integration.skill;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared reflection helpers for skill bridges.
 *
 * @author JExcellence
 * @since 1.0.0
 */
abstract class AbstractReflectionSkillBridge {

    /** The platform logger. */
    protected final JExLogger logger;

    /**
     * Creates the reflection bridge.
     *
     * @param logger the platform logger
     */
    protected AbstractReflectionSkillBridge(@NotNull JExLogger logger) {
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

    /**
     * Resolves an enum constant by name (case-insensitive).
     *
     * @param enumClass the enum class
     * @param name      the constant name
     * @param <E>       the enum type
     * @return the constant, or {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    protected static <E extends Enum<E>> E resolveEnum(@NotNull Class<?> enumClass,
                                                       @NotNull String name) {
        try {
            return Enum.valueOf((Class<E>) enumClass, name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
