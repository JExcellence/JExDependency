package com.raindropcentral.rplatform.api.impl;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.raindropcentral.rplatform.api.PlatformType;
import com.raindropcentral.rplatform.scheduler.ISchedulerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class SpigotPlatformAPITest {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ServerMock server;
    private JavaPlugin plugin;
    private SpigotPlatformAPI api;
    private SchedulerStub scheduler;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SpigotTestPlugin.class);
        scheduler = new SchedulerStub();

        try (MockedStatic<ISchedulerAdapter> mocked = Mockito.mockStatic(ISchedulerAdapter.class)) {
            mocked.when(() -> ISchedulerAdapter.create(plugin, PlatformType.SPIGOT)).thenReturn(scheduler);
            api = new SpigotPlatformAPI(plugin);
        }
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void metadataReportsSpigotCapabilities() {
        assertEquals(PlatformType.SPIGOT, api.getType());
        assertFalse(api.supportsAdventure());
        assertFalse(api.supportsFolia());
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
    void messagingSerializesComponentsToLegacyStrings() {
        Player player = Mockito.mock(Player.class);
        Component single = Component.text("Green", NamedTextColor.GREEN);
        api.sendMessage(player, single);
        Mockito.verify(player).sendMessage(LEGACY.serialize(single));

        List<Component> batch = List.of(
                Component.text("One", NamedTextColor.BLUE),
                Component.text("Two", NamedTextColor.RED)
        );
        api.sendMessages(player, batch);
        Mockito.verify(player).sendMessage(LEGACY.serialize(batch.get(0)));
        Mockito.verify(player).sendMessage(LEGACY.serialize(batch.get(1)));

        Component action = Component.text("Bar", NamedTextColor.GOLD);
        api.sendActionBar(player, action);
        Mockito.verify(player).sendActionBar(LEGACY.serialize(action));
    }

    @Test
    void sendTitleSerializesComponentsAndAppliesFallback() {
        Player player = Mockito.mock(Player.class);
        Component title = Component.text("Main", NamedTextColor.YELLOW);
        Component subtitle = Component.text("Sub", NamedTextColor.AQUA);

        api.sendTitle(player, title, subtitle, 10, 40, 5);
        Mockito.verify(player).sendTitle(
                LEGACY.serialize(title),
                LEGACY.serialize(subtitle),
                10,
                40,
                5
        );

        Mockito.reset(player);
        api.sendTitle(player, title, null, 2, 20, 2);
        Mockito.verify(player).sendTitle(
                LEGACY.serialize(title),
                "",
                2,
                20,
                2
        );
    }

    @Test
    void displayNameConversionsHandleLegacyAndNulls() {
        Player player = Mockito.mock(Player.class);
        Component expected = Component.text("Display", NamedTextColor.LIGHT_PURPLE);
        Mockito.when(player.getDisplayName()).thenReturn(LEGACY.serialize(expected), null);

        assertEquals(expected, api.getDisplayName(player));
        assertEquals(Component.empty(), api.getDisplayName(player));

        Component update = Component.text("Updated", NamedTextColor.DARK_GREEN);
        api.setDisplayName(player, update);
        Mockito.verify(player).setDisplayName(LEGACY.serialize(update));
    }

    @Test
    void itemDisplayNameAndLoreConversionsRoundTrip() {
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = stack.getItemMeta();
        assertNotNull(meta);
        meta.setDisplayName("§aEmerald Blade");
        meta.setLore(List.of("§bFirst", "Second"));
        stack.setItemMeta(meta);

        Component expectedName = LEGACY.deserialize("§aEmerald Blade");
        assertEquals(expectedName, api.getItemDisplayName(stack));
        List<Component> lore = api.getItemLore(stack);
        assertEquals(List.of(
                LEGACY.deserialize("§bFirst"),
                LEGACY.deserialize("Second")
        ), lore);

        Component renamed = Component.text("Shiny", NamedTextColor.GOLD);
        api.setItemDisplayName(stack, renamed);
        assertEquals(LEGACY.serialize(renamed), stack.getItemMeta().getDisplayName());

        api.setItemDisplayName(stack, null);
        assertNull(stack.getItemMeta().getDisplayName());
        assertNull(api.getItemDisplayName(stack));

        List<Component> newLore = List.of(Component.text("Line", NamedTextColor.WHITE));
        api.setItemLore(stack, newLore);
        List<String> storedLore = stack.getItemMeta().getLore();
        assertNotNull(storedLore);
        assertEquals(List.of(LEGACY.serialize(newLore.get(0))), storedLore);
    }

    @Test
    void itemOperationsHandleMissingMeta() {
        ItemStack stack = new ItemStack(Material.AIR);
        assertNull(stack.getItemMeta());

        assertNull(api.getItemDisplayName(stack));
        assertSame(stack, api.setItemDisplayName(stack, Component.text("Ignored")));
        assertSame(stack, api.setItemDisplayName(stack, null));
        assertTrue(api.getItemLore(stack).isEmpty());
        assertSame(stack, api.setItemLore(stack, List.of(Component.text("Lore"))));
    }

    @Test
    void createPlayerHeadAssignsOwnersAndHandlesNulls() {
        ItemStack emptyHead = api.createPlayerHead((Player) null);
        SkullMeta emptyMeta = (SkullMeta) emptyHead.getItemMeta();
        assertNotNull(emptyMeta);
        assertFalse(emptyMeta.hasOwner());
        assertNull(emptyMeta.getOwningPlayer());

        Player player = server.addPlayer();
        ItemStack playerHead = api.createPlayerHead(player);
        SkullMeta playerMeta = (SkullMeta) playerHead.getItemMeta();
        assertNotNull(playerMeta);
        assertNotNull(playerMeta.getOwningPlayer());
        assertEquals(player.getUniqueId(), playerMeta.getOwningPlayer().getUniqueId());

        UUID offlineId = UUID.randomUUID();
        OfflinePlayer offlinePlayer = server.getOfflinePlayer(offlineId);
        ItemStack offlineHead = api.createPlayerHead(offlinePlayer);
        SkullMeta offlineMeta = (SkullMeta) offlineHead.getItemMeta();
        assertNotNull(offlineMeta);
        assertNotNull(offlineMeta.getOwningPlayer());
        assertEquals(offlineId, offlineMeta.getOwningPlayer().getUniqueId());
    }

    @Test
    void createCustomHeadAppliesTextureAndOptionalDisplayName() throws Exception {
        UUID uuid = UUID.randomUUID();
        String texture = "base64-texture";

        ItemStack head = api.createCustomHead(uuid, texture);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assertNotNull(meta);
        assertProfileMatches(meta, uuid, texture);
        assertFalse(meta.hasDisplayName());

        Component displayName = Component.text("Custom", NamedTextColor.GRAY);
        ItemStack named = api.createCustomHead(uuid, texture, displayName);
        SkullMeta namedMeta = (SkullMeta) named.getItemMeta();
        assertNotNull(namedMeta);
        assertProfileMatches(namedMeta, uuid, texture);
        assertEquals(LEGACY.serialize(displayName), namedMeta.getDisplayName());
        assertEquals(displayName, api.getItemDisplayName(named));
    }

    @Test
    void applyCustomTextureMutatesSkullsAndIgnoresOthers() throws Exception {
        UUID uuid = UUID.randomUUID();
        String texture = "updated-texture";

        ItemStack skull = api.createPlayerHead((Player) null);
        ItemStack mutated = api.applyCustomTexture(skull, uuid, texture);
        assertSame(skull, mutated);
        assertProfileMatches((SkullMeta) skull.getItemMeta(), uuid, texture);

        ItemStack stone = new ItemStack(Material.STONE);
        assertSame(stone, api.applyCustomTexture(stone, uuid, texture));
        assertNull(stone.getItemMeta());
    }

    @Test
    void schedulerDelegatesToStub() {
        assertSame(scheduler, api.scheduler());

        Runnable task = Mockito.mock(Runnable.class);
        api.scheduler().runSync(task);
        assertEquals(1, scheduler.getSyncRuns());
        Mockito.verify(task).run();
    }

    private void assertProfileMatches(final SkullMeta meta, final UUID uuid, final String texture) throws Exception {
        Field profileField = meta.getClass().getDeclaredField("profile");
        profileField.setAccessible(true);
        Object profile = profileField.get(meta);
        assertNotNull(profile);

        Method getId = profile.getClass().getMethod("getId");
        assertEquals(uuid, getId.invoke(profile));

        Method getProperties = profile.getClass().getMethod("getProperties");
        Object properties = getProperties.invoke(profile);
        Method get = properties.getClass().getMethod("get", Object.class);
        Collection<?> values = (Collection<?>) get.invoke(properties, "textures");
        assertNotNull(values);
        Iterator<?> iterator = values.iterator();
        assertTrue(iterator.hasNext());
        Object property = iterator.next();
        Method getValue = property.getClass().getMethod("getValue");
        assertEquals(texture, getValue.invoke(property));
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

    public static class SpigotTestPlugin extends JavaPlugin {
    }
}
