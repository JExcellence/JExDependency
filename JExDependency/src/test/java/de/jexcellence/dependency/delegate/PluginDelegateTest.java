package de.jexcellence.dependency.delegate;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDelegateTest {

    private JavaPlugin plugin;
    private TestPluginDelegate delegate;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        delegate = new TestPluginDelegate(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getPluginReturnsSuppliedInstance() {
        assertSame(plugin, delegate.getPlugin());
    }

    @Test
    void onLoadMarksDelegateAsLoaded() {
        assertFalse(delegate.isLoaded());

        delegate.onLoad();

        assertTrue(delegate.isLoaded());
    }

    @Test
    void onEnableMarksDelegateAsEnabled() {
        assertFalse(delegate.isEnabled());

        delegate.onEnable();

        assertTrue(delegate.isEnabled());
    }

    @Test
    void onDisableMarksDelegateAsDisabled() {
        assertFalse(delegate.isDisabled());

        delegate.onDisable();

        assertTrue(delegate.isDisabled());
    }

    private static final class TestPluginDelegate implements PluginDelegate<JavaPlugin> {

        private final JavaPlugin plugin;
        private boolean loaded;
        private boolean enabled;
        private boolean disabled;

        private TestPluginDelegate(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public JavaPlugin getPlugin() {
            return plugin;
        }

        @Override
        public void onLoad() {
            loaded = !loaded;
        }

        @Override
        public void onEnable() {
            enabled = !enabled;
        }

        @Override
        public void onDisable() {
            disabled = !disabled;
        }

        private boolean isLoaded() {
            return loaded;
        }

        private boolean isEnabled() {
            return enabled;
        }

        private boolean isDisabled() {
            return disabled;
        }
    }
}

