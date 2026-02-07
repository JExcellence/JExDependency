package com.raindropcentral.rplatform.database.converter;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.json.RewardParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA {@link AttributeConverter} for converting {@link AbstractReward} objects
 * to and from their JSON string representations for database storage.
 * <p>
 * This converter uses {@link RewardParser} for serialization with support for
 * Bukkit ItemStack serialization.
 * </p>
 */
@Converter
public class RewardConverter implements AttributeConverter<AbstractReward, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewardConverter.class);
    
    // Lazy initialization flag to avoid triggering RewardParser during class loading
    private static volatile boolean parserReady = false;

    @Override
    public String convertToDatabaseColumn(AbstractReward attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            ensureParserReady();
            return RewardParser.serialize(attribute);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize reward: {}", attribute, e);
            throw new RuntimeException("Failed to serialize reward", e);
        }
    }

    @Override
    public AbstractReward convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            ensureParserReady();
            return RewardParser.parse(dbData);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize reward from JSON: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize reward", e);
        }
    }
    
    /**
     * Ensures the RewardParser is ready before use.
     * This prevents triggering parser initialization during entity class loading.
     */
    private void ensureParserReady() {
        if (!parserReady) {
            synchronized (RewardConverter.class) {
                if (!parserReady) {
                    // Trigger parser initialization
                    RewardParser.getObjectMapper();
                    parserReady = true;
                }
            }
        }
    }
}
