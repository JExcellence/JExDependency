package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * RotatingFileHandler manages log file rotation with a simple 2-file strategy.
 * 
 * <p>This handler maintains two log files:
 * <ul>
 *   <li>{pluginname}-latest.log - The current active log file</li>
 *   <li>{pluginname}-backup.log - The previous log file (backup)</li>
 * </ul>
 * 
 * <p>When the current log file exceeds 10MB, it is renamed to the backup file
 * (overwriting any existing backup), and a new current file is created.
 * 
 * <p>Features:
 * <ul>
 *   <li>Automatic log directory creation</li>
 *   <li>UTF-8 encoding for all log files</li>
 *   <li>Simple 2-file rotation (no complex file management)</li>
 *   <li>Thread-safe file operations</li>
 * </ul>
 * 
 * @see PluginLogger
 */
public class RotatingFileHandler extends Handler {
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private final File logDirectory;
    private final File currentFile;
    private final File backupFile;
    private final String pluginName;
    
    private PrintWriter writer;
    private long currentSize;
    private boolean closed;
    
    /**
     * Creates a new RotatingFileHandler for the specified plugin.
     * 
     * @param pluginDataFolder the plugin's data folder
     * @param pluginName the name of the plugin
     * @throws IOException if the log directory or files cannot be created
     */
    public RotatingFileHandler(@NotNull File pluginDataFolder, @NotNull String pluginName) throws IOException {
        this.pluginName = pluginName;
        this.closed = false;
        
        // Create logs directory
        this.logDirectory = new File(pluginDataFolder, "logs");
        if (!logDirectory.exists() && !logDirectory.mkdirs()) {
            throw new IOException("Failed to create log directory: " + logDirectory.getAbsolutePath());
        }
        
        // Define file paths
        String sanitizedName = pluginName.toLowerCase().replaceAll("[^a-z0-9-_]", "");
        this.currentFile = new File(logDirectory, sanitizedName + "-latest.log");
        this.backupFile = new File(logDirectory, sanitizedName + "-backup.log");
        
        // Initialize the current file
        openCurrentFile();
    }
    
    /**
     * Opens or creates the current log file.
     * If the file already exists, appends to it and tracks its current size.
     * 
     * @throws IOException if the file cannot be opened
     */
    private void openCurrentFile() throws IOException {
        // Check if file exists and get its size
        if (currentFile.exists()) {
            currentSize = currentFile.length();
        } else {
            currentSize = 0;
        }
        
        // Open file for appending with UTF-8 encoding
        FileOutputStream fos = new FileOutputStream(currentFile, true);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        writer = new PrintWriter(osw, true); // Auto-flush enabled
    }
    
    /**
     * Publishes a log record to the file.
     * Checks file size before writing and rotates if necessary.
     * 
     * @param record the log record to publish
     */
    @Override
    public synchronized void publish(LogRecord record) {
        // Check if handler is closed or record is null
        if (closed || record == null) {
            return;
        }
        
        // Check if record should be logged based on level
        if (!isLoggable(record)) {
            return;
        }
        
        try {
            // Check if rotation is needed
            if (currentSize >= MAX_FILE_SIZE) {
                rotate();
            }
            
            // Format and write the log record
            String message = getFormatter().format(record);
            writer.write(message);
            
            // Update current size (approximate)
            currentSize += message.getBytes(StandardCharsets.UTF_8).length;
            
        } catch (Exception e) {
            // Report error but don't throw exception
            reportError("Error publishing log record", e, ErrorManager.WRITE_FAILURE);
        }
    }
    
    /**
     * Rotates the log files by:
     * 1. Closing the current file
     * 2. Deleting the backup file (if it exists)
     * 3. Renaming current file to backup
     * 4. Creating a new current file
     * 
     * @throws IOException if rotation fails
     */
    private void rotate() throws IOException {
        // Close current writer
        if (writer != null) {
            writer.close();
            writer = null;
        }
        
        // Delete old backup if it exists
        if (backupFile.exists()) {
            if (!backupFile.delete()) {
                // Log warning but continue - we'll overwrite it
                System.err.println("[RotatingFileHandler] Warning: Could not delete backup file: " + 
                                   backupFile.getAbsolutePath());
            }
        }
        
        // Rename current to backup
        if (currentFile.exists()) {
            if (!currentFile.renameTo(backupFile)) {
                throw new IOException("Failed to rename current log file to backup: " + 
                                      currentFile.getAbsolutePath());
            }
        }
        
        // Reset size counter
        currentSize = 0;
        
        // Open new current file
        openCurrentFile();
    }
    
    /**
     * Flushes any buffered output.
     */
    @Override
    public synchronized void flush() {
        if (writer != null && !closed) {
            writer.flush();
        }
    }
    
    /**
     * Closes the handler and releases all resources.
     * Flushes any buffered output before closing.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (Exception e) {
                reportError("Error closing log file", e, ErrorManager.CLOSE_FAILURE);
            } finally {
                writer = null;
            }
        }
    }
    
    /**
     * Gets the path to the current log file.
     * 
     * @return the absolute path to the current log file
     */
    @NotNull
    public String getCurrentFilePath() {
        return currentFile.getAbsolutePath();
    }
    
    /**
     * Gets the path to the backup log file.
     * 
     * @return the absolute path to the backup log file
     */
    @NotNull
    public String getBackupFilePath() {
        return backupFile.getAbsolutePath();
    }
    
    /**
     * Gets the current size of the active log file in bytes.
     * 
     * @return the current file size
     */
    public long getCurrentSize() {
        return currentSize;
    }
    
    /**
     * Gets the maximum file size before rotation occurs.
     * 
     * @return the maximum file size in bytes (10MB)
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
    
    /**
     * Checks if this handler has been closed.
     * 
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }
}
