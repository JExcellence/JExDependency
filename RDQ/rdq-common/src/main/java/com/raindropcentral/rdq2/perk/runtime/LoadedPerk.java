package com.raindropcentral.rdq2.perk.runtime;

import com.raindropcentral.rdq2.perk.config.PerkConfig;
import org.jetbrains.annotations.NotNull;

public record LoadedPerk(
    @NotNull PerkConfig config,
    @NotNull PerkType type
) {

    public @NotNull String getId() {
        return config.id();
    }

    public @NotNull String getDisplayName() {
        return config.displayName();
    }

    public @NotNull String getTypeId() {
        return type.getTypeId();
    }
}
