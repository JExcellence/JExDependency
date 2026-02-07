package com.raindropcentral.rplatform.reward.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.raindropcentral.rplatform.json.ItemStackJSONDeserializer;
import com.raindropcentral.rplatform.json.ItemStackJSONSerializer;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.RewardRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class RewardParser {

    private static ObjectMapper objectMapper;

    private RewardParser() {}

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = createObjectMapper();
        }
        return objectMapper;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // Register ItemStack serializer/deserializer
        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(ItemStack.class, new ItemStackJSONSerializer());
        bukkitModule.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());
        mapper.registerModule(bukkitModule);
        
        RewardRegistry.getInstance().configureObjectMapper(mapper);
        
        return mapper;
    }

    public static AbstractReward parse(@NotNull final String json) {
        try {
            return getObjectMapper().readValue(json, AbstractReward.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse reward JSON", e);
        }
    }

    public static String serialize(@NotNull final AbstractReward reward) {
        try {
            return getObjectMapper().writeValueAsString(reward);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reward", e);
        }
    }

    public static void resetMapper() {
        objectMapper = null;
    }
}
