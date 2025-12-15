package com.raindropcentral.rplatform.console;

import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Formats and logs translated console messages using the JExTranslate I18n system.
 *
 * <p>This helper mirrors the player-facing translation flow provided by {@link I18n} while removing
 * the {@link org.bukkit.entity.Player} dependency so server operators receive localized diagnostics.
 * Prefix support matches the in-game experience by optionally prepending the {@code prefix} translation
 * before each message.</p>
 *
 * <p>Console consumers should create a single instance per {@link Logger} and reuse it so prefix
 * preferences remain stable across log lines.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public final class ConsoleMessenger {

    private final Logger logger;
    private final boolean withPrefix;

    /**
     * Creates a messenger that logs with the translation system's default locale and includes the shared prefix.
     *
     * @param logger the logger used to emit console messages
     */
    public ConsoleMessenger(@NotNull final Logger logger) {
        this(logger, true);
    }

    /**
     * Creates a messenger with control over prefix behaviour.
     *
     * @param logger     the logger used to emit console messages
     * @param withPrefix whether the default prefix should be prepended to each message
     */
    public ConsoleMessenger(
            @NotNull final Logger logger,
            final boolean withPrefix
    ) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.withPrefix = withPrefix;
    }


    /**
     * Logs a translated message at {@link Level#INFO} using the supplied translation key and placeholder data.
     *
     * @param key          the translation key that should be rendered
     * @param placeholders placeholder data applied to the translation template
     */
    public void info(@NotNull final String key, @NotNull final Map<String, Object> placeholders) {
        log(Level.INFO, key, placeholders);
    }

    /**
     * Logs a translated message at {@link Level#INFO} using the supplied translation key.
     *
     * @param key the translation key that should be rendered
     */
    public void info(@NotNull final String key) {
        log(Level.INFO, key, Map.of());
    }

    /**
     * Logs a translated message at {@link Level#WARNING} using the supplied translation key and placeholder data.
     *
     * @param key          the translation key that should be rendered
     * @param placeholders placeholder data applied to the translation template
     */
    public void warn(@NotNull final String key, @NotNull final Map<String, Object> placeholders) {
        log(Level.WARNING, key, placeholders);
    }

    /**
     * Logs a translated message at {@link Level#WARNING} using the supplied translation key.
     *
     * @param key the translation key that should be rendered
     */
    public void warn(@NotNull final String key) {
        log(Level.WARNING, key, Map.of());
    }

    /**
     * Logs a translated message at {@link Level#SEVERE} using the supplied translation key and placeholder data.
     *
     * @param key          the translation key that should be rendered
     * @param placeholders placeholder data applied to the translation template
     */
    public void error(@NotNull final String key, @NotNull final Map<String, Object> placeholders) {
        log(Level.SEVERE, key, placeholders);
    }

    /**
     * Logs a translated message at {@link Level#SEVERE} using the supplied translation key.
     *
     * @param key the translation key that should be rendered
     */
    public void error(@NotNull final String key) {
        log(Level.SEVERE, key, Map.of());
    }

    /**
     * Logs a translated message using the provided log level.
     *
     * @param level        the log level to apply when emitting the translation
     * @param key          the translation key that should be rendered
     * @param placeholders placeholder data applied to the translation template
     */
    public void log(
            @NotNull final Level level,
            @NotNull final String key,
            @NotNull final Map<String, Object> placeholders
    ) {
        Objects.requireNonNull(level, "Level cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(placeholders, "Placeholders cannot be null");

        try {
            final String message = translate(key, placeholders);
            this.logger.log(level, message);
        } catch (final Exception exception) {
            this.logger.log(Level.WARNING, "Failed to render console translation for key: " + key, exception);
            this.logger.log(level, "[" + key + "]");
        }
    }

    /**
     * Renders a console-compatible translated message using the configured prefix settings.
     *
     * @param key          the translation key that should be rendered
     * @param placeholders placeholder data applied to the translation template
     * @return the rendered message as plain text ready for console output
     */
    @NotNull
    public String translate(
            @NotNull final String key,
            @NotNull final Map<String, Object> placeholders
    ) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(placeholders, "Placeholders cannot be null");

        final I18n.Builder builder = new I18n.Builder(key);
        
        if (!placeholders.isEmpty()) {
            builder.withPlaceholders(placeholders);
        }
        
        if (this.withPrefix) {
            builder.includePrefix();
        }

        final I18n i18n = builder.build();
        final Object component = i18n.component();
        
        if (component instanceof Component adventureComponent) {
            return PlainTextComponentSerializer.plainText().serialize(adventureComponent);
        } else if (component instanceof String string) {
            return string;
        } else {
            return component != null ? component.toString() : "[" + key + "]";
        }
    }

    /**
     * Creates a placeholder map builder for convenience.
     *
     * @return a new mutable map for building placeholders
     */
    @NotNull
    public static Map<String, Object> placeholders() {
        return new HashMap<>();
    }

    /**
     * Creates a placeholder map with a single entry.
     *
     * @param key   the placeholder key
     * @param value the placeholder value
     * @return a map containing the single placeholder
     */
    @NotNull
    public static Map<String, Object> placeholder(@NotNull final String key, @Nullable final Object value) {
        final Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
