package com.raindropcentral.core.database.entity.statistic;

import com.raindropcentral.core.database.entity.player.RPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RPlayerStatisticTest {

    @Test
    @DisplayName("constructs aggregates and exposes safe statistic views")
    void shouldConstructAggregateAndExposeStatisticViews() {
        RPlayer player = new RPlayer(UUID.randomUUID(), "PlayerOne");
        RPlayerStatistic aggregate = new RPlayerStatistic(player);
        player.setPlayerStatistic(aggregate);

        assertSame(player, aggregate.getPlayer());
        assertSame(aggregate, player.getPlayerStatistic());

        Set<RAbstractStatistic> initialCopy = aggregate.getStatistics();
        assertTrue(initialCopy.isEmpty(), "Aggregate should start with no statistics");

        TestStatistic kills = new TestStatistic("kills", "plugin-alpha", 5);
        TestStatistic assists = new TestStatistic("assists", "plugin-beta", 2);

        aggregate.addOrReplaceStatistic(kills);
        aggregate.addOrReplaceStatistic(assists);

        assertEquals(2, aggregate.getTotalStatisticCount());
        assertTrue(aggregate.getStatisticsInternal().contains(kills));
        assertTrue(aggregate.getStatisticsInternal().contains(assists));

        Optional<Object> killsValue = aggregate.getStatisticValue("kills", "plugin-alpha");
        assertTrue(killsValue.isPresent(), "Statistic lookup should return an Optional value");
        assertEquals(5, killsValue.orElseThrow());
        assertTrue(aggregate.getStatisticValue("missing", "plugin-alpha").isEmpty());

        assertEquals(2, aggregate.getStatistics().size(), "Exposed set should reflect current statistics");
        assertTrue(initialCopy.isEmpty(), "Original copy must not mutate after aggregate changes");
        assertThrows(UnsupportedOperationException.class, () -> aggregate.getStatistics().add(new TestStatistic("damage", "plugin-gamma", 1)));
    }

    @Test
    @DisplayName("mutating helpers maintain bidirectional links and uniqueness")
    void shouldMaintainBidirectionalLinksAndUniqueness() {
        RPlayerStatistic aggregate = new RPlayerStatistic(new RPlayer(UUID.randomUUID(), "PlayerTwo"));

        TestStatistic killsAlpha = new TestStatistic("kills", "plugin-alpha", 10);
        aggregate.addOrReplaceStatistic(killsAlpha);

        assertSame(aggregate, killsAlpha.getPlayerStatistic());
        assertTrue(aggregate.hasStatistic("kills", "plugin-alpha"));
        assertTrue(aggregate.hasStatisticByIdentifier("kills"));
        assertEquals(1, aggregate.getStatisticCountForPlugin("plugin-alpha"));
        assertEquals(1, aggregate.getTotalStatisticCount());

        TestStatistic killsBeta = new TestStatistic("kills", "plugin-beta", 12);
        aggregate.addOrReplaceStatistic(killsBeta);

        assertNull(killsAlpha.getPlayerStatistic(), "Replaced statistic should be detached");
        assertSame(aggregate, killsBeta.getPlayerStatistic());
        assertFalse(aggregate.hasStatistic("kills", "plugin-alpha"));
        assertTrue(aggregate.hasStatistic("kills", "plugin-beta"));
        assertEquals(0, aggregate.getStatisticCountForPlugin("plugin-alpha"));
        assertEquals(1, aggregate.getStatisticCountForPlugin("plugin-beta"));
        assertEquals(1, aggregate.getTotalStatisticCount(), "Identifier uniqueness must be preserved");

        assertTrue(aggregate.removeStatistic("kills", "plugin-beta"));
        assertEquals(0, aggregate.getTotalStatisticCount());
        assertFalse(aggregate.hasStatisticByIdentifier("kills"));

        aggregate.addOrReplaceStatistic(killsAlpha);
        aggregate.addOrReplaceStatistic(new TestStatistic("assists", "plugin-alpha", 3));
        assertEquals(2, aggregate.getTotalStatisticCount());
        assertTrue(aggregate.removeStatisticByIdentifier("kills"));
        assertFalse(aggregate.hasStatisticByIdentifier("kills"));
        assertEquals(1, aggregate.getTotalStatisticCount());
    }

    @Test
    @DisplayName("guards enforce non-null arguments across lookup helpers")
    void shouldGuardAgainstNullArguments() {
        RPlayerStatistic aggregate = new RPlayerStatistic(new RPlayer(UUID.randomUUID(), "PlayerThree"));
        TestStatistic statistic = new TestStatistic("blocks", "plugin-alpha", 7);

        assertThrows(NullPointerException.class, () -> new RPlayerStatistic(null));
        assertThrows(NullPointerException.class, () -> aggregate.addOrReplaceStatistic(null));
        assertThrows(NullPointerException.class, () -> aggregate.getStatisticValue(null, "plugin-alpha"));
        assertThrows(NullPointerException.class, () -> aggregate.getStatisticValue("blocks", null));
        assertThrows(NullPointerException.class, () -> aggregate.hasStatistic(null, "plugin-alpha"));
        assertThrows(NullPointerException.class, () -> aggregate.hasStatistic("blocks", null));
        assertThrows(NullPointerException.class, () -> aggregate.hasStatisticByIdentifier(null));
        assertThrows(NullPointerException.class, () -> aggregate.removeStatistic(null, "plugin-alpha"));
        assertThrows(NullPointerException.class, () -> aggregate.removeStatistic("blocks", null));
        assertThrows(NullPointerException.class, () -> aggregate.removeStatisticByIdentifier(null));
        assertThrows(NullPointerException.class, () -> aggregate.getStatisticCountForPlugin(null));

        aggregate.addOrReplaceStatistic(statistic);
        assertEquals(7, aggregate.getStatisticValue("blocks", "plugin-alpha").orElseThrow());
    }

    private static final class TestStatistic extends RAbstractStatistic {

        private final Object value;

        private TestStatistic(String identifier, String plugin, Object value) {
            super(identifier, plugin);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }
    }
}
