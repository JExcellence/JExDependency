package com.raindropcentral.rplatform.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section that describes how the platform bootstraps logging categories.
 *
 * <p>It allows administrators to control the default log level, override categories, and toggle
 * auxiliary logging behaviours such as debug mode or console forwarding. The values are read during
 * platform start-up and applied to the plugin logger hierarchy.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@CSAlways
public class LoggerSection extends AConfigSection {

    /**
     * Root level applied to the plugin logger when a category-specific override is not provided.
     * Maps the YAML key {@code defaultLevel}.
     */
    private String defaultLevel;

    /**
     * Map of logger names to explicit levels, mirroring the {@code loggers:} block in configuration.
     */
    private Map<String, String> loggers;

    /**
     * Flag that enables verbose debug-level logging when set to {@code true}.
     */
    private Boolean debugMode;

    /**
     * Flag that determines whether plugin logs should also be echoed to the console appender.
     */
    private Boolean consoleLogging;

    /**
     * Creates the logging configuration section using the supplied evaluation environment so that.
     * any embedded expressions resolve consistently with other sections.
     *
     * @param baseEnvironment environment builder backing the config mapper
     */
    public LoggerSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    /**
     * Resolves the default log level. When no value is configured the platform falls back to.
     * {@code INFO} to avoid excessive logging noise.
     *
     * @return configured default level or {@code INFO}
     */
    public @NotNull String getDefaultLevel() {
        return this.defaultLevel == null ? "INFO" : this.defaultLevel;
    }

    /**
     * Returns a copy of the logger override map so callers can mutate the result without affecting.
     * the underlying configuration state.
     *
     * @return map of logger categories to level strings; defaults to recommended platform settings
     */
    public @NotNull Map<String, String> getLoggers() {
        return this.loggers == null ? new HashMap<>(Map.of(
                "com.raindropcentral", "ALL",
                "de.jexcellence", "INFO",
                "me.devnatan.inventoryframework", "WARNING",
                "org.bukkit", "INFO",
                "org.hibernate", "WARNING",
                "net.minecraft", "WARNING"
        )) : this.loggers;
    }

    /**
     * Indicates whether debug output should be enabled.
     *
     * @return {@code true} when debug mode is explicitly enabled
     */
    public boolean isDebugMode() {
        return this.debugMode != null && this.debugMode;
    }

    /**
     * Indicates whether console logging is enabled.
     *
     * @return {@code true} when console mirroring is requested in configuration
     */
    public boolean isConsoleLogging() {
        return this.consoleLogging != null && this.consoleLogging;
    }
}
