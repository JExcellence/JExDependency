package de.jexcellence.core.api.reward;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Dispatch context for a reward grant. The executor uses this to
 * address the correct player, attribute the grant to a source plugin
 * (for logs / telemetry), and resolve placeholders.
 *
 * @param playerUuid target player
 * @param sourcePlugin originating plugin name, or {@code null} for anonymous grants
 * @param reason free-form tag describing the reward cause, or {@code null}
 */
public record RewardContext(
        @NotNull UUID playerUuid,
        @Nullable String sourcePlugin,
        @Nullable String reason
) {
    public static @NotNull RewardContext of(@NotNull UUID playerUuid) {
        return new RewardContext(playerUuid, null, null);
    }
}
