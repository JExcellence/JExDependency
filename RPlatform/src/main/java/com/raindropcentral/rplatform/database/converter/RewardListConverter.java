/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.database.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.json.RewardParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the RewardListConverter API type.
 */
@Converter
public class RewardListConverter implements AttributeConverter<List<AbstractReward>, String> {

    /**
     * Executes convertToDatabaseColumn.
     */
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

    /**
     * Executes convertToEntityAttribute.
     */
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
