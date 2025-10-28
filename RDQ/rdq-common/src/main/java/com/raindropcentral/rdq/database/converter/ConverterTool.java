package com.raindropcentral.rdq.database.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small reflective helper used by JPA converters to access non-public fields
 * of section/config objects during JSON serialization/deserialization.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.2
 */
public class ConverterTool {

    /**
     * Writes a value into a private or protected field on the given instance.
     *
     * @param instance the target object
     * @param fieldName the field name to set
     * @param value the value to assign
     * @param logger the logger for error reporting
     */
    public void setPrivateField(
            final @NotNull Object instance,
            final @NotNull String fieldName,
            final @Nullable Object value,
            final @NotNull Logger logger
    ) {
        try {
            final Field field = locateField(instance.getClass(), fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (final Exception ex) {
            logger.log(Level.FINE, "Failed to write private field '" + fieldName + "' on " + instance.getClass().getName(), ex);
        }
    }

    private static Field locateField(final Class<?> type, final String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Retrieves the value of a private field using reflection.
     *
     * @param target    the object whose field should be read
     * @param fieldName the name of the field to access
     * @param logger    the logger used to report failures when reading the field
     * @return the field value
     * @throws RuntimeException if the field cannot be accessed
     */
    @SuppressWarnings("unchecked")
    public <T> T getPrivateField(
            @NotNull final Object target,
            @NotNull final String fieldName,
            @NotNull final Logger logger
    ) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            logger.log(Level.SEVERE, "Failed to access field: " + fieldName, e);
            throw new RuntimeException("Failed to access field: " + fieldName, e);
        }
    }
}
