package com.raindropcentral.rdq.database.json.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raindropcentral.rdq.database.json.serializer.ItemStackJSONDeserializer;
import com.raindropcentral.rdq.database.json.serializer.ItemStackJSONSerializer;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Centralized helper for converting {@link AbstractRequirement} instances to and from their JSON representation.
 * <p>
 * The parser exposes static serialization utilities backed by a shared Jackson {@link ObjectMapper} that is prepared with
 * Bukkit-aware serializers, deserializers, and mixins. This guarantees that {@link ItemStack} payloads and the
 * polymorphic requirement hierarchy described through {@link RequirementMixin} are consistently processed whether the
 * JSON originates from configuration files, a database column, or network transport.
 * </p>
 *
 * <ul>
 *   <li>Registers bespoke handlers for {@link ItemStack} values.</li>
 *   <li>Supports polymorphic requirement types via {@link RequirementMixin}.</li>
 *   <li>Disables {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} so empty beans can still round-trip.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
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
     * Creates and configures the {@link ObjectMapper} with custom serializers and deserializers tailored for RDQ requirements.
     * <p>
     * The mapper is enriched with:
     * <ul>
     *   <li>{@link ItemStack} serializers and deserializers for Bukkit stacks.</li>
     *   <li>{@link JavaTimeModule} support so temporal requirement metadata is preserved.</li>
     *   <li>{@link RequirementMixin} for polymorphic requirement discovery based on the configured {@code type} field.</li>
     * </ul>
     * Additionally, {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} is disabled to permit serialization of sentinel
     * objects that intentionally expose no properties.
     * </p>
     *
     * @return configured {@link ObjectMapper} instance capable of handling requirement payloads
     */
    private static @NotNull ObjectMapper createObjectMapper() {
        final SimpleModule bukkitModule = new SimpleModule("BukkitModule");
        bukkitModule.addSerializer(ItemStack.class, new ItemStackJSONSerializer());
        bukkitModule.addDeserializer(ItemStack.class, new ItemStackJSONDeserializer());

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(bukkitModule)
                .addMixIn(AbstractRequirement.class, RequirementMixin.class)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Parses a JSON document into an {@link AbstractRequirement} object graph.
     * <p>
     * The configured {@link RequirementMixin} inspects the {@code type} discriminator embedded in the payload to
     * construct the corresponding requirement subtype before delegating to the subtype-specific deserializer.
     * </p>
     *
     * @param json the JSON string to parse
     * @return the parsed {@link AbstractRequirement} instance representing the provided JSON
     * @throws IOException if parsing fails because of malformed JSON or when an unknown requirement type is encountered
     */
    public static @NotNull AbstractRequirement parse(
            @NotNull final String json
    ) throws IOException {
        return MAPPER.readValue(json, AbstractRequirement.class);
    }

    /**
     * Serializes an {@link AbstractRequirement} hierarchy into a JSON string.
     * <p>
     * The output payload contains the {@code type} field required to reconstruct the concrete requirement during
     * {@link #parse(String)} so the round-trip process remains lossless across storage mediums.
     * </p>
     *
     * @param requirement the requirement to serialize
     * @return the JSON string representation of the provided requirement
     * @throws IOException if serialization fails, for example when encountering a Jackson-mapped field that cannot be rendered
     */
    public static @NotNull String serialize(
            @NotNull final AbstractRequirement requirement
    ) throws IOException {
        return MAPPER.writeValueAsString(requirement);
    }
}
