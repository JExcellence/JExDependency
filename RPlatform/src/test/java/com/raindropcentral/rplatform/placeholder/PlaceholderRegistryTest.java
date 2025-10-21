package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderRegistryTest {

    private JavaPlugin plugin;
    private PluginMeta pluginMeta;

    @BeforeEach
    void setUp() {
        this.plugin = Mockito.mock(JavaPlugin.class);
        this.pluginMeta = Mockito.mock(PluginMeta.class);

        Mockito.when(this.plugin.getName()).thenReturn("RegistryPlugin");
        Mockito.when(this.plugin.getPluginMeta()).thenReturn(this.pluginMeta);

        Mockito.when(this.pluginMeta.getAuthors()).thenReturn(List.of("Tester"));
        Mockito.when(this.pluginMeta.getVersion()).thenReturn("9.9.9");
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(this.plugin, this.pluginMeta);
    }

    @Test
    void registerTracksExpansionWhenPlaceholderApiAvailable() {
        final TrackingExpansion expansion = new TrackingExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);

            assertTrue(registry.isAvailable(), "PlaceholderAPI should be reported as available");
            assertFalse(registry.isRegistered(), "Expansion should start in an unregistered state");

            registry.register();

            assertEquals(1, expansion.getRegisterCalls(), "Expansion should be registered once");
            assertTrue(registry.isRegistered(), "Registry lookup should reflect registered state");
        }
    }

    @Test
    void unregisterRemovesExpansionWhenPreviouslyRegistered() {
        final TrackingExpansion expansion = new TrackingExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);
            registry.register();

            registry.unregister();

            assertEquals(1, expansion.getUnregisterCalls(), "Expansion should be unregistered once");
            assertFalse(registry.isRegistered(), "Registry should report placeholders as removed");
        }
    }

    @Test
    void registerNoOpsWhenPlaceholderApiMissing() {
        final TrackingExpansion expansion = new TrackingExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);

            assertFalse(registry.isAvailable(), "Registry should report PlaceholderAPI as unavailable");

            registry.register();
            registry.unregister();

            assertEquals(0, expansion.getRegisterCalls(), "Register should not be invoked without PlaceholderAPI");
            assertEquals(0, expansion.getUnregisterCalls(), "Unregister should not be invoked without PlaceholderAPI");
        }
    }

    @Test
    void iterationReturnsFormattedPlaceholderIdentifiers() {
        final TrackingExpansion expansion = new TrackingExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);
            registry.register();

            final List<String> placeholders = expansion.getPlaceholders();
            assertIterableEquals(
                    List.of("%registryplugin_alpha%", "%registryplugin_beta%", "%registryplugin_gamma%"),
                    placeholders,
                    "Iteration should return all formatted placeholders"
            );
            assertEquals(Set.copyOf(placeholders).size(), placeholders.size(),
                    "Iteration should not introduce duplicate placeholders");
        }
    }

    @Test
    void duplicateRegistrationsDoNotAffectPlaceholderTracking() {
        final TrackingExpansion expansion = new TrackingExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);

            registry.register();
            registry.register();

            assertEquals(1, expansion.getRegisterCalls(), "Only the first registration should take effect");
            assertTrue(registry.isRegistered(), "Registry should continue to report registered state");

            final List<String> placeholders = expansion.getPlaceholders();
            assertIterableEquals(
                    List.of("%registryplugin_alpha%", "%registryplugin_beta%", "%registryplugin_gamma%"),
                    placeholders,
                    "Duplicate registrations must not change cached placeholders"
            );
        }
    }

    @Test
    void concurrentRegistrationAndRemovalMaintainConsistentState() throws InterruptedException {
        final TrackingExpansion expansion = new TrackingExpansion(this.plugin);
        final PluginManager pluginManager = Mockito.mock(PluginManager.class);
        Mockito.when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            final PlaceholderRegistry registry = new PlaceholderRegistry(this.plugin, expansion);

            runConcurrent(registry::register, 8);

            assertEquals(1, expansion.getRegisterCalls(), "Concurrent registrations should only succeed once");
            assertTrue(registry.isRegistered(), "Registry should remain registered after concurrent registrations");

            runConcurrent(registry::unregister, 8);

            assertEquals(1, expansion.getUnregisterCalls(), "Concurrent removals should only succeed once");
            assertFalse(registry.isRegistered(), "Registry should report unregistered after concurrent removals");
        }
    }

    private void runConcurrent(final Runnable action, final int threads) throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    action.run();
                } catch (final InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static final class TrackingExpansion extends AbstractPlaceholderExpansion {

        private final AtomicInteger registerCalls = new AtomicInteger();
        private final AtomicInteger unregisterCalls = new AtomicInteger();
        private final AtomicInteger resolveCalls = new AtomicInteger();
        private final AtomicBoolean registered = new AtomicBoolean(false);

        private TrackingExpansion(final @NotNull JavaPlugin plugin) {
            super(plugin);
        }

        @Override
        protected @NotNull List<String> definePlaceholders() {
            return List.of("alpha", "beta", "gamma");
        }

        @Override
        protected String resolvePlaceholder(final Player player, final @NotNull String params) {
            resolveCalls.incrementAndGet();
            return switch (params) {
                case "alpha" -> "A";
                case "beta" -> "B";
                case "gamma" -> "G";
                default -> null;
            };
        }

        @Override
        public boolean register() {
            if (this.registered.compareAndSet(false, true)) {
                this.registerCalls.incrementAndGet();
                return true;
            }
            return false;
        }

        @Override
        public boolean unregister() {
            if (this.registered.compareAndSet(true, false)) {
                this.unregisterCalls.incrementAndGet();
                return true;
            }
            return false;
        }

        @Override
        public boolean isRegistered() {
            return this.registered.get();
        }

        private int getRegisterCalls() {
            return this.registerCalls.get();
        }

        private int getUnregisterCalls() {
            return this.unregisterCalls.get();
        }

        @SuppressWarnings("unused")
        private int getResolveCalls() {
            return this.resolveCalls.get();
        }
    }
}
