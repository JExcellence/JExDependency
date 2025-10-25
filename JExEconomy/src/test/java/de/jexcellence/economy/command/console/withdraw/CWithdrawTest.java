package de.jexcellence.economy.command.console.withdraw;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CWithdraw} validating argument parsing, dependency usage, and logging behaviour.
 */
@ExtendWith(MockitoExtension.class)
class CWithdrawTest {

        private ServerMock server;
        private ConsoleCommandSender consoleSender;
        private DirectExecutorService directExecutor;
        private Map<Long, Currency> currencyCache;
        private CWithdraw command;

        @Mock
        private CWithdrawSection commandSection;

        @Mock
        private JExEconomy plugin;

        @Mock
        private JExEconomyImpl pluginImpl;

        @Mock
        private CurrencyAdapter currencyAdapter;

        @Mock
        private Currency currency;

        @BeforeEach
        void setUp() {
                this.server = MockBukkit.mock();
                this.consoleSender = this.server.getConsoleSender();
                this.directExecutor = new DirectExecutorService();
                this.currencyCache = new LinkedHashMap<>();

                when(this.plugin.getImpl()).thenReturn(this.pluginImpl);
                when(this.pluginImpl.getExecutor()).thenReturn(this.directExecutor);
                when(this.pluginImpl.getCurrencyAdapter()).thenReturn(this.currencyAdapter);
                when(this.pluginImpl.getCurrencies()).then(invocation -> this.currencyCache);

                when(this.commandSection.getName()).thenReturn("cwithdraw");
                when(this.commandSection.getDescription()).thenReturn("Withdraw currency from an offline player.");
                when(this.commandSection.getUsage()).thenReturn("/cwithdraw <player> <currency> <amount>");
                when(this.commandSection.getAliases()).thenReturn(List.of());

                this.command = new CWithdraw(this.commandSection, this.plugin);
        }

        @AfterEach
        void tearDown() {
                MockBukkit.unmock();
                this.directExecutor.shutdown();
        }

