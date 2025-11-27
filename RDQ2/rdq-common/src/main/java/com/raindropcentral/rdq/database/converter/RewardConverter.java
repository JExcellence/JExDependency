package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.reward.AbstractReward;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public final class RewardConverter implements AttributeConverter<AbstractReward, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AbstractReward reward) {
        if (reward == null) return null;
        try {
            return MAPPER.writeValueAsString(reward);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize reward", e);
        }
    }

    @Override
    public AbstractReward convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, AbstractReward.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize reward", e);
        }
    }
}
