package com.raindropcentral.rplatform.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all requirement implementations.
 *
 * <p>Each requirement is identified by its type ID which is registered in the RequirementRegistry.
 */
public abstract non-sealed class AbstractRequirement implements Requirement {
    
    @JsonProperty("type")
    protected final String typeId;
    
    @JsonProperty("consumeOnComplete")
    protected boolean consumeOnComplete = false;

    protected AbstractRequirement(@NotNull String typeId) {
        this.typeId = typeId;
    }
    
    protected AbstractRequirement(@NotNull String typeId, boolean consumeOnComplete) {
        this.typeId = typeId;
        this.consumeOnComplete = consumeOnComplete;
    }

    /**
     * Gets typeId.
     */
    @Override
    @NotNull
    public String getTypeId() {
        return typeId;
    }
    
    /**
     * Whether this requirement should consume resources when completed.
     *
     * @return true if resources should be consumed
     */
    @JsonIgnore
    public boolean shouldConsume() {
        return consumeOnComplete;
    }
    
    /**
     * Sets whether this requirement should consume resources when completed.
     *
     * @param consumeOnComplete true to consume resources
     */
    public void setConsumeOnComplete(boolean consumeOnComplete) {
        this.consumeOnComplete = consumeOnComplete;
    }
}
