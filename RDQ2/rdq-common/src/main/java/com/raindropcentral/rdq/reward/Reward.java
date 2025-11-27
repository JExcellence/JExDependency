package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public sealed interface Reward permits AbstractReward {

    enum Type { ITEM, CURRENCY, EXPERIENCE, PERMISSION, COMMAND, COMPOSITE }

    @NotNull Type getType();
    @NotNull CompletableFuture<Boolean> grant(@NotNull Player player);
    double getEstimatedValue();
    
    @JsonIgnore
    @NotNull String getDescriptionKey();
}
