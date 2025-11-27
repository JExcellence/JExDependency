package com.raindropcentral.rdq.shared.error;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public sealed interface RDQError {

    record NotFound(
        @NotNull String type,
        @NotNull String id
    ) implements RDQError {
        public NotFound {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
        }
    }

    record InsufficientFunds(
        @NotNull BigDecimal required,
        @NotNull BigDecimal available
    ) implements RDQError {
        public InsufficientFunds {
            Objects.requireNonNull(required, "required");
            Objects.requireNonNull(available, "available");
        }
    }

    record OnCooldown(@NotNull Duration remaining) implements RDQError {
        public OnCooldown {
            Objects.requireNonNull(remaining, "remaining");
        }
    }

    record RequirementsNotMet(@NotNull List<String> missing) implements RDQError {
        public RequirementsNotMet {
            missing = missing != null ? List.copyOf(missing) : List.of();
        }
    }

    record SelfTargeting() implements RDQError {
    }

    record AlreadyExists(
        @NotNull String type,
        @NotNull String id
    ) implements RDQError {
        public AlreadyExists {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
        }
    }

    record Expired(
        @NotNull String type,
        @NotNull String id
    ) implements RDQError {
        public Expired {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
        }
    }

    record NotUnlocked(
        @NotNull String type,
        @NotNull String id
    ) implements RDQError {
        public NotUnlocked {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(id, "id");
        }
    }

    record PermissionDenied(@NotNull String permission) implements RDQError {
        public PermissionDenied {
            Objects.requireNonNull(permission, "permission");
        }
    }

    record InvalidState(@NotNull String message) implements RDQError {
        public InvalidState {
            Objects.requireNonNull(message, "message");
        }
    }

    record InvalidAmount(
        @NotNull BigDecimal minAmount,
        @NotNull BigDecimal maxAmount
    ) implements RDQError {
        public InvalidAmount {
            Objects.requireNonNull(minAmount, "minAmount");
            Objects.requireNonNull(maxAmount, "maxAmount");
        }
    }
}
