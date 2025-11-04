package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class BountyOverviewViewTest {

    private BountyService bountyService;

    @BeforeEach
    void setUp() {
        this.bountyService = Mockito.mock(BountyService.class);
        BountyServiceProvider.setInstance(this.bountyService);
    }

    @AfterEach
    void tearDown() {
        BountyServiceProvider.reset();
    }

    @Test
    void getAsyncPaginationSourceDelegatesToServiceInstance() {
        final List<RBounty> bounties = List.of(Mockito.mock(RBounty.class));
        final CompletableFuture<List<RBounty>> expected = CompletableFuture.completedFuture(bounties);
        Mockito.when(this.bountyService.getAllBounties(1, 128)).thenReturn(expected);

        final BountyOverviewView view = new BountyOverviewView();
        final Context context = Mockito.mock(Context.class);

        final CompletableFuture<List<RBounty>> result = view.getAsyncPaginationSource(context);

        assertSame(expected, result, "View should return the service future directly");
        Mockito.verify(this.bountyService).getAllBounties(1, 128);
    }

    @Test
    void renderEntryBuildsHeadLoreAndNavigationAction() {
        final RenderContext context = Mockito.mock(RenderContext.class);
        final Player viewer = Mockito.mock(Player.class);
        Mockito.when(context.getPlayer()).thenReturn(viewer);

        final BukkitItemComponentBuilder builder = Mockito.mock(BukkitItemComponentBuilder.class);
        Mockito.when(builder.withItem(any(ItemStack.class))).thenReturn(builder);
        Mockito.when(builder.onClick(any())).thenReturn(builder);

        final UUID targetId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        final RDQPlayer targetPlayer = new RDQPlayer(targetId, "TargetUser");

        final UUID commissionerId = UUID.fromString("ffffffff-1111-2222-3333-444444444444");
        final OffsetDateTime createdAt = OffsetDateTime.of(2024, 1, 5, 14, 23, 45, 0, ZoneOffset.UTC);

        final RBounty bounty = Mockito.mock(RBounty.class);
        Mockito.when(bounty.getPlayer()).thenReturn(targetPlayer);
        Mockito.when(bounty.getCommissioner()).thenReturn(commissionerId);
        Mockito.when(bounty.getCreatedAt()).thenReturn(createdAt);
        Mockito.when(bounty.getRewardItems()).thenReturn(Set.of(
                Mockito.mock(RewardItem.class),
                Mockito.mock(RewardItem.class)
        ));

        final TranslationService nameTranslation = Mockito.mock(TranslationService.class);
        final TranslatedMessage nameMessage = Mockito.mock(TranslatedMessage.class);
        Mockito.when(nameTranslation.with(eq("target_name"), eq(targetPlayer.getPlayerName()))).thenReturn(nameTranslation);
        Mockito.when(nameTranslation.with(any(String.class), any())).thenReturn(nameTranslation);
        Mockito.when(nameTranslation.build()).thenReturn(nameMessage);
        Mockito.when(nameMessage.component()).thenReturn(Component.text("name-component"));

        final TranslationService loreTranslation = Mockito.mock(TranslationService.class);
        final TranslatedMessage loreMessage = Mockito.mock(TranslatedMessage.class);
        Mockito.when(loreTranslation.withAll(any(Map.class))).thenReturn(loreTranslation);
        Mockito.when(loreTranslation.build()).thenReturn(loreMessage);
        Mockito.when(loreMessage.splitLines()).thenReturn(List.of(Component.text("lore-line")));

        final BountyOverviewView view = Mockito.spy(new BountyOverviewView());
        Mockito.doReturn(nameTranslation).when(view).i18n("entry.name", viewer);
        Mockito.doReturn(loreTranslation).when(view).i18n("entry.lore", viewer);

        final OfflinePlayer targetOffline = Mockito.mock(OfflinePlayer.class);
        Mockito.when(targetOffline.getName()).thenReturn("StoredTarget");
        final OfflinePlayer commissionerOffline = Mockito.mock(OfflinePlayer.class);
        Mockito.when(commissionerOffline.getName()).thenReturn("Commissioner");

        final StubHeadBuilder headBuilder = new StubHeadBuilder();

        try (MockedStatic<UnifiedBuilderFactory> builders = Mockito.mockStatic(UnifiedBuilderFactory.class)) {
            builders.when(UnifiedBuilderFactory::head).thenReturn(headBuilder);

            try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
                bukkit.when(() -> Bukkit.getOfflinePlayer(eq(targetId))).thenReturn(targetOffline);
                bukkit.when(() -> Bukkit.getOfflinePlayer(eq(commissionerId))).thenReturn(commissionerOffline);

                view.renderEntry(context, builder, 4, bounty);
            }
        }

        assertSame(targetOffline, headBuilder.appliedOfflinePlayer, "Head builder should receive the target player");
        assertNotNull(headBuilder.nameComponent, "Head builder should receive a name component");
        assertNotNull(headBuilder.loreComponents, "Head builder should receive lore components");
        assertNotNull(headBuilder.itemStack, "Head builder should build an item stack");
        Mockito.verify(builder).withItem(eq(headBuilder.itemStack));

        final ArgumentCaptor<Map<String, Object>> loreCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(loreTranslation).withAll(loreCaptor.capture());
        final Map<String, Object> lorePlaceholders = loreCaptor.getValue();
        assertEquals("TargetUser", lorePlaceholders.get("target_name"));
        assertEquals("Commissioner", lorePlaceholders.get("commissioner_name"));
        assertEquals("14:23:45", lorePlaceholders.get("created_at"));
        assertEquals(2, lorePlaceholders.get("reward_count"));
        assertEquals(5, lorePlaceholders.get("index"));

        Mockito.verify(nameTranslation).with("target_name", "TargetUser");

        final ArgumentCaptor<Consumer<SlotClickContext>> clickCaptor = ArgumentCaptor.forClass(Consumer.class);
        Mockito.verify(builder).onClick(clickCaptor.capture());
        final Consumer<SlotClickContext> clickHandler = clickCaptor.getValue();
        assertNotNull(clickHandler, "Click handler should be configured");

        final SlotClickContext clickContext = Mockito.mock(SlotClickContext.class);
        clickHandler.accept(clickContext);

        final ArgumentCaptor<Map<String, Object>> navigationCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(clickContext).openForPlayer(eq(BountyPlayerInfoView.class), navigationCaptor.capture());
        final Map<String, Object> navigationData = navigationCaptor.getValue();
        assertSame(bounty, navigationData.get("bounty"));
        assertSame(targetOffline, navigationData.get("target"));
    }

    private static final class StubHeadBuilder implements IHeadBuilder<StubHeadBuilder> {

        private OfflinePlayer appliedOfflinePlayer;
        private Component nameComponent;
        private List<Component> loreComponents;
        private final ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

        @Override
        public StubHeadBuilder setPlayerHead(final OfflinePlayer offlinePlayer) {
            this.appliedOfflinePlayer = offlinePlayer;
            return this;
        }

        @Override
        public StubHeadBuilder setPlayerHead(final Player player) {
            return this;
        }

        @Override
        public StubHeadBuilder setCustomTexture(final UUID uuid, final String textures) {
            return this;
        }

        @Override
        public StubHeadBuilder setName(final Component name) {
            this.nameComponent = name;
            return this;
        }

        @Override
        public StubHeadBuilder setLore(final List<Component> lore) {
            this.loreComponents = lore;
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLine(final Component line) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLines(final List<Component> lore) {
            return this;
        }

        @Override
        public StubHeadBuilder addLoreLines(final Component... lore) {
            return this;
        }

        @Override
        public StubHeadBuilder setAmount(final int amount) {
            return this;
        }

        @Override
        public StubHeadBuilder setCustomModelData(final int data) {
            return this;
        }

        @Override
        public StubHeadBuilder addEnchantment(final org.bukkit.enchantments.Enchantment enchantment, final int level) {
            return this;
        }

        @Override
        public StubHeadBuilder addItemFlags(final ItemFlag... flags) {
            return this;
        }

        @Override
        public StubHeadBuilder setGlowing(final boolean glowing) {
            return this;
        }

        @Override
        public ItemStack build() {
            return this.itemStack;
        }
    }
}
