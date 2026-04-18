package de.jexcellence.jexplatform.requirement.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base exception for requirement system errors.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RequirementException extends RuntimeException {

    private final String requirementType;

    /**
     * Creates an exception with a message.
     *
     * @param message the error message
     */
    public RequirementException(@NotNull String message) {
        this(message, null, null);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public RequirementException(@NotNull String message, @Nullable Throwable cause) {
        this(message, null, cause);
    }

    /**
     * Creates an exception with a message and requirement type.
     *
     * @param message         the error message
     * @param requirementType the type that caused the error
     * @param cause           the underlying cause
     */
    public RequirementException(@NotNull String message,
                                @Nullable String requirementType,
                                @Nullable Throwable cause) {
        super(message, cause);
        this.requirementType = requirementType;
    }

    /**
     * Returns the requirement type that caused this error.
     *
     * @return the type identifier, or {@code null}
     */
    public @Nullable String getRequirementType() {
        return requirementType;
    }
}
