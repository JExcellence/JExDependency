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

package com.raindropcentral.rplatform.requirement.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link RdtPluginIntegrationBridge}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class RdtPluginIntegrationBridgeTest {

    private static Field bukkitServerField;
    private static @Nullable Server originalServer;

    private PluginManager pluginManager;

    @BeforeAll
    static void captureBukkitServer() throws ReflectiveOperationException {
        bukkitServerField = Bukkit.class.getDeclaredField("server");
        bukkitServerField.setAccessible(true);
        originalServer = (Server) bukkitServerField.get(null);
    }

    @AfterAll
    static void restoreBukkitServer() throws ReflectiveOperationException {
        bukkitServerField.set(null, originalServer);
    }

    @BeforeEach
    void setUpBukkitServer() throws ReflectiveOperationException {
        this.pluginManager = mock(PluginManager.class);

        final Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(this.pluginManager);
        when(server.getLogger()).thenReturn(Logger.getLogger(RdtPluginIntegrationBridgeTest.class.getName()));

        bukkitServerField.set(null, server);
    }

    @Test
    void isAvailableReturnsFalseWhenRdtIsMissing() {
        final RdtPluginIntegrationBridge bridge = new RdtPluginIntegrationBridge();
        when(this.pluginManager.getPlugin("RDT")).thenReturn(null);

        assertFalse(bridge.isAvailable());
        assertEquals(0.0D, bridge.getValue(mock(Player.class), "town_level"));
    }

    @Test
    void getValueResolvesTownAndNexusLevels() {
        final UUID playerUuid = UUID.randomUUID();
        final FakeTown ownTown = new FakeTown(UUID.randomUUID(), "Alpha", 10, 4);
        final FakeTownChunk securityChunk = ownTown.addChunk("world", 0, 0, FakeChunkType.SECURITY, 3);

        final FakePlayerRepository playerRepository = new FakePlayerRepository(Map.of(playerUuid, new FakePlayerRecord(ownTown.getTownUUID())));
        final FakeTownRepository townRepository = new FakeTownRepository(Map.of(ownTown.getTownUUID(), ownTown));
        final FakeTownChunkRepository townChunkRepository = new FakeTownChunkRepository(Map.of(chunkKey("world", 0, 0), securityChunk));

        final Player player = this.mockPlayer(playerUuid, "world", 0, 0);
        final Plugin plugin = this.mockRdtPlugin(playerRepository, townRepository, townChunkRepository);
        final RdtPluginIntegrationBridge bridge = new RdtPluginIntegrationBridge();
        this.mockAvailableRdt(plugin);

        assertEquals(10.0D, bridge.getValue(player, "town_level"));
        assertEquals(4.0D, bridge.getValue(player, "nexus_level"));
    }

    @Test
    void getValueResolvesStandingChunkLevelOnlyForOwnTown() {
        final UUID playerUuid = UUID.randomUUID();
        final FakeTown ownTown = new FakeTown(UUID.randomUUID(), "Alpha", 8, 3);
        ownTown.addChunk("world", 2, 1, FakeChunkType.FARM, 5);

        final FakeTown foreignTown = new FakeTown(UUID.randomUUID(), "Beta", 7, 2);
        final FakeTownChunk foreignChunk = foreignTown.addChunk("world", 3, 1, FakeChunkType.BANK, 6);

        final FakePlayerRepository playerRepository = new FakePlayerRepository(Map.of(playerUuid, new FakePlayerRecord(ownTown.getTownUUID())));
        final FakeTownRepository townRepository = new FakeTownRepository(Map.of(
            ownTown.getTownUUID(), ownTown,
            foreignTown.getTownUUID(), foreignTown
        ));

        final Map<String, FakeTownChunk> chunksByLocation = new LinkedHashMap<>();
        chunksByLocation.put(chunkKey("world", 2, 1), ownTown.findChunk("world", 2, 1));
        chunksByLocation.put(chunkKey("world", 3, 1), foreignChunk);
        final FakeTownChunkRepository townChunkRepository = new FakeTownChunkRepository(chunksByLocation);
        final Plugin plugin = this.mockRdtPlugin(playerRepository, townRepository, townChunkRepository);
        final RdtPluginIntegrationBridge bridge = new RdtPluginIntegrationBridge();
        this.mockAvailableRdt(plugin);

        assertEquals(5.0D, bridge.getValue(this.mockPlayer(playerUuid, "world", 2, 1), "chunk_level"));
        assertEquals(0.0D, bridge.getValue(this.mockPlayer(playerUuid, "world", 3, 1), "chunk_level"));
    }

    @Test
    void getValueResolvesHighestOwnedTypedChunkLevels() {
        final UUID playerUuid = UUID.randomUUID();
        final FakeTown ownTown = new FakeTown(UUID.randomUUID(), "Alpha", 15, 5);
        ownTown.addChunk("world", 0, 0, FakeChunkType.SECURITY, 2);
        ownTown.addChunk("world", 1, 0, FakeChunkType.SECURITY, 4);
        ownTown.addChunk("world", 2, 0, FakeChunkType.BANK, 3);
        ownTown.addChunk("world", 3, 0, FakeChunkType.ARMORY, 6);

        final FakePlayerRepository playerRepository = new FakePlayerRepository(Map.of(playerUuid, new FakePlayerRecord(ownTown.getTownUUID())));
        final FakeTownRepository townRepository = new FakeTownRepository(Map.of(ownTown.getTownUUID(), ownTown));
        final FakeTownChunkRepository townChunkRepository = new FakeTownChunkRepository(Map.of());
        final Player player = this.mockPlayer(playerUuid, "world", 10, 10);
        final Plugin plugin = this.mockRdtPlugin(playerRepository, townRepository, townChunkRepository);
        final RdtPluginIntegrationBridge bridge = new RdtPluginIntegrationBridge();
        this.mockAvailableRdt(plugin);

        assertEquals(4.0D, bridge.getValue(player, "security_chunk_level"));
        assertEquals(3.0D, bridge.getValue(player, "bank_chunk_level"));
        assertEquals(6.0D, bridge.getValue(player, "armory_chunk_level"));
        assertEquals(0.0D, bridge.getValue(player, "medic_chunk_level"));
    }

    @Test
    void getValueReturnsZeroForUnknownKeys() {
        final UUID playerUuid = UUID.randomUUID();
        final FakeTown ownTown = new FakeTown(UUID.randomUUID(), "Alpha", 9, 3);

        final FakePlayerRepository playerRepository = new FakePlayerRepository(Map.of(playerUuid, new FakePlayerRecord(ownTown.getTownUUID())));
        final FakeTownRepository townRepository = new FakeTownRepository(Map.of(ownTown.getTownUUID(), ownTown));
        final FakeTownChunkRepository townChunkRepository = new FakeTownChunkRepository(Map.of());
        final Player player = this.mockPlayer(playerUuid, "world", 0, 0);
        final Plugin plugin = this.mockRdtPlugin(playerRepository, townRepository, townChunkRepository);
        final RdtPluginIntegrationBridge bridge = new RdtPluginIntegrationBridge();
        this.mockAvailableRdt(plugin);

        assertEquals(0.0D, bridge.getValue(player, "unknown_requirement"));
    }

    private void mockAvailableRdt(final @NotNull Plugin plugin) {
        when(this.pluginManager.getPlugin("RDT")).thenReturn(plugin);
    }

    private @NotNull Plugin mockRdtPlugin(
        final @NotNull FakePlayerRepository playerRepository,
        final @NotNull FakeTownRepository townRepository,
        final @NotNull FakeTownChunkRepository townChunkRepository
    ) {
        final TestRdtPlugin plugin = mock(TestRdtPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getPlayerRepository()).thenReturn(playerRepository);
        when(plugin.getTownRepository()).thenReturn(townRepository);
        when(plugin.getTownChunkRepository()).thenReturn(townChunkRepository);
        return plugin;
    }

    private @NotNull Player mockPlayer(final @NotNull UUID playerUuid, final @NotNull String worldName, final int chunkX, final int chunkZ) {
        final Player player = mock(Player.class);
        final Location location = mock(Location.class);
        final World world = mock(World.class);
        final Chunk chunk = mock(Chunk.class);

        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        when(world.getName()).thenReturn(worldName);
        when(chunk.getX()).thenReturn(chunkX);
        when(chunk.getZ()).thenReturn(chunkZ);
        return player;
    }

    private static @NotNull String chunkKey(final @NotNull String worldName, final int chunkX, final int chunkZ) {
        return worldName + ':' + chunkX + ':' + chunkZ;
    }

    private static final class FakePlayerRepository {

        private final Map<UUID, FakePlayerRecord> records;

        private FakePlayerRepository(final @NotNull Map<UUID, FakePlayerRecord> records) {
            this.records = records;
        }

        public @Nullable FakePlayerRecord findByPlayer(final @NotNull UUID playerUuid) {
            return this.records.get(playerUuid);
        }

        public @Nullable FakePlayerRecord findByIdentifier(final @NotNull UUID playerUuid) {
            return this.records.get(playerUuid);
        }
    }

    private static final class FakeTownRepository {

        private final Map<UUID, FakeTown> towns;

        private FakeTownRepository(final @NotNull Map<UUID, FakeTown> towns) {
            this.towns = towns;
        }

        public @Nullable FakeTown findByTownUUID(final @NotNull UUID townUuid) {
            return this.towns.get(townUuid);
        }

        public @Nullable FakeTown findByTownUuid(final @NotNull UUID townUuid) {
            return this.towns.get(townUuid);
        }

        public @Nullable FakeTown findByIdentifier(final @NotNull UUID townUuid) {
            return this.towns.get(townUuid);
        }

        public @Nullable FakeTown findById(final @NotNull UUID townUuid) {
            return this.towns.get(townUuid);
        }
    }

    private static final class FakeTownChunkRepository {

        private final Map<String, FakeTownChunk> chunksByLocation;

        private FakeTownChunkRepository(final @NotNull Map<String, FakeTownChunk> chunksByLocation) {
            this.chunksByLocation = chunksByLocation;
        }

        public @Nullable FakeTownChunk findByChunk(final @NotNull String worldName, final int chunkX, final int chunkZ) {
            return this.chunksByLocation.get(chunkKey(worldName, chunkX, chunkZ));
        }
    }

    private static final class FakePlayerRecord {

        private final UUID townUuid;

        private FakePlayerRecord(final @Nullable UUID townUuid) {
            this.townUuid = townUuid;
        }

        public @Nullable UUID getTownUUID() {
            return this.townUuid;
        }

        public @Nullable UUID getTownUuid() {
            return this.townUuid;
        }
    }

    private static final class FakeTown {

        private final UUID townUuid;
        private final String townName;
        private final int townLevel;
        private final int nexusLevel;
        private final Map<String, FakeTownChunk> chunksByLocation = new LinkedHashMap<>();

        private FakeTown(
            final @NotNull UUID townUuid,
            final @NotNull String townName,
            final int townLevel,
            final int nexusLevel
        ) {
            this.townUuid = townUuid;
            this.townName = townName;
            this.townLevel = townLevel;
            this.nexusLevel = nexusLevel;
        }

        public @NotNull UUID getTownUUID() {
            return this.townUuid;
        }

        public @NotNull UUID getTownUuid() {
            return this.townUuid;
        }

        public @NotNull String getTownName() {
            return this.townName;
        }

        public int getTownLevel() {
            return this.townLevel;
        }

        public int getNexusLevel() {
            return this.nexusLevel;
        }

        public @NotNull List<FakeTownChunk> getChunks() {
            return List.copyOf(this.chunksByLocation.values());
        }

        public @Nullable FakeTownChunk findChunk(final @NotNull String worldName, final int chunkX, final int chunkZ) {
            return this.chunksByLocation.get(chunkKey(worldName, chunkX, chunkZ));
        }

        private @NotNull FakeTownChunk addChunk(
            final @NotNull String worldName,
            final int chunkX,
            final int chunkZ,
            final @NotNull FakeChunkType chunkType,
            final int chunkLevel
        ) {
            final FakeTownChunk chunk = new FakeTownChunk(this, worldName, chunkX, chunkZ, chunkType, chunkLevel);
            this.chunksByLocation.put(chunkKey(worldName, chunkX, chunkZ), chunk);
            return chunk;
        }
    }

    private static final class FakeTownChunk {

        private final FakeTown town;
        private final FakeChunkType chunkType;
        private final int chunkLevel;

        private FakeTownChunk(
            final @NotNull FakeTown town,
            final @NotNull String worldName,
            final int xLoc,
            final int zLoc,
            final @NotNull FakeChunkType chunkType,
            final int chunkLevel
        ) {
            this.town = town;
            this.chunkType = chunkType;
            this.chunkLevel = chunkLevel;
        }

        public @NotNull FakeTown getTown() {
            return this.town;
        }

        public @NotNull FakeChunkType getChunkType() {
            return this.chunkType;
        }

        public int getChunkLevel() {
            return this.chunkLevel;
        }
    }

    private enum FakeChunkType {
        SECURITY,
        BANK,
        FARM,
        OUTPOST,
        MEDIC,
        ARMORY
    }
}
