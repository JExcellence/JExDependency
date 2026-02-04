package com.raindropcentral.rplatform.database.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.json.RewardParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class RewardListConverter implements AttributeConverter<List<AbstractReward>, String> {

    @Override
    public String convertToDatabaseColumn(List<AbstractReward> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return null;
        }
        try {
            return RewardParser.getObjectMapper().writeValueAsString(requirements);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reward list", e);
        }
    }

    @Override
    public List<AbstractReward> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return RewardParser.getObjectMapper().readValue(
                dbData,
                new TypeReference<List<AbstractReward>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize reward list", e);
        }
    }
}
