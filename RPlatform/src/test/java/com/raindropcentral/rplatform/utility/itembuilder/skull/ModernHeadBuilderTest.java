package com.raindropcentral.rplatform.utility.itembuilder.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModernHeadBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        resetServerEnvironment();
        installPaperEnvironment();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @Test
    void buildsHeadWithModernTextureAndAdventureMetadata() {
        ModernHeadBuilder builder = new ModernHeadBuilder();

        Component displayName = Component.text("Starbound Visage");
        Component firstLore = Component.text("Threads of light");
        Component secondLore = Component.text("Stitched to eternity");
        Component thirdLore = Component.text("Guided by constellations");

        ModernHeadBuilder chained = builder.setName(displayName);
        assertSame(builder, chained);

        chained = chained.setLore(List.of(firstLore));
        assertSame(builder, chained);

        chained = chained.addLoreLine(secondLore);
        assertSame(builder, chained);

        UUID uuid = UUID.randomUUID();
        String textures = "base64-texture";

        chained = chained.addLoreLines(thirdLore);
        assertSame(builder, chained);

        chained = chained.setCustomTexture(uuid, textures);
        assertSame(builder, chained);

        ItemStack head = chained.build();
        assertNotNull(head);

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);

        assertEquals(displayName, meta.displayName());
        List<Component> lore = meta.lore();
        assertNotNull(lore);
        assertEquals(List.of(firstLore, secondLore, thirdLore), lore);

        PlayerProfile profile = meta.getPlayerProfile();
        assertNotNull(profile);
        assertEquals(uuid, profile.getId());
        assertEquals(textures, findTexture(profile.getProperties()));
    }

    @Test
    void setPlayerHeadUsesLiveProfiles() {
        Player player = server.addPlayer();

        ModernHeadBuilder builder = new ModernHeadBuilder();
        ModernHeadBuilder chained = builder.setPlayerHead(player);
        assertSame(builder, chained);

        ItemStack head = builder.build();
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);

        PlayerProfile profile = meta.getPlayerProfile();
        assertNotNull(profile);
        assertEquals(player.getUniqueId(), profile.getId());
    }

    @Test
    void setPlayerHeadGracefullyHandlesMissingMetadata() {
        ModernHeadBuilder builder = new ModernHeadBuilder();

        ModernHeadBuilder chained = builder.setPlayerHead((Player) null);
        assertSame(builder, chained);

        chained = builder.setPlayerHead((OfflinePlayer) null);
        assertSame(builder, chained);

        ItemStack head = builder.build();
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertNull(meta.getPlayerProfile());
        assertFalse(meta.hasOwner());
    }

    private String findTexture(final Collection<ProfileProperty> properties) {
        return properties.stream()
                .filter(property -> "textures".equals(property.getName()))
                .findFirst()
                .map(ProfileProperty::getValue)
                .orElse(null);
    }

    private void installPaperEnvironment() {
        ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
        Mockito.when(environment.isPaper()).thenReturn(true);
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, environment);
        } catch (ReflectiveOperationException exception) {
            fail("Unable to install test server environment", exception);
        }
    }

    private void resetServerEnvironment() {
        try {
            Field instance = ServerEnvironment.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (ReflectiveOperationException exception) {
            fail("Unable to reset test server environment", exception);
        }
    }
}
