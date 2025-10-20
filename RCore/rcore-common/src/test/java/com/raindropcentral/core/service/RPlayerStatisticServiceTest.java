package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RBooleanStatistic;
import com.raindropcentral.core.database.entity.statistic.RDateStatistic;
import com.raindropcentral.core.database.entity.statistic.RNumberStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.database.entity.statistic.RStringStatistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RPlayerStatisticServiceTest {

    private static final String PLUGIN_A = "plugin-a";
    private static final String PLUGIN_B = "plugin-b";

    private RPlayer player;

    @BeforeEach
    void setUp() {
        this.player = new RPlayer(UUID.randomUUID(), "TestUser01");
    }

    @Test
    void createPlayerStatisticInitialisesEmptyAggregate() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);

        assertAll(
                () -> assertSame(player, statistic.getPlayer()),
                () -> assertTrue(statistic.getStatistics().isEmpty())
        );
    }

    @Test
    void createPlayerStatisticWithDataSeedsValues() {
        final Map<String, Object> initial = Map.of(
                "kills", 5,
                "title", "Hunter"
        );

        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatisticWithData(
                player,
                initial,
                PLUGIN_A
        );

        assertEquals(2, statistic.getStatistics().size());
        assertTrue(statistic.hasStatistic("kills", PLUGIN_A));
        assertTrue(statistic.hasStatistic("title", PLUGIN_A));
        assertInstanceOf(RNumberStatistic.class, findStatistic(statistic, "kills"));
        assertInstanceOf(RStringStatistic.class, findStatistic(statistic, "title"));
    }

    @Test
    void addOrUpdateStatisticUpdatesInPlaceForCompatibleType() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "level", PLUGIN_A, 10.0);

        final RAbstractStatistic original = findStatistic(statistic, "level");
        assertNotNull(original);
        assertInstanceOf(RNumberStatistic.class, original);

        RPlayerStatisticService.addOrUpdateStatistic(statistic, "level", PLUGIN_A, 42);

        final RAbstractStatistic afterUpdate = findStatistic(statistic, "level");
        assertSame(original, afterUpdate);
        assertEquals(42.0, ((RNumberStatistic) afterUpdate).getValue());
    }

    @Test
    void addOrUpdateStatisticReplacesOnTypeMismatch() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "level", PLUGIN_A, 10.0);

        final RAbstractStatistic original = findStatistic(statistic, "level");

        RPlayerStatisticService.addOrUpdateStatistic(statistic, "level", PLUGIN_A, "ten");

        final RAbstractStatistic replacement = findStatistic(statistic, "level");
        assertNotSame(original, replacement);
        assertInstanceOf(RStringStatistic.class, replacement);
        assertEquals("ten", replacement.getValue());
    }

    @Test
    void addStatisticsBulkAppliesAllEntries() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        final Map<String, Object> values = new HashMap<>();
        values.put("kills", 1);
        values.put("deaths", 2);
        values.put("title", "Rookie");

        final int applied = RPlayerStatisticService.addStatisticsBulk(statistic, values, PLUGIN_A);

        assertEquals(3, applied);
        assertEquals(3, statistic.getTotalStatisticCount());
        assertTrue(statistic.hasStatistic("kills", PLUGIN_A));
        assertTrue(statistic.hasStatistic("deaths", PLUGIN_A));
        assertTrue(statistic.hasStatistic("title", PLUGIN_A));
    }

    @Test
    void removeStatisticSupportsPluginFallback() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "kills", PLUGIN_A, 1);

        assertTrue(RPlayerStatisticService.removeStatistic(statistic, "kills", PLUGIN_A));
        assertFalse(statistic.hasStatistic("kills", PLUGIN_A));

        RPlayerStatisticService.addOrUpdateStatistic(statistic, "kills", PLUGIN_A, 5);
        assertTrue(RPlayerStatisticService.removeStatistic(statistic, "kills", PLUGIN_B));
        assertFalse(statistic.hasStatisticByIdentifier("kills"));
    }

    @Test
    void removeStatisticsByPluginClearsOnlyMatchingEntries() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "kills", PLUGIN_A, 1);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "wins", PLUGIN_A, 2);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "losses", PLUGIN_B, 3);

        final int removed = RPlayerStatisticService.removeStatisticsByPlugin(statistic, PLUGIN_A);

        assertEquals(2, removed);
        assertFalse(statistic.hasStatisticByIdentifier("kills"));
        assertFalse(statistic.hasStatisticByIdentifier("wins"));
        assertTrue(statistic.hasStatistic("losses", PLUGIN_B));
    }

    @Test
    void valueAccessorsRespectTypeExpectations() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "title", PLUGIN_A, "Champion");

        final Optional<String> title = RPlayerStatisticService.getStatisticValue(statistic, "title", PLUGIN_A, String.class);
        final Optional<Integer> wrongType = RPlayerStatisticService.getStatisticValue(statistic, "title", PLUGIN_A, Integer.class);

        assertEquals(Optional.of("Champion"), title);
        assertTrue(wrongType.isEmpty());
        assertEquals("Champion", RPlayerStatisticService.getStatisticAsString(statistic, "title", PLUGIN_A));
        assertNull(RPlayerStatisticService.getStatisticAsString(statistic, "missing", PLUGIN_A));
    }

    @Test
    void incrementNumericStatisticUpdatesStoredValue() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "level", PLUGIN_A, 10);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "title", PLUGIN_A, "Starter");

        final Double updated = RPlayerStatisticService.incrementNumericStatistic(statistic, "level", PLUGIN_A, 2.5);
        final Double failed = RPlayerStatisticService.incrementNumericStatistic(statistic, "title", PLUGIN_A, 1);

        assertEquals(12.5, updated);
        assertNull(failed);
        assertEquals(12.5, ((RNumberStatistic) findStatistic(statistic, "level")).getValue());
    }

    @Test
    void setCurrentTimestampStoresUtcMilliseconds() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        final long epochMillis = RPlayerStatisticService.setCurrentTimestamp(statistic, "last-login", PLUGIN_A);

        final RAbstractStatistic stored = findStatistic(statistic, "last-login");
        assertInstanceOf(RDateStatistic.class, stored);
        final long storedValue = ((RDateStatistic) stored).getValue();

        assertTrue(Math.abs(epochMillis - storedValue) < 1000, "timestamp should be within one second of returned value");
    }

    @Test
    void exportAndImportStatisticsRoundTripValues() {
        final RPlayerStatistic source = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(source, "kills", PLUGIN_A, 4);
        RPlayerStatisticService.addOrUpdateStatistic(source, "title", PLUGIN_A, "Warrior");
        RPlayerStatisticService.addOrUpdateStatistic(source, "wins", PLUGIN_B, true);

        final Map<String, Map<String, Object>> export = RPlayerStatisticService.exportStatistics(source);
        assertEquals(Set.of(PLUGIN_A, PLUGIN_B), export.keySet());
        assertEquals(2, export.get(PLUGIN_A).size());
        assertEquals(1, export.get(PLUGIN_B).size());

        final RPlayerStatistic target = RPlayerStatisticService.createPlayerStatistic(new RPlayer(UUID.randomUUID(), "Target01"));
        final int imported = RPlayerStatisticService.importStatistics(target, export);

        assertEquals(3, imported);
        assertEquals(source.getTotalStatisticCount(), target.getTotalStatisticCount());
        assertEquals(RPlayerStatisticService.getStatisticCountByPlugin(source), RPlayerStatisticService.getStatisticCountByPlugin(target));
        assertTrue(RPlayerStatisticService.hasStatistic(target, "wins", PLUGIN_B));
    }

    @Test
    void getStatisticCountByPluginSummarisesEntries() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "kills", PLUGIN_A, 1);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "deaths", PLUGIN_A, 2);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "wins", PLUGIN_B, 3);

        final Map<String, Long> counts = RPlayerStatisticService.getStatisticCountByPlugin(statistic);

        assertEquals(2, counts.get(PLUGIN_A));
        assertEquals(1, counts.get(PLUGIN_B));
    }

    @Test
    void hasStatisticReportsPresence() {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "kills", PLUGIN_A, 1);

        assertTrue(RPlayerStatisticService.hasStatistic(statistic, "kills", PLUGIN_A));
        assertFalse(RPlayerStatisticService.hasStatistic(statistic, "kills", PLUGIN_B));
    }

    @Test
    void createStatisticFromValueCoversAllSupportedTypes() throws Exception {
        final Method method = RPlayerStatisticService.class.getDeclaredMethod(
                "createStatisticFromValue",
                String.class,
                String.class,
                Object.class
        );
        method.setAccessible(true);

        final RAbstractStatistic booleanStat = (RAbstractStatistic) method.invoke(null, "bool", PLUGIN_A, true);
        final RAbstractStatistic numberStat = (RAbstractStatistic) method.invoke(null, "num", PLUGIN_A, 5);
        final RAbstractStatistic stringStat = (RAbstractStatistic) method.invoke(null, "str", PLUGIN_A, "value");
        final LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC);
        final RAbstractStatistic dateStat = (RAbstractStatistic) method.invoke(null, "date", PLUGIN_A, now);
        final Object fallback = new Object();
        final RAbstractStatistic fallbackStat = (RAbstractStatistic) method.invoke(null, "obj", PLUGIN_A, fallback);

        assertAll(
                () -> assertInstanceOf(RBooleanStatistic.class, booleanStat),
                () -> assertInstanceOf(RNumberStatistic.class, numberStat),
                () -> assertEquals(5.0, ((RNumberStatistic) numberStat).getValue()),
                () -> assertInstanceOf(RStringStatistic.class, stringStat),
                () -> assertInstanceOf(RDateStatistic.class, dateStat),
                () -> assertEquals(now.toEpochSecond(ZoneOffset.UTC) * 1000, ((RDateStatistic) dateStat).getValue()),
                () -> assertInstanceOf(RStringStatistic.class, fallbackStat),
                () -> assertEquals(fallback.toString(), fallbackStat.getValue())
        );
    }

    @Test
    void findByIdentifierResolvesManagedStatistic() throws Exception {
        final RPlayerStatistic statistic = RPlayerStatisticService.createPlayerStatistic(player);
        RPlayerStatisticService.addOrUpdateStatistic(statistic, "kills", PLUGIN_A, 5);

        final Method method = RPlayerStatisticService.class.getDeclaredMethod(
                "findByIdentifier",
                RPlayerStatistic.class,
                String.class
        );
        method.setAccessible(true);

        final Optional<?> located = (Optional<?>) method.invoke(null, statistic, "kills");
        final Optional<?> missing = (Optional<?>) method.invoke(null, statistic, "missing");

        assertTrue(located.isPresent());
        assertTrue(missing.isEmpty());
    }

    private RAbstractStatistic findStatistic(final RPlayerStatistic statistic, final String identifier) {
        return statistic.getStatistics().stream()
                .filter(entry -> entry.getIdentifier().equals(identifier))
                .findFirst()
                .orElse(null);
    }
}
