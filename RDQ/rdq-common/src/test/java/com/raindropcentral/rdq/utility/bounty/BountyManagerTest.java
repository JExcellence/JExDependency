package com.raindropcentral.rdq.utility.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.type.EBountyClaimMode;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPI;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BountyManager behaviour")
class BountyManagerTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private RDQ rdq;
    private BountyService bountyService;
    private RDQPlayerRepository playerRepository;
    private PlatformAPI platformAPI;
    private ExecutorService executor;
    private MockedStatic<TranslationService> translationServiceMock;
    private final List<TranslationInvocation> translationInvocations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();

        this.executor = new DirectExecutorService();

        this.rdq = mock(RDQ.class);
        this.bountyService = mock(BountyService.class);
        this.playerRepository = mock(RDQPlayerRepository.class);
        final RPlatform platform = mock(RPlatform.class);
        this.platformAPI = mock(PlatformAPI.class);

        when(this.rdq.getPlugin()).thenReturn(this.plugin);
        when(this.rdq.getExecutor()).thenReturn(this.executor);
        when(this.rdq.getPlayerRepository()).thenReturn(this.playerRepository);
        when(this.rdq.getPlatform()).thenReturn(platform);
        when(platform.getPlatformAPI()).thenReturn(this.platformAPI);

        this.translationInvocations.clear();
        this.translationServiceMock = mockStatic(TranslationService.class);
        this.translationServiceMock.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                .thenAnswer(this::handleTranslationCreate);
    }

    @AfterEach
    void tearDown() {
        if (this.translationServiceMock != null) {
            this.translationServiceMock.close();
        }
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("createBounty populates active state and updates displays")
    void createBountyPopulatesStateAndUpdatesDisplay() {
        final PlayerMock targetPlayer = this.server.addPlayer("Target");
        final Player commissioner = this.server.addPlayer("Commissioner");
        final RDQPlayer targetEntity = new RDQPlayer(targetPlayer.getUniqueId(), targetPlayer.getName());

        final RBounty bounty = mock(RBounty.class);
        when(bounty.getPlayer()).thenReturn(targetEntity);
        when(bounty.getRewardItems()).thenReturn(Set.of());
        when(this.bountyService.createBounty(eq(targetEntity), eq(commissioner), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(bounty));

        final BountyManager manager = createManagerWithFallback();

        manager.createBounty(targetEntity, commissioner, Set.of(), Map.of());

        assertTrue(manager.hasActiveBounty(targetPlayer.getUniqueId()), "Target must have an active bounty");

        final Map<UUID, Map<UUID, Double>> damageTracker = extractDamageTracker(manager);
        assertThat(damageTracker).containsKey(targetPlayer.getUniqueId());

        verify(this.platformAPI, atLeastOnce()).setDisplayName(eq(targetPlayer), any(Component.class));

        final List<TranslationInvocation> displayTranslations = findTranslations("bounty.display.player_list_name");
        assertThat(displayTranslations).isNotEmpty();
    }

    @Test
    @DisplayName("handleBountyKill awards bounty to top damager when claim mode is MOST_DAMAGE")
    void handleBountyKillRewardsTopDamager() {
        final PlayerMock targetPlayer = this.server.addPlayer("Target");
        final PlayerMock topDamager = this.server.addPlayer("TopDamager");
        final PlayerMock otherDamager = this.server.addPlayer("OtherDamager");
        final Player commissioner = this.server.addPlayer("Commissioner");

        final RDQPlayer targetEntity = new RDQPlayer(targetPlayer.getUniqueId(), targetPlayer.getName());

        final RBounty bounty = mock(RBounty.class);
        when(bounty.getPlayer()).thenReturn(targetEntity);
        when(bounty.getRewardItems()).thenReturn(Set.of());
        when(bounty.getId()).thenReturn(42L);
        targetEntity.setBounty(bounty);

        when(this.bountyService.createBounty(eq(targetEntity), eq(commissioner), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(bounty));
        when(this.playerRepository.updateAsync(targetEntity)).thenReturn(CompletableFuture.completedFuture(targetEntity));
        when(this.bountyService.deleteBounty(42L)).thenReturn(CompletableFuture.completedFuture(Boolean.TRUE));

        final BountyManager manager = createManagerWithFallback();
        setClaimMode(manager, EBountyClaimMode.MOST_DAMAGE);

        manager.createBounty(targetEntity, commissioner, Set.of(), Map.of());
        manager.trackDamage(targetPlayer.getUniqueId(), topDamager.getUniqueId(), 15.0);
        manager.trackDamage(targetPlayer.getUniqueId(), otherDamager.getUniqueId(), 5.0);

        resetTranslations();

        manager.handleBountyKill(targetPlayer);

        verify(this.playerRepository).updateAsync(targetEntity);
        verify(this.bountyService).deleteBounty(42L);

        assertThat(manager.hasActiveBounty(targetPlayer.getUniqueId())).isFalse();

        final Map<UUID, Map<UUID, Double>> damageTracker = extractDamageTracker(manager);
        assertThat(damageTracker).doesNotContainKey(targetPlayer.getUniqueId());

        final List<TranslationInvocation> claimTranslations = findTranslations("bounty.claimed.broadcast");
        assertThat(claimTranslations).hasSize(this.server.getOnlinePlayers().size());
        assertThat(claimTranslations)
                .allSatisfy(call -> {
                    assertThat(call.sendCount).isEqualTo(1);
                    assertThat(call.placeholderMaps)
                            .anySatisfy(map -> assertThat(map.get("receiver_name")).isEqualTo(topDamager.getName()));
                });
    }

    @Test
    @DisplayName("handleBountyKill respects LAST_HIT mode when damage exists")
    void handleBountyKillRespectsLastHitMode() {
        final PlayerMock targetPlayer = this.server.addPlayer("Target");
        final PlayerMock lastHitter = this.server.addPlayer("LastHit");
        final Player commissioner = this.server.addPlayer("Commissioner");

        final RDQPlayer targetEntity = new RDQPlayer(targetPlayer.getUniqueId(), targetPlayer.getName());

        final RBounty bounty = mock(RBounty.class);
        when(bounty.getPlayer()).thenReturn(targetEntity);
        when(bounty.getRewardItems()).thenReturn(Set.of());
        when(bounty.getId()).thenReturn(7L);
        targetEntity.setBounty(bounty);

        when(this.bountyService.createBounty(eq(targetEntity), eq(commissioner), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(bounty));
        when(this.playerRepository.updateAsync(targetEntity)).thenReturn(CompletableFuture.completedFuture(targetEntity));
        when(this.bountyService.deleteBounty(7L)).thenReturn(CompletableFuture.completedFuture(Boolean.TRUE));

        final BountyManager manager = createManagerWithFallback();
        setClaimMode(manager, EBountyClaimMode.LAST_HIT);

        manager.createBounty(targetEntity, commissioner, Set.of(), Map.of());
        manager.trackDamage(targetPlayer.getUniqueId(), lastHitter.getUniqueId(), 20.0);

        resetTranslations();

        manager.handleBountyKill(targetPlayer);

        verify(this.playerRepository).updateAsync(targetEntity);
        verify(this.bountyService).deleteBounty(7L);

        final List<TranslationInvocation> claimTranslations = findTranslations("bounty.claimed.broadcast");
        assertThat(claimTranslations).hasSize(this.server.getOnlinePlayers().size());
        assertThat(claimTranslations)
                .allSatisfy(call -> assertThat(call.placeholderMaps)
                        .anySatisfy(map -> assertThat(map.get("receiver_name")).isEqualTo(lastHitter.getName())));
    }

    @Test
    @DisplayName("loadConfig falls back to LAST_HIT when configuration fails")
    void loadConfigFallsBackToLastHit() {
        this.translationServiceMock.close();
        this.translationServiceMock = null;

        try (MockedConstruction<ConfigManager> ignoredManager = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> ignoredKeeper = mockConstruction(ConfigKeeper.class,
                     (mock, context) -> { throw new IllegalStateException("boom"); })) {
            final BountyManager manager = new BountyManager(this.rdq, this.bountyService);
            final EBountyClaimMode claimMode = extractClaimMode(manager);
            assertEquals(EBountyClaimMode.LAST_HIT, claimMode, "Fallback claim mode should be LAST_HIT");
        }
    }

    @Test
    @DisplayName("giveRewardItemsToPlayer drops leftovers and notifies player")
    void giveRewardItemsDropsLeftovers() {
        final PlayerMock player = this.server.addPlayer("Collector");
        final ItemStack filler = new ItemStack(Material.STONE, 64);
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, filler.clone());
        }

        final ItemStack diamondStack = new ItemStack(Material.DIAMOND, 64);
        final RewardItem rewardItem = new RewardItem(diamondStack);
        rewardItem.setAmount(64);

        final BountyManager manager = createManagerWithFallback();

        resetTranslations();

        manager.giveRewardItemsToPlayer(player, Set.of(rewardItem));

        final List<Item> droppedItems = player.getWorld().getEntitiesByClass(Item.class);
        assertThat(droppedItems).hasSize(1);
        assertThat(droppedItems.getFirst().getItemStack().getType()).isEqualTo(Material.DIAMOND);
        assertThat(droppedItems.getFirst().getItemStack().getAmount()).isEqualTo(64);

        final List<TranslationInvocation> leftovers = findTranslations("bounty_reward_ui.left_overs");
        assertThat(leftovers).hasSize(1);
        assertThat(leftovers.getFirst().prefixUsed).isTrue();
        assertThat(leftovers.getFirst().sendCount).isEqualTo(1);
    }

    private BountyManager createManagerWithFallback() {
        try (MockedConstruction<ConfigManager> ignoredManager = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> ignoredKeeper = mockConstruction(ConfigKeeper.class,
                     (mock, context) -> { throw new IllegalStateException("boom"); })) {
            return new BountyManager(this.rdq, this.bountyService);
        }
    }

    private void resetTranslations() {
        this.translationInvocations.clear();
        this.translationServiceMock.close();
        this.translationServiceMock = mockStatic(TranslationService.class);
        this.translationServiceMock.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                .thenAnswer(this::handleTranslationCreate);
    }

    private TranslationService handleTranslationCreate(final InvocationOnMock invocation) {
        final TranslationKey key = invocation.getArgument(0);
        final Player player = invocation.getArgument(1);
        final TranslationInvocation call = new TranslationInvocation(key, player);
        this.translationInvocations.add(call);

        final TranslationService service = mock(TranslationService.class);
        when(service.withAll(any(Map.class))).thenAnswer(storePlaceholders(call));
        when(service.withPrefix()).thenAnswer(inv -> {
            call.prefixUsed = true;
            return service;
        });
        when(service.withPrefix(any(TranslationKey.class))).thenAnswer(inv -> {
            call.prefixUsed = true;
            return service;
        });
        when(service.build()).thenReturn(call.message);
        doAnswer(inv -> {
            call.sendCount++;
            return null;
        }).when(service).send();
        doAnswer(inv -> {
            call.sendTitleCount++;
            return null;
        }).when(service).sendTitle();
        return service;
    }

    private Answer<TranslationService> storePlaceholders(final TranslationInvocation call) {
        return invocation -> {
            final Map<String, Object> placeholders = new HashMap<>();
            final Map<String, ?> provided = invocation.getArgument(0);
            placeholders.putAll(provided);
            call.placeholderMaps.add(placeholders);
            return (TranslationService) invocation.getMock();
        };
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<UUID, Double>> extractDamageTracker(final BountyManager manager) {
        try {
            final Field field = BountyManager.class.getDeclaredField("damageTracker");
            field.setAccessible(true);
            return (Map<UUID, Map<UUID, Double>>) field.get(manager);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to extract damage tracker", exception);
        }
    }

    private EBountyClaimMode extractClaimMode(final BountyManager manager) {
        try {
            final Field field = BountyManager.class.getDeclaredField("claimMode");
            field.setAccessible(true);
            return (EBountyClaimMode) field.get(manager);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to extract claim mode", exception);
        }
    }

    private void setClaimMode(final BountyManager manager, final EBountyClaimMode mode) {
        try {
            final Field field = BountyManager.class.getDeclaredField("claimMode");
            field.setAccessible(true);
            field.set(manager, mode);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set claim mode", exception);
        }
    }

    private List<TranslationInvocation> findTranslations(final String key) {
        return this.translationInvocations.stream()
                .filter(call -> call.key.key().equals(key))
                .toList();
    }

    private static final class TranslationInvocation {
        private final TranslationKey key;
        private final Player player;
        private final List<Map<String, Object>> placeholderMaps = new ArrayList<>();
        private final TranslatedMessage message;
        private boolean prefixUsed;
        private int sendCount;
        private int sendTitleCount;

        private TranslationInvocation(final TranslationKey key, final Player player) {
            this.key = key;
            this.player = player;
            this.message = new TranslatedMessage(Component.text("component-" + key.key()), key);
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
            return true;
        }

        @Override
        public void execute(final Runnable command) {
            if (this.shutdown) {
                throw new RejectedExecutionException("Executor shut down");
            }
            command.run();
        }
    }
}
