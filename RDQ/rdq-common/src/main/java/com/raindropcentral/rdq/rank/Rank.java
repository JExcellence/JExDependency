package com.raindropcentral.rdq.rank;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Record representing a rank within a rank tree.
 *
 * <p>Ranks have tiers (progression level), weights (for sorting), and
 * requirements that must be met to unlock them.
 *
 * @param id unique identifier for the rank
 * @param treeId the rank tree this rank belongs to
 * @param displayNameKey translation key for the display name
 * @param descriptionKey translation key for the description
 * @param tier the progression tier (higher = more advanced)
 * @param weight sorting weight within the tier
 * @param luckPermsGroup optional LuckPerms group to assign
 * @param prefixKey optional translation key for chat prefix
 * @param suffixKey optional translation key for chat suffix
 * @param iconMaterial material for GUI display
 * @param enabled whether this rank is active
 * @param requirements list of requirements to unlock
 * @see RankTree
 * @see RankRequirement
 */
public record Rank(
    @NotNull String id,
    @NotNull String treeId,
    @NotNull String displayNameKey,
    @NotNull String descriptionKey,
    int tier,
    int weight,
    @Nullable String luckPermsGroup,
    @Nullable String prefixKey,
    @Nullable String suffixKey,
    @NotNull String iconMaterial,
    boolean enabled,
    @NotNull List<RankRequirement> requirements
) {
    public Rank {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(treeId, "treeId");
        Objects.requireNonNull(displayNameKey, "displayNameKey");
        Objects.requireNonNull(descriptionKey, "descriptionKey");
        Objects.requireNonNull(iconMaterial, "iconMaterial");
        requirements = requirements != null ? List.copyOf(requirements) : List.of();
    }

    public boolean hasLuckPermsGroup() {
        return luckPermsGroup != null && !luckPermsGroup.isBlank();
    }

    public boolean hasPrefix() {
        return prefixKey != null && !prefixKey.isBlank();
    }

    public boolean hasSuffix() {
        return suffixKey != null && !suffixKey.isBlank();
    }
}
