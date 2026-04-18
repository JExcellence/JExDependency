package de.jexcellence.jexplatform.reward.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import de.jexcellence.jexplatform.reward.AbstractReward;
import de.jexcellence.jexplatform.reward.RewardRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Jackson-based JSON parser for reward instances.
 *
 * <p>Registers all known reward types from the registry as named
 * subtypes for polymorphic deserialization.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RewardParser {

    private final ObjectMapper objectMapper;

    /**
     * Creates a parser configured with the given registry's types.
     *
     * @param registry the reward registry
     */
    public RewardParser(@NotNull RewardRegistry registry) {
        this.objectMapper = new ObjectMapper();
        registry.types().forEach((name, type) ->
                objectMapper.registerSubtypes(
                        new NamedType(type.implementationClass(), type.id())));
    }

    /**
     * Parses a JSON string into a reward.
     *
     * @param json the JSON representation
     * @return the parsed reward
     * @throws RuntimeException if parsing fails
     */
    public @NotNull AbstractReward parse(@NotNull String json) {
        try {
            return objectMapper.readValue(json, AbstractReward.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse reward JSON", e);
        }
    }

    /**
     * Serializes a reward to JSON.
     *
     * @param reward the reward to serialize
     * @return the JSON string
     * @throws RuntimeException if serialization fails
     */
    public @NotNull String serialize(@NotNull AbstractReward reward) {
        try {
            return objectMapper.writeValueAsString(reward);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize reward", e);
        }
    }

    /**
     * Attempts to parse a JSON string, returning {@code null} on failure.
     *
     * @param json the JSON representation
     * @return the parsed reward, or {@code null}
     */
    public @Nullable AbstractReward tryParse(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the configured ObjectMapper.
     *
     * @return the object mapper
     */
    public @NotNull ObjectMapper objectMapper() {
        return objectMapper;
    }
}
