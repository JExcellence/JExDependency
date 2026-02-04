package com.raindropcentral.rplatform.requirement.validation;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of requirement validation.
 *
 * @param valid true if the requirement is valid
 * @param errors list of error messages (empty if valid)
 * @param warnings list of warning messages
 */
public record ValidationResult(
    boolean valid,
    @NotNull List<String> errors,
    @NotNull List<String> warnings
) {
    
    /**
     * Creates a successful validation result.
     */
    @NotNull
    public static ValidationResult success() {
        return new ValidationResult(true, List.of(), List.of());
    }
    
    /**
     * Creates a failed validation result with errors.
     */
    @NotNull
    public static ValidationResult error(@NotNull String... errors) {
        return new ValidationResult(false, List.of(errors), List.of());
    }
    
    /**
     * Creates a successful validation result with warnings.
     */
    @NotNull
    public static ValidationResult warning(@NotNull String... warnings) {
        return new ValidationResult(true, List.of(), List.of(warnings));
    }
    
    /**
     * Adds an error to this result.
     */
    @NotNull
    public ValidationResult withError(@NotNull String error) {
        var newErrors = new ArrayList<>(errors);
        newErrors.add(error);
        return new ValidationResult(false, newErrors, warnings);
    }
    
    /**
     * Adds a warning to this result.
     */
    @NotNull
    public ValidationResult withWarning(@NotNull String warning) {
        var newWarnings = new ArrayList<>(warnings);
        newWarnings.add(warning);
        return new ValidationResult(valid, errors, newWarnings);
    }
    
    /**
     * Combines this result with another.
     */
    @NotNull
    public ValidationResult combine(@NotNull ValidationResult other) {
        var combinedErrors = new ArrayList<>(errors);
        combinedErrors.addAll(other.errors);
        
        var combinedWarnings = new ArrayList<>(warnings);
        combinedWarnings.addAll(other.warnings);
        
        return new ValidationResult(
            valid && other.valid,
            combinedErrors,
            combinedWarnings
        );
    }
    
    /**
     * Gets a formatted message of all errors and warnings.
     */
    @NotNull
    public String getMessage() {
        var sb = new StringBuilder();
        
        if (!errors.isEmpty()) {
            sb.append("Errors: ").append(String.join(", ", errors));
        }
        
        if (!warnings.isEmpty()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("Warnings: ").append(String.join(", ", warnings));
        }
        
        return sb.length() > 0 ? sb.toString() : "Valid";
    }
    
    // ==================== Builder Pattern ====================
    
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        private Builder() {}

        public Builder addError(@NotNull String message) {
            errors.add(message);
            return this;
        }

        public Builder addWarning(@NotNull String message) {
            warnings.add(message);
            return this;
        }

        public Builder addErrorIf(boolean condition, @NotNull String message) {
            if (condition) errors.add(message);
            return this;
        }

        public Builder addWarningIf(boolean condition, @NotNull String message) {
            if (condition) warnings.add(message);
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(!errors.isEmpty() ? false : true, errors, warnings);
        }
    }
}
