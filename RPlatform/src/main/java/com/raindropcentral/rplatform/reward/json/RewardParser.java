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

package com.raindropcentral.rplatform.reward.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.raindropcentral.rplatform.json.ItemStackJSONDeserializer;
import com.raindropcentral.rplatform.json.ItemStackJSONSerializer;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardParser API type.
 */
public final class RewardParser {

    private static ObjectMapper objectMapper;
    private static final Object MAPPER_LOCK = new Object();

    private RewardParser() {}

    /**
     * Gets objectMapper.
     */
    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            synchronized (MAPPER_LOCK) {
                if (objectMapper == null) {
                    objectMapper = createObjectMapper();
                }
            }
        }
        return objectMapper;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        // Register ItemStack serializer/deserializer
        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(ItemStack.class, new ItemStackJSONSerializer());
        bukkitModule.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());
        mapper.registerModule(bukkitModule);
        
        RewardRegistry.getInstance().configureObjectMapper(mapper);
        
        return mapper;
    }

    /**
     * Executes parse.
     */
    public static AbstractReward parse(@NotNull final String json) {
        try {
            return getObjectMapper().readValue(json, AbstractReward.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse reward JSON", e);
        }
    }

    /**
     * Executes serialize.
     */
    public static String serialize(@NotNull final AbstractReward reward) {
        try {
            return getObjectMapper().writeValueAsString(reward);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reward", e);
        }
    }

    /**
     * Executes resetMapper.
     */
    public static void resetMapper() {
        objectMapper = null;
    }
}
