package com.raindropcentral.rplatform.utility.heads;

import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static com.raindropcentral.rplatform.utility.heads.HeadAssertions.assertHeadBuilderInteractions;

/**
 * Shared helper utilities for tests that validate {@link RHead} implementations.
 */
public final class HeadTestHelper {

    private HeadTestHelper() {
    }

    /**
     * Creates a {@link HeadMockContext} with mocked builder and translation services for the provided identifier.
     *
     * @param identifier   head identifier used to derive the translation namespace
     * @param player       player requesting the head, used for locale resolution
     * @param nameComponent component returned by the mocked translation service for the name
     * @param loreComponent component returned by the mocked translation service for the lore
     * @param builtStack   item stack returned by the mocked builder
     * @return configured mock context
     */
    public static HeadMockContext mockLocalizedBuilder(
        final String identifier,
        final Player player,
        final Component nameComponent,
        final Component loreComponent,
        final ItemStack builtStack
    ) {
        @SuppressWarnings("unchecked")
        final IHeadBuilder<IHeadBuilder<?>> builder = (IHeadBuilder<IHeadBuilder<?>>) Mockito.mock(IHeadBuilder.class);
        Mockito.when(builder.setCustomTexture(Mockito.any(UUID.class), Mockito.anyString())).thenReturn(builder);
        Mockito.when(builder.setName(Mockito.any(Component.class))).thenReturn(builder);
        Mockito.when(builder.setLore(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.build()).thenReturn(builtStack);

        final MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
        builders.when(UnifiedBuilderFactory::head).thenReturn(builder);

        final TranslationKey nameKey = TranslationKey.of("head." + identifier, "name");
        final TranslationKey loreKey = TranslationKey.of("head." + identifier, "lore");

        final TranslationService nameService = Mockito.mock(TranslationService.class);
        final TranslationService loreService = Mockito.mock(TranslationService.class);

        final TranslatedMessage nameMessage = new TranslatedMessage(nameComponent, nameKey);
        final TranslatedMessage loreMessage = new TranslatedMessage(loreComponent, loreKey);

        Mockito.when(nameService.build()).thenReturn(nameMessage);
        Mockito.when(loreService.build()).thenReturn(loreMessage);

        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
        translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

        return new HeadMockContext(
            builder,
            builders,
            translations,
            nameKey,
            loreKey,
            nameService,
            loreService,
            nameMessage.component(),
            List.copyOf(loreMessage.splitLines())
        );
    }

    /**
     * Auto-closeable wrapper exposing mocked builder and translation interactions.
     */
    public static final class HeadMockContext implements AutoCloseable {

        private final IHeadBuilder<IHeadBuilder<?>> builder;
        private final MockedStatic<UnifiedBuilderFactory> builders;
        private final MockedStatic<TranslationService> translations;
        private final TranslationKey nameKey;
        private final TranslationKey loreKey;
        private final TranslationService nameService;
        private final TranslationService loreService;
        private final Component expectedName;
        private final List<Component> expectedLore;

        private HeadMockContext(
            final IHeadBuilder<IHeadBuilder<?>> builder,
            final MockedStatic<UnifiedBuilderFactory> builders,
            final MockedStatic<TranslationService> translations,
            final TranslationKey nameKey,
            final TranslationKey loreKey,
            final TranslationService nameService,
            final TranslationService loreService,
            final Component expectedName,
            final List<Component> expectedLore
        ) {
            this.builder = builder;
            this.builders = builders;
            this.translations = translations;
            this.nameKey = nameKey;
            this.loreKey = loreKey;
            this.nameService = nameService;
            this.loreService = loreService;
            this.expectedName = expectedName;
            this.expectedLore = expectedLore;
        }

        /**
         * Verifies the builder interactions using the expected UUID and texture payload.
         *
         * @param uuid    expected UUID applied to the head
         * @param texture expected base64 texture string
         */
        public void verifyBuilderInteractions(
            final UUID uuid,
            final String texture
        ) {
            this.builders.verify(UnifiedBuilderFactory::head);
            assertHeadBuilderInteractions(
                this.builder,
                uuid,
                texture,
                this.expectedName,
                this.expectedLore
            );
        }

        /**
         * Verifies the translation service invocations for the stored keys and player.
         *
         * @param player player used to resolve translations
         */
        public void verifyTranslationCalls(final Player player) {
            this.translations.verify(() -> TranslationService.create(this.nameKey, player));
            this.translations.verify(() -> TranslationService.create(this.loreKey, player));
            Mockito.verify(this.nameService).build();
            Mockito.verify(this.loreService).build();
        }

        /**
         * Returns the translation key used for the item name.
         *
         * @return translation key for the head name
         */
        public TranslationKey nameKey() {
            return this.nameKey;
        }

        /**
         * Returns the translation key used for the item lore.
         *
         * @return translation key for the head lore
         */
        public TranslationKey loreKey() {
            return this.loreKey;
        }

        /**
         * Expected translated item name.
         *
         * @return component representing the localized name
         */
        public Component expectedName() {
            return this.expectedName;
        }

        /**
         * Expected translated lore lines.
         *
         * @return immutable list of lore components
         */
        public List<Component> expectedLore() {
            return this.expectedLore;
        }

        @Override
        public void close() {
            this.translations.close();
            this.builders.close();
        }
    }
}

