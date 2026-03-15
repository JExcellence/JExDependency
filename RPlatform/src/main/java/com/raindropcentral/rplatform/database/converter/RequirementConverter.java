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
 * JPA {@link AttributeConverter} for converting {@link AbstractRequirement} objects.
 * to and from their JSON string representations for database storage.
 *
 * <p>This converter uses {@link RequirementParser} for serialization, which supports:
 * <ul>
 *   <li>Polymorphic requirement types via {@link com.raindropcentral.rplatform.requirement.json.RequirementMixin}</li>
 *   <li>Plugin-registered custom requirement types via {@link RequirementRegistry}</li>
 *   <li>Bukkit-specific types like ItemStack</li>
 * </ul>
 */
@Converter(autoApply = true)
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequirementConverter.class);
    
    // Lazy initialization flag to avoid triggering RequirementParser during class loading
    private static volatile boolean parserReady = false;

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
            ensureParserReady();
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
            ensureParserReady();
            // Migrate old format to new format if needed
            String migratedJson = migrateOldFormat(dbData);
            return RequirementParser.parse(migratedJson);
        } catch (IOException e) {
            LOGGER.error("Failed to deserialize requirement from JSON: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize requirement", e);
        }
    }
    
    /**
     * Ensures the RequirementParser is ready before use.
     * This prevents triggering parser initialization during entity class loading.
     */
    private void ensureParserReady() {
        if (!parserReady) {
            synchronized (RequirementConverter.class) {
                if (!parserReady) {
                    // Trigger parser initialization
                    RequirementParser.getObjectMapper();
                    parserReady = true;
                }
            }
        }
    }

    /**
     * Migrates old requirement JSON format (class names) to new format (type IDs).
     * 
     * @param json the JSON string to migrate
     * @return the migrated JSON string
     */
    private String migrateOldFormat(String json) {
        // Check if migration is needed
        if (!json.contains("Requirement\"")) {
            return json;
        }
        
        // Migrate class names to type IDs
        return json
            .replace("\"type\":\"ItemRequirement\"", "\"type\":\"ITEM\"")
            .replace("\"type\":\"CurrencyRequirement\"", "\"type\":\"CURRENCY\"")
            .replace("\"type\":\"ExperienceLevelRequirement\"", "\"type\":\"EXPERIENCE_LEVEL\"")
            .replace("\"type\":\"PermissionRequirement\"", "\"type\":\"PERMISSION\"")
            .replace("\"type\":\"LocationRequirement\"", "\"type\":\"LOCATION\"")
            .replace("\"type\":\"PlaytimeRequirement\"", "\"type\":\"PLAYTIME\"")
            .replace("\"type\":\"CompositeRequirement\"", "\"type\":\"COMPOSITE\"")
            .replace("\"type\":\"ChoiceRequirement\"", "\"type\":\"CHOICE\"");
    }
}
