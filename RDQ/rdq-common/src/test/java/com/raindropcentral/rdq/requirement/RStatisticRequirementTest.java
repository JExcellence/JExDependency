package com.raindropcentral.rdq.requirement;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rdq.requirement.RStatisticRequirement.RequirementMode;
import com.raindropcentral.rdq.service.RCoreBridge;
import com.raindropcentral.rplatform.statistic.StatisticType;
import com.raindropcentral.rplatform.type.EStatisticType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RStatisticRequirementTest {

    private ServerMock server;
    private PlayerMock player;
    private RCoreBridge bridge;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("RStatisticRequirementTest");
        this.bridge = mock(RCoreBridge.class);
        RStatisticRequirement.setBridgeSupplier(() -> this.bridge);
    }

    @AfterEach
    void tearDown() {
        RStatisticRequirement.clearBridgeSupplier();
        MockBukkit.unmock();
    }

    @Test
    void shouldEvaluateAbsoluteModeWithNumberValues() {
        final RStatisticRequirement requirement = new RStatisticRequirement(
                "RDQ",
                "kills_zombie",
                20.0,
                RequirementMode.ABSOLUTE,
                null,
                StatisticType.DataType.NUMBER,
                null,
                200L
        );

        stubStatisticValue(requirement, 25.0);

        assertTrue(requirement.isMet(this.player), "isMet should return true when the statistic exceeds the requirement");
        assertEquals(1.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Progress should clamp to 1.0 when current value exceeds the target");

        stubStatisticValue(requirement, 5.0);

        assertFalse(requirement.isMet(this.player), "isMet should return false when the statistic is below the requirement");
        assertEquals(0.25, requirement.calculateProgress(this.player), 1.0e-6,
                "Progress should reflect the ratio between current and target values");
    }

    @Test
    void shouldTrackRelativeModeInitializationAndConsumption() {
        final RStatisticRequirement requirement = new RStatisticRequirement(
                "RDQ",
                "blocks_broken_diamond",
                10.0,
                RequirementMode.RELATIVE,
                "DIAMOND",
                StatisticType.DataType.NUMBER,
                null,
                200L
        );

        queueStatisticValues(requirement, new double[]{20.0, 20.0, 35.0, 35.0, 40.0, 42.0});

        assertFalse(requirement.isMet(this.player), "Initial evaluation should fail before any progress is made");
        assertNotNull(requirement.getStartingValue(), "Starting value should be initialized after first evaluation");
        assertEquals(20.0, requirement.getStartingValue(), 1.0e-6,
                "Starting value should reflect the initial statistic returned by the bridge");

        assertTrue(requirement.isMet(this.player), "Requirement should pass once the gained statistic exceeds the threshold");
        assertEquals(1.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Progress should clamp to 1.0 when gained value exceeds the relative target");

        requirement.consume(this.player);
        assertEquals(40.0, requirement.getStartingValue(), 1.0e-6,
                "Consume should refresh the starting value to the latest statistic");

        assertEquals(0.2, requirement.calculateProgress(this.player), 1.0e-6,
                "Progress should be recalculated from the updated starting value");
    }

    @Test
    void shouldConvertVariousValueTypes() {
        final RStatisticRequirement requirement = new RStatisticRequirement(
                "RDQ",
                "miscellaneous_stat",
                1.0,
                RequirementMode.ABSOLUTE,
                null,
                StatisticType.DataType.NUMBER,
                null,
                200L
        );

        when(this.bridge.findStatisticValueAsync(eq(this.player.getUniqueId()), eq(requirement.getIdentifier()), eq(requirement.getPlugin())))
                .thenReturn(
                        completed(0.5),
                        completed(true),
                        completed(false),
                        completed("0.25"),
                        completed("non_numeric"),
                        completed("")
                );

        assertEquals(0.5, requirement.calculateProgress(this.player), 1.0e-6,
                "Numeric values should convert directly to their double representation");
        assertEquals(1.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Boolean true should convert to 1.0");
        assertEquals(0.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Boolean false should convert to 0.0");
        assertEquals(0.25, requirement.calculateProgress(this.player), 1.0e-6,
                "Numeric strings should parse to their double value");
        assertEquals(1.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Non-numeric strings should default to 1.0 when not empty");
        assertEquals(0.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Empty strings should convert to 0.0");
    }

    @Test
    void shouldBuildDescriptionKeyBasedOnConfiguration() {
        final RStatisticRequirement requirement = new RStatisticRequirement(
                "RDQ",
                "Kills-Dragon Rage",
                5.0,
                RequirementMode.RELATIVE,
                "Dragon Rage",
                StatisticType.DataType.STRING,
                null,
                150L
        );

        assertEquals("requirement.statistic.relative.string.kills_dragon_rage.dragon_rage", requirement.getDescriptionKey(),
                "Description key should incorporate the mode, data type, identifier, and qualifier");
    }

    @Test
    void shouldCreateFactoryRequirementsWithExpectedConfiguration() {
        final RStatisticRequirement killRequirement = RStatisticRequirement.createKillRequirement("Zombie", 3, RequirementMode.RELATIVE);
        assertEquals(EStatisticType.StatisticCategory.RDQ.name(), killRequirement.getPlugin(),
                "Kill requirement should scope to the RDQ statistic namespace");
        assertEquals("kills_zombie", killRequirement.getIdentifier(),
                "Kill requirement should normalize the identifier using the entity type");
        assertEquals("Zombie", killRequirement.getQualifier(),
                "Kill requirement should retain the entity type as qualifier");

        final RStatisticRequirement blockRequirement = RStatisticRequirement.createBlockBreakRequirement("Obsidian", 12, RequirementMode.ABSOLUTE);
        assertEquals("blocks_broken_obsidian", blockRequirement.getIdentifier(),
                "Block requirement should prefix the identifier with blocks_broken_");
        assertEquals("Obsidian", blockRequirement.getQualifier(),
                "Block requirement should retain the block type as qualifier");

        final RStatisticRequirement playtimeRequirement = RStatisticRequirement.createPlaytimeRequirement(240, RequirementMode.ABSOLUTE);
        assertEquals("playtime_minutes", playtimeRequirement.getIdentifier(),
                "Playtime requirement should rely on the playtime_minutes identifier");
        assertNull(playtimeRequirement.getQualifier(),
                "Playtime requirement should not define a qualifier");
    }

    @Test
    void shouldFallbackToZeroWhenBridgeUnavailableOrTimeout() {
        final RStatisticRequirement requirement = new RStatisticRequirement(
                "RDQ",
                "playtime_minutes",
                60.0,
                RequirementMode.ABSOLUTE,
                null,
                StatisticType.DataType.NUMBER,
                null,
                25L
        );

        final CompletableFuture<Optional<Object>> slowFuture = new CompletableFuture<>();
        when(this.bridge.findStatisticValueAsync(eq(this.player.getUniqueId()), eq(requirement.getIdentifier()), eq(requirement.getPlugin())))
                .thenReturn(slowFuture);

        assertFalse(requirement.isMet(this.player), "Timeouts should resolve to unmet requirements");
        assertEquals(0.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Timeouts should resolve to zero progress");

        RStatisticRequirement.setBridgeSupplier(() -> null);

        assertFalse(requirement.isMet(this.player), "Null bridge suppliers should degrade to unmet requirements");
        assertEquals(0.0, requirement.calculateProgress(this.player), 1.0e-6,
                "Null bridge suppliers should degrade to zero progress");
    }

    private void stubStatisticValue(final RStatisticRequirement requirement, final Object value) {
        when(this.bridge.findStatisticValueAsync(eq(this.player.getUniqueId()), eq(requirement.getIdentifier()), eq(requirement.getPlugin())))
                .thenReturn(completed(value));
    }

    private void queueStatisticValues(final RStatisticRequirement requirement, final double[] values) {
        final Deque<CompletableFuture<Optional<Object>>> responses = new ArrayDeque<>();
        for (final double value : values) {
            responses.add(completed(value));
        }
        when(this.bridge.findStatisticValueAsync(eq(this.player.getUniqueId()), eq(requirement.getIdentifier()), eq(requirement.getPlugin())))
                .thenAnswer(invocation -> {
                    final CompletableFuture<Optional<Object>> response = responses.pollFirst();
                    if (response == null) {
                        return completed(null);
                    }
                    return response;
                });
    }

    private CompletableFuture<Optional<Object>> completed(final Object value) {
        return CompletableFuture.completedFuture(Optional.ofNullable(value));
    }
}
