package de.jexcellence.economy.currency.anvil;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.Context;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyIconAnvilViewTest {

    private ServerMock server;
    private PlayerMock player;
    private TestCurrencyIconAnvilView view;
    private StubState<Currency> currencyState;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
        this.view = new TestCurrencyIconAnvilView();
        injectState("jexEconomy");
        this.currencyState = injectState("targetCurrency");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void processInputUpdatesExistingCurrencyIcon() {
        final Currency existingCurrency = new Currency("", "", "vault", "", Material.GOLD_INGOT);
        final Context processingContext = Mockito.mock(
                Context.class,
                Mockito.withSettings().extraInterfaces(StateValueHost.class)
        );
        this.currencyState.put((StateValueHost) processingContext, existingCurrency);

        final Object result = this.view.processInput("diamond", processingContext);

        assertSame(existingCurrency, result);
        assertEquals(Material.DIAMOND, existingCurrency.getIcon());
    }

    @Test
    void processInputThrowsForInvalidMaterialName() {
        final Context processingContext = Mockito.mock(
                Context.class,
                Mockito.withSettings().extraInterfaces(StateValueHost.class)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> this.view.processInput("not_a_material", processingContext)
        );
    }

    @Test
    void isValidInputRejectsUnknownMaterialNames() {
        final Context validationContext = Mockito.mock(
                Context.class,
                Mockito.withSettings().extraInterfaces(StateValueHost.class)
        );

        assertFalse(this.view.validate("unknown_item", validationContext));
    }

    @Test
    void onValidationFailedUsesInvalidMaterialKey() {
        final Context validationContext = Mockito.mock(
                Context.class,
                Mockito.withSettings().extraInterfaces(StateValueHost.class)
        );
        Mockito.when(validationContext.getPlayer()).thenReturn(this.player);

        final TranslationService translationService = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);

        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            translations.when(() -> TranslationService.create(
                    TranslationKey.of(
                            "currency_icon_anvil_ui",
                            "error.invalid_input.invalid_material"
                    ),
                    this.player
            )).thenReturn(translationService);

            this.view.fail("mystic_shard", validationContext);

            final ArgumentCaptor<Map<String, Object>> placeholderCaptor = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(translationService).withAll(placeholderCaptor.capture());

            final Map<String, Object> placeholders = placeholderCaptor.getValue();
            assertEquals("mystic_shard", placeholders.get("input"));
            assertEquals(
                    "GOLD_INGOT, DIAMOND, EMERALD, IRON_INGOT",
                    placeholders.get("example_materials")
            );
        }
    }

    private <T> StubState<T> injectState(final String fieldName) throws Exception {
        final Field field = CurrencyIconAnvilView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final StubState<T> stubState = new StubState<>();
        field.set(this.view, stubState);
        return stubState;
    }

    private static final class TestCurrencyIconAnvilView extends CurrencyIconAnvilView {

        boolean validate(final String input, final Context context) {
            return super.isValidInput(input, context);
        }

        void fail(final String input, final Context context) {
            super.onValidationFailed(input, context);
        }
    }

    private static final class StubState<T> implements State<T> {

        private static final StateValueFactory NOOP_FACTORY = new StateValueFactory() {
            @Override
            public StateValue create(@NotNull final StateValueHost host, @NotNull final State<?> state) {
                throw new UnsupportedOperationException("Not required for test stubs.");
            }
        };

        private final Map<StateValueHost, T> values = new IdentityHashMap<>();

        void put(final StateValueHost host, final T value) {
            this.values.put(host, value);
        }

        @Override
        public T get(final @NotNull StateValueHost host) {
            return this.values.get(host);
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
