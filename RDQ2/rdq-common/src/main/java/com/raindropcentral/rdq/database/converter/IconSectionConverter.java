package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hibernate converter for {@link IconSection IconSection} objects that serializes and
 * deserializes section data to JSON for persistence.
 * <p>
 * This converter focuses on a minimal subset of {@link IconSection} state that is required
 * to rebuild icon metadata inside quest definitions. Each conversion lazily instantiates a new
 * {@link IconSection} backed by {@link EvaluationEnvironmentBuilder} to mirror how sections are
 * normally created at runtime.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class IconSectionConverter implements AttributeConverter<IconSection, String> {
    
    private static final Logger        LOGGER        = Logger.getLogger(IconSectionConverter.class.getName());
    private static final ObjectMapper  OBJECT_MAPPER = new ObjectMapper();
    private static final ConverterTool CONVERTER_TOOL = new ConverterTool();
    
    /**
     * Converts an {@link IconSection} to its JSON representation for database persistence.
     *
     * @param iconSection the {@link IconSection} to convert; may be {@code null}
     *
     * @return JSON representation of the section, or {@code null} when the input is {@code null}
     *
     * @throws RuntimeException if serialization fails because Jackson cannot process the section
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
        } catch (final JsonProcessingException e) {
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
     * Converts a JSON payload from the database back into an {@link IconSection} instance.
     *
     * @param jsonString the JSON payload produced by {@link #convertToDatabaseColumn(IconSection)};
     *                   blank or {@code null} values yield a default {@link IconSection}
     *
     * @return reconstructed {@link IconSection} backed by a fresh
     *         {@link EvaluationEnvironmentBuilder}
     *
     * @throws RuntimeException if the payload cannot be deserialized into a valid section snapshot
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
     * Data transfer object representing the persistable properties of an {@link IconSection}.
     * The {@link ObjectMapper} uses this structure to serialize and deserialize quest icon
     * metadata cleanly without exposing the internal state of the {@link IconSection} itself.
     */
    private static class IconSectionData {
        
        public String material;
        public String displayNameKey;
        public String descriptionKey;
        
        /**
         * Default constructor required by Jackson during deserialization.
         */
        public IconSectionData() {}
        
        /**
         * Constructs a snapshot representing an {@link IconSection IconSection's} persisted state.
         *
         * @param material       the Bukkit material identifier backing the icon
         * @param displayNameKey the translation key for the display name
         * @param descriptionKey the translation key for the description tooltip
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