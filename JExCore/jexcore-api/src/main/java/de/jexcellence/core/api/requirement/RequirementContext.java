package de.jexcellence.core.api.requirement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Evaluation context for a {@link Requirement}.
 *
 * @param playerUuid subject player
 * @param sourcePlugin originating plugin, or {@code null} for anonymous checks
 * @param reason free-form tag for logs / telemetry
 */
public record RequirementContext(
        @NotNull UUID playerUuid,
        @Nullable String sourcePlugin,
        @Nullable String reason
) {
    public static @NotNull RequirementContext of(@NotNull UUID playerUuid) {
        return new RequirementContext(playerUuid, null, null);
    }
}
