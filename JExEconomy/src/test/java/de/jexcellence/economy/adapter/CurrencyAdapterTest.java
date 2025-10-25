package de.jexcellence.economy.adapter;

import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import de.jexcellence.economy.event.BalanceChangeEvent;
import de.jexcellence.economy.event.BalanceChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyAdapterTest {

    private static final double INITIAL_BALANCE = 100.0;

    @Mock
    private JExEconomyImpl economy;

    @Mock
    private UserCurrencyRepository userCurrencyRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrencyLogRepository currencyLogRepository;

    @Mock
    private RPlatform platform;

    @Mock
    private ISchedulerAdapter scheduler;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private OfflinePlayer offlinePlayer;

    @Mock
    private Player onlinePlayer;

    private ExecutorService executorService;
    private MockedStatic<Bukkit> bukkitMock;
    private CurrencyAdapter currencyAdapter;
    private Currency currency;
    private UserCurrency userCurrency;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(8);

        when(economy.getExecutor()).thenReturn(executorService);
        when(economy.getUserCurrencyRepository()).thenReturn(userCurrencyRepository);
        when(economy.getCurrencyRepository()).thenReturn(currencyRepository);
        when(economy.getUserRepository()).thenReturn(userRepository);
        when(economy.getCurrencyLogRepository()).thenReturn(currencyLogRepository);
        when(economy.getPlatform()).thenReturn(platform);
        when(platform.getScheduler()).thenReturn(scheduler);

        doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(scheduler).runSync(any());
        lenient().doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(scheduler).runAsync(any());

        bukkitMock = org.mockito.Mockito.mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        lenient().when(pluginManager.callEvent(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final UUID playerId = UUID.randomUUID();
        when(offlinePlayer.getUniqueId()).thenReturn(playerId);
        when(offlinePlayer.getName()).thenReturn("TestPlayer");
        when(offlinePlayer.getPlayer()).thenReturn(onlinePlayer);
        when(onlinePlayer.getUniqueId()).thenReturn(playerId);
        when(onlinePlayer.getName()).thenReturn("TestPlayer");

        bukkitMock.when(() -> Bukkit.getPlayer(playerId)).thenReturn(onlinePlayer);

        currency = new Currency("$", " coins", "test-currency", "$", Material.DIAMOND);
        final User user = new User(playerId, "TestPlayer");
        userCurrency = new UserCurrency(user, currency, INITIAL_BALANCE);

        currencyAdapter = new CurrencyAdapter(economy);
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
        executorService.shutdownNow();
    }

    @Test
    void depositUpdatesBalanceAndFiresEvents() {
        when(userCurrencyRepository.findByAttributes(anyMap())).thenReturn(userCurrency);

        final CurrencyResponse response = currencyAdapter.deposit(offlinePlayer, currency, 25.0).join();

        assertAll(
                () -> assertTrue(response.isTransactionSuccessful()),
                () -> assertEquals(INITIAL_BALANCE + 25.0, response.resultingBalance()),
                () -> assertEquals(INITIAL_BALANCE + 25.0, userCurrency.getBalance())
        );

        verify(userCurrencyRepository).update(userCurrency);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pluginManager, times(2)).callEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getAllValues().getFirst() instanceof BalanceChangeEvent);
        assertTrue(eventCaptor.getAllValues().getLast() instanceof BalanceChangedEvent);
        final BalanceChangedEvent changedEvent = (BalanceChangedEvent) eventCaptor.getAllValues().getLast();
        assertEquals(INITIAL_BALANCE + 25.0, changedEvent.getNewBalance());
    }

    @Test
    void withdrawUpdatesBalanceAndFiresEvents() {
        when(userCurrencyRepository.findByAttributes(anyMap())).thenReturn(userCurrency);

        final CurrencyResponse response = currencyAdapter.withdraw(offlinePlayer, currency, 40.0).join();

        assertAll(
                () -> assertTrue(response.isTransactionSuccessful()),
                () -> assertEquals(INITIAL_BALANCE - 40.0, response.resultingBalance()),
                () -> assertEquals(INITIAL_BALANCE - 40.0, userCurrency.getBalance())
        );

        verify(userCurrencyRepository).update(userCurrency);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(pluginManager, times(2)).callEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getAllValues().getFirst() instanceof BalanceChangeEvent);
        assertTrue(eventCaptor.getAllValues().getLast() instanceof BalanceChangedEvent);
        final BalanceChangedEvent changedEvent = (BalanceChangedEvent) eventCaptor.getAllValues().getLast();
        assertEquals(INITIAL_BALANCE - 40.0, changedEvent.getNewBalance());
    }

    @Test
    void getBalanceReturnsPersistedValue() {
        when(userCurrencyRepository.findByAttributes(anyMap())).thenReturn(userCurrency);

        final double balance = currencyAdapter.getBalance(offlinePlayer, currency).join();

        assertEquals(INITIAL_BALANCE, balance);
        verify(userCurrencyRepository).findByAttributes(anyMap());
    }

    @Test
    void createPlayerInitializesNewUserRecord() {
        when(userRepository.findByAttributes(Map.of("uniqueId", offlinePlayer.getUniqueId()))).thenReturn(null);
        final User persistedUser = new User(offlinePlayer.getUniqueId(), "TestPlayer");
        when(userRepository.create(any(User.class))).thenReturn(persistedUser);

        final boolean created = currencyAdapter.createPlayer(offlinePlayer).join();

        assertTrue(created);
        verify(userRepository).create(any(User.class));
    }

    @Test
    void concurrentOperationsRemainConsistent() throws Exception {
        lenient().when(userCurrencyRepository.update(any(UserCurrency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final int depositOperations = 30;
        final int withdrawOperations = 20;
        final double depositAmount = 3.0;
        final double withdrawAmount = 2.0;

        final CountDownLatch readyLatch = new CountDownLatch(depositOperations + withdrawOperations);
        final CountDownLatch startLatch = new CountDownLatch(1);

        final ExecutorService callerExecutor = Executors.newFixedThreadPool(depositOperations + withdrawOperations);

        try {
            for (int i = 0; i < depositOperations; i++) {
                callerExecutor.submit(() -> {
                    awaitLatch(readyLatch);
                    awaitStart(startLatch);
                    currencyAdapter.deposit(userCurrency, depositAmount).join();
                });
            }

            for (int i = 0; i < withdrawOperations; i++) {
                callerExecutor.submit(() -> {
                    awaitLatch(readyLatch);
                    awaitStart(startLatch);
                    currencyAdapter.withdraw(userCurrency, withdrawAmount).join();
                });
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
            startLatch.countDown();

            callerExecutor.shutdown();
            assertTrue(callerExecutor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            callerExecutor.shutdownNow();
        }

        final double expectedBalance = INITIAL_BALANCE + (depositOperations * depositAmount) - (withdrawOperations * withdrawAmount);
        assertEquals(expectedBalance, userCurrency.getBalance());
        verify(userCurrencyRepository, atLeast(depositOperations + withdrawOperations)).update(userCurrency);
    }

    private static void awaitLatch(final @NotNull CountDownLatch readyLatch) {
        readyLatch.countDown();
    }

    private static void awaitStart(final @NotNull CountDownLatch startLatch) {
        try {
            startLatch.await();
        } catch (final InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interruptedException);
        }
    }
}
