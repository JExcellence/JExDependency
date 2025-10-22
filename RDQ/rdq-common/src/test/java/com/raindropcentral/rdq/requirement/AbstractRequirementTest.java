package com.raindropcentral.rdq.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class AbstractRequirementTest {

    private static final String DESCRIPTION_KEY = "rdq.requirement.test.description";

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void baseContractDelegatesToSubclassImplementations() {
        PlayerMock player = this.server.addPlayer("RequirementTester");
        TestRequirement requirement = new TestRequirement(true, 0.85d, DESCRIPTION_KEY);

        assertEquals(TestRequirement.TYPE, requirement.getType());
        assertTrue(requirement.isMet(player));
        assertEquals(0.85d, requirement.calculateProgress(player));

        requirement.consume(player);

        assertEquals(1, requirement.getConsumeCount());
        assertSame(player, requirement.getLastConsumedPlayer());
        assertEquals(DESCRIPTION_KEY, requirement.getDescriptionKey());
    }

    private static final class TestRequirement extends AbstractRequirement {

        private static final Type TYPE = Type.CUSTOM;

        private final boolean metResult;
        private final double progressResult;
        private final String descriptionKey;
        private int consumeCount;
        private Player lastConsumedPlayer;

        private TestRequirement(final boolean metResult,
                                final double progressResult,
                                final @NotNull String descriptionKey) {
            super(TYPE);
            this.metResult = metResult;
            this.progressResult = progressResult;
            this.descriptionKey = descriptionKey;
        }

        @Override
        public boolean isMet(final @NotNull Player player) {
            return this.metResult;
        }

        @Override
        public double calculateProgress(final @NotNull Player player) {
            return this.progressResult;
        }

        @Override
        public void consume(final @NotNull Player player) {
            this.consumeCount++;
            this.lastConsumedPlayer = player;
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return this.descriptionKey;
        }

        private int getConsumeCount() {
            return this.consumeCount;
        }

        private Player getLastConsumedPlayer() {
            return this.lastConsumedPlayer;
        }
    }
}
