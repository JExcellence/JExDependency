package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * PlatformLogFormatter
 * - No emojis
 * - Optional ANSI colors for console (level only)
 * - Full timestamp for files, time-only for console
 * - Abbreviated logger names for long packages
 * - Sanitizes control chars/ANSI sequences from messages
 */
public class PlatformLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter TIME_ONLY_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    // Strip existing ANSI and control characters from incoming messages
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*[ -/]*[@-~]");
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("\\p{Cntrl}");

    private static final String RESET = "\u001B[0m";
    private final boolean useColors;

    public PlatformLogFormatter(final boolean useColors) {
        // enable color only if console exists; for files, pass false
        this.useColors = useColors && System.console() != null;
    }

    @Override
    public @NotNull String format(final @NotNull LogRecord record) {
        final StringBuilder builder = new StringBuilder();

        final String timestamp = formatTimestamp(record.getMillis(), useColors);
        final String levelStr = record.getLevel() != null ? record.getLevel().getName() : "INFO";
        final String coloredLevel = useColors ? colorize(levelStr) : levelStr;

        builder.append("[")
                .append(timestamp)
                .append("] ")
                .append("[")
                .append(coloredLevel)
                .append("] ");

        final String loggerName = record.getLoggerName();
        if (loggerName != null && !loggerName.isEmpty()) {
            builder.append(abbreviateLogger(loggerName)).append(": ");
        }

        // Use formatMessage to resolve parameters/ResourceBundle, then sanitize
        final String message = sanitize(formatMessage(record));
        builder.append(message);

        if (record.getThrown() != null) {
            builder.append("\n").append(formatThrowable(record.getThrown()));
        }

        builder.append("\n");
        return builder.toString();
    }

    private @NotNull String formatTimestamp(final long millis, final boolean compact) {
        return (compact ? TIME_ONLY_FORMAT : TIMESTAMP_FORMAT)
                .format(Instant.ofEpochMilli(millis));
    }

    private @NotNull String colorize(final @NotNull String level) {
        // Only wrap the level token in color; rest stays plain
        final String code;
        switch (level) {
            case "SEVERE":
            case "CRITICAL":
            case "ERROR":
                code = "\u001B[31m"; // red
                break;
            case "WARNING":
            case "WARN":
                code = "\u001B[33m"; // yellow
                break;
            case "INFO":
                code = "\u001B[32m"; // green
                break;
            case "CONFIG":
                code = "\u001B[34m"; // blue
                break;
            case "DEBUG":
            case "TRACE":
            case "FINE":
            case "FINER":
            case "FINEST":
                code = "\u001B[36m"; // cyan
                break;
            default:
                return level; // unknown: no color
        }
        return code + level + RESET;
    }

    private @NotNull String abbreviateLogger(final @NotNull String loggerName) {
        if (loggerName.length() <= 30) {
            return loggerName;
        }
        final String[] parts = loggerName.split("\\.");
        final StringBuilder abbreviated = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                abbreviated.append(parts[i].charAt(0)).append(".");
            }
        }
        abbreviated.append(parts[parts.length - 1]);
        return abbreviated.toString();
    }

    private @NotNull String sanitize(final @Nullable String text) {
        if (text == null) return "";
        String cleaned = ANSI_PATTERN.matcher(text).replaceAll("");
        cleaned = CONTROL_CHAR_PATTERN.matcher(cleaned).replaceAll("");
        return cleaned;
    }

    private @NotNull String formatThrowable(final @NotNull Throwable throwable) {
        // Use a StringWriter to preserve original stack trace format, but sanitize lines
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sanitize(sw.toString());
    }
}