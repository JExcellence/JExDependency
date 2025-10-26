package com.raindropcentral.rdq.view.bounty;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.unified.IUnifiedItemBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BountyMainViewTest {

    private ServerMock server;
    private PlayerMock player;
    private BountyService bountyService;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
        this.bountyService = Mockito.mock(BountyService.class);
        BountyServiceProvider.setInstance(this.bountyService);
        Mockito.when(this.bountyService.isPremium()).thenReturn(true);
        Mockito.when(this.bountyService.canCreateBounty(this.player)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        BountyServiceProvider.reset();
        MockBukkit.unmock();
    }

    @Test
    void onFirstRenderFetchesCountAndConfiguresBrowseSlot() {
        final CompletableFuture<Integer> totalCount = new CompletableFuture<>();
        Mockito.when(this.bountyService.getTotalBountyCount()).thenReturn(totalCount);

        final List<SlotCapture> captures = new ArrayList<>();
        final RenderContext render = this.createRenderContext(captures);
        final BountyMainView view = new BountyMainView();

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders();
             MockedStatic<TranslationService> translations = this.mockTranslations()) {
            view.onFirstRender(render, this.player);

            Mockito.verify(this.bountyService).getTotalBountyCount();

            totalCount.complete(7);

            final SlotCapture browseSlot = this.findSlot(captures, 11);
            assertNotNull(browseSlot, "Browse slot should be registered");
            assertEquals(Material.DIAMOND_SWORD, browseSlot.itemStack().getType());
            assertNotNull(browseSlot.clickHandler(), "Browse slot should capture a click handler");

            final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
            browseSlot.clickHandler().accept(clickContext);
            Mockito.verify(clickContext).openForPlayer(BountyOverviewView.class);
        }
    }

    @Test
    void createBountySlotReflectsCreationEligibility() {
        Mockito.when(this.bountyService.getTotalBountyCount()).thenReturn(CompletableFuture.completedFuture(2));
        Mockito.when(this.bountyService.canCreateBounty(this.player)).thenReturn(true);

        final List<SlotCapture> captures = new ArrayList<>();
        final RenderContext render = this.createRenderContext(captures);
        final BountyMainView view = new BountyMainView();

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders();
             MockedStatic<TranslationService> translations = this.mockTranslations()) {
            view.onFirstRender(render, this.player);

            final TranslationKey allowedName = TranslationKey.of("bounty.main", "create_bounty.name");
            final TranslationKey allowedLore = TranslationKey.of("bounty.main", "create_bounty.lore");
            final TranslationKey lockedName = TranslationKey.of("bounty.main", "create_bounty_locked.name");
            final TranslationKey lockedLore = TranslationKey.of("bounty.main", "create_bounty_locked.lore");

            translations.verify(() -> TranslationService.create(allowedName, this.player));
            translations.verify(() -> TranslationService.create(allowedLore, this.player));
            translations.verify(() -> TranslationService.create(lockedName, this.player), Mockito.never());
            translations.verify(() -> TranslationService.create(lockedLore, this.player), Mockito.never());
            builders.verify(() -> UnifiedBuilderFactory.item(Material.EMERALD));

            final SlotCapture createSlot = this.findSlot(captures, 13);
            assertNotNull(createSlot, "Create slot should be registered");
            assertEquals(Material.EMERALD, createSlot.itemStack().getType());
            assertNotNull(createSlot.clickHandler(), "Create slot should capture a click handler");

            final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
            createSlot.clickHandler().accept(clickContext);
            Mockito.verify(clickContext).openForPlayer(Mockito.eq(BountyCreationView.class), Mockito.any(Map.class));
        }
    }

    @Test
    void lockedCreateBountySlotBlocksNavigationAndNotifiesPlayer() {
        Mockito.when(this.bountyService.getTotalBountyCount()).thenReturn(CompletableFuture.completedFuture(0));
        Mockito.when(this.bountyService.canCreateBounty(this.player)).thenReturn(false);

        final List<SlotCapture> captures = new ArrayList<>();
        final RenderContext render = this.createRenderContext(captures);
        final BountyMainView view = new BountyMainView();

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders();
             MockedStatic<TranslationService> translations = this.mockTranslations()) {
            view.onFirstRender(render, this.player);

            final TranslationKey lockedName = TranslationKey.of("bounty.main", "create_bounty_locked.name");
            final TranslationKey lockedLore = TranslationKey.of("bounty.main", "create_bounty_locked.lore");
            final TranslationKey lockedMessage = TranslationKey.of("bounty.main", "create_bounty_locked.message");
            translations.verify(() -> TranslationService.create(lockedName, this.player));
            translations.verify(() -> TranslationService.create(lockedLore, this.player));
            translations.verify(() -> TranslationService.create(lockedMessage, this.player));
            builders.verify(() -> UnifiedBuilderFactory.item(Material.BARRIER));

            final SlotCapture createSlot = this.findSlot(captures, 13);
            assertNotNull(createSlot, "Locked create slot should be registered");
            assertEquals(Material.BARRIER, createSlot.itemStack().getType());

            final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
            createSlot.clickHandler().accept(clickContext);

            assertEquals("bounty.main.create_bounty_locked.message", this.player.nextMessage());
            Mockito.verify(clickContext, Mockito.never()).openForPlayer(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void nonPremiumPlayersSeeUpgradePrompt() {
        Mockito.when(this.bountyService.getTotalBountyCount()).thenReturn(CompletableFuture.completedFuture(1));
        Mockito.when(this.bountyService.canCreateBounty(this.player)).thenReturn(false);
        Mockito.when(this.bountyService.isPremium()).thenReturn(false);

        final List<SlotCapture> captures = new ArrayList<>();
        final RenderContext render = this.createRenderContext(captures);
        final BountyMainView view = new BountyMainView();

        try (MockedStatic<UnifiedBuilderFactory> builders = this.mockItemBuilders();
             MockedStatic<TranslationService> translations = this.mockTranslations()) {
            view.onFirstRender(render, this.player);

            final TranslationKey upgradeName = TranslationKey.of("bounty.main", "upgrade_premium.name");
            final TranslationKey upgradeLore = TranslationKey.of("bounty.main", "upgrade_premium.lore");
            final TranslationKey upgradeMessage = TranslationKey.of("bounty.main", "upgrade_premium.message");
            translations.verify(() -> TranslationService.create(upgradeName, this.player));
            translations.verify(() -> TranslationService.create(upgradeLore, this.player));
            translations.verify(() -> TranslationService.create(upgradeMessage, this.player));
            builders.verify(() -> UnifiedBuilderFactory.item(Material.NETHER_STAR));

            final SlotCapture upgradeSlot = this.findSlot(captures, 15);
            assertNotNull(upgradeSlot, "Upgrade slot should be registered");
            assertEquals(Material.NETHER_STAR, upgradeSlot.itemStack().getType());

            final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
            upgradeSlot.clickHandler().accept(clickContext);

            assertEquals("bounty.main.upgrade_premium.message", this.player.nextMessage());
            Mockito.verify(clickContext).closeForPlayer();
        }
    }

    private RenderContext createRenderContext(final List<SlotCapture> captures) {
        final RenderContext render = Mockito.mock(RenderContext.class);
        Mockito.when(render.getPlayer()).thenReturn(this.player);
        Mockito.when(render.slot(Mockito.anyInt(), Mockito.any(ItemStack.class))).thenAnswer(invocation -> {
            final int index = invocation.getArgument(0);
            final ItemStack item = invocation.getArgument(1);
            final SlotCapture capture = new SlotCapture(index, item);
            captures.add(capture);

            final BukkitItemComponentBuilder slotBuilder = Mockito.mock(BukkitItemComponentBuilder.class);
            Mockito.when(slotBuilder.onClick(Mockito.any())).thenAnswer(clickInvocation -> {
                @SuppressWarnings("unchecked") final Consumer<SlotClickContext> handler = clickInvocation.getArgument(0);
                capture.setClickHandler(handler);
                return slotBuilder;
            });
            return slotBuilder;
        });
        return render;
    }

    private MockedStatic<UnifiedBuilderFactory> mockItemBuilders() {
        final MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class);
        builders.when(() -> UnifiedBuilderFactory.item(Mockito.any(Material.class))).thenAnswer(invocation -> {
            final Material material = invocation.getArgument(0);
            return this.createItemBuilder(material);
        });
        return builders;
    }

    private IUnifiedItemBuilder<?, ?> createItemBuilder(final Material material) {
        @SuppressWarnings("unchecked")
        final IUnifiedItemBuilder<?, ?> builder = Mockito.mock(IUnifiedItemBuilder.class);
        Mockito.when(builder.setName(Mockito.any())).thenReturn(builder);
        Mockito.when(builder.setLore(Mockito.anyList())).thenReturn(builder);
        Mockito.when(builder.addItemFlags(Mockito.<ItemFlag[]>any())).thenReturn(builder);
        Mockito.when(builder.build()).thenAnswer(invocation -> new ItemStack(material));
        return builder;
    }

    private MockedStatic<TranslationService> mockTranslations() {
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        translations.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any(Player.class)))
            .thenAnswer(invocation -> {
                final TranslationKey key = invocation.getArgument(0);
                final Player target = invocation.getArgument(1);
                return this.createTranslation(key, target);
            });
        return translations;
    }

    private TranslationService createTranslation(final TranslationKey key, final Player target) {
        final TranslationService translation = Mockito.mock(TranslationService.class);
        final TranslatedMessage message = new TranslatedMessage(Component.text(key.key()), key);

        Mockito.when(translation.with(Mockito.anyString(), Mockito.any())).thenReturn(translation);
        Mockito.when(translation.withPrefix()).thenReturn(translation);
        Mockito.when(translation.build()).thenReturn(message);
        Mockito.doAnswer(invocation -> {
            target.sendMessage(key.key());
            return null;
        }).when(translation).send();
        return translation;
    }

    private SlotCapture findSlot(final List<SlotCapture> captures, final int slot) {
        return captures.stream()
            .filter(capture -> capture.index() == slot)
            .findFirst()
            .orElse(null);
    }

    private static final class SlotCapture {
        private final int index;
        private final ItemStack itemStack;
        private Consumer<SlotClickContext> clickHandler;

        private SlotCapture(final int index, final ItemStack itemStack) {
            this.index = index;
            this.itemStack = itemStack;
        }

        private int index() {
            return this.index;
        }

        private ItemStack itemStack() {
            return this.itemStack;
        }

        private Consumer<SlotClickContext> clickHandler() {
            return this.clickHandler;
        }

        private void setClickHandler(final Consumer<SlotClickContext> handler) {
            this.clickHandler = handler;
        }
    }
}
