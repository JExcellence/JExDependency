package de.jexcellence.jexplatform.integration.protection;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Shared reflection helpers for protection bridges.
 *
 * @author JExcellence
 * @since 1.0.0
 */
abstract class AbstractReflectionProtectionBridge {

    /** The platform logger. */
    protected final JExLogger logger;

    /**
     * Creates the reflection bridge.
     *
     * @param logger the platform logger
     */
    protected AbstractReflectionProtectionBridge(@NotNull JExLogger logger) {
        this.logger = logger;
    }

    /**
     * Finds a method via reflection.
     *
     * @param clazz      the class
     * @param methodName the method name
     * @param paramTypes the parameter types
     * @return the method, or {@code null}
     */
    @Nullable
    protected static Method findMethod(@NotNull Class<?> clazz, @NotNull String methodName,
                                       Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Invokes a method, returning the result or {@code null} on error.
     *
     * @param method   the method
     * @param instance the target instance
     * @param args     the arguments
     * @return the result, or {@code null}
     */
    @Nullable
    protected Object invokeQuietly(@NotNull Method method, @Nullable Object instance,
                                   Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (Exception e) {
            logger.debug("Reflection call failed: {}.{} — {}",
                    method.getDeclaringClass().getSimpleName(), method.getName(),
                    e.getMessage());
            return null;
        }
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
