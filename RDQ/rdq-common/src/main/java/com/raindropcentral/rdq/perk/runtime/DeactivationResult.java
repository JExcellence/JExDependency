package com.raindropcentral.rdq.perk.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DeactivationResult(
    boolean success,
    @Nullable String reason
) {

    public static @NotNull DeactivationResult success() {
        return new DeactivationResult(true, null);
    }

    public static @NotNull DeactivationResult failure(@NotNull String reason) {
        return new DeactivationResult(false, reason);
    }
}
