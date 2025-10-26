package com.raindropcentral.rplatform.view;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationViewTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void acceptAndDeclineCallbacksReturnMergedDataAndInvokeConsumer() throws Exception {
        final ConfirmationView view = new ConfirmationView();
        final List<Boolean> decisions = new ArrayList<>();
        final Consumer<Boolean> callback = decisions::add;

        final SlotClickContext confirmContext = Mockito.mock(SlotClickContext.class);
        final Map<String, Object> confirmInitial = new HashMap<>();
        confirmInitial.put("id", 42);
        setStateValue(view, "initialData", confirmContext, confirmInitial);
        setStateValue(view, "callback", confirmContext, callback);

        final Method handleConfirm = ConfirmationView.class.getDeclaredMethod("handleConfirm", Context.class);
        handleConfirm.setAccessible(true);
        handleConfirm.invoke(view, confirmContext);

        final ArgumentCaptor<Map<String, Object>> confirmCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(confirmContext).back(confirmCaptor.capture());
        final Map<String, Object> confirmResult = confirmCaptor.getValue();
        assertEquals(Boolean.TRUE, confirmResult.get("confirmed"));
        assertEquals(42, confirmResult.get("id"));

        final SlotClickContext cancelContext = Mockito.mock(SlotClickContext.class);
        final Map<String, Object> cancelInitial = new HashMap<>();
        cancelInitial.put("id", 73);
        setStateValue(view, "initialData", cancelContext, cancelInitial);
        setStateValue(view, "callback", cancelContext, callback);

        final Method handleCancel = ConfirmationView.class.getDeclaredMethod("handleCancel", Context.class);
        handleCancel.setAccessible(true);
        handleCancel.invoke(view, cancelContext);

        final ArgumentCaptor<Map<String, Object>> cancelCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(cancelContext).back(cancelCaptor.capture());
        final Map<String, Object> cancelResult = cancelCaptor.getValue();
        assertEquals(Boolean.FALSE, cancelResult.get("confirmed"));
        assertEquals(73, cancelResult.get("id"));

        final SlotClickContext backContext = Mockito.mock(SlotClickContext.class);
        final Map<String, Object> backInitial = new HashMap<>();
        backInitial.put("id", 7);
        setStateValue(view, "initialData", backContext, backInitial);
        setStateValue(view, "callback", backContext, callback);

        view.handleBackButtonClick(backContext);

        final ArgumentCaptor<Map<String, Object>> backCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(backContext).back(backCaptor.capture());
        final Map<String, Object> backResult = backCaptor.getValue();
        assertEquals(Boolean.FALSE, backResult.get("confirmed"));
        assertEquals(7, backResult.get("id"));

        assertIterableEquals(List.of(true, false, false), decisions);
    }

    @Test
    void firstRenderBuildsLocalizedButtonsWithPlaceholders() throws Exception {
        final ConfirmationView view = new ConfirmationView();
        final RenderContext render = Mockito.mock(RenderContext.class);
        Mockito.when(render.getPlayer()).thenReturn(this.player);

        final BukkitItemComponentBuilder confirmSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class);
        Mockito.when(confirmSlotBuilder.onClick(Mockito.any())).thenReturn(confirmSlotBuilder);
        final BukkitItemComponentBuilder cancelSlotBuilder = Mockito.mock(BukkitItemComponentBuilder.class);
        Mockito.when(cancelSlotBuilder.onClick(Mockito.any())).thenReturn(cancelSlotBuilder);
        Mockito.when(render.layoutSlot(Mockito.eq('c'), Mockito.any(ItemStack.class))).thenReturn(confirmSlotBuilder);
        Mockito.when(render.layoutSlot(Mockito.eq('x'), Mockito.any(ItemStack.class))).thenReturn(cancelSlotBuilder);

        final String translationBase = "confirmation.test";
        final Map<String, Object> placeholders = Map.of("player", this.player.getName());
        setStateValue(view, "customKey", render, translationBase);
        setStateValue(view, "initialData", render, placeholders);

        final TranslationKey confirmNameKey = TranslationKey.of(translationBase, "confirm.name");
        final TranslationKey confirmLoreKey = TranslationKey.of(translationBase, "confirm.lore");
        final TranslationKey cancelNameKey = TranslationKey.of(translationBase, "cancel.name");
        final TranslationKey cancelLoreKey = TranslationKey.of(translationBase, "cancel.lore");

        final TranslationService confirmNameService = Mockito.mock(TranslationService.class);
        final TranslationService confirmLoreService = Mockito.mock(TranslationService.class);
        final TranslationService cancelNameService = Mockito.mock(TranslationService.class);
        final TranslationService cancelLoreService = Mockito.mock(TranslationService.class);

        final TranslatedMessage confirmNameMessage = new TranslatedMessage(Component.text("Confirm"), confirmNameKey);
        final TranslatedMessage confirmLoreMessage = new TranslatedMessage(Component.text("Line 1\nLine 2"), confirmLoreKey);
        final TranslatedMessage cancelNameMessage = new TranslatedMessage(Component.text("Cancel"), cancelNameKey);
        final TranslatedMessage cancelLoreMessage = new TranslatedMessage(Component.text("Back 1\nBack 2"), cancelLoreKey);

        Mockito.when(confirmNameService.build()).thenReturn(confirmNameMessage);
        Mockito.when(confirmLoreService.withAll(placeholders)).thenReturn(confirmLoreService);
        Mockito.when(confirmLoreService.build()).thenReturn(confirmLoreMessage);
        Mockito.when(cancelNameService.build()).thenReturn(cancelNameMessage);
        Mockito.when(cancelLoreService.withAll(placeholders)).thenReturn(cancelLoreService);
        Mockito.when(cancelLoreService.build()).thenReturn(cancelLoreMessage);

        @SuppressWarnings("unchecked")
        final IUnifiedItemBuilder<?, ?> confirmBuilder = (IUnifiedItemBuilder<?, ?>) Mockito.mock(IUnifiedItemBuilder.class);
        Mockito.when(confirmBuilder.setName(Mockito.any())).thenReturn(confirmBuilder);
        Mockito.when(confirmBuilder.setLore(Mockito.anyList())).thenReturn(confirmBuilder);
        final ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        Mockito.when(confirmBuilder.build()).thenReturn(confirmItem);

        @SuppressWarnings("unchecked")
        final IUnifiedItemBuilder<?, ?> cancelBuilder = (IUnifiedItemBuilder<?, ?>) Mockito.mock(IUnifiedItemBuilder.class);
        Mockito.when(cancelBuilder.setName(Mockito.any())).thenReturn(cancelBuilder);
        Mockito.when(cancelBuilder.setLore(Mockito.anyList())).thenReturn(cancelBuilder);
        final ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        Mockito.when(cancelBuilder.build()).thenReturn(cancelItem);

        try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
             MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            builders.when(() -> UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)).thenReturn(confirmBuilder);
            builders.when(() -> UnifiedBuilderFactory.item(Material.RED_STAINED_GLASS_PANE)).thenReturn(cancelBuilder);

            translations.when(() -> TranslationService.create(confirmNameKey, this.player)).thenReturn(confirmNameService);
            translations.when(() -> TranslationService.create(confirmLoreKey, this.player)).thenReturn(confirmLoreService);
            translations.when(() -> TranslationService.create(cancelNameKey, this.player)).thenReturn(cancelNameService);
            translations.when(() -> TranslationService.create(cancelLoreKey, this.player)).thenReturn(cancelLoreService);

            view.onFirstRender(render, this.player);

            builders.verify(() -> UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE));
            builders.verify(() -> UnifiedBuilderFactory.item(Material.RED_STAINED_GLASS_PANE));
            translations.verify(() -> TranslationService.create(confirmNameKey, this.player));
            translations.verify(() -> TranslationService.create(confirmLoreKey, this.player));
            translations.verify(() -> TranslationService.create(cancelNameKey, this.player));
            translations.verify(() -> TranslationService.create(cancelLoreKey, this.player));
        }

        final ArgumentCaptor<Component> confirmNameCaptor = ArgumentCaptor.forClass(Component.class);
        Mockito.verify(confirmBuilder).setName(confirmNameCaptor.capture());
        assertEquals(confirmNameMessage.component(), confirmNameCaptor.getValue());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Component>> confirmLoreCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(confirmBuilder).setLore(confirmLoreCaptor.capture());
        assertEquals(confirmLoreMessage.splitLines(), confirmLoreCaptor.getValue());
        Mockito.verify(confirmLoreService).withAll(placeholders);

        final ArgumentCaptor<Component> cancelNameCaptor = ArgumentCaptor.forClass(Component.class);
        Mockito.verify(cancelBuilder).setName(cancelNameCaptor.capture());
        assertEquals(cancelNameMessage.component(), cancelNameCaptor.getValue());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Component>> cancelLoreCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(cancelBuilder).setLore(cancelLoreCaptor.capture());
        assertEquals(cancelLoreMessage.splitLines(), cancelLoreCaptor.getValue());
        Mockito.verify(cancelLoreService).withAll(placeholders);

        Mockito.verify(render).layoutSlot('c', confirmItem);
        Mockito.verify(render).layoutSlot('x', cancelItem);
        Mockito.verify(confirmSlotBuilder).onClick(Mockito.any());
        Mockito.verify(cancelSlotBuilder).onClick(Mockito.any());
    }

    @Test
    void builderAppliesCustomPromptConfiguration() {
        final ConfirmationView.Builder builder = new ConfirmationView.Builder()
                .withKey("quest.confirmation")
                .withMessageKey("quest.confirmation.message")
                .withInitialData(Map.of("player", this.player.getName()))
                .withCallback(result -> { })
                .withParentView(DummyParentView.class);

        final Context context = Mockito.mock(Context.class);
        Mockito.doNothing().when(context).openForPlayer(Mockito.eq(ConfirmationView.class), Mockito.anyMap());

        builder.openFor(context, this.player);

        final ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(context).openForPlayer(Mockito.eq(ConfirmationView.class), dataCaptor.capture());
        final Map<String, Object> payload = dataCaptor.getValue();

        assertEquals("quest.confirmation", payload.get("key"));
        assertEquals("quest.confirmation.message", payload.get("messageKey"));
        assertEquals(this.player.getName(), ((Map<?, ?>) payload.get("initialData")).get("player"));
        assertTrue(payload.containsKey("callback"));
        assertSame(DummyParentView.class, payload.get("parentViewClass"));
    }

    private static void setStateValue(
            final ConfirmationView view,
            final String fieldName,
            final Context context,
            final Object value
    ) throws Exception {
        final Field field = ConfirmationView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final Object state = field.get(view);

        Method setter = null;
        for (final Method method : state.getClass().getMethods()) {
            if ("set".equals(method.getName()) && method.getParameterCount() == 2) {
                setter = method;
                break;
            }
        }

        if (setter == null) {
            throw new IllegalStateException("Unable to locate state setter for " + fieldName);
        }

        setter.invoke(state, value, context);
    }

    private static class DummyParentView extends View {
    }
}