        @Test
        void executesWithdrawalAndLogsSuccess() {
                final PlayerMock player = this.server.addPlayer("TestTarget");
                player.setHasPlayedBefore(true);
                when(this.currency.getIdentifier()).thenReturn("gems");
                this.currencyCache.put(1L, this.currency);

                final CurrencyResponse response = new CurrencyResponse(
                        -50.0,
                        150.0,
                        CurrencyResponse.ResponseType.SUCCESS,
                        null
                );

                when(this.currencyAdapter.withdraw(any(OfflinePlayer.class), eq(this.currency), anyDouble()))
                        .thenReturn(CompletableFuture.completedFuture(response));

                final TestLogHandler handler = new TestLogHandler();
                final Logger logger = Logger.getLogger(CWithdraw.class.getName());
                logger.addHandler(handler);
                logger.setLevel(Level.ALL);

                try {
                        final boolean executed = this.command.execute(
                                this.consoleSender,
                                "cwithdraw",
                                new String[]{player.getName(), "gems", "50"}
                        );

                        assertTrue(executed, "Command should execute successfully");

                        final ArgumentCaptor<OfflinePlayer> playerCaptor = ArgumentCaptor.forClass(OfflinePlayer.class);
                        final ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);

                        verify(this.currencyAdapter).withdraw(playerCaptor.capture(), eq(this.currency), amountCaptor.capture());
                        assertEquals(player.getUniqueId(), playerCaptor.getValue().getUniqueId(), "Player UUID should match argument");
                        assertEquals(50.0, amountCaptor.getValue(), 0.0001, "Amount should be parsed with full precision");

                        assertTrue(
                                handler.contains(Level.INFO, "Console withdrawal completed successfully"),
                                "Success log entry should be published"
                        );
                        assertTrue(
                                handler.contains(Level.INFO, String.format("new_balance=%.2f", response.resultingBalance())),
                                "Resulting balance should be included in log output"
                        );
                } finally {
                        logger.removeHandler(handler);
                }
        }

        @Test
        void logsFailureMessageWhenFundsAreInsufficient() {
                final PlayerMock player = this.server.addPlayer("LowBalancePlayer");
                player.setHasPlayedBefore(true);
                when(this.currency.getIdentifier()).thenReturn("tokens");
                this.currencyCache.put(7L, this.currency);

                final CurrencyResponse response = new CurrencyResponse(
                        -125.0,
                        25.0,
                        CurrencyResponse.ResponseType.FAILURE,
                        "Insufficient funds"
                );

                when(this.currencyAdapter.withdraw(any(OfflinePlayer.class), eq(this.currency), anyDouble()))
                        .thenReturn(CompletableFuture.completedFuture(response));

                final TestLogHandler handler = new TestLogHandler();
                final Logger logger = Logger.getLogger(CWithdraw.class.getName());
                logger.addHandler(handler);
                logger.setLevel(Level.ALL);

                try {
                        final boolean executed = this.command.execute(
                                this.consoleSender,
                                "cwithdraw",
                                new String[]{player.getName(), "tokens", "125"}
                        );

                        assertTrue(executed, "Command should still report successful execution even on adapter failure");

                        verify(this.currencyAdapter).withdraw(any(OfflinePlayer.class), eq(this.currency), eq(125.0));

                        assertTrue(
                                handler.contains(Level.WARNING, "Console withdrawal failed"),
                                "Failure log must mention command failure"
                        );
                        assertTrue(
                                handler.contains(Level.WARNING, response.failureMessage()),
                                "Failure message from adapter should be logged"
                        );
                } finally {
                        logger.removeHandler(handler);
                }
        }

        @Test
        void logsAvailableCurrenciesWhenIdentifierIsUnknown() {
                when(this.currency.getIdentifier()).thenReturn("coins");
                this.currencyCache.put(3L, this.currency);

                final TestLogHandler handler = new TestLogHandler();
                final Logger logger = Logger.getLogger(CWithdraw.class.getName());
                logger.addHandler(handler);
                logger.setLevel(Level.ALL);

                try {
                        final boolean executed = this.command.execute(
                                this.consoleSender,
                                "cwithdraw",
                                new String[]{UUID.randomUUID().toString(), "gems", "10"}
                        );

                        assertTrue(executed, "Command wrapper should complete normally even when currency is unknown");

                        verify(this.currencyAdapter, never()).withdraw(any(), any(), anyDouble());

                        assertTrue(
                                handler.contains(Level.WARNING, "unknown currency identifier"),
                                "Warning log for unknown currency should be emitted"
                        );
                        assertTrue(
                                handler.contains(Level.INFO, "coins"),
                                "Available currency identifiers must be listed"
                        );
                } finally {
                        logger.removeHandler(handler);
                }
        }

        private static final class TestLogHandler extends Handler {

                private final List<LogRecord> records = new java.util.concurrent.CopyOnWriteArrayList<>();

                @Override
                public void publish(final LogRecord record) {
                        if (record != null) {
                                this.records.add(record);
                        }
                }

                @Override
                public void flush() {
                        // No-op for in-memory handler
                }

                @Override
                public void close() {
                        this.records.clear();
                }

                boolean contains(final Level level, final String expectedFragment) {
                        return this.records.stream()
                                           .filter(record -> record.getLevel().intValue() >= level.intValue())
                                           .map(LogRecord::getMessage)
                                           .anyMatch(message -> message != null && message.contains(expectedFragment));
                }
        }

        private static final class DirectExecutorService extends java.util.concurrent.AbstractExecutorService {

                private volatile boolean shutdown;

                @Override
                public void shutdown() {
                        this.shutdown = true;
                }

                @Override
                public List<Runnable> shutdownNow() {
                        this.shutdown = true;
                        return List.of();
                }

                @Override
                public boolean isShutdown() {
                        return this.shutdown;
                }

                @Override
                public boolean isTerminated() {
                        return this.shutdown;
                }

                @Override
                public boolean awaitTermination(final long timeout, final TimeUnit unit) {
                        return this.shutdown;
                }

                @Override
                public void execute(final Runnable command) {
                        if (command != null) {
                                command.run();
                        }
                }
        }
}
