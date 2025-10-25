package de.jexcellence.economy.currency;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.economy.database.repository.UserRepository;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValueHost;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrenciesCreatingViewTest {

    private ServerMock server;
    private PlayerMock player;
    private CurrenciesCreatingView view;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        view = new CurrenciesCreatingView();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onResumePropagatesCurrencyStateFromSubview() throws Exception {
        final Context originContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        final Context targetContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));

        final MutableState<Currency> currencyState = mock(MutableState.class);
        setStateField("targetCurrency", currencyState);

        final Currency updatedCurrency = new Currency("Coins: ", " each", "coins", "$", Material.GOLD_INGOT);
        when(originContext.getInitialData()).thenReturn(Map.of("currency", updatedCurrency));

        view.onResume(originContext, targetContext);

        verify(currencyState).set(updatedCurrency, targetContext);
        verify(targetContext).update();
    }

    @Test
    void handleCurrencyCreationPersistsCurrencyAndInitializesAccounts() throws Exception {
        final MutableState<Currency> currencyState = mock(MutableState.class);
        final State<JExEconomyImpl> pluginState = mock(State.class);
        setStateField("targetCurrency", currencyState);
        setStateField("jexEconomy", pluginState);

        final Context clickContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        final Currency configuredCurrency = new Currency("Gold: ", " coins", "gold", "G", Material.GOLD_INGOT);
        when(currencyState.get(clickContext)).thenReturn(configuredCurrency);

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        when(pluginState.get(clickContext)).thenReturn(plugin);

        final CurrencyAdapter currencyAdapter = mock(CurrencyAdapter.class);
        final UserRepository userRepository = mock(UserRepository.class);
        final UserCurrencyRepository userCurrencyRepository = mock(UserCurrencyRepository.class);

        when(plugin.getCurrencyAdapter()).thenReturn(currencyAdapter);
        when(plugin.getUserRepository()).thenReturn(userRepository);
        when(plugin.getUserCurrencyRepository()).thenReturn(userCurrencyRepository);

        final ExecutorService directExecutor = new DirectExecutorService();
        when(plugin.getExecutor()).thenReturn(directExecutor);

        when(currencyAdapter.hasGivenCurrency("gold")).thenReturn(CompletableFuture.completedFuture(false));
        when(currencyAdapter.createCurrency(any())).thenReturn(CompletableFuture.completedFuture(true));

        final Currency cachedCurrency = new Currency("Gold: ", " coins", "gold", "G", Material.GOLD_INGOT);
        final Map<Long, Currency> currencyCache = new LinkedHashMap<>();
        currencyCache.put(1L, cachedCurrency);
        when(plugin.getCurrencies()).thenReturn(currencyCache);

        final List<User> existingUsers = List.of(
                new User(UUID.randomUUID(), "Alice"),
                new User(UUID.randomUUID(), "Bob")
        );
        when(userRepository.findAllAsync(0, 128)).thenReturn(CompletableFuture.completedFuture(existingUsers));

        final List<TranslationKey> capturedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(capturedKeys)) {
            invokeHandleCurrencyCreation(clickContext, player);
        }

        final ArgumentCaptor<Currency> persistedCurrencyCaptor = ArgumentCaptor.forClass(Currency.class);
        verify(currencyAdapter).createCurrency(persistedCurrencyCaptor.capture());
        final Currency persistedCurrency = persistedCurrencyCaptor.getValue();
        assertEquals(configuredCurrency.getPrefix(), persistedCurrency.getPrefix());
        assertEquals(configuredCurrency.getSuffix(), persistedCurrency.getSuffix());
        assertEquals(configuredCurrency.getIdentifier(), persistedCurrency.getIdentifier());
        assertEquals(configuredCurrency.getSymbol(), persistedCurrency.getSymbol());
        assertEquals(configuredCurrency.getIcon(), persistedCurrency.getIcon());

        final ArgumentCaptor<UserCurrency> userCurrencyCaptor = ArgumentCaptor.forClass(UserCurrency.class);
        verify(userCurrencyRepository, times(existingUsers.size())).create(userCurrencyCaptor.capture());
        final List<UserCurrency> createdAccounts = userCurrencyCaptor.getAllValues();
        assertTrue(createdAccounts.stream().allMatch(account -> account.getCurrency() == cachedCurrency));

        verify(userRepository).findAllAsync(0, 128);
        verify(currencyState).set(argThat(newCurrency -> newCurrency.getIdentifier().isEmpty()), eq(clickContext));
        verify(clickContext).update();
        verify(clickContext).closeForPlayer();
    }

    @Test
    void handleCurrencyCreationHandlesExistingCurrencyGracefully() throws Exception {
        final MutableState<Currency> currencyState = mock(MutableState.class);
        final State<JExEconomyImpl> pluginState = mock(State.class);
        setStateField("targetCurrency", currencyState);
        setStateField("jexEconomy", pluginState);

        final Context clickContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        final Currency configuredCurrency = new Currency("Gem: ", "", "gems", "✦", Material.EMERALD);
        when(currencyState.get(clickContext)).thenReturn(configuredCurrency);

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        when(pluginState.get(clickContext)).thenReturn(plugin);

        final CurrencyAdapter currencyAdapter = mock(CurrencyAdapter.class);
        when(plugin.getCurrencyAdapter()).thenReturn(currencyAdapter);
        when(plugin.getExecutor()).thenReturn(new DirectExecutorService());
        when(currencyAdapter.hasGivenCurrency("gems")).thenReturn(CompletableFuture.completedFuture(true));

        final List<TranslationKey> capturedKeys = new ArrayList<>();
        try (MockedStatic<TranslationService> translations = mockTranslations(capturedKeys)) {
            invokeHandleCurrencyCreation(clickContext, player);
        }

        verify(currencyAdapter, never()).createCurrency(any());
        verify(currencyState, never()).set(any(), eq(clickContext));
        verify(clickContext).update();
        verify(clickContext).closeForPlayer();
        assertTrue(
                capturedKeys.stream()
                            .map(TranslationKey::key)
                            .anyMatch(key -> key.equals("currencies_creating_ui.create.already_exists"))
        );
    }

    private void setStateField(final String fieldName, final Object value) throws Exception {
        final Field field = CurrenciesCreatingView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(view, value);
    }

    private void invokeHandleCurrencyCreation(final Context context, final PlayerMock requester) throws Exception {
        final Method method = CurrenciesCreatingView.class.getDeclaredMethod(
                "handleCurrencyCreation",
                Context.class,
                org.bukkit.entity.Player.class
        );
        method.setAccessible(true);
        method.invoke(view, context, requester);
    }

    private MockedStatic<TranslationService> mockTranslations(final List<TranslationKey> capturedKeys) {
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        final Answer<TranslationService> answer = invocation -> {
            final TranslationKey key = invocation.getArgument(0);
            capturedKeys.add(key);
            final TranslationService service = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
            Mockito.when(service.build()).thenReturn(new TranslatedMessage(net.kyori.adventure.text.Component.empty(), key));
            return service;
        };
        translations.when(() -> TranslationService.create(Mockito.any(), Mockito.any())).thenAnswer(answer);
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
