package com.raindropcentral.rdq.utility;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.bounty.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.type.EBountyClaimMode;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BountyManagerTest {

    private static ServerMock server;
    private static JavaPlugin plugin;

    private RDQ rdq;
    private RPlatform platform;
    private PlatformAPI platformAPI;
    private ISchedulerAdapter scheduler;
    private RBountyRepository bountyRepository;
    private RDQPlayerRepository playerRepository;
    private ExecutorService executor;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterAll
    static void shutdownServer() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @BeforeEach
    void setUp() {
        this.executor = new DirectExecutorService();
        this.rdq = mock(RDQ.class);
        this.platform = mock(RPlatform.class);
        this.platformAPI = mock(PlatformAPI.class);
        this.scheduler = mock(ISchedulerAdapter.class);
        this.bountyRepository = mock(RBountyRepository.class);
        this.playerRepository = mock(RDQPlayerRepository.class);

        when(this.rdq.getPlugin()).thenReturn(plugin);
        when(this.rdq.getPlatform()).thenReturn(this.platform);
        when(this.rdq.getExecutor()).thenReturn(this.executor);
        when(this.rdq.getBountyRepository()).thenReturn(this.bountyRepository);
        when(this.rdq.getPlayerRepository()).thenReturn(this.playerRepository);

        when(this.platform.getScheduler()).thenReturn(this.scheduler);
        when(this.platform.getPlatformAPI()).thenReturn(this.platformAPI);

        doAnswer(invocation -> {
            final Runnable task = invocation.getArgument(0, Runnable.class);
            task.run();
            return null;
        }).when(this.scheduler).runSync(any());

        doAnswer(invocation -> {
            final Runnable task = invocation.getArgument(0, Runnable.class);
            task.run();
            return null;
        }).when(this.scheduler).runAsync(any());

        doAnswer(invocation -> {
            final Runnable task = invocation.getArgument(0, Runnable.class);
            task.run();
            return null;
        }).when(this.scheduler).runGlobal(any());
    }

    @Test
    void constructorUsesConfiguredClaimMode() {
        final BountyManager manager = createManagerWithClaimMode(EBountyClaimMode.MOST_DAMAGE);

        assertEquals(EBountyClaimMode.MOST_DAMAGE, readClaimMode(manager));
    }

    @Test
    void constructorFallsBackToLastHitWhenConfigurationFails() {
        final BountyManager manager = createManagerWithFailingConfiguration();

        assertEquals(EBountyClaimMode.LAST_HIT, readClaimMode(manager));
    }

    @Test
    void createTrackAndHandleBountyLifecycle() {
        final Player commissioner = server.addPlayer("Commissioner");
        final Player targetPlayer = server.addPlayer("Target");
        final Player attacker = server.addPlayer("Attacker");
        final RDQPlayer target = new RDQPlayer(targetPlayer.getUniqueId(), targetPlayer.getName());
        final Set<RewardItem> rewardItems = Set.of(new RewardItem(new ItemStack(Material.DIAMOND, 3)));
        final Map<String, Double> rewardCurrencies = Map.of("tokens", 12.5d);

        when(this.bountyRepository.createAsync(any(RBounty.class))).thenAnswer(invocation -> {
            final RBounty bounty = invocation.getArgument(0, RBounty.class);
            return CompletableFuture.completedFuture(bounty);
        });
        when(this.playerRepository.updateAsync(any(RDQPlayer.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(this.bountyRepository.deleteAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<TranslationService> translation = mockTranslations(key -> Component.text(key.key()))) {
            final BountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);

            manager.createBounty(target, commissioner, rewardItems, rewardCurrencies);

            assertTrue(manager.hasActiveBounty(targetPlayer.getUniqueId()));
            assertNotNull(manager.getBounty(targetPlayer.getUniqueId()));

            manager.trackDamage(targetPlayer.getUniqueId(), attacker.getUniqueId(), 5.0d);

            final Map<UUID, Map<UUID, Double>> damageTracker = readDamageTracker(manager);
            assertThat(damageTracker).containsKey(targetPlayer.getUniqueId());
            assertEquals(5.0d, damageTracker.get(targetPlayer.getUniqueId()).get(attacker.getUniqueId()));

            final Map<UUID, UUID> lastHitTracker = readLastHitTracker(manager);
            assertEquals(attacker.getUniqueId(), lastHitTracker.get(targetPlayer.getUniqueId()));

            manager.handleBountyKill(targetPlayer);

            assertFalse(manager.hasActiveBounty(targetPlayer.getUniqueId()));
            assertNull(manager.getBounty(targetPlayer.getUniqueId()));
            assertFalse(readDamageTracker(manager).containsKey(targetPlayer.getUniqueId()));
            assertFalse(readLastHitTracker(manager).containsKey(targetPlayer.getUniqueId()));
            assertNull(target.getBounty());
        }

        verify(this.bountyRepository).createAsync(any(RBounty.class));
        verify(this.playerRepository).updateAsync(eq(target));
        verify(this.bountyRepository).deleteAsync(any());
        verify(this.scheduler, atLeast(1)).runSync(any());
    }

    @Test
    void updateDisplayReflectsBountyStatus() {
        final Player player = server.addPlayer("DisplayTarget");
        final RDQPlayer rdqPlayer = new RDQPlayer(player.getUniqueId(), player.getName());
        final RBounty bounty = new RBounty(rdqPlayer, player, Collections.emptySet(), Collections.emptyMap());
        final BountyManager manager = createManagerWithClaimMode(EBountyClaimMode.MOST_DAMAGE);

        readActiveBounties(manager).put(player.getUniqueId(), bounty);

        try (MockedStatic<TranslationService> translation = mockTranslations(key -> {
            if ("bounty.display.player_list_name".equals(key.key())) {
                return Component.text("☠ " + player.getName());
            }
            return Component.text(key.key());
        })) {
            manager.updateBountyPlayerDisplay(player.getUniqueId());
            manager.removeBounty(player.getUniqueId());
        }

        final ArgumentCaptor<Component> displayCaptor = ArgumentCaptor.forClass(Component.class);
        verify(this.platformAPI, atLeastOnce()).setDisplayName(eq(player), displayCaptor.capture());

        final List<Component> captured = displayCaptor.getAllValues();
        assertThat(captured).isNotEmpty();
        final String first = PlainTextComponentSerializer.plainText().serialize(captured.get(0));
        assertEquals("☠ " + player.getName(), first);
        final String last = PlainTextComponentSerializer.plainText().serialize(captured.get(captured.size() - 1));
        assertEquals(player.getName(), last);
    }

    @Test
    void giveRewardItemsDropsLeftoversAndStubsReturnSameBounty() {
        final Player player = server.addPlayer("RewardReceiver");
        final ItemStack[] fullInventory = new ItemStack[player.getInventory().getSize()];
        Arrays.fill(fullInventory, new ItemStack(Material.DIRT, 64));
        player.getInventory().setContents(fullInventory);

        final RewardItem rewardItem = new RewardItem(new ItemStack(Material.GOLD_INGOT, 1));
        final Set<RewardItem> rewards = Set.of(rewardItem);
        final BountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);

        try (MockedStatic<TranslationService> translation = mockTranslations(key -> Component.text(key.key()))) {
            manager.giveRewardItemsToPlayer(player, rewards);
        }

        final long droppedItems = player.getWorld().getEntities().stream()
                .filter(entity -> entity instanceof Item)
                .count();
        assertEquals(1L, droppedItems);

        final RBounty bounty = new RBounty(new RDQPlayer(UUID.randomUUID(), "Stub"), player);
        assertSame(bounty, manager.addItemRewards(bounty, List.of(new ItemStack(Material.DIAMOND))));
        assertSame(bounty, manager.addCurrencyReward(bounty, "coins", 5.0d));
    }

    private BountyManager createManagerWithClaimMode(final EBountyClaimMode mode) {
        BountyManager manager;
        try (MockedConstruction<ConfigManager> ignored = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keepers = mockConstruction(ConfigKeeper.class, (mock, context) -> {
                 final BountySection section = mock(BountySection.class);
                 when(section.getClaimMode()).thenReturn(mode);
                 injectRootSection(mock, section);
             })) {
            manager = new BountyManager(this.rdq);
        }
        return manager;
    }

    private BountyManager createManagerWithFailingConfiguration() {
        BountyManager manager;
        try (MockedConstruction<ConfigManager> ignored = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keepers = mockConstruction(ConfigKeeper.class, (mock, context) -> {
                 final BountySection section = mock(BountySection.class);
                 when(section.getClaimMode()).thenThrow(new IllegalStateException("boom"));
                 injectRootSection(mock, section);
             })) {
            manager = new BountyManager(this.rdq);
        }
        return manager;
    }

    private MockedStatic<TranslationService> mockTranslations(final java.util.function.Function<TranslationKey, Component> componentProvider) {
        final MockedStatic<TranslationService> translation = mockStatic(TranslationService.class);

        translation.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                .thenAnswer(invocation -> createTranslationMock(componentProvider.apply(invocation.getArgument(0, TranslationKey.class)),
                        invocation.getArgument(0, TranslationKey.class)));
        translation.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class), any()))
                .thenAnswer(invocation -> createTranslationMock(componentProvider.apply(invocation.getArgument(0, TranslationKey.class)),
                        invocation.getArgument(0, TranslationKey.class)));
        return translation;
    }

    private TranslationService createTranslationMock(final Component component, final TranslationKey key) {
        final TranslationService service = mock(TranslationService.class, RETURNS_SELF);
        when(service.build()).thenReturn(new TranslatedMessage(component == null ? Component.text(key.key()) : component, key));
        return service;
    }

    private EBountyClaimMode readClaimMode(final BountyManager manager) {
        try {
            final Field field = BountyManager.class.getDeclaredField("claimMode");
            field.setAccessible(true);
            return (EBountyClaimMode) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<UUID, Double>> readDamageTracker(final BountyManager manager) {
        try {
            final Field field = BountyManager.class.getDeclaredField("damageTracker");
            field.setAccessible(true);
            return (Map<UUID, Map<UUID, Double>>) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, UUID> readLastHitTracker(final BountyManager manager) {
        try {
            final Field field = BountyManager.class.getDeclaredField("lastHitTracker");
            field.setAccessible(true);
            return (Map<UUID, UUID>) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, RBounty> readActiveBounties(final BountyManager manager) {
        try {
            final Field field = BountyManager.class.getDeclaredField("activeBounties");
            field.setAccessible(true);
            return (Map<UUID, RBounty>) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    private void injectRootSection(final ConfigKeeper<?> keeper, final BountySection section) {
        try {
            final Field field = ConfigKeeper.class.getDeclaredField("rootSection");
            field.setAccessible(true);
            field.set(keeper, section);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to inject rootSection", exception);
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {

        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            this.shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return this.shutdown;
        }

        @Override
        public boolean isTerminated() {
            return this.shutdown;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return this.shutdown;
        }

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    }
}
