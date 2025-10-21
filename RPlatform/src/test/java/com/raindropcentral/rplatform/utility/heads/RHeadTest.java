package com.raindropcentral.rplatform.utility.heads;

import com.raindropcentral.rplatform.utility.heads.HeadTestHelper.HeadMockContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RHeadTest {

    private static final String IDENTIFIER = "test-head";
    private static final String UUID_STRING = "00000000-0000-0000-0000-000000000001";
    private static final String TEXTURE = "base64-texture";

    @Test
    void constructorWithExplicitFilterAssignsMetadata() {
        final TestHead head = new TestHead(IDENTIFIER, UUID_STRING, TEXTURE, EHeadFilter.DECORATION);

        assertEquals(IDENTIFIER, head.getIdentifier());
        assertEquals(UUID.fromString(UUID_STRING), head.getUuid());
        assertEquals(TEXTURE, head.getTexture());
        assertEquals(EHeadFilter.DECORATION, head.getFilter());
        assertEquals("head." + IDENTIFIER, head.getTranslationKey());
    }

    @Test
    void constructorDefaultsInventoryFilterWhenNotProvided() {
        final TestHead head = new TestHead(IDENTIFIER, UUID_STRING, TEXTURE);

        assertEquals(IDENTIFIER, head.getIdentifier());
        assertEquals(UUID.fromString(UUID_STRING), head.getUuid());
        assertEquals(TEXTURE, head.getTexture());
        assertEquals(EHeadFilter.INVENTORY, head.getFilter());
        assertEquals("head." + IDENTIFIER, head.getTranslationKey());
    }

    @Test
    void getHeadBuildsItemStackWithLocalizedMetadata() {
        final Player player = Mockito.mock(Player.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);

        try (HeadMockContext context = HeadTestHelper.mockLocalizedBuilder(
            IDENTIFIER,
            player,
            Component.text("Display Name"),
            Component.text("Line 1\nLine 2"),
            expectedStack
        )) {
            final TestHead head = new TestHead(IDENTIFIER, UUID_STRING, TEXTURE, EHeadFilter.PLAYER);
            final ItemStack result = head.getHead(player);

            assertSame(expectedStack, result);
            context.verifyBuilderInteractions(UUID.fromString(UUID_STRING), TEXTURE);
            context.verifyTranslationCalls(player);
        }
    }

    private static final class TestHead extends RHead {

        private TestHead(
            final String identifier,
            final String uuid,
            final String texture,
            final EHeadFilter filter
        ) {
            super(identifier, uuid, texture, filter);
        }

        private TestHead(
            final String identifier,
            final String uuid,
            final String texture
        ) {
            super(identifier, uuid, texture);
        }
    }
}
