package com.raindropcentral.rplatform.view;

import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class APaginatedViewTest {

    private TestPaginatedView view;
    private RenderContext renderContext;
    private Player player;
    private Pagination pagination;
    private State<Pagination> paginationState;
    private BukkitItemComponentBuilder previousBuilder;
    private BukkitItemComponentBuilder nextBuilder;
    private MockedStatic<TranslationService> translationServiceMock;
    private MockedStatic<UnifiedBuilderFactory> unifiedBuilderMock;

    @BeforeEach
    void setUp() throws Exception {
        view = new TestPaginatedView();
        renderContext = Mockito.mock(RenderContext.class);
        player = Mockito.mock(Player.class);
        pagination = Mockito.mock(Pagination.class);
        paginationState = Mockito.mock(State.class);
        previousBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);
        nextBuilder = Mockito.mock(BukkitItemComponentBuilder.class, Answers.RETURNS_SELF);

        when(paginationState.get(renderContext)).thenReturn(pagination);

        translationServiceMock = Mockito.mockStatic(TranslationService.class);
        TranslatedMessage translatedMessage = Mockito.mock(TranslatedMessage.class);
        when(translatedMessage.component()).thenReturn(Component.empty());
        when(translatedMessage.splitLines()).thenReturn(Collections.emptyList());

        TranslationService translationBuilder = Mockito.mock(TranslationService.class, Answers.RETURNS_SELF);
        when(translationBuilder.build()).thenReturn(translatedMessage);
        translationServiceMock.when(() -> TranslationService.create(any(), any())).thenReturn(translationBuilder);

        unifiedBuilderMock = Mockito.mockStatic(UnifiedBuilderFactory.class);
        unifiedBuilderMock.when(UnifiedBuilderFactory::head).thenReturn(new StubHeadBuilder());

        Mockito.doReturn(Collections.emptyList()).when(renderContext).getLayoutSlots();
        Mockito.doReturn(previousBuilder).when(renderContext).layoutSlot(eq(view.getPreviousButtonChar()), any());
        Mockito.doReturn(nextBuilder).when(renderContext).layoutSlot(eq(view.getNextButtonChar()), any());

        setPaginationStateField(paginationState);
    }

    @AfterEach
    void tearDown() {
        translationServiceMock.close();
        unifiedBuilderMock.close();
    }

    @Test
    void onFirstRenderBindsNavigationButtonsAndCallbacks() {
        when(pagination.canBack()).thenReturn(true);
        when(pagination.canAdvance()).thenReturn(true);

        view.onFirstRender(renderContext, player);

        verify(previousBuilder).updateOnStateChange(paginationState);
        verify(nextBuilder).updateOnStateChange(paginationState);

        ArgumentCaptor<BooleanSupplier> previousVisibility = ArgumentCaptor.forClass(BooleanSupplier.class);
        ArgumentCaptor<BooleanSupplier> nextVisibility = ArgumentCaptor.forClass(BooleanSupplier.class);
        verify(previousBuilder).displayIf(previousVisibility.capture());
        verify(nextBuilder).displayIf(nextVisibility.capture());
        assertTrue(previousVisibility.getValue().getAsBoolean());
        assertTrue(nextVisibility.getValue().getAsBoolean());

        ArgumentCaptor<Runnable> backCallback = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> nextCallback = ArgumentCaptor.forClass(Runnable.class);
        verify(previousBuilder).onClick(backCallback.capture());
        verify(nextBuilder).onClick(nextCallback.capture());

        backCallback.getValue().run();
        nextCallback.getValue().run();

        verify(pagination).back();
        verify(pagination).advance();
    }

    @Test
    void navigationVisibilityReflectsBoundaries() {
        when(pagination.canBack()).thenReturn(false);
        when(pagination.canAdvance()).thenReturn(false);

        view.onFirstRender(renderContext, player);

        ArgumentCaptor<BooleanSupplier> previousVisibility = ArgumentCaptor.forClass(BooleanSupplier.class);
        ArgumentCaptor<BooleanSupplier> nextVisibility = ArgumentCaptor.forClass(BooleanSupplier.class);
        verify(previousBuilder).displayIf(previousVisibility.capture());
        verify(nextBuilder).displayIf(nextVisibility.capture());

        assertFalse(previousVisibility.getValue().getAsBoolean());
        assertFalse(nextVisibility.getValue().getAsBoolean());
    }

    @Test
    void asyncSourceSuppliesItemsAndEmptyState() {
        Context context = Mockito.mock(Context.class);

        view.setItems(List.of("first", "second"));
        assertEquals(List.of("first", "second"), view.getAsyncPaginationSource(context).join());

        view.setItems(Collections.emptyList());
        assertEquals(Collections.emptyList(), view.getAsyncPaginationSource(context).join());
    }

    @Test
    void paginationStatePreservedAcrossPageChanges() {
        AtomicInteger pageIndex = new AtomicInteger(0);
        when(pagination.currentPageIndex()).thenAnswer(invocation -> pageIndex.get());

        Pagination retrievedInitial = view.exposePagination(renderContext);
        assertSame(pagination, retrievedInitial);

        pageIndex.set(1);
        Pagination retrievedNext = view.exposePagination(renderContext);
        assertSame(retrievedInitial, retrievedNext);
        verify(paginationState, times(2)).get(renderContext);
    }

    @Test
    void renderEntryTracksRenderedItems() {
        Context context = Mockito.mock(Context.class);
        BukkitItemComponentBuilder builder = Mockito.mock(BukkitItemComponentBuilder.class);

        view.renderEntry(context, builder, 0, "alpha");
        view.renderEntry(context, builder, 1, "beta");

        assertEquals(List.of("alpha", "beta"), view.getRenderedEntries());
        assertEquals(List.of(0, 1), view.getRenderedIndices());
    }

    private void setPaginationStateField(State<Pagination> state) throws Exception {
        var field = APaginatedView.class.getDeclaredField("pagination");
        field.setAccessible(true);
        field.set(view, state);
    }

    private static final class TestPaginatedView extends APaginatedView<String> {

        private List<String> items = new ArrayList<>();
        private final List<String> renderedEntries = new ArrayList<>();
        private final List<Integer> renderedIndices = new ArrayList<>();

        @Override
        protected CompletableFuture<List<String>> getAsyncPaginationSource(@NotNull Context context) {
            return CompletableFuture.completedFuture(new ArrayList<>(items));
        }

        @Override
        protected void renderEntry(@NotNull Context context, @NotNull BukkitItemComponentBuilder builder, int index, @NotNull String entry) {
            renderedIndices.add(index);
            renderedEntries.add(entry);
        }

        @Override
        protected void onPaginatedRender(@NotNull RenderContext render, @NotNull Player player) {
            // No-op for tests.
        }

        @Override
        protected String getKey() {
            return "test.paginated";
        }

        void setItems(List<String> items) {
            this.items = new ArrayList<>(items);
        }

        List<String> getRenderedEntries() {
            return renderedEntries;
        }

        List<Integer> getRenderedIndices() {
            return renderedIndices;
        }

        Pagination exposePagination(RenderContext context) {
            return getPagination(context);
        }
    }

    private static final class StubHeadBuilder implements IHeadBuilder<StubHeadBuilder> {

        private final ItemStack itemStack = new ItemStack(Material.PAPER);

        @Override
        public StubHeadBuilder setPlayerHead(@Nullable Player player) {
            return this;
        }

        @Override
        public StubHeadBuilder setPlayerHead(@Nullable org.bukkit.OfflinePlayer offlinePlayer) {
            return this;
        }

        @Override
        public StubHeadBuilder setCustomTexture(@NotNull java.util.UUID uuid, @NotNull String textures) {
            return this;
        }

        @Override
        public StubHeadBuilder setName(@NotNull Component name) {
            return this;
        }

        @Override
        public StubHeadBuilder setLore(@NotNull List<Component> lore) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLine(@NotNull Component line) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLines(@NotNull List<Component> lore) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLines(@NotNull Component... lore) {
            return this;
        }

        @Override
        public StubHeadBuilder setAmount(int amount) {
            return this;
        }

        @Override
        public StubHeadBuilder setCustomModelData(int data) {
            return this;
        }

        @Override
        public StubHeadBuilder addEnchantment(@NotNull org.bukkit.enchantments.Enchantment enchantment, int level) {
            return this;
        }

        @Override
        public StubHeadBuilder addItemFlags(@NotNull org.bukkit.inventory.ItemFlag... flags) {
            return this;
        }

        @Override
        public StubHeadBuilder setGlowing(boolean glowing) {
            return this;
        }

        @Override
        public ItemStack build() {
            return itemStack.clone();
        }
    }
}
