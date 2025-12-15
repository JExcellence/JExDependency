package com.raindropcentral.rdq.database.converter;

import com.raindropcentral.rdq.config.utility.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hibernate converter for {@link IconSection} objects.
 * Converts IconSection instances to/from JSON strings for database storage.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Converter(autoApply = true)
public class IconSectionConverter implements AttributeConverter<IconSection, String> {
    
    private static final Logger        LOGGER        = Logger.getLogger(IconSectionConverter.class.getName());
    private static final ObjectMapper  OBJECT_MAPPER = new ObjectMapper();
    private static final ConverterTool CONVERTER_TOOL = new ConverterTool();
    
    /**
     * Converts an IconSection to its JSON string representation for database storage.
     *
     * @param iconSection the IconSection to convert
     *
     * @return JSON string representation, or null if input is null
     */
    @Override
    public String convertToDatabaseColumn(@Nullable final IconSection iconSection) {
        
        if (iconSection == null) {
            return null;
        }
        
        try {
            final IconSectionData data = new IconSectionData(
                iconSection.getMaterial(),
                iconSection.getDisplayNameKey(),
                iconSection.getDescriptionKey()
            );
            
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (final JacksonException e) {
            LOGGER.log(
                Level.SEVERE,
                "Failed to convert IconSection to JSON",
                e
            );
            throw new RuntimeException(
                "Failed to serialize IconSection",
                e
            );
        }
    }
    
    /**
     * Converts a JSON string back to an IconSection instance.
     *
     * @param jsonString the JSON string from database
     *
     * @return reconstructed IconSection, or null if input is null
     */
    @Override
    public IconSection convertToEntityAttribute(
        @Nullable final String jsonString
    ) {
        
        if (
            jsonString == null ||
            jsonString.trim().isEmpty()
        ) {
            return new IconSection(new EvaluationEnvironmentBuilder());
        }
        
        try {
            final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
            final IconSectionData data = OBJECT_MAPPER.treeToValue(
                jsonNode,
                IconSectionData.class
            );
            final IconSection iconSection = new IconSection(new EvaluationEnvironmentBuilder());
            
            try {
                Material material = Material.valueOf(data.material);
                CONVERTER_TOOL.setPrivateField(
                    iconSection,
                    "type",
                    data.material,
                    LOGGER
                );
            } catch (
                  final Exception exception
            ) {
                LOGGER.warning("Failed to convert IconSection to Material: " + exception.getMessage());
                CONVERTER_TOOL.setPrivateField(
                    iconSection,
                    "type",
                    "BARRIER",
                    LOGGER
                );
            }
            
            CONVERTER_TOOL.setPrivateField(
                iconSection,
                "displayNameKey",
                data.displayNameKey,
                LOGGER
            );
            CONVERTER_TOOL.setPrivateField(
                iconSection,
                "descriptionKey",
                data.descriptionKey,
                LOGGER
            );
            
            return iconSection;
        } catch (
            final Exception exception
        ) {
            LOGGER.log(
                Level.SEVERE,
                "Failed to convert JSON to IconSection: " + jsonString,
                exception
            );
            throw new RuntimeException(
                "Failed to deserialize IconSection",
                exception
            );
        }
    }
    
    /**
     * Data transfer object for JSON serialization/deserialization.
     * Contains the essential data from IconSection that needs to be persisted.
     */
    private static class IconSectionData {
        
        public String material;
        public String displayNameKey;
        public String descriptionKey;
        
        /**
         * Default constructor for Jackson deserialization.
         */
        public IconSectionData() {}
        
        /**
         * Constructor for creating data object from IconSection.
         *
         * @param material       the material type
         * @param displayNameKey the display name localization key
         * @param descriptionKey the description localization key
         */
        public IconSectionData(
           final @Nullable String material,
           final @Nullable String displayNameKey,
           final @Nullable String descriptionKey
        ) {
            
            this.material = material;
            this.displayNameKey = displayNameKey;
            this.descriptionKey = descriptionKey;
        }
        
    }
    
}