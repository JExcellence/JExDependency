package com.raindropcentral.rplatform.view;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AbstractAnvilViewTest {

    private ServerMock server;
    private Player player;
    private TestAnvilView view;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        view = new TestAnvilView();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onInitConfiguresAnvilInputFeature() {
        final ViewConfigBuilder config = Mockito.mock(ViewConfigBuilder.class, Mockito.RETURNS_SELF);

        view.onInit(config);

        Mockito.verify(config).type(ViewType.ANVIL);
        Mockito.verify(config).use(view.captureAnvilInput());
        Mockito.verify(config).title("");
    }

    @Test
    void onOpenAppliesAdventureTitleOnPaperServers() {
        final OpenContext open = Mockito.mock(OpenContext.class);
        final ViewConfigBuilder config = Mockito.mock(ViewConfigBuilder.class, Mockito.RETURNS_SELF);
        Mockito.when(open.modifyConfig()).thenReturn(config);
        Mockito.when(open.getPlayer()).thenReturn(player);

        final TranslationKey titleKey = TranslationKey.of(view.getKey(), "title");
        final TranslationService translation = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        final Component titleComponent = Component.text("Anvil Title");
        Mockito.when(translation.build()).thenReturn(new TranslatedMessage(titleComponent, titleKey));

        view.setTitlePlaceholders(Map.of("player", player.getName()));

        final ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
        Mockito.when(environment.isPaper()).thenReturn(true);

        try (MockedStatic<ServerEnvironment> env = Mockito.mockStatic(ServerEnvironment.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            env.when(ServerEnvironment::getInstance).thenReturn(environment);
            translations.when(() -> TranslationService.create(titleKey, player)).thenReturn(translation);

            view.onOpen(open);

            translations.verify(() -> TranslationService.create(titleKey, player));
            Mockito.verify(translation).withAll(view.getTitlePlaceholders(open));
            Mockito.verify(config).title(titleComponent);
        }
    }

    @Test
    void onOpenFallsBackToLegacyTitleWhenNotPaper() {
        final OpenContext open = Mockito.mock(OpenContext.class);
        final ViewConfigBuilder config = Mockito.mock(ViewConfigBuilder.class, Mockito.RETURNS_SELF);
        Mockito.when(open.modifyConfig()).thenReturn(config);
        Mockito.when(open.getPlayer()).thenReturn(player);

        final TranslationKey titleKey = TranslationKey.of(view.getKey(), "title");
        final TranslationService translation = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        final Component titleComponent = Component.text("Legacy Title");
        Mockito.when(translation.build()).thenReturn(new TranslatedMessage(titleComponent, titleKey));

        final ServerEnvironment environment = Mockito.mock(ServerEnvironment.class);
        Mockito.when(environment.isPaper()).thenReturn(false);

        try (MockedStatic<ServerEnvironment> env = Mockito.mockStatic(ServerEnvironment.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            env.when(ServerEnvironment::getInstance).thenReturn(environment);
            translations.when(() -> TranslationService.create(titleKey, player)).thenReturn(translation);

            view.onOpen(open);

            final String expected = LegacyComponentSerializer.legacySection().serialize(titleComponent);
            Mockito.verify(config).title(expected);
        }
    }

    @Test
    void onFirstRenderConfiguresSlotsAndProcessesSuccessfulInput() {
        final RenderContext render = Mockito.mock(RenderContext.class);
        Mockito.when(render.getPlayer()).thenReturn(player);

        final IUnifiedItemBuilder<?, ?> itemBuilder = Mockito.mock(IUnifiedItemBuilder.class, Mockito.RETURNS_SELF);
        final ItemStack builtItem = new ItemStack(Material.NAME_TAG);
        Mockito.when(itemBuilder.build()).thenReturn(builtItem);

        final TranslationKey nameKey = TranslationKey.of(view.getKey(), "input.name");
        final TranslationKey loreKey = TranslationKey.of(view.getKey(), "input.lore");
        final TranslationService nameService = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        final TranslationService loreService = Mockito.mock(TranslationService.class, Mockito.RETURNS_SELF);
        Mockito.when(nameService.build()).thenReturn(new TranslatedMessage(Component.text("Enter Name"), nameKey));
        Mockito.when(loreService.build()).thenReturn(new TranslatedMessage(Component.text("Line"), loreKey));

        final BukkitItemComponentBuilder firstSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
        final BukkitItemComponentBuilder resultSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
        Mockito.when(render.firstSlot(Mockito.any(ItemStack.class))).thenReturn(firstSlotBuilder);
        Mockito.when(render.resultSlot()).thenReturn(resultSlotBuilder);

        final AtomicReference<java.util.function.Consumer<SlotClickContext>> clickHandler = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            clickHandler.set(invocation.getArgument(0));
            return resultSlotBuilder;
        }).when(resultSlotBuilder).onClick(Mockito.any());

        final AnvilInput anvilInput = Mockito.mock(AnvilInput.class);
        view.setAnvilInput(anvilInput);
        view.setProcessResult(new ItemStack(Material.DIAMOND));

        try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            builders.when(() -> UnifiedBuilderFactory.item(Material.NAME_TAG)).thenReturn(itemBuilder);
            translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
            translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

            view.onFirstRender(render);
        }

        assertTrue(view.wasFirstSlotConfigured());
        assertTrue(view.wasMiddleSlotConfigured());
        Mockito.verify(render).firstSlot(builtItem);

        final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(anvilInput.get(clickContext)).thenReturn("Diamond Sword");

        final java.util.function.Consumer<SlotClickContext> handler = clickHandler.get();
        assertNotNull(handler, "Result slot click handler should be registered");
        handler.accept(clickContext);

        assertEquals("Diamond Sword", view.getLastValidatedInput());
        assertSame(render, view.getValidationContext());
        assertSame(clickContext, view.getProcessContext());
        assertFalse(view.wasValidationFailed(), "Validation should pass for non-empty input");
        assertFalse(view.wasProcessingFailed(), "Processing should succeed when no exception is thrown");

        final ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(clickContext).back(dataCaptor.capture());
        final Map<String, Object> data = dataCaptor.getValue();
        assertEquals(view.getProcessResult(), data.get("item"));
    }

    @Test
    void onFirstRenderInvokesValidationFailureForInvalidInput() {
        final RenderContext render = Mockito.mock(RenderContext.class);
        Mockito.when(render.getPlayer()).thenReturn(player);

        final BukkitItemComponentBuilder firstSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
        final BukkitItemComponentBuilder resultSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
        Mockito.when(render.firstSlot(Mockito.any(ItemStack.class))).thenReturn(firstSlotBuilder);
        Mockito.when(render.resultSlot()).thenReturn(resultSlotBuilder);

        final AtomicReference<java.util.function.Consumer<SlotClickContext>> clickHandler = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            clickHandler.set(invocation.getArgument(0));
            return resultSlotBuilder;
        }).when(resultSlotBuilder).onClick(Mockito.any());

        final AnvilInput anvilInput = Mockito.mock(AnvilInput.class);
        view.setAnvilInput(anvilInput);
        view.setValid(false);

        view.onFirstRender(render);

        final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(anvilInput.get(clickContext)).thenReturn(" ");

        final java.util.function.Consumer<SlotClickContext> handler = clickHandler.get();
        assertNotNull(handler, "Result slot click handler should be registered");
        handler.accept(clickContext);

        assertTrue(view.wasValidationFailed(), "Validation failure hook should be triggered for blank input");
        assertNull(view.getProcessContext(), "Processing should not execute when validation fails");
        Mockito.verify(clickContext, Mockito.never()).back(Mockito.any());
    }

    @Test
    void onFirstRenderInvokesProcessingFailureWhenExceptionThrown() {
        final RenderContext render = Mockito.mock(RenderContext.class);
        Mockito.when(render.getPlayer()).thenReturn(player);

        final BukkitItemComponentBuilder firstSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
        final BukkitItemComponentBuilder resultSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Mockito.RETURNS_SELF);
        Mockito.when(render.firstSlot(Mockito.any(ItemStack.class))).thenReturn(firstSlotBuilder);
        Mockito.when(render.resultSlot()).thenReturn(resultSlotBuilder);

        final AtomicReference<java.util.function.Consumer<SlotClickContext>> clickHandler = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            clickHandler.set(invocation.getArgument(0));
            return resultSlotBuilder;
        }).when(resultSlotBuilder).onClick(Mockito.any());

        final AnvilInput anvilInput = Mockito.mock(AnvilInput.class);
        view.setAnvilInput(anvilInput);
        view.throwOnProcess(new IllegalStateException("failure"));

        view.onFirstRender(render);

        final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(anvilInput.get(clickContext)).thenReturn("Broken Input");

        final java.util.function.Consumer<SlotClickContext> handler = clickHandler.get();
        assertNotNull(handler, "Result slot click handler should be registered");
        handler.accept(clickContext);

        assertTrue(view.wasProcessingFailed(), "Processing failure hook should trigger when exception occurs");
        assertNotNull(view.getLastProcessingException());
        Mockito.verify(clickContext, Mockito.never()).back(Mockito.any());
    }

    private static final class TestAnvilView extends AbstractAnvilView {

        private static final VarHandle ANVIL_INPUT_HANDLE;

        static {
            try {
                ANVIL_INPUT_HANDLE = MethodHandles.privateLookupIn(AbstractAnvilView.class, MethodHandles.lookup())
                        .findVarHandle(AbstractAnvilView.class, "anvilInput", AnvilInput.class);
            } catch (final ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        private Map<String, Object> titlePlaceholders = Map.of();
        private boolean firstSlotConfigured;
        private boolean middleSlotConfigured;
        private boolean validationFailed;
        private boolean processingFailed;
        private boolean valid = true;
        private Object processResult = new Object();
        private RuntimeException processingException;
        private String lastValidatedInput;
        private Context validationContext;
        private Context processContext;
        private Exception lastProcessingException;

        private TestAnvilView() {
            super((Class<? extends View>) null);
        }

        @Override
        protected String getKey() {
            return "test.anvil";
        }

        @Override
        protected Map<String, Object> getTitlePlaceholders(@NotNull final OpenContext context) {
            return this.titlePlaceholders;
        }

        @Override
        protected void setupFirstSlot(@NotNull final RenderContext render, @NotNull final Player player) {
            this.firstSlotConfigured = true;
            super.setupFirstSlot(render, player);
        }

        @Override
        protected void setupMiddleSlot(@NotNull final RenderContext render, @NotNull final Player player) {
            this.middleSlotConfigured = true;
        }

        @Override
        protected boolean isValidInput(@NotNull final String input, @NotNull final Context context) {
            this.lastValidatedInput = input;
            this.validationContext = context;
            return this.valid && super.isValidInput(input, context);
        }

        @Override
        protected Object processInput(@NotNull final String input, @NotNull final Context context) {
            this.processContext = context;
            if (this.processingException != null) {
                throw this.processingException;
            }
            return this.processResult;
        }

        @Override
        protected void onValidationFailed(final String input, final Context context) {
            this.validationFailed = true;
        }

        @Override
        protected void onProcessingFailed(final String input, final Context context, final @NotNull Exception exception) {
            this.processingFailed = true;
            this.lastProcessingException = exception;
        }

        void setTitlePlaceholders(final Map<String, Object> placeholders) {
            this.titlePlaceholders = placeholders;
        }

        Map<String, Object> getTitlePlaceholders(final OpenContext context) {
            return this.titlePlaceholders;
        }

        void setValid(final boolean valid) {
            this.valid = valid;
        }

        void setProcessResult(final Object result) {
            this.processResult = result;
            this.processingException = null;
        }

        Object getProcessResult() {
            return this.processResult;
        }

        void throwOnProcess(final RuntimeException exception) {
            this.processingException = exception;
        }

        void setAnvilInput(final AnvilInput anvilInput) {
            ANVIL_INPUT_HANDLE.set(this, anvilInput);
        }

        AnvilInput captureAnvilInput() {
            return (AnvilInput) ANVIL_INPUT_HANDLE.get(this);
        }

        boolean wasFirstSlotConfigured() {
            return this.firstSlotConfigured;
        }

        boolean wasMiddleSlotConfigured() {
            return this.middleSlotConfigured;
        }

        boolean wasValidationFailed() {
            return this.validationFailed;
        }

        boolean wasProcessingFailed() {
            return this.processingFailed;
        }

        String getLastValidatedInput() {
            return this.lastValidatedInput;
        }

        Context getValidationContext() {
            return this.validationContext;
        }

        Context getProcessContext() {
            return this.processContext;
        }

        Exception getLastProcessingException() {
            return this.lastProcessingException;
        }
    }
}
