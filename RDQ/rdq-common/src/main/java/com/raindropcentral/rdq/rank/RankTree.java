package com.raindropcentral.rdq.rank;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Record representing a rank progression tree.
 *
 * <p>A rank tree contains an ordered list of ranks that players can
 * progress through. Examples include warrior, cleric, mage, etc.
 *
 * @param id unique identifier for the tree
 * @param displayNameKey translation key for the display name
 * @param descriptionKey translation key for the description
 * @param iconMaterial material for GUI display
 * @param displayOrder order in GUI listings
 * @param enabled whether this tree is active
 * @param ranks ordered list of ranks in this tree
 * @see Rank
 */
public record RankTree(
    @NotNull String id,
    @NotNull String displayNameKey,
    @NotNull String descriptionKey,
    @NotNull String iconMaterial,
    int displayOrder,
    boolean enabled,
    @NotNull List<Rank> ranks
) {
    public RankTree {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayNameKey, "displayNameKey");
        Objects.requireNonNull(descriptionKey, "descriptionKey");
        Objects.requireNonNull(iconMaterial, "iconMaterial");
        ranks = ranks != null ? List.copyOf(ranks) : List.of();
    }

    public int rankCount() {
        return ranks.size();
    }

    public int maxTier() {
        return ranks.stream()
            .mapToInt(Rank::tier)
            .max()
            .orElse(0);
    }
}
