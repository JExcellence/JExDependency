package com.raindropcentral.rdq.perk.config;

import org.jetbrains.annotations.NotNull;

public sealed interface PerkRequirementConfig permits
        PerkRequirementConfig.LevelRequirementConfig,
        PerkRequirementConfig.PermissionRequirementConfig,
        PerkRequirementConfig.PlaytimeRequirementConfig,
        PerkRequirementConfig.ItemRequirementConfig {

    @NotNull String type();

    record LevelRequirementConfig(
        @NotNull String type,
        int level
    ) implements PerkRequirementConfig {
        public LevelRequirementConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
        }
    }

    record PermissionRequirementConfig(
        @NotNull String type,
        @NotNull String permission
    ) implements PerkRequirementConfig {
        public PermissionRequirementConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            if (permission == null) throw new IllegalArgumentException("permission cannot be null");
        }
    }

    record PlaytimeRequirementConfig(
        @NotNull String type,
        long requiredPlaytimeHours
    ) implements PerkRequirementConfig {
        public PlaytimeRequirementConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
        }
    }

    record ItemRequirementConfig(
        @NotNull String type,
        @NotNull String itemType,
        int amount
    ) implements PerkRequirementConfig {
        public ItemRequirementConfig {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            if (itemType == null) throw new IllegalArgumentException("itemType cannot be null");
        }
    }
}
