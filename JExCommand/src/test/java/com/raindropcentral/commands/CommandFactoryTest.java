package com.raindropcentral.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.raindropcentral.commands.testplugin.TestContexts;
import com.raindropcentral.commands.testplugin.TestPlugin;
import com.raindropcentral.commands.testplugin.TestPluginBase;
import com.raindropcentral.commands.testplugin.command.ContextAwareCommand;
import com.raindropcentral.commands.testplugin.command.PluginFallbackCommand;
import com.raindropcentral.commands.testplugin.command.Pr18n;
import com.raindropcentral.commands.testplugin.command.Pr18nCommand;
import com.raindropcentral.commands.testplugin.command.SuperclassFallbackCommand;
import com.raindropcentral.tests.misconfigured.MissingConstructorCommand;
import com.raindropcentral.tests.misconfigured.MalformedConstructorListener;
import com.raindropcentral.commands.testplugin.listener.ContextAwareListener;
import com.raindropcentral.commands.testplugin.listener.PluginFallbackListener;
import com.raindropcentral.commands.testplugin.listener.SuperclassFallbackListener;
import de.jexcellence.evaluable.CommandUpdater;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CommandFactoryTest {

    private ServerMock server;
    private TestPlugin plugin;
    private CommandFactory factory;
    private TestContexts.ConcreteContext context;
    private Handler logHandler;
    private final List<LogRecord> logRecords = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.load(TestPlugin.class);
        this.plugin.getDataFolder().mkdirs();
        this.context = new TestContexts.ConcreteContext();
        this.factory = new CommandFactory(this.plugin, this.context);
        this.logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logRecords.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        this.plugin.getLogger().addHandler(this.logHandler);
        resetCommandCounters();
        resetListenerCounters();
        logRecords.clear();
        copyCommandResources();
    }

    @AfterEach
    void tearDown() {
        if (this.plugin != null) {
            this.plugin.getLogger().removeHandler(this.logHandler);
        }
        MockBukkit.unmock();
    }

    @Test
    void registerAllCommandsAndListenersRegistersAnnotatedTypes() throws Exception {
        CommandUpdater updaterMock = injectUpdaterMock(this.factory);

        this.factory.registerAllCommandsAndListeners();

        assertEquals(1, ContextAwareCommand.instances(), "Context aware command should be constructed once");
        assertEquals(1, PluginFallbackCommand.instances(), "Plugin fallback command should be constructed once");
        assertEquals(1, SuperclassFallbackCommand.instances(), "Superclass fallback command should be constructed once");
        assertEquals(1, Pr18nCommand.instances(), "Pr18n command should be constructed once");

        assertEquals(1, ContextAwareListener.INSTANCES.get(), "Context aware listener should be constructed once");
        assertEquals(1, PluginFallbackListener.INSTANCES.get(), "Plugin fallback listener should be constructed once");
        assertEquals(1, SuperclassFallbackListener.INSTANCES.get(), "Superclass fallback listener should be constructed once");

        ArgumentCaptor<BukkitCommand> commandCaptor = ArgumentCaptor.forClass(BukkitCommand.class);
        verify(updaterMock, times(4)).tryRegisterCommand(commandCaptor.capture());
        verify(updaterMock, times(4)).trySyncCommands();

        long uniqueTypes = commandCaptor.getAllValues().stream()
                .map(Object::getClass)
                .distinct()
                .count();
        assertEquals(4, uniqueTypes, "Each command type should be registered exactly once");

        BukkitCommand pr18nCommand = commandCaptor.getAllValues().stream()
                .filter(Pr18nCommand.class::isInstance)
                .findFirst()
                .orElseThrow();
        assertTrue(pr18nCommand.getAliases().stream()
                        .anyMatch(alias -> alias.contains("reload")),
                "Pr18n command should expose a reload alias");

        assertFalse(logRecords.stream()
                        .anyMatch(record -> record.getLevel().intValue() >= Level.WARNING.intValue()),
                "Successful registration should not emit warnings");
    }

    @Test
    void registerCommandLogsWarningWhenConstructorMissing() throws Exception {
        CommandFactory localFactory = new CommandFactory(this.plugin, this.context);
        CommandUpdater updaterMock = injectUpdaterMock(localFactory);
        logRecords.clear();

        invokeRegisterCommand(localFactory, MissingConstructorCommand.class);

        assertTrue(logRecords.stream().anyMatch(record -> record.getMessage().contains("No compatible constructor")));
        verifyNoInteractions(updaterMock);
    }

    @Test
    void registerListenerLogsWarningWhenConstructorMissing() throws Exception {
        logRecords.clear();
        invokeRegisterListener(this.factory, MalformedConstructorListener.class);

        assertTrue(logRecords.stream().anyMatch(record -> record.getMessage().contains("No compatible constructor")),
                "Factory should warn about listener without compatible constructor");
    }

    @Test
    void registerCommandUsesPluginWhenContextMissing() throws Exception {
        CommandFactory contextlessFactory = new CommandFactory(this.plugin);
        CommandUpdater updaterMock = injectUpdaterMock(contextlessFactory);
        logRecords.clear();
        PluginFallbackCommand.reset();

        invokeRegisterCommand(contextlessFactory, PluginFallbackCommand.class);

        assertSame(this.plugin, PluginFallbackCommand.lastDependency());
        verify(updaterMock, times(1)).tryRegisterCommand(any());
        verify(updaterMock, times(1)).trySyncCommands();
    }

    @Test
    void registerCommandUsesSuperclassContextWhenAvailable() throws Exception {
        CommandUpdater updaterMock = injectUpdaterMock(this.factory);
        SuperclassFallbackCommand.reset();

        invokeRegisterCommand(this.factory, SuperclassFallbackCommand.class);

        assertTrue(SuperclassFallbackCommand.lastDependency() instanceof TestContexts.BaseContext);
        verify(updaterMock, times(1)).tryRegisterCommand(any());
        verify(updaterMock, times(1)).trySyncCommands();
    }

    @Test
    void registerCommandAppliesPr18nOverrides() throws Exception {
        CommandUpdater updaterMock = injectUpdaterMock(this.factory);
        ArgumentCaptor<BukkitCommand> captor = ArgumentCaptor.forClass(BukkitCommand.class);

        invokeRegisterCommand(this.factory, Pr18n.class);

        verify(updaterMock, times(1)).tryRegisterCommand(captor.capture());
        verify(updaterMock, times(1)).trySyncCommands();

        BukkitCommand command = captor.getValue();
        String pluginPrefix = this.plugin.getName().toLowerCase(Locale.ROOT);
        assertEquals(pluginPrefix + ":pr18n", command.getName());
        assertTrue(command.getAliases().contains(pluginPrefix + "reloadI18n"));
    }

    @Test
    void registerListenerUsesContextWhenAvailable() throws Exception {
        logRecords.clear();
        ContextAwareListener.INSTANCES.set(0);

        invokeRegisterListener(this.factory, ContextAwareListener.class);

        assertEquals(1, ContextAwareListener.INSTANCES.get());
        assertFalse(logRecords.stream()
                .anyMatch(record -> record.getLevel().intValue() >= Level.WARNING.intValue()));
    }

    @Test
    void isAssignableFromSuperclassRecognisesHierarchy() throws Exception {
        Method method = CommandFactory.class.getDeclaredMethod("isAssignableFromSuperclass", Class.class, Class.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(this.factory, TestPluginBase.class, TestPlugin.class);
        assertTrue(result, "Superclass check should recognise plugin inheritance");
    }

    @Test
    void gettersExposeInjectedValues() {
        assertSame(this.plugin, this.factory.getLoadedPlugin());
        assertSame(this.context, this.factory.getContextObject());
    }

    private void copyCommandResources() throws Exception {
        Path dataFolder = this.plugin.getDataFolder().toPath().resolve("commands");
        Files.createDirectories(dataFolder);
        for (String resource : List.of(
                "commands/contextawarecommand.yml",
                "commands/pluginfallbackcommand.yml",
                "commands/superclassfallbackcommand.yml",
                "commands/pr18ncommand.yml",
                "commands/pr18n.yml"
        )) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                Objects.requireNonNull(stream, "Missing test resource: " + resource);
                Files.copy(stream, dataFolder.resolve(Path.of(resource).getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void resetCommandCounters() {
        ContextAwareCommand.reset();
        PluginFallbackCommand.reset();
        SuperclassFallbackCommand.reset();
        Pr18nCommand.reset();
    }

    private void resetListenerCounters() {
        ContextAwareListener.INSTANCES.set(0);
        PluginFallbackListener.INSTANCES.set(0);
        SuperclassFallbackListener.INSTANCES.set(0);
    }

    private CommandUpdater injectUpdaterMock(CommandFactory factory) throws Exception {
        CommandUpdater updaterMock = mock(CommandUpdater.class);
        Field field = CommandFactory.class.getDeclaredField("commandUpdater");
        field.setAccessible(true);
        field.set(factory, updaterMock);
        return updaterMock;
    }

    private void invokeRegisterCommand(CommandFactory factory, Class<?> commandClass) throws Exception {
        Method method = CommandFactory.class.getDeclaredMethod("registerCommand", Class.class);
        method.setAccessible(true);
        method.invoke(factory, commandClass);
    }

    private void invokeRegisterListener(CommandFactory factory, Class<? extends Listener> listenerClass) throws Exception {
        Method method = CommandFactory.class.getDeclaredMethod("registerListener", Class.class);
        method.setAccessible(true);
        method.invoke(factory, listenerClass);
    }
}
