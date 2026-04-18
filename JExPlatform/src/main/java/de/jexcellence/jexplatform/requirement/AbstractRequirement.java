package de.jexcellence.jexplatform.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all requirement implementations.
 *
 * <p>Identified by a type ID registered in {@link RequirementRegistry}.
 * Jackson polymorphism is configured via {@code @JsonTypeInfo} — subtypes
 * are registered dynamically through the registry.
 *
 * <p>The {@code consumeOnComplete} flag is immutable after construction
 * (set via constructor, no setter).
 *
 * @author JExcellence
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({})
public abstract non-sealed class AbstractRequirement implements Requirement {

    @JsonProperty("type")
    private final String typeId;

    @JsonProperty("consumeOnComplete")
    private final boolean consumeOnComplete;

    /**
     * Creates a requirement with the given type ID and no consumption.
     *
     * @param typeId the requirement type identifier
     */
    protected AbstractRequirement(@NotNull String typeId) {
        this(typeId, false);
    }

    /**
     * Creates a requirement with the given type ID and consumption flag.
     *
     * @param typeId            the requirement type identifier
     * @param consumeOnComplete whether resources should be consumed on completion
     */
    protected AbstractRequirement(@NotNull String typeId, boolean consumeOnComplete) {
        this.typeId = typeId;
        this.consumeOnComplete = consumeOnComplete;
    }

    @Override
    public @NotNull String typeId() {
        return typeId;
    }

    /**
     * Returns whether this requirement should consume resources on completion.
     *
     * @return {@code true} when resources should be consumed
     */
    @JsonIgnore
    public boolean shouldConsume() {
        return consumeOnComplete;
    }
}
