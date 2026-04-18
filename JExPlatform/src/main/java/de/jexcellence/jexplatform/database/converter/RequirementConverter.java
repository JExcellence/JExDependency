package de.jexcellence.jexplatform.database.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

/**
 * JPA converter for single {@link AbstractRequirement} instances stored as JSON.
 *
 * <p>Requires a configured {@link ObjectMapper} with requirement subtypes
 * registered. Call {@link #setObjectMapper(ObjectMapper)} before use.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {

    private static volatile ObjectMapper objectMapper;

    /**
     * Configures the shared ObjectMapper.
     *
     * @param mapper the mapper with requirement types registered
     */
    public static void setObjectMapper(@Nullable ObjectMapper mapper) {
        objectMapper = mapper;
    }

    @Override
    public String convertToDatabaseColumn(@Nullable AbstractRequirement requirement) {
        if (requirement == null) {
            return null;
        }
        try {
            return getMapper().writeValueAsString(requirement);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize requirement", e);
        }
    }

    @Override
    public AbstractRequirement convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return getMapper().readValue(columnValue, AbstractRequirement.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize requirement from: " + columnValue, e);
        }
    }

    private static ObjectMapper getMapper() {
        if (objectMapper == null) {
            throw new IllegalStateException(
                    "RequirementConverter ObjectMapper not configured. "
                            + "Call RequirementConverter.setObjectMapper() first.");
        }
        return objectMapper;
    }
}
