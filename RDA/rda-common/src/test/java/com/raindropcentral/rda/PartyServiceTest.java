/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rda;

import com.raindropcentral.rda.database.entity.RDAParty;
import com.raindropcentral.rda.database.entity.RDAPartyInvite;
import com.raindropcentral.rda.database.entity.RDAPartyInviteStatus;
import com.raindropcentral.rda.database.entity.RDAPartyMember;
import com.raindropcentral.rda.database.entity.RDAPlayer;
import com.raindropcentral.rda.database.repository.RRDAParty;
import com.raindropcentral.rda.database.repository.RRDAPartyInvite;
import com.raindropcentral.rda.database.repository.RRDAPartyMember;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PartyService}.
 */
class PartyServiceTest {

    @Test
    void refreshesExistingPendingInviteForSamePartyAndTarget() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID inviterUuid = UUID.randomUUID();
        final UUID targetUuid = UUID.randomUUID();

        final PartyService.InviteResult firstInvite = context.service.invite(inviterUuid, targetUuid);
        final PartyService.InviteResult secondInvite = context.service.invite(inviterUuid, targetUuid);

        assertEquals(PartyService.InviteStatus.INVITED, firstInvite.status());
        assertEquals(PartyService.InviteStatus.INVITED, secondInvite.status());
        assertNotNull(firstInvite.invite());
        assertNotNull(secondInvite.invite());
        assertEquals(firstInvite.invite().getInviteUuid(), secondInvite.invite().getInviteUuid());
        assertEquals(1, context.invitesByUuid.size());
    }

    @Test
    void acceptsPendingInviteAndCreatesMembership() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID inviterUuid = UUID.randomUUID();
        final UUID targetUuid = UUID.randomUUID();

        context.service.invite(inviterUuid, targetUuid);
        final PartyService.AcceptResult acceptResult = context.service.accept(targetUuid, inviterUuid);

        assertEquals(PartyService.AcceptStatus.ACCEPTED, acceptResult.status());
        assertNotNull(context.service.getPartyForPlayer(targetUuid));
        assertEquals(RDAPartyInviteStatus.ACCEPTED, acceptResult.invite().getStatus());
    }

    @Test
    void declinesPendingInvite() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID inviterUuid = UUID.randomUUID();
        final UUID targetUuid = UUID.randomUUID();

        context.service.invite(inviterUuid, targetUuid);
        final PartyService.DeclineResult declineResult = context.service.decline(targetUuid, inviterUuid);

        assertEquals(PartyService.DeclineStatus.DECLINED, declineResult.status());
        assertNotNull(declineResult.invite());
        assertEquals(RDAPartyInviteStatus.DECLINED, declineResult.invite().getStatus());
    }

    @Test
    void enforcesConfiguredPartyCapacity() throws ReflectiveOperationException {
        final TestContext context = new TestContext(new PartyConfig(
            2,
            300L,
            new PartyConfig.XpShareSettings(0.75D, 0.25D, 32.0D)
        ));
        final UUID leaderUuid = UUID.randomUUID();
        final UUID firstMemberUuid = UUID.randomUUID();
        final UUID secondMemberUuid = UUID.randomUUID();

        context.service.invite(leaderUuid, firstMemberUuid);
        context.service.accept(firstMemberUuid, leaderUuid);
        final PartyService.InviteResult fullResult = context.service.invite(leaderUuid, secondMemberUuid);

        assertEquals(PartyService.InviteStatus.PARTY_FULL, fullResult.status());
    }

    @Test
    void transfersLeadershipWhenLeaderLeaves() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID leaderUuid = UUID.randomUUID();
        final UUID firstMemberUuid = UUID.randomUUID();
        final UUID secondMemberUuid = UUID.randomUUID();
        final RDAParty party = context.seedParty(leaderUuid, firstMemberUuid, secondMemberUuid);

        context.setMemberJoinOrder(firstMemberUuid, 10L);
        context.setMemberJoinOrder(secondMemberUuid, 20L);

        final PartyService.LeaveResult leaveResult = context.service.leave(leaderUuid);

        assertEquals(PartyService.LeaveStatus.LEFT, leaveResult.status());
        assertEquals(firstMemberUuid, leaveResult.newLeaderUuid());
        assertSame(firstMemberUuid, party.getLeaderProfile().getPlayerUuid());
        assertNull(context.service.getPartyForPlayer(leaderUuid));
    }

    @Test
    void transfersLeadershipWhenLeaderDisconnects() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID leaderUuid = UUID.randomUUID();
        final UUID firstMemberUuid = UUID.randomUUID();
        final UUID secondMemberUuid = UUID.randomUUID();
        final RDAParty party = context.seedParty(leaderUuid, firstMemberUuid, secondMemberUuid);

        context.setMemberJoinOrder(firstMemberUuid, 10L);
        context.setMemberJoinOrder(secondMemberUuid, 20L);

        context.service.handlePlayerQuit(leaderUuid);

        assertEquals(firstMemberUuid, party.getLeaderProfile().getPlayerUuid());
        assertNotNull(context.service.getPartyForPlayer(leaderUuid));
    }

    @Test
    void disbandsLastMemberPartyOnLeave() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID leaderUuid = UUID.randomUUID();

        context.seedParty(leaderUuid);
        final PartyService.LeaveResult leaveResult = context.service.leave(leaderUuid);

        assertEquals(PartyService.LeaveStatus.DISBANDED, leaveResult.status());
        assertNull(context.service.getPartyForPlayer(leaderUuid));
        assertTrue(context.partiesByUuid.isEmpty());
    }

    @Test
    void distributesConfiguredSharesToNearbyMembers() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID leaderUuid = UUID.randomUUID();
        final UUID memberUuid = UUID.randomUUID();
        context.seedParty(leaderUuid, memberUuid);

        final World world = mock(World.class);
        final Player earner = context.onlinePlayer(leaderUuid, "Leader", world, 0.0D, 0.0D, 0.0D);
        final Player member = context.onlinePlayer(memberUuid, "Member", world, 5.0D, 0.0D, 0.0D);
        final SkillProgressionService progressionService = mock(SkillProgressionService.class);

        context.service.distributeSkillXp(progressionService, earner, 20L, "Test");

        final InOrder inOrder = inOrder(progressionService);
        inOrder.verify(progressionService).awardDistributedBaseXp(earner, 15L, "Test", null);
        inOrder.verify(progressionService).awardDistributedBaseXp(member, 5L, "Test", earner);
    }

    @Test
    void grantsBonusShareWhenSharesExceedOneHundredPercent() throws ReflectiveOperationException {
        final TestContext context = new TestContext(new PartyConfig(
            4,
            300L,
            new PartyConfig.XpShareSettings(1.0D, 0.25D, 32.0D)
        ));
        final UUID leaderUuid = UUID.randomUUID();
        final UUID memberUuid = UUID.randomUUID();
        context.seedParty(leaderUuid, memberUuid);

        final World world = mock(World.class);
        final Player earner = context.onlinePlayer(leaderUuid, "Leader", world, 0.0D, 0.0D, 0.0D);
        final Player member = context.onlinePlayer(memberUuid, "Member", world, 3.0D, 0.0D, 0.0D);
        final SkillProgressionService progressionService = mock(SkillProgressionService.class);

        context.service.distributeSkillXp(progressionService, earner, 20L, "Test");

        verify(progressionService).awardDistributedBaseXp(earner, 20L, "Test", null);
        verify(progressionService).awardDistributedBaseXp(member, 5L, "Test", earner);
    }

    @Test
    void fallsBackToFullXpWhenNoRecipientsAreNearby() throws ReflectiveOperationException {
        final TestContext context = new TestContext(this.defaultConfig());
        final UUID leaderUuid = UUID.randomUUID();
        final UUID memberUuid = UUID.randomUUID();
        context.seedParty(leaderUuid, memberUuid);

        final World world = mock(World.class);
        final Player earner = context.onlinePlayer(leaderUuid, "Leader", world, 0.0D, 0.0D, 0.0D);
        context.onlinePlayer(memberUuid, "Member", world, 80.0D, 0.0D, 0.0D);
        final SkillProgressionService progressionService = mock(SkillProgressionService.class);

        context.service.distributeSkillXp(progressionService, earner, 20L, "Test");

        verify(progressionService).awardDistributedBaseXp(earner, 20L, "Test", null);
        verify(progressionService, never()).awardDistributedBaseXp(eq(context.onlinePlayers.get(memberUuid)), anyLong(), anyString(), eq(earner));
    }

    @Test
    void filtersRecipientsByRangeAndUsesDeterministicRemainderOrder() throws ReflectiveOperationException {
        final TestContext context = new TestContext(new PartyConfig(
            4,
            300L,
            new PartyConfig.XpShareSettings(0.0D, 1.0D, 32.0D)
        ));
        final UUID leaderUuid = UUID.randomUUID();
        final UUID firstMemberUuid = UUID.randomUUID();
        final UUID secondMemberUuid = UUID.randomUUID();
        final UUID ignoredMemberUuid = UUID.randomUUID();
        context.seedParty(leaderUuid, firstMemberUuid, secondMemberUuid, ignoredMemberUuid);
        context.setMemberJoinOrder(firstMemberUuid, 10L);
        context.setMemberJoinOrder(secondMemberUuid, 20L);
        context.setMemberJoinOrder(ignoredMemberUuid, 30L);

        final World world = mock(World.class);
        final World otherWorld = mock(World.class);
        final Player earner = context.onlinePlayer(leaderUuid, "Leader", world, 0.0D, 0.0D, 0.0D);
        final Player firstMember = context.onlinePlayer(firstMemberUuid, "First", world, 2.0D, 0.0D, 0.0D);
        final Player secondMember = context.onlinePlayer(secondMemberUuid, "Second", world, 3.0D, 0.0D, 0.0D);
        context.onlinePlayer(ignoredMemberUuid, "Ignored", otherWorld, 1.0D, 0.0D, 0.0D);
        final SkillProgressionService progressionService = mock(SkillProgressionService.class);

        context.service.distributeSkillXp(progressionService, earner, 5L, "Test");

        final InOrder inOrder = inOrder(progressionService);
        inOrder.verify(progressionService).awardDistributedBaseXp(earner, 0L, "Test", null);
        inOrder.verify(progressionService).awardDistributedBaseXp(firstMember, 3L, "Test", earner);
        inOrder.verify(progressionService).awardDistributedBaseXp(secondMember, 2L, "Test", earner);
    }

    private @NotNull PartyConfig defaultConfig() {
        return new PartyConfig(4, 300L, new PartyConfig.XpShareSettings(0.75D, 0.25D, 32.0D));
    }

    private static final class TestContext {

        private final JavaPlugin plugin = mock(JavaPlugin.class);
        private final Server server = mock(Server.class);
        private final RRDAPlayer playerRepository = mock(RRDAPlayer.class);
        private final RRDAParty partyRepository = mock(RRDAParty.class);
        private final RRDAPartyMember partyMemberRepository = mock(RRDAPartyMember.class);
        private final RRDAPartyInvite partyInviteRepository = mock(RRDAPartyInvite.class);
        private final Map<UUID, RDAPlayer> playersByUuid = new LinkedHashMap<>();
        private final Map<UUID, RDAParty> partiesByUuid = new LinkedHashMap<>();
        private final Map<UUID, RDAPartyMember> membersByPlayerUuid = new LinkedHashMap<>();
        private final Map<UUID, RDAPartyInvite> invitesByUuid = new LinkedHashMap<>();
        private final Map<UUID, Player> onlinePlayers = new LinkedHashMap<>();
        private final PartyService service;

        private TestContext(final @NotNull PartyConfig config) throws ReflectiveOperationException {
            when(this.plugin.getServer()).thenReturn(this.server);
            when(this.server.getPlayer(any(UUID.class))).thenAnswer(invocation -> this.onlinePlayers.get(invocation.getArgument(0)));
            this.stubPlayerRepository();
            this.stubPartyRepository();
            this.stubPartyMemberRepository();
            this.stubPartyInviteRepository();
            this.service = new PartyService(
                this.plugin,
                Logger.getLogger("PartyServiceTest"),
                config,
                this.playerRepository,
                this.partyRepository,
                this.partyMemberRepository,
                this.partyInviteRepository
            );
        }

        private void stubPlayerRepository() throws ReflectiveOperationException {
            when(this.playerRepository.findOrCreateByPlayer(any(UUID.class))).thenAnswer(invocation -> {
                final UUID playerUuid = invocation.getArgument(0);
                final RDAPlayer existingProfile = this.playersByUuid.get(playerUuid);
                if (existingProfile != null) {
                    return existingProfile;
                }
                return this.playerRepository.create(new RDAPlayer(playerUuid));
            });
            when(this.playerRepository.create(any(RDAPlayer.class))).thenAnswer(invocation -> {
                final RDAPlayer entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    setEntityId(entity, this.playersByUuid.size() + 1L);
                }
                this.playersByUuid.put(entity.getPlayerUuid(), entity);
                return entity;
            });
        }

        private void stubPartyRepository() throws ReflectiveOperationException {
            when(this.partyRepository.create(any(RDAParty.class))).thenAnswer(invocation -> {
                final RDAParty entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    setEntityId(entity, this.partiesByUuid.size() + 1L);
                }
                this.partiesByUuid.put(entity.getPartyUuid(), entity);
                return entity;
            });
            when(this.partyRepository.update(any(RDAParty.class))).thenAnswer(invocation -> {
                final RDAParty entity = invocation.getArgument(0);
                this.partiesByUuid.put(entity.getPartyUuid(), entity);
                return entity;
            });
            when(this.partyRepository.delete(anyLong())).thenAnswer(invocation -> {
                final long id = invocation.getArgument(0);
                return this.partiesByUuid.values().removeIf(party -> party.getId() != null && party.getId() == id);
            });
        }

        private void stubPartyMemberRepository() throws ReflectiveOperationException {
            when(this.partyMemberRepository.findByPlayer(any(UUID.class))).thenAnswer(
                invocation -> this.membersByPlayerUuid.get(invocation.getArgument(0))
            );
            when(this.partyMemberRepository.findAllByParty(any(UUID.class))).thenAnswer(invocation -> {
                final UUID partyUuid = invocation.getArgument(0);
                return this.membersByPlayerUuid.values().stream()
                    .filter(member -> member.getParty().getPartyUuid().equals(partyUuid))
                    .toList();
            });
            when(this.partyMemberRepository.create(any(RDAPartyMember.class))).thenAnswer(invocation -> {
                final RDAPartyMember entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    setEntityId(entity, this.membersByPlayerUuid.size() + 1L);
                }
                this.membersByPlayerUuid.put(entity.getPlayerProfile().getPlayerUuid(), entity);
                return entity;
            });
            when(this.partyMemberRepository.delete(anyLong())).thenAnswer(invocation -> {
                final long id = invocation.getArgument(0);
                return this.membersByPlayerUuid.values().removeIf(member -> member.getId() != null && member.getId() == id);
            });
        }

        private void stubPartyInviteRepository() throws ReflectiveOperationException {
            when(this.partyInviteRepository.findAll()).thenAnswer(invocation -> List.copyOf(this.invitesByUuid.values()));
            when(this.partyInviteRepository.findPendingByInvitedPlayer(any(UUID.class))).thenAnswer(invocation -> {
                final UUID invitedPlayerUuid = invocation.getArgument(0);
                return this.invitesByUuid.values().stream()
                    .filter(invite -> invite.getInvitedPlayerUuid().equals(invitedPlayerUuid))
                    .filter(RDAPartyInvite::isPending)
                    .toList();
            });
            when(this.partyInviteRepository.findAllByParty(any(UUID.class))).thenAnswer(invocation -> {
                final UUID partyUuid = invocation.getArgument(0);
                return this.invitesByUuid.values().stream()
                    .filter(invite -> invite.getParty().getPartyUuid().equals(partyUuid))
                    .toList();
            });
            when(this.partyInviteRepository.findPendingByPartyAndTarget(any(UUID.class), any(UUID.class))).thenAnswer(invocation -> {
                final UUID partyUuid = invocation.getArgument(0);
                final UUID targetUuid = invocation.getArgument(1);
                return this.invitesByUuid.values().stream()
                    .filter(invite -> invite.getParty().getPartyUuid().equals(partyUuid))
                    .filter(invite -> invite.getInvitedPlayerUuid().equals(targetUuid))
                    .filter(RDAPartyInvite::isPending)
                    .findFirst()
                    .orElse(null);
            });
            when(this.partyInviteRepository.create(any(RDAPartyInvite.class))).thenAnswer(invocation -> {
                final RDAPartyInvite entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    setEntityId(entity, this.invitesByUuid.size() + 1L);
                }
                this.invitesByUuid.put(entity.getInviteUuid(), entity);
                return entity;
            });
            when(this.partyInviteRepository.update(any(RDAPartyInvite.class))).thenAnswer(invocation -> {
                final RDAPartyInvite entity = invocation.getArgument(0);
                this.invitesByUuid.put(entity.getInviteUuid(), entity);
                return entity;
            });
            when(this.partyInviteRepository.delete(anyLong())).thenAnswer(invocation -> {
                final long id = invocation.getArgument(0);
                return this.invitesByUuid.values().removeIf(invite -> invite.getId() != null && invite.getId() == id);
            });
        }

        private @NotNull RDAParty seedParty(final @NotNull UUID leaderUuid, final @NotNull UUID... memberUuids)
            throws ReflectiveOperationException {
            final RDAPlayer leaderProfile = this.ensureProfile(leaderUuid);
            final RDAParty party = this.partyRepository.create(new RDAParty(leaderProfile));
            this.partyMemberRepository.create(new RDAPartyMember(party, leaderProfile));
            this.setMemberJoinOrder(leaderUuid, 0L);
            long joinedAt = 10L;
            for (final UUID memberUuid : memberUuids) {
                final RDAPlayer memberProfile = this.ensureProfile(memberUuid);
                this.partyMemberRepository.create(new RDAPartyMember(party, memberProfile));
                this.setMemberJoinOrder(memberUuid, joinedAt);
                joinedAt += 10L;
            }
            return party;
        }

        private @NotNull RDAPlayer ensureProfile(final @NotNull UUID playerUuid) throws ReflectiveOperationException {
            final RDAPlayer existing = this.playersByUuid.get(playerUuid);
            if (existing != null) {
                return existing;
            }

            return this.playerRepository.create(new RDAPlayer(playerUuid));
        }

        private void setMemberJoinOrder(final @NotNull UUID playerUuid, final long joinedAt) throws ReflectiveOperationException {
            final RDAPartyMember member = this.membersByPlayerUuid.get(playerUuid);
            assertNotNull(member);
            setField(member, "joinedAt", joinedAt);
        }

        private @NotNull Player onlinePlayer(
            final @NotNull UUID playerUuid,
            final @NotNull String playerName,
            final @NotNull World world,
            final double x,
            final double y,
            final double z
        ) {
            final Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(playerUuid);
            when(player.getName()).thenReturn(playerName);
            when(player.getWorld()).thenReturn(world);
            when(player.getLocation()).thenReturn(new Location(world, x, y, z));
            when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
            this.onlinePlayers.put(playerUuid, player);
            return player;
        }
    }

    private static void setEntityId(final Object target, final long value) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (final NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id");
    }

    private static void setField(final Object target, final String fieldName, final Object value)
        throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (final NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
