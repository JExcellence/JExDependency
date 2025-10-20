package com.raindropcentral.core.database.entity.statistic;

import com.raindropcentral.core.database.entity.player.RPlayer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RAbstractStatisticTest {

    private static final String IDENTIFIER = "blocks_mined";
    private static final String PLUGIN = "rcore";

    @Test
    void constructorRejectsNullIdentifier() {
        final Executable factory = () -> new TestStatistic(null, PLUGIN, 5);

        final NullPointerException exception = assertThrows(NullPointerException.class, factory);
        assertEquals("identifier cannot be null", exception.getMessage());
    }

    @Test
    void constructorRejectsNullPlugin() {
        final Executable factory = () -> new TestStatistic(IDENTIFIER, null, 5);

        final NullPointerException exception = assertThrows(NullPointerException.class, factory);
        assertEquals("plugin cannot be null", exception.getMessage());
    }

    @Test
    void gettersExposeConstructorMetadata() {
        final TestStatistic statistic = new TestStatistic(IDENTIFIER, PLUGIN, 9);

        assertEquals(IDENTIFIER, statistic.getIdentifier());
        assertEquals(PLUGIN, statistic.getPlugin());
        assertEquals(IDENTIFIER, statistic.identifierFromSubclass());
        assertEquals(9, statistic.getValue());
    }

    @Test
    void matchesUsesIdentifierAndPlugin() {
        final TestStatistic statistic = new TestStatistic(IDENTIFIER, PLUGIN, 3);

        assertTrue(statistic.matches(IDENTIFIER, PLUGIN));
    }

    @Nested
    class PlayerStatisticAssociation {

        @Test
        void setPlayerStatisticAllowsNullAndPreservesReference() {
            final TestStatistic statistic = new TestStatistic(IDENTIFIER, PLUGIN, 2);
            assertNull(statistic.getPlayerStatistic());

            final RPlayerStatistic playerStatistic = new RPlayerStatistic(new RPlayer(UUID.randomUUID(), "Tester"));
            statistic.setPlayerStatistic(playerStatistic);
            assertSame(playerStatistic, statistic.getPlayerStatistic());

            statistic.setPlayerStatistic(null);
            assertNull(statistic.getPlayerStatistic());
        }
    }

    private static final class TestStatistic extends RAbstractStatistic {

        private final Object value;

        private TestStatistic(final String identifier, final String plugin, final Object value) {
            super(identifier, plugin);
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        private String identifierFromSubclass() {
            return this.identifier;
        }
    }
}
