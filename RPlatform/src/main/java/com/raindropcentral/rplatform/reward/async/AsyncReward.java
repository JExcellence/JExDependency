package com.raindropcentral.rplatform.reward.async;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class AsyncReward extends AbstractReward {

    @Override
    public final @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return grantAsync(player);
    }

    public abstract @NotNull CompletableFuture<Boolean> grantAsync(@NotNull Player player);
}
