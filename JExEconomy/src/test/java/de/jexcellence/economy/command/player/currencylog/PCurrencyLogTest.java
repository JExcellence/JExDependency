package de.jexcellence.economy.command.player.currencylog;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.service.CurrencyLogService;
import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.economy.type.ELogLevel;
import de.jexcellence.economy.type.ELogType;
import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PCurrencyLogTest {

    private ServerMock server;
    private PlayerMock player;
    private ExecutorService executor;
    private PCurrencyLog command;
    private CurrencyLogRepository logRepository;
    private JExEconomyImpl economyImpl;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("Viewer");
        this.executor = new DirectExecutorService();

        final PCurrencyLogSection section = mock(PCurrencyLogSection.class);
        final PermissionsSection permissions = mock(PermissionsSection.class);
        when(section.getName()).thenReturn("pcurrencylog");
        when(section.getDescription()).thenReturn("");
        when(section.getUsage()).thenReturn("/pcurrencylog");
        when(section.getAliases()).thenReturn(List.of("pcurrencylog"));
        when(section.getPermissions()).thenReturn(permissions);
        when(permissions.hasPermission(any(Player.class), any(IPermissionNode.class))).thenReturn(true);
        doNothing().when(permissions).sendMissingMessage(any(Player.class), any(IPermissionNode.class));

        final JExEconomy plugin = mock(JExEconomy.class);
        this.economyImpl = mock(JExEconomyImpl.class);
        this.logRepository = mock(CurrencyLogRepository.class);
        final CurrencyLogService logService = mock(CurrencyLogService.class);

        when(plugin.getImpl()).thenReturn(this.economyImpl);
        when(this.economyImpl.getExecutor()).thenReturn(this.executor);
        when(this.economyImpl.getCurrencyLogRepository()).thenReturn(this.logRepository);
        when(this.economyImpl.getLogService()).thenReturn(logService);

        this.command = new PCurrencyLog(section, plugin);
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
        MockBukkit.unmock();
    }

    @Test
    void viewDisplaysFormattedLogsWithCurrencyFilter() {
        final Currency currency = new Currency("$", "Gold", "Gold Coins", "G", Material.GOLD_INGOT);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(7L, currency);
        when(this.economyImpl.getCurrencies()).thenReturn(currencies);

        final CurrencyLog firstLog = createTransactionLog(currency, this.player.getUniqueId(), this.player.getName(),
                "Deposited bonus winnings", LocalDateTime.of(2024, 5, 1, 10, 15, 30), 7L);
        final CurrencyLog secondLog = createTransactionLog(currency, this.player.getUniqueId(), this.player.getName(),
                "Withdrew for shop purchase", LocalDateTime.of(2024, 5, 2, 9, 0, 5), 8L);
        when(this.logRepository.findByCriteria(any(), any(), any(), eq(7L), any(), any(), anyInt()))
                .thenReturn(List.of(firstLog, secondLog));
        when(this.logRepository.findAll(anyInt(), anyInt())).thenReturn(List.of(firstLog, secondLog));

        this.command.execute(this.player, "pcurrencylog", new String[]{"filter", "currency", currency.getIdentifier()});
        drainAllMessages(this.player);

        this.command.execute(this.player, "pcurrencylog", new String[]{"view"});
        final List<String> messages = drainAllMessages(this.player);

        assertTrue(messages.stream().anyMatch(message -> message.contains("Currency Filter Applied")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Deposited bonus winnings")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("2024-05-01 10:15:30")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Withdrew for shop purchase")));
    }

    @Test
    void viewWithStrictDateRangeShowsNoLogsMessage() {
        final Currency currency = new Currency("$", "Silver", "Silver Credits", "S", Material.IRON_INGOT);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(3L, currency);
        when(this.economyImpl.getCurrencies()).thenReturn(currencies);

        when(this.logRepository.findByCriteria(any(), any(), any(), eq(3L), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(this.logRepository.findAll(anyInt(), anyInt())).thenReturn(List.of());

        this.command.execute(this.player, "pcurrencylog", new String[]{"filter", "currency", currency.getIdentifier()});
        drainAllMessages(this.player);

        this.command.execute(this.player, "pcurrencylog", new String[]{"view"});
        final List<String> messages = drainAllMessages(this.player);

        assertTrue(messages.stream().anyMatch(message -> message.contains("No Currency Logs Found")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("Adjust Filters")));
    }

    @Test
    void detailsCommandHandlesInvalidArguments() {
        this.command.execute(this.player, "pcurrencylog", new String[]{"details"});
        final List<String> missingMessages = drainAllMessages(this.player);
        assertTrue(missingMessages.stream().anyMatch(message -> message.contains("Missing Log ID")));

        this.command.execute(this.player, "pcurrencylog", new String[]{"details", "abc"});
        final List<String> invalidMessages = drainAllMessages(this.player);
        assertTrue(invalidMessages.stream().anyMatch(message -> message.contains("Invalid Log ID")));
    }

    @Test
    void detailsCommandNotifiesWhenLogMissing() {
        when(this.logRepository.findById(42L)).thenReturn(null);

        this.command.execute(this.player, "pcurrencylog", new String[]{"details", "42"});
        final List<String> messages = drainAllMessages(this.player);

        assertTrue(messages.stream().anyMatch(message -> message.contains("Log Not Found")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("ID: 42")));
    }

    private CurrencyLog createTransactionLog(
            final Currency currency,
            final UUID playerUuid,
            final String playerName,
            final String description,
            final LocalDateTime timestamp,
            final long id
    ) {
        final CurrencyLog log = new CurrencyLog(
                playerUuid,
                playerName,
                currency,
                EChangeType.DEPOSIT,
                100.0,
                150.0,
                50.0,
                description,
                "test"
        );
        log.setTimestamp(timestamp);
        log.setLogLevel(ELogLevel.INFO);
        log.setLogType(ELogType.TRANSACTION);
        log.setSuccess(true);
        log.setAmount(50.0);
        setLogId(log, id);
        return log;
    }

    private void setLogId(final CurrencyLog log, final long id) {
        try {
            final Field idField = log.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(log, id);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Tests only require the identifier when available; ignore if structure differs.
        }
    }

    private List<String> drainAllMessages(final PlayerMock mockPlayer) {
        final List<String> messages = new ArrayList<>();
        final PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        while (true) {
            try {
                final String legacy = mockPlayer.nextMessage();
                messages.add(legacy);
            } catch (AssertionError ex) {
                break;
            }
        }

        while (true) {
            try {
                final Component component = mockPlayer.nextComponentMessage();
                if (component == null) {
                    break;
                }
                messages.add(serializer.serialize(component));
            } catch (AssertionError ex) {
                break;
            }
        }

        return messages;
    }

    private static final class DirectExecutorService extends AbstractExecutorService {

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
            command.run();
        }
    }
}
