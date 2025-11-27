package com.raindropcentral.rdq.fixtures;

import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.BountyStatus;
import com.raindropcentral.rdq.bounty.HunterStats;
import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PerkEffect;
import com.raindropcentral.rdq.perk.PerkType;
import com.raindropcentral.rdq.perk.PlayerPerkState;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.rank.PlayerRankData;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankRequirement;
import com.raindropcentral.rdq.rank.RankTree;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestData {

    private TestData() {
    }

    @NotNull
    public static RDQPlayer player(@NotNull String name) {
        return RDQPlayer.create(UUID.randomUUID(), name);
    }

    @NotNull
    public static RDQPlayer player(@NotNull UUID id, @NotNull String name) {
        return RDQPlayer.create(id, name);
    }

    @NotNull
    public static Bounty activeBounty(@NotNull UUID placerId, @NotNull UUID targetId, double amount) {
        return Bounty.create(
            placerId,
            targetId,
            BigDecimal.valueOf(amount),
            "coins",
            Instant.now().plus(Duration.ofDays(7))
        );
    }

    @NotNull
    public static Bounty claimedBounty(@NotNull UUID placerId, @NotNull UUID targetId, @NotNull UUID hunterId, double amount) {
        return new Bounty(
            1L,
            placerId,
            targetId,
            BigDecimal.valueOf(amount),
            "coins",
            BountyStatus.CLAIMED,
            Instant.now().minus(Duration.ofHours(1)),
            Instant.now().plus(Duration.ofDays(7)),
            hunterId,
            Instant.now()
        );
    }

    @NotNull
    public static HunterStats hunterStats(@NotNull UUID playerId, @NotNull String name, int claimed, int placed) {
        return new HunterStats(
            playerId,
            name,
            placed,
            claimed,
            0,
            BigDecimal.valueOf(claimed * 100),
            BigDecimal.valueOf(placed * 100)
        );
    }

    @NotNull
    public static RankTree rankTree(@NotNull String id) {
        return new RankTree(
            id,
            "rank.tree." + id + ".name",
            "rank.tree." + id + ".description",
            "DIAMOND_SWORD",
            1,
            true,
            List.of(
                rank(id, id + "_1", 1),
                rank(id, id + "_2", 2),
                rank(id, id + "_3", 3)
            )
        );
    }

    @NotNull
    public static Rank rank(@NotNull String treeId, @NotNull String rankId, int tier) {
        return new Rank(
            rankId,
            treeId,
            "rank." + rankId + ".name",
            "rank." + rankId + ".description",
            tier,
            tier * 10,
            rankId + "_group",
            "rank." + rankId + ".prefix",
            null,
            "IRON_INGOT",
            true,
            tier > 1 ? List.of(new RankRequirement.PreviousRankRequirement(treeId + "_" + (tier - 1))) : List.of()
        );
    }

    @NotNull
    public static PlayerRankData playerRankData(@NotNull UUID playerId, @NotNull String treeId, @NotNull String currentRankId) {
        return new PlayerRankData(
            playerId,
            List.of(new PlayerRankData.ActivePath(treeId, currentRankId, Instant.now())),
            Map.of(currentRankId, Instant.now())
        );
    }

    @NotNull
    public static Perk toggleablePerk(@NotNull String id) {
        return new Perk(
            id,
            "perk." + id + ".name",
            "perk." + id + ".description",
            new PerkType.Toggleable(),
            "combat",
            300,
            60,
            true,
            new PerkEffect.PotionEffect("SPEED", 1),
            "SUGAR",
            List.of()
        );
    }

    @NotNull
    public static Perk eventBasedPerk(@NotNull String id, @NotNull String eventType) {
        return new Perk(
            id,
            "perk." + id + ".name",
            "perk." + id + ".description",
            new PerkType.EventBased(eventType),
            "utility",
            600,
            0,
            true,
            new PerkEffect.ExperienceMultiplier(2.0),
            "EXPERIENCE_BOTTLE",
            List.of()
        );
    }

    @NotNull
    public static PlayerPerkState unlockedPerkState(@NotNull UUID playerId, @NotNull String perkId) {
        return new PlayerPerkState(playerId, perkId, true, false, null);
    }

    @NotNull
    public static PlayerPerkState activePerkState(@NotNull UUID playerId, @NotNull String perkId) {
        return new PlayerPerkState(playerId, perkId, true, true, null);
    }

    @NotNull
    public static PlayerPerkState cooldownPerkState(@NotNull UUID playerId, @NotNull String perkId, @NotNull Duration remaining) {
        return new PlayerPerkState(playerId, perkId, true, false, Instant.now().plus(remaining));
    }
}
