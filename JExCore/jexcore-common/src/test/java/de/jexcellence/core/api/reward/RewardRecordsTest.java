package de.jexcellence.core.api.reward;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardRecordsTest {

    @Test
    void xpRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Reward.Xp(-1));
    }

    @Test
    void currencyRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Reward.Currency("coins", -0.01));
    }

    @Test
    void itemRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new Reward.Item("STONE", 0, ""));
    }

    @Test
    void itemPlainIsEquivalent() {
        assertEquals(Reward.Item.plain("STONE", 3), new Reward.Item("STONE", 3, ""));
    }

    @Test
    void compositeCopiesChildren() {
        final Reward.Xp xp = new Reward.Xp(10);
        final java.util.List<Reward> src = new java.util.ArrayList<>(List.of(xp));
        final Reward.Composite c = new Reward.Composite(src);
        src.clear();
        assertEquals(1, c.children().size());
    }

    @Test
    void customCopiesData() {
        final java.util.Map<String, Object> mutable = new java.util.HashMap<>(Map.of("count", 5));
        final Reward.Custom custom = new Reward.Custom("my-type", mutable);
        mutable.clear();
        assertEquals(5, custom.data().get("count"));
    }

    @Test
    void rewardResultStaticsMatch() {
        assertSame(RewardResult.Granted.class, RewardResult.granted("ok").getClass());
        assertSame(RewardResult.Denied.class, RewardResult.denied("no").getClass());
        assertSame(RewardResult.Failed.class, RewardResult.failed("err").getClass());
    }
}
