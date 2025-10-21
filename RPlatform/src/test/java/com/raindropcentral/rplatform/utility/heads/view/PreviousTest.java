package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static com.raindropcentral.rplatform.utility.heads.HeadAssertions.assertHeadBuilderInteractions;
import static com.raindropcentral.rplatform.utility.heads.HeadAssertions.assertHeadDefinition;
import static org.junit.jupiter.api.Assertions.assertSame;

class PreviousTest {

    private static final String IDENTIFIER = "previous";
    private static final UUID UUID_VALUE = UUID.fromString("2903c9aa-6ea6-43f9-9601-75f0a50c49ca");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmE2Y2FhMWUxZTlkOWFlZjU5Mjc4NzExNDIyNzg3YTAxNzk5M2M1YjI5MjUxOGM5ZjYzMmQ0MTJmNWE2NTkifX19";

    @Test
    void definitionMatchesConfiguredMetadata() {
        final Previous head = new Previous();

        assertHeadDefinition(head, IDENTIFIER, UUID_VALUE, TEXTURE, EHeadFilter.INVENTORY);
    }

    @Test
    void getHeadBuildsItemStackWithTranslatedMetadata() {
        final Player player = Mockito.mock(Player.class);

        @SuppressWarnings("unchecked")
        final IHeadBuilder<IHeadBuilder<?>> builder = (IHeadBuilder<IHeadBuilder<?>>) Mockito.mock(IHeadBuilder.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);
        Mockito.when(builder.setCustomTexture(Mockito.any(UUID.class), Mockito.eq(TEXTURE))).thenReturn(builder);
        Mockito.when(builder.setName(Mockito.any(Component.class))).thenReturn(builder);
        Mockito.when(builder.setLore(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(expectedStack);

        final TranslationKey nameKey = TranslationKey.of("head." + IDENTIFIER, "name");
        final TranslationKey loreKey = TranslationKey.of("head." + IDENTIFIER, "lore");

        final TranslationService nameService = Mockito.mock(TranslationService.class);
        final TranslationService loreService = Mockito.mock(TranslationService.class);

        final Component displayName = Component.text("Previous Page");
        final TranslatedMessage nameMessage = new TranslatedMessage(displayName, nameKey);

        final Component loreComponent = Component.text("Line 1\nLine 2");
        final TranslatedMessage loreMessage = new TranslatedMessage(loreComponent, loreKey);
        final List<Component> expectedLore = loreMessage.splitLines();

        Mockito.when(nameService.build()).thenReturn(nameMessage);
        Mockito.when(loreService.build()).thenReturn(loreMessage);

        try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            builders.when(UnifiedBuilderFactory::head).thenReturn(builder);
            translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
            translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

            final Previous head = new Previous();
            final ItemStack result = head.getHead(player);

            assertSame(expectedStack, result);
            builders.verify(UnifiedBuilderFactory::head);
            translations.verify(() -> TranslationService.create(nameKey, player));
            translations.verify(() -> TranslationService.create(loreKey, player));
            assertHeadBuilderInteractions(builder, UUID_VALUE, TEXTURE, displayName, expectedLore);
        }
    }
}
