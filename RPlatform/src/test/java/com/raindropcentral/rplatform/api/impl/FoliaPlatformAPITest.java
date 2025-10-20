package com.raindropcentral.rplatform.api.impl;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class FoliaPlatformAPITest {

    private ServerMock server;
    private JavaPlugin plugin;
    private FoliaPlatformAPI api;
    private SchedulerStub scheduler;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(FoliaTestPlugin.class);
        scheduler = new SchedulerStub();

        try (MockedStatic<ISchedulerAdapter> mocked = Mockito.mockStatic(ISchedulerAdapter.class)) {
            mocked.when(() -> ISchedulerAdapter.create(plugin, PlatformType.FOLIA)).thenReturn(scheduler);
            api = new FoliaPlatformAPI(plugin);
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void metadataReportsFoliaCapabilities() {
        assertEquals(PlatformType.FOLIA, api.getType());
        assertTrue(api.supportsAdventure());
        assertTrue(api.supportsFolia());
        assertEquals(Bukkit.getVersion(), api.getServerVersion());
    }

    @Test
    void closeIsIdempotentNoOp() {
        assertDoesNotThrow(() -> {
            api.close();
            api.close();
        });
    }

    @Test
    void messagingDelegatesToPlayer() {
        Player player = Mockito.mock(Player.class);
        Component message = Component.text("folia");
        api.sendMessage(player, message);
        Mockito.verify(player).sendMessage(message);

        List<Component> batch = List.of(Component.text("one"), Component.text("two"));
        api.sendMessages(player, batch);
        Mockito.verify(player).sendMessage(batch.get(0));
        Mockito.verify(player).sendMessage(batch.get(1));

        Component actionBar = Component.text("action");
        api.sendActionBar(player, actionBar);
        Mockito.verify(player).sendActionBar(actionBar);
    }

    @Test
    void sendTitleBuildsAdventureTitle() {
        Player player = Mockito.mock(Player.class);
        Component title = Component.text("Main");
        Component subtitle = Component.text("Sub");

        api.sendTitle(player, title, subtitle, 5, 40, 10);

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        Mockito.verify(player).showTitle(captor.capture());
        Title built = captor.getValue();
        assertEquals(title, built.title());
        assertEquals(subtitle, built.subtitle());
        assertNotNull(built.times());
        assertEquals(Duration.ofMillis(5 * 50L), built.times().fadeIn());
        assertEquals(Duration.ofMillis(40 * 50L), built.times().stay());
        assertEquals(Duration.ofMillis(10 * 50L), built.times().fadeOut());
    }

    @Test
    void sendTitleReplacesNullSubtitleWithEmptyComponent() {
        Player player = Mockito.mock(Player.class);
        Component title = Component.text("Title");

        api.sendTitle(player, title, null, 0, 10, 0);

        ArgumentCaptor<Title> captor = ArgumentCaptor.forClass(Title.class);
        Mockito.verify(player).showTitle(captor.capture());
        Title built = captor.getValue();
        assertEquals(Component.empty(), built.subtitle());
    }

    @Test
    void displayNameDelegatesToPlayer() {
        Player player = Mockito.mock(Player.class);
        Component displayName = Component.text("FoliaUser");
        Mockito.when(player.displayName()).thenReturn(displayName);

        assertEquals(displayName, api.getDisplayName(player));

        Component newName = Component.text("Updated");
        api.setDisplayName(player, newName);
        Mockito.verify(player).displayName(newName);
    }

    @Test
    void itemDisplayNameAndLoreMutationsAreReflected() {
        ItemStack stack = new ItemStack(Material.DIRT);
        assertNull(api.getItemDisplayName(stack));

        Component displayName = Component.text("Fancy Dirt");
        ItemStack mutated = api.setItemDisplayName(stack, displayName);
        assertSame(stack, mutated);
        assertEquals(displayName, api.getItemDisplayName(stack));

        api.setItemDisplayName(stack, null);
        assertNull(api.getItemDisplayName(stack));

        List<Component> lore = List.of(Component.text("Line 1"), Component.text("Line 2"));
        ItemStack loreMutated = api.setItemLore(stack, lore);
        assertSame(stack, loreMutated);
        assertEquals(lore, api.getItemLore(stack));

        ItemStack fresh = new ItemStack(Material.STONE);
        assertTrue(api.getItemLore(fresh).isEmpty());
    }

    @Test
    void createPlayerHeadCopiesPlayerProfile() {
        Player player = Mockito.mock(Player.class);
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "FoliaMock");
        Mockito.when(player.getPlayerProfile()).thenReturn(profile);

        ItemStack head = api.createPlayerHead(player);
        assertEquals(Material.PLAYER_HEAD, head.getType());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertNotNull(meta.getPlayerProfile());
        assertEquals(profile.getId(), meta.getPlayerProfile().getId());
    }

    @Test
    void createPlayerHeadHandlesNullInputs() {
        ItemStack head = api.createPlayerHead((Player) null);
        assertEquals(Material.PLAYER_HEAD, head.getType());
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertFalse(meta.hasOwner());
        assertNull(meta.getOwningPlayer());

        ItemStack offlineHead = api.createPlayerHead((OfflinePlayer) null);
        SkullMeta offlineMeta = (SkullMeta) offlineHead.getItemMeta();
        assertNotNull(offlineMeta);
        assertFalse(offlineMeta.hasOwner());
        assertNull(offlineMeta.getOwningPlayer());
    }

    @Test
    void createPlayerHeadUsesOfflineProfile() {
        UUID id = UUID.randomUUID();
        OfflinePlayer offlinePlayer = server.getOfflinePlayer(id);

        ItemStack head = api.createPlayerHead(offlinePlayer);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertNotNull(meta.getOwningPlayer());
        assertEquals(offlinePlayer.getUniqueId(), meta.getOwningPlayer().getUniqueId());
    }

    @Test
    void createCustomHeadAppliesTextureAndOptionalDisplayName() {
        UUID uuid = UUID.randomUUID();
        String texture = "base64-texture";

        ItemStack head = api.createCustomHead(uuid, texture);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        PlayerProfile profile = meta.getPlayerProfile();
        assertNotNull(profile);
        assertEquals(uuid, profile.getId());
        assertEquals(texture, findTexture(profile.getProperties()));
        assertNull(meta.displayName());

        Component displayName = Component.text("Custom");
        ItemStack named = api.createCustomHead(uuid, texture, displayName);
        SkullMeta namedMeta = (SkullMeta) named.getItemMeta();
        assertNotNull(namedMeta);
        assertEquals(displayName, namedMeta.displayName());
        assertEquals(texture, findTexture(namedMeta.getPlayerProfile().getProperties()));
    }

    @Test
    void applyCustomTextureMutatesExistingHeadAndIgnoresNonSkulls() {
        UUID uuid = UUID.randomUUID();
        String texture = "updated-texture";

        ItemStack skull = api.createPlayerHead((Player) null);
        ItemStack mutated = api.applyCustomTexture(skull, uuid, texture);
        assertSame(skull, mutated);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        assertNotNull(meta);
        assertEquals(uuid, meta.getPlayerProfile().getId());
        assertEquals(texture, findTexture(meta.getPlayerProfile().getProperties()));

        ItemStack nonSkull = new ItemStack(Material.STONE);
        assertSame(nonSkull, api.applyCustomTexture(nonSkull, uuid, texture));
    }

    @Test
    void schedulerDelegatesToStub() {
        assertSame(scheduler, api.scheduler());

        Runnable task = Mockito.mock(Runnable.class);
        api.scheduler().runSync(task);
        assertEquals(1, scheduler.getSyncRuns());
        Mockito.verify(task).run();
    }

    private String findTexture(final Collection<ProfileProperty> properties) {
        return properties.stream()
                .filter(property -> "textures".equals(property.getName()))
                .findFirst()
                .map(ProfileProperty::getValue)
                .orElse(null);
    }

    private static final class SchedulerStub implements ISchedulerAdapter {

        private int syncRuns;

        @Override
        public void runSync(final @NotNull Runnable task) {
            syncRuns++;
            task.run();
        }

        @Override
        public void runAsync(final @NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runDelayed(final @NotNull Runnable task, final long delayTicks) {
            task.run();
        }

        @Override
        public void runRepeating(final @NotNull Runnable task, final long delayTicks, final long periodTicks) {
            task.run();
        }

        @Override
        public void runAtEntity(final @NotNull org.bukkit.entity.Entity entity, final @NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runAtLocation(final @NotNull org.bukkit.Location location, final @NotNull Runnable task) {
            task.run();
        }

        @Override
        public void runGlobal(final @NotNull Runnable task) {
            task.run();
        }

        @Override
        public @NotNull CompletableFuture<Void> runAsyncFuture(final @NotNull Runnable task) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            try {
                task.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
            return future;
        }

        int getSyncRuns() {
            return syncRuns;
        }
    }

    public static class FoliaTestPlugin extends JavaPlugin {
    }
}
