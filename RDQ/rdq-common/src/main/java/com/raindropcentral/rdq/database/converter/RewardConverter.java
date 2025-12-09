package com.raindropcentral.rdq.database.converter;

import com.raindropcentral.rdq.json.reward.RewardParser;
import com.raindropcentral.rdq.reward.AbstractReward;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JPA {@link jakarta.persistence.AttributeConverter} implementation for converting {@link com.raindropcentral.rdq2.reward.AbstractReward}
 * objects to and from their JSON string representations for database storage.
 * <p>
 * This converter ensures that complex requirement objects, including those with Bukkit-specific
 * fields or polymorphic types, can be persisted and reconstructed reliably using Jackson-based
 * serialization provided by {@link com.raindropcentral.rdq2.json.reward.RewardParser}.
 * </p>
 *
 * <ul>
 *   <li>When saving an entity, {@link #convertToDatabaseColumn(com.raindropcentral.rdq2.reward.AbstractReward)} serializes the requirement to JSON.</li>
 *   <li>When loading an entity, {@link #convertToEntityAttribute(String)} deserializes the JSON back to an {@code AbstractReward}.</li>
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
public class RewardConverter implements AttributeConverter<AbstractReward, String> {
	
	/**
	 * SLF4J logger for error reporting during (de)serialization.
	 */
	private static final Logger logger = LoggerFactory.getLogger(RewardConverter.class);
	
	/**
	 * Converts an {@link com.raindropcentral.rdq2.reward.AbstractReward} object to its JSON string representation for database storage.
	 * <p>
	 * If the input is {@code null}, {@code null} is returned to represent a missing reward.
	 * Otherwise, the requirement is serialized using {@link com.raindropcentral.rdq2.json.reward.RewardParser#serialize(com.raindropcentral.rdq2.reward.AbstractReward)}.
	 * </p>
	 *
	 * @param attribute the {@code AbstractReward} instance to convert (may be {@code null})
	 *
	 * @return the JSON string representation of the requirement, or {@code null} if the input is {@code null}
	 *
	 * @throws RuntimeException if serialization fails due to an {@link java.io.IOException}
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
				"Failed to serialize requirement: {}",
				attribute,
				e
			);
			throw new RuntimeException(
				"Failed to serialize requirement",
				e
			);
		}
	}
	
	/**
	 * Converts a JSON string from the database back into an {@link com.raindropcentral.rdq2.reward.AbstractReward} object.
	 * <p>
	 * If the input is {@code null}, {@code null} is returned to represent a missing reward.
	 * Otherwise, the JSON is deserialized using {@link com.raindropcentral.rdq2.json.reward.RewardParser#parse(String)}.
	 * </p>
	 *
	 * @param dbData the JSON string from the database (may be {@code null})
	 *
	 * @return the deserialized {@code AbstractReward} instance, or {@code null} if the input is {@code null}
	 *
	 * @throws RuntimeException if deserialization fails due to an {@link java.io.IOException}
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
				"Failed to deserialize requirement from database string: {}",
				dbData,
				e
			);
			throw new RuntimeException(
				"Failed to deserialize requirement",
				e
			);
		}
	}
}
