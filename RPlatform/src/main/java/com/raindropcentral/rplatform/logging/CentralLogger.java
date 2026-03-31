/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.logging;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * CentralLogger is the main facade for the RPlatform centralized logging system.
 * It provides clean, plugin-specific logging with minimal console spam and automatic file rotation.
 * 
 * <p>Usage example:
 * <pre>
 * PluginLogger logger = Logger.getLogger(this);
 * logger.info("Plugin initialized");
 * logger.warning("Configuration issue detected");
 * logger.severe("Critical error", exception);
 * </pre>
 * 
 * <p>Features:
 * <ul>
 *   <li>Per-plugin logger instances with independent configuration</li>
 *   <li>Automatic 2-file log rotation (current + backup)</li>
 *   <li>Minimal console output (WARNING+ by default)</li>
 *   <li>Comprehensive file logging (all levels)</li>
 *   <li>Recursion protection to prevent StackOverflowError</li>
 *   <li>Safe System.out/err redirection</li>
 * </ul>
 * 
 * @see PluginLogger
 */
public final class CentralLogger {

    // Plugin logger registry - one logger per plugin
    private static final Map<String, PluginLogger> PLUGIN_LOGGERS = new ConcurrentHashMap<>();
    
    // Original system streams for emergency fallback
    private static final PrintStream ORIGINAL_OUT = System.out;
    private static final PrintStream ORIGINAL_ERR = System.err;
    
    // Global state
    private static volatile boolean SYSTEM_STREAMS_REDIRECTED = false;
    private static volatile Level GLOBAL_CONSOLE_LEVEL = Level.WARNING;
    
    // Private constructor - static facade only
    private CentralLogger() {
        throw new UnsupportedOperationException("CentralLogger is a static facade and cannot be instantiated");
    }
    
    /**
     * Gets or creates a logger instance for the specified plugin.
     * This method is thread-safe and will return the same instance for subsequent calls with the same plugin.
     * 
     * <p>The logger is automatically initialized with:
     * <ul>
     *   <li>File handler writing to plugins/{PluginName}/logs/</li>
     *   <li>Console handler with WARNING+ level by default</li>
     *   <li>Recursion protection enabled</li>
     *   <li>UTF-8 encoding</li>
     * </ul>
     * 
     * @param plugin the plugin requesting a logger instance
     * @return a configured PluginLogger instance for the plugin
     * @throws NullPointerException if plugin is null
     */
    @NotNull
    public static PluginLogger getLogger(@NotNull JavaPlugin plugin) {
        if (plugin == null) {
            throw new NullPointerException("Plugin cannot be null");
        }
        
        String pluginName = plugin.getName();
        
        // Return existing logger if already created
        PluginLogger existing = PLUGIN_LOGGERS.get(pluginName);
        if (existing != null) {
            return existing;
        }
        
        // Create new logger instance (synchronized to prevent duplicate creation)
        synchronized (PLUGIN_LOGGERS) {
            // Double-check after acquiring lock
            existing = PLUGIN_LOGGERS.get(pluginName);
            if (existing != null) {
                return existing;
            }
            
            // Create and register new logger
            PluginLogger newLogger = new PluginLogger(plugin);
            PLUGIN_LOGGERS.put(pluginName, newLogger);
            
            return newLogger;
        }
    }
    
    /**
     * Gets an existing logger by plugin name, or returns a fallback Java logger if not found.
     * This method is useful for classes that don't have direct access to the plugin instance.
     * 
     * <p>Note: This method will NOT create a new PluginLogger if one doesn't exist.
     * It returns a standard Java logger as a fallback. For proper initialization,
     * use {@link #getLogger(JavaPlugin)} from the plugin's main class first.
     * 
     * @param pluginName the name of the plugin
     * @return an existing PluginLogger, or a fallback Java logger
     */
    @NotNull
    public static java.util.logging.Logger getLoggerByName(@NotNull String pluginName) {
        if (pluginName == null) {
            throw new NullPointerException("Plugin name cannot be null");
        }
        
        PluginLogger existing = PLUGIN_LOGGERS.get(pluginName);
        if (existing != null) {
            return existing.getJavaLogger();
        }
        
        // Fallback to standard Java logger if PluginLogger not initialized
        return java.util.logging.Logger.getLogger(pluginName);
    }
    
    /**
     * Sets the global console log level for all plugin loggers.
     * This affects what messages are written to the server console.
     * 
     * <p>Common levels:
     * <ul>
     *   <li>Level.SEVERE - Only critical errors</li>
     *   <li>Level.WARNING - Warnings and errors (default)</li>
     *   <li>Level.INFO - Informational messages and above</li>
     *   <li>Level.ALL - Everything including debug</li>
     * </ul>
     * 
     * @param level the minimum level for console output
     */
    public static void setGlobalConsoleLevel(@NotNull Level level) {
        if (level == null) {
            throw new NullPointerException("Level cannot be null");
        }
        
        GLOBAL_CONSOLE_LEVEL = level;
        
        // Update all existing loggers
        for (PluginLogger logger : PLUGIN_LOGGERS.values()) {
            logger.setConsoleLevel(level);
        }
    }
    
    /**
     * Gets the current global console log level.
     * 
     * @return the current global console level
     */
    @NotNull
    public static Level getGlobalConsoleLevel() {
        return GLOBAL_CONSOLE_LEVEL;
    }
    
