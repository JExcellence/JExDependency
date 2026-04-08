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
import com.raindropcentral.rdt.configs.BankConfigSection;
import com.raindropcentral.rdt.configs.ConfigSection;
import com.raindropcentral.rdt.configs.FarmConfigSection;
import com.raindropcentral.rdt.configs.NexusConfigSection;
import com.raindropcentral.rdt.configs.OutpostConfigSection;
import com.raindropcentral.rdt.configs.SecurityConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.database.repository.RRTownChunk;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownArchetype;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rdt.utils.TownProtections;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import com.raindropcentral.rplatform.reward.RewardService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
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
    void getChunkLevelProgressUsesTypeSpecificConfigsForBankFarmAndOutpost() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk bankChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.BANK);
        final RTownChunk farmChunk = new RTownChunk(liveTown, "world", 5, 6, ChunkType.FARM);
        final RTownChunk outpostChunk = new RTownChunk(liveTown, "world", 7, 8, ChunkType.OUTPOST);
        liveTown.addChunk(bankChunk);
        liveTown.addChunk(farmChunk);
        liveTown.addChunk(outpostChunk);

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
                """
        );
        setField(plugin, "townRepository", townRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        final LevelProgressSnapshot bankSnapshot = service.getChunkLevelProgress(player, bankChunk);
        final LevelProgressSnapshot farmSnapshot = service.getChunkLevelProgress(player, farmChunk);
        final LevelProgressSnapshot outpostSnapshot = service.getChunkLevelProgress(player, outpostChunk);

        assertEquals(LevelScope.BANK, bankSnapshot.scope());
        assertEquals(3, bankSnapshot.maxLevel());
        assertEquals(3, bankSnapshot.displayLevel());
        assertEquals(LevelScope.FARM, farmSnapshot.scope());
        assertEquals(4, farmSnapshot.maxLevel());
        assertEquals(4, farmSnapshot.displayLevel());
        assertEquals(LevelScope.OUTPOST, outpostSnapshot.scope());
        assertEquals(5, outpostSnapshot.maxLevel());
        assertEquals(5, outpostSnapshot.displayLevel());
    }

    @Test
    void levelUpChunkSupportsBankFarmAndOutpostAndBankUnlocksRemoteAccess() throws ReflectiveOperationException {
        final UUID townUuid = UUID.randomUUID();
        final UUID mayorUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final RTown liveTown = new RTown(townUuid, mayorUuid, "Test Town", null);
        final RTownChunk bankChunk = new RTownChunk(liveTown, "world", 3, 4, ChunkType.BANK);
        final RTownChunk farmChunk = new RTownChunk(liveTown, "world", 5, 6, ChunkType.FARM);
        final RTownChunk outpostChunk = new RTownChunk(liveTown, "world", 7, 8, ChunkType.OUTPOST);
        liveTown.addChunk(bankChunk);
        liveTown.addChunk(farmChunk);
        liveTown.addChunk(outpostChunk);
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
                """
        );
        setField(plugin, "townRepository", townRepository);
        setField(plugin, "playerRepository", playerRepository);
        final TownRuntimeService service = new TownRuntimeService(plugin);

        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, bankChunk).status());
        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, farmChunk).status());
        assertEquals(LevelUpStatus.SUCCESS, service.levelUpChunk(player, outpostChunk).status());
        assertEquals(2, bankChunk.getChunkLevel());
        assertEquals(2, farmChunk.getChunkLevel());
        assertEquals(2, outpostChunk.getChunkLevel());
        assertTrue(liveTown.supportsRemoteBankAccess());
        verify(townRepository, times(3)).update(liveTown);
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

    private RDT createPluginWithConfig(final String yaml) {
        return this.createPluginWithConfigs(yaml, "", "", "", "", "");
    }

    private RDT createPluginWithConfigs(
        final String configYaml,
        final String nexusYaml,
        final String securityYaml
    ) {
        return this.createPluginWithConfigs(configYaml, nexusYaml, securityYaml, "", "", "");
    }

    private RDT createPluginWithConfigs(
        final String configYaml,
        final String nexusYaml,
        final String securityYaml,
        final String bankYaml,
        final String farmYaml,
        final String outpostYaml
    ) {
        final ConfigSection config = ConfigSection.fromInputStream(
            new ByteArrayInputStream(configYaml.getBytes(StandardCharsets.UTF_8))
        );
        final NexusConfigSection nexusConfig = NexusConfigSection.fromInputStream(
            new ByteArrayInputStream(nexusYaml.getBytes(StandardCharsets.UTF_8))
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
        };
    }

    private static RTown createTownWithClaim(final String worldName, final int chunkX, final int chunkZ) {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Test Town", null);
        town.addChunk(new RTownChunk(town, worldName, chunkX, chunkZ, ChunkType.DEFAULT));
        return town;
    }

    private static void setField(final RDT target, final String fieldName, final Object value) throws ReflectiveOperationException {
        final Field field = RDT.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
