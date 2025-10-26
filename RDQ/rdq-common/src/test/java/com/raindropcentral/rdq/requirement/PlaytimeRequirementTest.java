package com.raindropcentral.rdq.requirement;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

class PlaytimeRequirementTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("PlaytimeRequirementTest");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void itEvaluatesTotalPlaytimeAgainstRequirement() {
        final long requiredSeconds = TimeUnit.MINUTES.toSeconds(10);
        final PlaytimeRequirement requirement = new PlaytimeRequirement(requiredSeconds);

        setPlaytimeSeconds(this.player, TimeUnit.MINUTES.toSeconds(2));
        assertFalse(requirement.isMet(this.player), "isMet should return false when total playtime is below the threshold");
        assertEquals(0.2d, requirement.calculateProgress(this.player), 1.0e-6,
                "calculateProgress should report the ratio of accumulated playtime to the requirement");

        setPlaytimeSeconds(this.player, TimeUnit.MINUTES.toSeconds(12));
        assertTrue(requirement.isMet(this.player), "isMet should return true when the player meets the total playtime threshold");
        assertEquals(1.0d, requirement.calculateProgress(this.player), 1.0e-6,
                "calculateProgress should cap the result at 1.0 when the requirement is satisfied");
    }

    @Test
    void itEvaluatesWorldSpecificPlaytimeRequirements() {
        final Map<String, Long> worldRequirements = new LinkedHashMap<>();
        worldRequirements.put("alpha", 120L);
        worldRequirements.put("beta", 90L);

        final PlaytimeRequirement requirement = spy(new PlaytimeRequirement(0, worldRequirements, false, "World Specific"));

        final Map<String, Long> worldPlaytime = new HashMap<>();
        worldPlaytime.put("alpha", 150L);
        worldPlaytime.put("beta", 100L);

        doAnswer(invocation -> {
            final String worldName = invocation.getArgument(1);
            return worldPlaytime.getOrDefault(worldName, 0L);
        }).when(requirement).getWorldPlaytimeSeconds(any(Player.class), anyString());

        assertTrue(requirement.isMet(this.player),
                "isMet should return true when each world meets or exceeds its configured requirement");
        assertEquals(1.0d, requirement.calculateProgress(this.player), 1.0e-6,
                "calculateProgress should report full progress when every world requirement is satisfied");

        worldPlaytime.put("beta", 30L);
        assertFalse(requirement.isMet(this.player),
                "isMet should return false when any world falls below its configured requirement");
        assertEquals((1.0d + (30.0d / 90.0d)) / 2.0d, requirement.calculateProgress(this.player), 1.0e-6,
                "calculateProgress should average the per-world progress ratios");
    }

    @Test
    void itFormatsDurations() {
        assertEquals("0s", PlaytimeRequirement.formatDuration(0),
                "formatDuration should represent non-positive values as 0s");
        assertEquals("45s", PlaytimeRequirement.formatDuration(45),
                "formatDuration should display only seconds when less than a minute");
        assertEquals("1h 5s", PlaytimeRequirement.formatDuration(3605),
                "formatDuration should include hours and remaining seconds");

        final long complexDuration = TimeUnit.DAYS.toSeconds(1)
                + TimeUnit.HOURS.toSeconds(2)
                + TimeUnit.MINUTES.toSeconds(30);
        assertEquals("1d 2h 30m", PlaytimeRequirement.formatDuration(complexDuration),
                "formatDuration should include days, hours, and minutes when applicable");
    }

    @Test
    void itFormatsRequiredAndCurrentPlaytime() {
        final long requiredSeconds = TimeUnit.MINUTES.toSeconds(10);
        final PlaytimeRequirement totalRequirement = new PlaytimeRequirement(requiredSeconds);

        assertEquals("10m", totalRequirement.getFormattedRequiredPlaytime(),
                "getFormattedRequiredPlaytime should format the total requirement duration");

        setPlaytimeSeconds(this.player, TimeUnit.HOURS.toSeconds(2));
        assertEquals("2h", totalRequirement.getFormattedCurrentPlaytime(this.player),
                "getFormattedCurrentPlaytime should format the player's total playtime");

        final Map<String, Long> worldRequirements = new LinkedHashMap<>();
        worldRequirements.put("alpha", TimeUnit.MINUTES.toSeconds(2));
        worldRequirements.put("beta", 90L);
        final PlaytimeRequirement worldRequirement = spy(new PlaytimeRequirement(0, worldRequirements, false, null));

        final Map<String, Long> worldPlaytime = new HashMap<>();
        worldPlaytime.put("alpha", TimeUnit.MINUTES.toSeconds(2) + 30L);
        worldPlaytime.put("beta", 45L);

        doAnswer(invocation -> {
            final String worldName = invocation.getArgument(1);
            return worldPlaytime.getOrDefault(worldName, 0L);
        }).when(worldRequirement).getWorldPlaytimeSeconds(any(Player.class), anyString());

        final Set<String> requiredParts = toPartSet(worldRequirement.getFormattedRequiredPlaytime());
        assertEquals(Set.of("alpha: 2m", "beta: 1m 30s"), requiredParts,
                "World-based formatted requirements should include each world entry");

        final Set<String> currentParts = toPartSet(worldRequirement.getFormattedCurrentPlaytime(this.player));
        assertEquals(Set.of("alpha: 2m 30s", "beta: 45s"), currentParts,
                "World-based formatted current playtime should include formatted per-world values");
    }

    @Test
    void itExposesConfiguredAccessors() {
        final Map<String, Long> worldRequirements = new HashMap<>();
        worldRequirements.put("alpha", 60L);
        final PlaytimeRequirement requirement = new PlaytimeRequirement(TimeUnit.HOURS.toSeconds(1), worldRequirements, true, "Desc");

        assertEquals(TimeUnit.HOURS.toSeconds(1), requirement.getRequiredPlaytimeSeconds(),
                "getRequiredPlaytimeSeconds should return the configured total requirement");
        assertEquals(TimeUnit.SECONDS.toMinutes(TimeUnit.HOURS.toSeconds(1)), requirement.getRequiredPlaytimeMinutes(),
                "getRequiredPlaytimeMinutes should convert seconds to minutes");
        assertEquals(1L, requirement.getRequiredPlaytimeHours(),
                "getRequiredPlaytimeHours should convert seconds to hours");
        assertEquals(0L, requirement.getRequiredPlaytimeDays(),
                "getRequiredPlaytimeDays should convert seconds to days");
        assertTrue(requirement.isUseTotalPlaytime(),
                "isUseTotalPlaytime should reflect the configured flag");
        assertEquals("Desc", requirement.getDescription(),
                "getDescription should return the supplied description");

        final Map<String, Long> retrieved = requirement.getWorldPlaytimeRequirements();
        assertEquals(worldRequirements, retrieved,
                "getWorldPlaytimeRequirements should return the configured world thresholds");
        retrieved.put("beta", 30L);
        assertEquals(1, requirement.getWorldPlaytimeRequirements().size(),
                "getWorldPlaytimeRequirements should provide a defensive copy of the world map");
    }

    @Test
    void validateShouldDetectInvalidConfigurations() {
        final PlaytimeRequirement negativeTotal = new PlaytimeRequirement(10L);
        setField(negativeTotal, "requiredPlaytimeSeconds", -5L);
        assertThrows(IllegalStateException.class, negativeTotal::validate,
                "validate should reject negative total playtime thresholds");

        final PlaytimeRequirement missingWorlds = new PlaytimeRequirement(0L);
        setField(missingWorlds, "useTotalPlaytime", false);
        setField(missingWorlds, "worldPlaytimeRequirements", new HashMap<String, Long>());
        assertThrows(IllegalStateException.class, missingWorlds::validate,
                "validate should reject empty world requirements when total playtime is disabled");

        final PlaytimeRequirement blankWorld = new PlaytimeRequirement(0L, Map.of("", 10L), false, null);
        assertThrows(IllegalStateException.class, blankWorld::validate,
                "validate should reject blank world identifiers");

        final PlaytimeRequirement negativeWorld = new PlaytimeRequirement(0L, Map.of("alpha", -10L), false, null);
        assertThrows(IllegalStateException.class, negativeWorld::validate,
                "validate should reject negative world-specific thresholds");
    }

    @Test
    void fromTimeConfigShouldRejectAmbiguousTimeUnits() {
        assertThrows(IllegalArgumentException.class,
                () -> PlaytimeRequirement.fromTimeConfig(10L, 1L, null, null, null, true, null),
                "fromTimeConfig should reject configurations with multiple global units");
        assertThrows(IllegalArgumentException.class,
                () -> PlaytimeRequirement.fromTimeConfig(null, 1L, 1L, null, null, true, null),
                "fromTimeConfig should reject configurations with multiple global units");
        assertThrows(IllegalArgumentException.class,
                () -> PlaytimeRequirement.fromTimeConfig(null, null, null, null, Map.of(), true, null),
                "fromTimeConfig should require at least one requirement when maps are empty");
    }

    @Test
    void fromTimeConfigShouldConvertUnits() {
        final PlaytimeRequirement seconds = PlaytimeRequirement.fromTimeConfig(90L, null, null, null, null, true, "Seconds");
        assertEquals(90L, seconds.getRequiredPlaytimeSeconds(),
                "fromTimeConfig should use the seconds value when provided");
        assertEquals("Seconds", seconds.getDescription(),
                "fromTimeConfig should propagate the description");

        final PlaytimeRequirement minutes = PlaytimeRequirement.fromTimeConfig(null, 2L, null, null, null, null, null);
        assertEquals(TimeUnit.MINUTES.toSeconds(2L), minutes.getRequiredPlaytimeSeconds(),
                "fromTimeConfig should convert minutes to seconds");

        final PlaytimeRequirement hours = PlaytimeRequirement.fromTimeConfig(null, null, 3L, null, null, null, null);
        assertEquals(TimeUnit.HOURS.toSeconds(3L), hours.getRequiredPlaytimeSeconds(),
                "fromTimeConfig should convert hours to seconds");

        final PlaytimeRequirement days = PlaytimeRequirement.fromTimeConfig(null, null, null, 1L, null, null, null);
        assertEquals(TimeUnit.DAYS.toSeconds(1L), days.getRequiredPlaytimeSeconds(),
                "fromTimeConfig should convert days to seconds");

        final Map<String, Long> worldRequirements = Map.of("alpha", 30L);
        final PlaytimeRequirement worldsOnly = PlaytimeRequirement.fromTimeConfig(null, null, null, null, worldRequirements, false, null);
        assertEquals(worldRequirements, worldsOnly.getWorldPlaytimeRequirements(),
                "fromTimeConfig should retain provided world requirements");
        assertFalse(worldsOnly.isUseTotalPlaytime(),
                "fromTimeConfig should respect the useTotalPlaytime flag");
    }

    private void setPlaytimeSeconds(final PlayerMock player, final long seconds) {
        player.setStatistic(Statistic.PLAY_ONE_MINUTE, (int) (seconds * 20));
    }

    private static Set<String> toPartSet(final String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(formatted.split(",\\s*")).collect(Collectors.toCollection(HashSet::new));
    }

    private static void setField(final PlaytimeRequirement requirement, final String name, final Object value) {
        try {
            final Field field = PlaytimeRequirement.class.getDeclaredField(name);
            field.setAccessible(true);
            if (value instanceof Long longValue) {
                field.setLong(requirement, longValue);
            } else if (value instanceof Boolean booleanValue) {
                field.setBoolean(requirement, booleanValue);
            } else {
                field.set(requirement, value);
            }
        } catch (final ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
