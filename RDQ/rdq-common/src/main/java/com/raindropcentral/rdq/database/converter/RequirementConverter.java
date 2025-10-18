package com.raindropcentral.rdq.database.converter;


import com.raindropcentral.rdq.database.json.requirement.RequirementParser;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JPA {@link AttributeConverter} implementation for converting {@link AbstractRequirement}
 * objects to and from their JSON string representations for database storage.
 * <p>
 * This converter ensures that complex requirement objects, including those with Bukkit-specific
 * fields or polymorphic types, can be persisted and reconstructed reliably using Jackson-based
 * serialization provided by {@link RequirementParser}.
 * </p>
 *
 * <ul>
 *   <li>When saving an entity, {@link #convertToDatabaseColumn(AbstractRequirement)} serializes the requirement to JSON.</li>
 *   <li>When loading an entity, {@link #convertToEntityAttribute(String)} deserializes the JSON back to an {@code AbstractRequirement}.</li>
 * </ul>
 *
 * <p>
 * Any serialization or deserialization errors are logged and rethrown as unchecked exceptions,
 * preventing silent data corruption.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Converter(autoApply = true)
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {

    /**
     * SLF4J logger for error reporting during (de)serialization.
     */
    private static final Logger logger = LoggerFactory.getLogger(RequirementConverter.class);

    /**
     * Converts an {@link AbstractRequirement} object to its JSON string representation for database storage.
     * <p>
     * If the input is {@code null}, {@code null} is returned to represent a missing requirement.
     * Otherwise, the requirement is serialized using {@link RequirementParser#serialize(AbstractRequirement)}.
     * </p>
     *
     * @param attribute the {@code AbstractRequirement} instance to convert (may be {@code null})
     * @return the JSON string representation of the requirement, or {@code null} if the input is {@code null}
     * @throws RuntimeException if serialization fails due to an {@link IOException}
     */
    @Override
    public String convertToDatabaseColumn(AbstractRequirement attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return RequirementParser.serialize(attribute);
        } catch (IOException e) {
            logger.error("Failed to serialize requirement: {}", attribute, e);
            throw new RuntimeException("Failed to serialize requirement", e);
        }
    }

    /**
     * Converts a JSON string from the database back into an {@link AbstractRequirement} object.
     * <p>
     * If the input is {@code null}, {@code null} is returned to represent a missing requirement.
     * Otherwise, the JSON is deserialized using {@link RequirementParser#parse(String)}.
     * </p>
     *
     * @param dbData the JSON string from the database (may be {@code null})
     * @return the deserialized {@code AbstractRequirement} instance, or {@code null} if the input is {@code null}
     * @throws RuntimeException if deserialization fails due to an {@link IOException}
     */
    @Override
    public AbstractRequirement convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return RequirementParser.parse(dbData);
        } catch (IOException e) {
            logger.error("Failed to deserialize requirement from database string: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize requirement", e);
        }
    }
}
