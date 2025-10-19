package com.raindropcentral.rdq.database.json.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raindropcentral.rdq.database.json.reward.RewardMixin;
import com.raindropcentral.rdq.database.json.serializer.ItemStackJSONDeserializer;
import com.raindropcentral.rdq.database.json.serializer.ItemStackJSONSerializer;
import com.raindropcentral.rdq.reward.AbstractReward;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Utility class for converting {@link AbstractReward} definitions to and from JSON payloads.
 * <p>
 * Provides static helpers that use a shared {@link ObjectMapper} configured with Bukkit-aware
 * serializers so that reward data can be persisted and retrieved consistently across editions.
 * </p>
 *
 * <ul>
 *   <li>Registers {@link ItemStack} serializers for safe inventory metadata round-tripping.</li>
 *   <li>Applies {@link RewardMixin} to honour polymorphic reward implementations.</li>
 *   <li>Exposes convenience methods for persistence, configuration, and network transport layers.</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class RewardParser {

    /**
     * The shared, pre-configured Jackson {@link ObjectMapper} instance for reward (de)serialization.
     */
    private static final ObjectMapper MAPPER = createObjectMapper();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private RewardParser() {

    }

    /**
     * Creates and configures the {@link ObjectMapper} with custom serializers and deserializers.
     * <p>
     * Registers support for:
     * <ul>
     *   <li>{@link ItemStack} serialization and deserialization</li>
     *   <li>Java time types via {@link JavaTimeModule}</li>
     *   <li>Polymorphic reward types via {@link RewardMixin}</li>
     * </ul>
     * Disables {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} to allow serialization of empty objects.
     * </p>
     *
     * @return configured {@link ObjectMapper} instance
     */
    private static @NotNull ObjectMapper createObjectMapper() {

        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(
                ItemStack.class,
                new ItemStackJSONSerializer()
        );
        bukkitModule.addDeserializer(
                ItemStack.class,
                new ItemStackJSONDeserializer()
        );

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(bukkitModule)
                .addMixIn(
                        AbstractReward.class,
                        RewardMixin.class
                )
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Parses a JSON string into an {@link AbstractReward} instance.
     * <p>
     * The concrete implementation type is determined by the {@code type} discriminator configured via
     * {@link RewardMixin}.
     * </p>
     *
     * @param json JSON payload describing the reward definition
     *
     * @return the parsed {@link AbstractReward} instance
     *
     * @throws IOException if parsing fails due to invalid JSON or mapping errors
     */
    public static @NotNull AbstractReward parse(
            @NotNull final String json
    ) throws IOException {

        return MAPPER.readValue(
                json,
                AbstractReward.class
        );
    }

    /**
     * Serializes an {@link AbstractReward} object to a JSON string.
     * <p>
     * The resulting JSON includes the type discriminator to enable polymorphic deserialization.
     * </p>
     *
     * @param reward the reward instance to serialize
     *
     * @return the JSON string representation of the reward
     *
     * @throws IOException if serialization fails
     */
    public static @NotNull String serialize(
            @NotNull final AbstractReward reward
    ) throws IOException {

        return MAPPER.writeValueAsString(reward);
    }

}