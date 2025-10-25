package de.jexcellence.economy.command.player.currency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.OfflinePlayerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PCurrencyTest {

    private ServerMock server;
    private PlayerMock executingPlayer;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.executingPlayer = this.server.addPlayer("Alice");
        this.executor = new DirectExecutorService();
    }

    @AfterEach
    void tearDown() {
        this.executor.shutdownNow();
        MockBukkit.unmock();
    }

    @Test
    void displaysSelfBalanceWhenCurrencyExists() {
        final Currency currency = new Currency("$", "", "gems", "♦", Material.EMERALD);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(1L, currency);

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        when(adapter.getBalance(this.executingPlayer, currency)).thenReturn(CompletableFuture.completedFuture(125.0));

        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            command.execute(this.executingPlayer, "pcurrency", new String[]{currency.getIdentifier()});
        }

        assertTrue(invokedKeys.stream().map(TranslationKey::key).anyMatch("currency.balance.self"::equals));
        verify(adapter).getBalance(this.executingPlayer, currency);
    }

    @Test
    void displaysAllBalancesWithLocalizedEntries() {
        final Currency currency = new Currency("", "coins", "credits", "¤", Material.GOLD_INGOT);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(5L, currency);

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        final UserCurrency userCurrency = new UserCurrency(new User(this.executingPlayer), currency, 42.5);
        when(adapter.getUserCurrencies(this.executingPlayer)).thenReturn(CompletableFuture.completedFuture(List.of(userCurrency)));

        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            command.execute(this.executingPlayer, "pcurrency", new String[]{"all"});
        }

        final List<String> keys = invokedKeys.stream().map(TranslationKey::key).toList();
        assertTrue(keys.contains("currency.balance.all_header_self"));
        assertTrue(keys.contains("currency.balance.entry"));
        assertTrue(keys.contains("currency.balance.all_footer"));
        verify(adapter).getUserCurrencies(this.executingPlayer);
    }

    @Test
    void displaysOtherBalanceWhenPermitted() {
        final Currency currency = new Currency("", "", "tokens", "T", Material.DIAMOND);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(2L, currency);

        final PlayerMock targetPlayer = this.server.addPlayer("Bob");

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        when(adapter.getBalance(targetPlayer, currency)).thenReturn(CompletableFuture.completedFuture(64.0));

        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            command.execute(this.executingPlayer, "pcurrency", new String[]{currency.getIdentifier(), targetPlayer.getName()});
        }

        assertTrue(invokedKeys.stream().map(TranslationKey::key).anyMatch("currency.balance.other"::equals));
        verify(adapter).getBalance(targetPlayer, currency);
    }

    @Test
    void deniesOtherBalanceWhenPermissionMissing() {
        final Currency currency = new Currency("", "", "credits", "¤", Material.GOLD_INGOT);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(3L, currency);

        final PlayerMock targetPlayer = this.server.addPlayer("Charlie");

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);

        final PermissionsSection permissions = mockPermissionsSection(true, false);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        command.execute(this.executingPlayer, "pcurrency", new String[]{currency.getIdentifier(), targetPlayer.getName()});

        verify(adapter, never()).getBalance(any(OfflinePlayer.class), eq(currency));
        verify(permissions).sendMissingMessage(this.executingPlayer, ECurrencyPermission.CURRENCY_OTHER);
    }

    @Test
    void reportsInvalidCurrencyForDepositAttempt() {
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(7L, new Currency("", "", "coins", "$", Material.GOLD_INGOT));

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            command.execute(this.executingPlayer, "pcurrency", new String[]{"deposit"});
        }

        assertTrue(invokedKeys.stream().map(TranslationKey::key).anyMatch("general.invalid_currency"::equals));
        verify(adapter, never()).getBalance(any(OfflinePlayer.class), any());
    }

    @Test
    void reportsInvalidCurrencyForWithdrawalAttempt() {
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(9L, new Currency("", "", "coins", "$", Material.GOLD_INGOT));

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            command.execute(this.executingPlayer, "pcurrency", new String[]{"withdraw"});
        }

        assertTrue(invokedKeys.stream().map(TranslationKey::key).anyMatch("general.invalid_currency"::equals));
        verify(adapter, never()).getBalance(any(OfflinePlayer.class), any());
    }

    @Test
    void notifiesWhenTargetPlayerHasNeverJoined() {
        final Currency currency = new Currency("", "", "stars", "*", Material.NETHER_STAR);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(11L, currency);

        final OfflinePlayerMock neverPlayed = this.server.getOfflinePlayer("Dana");
        neverPlayed.setHasPlayedBefore(false);

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            command.execute(this.executingPlayer, "pcurrency", new String[]{currency.getIdentifier(), neverPlayed.getName()});
        }

        assertTrue(invokedKeys.stream().map(TranslationKey::key).anyMatch("general.invalid_player"::equals));
        verify(adapter, never()).getBalance(any(OfflinePlayer.class), eq(currency));
    }

    @Test
    void handlesAdapterFailuresGracefully() {
        final Currency currency = new Currency("", "", "tokens", "T", Material.DIAMOND);
        final Map<Long, Currency> currencies = new LinkedHashMap<>();
        currencies.put(13L, currency);

        final CurrencyAdapter adapter = mock(CurrencyAdapter.class);
        when(adapter.getBalance(this.executingPlayer, currency)).thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        final PermissionsSection permissions = mockPermissionsSection(true, true);
        final PCurrency command = createCommand(currencies, adapter, permissions);

        final List<TranslationKey> invokedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(invokedKeys)) {
            assertDoesNotThrow(() -> command.execute(this.executingPlayer, "pcurrency", new String[]{currency.getIdentifier()}));
        }

        assertTrue(invokedKeys.isEmpty());
    }

    private PCurrency createCommand(
            final Map<Long, Currency> currencies,
            final CurrencyAdapter adapter,
            final PermissionsSection permissionsSection
    ) {
        final PCurrencySection section = mock(PCurrencySection.class);
        when(section.getName()).thenReturn("pcurrency");
        when(section.getDescription()).thenReturn("");
        when(section.getUsage()).thenReturn("/pcurrency");
        when(section.getAliases()).thenReturn(List.of("pcurrency"));
        when(section.getPermissions()).thenReturn(permissionsSection);

        final JExEconomy plugin = mock(JExEconomy.class);
        final JExEconomyImpl implementation = mock(JExEconomyImpl.class);
        when(plugin.getImpl()).thenReturn(implementation);
        when(implementation.getCurrencies()).thenReturn(currencies);
        when(implementation.getCurrencyAdapter()).thenReturn(adapter);
        when(implementation.getExecutor()).thenReturn(this.executor);

        return new PCurrency(section, plugin);
    }

    private PermissionsSection mockPermissionsSection(final boolean basePermission, final boolean otherPermission) {
        final PermissionsSection permissions = mock(PermissionsSection.class);
        when(permissions.hasPermission(any(Player.class), any(IPermissionNode.class))).thenReturn(true);
        when(permissions.hasPermission(any(Player.class), eq(ECurrencyPermission.CURRENCY))).thenReturn(basePermission);
        when(permissions.hasPermission(any(Player.class), eq(ECurrencyPermission.CURRENCY_OTHER))).thenReturn(otherPermission);
        doNothing().when(permissions).sendMissingMessage(any(Player.class), any(IPermissionNode.class));
        return permissions;
    }

    private MockedStatic<TranslationService> mockTranslations(final List<TranslationKey> capturedKeys) {
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        final Answer<TranslationService> answer = invocation -> {
            final TranslationKey key = invocation.getArgument(0);
            capturedKeys.add(key);
            return Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        };
        translations.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any(Player.class))).thenAnswer(answer);
        translations.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any(OfflinePlayer.class))).thenAnswer(answer);
        return translations;
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
