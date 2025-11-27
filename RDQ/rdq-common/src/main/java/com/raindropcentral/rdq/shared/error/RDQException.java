package com.raindropcentral.rdq.shared.error;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RDQException extends RuntimeException {

    private final RDQError error;

    public RDQException(@NotNull RDQError error) {
        super(formatMessage(error));
        this.error = Objects.requireNonNull(error, "error");
    }

    public RDQException(@NotNull RDQError error, @NotNull Throwable cause) {
        super(formatMessage(error), cause);
        this.error = Objects.requireNonNull(error, "error");
    }

    @NotNull
    public RDQError getError() {
        return error;
    }

    private static String formatMessage(@NotNull RDQError error) {
        return switch (error) {
            case RDQError.NotFound(var type, var id) -> type + " not found: " + id;
            case RDQError.InsufficientFunds(var req, var avail) -> "Insufficient funds: required " + req + ", available " + avail;
            case RDQError.OnCooldown(var remaining) -> "On cooldown: " + remaining.toSeconds() + "s remaining";
            case RDQError.RequirementsNotMet(var missing) -> "Requirements not met: " + String.join(", ", missing);
            case RDQError.SelfTargeting() -> "Cannot target yourself";
            case RDQError.AlreadyExists(var type, var id) -> type + " already exists: " + id;
            case RDQError.Expired(var type, var id) -> type + " has expired: " + id;
            case RDQError.NotUnlocked(var type, var id) -> type + " not unlocked: " + id;
            case RDQError.PermissionDenied(var perm) -> "Permission denied: " + perm;
            case RDQError.InvalidState(var msg) -> "Invalid state: " + msg;
            case RDQError.InvalidAmount(var min, var max) -> "Invalid amount: must be between " + min + " and " + max;
        };
    }
}
