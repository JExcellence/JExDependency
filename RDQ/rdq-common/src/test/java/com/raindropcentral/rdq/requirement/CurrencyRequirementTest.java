package com.raindropcentral.rdq.requirement;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import be.seeseemelk.mockbukkit.plugin.PluginManagerMock;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class CurrencyRequirementTest {

    private ServerMock server;
    private PlayerMock player;
    private Currency currencyA;
    private Currency currencyB;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("CurrencyRequirementTest");
        this.currencyA = mockCurrency("GEMS");
        this.currencyB = mockCurrency("COINS");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldSatisfyRequirementAndConsumeCurrencies() {
        final CurrencyAdapter adapter = Mockito.mock(CurrencyAdapter.class);
        Mockito.when(adapter.getBalance(this.player, this.currencyA))
                .thenReturn(CompletableFuture.completedFuture(75.0));
        Mockito.when(adapter.getBalance(this.player, this.currencyB))
                .thenReturn(CompletableFuture.completedFuture(30.0));
        Mockito.when(adapter.withdraw(this.player, this.currencyA, 50.0))
                .thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(adapter.withdraw(this.player, this.currencyB, 25.0))
                .thenReturn(CompletableFuture.completedFuture(true));

        registerCurrencyPlugin(adapter);

        final Map<Currency, Double> requirements = new HashMap<>();
        requirements.put(this.currencyA, 50.0);
        requirements.put(this.currencyB, 25.0);

        final CurrencyRequirement requirement = new CurrencyRequirement(requirements, null, TimeUnit.SECONDS.toMillis(2));

        assertTrue(requirement.isMetAsync(this.player).join(), "isMetAsync should complete with true when balances exceed targets");
        assertEquals(1.0, requirement.calculateProgressAsync(this.player).join(),
                "calculateProgressAsync should report full progress when all balances satisfy requirements");

        requirement.consumeAsync(this.player).join();
        Mockito.verify(adapter).withdraw(this.player, this.currencyA, 50.0);
        Mockito.verify(adapter).withdraw(this.player, this.currencyB, 25.0);

        assertTrue(requirement.isMet(this.player), "Synchronous isMet should mirror the async result");
        assertEquals(1.0, requirement.calculateProgress(this.player),
                "Synchronous calculateProgress should mirror the async result");

        Mockito.reset(adapter);
        Mockito.when(adapter.withdraw(this.player, this.currencyA, 50.0))
                .thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(adapter.withdraw(this.player, this.currencyB, 25.0))
                .thenReturn(CompletableFuture.completedFuture(true));

        requirement.consume(this.player);
        Mockito.verify(adapter).withdraw(this.player, this.currencyA, 50.0);
        Mockito.verify(adapter).withdraw(this.player, this.currencyB, 25.0);
    }

    @Test
    void shouldReturnDefaultsWhenAdapterUnavailable() {
        registerCurrencyPlugin(null);

        final Map<Currency, Double> requirements = Map.of(this.currencyA, 40.0);
        final CurrencyRequirement requirement = new CurrencyRequirement(requirements);

        assertFalse(requirement.isMetAsync(this.player).join(),
                "isMetAsync should default to false when the currency adapter is missing");
        assertEquals(0.0, requirement.calculateProgressAsync(this.player).join(),
                "calculateProgressAsync should default to 0 when the currency adapter is missing");
        requirement.consumeAsync(this.player).join();

        assertFalse(requirement.isMet(this.player),
                "Synchronous isMet should default to false when the currency adapter is missing");
        assertEquals(0.0, requirement.calculateProgress(this.player),
                "Synchronous calculateProgress should default to 0 when the currency adapter is missing");

        requirement.consume(this.player);
    }

    @Test
    void shouldHandleTimeoutGracefully() {
        final CurrencyAdapter adapter = Mockito.mock(CurrencyAdapter.class);
        final CompletableFuture<Double> slowFuture = new CompletableFuture<>();
        Mockito.when(adapter.getBalance(this.player, this.currencyA)).thenReturn(slowFuture);

        registerCurrencyPlugin(adapter);

        final Map<Currency, Double> requirements = Map.of(this.currencyA, 10.0);
        final CurrencyRequirement requirement = new CurrencyRequirement(requirements, null, 50L);

        assertFalse(requirement.isMetAsync(this.player).join(),
                "isMetAsync should resolve to false when the balance check times out");
        assertEquals(0.0, requirement.calculateProgressAsync(this.player).join(),
                "calculateProgressAsync should resolve to 0 when the balance check times out");

        assertFalse(requirement.isMet(this.player),
                "Synchronous isMet should resolve to false when the balance check times out");
        assertEquals(0.0, requirement.calculateProgress(this.player),
                "Synchronous calculateProgress should resolve to 0 when the balance check times out");
    }

    private void registerCurrencyPlugin(final CurrencyAdapter adapter) {
        final JECurrency plugin = Mockito.mock(JECurrency.class, Mockito.withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        Mockito.when(plugin.getName()).thenReturn("JECurrency");
        Mockito.when(plugin.isEnabled()).thenReturn(true);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("JECurrency"));
        Mockito.when(plugin.getServer()).thenReturn(this.server);
        Mockito.when(plugin.getImpl().getCurrencyAdapter()).thenReturn(adapter);

        final PluginManagerMock pluginManager = this.server.getPluginManager();
        pluginManager.registerMockPlugin(plugin);
    }

    private Currency mockCurrency(final String identifier) {
        final Currency currency = Mockito.mock(Currency.class);
        Mockito.when(currency.getIdentifier()).thenReturn(identifier);
        return currency;
    }
}
