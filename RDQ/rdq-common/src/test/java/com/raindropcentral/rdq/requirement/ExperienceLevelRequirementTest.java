package com.raindropcentral.rdq.requirement;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static com.raindropcentral.rdq.requirement.ExperienceLevelRequirement.ExperienceType.LEVEL;
import static com.raindropcentral.rdq.requirement.ExperienceLevelRequirement.ExperienceType.POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceLevelRequirementTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("ExperienceRequirementPlayer");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldConsumeLevelsWhenConfigured() {
        this.player.setLevel(7);

        final ExperienceLevelRequirement requirement = new ExperienceLevelRequirement(5, LEVEL, true, null);

        assertTrue(requirement.isLevelBased());
        assertFalse(requirement.isPointsBased());
        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0, requirement.calculateProgress(this.player));

        requirement.consume(this.player);

        assertEquals(2, this.player.getLevel(), "Consumption should remove the required levels");
        assertFalse(requirement.isMet(this.player), "Player should no longer meet the requirement after consuming levels");
        assertEquals(0.4, requirement.calculateProgress(this.player), 1.0e-9);
    }

    @Test
    void shouldRespectLevelRequirementWithoutConsumption() {
        this.player.setLevel(7);

        final ExperienceLevelRequirement requirement = new ExperienceLevelRequirement(5, LEVEL, false, null);

        assertTrue(requirement.isMet(this.player));
        requirement.consume(this.player);

        assertEquals(7, this.player.getLevel(), "Level should remain untouched when consumption is disabled");
        assertTrue(requirement.isMet(this.player));
    }

    @Test
    void shouldConsumeExperiencePointsAndRestoreState() {
        this.player.setLevel(10);
        this.player.setExp(0.5f);
        this.player.setTotalExperience(200);

        final ExperienceLevelRequirement requirement = new ExperienceLevelRequirement(150, POINTS, true, null);

        assertFalse(requirement.isLevelBased());
        assertTrue(requirement.isPointsBased());
        assertTrue(requirement.isMet(this.player));
        assertEquals(1.0, requirement.calculateProgress(this.player));

        requirement.consume(this.player);

        assertEquals(50, this.player.getTotalExperience(),
                "Consumption should deduct points and restore the remaining total experience");
        assertTrue(this.player.getLevel() >= 0, "Level should remain valid after consumption");
    }

    @Test
    void shouldReportHelperValuesCorrectly() {
        final ExperienceLevelRequirement levelRequirement = new ExperienceLevelRequirement(5, LEVEL, false, null);
        this.player.setLevel(3);

        assertEquals(3, levelRequirement.getCurrentExperience(this.player));
        assertEquals(2, levelRequirement.getShortage(this.player));
        assertEquals(0.6, levelRequirement.calculateProgress(this.player), 1.0e-9);

        final ExperienceLevelRequirement pointsRequirement = new ExperienceLevelRequirement(50, POINTS, false, null);
        this.player.setTotalExperience(30);

        assertEquals(30, pointsRequirement.getCurrentExperience(this.player));
        assertEquals(20, pointsRequirement.getShortage(this.player));
        assertEquals(0.6, pointsRequirement.calculateProgress(this.player), 1.0e-9);
    }

    @Test
    void shouldValidateState() throws Exception {
        final ExperienceLevelRequirement requirement = new ExperienceLevelRequirement(5, LEVEL, false, null);

        final Field requiredLevelField = ExperienceLevelRequirement.class.getDeclaredField("requiredLevel");
        requiredLevelField.setAccessible(true);
        requiredLevelField.setInt(requirement, -1);

        assertThrows(IllegalStateException.class, requirement::validate,
                "validate should reject negative required levels");

        requiredLevelField.setInt(requirement, 5);

        final Field experienceTypeField = ExperienceLevelRequirement.class.getDeclaredField("experienceType");
        experienceTypeField.setAccessible(true);
        experienceTypeField.set(requirement, null);

        assertThrows(IllegalStateException.class, requirement::validate,
                "validate should reject a null experience type");
    }

    @Test
    void shouldRejectInvalidExperienceTypeToken() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ExperienceLevelRequirement.fromString(10, "invalid", true));

        assertTrue(exception.getMessage().contains("Invalid experience type"));
    }
}
