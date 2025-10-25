package de.jexcellence.economy.currency.anvil;

import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueFactory;
import me.devnatan.inventoryframework.state.StateValueHost;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyPrefixAnvilViewTest {

    private TestCurrencyPrefixAnvilView view;
    private StubState<JExEconomyImpl> pluginState;
    private StubState<Currency> currencyState;

    @BeforeEach
    void setUp() throws Exception {
        this.view = new TestCurrencyPrefixAnvilView();
        this.pluginState = injectState("jexEconomy");
        this.currencyState = injectState("targetCurrency");
    }

    @Test
    void processInputUpdatesExistingCurrencyPrefixAndPlaceholders() {
        final Currency existingCurrency = new Currency("Gold: ", " coins", "gold", "G", Material.GOLD_INGOT);
        final Context processingContext = Mockito.mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        this.currencyState.put((StateValueHost) processingContext, existingCurrency);

        final Object result = this.view.processInput("Credits: ", processingContext);

        assertSame(existingCurrency, result, "Processing should return the existing currency instance");
        assertEquals("Credits: ", existingCurrency.getPrefix(), "Existing currency must receive the new prefix");

        final OpenContext openContext = Mockito.mock(OpenContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        this.currencyState.put((StateValueHost) openContext, existingCurrency);

        final Map<String, Object> placeholders = this.view.getTitlePlaceholders(openContext);
        assertEquals("Credits: ", placeholders.get("current_prefix"), "Title placeholder must reflect updated prefix");
        assertEquals("gold", placeholders.get("identifier"), "Identifier placeholder must surface currency identifier");
        assertEquals("Credits: ", this.view.getInitialInputText(openContext), "Initial input should mirror the updated prefix");
    }

    @Test
    void processInputCreatesNewCurrencyWhenMissing() {
        final Context processingContext = Mockito.mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));

        final Object result = this.view.processInput("Tokens: ", processingContext);

        assertTrue(result instanceof Currency, "Processing should create a new currency when none exists");
        final Currency createdCurrency = (Currency) result;
        assertEquals("Tokens: ", createdCurrency.getPrefix(), "Created currency must capture the provided prefix");
        assertEquals("", createdCurrency.getSuffix(), "Created currency should start with an empty suffix");
        assertEquals("", createdCurrency.getIdentifier(), "Created currency should start with an empty identifier");
        assertEquals("", createdCurrency.getSymbol(), "Created currency should start with an empty symbol");
        assertEquals(Material.GOLD_INGOT, createdCurrency.getIcon(), "Created currency should default to the gold ingot icon");
    }

    @Test
    void isValidInputEnforcesLengthAndAllowsUnicode() {
        final Context validationContext = Mockito.mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));

        assertTrue(this.view.validate("Guild: ", validationContext), "Prefixes within sixteen characters must be accepted");
        assertTrue(this.view.validate("⚡ Power: ", validationContext), "Unicode prefixes inside the limit must be accepted");
        assertFalse(this.view.validate("A".repeat(17), validationContext), "Prefixes exceeding sixteen characters must be rejected");
    }

    @Test
    void getTitlePlaceholdersProvideDefaultsWhenCurrencyMissing() {
        final OpenContext openContext = Mockito.mock(OpenContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));

        final Map<String, Object> placeholders = this.view.getTitlePlaceholders(openContext);

        assertEquals("", placeholders.get("current_prefix"), "Missing currencies should use an empty prefix placeholder");
        assertEquals("New Currency", placeholders.get("identifier"), "Missing currencies should display the new currency identifier placeholder");
        assertEquals("", this.view.getInitialInputText(openContext), "Initial input should fall back to the base implementation when no currency is present");
    }

    @Test
    void prepareResultDataIncludesNavigationPayload() {
        final Context context = Mockito.mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        final Currency currency = new Currency("Vault: ", "", "vault", "V", Material.EMERALD);
        final JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        this.pluginState.put((StateValueHost) context, plugin);

        final Map<String, Object> data = this.view.prepare(currency, "Vault: ", context);

        assertEquals(plugin, data.get("plugin"), "Navigation payload should include the plugin instance");
        assertEquals(currency, data.get("currency"), "Navigation payload should include the updated currency");
        assertEquals(currency, data.get("result"), "Result payload should expose the processed currency");
    }

    private <T> StubState<T> injectState(final String fieldName) throws Exception {
        final Field field = CurrencyPrefixAnvilView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked") final StubState<T> state = new StubState<>();
        field.set(this.view, state);
        return state;
    }

    private static final class TestCurrencyPrefixAnvilView extends CurrencyPrefixAnvilView {

        Map<String, Object> prepare(final Object result, final String input, final Context context) {
            return super.prepareResultData(result, input, context);
        }

        boolean validate(final String input, final Context context) {
            return super.isValidInput(input, context);
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
        public T get(@NotNull final StateValueHost host) {
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
