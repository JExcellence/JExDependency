package com.raindropcentral.rplatform.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = com.raindropcentral.rplatform.reward.impl.ItemReward.class, name = "ITEM"),
    @JsonSubTypes.Type(value = com.raindropcentral.rplatform.reward.impl.CurrencyReward.class, name = "CURRENCY"),
    @JsonSubTypes.Type(value = com.raindropcentral.rplatform.reward.impl.ExperienceReward.class, name = "EXPERIENCE"),
    @JsonSubTypes.Type(value = com.raindropcentral.rplatform.reward.impl.CommandReward.class, name = "COMMAND"),
    @JsonSubTypes.Type(value = com.raindropcentral.rplatform.reward.impl.CompositeReward.class, name = "COMPOSITE"),
    @JsonSubTypes.Type(value = com.raindropcentral.rplatform.reward.impl.ChoiceReward.class, name = "CHOICE")
})
public abstract non-sealed class AbstractReward implements Reward {

    @Override
    public abstract @NotNull String getTypeId();

    @Override
    public abstract @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);

    @Override
    public abstract double getEstimatedValue();

    @Override
    public @NotNull String getDescriptionKey() {
        return "reward." + getTypeId().toLowerCase() + ".description";
    }

    public void validate() {
    }
}
