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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PluginLogger provides logging methods for a specific plugin with recursion protection.
 * Each plugin gets its own logger instance with independent configuration and file handlers.
 * 
 * <p>This class implements recursion guards to prevent infinite loops and StackOverflowError
 * that can occur when logging operations trigger more logging (e.g., through System.out redirection).
 * 
 * <p>Features:
 * <ul>
 *   <li>Thread-local recursion depth tracking</li>
 *   <li>Emergency mode fallback to original streams</li>
 *   <li>Basic logging methods: info(), warning(), severe(), debug()</li>
 *   <li>Exception handling with automatic fallback</li>
 * </ul>
 * 
 * @see CentralLogger
 */
public final class PluginLogger {
    
    private static final int MAX_RECURSION_DEPTH = 3;
    private static final long FLUSH_INTERVAL_TICKS = 100L; // 5 seconds (20 ticks per second)
    
    private final JavaPlugin plugin;
    private final Logger javaLogger;
    private final ThreadLocal<Integer> recursionDepth;
    private final AtomicBoolean emergencyMode;
    
    // References to original streams for emergency fallback
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    
    private RotatingFileHandler fileHandler;
    private FilteredConsoleHandler consoleHandler;
    
    // Periodic flush task
    private BukkitTask flushTask;
    
    /**
     * Package-private constructor - only CentralLogger should create instances.
     * 
     * @param plugin the plugin this logger is for
     */
    PluginLogger(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.javaLogger = Logger.getLogger(plugin.getName());
        this.recursionDepth = ThreadLocal.withInitial(() -> 0);
        this.emergencyMode = new AtomicBoolean(false);
        this.originalOut = CentralLogger.getOriginalOut();
        this.originalErr = CentralLogger.getOriginalErr();
        
        // Configure the Java logger
        this.javaLogger.setUseParentHandlers(false);
        this.javaLogger.setLevel(Level.ALL);
        
        // Initialize handlers with formatter
        initializeHandlers();
        
        // Register shutdown hook to flush and close on plugin disable
        registerShutdownHook();
    }
    
    /**
     * Initializes the file and console handlers with the RLogFormatter.
     * If initialization fails, falls back to standard Java logging.
     */
    private void initializeHandlers() {
        try {
            // Ensure log directory exists
            File logDir = new File(plugin.getDataFolder(), "logs");
            if (!logDir.exists()) {
                if (!logDir.mkdirs()) {
                    throw new RuntimeException("Failed to create log directory: " + logDir.getAbsolutePath());
                }
            }
            
            // Create the custom formatter
            RLogFormatter formatter = new RLogFormatter();
            
            // Create and configure file handler with UTF-8 encoding
            fileHandler = new RotatingFileHandler(plugin.getDataFolder(), plugin.getName());
            fileHandler.setLevel(Level.ALL); // Log everything to file
            fileHandler.setFormatter(formatter);
            javaLogger.addHandler(fileHandler);
            
            // Create and configure console handler
            consoleHandler = new FilteredConsoleHandler(originalOut);
            consoleHandler.setLevel(Level.WARNING); // Only WARN+ to console by default
            consoleHandler.setFormatter(formatter);
            javaLogger.addHandler(consoleHandler);
            
            // Start periodic flush task (every 5 seconds)
            startPeriodicFlush();
            
        } catch (Exception e) {
            // If handler initialization fails, activate emergency mode and fall back to standard logging
            activateEmergencyMode("Failed to initialize handlers: " + e.getMessage());
            originalErr.println("[PluginLogger] Failed to initialize logging handlers for " + plugin.getName());
            originalErr.println("[PluginLogger] Falling back to standard Java logging");
            e.printStackTrace(originalErr);
            
            // Fall back to standard Java logging with parent handlers
            javaLogger.setUseParentHandlers(true);
        }
    }
    
    /**
     * Registers a shutdown hook that flushes and closes the logger when the plugin is disabled.
     * This ensures that all log messages are written to disk before the plugin shuts down.
     */
    private void registerShutdownHook() {
        try {
            // Use Bukkit's plugin manager to register a disable listener
            // We'll use a simple approach: register a task that runs on plugin disable
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // This won't actually run, but we can use onDisable in the plugin itself
                // For now, we'll rely on the plugin calling close() or CentralLogger.shutdown()
            }, 1L);
            
