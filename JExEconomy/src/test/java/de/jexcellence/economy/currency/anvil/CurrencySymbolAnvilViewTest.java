package de.jexcellence.economy.currency.anvil;

import de.jexcellence.economy.database.entity.Currency;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencySymbolAnvilViewTest {

    private CurrencySymbolAnvilView view;
    private State<Currency> targetCurrency;

    @BeforeEach
    void setUp() throws Exception {
        this.view = new CurrencySymbolAnvilView();
        this.targetCurrency = Mockito.mock(State.class);

        final Field field = CurrencySymbolAnvilView.class.getDeclaredField("targetCurrency");
        field.setAccessible(true);
        field.set(this.view, this.targetCurrency);
    }

    @Test
    void processInputUpdatesExistingCurrencySymbol() {
        final Context context = Mockito.mock(Context.class);
        final Currency existingCurrency = new Currency("", "", "credits", "C", Material.EMERALD);

        Mockito.when(this.targetCurrency.get(context)).thenReturn(existingCurrency);

        final Object result = this.view.processInput("♦", context);

        assertSame(existingCurrency, result, "Processing should return the existing currency instance");
        assertEquals("♦", existingCurrency.getSymbol(), "Existing currency must receive the new symbol");
    }

    @Test
    void processInputCreatesCurrencyWhenNoneIsPresent() {
        final Context context = Mockito.mock(Context.class);
        Mockito.when(this.targetCurrency.get(context)).thenReturn(null);

        final Object result = this.view.processInput("⛁", context);

        assertTrue(result instanceof Currency, "Processing should create a currency when none is present");

        final Currency createdCurrency = (Currency) result;
        assertEquals("", createdCurrency.getIdentifier(), "New currencies should start with an empty identifier");
        assertEquals(Material.GOLD_INGOT, createdCurrency.getIcon(), "New currencies default to a gold ingot icon");
        assertEquals("⛁", createdCurrency.getSymbol(), "Created currency must use the provided symbol");
    }

    @Test
    void getInitialInputTextReturnsExistingSymbol() {
        final OpenContext openContext = Mockito.mock(OpenContext.class);
        final Currency existingCurrency = new Currency("", "", "gems", "💎", Material.DIAMOND);

        Mockito.when(this.targetCurrency.get(openContext)).thenReturn(existingCurrency);

        final String initialInput = this.view.getInitialInputText(openContext);

        assertEquals("💎", initialInput, "Initial input should reflect the current currency symbol");
    }

    @Test
    void getInitialInputTextFallsBackToEmptyWhenCurrencyMissing() {
        final OpenContext openContext = Mockito.mock(OpenContext.class);
        Mockito.when(this.targetCurrency.get(openContext)).thenReturn(null);

        final String initialInput = this.view.getInitialInputText(openContext);

        assertEquals("", initialInput, "Initial input should be empty when no currency is provided");
    }

    @Test
    void isValidInputHonorsLengthAndBlankConstraints() {
        final Context context = Mockito.mock(Context.class);

        assertTrue(this.view.isValidInput("Gold", context), "Symbols within the limit should be accepted");
        assertTrue(this.view.isValidInput("⚡", context), "Unicode symbols should be accepted within the limit");

        assertFalse(this.view.isValidInput("", context), "Empty strings must be rejected");
        assertFalse(this.view.isValidInput("   ", context), "Whitespace-only symbols must be rejected");
        assertFalse(this.view.isValidInput("123456", context), "Symbols longer than five characters must be rejected");
    }
}
