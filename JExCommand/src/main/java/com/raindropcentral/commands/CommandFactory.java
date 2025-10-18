package com.raindropcentral.commands;

import com.google.common.reflect.ClassPath;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.configmapper.ConfigMapper;
import de.jexcellence.evaluable.CommandUpdater;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.evaluable.section.ACommandSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.logging.Level;

/**
 * Generic CommandFactory that can register commands and listeners for any JavaPlugin.
 * Supports passing custom context objects (like RDQ, RDQFree, RDQPremium) to commands and listeners.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@SuppressWarnings("unchecked")
public class CommandFactory {

    /**
     * Root folder that stores command configuration files under the plugin's data directory.
     */
    private static final String COMMANDS_FOLDER = "commands";
    /**
     * Configuration key that contains permission metadata within each command section.
     */
    private static final String PERMISSIONS_SECTION = "permissions";

    /**
     * Plugin whose class loader is scanned for command and listener classes.
     */
    private final JavaPlugin loadedPlugin;
    /**
     * Optional context object forwarded to handlers requiring additional state (for example RDQ variants).
     */
    private final Object contextObject; // Can be RDQ, RDQFree, RDQPremium, or any other context
    /**
     * Synchronizes Paper/Bukkit's command map with the generated command instances.
     */
    private final CommandUpdater commandUpdater;

    /**
     * Constructor for CommandFactory without a context object (legacy support).
     *
     * @param loadedPlugin the JavaPlugin instance being bootstrapped
     */
    public CommandFactory(final @NotNull JavaPlugin loadedPlugin) {
        this(loadedPlugin, null);
    }

    /**
     * Constructor for CommandFactory with an optional context object.
     * The context object is forwarded to commands or listeners when their constructors declare it so.
     *
     * @param loadedPlugin  the JavaPlugin instance being bootstrapped
     * @param contextObject optional context object (e.g., RDQ instance) to pass to commands/listeners
     */
    public CommandFactory(
            final @NotNull JavaPlugin loadedPlugin,
            final @Nullable Object contextObject
    ) {
        this.loadedPlugin = loadedPlugin;
        this.contextObject = contextObject;
        this.commandUpdater = new CommandUpdater(this.loadedPlugin);
    }

    /**
     * Scans the plugin classpath for command and listener implementations annotated with
     * {@link com.raindropcentral.commands.utility.Command} and registers them.
     * <p>
     * The discovery process performs two passes:
     * <ul>
     *     <li>Command classes under {@code *command*} packages are instantiated via {@link #registerCommand(Class)}.</li>
     *     <li>Listener classes under {@code *listener*} packages are instantiated via {@link #registerListener(Class)}.</li>
     * </ul>
     * Any exception during discovery is logged and does not halt plugin enablement.
     */
    public void registerAllCommandsAndListeners() {
        try {
            ClassPath.from(this.loadedPlugin.getClass().getClassLoader())
                    .getAllClasses().stream()
                    .filter(classInfo -> classInfo.getPackageName().contains("command"))
                    .map(classInfo -> {
                        try {
                            return classInfo.load();
                        } catch (final Exception ignored) {
                            // may be thrown through remapping / relocation
                            // example h2 driver
                            return null;
                        }
                    })
                    .filter(clazz -> clazz != null && clazz.isAnnotationPresent(Command.class))
                    .forEach(this::registerCommand);

            ClassPath.from(this.loadedPlugin.getClass().getClassLoader())
                    .getAllClasses().stream()
                    .filter(classInfo -> classInfo.getPackageName().contains("listener"))
                    .map(ClassPath.ClassInfo::load)
                    .forEach(clazz -> {
                        if (Listener.class.isAssignableFrom(clazz)) {
                            registerListener(clazz);
                        }
                    });
        } catch (final Exception exception) {
            this.loadedPlugin.getLogger().log(
                    Level.WARNING,
                    "Could not register commands or listeners",
                    exception
            );
        }
    }

    /**
     * Instantiates a command class by locating its configuration section, selecting the most specific
     * constructor, and delegating to the {@link CommandUpdater} for registration.
     * <p>
     * Constructor selection follows these precedence rules:
     * <ol>
     *     <li>Exact match with the configured context object's class.</li>
     *     <li>Constructor whose parameter type is assignable from the context object's class.</li>
     *     <li>Constructor accepting the concrete {@link JavaPlugin} implementation.</li>
     *     <li>Constructor whose second parameter matches any superclass of the plugin (checked via
     *     {@link #isAssignableFromSuperclass(Class, Class)}).</li>
     * </ol>
     *
     * @param commandClass discovered command implementation annotated with {@link Command}
     */
    private void registerCommand(Class<?> commandClass) {
        try {
            String className = commandClass.getSimpleName();
            char prefix = className.charAt(0);
            String sectionClassName = prefix + className.substring(1) + "Section";
            String configFileName = prefix + className.substring(1) + ".yml";
            String packageName = commandClass.getPackageName();

            Class<? extends ACommandSection> sectionClass = (Class<? extends ACommandSection>) Class.forName(packageName + "." + sectionClassName);

            final ACommandSection mapSection = getACommandSection(configFileName, sectionClass, className);

            BukkitCommand command = null;

            try {
                Constructor<?> selectedConstructor = null;
                Object secondArg = null;

                // Try to find a compatible constructor
                for (final Constructor<?> ctor : commandClass.getDeclaredConstructors()) {
                    Class<?>[] paramTypes = ctor.getParameterTypes();

                    // Must have 2 parameters, first must be the section
                    if (paramTypes.length != 2 || !paramTypes[0].isAssignableFrom(mapSection.getClass())) {
                        continue;
                    }

                    Class<?> secondParamType = paramTypes[1];

                    // Priority 1: Exact match with context object's class
                    if (contextObject != null && secondParamType.equals(contextObject.getClass())) {
                        selectedConstructor = ctor;
                        secondArg = contextObject;
                        break;
                    }

                    // Priority 2: Context object is assignable to parameter type (polymorphism)
                    // e.g., RDQFree/RDQPremium can be passed to Constructor(Section, RDQ)
                    if (contextObject != null && secondParamType.isAssignableFrom(contextObject.getClass())) {
                        selectedConstructor = ctor;
                        secondArg = contextObject;
                        break;
                    }

                    // Priority 3: JavaPlugin constructor (direct match)
                    if (secondParamType.isAssignableFrom(this.loadedPlugin.getClass())) {
                        selectedConstructor = ctor;
                        secondArg = this.loadedPlugin;
                        // Don't break - keep looking for better match
                        continue;
                    }

                    // Priority 4: Check superclass hierarchy safely
                    if (isAssignableFromSuperclass(secondParamType, this.loadedPlugin.getClass())) {
                        selectedConstructor = ctor;
                        secondArg = this.loadedPlugin;
                        // Don't break - keep looking for better match
                    }
                }

                if (selectedConstructor != null) {
                    command = (BukkitCommand) selectedConstructor.newInstance(mapSection, secondArg);
                    this.loadedPlugin.getLogger().info(
                            "Registered command: " + commandClass.getSimpleName()
                                    + " with " + secondArg.getClass().getSimpleName()
                    );
                } else {
                    this.loadedPlugin.getLogger().log(
                            Level.WARNING,
                            "No compatible constructor found for command: " + commandClass.getName()
                                    + " (requires constructor with ACommandSection and JavaPlugin or context object)"
                    );
                    return;
                }
            } catch (final Exception exception) {
                this.loadedPlugin.getLogger().log(
                        Level.WARNING,
                        "Could not register command '" + commandClass.getName() + "'",
                        exception
                );
                return;
            }

            if (className.equalsIgnoreCase("pr18n")) {
                command.setName(this.loadedPlugin.getName().toLowerCase() + ":pr18n");
                command.getAliases().add(this.loadedPlugin.getName().toLowerCase() + "reloadI18n");
            }

            this.commandUpdater.tryRegisterCommand(command);
            this.commandUpdater.trySyncCommands();

        } catch (final Exception exception) {
            this.loadedPlugin.getLogger().log(
                    Level.WARNING,
                    "Could not register command: " + commandClass.getName(),
                    exception
            );
        }
    }

    /**
     * Loads a command configuration section, ensuring the YAML file and section type follow the
     * naming conventions documented in {@code AGENTS.md}.
     *
     * @param configFileName expected YAML file name (e.g. {@code Example.yml})
     * @param sectionClass   configuration section type corresponding to the command
     * @param className      command simple name used to compute the configuration path
     * @return hydrated command section instance ready for use in the command constructor
     * @throws Exception when the configuration cannot be loaded or mapped to the section class
     */
    private ACommandSection getACommandSection(String configFileName, Class<? extends ACommandSection> sectionClass, String className) throws Exception {
        final ConfigManager cfgManager = new ConfigManager(
                this.loadedPlugin,
                COMMANDS_FOLDER
        );
        final ConfigMapper cfgMapper = cfgManager.loadConfig(configFileName.toLowerCase());
        final ConfigKeeper<? extends ACommandSection> cfgKeeper = new ConfigKeeper<>(
                cfgManager,
                configFileName.toLowerCase(),
                sectionClass
        );
        return cfgKeeper.mapSection(
                COMMANDS_FOLDER + "." + className.toLowerCase()
        );
    }

    /**
     * Safely checks whether {@code targetType} is assignable from any superclass of {@code sourceClass}.
     * The helper is used when evaluating constructor compatibility against the plugin type while
     * guarding against {@code null} superclasses.
     *
     * @param targetType  type being matched (typically the constructor parameter)
     * @param sourceClass class whose hierarchy should be inspected
     * @return {@code true} when the target type matches any superclass, otherwise {@code false}
     */
    private boolean isAssignableFromSuperclass(Class<?> targetType, Class<?> sourceClass) {
        Class<?> current = sourceClass.getSuperclass();
        while (current != null) {
            if (targetType.isAssignableFrom(current)) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    /**
     * Instantiates a listener class and registers it with Bukkit's {@link org.bukkit.event.PluginManager}.
     * Constructor precedence mirrors command registration but for single-argument constructors:
     * <ol>
     *     <li>Exact match with the context object's class.</li>
     *     <li>Constructor whose parameter type is assignable from the context object's class.</li>
     *     <li>Constructor receiving the concrete {@link JavaPlugin} implementation.</li>
     * </ol>
     *
     * @param listenerClass discovered listener implementation under a {@code *listener*} package
     */
    private void registerListener(final @NotNull Class<?> listenerClass) {
        try {
            if (!Listener.class.isAssignableFrom(listenerClass)) {
                return;
            }

            Listener listener = null;
            Constructor<?> selectedConstructor = null;
            Object constructorArg = null;

            // Try to find a compatible constructor
            for (final Constructor<?> ctor : listenerClass.getDeclaredConstructors()) {
                Class<?>[] paramTypes = ctor.getParameterTypes();

                // Skip if not single-parameter constructor
                if (paramTypes.length != 1) {
                    continue;
                }

                Class<?> paramType = paramTypes[0];

                // Priority 1: Exact match with context object's class
                if (contextObject != null && paramType.equals(contextObject.getClass())) {
                    selectedConstructor = ctor;
                    constructorArg = contextObject;
                    break;
                }

                // Priority 2: Context object is assignable to parameter type (polymorphism)
                // e.g., RDQFree/RDQPremium can be passed to Constructor(RDQ)
                if (contextObject != null && paramType.isAssignableFrom(contextObject.getClass())) {
                    selectedConstructor = ctor;
                    constructorArg = contextObject;
                    break;
                }

                // Priority 3: JavaPlugin constructor (legacy)
                if (paramType.isAssignableFrom(this.loadedPlugin.getClass())) {
                    selectedConstructor = ctor;
                    constructorArg = this.loadedPlugin;
                    // Don't break - keep looking for better match
                }
            }

            if (selectedConstructor != null) {
                listener = (Listener) selectedConstructor.newInstance(constructorArg);
                this.loadedPlugin.getServer().getPluginManager().registerEvents(
                        listener,
                        this.loadedPlugin
                );
                this.loadedPlugin.getLogger().info(
                        "Registered listener: " + listenerClass.getSimpleName()
                                + " with " + constructorArg.getClass().getSimpleName()
                );
            } else {
                this.loadedPlugin.getLogger().log(
                        Level.WARNING,
                        "No compatible constructor found for listener: " + listenerClass.getName()
                                + " (requires JavaPlugin or context object)"
                );
            }

        } catch (final Exception exception) {
            this.loadedPlugin.getLogger().log(
                    Level.WARNING,
                    "Could not register listener: " + listenerClass.getName(),
                    exception
            );
        }
    }

    /**
     * Returns the plugin instance being scanned by the factory.
     *
     * @return plugin provided during construction
     */
    @NotNull
    public JavaPlugin getLoadedPlugin() {
        return this.loadedPlugin;
    }

    /**
     * Returns the optional context object forwarded to commands and listeners.
     *
     * @return configured context object or {@code null} when none was supplied
     */
    @Nullable
    public Object getContextObject() {
        return this.contextObject;
    }
}