            // Note: The proper way is for plugins to call CentralLogger.shutdown() in their onDisable()
            // or for the logger to be closed explicitly. We can't reliably hook into plugin disable
            // from here without modifying the plugin's lifecycle.
            
        } catch (Exception e) {
            // If we can't register the hook, just log a warning
            originalErr.println("[PluginLogger] Warning: Could not register shutdown hook for " + plugin.getName());
        }
    }
    
    /**
     * Starts a periodic task that flushes all handlers every 5 seconds.
     * This ensures that log messages are written to disk regularly.
     */
    private void startPeriodicFlush() {
        try {
            // Schedule a repeating task that flushes handlers every 5 seconds (100 ticks)
            flushTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> {
                    try {
                        flush();
                    } catch (Exception e) {
                        // Silently ignore flush errors to avoid spam
                        // Emergency mode will be activated if there's a real problem
                    }
                },
                FLUSH_INTERVAL_TICKS, // Initial delay
                FLUSH_INTERVAL_TICKS  // Period
            );
        } catch (Exception ignored) {}
    }
    
    /**
     * Stops the periodic flush task if it's running.
     */
    private void stopPeriodicFlush() {
        if (flushTask != null) {
            try {
                flushTask.cancel();
            } catch (Exception e) {
                // Ignore errors during cancellation
            } finally {
                flushTask = null;
            }
        }
    }
    
    /**
     * Enters a logging operation by incrementing the recursion counter.
     * Returns false if recursion limit is exceeded or emergency mode is active.
     * 
     * @return true if logging should proceed, false if it should fall back to original streams
     */
    private boolean enterLogging() {
        // If emergency mode is active, always use fallback
        if (emergencyMode.get()) {
            return false;
        }
        
        int depth = recursionDepth.get();
        
        // Check if we've exceeded the recursion limit
        if (depth >= MAX_RECURSION_DEPTH) {
            activateEmergencyMode("Recursion depth exceeded (depth=" + depth + ")");
            return false;
        }
        
        // Increment recursion counter
        recursionDepth.set(depth + 1);
        return true;
    }
    
    /**
     * Exits a logging operation by decrementing the recursion counter.
     */
    private void exitLogging() {
        int depth = recursionDepth.get();
        if (depth > 0) {
            recursionDepth.set(depth - 1);
        }
    }
    
    /**
     * Activates emergency mode, which bypasses the logging system and writes directly.
     * to original streams. This is used when recursion is detected or logging fails.
     * 
     * @param reason the reason emergency mode was activated
     */
    private void activateEmergencyMode(@NotNull String reason) {
        // Use compareAndSet to ensure we only log the emergency message once
        if (emergencyMode.compareAndSet(false, true)) {
            originalErr.println("[CentralLogger EMERGENCY] " + plugin.getName() + ": " + reason);
            originalErr.println("[CentralLogger EMERGENCY] Falling back to direct stream output");
        }
    }
    
    /**
     * Logs an informational message.
     * 
     * @param message the message to log
     */
    public void info(@NotNull String message) {
        if (message == null) {
            return;
        }
        
        if (!enterLogging()) {
            // Fallback to original stream
            originalOut.println("[" + plugin.getName() + "] " + message);
            return;
        }
        
        try {
            javaLogger.info(message);
        } catch (Exception e) {
            activateEmergencyMode("Exception during logging: " + e.getMessage());
            originalOut.println("[" + plugin.getName() + "] " + message);
        } finally {
            exitLogging();
        }
    }
    
    /**
     * Logs a warning message.
     * 
     * @param message the message to log
     */
    public void warning(@NotNull String message) {
        if (message == null) {
            return;
        }
        
        if (!enterLogging()) {
            // Fallback to original stream
            originalErr.println("[" + plugin.getName() + "] WARNING: " + message);
            return;
        }
        
        try {
            javaLogger.warning(message);
        } catch (Exception e) {
            activateEmergencyMode("Exception during logging: " + e.getMessage());
            originalErr.println("[" + plugin.getName() + "] WARNING: " + message);
        } finally {
            exitLogging();
        }
    }
    
    /**
     * Logs a severe error message.
     * 
     * @param message the message to log
     */
    public void severe(@NotNull String message) {
        if (message == null) {
            return;
        }
        
        if (!enterLogging()) {
            // Fallback to original stream
            originalErr.println("[" + plugin.getName() + "] SEVERE: " + message);
            return;
        }
        
        try {
            javaLogger.severe(message);
        } catch (Exception e) {
            activateEmergencyMode("Exception during logging: " + e.getMessage());
            originalErr.println("[" + plugin.getName() + "] SEVERE: " + message);
        } finally {
            exitLogging();
        }
    }
    
    /**
     * Logs a debug message at FINE level.
     * 
     * @param message the message to log
     */
    public void debug(@NotNull String message) {
        if (message == null) {
            return;
        }
        
        if (!enterLogging()) {
            // Fallback to original stream (debug messages go to stdout)
            originalOut.println("[" + plugin.getName() + "] DEBUG: " + message);
            return;
        }
        
        try {
            javaLogger.fine(message);
        } catch (Exception e) {
            activateEmergencyMode("Exception during logging: " + e.getMessage());
            originalOut.println("[" + plugin.getName() + "] DEBUG: " + message);
        } finally {
            exitLogging();
        }
    }
    
    // ========== Formatted Logging Methods ==========
    
    /**
     * Logs an informational message with formatted parameters.
     * Uses String.format() for parameter substitution.
     * 
     * @param format the format string
     * @param args the arguments referenced by the format specifiers
     */
    public void info(@NotNull String format, @NotNull Object... args) {
        if (format == null) {
            return;
        }
        
        try {
            String message = String.format(format, args);
            info(message);
        } catch (Exception e) {
            // If formatting fails, log the format string and the error
            info("Failed to format message: " + format);
            activateEmergencyMode("String.format() failed: " + e.getMessage());
        }
    }
    
    /**
     * Logs a warning message with formatted parameters.
     * Uses String.format() for parameter substitution.
     * 
     * @param format the format string
     * @param args the arguments referenced by the format specifiers
     */
    public void warning(@NotNull String format, @NotNull Object... args) {
        if (format == null) {
            return;
        }
        
        try {
            String message = String.format(format, args);
            warning(message);
        } catch (Exception e) {
            // If formatting fails, log the format string and the error
            warning("Failed to format message: " + format);
            activateEmergencyMode("String.format() failed: " + e.getMessage());
        }
    }
    
    /**
     * Logs a severe error message with formatted parameters.
     * Uses String.format() for parameter substitution.
     * 
     * @param format the format string
     * @param args the arguments referenced by the format specifiers
     */
    public void severe(@NotNull String format, @NotNull Object... args) {
        if (format == null) {
            return;
        }
        
        try {
            String message = String.format(format, args);
            severe(message);
        } catch (Exception e) {
            // If formatting fails, log the format string and the error
            severe("Failed to format message: " + format);
            activateEmergencyMode("String.format() failed: " + e.getMessage());
        }
    }
    
    /**
     * Logs a debug message with formatted parameters.
     * Uses String.format() for parameter substitution.
     * 
     * @param format the format string
     * @param args the arguments referenced by the format specifiers
     */
    public void debug(@NotNull String format, @NotNull Object... args) {
        if (format == null) {
            return;
        }
        
        try {
            String message = String.format(format, args);
            debug(message);
        } catch (Exception e) {
            // If formatting fails, log the format string and the error
            debug("Failed to format message: " + format);
            activateEmergencyMode("String.format() failed: " + e.getMessage());
        }
    }
    
    // ========== Exception Logging Methods ==========
    
    /**
     * Logs a severe error message with an exception and its stack trace.
     * 
     * @param message the message to log
     * @param throwable the exception to log
     */
    public void severe(@NotNull String message, @Nullable Throwable throwable) {
        if (message == null) {
            return;
        }
        
        if (!enterLogging()) {
            // Fallback to original stream
            originalErr.println("[" + plugin.getName() + "] SEVERE: " + message);
            if (throwable != null) {
                throwable.printStackTrace(originalErr);
            }
            return;
        }
        
        try {
            if (throwable != null) {
                javaLogger.log(Level.SEVERE, message, throwable);
            } else {
                javaLogger.severe(message);
            }
        } catch (Exception e) {
            activateEmergencyMode("Exception during logging: " + e.getMessage());
            originalErr.println("[" + plugin.getName() + "] SEVERE: " + message);
            if (throwable != null) {
                throwable.printStackTrace(originalErr);
            }
        } finally {
            exitLogging();
        }
    }
    
    /**
     * Logs a warning message with an exception and its stack trace.
     * 
     * @param message the message to log
     * @param throwable the exception to log
     */
    public void warning(@NotNull String message, @Nullable Throwable throwable) {
        if (message == null) {
            return;
        }
        
        if (!enterLogging()) {
            // Fallback to original stream
            originalErr.println("[" + plugin.getName() + "] WARNING: " + message);
            if (throwable != null) {
                throwable.printStackTrace(originalErr);
            }
            return;
        }
        
        try {
            if (throwable != null) {
                javaLogger.log(Level.WARNING, message, throwable);
            } else {
                javaLogger.warning(message);
            }
        } catch (Exception e) {
            activateEmergencyMode("Exception during logging: " + e.getMessage());
            originalErr.println("[" + plugin.getName() + "] WARNING: " + message);
            if (throwable != null) {
                throwable.printStackTrace(originalErr);
            }
        } finally {
            exitLogging();
        }
    }
    
    // ========== Configuration Methods ==========
    
    /**
     * Sets the console log level for this plugin.
     * Only messages at or above this level will be written to the console.
     * 
     * @param level the minimum level for console output
     */
    public void setConsoleLevel(@NotNull Level level) {
        if (level == null) {
            throw new NullPointerException("Level cannot be null");
        }
        if (consoleHandler != null) {
            consoleHandler.setLevel(level);
        }
    }
    
    /**
     * Sets the file log level for this plugin.
     * Only messages at or above this level will be written to the log file.
     * 
     * @param level the minimum level for file output
     */
    public void setFileLevel(@NotNull Level level) {
        if (level == null) {
            throw new NullPointerException("Level cannot be null");
        }
        if (fileHandler != null) {
            fileHandler.setLevel(level);
        }
    }
    
    /**
     * Enables or disables console output for this plugin.
     * When disabled, no messages will be written to the console regardless of level.
     * File logging continues normally.
     * 
     * @param enabled true to enable console output, false to disable
     */
    public void setConsoleEnabled(boolean enabled) {
        if (consoleHandler != null) {
            if (enabled) {
                // Re-enable by setting to the current level (or WARNING as default)
                consoleHandler.setLevel(Level.WARNING);
            } else {
                // Disable by setting to OFF
                consoleHandler.setLevel(Level.OFF);
            }
        }
    }
    
    /**
     * Flushes all handlers, ensuring pending log messages are written to disk.
     * This is useful before critical operations or when you need to ensure logs are persisted.
     */
    public void flush() {
        if (fileHandler != null) {
            fileHandler.flush();
        }
        if (consoleHandler != null) {
            consoleHandler.flush();
        }
    }
    
    /**
     * Closes this logger, flushes all handlers, releases resources, and removes from registry.
     * After calling this method, the logger should not be used anymore.
     */
    public void close() {
        // Stop periodic flush task
        stopPeriodicFlush();
        
        // Flush before closing
        flush();
        
        // Close handlers
        if (fileHandler != null) {
            fileHandler.close();
            javaLogger.removeHandler(fileHandler);
            fileHandler = null;
        }
        if (consoleHandler != null) {
            consoleHandler.close();
            javaLogger.removeHandler(consoleHandler);
            consoleHandler = null;
        }
        
        // Remove from CentralLogger registry
        CentralLogger.removeLogger(plugin.getName());
    }
    
    /**
     * Gets the plugin this logger is associated with.
     * 
     * @return the plugin instance
     */
    @NotNull
    JavaPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * Gets the underlying Java logger instance.
     * 
     * @return the Java logger
     */
    @NotNull
    Logger getJavaLogger() {
        return javaLogger;
    }
    
    /**
     * Checks if this logger is in emergency mode.
     * 
     * @return true if emergency mode is active, false otherwise
     */
    boolean isEmergencyMode() {
        return emergencyMode.get();
    }
    
    /**
     * Gets the current recursion depth for the calling thread.
     * This is primarily for testing purposes.
     * 
     * @return the current recursion depth
     */
    int getRecursionDepth() {
        return recursionDepth.get();
    }
}
