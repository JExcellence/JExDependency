package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * JPA attribute converter for mapping {@link UUID} to String and back.
 *
 * Behavior:
 * - null UUID -> null column
 * - null/blank column -> null UUID
 */
@Converter(autoApply = true)
public class UUIDConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable final UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    @Override
    public UUID convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(columnValue.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid UUID string in column: '" + columnValue + "'", ex);
        }
    }
}