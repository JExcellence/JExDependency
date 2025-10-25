package de.jexcellence.economy.currency.anvil;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.currency.anvil.CurrencySuffixAnvilView;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueFactory;
import me.devnatan.inventoryframework.state.StateValueHost;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CurrencySuffixAnvilViewTest {

    private ServerMock server;
    private PlayerMock player;
    private TestCurrencySuffixAnvilView view;
    private StubState<JExEconomyImpl> pluginState;
    private StubState<Currency> currencyState;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        player = server.addPlayer();
        view = new TestCurrencySuffixAnvilView();
        pluginState = injectState("jexEconomy");
        currencyState = injectState("targetCurrency");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void processInputUpdatesExistingCurrencyAndPlaceholders() {
        Currency existing = new Currency("Gold: ", " coins", "gold", "G", Material.GOLD_INGOT);
        Context processingContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        currencyState.put((StateValueHost) processingContext, existing);

        Object result = view.processInput(" bars", processingContext);

        assertSame(existing, result);
        assertEquals(" bars", existing.getSuffix());

        OpenContext openContext = mock(OpenContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        currencyState.put((StateValueHost) openContext, existing);

        Map<String, Object> placeholders = view.getTitlePlaceholders(openContext);
        assertEquals(" bars", placeholders.get("current_suffix"));
        assertEquals("gold", placeholders.get("identifier"));
        assertEquals(" bars", view.getInitialInputText(openContext));
    }

    @Test
    void processInputCreatesNewCurrencyWhenMissing() {
        Context processingContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));

        Object result = view.processInput(" tokens", processingContext);

        assertTrue(result instanceof Currency);
        Currency created = (Currency) result;
        assertEquals("", created.getPrefix());
        assertEquals(" tokens", created.getSuffix());
        assertEquals("", created.getIdentifier());
        assertEquals("", created.getSymbol());
        assertEquals(Material.GOLD_INGOT, created.getIcon());
    }

    @Test
    void prepareResultDataIncludesParentStatePayload() {
        JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        Context context = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        Currency currency = new Currency("", "", "emerald", "E", Material.EMERALD);
        currencyState.put((StateValueHost) context, currency);
        pluginState.put((StateValueHost) context, plugin);

        Map<String, Object> data = view.prepare(currency, " emerald", context);

        assertEquals(plugin, data.get("plugin"));
        assertEquals(currency, data.get("currency"));
        assertEquals(currency, data.get("result"));
    }

    @Test
    void onValidationFailedUsesLengthSpecificKey() {
        Context context = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        Mockito.when(context.getPlayer()).thenReturn(player);
        String invalidInput = "a".repeat(20);

        TranslationService service = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            translations.when(() -> TranslationService.create(
                    TranslationKey.of("currency_suffix_anvil_ui", "error.invalid_input.too_long"),
                    player
            )).thenReturn(service);

            view.fail(invalidInput, context);

            ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(service).withAll(placeholderCaptor.capture());
            Map<String, Object> placeholders = placeholderCaptor.getValue();
            assertEquals(invalidInput, placeholders.get("input"));
            assertEquals(invalidInput.length(), placeholders.get("current_length"));
            assertEquals(16, placeholders.get("max_length"));
        }
    }

    @Test
    void isValidInputAllowsUnicodeWithinLimit() {
        Context context = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        assertTrue(view.validate("⚡💎", context));
    }

    @Test
    void isValidInputRejectsTooLongSuffix() {
        Context context = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        assertFalse(view.validate("x".repeat(32), context));
    }

    private <T> StubState<T> injectState(String fieldName) throws Exception {
        Field field = CurrencySuffixAnvilView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        StubState<T> stub = new StubState<>();
        field.set(view, stub);
        return stub;
    }

    private static final class TestCurrencySuffixAnvilView extends CurrencySuffixAnvilView {

        Map<String, Object> prepare(Object result, String input, Context context) {
            return super.prepareResultData(result, input, context);
        }

        boolean validate(String input, Context context) {
            return super.isValidInput(input, context);
        }

        void fail(String input, Context context) {
            super.onValidationFailed(input, context);
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
