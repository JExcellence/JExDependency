package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractPlaceholderExpansionTest {

    private JavaPlugin plugin;
    private PluginMeta pluginMeta;

    @BeforeEach
    void setUp() {
        this.plugin = Mockito.mock(JavaPlugin.class);
        this.pluginMeta = Mockito.mock(PluginMeta.class);

        Mockito.when(this.plugin.getName()).thenReturn("TestPlugin");
        Mockito.when(this.plugin.getPluginMeta()).thenReturn(this.pluginMeta);
        Mockito.when(this.plugin.getLogger()).thenReturn(Logger.getLogger("TestPlugin"));

        Mockito.when(this.pluginMeta.getAuthors()).thenReturn(List.of("AuthorOne", "AuthorTwo"));
        Mockito.when(this.pluginMeta.getVersion()).thenReturn("1.2.3");
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(this.plugin, this.pluginMeta);
    }

    @Test
    void metadataAccessorsExposePluginInformation() {
        final TestExpansion expansion = new TestExpansion(this.plugin);

        assertEquals("testplugin", expansion.getIdentifier());
        assertEquals("AuthorOne, AuthorTwo", expansion.getAuthor());
        assertEquals("1.2.3", expansion.getVersion());
        assertTrue(expansion.persist(), "Expansion should persist across reloads");
    }

    @Test
    void placeholdersAreCachedAndFormattedWithIdentifier() {
        final TestExpansion expansion = new TestExpansion(this.plugin);

        final List<String> placeholders = expansion.getPlaceholders();
        assertEquals(List.of("%testplugin_greeting%", "%testplugin_farewell%", "%testplugin_status%"), placeholders);
        assertEquals(1, expansion.getDefineCalls(), "Placeholder definitions should be cached during construction");

        placeholders.add("%testplugin_custom%");

        final List<String> refreshed = expansion.getPlaceholders();
        assertEquals(List.of("%testplugin_greeting%", "%testplugin_farewell%", "%testplugin_status%"), refreshed,
                "Mutating the returned collection must not affect the cached placeholders");
        assertEquals(1, expansion.getDefineCalls(), "Cached definitions should avoid repeated lookups");
    }

    @Test
    void onPlaceholderRequestDelegatesToResolver() {
        final TestExpansion expansion = new TestExpansion(this.plugin);
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Steve");

        final String value = expansion.onPlaceholderRequest(player, "greeting");

        assertEquals("Hello, Steve!", value);
        assertSame(player, expansion.getLastPlayer());
        assertEquals("greeting", expansion.getLastParams());
        assertEquals(1, expansion.getResolveCalls());
        assertNull(expansion.onPlaceholderRequest(player, "unknown"),
                "Unknown placeholders should return null");
    }

    @Test
    void onRequestReturnsNullWhenOfflinePlayerUnavailable() {
        final TestExpansion expansion = new TestExpansion(this.plugin);
        final OfflinePlayer offlinePlayer = Mockito.mock(OfflinePlayer.class);
        Mockito.when(offlinePlayer.isOnline()).thenReturn(false);

        assertNull(expansion.onRequest(offlinePlayer, "greeting"));

        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Alex");
        Mockito.when(offlinePlayer.isOnline()).thenReturn(true);
        Mockito.when(offlinePlayer.getPlayer()).thenReturn(player);

        assertEquals("Hello, Alex!", expansion.onRequest(offlinePlayer, "greeting"));
        assertSame(player, expansion.getLastPlayer());
    }

    @Test
    void placeholderRegistryRegistersAndUnregistersWhenAvailable() {
        final TestExpansion expansion = new TestExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);
            assertTrue(registry.isAvailable(), "Registry should detect PlaceholderAPI availability");

            registry.register();
            assertEquals(1, expansion.getRegisterCalls(), "Registration should delegate to the expansion");
            assertTrue(registry.isRegistered(), "Registry should report registered state");

            registry.unregister();
            assertEquals(1, expansion.getUnregisterCalls(), "Unregister should delegate to the expansion");
            assertFalse(registry.isRegistered(), "Registry should report unregistered state after cleanup");
        }
    }

    @Test
    void placeholderRegistryNoOpsWhenPlaceholderApiMissing() {
        final TestExpansion expansion = new TestExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);
            assertFalse(registry.isAvailable(), "Registry should report unavailable state without PlaceholderAPI");

            registry.register();
            assertEquals(0, expansion.getRegisterCalls(), "Registration should not run without PlaceholderAPI");
            assertFalse(registry.isRegistered(), "Registry should remain unregistered when dependency is missing");

            registry.unregister();
            assertEquals(0, expansion.getUnregisterCalls(), "Unregister should not run when registration never occurred");
        }
    }

    private static final class TestExpansion extends AbstractPlaceholderExpansion {

        private int defineCalls;
        private int resolveCalls;
        private int registerCalls;
        private int unregisterCalls;
        private boolean registered;
        private Player lastPlayer;
        private String lastParams;

        private TestExpansion(final @NotNull JavaPlugin plugin) {
            super(plugin);
        }

        @Override
        protected @NotNull List<String> definePlaceholders() {
            defineCalls++;
            return List.of("greeting", "farewell", "status");
        }

        @Override
        protected String resolvePlaceholder(final Player player, final @NotNull String params) {
            resolveCalls++;
            lastPlayer = player;
            lastParams = params;

            return switch (params) {
                case "greeting" -> player != null ? "Hello, " + player.getName() + "!" : "Hello!";
                case "farewell" -> "Goodbye!";
                case "status" -> registered ? "registered" : "unregistered";
                default -> null;
            };
        }

        @Override
        public boolean register() {
            registerCalls++;
            registered = true;
            return true;
        }

        @Override
        public boolean unregister() {
            unregisterCalls++;
            registered = false;
            return true;
        }

        @Override
        public boolean isRegistered() {
            return registered;
        }

        private int getDefineCalls() {
            return defineCalls;
        }

        private int getResolveCalls() {
            return resolveCalls;
        }

        private int getRegisterCalls() {
            return registerCalls;
        }

        private int getUnregisterCalls() {
            return unregisterCalls;
        }

        private Player getLastPlayer() {
            return lastPlayer;
        }

        private String getLastParams() {
            return lastParams;
        }
    }
}
