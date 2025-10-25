package de.jexcellence.economy.command.console.deposit;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.evaluable.EnumInfo;
import de.jexcellence.evaluable.error.ErrorContext;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDepositTest {

    private ServerMock server;
    private ConsoleCommandSenderMock consoleSender;
    private PlayerMock playerSender;
    private TestableDepositSection commandSection;
    private CDeposit command;
    private JExEconomyImpl economyImpl;
    private Logger commandLogger;
    private TestLogHandler logHandler;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        this.consoleSender = this.server.getConsoleSender();
        this.playerSender = this.server.addPlayer("CommandPlayer");

        this.commandSection = new TestableDepositSection();

        JExEconomy plugin = Mockito.mock(JExEconomy.class);
        this.economyImpl = Mockito.mock(JExEconomyImpl.class);
        Mockito.when(plugin.getImpl()).thenReturn(this.economyImpl);

        this.command = new CDeposit(this.commandSection, plugin);

        this.commandLogger = getCommandLogger();
        this.logHandler = new TestLogHandler();
        this.commandLogger.addHandler(this.logHandler);
    }

    @AfterEach
    void tearDown() {
        if (this.commandLogger != null && this.logHandler != null) {
            this.commandLogger.removeHandler(this.logHandler);
        }
        MockBukkit.unmock();
    }

    @Test
    void executeRejectsPlayerSenders() {
        boolean result = this.command.execute(this.playerSender, "cdeposit", new String[]{"target", "currency", "10"});

        assertFalse(result, "Player senders should be rejected by console-only commands");
        assertEquals(
                "cdeposit-not-a-console",
                this.playerSender.nextMessage(),
                "Player should receive localized not-a-console feedback"
        );
    }

    @Test
    void executeValidatesArgumentCountForConsoleSender() {
        boolean result = this.command.execute(this.consoleSender, "cdeposit", new String[]{"target"});

        assertFalse(result, "Missing arguments should cause command execution to fail");
        assertEquals(
                "cdeposit-missing-argument",
                this.consoleSender.nextMessage(),
                "Console should receive localized missing argument feedback"
        );
    }

    @Test
    void successfulDepositDelegatesToAdapterAndLogsOutcome() {
        Player target = this.server.addPlayer("EconomyTarget");
        Map<Long, Currency> currencies = new LinkedHashMap<>();
        Currency currency = Mockito.mock(Currency.class);
        Mockito.when(currency.getIdentifier()).thenReturn("coins");
        currencies.put(42L, currency);
        Mockito.when(this.economyImpl.getCurrencies()).thenReturn(currencies);

        CurrencyAdapter currencyAdapter = Mockito.mock(CurrencyAdapter.class);
        Mockito.when(this.economyImpl.getCurrencyAdapter()).thenReturn(currencyAdapter);

        ExecutorService executor = Mockito.mock(ExecutorService.class);
        Mockito.when(this.economyImpl.getExecutor()).thenReturn(executor);
        Mockito.doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).execute(Mockito.any());

        CurrencyResponse response = CurrencyResponse.createSuccessfulResponse(50.0, 150.0);
        CompletableFuture<CurrencyResponse> responseFuture = CompletableFuture.completedFuture(response);
        Mockito.when(currencyAdapter.deposit(Mockito.any(), Mockito.eq(currency), Mockito.eq(50.0)))
               .thenReturn(responseFuture);

        boolean result = this.command.execute(
                this.consoleSender,
                "cdeposit",
                new String[]{target.getName(), "coins", "50"}
        );

        assertTrue(result, "Successful execution should return true");

        ArgumentCaptor<OfflinePlayer> playerCaptor = ArgumentCaptor.forClass(OfflinePlayer.class);
        Mockito.verify(currencyAdapter).deposit(playerCaptor.capture(), Mockito.eq(currency), Mockito.eq(50.0));
        assertEquals(target.getUniqueId(), playerCaptor.getValue().getUniqueId(), "Deposit should target the resolved player");
        Mockito.verify(executor).execute(Mockito.any());

        assertTrue(
                this.logHandler.contains(Level.INFO, "Executing console deposit"),
                "Command should log the start of the deposit operation"
        );
        assertTrue(
                this.logHandler.contains(Level.INFO, "Console deposit completed successfully"),
                "Command should log successful completion of the deposit operation"
        );
    }

    private static Logger getCommandLogger() throws Exception {
        var field = CDeposit.class.getDeclaredField("COMMAND_LOGGER");
        field.setAccessible(true);
        return (Logger) field.get(null);
    }

    private static final class TestableDepositSection extends CDepositSection {

        TestableDepositSection() {
            super(new EvaluationEnvironmentBuilder());
        }

        @Override
        public String getDescription() {
            return "Console deposit";
        }

        @Override
        public String getUsage() {
            return "/cdeposit <player> <currency> <amount>";
        }

        @Override
        public List<String> getAliases() {
            return List.of();
        }

        private Component message(String suffix) {
            return Component.text("cdeposit-" + suffix);
        }

        @Override
        public Component getInternalErrorMessage(ErrorContext context) {
            return message("internal-error");
        }

        @Override
        public Component getMalformedDoubleMessage(ErrorContext context) {
            return message("malformed-double");
        }

        @Override
        public Component getMalformedFloatMessage(ErrorContext context) {
            return message("malformed-float");
        }

        @Override
        public Component getMalformedLongMessage(ErrorContext context) {
            return message("malformed-long");
        }

        @Override
        public Component getMalformedIntegerMessage(ErrorContext context) {
            return message("malformed-integer");
        }

        @Override
        public Component getMalformedUuidMessage(ErrorContext context) {
            return message("malformed-uuid");
        }

        @Override
        public Component getMalformedEnumMessage(ErrorContext context, EnumInfo enumInfo) {
            return message("malformed-enum");
        }

        @Override
        public Component getMissingArgumentMessage(ErrorContext context) {
            return message("missing-argument");
        }

        @Override
        public Component getNotAPlayerMessage(ErrorContext context) {
            return message("not-a-player");
        }

        @Override
        public Component getNotAConsoleMessage(ErrorContext context) {
            return message("not-a-console");
        }

        @Override
        public Component getPlayerUnknownMessage(ErrorContext context) {
            return message("player-unknown");
        }

        @Override
        public Component getPlayerNotOnlineMessage(ErrorContext context) {
            return message("player-not-online");
        }
    }

    private static final class TestLogHandler extends Handler {

        private final List<LogRecord> records = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            this.records.add(record);
        }

        @Override
        public void flush() {
            // No-op for in-memory handler
        }

        @Override
        public void close() {
            this.records.clear();
        }

        boolean contains(Level level, String fragment) {
            return this.records.stream()
                    .filter(record -> record.getLevel().equals(level))
                    .anyMatch(record -> record.getMessage().contains(fragment));
        }
    }
}
