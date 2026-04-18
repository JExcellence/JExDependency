package de.jexcellence.jexplatform.database.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jexcellence.jexplatform.reward.AbstractReward;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JPA converter for {@link List lists} of {@link AbstractReward}
 * stored as a JSON array.
 *
 * <p>Shares the ObjectMapper configured via {@link RewardConverter}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = false)
public class RewardListConverter
        implements AttributeConverter<List<AbstractReward>, String> {

    private static final TypeReference<List<AbstractReward>> TYPE_REF =
            new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(@Nullable List<AbstractReward> rewards) {
        if (rewards == null) {
            return null;
        }
        try {
            return getMapper().writeValueAsString(rewards);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize reward list", e);
        }
    }

    @Override
    public List<AbstractReward> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return getMapper().readValue(columnValue, TYPE_REF);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to deserialize reward list", e);
        }
    }

    private static ObjectMapper getMapper() {
        // Reuse the same mapper configured for RewardConverter
        try {
            var field = RewardConverter.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            var mapper = (ObjectMapper) field.get(null);
            if (mapper == null) {
                throw new IllegalStateException(
                        "RewardConverter ObjectMapper not configured.");
            }
            return mapper;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot access RewardConverter mapper", e);
        }
    }
}
