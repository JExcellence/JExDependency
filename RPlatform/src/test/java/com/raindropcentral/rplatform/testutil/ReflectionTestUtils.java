package com.raindropcentral.rplatform.testutil;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Small reflection helper for assigning private test fixture state.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public final class ReflectionTestUtils {

    private ReflectionTestUtils() {
    }

    /**
     * Sets a private field value, searching superclasses when required.
     *
     * @param target target object
     * @param fieldName field to update
     * @param value replacement value
     */
    public static void setField(
        final @NotNull Object target,
        final @NotNull String fieldName,
        final Object value
    ) {
        try {
            final Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(
                "Failed to set field '" + fieldName + "' on " + target.getClass().getName(),
                exception
            );
        }
    }

    private static @NotNull Field findField(
        final @NotNull Class<?> type,
        final @NotNull String fieldName
    ) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
