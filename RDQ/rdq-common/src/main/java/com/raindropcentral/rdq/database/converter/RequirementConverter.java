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

package com.raindropcentral.rdq.database.converter;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.json.RequirementParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JPA {@link AttributeConverter} for converting {@link AbstractRequirement} objects.
 * to and from their JSON string representations for database storage.
 *
 * <p>This converter delegates to RPlatform's {@link RequirementParser} for serialization.
 * Uses lazy initialization to avoid triggering RequirementParser during entity class loading.
 */
@Converter(autoApply = true)
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequirementConverter.class);
    
    // Lazy initialization flag to avoid triggering RequirementParser during class loading
    private static volatile boolean parserReady = false;

    /**
     * Executes convertToDatabaseColumn.
     */
    @Override
    public String convertToDatabaseColumn(AbstractRequirement attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            ensureParserReady();
            return RequirementParser.serialize(attribute);
        } catch (IOException e) {
            LOGGER.error("Failed to serialize requirement: {}", attribute, e);
            throw new RuntimeException("Failed to serialize requirement", e);
        }
    }

    /**
     * Executes convertToEntityAttribute.
     */
    @Override
    public AbstractRequirement convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            ensureParserReady();
            // Migrate old format to new format if needed
            String migratedJson = migrateOldFormat(dbData);
            return RequirementParser.parse(migratedJson);
        } catch (IOException e) {
            LOGGER.error("Failed to deserialize requirement from JSON: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize requirement", e);
        }
    }
    
    /**
     * Ensures the RequirementParser is ready before use.
     * This prevents triggering parser initialization during entity class loading.
     */
    private void ensureParserReady() {
        if (!parserReady) {
            synchronized (RequirementConverter.class) {
                if (!parserReady) {
                    // Trigger parser initialization
                    RequirementParser.getObjectMapper();
                    parserReady = true;
                }
            }
        }
    }

    /**
     * Migrates old requirement JSON format (class names) to new format (type IDs).
     * 
     * @param json the JSON string to migrate
     * @return the migrated JSON string
     */
    private String migrateOldFormat(String json) {
        // Check if migration is needed
        if (!json.contains("Requirement\"")) {
            return json;
        }
        
        // Migrate class names to type IDs
        return json
            .replace("\"type\":\"ItemRequirement\"", "\"type\":\"ITEM\"")
            .replace("\"type\":\"CurrencyRequirement\"", "\"type\":\"CURRENCY\"")
            .replace("\"type\":\"ExperienceLevelRequirement\"", "\"type\":\"EXPERIENCE_LEVEL\"")
            .replace("\"type\":\"PermissionRequirement\"", "\"type\":\"PERMISSION\"")
            .replace("\"type\":\"LocationRequirement\"", "\"type\":\"LOCATION\"")
            .replace("\"type\":\"PlaytimeRequirement\"", "\"type\":\"PLAYTIME\"")
            .replace("\"type\":\"CompositeRequirement\"", "\"type\":\"COMPOSITE\"")
            .replace("\"type\":\"ChoiceRequirement\"", "\"type\":\"CHOICE\"");
    }
}
