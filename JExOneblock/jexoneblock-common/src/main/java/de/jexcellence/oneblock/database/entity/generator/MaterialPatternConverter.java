package de.jexcellence.oneblock.database.entity.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JPA converter for serializing Material[][] patterns to JSON.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Converter
public class MaterialPatternConverter implements AttributeConverter<Material[][], String> {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable Material[][] pattern) {
        if (pattern == null) {
            return null;
        }
        
        try {
            String[][] stringPattern = new String[pattern.length][];
            for (int i = 0; i < pattern.length; i++) {
                stringPattern[i] = new String[pattern[i].length];
                for (int j = 0; j < pattern[i].length; j++) {
                    stringPattern[i][j] = pattern[i][j] != null ? pattern[i][j].name() : "AIR";
                }
            }
            return MAPPER.writeValueAsString(stringPattern);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize material pattern", e);
            throw new RuntimeException("Failed to serialize material pattern", e);
        }
    }
    
    @Override
    @Nullable
    public Material[][] convertToEntityAttribute(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            String[][] stringPattern = MAPPER.readValue(json, String[][].class);
            Material[][] pattern = new Material[stringPattern.length][];
            for (int i = 0; i < stringPattern.length; i++) {
                pattern[i] = new Material[stringPattern[i].length];
                for (int j = 0; j < stringPattern[i].length; j++) {
                    try {
                        pattern[i][j] = Material.valueOf(stringPattern[i][j]);
                    } catch (IllegalArgumentException e) {
                        pattern[i][j] = Material.AIR;
                    }
                }
            }
            return pattern;
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize material pattern", e);
            throw new RuntimeException("Failed to deserialize material pattern", e);
        }
    }
}
