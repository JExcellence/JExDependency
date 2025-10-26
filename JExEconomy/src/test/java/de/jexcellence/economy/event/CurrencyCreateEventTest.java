package de.jexcellence.economy.event;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyCreateEventTest {

    private ServerMock server;

    @Mock
    private JExEconomyImpl pluginImplementation;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private RPlatform platform;

    @Mock
    private ISchedulerAdapter scheduler;

    private Map<Long, Currency> currencyCache;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.currencyCache = new LinkedHashMap<>();

        when(this.pluginImplementation.getPlatform()).thenReturn(this.platform);
        when(this.platform.getScheduler()).thenReturn(this.scheduler);
        when(this.pluginImplementation.getCurrencyRepository()).thenReturn(this.currencyRepository);
        when(this.pluginImplementation.getCurrencies()).thenReturn(this.currencyCache);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldRetainCurrencySnapshotAndInitiatorMetadataBeforeDispatch() {
        Currency currency = new Currency("test-currency");
        var player = this.server.addPlayer("Creator");

        CurrencyCreateEvent event = new CurrencyCreateEvent(currency, player);

        assertThat(event.getCurrency()).isSameAs(currency);
        assertThat(event.getPlayer()).isSameAs(player);
        assertTrue(event.hasPlayer(), "Player metadata should be available before dispatch");
        assertFalse(event.isCancelled(), "Event should not be cancelled by default");
        assertNull(event.getCancelReason(), "No cancellation reason should be set prior to firing");
    }

    @Test
    void shouldAbortCreationWhenCreateEventIsCancelled() {
        Currency currency = new Currency("tokens");
        JavaPlugin plugin = MockBukkit.createMockPlugin();
        AtomicReference<CurrencyCreateEvent> dispatchedEvent = new AtomicReference<>();

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(this.scheduler).runSync(any());

        this.server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onCurrencyCreate(final CurrencyCreateEvent event) {
                dispatchedEvent.set(event);
                event.setCancelled("Creation blocked by test");
            }
        }, plugin);

        CurrencyAdapter adapter = new CurrencyAdapter(this.pluginImplementation);

        CompletableFuture<Boolean> result = adapter.createCurrency(currency, null);

        assertFalse(result.join(), "Creation should be aborted when event is cancelled");
        CurrencyCreateEvent event = dispatchedEvent.get();
        assertNotNull(event, "Listener should capture the dispatched event instance");
        assertTrue(event.isCancelled(), "Event should be marked as cancelled by the listener");
        assertThat(event.getCancelReason()).isEqualTo("Creation blocked by test");

        verify(this.currencyRepository, never()).createAsync(any());
        assertThat(this.currencyCache).isEmpty();
        verify(this.scheduler).runSync(any());
    }
}
