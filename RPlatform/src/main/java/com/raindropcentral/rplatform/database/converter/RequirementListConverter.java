package com.raindropcentral.rplatform.database.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.json.RequirementParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA {@link AttributeConverter} for converting lists of {@link AbstractRequirement} objects
 * to and from their JSON string representations for database storage.
 * <p>
 * Use this converter when an entity needs to store multiple requirements in a single column.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * @Column(name = "requirements", columnDefinition = "LONGTEXT")
 * @Convert(converter = RequirementListConverter.class)
 * private List<AbstractRequirement> requirements;
 * }</pre>
 */
@Converter
public class RequirementListConverter implements AttributeConverter<List<AbstractRequirement>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequirementListConverter.class);
    private static final TypeReference<List<AbstractRequirement>> LIST_TYPE = new TypeReference<>() {};

    /**
     * Converts a list of requirements to a JSON array string.
     *
     * @param requirements the list of requirements to convert (may be null or empty)
     * @return the JSON array string, or null if input is null
     * @throws RuntimeException if serialization fails
     */
    @Override
    public String convertToDatabaseColumn(List<AbstractRequirement> requirements) {
        if (requirements == null) {
            return null;
        }
        if (requirements.isEmpty()) {
            return "[]";
        }
        try {
            return RequirementParser.getObjectMapper().writeValueAsString(requirements);
        } catch (IOException e) {
            LOGGER.error("Failed to serialize requirement list", e);
            throw new RuntimeException("Failed to serialize requirement list", e);
        }
    }

    /**
     * Converts a JSON array string back into a list of requirements.
     *
     * @param dbData the JSON array string from the database (may be null or empty)
     * @return the deserialized list of requirements, or empty list if input is null/empty
     * @throws RuntimeException if deserialization fails
     */
    @Override
    public List<AbstractRequirement> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            return RequirementParser.getObjectMapper().readValue(dbData, LIST_TYPE);
        } catch (IOException e) {
            LOGGER.error("Failed to deserialize requirement list from JSON: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize requirement list", e);
        }
    }
}