    /**
     * Shuts down the logging system, closing all plugin loggers and restoring system streams.
     * This should be called during plugin disable or server shutdown.
     * 
     * <p>This method:
     * <ul>
     *   <li>Flushes all pending log messages</li>
     *   <li>Closes all file handlers</li>
     *   <li>Removes all loggers from the registry</li>
     *   <li>Restores original System.out and System.err</li>
     * </ul>
     */
    public static synchronized void shutdown() {
        // Close all plugin loggers
        for (PluginLogger logger : PLUGIN_LOGGERS.values()) {
            try {
                logger.close();
            } catch (Exception e) {
                // Use original streams for error reporting during shutdown
                ORIGINAL_ERR.println("[CentralLogger] Error closing logger: " + e.getMessage());
            }
        }
        
        // Clear registry
        PLUGIN_LOGGERS.clear();
        
        // Restore system streams if they were redirected
        if (SYSTEM_STREAMS_REDIRECTED) {
            try {
                System.setOut(ORIGINAL_OUT);
                System.setErr(ORIGINAL_ERR);
                SYSTEM_STREAMS_REDIRECTED = false;
            } catch (Exception e) {
                ORIGINAL_ERR.println("[CentralLogger] Error restoring system streams: " + e.getMessage());
            }
        }
    }
    
    /**
     * Checks if a logger has been initialized for the specified plugin.
     * 
     * @param plugin the plugin to check
     * @return true if a logger exists for this plugin, false otherwise
     */
    public static boolean isInitialized(@NotNull JavaPlugin plugin) {
        if (plugin == null) {
            return false;
        }
        return PLUGIN_LOGGERS.containsKey(plugin.getName());
    }
    
    /**
     * Gets the log file path for the specified plugin.
     * Returns null if the plugin has no logger initialized.
     * 
     * @param plugin the plugin to get the log file path for
     * @return the absolute path to the plugin's current log file, or null if not initialized
     */
    @Nullable
    public static String getLogFilePath(@NotNull JavaPlugin plugin) {
        if (plugin == null) {
            return null;
        }
        
        PluginLogger logger = PLUGIN_LOGGERS.get(plugin.getName());
        if (logger == null) {
            return null;
        }
        
        // Construct the expected log file path
        File logDir = new File(plugin.getDataFolder(), "logs");
        String pluginName = plugin.getName().toLowerCase();
        File currentLog = new File(logDir, pluginName + "-latest.log");
        
        return currentLog.getAbsolutePath();
    }
    
    /**
     * Gets the original System.out stream before any redirection.
     * This is useful for emergency fallback scenarios.
     * 
     * @return the original System.out PrintStream
     */
    @NotNull
    static PrintStream getOriginalOut() {
        return ORIGINAL_OUT;
    }
    
    /**
     * Gets the original System.err stream before any redirection.
     * This is useful for emergency fallback scenarios.
     * 
     * @return the original System.err PrintStream
     */
    @NotNull
    static PrintStream getOriginalErr() {
        return ORIGINAL_ERR;
    }
    
    /**
     * Checks if system streams have been redirected to the logging system.
     * 
     * @return true if System.out/err are redirected, false otherwise
     */
    static boolean areSystemStreamsRedirected() {
        return SYSTEM_STREAMS_REDIRECTED;
    }
    
    /**
     * Marks system streams as redirected.
     * This is called internally by the logging system.
     */
    static void markSystemStreamsRedirected() {
        SYSTEM_STREAMS_REDIRECTED = true;
    }
    
    /**
     * Redirects System.out and System.err to the logging system.
     * This method ensures that all console output goes through the centralized logger,
     * providing consistent formatting and file logging for all output.
     * 
     * <p>Redirection behavior:
     * <ul>
     *   <li>System.out is redirected to INFO level</li>
     *   <li>System.err is redirected to SEVERE level</li>
     *   <li>Recursion protection prevents infinite loops</li>
     *   <li>Automatic fallback to original streams on errors</li>
     *   <li>Redirection happens only once globally</li>
     * </ul>
     * 
     * <p>This method is thread-safe and idempotent - calling it multiple times
     * will only redirect the streams once.
     * 
     * @param defaultLogger the logger to use for redirected output
     * @throws NullPointerException if defaultLogger is null
     */
    public static synchronized void redirectSystemStreams(@NotNull PluginLogger defaultLogger) {
        if (defaultLogger == null) {
            throw new NullPointerException("Default logger cannot be null");
        }
        
        // Only redirect once
        if (SYSTEM_STREAMS_REDIRECTED) {
            return;
        }
        
        try {
            // Create safe logging print streams
            SafeLoggingPrintStream outStream = new SafeLoggingPrintStream(
                defaultLogger, 
                Level.INFO, 
                ORIGINAL_OUT
            );
            
            SafeLoggingPrintStream errStream = new SafeLoggingPrintStream(
                defaultLogger, 
                Level.SEVERE, 
                ORIGINAL_ERR
            );
            
            // Redirect system streams
            System.setOut(outStream);
            System.setErr(errStream);
            
            // Mark as redirected
            SYSTEM_STREAMS_REDIRECTED = true;
            
            // Log success using original stream to avoid recursion during setup
            ORIGINAL_OUT.println("[CentralLogger] System streams redirected to logging system");
            
        } catch (Exception e) {
            // If redirection fails, log error and continue without redirection
            ORIGINAL_ERR.println("[CentralLogger] Failed to redirect system streams: " + e.getMessage());
            e.printStackTrace(ORIGINAL_ERR);
        }
    }
    
    /**
     * Removes a logger from the registry.
     * This is called internally by PluginLogger.close().
     * 
     * @param pluginName the name of the plugin whose logger should be removed
     */
    static void removeLogger(@NotNull String pluginName) {
        if (pluginName != null) {
            PLUGIN_LOGGERS.remove(pluginName);
        }
    }
}
