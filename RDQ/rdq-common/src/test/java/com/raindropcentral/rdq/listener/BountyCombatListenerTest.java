package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.manager.bounty.BountyManager;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BountyCombatListenerTest {

    @Mock
    private RDQ rdq;

    @Mock
    private RDQManager manager;

    @Mock
    private BountyManager bountyManager;

    @Mock
    private EntityDamageByEntityEvent damageEvent;

    @Mock
    private PlayerDeathEvent deathEvent;

    private BountyCombatListener listener;

    @BeforeEach
    void setUp() {
        when(this.rdq.getManager()).thenReturn(this.manager);
        when(this.manager.getBountyManager()).thenReturn(this.bountyManager);
        this.listener = new BountyCombatListener(this.rdq);
    }

    @Test
    void onEntityDamageByEntityTracksDamageWhenBothEntitiesArePlayers() {
        Player target = mock(Player.class);
        Player damager = mock(Player.class);
        UUID targetId = UUID.randomUUID();
        UUID damagerId = UUID.randomUUID();
        double finalDamage = 12.5D;

        when(this.damageEvent.getEntity()).thenReturn(target);
        when(this.damageEvent.getDamager()).thenReturn(damager);
        when(this.damageEvent.getFinalDamage()).thenReturn(finalDamage);
        when(target.getUniqueId()).thenReturn(targetId);
        when(damager.getUniqueId()).thenReturn(damagerId);

        this.listener.onEntityDamageByEntity(this.damageEvent);

        verify(this.bountyManager).trackDamage(targetId, damagerId, finalDamage);
        verifyNoMoreInteractions(this.bountyManager);
    }

    @Test
    void onEntityDamageByEntityDoesNotTrackWhenTargetIsNotPlayer() {
        Entity nonPlayer = mock(Entity.class);
        Player damager = mock(Player.class);

        when(this.damageEvent.getEntity()).thenReturn(nonPlayer);
        when(this.damageEvent.getDamager()).thenReturn(damager);

        this.listener.onEntityDamageByEntity(this.damageEvent);

        verifyNoInteractions(this.bountyManager);
    }

    @Test
    void onEntityDamageByEntityDoesNotTrackWhenDamagerIsNotPlayer() {
        Player target = mock(Player.class);
        Entity nonPlayer = mock(Entity.class);

        when(this.damageEvent.getEntity()).thenReturn(target);
        when(this.damageEvent.getDamager()).thenReturn(nonPlayer);

        this.listener.onEntityDamageByEntity(this.damageEvent);

        verifyNoInteractions(this.bountyManager);
    }

    @Test
    void onPlayerDeathDelegatesToBountyManager() {
        Player player = mock(Player.class);
        when(this.deathEvent.getEntity()).thenReturn(player);

        this.listener.onPlayerDeath(this.deathEvent);

        verify(this.bountyManager).handleBountyKill(player);
        verifyNoMoreInteractions(this.bountyManager);
    }
}
