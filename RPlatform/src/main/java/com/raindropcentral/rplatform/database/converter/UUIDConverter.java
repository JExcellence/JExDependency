package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Converts {@link UUID} attributes to their canonical string representation for persistence and back again.
 *
 * <p>{@code null} UUID attributes produce {@code null} column values, and blank column values are treated as
 * {@code null} attributes. Invalid UUID strings raise an {@link IllegalArgumentException} during hydration to
 * ensure the application does not operate on corrupted identifiers.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class UUIDConverter implements AttributeConverter<UUID, String> {

    /**
     * Serialises the provided {@link UUID} to its string form.
     *
     * @param uuid the UUID targeted for persistence; may be {@code null}
     * @return the canonical string or {@code null} when the UUID is {@code null}
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    /**
     * Reconstructs a {@link UUID} from its stored canonical string form.
     *
     * @param columnValue the raw database value; blank and {@code null} values return {@code null}
     * @return the decoded UUID, or {@code null} when the column value is blank
     * @throws IllegalArgumentException when the stored string is not a valid UUID
     */
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