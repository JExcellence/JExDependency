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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.configs.*;
import com.raindropcentral.rdt.database.entity.NationInvite;
import com.raindropcentral.rdt.database.entity.NationInviteStatus;
import com.raindropcentral.rdt.database.entity.NationInviteType;
import com.raindropcentral.rdt.database.entity.NationStatus;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.database.entity.RTownRelationship;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRNation;
import com.raindropcentral.rdt.database.repository.RRNationInvite;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.database.repository.RRTownChunk;
import com.raindropcentral.rdt.database.repository.RRTownRelationship;
import com.raindropcentral.rdt.items.SeedBox;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.FarmReplantPriority;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rdt.utils.TownRelationshipState;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.reward.RewardService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TownRuntimeServiceTest {

    @Mock
    private JavaPlugin javaPlugin;

    @Mock
    private RRTownChunk townChunkRepository;

    @Mock
    private RRDTPlayer playerRepository;

    @Mock
    private RRTown townRepository;

    @Mock
    private RRTownRelationship townRelationshipRepository;

    @Mock
    private RRNation nationRepository;

    @Mock
    private RRNationInvite nationInviteRepository;

    @Mock
    private TownService townService;

    @Mock
    private Location location;

    @Mock
    private World world;

    @Mock
    private Chunk chunk;

    @Mock
    private Chunk loadedChunk;

    @Mock
    private Chunk secondaryLoadedChunk;

    @Mock
    private Player player;

    @Mock
    private RTownChunk townChunk;

    @Mock
    private Server server;

    @Mock
    private Monster hostileMonster;

    @Mock
    private Animals passiveAnimal;

    @Mock
    private Animals overriddenPassiveAnimal;

    @Test
    void getChunkAtUsesBlockCoordinatesWithoutChunkLookup() throws ReflectiveOperationException {
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(-5);
        when(location.getBlockZ()).thenReturn(1);
        when(townChunkRepository.findByChunk("world", -1, 0)).thenReturn(townChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertSame(townChunk, service.getChunkAt(location));
        verify(townChunkRepository).findByChunk("world", -1, 0);
        verify(location, never()).getChunk();
    }

    @Test
    void getTownCreationProgressUsesNexusLevelOneProgress() throws ReflectiveOperationException {
        final UUID playerUuid = UUID.randomUUID();
        final RDTPlayer playerData = new RDTPlayer(playerUuid);
        playerData.setTownCreationCurrencyProgress("nexus.level.1.town_charter", 250.0D);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "1":
                    combat:
                      max_health: 1600
                      defense: 5
                    requirements:
                      town_charter:
                        type: CURRENCY
                        currency: vault
                        amount: 1000
                        consumable: true
                        description: Charter funding
                    rewards:
                      town_broadcast:
                        type: COMMAND
                        command: "rt broadcast {town_uuid} founded"
                """,
            ""
        );
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);
        final JExEconomyBridge economyBridge = org.mockito.Mockito.mock(JExEconomyBridge.class);

        try (MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class)) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(economyBridge);
            when(economyBridge.hasCurrency("vault")).thenReturn(true);
            when(economyBridge.getBalance(player, "vault")).thenReturn(500.0D);

            final TownCreationProgressSnapshot snapshot = service.getTownCreationProgress(player);

            assertTrue(snapshot.available());
            assertFalse(snapshot.alreadyInTown());
            assertFalse(snapshot.readyToCreate());
            assertEquals(1, snapshot.requirements().size());
            assertEquals(1, snapshot.rewards().size());
            assertEquals(250.0D, snapshot.findRequirement("town_charter").currentAmount(), 0.000_1D);
            assertEquals(500.0D, snapshot.findRequirement("town_charter").availableAmount(), 0.000_1D);
            assertEquals(0.25D, snapshot.progress(), 0.000_1D);
            assertFalse(service.canCreateTown(player));
        }
    }

    @Test
    void contributeTownCreationCurrencyPersistsPlayerProgressAndUnlocksCreation() throws ReflectiveOperationException {
        final UUID playerUuid = UUID.randomUUID();
        final RDTPlayer playerData = new RDTPlayer(playerUuid);
        playerData.setTownCreationCurrencyProgress("nexus.level.1.town_charter", 250.0D);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "1":
                    combat:
                      max_health: 1600
                      defense: 5
                    requirements:
                      town_charter:
                        type: CURRENCY
                        currency: vault
                        amount: 1000
                        consumable: true
                    rewards:
                      town_broadcast:
                        type: COMMAND
                        command: "rt broadcast {town_uuid} founded"
                """,
            "",
            "",
            "",
            ""
        );
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);
        final JExEconomyBridge economyBridge = org.mockito.Mockito.mock(JExEconomyBridge.class);

        try (MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class)) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(economyBridge);
            when(economyBridge.hasCurrency("vault")).thenReturn(true);
            when(economyBridge.getBalance(player, "vault")).thenReturn(900.0D);
            when(economyBridge.withdraw(player, "vault", 750.0D)).thenReturn(CompletableFuture.completedFuture(true));

            final ContributionResult result = service.contributeTownCreationCurrency(player, "town_charter", 750.0D);

            assertEquals(ContributionStatus.SUCCESS, result.status());
            assertEquals(750.0D, result.contributedAmount(), 0.000_1D);
            assertTrue(result.requirementCompleted());
            assertTrue(result.levelReady());
            assertEquals(1000.0D, playerData.getTownCreationCurrencyProgress("nexus.level.1.town_charter"), 0.000_1D);
            assertTrue(service.canCreateTown(player));
            verify(playerRepository).update(playerData);
        }
    }

    @Test
    void finalizeTownCreationGrantsCreationRewardsAndClearsPlayerProgress() throws ReflectiveOperationException {
        final UUID playerUuid = UUID.randomUUID();
        final UUID townUuid = UUID.randomUUID();
        final RDTPlayer playerData = new RDTPlayer(playerUuid);
        playerData.setTownCreationCurrencyProgress("nexus.level.1.town_charter", 1000.0D);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getX()).thenReturn(2);
        when(chunk.getZ()).thenReturn(3);
        when(location.clone()).thenReturn(location);
        when(location.add(0.5D, 1.0D, 0.5D)).thenReturn(location);
        when(townRepository.findByTName("Founders")).thenReturn(null);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "1":
                    combat:
                      max_health: 1600
                      defense: 5
                    requirements:
                      town_charter:
                        type: CURRENCY
                        currency: vault
                        amount: 1000
                        consumable: true
                    rewards:
                      town_broadcast:
                        type: COMMAND
                        command: "rt broadcast {town_uuid} founded"
                """,
            ""
        );
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);
        final JExEconomyBridge economyBridge = org.mockito.Mockito.mock(JExEconomyBridge.class);
        final RewardService rewardService = org.mockito.Mockito.mock(RewardService.class);

        try (
            MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class);
            MockedStatic<RewardService> mockedRewardService = org.mockito.Mockito.mockStatic(RewardService.class)
        ) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(economyBridge);
            when(economyBridge.hasCurrency("vault")).thenReturn(true);
            when(economyBridge.getBalance(player, "vault")).thenReturn(0.0D);
            mockedRewardService.when(RewardService::getInstance).thenReturn(rewardService);
            when(rewardService.grantAll(eq(player), anyList())).thenReturn(CompletableFuture.completedFuture(true));

            final RTown createdTown = service.finalizeTownCreation(player, location, townUuid, "Founders");

            assertNotNull(createdTown);
            assertEquals(townUuid, createdTown.getTownUUID());
            assertEquals("#55CDFC", createdTown.getTownColorHex());
            assertEquals(1, createdTown.getNexusLevel());
            assertEquals(1, createdTown.getTownLevel());
            assertEquals(1600.0D, createdTown.getCurrentNexusHealth(1600.0D), 0.000_1D);
            assertEquals(townUuid, playerData.getTownUUID());
            assertEquals(RTown.MAYOR_ROLE_ID, playerData.getTownRoleId());
            assertTrue(playerData.getTownCreationCurrencyProgress().isEmpty());
            assertEquals(1, createdTown.getChunks().size());
            assertEquals(ChunkType.NEXUS, createdTown.getChunks().getFirst().getChunkType());

            final var inOrder = inOrder(townRepository, rewardService);
            inOrder.verify(townRepository).create(createdTown);
            inOrder.verify(rewardService).grantAll(eq(player), anyList());
            verify(townRepository).update(createdTown);
        }
    }

    @Test
    void acceptNationInviteFormsNationAndAssignsMemberTowns() throws ReflectiveOperationException {
        final UUID capitalPlayerUuid = UUID.randomUUID();
        final UUID invitedPlayerUuid = UUID.randomUUID();
        final RTown capitalTown = this.createUnlockedTown("Capital");
        final RTown invitedTown = this.createUnlockedTown("Invited");
        final RDTPlayer capitalPlayerData = new RDTPlayer(capitalPlayerUuid, capitalTown.getTownUUID(), RTown.MAYOR_ROLE_ID);
        final RDTPlayer invitedPlayerData = new RDTPlayer(invitedPlayerUuid, invitedTown.getTownUUID(), RTown.MAYOR_ROLE_ID);
        capitalTown.setLevelCurrencyProgress("nation.level.1.charter", 1_000.0D);

        final RDT plugin = this.createPluginWithNationConfig(
            """
                town:
                  relationship_unlock_level: 5
                  relationship_change_cooldown_seconds: 60
                  nation_unlock_level: 5
                  nation_min_towns: 2
                  nation_invite_timeout_seconds: 3600
                """,
            """
                levels:
                  "1":
                    requirements:
                      charter:
                        type: CURRENCY
                        currency: vault
                        amount: 1000
                        consumable: true
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        setField(plugin, "nationRepository", nationRepository);
        setField(plugin, "nationInviteRepository", nationInviteRepository);
        this.stubTownRepository(capitalTown, invitedTown);
        this.stubPlayerRepository(capitalPlayerData, invitedPlayerData);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();
        this.stubNationRepository();
        this.stubNationInviteRepository();

        final RTownRelationship storedRelationship = new RTownRelationship(capitalTown.getTownUUID(), invitedTown.getTownUUID());
        storedRelationship.setConfirmedState(TownRelationshipState.ALLIED);
        storedRelationship.setPendingState(TownRelationshipState.HOSTILE);
        storedRelationship.setPendingRequesterTownUuid(invitedTown.getTownUUID());
        storedRelationship.setCooldownUntilMillis(60_000L);
        setEntityId(storedRelationship, 1L);
        relationships.put(storedRelationship.getPairKey(), storedRelationship);

        when(player.getUniqueId()).thenReturn(capitalPlayerUuid);
        final Player invitedPlayer = org.mockito.Mockito.mock(Player.class);
        when(invitedPlayer.getUniqueId()).thenReturn(invitedPlayerUuid);

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final JExEconomyBridge economyBridge = org.mockito.Mockito.mock(JExEconomyBridge.class);

        try (MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class)) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(economyBridge);
            when(economyBridge.hasCurrency("vault")).thenReturn(true);
            when(economyBridge.getBalance(player, "vault")).thenReturn(0.0D);

            final NationActionResult createResult = service.createNation(
                player,
                "Federation",
                Set.of(invitedTown.getTownUUID())
            );

            assertEquals(NationActionStatus.SUCCESS, createResult.status());
            assertNotNull(createResult.nation());
            assertEquals(NationStatus.PENDING, createResult.nation().getStatus());

            final NationInvite pendingInvite = service.getPendingNationInviteFor(invitedTown);
            assertNotNull(pendingInvite);
            assertEquals(NationInviteType.FORMATION, pendingInvite.getInviteType());
            assertTrue(pendingInvite.isPending());

            final NationInviteResponseResult acceptResult = service.acceptNationInvite(invitedPlayer, pendingInvite);

            assertEquals(NationInviteResponseStatus.ACCEPTED, acceptResult.status());
            assertNotNull(acceptResult.nation());
            assertEquals(NationStatus.ACTIVE, acceptResult.nation().getStatus());
            assertEquals(acceptResult.nation().getNationUuid(), capitalTown.getNationUuid());
            assertEquals(acceptResult.nation().getNationUuid(), invitedTown.getNationUuid());
            assertEquals(2, service.getNationMemberTowns(acceptResult.nation()).size());
            assertEquals(1, acceptResult.nation().getNationLevel());
            assertTrue(acceptResult.nation().getLevelCurrencyProgress().isEmpty());
            assertTrue(acceptResult.nation().getLevelItemProgress().isEmpty());
            assertEquals(TownRelationshipState.ALLIED, storedRelationship.getConfirmedState());
            assertNull(storedRelationship.getPendingState());
            assertEquals(0L, storedRelationship.getCooldownUntilMillis());
            assertEquals(0.0D, capitalTown.getLevelCurrencyProgress("nation.level.1.charter"), 0.000_1D);
            assertEquals(NationInviteStatus.ACCEPTED, acceptResult.invite().getStatus());
        }
    }

    @Test
    void memberTownContributionsFlowIntoSharedNationProgressPool() throws ReflectiveOperationException {
        final UUID memberPlayerUuid = UUID.randomUUID();
        final RTown capitalTown = this.createUnlockedTown("Capital");
        final RTown memberTown = this.createUnlockedTown("Member");
        final RDTPlayer memberPlayerData = new RDTPlayer(memberPlayerUuid, memberTown.getTownUUID(), RTown.MEMBER_ROLE_ID);
        final RNation nation = new RNation(
            UUID.randomUUID(),
            "Federation",
            capitalTown.getTownUUID(),
            capitalTown.getTownUUID(),
            null,
            2,
            0L
        );
        nation.setStatus(NationStatus.ACTIVE);
        setEntityId(nation, 1L);
        capitalTown.setNationUuid(nation.getNationUuid());
        memberTown.setNationUuid(nation.getNationUuid());

        final RDT plugin = this.createPluginWithNationConfig(
            """
                town:
                  nation_unlock_level: 5
                  nation_min_towns: 2
                """,
            """
                progression:
                  levels:
                    "1":
                      requirements: {}
                      rewards: {}
                    "2":
                      requirements:
                        treasury:
                          type: CURRENCY
                          currency: vault
                          amount: 500
                          consumable: true
                      rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "nationRepository", nationRepository);
        this.stubTownRepository(capitalTown, memberTown);
        this.stubPlayerRepository(memberPlayerData);
        final Map<UUID, RNation> nations = this.stubNationRepository();
        nations.put(nation.getNationUuid(), nation);
        when(player.getUniqueId()).thenReturn(memberPlayerUuid);

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final JExEconomyBridge economyBridge = org.mockito.Mockito.mock(JExEconomyBridge.class);

        try (MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class)) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(economyBridge);
            when(economyBridge.hasCurrency("vault")).thenReturn(true);
            when(economyBridge.getBalance(player, "vault")).thenReturn(500.0D);
            when(economyBridge.withdraw(player, "vault", 250.0D)).thenReturn(CompletableFuture.completedFuture(true));

            final LevelProgressSnapshot snapshot = service.getNationLevelProgress(player, memberTown);
            assertEquals(1, snapshot.currentLevel());
            assertEquals(2, snapshot.displayLevel());
            assertEquals(1, nation.getNationLevel());

            final ContributionResult result = service.contributeNationCurrency(player, memberTown, "treasury", 250.0D);

            assertEquals(ContributionStatus.SUCCESS, result.status());
            assertEquals(250.0D, result.contributedAmount(), 0.000_1D);
            assertEquals(250.0D, nation.getLevelCurrencyProgress("nation.level.2.treasury"), 0.000_1D);
            assertTrue(capitalTown.getLevelCurrencyProgress().isEmpty());
            assertTrue(memberTown.getLevelCurrencyProgress().isEmpty());
            assertEquals(
                250.0D,
                service.getNationLevelProgress(player, capitalTown).findRequirement("treasury").currentAmount(),
                0.000_1D
            );
        }
    }

    @Test
    void onlyCapitalTownManagersCanFinalizeNationLevel() throws ReflectiveOperationException {
        final UUID capitalPlayerUuid = UUID.randomUUID();
        final UUID memberPlayerUuid = UUID.randomUUID();
        final RTown capitalTown = this.createUnlockedTown("Capital");
        final RTown memberTown = this.createUnlockedTown("Member");
        final RDTPlayer capitalPlayerData = new RDTPlayer(capitalPlayerUuid, capitalTown.getTownUUID(), RTown.MAYOR_ROLE_ID);
        final RDTPlayer memberPlayerData = new RDTPlayer(memberPlayerUuid, memberTown.getTownUUID(), RTown.MAYOR_ROLE_ID);
        final RNation nation = new RNation(
            UUID.randomUUID(),
            "Federation",
            capitalTown.getTownUUID(),
            capitalTown.getTownUUID(),
            null,
            2,
            0L
        );
        nation.setStatus(NationStatus.ACTIVE);
        nation.setNationLevel(1);
        nation.setLevelCurrencyProgress("nation.level.2.treasury", 100.0D);
        setEntityId(nation, 1L);
        capitalTown.setNationUuid(nation.getNationUuid());
        memberTown.setNationUuid(nation.getNationUuid());

        final RDT plugin = this.createPluginWithNationConfig(
            """
                town:
                  nation_unlock_level: 5
                  nation_min_towns: 2
                """,
            """
                progression:
                  levels:
                    "1":
                      requirements: {}
                      rewards: {}
                    "2":
                      requirements:
                        treasury:
                          type: CURRENCY
                          currency: vault
                          amount: 100
                          consumable: true
                      rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "nationRepository", nationRepository);
        this.stubTownRepository(capitalTown, memberTown);
        this.stubPlayerRepository(capitalPlayerData, memberPlayerData);
        final Map<UUID, RNation> nations = this.stubNationRepository();
        nations.put(nation.getNationUuid(), nation);

        final Player capitalPlayer = org.mockito.Mockito.mock(Player.class);
        when(capitalPlayer.getUniqueId()).thenReturn(capitalPlayerUuid);
        final Player memberPlayer = org.mockito.Mockito.mock(Player.class);
        org.mockito.Mockito.lenient().when(memberPlayer.getUniqueId()).thenReturn(memberPlayerUuid);

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final LevelUpResult memberResult = service.levelUpNation(memberPlayer, memberTown);
        final LevelUpResult capitalResult = service.levelUpNation(capitalPlayer, capitalTown);

        assertEquals(LevelUpStatus.NO_PERMISSION, memberResult.status());
        assertEquals(LevelUpStatus.SUCCESS, capitalResult.status());
        assertEquals(2, nation.getNationLevel());
    }

    @Test
    void promoteNationCapitalPreservesNationProgress() throws ReflectiveOperationException {
        final UUID capitalPlayerUuid = UUID.randomUUID();
        final RTown capitalTown = this.createUnlockedTown("Capital");
        final RTown memberTown = this.createUnlockedTown("Member");
        final RDTPlayer capitalPlayerData = new RDTPlayer(capitalPlayerUuid, capitalTown.getTownUUID(), RTown.MAYOR_ROLE_ID);
        final RNation nation = new RNation(
            UUID.randomUUID(),
            "Federation",
            capitalTown.getTownUUID(),
            capitalTown.getTownUUID(),
            null,
            2,
            0L
        );
        nation.setStatus(NationStatus.ACTIVE);
        nation.setNationLevel(2);
        nation.setLevelCurrencyProgress("nation.level.3.treasury", 75.0D);
        setEntityId(nation, 1L);
        capitalTown.setNationUuid(nation.getNationUuid());
        memberTown.setNationUuid(nation.getNationUuid());

        final RDT plugin = this.createPluginWithNationConfig(
            """
                town:
                  nation_unlock_level: 5
                  nation_min_towns: 2
                """,
            ""
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "nationRepository", nationRepository);
        this.stubTownRepository(capitalTown, memberTown);
        this.stubPlayerRepository(capitalPlayerData);
        final Map<UUID, RNation> nations = this.stubNationRepository();
        nations.put(nation.getNationUuid(), nation);
        when(player.getUniqueId()).thenReturn(capitalPlayerUuid);

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final NationActionResult result = service.promoteNationCapital(player, nation, memberTown.getTownUUID());

        assertEquals(NationActionStatus.SUCCESS, result.status());
        assertNotNull(result.nation());
        assertEquals(memberTown.getTownUUID(), result.nation().getCapitalTownUuid());
        assertEquals(2, result.nation().getNationLevel());
        assertEquals(75.0D, result.nation().getLevelCurrencyProgress("nation.level.3.treasury"), 0.000_1D);
    }

    @Test
    void leavingNationDisbandsItWhenMembershipDropsBelowMinimumThreshold() throws ReflectiveOperationException {
        final UUID memberPlayerUuid = UUID.randomUUID();
        final RTown capitalTown = this.createUnlockedTown("Capital");
        final RTown memberTown = this.createUnlockedTown("Member");
        final RDTPlayer memberPlayerData = new RDTPlayer(memberPlayerUuid, memberTown.getTownUUID(), RTown.MAYOR_ROLE_ID);
        final RNation nation = new RNation(
            UUID.randomUUID(),
            "Federation",
            capitalTown.getTownUUID(),
            capitalTown.getTownUUID(),
            null,
            2,
            0L
        );
        nation.setStatus(NationStatus.ACTIVE);
        nation.setNationLevel(3);
        nation.setLevelCurrencyProgress("nation.level.4.treasury", 275.0D);
        setEntityId(nation, 1L);
        capitalTown.setNationUuid(nation.getNationUuid());
        memberTown.setNationUuid(nation.getNationUuid());

        final RDT plugin = this.createPluginWithNationConfig(
            """
                town:
                  relationship_unlock_level: 5
                  nation_unlock_level: 5
                  nation_min_towns: 2
                """,
            """
                levels:
                  "1":
                    requirements: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "nationRepository", nationRepository);
        setField(plugin, "nationInviteRepository", nationInviteRepository);
        this.stubTownRepository(capitalTown, memberTown);
        this.stubPlayerRepository(memberPlayerData);
        final Map<UUID, RNation> nations = this.stubNationRepository();
        this.stubNationInviteRepository();
        nations.put(nation.getNationUuid(), nation);
        when(player.getUniqueId()).thenReturn(memberPlayerUuid);

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final NationActionResult leaveResult = service.leaveNation(player);

        assertEquals(NationActionStatus.SUCCESS, leaveResult.status());
        assertNotNull(leaveResult.nation());
        assertEquals(NationStatus.DISBANDED, leaveResult.nation().getStatus());
        assertEquals(3, leaveResult.nation().getNationLevel());
        assertEquals(275.0D, leaveResult.nation().getLevelCurrencyProgress("nation.level.4.treasury"), 0.000_1D);
        assertNull(capitalTown.getNationUuid());
        assertNull(memberTown.getNationUuid());
        assertEquals(NationStatus.DISBANDED, service.getNation(nation.getNationUuid()).getStatus());
    }

    @Test
    void setTownColorUpdatesLiveTownAndLocalReference() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownColor(staleTown, "ff00aa"));
        assertEquals("#FF00AA", liveTown.getTownColorHex());
        assertEquals("#FF00AA", staleTown.getTownColorHex());
        verify(townRepository).update(liveTown);
    }

    @Test
    void isChunkClaimableRejectsDiagonalAdjacencyWhenCornersAreExcluded() {
        final RTown town = createTownWithClaim("world", 0, 0);
        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """));

        assertTrue(service.isChunkClaimable(town, "world", 1, 0));
        assertFalse(service.isChunkClaimable(town, "world", 1, 1));
    }

    @Test
    void isChunkClaimableAllowsDiagonalAdjacencyWhenCornersAreIncluded() {
        final RTown town = createTownWithClaim("world", 0, 0);
        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: false
            global_max_chunk_limit: 64
            """));

        assertTrue(service.isChunkClaimable(town, "world", 1, 1));
    }

    @Test
    void isChunkClaimableAllowsOneFobWithoutAdjacency() {
        final RTown town = createTownWithClaim("world", 0, 0);
        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """));

        assertTrue(service.isChunkClaimable(town, "world_nether", 12, 14, ChunkType.FOB));

        town.addChunk(new RTownChunk(town, "world_nether", 12, 14, ChunkType.FOB));

        assertFalse(service.isChunkClaimable(town, "world", 20, 20, ChunkType.FOB));
    }

    @Test
    void isChunkClaimableRejectsFobWhenTheTownIsAtTheGlobalChunkLimit() {
        final RTown town = createTownWithClaim("world", 0, 0);
        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 1
            """));

        assertFalse(service.isChunkClaimable(town, "world", 8, 8, ChunkType.FOB));
    }

    @Test
    void isChunkClaimableRejectsChunksThatAnotherTownAlreadyOwns() throws ReflectiveOperationException {
        final RTown town = createTownWithClaim("world", 0, 0);
        final RTown foreignTown = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Foreign", null);
        final RTownChunk foreignChunk = new RTownChunk(foreignTown, "world", 5, 5, ChunkType.DEFAULT);
        final RDT plugin = this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """);
        setField(plugin, "townChunkRepository", townChunkRepository);
        when(townChunkRepository.findByChunk("world", 5, 5)).thenReturn(foreignChunk);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.isChunkClaimable(town, "world", 5, 5));
        assertFalse(service.isChunkClaimable(town, "world", 5, 5, ChunkType.FOB));
    }

    @Test
    void claimChunkCreatesDefaultChunkTypeWhenPlacementIsValid() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, playerUuid, "Test Town", null);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.CLAIM_CHUNK);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getX()).thenReturn(2);
        when(chunk.getZ()).thenReturn(3);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(town);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        final RTownChunk claimedChunk = service.claimChunk(player, location, townUuid, "world", 2, 3);

        assertNotNull(claimedChunk);
        assertEquals(ChunkType.DEFAULT, claimedChunk.getChunkType());
        verify(townRepository).update(town);
    }

    @Test
    void claimChunkCreatesFobWhenPlacementIsValidOutsideTheNormalYWindow() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, playerUuid, "Test Town", null);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.CLAIM_CHUNK);
        town.setNexusLocation(new Location(world, 0.0D, 64.0D, 0.0D));

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getX()).thenReturn(10);
        when(chunk.getZ()).thenReturn(11);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(town);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            chunk_block_min_y: -5
            chunk_block_max_y: 5
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        final RTownChunk claimedChunk = service.claimChunk(player, location, townUuid, "world", 10, 11, ChunkType.FOB);

        assertNotNull(claimedChunk);
        assertEquals(ChunkType.FOB, claimedChunk.getChunkType());
        verify(townRepository).update(town);
    }

    @Test
    void claimChunkRejectsFobWhenPlacedOutsideItsReservedChunk() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, playerUuid, "Test Town", null);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.CLAIM_CHUNK);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getX()).thenReturn(10);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(town);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfig("""
            exclude_corner_claim_adjacency: true
            global_max_chunk_limit: 64
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertNull(service.claimChunk(player, location, townUuid, "world", 9, 11, ChunkType.FOB));
        verify(townRepository, never()).update(town);
    }

    @Test
    void resolveFobHelpersReturnTheTownFobAndItsCenteredTeleportLocation() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        final RTownChunk fobChunk = new RTownChunk(town, "world", 4, 5, ChunkType.FOB);
        final Location markerLocation = new Location(world, 64.0D, 80.0D, 96.0D);
        fobChunk.setChunkBlockLocation(markerLocation);
        town.addChunk(new RTownChunk(town, "world", 0, 0, ChunkType.DEFAULT));
        town.addChunk(fobChunk);

        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            global_max_chunk_limit: 64
            """));

        assertTrue(service.hasFobChunk(town));
        assertSame(fobChunk, service.getFobChunk(town));
        assertEquals(new Location(world, 64.5D, 81.0D, 96.5D), service.resolveFobTeleportLocation(town));
    }

    @Test
    void getNexusLevelProgressUsesLevelsFromNexusConfig() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "3":
                    requirements: {}
                    rewards: {}
                  "7":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        final LevelProgressSnapshot snapshot = service.getNexusLevelProgress(player, liveTown);

        assertEquals(1, snapshot.currentLevel());
        assertEquals(3, snapshot.displayLevel());
        assertEquals(7, snapshot.maxLevel());
    }

    @Test
    void levelUpNexusRequiresUpgradePermission() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MEMBER_ROLE_ID);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        final LevelUpResult result = service.levelUpNexus(player, liveTown);

        assertEquals(LevelUpStatus.NO_PERMISSION, result.status());
        assertEquals(1, liveTown.getTownLevel());
    }

    @Test
    void levelUpNexusSucceedsWhenReadyAndConfiguredInNexusYaml() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        liveTown.setCurrentNexusHealth(125.0D, 1000.0D);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.UPGRADE_TOWN);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    combat:
                      max_health: 2000
                      defense: 9
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        final LevelUpResult result = service.levelUpNexus(player, liveTown);

        assertEquals(LevelUpStatus.SUCCESS, result.status());
        assertEquals(2, liveTown.getTownLevel());
        final NexusCombatSnapshot combatSnapshot = service.getNexusCombatSnapshot(liveTown);
        assertEquals(2000.0D, combatSnapshot.currentHealth(), 0.000_1D);
        assertEquals(2000.0D, combatSnapshot.maxHealth(), 0.000_1D);
        assertEquals(9.0D, combatSnapshot.defense(), 0.000_1D);
        verify(townRepository).update(liveTown);
    }

    @Test
    void getNexusCombatSnapshotBackfillsLegacyPersistedHealthToConfiguredMaximum() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        setEntityId(liveTown, 1L);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "1":
                    combat:
                      max_health: 1800
                      defense: 4
                    requirements: {}
                    rewards: {}
                """,
            ""
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        final NexusCombatSnapshot snapshot = service.getNexusCombatSnapshot(liveTown);

        assertTrue(liveTown.hasPersistedCurrentNexusHealth());
        assertEquals(1800.0D, snapshot.currentHealth(), 0.000_1D);
        assertEquals(1800.0D, snapshot.maxHealth(), 0.000_1D);
        assertEquals(4.0D, snapshot.defense(), 0.000_1D);
        verify(townRepository).update(liveTown);
    }

    @Test
    void contributeSecurityCurrencySupportsCurrencyDisplayNameAliases() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.CONTRIBUTE);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements:
                      vault_upgrade:
                        type: CURRENCY
                        currency: "Gold Ingots"
                        amount: 1000
                        consumable: true
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);
        final JExEconomyBridge economyBridge = org.mockito.Mockito.mock(JExEconomyBridge.class);

        try (MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class)) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(economyBridge);
            when(economyBridge.hasCurrency(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> "gold_ingot".equals(invocation.getArgument(0)));
            when(economyBridge.getAvailableCurrencies()).thenReturn(Map.of("gold_ingot", "Gold Ingots"));
            when(economyBridge.getBalance(player, "gold_ingot")).thenReturn(1000.0D);
            when(economyBridge.withdraw(player, "gold_ingot", 250.0D))
                .thenReturn(CompletableFuture.completedFuture(true));

            final LevelProgressSnapshot snapshot = service.getSecurityLevelProgress(player, liveChunk);
            final ContributionResult contributionResult = service.contributeSecurityCurrency(
                player,
                liveChunk,
                "vault_upgrade",
                250.0D
            );

            assertEquals(1000.0D, snapshot.findRequirement("vault_upgrade").availableAmount(), 0.000_1D);
            assertEquals("gold_ingot", snapshot.findRequirement("vault_upgrade").currencyId());
            assertEquals(ContributionStatus.SUCCESS, contributionResult.status());
            assertEquals(250.0D, liveChunk.getLevelCurrencyProgress("security.level.2.vault_upgrade"), 0.000_1D);
        }
    }

    @Test
    void levelUpSecuritySkipsUnsupportedCurrencyRequirementWhenEconomyIsUnavailable() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.UPGRADE_CHUNK);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements:
                      vault_upgrade:
                        type: CURRENCY
                        currency: "vault"
                        amount: 1000
                        consumable: true
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        try (MockedStatic<JExEconomyBridge> mockedBridge = org.mockito.Mockito.mockStatic(JExEconomyBridge.class)) {
            mockedBridge.when(JExEconomyBridge::getBridge).thenReturn(null);

            final LevelProgressSnapshot snapshot = service.getSecurityLevelProgress(player, liveChunk);
            final LevelUpResult levelUpResult = service.levelUpSecurity(player, liveChunk);

            assertTrue(snapshot.requirements().isEmpty());
            assertTrue(snapshot.readyToLevelUp());
            assertNull(snapshot.findRequirement("vault_upgrade"));
            assertEquals(LevelUpStatus.SUCCESS, levelUpResult.status());
            assertEquals(2, liveChunk.getChunkLevel());
            verify(townRepository).update(liveTown);
        }
    }

    @Test
    void getChunkLevelProgressUsesTypeSpecificConfigsForBankFarmOutpostMedicAndArmory() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk bankChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.BANK);
        final RTownChunk farmChunk = new RTownChunk(liveTown, "world", 5, 6, ChunkType.FARM);
        final RTownChunk outpostChunk = new RTownChunk(liveTown, "world", 7, 8, ChunkType.OUTPOST);
        final RTownChunk medicChunk = new RTownChunk(liveTown, "world", 9, 10, ChunkType.MEDIC);
        final RTownChunk armoryChunk = new RTownChunk(liveTown, "world", 11, 12, ChunkType.ARMORY);
        liveTown.addChunk(bankChunk);
        liveTown.addChunk(farmChunk);
        liveTown.addChunk(outpostChunk);
        liveTown.addChunk(medicChunk);
        liveTown.addChunk(armoryChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfigs(
            """
                chunk_type:
                  reset_state_on_change: true
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "3":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "4":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "5":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "6":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "7":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        final LevelProgressSnapshot bankSnapshot = service.getChunkLevelProgress(player, bankChunk);
        final LevelProgressSnapshot farmSnapshot = service.getChunkLevelProgress(player, farmChunk);
        final LevelProgressSnapshot outpostSnapshot = service.getChunkLevelProgress(player, outpostChunk);
        final LevelProgressSnapshot medicSnapshot = service.getChunkLevelProgress(player, medicChunk);
        final LevelProgressSnapshot armorySnapshot = service.getChunkLevelProgress(player, armoryChunk);

        assertEquals(LevelScope.BANK, bankSnapshot.scope());
        assertEquals(3, bankSnapshot.maxLevel());
        assertEquals(3, bankSnapshot.displayLevel());
        assertEquals(LevelScope.FARM, farmSnapshot.scope());
        assertEquals(4, farmSnapshot.maxLevel());
        assertEquals(4, farmSnapshot.displayLevel());
        assertEquals(LevelScope.OUTPOST, outpostSnapshot.scope());
        assertEquals(5, outpostSnapshot.maxLevel());
        assertEquals(5, outpostSnapshot.displayLevel());
        assertEquals(LevelScope.MEDIC, medicSnapshot.scope());
        assertEquals(6, medicSnapshot.maxLevel());
        assertEquals(6, medicSnapshot.displayLevel());
        assertEquals(LevelScope.ARMORY, armorySnapshot.scope());
        assertEquals(7, armorySnapshot.maxLevel());
        assertEquals(7, armorySnapshot.displayLevel());
    }

    @Test
    void levelUpChunkSupportsBankFarmOutpostMedicAndArmoryAndBankUsesConfigDrivenUnlocks() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk bankChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.BANK);
        final RTownChunk farmChunk = new RTownChunk(liveTown, "world", 5, 6, ChunkType.FARM);
        final RTownChunk outpostChunk = new RTownChunk(liveTown, "world", 7, 8, ChunkType.OUTPOST);
        final RTownChunk medicChunk = new RTownChunk(liveTown, "world", 9, 10, ChunkType.MEDIC);
        final RTownChunk armoryChunk = new RTownChunk(liveTown, "world", 11, 12, ChunkType.ARMORY);
        liveTown.addChunk(bankChunk);
        liveTown.addChunk(farmChunk);
        liveTown.addChunk(outpostChunk);
        liveTown.addChunk(medicChunk);
        liveTown.addChunk(armoryChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        playerData.grantTownPermission(TownPermissions.UPGRADE_CHUNK);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);

        final RDT plugin = this.createPluginWithConfigs(
            """
                chunk_type:
                  reset_state_on_change: true
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, bankChunk).status());
        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, farmChunk).status());
        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, outpostChunk).status());
        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, medicChunk).status());
        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, armoryChunk).status());
        assertEquals(2, bankChunk.getChunkLevel());
        assertEquals(2, farmChunk.getChunkLevel());
        assertEquals(2, outpostChunk.getChunkLevel());
        assertEquals(2, medicChunk.getChunkLevel());
        assertEquals(2, armoryChunk.getChunkLevel());
        assertTrue(plugin.getBankConfig().getItemStorage().isUnlocked(bankChunk.getChunkLevel()));
        assertFalse(plugin.getBankConfig().getRemoteAccess().isUnlocked(bankChunk.getChunkLevel()));
        verify(townRepository, times(5)).update(liveTown);
    }

    @Test
    void setChunkTypeResetsChunkStateWhenConfigured() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);
        liveChunk.setChunkLevel(4);
        liveChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        liveChunk.setLevelCurrencyProgress("security.level.4.vault", 500.0D);

        final RDT plugin = this.createPluginWithConfig("""
            chunk_type:
              reset_state_on_change: true
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkType(liveChunk, ChunkType.DEFAULT));
        assertEquals(ChunkType.DEFAULT, liveChunk.getChunkType());
        assertEquals(1, liveChunk.getChunkLevel());
        assertTrue(liveChunk.getProtectionRoleIds().isEmpty());
        assertTrue(liveChunk.getLevelCurrencyProgress().isEmpty());
    }

    @Test
    void setChunkTypePreservesChunkStateWhenResetToggleIsDisabled() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);
        liveChunk.setChunkLevel(4);
        liveChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        liveChunk.setLevelCurrencyProgress("security.level.4.vault", 500.0D);

        final RDT plugin = this.createPluginWithConfig("""
            chunk_type:
              reset_state_on_change: false
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkType(liveChunk, ChunkType.DEFAULT));
        assertEquals(ChunkType.DEFAULT, liveChunk.getChunkType());
        assertEquals(4, liveChunk.getChunkLevel());
        assertEquals(RTown.MAYOR_ROLE_ID, liveChunk.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        assertEquals(500.0D, liveChunk.getLevelCurrencyProgress("security.level.4.vault"), 0.000_1D);
    }

    @Test
    void setChunkTypeUpdatesPlacedMarkerBlockToConfiguredChunkMaterial() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.DEFAULT);
        final Location markerLocation = new Location(world, 48.0D, 64.0D, 64.0D);
        final Block markerBlock = org.mockito.Mockito.mock(Block.class);
        liveTown.addChunk(liveChunk);
        liveChunk.setChunkBlockLocation(markerLocation);

        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getBlockAt(markerLocation)).thenReturn(markerBlock);

        final RDT plugin = this.createPluginWithConfigs(
            """
                chunk_type:
                  reset_state_on_change: false
                """,
            "",
            "",
            "",
            "",
            "",
            """
                block_material: SEA_LANTERN
                """,
            ""
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkType(liveChunk, ChunkType.MEDIC));
        assertEquals(ChunkType.MEDIC, liveChunk.getChunkType());
        verify(markerBlock).setType(Material.SEA_LANTERN, false);
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkTypeFallsBackToDefaultChunkMarkerMaterialWhenConfiguredBlockMaterialIsInvalid() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.DEFAULT);
        final Location markerLocation = new Location(world, 48.0D, 64.0D, 64.0D);
        final Block markerBlock = org.mockito.Mockito.mock(Block.class);
        liveTown.addChunk(liveChunk);
        liveChunk.setChunkBlockLocation(markerLocation);

        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getBlockAt(markerLocation)).thenReturn(markerBlock);

        final RDT plugin = this.createPluginWithConfigs(
            """
                chunk_type:
                  reset_state_on_change: false
                """,
            "",
            """
                block_material: HOPPER
                """,
            "",
            "",
            ""
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkType(liveChunk, ChunkType.SECURITY));
        assertEquals(ChunkType.SECURITY, liveChunk.getChunkType());
        verify(markerBlock).setType(Material.CRYING_OBSIDIAN, false);
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkTypeDoesNotFailWhenChunkMarkerLocationIsMissing() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.DEFAULT);
        liveTown.addChunk(liveChunk);

        final RDT plugin = this.createPluginWithConfigs(
            """
                chunk_type:
                  reset_state_on_change: false
                """,
            "",
            "",
            "",
            "",
            "",
            """
                block_material: SEA_LANTERN
                """,
            ""
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkType(liveChunk, ChunkType.MEDIC));
        assertEquals(ChunkType.MEDIC, liveChunk.getChunkType());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkTypeRejectsTransitionsIntoFob() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.DEFAULT);
        liveTown.addChunk(liveChunk);

        final RDT plugin = this.createPluginWithConfig("""
            chunk_type:
              reset_state_on_change: false
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.setChunkType(liveChunk, ChunkType.FOB));
        assertEquals(ChunkType.DEFAULT, liveChunk.getChunkType());
        verify(townRepository, never()).update(any(RTown.class));
    }

    @Test
    void setChunkTypeRejectsTransitionsOutOfFob() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.FOB);
        liveTown.addChunk(liveChunk);

        final RDT plugin = this.createPluginWithConfig("""
            chunk_type:
              reset_state_on_change: false
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.setChunkType(liveChunk, ChunkType.DEFAULT));
        assertEquals(ChunkType.FOB, liveChunk.getChunkType());
        verify(townRepository, never()).update(any(RTown.class));
    }

    @Test
    void setChunkTypeStillClearsFuelTankStateWhenLeavingSecurity() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);
        liveChunk.setFuelTankLocation(new Location(null, 48.0D, 64.0D, 64.0D));

        final RDT plugin = this.createPluginWithConfig("""
            chunk_type:
              reset_state_on_change: false
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkType(liveChunk, ChunkType.DEFAULT));
        assertEquals(ChunkType.DEFAULT, liveChunk.getChunkType());
        assertFalse(liveChunk.hasFuelTank());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkTypeEnteringFarmWithPreservedLevelInitializesFarmDefaultsAndGrantsSeedBox() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.DEFAULT);
        final PlayerInventory inventory = org.mockito.Mockito.mock(PlayerInventory.class);
        liveTown.addChunk(liveChunk);
        liveChunk.setChunkLevel(3);

        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(org.mockito.ArgumentMatchers.any(ItemStack.class))).thenReturn(new HashMap<>());
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfigs(
            """
                chunk_type:
                  reset_state_on_change: false
                """,
            "",
            "",
            "",
            """
                growth:
                  enabled_by_default: true
                seed_box:
                  unlock_level: 3
                replant:
                  unlock_level: 3
                  enabled_by_default: true
                  default_source_priority: INVENTORY_FIRST
                levels:
                  "3":
                    requirements: {}
                    rewards: {}
                """,
            "",
            "",
            ""
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);
        final ItemStack seedBoxItem = this.mockItemStack(Material.WHEAT_SEEDS, 1);

        try (MockedStatic<SeedBox> seedBoxMock = org.mockito.Mockito.mockStatic(SeedBox.class)) {
            seedBoxMock.when(() -> SeedBox.getSeedBoxItem(plugin, player, townUuid, "world", 3, 4))
                .thenReturn(seedBoxItem);

            final TownRuntimeService.ChunkTypeChangeResult result = service.setChunkType(player, liveChunk, ChunkType.FARM);

            assertTrue(result.success());
            assertTrue(result.seedBoxGranted());
            assertEquals(ChunkType.FARM, liveChunk.getChunkType());
            assertTrue(liveChunk.isFarmGrowthEnabled(false));
            assertTrue(liveChunk.isFarmAutoReplantEnabled(false));
            assertEquals(FarmReplantPriority.INVENTORY_FIRST, liveChunk.getFarmReplantPriorityValue());
            verify(inventory).addItem(org.mockito.ArgumentMatchers.any(ItemStack.class));
            verify(townRepository).update(liveTown);
        }
    }

    @Test
    void levelUpFarmChunkToSeedBoxUnlockGrantsBoundSeedBoxItem() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk farmChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.FARM);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MAYOR_ROLE_ID);
        final PlayerInventory inventory = org.mockito.Mockito.mock(PlayerInventory.class);
        liveTown.addChunk(farmChunk);
        farmChunk.setChunkLevel(2);
        playerData.grantTownPermission(TownPermissions.UPGRADE_CHUNK);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(org.mockito.ArgumentMatchers.any(ItemStack.class))).thenReturn(new HashMap<>());
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);
        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfigs(
            "",
            "",
            "",
            "",
            """
                seed_box:
                  unlock_level: 3
                replant:
                  unlock_level: 3
                levels:
                  "3":
                    requirements: {}
                    rewards: {}
                """,
            "",
            "",
            ""
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);
        final ItemStack seedBoxItem = this.mockItemStack(Material.WHEAT_SEEDS, 1);

        final LevelUpResult result;
        try (MockedStatic<SeedBox> seedBoxMock = org.mockito.Mockito.mockStatic(SeedBox.class)) {
            seedBoxMock.when(() -> SeedBox.getSeedBoxItem(plugin, player, townUuid, "world", 3, 4))
                .thenReturn(seedBoxItem);
            result = service.levelUpChunk(player, farmChunk);
        }

        assertEquals(LevelUpStatus.SUCCESS, result.status());
        assertEquals(3, farmChunk.getChunkLevel());
        verify(inventory).addItem(org.mockito.ArgumentMatchers.any(ItemStack.class));
    }

    @Test
    void syncSeedBoxContentsFiltersDisallowedMaterials() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk farmChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.FARM);
        liveTown.addChunk(farmChunk);
        farmChunk.setSeedBoxLocation(new Location(null, 48.0D, 64.0D, 64.0D));

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfigs(
            "",
            "",
            "",
            "",
            """
                seed_box:
                  allowed_materials:
                    - WHEAT_SEEDS
                """,
            "",
            "",
            ""
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.syncSeedBoxContents(farmChunk, Map.of(
            "0", this.mockItemStack(Material.WHEAT_SEEDS, 8),
            "1", this.mockItemStack(Material.DIAMOND, 2)
        )));
        assertEquals(1, farmChunk.getSeedBoxContents().size());
        assertEquals(Material.WHEAT_SEEDS, farmChunk.getSeedBoxContents().get("0").getType());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownProtectionRoleIdRefreshesTheLiveTownBeforeSaving() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              archetype_change_cooldown_seconds: 86400
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownProtectionRoleId(staleTown, TownProtections.BREAK_BLOCK, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.MEMBER_ROLE_ID, liveTown.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        assertEquals(TownProtections.BREAK_BLOCK.getDefaultRoleId(), staleTown.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownProtectionRoleIdPreservesCustomRoleThresholdsForRoleBasedProtections() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        staleTown.addRole("guard", "Guard", Set.of());
        liveTown.addRole("guard", "Guard", Set.of());

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownProtectionRoleId(staleTown, TownProtections.BREAK_BLOCK, "guard"));
        assertEquals("GUARD", liveTown.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownProtectionRoleIdCoercesBinaryProtectionsToRestrictedForNonPublicRoles() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownProtectionRoleId(staleTown, TownProtections.TOWN_FIRE, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.RESTRICTED_ROLE_ID, liveTown.getProtectionRoleId(TownProtections.TOWN_FIRE));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownArchetypeRefreshesTheLiveTownBeforeSaving() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              archetype_change_cooldown_seconds: 86400
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownArchetype(staleTown, TownArchetype.COMMUNIST));
        assertEquals(TownArchetype.COMMUNIST, liveTown.getArchetype());
        assertEquals(TownArchetype.COMMUNIST, staleTown.getArchetype());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownArchetypeHonorsConfiguredCooldown() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              archetype_change_cooldown_seconds: 86400
            """);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownArchetype(staleTown, TownArchetype.COMMUNIST));
        assertFalse(service.setTownArchetype(staleTown, TownArchetype.SOCIALIST));
        assertTrue(service.getRemainingTownArchetypeChangeCooldownMillis(staleTown) > 0L);
        assertEquals(TownArchetype.COMMUNIST, liveTown.getArchetype());
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkProtectionRoleIdRefreshesTheLiveChunkBeforeSaving() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleChunk = new RTownChunk(staleTown, "world", 3, 4, ChunkType.SECURITY);
        staleTown.addChunk(staleChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkProtectionRoleId(staleChunk, TownProtections.BREAK_BLOCK, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.MEMBER_ROLE_ID, liveChunk.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        assertNull(staleChunk.getProtectionRoleId(TownProtections.BREAK_BLOCK));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkProtectionRoleIdCoercesBinaryProtectionsToRestrictedForNonPublicRoles() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleChunk = new RTownChunk(staleTown, "world", 3, 4, ChunkType.SECURITY);
        staleTown.addChunk(staleChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveTown.addChunk(liveChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkProtectionRoleId(staleChunk, TownProtections.TOWN_LAVA, RTown.MAYOR_ROLE_ID));
        assertEquals(RTown.RESTRICTED_ROLE_ID, liveChunk.getProtectionRoleId(TownProtections.TOWN_LAVA));
        verify(townRepository).update(liveTown);
    }

    @Test
    void setChunkProtectionRoleIdRemovesLoadedHostileEntitiesWhenChunkBecomesRestricted() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleChunk = new RTownChunk(staleTown, "world", 3, 4, ChunkType.SECURITY);
        staleChunk.setProtectionRoleId(TownProtections.TOWN_HOSTILE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        staleTown.addChunk(staleChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.SECURITY);
        liveChunk.setProtectionRoleId(TownProtections.TOWN_HOSTILE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        liveTown.addChunk(liveChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(javaPlugin.getServer()).thenReturn(server);
        when(server.getWorld("world")).thenReturn(world);
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(loadedChunk);
        when(loadedChunk.getEntities()).thenReturn(new Entity[]{hostileMonster, passiveAnimal});

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setChunkProtectionRoleId(staleChunk, TownProtections.TOWN_HOSTILE_ENTITIES, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.RESTRICTED_ROLE_ID, liveChunk.getProtectionRoleId(TownProtections.TOWN_HOSTILE_ENTITIES));
        verify(hostileMonster).remove();
        verify(passiveAnimal, never()).remove();
        verify(townRepository).update(liveTown);
    }

    @Test
    void setTownProtectionRoleIdRemovesLoadedPassiveEntitiesOnlyFromAffectedChunks() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown staleTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk staleInheritingChunk = new RTownChunk(staleTown, "world", 1, 2, ChunkType.DEFAULT);
        final RTownChunk stalePublicOverrideChunk = new RTownChunk(staleTown, "world", 5, 6, ChunkType.DEFAULT);
        stalePublicOverrideChunk.setProtectionRoleId(TownProtections.TOWN_PASSIVE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        staleTown.addChunk(staleInheritingChunk);
        staleTown.addChunk(stalePublicOverrideChunk);

        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk liveInheritingChunk = new RTownChunk(liveTown, "world", 1, 2, ChunkType.DEFAULT);
        final RTownChunk livePublicOverrideChunk = new RTownChunk(liveTown, "world", 5, 6, ChunkType.DEFAULT);
        livePublicOverrideChunk.setProtectionRoleId(TownProtections.TOWN_PASSIVE_ENTITIES, RTown.PUBLIC_ROLE_ID);
        liveTown.addChunk(liveInheritingChunk);
        liveTown.addChunk(livePublicOverrideChunk);

        when(townRepository.findByTownUUID(townUuid)).thenReturn(liveTown);
        when(javaPlugin.getServer()).thenReturn(server);
        when(server.getWorld("world")).thenReturn(world);
        when(world.isChunkLoaded(1, 2)).thenReturn(true);
        when(world.getChunkAt(1, 2)).thenReturn(loadedChunk);
        when(loadedChunk.getEntities()).thenReturn(new Entity[]{passiveAnimal, hostileMonster});

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.setTownProtectionRoleId(staleTown, TownProtections.TOWN_PASSIVE_ENTITIES, RTown.MEMBER_ROLE_ID));
        assertEquals(RTown.RESTRICTED_ROLE_ID, liveTown.getProtectionRoleId(TownProtections.TOWN_PASSIVE_ENTITIES));
        verify(passiveAnimal).remove();
        verify(hostileMonster, never()).remove();
        verify(overriddenPassiveAnimal, never()).remove();
        verify(townRepository).update(liveTown);
    }

    @Test
    void isPlayerAllowedUsesSecurityChunkOverrideWhenPresent() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, mayorUuid, "Test Town", null);
        town.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        securityChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MEMBER_ROLE_ID);
        town.addChunk(securityChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MEMBER_ROLE_ID);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(securityChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.isPlayerAllowed(player, location, TownProtections.BREAK_BLOCK));
    }

    @Test
    void isPlayerAllowedUsesNestedItemUseFallbacksFromChunkOverrideWhenSpecificActionIsUnset() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, mayorUuid, "Test Town", null);
        town.setProtectionRoleId(TownProtections.SWITCH_ACCESS, RTown.PUBLIC_ROLE_ID);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        securityChunk.setProtectionRoleId(TownProtections.ITEM_USE, RTown.MAYOR_ROLE_ID);
        town.addChunk(securityChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MEMBER_ROLE_ID);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(securityChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.isPlayerAllowed(player, location, TownProtections.ENDER_PEARL));
    }

    @Test
    void isPlayerAllowedFallsBackToTownProtectionWhenSecurityChunkOverrideIsMissing() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, mayorUuid, "Test Town", null);
        town.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        town.addChunk(securityChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MEMBER_ROLE_ID);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(securityChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.isPlayerAllowed(player, location, TownProtections.BREAK_BLOCK));
    }

    @Test
    void isPlayerAllowedIgnoresNonSecurityChunkOverrides() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown town = new RTown(townUuid, mayorUuid, "Test Town", null);
        town.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        final RTownChunk defaultChunk = new RTownChunk(town, "world", 2, 3, ChunkType.DEFAULT);
        defaultChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.PUBLIC_ROLE_ID);
        town.addChunk(defaultChunk);
        final RDTPlayer playerData = new RDTPlayer(playerUuid, townUuid, RTown.MEMBER_ROLE_ID);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(playerRepository.findByPlayer(playerUuid)).thenReturn(playerData);
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(defaultChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.isPlayerAllowed(player, location, TownProtections.BREAK_BLOCK));
    }

    @Test
    void isWorldActionAllowedUsesSecurityChunkOverrideWhenPresent() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.setProtectionRoleId(TownProtections.TOWN_LAVA, RTown.RESTRICTED_ROLE_ID);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 2, 3, ChunkType.SECURITY);
        securityChunk.setProtectionRoleId(TownProtections.TOWN_LAVA, RTown.PUBLIC_ROLE_ID);
        town.addChunk(securityChunk);

        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(securityChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.isWorldActionAllowed(location, TownProtections.TOWN_LAVA));
    }

    @Test
    void isWorldActionAllowedIgnoresNonSecurityChunkOverrides() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.setProtectionRoleId(TownProtections.TOWN_LAVA, RTown.RESTRICTED_ROLE_ID);
        final RTownChunk defaultChunk = new RTownChunk(town, "world", 2, 3, ChunkType.DEFAULT);
        defaultChunk.setProtectionRoleId(TownProtections.TOWN_LAVA, RTown.PUBLIC_ROLE_ID);
        town.addChunk(defaultChunk);

        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(defaultChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.isWorldActionAllowed(location, TownProtections.TOWN_LAVA));
    }

    @Test
    void isWorldActionAllowedFallsBackToTownBinaryProtectionWhenChunkOverrideIsMissing() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.setProtectionRoleId(TownProtections.TOWN_WATER, RTown.PUBLIC_ROLE_ID);
        final RTownChunk liveChunk = new RTownChunk(town, "world", 2, 3, ChunkType.DEFAULT);
        town.addChunk(liveChunk);

        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(liveChunk);

        final RDT plugin = new RDT(javaPlugin, "test", townService);
        setField(plugin, "townChunkRepository", townChunkRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.isWorldActionAllowed(location, TownProtections.TOWN_WATER));
    }

    @Test
    void isWorldActionAllowedFailsOpenWhenFuelEnabledAndTownHasNoFuel() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.setProtectionRoleId(TownProtections.TOWN_WATER, RTown.RESTRICTED_ROLE_ID);
        final RTownChunk liveChunk = new RTownChunk(town, "world", 2, 3, ChunkType.DEFAULT);
        town.addChunk(liveChunk);

        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(liveChunk);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                fuel:
                  enabled: true
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townChunkRepository", townChunkRepository);
        setField(plugin, "townFuelService", new TownFuelService(plugin));
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.isWorldActionAllowed(location, TownProtections.TOWN_WATER));
    }

    @Test
    void isWorldActionAllowedUsesConfiguredProtectionThresholdWhenTownHasBufferedFuel() throws ReflectiveOperationException {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.setProtectionRoleId(TownProtections.TOWN_WATER, RTown.RESTRICTED_ROLE_ID);
        town.setBufferedFuelUnits(25.0D);
        final RTownChunk liveChunk = new RTownChunk(town, "world", 2, 3, ChunkType.DEFAULT);
        town.addChunk(liveChunk);

        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(location.getBlockX()).thenReturn(32);
        when(location.getBlockZ()).thenReturn(48);
        when(townChunkRepository.findByChunk("world", 2, 3)).thenReturn(liveChunk);

        final RDT plugin = this.createPluginWithConfigs(
            """
                town:
                  archetype_change_cooldown_seconds: 86400
                """,
            """
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """,
            """
                fuel:
                  enabled: true
                levels:
                  "2":
                    requirements: {}
                    rewards: {}
                """
        );
        setField(plugin, "townChunkRepository", townChunkRepository);
        setField(plugin, "townFuelService", new TownFuelService(plugin));
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertFalse(service.isWorldActionAllowed(location, TownProtections.TOWN_WATER));
    }

    @Test
    void getTownRelationshipViewEntriesExcludesTheSourceTownAndSortsTargetsByName() throws ReflectiveOperationException {
        final RTown sourceTown = this.createUnlockedTown("Source");
        final RTown zetaTown = this.createUnlockedTown("Zeta");
        final RTown alphaTown = this.createUnlockedTown("Alpha");

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(sourceTown, zetaTown, alphaTown);
        this.stubTownRelationshipRepository();

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final List<TownRelationshipViewEntry> entries = service.getTownRelationshipViewEntries(sourceTown);

        assertEquals(List.of("Alpha", "Zeta"), entries.stream().map(entry -> entry.targetTown().getTownName()).toList());
        assertTrue(entries.stream().noneMatch(entry -> entry.targetTown().getTownUUID().equals(sourceTown.getTownUUID())));
    }

    @Test
    void neutralToAlliedCreatesPendingRelationshipUntilTheTargetTownMatchesIt() throws ReflectiveOperationException {
        final RTown alphaTown = this.createUnlockedTown("Alpha");
        final RTown betaTown = this.createUnlockedTown("Beta");

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(alphaTown, betaTown);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final TownRelationshipChangeResult pending = service.changeTownRelationship(
            alphaTown,
            betaTown,
            TownRelationshipState.ALLIED
        );

        assertEquals(TownRelationshipChangeStatus.PENDING, pending.status());
        final RTownRelationship storedRelationship = relationships.get(
            RTownRelationship.buildPairKey(alphaTown.getTownUUID(), betaTown.getTownUUID())
        );
        assertNotNull(storedRelationship);
        assertEquals(TownRelationshipState.NEUTRAL, storedRelationship.getConfirmedState());
        assertEquals(TownRelationshipState.ALLIED, storedRelationship.getPendingState());
        assertEquals(alphaTown.getTownUUID(), storedRelationship.getPendingRequesterTownUuid());
        assertEquals(TownRelationshipState.NEUTRAL, pending.relationship().effectiveState());

        final TownRelationshipChangeResult confirmed = service.changeTownRelationship(
            betaTown,
            alphaTown,
            TownRelationshipState.ALLIED
        );

        assertEquals(TownRelationshipChangeStatus.CONFIRMED, confirmed.status());
        assertEquals(TownRelationshipState.ALLIED, storedRelationship.getConfirmedState());
        assertNull(storedRelationship.getPendingState());
        assertNull(storedRelationship.getPendingRequesterTownUuid());
        assertEquals(TownRelationshipState.ALLIED, confirmed.relationship().effectiveState());
        assertTrue(storedRelationship.getCooldownUntilMillis() > System.currentTimeMillis());
    }

    @Test
    void hostileRelationshipAppliesImmediatelyWithoutConfirmation() throws ReflectiveOperationException {
        final RTown alphaTown = this.createUnlockedTown("Alpha");
        final RTown betaTown = this.createUnlockedTown("Beta");

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(alphaTown, betaTown);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final TownRelationshipChangeResult result = service.changeTownRelationship(
            alphaTown,
            betaTown,
            TownRelationshipState.HOSTILE
        );

        final RTownRelationship storedRelationship = relationships.get(
            RTownRelationship.buildPairKey(alphaTown.getTownUUID(), betaTown.getTownUUID())
        );
        assertEquals(TownRelationshipChangeStatus.CONFIRMED, result.status());
        assertNotNull(storedRelationship);
        assertEquals(TownRelationshipState.HOSTILE, storedRelationship.getConfirmedState());
        assertNull(storedRelationship.getPendingState());
        assertEquals(TownRelationshipState.HOSTILE, result.relationship().effectiveState());
    }

    @Test
    void hostileToNeutralRequiresConfirmationAndStartsCooldownAfterItChanges() throws ReflectiveOperationException {
        final RTown alphaTown = this.createUnlockedTown("Alpha");
        final RTown betaTown = this.createUnlockedTown("Beta");

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(alphaTown, betaTown);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();

        final TownRuntimeService service = new TownRuntimeService(plugin);
        service.changeTownRelationship(alphaTown, betaTown, TownRelationshipState.HOSTILE);
        final RTownRelationship storedRelationship = relationships.get(
            RTownRelationship.buildPairKey(alphaTown.getTownUUID(), betaTown.getTownUUID())
        );
        assertNotNull(storedRelationship);
        storedRelationship.setCooldownUntilMillis(0L);

        final TownRelationshipChangeResult pending = service.changeTownRelationship(
            alphaTown,
            betaTown,
            TownRelationshipState.NEUTRAL
        );
        assertEquals(TownRelationshipChangeStatus.PENDING, pending.status());
        assertEquals(TownRelationshipState.HOSTILE, storedRelationship.getConfirmedState());
        assertEquals(TownRelationshipState.NEUTRAL, storedRelationship.getPendingState());

        final TownRelationshipChangeResult confirmed = service.changeTownRelationship(
            betaTown,
            alphaTown,
            TownRelationshipState.NEUTRAL
        );
        assertEquals(TownRelationshipChangeStatus.CONFIRMED, confirmed.status());
        assertEquals(TownRelationshipState.NEUTRAL, storedRelationship.getConfirmedState());
        assertNull(storedRelationship.getPendingState());

        final TownRelationshipChangeResult cooldown = service.changeTownRelationship(
            alphaTown,
            betaTown,
            TownRelationshipState.ALLIED
        );
        assertEquals(TownRelationshipChangeStatus.COOLDOWN, cooldown.status());
    }

    @Test
    void pendingNonHostileRequestsCanBeReplacedBeforeTheTargetTownConfirmsThem() throws ReflectiveOperationException {
        final RTown alphaTown = this.createUnlockedTown("Alpha");
        final RTown betaTown = this.createUnlockedTown("Beta");

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(alphaTown, betaTown);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();

        final TownRuntimeService service = new TownRuntimeService(plugin);
        service.changeTownRelationship(alphaTown, betaTown, TownRelationshipState.HOSTILE);
        final RTownRelationship storedRelationship = relationships.get(
            RTownRelationship.buildPairKey(alphaTown.getTownUUID(), betaTown.getTownUUID())
        );
        assertNotNull(storedRelationship);
        storedRelationship.setCooldownUntilMillis(0L);

        service.changeTownRelationship(alphaTown, betaTown, TownRelationshipState.ALLIED);
        assertEquals(TownRelationshipState.ALLIED, storedRelationship.getPendingState());

        final TownRelationshipChangeResult replaced = service.changeTownRelationship(
            alphaTown,
            betaTown,
            TownRelationshipState.NEUTRAL
        );

        assertEquals(TownRelationshipChangeStatus.PENDING, replaced.status());
        assertEquals(TownRelationshipState.HOSTILE, storedRelationship.getConfirmedState());
        assertEquals(TownRelationshipState.NEUTRAL, storedRelationship.getPendingState());
        assertEquals(alphaTown.getTownUUID(), storedRelationship.getPendingRequesterTownUuid());
    }

    @Test
    void relationshipViewForcesNeutralWhenEitherTownHasNotUnlockedDiplomacy() throws ReflectiveOperationException {
        final RTown alphaTown = this.createUnlockedTown("Alpha");
        final RTown betaTown = this.createUnlockedTown("Beta");
        betaTown.setNexusLevel(4);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(alphaTown, betaTown);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();
        final RTownRelationship storedRelationship = new RTownRelationship(alphaTown.getTownUUID(), betaTown.getTownUUID());
        storedRelationship.setConfirmedState(TownRelationshipState.ALLIED);
        setEntityId(storedRelationship, 1L);
        relationships.put(storedRelationship.getPairKey(), storedRelationship);

        final TownRuntimeService service = new TownRuntimeService(plugin);
        final TownRelationshipViewEntry entry = service.getTownRelationshipViewEntry(alphaTown, betaTown);

        assertEquals(TownRelationshipState.ALLIED, entry.confirmedState());
        assertEquals(TownRelationshipState.NEUTRAL, entry.effectiveState());
        assertTrue(entry.lockedByLevel());
    }

    @Test
    void alliedTownMembersCanPassRoleBasedChecksOnlyWhenAlliedAccessIsAllowed() throws ReflectiveOperationException {
        final UUID alliedPlayerUuid = UUID.randomUUID();
        final RTown ownerTown = this.createUnlockedTown("Owner");
        final RTown alliedTown = this.createUnlockedTown("Allies");
        final RTownChunk securityChunk = new RTownChunk(ownerTown, "world", 2, 3, ChunkType.SECURITY);
        ownerTown.addChunk(securityChunk);
        ownerTown.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.MAYOR_ROLE_ID);
        ownerTown.setAlliedProtectionAllowed(TownProtections.BREAK_BLOCK, true);
        final RDTPlayer alliedPlayerData = new RDTPlayer(alliedPlayerUuid, alliedTown.getTownUUID(), RTown.MEMBER_ROLE_ID);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        setField(plugin, "townRelationshipRepository", townRelationshipRepository);
        this.stubTownRepository(ownerTown, alliedTown);
        final Map<String, RTownRelationship> relationships = this.stubTownRelationshipRepository();
        final RTownRelationship storedRelationship = new RTownRelationship(ownerTown.getTownUUID(), alliedTown.getTownUUID());
        storedRelationship.setConfirmedState(TownRelationshipState.ALLIED);
        setEntityId(storedRelationship, 1L);
        relationships.put(storedRelationship.getPairKey(), storedRelationship);
        when(player.getUniqueId()).thenReturn(alliedPlayerUuid);
        when(playerRepository.findByPlayer(alliedPlayerUuid)).thenReturn(alliedPlayerData);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.isPlayerAllowed(player, securityChunk, TownProtections.BREAK_BLOCK));

        ownerTown.setAlliedProtectionAllowed(TownProtections.BREAK_BLOCK, false);
        assertFalse(service.isPlayerAllowed(player, securityChunk, TownProtections.BREAK_BLOCK));
    }

    @Test
    void publicRoleChecksStillPassEvenWhenAlliedAccessIsRestricted() throws ReflectiveOperationException {
        final RTown ownerTown = this.createUnlockedTown("Owner");
        final RTownChunk securityChunk = new RTownChunk(ownerTown, "world", 2, 3, ChunkType.SECURITY);
        ownerTown.addChunk(securityChunk);
        ownerTown.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.PUBLIC_ROLE_ID);
        ownerTown.setAlliedProtectionAllowed(TownProtections.BREAK_BLOCK, false);

        final RDT plugin = this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """);
        setField(plugin, "townRepository", townRepository);
        this.stubTownRepository(ownerTown);

        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertTrue(service.isPlayerAllowed(player, securityChunk, TownProtections.BREAK_BLOCK));
    }

    @Test
    void fobChunksApplyChunkScopedProtectionRoleOverrides() {
        final RTown ownerTown = this.createUnlockedTown("Owner");
        final RTownChunk fobChunk = new RTownChunk(ownerTown, "world", 2, 3, ChunkType.FOB);
        ownerTown.addChunk(fobChunk);
        ownerTown.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.RESTRICTED_ROLE_ID);
        fobChunk.setProtectionRoleId(TownProtections.BREAK_BLOCK, RTown.PUBLIC_ROLE_ID);

        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """));

        assertTrue(service.isPlayerAllowed(player, fobChunk, TownProtections.BREAK_BLOCK));
    }

    @Test
    void fobChunksApplyChunkScopedAlliedOverrides() {
        final RTown ownerTown = this.createUnlockedTown("Owner");
        final RTownChunk fobChunk = new RTownChunk(ownerTown, "world", 2, 3, ChunkType.FOB);
        ownerTown.addChunk(fobChunk);
        ownerTown.setAlliedProtectionAllowed(TownProtections.BREAK_BLOCK, false);
        fobChunk.setAlliedProtectionAllowed(TownProtections.BREAK_BLOCK, true);

        final TownRuntimeService service = new TownRuntimeService(this.createPluginWithConfig("""
            town:
              relationship_unlock_level: 5
              relationship_change_cooldown_seconds: 60
            """));

        assertTrue(service.isAlliedProtectionAllowed(ownerTown, fobChunk, TownProtections.BREAK_BLOCK));
    }

    private RDT createPluginWithConfig(final String yaml) {
        return this.createPluginWithConfigs(yaml, "", "", "", "", "", "", "", "");
    }

    private RDT createPluginWithNationConfig(
        final String configYaml,
        final String nationYaml
    ) {
        return this.createPluginWithConfigs(configYaml, "", "", "", "", "", "", "", nationYaml);
    }

    private RDT createPluginWithConfigs(
        final String configYaml,
        final String nexusYaml,
        final String securityYaml
    ) {
        return this.createPluginWithConfigs(configYaml, nexusYaml, securityYaml, "", "", "", "", "", "");
    }

    private RDT createPluginWithConfigs(
        final String configYaml,
        final String nexusYaml,
        final String securityYaml,
        final String bankYaml,
        final String farmYaml,
        final String outpostYaml
    ) {
        return this.createPluginWithConfigs(
            configYaml,
            nexusYaml,
            securityYaml,
            bankYaml,
            farmYaml,
            outpostYaml,
            "",
            "",
            ""
        );
    }

    private RDT createPluginWithConfigs(
        final String configYaml,
        final String nexusYaml,
        final String securityYaml,
        final String bankYaml,
        final String farmYaml,
        final String outpostYaml,
        final String medicYaml,
        final String armoryYaml
    ) {
        return this.createPluginWithConfigs(
            configYaml,
            nexusYaml,
            securityYaml,
            bankYaml,
            farmYaml,
            outpostYaml,
            medicYaml,
            armoryYaml,
            ""
        );
    }

    private RDT createPluginWithConfigs(
        final String configYaml,
        final String nexusYaml,
        final String securityYaml,
        final String bankYaml,
        final String farmYaml,
        final String outpostYaml,
        final String medicYaml,
        final String armoryYaml,
        final String nationYaml
    ) {
        final ConfigSection config = ConfigSection.fromInputStream(
            new ByteArrayInputStream(configYaml.getBytes(StandardCharsets.UTF_8))
        );
        final NexusConfigSection nexusConfig = NexusConfigSection.fromInputStream(
            new ByteArrayInputStream(nexusYaml.getBytes(StandardCharsets.UTF_8))
        );
        final NationConfigSection nationConfig = NationConfigSection.fromInputStream(
            new ByteArrayInputStream(nationYaml.getBytes(StandardCharsets.UTF_8))
        );
        final SecurityConfigSection securityConfig = SecurityConfigSection.fromInputStream(
            new ByteArrayInputStream(securityYaml.getBytes(StandardCharsets.UTF_8))
        );
        final BankConfigSection bankConfig = BankConfigSection.fromInputStream(
            new ByteArrayInputStream(bankYaml.getBytes(StandardCharsets.UTF_8))
        );
        final FarmConfigSection farmConfig = FarmConfigSection.fromInputStream(
            new ByteArrayInputStream(farmYaml.getBytes(StandardCharsets.UTF_8))
        );
        final OutpostConfigSection outpostConfig = OutpostConfigSection.fromInputStream(
            new ByteArrayInputStream(outpostYaml.getBytes(StandardCharsets.UTF_8))
        );
        final MedicConfigSection medicConfig = MedicConfigSection.fromInputStream(
            new ByteArrayInputStream(medicYaml.getBytes(StandardCharsets.UTF_8))
        );
        final ArmoryConfigSection armoryConfig = ArmoryConfigSection.fromInputStream(
            new ByteArrayInputStream(armoryYaml.getBytes(StandardCharsets.UTF_8))
        );
        final FobConfigSection fobConfig = FobConfigSection.createDefault();
        return new RDT(this.javaPlugin, "test", this.townService) {
            @Override
            public ConfigSection getDefaultConfig() {
                return config;
            }

            @Override
            public NexusConfigSection getNexusConfig() {
                return nexusConfig;
            }

            @Override
            public NationConfigSection getNationConfig() {
                return nationConfig;
            }

            @Override
            public SecurityConfigSection getSecurityConfig() {
                return securityConfig;
            }

            @Override
            public BankConfigSection getBankConfig() {
                return bankConfig;
            }

            @Override
            public FarmConfigSection getFarmConfig() {
                return farmConfig;
            }

            @Override
            public OutpostConfigSection getOutpostConfig() {
                return outpostConfig;
            }

            @Override
            public MedicConfigSection getMedicConfig() {
                return medicConfig;
            }

            @Override
            public ArmoryConfigSection getArmoryConfig() {
                return armoryConfig;
            }

            @Override
            public FobConfigSection getFobConfig() {
                return fobConfig;
            }
        };
    }

    private static RTown createTownWithClaim(final String worldName, final int chunkX, final int chunkZ) {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.addChunk(new RTownChunk(town, worldName, chunkX, chunkZ, ChunkType.DEFAULT));
        return town;
    }

    private RTown createUnlockedTown(final String townName) {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), townName, null);
        town.setNexusLevel(5);
        return town;
    }

    private void stubPlayerRepository(final RDTPlayer... players) {
        final Map<UUID, RDTPlayer> playersById = new HashMap<>();
        for (final RDTPlayer playerData : players) {
            playersById.put(playerData.getIdentifier(), playerData);
        }
        org.mockito.Mockito.lenient()
            .when(playerRepository.findByPlayer(any(UUID.class)))
            .thenAnswer(invocation -> playersById.get(invocation.getArgument(0)));
    }

    private void stubTownRepository(final RTown... towns) {
        final Map<UUID, RTown> townsById = new HashMap<>();
        for (final RTown town : towns) {
            townsById.put(town.getTownUUID(), town);
        }
        org.mockito.Mockito.lenient()
            .when(townRepository.findByTownUUID(any(UUID.class)))
            .thenAnswer(invocation -> townsById.get(invocation.getArgument(0)));
        org.mockito.Mockito.lenient().when(townRepository.findAll()).thenReturn(List.of(towns));
    }

    private Map<String, RTownRelationship> stubTownRelationshipRepository() {
        final Map<String, RTownRelationship> relationshipsByPair = new HashMap<>();
        org.mockito.Mockito.lenient()
            .when(townRelationshipRepository.findByTownPair(any(UUID.class), any(UUID.class)))
            .thenAnswer(invocation -> relationshipsByPair.get(
                RTownRelationship.buildPairKey(invocation.getArgument(0), invocation.getArgument(1))
            ));
        org.mockito.Mockito.lenient()
            .when(townRelationshipRepository.findByTownUuid(any(UUID.class)))
            .thenAnswer(invocation -> relationshipsByPair.values()
                .stream()
                .filter(relationship -> relationship.containsTown(invocation.getArgument(0)))
                .toList());
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            final RTownRelationship relationship = invocation.getArgument(0);
            this.persistRelationshipForTest(relationshipsByPair, relationship);
            return null;
        }).when(townRelationshipRepository).create(any(RTownRelationship.class));
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            final RTownRelationship relationship = invocation.getArgument(0);
            this.persistRelationshipForTest(relationshipsByPair, relationship);
            return null;
        }).when(townRelationshipRepository).update(any(RTownRelationship.class));
        return relationshipsByPair;
    }

    private Map<UUID, RNation> stubNationRepository() {
        final Map<UUID, RNation> nationsById = new HashMap<>();
        org.mockito.Mockito.lenient()
            .when(nationRepository.findByNationUuid(any(UUID.class)))
            .thenAnswer(invocation -> nationsById.get(invocation.getArgument(0)));
        org.mockito.Mockito.lenient()
            .when(nationRepository.findByNationName(any(String.class)))
            .thenAnswer(invocation -> {
                final String lookup = String.valueOf(invocation.getArgument(0, String.class)).trim();
                return nationsById.values().stream()
                    .filter(nation -> nation.getNationName().equalsIgnoreCase(lookup))
                    .findFirst()
                    .orElse(null);
            });
        org.mockito.Mockito.lenient()
            .when(nationRepository.findPendingByInitiatingTownUuid(any(UUID.class)))
            .thenAnswer(invocation -> nationsById.values().stream()
                .filter(RNation::isPending)
                .filter(nation -> nation.getInitiatingTownUuid().equals(invocation.getArgument(0)))
                .toList());
        org.mockito.Mockito.lenient().when(nationRepository.findAll()).thenAnswer(invocation -> List.copyOf(nationsById.values()));
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            final RNation nation = invocation.getArgument(0);
            if (nation.getId() == null) {
                setEntityId(nation, nationsById.size() + 1L);
            }
            nationsById.put(nation.getNationUuid(), nation);
            return null;
        }).when(nationRepository).create(any(RNation.class));
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            final RNation nation = invocation.getArgument(0);
            if (nation.getId() == null) {
                setEntityId(nation, nationsById.size() + 1L);
            }
            nationsById.put(nation.getNationUuid(), nation);
            return null;
        }).when(nationRepository).update(any(RNation.class));
        return nationsById;
    }

    private Map<Long, NationInvite> stubNationInviteRepository() {
        final Map<Long, NationInvite> invitesById = new HashMap<>();
        org.mockito.Mockito.lenient().when(nationInviteRepository.findAll()).thenAnswer(invocation -> List.copyOf(invitesById.values()));
        org.mockito.Mockito.lenient()
            .when(nationInviteRepository.findByNationUuid(any(UUID.class)))
            .thenAnswer(invocation -> invitesById.values().stream()
                .filter(invite -> invite.getNationUuid().equals(invocation.getArgument(0)))
                .toList());
        org.mockito.Mockito.lenient()
            .when(nationInviteRepository.findByTargetTownUuid(any(UUID.class)))
            .thenAnswer(invocation -> invitesById.values().stream()
                .filter(invite -> invite.getTargetTownUuid().equals(invocation.getArgument(0)))
                .toList());
        org.mockito.Mockito.lenient()
            .when(nationInviteRepository.findPendingByTargetTownUuid(any(UUID.class)))
            .thenAnswer(invocation -> invitesById.values().stream()
                .filter(NationInvite::isPending)
                .filter(invite -> invite.getTargetTownUuid().equals(invocation.getArgument(0)))
                .toList());
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            final NationInvite invite = invocation.getArgument(0);
            if (invite.getId() == null) {
                setEntityId(invite, invitesById.size() + 1L);
            }
            invitesById.put(invite.getId(), invite);
            return null;
        }).when(nationInviteRepository).create(any(NationInvite.class));
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            final NationInvite invite = invocation.getArgument(0);
            if (invite.getId() == null) {
                setEntityId(invite, invitesById.size() + 1L);
            }
            invitesById.put(invite.getId(), invite);
            return null;
        }).when(nationInviteRepository).update(any(NationInvite.class));
        return invitesById;
    }

    private void persistRelationshipForTest(
        final Map<String, RTownRelationship> relationshipsByPair,
        final RTownRelationship relationship
    ) {
        if (relationship.getId() == null) {
            try {
                setEntityId(relationship, relationshipsByPair.size() + 1L);
            } catch (final ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        }
        relationshipsByPair.put(relationship.getPairKey(), relationship);
    }

    private ItemStack mockItemStack(final Material material, final int initialAmount) {
        final ItemStack itemStack = org.mockito.Mockito.mock(ItemStack.class);
        final AtomicInteger amount = new AtomicInteger(initialAmount);

        org.mockito.Mockito.lenient().when(itemStack.getType()).thenReturn(material);
        org.mockito.Mockito.lenient().when(itemStack.getAmount()).thenAnswer(invocation -> amount.get());
        org.mockito.Mockito.lenient().when(itemStack.isEmpty()).thenAnswer(invocation -> amount.get() <= 0);
        org.mockito.Mockito.lenient().when(itemStack.clone()).thenReturn(itemStack);
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            amount.set(invocation.getArgument(0, Integer.class));
            return null;
        }).when(itemStack).setAmount(org.mockito.ArgumentMatchers.anyInt());
        return itemStack;
    }

    private static void setField(final RDT target, final String fieldName, final Object value) throws ReflectiveOperationException {
        final Field field = RDT.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
}
