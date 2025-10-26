package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.UserCurrencyRepository;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyLeaderboardViewTest {

        private CurrencyLeaderboardView view;
        private State<JExEconomyImpl>   pluginState;
        private State<Currency>         currencyState;
        private JExEconomyImpl          plugin;
        private UserCurrencyRepository  repository;
        private Currency                currency;
        private Context                 context;

        @BeforeEach
        void setUp() throws ReflectiveOperationException {
                this.view = new CurrencyLeaderboardView();
                this.pluginState = mock(State.class);
                this.currencyState = mock(State.class);
                this.plugin = mock(JExEconomyImpl.class);
                this.repository = mock(UserCurrencyRepository.class);
                this.currency = new Currency(
                        "$",
                        " coins",
                        "aurum",
                        "AUR",
                        Material.GOLD_INGOT
                );
                this.context = mock(Context.class);

                when(this.plugin.getUserCurrencyRepository()).thenReturn(this.repository);
                when(this.pluginState.get(any(Context.class))).thenReturn(this.plugin);
                when(this.currencyState.get(any(Context.class))).thenReturn(this.currency);

                injectState("jexEconomy", this.pluginState);
                injectState("targetCurrency", this.currencyState);
        }

        @Test
        void getAsyncPaginationSourceSortsDescendingAndPreservesTieOrder() {
                final UserCurrency runnerUp = createUserCurrency("Runner", 200.0);
                final UserCurrency zeroBalance = createUserCurrency("Empty", 0.0);
                final UserCurrency tiedSecondB = createUserCurrency("Tie-B", 350.0);
                final UserCurrency leader = createUserCurrency("Champion", 500.0);
                final UserCurrency tiedSecondA = createUserCurrency("Tie-A", 350.0);

                final List<UserCurrency> unsortedResults = List.of(
                        runnerUp,
                        zeroBalance,
                        tiedSecondB,
                        leader,
                        tiedSecondA
                );

                when(this.repository.findTopByCurrency(this.currency, 25))
                        .thenReturn(CompletableFuture.completedFuture(unsortedResults));

                final List<UserCurrency> sortedLeaderboard = this.view.getAsyncPaginationSource(this.context).join();

                assertEquals(4, sortedLeaderboard.size());
                assertEquals(List.of(
                        leader,
                        tiedSecondB,
                        tiedSecondA,
                        runnerUp
                ), sortedLeaderboard);
        }

        @Test
        void getAsyncPaginationSourceReturnsEmptyWhenNoBalancesRemain() {
                final UserCurrency zeroBalance = createUserCurrency("Empty", 0.0);

                when(this.repository.findTopByCurrency(this.currency, 25))
                        .thenReturn(CompletableFuture.completedFuture(List.of(zeroBalance)));

                final List<UserCurrency> sortedLeaderboard = this.view.getAsyncPaginationSource(this.context).join();

                assertTrue(sortedLeaderboard.isEmpty());
        }

        @Test
        void renderEntryPopulatesNameAndLorePlaceholders() {
                final RenderContext renderContext = mock(RenderContext.class);
                final Player        viewer        = mock(Player.class);
                final BukkitItemComponentBuilder itemComponentBuilder = mock(
                        BukkitItemComponentBuilder.class,
                        Answers.RETURNS_SELF
                );
                when(renderContext.getPlayer()).thenReturn(viewer);
                when(this.pluginState.get(any(RenderContext.class))).thenReturn(this.plugin);
                when(this.currencyState.get(any(RenderContext.class))).thenReturn(this.currency);

                final UserCurrency topEntry = createUserCurrency("TopPlayer", 1234.5);

                final List<Map<String, Object>> placeholderCalls = new ArrayList<>();

                try (MockedStatic<TranslationService> translationMock = Mockito.mockStatic(TranslationService.class);
                     MockedStatic<UnifiedBuilderFactory> unifiedBuilderMock = Mockito.mockStatic(UnifiedBuilderFactory.class)) {

                        translationMock.when(() -> TranslationService.create(any(), any())).thenAnswer(invocation -> {
                                final TranslationService translationBuilder = mock(TranslationService.class);
                                final TranslatedMessage  translatedMessage  = mock(TranslatedMessage.class);

                                when(translationBuilder.withAll(Mockito.<Map<String, Object>>any())).thenAnswer(mapInvocation -> {
                                        placeholderCalls.add(Map.copyOf(mapInvocation.getArgument(0)));
                                        return translationBuilder;
                                });
                                when(translationBuilder.build()).thenReturn(translatedMessage);
                                when(translatedMessage.component()).thenReturn(Component.empty());
                                when(translatedMessage.splitLines()).thenReturn(List.of());
                                return translationBuilder;
                        });

                        final IUnifiedItemBuilder<?, ?> itemBuilder = mock(IUnifiedItemBuilder.class, Answers.RETURNS_SELF);
                        final ItemStack                builtItem   = mock(ItemStack.class);
                        when(itemBuilder.build()).thenReturn(builtItem);

                        unifiedBuilderMock.when(() -> UnifiedBuilderFactory.item(any(Material.class))).thenReturn(itemBuilder);

                        this.view.renderEntry(renderContext, itemComponentBuilder, 0, topEntry);
                }

                assertEquals(2, placeholderCalls.size());

                final Map<String, Object> namePlaceholders = placeholderCalls
                        .stream()
                        .filter(placeholders -> !placeholders.containsKey("player_uuid"))
                        .findFirst()
                        .orElseThrow();

                final Map<String, Object> lorePlaceholders = placeholderCalls
                        .stream()
                        .filter(placeholders -> placeholders.containsKey("player_uuid"))
                        .findFirst()
                        .orElseThrow();

                assertEquals(1, namePlaceholders.get("rank"));
                assertEquals("<gradient:#FFD700:#FFA500>", namePlaceholders.get("rank_color"));
                assertEquals(topEntry.getPlayer().getPlayerName(), namePlaceholders.get("player_name"));
                assertEquals(this.currency.getSymbol(), namePlaceholders.get("currency_symbol"));

                final DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
                final String        formattedAmount = decimalFormat.format(topEntry.getBalance());
                final String        formattedWithCurrency = this.currency.getPrefix() + formattedAmount + " "
                        + this.currency.getSymbol() + this.currency.getSuffix();

                assertEquals(1, lorePlaceholders.get("rank"));
                assertEquals("<gradient:#FFD700:#FFA500>", lorePlaceholders.get("rank_color"));
                assertEquals(topEntry.getPlayer().getPlayerName(), lorePlaceholders.get("player_name"));
                assertEquals(topEntry.getPlayer().getUniqueId(), lorePlaceholders.get("player_uuid"));
                assertEquals(formattedAmount, lorePlaceholders.get("balance"));
                assertEquals(this.currency.getSymbol(), lorePlaceholders.get("currency_symbol"));
                assertEquals(this.currency.getIdentifier(), lorePlaceholders.get("currency_identifier"));
                assertEquals(formattedWithCurrency, lorePlaceholders.get("formatted_balance"));
        }

        @Test
        void onPaginatedRenderBuildsCurrencyInfoWithPlaceholders() {
                final RenderContext renderContext = mock(RenderContext.class);
                final Player        viewer        = mock(Player.class);
                when(renderContext.getPlayer()).thenReturn(viewer);
                when(this.pluginState.get(any(RenderContext.class))).thenReturn(this.plugin);
                when(this.currencyState.get(any(RenderContext.class))).thenReturn(this.currency);

                final List<Map<String, Object>> placeholderCalls = new ArrayList<>();

                try (MockedStatic<TranslationService> translationMock = Mockito.mockStatic(TranslationService.class);
                     MockedStatic<UnifiedBuilderFactory> unifiedBuilderMock = Mockito.mockStatic(UnifiedBuilderFactory.class)) {

                        translationMock.when(() -> TranslationService.create(any(), any())).thenAnswer(invocation -> {
                                final TranslationService translationBuilder = mock(TranslationService.class);
                                final TranslatedMessage  translatedMessage  = mock(TranslatedMessage.class);

                                when(translationBuilder.withAll(Mockito.<Map<String, Object>>any())).thenAnswer(mapInvocation -> {
                                        placeholderCalls.add(Map.copyOf(mapInvocation.getArgument(0)));
                                        return translationBuilder;
                                });
                                when(translationBuilder.build()).thenReturn(translatedMessage);
                                when(translatedMessage.component()).thenReturn(Component.empty());
                                when(translatedMessage.splitLines()).thenReturn(List.of());
                                return translationBuilder;
                        });

                        final IUnifiedItemBuilder<?, ?> itemBuilder = mock(IUnifiedItemBuilder.class, Answers.RETURNS_SELF);
                        final ItemStack                headerItem  = mock(ItemStack.class);
                        when(itemBuilder.build()).thenReturn(headerItem);

                        unifiedBuilderMock.when(() -> UnifiedBuilderFactory.item(any(Material.class))).thenReturn(itemBuilder);

                        this.view.onPaginatedRender(renderContext, viewer);

                        verify(renderContext).slot(1, 5, headerItem);
                }

                assertEquals(2, placeholderCalls.size());

                for (final Map<String, Object> placeholderMap : placeholderCalls) {
                        assertEquals(this.currency.getIdentifier(), placeholderMap.get("currency_identifier"));
                        assertEquals(this.currency.getSymbol(), placeholderMap.get("currency_symbol"));
                }
        }

        @Test
        void layoutDefinesPaginationControlsRow() {
                assertArrayEquals(new String[]{
                        "         ",
                        "         ",
                        "         ",
                        "  OOOOO  ",
                        "  OOOOO  ",
                        "   <p>   "
                }, this.view.getLayout());
        }

        private UserCurrency createUserCurrency(final String name, final double balance) {
                final UUID playerId = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                final User player = new User(playerId, name);
                return new UserCurrency(player, this.currency, balance);
        }

        @SuppressWarnings("unchecked")
        private <T> void injectState(
                final String fieldName,
                final State<T> state
        ) throws ReflectiveOperationException {
                final Field field = CurrencyLeaderboardView.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(this.view, state);
        }
}
