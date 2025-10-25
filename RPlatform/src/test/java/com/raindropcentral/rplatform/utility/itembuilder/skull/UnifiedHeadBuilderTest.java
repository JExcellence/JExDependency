package com.raindropcentral.rplatform.utility.itembuilder.skull;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedHeadBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        resetServerEnvironment();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @Test
    void headFactorySelectsModernBuilderWhenPaperDetected() {
        installServerEnvironment(true);

        var builder = UnifiedBuilderFactory.head();
        assertInstanceOf(ModernHeadBuilder.class, builder);

        ItemStack expected = new ModernHeadBuilder().build();
        ItemStack actual = builder.build();
        assertEquals(expected, actual);
    }

    @Test
    void headFactorySelectsLegacyBuilderWhenPaperUnavailable() {
        installServerEnvironment(false);

        var builder = UnifiedBuilderFactory.head();
        assertInstanceOf(LegacyHeadBuilder.class, builder);

        ItemStack expected = new LegacyHeadBuilder().build();
        ItemStack actual = builder.build();
        assertEquals(expected, actual);
    }

    @Test
    void safeHeadFactoryProvidesNewSafeBuilderEachCall() {
        installServerEnvironment(true);

        SafeHeadBuilder first = UnifiedBuilderFactory.safeHead();
        SafeHeadBuilder second = UnifiedBuilderFactory.safeHead();

        assertNotSame(first, second);

        ItemStack expected = new SafeHeadBuilder().build();
        assertEquals(expected, first.build());
    }

    @Test
    void unifiedHeadFactoryDelegatesAcrossAllOverloads() {
        installServerEnvironment(false);

        UnifiedHeadBuilder defaultFactory = UnifiedBuilderFactory.unifiedHead();
        UnifiedHeadBuilder defaultDirect = new UnifiedHeadBuilder();
        assertEquals(defaultDirect.build(), defaultFactory.build());

        Player player = server.addPlayer("UnifiedPlayer");
        UnifiedHeadBuilder playerFactory = UnifiedBuilderFactory.unifiedHead(player);
        UnifiedHeadBuilder playerDirect = new UnifiedHeadBuilder(player);
        assertEquals(playerDirect.build(), playerFactory.build());

        OfflinePlayer offlinePlayer = server.getOfflinePlayer("UnifiedOffline");
        UnifiedHeadBuilder offlineFactory = UnifiedBuilderFactory.unifiedHead(offlinePlayer);
        UnifiedHeadBuilder offlineDirect = new UnifiedHeadBuilder(offlinePlayer);
        assertEquals(offlineDirect.build(), offlineFactory.build());

        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String texture = Base64.getEncoder().encodeToString("texture-data".getBytes(StandardCharsets.UTF_8));
        UnifiedHeadBuilder uuidFactory = UnifiedBuilderFactory.unifiedHead(uuid, texture);
        UnifiedHeadBuilder uuidDirect = new UnifiedHeadBuilder(uuid, texture);
        assertEquals(uuidDirect.build(), uuidFactory.build());

        String autoTexture = Base64.getEncoder().encodeToString("auto-texture".getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<UUID> mocked = Mockito.mockStatic(UUID.class)) {
            UUID generated = UUID.fromString("0a1b2c3d-4e5f-6789-aaaa-bbccddeeff00");
            mocked.when(UUID::randomUUID).thenReturn(generated);

            UnifiedHeadBuilder stringFactory = UnifiedBuilderFactory.unifiedHead(autoTexture);
            UnifiedHeadBuilder stringDirect = new UnifiedHeadBuilder(generated, autoTexture);

            Component label = Component.text("Delegated");
            stringFactory.setDisplayName(label);
            stringDirect.setDisplayName(label);

            assertEquals(stringDirect.build(), stringFactory.build());
        }
    }

    @Test
    void unifiedHeadStaticFactoriesMirrorConstructors() {
        installServerEnvironment(false);

        Player player = server.addPlayer("StaticPlayer");
        assertEquals(new UnifiedHeadBuilder(player).build(), UnifiedHeadBuilder.player(player).build());

        OfflinePlayer offlinePlayer = server.getOfflinePlayer("StaticOffline");
        assertEquals(new UnifiedHeadBuilder(offlinePlayer).build(), UnifiedHeadBuilder.player(offlinePlayer).build());

        UUID uuid = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");
        String texture = Base64.getEncoder().encodeToString("static-texture".getBytes(StandardCharsets.UTF_8));
        assertEquals(new UnifiedHeadBuilder(uuid, texture).build(), UnifiedHeadBuilder.custom(uuid, texture).build());

        try (MockedStatic<UUID> mocked = Mockito.mockStatic(UUID.class)) {
            UUID generated = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
            mocked.when(UUID::randomUUID).thenReturn(generated);

            UnifiedHeadBuilder staticFactory = UnifiedHeadBuilder.custom("string-texture");
            UnifiedHeadBuilder direct = new UnifiedHeadBuilder(generated, "string-texture");

            Component label = Component.text("StaticDelegated");
            staticFactory.setDisplayName(label);
            direct.setDisplayName(label);

            assertEquals(direct.build(), staticFactory.build());
        }
    }

    private void installServerEnvironment(boolean paper) {
        ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
        Mockito.when(environment.isPaper()).thenReturn(paper);
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, environment);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to install test server environment", exception);
        }
    }

    private void resetServerEnvironment() {
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to reset server environment", exception);
        }
    }
}
