package com.raindropcentral.commands;

import com.raindropcentral.commands.permission.PermissionHierarchyRegistrar;
import com.raindropcentral.commands.permission.PermissionParentProvider;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.commands.v2.CommandDefinition;
import com.raindropcentral.commands.v2.CommandHandler;
import com.raindropcentral.commands.v2.CommandMessages;
import com.raindropcentral.commands.v2.CommandTreeHandler;
import com.raindropcentral.commands.v2.CommandTreeLoader;
import com.raindropcentral.commands.v2.argument.ArgumentTypeRegistry;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.evaluable.command.AliasCollisionPolicy;
import de.jexcellence.evaluable.command.CommandRegistrar;
import de.jexcellence.evaluable.command.RegisteredCommand;
import de.jexcellence.evaluable.section.CommandSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Discovers and registers {@link Command @Command}-annotated handlers and
 * {@link Listener} implementations from the plugin classpath.
 *
 * <p>Supports edition gating through an optional context object so free and
 * premium variants receive the correct dependencies via constructor injection.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public class CommandFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CommandFactory.class);

    private static final String CLASS_SUFFIX      = ".class";
    private static final String COMMAND_PKG        = "command";
    private static final String COMMANDS_PKG       = "commands";
    private static final String COMMANDS_FOLDER    = "commands";
    private static final String LISTENER_PKG       = "listener";
    private static final String LISTENERS_PKG      = "listeners";
    private static final String MODULE_INFO        = "module-info";
    private static final String PACKAGE_INFO       = "package-info";
    private static final String PERMISSIONS_KEY    = "permissions";

    private final JavaPlugin loadedPlugin;
    private final Object contextObject;
    private final CommandRegistrar commandRegistrar;

    public CommandFactory(@NotNull JavaPlugin loadedPlugin) {
        this(loadedPlugin, null);
    }

    public CommandFactory(@NotNull JavaPlugin loadedPlugin, @Nullable Object contextObject) {
        this.loadedPlugin = loadedPlugin;
        this.contextObject = contextObject;
        this.commandRegistrar = CommandRegistrar.detect(loadedPlugin);
        LOG.info("Selected command registrar: {}", this.commandRegistrar.getClass().getSimpleName());
    }

    /** Scans the plugin artifact and registers all commands and listeners. */
    public void registerAllCommandsAndListeners() {
        try {
            var classes = discoverClasses();
            classes.stream().filter(this::isCommandClass).forEach(this::registerCommand);
            classes.stream().filter(this::isListenerClass).forEach(this::registerListener);
        } catch (Exception e) {
            LOG.warn("Could not register commands or listeners", e);
        }
    }

    @NotNull public JavaPlugin getLoadedPlugin()   { return loadedPlugin; }
    @Nullable public Object    getContextObject()   { return contextObject; }

    // ── JExCommand 2.0 tree registration ──────────────────────────────────────

    /**
     * Registers a JExCommand 2.0 command tree loaded from a YAML file bundled as
     * a classpath resource.
     *
     * <p>The YAML is parsed and compiled via {@link CommandTreeLoader} against the
     * supplied {@link ArgumentTypeRegistry}, then handed to a
     * {@link CommandTreeHandler} that bridges Bukkit execution/completion to the
     * supplied handler map.
     *
     * @param resourcePath slash-separated classpath resource (e.g. {@code "commands/economy.yml"})
     * @param handlers     path → handler map — keys are dot-separated tree paths such
     *                     as {@code "economy"} or {@code "economy.give"}
     * @param messages     i18n SPI used to surface parse/permission errors
     * @param registry     argument type registry (defaults + any custom types)
     * @return the registered Bukkit command, or {@code null} if registration failed
     */
    public @Nullable CommandTreeHandler registerTree(@NotNull String resourcePath,
                                                      @NotNull Map<String, CommandHandler> handlers,
                                                      @NotNull CommandMessages messages,
                                                      @NotNull ArgumentTypeRegistry registry) {
        try {
            var loader = new CommandTreeLoader(registry);
            var definition = loader.loadResource(loadedPlugin.getClass().getClassLoader(), resourcePath);
            return registerTree(definition, handlers, messages);
        } catch (Exception e) {
            LOG.warn("Could not load command tree from resource '{}'", resourcePath, e);
            return null;
        }
    }

    /**
     * Variant that loads the tree from a {@link File} on disk (e.g. from the plugin's
     * data folder).
     */
    public @Nullable CommandTreeHandler registerTree(@NotNull File file,
                                                      @NotNull Map<String, CommandHandler> handlers,
                                                      @NotNull CommandMessages messages,
                                                      @NotNull ArgumentTypeRegistry registry) {
        try {
            var loader = new CommandTreeLoader(registry);
            var definition = loader.load(file);
            return registerTree(definition, handlers, messages);
        } catch (Exception e) {
            LOG.warn("Could not load command tree from file '{}'", file, e);
            return null;
        }
    }

    /**
     * Registers a pre-compiled {@link CommandDefinition}. Useful when the tree is
     * built in code rather than loaded from YAML.
     */
    public @Nullable CommandTreeHandler registerTree(@NotNull CommandDefinition definition,
                                                      @NotNull Map<String, CommandHandler> handlers,
                                                      @NotNull CommandMessages messages) {
        try {
            var treeCommand = new CommandTreeHandler(definition, handlers, messages);
            RegisteredCommand registered = commandRegistrar.register(loadedPlugin, treeCommand, AliasCollisionPolicy.PREFIX);
            if (!registered.collisions().isEmpty()) {
                registered.collisions().forEach(collision ->
                        LOG.warn("Alias collision on /{}: owned by '{}', policy={} (tree={})",
                                collision.alias(), collision.existingOwner(),
                                collision.policy(), definition.name())
                );
            }
            return treeCommand;
        } catch (Exception e) {
            LOG.warn("Could not register command tree '{}'", definition.name(), e);
            return null;
        }
    }

    // ── Class discovery ───────────────────────────────────────────────────────

    private List<Class<?>> discoverClasses() throws IOException, URISyntaxException {
        var codeSource = loadedPlugin.getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            throw new IOException("Could not resolve plugin code source for command discovery.");
        }

        var path = Path.of(codeSource.getLocation().toURI());
        var pkg = loadedPlugin.getClass().getPackageName();
        var names = Files.isDirectory(path)
                ? scanDirectory(path, pkg)
                : scanJar(path, pkg);

        return names.stream()
                .<Class<?>>mapMulti((name, downstream) -> {
                    var clazz = tryLoad(name);
                    if (clazz != null) downstream.accept(clazz);
                })
                .toList();
    }

    private List<String> scanDirectory(Path root, String pkg) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(this::toClassName)
                    .filter(name -> isRelevant(name, pkg))
                    .sorted().toList();
        }
    }

    private List<String> scanJar(Path jarPath, String pkg) throws IOException {
        try (var jar = new JarFile(jarPath.toFile())) {
            return jar.stream()
                    .filter(e -> !e.isDirectory())
                    .map(e -> toClassName(e.getName()))
                    .filter(name -> isRelevant(name, pkg))
                    .sorted().toList();
        }
    }

    private @Nullable Class<?> tryLoad(String className) {
        try {
            return Class.forName(className, false, loadedPlugin.getClass().getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    private boolean isRelevant(String className, String pluginPkg) {
        if (className.isBlank()
                || !className.startsWith(pluginPkg + ".")
                || className.contains("$")
                || className.endsWith("." + PACKAGE_INFO)
                || className.endsWith("." + MODULE_INFO)) {
            return false;
        }
        var lastDot = className.lastIndexOf('.');
        if (lastDot <= 0) return false;
        var pkgName = className.substring(0, lastDot);
        return isCommandPkg(pkgName) || isListenerPkg(pkgName);
    }

    private boolean isCommandClass(Class<?> c)  { return c.isAnnotationPresent(Command.class) && isCommandPkg(c.getPackageName()); }
    private boolean isListenerClass(Class<?> c) { return Listener.class.isAssignableFrom(c)   && isListenerPkg(c.getPackageName()); }
    private boolean isCommandPkg(String pkg)    { return hasSegment(pkg, COMMAND_PKG) || hasSegment(pkg, COMMANDS_PKG); }
    private boolean isListenerPkg(String pkg)   { return hasSegment(pkg, LISTENER_PKG) || hasSegment(pkg, LISTENERS_PKG); }

    private boolean hasSegment(String pkgName, String segment) {
        for (var part : pkgName.split("\\.")) {
            if (segment.equals(part)) return true;
        }
        return false;
    }

    private String toClassName(String resource) {
        var normalized = resource.replace('\\', '/');
        if (!normalized.endsWith(CLASS_SUFFIX)) return "";
        return normalized.substring(0, normalized.length() - CLASS_SUFFIX.length()).replace('/', '.');
    }

    // ── Command registration ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void registerCommand(Class<?> commandClass) {
        try {
            var name = commandClass.getSimpleName();
            var sectionName = name.charAt(0) + name.substring(1) + "Section";
            var configFile = name.charAt(0) + name.substring(1) + ".yml";
            var pkg = commandClass.getPackageName();

            var sectionClass = (Class<? extends CommandSection>) Class.forName(pkg + "." + sectionName);
            var section = loadSection(configFile, sectionClass, name);

            if (section instanceof PermissionParentProvider provider) {
                try {
                    PermissionHierarchyRegistrar.register(loadedPlugin, section.getPermissions(), provider.getPermissionParents());
                } catch (Exception e) {
                    LOG.warn("Could not register permission hierarchy for: {}", commandClass.getName(), e);
                }
            }

            var command = instantiate(commandClass, section);
            if (command == null) return;

            RegisteredCommand registered = commandRegistrar.register(loadedPlugin, command, AliasCollisionPolicy.PREFIX);
            if (!registered.collisions().isEmpty()) {
                registered.collisions().forEach(collision ->
                        LOG.warn("Alias collision on /{}: owned by '{}', policy={} (command={})",
                                collision.alias(), collision.existingOwner(),
                                collision.policy(), command.getName())
                );
            }

        } catch (Exception e) {
            LOG.warn("Could not register command: {}", commandClass.getName(), e);
        }
    }

    private @Nullable BukkitCommand instantiate(Class<?> commandClass, CommandSection section) {
        try {
            Constructor<?> best = null;
            Object secondArg = null;

            for (var ctor : commandClass.getDeclaredConstructors()) {
                var params = ctor.getParameterTypes();
                if (params.length != 2 || !params[0].isAssignableFrom(section.getClass())) continue;

                var secondType = params[1];

                // Priority 1-2: context object (exact match, then polymorphic)
                if (contextObject != null && secondType.equals(contextObject.getClass())) {
                    return (BukkitCommand) ctor.newInstance(section, contextObject);
                }
                if (contextObject != null && secondType.isAssignableFrom(contextObject.getClass())) {
                    return (BukkitCommand) ctor.newInstance(section, contextObject);
                }

                // Priority 3-4: plugin (direct, then superclass hierarchy)
                if (secondType.isAssignableFrom(loadedPlugin.getClass())) {
                    best = ctor; secondArg = loadedPlugin; continue;
                }
                if (isAssignableFromSuperclass(secondType, loadedPlugin.getClass())) {
                    best = ctor; secondArg = loadedPlugin;
                }
            }

            if (best != null) return (BukkitCommand) best.newInstance(section, secondArg);

            LOG.warn("No compatible constructor for command: {} (needs CommandSection + JavaPlugin or context object)",
                    commandClass.getName());
            return null;

        } catch (Exception e) {
            LOG.warn("Could not instantiate command: {}", commandClass.getName(), e);
            return null;
        }
    }

    private CommandSection loadSection(String configFile, Class<? extends CommandSection> sectionClass, String name) throws Exception {
        var cfgManager = new ConfigManager(loadedPlugin, COMMANDS_FOLDER);
        cfgManager.loadConfig(configFile.toLowerCase());
        var cfgKeeper = new ConfigKeeper<>(cfgManager, configFile.toLowerCase(), sectionClass);
        return cfgKeeper.mapSection(COMMANDS_FOLDER + "." + name.toLowerCase());
    }

    private boolean isAssignableFromSuperclass(Class<?> target, Class<?> source) {
        for (var current = source.getSuperclass(); current != null; current = current.getSuperclass()) {
            if (target.isAssignableFrom(current)) return true;
        }
        return false;
    }

    // ── Listener registration ─────────────────────────────────────────────────

    private void registerListener(Class<?> listenerClass) {
        try {
            if (!Listener.class.isAssignableFrom(listenerClass)) return;

            Constructor<?> best = null;
            Object arg = null;

            for (var ctor : listenerClass.getDeclaredConstructors()) {
                var params = ctor.getParameterTypes();
                if (params.length != 1) continue;

                var paramType = params[0];

                if (contextObject != null && paramType.equals(contextObject.getClass())) {
                    best = ctor; arg = contextObject; break;
                }
                if (contextObject != null && paramType.isAssignableFrom(contextObject.getClass())) {
                    best = ctor; arg = contextObject; break;
                }
                if (paramType.isAssignableFrom(loadedPlugin.getClass())) {
                    best = ctor; arg = loadedPlugin;
                }
            }

            if (best != null) {
                var listener = (Listener) best.newInstance(arg);
                loadedPlugin.getServer().getPluginManager().registerEvents(listener, loadedPlugin);
            } else {
                LOG.warn("No compatible constructor for listener: {}", listenerClass.getName());
            }

        } catch (Exception e) {
            LOG.warn("Could not register listener: {}", listenerClass.getName(), e);
        }
    }
}
