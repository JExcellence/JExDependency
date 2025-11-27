package com.raindropcentral.rdq.rank.config;

import org.jetbrains.annotations.NotNull;

public record RankSystemConfig(
    boolean enabled,
    boolean linearProgression,
    boolean allowSkipping,
    int maxActiveTrees,
    boolean crossTreeSwitching,
    long switchingCooldownSeconds,
    @NotNull NotificationConfig notifications
) {
    public RankSystemConfig {
        if (maxActiveTrees < 1) {
            throw new IllegalArgumentException("maxActiveTrees must be at least 1");
        }
        if (switchingCooldownSeconds < 0) {
            throw new IllegalArgumentException("switchingCooldownSeconds must be non-negative");
        }
    }

    public static RankSystemConfig defaults() {
        return new RankSystemConfig(
            true,
            true,
            false,
            1,
            false,
            1728000,
            NotificationConfig.defaults()
        );
    }

    public record NotificationConfig(
        boolean titleEnabled,
        boolean subtitleEnabled,
        boolean actionbarEnabled,
        boolean soundEnabled,
        @NotNull String unlockSound,
        boolean broadcastEnabled
    ) {
        public static NotificationConfig defaults() {
            return new NotificationConfig(
                true,
                true,
                false,
                true,
                "ENTITY_PLAYER_LEVELUP",
                true
            );
        }
    }
}
