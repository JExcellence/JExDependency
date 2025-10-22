package com.raindropcentral.rdq.database.entity.player;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class RDQPlayerTest {

    @Test
    void constructorWithUuidAndNameInitializesFields() {
        final UUID uniqueId = UUID.randomUUID();
        final RDQPlayer player = new RDQPlayer(uniqueId, "TestPlayer");

        assertEquals(uniqueId, player.getUniqueId());
        assertEquals("TestPlayer", player.getPlayerName());
        assertNull(player.getBounty());
        assertTrue(player.getPlayerRanks().isEmpty());
        assertTrue(player.getPlayerRankPaths().isEmpty());
        assertTrue(player.getPlayerPerks().isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> player.getPlayerRanks().add(mock(RPlayerRank.class)));
        assertThrows(UnsupportedOperationException.class, () -> player.getPlayerRankPaths().add(mock(RPlayerRankPath.class)));
        assertThrows(UnsupportedOperationException.class, () -> player.getPlayerPerks().add(null));
    }

    @Test
    void constructorRequiresNonNullUniqueId() {
        final NullPointerException exception = assertThrows(NullPointerException.class, () -> new RDQPlayer(null, "Name"));
        assertEquals("uniqueId cannot be null", exception.getMessage());
    }

    @Test
    void constructorRequiresNonNullPlayerName() {
        final NullPointerException exception = assertThrows(NullPointerException.class, () -> new RDQPlayer(UUID.randomUUID(), null));
        assertEquals("playerName cannot be null", exception.getMessage());
    }

    @Test
    void playerConstructorCopiesBukkitPlayerDetails() {
        final Player bukkitPlayer = mock(Player.class);
        final UUID uniqueId = UUID.randomUUID();
        when(bukkitPlayer.getUniqueId()).thenReturn(uniqueId);
        when(bukkitPlayer.getName()).thenReturn("BukkitName");

        final RDQPlayer player = new RDQPlayer(bukkitPlayer);

        assertEquals(uniqueId, player.getUniqueId());
        assertEquals("BukkitName", player.getPlayerName());
    }

    @Test
    void setBountySynchronizesBackReference() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "Hunter");
        final RBounty bounty = mock(RBounty.class);

        player.setBounty(bounty);

        assertSame(bounty, player.getBounty());
        verify(bounty).setPlayer(eq(player));
    }

    @Test
    void setBountyAllowsClearingAssociation() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "Hunter");
        final RBounty bounty = mock(RBounty.class);
        player.setBounty(bounty);

        player.setBounty(null);

        assertNull(player.getBounty());
        verify(bounty).setPlayer(eq(player));
        verifyNoMoreInteractions(bounty);
    }

    @Test
    void setPlayerRanksReplacesContentsWithDefensiveCopy() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        final RPlayerRank first = mock(RPlayerRank.class);
        final RPlayerRank second = mock(RPlayerRank.class);
        final List<RPlayerRank> newRanks = new ArrayList<>();
        newRanks.add(first);
        newRanks.add(second);

        player.setPlayerRanks(newRanks);

        assertEquals(2, player.getPlayerRanks().size());
        assertTrue(player.getPlayerRanks().contains(first));
        assertTrue(player.getPlayerRanks().contains(second));
        assertThrows(UnsupportedOperationException.class, () -> player.getPlayerRanks().add(mock(RPlayerRank.class)));

        newRanks.add(mock(RPlayerRank.class));
        assertEquals(2, player.getPlayerRanks().size());
    }

    @Test
    void setPlayerRanksClearsWhenNullProvided() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        player.addPlayerRank(mock(RPlayerRank.class));
        assertFalse(player.getPlayerRanks().isEmpty());

        player.setPlayerRanks(null);

        assertTrue(player.getPlayerRanks().isEmpty());
    }

    @Test
    void addPlayerRankMaintainsBidirectionalLinkAndPreventsDuplicates() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        final RPlayerRank rank = mock(RPlayerRank.class);

        player.addPlayerRank(rank);
        verify(rank).setRdqPlayer(eq(player));

        player.addPlayerRank(rank);
        verify(rank, times(1)).setRdqPlayer(eq(player));
        assertEquals(1, player.getPlayerRanks().size());
    }

    @Test
    void removePlayerRankClearsBidirectionalLinkWhenPresent() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        final RPlayerRank rank = mock(RPlayerRank.class);
        player.addPlayerRank(rank);
        clearInvocations(rank);

        player.removePlayerRank(rank);

        assertTrue(player.getPlayerRanks().isEmpty());
        verify(rank).setRdqPlayer(isNull());
    }

    @Test
    void removePlayerRankDoesNothingWhenRankMissing() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        final RPlayerRank rank = mock(RPlayerRank.class);

        player.removePlayerRank(rank);

        assertTrue(player.getPlayerRanks().isEmpty());
        verify(rank, never()).setRdqPlayer(any());
    }

    @Test
    void setPlayerRankPathsReplacesContentsWithDefensiveCopy() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "PathPlayer");
        final RPlayerRankPath first = mock(RPlayerRankPath.class);
        final RPlayerRankPath second = mock(RPlayerRankPath.class);
        final List<RPlayerRankPath> newPaths = new ArrayList<>();
        newPaths.add(first);
        newPaths.add(second);

        player.setPlayerRankPaths(newPaths);

        assertEquals(2, player.getPlayerRankPaths().size());
        assertTrue(player.getPlayerRankPaths().contains(first));
        assertTrue(player.getPlayerRankPaths().contains(second));
        assertThrows(UnsupportedOperationException.class, () -> player.getPlayerRankPaths().add(mock(RPlayerRankPath.class)));

        newPaths.add(mock(RPlayerRankPath.class));
        assertEquals(2, player.getPlayerRankPaths().size());
    }

    @Test
    void getPlayerPerksReturnsUnmodifiableSet() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "PerkPlayer");
        assertTrue(player.getPlayerPerks().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> player.getPlayerPerks().add(null));
    }

    @Test
    void getPlayerRankForTreeReturnsMatchingRank() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        final RPlayerRank matching = mock(RPlayerRank.class);
        final RPlayerRank other = mock(RPlayerRank.class);
        player.addPlayerRank(matching);
        player.addPlayerRank(other);
        when(matching.belongsToRankTree("tree-1")).thenReturn(true);
        when(other.belongsToRankTree("tree-1")).thenReturn(false);

        final Optional<RPlayerRank> result = player.getPlayerRankForTree("tree-1");

        assertTrue(result.isPresent());
        assertSame(matching, result.orElseThrow());
    }

    @Test
    void getActivePlayerRankReturnsFirstActiveRank() {
        final RDQPlayer player = new RDQPlayer(UUID.randomUUID(), "RankedPlayer");
        final RPlayerRank inactive = mock(RPlayerRank.class);
        final RPlayerRank active = mock(RPlayerRank.class);
        player.addPlayerRank(inactive);
        player.addPlayerRank(active);
        when(inactive.isActive()).thenReturn(false);
        when(active.isActive()).thenReturn(true);

        final Optional<RPlayerRank> result = player.getActivePlayerRank();

        assertTrue(result.isPresent());
        assertSame(active, result.orElseThrow());
    }

    @Test
    void equalsAndHashCodeDependOnUniqueId() {
        final UUID uniqueId = UUID.randomUUID();
        final RDQPlayer first = new RDQPlayer(uniqueId, "First");
        final RDQPlayer second = new RDQPlayer(uniqueId, "Second");
        final RDQPlayer different = new RDQPlayer(UUID.randomUUID(), "Other");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
        assertNotEquals(first.hashCode(), different.hashCode());
        assertNotEquals(first, "not-a-player");
    }
}
