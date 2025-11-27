package com.raindropcentral.rdq.shared.edition;

import org.jetbrains.annotations.NotNull;

public final class PremiumEditionFeatures implements EditionFeatures {

    private static final String EDITION_NAME = "Premium";
    private static final int DEFAULT_MAX_ACTIVE_RANK_TREES = 3;
    private static final int DEFAULT_MAX_ACTIVE_PERKS = 3;

    private final int maxActiveRankTrees;
    private final int maxActivePerks;

    public PremiumEditionFeatures() {
        this(DEFAULT_MAX_ACTIVE_RANK_TREES, DEFAULT_MAX_ACTIVE_PERKS);
    }

    public PremiumEditionFeatures(int maxActiveRankTrees, int maxActivePerks) {
        this.maxActiveRankTrees = maxActiveRankTrees;
        this.maxActivePerks = maxActivePerks;
    }

    @Override
    @NotNull
    public String getEditionName() {
        return EDITION_NAME;
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public int getMaxActiveRankTrees() {
        return maxActiveRankTrees;
    }

    @Override
    public boolean allowsCrossTreeSwitching() {
        return true;
    }

    @Override
    public int getMaxActivePerks() {
        return maxActivePerks;
    }

    @Override
    public boolean allowsAdvancedBountyDistribution() {
        return true;
    }

    @Override
    public boolean allowsMultipleBountyTargets() {
        return true;
    }

    @Override
    public boolean allowsPremiumPerks() {
        return true;
    }
}
