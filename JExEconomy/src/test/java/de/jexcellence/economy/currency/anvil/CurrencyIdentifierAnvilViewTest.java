package de.jexcellence.economy.currency.anvil;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrencyIdentifierAnvilViewTest {

    private ServerMock server;
    private CurrencyIdentifierAnvilView view;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.view = new CurrencyIdentifierAnvilView();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void processInputCreatesNewCurrencyWhenNonePresent() throws ReflectiveOperationException {
        final Context context = mock(Context.class);
        final State<Currency> currencyState = mock(State.class);
        injectState("targetCurrency", currencyState);

        when(currencyState.get(context)).thenReturn(null);

        final Object result = this.view.processInput("new_currency", context);

        assertNotNull(result);
        assertInstanceOf(Currency.class, result);

        final Currency createdCurrency = (Currency) result;
        assertEquals("new_currency", createdCurrency.getIdentifier());
        assertEquals("", createdCurrency.getPrefix());
        assertEquals("", createdCurrency.getSuffix());
        assertEquals("", createdCurrency.getSymbol());
        assertEquals(Material.GOLD_INGOT, createdCurrency.getIcon());
    }

    @Test
    void processInputUpdatesExistingCurrencyIdentifier() throws ReflectiveOperationException {
        final Context context = mock(Context.class);
        final State<Currency> currencyState = mock(State.class);
        injectState("targetCurrency", currencyState);

        final Currency existingCurrency = new Currency("", "", "legacy", "", Material.GOLD_INGOT);
        when(currencyState.get(context)).thenReturn(existingCurrency);

        final Object result = this.view.processInput("updated_identifier", context);

        assertSame(existingCurrency, result);
        assertEquals("updated_identifier", existingCurrency.getIdentifier());
    }

    @Test
    void validationRejectsDuplicateIdentifierFromPluginState() throws ReflectiveOperationException {
        final Context context = mock(RenderContext.class);
        final State<JExEconomyImpl> pluginState = mock(State.class);
        injectState("jexEconomy", pluginState);

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(1L, new Currency("", "", "existing", "", Material.GOLD_INGOT));
        when(pluginState.get(context)).thenReturn(plugin);
        when(plugin.getCurrencies()).thenReturn(currencies);

        assertFalse(this.view.isValidInput("existing", context));
        assertTrue(this.view.isValidInput("unique_value", context));
    }

    @Test
    void prepareResultDataIncludesPluginAndCurrencyForNavigation() throws ReflectiveOperationException {
        final Context context = mock(Context.class);
        final State<JExEconomyImpl> pluginState = mock(State.class);
        injectState("jexEconomy", pluginState);

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        when(pluginState.get(context)).thenReturn(plugin);

        final Currency updatedCurrency = new Currency("", "", "gold", "", Material.GOLD_INGOT);

        final Map<String, Object> resultData = this.view.prepareResultData(updatedCurrency, "gold", context);

        assertEquals(updatedCurrency, resultData.get("result"));
        assertEquals(plugin, resultData.get("plugin"));
        assertEquals(updatedCurrency, resultData.get("currency"));
    }

    @SuppressWarnings("unchecked")
    private <T> void injectState(final String fieldName, final State<T> state) throws ReflectiveOperationException {
        final Field field = CurrencyIdentifierAnvilView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(this.view, state);
    }
}
