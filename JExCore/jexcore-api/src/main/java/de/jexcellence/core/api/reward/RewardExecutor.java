package de.jexcellence.core.api.reward;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Public entry point for granting rewards. Obtained via
 * {@code Bukkit.getServicesManager().load(RewardExecutor.class)} once
 * JExCore's common module has registered its default implementation.
 *
 * <p>Third-party plugins use {@link #registerHandler(String, RewardHandler)}
 * to attach handlers for {@link Reward.Custom} entries with their
 * plugin-specific {@code type} key.
 */
public interface RewardExecutor {

    /**
     * Grant a reward. Schedules the handler on the Bukkit main thread
     * and resolves the returned future there, so callers on async
     * threads can chain without manually round-tripping.
     */
    @NotNull CompletableFuture<RewardResult> grant(
            @NotNull Reward reward,
            @NotNull RewardContext context
    );

    /** Grant a reward synchronously on the current thread (must be the main thread). */
    @NotNull RewardResult grantSync(
            @NotNull Reward reward,
            @NotNull RewardContext context
    );

    /**
     * Register a handler for one custom reward {@code type}. Overwrites
     * any previous registration.
     */
    void registerHandler(@NotNull String type, @NotNull RewardHandler handler);

    /** Remove a custom handler. No-op if absent. */
    void unregisterHandler(@NotNull String type);

    /**
     * Convenience lookup via {@code ServicesManager}.
     *
     * @return the registered executor, or {@code null} if JExCore isn't loaded
     */
    static @Nullable RewardExecutor get() {
        return Bukkit.getServicesManager().load(RewardExecutor.class);
    }
}
