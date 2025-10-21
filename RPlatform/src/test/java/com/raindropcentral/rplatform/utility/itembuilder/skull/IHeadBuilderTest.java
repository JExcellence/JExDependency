package com.raindropcentral.rplatform.utility.itembuilder.skull;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.utility.itembuilder.AItemBuilder;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IHeadBuilderTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        resetServerEnvironment();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
        resetServerEnvironment();
    }

    @Test
    void fluentHeadContractPreservesBuilderTypeAndMetadata() {
        StubHeadBuilder builder = new StubHeadBuilder();

        assertSame(builder, builder.setPlayerHead((Player) null));
        assertNull(builder.getLastPlayer(), "Null player should be accepted without mutation");

        Player onlinePlayer = Mockito.mock(Player.class);
        assertSame(builder, builder.setPlayerHead(onlinePlayer));
        assertSame(onlinePlayer, builder.getLastPlayer());
        assertNull(builder.getLastOfflinePlayer(), "Setting a player head should clear the offline reference");

        assertSame(builder, builder.setPlayerHead((OfflinePlayer) null));
        assertNull(builder.getLastOfflinePlayer(), "Null offline player should be accepted without mutation");

        OfflinePlayer offlinePlayer = Mockito.mock(OfflinePlayer.class);
        assertSame(builder, builder.setPlayerHead(offlinePlayer));
        assertSame(offlinePlayer, builder.getLastOfflinePlayer());

        UUID textureId = UUID.randomUUID();
        String textureData = "base64-texture";
        assertSame(builder, builder.setCustomTexture(textureId, textureData));
        assertEquals(textureId, builder.getLastTextureId());
        assertEquals(textureData, builder.getLastTexture());

        Component displayName = Component.text("Sky Seer");
        Component loreOne = Component.text("Sees beyond the clouds");
        Component loreTwo = Component.text("Guards the horizon");

        assertSame(builder, builder.setName(displayName));
        assertSame(builder, builder.setLore(List.of(loreOne)));
        assertSame(builder, builder.addLoreLine(loreTwo));
        assertSame(builder, builder.addLoreLines(List.of(loreOne, loreTwo)));
        assertSame(builder, builder.addLoreLines(loreOne, loreTwo));
        assertSame(builder, builder.setAmount(2));
        assertSame(builder, builder.setCustomModelData(45));
        assertSame(builder, builder.addEnchantment(Enchantment.MENDING, 3));
        assertSame(builder, builder.addItemFlags(ItemFlag.HIDE_ATTRIBUTES));
        assertSame(builder, builder.setGlowing(true));

        ItemStack built = builder.build();
        assertEquals(Material.PLAYER_HEAD, built.getType());
        assertEquals(2, built.getAmount());

        SkullMeta meta = (SkullMeta) built.getItemMeta();
        assertNotNull(meta);

        assertNotNull(meta.getDisplayName());
        Component actualName = LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
        assertEquals(displayName, actualName);

        List<String> rawLore = meta.getLore();
        assertNotNull(rawLore);
        List<Component> actualLore = rawLore.stream()
                .map(line -> LegacyComponentSerializer.legacySection().deserialize(line))
                .collect(Collectors.toList());

        List<Component> expectedLore = new ArrayList<>();
        expectedLore.add(loreOne);
        expectedLore.add(loreTwo);
        expectedLore.add(loreOne);
        expectedLore.add(loreTwo);
        expectedLore.add(loreOne);
        expectedLore.add(loreTwo);

        assertEquals(expectedLore, actualLore);
        assertTrue(meta.hasCustomModelData());
        assertEquals(45, meta.getCustomModelData());
        assertTrue(meta.hasEnchant(Enchantment.MENDING));
        assertEquals(3, meta.getEnchantLevel(Enchantment.MENDING));
        assertTrue(meta.hasEnchant(Enchantment.LURE));
        assertEquals(1, meta.getEnchantLevel(Enchantment.LURE));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));
        assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS));
    }

    private static void resetServerEnvironment() {
        try {
            Field instanceField = ServerEnvironment.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException exception) {
            fail("Failed to reset ServerEnvironment", exception);
        }
    }

    private static final class StubHeadBuilder extends AItemBuilder<SkullMeta, StubHeadBuilder> implements IHeadBuilder<StubHeadBuilder> {

        private Player lastPlayer;
        private OfflinePlayer lastOfflinePlayer;
        private UUID lastTextureId;
        private String lastTexture;

        private StubHeadBuilder() {
            super(Material.PLAYER_HEAD);
        }

        @Override
        public StubHeadBuilder setPlayerHead(Player player) {
            this.lastPlayer = player;
            this.lastOfflinePlayer = null;
            if (player != null) {
                meta.setOwningPlayer(player);
            }
            return this;
        }

        @Override
        public StubHeadBuilder setPlayerHead(OfflinePlayer offlinePlayer) {
            this.lastOfflinePlayer = offlinePlayer;
            this.lastPlayer = null;
            if (offlinePlayer != null) {
                meta.setOwningPlayer(offlinePlayer);
            }
            return this;
        }

        @Override
        public StubHeadBuilder setCustomTexture(UUID uuid, String textures) {
            this.lastTextureId = uuid;
            this.lastTexture = textures;
            return this;
        }

        private Player getLastPlayer() {
            return lastPlayer;
        }

        private OfflinePlayer getLastOfflinePlayer() {
            return lastOfflinePlayer;
        }

        private UUID getLastTextureId() {
            return lastTextureId;
        }

        private String getLastTexture() {
            return lastTexture;
        }
    }
}
