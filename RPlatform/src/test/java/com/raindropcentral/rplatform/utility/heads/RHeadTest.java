package com.raindropcentral.rplatform.utility.heads;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        try (HeadTestFixtures.HeadFixture fixture = HeadTestFixtures.create(IDENTIFIER, UUID_STRING, TEXTURE)) {
            final TestHead head = new TestHead(IDENTIFIER, UUID_STRING, TEXTURE, EHeadFilter.PLAYER);
            final ItemStack result = fixture.invokeGetHead(head);

            fixture.verifyBuilderInteractions(result);
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
