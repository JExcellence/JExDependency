package com.raindropcentral.rdq.reward;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeRewardTest {

    @Test
    void applyInvokesEachRewardInOrderWhenContinuingOnError() {
        final AbstractReward first = Mockito.mock(AbstractReward.class);
        final AbstractReward second = Mockito.mock(AbstractReward.class);
        final AbstractReward third = Mockito.mock(AbstractReward.class);
        final Player player = Mockito.mock(Player.class);

        final CompositeReward reward = new CompositeReward(List.of(first, second, third), true);

        reward.apply(player);

        final InOrder order = Mockito.inOrder(first, second, third);
        order.verify(first).apply(player);
        order.verify(second).apply(player);
        order.verify(third).apply(player);
        order.verifyNoMoreInteractions();
    }

    @Test
    void applyContinuesWhenSubRewardFailsAndContinueOnErrorIsTrue() {
        final AbstractReward first = Mockito.mock(AbstractReward.class);
        final AbstractReward second = Mockito.mock(AbstractReward.class);
        final AbstractReward third = Mockito.mock(AbstractReward.class);
        final Player player = Mockito.mock(Player.class);

        Mockito.doThrow(new IllegalStateException("boom")).when(second).apply(player);

        final CompositeReward reward = new CompositeReward(List.of(first, second, third), true);

        reward.apply(player);

        Mockito.verify(first).apply(player);
        Mockito.verify(second).apply(player);
        Mockito.verify(third).apply(player);
    }

    @Test
    void applyThrowsWhenContinueOnErrorIsFalse() {
        final AbstractReward first = Mockito.mock(AbstractReward.class);
        final AbstractReward second = Mockito.mock(AbstractReward.class);
        final AbstractReward third = Mockito.mock(AbstractReward.class);
        final Player player = Mockito.mock(Player.class);

        final IllegalStateException failure = new IllegalStateException("boom");
        Mockito.doThrow(failure).when(second).apply(player);

        final CompositeReward reward = new CompositeReward(List.of(first, second, third), false);

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> reward.apply(player));

        assertEquals("Failed to apply composite reward", exception.getMessage());
        assertEquals(failure, exception.getCause());
        Mockito.verify(first).apply(player);
        Mockito.verify(second).apply(player);
        Mockito.verifyNoInteractions(third);
    }

    @Test
    void validateThrowsWhenRewardsEmpty() {
        final List<AbstractReward> empty = Mockito.spy(new ArrayList<AbstractReward>());
        Mockito.doReturn(false).when(empty).isEmpty();

        final CompositeReward reward = new CompositeReward(empty, true);

        final IllegalStateException exception = assertThrows(IllegalStateException.class, reward::validate);
        assertEquals("CompositeReward must have at least one sub-reward", exception.getMessage());
    }

    @Test
    void validateThrowsWhenRewardContainsNull() {
        final List<AbstractReward> rewards = new ArrayList<>();
        rewards.add(Mockito.mock(AbstractReward.class));
        rewards.add(null);

        final CompositeReward reward = new CompositeReward(rewards, true);

        final IllegalStateException exception = assertThrows(IllegalStateException.class, reward::validate);
        assertTrue(exception.getMessage().contains("Sub-reward at index 1 is null"));
    }

    @Test
    void getRewardsReturnsDefensiveCopy() {
        final AbstractReward first = Mockito.mock(AbstractReward.class);
        final AbstractReward second = Mockito.mock(AbstractReward.class);

        final CompositeReward reward = new CompositeReward(List.of(first, second), true);

        final List<AbstractReward> copy = reward.getRewards();
        copy.clear();

        assertEquals(2, reward.getRewardCount());
        assertEquals(0, copy.size());
    }
}
