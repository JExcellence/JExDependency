package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * JPA converter that persists {@link UUID} attributes as their canonical
 * string representation.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class UUIDConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(@Nullable UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    @Override
    public UUID convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(columnValue.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid UUID string in column: '" + columnValue + "'", ex);
        }
    }
}
