package de.jexcellence.economy.currency;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueFactory;
import me.devnatan.inventoryframework.state.StateValueHost;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyDetailViewTest {

        private ServerMock server;
        private PlayerMock viewer;
        private CurrencyDetailView view;
        private StubState<JExEconomyImpl> pluginState;
        private StubState<Currency> currencyState;

        @BeforeEach
        void setUp() throws Exception {
                this.server = MockBukkit.mock();
                this.viewer = this.server.addPlayer("Viewer");
                this.view = Mockito.spy(new CurrencyDetailView());
                this.pluginState = this.injectState("jexEconomy");
                this.currencyState = this.injectState("targetCurrency");
        }

        @AfterEach
        void tearDown() {
                MockBukkit.unmock();
        }

        @Test
        void onFirstRenderPopulatesTranslatedPlaceholders() {
                final Currency currency = new Currency(
                        "Gold: ",
                        " coins",
                        "gold",
                        "G",
                        Material.GOLD_INGOT
                );

                final JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
                final RenderContext renderContext = Mockito.mock(
                        RenderContext.class,
                        Mockito.withSettings().extraInterfaces(StateValueHost.class)
                );

                this.pluginState.put((StateValueHost) renderContext, plugin);
                this.currencyState.put((StateValueHost) renderContext, currency);

                final Map<String, TranslationService> translations = this.prepareTranslations(this.viewer);
                this.stubTranslations(translations);

                final Map<Character, LayoutSlotCapture> slotCaptures = new HashMap<>();
                this.stubLayoutSlots(renderContext, slotCaptures);

                try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class)) {
                        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(Material.class)))
                                .thenAnswer(materialInvocation -> new CapturingItemBuilder((Material) materialInvocation.getArgument(0)));
                        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(ItemStack.class)))
                                .thenAnswer(stackInvocation -> new CapturingItemBuilder(((ItemStack) stackInvocation.getArgument(0)).clone()));

                        this.view.onFirstRender(renderContext, this.viewer);
                }

                Mockito.verify(translations.get("currency_icon.name"))
                        .with("currency_identifier", "gold");

                final ArgumentCaptor<Map<String, Object>> iconLoreCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(translations.get("currency_icon.lore")).withAll(iconLoreCaptor.capture());
                final Map<String, Object> iconLorePlaceholders = iconLoreCaptor.getValue();
                assertEquals(currency.getIcon().translationKey(), iconLorePlaceholders.get("material_name"));
                assertEquals("gold", iconLorePlaceholders.get("currency_identifier"));

                Mockito.verify(translations.get("currency_symbol.name"))
                        .with("currency_symbol", "G");

                final ArgumentCaptor<Map<String, Object>> symbolLoreCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(translations.get("currency_symbol.lore")).withAll(symbolLoreCaptor.capture());
                final Map<String, Object> symbolLore = symbolLoreCaptor.getValue();
                assertEquals("G", symbolLore.get("currency_symbol"));
                assertEquals("gold", symbolLore.get("currency_identifier"));

                final ArgumentCaptor<Map<String, Object>> prefixLoreCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(translations.get("currency_prefix.lore")).withAll(prefixLoreCaptor.capture());
                final Map<String, Object> prefixLore = prefixLoreCaptor.getValue();
                assertEquals("Gold: ", prefixLore.get("currency_prefix"));
                assertEquals("true", prefixLore.get("has_prefix"));

                final ArgumentCaptor<Map<String, Object>> suffixLoreCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(translations.get("currency_suffix.lore")).withAll(suffixLoreCaptor.capture());
                final Map<String, Object> suffixLore = suffixLoreCaptor.getValue();
                assertEquals(" coins", suffixLore.get("currency_suffix"));
                assertEquals("true", suffixLore.get("has_suffix"));

                Mockito.verify(translations.get("leaderboard.name"))
                        .with("currency_identifier", "gold");

                final ArgumentCaptor<Map<String, Object>> leaderboardLoreCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(translations.get("leaderboard.lore")).withAll(leaderboardLoreCaptor.capture());
                final Map<String, Object> leaderboardLore = leaderboardLoreCaptor.getValue();
                assertEquals("gold", leaderboardLore.get("currency_identifier"));
                assertEquals("G", leaderboardLore.get("currency_symbol"));

                Mockito.verify(translations.get("reset_all.name"))
                        .with("currency_identifier", "gold");

                final ArgumentCaptor<Map<String, Object>> resetLoreCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(translations.get("reset_all.lore")).withAll(resetLoreCaptor.capture());
                final Map<String, Object> resetLore = resetLoreCaptor.getValue();
                assertEquals("gold", resetLore.get("currency_identifier"));
                assertEquals("G", resetLore.get("currency_symbol"));
        }

        @Test
        void interactiveComponentsNavigateAndEnforcePermissions() throws Exception {
                final Currency currency = new Currency(
                        "",
                        "",
                        "emerald",
                        "E",
                        Material.EMERALD
                );

                final JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
                final RenderContext renderContext = Mockito.mock(
                        RenderContext.class,
                        Mockito.withSettings().extraInterfaces(StateValueHost.class)
                );

                this.pluginState.put((StateValueHost) renderContext, plugin);
                this.currencyState.put((StateValueHost) renderContext, currency);

                final Map<String, TranslationService> translations = this.prepareTranslations(this.viewer);
                this.stubTranslations(translations);

                final Map<Character, LayoutSlotCapture> slotCaptures = new HashMap<>();
                this.stubLayoutSlots(renderContext, slotCaptures);

                try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class)) {
                        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(Material.class)))
                                .thenAnswer(materialInvocation -> new CapturingItemBuilder((Material) materialInvocation.getArgument(0)));
                        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(ItemStack.class)))
                                .thenAnswer(stackInvocation -> new CapturingItemBuilder(((ItemStack) stackInvocation.getArgument(0)).clone()));

                        this.view.onFirstRender(renderContext, this.viewer);
                }

                final LayoutSlotCapture leaderboardSlot = Optional.ofNullable(slotCaptures.get('l'))
                        .orElseThrow(() -> new AssertionError("Leaderboard slot should be registered"));
                final Object leaderboardHandler = leaderboardSlot.getClickHandler();
                assertNotNull(leaderboardHandler, "Leaderboard slot must configure an onClick handler");

                @SuppressWarnings("unchecked")
                final Consumer<SlotClickContext> leaderboardConsumer = (Consumer<SlotClickContext>) leaderboardHandler;
                final SlotClickContext leaderboardContext = Mockito.mock(
                        SlotClickContext.class,
                        Mockito.withSettings().extraInterfaces(StateValueHost.class)
                );
                final Map<String, Object> initialData = Map.of("source", "unit-test");
                Mockito.when(leaderboardContext.getInitialData()).thenReturn(initialData);
                this.pluginState.put((StateValueHost) leaderboardContext, plugin);
                this.currencyState.put((StateValueHost) leaderboardContext, currency);

                leaderboardConsumer.accept(leaderboardContext);

                final ArgumentCaptor<Map<String, Object>> navigationCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(leaderboardContext).openForPlayer(
                        Mockito.eq(CurrencyLeaderboardView.class),
                        navigationCaptor.capture()
                );

                final Map<String, Object> navigationData = navigationCaptor.getValue();
                assertSame(plugin, navigationData.get("plugin"));
                assertSame(currency, navigationData.get("currency"));
                assertEquals(CurrencyDetailView.class, navigationData.get("parentClazz"));
                assertSame(initialData, navigationData.get("initialData"));

                final LayoutSlotCapture resetSlot = Optional.ofNullable(slotCaptures.get('r'))
                        .orElseThrow(() -> new AssertionError("Reset slot should be registered"));
                final Object displayPredicate = resetSlot.getDisplayPredicate();
                assertNotNull(displayPredicate, "Reset slot must define a display predicate");

                final PlayerMock authorized = this.server.addPlayer("Admin");
                authorized.setOp(true);
                final PlayerMock unauthorized = this.server.addPlayer("Guest");

                final SlotClickContext authorizedDisplayContext = Mockito.mock(SlotClickContext.class);
                Mockito.when(authorizedDisplayContext.getPlayer()).thenReturn(authorized);

                final SlotClickContext unauthorizedDisplayContext = Mockito.mock(SlotClickContext.class);
                Mockito.when(unauthorizedDisplayContext.getPlayer()).thenReturn(unauthorized);

                final Method testMethod = displayPredicate.getClass().getMethod("test", Object.class);
                final boolean authorizedVisible = (boolean) testMethod.invoke(displayPredicate, authorizedDisplayContext);
                final boolean unauthorizedVisible = (boolean) testMethod.invoke(displayPredicate, unauthorizedDisplayContext);

                assertTrue(authorizedVisible, "Administrators should see the reset control");
                assertFalse(unauthorizedVisible, "Players without permission must not see the reset control");
        }

        private Map<String, TranslationService> prepareTranslations(final Player player) {
                final Map<String, TranslationService> translations = new HashMap<>();
                this.registerTranslation(translations, "currency_icon.name");
                this.registerTranslation(translations, "currency_icon.lore");
                this.registerTranslation(translations, "currency_symbol.name");
                this.registerTranslation(translations, "currency_symbol.lore");
                this.registerTranslation(translations, "currency_prefix.name");
                this.registerTranslation(translations, "currency_prefix.lore");
                this.registerTranslation(translations, "currency_suffix.name");
                this.registerTranslation(translations, "currency_suffix.lore");
                this.registerTranslation(translations, "leaderboard.name");
                this.registerTranslation(translations, "leaderboard.lore");
                this.registerTranslation(translations, "reset_all.name");
                this.registerTranslation(translations, "reset_all.lore");
                return translations;
        }

        private void stubTranslations(final Map<String, TranslationService> translations) {
                Mockito.doAnswer(invocation -> {
                        final String suffix = invocation.getArgument(0, String.class);
                        final Player contextPlayer = invocation.getArgument(1, Player.class);
                        assertSame(this.viewer, contextPlayer, "Unexpected player context for translation request");
                        final TranslationService translation = translations.get(suffix);
                        return Objects.requireNonNull(translation, () -> "Missing translation mock for suffix: " + suffix);
                }).when(this.view).i18n(Mockito.anyString(), Mockito.any(Player.class));
        }

        private TranslationService registerTranslation(
                final Map<String, TranslationService> translations,
                final String suffix
        ) {
                final TranslationService translation = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
                final TranslatedMessage message = Mockito.mock(TranslatedMessage.class);
                Mockito.when(translation.build()).thenReturn(message);
                Mockito.when(message.component()).thenReturn(Component.text(suffix + "-component"));
                Mockito.when(message.splitLines()).thenReturn(List.of(Component.text(suffix + "-line")));
                translations.put(suffix, translation);
                return translation;
        }

        private void stubLayoutSlots(
                final RenderContext renderContext,
                final Map<Character, LayoutSlotCapture> slotCaptures
        ) {
                final Answer<Object> layoutAnswer = invocation -> {
                        final char layoutKey = invocation.getArgument(0, Character.class);
                        final Class<?> returnType = invocation.getMethod().getReturnType();
                        final LayoutSlotCapture capture = new LayoutSlotCapture(returnType);
                        slotCaptures.put(layoutKey, capture);
                        return capture.getProxy();
                };

                Mockito.when(renderContext.layoutSlot(Mockito.anyChar(), Mockito.any()))
                        .thenAnswer(layoutAnswer);
        }

        private <T> StubState<T> injectState(final String fieldName) throws Exception {
                final Field field = CurrencyDetailView.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                final StubState<T> stub = new StubState<>();
                field.set(this.view, stub);
                return stub;
        }

        private static final class CapturingItemBuilder implements IUnifiedItemBuilder<ItemMeta, CapturingItemBuilder> {

                private final ItemStack base;

                CapturingItemBuilder(final Material material) {
                        this.base = new ItemStack(material);
                }

                CapturingItemBuilder(final ItemStack stack) {
                        this.base = stack;
                }

                @Override
                public CapturingItemBuilder setName(final @NotNull Component name) {
                        return this;
                }

                @Override
                public CapturingItemBuilder setLore(final @NotNull List<Component> lore) {
                        return this;
                }

                @Override
                public CapturingItemBuilder addLoreLine(final @NotNull Component line) {
                        return this;
                }

                @Override
                public CapturingItemBuilder addLoreLines(final @NotNull List<Component> lore) {
                        return this;
                }

                @Override
                public CapturingItemBuilder addLoreLines(final @NotNull Component... lore) {
                        return this;
                }

                @Override
                public CapturingItemBuilder setAmount(final int amount) {
                        this.base.setAmount(amount);
                        return this;
                }

                @Override
                public CapturingItemBuilder setCustomModelData(final int data) {
                        return this;
                }

                @Override
                public CapturingItemBuilder addEnchantment(final @NotNull org.bukkit.enchantments.Enchantment enchantment, final int level) {
                        return this;
                }

                @Override
                public CapturingItemBuilder addItemFlags(final @NotNull ItemFlag... flags) {
                        return this;
                }

                @Override
                public CapturingItemBuilder setGlowing(final boolean glowing) {
                        return this;
                }

                @Override
                public ItemStack build() {
                        return this.base;
                }
        }

        private static final class LayoutSlotCapture implements InvocationHandler {

                private final Object proxy;
                private Object clickHandler;
                private Object displayPredicate;

                LayoutSlotCapture(final Class<?> slotType) {
                        this.proxy = Proxy.newProxyInstance(
                                slotType.getClassLoader(),
                                new Class<?>[]{slotType},
                                this
                        );
                }

                Object getProxy() {
                        return this.proxy;
                }

                Object getClickHandler() {
                        return this.clickHandler;
                }

                Object getDisplayPredicate() {
                        return this.displayPredicate;
                }

                @Override
                public Object invoke(final Object proxy, final Method method, final Object[] args) {
                        final String methodName = method.getName();
                        if ("onClick".equals(methodName) && args != null && args.length == 1) {
                                this.clickHandler = args[0];
                                return proxy;
                        }
                        if ("displayIf".equals(methodName) && args != null && args.length == 1) {
                                this.displayPredicate = args[0];
                                return proxy;
                        }
                        if (method.getReturnType().isPrimitive()) {
                                if (boolean.class.equals(method.getReturnType())) {
                                        return false;
                                }
                                if (char.class.equals(method.getReturnType())) {
                                        return '\0';
                                }
                                if (byte.class.equals(method.getReturnType()) || short.class.equals(method.getReturnType()) || int.class.equals(method.getReturnType())) {
                                        return 0;
                                }
                                if (long.class.equals(method.getReturnType())) {
                                        return 0L;
                                }
                                if (float.class.equals(method.getReturnType())) {
                                        return 0.0f;
                                }
                                if (double.class.equals(method.getReturnType())) {
                                        return 0.0d;
                                }
                        }
                        return null;
                }
        }

        private static final class StubState<T> implements State<T> {

                private static final StateValueFactory FACTORY = new StateValueFactory() {
                        @Override
                        public StateValue create(final @NotNull StateValueHost host, final @NotNull State<?> state) {
                                throw new UnsupportedOperationException("Factory access not required for tests");
                        }
                };

                private final Map<StateValueHost, T> values = new IdentityHashMap<>();

                void put(final StateValueHost host, final T value) {
                        this.values.put(host, value);
                }

                @Override
                public T get(final @NotNull StateValueHost host) {
                        return this.values.get(host);
                }

                @Override
                public StateValueFactory factory() {
                        return FACTORY;
                }

                @Override
                public long internalId() {
                        return 0L;
                }
        }
}
