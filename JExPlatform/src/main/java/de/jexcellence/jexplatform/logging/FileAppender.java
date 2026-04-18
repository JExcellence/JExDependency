package de.jexcellence.jexplatform.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Rotating file handler that maintains two log files: the current log and one backup.
 *
 * <p>When the active file exceeds {@value #MAX_BYTES} bytes it is rotated:
 * the previous backup is discarded, the current file becomes the backup,
 * and a fresh file is opened. All output is UTF-8.
 *
 * @author JExcellence
 * @since 1.0.0
 */
final class FileAppender extends Handler {

    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10 MB

    private final Path logFile;
    private final Path backupFile;
    private final LogFormatter formatter;
    private FileHandler delegate;

    FileAppender(String name, Path logDirectory, LogLevel level) throws IOException {
        Files.createDirectories(logDirectory);
        this.logFile = logDirectory.resolve(name.toLowerCase() + ".log");
        this.backupFile = logDirectory.resolve(name.toLowerCase() + ".log.1");
        this.formatter = new LogFormatter(name);
        setLevel(level.toJulLevel());
        this.delegate = openHandler();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) return;

        delegate.publish(record);

        try {
            if (Files.exists(logFile) && Files.size(logFile) > MAX_BYTES) {
                rotate();
            }
        } catch (IOException ignored) {
            // Best-effort rotation — if it fails, keep writing to current file
        }
    }

    @Override
    public synchronized void flush() {
        delegate.flush();
    }

    @Override
    public synchronized void close() {
        delegate.close();
    }

    /**
     * Updates the minimum level written to the file.
     *
     * @param level the new minimum level
     */
    void setFileLevel(LogLevel level) {
        setLevel(level.toJulLevel());
    }

    private void rotate() throws IOException {
        delegate.close();
        Files.move(logFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        delegate = openHandler();
    }

    private FileHandler openHandler() throws IOException {
        var handler = new FileHandler(logFile.toString(), /* append */ true);
        handler.setFormatter(formatter);
        handler.setEncoding("UTF-8");
        handler.setLevel(getLevel());
        return handler;
    }
}
