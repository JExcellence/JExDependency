package com.raindropcentral.rplatform.logging;

import com.raindropcentral.rplatform.config.LoggerSection;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LoggerConfig encapsulates runtime configuration flags and per-package overrides consumed by
 * {@link CentralLogger}'s universal handler. Instances are loaded from {@code logs/logging.yml}
 * using the shared configuration infrastructure and then consulted whenever a
 * {@link PlatformLogger} is created or a JUL record is processed.
 *
 * <p>Default values mirror prior behaviour: console logging enabled, debug disabled, and common
 * package namespaces pinned to warning levels.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class LoggerConfig {

    /**
     * Indicates whether console mirroring should be active for {@link CentralLogger}.
     */
    private final boolean consoleEnabled;

    /**
     * Global debug flag gating {@link PlatformLogger#debug(String)} and trace level logging.
     */
    private final boolean debugEnabled;

    /**
     * Baseline severity applied when no package-specific rule is defined.
     */
    private final LogLevel defaultLevel;
    // For now, keep consoleLevel and fileLevel aligned with defaultLevel to match prior behavior.

    /**
     * Console severity derived from {@link #defaultLevel} for backward compatibility.
     */
    private final LogLevel consoleLevel;

    /**
     * File severity derived from {@link #defaultLevel} for backward compatibility.
     */
    private final LogLevel fileLevel;

    /**
     * Immutable map of package prefixes to explicit {@link LogLevel} overrides.
     */
    private final Map<String, LogLevel> packageLevels;

    private LoggerConfig(boolean consoleEnabled,
                         boolean debugEnabled,
                         LogLevel defaultLevel,
                         Map<String, LogLevel> packageLevels) {
        this.consoleEnabled = consoleEnabled;
        this.debugEnabled = debugEnabled;
        this.defaultLevel = defaultLevel != null ? defaultLevel : LogLevel.INFO;
        this.consoleLevel = this.defaultLevel;
        this.fileLevel = this.defaultLevel;
        this.packageLevels = Collections.unmodifiableMap(new HashMap<>(packageLevels));
    }

    /**
     * Indicates whether console logging is enabled for the current configuration snapshot.
     *
     * @return {@code true} when {@link CentralLogger} should emit to the console handler
     */
    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    /**
     * Reports whether debug and trace severity helpers should emit log records.
     *
     * @return {@code true} when {@link PlatformLogger#debug(String)} and trace logging should fire
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Returns the console severity derived from the default level.
     *
     * @return console {@link LogLevel}
     */
    public @NotNull LogLevel getConsoleLevel() {
        return consoleLevel;
    }

    /**
     * Returns the file severity derived from the default level.
     *
     * @return file {@link LogLevel}
     */
    public @NotNull LogLevel getFileLevel() {
        return fileLevel;
    }

    /**
     * Resolves the effective log level for the provided logger name using a longest-prefix lookup
     * across configured package overrides before falling back to {@link #defaultLevel}.
     *
     * @param loggerName name of the logger being evaluated
     * @return matching {@link LogLevel} for the logger name
     */
    public @NotNull LogLevel getLevelForPackage(final String loggerName) {
        if (loggerName == null || loggerName.isEmpty() || packageLevels.isEmpty()) {
            return defaultLevel;
        }
        String bestKey = null;
        for (String key : packageLevels.keySet()) {
            if (loggerName.startsWith(key)) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        if (bestKey != null) {
            return packageLevels.getOrDefault(bestKey, defaultLevel);
        }
        return defaultLevel;
    }

    /**
     * Loads configuration values from {@code logs/logging.yml} using {@link ConfigManager} and
     * {@link ConfigKeeper}. Safe defaults are applied when the file is absent or invalid, ensuring
     * {@link CentralLogger} can continue routing records.
     *
     * @param plugin plugin requesting the configuration; used to resolve the data directory
     * @return a populated {@link LoggerConfig} snapshot
     */
    public static @NotNull LoggerConfig load(@NotNull final JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        // Defaults consistent with your previous implementation
        boolean console = true;
        boolean debug = false;
        LogLevel defaultLvl = LogLevel.INFO;
        Map<String, LogLevel> pkg = defaultPackageDefaults();

        try {
            final ConfigManager cfgManager = new ConfigManager(plugin, "logs");
            final ConfigKeeper<LoggerSection> cfgKeeper = new ConfigKeeper<>(cfgManager, "logging.yml", LoggerSection.class);
            final LoggerSection section = cfgKeeper.rootSection;

            if (section != null) {
                console = section.isConsoleLogging();
                debug = section.isDebugMode();
                try {
                    defaultLvl = LogLevel.fromString(section.getDefaultLevel());
                } catch (Exception ignored) {
                    defaultLvl = LogLevel.INFO;
                }

                final Map<String, String> configured = section.getLoggers();
                if (configured != null && !configured.isEmpty()) {
                    Map<String, LogLevel> merged = new HashMap<>(pkg);
                    for (Map.Entry<String, String> e : configured.entrySet()) {
                        if (e.getKey() == null || e.getKey().isEmpty() || e.getValue() == null) continue;
                        merged.put(e.getKey(), LogLevel.fromString(e.getValue()));
                    }
                    pkg = merged;
                }
            } else {
                // No section? ensure file exists with defaults
                ensureDefaultConfigFile(plugin);
            }
        } catch (Exception e) {
            System.err.println("[LOGGER WARN] Failed to load LoggerConfig: " + e.getMessage());
            // Attempt to ensure default config exists for future runs
            try {
                ensureDefaultConfigFile(plugin);
            } catch (Exception ignored) {
            }
        }

        return new LoggerConfig(console, debug, defaultLvl, pkg);
    }

    private static Map<String, LogLevel> defaultPackageDefaults() {
        Map<String, LogLevel> map = new HashMap<>();
        map.put("com.raindropcentral", LogLevel.WARNING);
        map.put("de.jexcellence", LogLevel.WARNING);
        map.put("me.devnatan.inventoryframework", LogLevel.WARNING);
        map.put("org.bukkit", LogLevel.INFO);
        map.put("net.minecraft", LogLevel.WARNING);
        map.put("org.hibernate", LogLevel.WARNING);
        map.put("org.apache", LogLevel.WARNING);
        map.put("org.springframework", LogLevel.WARNING);
        return map;
    }

    private static void ensureDefaultConfigFile(@NotNull final JavaPlugin plugin) throws Exception {
        final File dir = new File(plugin.getDataFolder(), "logs");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        final File file = new File(dir, "logging.yml");
        if (file.exists()) return;

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("defaultLevel: INFO");
            writer.println("consoleLogging: true");
            writer.println("debugMode: false");
            writer.println("loggers:");
            writer.println("  com.raindropcentral: WARNING");
            writer.println("  de.jexcellence: WARNING");
            writer.println("  me.devnatan.inventoryframework: WARNING");
            writer.println("  org.bukkit: INFO");
            writer.println("  net.minecraft: WARNING");
            writer.println("  org.hibernate: WARNING");
            writer.println("  org.apache: WARNING");
            writer.println("  org.springframework: WARNING");
        }
    }
}