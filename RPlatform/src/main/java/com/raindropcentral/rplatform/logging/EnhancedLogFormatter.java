package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

public class EnhancedLogFormatter extends Formatter {

    private static final DateTimeFormatter STANDARD_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter COMPACT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    // Remove ANSI and control chars from external sources
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*[ -/]*[@-~]");
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("\\p{Cntrl}");

    private final boolean useColor;

    public EnhancedLogFormatter(final boolean useColor) {
        // enable color only if console is attached
        this.useColor = useColor && System.console() != null;
    }

    @Override
    public String format(@NotNull final LogRecord record) {
        // Internal logger lines: keep simple
        if (record.getLoggerName() != null
                && record.getLoggerName().startsWith("com.raindropcentral.rplatform.logging")) {
            return simple(record) + "\n";
        }

        final String message = clean(record.getMessage());
        final StringBuilder sb = new StringBuilder();

        // Shorter timestamps for console
        sb.append("[")
          .append(COMPACT_FORMATTER.format(Instant.ofEpochMilli(record.getMillis())))
          .append("] ");

        sb.append("[")
          .append(colorize(levelName(record.getLevel())))
          .append("] ");

        final String loggerName = abbreviateLoggerName(record.getLoggerName());
        if (loggerName != null && !loggerName.isEmpty()) {
            sb.append(loggerName).append(": ");
        }

        sb.append(message);

        if (record.getThrown() != null) {
            sb.append("\n").append(record.getThrown().toString());
            for (final StackTraceElement ste : record.getThrown().getStackTrace()) {
                sb.append("\n\tat ").append(clean(ste.toString()));
            }
        }

        return sb.append("\n").toString();
    }

    private String simple(final LogRecord r) {
        return "[" + r.getLevel() + "] " + (r.getMessage() == null ? "" : r.getMessage());
    }

    private String levelName(final Level lvl) {
        // Preserve custom LogLevel names when applicable
        return lvl.getName();
    }

    private String clean(final String in) {
        if (in == null) return "";
        String s = ANSI_PATTERN.matcher(in).replaceAll("");
        s = CONTROL_CHAR_PATTERN.matcher(s).replaceAll("");
        return s;
    }

    private String colorize(final @NotNull String level) {
        if (!useColor) return level;
        switch (level) {
            case "SEVERE":
            case "CRITICAL":
            case "ERROR":
                return "\u001B[31m" + level + "\u001B[0m";
            case "WARNING":
            case "WARN":
                return "\u001B[33m" + level + "\u001B[0m";
            case "INFO":
                return "\u001B[32m" + level + "\u001B[0m";
            case "CONFIG":
                return "\u001B[34m" + level + "\u001B[0m";
            case "DEBUG":
            case "FINE":
            case "FINER":
            case "FINEST":
            case "TRACE":
                return "\u001B[36m" + level + "\u001B[0m";
            default:
                return level;
        }
    }

    private @Nullable String abbreviateLoggerName(final @Nullable String name) {
        if (name == null || name.length() < 30) return name;
        final String[] parts = name.split("\\.");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i].charAt(0)).append('.');
            }
        }
        return sb.append(parts[parts.length - 1]).toString();
    }
}