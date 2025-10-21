package com.raindropcentral.rplatform.view.anvil;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import me.devnatan.inventoryframework.BukkitViewContainer;
import me.devnatan.inventoryframework.PlatformView;
import me.devnatan.inventoryframework.ViewConfig;
import me.devnatan.inventoryframework.ViewFrame;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.pipeline.Pipeline;
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor;
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateWatcher;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static me.devnatan.inventoryframework.IFViewFrame.FRAME_REGISTERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomAnvilInputFeatureTest {

    private CustomAnvilInputFeature feature;
    private ViewFrame frame;
    private Pipeline framePipeline;
    private PlatformView platformView;
    private Pipeline viewPipeline;

    @BeforeEach
    void setUp() throws Exception {
        feature = newFeatureInstance();
        frame = Mockito.mock(ViewFrame.class);
        framePipeline = Mockito.mock(Pipeline.class);
        platformView = Mockito.mock(PlatformView.class);
        viewPipeline = Mockito.mock(Pipeline.class);

        Mockito.when(frame.getPipeline()).thenReturn(framePipeline);
        Mockito.when(platformView.getPipeline()).thenReturn(viewPipeline);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void installRegistersConfiguredPipelineInterceptor() throws Exception {
        final CustomAnvilInputConfig expectedConfig = new CustomAnvilInputConfig()
                .initialInput("Configured Input")
                .closeOnSelect()
                .onInputChange(String::trim);

        feature.install(frame, ignored -> expectedConfig);

        final ArgumentCaptor<PipelineInterceptor> interceptorCaptor = ArgumentCaptor.forClass(PipelineInterceptor.class);
        Mockito.verify(framePipeline).intercept(Mockito.eq(FRAME_REGISTERED), interceptorCaptor.capture());
        assertNotNull(interceptorCaptor.getValue(), "Frame interceptor should be registered on install");

        final Field configField = CustomAnvilInputFeature.class.getDeclaredField("config");
        configField.setAccessible(true);
        final CustomAnvilInputConfig configured = (CustomAnvilInputConfig) configField.get(feature);

        assertSame(expectedConfig, configured, "Feature should retain configured settings");
        assertEquals("Configured Input", configured.initialInput);
        assertTrue(configured.closeOnSelect);
        assertSame(expectedConfig.inputChangeHandler, configured.inputChangeHandler);
    }

    @Test
    void frameInterceptorAttachesLifecycleHandlersToRegisteredViews() {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig().initialInput("Global");
        final PipelineInterceptor<?> frameInterceptor = installFeature(config);

        frameInterceptor.intercept(framePipeline, frame);

        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.OPEN), Mockito.any());
        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.CLOSE), Mockito.any());
        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.CLICK), Mockito.any());
    }

    @Test
    void openInterceptorInitializesAnvilContainerAndPropagatesStateChanges() {
        final ServerMock server = MockBukkit.mock();
        final Player player = server.addPlayer();
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig().initialInput("Global Prompt");
        final PipelineInterceptor<?> frameInterceptor = installFeature(config);

        frameInterceptor.intercept(framePipeline, frame);

        final ArgumentCaptor<PipelineInterceptor> openCaptor = ArgumentCaptor.forClass(PipelineInterceptor.class);
        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.OPEN), openCaptor.capture());
        final PipelineInterceptor<?> openInterceptor = openCaptor.getValue();

        final CustomAnvilInput anvilInput = Mockito.mock(CustomAnvilInput.class);
        Mockito.when(anvilInput.internalId()).thenReturn(42L);
        Mockito.when(anvilInput.get(Mockito.any())).thenReturn("");

        final ViewConfig viewConfig = createViewConfig(ViewType.ANVIL, anvilInput);
        final OpenContext openContext = Mockito.mock(OpenContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(openContext.getConfig()).thenReturn(viewConfig);
        Mockito.when(openContext.getPlayer()).thenReturn(player);
        Mockito.when(openContext.isShared()).thenReturn(false);
        Mockito.when(openContext.getInternalStateValue(anvilInput)).thenReturn(Mockito.mock(StateValue.class));

        final AtomicReference<StateWatcher> watcherReference = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            watcherReference.set(invocation.getArgument(1));
            return null;
        }).when(openContext).watchState(Mockito.eq(42L), Mockito.any());

        final Inventory inventory = server.createInventory(null, InventoryType.ANVIL);
        final ItemStack ingredient = new ItemStack(Material.PAPER);
        final ItemMeta ingredientMeta = ingredient.getItemMeta();
        ingredientMeta.setDisplayName("Original");
        ingredient.setItemMeta(ingredientMeta);
        inventory.setItem(0, ingredient);

        final Object title = viewConfig.getTitle();
        try (MockedStatic<CustomAnvilInputNMS> nms = Mockito.mockStatic(CustomAnvilInputNMS.class)) {
            nms.when(() -> CustomAnvilInputNMS.open(player, title, "Global Prompt")).thenReturn(inventory);

            openInterceptor.intercept(viewPipeline, openContext);

            nms.verify(() -> CustomAnvilInputNMS.open(player, title, "Global Prompt"));
        }

        final ArgumentCaptor<BukkitViewContainer> containerCaptor = ArgumentCaptor.forClass(BukkitViewContainer.class);
        Mockito.verify(openContext).setContainer(containerCaptor.capture());
        final BukkitViewContainer container = containerCaptor.getValue();
        assertNotNull(container, "Open handler must install a Bukkit view container");
        assertSame(inventory, container.getInventory());

        final StateWatcher watcher = watcherReference.get();
        assertNotNull(watcher, "Open handler should watch anvil state changes");

        final RenderContext renderContext = Mockito.mock(RenderContext.class);
        Mockito.when(renderContext.getContainer()).thenReturn(container);
        watcher.stateValueSet(renderContext, Mockito.mock(StateValue.class), "Original", "Updated");

        final ItemStack updated = container.getInventory().getItem(0);
        assertNotNull(updated, "Ingredient item must remain present");
        assertEquals("Updated", updated.getItemMeta().getDisplayName(), "Watcher should propagate new text to ingredient");
    }

    @Test
    void clickInterceptorUpdatesStateAndClosesWhenResultSelected() {
        final ServerMock server = MockBukkit.mock();
        final Player player = server.addPlayer();
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig().initialInput("Global").closeOnSelect();
        final PipelineInterceptor<?> frameInterceptor = installFeature(config);

        frameInterceptor.intercept(framePipeline, frame);

        final ArgumentCaptor<PipelineInterceptor> clickCaptor = ArgumentCaptor.forClass(PipelineInterceptor.class);
        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.CLICK), clickCaptor.capture());
        final PipelineInterceptor<?> clickInterceptor = clickCaptor.getValue();

        final CustomAnvilInput anvilInput = Mockito.mock(CustomAnvilInput.class);
        final ViewConfig viewConfig = createViewConfig(ViewType.ANVIL, anvilInput);

        final RenderContext parentContext = Mockito.mock(RenderContext.class, Mockito.RETURNS_DEEP_STUBS);
        final Inventory inventory = server.createInventory(null, InventoryType.ANVIL);
        final ItemStack ingredient = new ItemStack(Material.PAPER);
        final ItemMeta ingredientMeta = ingredient.getItemMeta();
        ingredientMeta.setDisplayName("Seed");
        ingredient.setItemMeta(ingredientMeta);
        inventory.setItem(0, ingredient);
        final BukkitViewContainer container = new BukkitViewContainer(inventory, false, ViewType.ANVIL, true);

        Mockito.when(parentContext.getContainer()).thenReturn(container);
        Mockito.when(parentContext.getConfig()).thenReturn(viewConfig);

        final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(clickContext.getParent()).thenReturn(parentContext);
        Mockito.when(clickContext.getConfig()).thenReturn(viewConfig);
        Mockito.when(clickContext.getPlayer()).thenReturn(player);

        final InventoryClickEvent event = Mockito.mock(InventoryClickEvent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(event.getClickedInventory()).thenReturn(inventory);
        Mockito.when(event.getCurrentItem()).thenAnswer(invocation -> clickContext.getItem());
        Mockito.when(event.getRawSlot()).thenReturn(ViewType.ANVIL.getResultSlots()[0]);
        Mockito.when(event.getClick()).thenReturn(ClickType.LEFT);
        Mockito.when(event.getWhoClicked()).thenReturn(player);
        Mockito.when(clickContext.getClickOrigin()).thenReturn(event);

        final ItemStack result = new ItemStack(Material.NAME_TAG);
        final ItemMeta resultMeta = result.getItemMeta();
        resultMeta.setDisplayName("Player Text");
        result.setItemMeta(resultMeta);
        Mockito.when(clickContext.getItem()).thenReturn(result);

        clickInterceptor.intercept(viewPipeline, clickContext);

        Mockito.verify(parentContext).updateState(anvilInput, "Player Text");
        Mockito.verify(parentContext).closeForPlayer();

        final ItemStack updatedIngredient = inventory.getItem(0);
        assertNotNull(updatedIngredient);
        assertEquals("Player Text", updatedIngredient.getItemMeta().getDisplayName());
    }

    @Test
    void closeInterceptorPersistsResultItemDisplayName() {
        final ServerMock server = MockBukkit.mock();
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig().initialInput("Global");
        final PipelineInterceptor<?> frameInterceptor = installFeature(config);

        frameInterceptor.intercept(framePipeline, frame);

        final ArgumentCaptor<PipelineInterceptor> closeCaptor = ArgumentCaptor.forClass(PipelineInterceptor.class);
        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.CLOSE), closeCaptor.capture());
        final PipelineInterceptor<?> closeInterceptor = closeCaptor.getValue();

        final CustomAnvilInput anvilInput = Mockito.mock(CustomAnvilInput.class);
        final ViewConfig viewConfig = createViewConfig(ViewType.ANVIL, anvilInput);

        final RenderContext parentContext = Mockito.mock(RenderContext.class);
        final Inventory inventory = server.createInventory(null, InventoryType.ANVIL);
        final int resultSlot = ViewType.ANVIL.getResultSlots()[0];
        final ItemStack result = new ItemStack(Material.PAPER);
        final ItemMeta meta = result.getItemMeta();
        meta.setDisplayName("Final Text");
        result.setItemMeta(meta);
        inventory.setItem(resultSlot, result);
        final BukkitViewContainer container = new BukkitViewContainer(inventory, false, ViewType.ANVIL, true);

        Mockito.when(parentContext.getContainer()).thenReturn(container);
        Mockito.when(parentContext.getConfig()).thenReturn(viewConfig);

        final CloseContext closeContext = Mockito.mock(CloseContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(closeContext.getParent()).thenReturn(parentContext);
        Mockito.when(closeContext.getConfig()).thenReturn(viewConfig);

        closeInterceptor.intercept(viewPipeline, closeContext);

        Mockito.verify(parentContext).updateState(anvilInput, "Final Text");
    }

    @Test
    void openInterceptorNoOpsWhenViewTypeIsNotAnvil() {
        final CustomAnvilInputConfig config = new CustomAnvilInputConfig();
        final PipelineInterceptor<?> frameInterceptor = installFeature(config);

        frameInterceptor.intercept(framePipeline, frame);

        final ArgumentCaptor<PipelineInterceptor> openCaptor = ArgumentCaptor.forClass(PipelineInterceptor.class);
        Mockito.verify(viewPipeline).intercept(Mockito.eq(StandardPipelinePhases.OPEN), openCaptor.capture());
        final PipelineInterceptor<?> openInterceptor = openCaptor.getValue();

        final CustomAnvilInput anvilInput = Mockito.mock(CustomAnvilInput.class);
        final ViewConfig viewConfig = createViewConfig(ViewType.CHEST, anvilInput);

        final OpenContext openContext = Mockito.mock(OpenContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(openContext.getConfig()).thenReturn(viewConfig);

        openInterceptor.intercept(viewPipeline, openContext);

        Mockito.verify(openContext, Mockito.never()).setContainer(Mockito.any());
        Mockito.verify(openContext, Mockito.never()).watchState(Mockito.anyLong(), Mockito.any());
    }

    private PipelineInterceptor<?> installFeature(CustomAnvilInputConfig config) {
        final Map<UUID, PlatformView> views = Map.of(UUID.randomUUID(), platformView);
        Mockito.when(frame.getRegisteredViews()).thenReturn(views);

        feature.install(frame, ignored -> config);

        final ArgumentCaptor<PipelineInterceptor> frameInterceptorCaptor = ArgumentCaptor.forClass(PipelineInterceptor.class);
        Mockito.verify(framePipeline).intercept(Mockito.eq(FRAME_REGISTERED), frameInterceptorCaptor.capture());
        return frameInterceptorCaptor.getValue();
    }

    private static CustomAnvilInputFeature newFeatureInstance() throws Exception {
        final Constructor<CustomAnvilInputFeature> constructor = CustomAnvilInputFeature.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static ViewConfig createViewConfig(ViewType type, CustomAnvilInput anvilInput) {
        return new ViewConfig(
                "Title",
                0,
                type,
                Map.of(),
                new String[0],
                Set.of(anvilInput),
                0L,
                0L,
                false);
    }
}
