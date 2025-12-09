/*
package com.raindropcentral.rdq2.perk.service;

import com.raindropcentral.rdq2.api.PremiumPerkService;
import com.raindropcentral.rdq2.perk.repository.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class DefaultPremiumPerkService extends DefaultFreePerkService implements PremiumPerkService {

    private static final Logger LOGGER = Logger.getLogger(DefaultPremiumPerkService.class.getName());
    private static final int DEFAULT_MAX_ACTIVE_PERKS = 3;
    private static final Set<String> PREMIUM_PERK_IDS = Set.of(
        "fly", "double_experience", "prevent_death", "treasure_hunter", "vampire"
    );

    public DefaultPremiumPerkService(
        @NotNull PerkRepository perkRepository,
        @NotNull PlayerPerkRepository playerPerkRepository,
        @NotNull PerkRequirementChecker requirementChecker
    ) {
        super(perkRepository, playerPerkRepository, requirementChecker);
    }

    @Override
    protected int getMaxActivePerks() {
        return DEFAULT_MAX_ACTIVE_PERKS;
    }

    @Override
    public CompletableFuture<Integer> getMaxActivePerks(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            var player = org.bukkit.Bukkit.getPlayer(playerId);
            if (player == null) {
                return DEFAULT_MAX_ACTIVE_PERKS;
            }
            if (player.hasPermission("rdq.perk.slots.5")) {
                return 5;
            }
            if (player.hasPermission("rdq.perk.slots.4")) {
                return 4;
            }
            return DEFAULT_MAX_ACTIVE_PERKS;
        });
    }

    @Override
    public CompletableFuture<Boolean> hasPremiumPerkAccess(@NotNull UUID playerId, @NotNull String perkId) {
        if (!PREMIUM_PERK_IDS.contains(perkId)) {
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.supplyAsync(() -> {
            var player = org.bukkit.Bukkit.getPlayer(playerId);
            return player != null && player.hasPermission("rdq.perk.premium." + perkId);
        });
    }
}
*/
