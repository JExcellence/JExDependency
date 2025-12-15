package com.raindropcentral.rdq.json.requirement;

import com.raindropcentral.rdq.json.ItemStackJSONDeserializer;
import com.raindropcentral.rdq.json.ItemStackJSONSerializer;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Utility class for serializing and deserializing {@link AbstractRequirement} objects to and from JSON.
 * <p>
 * This class provides static methods to convert between JSON strings and {@link AbstractRequirement} instances,
 * supporting polymorphic deserialization based on the "type" field and handling Bukkit-specific types such as {@link ItemStack}.
 * It uses a pre-configured Jackson {@link ObjectMapper} with custom serializers and deserializers for Bukkit classes.
 * </p>
 *
 * <ul>
 *   <li>Supports custom serialization/deserialization for {@link ItemStack}.</li>
 *   <li>Handles polymorphic requirement types via {@link com.raindropcentral.rdq.json.requirement.RequirementMixin}.</li>
 *   <li>Intended for use in persistence, configuration, and network transfer of requirements.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RequirementParser {

    /**
     * The shared, pre-configured Jackson {@link ObjectMapper} instance for requirement (de)serialization.
     */
    private static final ObjectMapper MAPPER = createObjectMapper();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RequirementParser() {
    }

    /**
     * Creates and configures the {@link ObjectMapper} with custom serializers and deserializers.
     * <p>
     * Registers support for:
     * <ul>
     *   <li>{@link ItemStack} serialization and deserialization</li>
     *   <li>Polymorphic requirement types via {@link com.raindropcentral.rdq.json.requirement.RequirementMixin}</li>
     * </ul>
     * </p>
     *
     * @return Configured {@link ObjectMapper} instance
     */
    private static @NotNull ObjectMapper createObjectMapper() {
        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(ItemStack.class, new ItemStackJSONSerializer());
        bukkitModule.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());

        return JsonMapper.builder()
                .addModule(bukkitModule)
                .addMixIn(AbstractRequirement.class, RequirementMixin.class)
                .build();
    }

    /**
     * Parses a JSON string into an {@link AbstractRequirement} object.
     * <p>
     * The concrete implementation type is determined by the "type" field in the JSON,
     * as configured in {@link RequirementMixin}.
     * </p>
     *
     * @param json The JSON string to parse
     * @return The parsed {@link AbstractRequirement} instance
     * @throws IOException If parsing fails due to invalid JSON or mapping errors
     */
    public static @NotNull AbstractRequirement parse(
            @NotNull final String json
    ) throws IOException {
        return MAPPER.readValue(json, AbstractRequirement.class);
    }

    /**
     * Serializes an {@link AbstractRequirement} object to a JSON string.
     * <p>
     * The resulting JSON will include type information to allow proper polymorphic deserialization.
     * </p>
     *
     * @param requirement The requirement to serialize
     * @return The JSON string representation of the requirement
     * @throws IOException If serialization fails
     */
    public static @NotNull String serialize(
            @NotNull final AbstractRequirement requirement
    ) throws IOException {
        return MAPPER.writeValueAsString(requirement);
    }
}
