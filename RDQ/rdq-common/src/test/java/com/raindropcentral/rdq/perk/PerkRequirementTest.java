package com.raindropcentral.rdq.perk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PerkRequirementTest {

    @Test
    void createRankRequired() {
        var req = new PerkRequirement.RankRequired("warrior_3");

        assertEquals("warrior_3", req.rankId());
    }

    @Test
    void rejectNullRankId() {
        assertThrows(NullPointerException.class, () ->
            new PerkRequirement.RankRequired(null)
        );
    }

    @Test
    void createPermissionRequired() {
        var req = new PerkRequirement.PermissionRequired("rdq.perk.fly");

        assertEquals("rdq.perk.fly", req.permission());
    }

    @Test
    void rejectNullPermission() {
        assertThrows(NullPointerException.class, () ->
            new PerkRequirement.PermissionRequired(null)
        );
    }

    @Test
    void createCurrencyRequired() {
        var req = new PerkRequirement.CurrencyRequired("coins", BigDecimal.valueOf(1000));

        assertEquals("coins", req.currency());
        assertEquals(BigDecimal.valueOf(1000), req.amount());
    }

    @Test
    void rejectNegativeCurrencyAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            new PerkRequirement.CurrencyRequired("coins", BigDecimal.valueOf(-100))
        );
    }

    @Test
    void createLevelRequired() {
        var req = new PerkRequirement.LevelRequired(30);

        assertEquals(30, req.level());
    }

    @Test
    void rejectNegativeLevel() {
        assertThrows(IllegalArgumentException.class, () ->
            new PerkRequirement.LevelRequired(-1)
        );
    }

    @Test
    void patternMatchingOnRequirements() {
        PerkRequirement req = new PerkRequirement.RankRequired("mage_2");

        var result = switch (req) {
            case PerkRequirement.RankRequired(var rankId) -> "rank:" + rankId;
            case PerkRequirement.PermissionRequired(var perm) -> "perm:" + perm;
            case PerkRequirement.CurrencyRequired(var currency, var amount) -> "currency:" + currency + ":" + amount;
            case PerkRequirement.LevelRequired(var level) -> "level:" + level;
        };

        assertEquals("rank:mage_2", result);
    }
}
