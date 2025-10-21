package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import com.raindropcentral.rplatform.utility.heads.RHead;
import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class HeadViewAssertions {

    private HeadViewAssertions() {
    }

    static void assertMetadata(
        final RHead head,
        final String expectedIdentifier,
        final UUID expectedUuid,
        final String expectedTexture,
        final EHeadFilter expectedFilter
    ) {
        assertEquals(expectedIdentifier, head.getIdentifier());
        assertEquals(expectedUuid, head.getUuid());
        assertEquals(expectedTexture, head.getTexture());
        assertEquals(expectedFilter, head.getFilter());
        assertEquals("head." + expectedIdentifier, head.getTranslationKey());
    }

    static void assertBuildsWithTranslationKey(
        final RHead head,
        final UUID expectedUuid,
        final String expectedTexture,
        final String expectedTranslationKey
    ) {
        assertEquals(expectedTranslationKey, head.getTranslationKey());

        final Player player = Mockito.mock(Player.class);

        @SuppressWarnings("unchecked")
        final IHeadBuilder<IHeadBuilder<?>> builder = (IHeadBuilder<IHeadBuilder<?>>) Mockito.mock(IHeadBuilder.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);
        Mockito.when(builder.setCustomTexture(expectedUuid, expectedTexture)).thenReturn(builder);
        Mockito.when(builder.setName(Mockito.any(Component.class))).thenReturn(builder);
        Mockito.when(builder.setLore(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(expectedStack);

        final TranslationKey nameKey = TranslationKey.of(expectedTranslationKey, "name");
        final TranslationKey loreKey = TranslationKey.of(expectedTranslationKey, "lore");
        final TranslationService nameService = Mockito.mock(TranslationService.class);
        final TranslationService loreService = Mockito.mock(TranslationService.class);

        final TranslatedMessage nameMessage = new TranslatedMessage(Component.text("Display Name"), nameKey);
        final TranslatedMessage loreMessage = new TranslatedMessage(Component.text("Lore Line 1\nLore Line 2"), loreKey);
        Mockito.when(nameService.build()).thenReturn(nameMessage);
        Mockito.when(loreService.build()).thenReturn(loreMessage);

        try (
            MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
            MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)
        ) {
            builders.when(UnifiedBuilderFactory::head).thenReturn(builder);
            translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
            translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

            final ItemStack result = head.getHead(player);

            assertSame(expectedStack, result);
            builders.verify(UnifiedBuilderFactory::head);
            Mockito.verify(builder).setCustomTexture(expectedUuid, expectedTexture);
            final ArgumentCaptor<Component> nameCaptor = ArgumentCaptor.forClass(Component.class);
            Mockito.verify(builder).setName(nameCaptor.capture());
            assertEquals(nameMessage.component(), nameCaptor.getValue());
            @SuppressWarnings("unchecked")
            final ArgumentCaptor<List<Component>> loreCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.verify(builder).setLore(loreCaptor.capture());
            assertEquals(loreMessage.splitLines(), loreCaptor.getValue());
            Mockito.verify(builder).build();
            translations.verify(() -> TranslationService.create(nameKey, player));
            translations.verify(() -> TranslationService.create(loreKey, player));
        }
    }
}
