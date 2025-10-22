package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class PlayerRankJoinListenerTest {

    @Test
    void onPlayerJoinRetrievesPlayerUuidWithoutTouchingManager() {
        final RDQ rdq = mock(RDQ.class);
        final PlayerRankJoinListener listener = new PlayerRankJoinListener(rdq);

        final PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        final Player player = mock(Player.class);
        final UUID uniqueId = UUID.randomUUID();

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(uniqueId);

        listener.onPlayerJoin(event);

        verify(event).getPlayer();
        verify(player).getUniqueId();
        verify(rdq, never()).getManager();
        verifyNoMoreInteractions(rdq);
    }
}
