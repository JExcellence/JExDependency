package de.jexcellence.jexplatform.requirement.validation;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating a requirement configuration.
 *
 * @param valid    whether the requirement is valid
 * @param errors   list of error messages
 * @param warnings list of warning messages
 * @author JExcellence
 * @since 1.0.0
 */
public record ValidationResult(
        boolean valid,
        @NotNull List<String> errors,
        @NotNull List<String> warnings
) {
    /**
     * Creates a successful validation result.
     *
     * @return an OK result
     */
    public static @NotNull ValidationResult ok() {
        return new ValidationResult(true, List.of(), List.of());
    }

    /**
     * Creates a failed validation result with errors.
     *
     * @param errors the error messages
     * @return a failing result
     */
    public static @NotNull ValidationResult error(@NotNull String... errors) {
        return new ValidationResult(false, List.of(errors), List.of());
    }

    /**
     * Creates a valid result with warnings.
     *
     * @param warnings the warning messages
     * @return a valid result with warnings
     */
    public static @NotNull ValidationResult warning(@NotNull String... warnings) {
        return new ValidationResult(true, List.of(), List.of(warnings));
    }

    /**
     * Returns a new result with an additional error appended.
     *
     * @param error the error to add
     * @return a new result
     */
    public @NotNull ValidationResult withError(@NotNull String error) {
        var newErrors = new ArrayList<>(errors);
        newErrors.add(error);
        return new ValidationResult(false, List.copyOf(newErrors), warnings);
    }

    /**
     * Combines this result with another, merging errors and warnings.
     *
     * @param other the other result
     * @return a combined result
     */
    public @NotNull ValidationResult combine(@NotNull ValidationResult other) {
        var combinedErrors = new ArrayList<>(errors);
        combinedErrors.addAll(other.errors);
        var combinedWarnings = new ArrayList<>(warnings);
        combinedWarnings.addAll(other.warnings);
        return new ValidationResult(
                valid && other.valid,
                List.copyOf(combinedErrors),
                List.copyOf(combinedWarnings));
    }
}
