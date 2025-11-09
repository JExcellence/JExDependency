package de.jexcellence.economy.event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.service.CurrencyLogService;
import de.jexcellence.economy.type.EChangeType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyLogEventListenerTest {

    @Mock
    private JExEconomyImpl economyImpl;

    @Mock
    private CurrencyLogService logService;

    @Mock
    private de.jexcellence.economy.database.entity.Currency currency;

    @Mock
    private Player player;

    private ServerMock server;
    private JavaPlugin plugin;
    private CurrencyLogEventListener listener;
    private User user;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();

        when(this.economyImpl.getLogService()).thenReturn(this.logService);

        this.listener = new CurrencyLogEventListener(this.economyImpl);
        this.server.getPluginManager().registerEvents(this.listener, this.plugin);

        this.user = new User(UUID.randomUUID(), "TestPlayer");
        when(this.currency.getIdentifier()).thenReturn("coins");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldLogSuccessfulBalanceChange() {
        BalanceChangeEvent event = new BalanceChangeEvent(
            this.user,
            this.currency,
            50.0,
            75.0,
            EChangeType.DEPOSIT,
            "Quest reward",
            this.player
        );

        TrackingCompletableFuture<CurrencyLog> future = prepareBalanceChangeFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logBalanceChange(
            same(this.user),
            same(this.currency),
            same(EChangeType.DEPOSIT),
            eq(50.0),
            eq(75.0),
            eq(25.0),
            eq("Quest reward"),
            same(this.player),
            successCaptor.capture(),
            errorCaptor.capture()
        );

        assertTrue(successCaptor.getValue());
        assertNull(errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogFailedBalanceChangeWhenEventCancelled() {
        BalanceChangeEvent event = new BalanceChangeEvent(
            this.user,
            this.currency,
            75.0,
            25.0,
            EChangeType.WITHDRAW,
            "Purchase",
            this.player
        );
        event.setCancelled(true);
        event.setCancelReason("Insufficient funds");

        TrackingCompletableFuture<CurrencyLog> future = prepareBalanceChangeFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logBalanceChange(
            same(this.user),
            same(this.currency),
            same(EChangeType.WITHDRAW),
            eq(75.0),
            eq(25.0),
            eq(50.0),
            eq("Purchase"),
            same(this.player),
            successCaptor.capture(),
            errorCaptor.capture()
        );

        assertFalse(successCaptor.getValue());
        assertEquals("Insufficient funds", errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogBalanceChangedEvent() {
        BalanceChangedEvent event = new BalanceChangedEvent(
            this.user,
            this.currency,
            25.0,
            40.0,
            EChangeType.DEPOSIT,
            "Daily reward",
            this.player
        );

        TrackingCompletableFuture<CurrencyLog> future = prepareBalanceChangeFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logBalanceChange(
            same(this.user),
            same(this.currency),
            same(EChangeType.DEPOSIT),
            eq(25.0),
            eq(40.0),
            eq(15.0),
            eq("Daily reward"),
            same(this.player),
            successCaptor.capture(),
            errorCaptor.capture()
        );

        assertTrue(successCaptor.getValue());
        assertNull(errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogCurrencyCreateAttempt() {
        CurrencyCreateEvent event = new CurrencyCreateEvent(this.currency, this.player);

        TrackingCompletableFuture<CurrencyLog> future = prepareCurrencyManagementFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logCurrencyManagement(
            same(this.currency),
            eq("CREATE_ATTEMPT"),
            same(this.player),
            successCaptor.capture(),
            detailsCaptor.capture(),
            errorCaptor.capture()
        );

        assertTrue(successCaptor.getValue());
        assertEquals("Currency creation attempt: coins", detailsCaptor.getValue());
        assertNull(errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogCurrencyCreateFailureWhenCancelled() {
        CurrencyCreateEvent event = new CurrencyCreateEvent(this.currency, this.player);
        event.setCancelled(true);
        event.setCancelReason("Identifier already used");

        TrackingCompletableFuture<CurrencyLog> future = prepareCurrencyManagementFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logCurrencyManagement(
            same(this.currency),
            eq("CREATE_ATTEMPT"),
            same(this.player),
            successCaptor.capture(),
            detailsCaptor.capture(),
            errorCaptor.capture()
        );

        assertFalse(successCaptor.getValue());
        assertEquals("Currency creation attempt: coins", detailsCaptor.getValue());
        assertEquals("Identifier already used", errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogCurrencyCreatedEvent() {
        CurrencyCreatedEvent event = new CurrencyCreatedEvent(this.currency, this.player);

        TrackingCompletableFuture<CurrencyLog> future = prepareCurrencyManagementFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logCurrencyManagement(
            same(this.currency),
            eq("CREATE_SUCCESS"),
            same(this.player),
            successCaptor.capture(),
            detailsCaptor.capture(),
            errorCaptor.capture()
        );

        assertTrue(successCaptor.getValue());
        assertEquals("Currency created successfully: coins", detailsCaptor.getValue());
        assertNull(errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogCurrencyDeleteAttempt() {
        CurrencyDeleteEvent event = new CurrencyDeleteEvent(this.currency, this.player, 5, 125.5);

        TrackingCompletableFuture<CurrencyLog> future = prepareCurrencyManagementFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logCurrencyManagement(
            same(this.currency),
            eq("DELETE_ATTEMPT"),
            same(this.player),
            successCaptor.capture(),
            detailsCaptor.capture(),
            errorCaptor.capture()
        );

        assertTrue(successCaptor.getValue());
        assertEquals(
            String.format("Deletion attempt - Affected players: %d, Total balance: %.2f", 5, 125.5),
            detailsCaptor.getValue()
        );
        assertNull(errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogCurrencyDeleteFailureWhenCancelled() {
        CurrencyDeleteEvent event = new CurrencyDeleteEvent(this.currency, this.player, 3, 40.0);
        event.setCancelled(true);
        event.setCancelReason("Currency still in use");

        TrackingCompletableFuture<CurrencyLog> future = prepareCurrencyManagementFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logCurrencyManagement(
            same(this.currency),
            eq("DELETE_ATTEMPT"),
            same(this.player),
            successCaptor.capture(),
            detailsCaptor.capture(),
            errorCaptor.capture()
        );

        assertFalse(successCaptor.getValue());
        assertEquals(
            String.format("Deletion attempt - Affected players: %d, Total balance: %.2f", 3, 40.0),
            detailsCaptor.getValue()
        );
        assertEquals("Currency still in use", errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    @Test
    void shouldLogCurrencyDeletedEvent() {
        CurrencyDeletedEvent event = new CurrencyDeletedEvent(this.currency, this.player, 6, 210.75);

        TrackingCompletableFuture<CurrencyLog> future = prepareCurrencyManagementFuture();

        this.server.getPluginManager().callEvent(event);

        ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);

        verify(this.logService).logCurrencyManagement(
            same(this.currency),
            eq("DELETE_SUCCESS"),
            same(this.player),
            successCaptor.capture(),
            detailsCaptor.capture(),
            errorCaptor.capture()
        );

        assertTrue(successCaptor.getValue());
        assertEquals(
            String.format(
                "Currency deleted successfully - Affected players: %d, Total balance lost: %.2f",
                6,
                210.75
            ),
            detailsCaptor.getValue()
        );
        assertNull(errorCaptor.getValue());
        assertTrue(future.wasExceptionallyAttached());
        assertFalse(future.isDone());
        future.complete(mock(CurrencyLog.class));
        assertTrue(future.isDone());
    }

    private TrackingCompletableFuture<CurrencyLog> prepareBalanceChangeFuture() {
        TrackingCompletableFuture<CurrencyLog> future = new TrackingCompletableFuture<>();
        when(this.logService.logBalanceChange(
            any(),
            any(),
            any(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            any(),
            any(),
            anyBoolean(),
            any()
        )).thenReturn(future);
        return future;
    }

    private TrackingCompletableFuture<CurrencyLog> prepareCurrencyManagementFuture() {
        TrackingCompletableFuture<CurrencyLog> future = new TrackingCompletableFuture<>();
        when(this.logService.logCurrencyManagement(
            any(),
            anyString(),
            any(),
            anyBoolean(),
            any(),
            any()
        )).thenReturn(future);
        return future;
    }

    private static final class TrackingCompletableFuture<T> extends CompletableFuture<T> {

        private final AtomicBoolean exceptionallyAttached = new AtomicBoolean(false);

        boolean wasExceptionallyAttached() {
            return this.exceptionallyAttached.get();
        }

        @Override
        public <U> CompletableFuture<U> exceptionally(Function<Throwable, ? extends U> fn) {
            this.exceptionallyAttached.set(true);
            return super.exceptionally(fn);
        }
    }
}
