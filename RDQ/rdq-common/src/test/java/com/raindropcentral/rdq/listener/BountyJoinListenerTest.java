package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.manager.bounty.BountyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BountyJoinListenerTest {

    private static final UUID PLAYER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Mock
    private RDQ rdq;

    @Mock
    private RDQManager manager;

    @Mock
    private BountyManager bountyManager;

    @Mock
    private PlayerJoinEvent event;

    @Mock
    private Player player;

    @InjectMocks
    private BountyJoinListener listener;

    @Test
    void onPlayerJoinUpdatesBountyDisplayForJoiningPlayer() {
        when(rdq.getManager()).thenReturn(manager);
        when(manager.getBountyManager()).thenReturn(bountyManager);
        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);

        listener.onPlayerJoin(event);

        verify(bountyManager).updateBountyPlayerDisplay(PLAYER_ID);
        verifyNoMoreInteractions(bountyManager);
    }
}
