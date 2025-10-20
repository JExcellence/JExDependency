package com.raindropcentral.rdq.database.converter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for interacting with private fields using reflection.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class ConverterTool {

    /**
     * Sets a private field value using reflection.
     *
     * @param target    the object whose field should be modified
     * @param fieldName the name of the field to update
     * @param value     the value that should be assigned to the field
     * @param logger    the logger used to report failures when setting the field
     * @throws RuntimeException if the field cannot be accessed or modified
     */
    public void setPrivateField(
            @NotNull final Object target,
            @NotNull final String fieldName,
            @Nullable final Object value,
            @NotNull final Logger logger
    ) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            logger.log(Level.SEVERE, "Failed to set field: " + fieldName, e);
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
