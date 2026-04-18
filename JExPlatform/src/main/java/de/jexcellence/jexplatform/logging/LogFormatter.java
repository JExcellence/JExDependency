package de.jexcellence.jexplatform.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Compact log formatter producing lines like
 * {@code [12:35:18 INFO] [JExEconomy] Loaded 42 currencies in 15ms}.
 *
 * <p>Thread name is appended only for non-main threads.
 *
 * @author JExcellence
 * @since 1.0.0
 */
final class LogFormatter extends Formatter {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final String name;

    LogFormatter(String name) {
        this.name = name;
    }

    @Override
    public String format(LogRecord record) {
        var sb = new StringBuilder(128);

        sb.append('[').append(TIME.format(Instant.ofEpochMilli(record.getMillis())));
        sb.append(' ').append(levelTag(record.getLevel()));
        sb.append("] [").append(name).append("] ");
        sb.append(record.getMessage());

        var thread = Thread.currentThread();
        if (!"main".equals(thread.getName()) && !thread.getName().startsWith("Server thread")) {
            sb.append(" [").append(thread.getName()).append(']');
        }

        sb.append(System.lineSeparator());

        if (record.getThrown() != null) {
            var sw = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }

        return sb.toString();
    }

    private static String levelTag(Level level) {
        if (level == Level.SEVERE)  return "ERROR";
        if (level == Level.WARNING) return "WARN";
        if (level == Level.INFO)    return "INFO";
        return "DEBUG";
    }
}
