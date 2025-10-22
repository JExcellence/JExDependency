package com.raindropcentral.rdq.reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class AbstractRewardTest {

    private static final String DESCRIPTION_KEY = "rdq.reward.test.description";

    @Test
    void baseContractDelegatesToSubclassImplementations() {
        Player player = Mockito.mock(Player.class);
        AbstractReward.Type expectedType = AbstractReward.Type.SOUND;
        TestReward reward = new TestReward(expectedType);

        assertSame(expectedType, reward.getType());
        assertEquals(DESCRIPTION_KEY, reward.getDescriptionKey());
        assertFalse(reward.wasApplied());

        reward.apply(player);

        assertTrue(reward.wasApplied());
        assertSame(expectedType, reward.getType());

        AbstractReward.Type alias = reward.getType();
        assertSame(expectedType, alias);
        alias = AbstractReward.Type.TITLE;
        assertSame(expectedType, reward.getType());
    }

    private static final class TestReward extends AbstractReward {

        private final String descriptionKey;
        private boolean applied;

        private TestReward(final @NotNull Type type) {
            super(type);
            this.descriptionKey = DESCRIPTION_KEY;
        }

        @Override
        public void apply(final @NotNull Player player) {
            this.applied = !this.applied;
        }

        @Override
        public @NotNull String getDescriptionKey() {
            return this.descriptionKey;
        }

        private boolean wasApplied() {
            return this.applied;
        }
    }
}
