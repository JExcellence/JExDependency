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

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.json.RewardParser;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA {@link AttributeConverter} for converting {@link AbstractReward} objects.
 * to and from their JSON string representations for database storage.
 *
 * <p>This converter uses {@link RewardParser} for serialization with support for
 * Bukkit ItemStack serialization.
 *
 * <p>Note: autoApply is set to false to avoid automatic application to all AbstractReward fields.
 * Each entity must explicitly specify @Convert(converter = RewardConverter.class) on the field.
 *
 * <p>The converted AbstractReward objects are treated as immutable to avoid unnecessary deep copying
 * during Hibernate merge operations.
 */
@Converter(autoApply = false)
public class RewardConverter implements AttributeConverter<AbstractReward, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger("RDQ");
    
    // Lazy initialization flag to avoid triggering RewardParser during class loading
    private static volatile boolean parserReady = false;
    private static final Object INIT_LOCK = new Object();

    /**
     * Executes convertToDatabaseColumn.
     */
    @Override
    public String convertToDatabaseColumn(AbstractReward attribute) {
        if (attribute == null) {
            LOGGER.debug("Reward is null, returning null for database column");
            return null;
        }
        try {
            ensureParserReady();
            String json = RewardParser.serialize(attribute);
            LOGGER.debug("Serialized reward: {} -> {} chars", attribute.getClass().getSimpleName(), json.length());
            return json;
        } catch (Exception e) {
            LOGGER.error("Failed to serialize reward of type: {}", attribute.getClass().getName(), e);
            // Don't throw - return null to prevent cascade failures
            return null;
        }
    }

    /**
     * Executes convertToEntityAttribute.
     */
    @Override
    public AbstractReward convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            LOGGER.debug("Database data is null or empty, returning null");
            return null;
        }
        try {
            ensureParserReady();
            LOGGER.debug("Deserializing reward from JSON: {} chars", dbData.length());
            
            // Try to migrate old format if needed
            String migratedData = migrateOldFormat(dbData);
            
            AbstractReward reward = RewardParser.parse(migratedData);
            LOGGER.debug("Deserialized reward: {}", reward.getClass().getSimpleName());
            return reward;
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize reward from JSON (length: {}): {}", 
                dbData.length(), dbData.substring(0, Math.min(200, dbData.length())), e);
            // Return null instead of throwing to prevent cascade failures
            // The BaseReward entity will fail validation, but won't crash the entire system
            return null;
        }
    }
    
    /**
     * Migrates old reward JSON format to new format.
     * Old format used class names like "ExperienceReward", "CompositeReward"
     * New format uses type IDs like "EXPERIENCE", "COMPOSITE"
     */
    private String migrateOldFormat(String json) {
        if (json == null) return json;
        
        // Map of old class names to new type IDs
        String migrated = json
            .replace("\"type\" : \"ExperienceReward\"", "\"type\" : \"EXPERIENCE\"")
            .replace("\"type\" : \"CompositeReward\"", "\"type\" : \"COMPOSITE\"")
            .replace("\"type\" : \"ChoiceReward\"", "\"type\" : \"CHOICE\"")
            .replace("\"type\" : \"ItemReward\"", "\"type\" : \"ITEM\"")
            .replace("\"type\" : \"CurrencyReward\"", "\"type\" : \"CURRENCY\"")
            .replace("\"type\" : \"CommandReward\"", "\"type\" : \"COMMAND\"")
            .replace("\"type\" : \"PermissionReward\"", "\"type\" : \"PERMISSION\"")
            .replace("\"type\" : \"TeleportReward\"", "\"type\" : \"TELEPORT\"")
            .replace("\"type\" : \"ParticleReward\"", "\"type\" : \"PARTICLE\"")
            .replace("\"type\" : \"VanishingChestReward\"", "\"type\" : \"VANISHING_CHEST\"");
        
        if (!migrated.equals(json)) {
            LOGGER.info("Migrated old reward format to new format");
        }
        
        return migrated;
    }
    
    /**
     * Ensures the RewardParser is ready before use.
     * This prevents triggering parser initialization during entity class loading.
     */
    private void ensureParserReady() {
        if (!parserReady) {
            synchronized (INIT_LOCK) {
                if (!parserReady) {
                    try {
                        // Trigger parser initialization
                        RewardParser.getObjectMapper();
                        parserReady = true;
                        LOGGER.debug("RewardParser initialized successfully");
                    } catch (Exception e) {
                        LOGGER.error("Failed to initialize RewardParser", e);
                        throw new RuntimeException("Failed to initialize RewardParser", e);
                    }
                }
            }
        }
    }
}
