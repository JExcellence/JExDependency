package com.raindropcentral.rplatform.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@CSAlways
public class LoggerSection extends AConfigSection {

    private String defaultLevel;
    private Map<String, String> loggers;
    private Boolean debugMode;
    private Boolean consoleLogging;

    public LoggerSection(final @NotNull EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }

    public @NotNull String getDefaultLevel() {
        return this.defaultLevel == null ? "INFO" : this.defaultLevel;
    }

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

    public boolean isDebugMode() {
        return this.debugMode != null && this.debugMode;
    }

    public boolean isConsoleLogging() {
        return this.consoleLogging != null && this.consoleLogging;
    }
}
