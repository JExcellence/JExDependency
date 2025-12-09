package com.raindropcentral.rdq2.perk.config;

import org.jetbrains.annotations.NotNull;

/**
 * System-wide configuration for the perk system.
 */
public record PerkSystemConfig(
    boolean enabled,
    int maxActivePerks,
    boolean allowMultipleSameCategory,
    int defaultCooldownSeconds,
    int defaultDurationSeconds,
    boolean requireUnlockBeforeActivation,
    @NotNull NotificationConfig notifications
) {
    
    public static PerkSystemConfig defaults() {
        return new PerkSystemConfig(
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
