package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import com.raindropcentral.rplatform.utility.heads.HeadTestHelper;
import com.raindropcentral.rplatform.utility.heads.HeadTestHelper.HeadMockContext;
import de.jexcellence.jextranslate.api.TranslationKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LeaderboardTest {

    @Test
    void leaderboardMetadataIsStable() {
        final Leaderboard leaderboard = new Leaderboard();

        assertEquals("leaderboard", leaderboard.getIdentifier());
        assertEquals(UUID.fromString(Leaderboard.UUID), leaderboard.getUuid());
        assertEquals(Leaderboard.TEXTURE, leaderboard.getTexture());
        assertEquals(EHeadFilter.INVENTORY, leaderboard.getFilter());
        assertEquals("head.leaderboard", leaderboard.getTranslationKey());
    }

    @Test
    void getHeadUsesLeaderboardTranslationKey() {
        final Leaderboard leaderboard = new Leaderboard();
        final Player player = Mockito.mock(Player.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);

        try (HeadMockContext context = HeadTestHelper.mockLocalizedBuilder(
            leaderboard.getIdentifier(),
            player,
            Component.text("Leaderboard"),
            Component.text("Line One\nLine Two"),
            expectedStack
        )) {
            final ItemStack result = leaderboard.getHead(player);

            assertSame(expectedStack, result);
            context.verifyBuilderInteractions(UUID.fromString(Leaderboard.UUID), Leaderboard.TEXTURE);
            context.verifyTranslationCalls(player);
            assertEquals(TranslationKey.of("head.leaderboard", "name"), context.nameKey());
            assertEquals(TranslationKey.of("head.leaderboard", "lore"), context.loreKey());
        }
    }
}

