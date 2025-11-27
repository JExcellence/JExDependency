package com.raindropcentral.rdq.shared.edition;

import org.jetbrains.annotations.NotNull;

public interface EditionFeatures {

    @NotNull
    String getEditionName();

    boolean isPremium();

    int getMaxActiveRankTrees();

    boolean allowsCrossTreeSwitching();

    int getMaxActivePerks();

    boolean allowsAdvancedBountyDistribution();

    boolean allowsMultipleBountyTargets();

    boolean allowsPremiumPerks();

    default boolean isFeatureAvailable(@NotNull String featureId) {
        return switch (featureId) {
            case "rank.multiple_trees" -> getMaxActiveRankTrees() > 1;
            case "rank.cross_tree_switching" -> allowsCrossTreeSwitching();
            case "perk.multiple_active" -> getMaxActivePerks() > 1;
            case "perk.premium_types" -> allowsPremiumPerks();
            case "bounty.advanced_distribution" -> allowsAdvancedBountyDistribution();
            case "bounty.multiple_targets" -> allowsMultipleBountyTargets();
            default -> !isPremium() || isPremium();
        };
    }
}
