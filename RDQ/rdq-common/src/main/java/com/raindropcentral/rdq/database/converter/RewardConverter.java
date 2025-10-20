package com.raindropcentral.rdq.database.converter;

import com.raindropcentral.rdq.database.json.reward.RewardParser;
import com.raindropcentral.rdq.reward.AbstractReward;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JPA {@link AttributeConverter} for converting {@link AbstractReward} objects to and from
 * their JSON string representations so rewards can be persisted transparently.
 * <p>
 * Complex reward hierarchies, including Bukkit-specific subtypes, are serialized and
 * deserialized via the Jackson-backed {@link RewardParser}. Centralizing that logic ensures
 * database rows remain compatible when new reward implementations are introduced.
 * </p>
 * <ul>
 *   <li>{@link #convertToDatabaseColumn(AbstractReward)} serializes rewards prior to persistence.</li>
 *   <li>{@link #convertToEntityAttribute(String)} recreates rewards when entities are loaded.</li>
 * </ul>
 * <p>
 * Serialization errors are logged and surfaced as unchecked exceptions so that data corruption
 * never fails silently during JPA operations.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class RewardConverter implements AttributeConverter<AbstractReward, String> {

    /**
     * SLF4J logger for error reporting during serialization and deserialization steps.
     */
    private static final Logger logger = LoggerFactory.getLogger(RewardConverter.class);

    /**
     * Converts an {@link AbstractReward} object to its JSON string representation for database storage.
     * <p>
     * {@code null} inputs are returned as {@code null} so missing rewards remain absent in the column;
     * otherwise the reward is serialized via {@link RewardParser#serialize(AbstractReward)}.
     * </p>
     *
     * @param attribute the {@code AbstractReward} instance to convert (may be {@code null})
     *
     * @return the JSON string representation of the reward, or {@code null} if the input is {@code null}
     *
     * @throws RuntimeException if serialization fails due to an {@link IOException}
     */
    @Override
    public String convertToDatabaseColumn(AbstractReward attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return RewardParser.serialize(attribute);
        } catch (IOException e) {
            logger.error(
                "Failed to serialize reward: {}",
                attribute,
                e
            );
            throw new RuntimeException(
                "Failed to serialize reward",
                e
            );
        }
    }

    /**
     * Converts a JSON string from the database back into an {@link AbstractReward} object.
     * <p>
     * {@code null} inputs are preserved so optional reward columns stay unset; otherwise the JSON is
     * deserialized via {@link RewardParser#parse(String)}.
     * </p>
     *
     * @param dbData the JSON string from the database (may be {@code null})
     *
     * @return the deserialized {@code AbstractReward} instance, or {@code null} if the input is {@code null}
     *
     * @throws RuntimeException if deserialization fails due to an {@link IOException}
     */
    @Override
    public AbstractReward convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return RewardParser.parse(dbData);
        } catch (IOException e) {
            logger.error(
                "Failed to deserialize reward from database string: {}",
                dbData,
                e
            );
            throw new RuntimeException(
                "Failed to deserialize reward",
                e
            );
        }
    }
}
