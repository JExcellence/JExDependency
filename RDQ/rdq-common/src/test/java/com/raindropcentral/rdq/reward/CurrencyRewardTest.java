package com.raindropcentral.rdq.reward;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class CurrencyRewardTest {

    private ServerMock server;
    private PlayerMock player;
    private Currency customCurrency;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("CurrencyRewardTest");
        this.customCurrency = mockCurrency("GEMS");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldRejectNonPositiveAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new CurrencyReward(0));
        assertThrows(IllegalArgumentException.class, () -> new CurrencyReward(-10.0, "GEMS", null, 100L));
    }

    @Test
    void shouldDepositConfiguredCurrency() {
        final CurrencyAdapter adapter = Mockito.mock(CurrencyAdapter.class);
        Mockito.when(adapter.deposit(this.player, this.customCurrency, 25.0))
                .thenReturn(CompletableFuture.completedFuture(true));

        registerCurrencyPlugin(adapter, Map.of(1L, this.customCurrency));

        final CurrencyReward reward = new CurrencyReward(25.0, "GEMS", null, 100L);
        final CompletableFuture<Void> future = reward.applyAsync(this.player);

        assertDoesNotThrow(() -> future.join());
        Mockito.verify(adapter).deposit(this.player, this.customCurrency, 25.0);
        assertTrue(future.isDone(), "applyAsync should complete when the deposit succeeds");
    }

    @Test
    void shouldDepositUsingVaultFallback() {
        final CurrencyAdapter adapter = Mockito.mock(CurrencyAdapter.class);
        Mockito.when(adapter.deposit(Mockito.eq(this.player), Mockito.any(Currency.class), Mockito.eq(10.0)))
                .thenReturn(CompletableFuture.completedFuture(true));

        registerCurrencyPlugin(adapter, Map.of());

        final CurrencyReward reward = new CurrencyReward(10.0);
        final CompletableFuture<Void> future = reward.applyAsync(this.player);

        assertDoesNotThrow(() -> future.join());

        final ArgumentCaptor<Currency> currencyCaptor = ArgumentCaptor.forClass(Currency.class);
        Mockito.verify(adapter).deposit(Mockito.eq(this.player), currencyCaptor.capture(), Mockito.eq(10.0));
        assertEquals("VAULT", currencyCaptor.getValue().getIdentifier(),
                "applyAsync should deposit using the VAULT fallback when no identifier is provided");
    }

    @Test
    void shouldCompleteWhenAdapterUnavailable() {
        registerCurrencyPlugin(null, Map.of(1L, this.customCurrency));

        final CurrencyReward reward = new CurrencyReward(5.0, "GEMS", null, 100L);
        final CompletableFuture<Void> future = reward.applyAsync(this.player);

        assertDoesNotThrow(() -> future.join());
        assertTrue(future.isDone(), "applyAsync should complete immediately when the adapter is unavailable");
    }

    @Test
    void shouldCompleteWhenCurrencyCannotBeResolved() {
        final CurrencyAdapter adapter = Mockito.mock(CurrencyAdapter.class);
        registerCurrencyPlugin(adapter, Map.of(1L, this.customCurrency));

        final CurrencyReward reward = new CurrencyReward(15.0, "UNKNOWN", null, 100L);
        final CompletableFuture<Void> future = reward.applyAsync(this.player);

        assertDoesNotThrow(() -> future.join());
        Mockito.verifyNoInteractions(adapter);
    }

    @Test
    void shouldRecoverWhenDepositTimesOut() {
        final CurrencyAdapter adapter = Mockito.mock(CurrencyAdapter.class);
        final CompletableFuture<Boolean> slowFuture = new CompletableFuture<>();
        Mockito.when(adapter.deposit(this.player, this.customCurrency, 30.0)).thenReturn(slowFuture);

        registerCurrencyPlugin(adapter, Map.of(1L, this.customCurrency));

        final CurrencyReward reward = new CurrencyReward(30.0, "GEMS", null, 50L);
        final CompletableFuture<Void> future = reward.applyAsync(this.player);

        assertDoesNotThrow(() -> future.get(500, TimeUnit.MILLISECONDS));
        Mockito.verify(adapter).deposit(this.player, this.customCurrency, 30.0);
    }

    private void registerCurrencyPlugin(final CurrencyAdapter adapter, final Map<Long, Currency> currencies) {
        final JECurrency plugin = Mockito.mock(JECurrency.class, Mockito.withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        Mockito.when(plugin.getName()).thenReturn("JECurrency");
        Mockito.when(plugin.isEnabled()).thenReturn(true);
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("JECurrency"));
        Mockito.when(plugin.getServer()).thenReturn(this.server);
        Mockito.when(plugin.getImpl().getCurrencyAdapter()).thenReturn(adapter);
        Mockito.when(plugin.getImpl().getCurrencies()).thenReturn(currencies);

        this.server.getPluginManager().registerMockPlugin(plugin);
    }

    private Currency mockCurrency(final String identifier) {
        final Currency currency = Mockito.mock(Currency.class);
        Mockito.when(currency.getIdentifier()).thenReturn(identifier);
        return currency;
    }
}
