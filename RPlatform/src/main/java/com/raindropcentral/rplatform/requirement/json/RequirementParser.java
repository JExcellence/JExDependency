package com.raindropcentral.rplatform.requirement.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.raindropcentral.rplatform.json.ItemStackJSONDeserializer;
import com.raindropcentral.rplatform.json.ItemStackJSONSerializer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Utility class for serializing and deserializing {@link AbstractRequirement} objects to and from JSON.
 * <p>
 * This class provides static methods to convert between JSON strings and {@link AbstractRequirement} instances,
 * supporting polymorphic deserialization based on the "type" field and handling Bukkit-specific types such as {@link ItemStack}.
 * </p>
 */
public final class RequirementParser {

    private static volatile ObjectMapper MAPPER;

    private RequirementParser() {
    }

    /**
     * Gets or creates the configured ObjectMapper instance.
     *
     * @return the configured ObjectMapper
     */
    @NotNull
    public static ObjectMapper getObjectMapper() {
        if (MAPPER == null) {
            synchronized (RequirementParser.class) {
                if (MAPPER == null) {
                    MAPPER = createObjectMapper();
                }
            }
        }
        return MAPPER;
    }

    /**
     * Creates and configures the ObjectMapper with custom serializers and deserializers.
     *
     * @return configured ObjectMapper instance
     */
    private static @NotNull ObjectMapper createObjectMapper() {
        Logger logger = CentralLogger.getLogger("RDQ");
        logger.info("Creating new ObjectMapper for requirement serialization...");
        
        // Get the registry (should already be initialized by RPlatform)
        RequirementRegistry registry = RequirementRegistry.getInstance();
        
        if (registry.getRequirementTypes().isEmpty()) {
            logger.severe("CRITICAL: RequirementRegistry has no types registered!");
            logger.severe("This indicates RPlatform.initialize() was not called before requirement parsing.");
            logger.severe("Ensure RPlatform.initialize() completes before using RequirementParser.");
            throw new IllegalStateException(
                "RequirementRegistry not initialized. Call RPlatform.initialize() first."
            );
        }
        
        logger.info("Registry has " + registry.getRequirementTypes().size() + " types: " + 
                    registry.getRequirementTypes().keySet());
        
        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(ItemStack.class, new ItemStackJSONSerializer());
        bukkitModule.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
            .addModule(bukkitModule)
            .addMixIn(AbstractRequirement.class, RequirementMixin.class)
            .build();
        
        registry.configureObjectMapper(mapper);
        
        logger.info("ObjectMapper created and configured successfully");

        return mapper;
    }

    /**
     * Parses a JSON string into an AbstractRequirement object.
     *
     * @param json the JSON string to parse
     * @return the parsed AbstractRequirement instance
     * @throws IOException if parsing fails
     */
    @NotNull
    public static AbstractRequirement parse(@NotNull final String json) throws IOException {
        return getObjectMapper().readValue(json, AbstractRequirement.class);
    }

    /**
     * Serializes an AbstractRequirement object to a JSON string.
     *
     * @param requirement the requirement to serialize
     * @return the JSON string representation
     * @throws IOException if serialization fails
     */
    @NotNull
    public static String serialize(@NotNull final AbstractRequirement requirement) throws IOException {
        return getObjectMapper().writeValueAsString(requirement);
    }

    /**
     * Resets the cached ObjectMapper, forcing recreation on next use.
     * <p>
     * Call this after registering new requirement types to ensure they are included.
     * </p>
     */
    public static void resetMapper() {
        synchronized (RequirementParser.class) {
            MAPPER = null;
        }
    }
}
