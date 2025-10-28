package com.raindropcentral.rdq.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeactivationResult {

    private final boolean success;
    private final @Nullable String reason;

    private DeactivationResult(boolean success, @Nullable String reason) {
        this.success = success;
        this.reason = reason;
    }

    public boolean success() {
        return success;
    }

    public @Nullable String reason() {
        return reason;
    }

    public static @NotNull DeactivationResult succeed() {
        return new DeactivationResult(true, null);
    }

    public static @NotNull DeactivationResult failed(@NotNull String reason) {
        return new DeactivationResult(false, reason);
    }
}
