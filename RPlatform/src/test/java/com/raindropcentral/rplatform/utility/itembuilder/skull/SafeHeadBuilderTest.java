package com.raindropcentral.rplatform.utility.itembuilder.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SafeHeadBuilderTest {

    private static final Field SERVER_ENVIRONMENT_INSTANCE;
    private static final Field META_FIELD;

    static {
        try {
            SERVER_ENVIRONMENT_INSTANCE = ServerEnvironment.class.getDeclaredField("instance");
            SERVER_ENVIRONMENT_INSTANCE.setAccessible(true);

            META_FIELD = AItemBuilder.class.getDeclaredField("meta");
            META_FIELD.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static ServerMock server;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @BeforeEach
    void resetEnvironment() {
        resetServerEnvironment();
    }

    @Test
    void setPlayerHeadDowngradesOnPaperProfileClassCast() {
        ServerEnvironment paperEnvironment = mock(ServerEnvironment.class);
        when(paperEnvironment.isPaper()).thenReturn(true);
        setServerEnvironment(paperEnvironment);

        SafeHeadBuilder builder = new SafeHeadBuilder();
        SkullMeta originalMeta = extractMeta(builder);
        SkullMeta metaSpy = spy(originalMeta);
        doThrow(new ClassCastException("incompatible profile")).when(metaSpy).setPlayerProfile(any(PlayerProfile.class));
        replaceMeta(builder, metaSpy);

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("PaperUser");
        when(player.getPlayerProfile()).thenReturn(mock(PlayerProfile.class));

        SafeHeadBuilder returnedBuilder = assertDoesNotThrow(() -> builder.setPlayerHead(player));
        assertSame(builder, returnedBuilder);

        ItemStack itemStack = assertDoesNotThrow(builder::build);
        assertEquals(Material.PLAYER_HEAD, itemStack.getType());

        SkullMeta resultMeta = (SkullMeta) itemStack.getItemMeta();
        assertNotNull(resultMeta);
        assertEquals("PaperUser", resultMeta.getOwner());

        verify(metaSpy).setPlayerProfile(any(PlayerProfile.class));
        verify(metaSpy).setOwner("PaperUser");
    }

    @Test
    void setOfflinePlayerHeadFallsBackWhenPaperProfileFails() {
        ServerEnvironment paperEnvironment = mock(ServerEnvironment.class);
        when(paperEnvironment.isPaper()).thenReturn(true);
        setServerEnvironment(paperEnvironment);

        SafeHeadBuilder builder = new SafeHeadBuilder();
        SkullMeta originalMeta = extractMeta(builder);
        SkullMeta metaSpy = spy(originalMeta);
        doThrow(new ClassCastException("offline profile unsupported")).when(metaSpy).setPlayerProfile(any(PlayerProfile.class));
        replaceMeta(builder, metaSpy);

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getName()).thenReturn("LegacyFriend");
        when(offlinePlayer.getPlayerProfile()).thenReturn(mock(PlayerProfile.class));

        SafeHeadBuilder returnedBuilder = assertDoesNotThrow(() -> builder.setPlayerHead(offlinePlayer));
        assertSame(builder, returnedBuilder);

        ItemStack itemStack = assertDoesNotThrow(builder::build);
        assertEquals(Material.PLAYER_HEAD, itemStack.getType());

        SkullMeta resultMeta = (SkullMeta) itemStack.getItemMeta();
        assertNotNull(resultMeta);
        assertEquals("LegacyFriend", resultMeta.getOwner());
        assertEquals(offlinePlayer, resultMeta.getOwningPlayer());

        verify(metaSpy).setPlayerProfile(any(PlayerProfile.class));
        verify(metaSpy).setOwner("LegacyFriend");
        verify(metaSpy).setOwningPlayer(offlinePlayer);
    }

    @Test
    void setCustomTextureFallsBackToLegacyBridgeOnPaperFailure() {
        ServerEnvironment paperEnvironment = mock(ServerEnvironment.class);
        when(paperEnvironment.isPaper()).thenReturn(true);
        setServerEnvironment(paperEnvironment);

        SafeHeadBuilder builder = new SafeHeadBuilder();
        SkullMeta originalMeta = extractMeta(builder);
        SkullMeta metaSpy = spy(originalMeta);
        doThrow(new ClassCastException("custom texture failure")).when(metaSpy).setPlayerProfile(any(PlayerProfile.class));
        replaceMeta(builder, metaSpy);

        UUID profileId = UUID.randomUUID();
        String textures = "base64-texture";

        SafeHeadBuilder returnedBuilder = assertDoesNotThrow(() -> builder.setCustomTexture(profileId, textures));
        assertSame(builder, returnedBuilder);

        ItemStack itemStack = assertDoesNotThrow(builder::build);
        assertEquals(Material.PLAYER_HEAD, itemStack.getType());

        verify(metaSpy).setPlayerProfile(any(PlayerProfile.class));
        verify(metaSpy, atLeastOnce()).getOwner();
    }

    @Test
    void legacyEnvironmentSkipsPaperSpecificMetadata() {
        ServerEnvironment legacyEnvironment = mock(ServerEnvironment.class);
        when(legacyEnvironment.isPaper()).thenReturn(false);
        setServerEnvironment(legacyEnvironment);

        SafeHeadBuilder builder = new SafeHeadBuilder();
        SkullMeta originalMeta = extractMeta(builder);
        SkullMeta metaSpy = spy(originalMeta);
        replaceMeta(builder, metaSpy);

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("LegacyPlayer");

        assertDoesNotThrow(() -> builder.setPlayerHead(player));
        assertDoesNotThrow(() -> builder.setCustomTexture(UUID.randomUUID(), "legacy-texture"));

        ItemStack itemStack = builder.build();
        assertEquals(Material.PLAYER_HEAD, itemStack.getType());

        verify(metaSpy, never()).setPlayerProfile(any(PlayerProfile.class));
        verify(metaSpy).setOwner("LegacyPlayer");
    }

    private static SkullMeta extractMeta(SafeHeadBuilder builder) {
        try {
            return (SkullMeta) META_FIELD.get(builder);
        } catch (IllegalAccessException exception) {
            fail("Failed to access builder metadata", exception);
            return null;
        }
    }

    private static void replaceMeta(SafeHeadBuilder builder, SkullMeta meta) {
        try {
            META_FIELD.set(builder, meta);
        } catch (IllegalAccessException exception) {
            fail("Failed to replace builder metadata", exception);
        }
    }

    private static void setServerEnvironment(ServerEnvironment environment) {
        try {
            SERVER_ENVIRONMENT_INSTANCE.set(null, environment);
        } catch (IllegalAccessException exception) {
            fail("Failed to set ServerEnvironment instance", exception);
        }
    }

    private static void resetServerEnvironment() {
        try {
            SERVER_ENVIRONMENT_INSTANCE.set(null, null);
        } catch (IllegalAccessException exception) {
            fail("Failed to reset ServerEnvironment instance", exception);
        }
    }
}
