package com.raindropcentral.rdq.reward;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.raindropcentral.rdq.reward.ExperienceReward.ExperienceType.LEVELS;
import static com.raindropcentral.rdq.reward.ExperienceReward.ExperienceType.POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceRewardTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("ExperienceRewardTestPlayer");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void itRejectsNonPositiveAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new ExperienceReward(0, LEVELS));
        assertThrows(IllegalArgumentException.class, () -> new ExperienceReward(-3, POINTS));
    }

    @Test
    void itGrantsLevelsWhenConfigured() {
        this.player.setLevel(4);

        final ExperienceReward reward = new ExperienceReward(3, LEVELS);

        final int initialLevel = this.player.getLevel();
        final int initialTotal = this.player.getTotalExperience();

        reward.apply(this.player);

        assertEquals(initialLevel + 3, this.player.getLevel(), "apply should add the configured levels");
        assertTrue(this.player.getTotalExperience() >= initialTotal,
                "Total experience should not decrease when awarding levels");
        assertEquals(3, reward.getAmount(), "getAmount should expose the configured amount");
        assertEquals(LEVELS, reward.getExperienceType(), "getExperienceType should report the configured type");
        assertEquals("reward.experience.levels", reward.getDescriptionKey(),
                "getDescriptionKey should describe level rewards");
    }

    @Test
    void itGrantsExperiencePointsWhenConfigured() {
        this.player.setLevel(0);
        this.player.setTotalExperience(10);

        final ExperienceReward reward = new ExperienceReward(25, POINTS);

        final int initialLevel = this.player.getLevel();
        final int initialTotal = this.player.getTotalExperience();

        reward.apply(this.player);

        assertEquals(initialTotal + 25, this.player.getTotalExperience(),
                "apply should add the configured experience points");
        assertTrue(this.player.getLevel() >= initialLevel,
                "Level should not decrease when awarding experience points");
        assertEquals(25, reward.getAmount(), "getAmount should expose the configured amount");
        assertEquals(POINTS, reward.getExperienceType(), "getExperienceType should report the configured type");
        assertEquals("reward.experience.points", reward.getDescriptionKey(),
                "getDescriptionKey should describe point rewards");
    }
}
