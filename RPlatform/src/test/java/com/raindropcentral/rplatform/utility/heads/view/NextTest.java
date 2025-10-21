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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NextTest {

    private static final String IDENTIFIER = "next";
    private static final UUID UUID_VALUE = UUID.fromString("e0b1b4fd-ec3c-46ca-96ff-85f0073cbfb8");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTk5YWI0NDFmYzk3ZjYwOTA4MWFkM2NlMzNkNTk4MjkxZDUxYmVmOGNiN2FkMjQ4NGI1YzEzODdjN2E4NCJ9fX0=";

    @Test
    void constructorAssignsCatalogConstants() {
        final RHead head = new Next();

        assertEquals(IDENTIFIER, head.getIdentifier());
        assertEquals(UUID_VALUE, head.getUuid());
        assertEquals(TEXTURE, head.getTexture());
        assertEquals(EHeadFilter.INVENTORY, head.getFilter());
        assertEquals("head." + IDENTIFIER, head.getTranslationKey());
    }

    @Test
    void getHeadBuildsLocalizedNextHead() {
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

        final TranslatedMessage nameMessage = new TranslatedMessage(Component.text("Next"), nameKey);
        final TranslatedMessage loreMessage = new TranslatedMessage(Component.text("Line 1\nLine 2"), loreKey);
        Mockito.when(nameService.build()).thenReturn(nameMessage);
        Mockito.when(loreService.build()).thenReturn(loreMessage);

        try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            builders.when(UnifiedBuilderFactory::head).thenReturn(builder);
            translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
            translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

            final RHead head = new Next();
            final ItemStack result = head.getHead(player);

            assertSame(expectedStack, result);
            builders.verify(UnifiedBuilderFactory::head);
            Mockito.verify(builder).setCustomTexture(UUID_VALUE, TEXTURE);
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
