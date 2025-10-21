package com.raindropcentral.rplatform.utility.heads.view.pagination;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import com.raindropcentral.rplatform.utility.heads.HeadTestHelper;
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

class Number0Test {

    private static final String IDENTIFIER = "0";
    private static final UUID UUID_VALUE = UUID.fromString("f56125cd-447b-424d-8168-9d030c2cec8b");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTE4OTc3Y2Y4ZjJmZjBlYTkyZTZkYjZhNTZjZWExZjVmOGFlNDRjN2NhOTM3YWZhZTdlNTI2Y2M5OGRiZDgifX19";
    private static final String TRANSLATION_KEY = "head.pagination.0";

    @Test
    void metadataMatchesConfiguredConstants() {
        final Number0 head = new Number0();

        assertEquals(IDENTIFIER, head.getIdentifier());
        assertEquals(UUID_VALUE, head.getUuid());
        assertEquals(TEXTURE, head.getTexture());
        assertEquals(EHeadFilter.INVENTORY, head.getFilter());
        assertEquals(TRANSLATION_KEY, head.getTranslationKey());
    }

    @Test
    void getHeadUsesConfiguredTextureAndTranslations() {
        final Player player = Mockito.mock(Player.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);

        try (HeadMockContext context = HeadTestHelper.mockLocalizedBuilder(
                "pagination.0",
                player,
                Component.text("Zero"),
                Component.text("Line 1\nLine 2"),
                expectedStack
        )) {
            final Number0 head = new Number0();
            final ItemStack result = head.getHead(player);

            assertSame(expectedStack, result);
            context.verifyBuilderInteractions(UUID_VALUE, TEXTURE);
            context.verifyTranslationCalls(player);
        }
    }
}

