package com.raindropcentral.rdq.shared.edition;

import org.jetbrains.annotations.NotNull;

public final class FreeEditionFeatures implements EditionFeatures {

    private static final String EDITION_NAME = "Free";
    private static final int MAX_ACTIVE_RANK_TREES = 1;
    private static final int MAX_ACTIVE_PERKS = 1;

    @Override
    @NotNull
    public String getEditionName() {
        return EDITION_NAME;
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    @Override
    public int getMaxActiveRankTrees() {
        return MAX_ACTIVE_RANK_TREES;
    }

    @Override
    public boolean allowsCrossTreeSwitching() {
        return false;
    }

    @Override
    public int getMaxActivePerks() {
        return MAX_ACTIVE_PERKS;
    }

    @Override
    public boolean allowsAdvancedBountyDistribution() {
        return false;
    }

    @Override
    public boolean allowsMultipleBountyTargets() {
        return false;
    }

    @Override
    public boolean allowsPremiumPerks() {
        return false;
    }
}
