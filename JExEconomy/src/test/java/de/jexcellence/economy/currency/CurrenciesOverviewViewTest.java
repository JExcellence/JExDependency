package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.LayoutSlot;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrenciesOverviewViewTest {

    private CurrenciesOverviewView view;
    private State<JExEconomyImpl> pluginState;
    private JExEconomyImpl plugin;
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() throws Exception {
        this.view = new CurrenciesOverviewView();
        this.pluginState = Mockito.mock(State.class);
        this.plugin = Mockito.mock(JExEconomyImpl.class);
        this.currencyRepository = Mockito.mock(CurrencyRepository.class);

        when(this.pluginState.get(any())).thenReturn(this.plugin);
        when(this.plugin.getCurrencyRepository()).thenReturn(this.currencyRepository);

        this.injectPluginState(this.pluginState);
    }

    @Test
    void getAsyncPaginationSourceFetchesCurrenciesFromRepository() {
        final Context context = Mockito.mock(Context.class);
        final List<Currency> currencies = List.of(
                new Currency("$", " coins", "gold", "G", Material.GOLD_INGOT),
                new Currency("", "", "silver", "S", Material.IRON_INGOT)
        );
        final CompletableFuture<List<Currency>> repositoryResult = CompletableFuture.completedFuture(currencies);

        when(this.currencyRepository.findAllAsync(1, 128)).thenReturn(repositoryResult);

        final CompletableFuture<List<Currency>> result = this.view.getAsyncPaginationSource(context);

        assertSame(currencies, result.join());
        verify(this.pluginState).get(context);
        verify(this.currencyRepository).findAllAsync(1, 128);
    }

    @Test
    void onFirstRenderConfiguresNavigationAndSummaryIndicator() throws Exception {
        final RenderContext renderContext = Mockito.mock(RenderContext.class);
        final Player player = Mockito.mock(Player.class);
        when(renderContext.getPlayer()).thenReturn(player);

        final Pagination pagination = Mockito.mock(Pagination.class);
        final State<Pagination> paginationState = Mockito.mock(State.class);
        when(paginationState.get(renderContext)).thenReturn(pagination);

        this.injectPaginationState(paginationState);

        when(pagination.canBack()).thenReturn(true);
        when(pagination.canAdvance()).thenReturn(true);
        when(pagination.currentPageIndex()).thenReturn(1);
        when(pagination.lastPageIndex()).thenReturn(3);
        when(pagination.source()).thenReturn(List.of(new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object(), new Object()));

        final BukkitItemComponentBuilder previousBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder nextBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder indicatorBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);

        when(renderContext.layoutSlot(eq(this.view.getPreviousButtonChar()), any(ItemStack.class))).thenReturn(previousBuilder);
        when(renderContext.layoutSlot(eq(this.view.getNextButtonChar()), any(ItemStack.class))).thenReturn(nextBuilder);
        when(renderContext.layoutSlot(eq(this.view.getPageIndicatorChar()), any(ItemStack.class))).thenReturn(indicatorBuilder);

        final LayoutSlot indicatorSlot = Mockito.mock(LayoutSlot.class);
        when(indicatorSlot.getCharacter()).thenReturn(this.view.getPageIndicatorChar());
        when(renderContext.getLayoutSlots()).thenReturn(List.of(indicatorSlot));

        try (TranslationMockContext translations = this.mockTranslations();
             MockedStatic<UnifiedBuilderFactory> builders = this.mockUnifiedBuilders()) {

            this.view.onFirstRender(renderContext, player);

            verify(previousBuilder).updateOnStateChange(paginationState);
            verify(nextBuilder).updateOnStateChange(paginationState);
            verify(indicatorBuilder).updateOnStateChange(paginationState);

            final ArgumentCaptor<BooleanSupplier> previousVisibility = ArgumentCaptor.forClass(BooleanSupplier.class);
            final ArgumentCaptor<BooleanSupplier> nextVisibility = ArgumentCaptor.forClass(BooleanSupplier.class);
            verify(previousBuilder).displayIf(previousVisibility.capture());
            verify(nextBuilder).displayIf(nextVisibility.capture());

            assertTrue(previousVisibility.getValue().getAsBoolean());
            assertTrue(nextVisibility.getValue().getAsBoolean());

            final ArgumentCaptor<Runnable> previousCallback = ArgumentCaptor.forClass(Runnable.class);
            final ArgumentCaptor<Runnable> nextCallback = ArgumentCaptor.forClass(Runnable.class);
            verify(previousBuilder).onClick(previousCallback.capture());
            verify(nextBuilder).onClick(nextCallback.capture());

            previousCallback.getValue().run();
            nextCallback.getValue().run();

            verify(pagination).back();
            verify(pagination).advance();

            final TranslationCapture pageLore = translations.findByKey("page.lore");
            assertNotNull(pageLore);
            final Map<String, Object> summaryPlaceholders = pageLore.bulkPlaceholders();
            assertEquals(2, summaryPlaceholders.get("page"));
            assertEquals(4, summaryPlaceholders.get("max_page"));
            assertEquals(1, summaryPlaceholders.get("first_page"));
            assertEquals(12, summaryPlaceholders.get("items_count"));
        }
    }

    @Test
    void renderEntryBuildsSummaryItemAndConfiguresDetailNavigation() {
        final Context context = Mockito.mock(Context.class);
        final Player player = Mockito.mock(Player.class);
        when(context.getPlayer()).thenReturn(player);

        final BukkitItemComponentBuilder itemBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final Currency currency = new Currency("<", ">", "emerald", "✦", Material.EMERALD);

        try (TranslationMockContext translations = this.mockTranslations();
             MockedStatic<UnifiedBuilderFactory> builders = this.mockUnifiedBuilders()) {

            this.view.renderEntry(context, itemBuilder, 0, currency);

            final ArgumentCaptor<ItemStack> renderedItem = ArgumentCaptor.forClass(ItemStack.class);
            verify(itemBuilder).withItem(renderedItem.capture());
            assertEquals(Material.EMERALD, renderedItem.getValue().getType());
            verify(itemBuilder).addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            final TranslationCapture nameCapture = translations.findByKey("currencies_overview_ui.currency.name");
            assertNotNull(nameCapture);
            assertEquals("emerald", nameCapture.singlePlaceholders().get("currency_identifier"));
            assertEquals("✦", nameCapture.singlePlaceholders().get("currency_symbol"));

            final TranslationCapture loreCapture = translations.findByKey("currencies_overview_ui.currency.lore");
            assertNotNull(loreCapture);
            final Map<String, Object> lorePlaceholders = loreCapture.bulkPlaceholders();
            assertEquals("emerald", lorePlaceholders.get("currency_identifier"));
            assertEquals("✦", lorePlaceholders.get("currency_symbol"));
            assertEquals("<", lorePlaceholders.get("currency_prefix"));
            assertEquals(">", lorePlaceholders.get("currency_suffix"));
            assertEquals(1, lorePlaceholders.get("index"));

            final ArgumentCaptor<Consumer<SlotClickContext>> clickHandler = ArgumentCaptor.forClass(Consumer.class);
            verify(itemBuilder).onClick(clickHandler.capture());

            final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
            final Map<String, Object> initialData = Map.of("origin", "overview");
            when(clickContext.getInitialData()).thenReturn(initialData);
            when(this.pluginState.get(clickContext)).thenReturn(this.plugin);

            clickHandler.getValue().accept(clickContext);

            final ArgumentCaptor<Map<String, Object>> navigationData = ArgumentCaptor.forClass(Map.class);
            verify(clickContext).openForPlayer(eq(CurrencyDetailView.class), navigationData.capture());

            final Map<String, Object> providedData = navigationData.getValue();
            assertSame(this.plugin, providedData.get("plugin"));
            assertSame(currency, providedData.get("currency"));
            assertSame(initialData, providedData.get("initialData"));
        }
    }

    private void injectPluginState(final State<JExEconomyImpl> state) throws ReflectiveOperationException {
        final Field field = CurrenciesOverviewView.class.getDeclaredField("jexEconomy");
        field.setAccessible(true);
        field.set(this.view, state);
    }

    private void injectPaginationState(final State<Pagination> paginationState) throws ReflectiveOperationException {
        final Field field = APaginatedView.class.getDeclaredField("pagination");
        field.setAccessible(true);
        field.set(this.view, paginationState);
    }

    private TranslationMockContext mockTranslations() {
        final List<TranslationCapture> captures = new ArrayList<>();
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        translations.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any(Player.class)))
                .thenAnswer(invocation -> this.createTranslationBuilder(invocation.getArgument(0), captures));
        return new TranslationMockContext(translations, captures);
    }

    private TranslationService createTranslationBuilder(
            final TranslationKey key,
            final List<TranslationCapture> captures
    ) {
        final TranslationService translation = Mockito.mock(TranslationService.class);
        final TranslationCapture capture = new TranslationCapture(key);
        captures.add(capture);

        Mockito.when(translation.with(Mockito.anyString(), Mockito.any())).thenAnswer(invocation -> {
            capture.recordSinglePlaceholder(invocation.getArgument(0), invocation.getArgument(1));
            return translation;
        });
        Mockito.when(translation.withAll(Mockito.anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            final Map<String, Object> placeholders = new LinkedHashMap<>((Map<String, Object>) invocation.getArgument(0));
            capture.recordBulkPlaceholders(placeholders);
            return translation;
        });
        Mockito.when(translation.withPrefix()).thenReturn(translation);

        final TranslatedMessage message = new TranslatedMessage(Component.text(key.key()), key);
        Mockito.when(translation.build()).thenReturn(message);
        Mockito.when(translation.buildAsync()).thenReturn(CompletableFuture.completedFuture(message));
        Mockito.doNothing().when(translation).send();

        return translation;
    }

    private MockedStatic<UnifiedBuilderFactory> mockUnifiedBuilders() {
        final MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
        builders.when(UnifiedBuilderFactory::head).thenReturn(new StubHeadBuilder());
        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(Material.class)))
                .thenAnswer(invocation -> this.createItemBuilder(new ItemStack(invocation.getArgument(0))));
        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(ItemStack.class)))
                .thenAnswer(invocation -> this.createItemBuilder(((ItemStack) invocation.getArgument(0)).clone()));
        return builders;
    }

    private IUnifiedItemBuilder<?, ?> createItemBuilder(final ItemStack baseItem) {
        @SuppressWarnings("unchecked")
        final IUnifiedItemBuilder<?, ?> builder = Mockito.mock(IUnifiedItemBuilder.class);
        Mockito.when(builder.setName(Mockito.any(Component.class))).thenReturn(builder);
        Mockito.when(builder.setLore(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.addItemFlags(Mockito.<ItemFlag[]>any())).thenReturn(builder);
        Mockito.when(builder.addLoreLine(Mockito.any(Component.class))).thenReturn(builder);
        Mockito.when(builder.addLoreLines(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.addLoreLines(Mockito.any(Component[].class))).thenReturn(builder);
        Mockito.when(builder.addEnchantment(Mockito.any(), Mockito.anyInt())).thenReturn(builder);
        Mockito.when(builder.setAmount(Mockito.anyInt())).thenReturn(builder);
        Mockito.when(builder.setCustomModelData(Mockito.anyInt())).thenReturn(builder);
        Mockito.when(builder.setGlowing(Mockito.anyBoolean())).thenReturn(builder);
        Mockito.when(builder.build()).thenAnswer(invocation -> baseItem.clone());
        return builder;
    }

    private static final class TranslationCapture {
        private final TranslationKey key;
        private final Map<String, Object> singlePlaceholders = new LinkedHashMap<>();
        private Map<String, Object> bulkPlaceholders = Map.of();

        private TranslationCapture(final TranslationKey key) {
            this.key = key;
        }

        private void recordSinglePlaceholder(final String name, final Object value) {
            this.singlePlaceholders.put(name, value);
        }

        private void recordBulkPlaceholders(final Map<String, Object> placeholders) {
            this.bulkPlaceholders = placeholders;
        }

        private TranslationKey key() {
            return this.key;
        }

        private Map<String, Object> singlePlaceholders() {
            return this.singlePlaceholders;
        }

        private Map<String, Object> bulkPlaceholders() {
            return this.bulkPlaceholders;
        }
    }

    private static final class TranslationMockContext implements AutoCloseable {
        private final MockedStatic<TranslationService> translations;
        private final List<TranslationCapture> captures;

        private TranslationMockContext(
                final MockedStatic<TranslationService> translations,
                final List<TranslationCapture> captures
        ) {
            this.translations = translations;
            this.captures = captures;
        }

        private TranslationCapture findByKey(final String key) {
            return this.captures.stream()
                    .filter(capture -> capture.key().key().equals(key))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void close() {
            this.translations.close();
        }
    }

    private static final class StubHeadBuilder implements com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder<StubHeadBuilder> {
        private final ItemStack itemStack = new ItemStack(Material.PAPER);

        @Override
        public StubHeadBuilder setPlayerHead(final Player player) {
            return this;
        }

        @Override
        public StubHeadBuilder setPlayerHead(final org.bukkit.OfflinePlayer offlinePlayer) {
            return this;
        }

        @Override
        public StubHeadBuilder setCustomTexture(final @NotNull java.util.UUID uuid, final @NotNull String textures) {
            return this;
        }

        @Override
        public StubHeadBuilder setName(final @NotNull Component name) {
            return this;
        }

        @Override
        public StubHeadBuilder setLore(final @NotNull List<Component> lore) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLine(final @NotNull Component line) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLines(final @NotNull List<Component> lore) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLines(final @NotNull Component... lore) {
            return this;
        }

        @Override
        public StubHeadBuilder setAmount(final int amount) {
            return this;
        }

        @Override
        public StubHeadBuilder setCustomModelData(final int data) {
            return this;
        }

        @Override
        public StubHeadBuilder addEnchantment(final @NotNull org.bukkit.enchantments.Enchantment enchantment, final int level) {
            return this;
        }

        @Override
        public StubHeadBuilder addItemFlags(final @NotNull ItemFlag... flags) {
            return this;
        }

        @Override
        public StubHeadBuilder setGlowing(final boolean glowing) {
            return this;
        }

        @Override
        public ItemStack build() {
            return this.itemStack.clone();
        }
    }
}
