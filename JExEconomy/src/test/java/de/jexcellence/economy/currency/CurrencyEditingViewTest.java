package de.jexcellence.economy.currency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueFactory;
import me.devnatan.inventoryframework.state.StateValueHost;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;

class CurrencyEditingViewTest {

    private ServerMock server;
    private PlayerMock player;
    private TestCurrencyEditingView view;
    private StubState<JExEconomyImpl> pluginState;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
        this.view = new TestCurrencyEditingView();
        this.pluginState = new StubState<>();
        injectPluginState();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getAsyncPaginationSourceRequestsLimitsAndCompletesWithRepositoryResult() {
        Context context = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        CurrencyRepository repository = Mockito.mock(CurrencyRepository.class);
        CompletableFuture<List<Currency>> repositoryFuture = new CompletableFuture<>();

        pluginState.put((StateValueHost) context, plugin);

        Mockito.when(plugin.getCurrencyRepository()).thenReturn(repository);
        Mockito.when(repository.findAllAsync(1, 128)).thenReturn(repositoryFuture);

        CompletableFuture<List<Currency>> resultFuture = view.load(context);

        Mockito.verify(repository).findAllAsync(1, 128);
        assertSame(repositoryFuture, resultFuture);

        Currency currency = new Currency("$", " coins", "gold", "G", Material.GOLD_INGOT);
        List<Currency> expected = List.of(currency);
        repositoryFuture.complete(expected);

        assertEquals(expected, resultFuture.join());
    }

    @Test
    void renderEntryBuildsMetadataAndNavigatesToPropertiesView() {
        RenderContext renderContext = mock(RenderContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        Mockito.when(renderContext.getPlayer()).thenReturn(this.player);
        Mockito.when(renderContext.getInitialData()).thenReturn(Map.of("previous", "data"));

        JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        pluginState.put((StateValueHost) renderContext, plugin);

        Currency currency = new Currency("$", " coins", "gold", "G", Material.GOLD_INGOT);

        TranslationService nameService = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        TranslatedMessage nameMessage = Mockito.mock(TranslatedMessage.class);
        Mockito.when(nameService.build()).thenReturn(nameMessage);
        Mockito.when(nameMessage.component()).thenReturn(text("Gold Currency"));

        TranslationService loreService = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        TranslatedMessage loreMessage = Mockito.mock(TranslatedMessage.class);
        Mockito.when(loreService.build()).thenReturn(loreMessage);
        Mockito.when(loreMessage.splitLines()).thenReturn(List.of(text("Line 1"), text("Line 2")));

        TranslationKey nameKey = TranslationKey.of("currency_editing_ui", "currency.name");
        TranslationKey loreKey = TranslationKey.of("currency_editing_ui", "currency.lore");

        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            translations.when(() -> TranslationService.create(nameKey, this.player)).thenReturn(nameService);
            translations.when(() -> TranslationService.create(loreKey, this.player)).thenReturn(loreService);

            BukkitItemComponentBuilder itemBuilder = Mockito.mock(BukkitItemComponentBuilder.class);
            AtomicReference<ItemStack> capturedItem = new AtomicReference<>();

            Mockito.when(itemBuilder.withItem(any(ItemStack.class))).thenAnswer(invocation -> {
                ItemStack item = invocation.getArgument(0);
                capturedItem.set(item);
                return itemBuilder;
            });

            ArgumentCaptor<Consumer<SlotClickContext>> clickCaptor = ArgumentCaptor.forClass(Consumer.class);
            Mockito.when(itemBuilder.onClick(clickCaptor.capture())).thenReturn(itemBuilder);

            view.render(renderContext, itemBuilder, 0, currency);

            Mockito.verify(nameService).with("currency_identifier", currency.getIdentifier());
            Mockito.verify(nameService).with("currency_symbol", currency.getSymbol());

            ArgumentCaptor<Map<String, Object>> lorePlaceholders = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(loreService).withAll(lorePlaceholders.capture());
            Map<String, Object> placeholderMap = lorePlaceholders.getValue();
            assertEquals(currency.getIdentifier(), placeholderMap.get("currency_identifier"));
            assertEquals(currency.getSymbol(), placeholderMap.get("currency_symbol"));
            assertEquals(currency.getPrefix(), placeholderMap.get("currency_prefix"));
            assertEquals(currency.getSuffix(), placeholderMap.get("currency_suffix"));
            assertEquals(1, placeholderMap.get("index"));

            ItemStack renderedItem = Objects.requireNonNull(capturedItem.get(), "Item stack should be captured");
            ItemMeta meta = renderedItem.getItemMeta();
            assertNotNull(meta);
            assertEquals(text("Gold Currency"), meta.displayName());
            assertEquals(List.of(text("Line 1"), text("Line 2")), meta.lore());
            assertTrue(meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES));

            SlotClickContext clickContext = mock(SlotClickContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
            Mockito.when(clickContext.getInitialData()).thenReturn(Map.of("previous", "data"));
            pluginState.put((StateValueHost) clickContext, plugin);

            Consumer<SlotClickContext> handler = clickCaptor.getValue();
            assertNotNull(handler);

            handler.accept(clickContext);

            ArgumentCaptor<Map<String, Object>> navigationData = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(clickContext).openForPlayer(eq(CurrencyPropertiesEditingView.class), navigationData.capture());
            Map<String, Object> capturedData = navigationData.getValue();
            assertEquals(plugin, capturedData.get("plugin"));
            assertEquals(currency, capturedData.get("currency"));
            assertEquals(Map.of("previous", "data"), capturedData.get("initialData"));
        }
    }

    private void injectPluginState() throws Exception {
        Field field = CurrencyEditingView.class.getDeclaredField("jexEconomy");
        field.setAccessible(true);
        field.set(this.view, this.pluginState);
    }

    private static final class TestCurrencyEditingView extends CurrencyEditingView {

        CompletableFuture<List<Currency>> load(Context context) {
            return super.getAsyncPaginationSource(context);
        }

        void render(RenderContext context, BukkitItemComponentBuilder builder, int index, Currency currency) {
            super.renderEntry(context, builder, index, currency);
        }
    }

    private static final class StubState<T> implements State<T> {

        private static final StateValueFactory NOOP_FACTORY = new StateValueFactory() {
            @Override
            public StateValue create(@NotNull StateValueHost host, @NotNull State<?> state) {
                throw new UnsupportedOperationException("Not required for test stubs.");
            }
        };

        private final Map<StateValueHost, T> values = new IdentityHashMap<>();

        void put(StateValueHost host, T value) {
            values.put(host, value);
        }

        @Override
        public T get(@NotNull StateValueHost host) {
            return values.get(host);
        }

        @Override
        public StateValueFactory factory() {
            return NOOP_FACTORY;
        }

        @Override
        public long internalId() {
            return 0L;
        }
    }
}
