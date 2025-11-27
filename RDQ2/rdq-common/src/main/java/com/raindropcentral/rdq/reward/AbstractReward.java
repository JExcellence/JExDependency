package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ItemReward.class, name = "ITEM"),
    @JsonSubTypes.Type(value = CurrencyReward.class, name = "CURRENCY"),
    @JsonSubTypes.Type(value = ExperienceReward.class, name = "EXPERIENCE"),
    @JsonSubTypes.Type(value = CommandReward.class, name = "COMMAND"),
    @JsonSubTypes.Type(value = CompositeReward.class, name = "COMPOSITE")
})
public abstract non-sealed class AbstractReward implements Reward {
    
    protected final Type type;
    protected final String descriptionKey;

    protected AbstractReward(Type type, String descriptionKey) {
        this.type = type;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public @NotNull Type getType() { return type; }

    @Override
    public @NotNull String getDescriptionKey() { return descriptionKey; }
}
