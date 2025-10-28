package com.raindropcentral.rdq.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActivationResult {

    private final boolean success;
    private final @Nullable String reason;

    private ActivationResult(boolean success, @Nullable String reason) {
        this.success = success;
        this.reason = reason;
    }

    public boolean success() {
        return success;
    }

    public @Nullable String reason() {
        return reason;
    }

    public static @NotNull ActivationResult succeed() {
        return new ActivationResult(true, null);
    }

    public static @NotNull ActivationResult failure(@NotNull String reason) {
        return new ActivationResult(false, reason);
    }
}
