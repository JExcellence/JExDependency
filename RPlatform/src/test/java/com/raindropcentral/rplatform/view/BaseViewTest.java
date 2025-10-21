package com.raindropcentral.rplatform.view;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.version.ServerEnvironment;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

class BaseViewTest {

        private ServerMock server;
        private PlayerMock player;

        @BeforeEach
        void setUp() {
                server = MockBukkit.mock();
                player = server.addPlayer("TestPlayer");
                resetServerEnvironment();
        }

        @AfterEach
        void tearDown() {
                MockBukkit.unmock();
                resetServerEnvironment();
        }

        @Test
        void onInitUsesLayoutConfigurationWhenLayoutIsMeaningful() {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        ", "         "}, 3, 20);
                final ViewConfigBuilder config = Mockito.mock(ViewConfigBuilder.class, Answers.RETURNS_SELF);

                view.onInit(config);

                Mockito.verify(config).layout(view.getLayout());
                Mockito.verify(config, never()).size(Mockito.anyInt());
                Mockito.verify(config).scheduleUpdate(20);
                Mockito.verify(config).build();
        }

        @Test
        void onInitFallsBackToSizeWhenLayoutIsBlank() {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"         "}, 4, 0);
                final ViewConfigBuilder config = Mockito.mock(ViewConfigBuilder.class, Answers.RETURNS_SELF);

                view.onInit(config);

                Mockito.verify(config, never()).layout(Mockito.any(String[].class));
                Mockito.verify(config).size(4);
                Mockito.verify(config, never()).scheduleUpdate(Mockito.anyInt());
                Mockito.verify(config).build();
        }

        @Test
        void onOpenTranslatesTitleAndAppliesPlaceholders() {
                final Map<String, Object> placeholders = Map.of("player", player.getName());
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        ", "         "}, 6, 0, placeholders);
                view.setTitleKey("custom.title");
                final OpenContext open = Mockito.mock(OpenContext.class);
                final ViewConfigBuilder configBuilder = Mockito.mock(ViewConfigBuilder.class, Answers.RETURNS_SELF);

                Mockito.when(open.getPlayer()).thenReturn(player);
                Mockito.when(open.modifyConfig()).thenReturn(configBuilder);

                final TranslationKey expectedKey = TranslationKey.of("test.view", "custom.title");
                final TranslationService translation = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
                final TranslatedMessage message = Mockito.mock(TranslatedMessage.class);

                Mockito.when(translation.build()).thenReturn(message);
                Mockito.when(message.asLegacyText()).thenReturn("Translated Title");

                try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
                        translations.when(() -> TranslationService.create(expectedKey, player)).thenReturn(translation);

                        view.onOpen(open);

                        translations.verify(() -> TranslationService.create(expectedKey, player));
                }

                Mockito.verify(translation).withAll(placeholders);
                Mockito.verify(message).asLegacyText();
                Mockito.verify(configBuilder).title("Translated Title");
        }

        @Test
        void onFirstRenderInvokesNavigationHookAndAutoFillWhenEnabled() {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        ", "         "}, 6, 0);
                final AtomicBoolean hookInvoked = new AtomicBoolean();
                view.setFirstRenderHook((context, currentPlayer) -> hookInvoked.set(true));

                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);
                Mockito.when(render.getPlayer()).thenReturn(player);

                final AtomicReference<Object> clickHandler = new AtomicReference<>();
                final SlotInvocationCapture capture = stubSlotInvocation(render, clickHandler);

                try (MockedStatic<UnifiedBuilderFactory> unified = Mockito.mockStatic(UnifiedBuilderFactory.class);
                     MockedStatic<TranslationService> translations = prepareReturnTranslations(player)) {
                        final IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
                        final ItemStack head = new ItemStack(Material.REDSTONE_BLOCK);
                        Mockito.when(headBuilder.build()).thenReturn(head);
                        unified.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

                        view.onFirstRender(render);
                }

                assertTrue(hookInvoked.get(), "onFirstRender hook should be invoked");
                assertTrue(view.wasFillItemCreated(), "Auto fill should create the filler item");
                assertTrue(view.wasFirstRenderExecuted(), "Abstract render hook should run");
                assertTrue(capture.slot().get() >= 9, "Navigation should target bottom row");
                assertNotNull(clickHandler.get(), "Click handler should be registered");
        }

        @Test
        void onFirstRenderSkipsAutoFillWhenDisabled() {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        ", "         "}, 6, 0);
                view.setAutoFill(false);

                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);
                Mockito.when(render.getPlayer()).thenReturn(player);

                final AtomicReference<Object> clickHandler = new AtomicReference<>();
                stubSlotInvocation(render, clickHandler);

                try (MockedStatic<UnifiedBuilderFactory> unified = Mockito.mockStatic(UnifiedBuilderFactory.class);
                     MockedStatic<TranslationService> translations = prepareReturnTranslations(player)) {
                        final IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
                        Mockito.when(headBuilder.build()).thenReturn(new ItemStack(Material.EMERALD));
                        unified.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

                        view.onFirstRender(render);
                }

                assertTrue(view.wasFirstRenderExecuted(), "First render hook should still run");
                assertThat(view.wasFillItemCreated()).isFalse();
        }

        @Test
        void renderNavigationButtonsPlacesBackButtonInBottomLeftSlot() {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        ", "         "}, 6, 0);
                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);
                final AtomicReference<Object> clickHandler = new AtomicReference<>();
                final SlotInvocationCapture capture = stubSlotInvocation(render, clickHandler);

                try (MockedStatic<UnifiedBuilderFactory> unified = Mockito.mockStatic(UnifiedBuilderFactory.class);
                     MockedStatic<TranslationService> translations = prepareReturnTranslations(player)) {
                        final IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
                        final ItemStack head = new ItemStack(Material.ARROW);
                        Mockito.when(headBuilder.build()).thenReturn(head);
                        unified.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

                        view.renderNavigationButtons(render, player);

                        assertThat(capture.slot().get()).isEqualTo(9);
                        assertThat(capture.item().get()).isSameAs(head);
                        assertNotNull(clickHandler.get());
                }
        }

        @Test
        void renderNavigationButtonsSkipsPlacementWhenSingleRow() {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        "}, 6, 0);
                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);

                view.renderNavigationButtons(render, player);

                Mockito.verifyNoInteractions(render);
        }

        @Test
        void backButtonClickClosesInventoryWhenNoParentConfigured() throws Exception {
                final InspectableBaseView view = new InspectableBaseView(null, new String[]{"x        ", "         "}, 6, 0);
                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);
                final AtomicReference<Object> clickHandler = new AtomicReference<>();
                stubSlotInvocation(render, clickHandler);

                try (MockedStatic<UnifiedBuilderFactory> unified = Mockito.mockStatic(UnifiedBuilderFactory.class);
                     MockedStatic<TranslationService> translations = prepareReturnTranslations(player)) {
                        final IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
                        Mockito.when(headBuilder.build()).thenReturn(new ItemStack(Material.STONE));
                        unified.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

                        view.renderNavigationButtons(render, player);
                }

                final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);

                assertDoesNotThrow(() -> invokeClickHandler(clickHandler.get(), clickContext));
                Mockito.verify(clickContext).closeForPlayer();
                Mockito.verify(clickContext, never()).openForPlayer(Mockito.any(), Mockito.any());
        }

        @Test
        void backButtonClickOpensParentWhenConfigured() throws Exception {
                final InspectableBaseView view = new InspectableBaseView(InspectableBaseView.class, new String[]{"x        ", "         "}, 6, 0);
                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);
                final AtomicReference<Object> clickHandler = new AtomicReference<>();
                stubSlotInvocation(render, clickHandler);

                try (MockedStatic<UnifiedBuilderFactory> unified = Mockito.mockStatic(UnifiedBuilderFactory.class);
                     MockedStatic<TranslationService> translations = prepareReturnTranslations(player)) {
                        final IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
                        Mockito.when(headBuilder.build()).thenReturn(new ItemStack(Material.STICK));
                        unified.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

                        view.renderNavigationButtons(render, player);
                }

                final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
                final Object initialData = new Object();
                Mockito.when(clickContext.getInitialData()).thenReturn(initialData);

                assertDoesNotThrow(() -> invokeClickHandler(clickHandler.get(), clickContext));
                Mockito.verify(clickContext).openForPlayer(InspectableBaseView.class, initialData);
                Mockito.verify(clickContext, never()).closeForPlayer();
        }

        @Test
        void backButtonClickFallsBackToCloseWhenParentOpenFails() throws Exception {
                final InspectableBaseView view = new InspectableBaseView(InspectableBaseView.class, new String[]{"x        ", "         "}, 6, 0);
                final RenderContext render = Mockito.mock(RenderContext.class, Answers.RETURNS_DEEP_STUBS);
                final AtomicReference<Object> clickHandler = new AtomicReference<>();
                stubSlotInvocation(render, clickHandler);

                try (MockedStatic<UnifiedBuilderFactory> unified = Mockito.mockStatic(UnifiedBuilderFactory.class);
                     MockedStatic<TranslationService> translations = prepareReturnTranslations(player)) {
                        final IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
                        Mockito.when(headBuilder.build()).thenReturn(new ItemStack(Material.LAPIS_BLOCK));
                        unified.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

                        view.renderNavigationButtons(render, player);
                }

                final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
                Mockito.doThrow(new IllegalStateException("boom")).when(clickContext).openForPlayer(Mockito.any(), Mockito.any());

                assertDoesNotThrow(() -> invokeClickHandler(clickHandler.get(), clickContext));
                Mockito.verify(clickContext).closeForPlayer();
        }

        private static MockedStatic<TranslationService> prepareReturnTranslations(final PlayerMock player) {
                final TranslationKey nameKey = TranslationKey.of("head.return", "name");
                final TranslationKey loreKey = TranslationKey.of("head.return", "lore");

                final TranslationService nameService = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
                final TranslationService loreService = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);

                final TranslatedMessage nameMessage = Mockito.mock(TranslatedMessage.class);
                final TranslatedMessage loreMessage = Mockito.mock(TranslatedMessage.class);

                Mockito.when(nameService.build()).thenReturn(nameMessage);
                Mockito.when(loreService.build()).thenReturn(loreMessage);

                Mockito.when(nameMessage.component()).thenReturn(Component.empty());
                Mockito.when(loreMessage.splitLines()).thenReturn(List.of());

                final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
                translations.when(() -> TranslationService.create(nameKey, player)).thenReturn(nameService);
                translations.when(() -> TranslationService.create(loreKey, player)).thenReturn(loreService);

                return translations;
        }

        private static SlotInvocationCapture stubSlotInvocation(
                final RenderContext render,
                final AtomicReference<Object> clickHandler
        ) {
                final AtomicInteger slotCapture = new AtomicInteger(-1);
                final AtomicReference<ItemStack> itemCapture = new AtomicReference<>();

                Mockito.when(render.slot(Mockito.anyInt(), Mockito.any())).thenAnswer(invocation -> {
                        slotCapture.set(invocation.getArgument(0));
                        itemCapture.set(invocation.getArgument(1));
                        final Class<?> returnType = invocation.getMethod().getReturnType();
                        return createClickCapturingMock(returnType, clickHandler);
                });

                return new SlotInvocationCapture(slotCapture, itemCapture);
        }

        private static Object createClickCapturingMock(
                final Class<?> returnType,
                final AtomicReference<Object> clickHandler
        ) {
                final Answer<Object> answer = invocation -> {
                        if ("onClick".equals(invocation.getMethod().getName()) && invocation.getArguments().length == 1) {
                                clickHandler.set(invocation.getArgument(0));
                                return invocation.getMock();
                        }
                        return defaultValue(invocation.getMethod().getReturnType());
                };

                if (returnType.isInterface()) {
                        return Proxy.newProxyInstance(
                                returnType.getClassLoader(),
                                new Class<?>[]{returnType},
                                (proxy, method, args) -> {
                                        if ("onClick".equals(method.getName()) && args != null && args.length == 1) {
                                                clickHandler.set(args[0]);
                                                return proxy;
                                        }
                                        return defaultValue(method.getReturnType());
                                }
                        );
                }

                return Mockito.mock(returnType, answer);
        }

        private static Object defaultValue(final Class<?> type) {
                if (!type.isPrimitive()) {
                        return null;
                }
                if (type == boolean.class) {
                        return false;
                }
                if (type == char.class) {
                        return '\0';
                }
                if (type == byte.class || type == short.class || type == int.class) {
                        return 0;
                }
                if (type == long.class) {
                        return 0L;
                }
                if (type == float.class) {
                        return 0F;
                }
                if (type == double.class) {
                        return 0D;
                }
                return null;
        }

        private static void invokeClickHandler(final Object handler, final SlotClickContext context) throws Exception {
                Objects.requireNonNull(handler, "Click handler must be registered");

                final Method method = Arrays.stream(handler.getClass().getMethods())
                        .filter(candidate -> candidate.getParameterCount() == 1 && candidate.getParameterTypes()[0].isInstance(context))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unable to locate click handler method"));

                method.invoke(handler, context);
        }

        private static void resetServerEnvironment() {
                try {
                        final Field field = ServerEnvironment.class.getDeclaredField("instance");
                        field.setAccessible(true);
                        field.set(null, null);
                } catch (final ReflectiveOperationException ignored) {
                        // no-op for tests when reflection is unavailable
                }
        }

        private record SlotInvocationCapture(AtomicInteger slot, AtomicReference<ItemStack> item) {
        }

        private static final class InspectableBaseView extends BaseView {

                private static final String DEFAULT_KEY = "test.view";

                private final String[] layout;
                private final int size;
                private final int updateSchedule;
                private final Map<String, Object> titlePlaceholders;
                private boolean autoFill = true;
                private Supplier<ItemStack> fillerSupplier = () -> new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                private BiConsumer<RenderContext, PlayerMock> firstRenderHook = (context, player) -> {};
                private boolean fillItemCreated;
                private boolean firstRenderExecuted;
                private String titleKey = "title";

                InspectableBaseView() {
                        this(null, new String[]{"         "}, 6, 0, Map.of());
                }

                InspectableBaseView(
                        final Class<? extends View> parent,
                        final String[] layout,
                        final int size,
                        final int updateSchedule
                ) {
                        this(parent, layout, size, updateSchedule, Map.of());
                }

                InspectableBaseView(
                        final Class<? extends View> parent,
                        final String[] layout,
                        final int size,
                        final int updateSchedule,
                        final Map<String, Object> titlePlaceholders
                ) {
                        super(parent);
                        this.layout = layout;
                        this.size = size;
                        this.updateSchedule = updateSchedule;
                        this.titlePlaceholders = titlePlaceholders;
                }

                InspectableBaseView(
                        final Class<? extends View> parent,
                        final String[] layout,
                        final int size,
                        final Map<String, Object> titlePlaceholders
                ) {
                        this(parent, layout, size, 0, titlePlaceholders);
                }

                @Override
                protected String getKey() {
                        return DEFAULT_KEY;
                }

                @Override
                protected String[] getLayout() {
                        return layout;
                }

                @Override
                protected int getSize() {
                        return size;
                }

                @Override
                protected int getUpdateSchedule() {
                        return updateSchedule;
                }

                @Override
                protected boolean shouldAutoFill() {
                        return autoFill;
                }

                @Override
                protected ItemStack createFillItem(final Player viewer) {
                        fillItemCreated = true;
                        return fillerSupplier.get();
                }

                @Override
                protected Map<String, Object> getTitlePlaceholders(final OpenContext open) {
                        return titlePlaceholders;
                }

                @Override
                protected String getTitleKey() {
                        return titleKey;
                }

                @Override
                public void onFirstRender(final RenderContext render, final Player viewer) {
                        firstRenderExecuted = true;
                        if (viewer instanceof PlayerMock mock) {
                                firstRenderHook.accept(render, mock);
                        }
                }

                void setAutoFill(final boolean autoFill) {
                        this.autoFill = autoFill;
                }

                void setFirstRenderHook(final BiConsumer<RenderContext, PlayerMock> hook) {
                        this.firstRenderHook = hook;
                }

                void setTitleKey(final String titleKey) {
                        this.titleKey = titleKey;
                }

                boolean wasFillItemCreated() {
                        return fillItemCreated;
                }

                boolean wasFirstRenderExecuted() {
                        return firstRenderExecuted;
                }
        }
}
