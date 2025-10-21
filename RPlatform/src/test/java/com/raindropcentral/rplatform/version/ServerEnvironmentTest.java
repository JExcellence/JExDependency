package com.raindropcentral.rplatform.version;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.LegacyServerMock;
import org.bukkit.craftbukkit.v1_20_R3.VersionedServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

class ServerEnvironmentTest {

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        bukkitMock = Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getVersion).thenReturn("git-Paper-1.20.1-25");
        bukkitMock.when(Bukkit::getServer).thenReturn(new VersionedServerMock());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
        resetSingleton();
    }

    @Test
    void detectsFoliaServerType() {
        try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class, Mockito.CALLS_REAL_METHODS)) {
            classMock.when(() -> Class.forName("io.papermc.paper.threadedregions.RegionizedServer"))
                    .thenReturn((Class) Object.class);

            ServerEnvironment environment = ServerEnvironment.getInstance();

            assertEquals(ServerEnvironment.ServerType.FOLIA, environment.getServerType());
            assertTrue(environment.isFolia());
            assertFalse(environment.isPaper());
            assertFalse(environment.isSpigot());
        }
    }

    @Test
    void detectsPaperServerType() {
        try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class, Mockito.CALLS_REAL_METHODS)) {
            classMock.when(() -> Class.forName("io.papermc.paper.threadedregions.RegionizedServer"))
                    .thenThrow(new ClassNotFoundException("folia unavailable"));
            classMock.when(() -> Class.forName("com.destroystokyo.paper.ParticleBuilder"))
                    .thenReturn((Class) Object.class);
            classMock.when(() -> Class.forName("org.purpurmc.purpur.PurpurConfig"))
                    .thenThrow(new ClassNotFoundException("purpur unavailable"));

            ServerEnvironment environment = ServerEnvironment.getInstance();

            assertEquals(ServerEnvironment.ServerType.PAPER, environment.getServerType());
            assertTrue(environment.isPaper());
            assertFalse(environment.isFolia());
            assertFalse(environment.isSpigot());
        }
    }

    @Test
    void defaultsToSpigotWhenNoMarkersPresent() {
        try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class, Mockito.CALLS_REAL_METHODS)) {
            classMock.when(() -> Class.forName("io.papermc.paper.threadedregions.RegionizedServer"))
                    .thenThrow(new ClassNotFoundException("folia unavailable"));
            classMock.when(() -> Class.forName("com.destroystokyo.paper.ParticleBuilder"))
                    .thenThrow(new ClassNotFoundException("paper unavailable"));

            ServerEnvironment environment = ServerEnvironment.getInstance();

            assertEquals(ServerEnvironment.ServerType.SPIGOT, environment.getServerType());
            assertTrue(environment.isSpigot());
            assertFalse(environment.isPaper());
            assertFalse(environment.isFolia());
        }
    }

    @Test
    void parsesServerAndMinecraftVersions() {
        ServerEnvironment environment = ServerEnvironment.getInstance();

        assertEquals("v1_20_R3", environment.getServerVersion());
        assertEquals("git-Paper-1.20.1-25", environment.getMinecraftVersion());
        assertTrue(environment.isModern());
        assertTrue(environment.isVersionAtLeast("v1_19_R1"));
    }

    @Test
    void flagsLegacyServersAsNotModern() {
        bukkitMock.when(Bukkit::getVersion).thenReturn("git-Spigot-1.12.2-b1643");
        bukkitMock.when(Bukkit::getServer).thenReturn(new LegacyServerMock());

        ServerEnvironment environment = ServerEnvironment.getInstance();

        assertEquals("v1_12_R1", environment.getServerVersion());
        assertEquals("git-Spigot-1.12.2-b1643", environment.getMinecraftVersion());
        assertFalse(environment.isModern());
        assertFalse(environment.isVersionAtLeast("v1_20_R3"));
    }

    @Test
    void fallsBackToUnknownWhenMinecraftVersionFails() {
        bukkitMock.when(Bukkit::getVersion).thenThrow(new IllegalStateException("version lookup failed"));

        ServerEnvironment environment = ServerEnvironment.getInstance();

        assertEquals("unknown", environment.getMinecraftVersion());
        assertEquals("v1_20_R3", environment.getServerVersion());
    }

    @Test
    void cachesEnvironmentBetweenCalls() {
        try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class, Mockito.CALLS_REAL_METHODS)) {
            classMock.when(() -> Class.forName("io.papermc.paper.threadedregions.RegionizedServer"))
                    .thenThrow(new ClassNotFoundException("folia unavailable"));
            classMock.when(() -> Class.forName("com.destroystokyo.paper.ParticleBuilder"))
                    .thenThrow(new ClassNotFoundException("paper unavailable"));

            ServerEnvironment first = ServerEnvironment.getInstance();
            ServerEnvironment second = ServerEnvironment.getInstance();

            assertSame(first, second);
            classMock.verify(() -> Class.forName("io.papermc.paper.threadedregions.RegionizedServer"), times(1));
            classMock.verify(() -> Class.forName("com.destroystokyo.paper.ParticleBuilder"), times(1));
        }
    }

    private void resetSingleton() throws Exception {
        Field instanceField = ServerEnvironment.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
