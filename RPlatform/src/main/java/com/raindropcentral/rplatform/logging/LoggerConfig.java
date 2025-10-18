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
 * LoggerConfig represents the runtime logging configuration and provides a loader
 * that uses the existing ConfigManager/ConfigKeeper + LoggerSection.
 *
 * Structure expected in logs/logging.yml:
 *   defaultLevel: INFO
 *   consoleLogging: true
 *   debugMode: false
 *   loggers:
 *     com.raindropcentral: WARNING
 *     de.jexcellence: WARNING
 *     me.devnatan.inventoryframework: WARNING
 *     org.bukkit: INFO
 *     net.minecraft: WARNING
 *     org.hibernate: WARNING
 *     org.apache: WARNING
 *     org.springframework: WARNING
 */
public class LoggerConfig {

    private final boolean consoleEnabled;
    private final boolean debugEnabled;
    private final LogLevel defaultLevel;
    // For now, keep consoleLevel and fileLevel aligned with defaultLevel to match prior behavior.
    private final LogLevel consoleLevel;
    private final LogLevel fileLevel;
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

    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public @NotNull LogLevel getConsoleLevel() {
        return consoleLevel;
    }

    public @NotNull LogLevel getFileLevel() {
        return fileLevel;
    }

    /**
     * Resolve the effective log level for a given logger name using longest-prefix match
     * against configured package levels; falls back to defaultLevel.
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
     * Load configuration from logs/logging.yml using ConfigManager/ConfigKeeper and LoggerSection.
     * Provides safe defaults if loading fails; optionally attempts to create a default file.
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