package com.raindropcentral.rdq.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ActivationResult(
    boolean success,
    @Nullable String reason
) {

    public static @NotNull ActivationResult success() {
        return new ActivationResult(true, null);
    }

    public static @NotNull ActivationResult failure(@NotNull String reason) {
        return new ActivationResult(false, reason);
    }
}
