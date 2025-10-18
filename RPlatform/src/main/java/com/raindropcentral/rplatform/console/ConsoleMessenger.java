package com.raindropcentral.rplatform.console;

import de.jexcellence.jextranslate.api.Placeholder;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Formats and logs translated console messages using the shared {@link TranslationService} pipeline.
 *
 * <p>This helper mirrors the player-facing translation flow provided by {@link TranslationService} while removing
 * the {@link org.bukkit.entity.Player} dependency so server operators receive localized diagnostics. Prefix support
 * matches the in-game experience by optionally prepending the {@code prefix} translation before each message.</p>
 *
 * <p>Console consumers should create a single instance per {@link Logger} and reuse it so prefix and locale
 * preferences remain stable across log lines.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class ConsoleMessenger {

    private static final TranslationKey DEFAULT_PREFIX_KEY = TranslationKey.of("prefix");

    private final Logger logger;
    private final Locale locale;
    private final boolean includePrefix;
    private final TranslationKey prefixKey;

    /**
     * Creates a messenger that logs with the translation system's default locale and includes the shared prefix.
     *
     * @param logger the logger used to emit console messages
     */
    public ConsoleMessenger(@NotNull final Logger logger) {
        this(logger, null, true, null);
    }

    /**
     * Creates a messenger with full control over locale and prefix behaviour.
     *
     * @param logger        the logger used to emit console messages
     * @param locale        the locale applied to translations; {@code null} uses the configured default
     * @param includePrefix whether the default prefix should be prepended to each message
     * @param prefixKey     optional replacement prefix key when {@code includePrefix} is {@code true}
     */
    public ConsoleMessenger(
            @NotNull final Logger logger,
            @Nullable final Locale locale,
            final boolean includePrefix,
            @Nullable final TranslationKey prefixKey
    ) {
        this.logger = Objects.requireNonNull(logger, "Logger cannot be null");
        this.locale = locale;
        this.includePrefix = includePrefix;
        this.prefixKey = prefixKey;
    }

    /**
     * Logs a translated message at {@link Level#INFO} using the supplied translation key and placeholder data.
     *
     * @param key           the translation key that should be rendered
     * @param placeholders  placeholder data applied to the translation template
     */
    public void info(@NotNull final TranslationKey key, @NotNull final Map<String, Object> placeholders) {
        log(Level.INFO, key, placeholders);
    }

    /**
     * Logs a translated message at {@link Level#WARNING} using the supplied translation key and placeholder data.
     *
     * @param key           the translation key that should be rendered
     * @param placeholders  placeholder data applied to the translation template
     */
    public void warn(@NotNull final TranslationKey key, @NotNull final Map<String, Object> placeholders) {
        log(Level.WARNING, key, placeholders);
    }

    /**
     * Logs a translated message at {@link Level#SEVERE} using the supplied translation key and placeholder data.
     *
     * @param key           the translation key that should be rendered
     * @param placeholders  placeholder data applied to the translation template
     */
    public void error(@NotNull final TranslationKey key, @NotNull final Map<String, Object> placeholders) {
        log(Level.SEVERE, key, placeholders);
    }

    /**
     * Logs a translated message using the provided log level.
     *
     * @param level         the log level to apply when emitting the translation
     * @param key           the translation key that should be rendered
     * @param placeholders  placeholder data applied to the translation template
     */
    public void log(
            @NotNull final Level level,
            @NotNull final TranslationKey key,
            @NotNull final Map<String, Object> placeholders
    ) {
        Objects.requireNonNull(level, "Level cannot be null");
        final TranslatedMessage message = translate(key, placeholders);
        this.logger.log(level, message.asPlainText());
    }

    /**
     * Renders a console-compatible {@link TranslatedMessage} using the configured locale and prefix settings.
     *
     * @param key          the translation key that should be rendered
     * @param placeholders placeholder data applied to the translation template
     * @return the rendered message ready for console output
     */
    @NotNull
    public TranslatedMessage translate(
            @NotNull final TranslationKey key,
            @NotNull final Map<String, Object> placeholders
    ) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(placeholders, "Placeholders cannot be null");

        final TranslationService.ServiceConfiguration configuration = ensureConfigured();
        final Locale effectiveLocale = resolveLocale(configuration);
        final List<Placeholder> placeholderList = buildPlaceholders(placeholders);

        try {
            final Component baseComponent = resolveComponent(configuration, key, effectiveLocale, placeholderList);
            final Component finalComponent = this.includePrefix
                    ? prependPrefix(configuration, effectiveLocale, placeholderList, baseComponent)
                    : baseComponent;
            return new TranslatedMessage(finalComponent, key);
        } catch (final Exception exception) {
            this.logger.log(Level.WARNING, "Failed to render console translation for key " + key + '.', exception);
            final Component fallback = Component.text('[' + key.key() + ']');
            return new TranslatedMessage(fallback, key);
        }
    }

    @NotNull
    private TranslationService.ServiceConfiguration ensureConfigured() {
        final TranslationService.ServiceConfiguration configuration = TranslationService.getConfiguration();
        if (configuration == null) {
            throw new IllegalStateException("TranslationService is not configured. Initialize TranslationManager first.");
        }
        return configuration;
    }

    @NotNull
    private Locale resolveLocale(@NotNull final TranslationService.ServiceConfiguration configuration) {
        if (this.locale != null) {
            return this.locale;
        }
        return configuration.localeResolver().getDefaultLocale();
    }

    @NotNull
    private Component resolveComponent(
            @NotNull final TranslationService.ServiceConfiguration configuration,
            @NotNull final TranslationKey key,
            @NotNull final Locale locale,
            @NotNull final List<Placeholder> placeholders
    ) {
        final Optional<String> translation = configuration.repository().getTranslation(key, locale);
        if (translation.isEmpty()) {
            return Component.text(key.key());
        }
        return configuration.formatter().formatComponent(translation.get(), placeholders, locale);
    }

    @NotNull
    private Component prependPrefix(
            @NotNull final TranslationService.ServiceConfiguration configuration,
            @NotNull final Locale locale,
            @NotNull final List<Placeholder> placeholders,
            @NotNull final Component messageComponent
    ) {
        final TranslationKey keyToUse = this.prefixKey != null ? this.prefixKey : DEFAULT_PREFIX_KEY;
        final Optional<String> prefixTranslation = configuration.repository().getTranslation(keyToUse, locale);
        if (prefixTranslation.isEmpty()) {
            return messageComponent;
        }

        final Component prefixComponent = configuration.formatter().formatComponent(
                prefixTranslation.get(),
                placeholders,
                locale
        );
        return Component.join(JoinConfiguration.noSeparators(), prefixComponent, messageComponent);
    }

    @NotNull
    private List<Placeholder> buildPlaceholders(@NotNull final Map<String, Object> placeholders) {
        final List<Placeholder> result = new ArrayList<>(placeholders.size());
        for (final Map.Entry<String, Object> entry : placeholders.entrySet()) {
            result.add(createPlaceholder(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @NotNull
    private Placeholder createPlaceholder(@NotNull final String key, @Nullable final Object value) {
        Objects.requireNonNull(key, "Placeholder key cannot be null");
        if (value == null) {
            return Placeholder.of(key, "");
        }
        if (value instanceof Placeholder placeholder) {
            return placeholder;
        }
        if (value instanceof String string) {
            return Placeholder.of(key, string);
        }
        if (value instanceof Number number) {
            return Placeholder.of(key, number);
        }
        if (value instanceof Component component) {
            return Placeholder.of(key, component);
        }
        if (value instanceof LocalDateTime dateTime) {
            return Placeholder.of(key, dateTime);
        }
        return Placeholder.of(key, value.toString());
    }
}
