package com.raindropcentral.core.database.entity.player;

import com.raindropcentral.core.database.entity.statistic.RAbstractStatistic;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RPlayerTest {

    @Test
    void shouldConstructFromUuidAndName() {
        final UUID uniqueId = UUID.randomUUID();

        final RPlayer player = new RPlayer(uniqueId, "  Alpha  ");

        assertEquals(uniqueId, player.getUniqueId());
        assertEquals("Alpha", player.getPlayerName());
        assertFalse(player.hasStatistics());
    }

    @Test
    void shouldConstructFromBukkitPlayerSnapshot() {
        final UUID uniqueId = UUID.randomUUID();
        final Player bukkitPlayer = Mockito.mock(Player.class);
        Mockito.when(bukkitPlayer.getUniqueId()).thenReturn(uniqueId);
        Mockito.when(bukkitPlayer.getName()).thenReturn("Bravo");

        final RPlayer player = new RPlayer(bukkitPlayer);

        assertEquals(uniqueId, player.getUniqueId());
        assertEquals("Bravo", player.getPlayerName());
    }

    @Test
    void shouldRejectNamesOutsideAllowedBounds() {
        final UUID uniqueId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new RPlayer(uniqueId, "ab"));
        assertThrows(IllegalArgumentException.class, () -> new RPlayer(uniqueId, "abcdefghijklmnopq"));

        final RPlayer player = new RPlayer(uniqueId, "Delta");
        assertThrows(IllegalArgumentException.class, () -> player.updatePlayerName("ab"));
        assertEquals("Delta", player.getPlayerName());
    }

    @Test
    void shouldSynchronizeStatisticsAssociation() {
        final RPlayer player = new RPlayer(UUID.randomUUID(), "Echo");
        final RPlayerStatistic statisticAggregate = new RPlayerStatistic(player);

        assertFalse(player.hasStatistics());
        assertSame(player, statisticAggregate.getPlayer());

        player.setPlayerStatistic(statisticAggregate);

        assertSame(statisticAggregate, player.getPlayerStatistic());
        assertSame(player, statisticAggregate.getPlayer());
        assertFalse(player.hasStatistics());

        statisticAggregate.addOrReplaceStatistic(new TestStatistic("kills", "rcore", 5));

        assertTrue(player.hasStatistics());
    }

    @Test
    void shouldUpdateNameAndReflectStateInToString() {
        final UUID uniqueId = UUID.randomUUID();
        final RPlayer player = new RPlayer(uniqueId, "Foxtrot");

        player.updatePlayerName("  GolfPlayer  ");
        assertEquals("GolfPlayer", player.getPlayerName());

        final long identifier = player.getId();
        final String expectedWithoutStats = "RPlayer[id=%d, uuid=%s, name=%s, hasStats=%b]"
            .formatted(identifier, uniqueId, "GolfPlayer", false);
        assertFalse(player.hasStatistics());
        assertEquals(expectedWithoutStats, player.toString());

        final RPlayerStatistic statisticAggregate = new RPlayerStatistic(player);
        player.setPlayerStatistic(statisticAggregate);
        statisticAggregate.addOrReplaceStatistic(new TestStatistic("wins", "rcore", 12));

        final String expectedWithStats = "RPlayer[id=%d, uuid=%s, name=%s, hasStats=%b]"
            .formatted(identifier, uniqueId, "GolfPlayer", true);
        assertTrue(player.hasStatistics());
        assertEquals(expectedWithStats, player.toString());
    }

    private static final class TestStatistic extends RAbstractStatistic {

        private final Object value;

        private TestStatistic(final String identifier, final String plugin, final Object value) {
            super(identifier, plugin);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }
}
