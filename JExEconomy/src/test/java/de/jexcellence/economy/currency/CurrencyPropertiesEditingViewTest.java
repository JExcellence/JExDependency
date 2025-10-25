package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.currency.anvil.CurrencyPrefixAnvilView;
import de.jexcellence.economy.currency.anvil.CurrencySuffixAnvilView;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyPropertiesEditingViewTest {

    private static final String BASE_KEY = "currency_properties_editing_ui";

    private CurrencyPropertiesEditingView view;
    private MutableState<Currency> currencyState;
    private State<JExEconomyImpl> pluginState;
    private RenderContext renderContext;
    private Player player;
    private Currency currency;
    private MockedStatic<TranslationService> translationServiceMock;
    private MockedStatic<UnifiedBuilderFactory> unifiedBuilderFactoryMock;

    @BeforeEach
    void setUp() throws Exception {
        view = new CurrencyPropertiesEditingView();
        currencyState = Mockito.mock(MutableState.class);
        pluginState = Mockito.mock(State.class);
        renderContext = Mockito.mock(RenderContext.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));
        player = Mockito.mock(Player.class);
        currency = new Currency("§f", " credits", "credits", "¤", Material.EMERALD);

        injectState("targetCurrency", currencyState);
        injectState("jexEconomy", pluginState);

        translationServiceMock = Mockito.mockStatic(TranslationService.class);
        unifiedBuilderFactoryMock = Mockito.mockStatic(UnifiedBuilderFactory.class);
    }

    @AfterEach
    void tearDown() {
        translationServiceMock.close();
        unifiedBuilderFactoryMock.close();
    }

    @Test
    void onFirstRenderPopulatesExistingCurrencyFormatting() {
        Map<String, Object> initialData = Map.of("currency", currency);
        when(renderContext.getInitialData()).thenReturn(initialData);
        when(currencyState.get(renderContext)).thenReturn(currency);

        BukkitItemComponentBuilder identifierSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        BukkitItemComponentBuilder symbolSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        BukkitItemComponentBuilder iconSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        BukkitItemComponentBuilder prefixSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        BukkitItemComponentBuilder suffixSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        BukkitItemComponentBuilder saveSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);

        Mockito.when(renderContext.layoutSlot(eq('n'), any(ItemStack.class))).thenReturn(identifierSlot);
        Mockito.when(renderContext.layoutSlot(eq('s'), any(ItemStack.class))).thenReturn(symbolSlot);
        Mockito.when(renderContext.layoutSlot(eq('i'), any(ItemStack.class))).thenReturn(iconSlot);
        Mockito.when(renderContext.layoutSlot(eq('p'), any(ItemStack.class))).thenReturn(prefixSlot);
        Mockito.when(renderContext.layoutSlot(eq('f'), any(ItemStack.class))).thenReturn(suffixSlot);
        Mockito.when(renderContext.layoutSlot(eq('v'), any(ItemStack.class))).thenReturn(saveSlot);

        registerBuilder(Material.NAME_TAG);
        registerBuilder(Material.GOLD_NUGGET);
        registerBuilder(currency.getIcon());
        registerBuilder(Material.WRITABLE_BOOK);
        registerBuilder(Material.PAPER);
        registerSaveHeadBuilders();
        registerHeadTranslations();

        TranslationService prefixNameService = registerTranslation("prefix.name");
        TranslationService prefixLoreService = registerTranslation("prefix.lore");
        TranslationService suffixNameService = registerTranslation("suffix.name");
        TranslationService suffixLoreService = registerTranslation("suffix.lore");

        // register remaining keys to satisfy builder wiring
        registerTranslation("identifier.name");
        registerTranslation("identifier.lore");
        registerTranslation("symbol.name");
        registerTranslation("symbol.lore");
        registerTranslation("icon.name");
        registerTranslation("icon.lore");
        registerTranslation("save_changes.name");
        registerTranslation("save_changes.lore");

        view.onFirstRender(renderContext, player);

        verify(currencyState).set(currency, renderContext);

        verify(prefixNameService).with("currency_prefix", currency.getPrefix());
        ArgumentCaptor<Map<String, Object>> prefixLoreCaptor = ArgumentCaptor.forClass(Map.class);
        verify(prefixLoreService).withAll(prefixLoreCaptor.capture());
        Map<String, Object> prefixPlaceholders = prefixLoreCaptor.getValue();
        assertEquals(currency.getPrefix(), prefixPlaceholders.get("currency_prefix"));
        assertEquals("true", prefixPlaceholders.get("has_prefix"));

        verify(suffixNameService).with("currency_suffix", currency.getSuffix());
        ArgumentCaptor<Map<String, Object>> suffixLoreCaptor = ArgumentCaptor.forClass(Map.class);
        verify(suffixLoreService).withAll(suffixLoreCaptor.capture());
        Map<String, Object> suffixPlaceholders = suffixLoreCaptor.getValue();
        assertEquals(currency.getSuffix(), suffixPlaceholders.get("currency_suffix"));
        assertEquals("true", suffixPlaceholders.get("has_suffix"));
    }

    @Test
    void prefixAndSuffixEditorsReuseExistingStateOnClick() {
        Map<String, Object> initialData = Map.of("currency", currency);
        when(renderContext.getInitialData()).thenReturn(initialData);
        when(currencyState.get(renderContext)).thenReturn(currency);

        BukkitItemComponentBuilder prefixSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        BukkitItemComponentBuilder suffixSlot = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);

        Mockito.when(renderContext.layoutSlot(eq('p'), any(ItemStack.class))).thenReturn(prefixSlot);
        Mockito.when(renderContext.layoutSlot(eq('f'), any(ItemStack.class))).thenReturn(suffixSlot);

        // ensure other layout slots do not fail
        registerBuilder(Material.NAME_TAG);
        registerBuilder(Material.GOLD_NUGGET);
        registerBuilder(currency.getIcon());
        registerBuilder(Material.WRITABLE_BOOK);
        registerBuilder(Material.PAPER);
        registerSaveHeadBuilders();
        registerHeadTranslations();

        Mockito.when(renderContext.layoutSlot(eq('n'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));
        Mockito.when(renderContext.layoutSlot(eq('s'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));
        Mockito.when(renderContext.layoutSlot(eq('i'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));
        Mockito.when(renderContext.layoutSlot(eq('v'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));

        registerTranslation("identifier.name");
        registerTranslation("identifier.lore");
        registerTranslation("symbol.name");
        registerTranslation("symbol.lore");
        registerTranslation("icon.name");
        registerTranslation("icon.lore");
        registerTranslation("prefix.name");
        registerTranslation("prefix.lore");
        registerTranslation("suffix.name");
        registerTranslation("suffix.lore");
        registerTranslation("save_changes.name");
        registerTranslation("save_changes.lore");

        view.onFirstRender(renderContext, player);

        ArgumentCaptor<Consumer<SlotClickContext>> prefixClickCaptor = ArgumentCaptor.forClass(Consumer.class);
        ArgumentCaptor<Consumer<SlotClickContext>> suffixClickCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(prefixSlot).onClick(prefixClickCaptor.capture());
        verify(suffixSlot).onClick(suffixClickCaptor.capture());

        SlotClickContext prefixClick = Mockito.mock(SlotClickContext.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));
        SlotClickContext suffixClick = Mockito.mock(SlotClickContext.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));

        when(currencyState.get(prefixClick)).thenReturn(currency);
        when(currencyState.get(suffixClick)).thenReturn(currency);
        JExEconomyImpl prefixPlugin = Mockito.mock(JExEconomyImpl.class);
        JExEconomyImpl suffixPlugin = Mockito.mock(JExEconomyImpl.class);
        when(pluginState.get(prefixClick)).thenReturn(prefixPlugin);
        when(pluginState.get(suffixClick)).thenReturn(suffixPlugin);

        prefixClickCaptor.getValue().accept(prefixClick);
        suffixClickCaptor.getValue().accept(suffixClick);

        verify(prefixClick).openForPlayer(eq(CurrencyPrefixAnvilView.class), Mockito.argThat(data ->
                currency.equals(data.get("currency")) &&
                        prefixPlugin.equals(data.get("plugin"))
        ));
        verify(suffixClick).openForPlayer(eq(CurrencySuffixAnvilView.class), Mockito.argThat(data ->
                currency.equals(data.get("currency")) &&
                        suffixPlugin.equals(data.get("plugin"))
        ));
    }

    @Test
    void onResumeUpdatesCurrencyStateAndRefreshesView() {
        Context originContext = Mockito.mock(Context.class);
        Context targetContext = Mockito.mock(Context.class);
        Currency updated = new Currency("$", " dollars", "usd", "$", Material.DIAMOND);
        when(originContext.getInitialData()).thenReturn(Map.of("currency", updated));

        view.onResume(originContext, targetContext);

        verify(currencyState).set(updated, targetContext);
        verify(targetContext).update();
    }

    @Test
    void saveHandlerPersistsCurrencyAndClosesInventory() throws Exception {
        SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));
        JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        CurrencyRepository repository = Mockito.mock(CurrencyRepository.class);
        Map<Long, Currency> cache = Mockito.mock(Map.class);

        when(currencyState.get(clickContext)).thenReturn(currency);
        when(pluginState.get(clickContext)).thenReturn(plugin);
        when(plugin.getCurrencyRepository()).thenReturn(repository);
        when(plugin.getCurrencies()).thenReturn(cache);
        when(plugin.getExecutor()).thenReturn(Runnable::run);

        CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();
        when(repository.updateAsync(currency)).thenReturn(updateFuture);

        TranslationService processing = registerTranslation("save.processing");
        TranslationService success = registerTranslation("save.success");

        invokeHandleSave(clickContext);

        verify(processing).withPrefix();
        verify(processing).with(eq("currency_identifier"), eq(currency.getIdentifier()));
        verify(processing).send();

        updateFuture.complete(Boolean.TRUE);

        verify(cache).put(any(), same(currency));
        verify(success).withPrefix();
        verify(success).with(eq("currency_identifier"), eq(currency.getIdentifier()));
        verify(success).send();
        verify(clickContext).closeForPlayer();
    }

    @Test
    void saveHandlerSurfacesFailedValidation() throws Exception {
        SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));
        JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        CurrencyRepository repository = Mockito.mock(CurrencyRepository.class);

        when(currencyState.get(clickContext)).thenReturn(currency);
        when(pluginState.get(clickContext)).thenReturn(plugin);
        when(plugin.getCurrencyRepository()).thenReturn(repository);
        when(plugin.getExecutor()).thenReturn(Runnable::run);

        CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();
        when(repository.updateAsync(currency)).thenReturn(updateFuture);

        TranslationService processing = registerTranslation("save.processing");
        TranslationService failed = registerTranslation("save.failed");

        invokeHandleSave(clickContext);

        verify(processing).send();

        updateFuture.complete(null);

        verify(failed).withPrefix();
        verify(failed).with(eq("currency_identifier"), eq(currency.getIdentifier()));
        verify(failed).send();
        verify(clickContext).closeForPlayer();
    }

    @Test
    void saveHandlerReportsRepositoryErrors() throws Exception {
        SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));
        JExEconomyImpl plugin = Mockito.mock(JExEconomyImpl.class);
        CurrencyRepository repository = Mockito.mock(CurrencyRepository.class);

        when(currencyState.get(clickContext)).thenReturn(currency);
        when(pluginState.get(clickContext)).thenReturn(plugin);
        when(plugin.getCurrencyRepository()).thenReturn(repository);
        when(plugin.getExecutor()).thenReturn(Runnable::run);

        CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();
        when(repository.updateAsync(currency)).thenReturn(updateFuture);

        TranslationService processing = registerTranslation("save.processing");
        TranslationService error = registerTranslation("save.error");

        invokeHandleSave(clickContext);

        verify(processing).send();

        RuntimeException failure = new RuntimeException("database offline");
        updateFuture.completeExceptionally(failure);

        ArgumentCaptor<Map<String, Object>> placeholders = ArgumentCaptor.forClass(Map.class);
        verify(error).withPrefix();
        verify(error).withAll(placeholders.capture());
        assertEquals(currency.getIdentifier(), placeholders.getValue().get("currency_identifier"));
        assertEquals(failure.getMessage(), placeholders.getValue().get("error"));
        verify(error).send();
        verify(clickContext).closeForPlayer();
    }

    private void injectState(String fieldName, Object value) throws Exception {
        Field field = CurrencyPropertiesEditingView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(view, value);
    }

    private TranslationService registerTranslation(String suffix) {
        return registerTranslation(BASE_KEY, suffix);
    }

    private TranslationService registerTranslation(String baseKey, String suffix) {
        TranslationKey key = TranslationKey.of(baseKey, suffix);
        TranslationService service = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
        TranslatedMessage message = new TranslatedMessage(Component.empty(), key);
        when(service.build()).thenReturn(message);
        translationServiceMock.when(() -> TranslationService.create(eq(key), any(Player.class))).thenReturn(service);
        return service;
    }

    private void registerBuilder(Material material) {
        @SuppressWarnings("unchecked")
        IUnifiedItemBuilder<?, ?> builder = Mockito.mock(IUnifiedItemBuilder.class, Answers.RETURNS_SELF);
        ItemStack item = new ItemStack(material);
        when(builder.build()).thenReturn(item);
        unifiedBuilderFactoryMock.when(() -> UnifiedBuilderFactory.item(material)).thenReturn(builder);
    }

    private void registerSaveHeadBuilders() {
        IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
        ItemStack proceedHead = new ItemStack(Material.PLAYER_HEAD);
        when(headBuilder.build()).thenReturn(proceedHead);
        unifiedBuilderFactoryMock.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

        @SuppressWarnings("unchecked")
        IUnifiedItemBuilder<?, ?> saveBuilder = Mockito.mock(IUnifiedItemBuilder.class, Answers.RETURNS_SELF);
        ItemStack saveItem = new ItemStack(Material.PLAYER_HEAD);
        when(saveBuilder.build()).thenReturn(saveItem);
        unifiedBuilderFactoryMock.when(() -> UnifiedBuilderFactory.item(proceedHead)).thenReturn(saveBuilder);
    }

    private void registerHeadTranslations() {
        registerTranslation("head.proceed", "name");
        registerTranslation("head.proceed", "lore");
    }

    private void invokeHandleSave(Context context) throws Exception {
        java.lang.reflect.Method method = CurrencyPropertiesEditingView.class.getDeclaredMethod("handleSaveChanges", Context.class, Player.class);
        method.setAccessible(true);
        method.invoke(view, context, player);
    }
}
