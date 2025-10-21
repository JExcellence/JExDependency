package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class PaginatedPlayerViewTest {

    @Test
    void getAsyncPaginationSourceReturnsOfflinePlayers() {
        PaginatedPlayerView view = new PaginatedPlayerView();
        Context context = Mockito.mock(Context.class);

        OfflinePlayer first = Mockito.mock(OfflinePlayer.class);
        OfflinePlayer second = Mockito.mock(OfflinePlayer.class);

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getOfflinePlayers).thenReturn(new OfflinePlayer[]{first, second});

            CompletableFuture<List<OfflinePlayer>> future = view.getAsyncPaginationSource(context);
            List<OfflinePlayer> result = future.join();

            assertEquals(List.of(first, second), result);
            mockedBukkit.verify(Bukkit::getOfflinePlayers);
        }
    }

    @Test
    void getAsyncPaginationSourceReturnsEmptyListWhenNoPlayers() {
        PaginatedPlayerView view = new PaginatedPlayerView();
        Context context = Mockito.mock(Context.class);

        try (MockedStatic<Bukkit> mockedBukkit = Mockito.mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getOfflinePlayers).thenReturn(new OfflinePlayer[0]);

            List<OfflinePlayer> result = view.getAsyncPaginationSource(context).join();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            mockedBukkit.verify(Bukkit::getOfflinePlayers);
        }
    }

    @Test
    void renderEntryBuildsLocalizedHeadAndBindsSelection() {
        PaginatedPlayerView view = new PaginatedPlayerView();
        Context context = Mockito.mock(Context.class);
        Player viewer = Mockito.mock(Player.class);
        Mockito.when(context.getPlayer()).thenReturn(viewer);

        OfflinePlayer offlinePlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(offlinePlayer.getName()).thenReturn("TestPlayer");

        @SuppressWarnings("unchecked")
        IHeadBuilder<?> headBuilder = Mockito.mock(IHeadBuilder.class, Answers.RETURNS_SELF);
        ItemStack renderedStack = Mockito.mock(ItemStack.class);
        Mockito.when(headBuilder.build()).thenReturn(renderedStack);

        try (MockedStatic<UnifiedBuilderFactory> headFactory = Mockito.mockStatic(UnifiedBuilderFactory.class)) {
            headFactory.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

            TranslationService translation = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
            TranslatedMessage message = new TranslatedMessage(
                net.kyori.adventure.text.Component.text("TestPlayer"),
                TranslationKey.of("paginated_player_ui.player_entry.name")
            );
            Mockito.when(translation.build()).thenReturn(message);

            try (MockedStatic<TranslationService> translationFactory = Mockito.mockStatic(TranslationService.class)) {
                translationFactory
                    .when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.eq(viewer)))
                    .thenReturn(translation);

                BukkitItemComponentBuilder builder = Mockito.mock(BukkitItemComponentBuilder.class);
                Mockito.when(builder.withItem(Mockito.any(ItemStack.class))).thenReturn(builder);

                AtomicReference<Consumer<SlotClickContext>> clickHandler = new AtomicReference<>();
                Mockito.when(builder.onClick(Mockito.any())).thenAnswer(invocation -> {
                    Consumer<SlotClickContext> consumer = invocation.getArgument(0);
                    clickHandler.set(consumer);
                    return builder;
                });

                view.renderEntry(context, builder, 0, offlinePlayer);

                headFactory.verify(UnifiedBuilderFactory::head);
                Mockito.verify(headBuilder).setPlayerHead(offlinePlayer);
                Mockito.verify(translation).with("player_name", "TestPlayer");
                Mockito.verify(headBuilder).setName(message.component());
                Mockito.verify(headBuilder).build();
                Mockito.verify(builder).withItem(renderedStack);
                assertNotNull(clickHandler.get(), "Click handler should be registered");

                SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
                Map<String, Object> initialData = new HashMap<>();
                initialData.put("page", 3);
                Mockito.when(clickContext.getInitialData()).thenReturn(initialData);

                clickHandler.get().accept(clickContext);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
                Mockito.verify(clickContext).back(mapCaptor.capture());
                Map<String, Object> returnedData = mapCaptor.getValue();

                assertNotSame(initialData, returnedData, "Handler should copy initial data before modification");
                assertEquals(Optional.of(offlinePlayer), returnedData.get("target"));
                assertEquals(3, returnedData.get("page"));
            }
        }
    }

    @Test
    void handleBackButtonClickPreservesInitialData() {
        PaginatedPlayerView view = new PaginatedPlayerView();
        SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
        Map<String, Object> initialData = Map.of("page", 1);
        Mockito.when(clickContext.getInitialData()).thenReturn(initialData);

        view.handleBackButtonClick(clickContext);

        Mockito.verify(clickContext).back(initialData);
    }
}
