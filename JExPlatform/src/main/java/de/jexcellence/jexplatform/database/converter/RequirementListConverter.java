package de.jexcellence.jexplatform.database.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JPA converter for {@link List lists} of {@link AbstractRequirement}
 * stored as a JSON array.
 *
 * <p>Shares the ObjectMapper configured via {@link RequirementConverter}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class RequirementListConverter
        implements AttributeConverter<List<AbstractRequirement>, String> {

    private static final TypeReference<List<AbstractRequirement>> TYPE_REF =
            new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(@Nullable List<AbstractRequirement> requirements) {
        if (requirements == null) {
            return null;
        }
        try {
            return getMapper().writeValueAsString(requirements);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize requirement list", e);
        }
    }

    @Override
    public List<AbstractRequirement> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return getMapper().readValue(columnValue, TYPE_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize requirement list", e);
        }
    }

    private static ObjectMapper getMapper() {
        // Reuse the same mapper configured for RequirementConverter
        try {
            var field = RequirementConverter.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            var mapper = (ObjectMapper) field.get(null);
            if (mapper == null) {
                throw new IllegalStateException(
                        "RequirementConverter ObjectMapper not configured.");
            }
            return mapper;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot access RequirementConverter mapper", e);
        }
    }
}
