package com.raindropcentral.rplatform.database.converter;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.json.RewardParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RewardConverter implements AttributeConverter<AbstractReward, String> {

    @Override
    public String convertToDatabaseColumn(AbstractReward attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return RewardParser.serialize(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reward", e);
        }
    }

    @Override
    public AbstractReward convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return RewardParser.parse(dbData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize reward", e);
        }
    }
}
