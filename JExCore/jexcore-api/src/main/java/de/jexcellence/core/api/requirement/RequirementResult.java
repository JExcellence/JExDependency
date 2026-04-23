package de.jexcellence.core.api.requirement;

import org.jetbrains.annotations.NotNull;

/**
 * Outcome of evaluating a {@link Requirement}. Three shapes:
 * {@link Met}, {@link NotMet}, {@link Error}.
 */
public sealed interface RequirementResult {

    static @NotNull Met met() { return Met.INSTANCE; }
    static @NotNull NotMet notMet(@NotNull String reason) { return new NotMet(reason); }
    static @NotNull Error error(@NotNull String message) { return new Error(message); }

    record Met() implements RequirementResult {
        public static final Met INSTANCE = new Met();
    }

    record NotMet(@NotNull String reason) implements RequirementResult {
    }

    record Error(@NotNull String message) implements RequirementResult {
    }

    default boolean isMet() {
        return this instanceof Met;
    }
}
