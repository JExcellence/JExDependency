package de.jexcellence.core.api.reward;

import org.jetbrains.annotations.NotNull;

/**
 * Outcome of one reward grant attempt. Three shapes:
 * {@link Granted}, {@link Denied}, {@link Failed}.
 */
public sealed interface RewardResult {

    static @NotNull Granted granted(@NotNull String summary) { return new Granted(summary); }
    static @NotNull Denied denied(@NotNull String reason) { return new Denied(reason); }
    static @NotNull Failed failed(@NotNull String error) { return new Failed(error); }

    /** Reward was applied successfully. */
    record Granted(@NotNull String summary) implements RewardResult {
    }

    /**
     * Reward was intentionally skipped — player offline, inventory
     * full, target world disallowed, cooldown active, etc. Not an
     * error; composite grants continue past a {@code Denied}.
     */
    record Denied(@NotNull String reason) implements RewardResult {
    }

    /**
     * Grant failed because of a runtime error. Composite grants
     * short-circuit on {@code Failed}.
     */
    record Failed(@NotNull String error) implements RewardResult {
    }

    default boolean isGranted() {
        return this instanceof Granted;
    }
}
