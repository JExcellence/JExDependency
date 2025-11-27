package com.raindropcentral.rdq.bounty.service;

import com.raindropcentral.rdq.api.PremiumBountyService;
import com.raindropcentral.rdq.bounty.DistributionMode;
import com.raindropcentral.rdq.bounty.config.BountyConfig;
import com.raindropcentral.rdq.bounty.economy.EconomyService;
import com.raindropcentral.rdq.bounty.repository.BountyRepository;
import com.raindropcentral.rdq.bounty.repository.HunterStatsRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class DefaultPremiumBountyService extends DefaultFreeBountyService implements PremiumBountyService {

    public DefaultPremiumBountyService(
        @NotNull BountyRepository bountyRepository,
        @NotNull HunterStatsRepository statsRepository,
        @NotNull EconomyService economyService,
        @NotNull BountyConfig config
    ) {
        super(bountyRepository, statsRepository, economyService, config);
    }

    @Override
    @NotNull
    public List<DistributionMode> getAvailableDistributionModes() {
        return Arrays.stream(DistributionMode.values())
            .filter(this::isDistributionModeEnabled)
            .toList();
    }

    @Override
    public boolean isDistributionModeEnabled(@NotNull DistributionMode mode) {
        return switch (mode) {
            case INSTANT -> config.instantDistributionEnabled();
            case CHEST -> config.chestDistributionEnabled();
            case DROP -> config.dropDistributionEnabled();
            case VIRTUAL -> config.virtualDistributionEnabled();
        };
    }

    @Override
    protected DistributionMode getDistributionMode() {
        return config.defaultDistributionMode();
    }
}
