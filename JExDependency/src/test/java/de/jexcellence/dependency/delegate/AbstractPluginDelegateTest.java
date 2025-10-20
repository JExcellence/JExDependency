package de.jexcellence.dependency.delegate;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AbstractPluginDelegateTest {

    private JavaPlugin plugin;
    private TestPluginDelegate delegate;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.spy(MockBukkit.createMockPlugin());
        delegate = new TestPluginDelegate(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getPluginReturnsOriginalInstance() {
        assertSame(plugin, delegate.getPlugin());
    }

    @Test
    void getLoggerForwardsToPlugin() {
        Logger logger = mock(Logger.class);
        doReturn(logger).when(plugin).getLogger();

        assertSame(logger, delegate.logger());
        verify(plugin).getLogger();
    }

    @Test
    void getServerForwardsToPlugin() {
        Server server = mock(Server.class);
        doReturn(server).when(plugin).getServer();

        assertSame(server, delegate.server());
        verify(plugin).getServer();
    }

    @Test
    void getPluginManagerDelegatesViaServer() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        doReturn(server).when(plugin).getServer();
        doReturn(pluginManager).when(server).getPluginManager();

        assertSame(pluginManager, delegate.pluginManager());
        verify(plugin).getServer();
        verify(server).getPluginManager();
    }

    @Test
    void getDataFolderForwardsToPlugin() {
        File dataFolder = mock(File.class);
        doReturn(dataFolder).when(plugin).getDataFolder();

        assertSame(dataFolder, delegate.dataFolder());
        verify(plugin).getDataFolder();
    }

    @Test
    void getConfigForwardsToPlugin() {
        FileConfiguration configuration = mock(FileConfiguration.class);
        doReturn(configuration).when(plugin).getConfig();

        assertSame(configuration, delegate.configuration());
        verify(plugin).getConfig();
    }

    @Test
    void saveConfigForwardsToPlugin() {
        doNothing().when(plugin).saveConfig();

        delegate.saveConfiguration();
        verify(plugin).saveConfig();
    }

    @Test
    void saveDefaultConfigForwardsToPlugin() {
        doNothing().when(plugin).saveDefaultConfig();

        delegate.saveDefaultConfiguration();
        verify(plugin).saveDefaultConfig();
    }

    @Test
    void reloadConfigForwardsToPlugin() {
        doNothing().when(plugin).reloadConfig();

        delegate.reloadConfiguration();
        verify(plugin).reloadConfig();
    }

    @Test
    void getDescriptionForwardsToPlugin() {
        PluginDescriptionFile description = new PluginDescriptionFile("MockPlugin", "1.2.3", "com.example.Mock");
        doReturn(description).when(plugin).getDescription();

        assertSame(description, delegate.description());
        verify(plugin).getDescription();
    }

    @Test
    void getNameReturnsDescriptionName() {
        PluginDescriptionFile description = new PluginDescriptionFile("DelegatePlugin", "1.0.0", "com.example.Mock");
        doReturn(description).when(plugin).getDescription();

        assertSame(description.getName(), delegate.name());
        verify(plugin, times(1)).getDescription();
    }

    @Test
    void getVersionReturnsDescriptionVersion() {
        PluginDescriptionFile description = new PluginDescriptionFile("DelegatePlugin", "4.5.6", "com.example.Mock");
        doReturn(description).when(plugin).getDescription();

        assertSame(description.getVersion(), delegate.version());
        verify(plugin, times(1)).getDescription();
    }

    @Test
    void getCommandForwardsToPlugin() {
        PluginCommand command = mock(PluginCommand.class);
        doReturn(command).when(plugin).getCommand("test");

        assertSame(command, delegate.command("test"));
        verify(plugin).getCommand("test");
    }

    private static final class TestPluginDelegate extends AbstractPluginDelegate<JavaPlugin> {

        private TestPluginDelegate(JavaPlugin plugin) {
            super(plugin);
        }

        private Logger logger() {
            return getLogger();
        }

        private Server server() {
            return getServer();
        }

        private PluginManager pluginManager() {
            return getPluginManager();
        }

        private File dataFolder() {
            return getDataFolder();
        }

        private FileConfiguration configuration() {
            return getConfig();
        }

        private void saveConfiguration() {
            saveConfig();
        }

        private void saveDefaultConfiguration() {
            saveDefaultConfig();
        }

        private void reloadConfiguration() {
            reloadConfig();
        }

        private PluginDescriptionFile description() {
            return getDescription();
        }

        private String name() {
            return getName();
        }

        private String version() {
            return getVersion();
        }

        private PluginCommand command(String name) {
            return getCommand(name);
        }

        @Override
        public void onLoad() {
            // no-op for testing
        }

        @Override
        public void onEnable() {
            // no-op for testing
        }

        @Override
        public void onDisable() {
            // no-op for testing
        }
    }
}
