package com.raindropcentral.rdq2.perk.config;

import com.raindropcentral.rdq2.type.EPerkCategory;
import com.raindropcentral.rdq2.type.EPerkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record PerkConfig(
    @NotNull String id,
    @NotNull String displayName,
    @Nullable String description,
    @NotNull EPerkType perkType,
    @NotNull EPerkCategory category,
    @NotNull String iconMaterial,
    int priority,
    boolean enabled,
    @Nullable Long cooldownSeconds,
    @Nullable Long durationSeconds,
    @NotNull Map<String, Object> metadata,
    @NotNull List<PerkRequirementConfig> requirements,
    @NotNull List<PerkRewardConfig> rewards,
    @NotNull Map<String, Long> permissionCooldowns,
    @NotNull Map<String, Integer> permissionAmplifiers
) {
}
