package de.jexcellence.economy.currency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.CurrencyLogRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueFactory;
import me.devnatan.inventoryframework.state.StateValueHost;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyDeletionViewTest {

    private ServerMock server;
    private PlayerMock player;
    private TestCurrencyDeletionView view;
    private StubState<JExEconomyImpl> pluginState;

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer("Admin");
        this.view = new TestCurrencyDeletionView();
        this.pluginState = injectState("jexEconomy");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void clickingCurrencyBuildsConfirmationWithImpactData() throws Exception {
        final Currency currency = mock(Currency.class);
        when(currency.getId()).thenReturn(42L);
        when(currency.getIdentifier()).thenReturn("gold");
        when(currency.getSymbol()).thenReturn("G");

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        final UserCurrencyRepository userCurrencyRepository = mock(UserCurrencyRepository.class);
        when(plugin.getUserCurrencyRepository()).thenReturn(userCurrencyRepository);
        when(plugin.getExecutor()).thenReturn(new DirectExecutorService());

        final List<UserCurrency> userCurrencies = List.of(
                mockUserCurrency(1L, 250.25),
                mockUserCurrency(2L, 1250.5)
        );
        when(userCurrencyRepository.findListByAttributes(Map.of("currency.id", 42L))).thenReturn(userCurrencies);

        final RenderContext renderContext = mock(RenderContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        when(renderContext.getPlayer()).thenReturn(this.player);
        this.pluginState.put((StateValueHost) renderContext, plugin);

        final Context clickContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        when(clickContext.getPlayer()).thenReturn(this.player);
        this.pluginState.put((StateValueHost) clickContext, plugin);
        doNothing().when(clickContext).openForPlayer(any(), any());

        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            configureItemTranslations(translations);

            final BukkitItemComponentBuilder builder = mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
            final ArgumentCaptor<Consumer<Context>> clickCaptor = ArgumentCaptor.forClass(Consumer.class);
            when(builder.onClick(clickCaptor.capture())).thenReturn(builder);

            this.view.renderEntry(renderContext, builder, 0, currency);

            try (MockedConstruction<ConfirmationView.Builder> builderConstruction = Mockito.mockConstruction(ConfirmationView.Builder.class, (mockBuilder, context) -> {
                when(mockBuilder.withKey(any())).thenReturn(mockBuilder);
                when(mockBuilder.withMessageKey(any())).thenReturn(mockBuilder);
                when(mockBuilder.withInitialData(anyMap())).thenReturn(mockBuilder);
                when(mockBuilder.withCallback(any())).thenReturn(mockBuilder);
                when(mockBuilder.withParentView(any())).thenReturn(mockBuilder);
            })) {
                clickCaptor.getValue().accept(clickContext);

                final ConfirmationView.Builder constructedBuilder = builderConstruction.constructed().get(0);
                verify(constructedBuilder).withKey("currency_deletion_ui");
                verify(constructedBuilder).withMessageKey("currency_deletion_ui.confirm.message");

                final ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
                verify(constructedBuilder).withInitialData(dataCaptor.capture());
                final Map<String, Object> initialData = dataCaptor.getValue();

                assertSame(plugin, initialData.get("plugin"));
                assertSame(currency, initialData.get("currency"));
                assertEquals(2, initialData.get("affected_players"));
                assertEquals(1500.75, (Double) initialData.get("total_balance"), 0.001);
                assertEquals("1,500.75", initialData.get("total_balance_formatted"));
                assertEquals("gold", initialData.get("currency_identifier"));
                assertEquals("G", initialData.get("currency_symbol"));

                verify(constructedBuilder).withParentView(CurrencyDeletionView.class);
                verify(constructedBuilder).openFor(clickContext, this.player);
            }
        }
    }

    @Test
    void confirmationCallbackDeletesCurrencyAndSendsSuccessFeedback() throws Exception {
        final Currency currency = mock(Currency.class);
        when(currency.getId()).thenReturn(99L);
        when(currency.getIdentifier()).thenReturn("emerald");
        when(currency.getSymbol()).thenReturn("E");

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        final UserCurrencyRepository userCurrencyRepository = mock(UserCurrencyRepository.class);
        final CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
        final CurrencyLogRepository currencyLogRepository = mock(CurrencyLogRepository.class);
        final JavaPlugin javaPlugin = mock(JavaPlugin.class);
        when(javaPlugin.getLogger()).thenReturn(Logger.getLogger("test"));

        when(plugin.getUserCurrencyRepository()).thenReturn(userCurrencyRepository);
        when(plugin.getCurrencyRepository()).thenReturn(currencyRepository);
        when(plugin.getCurrencyLogRepository()).thenReturn(currencyLogRepository);
        when(plugin.getPlugin()).thenReturn(javaPlugin);
        when(plugin.getExecutor()).thenReturn(new DirectExecutorService());

        final Map<Long, Currency> cache = new ConcurrentHashMap<>();
        cache.put(99L, currency);
        when(plugin.getCurrencies()).thenReturn(cache);

        when(currencyRepository.delete(99L)).thenReturn(true);
        when(currencyLogRepository.findByCriteria(any(), any(), any(), eq(99L), any(), any(), anyInt())).thenReturn(List.of());

        final List<UserCurrency> userCurrencies = List.of(
                mockUserCurrency(7L, 400.0),
                mockUserCurrency(8L, 275.5)
        );
        when(userCurrencyRepository.findListByAttributes(Map.of("currency.id", 99L))).thenReturn(userCurrencies);

        final RenderContext renderContext = mock(RenderContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        when(renderContext.getPlayer()).thenReturn(this.player);
        this.pluginState.put((StateValueHost) renderContext, plugin);

        final Context clickContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        when(clickContext.getPlayer()).thenReturn(this.player);
        this.pluginState.put((StateValueHost) clickContext, plugin);
        doNothing().when(clickContext).openForPlayer(any(), any());

        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            configureItemTranslations(translations);
            final TranslationService processingService = configureTranslation(translations, "currency_deletion_ui", "delete.processing");
            final TranslationService successService = configureTranslation(translations, "currency_deletion_ui", "delete.success");

            final BukkitItemComponentBuilder builder = mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
            final ArgumentCaptor<Consumer<Context>> clickCaptor = ArgumentCaptor.forClass(Consumer.class);
            when(builder.onClick(clickCaptor.capture())).thenReturn(builder);

            this.view.renderEntry(renderContext, builder, 1, currency);

            try (MockedConstruction<ConfirmationView.Builder> builderConstruction = Mockito.mockConstruction(ConfirmationView.Builder.class, (mockBuilder, context) -> {
                when(mockBuilder.withKey(any())).thenReturn(mockBuilder);
                when(mockBuilder.withMessageKey(any())).thenReturn(mockBuilder);
                when(mockBuilder.withInitialData(anyMap())).thenReturn(mockBuilder);
                when(mockBuilder.withCallback(any())).thenReturn(mockBuilder);
                when(mockBuilder.withParentView(any())).thenReturn(mockBuilder);
            })) {
                clickCaptor.getValue().accept(clickContext);

                final ConfirmationView.Builder constructedBuilder = builderConstruction.constructed().get(0);
                final ArgumentCaptor<Consumer<Boolean>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);
                verify(constructedBuilder).withCallback(callbackCaptor.capture());

                final Consumer<Boolean> callback = callbackCaptor.getValue();
                callback.accept(true);

                verify(userCurrencyRepository).delete(7L);
                verify(userCurrencyRepository).delete(8L);
                verify(currencyRepository).delete(99L);
                assertFalse(cache.containsKey(99L));

                final ArgumentCaptor<Map<String, Object>> processingPlaceholders = ArgumentCaptor.forClass(Map.class);
                verify(processingService).withAll(processingPlaceholders.capture());
                final Map<String, Object> processingValues = processingPlaceholders.getValue();
                assertEquals("emerald", processingValues.get("currency_identifier"));
                assertEquals(2, processingValues.get("affected_players"));

                final ArgumentCaptor<Map<String, Object>> successPlaceholders = ArgumentCaptor.forClass(Map.class);
                verify(successService).withAll(successPlaceholders.capture());
                final Map<String, Object> successValues = successPlaceholders.getValue();
                assertEquals("emerald", successValues.get("currency_identifier"));
                assertEquals(2, successValues.get("affected_players"));
                assertEquals("675.5", successValues.get("total_balance_removed"));
            }
        }
    }

    @Test
    void cancellationCallbackSendsNotificationAndRespectsNavigationChain() throws Exception {
        final Currency currency = mock(Currency.class);
        when(currency.getId()).thenReturn(13L);
        when(currency.getIdentifier()).thenReturn("token");
        when(currency.getSymbol()).thenReturn("T");

        final JExEconomyImpl plugin = mock(JExEconomyImpl.class);
        when(plugin.getUserCurrencyRepository()).thenReturn(mock(UserCurrencyRepository.class));
        when(plugin.getExecutor()).thenReturn(new DirectExecutorService());

        final RenderContext renderContext = mock(RenderContext.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        when(renderContext.getPlayer()).thenReturn(this.player);
        this.pluginState.put((StateValueHost) renderContext, plugin);

        final Context clickContext = mock(Context.class, Mockito.withSettings().extraInterfaces(StateValueHost.class));
        when(clickContext.getPlayer()).thenReturn(this.player);
        this.pluginState.put((StateValueHost) clickContext, plugin);
        doNothing().when(clickContext).openForPlayer(any(), any());

        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            configureItemTranslations(translations);
            final TranslationService cancelledService = configureTranslation(translations, "currency_deletion_ui", "delete.cancelled");

            final BukkitItemComponentBuilder builder = mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
            final ArgumentCaptor<Consumer<Context>> clickCaptor = ArgumentCaptor.forClass(Consumer.class);
            when(builder.onClick(clickCaptor.capture())).thenReturn(builder);

            this.view.renderEntry(renderContext, builder, 3, currency);

            try (MockedConstruction<ConfirmationView.Builder> builderConstruction = Mockito.mockConstruction(ConfirmationView.Builder.class, (mockBuilder, context) -> {
                when(mockBuilder.withKey(any())).thenReturn(mockBuilder);
                when(mockBuilder.withMessageKey(any())).thenReturn(mockBuilder);
                when(mockBuilder.withInitialData(anyMap())).thenReturn(mockBuilder);
                when(mockBuilder.withCallback(any())).thenReturn(mockBuilder);
                when(mockBuilder.withParentView(any())).thenReturn(mockBuilder);
            })) {
                clickCaptor.getValue().accept(clickContext);

                final ConfirmationView.Builder constructedBuilder = builderConstruction.constructed().get(0);
                final ArgumentCaptor<Consumer<Boolean>> callbackCaptor = ArgumentCaptor.forClass(Consumer.class);
                verify(constructedBuilder).withCallback(callbackCaptor.capture());

                callbackCaptor.getValue().accept(false);

                verify(cancelledService).withPrefix();
                verify(cancelledService).with(eq("currency_identifier"), eq("token"));
                verify(cancelledService).send();
            }
        }

        assertEquals(CurrenciesActionOverviewView.class, this.view.getParentView());
    }

    private void configureItemTranslations(final MockedStatic<TranslationService> translations) {
        configureTranslation(translations, "currency_deletion_ui", "currency.name");
        configureTranslation(translations, "currency_deletion_ui", "currency.lore");
    }

    private TranslationService configureTranslation(
            final MockedStatic<TranslationService> translations,
            final String baseKey,
            final String suffix
    ) {
        final TranslationService service = mock(TranslationService.class, Mockito.RETURNS_SELF);
        final TranslatedMessage message = new TranslatedMessage(Component.empty(), TranslationKey.of(baseKey, suffix));
        when(service.build()).thenReturn(message);
        doNothing().when(service).send();
        translations.when(() -> TranslationService.create(TranslationKey.of(baseKey, suffix), this.player)).thenReturn(service);
        return service;
    }

    private UserCurrency mockUserCurrency(final long id, final double balance) {
        final UserCurrency userCurrency = mock(UserCurrency.class);
        when(userCurrency.getId()).thenReturn(id);
        when(userCurrency.getBalance()).thenReturn(balance);
        return userCurrency;
    }

    private <T> StubState<T> injectState(final String fieldName) throws Exception {
        final Field field = CurrencyDeletionView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final StubState<T> stubState = new StubState<>();
        field.set(this.view, stubState);
        return stubState;
    }

    private static final class TestCurrencyDeletionView extends CurrencyDeletionView {
        Class<?> getParentView() {
            return this.parentClazz;
        }
    }

    private static final class StubState<T> implements State<T> {

        private static final StateValueFactory NOOP_FACTORY = new StateValueFactory() {
            @Override
            public StateValue create(final StateValueHost host, final State<?> state) {
                throw new UnsupportedOperationException("State factory not required for tests.");
            }
        };

        private final Map<StateValueHost, T> values = new IdentityHashMap<>();

        void put(final StateValueHost host, final T value) {
            this.values.put(host, value);
        }

        @Override
        public T get(final StateValueHost host) {
            return this.values.get(host);
        }

        @Override
        public StateValueFactory factory() {
            return NOOP_FACTORY;
        }

        @Override
        public long internalId() {
            return 0L;
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
            command.run();
        }
    }
}
