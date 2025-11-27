package com.raindropcentral.rdq.perk.config;

import org.jetbrains.annotations.NotNull;

public record PerkConfig(
    boolean enabled,
    int maxActivePerks,
    boolean allowMultipleSameCategory,
    int defaultCooldownSeconds,
    int defaultDurationSeconds,
    boolean requireUnlockBeforeActivation,
    @NotNull NotificationConfig notifications
) {
    @NotNull
    public static PerkConfig defaults() {
        return new PerkConfig(
            true,
            1,
            false,
            300,
            60,
            true,
            NotificationConfig.defaults()
        );
    }

    public record NotificationConfig(
        boolean activationEnabled,
        boolean deactivationEnabled,
        boolean unlockEnabled,
        boolean cooldownWarningEnabled,
        boolean soundEnabled,
        @NotNull String activationSound,
        @NotNull String deactivationSound,
        @NotNull String unlockSound
    ) {
        @NotNull
        public static NotificationConfig defaults() {
            return new NotificationConfig(
                true,
                true,
                true,
                true,
                true,
                "BLOCK_BEACON_ACTIVATE",
                "BLOCK_BEACON_DEACTIVATE",
                "ENTITY_PLAYER_LEVELUP"
            );
        }
    }
}
