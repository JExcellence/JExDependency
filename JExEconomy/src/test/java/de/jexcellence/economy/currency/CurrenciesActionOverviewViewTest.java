package de.jexcellence.economy.currency;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class CurrenciesActionOverviewViewTest {

    private ServerMock server;
    private PlayerMock player;
    private CurrenciesActionOverviewView view;
    private State<JExEconomyImpl> pluginState;
    private JExEconomyImpl plugin;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
        this.view = Mockito.spy(new CurrenciesActionOverviewView());
        this.pluginState = Mockito.mock(State.class);
        this.plugin = Mockito.mock(JExEconomyImpl.class);
        Mockito.when(this.pluginState.get(any())).thenReturn(this.plugin);
        this.injectPluginState();
        this.stubTranslations();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onFirstRenderConfiguresActionButtonsWithExpectedItems() {
        final RenderContext renderContext = Mockito.mock(RenderContext.class);
        Mockito.when(renderContext.getPlayer()).thenReturn(this.player);

        final BukkitItemComponentBuilder createBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder editBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder viewBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder deleteBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);

        Mockito.when(renderContext.layoutSlot(eq('c'), any(ItemStack.class))).thenReturn(createBuilder);
        Mockito.when(renderContext.layoutSlot(eq('e'), any(ItemStack.class))).thenReturn(editBuilder);
        Mockito.when(renderContext.layoutSlot(eq('v'), any(ItemStack.class))).thenReturn(viewBuilder);
        Mockito.when(renderContext.layoutSlot(eq('d'), any(ItemStack.class))).thenReturn(deleteBuilder);

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders()) {
            this.view.onFirstRender(renderContext, this.player);
        }

        final ArgumentCaptor<ItemStack> createItem = ArgumentCaptor.forClass(ItemStack.class);
        final ArgumentCaptor<ItemStack> editItem = ArgumentCaptor.forClass(ItemStack.class);
        final ArgumentCaptor<ItemStack> viewItem = ArgumentCaptor.forClass(ItemStack.class);
        final ArgumentCaptor<ItemStack> deleteItem = ArgumentCaptor.forClass(ItemStack.class);

        Mockito.verify(renderContext).layoutSlot(eq('c'), createItem.capture());
        Mockito.verify(renderContext).layoutSlot(eq('e'), editItem.capture());
        Mockito.verify(renderContext).layoutSlot(eq('v'), viewItem.capture());
        Mockito.verify(renderContext).layoutSlot(eq('d'), deleteItem.capture());

        assertEquals(Material.PLAYER_HEAD, createItem.getValue().getType());
        assertEquals(Material.ANVIL, editItem.getValue().getType());
        assertEquals(Material.SPYGLASS, viewItem.getValue().getType());
        assertEquals(Material.BARRIER, deleteItem.getValue().getType());

        Mockito.verify(createBuilder).onClick(any());
        Mockito.verify(editBuilder).onClick(any());
        Mockito.verify(viewBuilder).onClick(any());
        Mockito.verify(deleteBuilder).displayIf(any());
        Mockito.verify(deleteBuilder).onClick(any());
    }

    @Test
    void deleteActionVisibilityRequiresAdministrativePermission() {
        final RenderContext renderContext = Mockito.mock(RenderContext.class);
        Mockito.when(renderContext.getPlayer()).thenReturn(this.player);

        Mockito.when(renderContext.layoutSlot(eq('c'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));
        Mockito.when(renderContext.layoutSlot(eq('e'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));
        Mockito.when(renderContext.layoutSlot(eq('v'), any(ItemStack.class))).thenReturn(Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF));

        final BukkitItemComponentBuilder deleteBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final AtomicReference<Predicate<RenderContext>> visibilityCapture = new AtomicReference<>();
        Mockito.when(deleteBuilder.displayIf(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked") final Predicate<RenderContext> predicate = invocation.getArgument(0);
            visibilityCapture.set(predicate);
            return deleteBuilder;
        });
        Mockito.when(renderContext.layoutSlot(eq('d'), any(ItemStack.class))).thenReturn(deleteBuilder);

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders()) {
            this.view.onFirstRender(renderContext, this.player);
        }

        final Predicate<RenderContext> visibility = visibilityCapture.get();
        assertNotNull(visibility, "Delete action visibility predicate should be captured");

        final RenderContext noPermissionContext = Mockito.mock(RenderContext.class);
        final Player noPermissionPlayer = Mockito.mock(Player.class);
        Mockito.when(noPermissionContext.getPlayer()).thenReturn(noPermissionPlayer);
        Mockito.when(noPermissionPlayer.hasPermission("jexeconomy.admin.delete")).thenReturn(false);
        Mockito.when(noPermissionPlayer.hasPermission("jexeconomy.admin.*")).thenReturn(false);
        Mockito.when(noPermissionPlayer.isOp()).thenReturn(false);
        assertFalse(visibility.test(noPermissionContext));

        final RenderContext deletePermissionContext = Mockito.mock(RenderContext.class);
        final Player deletePermissionPlayer = Mockito.mock(Player.class);
        Mockito.when(deletePermissionContext.getPlayer()).thenReturn(deletePermissionPlayer);
        Mockito.when(deletePermissionPlayer.hasPermission("jexeconomy.admin.delete")).thenReturn(true);
        Mockito.when(deletePermissionPlayer.hasPermission("jexeconomy.admin.*")).thenReturn(false);
        Mockito.when(deletePermissionPlayer.isOp()).thenReturn(false);
        assertTrue(visibility.test(deletePermissionContext));

        final RenderContext wildcardPermissionContext = Mockito.mock(RenderContext.class);
        final Player wildcardPermissionPlayer = Mockito.mock(Player.class);
        Mockito.when(wildcardPermissionContext.getPlayer()).thenReturn(wildcardPermissionPlayer);
        Mockito.when(wildcardPermissionPlayer.hasPermission("jexeconomy.admin.delete")).thenReturn(false);
        Mockito.when(wildcardPermissionPlayer.hasPermission("jexeconomy.admin.*")).thenReturn(true);
        Mockito.when(wildcardPermissionPlayer.isOp()).thenReturn(false);
        assertTrue(visibility.test(wildcardPermissionContext));

        final RenderContext operatorContext = Mockito.mock(RenderContext.class);
        final Player operatorPlayer = Mockito.mock(Player.class);
        Mockito.when(operatorContext.getPlayer()).thenReturn(operatorPlayer);
        Mockito.when(operatorPlayer.hasPermission("jexeconomy.admin.delete")).thenReturn(false);
        Mockito.when(operatorPlayer.hasPermission("jexeconomy.admin.*")).thenReturn(false);
        Mockito.when(operatorPlayer.isOp()).thenReturn(true);
        assertTrue(visibility.test(operatorContext));
    }

    @Test
    void clickingActionButtonsNavigatesToExpectedViews() {
        final RenderContext renderContext = Mockito.mock(RenderContext.class);
        Mockito.when(renderContext.getPlayer()).thenReturn(this.player);

        final BukkitItemComponentBuilder createBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder editBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder viewBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        final BukkitItemComponentBuilder deleteBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);

        Mockito.when(renderContext.layoutSlot(eq('c'), any(ItemStack.class))).thenReturn(createBuilder);
        Mockito.when(renderContext.layoutSlot(eq('e'), any(ItemStack.class))).thenReturn(editBuilder);
        Mockito.when(renderContext.layoutSlot(eq('v'), any(ItemStack.class))).thenReturn(viewBuilder);
        Mockito.when(renderContext.layoutSlot(eq('d'), any(ItemStack.class))).thenReturn(deleteBuilder);

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders()) {
            this.view.onFirstRender(renderContext, this.player);
        }

        final ArgumentCaptor<Consumer<SlotClickContext>> createClick = ArgumentCaptor.forClass(Consumer.class);
        final ArgumentCaptor<Consumer<SlotClickContext>> editClick = ArgumentCaptor.forClass(Consumer.class);
        final ArgumentCaptor<Consumer<SlotClickContext>> viewClick = ArgumentCaptor.forClass(Consumer.class);
        final ArgumentCaptor<Consumer<SlotClickContext>> deleteClick = ArgumentCaptor.forClass(Consumer.class);

        Mockito.verify(createBuilder).onClick(createClick.capture());
        Mockito.verify(editBuilder).onClick(editClick.capture());
        Mockito.verify(viewBuilder).onClick(viewClick.capture());
        Mockito.verify(deleteBuilder).onClick(deleteClick.capture());

        final SlotClickContext createClickContext = Mockito.mock(SlotClickContext.class);
        createClick.getValue().accept(createClickContext);
        final ArgumentCaptor<Map<String, Object>> createNavigation = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(createClickContext).openForPlayer(eq(CurrenciesCreatingView.class), createNavigation.capture());
        assertSame(this.plugin, createNavigation.getValue().get("plugin"));

        final SlotClickContext editClickContext = Mockito.mock(SlotClickContext.class);
        editClick.getValue().accept(editClickContext);
        final ArgumentCaptor<Map<String, Object>> editNavigation = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(editClickContext).openForPlayer(eq(CurrencyEditingView.class), editNavigation.capture());
        assertSame(this.plugin, editNavigation.getValue().get("plugin"));

        final SlotClickContext viewClickContext = Mockito.mock(SlotClickContext.class);
        viewClick.getValue().accept(viewClickContext);
        final ArgumentCaptor<Map<String, Object>> viewNavigation = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(viewClickContext).openForPlayer(eq(CurrenciesOverviewView.class), viewNavigation.capture());
        assertSame(this.plugin, viewNavigation.getValue().get("plugin"));

        final SlotClickContext deleteClickContext = Mockito.mock(SlotClickContext.class);
        deleteClick.getValue().accept(deleteClickContext);
        final ArgumentCaptor<Map<String, Object>> deleteNavigation = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(deleteClickContext).openForPlayer(eq(CurrencyDeletionView.class), deleteNavigation.capture());
        assertSame(this.plugin, deleteNavigation.getValue().get("plugin"));
    }

    private void injectPluginState() throws ReflectiveOperationException {
        final Field stateField = CurrenciesActionOverviewView.class.getDeclaredField("jexEconomy");
        stateField.setAccessible(true);
        stateField.set(this.view, this.pluginState);
    }

    private void stubTranslations() {
        Mockito.doAnswer(invocation -> this.createTranslation(invocation.getArgument(0))).when(this.view)
            .i18n(Mockito.anyString(), Mockito.any(Player.class));
    }

    private TranslationService createTranslation(final String keySuffix) {
        final TranslationService translation = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
        final TranslatedMessage message = Mockito.mock(TranslatedMessage.class);
        Mockito.when(message.component()).thenReturn(Component.text(keySuffix));
        Mockito.when(message.splitLines()).thenReturn(List.of(Component.text(keySuffix)));
        Mockito.when(translation.build()).thenReturn(message);
        return translation;
    }

    private MockedStatic<UnifiedBuilderFactory> mockItemBuilders() {
        final MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
        builders.when(() -> UnifiedBuilderFactory.item(any(Material.class)))
            .thenAnswer(invocation -> new CapturingItemBuilder(new ItemStack(invocation.getArgument(0))));
        builders.when(() -> UnifiedBuilderFactory.item(any(ItemStack.class)))
            .thenAnswer(invocation -> new CapturingItemBuilder(invocation.getArgument(0)));
        return builders;
    }

    private static final class CapturingItemBuilder implements
        IUnifiedItemBuilder<ItemMeta, CapturingItemBuilder> {

        private final ItemStack itemStack;

        private CapturingItemBuilder(final ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public CapturingItemBuilder setName(final Component name) {
            return this;
        }

        @Override
        public CapturingItemBuilder setLore(final List<Component> lore) {
            return this;
        }

        @Override
        public CapturingItemBuilder addLoreLine(final Component line) {
            return this;
        }

        @Override
        public CapturingItemBuilder addLoreLines(final List<Component> lore) {
            return this;
        }

        @Override
        public CapturingItemBuilder addLoreLines(final Component... lore) {
            return this;
        }

        @Override
        public CapturingItemBuilder setAmount(final int amount) {
            this.itemStack.setAmount(amount);
            return this;
        }

        @Override
        public CapturingItemBuilder setCustomModelData(final int data) {
            return this;
        }

        @Override
        public CapturingItemBuilder addEnchantment(final Enchantment enchantment, final int level) {
            return this;
        }

        @Override
        public CapturingItemBuilder addItemFlags(final ItemFlag... flags) {
            return this;
        }

        @Override
        public CapturingItemBuilder setGlowing(final boolean glowing) {
            return this;
        }

        @Override
        public ItemStack build() {
            return this.itemStack;
        }
    }
}
