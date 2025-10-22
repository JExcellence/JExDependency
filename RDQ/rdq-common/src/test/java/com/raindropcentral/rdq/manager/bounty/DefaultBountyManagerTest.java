package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.api.PlatformAPI;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultBountyManagerTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private RPlatform platform;
    private ISchedulerAdapter scheduler;
    private PlatformAPI platformAPI;
    private RBountyRepository bountyRepository;
    private RDQPlayerRepository playerRepository;
    private Executor executor;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();

        this.platform = mock(RPlatform.class);
        this.scheduler = mock(ISchedulerAdapter.class);
        this.platformAPI = mock(PlatformAPI.class);
        this.bountyRepository = mock(RBountyRepository.class);
        this.playerRepository = mock(RDQPlayerRepository.class);
        this.executor = Runnable::run;

        when(this.platform.getScheduler()).thenReturn(this.scheduler);
        when(this.platform.getPlatformAPI()).thenReturn(this.platformAPI);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(this.scheduler).runSync(any());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createBountyTracksStateAndDamage() throws Exception {
        PlayerMock target = this.server.addPlayer("Target");
        PlayerMock commissioner = this.server.addPlayer("Commissioner");
        RDQPlayer rdqTarget = new RDQPlayer(target.getUniqueId(), target.getName());

        Set<RewardItem> rewards = Set.of(new RewardItem(new ItemStack(Material.DIAMOND, 3)));
        Map<String, Double> currencies = Map.of("coins", 25.0d);
        RBounty persistedBounty = new RBounty(rdqTarget, commissioner, rewards, currencies);

        when(this.bountyRepository.createAsync(any(RBounty.class))).thenReturn(CompletableFuture.completedFuture(persistedBounty));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class); MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            prepareOnlinePlayersStub(bukkit, List.of(target, commissioner));
            bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

            translations.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                    .thenAnswer(invocation -> createTranslationBuilder((TranslationKey) invocation.getArgument(0), (Player) invocation.getArgument(1)));

            DefaultBountyManager manager = new DefaultBountyManager(this.plugin, this.platform, this.executor, this.bountyRepository, this.playerRepository);

            manager.createBounty(rdqTarget, commissioner, rewards, currencies);

            assertTrue(manager.hasActiveBounty(target.getUniqueId()));
            assertSame(persistedBounty, manager.getBounty(target.getUniqueId()));

            Map<UUID, Map<UUID, Double>> damageTracker = getDamageTracker(manager);
            assertTrue(damageTracker.containsKey(target.getUniqueId()));
            assertTrue(damageTracker.get(target.getUniqueId()).isEmpty());

            verify(this.scheduler, atLeastOnce()).runSync(any());

            ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);
            verify(this.platformAPI).setDisplayName(eq(target), componentCaptor.capture());
            assertTrue(componentCaptor.getValue().toString().contains("bounty.display.player_list_name"));

            UUID attackerId = UUID.randomUUID();
            manager.trackDamage(target.getUniqueId(), attackerId, 0.0d);
            Map<UUID, UUID> lastHits = getLastHitTracker(manager);
            assertNull(lastHits.get(target.getUniqueId()));
            assertFalse(damageTracker.get(target.getUniqueId()).containsKey(attackerId));

            manager.trackDamage(target.getUniqueId(), attackerId, 4.0d);
            assertEquals(attackerId, lastHits.get(target.getUniqueId()));
            assertEquals(4.0d, damageTracker.get(target.getUniqueId()).get(attackerId));
        }
    }

    @Test
    void handleBountyKillRewardsWinnerAndClearsState() throws Exception {
        PlayerMock target = this.server.addPlayer("Target");
        PlayerMock commissioner = this.server.addPlayer("Commissioner");
        PlayerMock killer = this.server.addPlayer("Killer");
        PlayerMock helper = this.server.addPlayer("Helper");

        RDQPlayer rdqTarget = new RDQPlayer(target.getUniqueId(), target.getName());
        Set<RewardItem> rewards = Set.of(new RewardItem(new ItemStack(Material.EMERALD, 5)));
        Map<String, Double> currencies = Map.of("coins", 150.0d);
        RBounty persistedBounty = new RBounty(rdqTarget, commissioner, rewards, currencies);

        when(this.bountyRepository.createAsync(any(RBounty.class))).thenReturn(CompletableFuture.completedFuture(persistedBounty));
        when(this.playerRepository.updateAsync(any(RDQPlayer.class))).thenReturn(CompletableFuture.completedFuture(rdqTarget));
        when(this.bountyRepository.deleteAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class); MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            prepareOnlinePlayersStub(bukkit, List.of(target, commissioner, killer, helper));
            bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);
            bukkit.when(() -> Bukkit.getPlayer(killer.getUniqueId())).thenReturn(killer);
            bukkit.when(() -> Bukkit.getOfflinePlayer(killer.getUniqueId())).thenReturn(killer);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

            translations.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                    .thenAnswer(invocation -> createTranslationBuilder((TranslationKey) invocation.getArgument(0), (Player) invocation.getArgument(1)));

            DefaultBountyManager manager = new DefaultBountyManager(this.plugin, this.platform, this.executor, this.bountyRepository, this.playerRepository);
            manager.createBounty(rdqTarget, commissioner, rewards, currencies);

            UUID targetId = target.getUniqueId();
            manager.trackDamage(targetId, helper.getUniqueId(), 3.0d);
            manager.trackDamage(targetId, killer.getUniqueId(), 6.0d);
            manager.trackDamage(targetId, helper.getUniqueId(), 1.0d);
            manager.trackDamage(targetId, killer.getUniqueId(), 2.0d);

            manager.handleBountyKill(target);

            assertFalse(manager.hasActiveBounty(targetId));
            assertNull(manager.getBounty(targetId));
            assertFalse(getDamageTracker(manager).containsKey(targetId));
            assertNull(getLastHitTracker(manager).get(targetId));

            assertEquals(5, killer.getInventory().all(Material.EMERALD).values().stream().mapToInt(ItemStack::getAmount).sum());

            verify(this.playerRepository).updateAsync(rdqTarget);
            verify(this.bountyRepository).deleteAsync(any());
        }
    }

    @Test
    void giveRewardItemsRedispatchesAndDropsLeftovers() throws Exception {
        PlayerMock recipient = this.server.addPlayer("Recipient");
        fillInventory(recipient);

        Set<RewardItem> rewards = Set.of(new RewardItem(new ItemStack(Material.GOLD_INGOT, 4)));

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class); MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            prepareOnlinePlayersStub(bukkit, List.of(recipient));
            bukkit.when(() -> Bukkit.getPlayer(recipient.getUniqueId())).thenReturn(recipient);

            translations.when(() -> TranslationService.create(any(TranslationKey.class), any(Player.class)))
                    .thenAnswer(invocation -> createTranslationBuilder((TranslationKey) invocation.getArgument(0), (Player) invocation.getArgument(1)));

            DefaultBountyManager manager = new DefaultBountyManager(this.plugin, this.platform, this.executor, this.bountyRepository, this.playerRepository);

            bukkit.when(Bukkit::isPrimaryThread).thenAnswer(new Answer<Boolean>() {
                private boolean onPrimary = false;

                @Override
                public Boolean answer(org.mockito.invocation.InvocationOnMock invocation) {
                    if (!this.onPrimary) {
                        this.onPrimary = true;
                        return false;
                    }
                    return true;
                }
            });

            manager.giveRewardItemsToPlayer(recipient, rewards);

            verify(this.scheduler).runSync(any());
            assertFalse(recipient.getWorld().getEntitiesByClass(Item.class).isEmpty());

            translations.verify(() -> TranslationService.create(TranslationKey.of("bounty_reward_ui.left_overs"), recipient));
        }
    }

    private void prepareOnlinePlayersStub(MockedStatic<Bukkit> bukkit, Collection<? extends Player> players) {
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(new ArrayList<>(players));
    }

    private TranslationService createTranslationBuilder(TranslationKey key, Player player) {
        TranslationService builder = mock(TranslationService.class);
        TranslatedMessage message = mock(TranslatedMessage.class);
        when(message.component()).thenReturn(Component.text(key.key() + ":" + player.getName()));
        when(builder.withAll(anyMap())).thenReturn(builder);
        when(builder.withPrefix()).thenReturn(builder);
        when(builder.withPrefix(any(TranslationKey.class))).thenReturn(builder);
        when(builder.build()).thenReturn(message);
        doNothing().when(builder).send();
        doNothing().when(builder).sendTitle();
        doNothing().when(builder).sendActionBar();
        return builder;
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<UUID, Double>> getDamageTracker(DefaultBountyManager manager) throws Exception {
        Field field = DefaultBountyManager.class.getDeclaredField("damageTracker");
        field.setAccessible(true);
        return (Map<UUID, Map<UUID, Double>>) field.get(manager);
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, UUID> getLastHitTracker(DefaultBountyManager manager) throws Exception {
        Field field = DefaultBountyManager.class.getDeclaredField("lastHitTracker");
        field.setAccessible(true);
        return (Map<UUID, UUID>) field.get(manager);
    }

    private void fillInventory(PlayerMock player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.STONE));
        }
    }
}
