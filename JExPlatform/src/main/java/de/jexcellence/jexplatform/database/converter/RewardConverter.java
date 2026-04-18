package de.jexcellence.jexplatform.database.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.jexplatform.reward.AbstractReward;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

/**
 * JPA converter for single {@link AbstractReward} instances stored as JSON.
 *
 * <p>Requires a configured {@link ObjectMapper} with reward subtypes
 * registered. Call {@link #setObjectMapper(ObjectMapper)} before use.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class RewardConverter implements AttributeConverter<AbstractReward, String> {

    private static volatile ObjectMapper objectMapper;

    /**
     * Configures the shared ObjectMapper.
     *
     * @param mapper the mapper with reward types registered
     */
    public static void setObjectMapper(@Nullable ObjectMapper mapper) {
        objectMapper = mapper;
    }

    @Override
    public String convertToDatabaseColumn(@Nullable AbstractReward reward) {
        if (reward == null) {
            return null;
        }
        try {
            return getMapper().writeValueAsString(reward);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize reward", e);
        }
    }

    @Override
    public AbstractReward convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return getMapper().readValue(columnValue, AbstractReward.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize reward from: " + columnValue, e);
        }
    }

    private static ObjectMapper getMapper() {
        if (objectMapper == null) {
            throw new IllegalStateException(
                    "RewardConverter ObjectMapper not configured. "
                            + "Call RewardConverter.setObjectMapper() first.");
        }
        return objectMapper;
    }
}
