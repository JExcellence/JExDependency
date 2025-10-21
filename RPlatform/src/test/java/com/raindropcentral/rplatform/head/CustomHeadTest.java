package com.raindropcentral.rplatform.head;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.head.HeadCategory;
import com.raindropcentral.rplatform.utility.heads.RHead;
import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CustomHeadTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void customHeadConstructorsExposeProperties() {
        final UUID explicitUuid = UUID.fromString("38ca34d0-76c0-4c9b-9152-056130cf7ad1");
        final SimpleCustomHead explicit = new SimpleCustomHead("seasonal", explicitUuid, "texture-data", HeadCategory.PLAYER);

        assertEquals("seasonal", explicit.getIdentifier());
        assertEquals(explicitUuid, explicit.getTextureUuid());
        assertEquals("texture-data", explicit.getTextureValue());
        assertEquals(HeadCategory.PLAYER, explicit.getCategory());
        assertEquals("head.seasonal", explicit.getTranslationKey());
        assertNotNull(explicit.createItem());

        final UUID parsedUuid = UUID.fromString("9b0a2e25-40ad-4275-9f56-79ebfdf4f0a6");
        final SimpleCustomHead parsed = new SimpleCustomHead("inventory", parsedUuid.toString(), "alt-texture");

        assertEquals("inventory", parsed.getIdentifier());
        assertEquals(parsedUuid, parsed.getTextureUuid());
        assertEquals("alt-texture", parsed.getTextureValue());
        assertEquals(HeadCategory.INVENTORY, parsed.getCategory());
        assertEquals("head.inventory", parsed.getTranslationKey());
    }

    @Test
    void getHeadBuildsItemWithTranslatedMetadata() {
        final UUID headUuid = UUID.fromString("0cbd3161-4602-4c05-9c74-84e751fb1a27");
        final TestHead head = new TestHead("test", headUuid.toString(), "base64-texture");
        final PlayerMock player = this.server.addPlayer("Translator");

        final TranslationKey nameKey = TranslationKey.of("head.test", "name");
        final TranslationKey loreKey = TranslationKey.of("head.test", "lore");

        final Component expectedName = Component.text("Localized Name", Locale.US);
        final TranslatedMessage nameMessage = new TranslatedMessage(expectedName, nameKey);

        final Component loreComponent = Component.text("Line One\nLine Two", Locale.US);
        final TranslatedMessage loreMessage = new TranslatedMessage(loreComponent, loreKey);
        final List<Component> expectedLore = loreMessage.splitLines();

        final RecordingHeadBuilder builder = new RecordingHeadBuilder();

        try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {

            builders.when(UnifiedBuilderFactory::head).thenReturn(builder);

            final TranslationService nameService = Mockito.mock(TranslationService.class);
            final TranslationService loreService = Mockito.mock(TranslationService.class);

            translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
            translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

            Mockito.when(nameService.build()).thenReturn(nameMessage);
            Mockito.when(loreService.build()).thenReturn(loreMessage);

            final ItemStack item = head.getHead(player);
            final SkullMeta meta = (SkullMeta) Objects.requireNonNull(item.getItemMeta());

            assertSame(headUuid, builder.getCapturedUuid());
            assertEquals("base64-texture", builder.getCapturedTexture());
            assertEquals(expectedName, builder.getCapturedName());
            assertEquals(expectedLore, builder.getCapturedLore());

            final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
            assertEquals(plain.serialize(expectedName), plain.serialize(meta.displayName()));

            final List<String> actualLore = new ArrayList<>();
            if (meta.lore() != null) {
                meta.lore().forEach(component -> actualLore.add(plain.serialize(component)));
            }
            final List<String> expectedLorePlain = expectedLore.stream()
                    .map(plain::serialize)
                    .toList();
            assertEquals(expectedLorePlain, actualLore);
        }
    }

    private static final class TestHead extends RHead {

        private TestHead(final String identifier, final String uuid, final String texture) {
            super(identifier, uuid, texture);
        }
    }

    private static final class SimpleCustomHead extends CustomHead {

        private SimpleCustomHead(
                final String identifier,
                final UUID textureUuid,
                final String textureValue,
                final HeadCategory category
        ) {
            super(identifier, textureUuid, textureValue, category);
        }

        private SimpleCustomHead(
                final String identifier,
                final String textureUuid,
                final String textureValue,
                final HeadCategory category
        ) {
            super(identifier, textureUuid, textureValue, category);
        }

        private SimpleCustomHead(
                final String identifier,
                final String textureUuid,
                final String textureValue
        ) {
            super(identifier, textureUuid, textureValue);
        }

        @Override
        public @NotNull ItemStack createItem() {
            return new ItemStack(Material.PLAYER_HEAD);
        }
    }

    private static final class RecordingHeadBuilder implements IHeadBuilder<RecordingHeadBuilder> {

        private UUID capturedUuid;
        private String capturedTexture;
        private Component capturedName;
        private final List<Component> capturedLore = new ArrayList<>();

        @Override
        public RecordingHeadBuilder setPlayerHead(final Player player) {
            return this;
        }

        @Override
        public RecordingHeadBuilder setPlayerHead(final OfflinePlayer offlinePlayer) {
            return this;
        }

        @Override
        public RecordingHeadBuilder setCustomTexture(final @NotNull UUID uuid, final @NotNull String textures) {
            this.capturedUuid = uuid;
            this.capturedTexture = textures;
            return this;
        }

        @Override
        public RecordingHeadBuilder setName(final @NotNull Component name) {
            this.capturedName = name;
            return this;
        }

        @Override
        public RecordingHeadBuilder setLore(final @NotNull List<Component> lore) {
            this.capturedLore.clear();
            this.capturedLore.addAll(lore);
            return this;
        }

        @Override
        public RecordingHeadBuilder addLoreLine(final @NotNull Component line) {
            this.capturedLore.add(line);
            return this;
        }

        @Override
        public RecordingHeadBuilder addLoreLines(final @NotNull List<Component> lore) {
            this.capturedLore.addAll(lore);
            return this;
        }

        @Override
        public RecordingHeadBuilder addLoreLines(final @NotNull Component... lore) {
            this.capturedLore.addAll(List.of(lore));
            return this;
        }

        @Override
        public RecordingHeadBuilder setAmount(final int amount) {
            return this;
        }

        @Override
        public RecordingHeadBuilder setCustomModelData(final int data) {
            return this;
        }

        @Override
        public RecordingHeadBuilder addEnchantment(final @NotNull Enchantment enchantment, final int level) {
            return this;
        }

        @Override
        public RecordingHeadBuilder addItemFlags(final @NotNull ItemFlag... flags) {
            return this;
        }

        @Override
        public RecordingHeadBuilder setGlowing(final boolean glowing) {
            return this;
        }

        @Override
        public ItemStack build() {
            final ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
            final SkullMeta meta = (SkullMeta) Objects.requireNonNull(stack.getItemMeta());
            if (this.capturedName != null) {
                meta.displayName(this.capturedName);
            }
            meta.lore(new ArrayList<>(this.capturedLore));
            stack.setItemMeta(meta);
            return stack;
        }

        private UUID getCapturedUuid() {
            return this.capturedUuid;
        }

        private String getCapturedTexture() {
            return this.capturedTexture;
        }

        private Component getCapturedName() {
            return this.capturedName;
        }

        private List<Component> getCapturedLore() {
            return List.copyOf(this.capturedLore);
        }
    }
}
