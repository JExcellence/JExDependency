package de.jexcellence.jexplatform.requirement.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import de.jexcellence.jexplatform.requirement.RequirementRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Jackson-based JSON parser for requirement instances.
 *
 * <p>Registers all known requirement types from the registry as named
 * subtypes for polymorphic deserialization.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RequirementParser {

    private final ObjectMapper objectMapper;

    /**
     * Creates a parser configured with the given registry's types.
     *
     * @param registry the requirement registry
     */
    public RequirementParser(@NotNull RequirementRegistry registry) {
        this.objectMapper = new ObjectMapper();
        registry.types().forEach((name, type) ->
                objectMapper.registerSubtypes(
                        new NamedType(type.implementationClass(), type.id())));
    }

    /**
     * Parses a JSON string into a requirement.
     *
     * @param json the JSON representation
     * @return the parsed requirement
     * @throws RuntimeException if parsing fails
     */
    public @NotNull AbstractRequirement parse(@NotNull String json) {
        try {
            return objectMapper.readValue(json, AbstractRequirement.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse requirement JSON", e);
        }
    }

    /**
     * Serializes a requirement to JSON.
     *
     * @param requirement the requirement to serialize
     * @return the JSON string
     * @throws RuntimeException if serialization fails
     */
    public @NotNull String serialize(@NotNull AbstractRequirement requirement) {
        try {
            return objectMapper.writeValueAsString(requirement);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize requirement", e);
        }
    }

    /**
     * Attempts to parse a JSON string, returning {@code null} on failure.
     *
     * @param json the JSON representation
     * @return the parsed requirement, or {@code null}
     */
    public @Nullable AbstractRequirement tryParse(@Nullable String json) {
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
