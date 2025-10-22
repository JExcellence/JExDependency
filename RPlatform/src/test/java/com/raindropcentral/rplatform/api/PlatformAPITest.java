package com.raindropcentral.rplatform.api;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class PlatformAPITest {

    private ServerMock server;
    private PlayerMock player;
    private OfflinePlayer offlinePlayer;
    private RecordingPlatformAPI api;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer("TestPlayer");
        offlinePlayer = server.getOfflinePlayer(UUID.randomUUID());
        api = new RecordingPlatformAPI();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void platformMetadataExposesContract() {
        assertEquals(PlatformType.PAPER, api.getType());
        assertTrue(api.supportsAdventure());
        assertFalse(api.supportsFolia());
        assertNotNull(api.getServerVersion());
        assertFalse(api.getServerVersion().isEmpty());
    }

    @Test
    void closeIsIdempotent() {
        api.close();
        api.close();
        assertEquals(2, api.getCloseInvocations());
    }

    @Test
    void messagingContractsRecordPayloads() {
        Component message = Component.text("hello");
        api.sendMessage(player, message);
        assertEquals(message, api.getLastSingleMessage());

        List<Component> batch = List.of(Component.text("one"), Component.text("two"));
        api.sendMessages(player, batch);
        assertEquals(batch, api.getLastBatchMessages());

        Component actionBar = Component.text("status");
        api.sendActionBar(player, actionBar);
        assertEquals(actionBar, api.getLastActionBar());
    }

    @Test
    void titleContractSupportsNullableSubtitle() {
        Component title = Component.text("Title");
        api.sendTitle(player, title, null, 10, 40, 10);
        RecordingPlatformAPI.TitleInvocation invocation = api.getLastTitleInvocation();
        assertEquals(title, invocation.title());
        assertNull(invocation.subtitle());
        assertEquals(10, invocation.fadeInTicks());
        assertEquals(40, invocation.stayTicks());
        assertEquals(10, invocation.fadeOutTicks());
    }

    @Test
    void displayNameRoundTripUsesAdventureComponents() {
        Component defaultDisplayName = api.getDisplayName(player);
        assertEquals(Component.text(player.getName()), defaultDisplayName);

        Component custom = Component.text("Custom");
        api.setDisplayName(player, custom);
        assertEquals(custom, api.getDisplayName(player));
        assertThrows(NullPointerException.class, () -> api.setDisplayName(player, null));
    }

    @Test
    void itemDisplayNameHandlesNullGracefully() {
        ItemStack item = new ItemStack(Material.STONE);
        assertNull(api.getItemDisplayName(item));

        Component name = Component.text("Item");
        ItemStack mutated = api.setItemDisplayName(item, name);
        assertSame(item, mutated);
        assertEquals(name, api.getItemDisplayName(item));

        api.setItemDisplayName(item, null);
        assertNull(api.getItemDisplayName(item));
    }

    @Test
    void loreContractPreservesOrderingAndImmutability() {
        ItemStack item = new ItemStack(Material.DIRT);
        List<Component> emptyLore = api.getItemLore(item);
        assertTrue(emptyLore.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyLore.add(Component.text("fail")));

        List<Component> lore = List.of(Component.text("line1"), Component.text("line2"));
        ItemStack mutated = api.setItemLore(item, lore);
        assertSame(item, mutated);
        assertEquals(lore, api.getItemLore(item));
    }

    @Test
    void playerHeadFactoriesHandleNullPlayers() {
        ItemStack headWithPlayer = api.createPlayerHead(player);
        assertEquals(Material.PLAYER_HEAD, headWithPlayer.getType());
        SkullMeta withPlayerMeta = (SkullMeta) headWithPlayer.getItemMeta();
        assertEquals(player.getUniqueId(), Objects.requireNonNull(withPlayerMeta.getOwningPlayer()).getUniqueId());

        ItemStack headWithOffline = api.createPlayerHead(offlinePlayer);
        assertEquals(Material.PLAYER_HEAD, headWithOffline.getType());
        SkullMeta offlineMeta = (SkullMeta) headWithOffline.getItemMeta();
        assertEquals(offlinePlayer.getUniqueId(), Objects.requireNonNull(offlineMeta.getOwningPlayer()).getUniqueId());

        ItemStack headWithoutPlayer = api.createPlayerHead((Player) null);
        assertFalse(((SkullMeta) headWithoutPlayer.getItemMeta()).hasOwner());

        ItemStack headWithoutOffline = api.createPlayerHead((OfflinePlayer) null);
        assertFalse(((SkullMeta) headWithoutOffline.getItemMeta()).hasOwner());
    }

    @Test
    void customHeadFactoriesStoreTextureData() {
        UUID uuid = UUID.randomUUID();
        String texture = "texture-data";

        ItemStack head = api.createCustomHead(uuid, texture);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertEquals(texture, meta.getPersistentDataContainer().get(RecordingPlatformAPI.TEXTURE_KEY, PersistentDataType.STRING));
        assertEquals(uuid.toString(), meta.getPersistentDataContainer().get(RecordingPlatformAPI.PROFILE_KEY, PersistentDataType.STRING));
        assertFalse(meta.hasDisplayName());

        Component displayName = Component.text("Skull");
        ItemStack headWithName = api.createCustomHead(uuid, texture, displayName);
        SkullMeta withNameMeta = (SkullMeta) headWithName.getItemMeta();
        assertEquals(displayName, withNameMeta.displayName());
    }

    @Test
    void applyCustomTextureMutatesExistingStack() {
        ItemStack skull = api.createPlayerHead(player);
        ItemStack mutated = api.applyCustomTexture(skull, UUID.randomUUID(), "another-texture");
        assertSame(skull, mutated);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        assertEquals("another-texture", meta.getPersistentDataContainer().get(RecordingPlatformAPI.TEXTURE_KEY, PersistentDataType.STRING));
    }

    @Test
    void schedulerExecutesTasksSynchronouslyForTests() {
        RecordingPlatformAPI.SchedulerStub scheduler = api.getScheduler();
        Runnable syncTask = scheduler::markSyncExecuted;
        api.scheduler().runSync(syncTask);
        assertEquals(1, scheduler.getSyncExecutions());

        Runnable asyncTask = scheduler::markAsyncExecuted;
        api.scheduler().runAsync(asyncTask);
        assertEquals(1, scheduler.getAsyncExecutions());

        api.scheduler().runDelayed(scheduler::markDelayedExecuted, 5L);
        assertEquals(1, scheduler.getDelayedExecutions());

        api.scheduler().runRepeating(scheduler::markRepeatingExecuted, 5L, 10L);
        assertEquals(1, scheduler.getRepeatingExecutions());

        api.scheduler().runAtEntity(player, scheduler::markEntityExecuted);
        assertEquals(1, scheduler.getEntityExecutions());

        api.scheduler().runAtLocation(player.getLocation(), scheduler::markLocationExecuted);
        assertEquals(1, scheduler.getLocationExecutions());

        api.scheduler().runGlobal(scheduler::markGlobalExecuted);
        assertEquals(1, scheduler.getGlobalExecutions());

        CompletableFuture<Void> future = api.scheduler().runAsyncFuture(scheduler::markAsyncFutureExecuted);
        assertEquals(1, scheduler.getAsyncFutureExecutions());
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
        assertDoesNotThrow(future::join);
    }

    private static final class RecordingPlatformAPI implements PlatformAPI {

        private static final NamespacedKey TEXTURE_KEY = NamespacedKey.minecraft("rplatform_texture");
        private static final NamespacedKey PROFILE_KEY = NamespacedKey.minecraft("rplatform_profile_uuid");

        private final Map<UUID, Component> displayNames = new HashMap<>();
        private final SchedulerStub scheduler = new SchedulerStub();
        private Component lastSingleMessage;
        private List<Component> lastBatchMessages = List.of();
        private Component lastActionBar;
        private TitleInvocation lastTitleInvocation;
        private int closeInvocations;

        @Override
        public @NotNull PlatformType getType() {
            return PlatformType.PAPER;
        }

        @Override
        public boolean supportsAdventure() {
            return true;
        }

        @Override
        public boolean supportsFolia() {
            return false;
        }

        @Override
        public void close() {
            closeInvocations++;
        }

        @Override
        public void sendMessage(@NotNull Player player, @NotNull Component message) {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(message, "message");
            lastSingleMessage = message;
        }

        @Override
        public void sendMessages(@NotNull Player player, @NotNull List<Component> messages) {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(messages, "messages");
            lastBatchMessages = List.copyOf(messages);
        }

        @Override
        public void sendActionBar(@NotNull Player player, @NotNull Component message) {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(message, "message");
            lastActionBar = message;
        }

        @Override
        public void sendTitle(@NotNull Player player,
                              @NotNull Component title,
                              @Nullable Component subtitle,
                              int fadeInTicks,
                              int stayTicks,
                              int fadeOutTicks) {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(title, "title");
            if (fadeInTicks < 0 || stayTicks < 0 || fadeOutTicks < 0) {
                throw new IllegalArgumentException("Title timing cannot be negative");
            }
            lastTitleInvocation = new TitleInvocation(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
        }

        @Override
        public @NotNull Component getDisplayName(@NotNull Player player) {
            Objects.requireNonNull(player, "player");
            return displayNames.getOrDefault(player.getUniqueId(), Component.text(player.getName()));
        }

        @Override
        public void setDisplayName(@NotNull Player player, @NotNull Component displayName) {
            Objects.requireNonNull(player, "player");
            displayNames.put(player.getUniqueId(), Objects.requireNonNull(displayName, "displayName"));
        }

        @Override
        public @Nullable Component getItemDisplayName(@NotNull ItemStack itemStack) {
            Objects.requireNonNull(itemStack, "itemStack");
            ItemMeta meta = itemStack.getItemMeta();
            return meta != null ? meta.displayName() : null;
        }

        @Override
        public @NotNull ItemStack setItemDisplayName(@NotNull ItemStack itemStack, @Nullable Component displayName) {
            Objects.requireNonNull(itemStack, "itemStack");
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.displayName(displayName);
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }

        @Override
        public @NotNull List<Component> getItemLore(@NotNull ItemStack itemStack) {
            Objects.requireNonNull(itemStack, "itemStack");
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) {
                return List.of();
            }
            List<Component> lore = meta.lore();
            if (lore == null) {
                return List.of();
            }
            return List.copyOf(lore);
        }

        @Override
        public @NotNull ItemStack setItemLore(@NotNull ItemStack itemStack, @NotNull List<Component> lore) {
            Objects.requireNonNull(itemStack, "itemStack");
            Objects.requireNonNull(lore, "lore");
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.lore(new ArrayList<>(lore));
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }

        @Override
        public @NotNull ItemStack createPlayerHead(@Nullable Player player) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (player != null) {
                meta.setOwningPlayer(player);
            }
            skull.setItemMeta(meta);
            return skull;
        }

        @Override
        public @NotNull ItemStack createPlayerHead(@Nullable OfflinePlayer offlinePlayer) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (offlinePlayer != null) {
                meta.setOwningPlayer(offlinePlayer);
            }
            skull.setItemMeta(meta);
            return skull;
        }

        @Override
        public @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData) {
            return createCustomHead(uuid, textureData, null);
        }

        @Override
        public @NotNull ItemStack createCustomHead(@NotNull UUID uuid, @NotNull String textureData, @Nullable Component displayName) {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(textureData, "textureData");
            ItemStack skull = createPlayerHead((Player) null);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.getPersistentDataContainer().set(TEXTURE_KEY, PersistentDataType.STRING, textureData);
            meta.getPersistentDataContainer().set(PROFILE_KEY, PersistentDataType.STRING, uuid.toString());
            if (displayName != null) {
                meta.displayName(displayName);
            } else {
                meta.displayName(null);
            }
            skull.setItemMeta(meta);
            return skull;
        }

        @Override
        public @NotNull ItemStack applyCustomTexture(@NotNull ItemStack skull, @NotNull UUID uuid, @NotNull String textureData) {
            Objects.requireNonNull(skull, "skull");
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(textureData, "textureData");
            if (skull.getType() != Material.PLAYER_HEAD) {
                return skull;
            }
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.getPersistentDataContainer().set(TEXTURE_KEY, PersistentDataType.STRING, textureData);
            meta.getPersistentDataContainer().set(PROFILE_KEY, PersistentDataType.STRING, uuid.toString());
            skull.setItemMeta(meta);
            return skull;
        }

        @Override
        public @NotNull String getServerVersion() {
            return "MockServer-1.0";
        }

        @Override
        public @NotNull ISchedulerAdapter scheduler() {
            return scheduler;
        }

        Component getLastSingleMessage() {
            return lastSingleMessage;
        }

        List<Component> getLastBatchMessages() {
            return lastBatchMessages;
        }

        Component getLastActionBar() {
            return lastActionBar;
        }

        TitleInvocation getLastTitleInvocation() {
            return lastTitleInvocation;
        }

        int getCloseInvocations() {
            return closeInvocations;
        }

        SchedulerStub getScheduler() {
            return scheduler;
        }

        private record TitleInvocation(Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        }

        private static final class SchedulerStub implements ISchedulerAdapter {

            private int syncExecutions;
            private int asyncExecutions;
            private int delayedExecutions;
            private int repeatingExecutions;
            private int entityExecutions;
            private int locationExecutions;
            private int globalExecutions;
            private int asyncFutureExecutions;

            @Override
            public void runSync(@NotNull Runnable task) {
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public void runAsync(@NotNull Runnable task) {
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public void runDelayed(@NotNull Runnable task, long delayTicks) {
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public void runRepeating(@NotNull Runnable task, long delayTicks, long periodTicks) {
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public void runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
                Objects.requireNonNull(entity, "entity");
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
                Objects.requireNonNull(location, "location");
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public void runGlobal(@NotNull Runnable task) {
                Objects.requireNonNull(task, "task").run();
            }

            @Override
            public @NotNull CompletableFuture<Void> runAsyncFuture(@NotNull Runnable task) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                try {
                    Objects.requireNonNull(task, "task").run();
                    future.complete(null);
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
                return future;
            }

            void markSyncExecuted() {
                syncExecutions++;
            }

            void markAsyncExecuted() {
                asyncExecutions++;
            }

            void markDelayedExecuted() {
                delayedExecutions++;
            }

            void markRepeatingExecuted() {
                repeatingExecutions++;
            }

            void markEntityExecuted() {
                entityExecutions++;
            }

            void markLocationExecuted() {
                locationExecutions++;
            }

            void markGlobalExecuted() {
                globalExecutions++;
            }

            void markAsyncFutureExecuted() {
                asyncFutureExecutions++;
            }

            int getSyncExecutions() {
                return syncExecutions;
            }

            int getAsyncExecutions() {
                return asyncExecutions;
            }

            int getDelayedExecutions() {
                return delayedExecutions;
            }

            int getRepeatingExecutions() {
                return repeatingExecutions;
            }

            int getEntityExecutions() {
                return entityExecutions;
            }

            int getLocationExecutions() {
                return locationExecutions;
            }

            int getGlobalExecutions() {
                return globalExecutions;
            }

            int getAsyncFutureExecutions() {
                return asyncFutureExecutions;
            }
        }
    }
}
