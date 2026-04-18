package de.jexcellence.jexplatform.reward.async;

import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract reward that supports asynchronous granting.
 *
 * <p>Implementations should override {@link #grantAsync(Player)}.
 * The synchronous {@link #grant(Player)} delegates to the async counterpart.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class AsyncReward extends AbstractReward {

    /**
     * Creates an async reward.
     *
     * @param typeId the type identifier
     */
    protected AsyncReward(@NotNull String typeId) {
        super(typeId);
    }

    /**
     * Asynchronously grants this reward to the player.
     *
     * @param player the player
     * @return a future resolving to {@code true} on success
     */
    public abstract @NotNull CompletableFuture<Boolean> grantAsync(@NotNull Player player);

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return grantAsync(player);
    }
}
