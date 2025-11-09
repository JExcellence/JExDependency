package com.raindropcentral.rdq.manager.bounty;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rdq.config.bounty.BountySection;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FreeBountyManagerTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private RPlatform platform;
    private ISchedulerAdapter scheduler;
    private PlatformAPI platformAPI;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin();
        this.scheduler = mock(ISchedulerAdapter.class);
        this.platformAPI = mock(PlatformAPI.class);
        this.platform = mock(RPlatform.class);

        when(this.platform.getScheduler()).thenReturn(this.scheduler);
        when(this.platform.getPlatformAPI()).thenReturn(this.platformAPI);
        doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(this.scheduler).runSync(any());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void constructorLoadsClaimModeFromConfig() {
        final BountySection section = mock(BountySection.class);
        when(section.getClaimMode()).thenReturn(EBountyClaimMode.MOST_DAMAGE);

        final FreeBountyManager manager;
        try (MockedConstruction<ConfigManager> ignored = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keeperConstruction = mockConstruction(ConfigKeeper.class,
                     (mock, context) -> injectRootSection(mock, section))) {
            manager = new FreeBountyManager(this.plugin, this.platform);
        }

        assertEquals(EBountyClaimMode.MOST_DAMAGE, readClaimMode(manager));
    }

    @Test
    void constructorFallsBackToLastHitWhenConfigFails() {
        final FreeBountyManager manager;
        try (MockedConstruction<ConfigManager> ignored = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keeperConstruction = mockConstruction(ConfigKeeper.class,
                     (mock, context) -> {
                         throw new IllegalStateException("boom");
                     })) {
            manager = new FreeBountyManager(this.plugin, this.platform);
        }

        assertEquals(EBountyClaimMode.LAST_HIT, readClaimMode(manager));
    }

    @Test
    void createBountySendsPremiumFeatureMessage() {
        final FreeBountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);
        final Player commissioner = this.server.addPlayer("Commissioner");
        final RDQPlayer targetPlayer = new RDQPlayer(UUID.randomUUID(), "Target");

        final TranslationService builder = mock(TranslationService.class);
        when(builder.withPrefix()).thenReturn(builder);
        doNothing().when(builder).send();

        try (MockedStatic<TranslationService> translation = mockStatic(TranslationService.class)) {
            translation.when(() -> TranslationService.create(
                    argThat(key -> key.key().equals("general.premium-feature")),
                    org.mockito.ArgumentMatchers.eq(commissioner)
            )).thenReturn(builder);

            manager.createBounty(targetPlayer, commissioner, Collections.emptySet(), Collections.emptyMap());

            translation.verify(() -> TranslationService.create(
                    argThat(key -> key.key().equals("general.premium-feature")),
                    org.mockito.ArgumentMatchers.eq(commissioner)
            ));
            verify(builder).withPrefix();
            verify(builder).send();
            verifyNoMoreInteractions(builder);
        }
    }

    @Test
    void handleBountyKillRewardsLastHitAttacker() {
        final FreeBountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);
        final Player target = this.server.addPlayer("Target");
        final Player lastHit = this.server.addPlayer("LastHit");
        this.server.addPlayer("Other");

        final RBounty bounty = new RBounty(new RDQPlayer(target.getUniqueId(), target.getName()), target);
        bounty.setRewardItems(new HashSet<>(Set.of(new RewardItem(new ItemStack(Material.DIAMOND)))));

        activeBounties(manager).put(target.getUniqueId(), bounty);
        lastHitTracker(manager).put(target.getUniqueId(), lastHit.getUniqueId());

        manager.handleBountyKill(target);

        assertFalse(manager.hasActiveBounty(target.getUniqueId()));
        assertNull(manager.getBounty(target.getUniqueId()));
        assertTrue(lastHit.getInventory().contains(Material.DIAMOND));
        verify(this.platformAPI).setDisplayName(org.mockito.ArgumentMatchers.eq(target),
                org.mockito.ArgumentMatchers.any(Component.class));
    }

    @Test
    void handleBountyKillRewardsTopDamagerWhenConfigured() {
        final FreeBountyManager manager = createManagerWithClaimMode(EBountyClaimMode.MOST_DAMAGE);
        final Player target = this.server.addPlayer("Target");
        final Player lastHit = this.server.addPlayer("LastHit");
        final Player topDamager = this.server.addPlayer("TopDamager");

        final RBounty bounty = new RBounty(new RDQPlayer(target.getUniqueId(), target.getName()), target);
        bounty.setRewardItems(new HashSet<>(Set.of(new RewardItem(new ItemStack(Material.EMERALD)))));

        activeBounties(manager).put(target.getUniqueId(), bounty);
        lastHitTracker(manager).put(target.getUniqueId(), lastHit.getUniqueId());
        damageTracker(manager).put(target.getUniqueId(), new ConcurrentHashMap<>(Map.of(
                lastHit.getUniqueId(), 4.0d,
                topDamager.getUniqueId(), 10.0d
        )));

        manager.handleBountyKill(target);

        assertFalse(manager.hasActiveBounty(target.getUniqueId()));
        assertNull(manager.getBounty(target.getUniqueId()));
        assertTrue(topDamager.getInventory().contains(Material.EMERALD));
        verify(this.platformAPI).setDisplayName(org.mockito.ArgumentMatchers.eq(target),
                org.mockito.ArgumentMatchers.any(Component.class));
    }

    @Test
    void updateBountyPlayerDisplaySchedulesOffThreadAndUsesTranslation() {
        final FreeBountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);
        final Player player = this.server.addPlayer("TrackedPlayer");

        activeBounties(manager).put(player.getUniqueId(), new RBounty(new RDQPlayer(player.getUniqueId(), player.getName()), player));

        final TranslationService builder = mock(TranslationService.class);
        when(builder.withAll(org.mockito.ArgumentMatchers.anyMap())).thenReturn(builder);
        final TranslatedMessage message = new TranslatedMessage(Component.text("BountyDisplay"), TranslationKey.of("test"));
        when(builder.build()).thenReturn(message);

        try (MockedStatic<TranslationService> translation = mockStatic(TranslationService.class)) {
            translation.when(() -> TranslationService.create(
                    argThat(key -> key.key().equals("bounty.display.player_list_name")),
                    org.mockito.ArgumentMatchers.eq(player)
            )).thenReturn(builder);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
                bukkit.when(Bukkit::isPrimaryThread).thenReturn(false, true);
                manager.updateBountyPlayerDisplay(player.getUniqueId());
            }
        }

        verify(this.scheduler).runSync(any());
        final ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(this.platformAPI).setDisplayName(org.mockito.ArgumentMatchers.eq(player), componentCaptor.capture());
        assertEquals("BountyDisplay", PlainTextComponentSerializer.plainText().serialize(componentCaptor.getValue()));
    }

    @Test
    void giveRewardItemsToPlayerDropsLeftoversAndTranslates() {
        final FreeBountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);
        final Player player = this.server.addPlayer("RewardedPlayer");

        final ItemStack filler = new ItemStack(Material.STONE, 64);
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, filler);
        }

        final RewardItem rewardItem = new RewardItem(new ItemStack(Material.DIAMOND));
        final TranslationService builder = mock(TranslationService.class);
        when(builder.withPrefix()).thenReturn(builder);
        doNothing().when(builder).send();

        try (MockedStatic<TranslationService> translation = mockStatic(TranslationService.class)) {
            translation.when(() -> TranslationService.create(
                    argThat(key -> key.key().equals("bounty_reward_ui.left_overs")),
                    org.mockito.ArgumentMatchers.eq(player)
            )).thenReturn(builder);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
                bukkit.when(Bukkit::isPrimaryThread).thenReturn(false, true);
                manager.giveRewardItemsToPlayer(player, Set.of(rewardItem));
            }

            translation.verify(() -> TranslationService.create(
                    argThat(key -> key.key().equals("bounty_reward_ui.left_overs")),
                    org.mockito.ArgumentMatchers.eq(player)
            ));
        }

        verify(this.scheduler).runSync(any());
        verify(builder).withPrefix();
        verify(builder).send();
        assertTrue(player.getWorld().getEntities().stream()
                .anyMatch(entity -> entity instanceof org.bukkit.entity.Item));
    }

    @Test
    void removeBountyClearsTrackingAndResetsDisplay() {
        final FreeBountyManager manager = createManagerWithClaimMode(EBountyClaimMode.LAST_HIT);
        final Player player = this.server.addPlayer("BountiedPlayer");

        activeBounties(manager).put(player.getUniqueId(), new RBounty(new RDQPlayer(player.getUniqueId(), player.getName()), player));
        damageTracker(manager).put(player.getUniqueId(), new ConcurrentHashMap<>());
        lastHitTracker(manager).put(player.getUniqueId(), UUID.randomUUID());

        manager.removeBounty(player.getUniqueId());

        assertFalse(manager.hasActiveBounty(player.getUniqueId()));
        assertNull(manager.getBounty(player.getUniqueId()));
        assertFalse(damageTracker(manager).containsKey(player.getUniqueId()));
        assertFalse(lastHitTracker(manager).containsKey(player.getUniqueId()));
        verify(this.platformAPI).setDisplayName(org.mockito.ArgumentMatchers.eq(player),
                org.mockito.ArgumentMatchers.any(Component.class));
    }

    private FreeBountyManager createManagerWithClaimMode(final EBountyClaimMode claimMode) {
        final BountySection section = mock(BountySection.class);
        when(section.getClaimMode()).thenReturn(claimMode);

        try (MockedConstruction<ConfigManager> ignored = mockConstruction(ConfigManager.class);
             MockedConstruction<ConfigKeeper> keeperConstruction = mockConstruction(ConfigKeeper.class,
                     (mock, context) -> injectRootSection(mock, section))) {
            return new FreeBountyManager(this.plugin, this.platform);
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

    private EBountyClaimMode readClaimMode(final FreeBountyManager manager) {
        try {
            final Field field = FreeBountyManager.class.getDeclaredField("claimMode");
            field.setAccessible(true);
            return (EBountyClaimMode) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, RBounty> activeBounties(final FreeBountyManager manager) {
        try {
            final Field field = FreeBountyManager.class.getDeclaredField("activeBounties");
            field.setAccessible(true);
            return (Map<UUID, RBounty>) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Map<UUID, Double>> damageTracker(final FreeBountyManager manager) {
        try {
            final Field field = FreeBountyManager.class.getDeclaredField("damageTracker");
            field.setAccessible(true);
            return (Map<UUID, Map<UUID, Double>>) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, UUID> lastHitTracker(final FreeBountyManager manager) {
        try {
            final Field field = FreeBountyManager.class.getDeclaredField("lastHitTracker");
            field.setAccessible(true);
            return (Map<UUID, UUID>) field.get(manager);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new AssertionError(exception);
        }
    }
}
