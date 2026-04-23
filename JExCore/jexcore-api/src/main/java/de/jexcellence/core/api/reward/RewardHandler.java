package de.jexcellence.core.api.reward;

import org.jetbrains.annotations.NotNull;

/**
 * Synchronous per-type reward handler. Always invoked on the Bukkit
 * main thread by the executor — handlers may touch player inventory,
 * dispatch commands, grant XP, etc. without scheduling.
 */
@FunctionalInterface
public interface RewardHandler {

    @NotNull RewardResult grant(@NotNull Reward reward, @NotNull RewardContext context);
}
