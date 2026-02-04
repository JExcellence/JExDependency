package com.raindropcentral.rplatform.database.converter;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementRegistry;
import com.raindropcentral.rplatform.requirement.json.RequirementParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JPA {@link AttributeConverter} for converting {@link AbstractRequirement} objects
 * to and from their JSON string representations for database storage.
 * <p>
 * This converter uses {@link RequirementParser} for serialization, which supports:
 * <ul>
 *   <li>Polymorphic requirement types via {@link com.raindropcentral.rplatform.requirement.json.RequirementMixin}</li>
 *   <li>Plugin-registered custom requirement types via {@link RequirementRegistry}</li>
 *   <li>Bukkit-specific types like ItemStack</li>
 * </ul>
 * </p>
 */
@Converter(autoApply = true)
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequirementConverter.class);

    /**
     * Converts an {@link AbstractRequirement} to its JSON string representation.
     *
     * @param attribute the requirement to convert (may be null)
     * @return the JSON string, or null if input is null
     * @throws RuntimeException if serialization fails
     */
    @Override
    public String convertToDatabaseColumn(AbstractRequirement attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return RequirementParser.serialize(attribute);
        } catch (IOException e) {
            LOGGER.error("Failed to serialize requirement: {}", attribute, e);
            throw new RuntimeException("Failed to serialize requirement", e);
        }
    }

    /**
     * Converts a JSON string from the database back into an {@link AbstractRequirement}.
     *
     * @param dbData the JSON string from the database (may be null)
     * @return the deserialized requirement, or null if input is null
     * @throws RuntimeException if deserialization fails
     */
    @Override
    public AbstractRequirement convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return RequirementParser.parse(dbData);
        } catch (IOException e) {
            LOGGER.error("Failed to deserialize requirement from JSON: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize requirement", e);
        }
    }
}
