package com.raindropcentral.rplatform.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class PlatformLogger {

    private final Logger logger;
    private final LoggerConfig config;

    private PlatformLogger(final @NotNull JavaPlugin plugin) {
        // Ensure CentralLogger is initialized in your plugin's onEnable before creating this
        this.logger = CentralLogger.getLogger(plugin.getName());
        this.config = LoggerConfig.load(plugin);

        // Respect package-level config immediately for this logger
        final LogLevel lvl = config.getLevelForPackage(plugin.getClass().getPackage().getName());
        this.logger.setLevel(lvl.toJavaLevel());
        this.logger.setUseParentHandlers(true);
    }

    public static @NotNull PlatformLogger create(final @NotNull JavaPlugin plugin) {
        return new PlatformLogger(plugin);
    }

    public void critical(final @NotNull String message) {
        logger.log(LogLevel.CRITICAL.toJavaLevel(), message);
    }

    public void critical(final @NotNull String message, final @NotNull Throwable throwable) {
        logger.log(LogLevel.CRITICAL.toJavaLevel(), message, throwable);
    }

    public void error(final @NotNull String message) {
        logger.log(LogLevel.ERROR.toJavaLevel(), message);
    }

    public void error(final @NotNull String message, final @NotNull Throwable throwable) {
        logger.log(LogLevel.ERROR.toJavaLevel(), message, throwable);
    }

    public void warn(final @NotNull String message) {
        logger.log(LogLevel.WARNING.toJavaLevel(), message);
    }

    public void info(final @NotNull String message) {
        logger.log(LogLevel.INFO.toJavaLevel(), message);
    }

    public void debug(final @NotNull String message) {
        if (config.isDebugEnabled()) {
            logger.log(LogLevel.DEBUG.toJavaLevel(), message);
        }
    }

    public void trace(final @NotNull String message) {
        if (config.isDebugEnabled()) {
            logger.log(LogLevel.TRACE.toJavaLevel(), message);
        }
    }

    public void setConsoleEnabled(final boolean enabled) {
        CentralLogger.setConsoleLoggingEnabled(enabled);
    }

    public boolean isConsoleEnabled() {
        return CentralLogger.isConsoleLoggingEnabled();
    }

    public @NotNull Logger getJavaLogger() {
        return logger;
    }

    public void close() {
        // no-op
    }
}