package com.raindropcentral.rdq.database.entity.bounty;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RBountyTest {

    private RDQPlayer rdqPlayer;
    private Player commissioner;
    private UUID commissionerId;

    @BeforeEach
    void setUp() {
        this.rdqPlayer = new RDQPlayer(UUID.randomUUID(), "PlayerOne");
        this.commissionerId = UUID.randomUUID();
        this.commissioner = mock(Player.class);
        when(this.commissioner.getUniqueId()).thenReturn(this.commissionerId);
    }

    @Test
    void constructorRequiresNonNullPlayer() {
        assertThrows(NullPointerException.class, () -> new RBounty(null, this.commissioner));
    }

    @Test
    void constructorRequiresNonNullCommissioner() {
        assertThrows(NullPointerException.class, () -> new RBounty(this.rdqPlayer, null));
    }

    @Test
    void constructorRequiresNonNullRewardItems() {
        assertThrows(NullPointerException.class, () -> new RBounty(this.rdqPlayer, this.commissioner, null, new HashMap<>()));
    }

    @Test
    void constructorRequiresNonNullRewardCurrencies() {
        assertThrows(NullPointerException.class, () -> new RBounty(this.rdqPlayer, this.commissioner, new HashSet<>(), null));
    }

    @Test
    void constructorCopiesProvidedCollections() {
        Set<RewardItem> rewardItems = new HashSet<>();
        RewardItem rewardItem = mock(RewardItem.class);
        rewardItems.add(rewardItem);

        Map<String, Double> rewardCurrencies = new HashMap<>();
        rewardCurrencies.put("coins", 10.0);

        RBounty bounty = new RBounty(this.rdqPlayer, this.commissioner, rewardItems, rewardCurrencies);

        rewardItems.add(mock(RewardItem.class));
        rewardCurrencies.put("gems", 5.0);

        assertEquals(1, bounty.getRewardItems().size());
        assertTrue(bounty.getRewardItems().contains(rewardItem));
        assertEquals(1, bounty.getRewardCurrencies().size());
        assertEquals(10.0, bounty.getRewardCurrencies().get("coins"));
        assertFalse(bounty.getRewardCurrencies().containsKey("gems"));
    }

    @Test
    void addRewardItemMaintainsImmutability() {
        RBounty bounty = new RBounty(this.rdqPlayer, this.commissioner);
        RewardItem rewardItem = mock(RewardItem.class);

        bounty.addRewardItem(rewardItem);

        assertTrue(bounty.getRewardItems().contains(rewardItem));
        assertThrows(UnsupportedOperationException.class, () -> bounty.getRewardItems().add(mock(RewardItem.class)));
    }

    @Test
    void addRewardHistoryEntryMaintainsImmutability() {
        RBounty bounty = new RBounty(this.rdqPlayer, this.commissioner);

        bounty.addRewardHistoryEntry("First reward");

        assertTrue(bounty.getRewardHistory().contains("First reward"));
        assertThrows(UnsupportedOperationException.class, () -> bounty.getRewardHistory().add("Another reward"));
    }

    @Test
    void addRewardCurrencyMaintainsImmutability() {
        RBounty bounty = new RBounty(this.rdqPlayer, this.commissioner);

        bounty.addRewardCurrency("coins", 5.0);

        assertEquals(5.0, bounty.getRewardCurrencies().get("coins"));
        assertThrows(UnsupportedOperationException.class, () -> bounty.getRewardCurrencies().put("gems", 3.0));
    }

    @Test
    void commissionerSetterAcceptsUuid() {
        RBounty bounty = new RBounty(this.rdqPlayer, this.commissioner);
        UUID newCommissionerId = UUID.randomUUID();

        bounty.setCommissioner(newCommissionerId);

        assertEquals(newCommissionerId, bounty.getCommissioner());
    }
}
