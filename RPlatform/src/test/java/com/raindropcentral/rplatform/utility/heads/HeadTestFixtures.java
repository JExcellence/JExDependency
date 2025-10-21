package com.raindropcentral.rplatform.utility.heads;

import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Shared fixtures for testing {@link RHead} implementations.
 */
public final class HeadTestFixtures {

    private HeadTestFixtures() {
    }

    /**
     * Creates a reusable fixture that prepares mocked builder and translation services for exercising
     * {@link RHead#getHead(Player)}.
     *
     * @param identifier the expected head identifier
     * @param uuidString the expected UUID string backing the head texture
     * @param texture    the expected Base64 texture value
     * @return the prepared test fixture
     */
    public static @NotNull HeadFixture create(
        final @NotNull String identifier,
        final @NotNull String uuidString,
        final @NotNull String texture
    ) {
        final Player player = Mockito.mock(Player.class);

        @SuppressWarnings("unchecked")
        final IHeadBuilder<IHeadBuilder<?>> builder = (IHeadBuilder<IHeadBuilder<?>>) Mockito.mock(IHeadBuilder.class);
        final ItemStack expectedStack = new ItemStack(Material.PLAYER_HEAD);
        Mockito.when(builder.setCustomTexture(Mockito.any(UUID.class), Mockito.eq(texture))).thenReturn(builder);
        Mockito.when(builder.setName(Mockito.any(Component.class))).thenReturn(builder);
        Mockito.when(builder.setLore(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(expectedStack);

        final TranslationKey nameKey = TranslationKey.of("head." + identifier, "name");
        final TranslationKey loreKey = TranslationKey.of("head." + identifier, "lore");
        final TranslationService nameService = Mockito.mock(TranslationService.class);
        final TranslationService loreService = Mockito.mock(TranslationService.class);
        final TranslatedMessage nameMessage = new TranslatedMessage(Component.text("Display Name"), nameKey);
        final TranslatedMessage loreMessage = new TranslatedMessage(Component.text("Line 1\nLine 2"), loreKey);
        Mockito.when(nameService.build()).thenReturn(nameMessage);
        Mockito.when(loreService.build()).thenReturn(loreMessage);

        final MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        builders.when(UnifiedBuilderFactory::head).thenReturn(builder);
        translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
        translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

        return new HeadFixture(
            player,
            builder,
            expectedStack,
            builders,
            translations,
            nameKey,
            loreKey,
            nameMessage,
            loreMessage,
            uuidString,
            texture
        );
    }

    /**
     * Context holding the mocked dependencies for a single head test.
     */
    public static final class HeadFixture implements AutoCloseable {

        private final Player player;
        private final IHeadBuilder<IHeadBuilder<?>> builder;
        private final ItemStack expectedStack;
        private final MockedStatic<UnifiedBuilderFactory> builders;
        private final MockedStatic<TranslationService> translations;
        private final TranslationKey nameKey;
        private final TranslationKey loreKey;
        private final TranslatedMessage nameMessage;
        private final TranslatedMessage loreMessage;
        private final String uuidString;
        private final String texture;

        private HeadFixture(
            final Player player,
            final IHeadBuilder<IHeadBuilder<?>> builder,
            final ItemStack expectedStack,
            final MockedStatic<UnifiedBuilderFactory> builders,
            final MockedStatic<TranslationService> translations,
            final TranslationKey nameKey,
            final TranslationKey loreKey,
            final TranslatedMessage nameMessage,
            final TranslatedMessage loreMessage,
            final String uuidString,
            final String texture
        ) {
            this.player = player;
            this.builder = builder;
            this.expectedStack = expectedStack;
            this.builders = builders;
            this.translations = translations;
            this.nameKey = nameKey;
            this.loreKey = loreKey;
            this.nameMessage = nameMessage;
            this.loreMessage = loreMessage;
            this.uuidString = uuidString;
            this.texture = texture;
        }

        /**
         * Invokes {@link RHead#getHead(Player)} using the prepared player mock.
         *
         * @param head the head under test
         * @return the returned item stack
         */
        public @NotNull ItemStack invokeGetHead(final @NotNull RHead head) {
            return head.getHead(player);
        }

        /**
         * Verifies that the mocked builder and translation services were invoked with the expected metadata.
         *
         * @param result the item stack returned from {@link #invokeGetHead(RHead)}
         */
        public void verifyBuilderInteractions(final @NotNull ItemStack result) {
            assertSame(expectedStack, result);
            builders.verify(UnifiedBuilderFactory::head);
            Mockito.verify(builder).setCustomTexture(UUID.fromString(uuidString), texture);
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

        @Override
        public void close() {
            translations.close();
            builders.close();
        }
    }
}
