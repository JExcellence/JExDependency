package de.jexcellence.jexplatform.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all reward implementations.
 *
 * <p>Identified by a type ID registered in {@link RewardRegistry}.
 * Jackson polymorphism is configured via {@code @JsonTypeInfo}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({})
public abstract non-sealed class AbstractReward implements Reward {

    @JsonProperty("type")
    private final String typeId;

    /**
     * Creates a reward with the given type ID.
     *
     * @param typeId the reward type identifier
     */
    protected AbstractReward(@NotNull String typeId) {
        this.typeId = typeId;
    }

    @Override
    public @NotNull String typeId() {
        return typeId;
    }

    @Override
    public double estimatedValue() {
        return 0.0;
    }
}
