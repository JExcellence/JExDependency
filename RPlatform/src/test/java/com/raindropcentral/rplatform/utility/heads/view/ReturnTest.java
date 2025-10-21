package com.raindropcentral.rplatform.utility.heads.view;

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

import static com.raindropcentral.rplatform.utility.heads.HeadAssertions.assertHeadDefinition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReturnTest {

    private static final String IDENTIFIER = "return";
    private static final String UUID_STRING = "7d7075a9-1df3-485d-bf00-ffd6ce2d6244";
    private static final UUID UUID_VALUE = UUID.fromString(UUID_STRING);
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJiYTgxYjQ3ZDVlZTA2YjQ4NGVhOWJkZjIyOTM0ZTZhYmNhNWU0Y2VkN2JlMzkwNWQ2YWU2ZWNkNmZjZWEyYSJ9fX0=";

    @Test
    void constantsExposeConfiguredMetadata() {
        assertEquals(UUID_STRING, Return.UUID);
        assertEquals(TEXTURE, Return.TEXTURE);

        final Return head = new Return();

        assertHeadDefinition(head, IDENTIFIER, UUID_VALUE, TEXTURE, EHeadFilter.INVENTORY);
    }

    @Test
    void getHeadBuildsItemStackWithTranslatedMetadata() {
        final Player player = Mockito.mock(Player.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);

        try (HeadMockContext context = HeadTestHelper.mockLocalizedBuilder(
            IDENTIFIER,
            player,
            Component.text("Return"),
            Component.text("Line 1\nLine 2"),
            expectedStack
        )) {
            final Return head = new Return();
            final ItemStack result = head.getHead(player);

            assertSame(expectedStack, result);
            context.verifyBuilderInteractions(UUID_VALUE, TEXTURE);
            context.verifyTranslationCalls(player);
        }
    }
}

