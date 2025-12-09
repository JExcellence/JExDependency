package com.raindropcentral.rdq2.shared.translation;

import de.jexcellence.jextranslate.api.LocaleResolver;
import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.MissingKeyTracker;
import de.jexcellence.jextranslate.api.Placeholder;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.api.TranslationService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class RDQTranslationService {

    private static final Logger LOGGER = Logger.getLogger(RDQTranslationService.class.getName());
    private static final Locale DEFAULT_LOCALE = Locale.US;

    private final TranslationRepository repository;
    private final MessageFormatter formatter;
    private final LocaleResolver localeResolver;
    private final MissingKeyTracker missingKeyTracker;
    private final Set<String> reportedMissingKeys = ConcurrentHashMap.newKeySet();

    public RDQTranslationService(
        @NotNull TranslationRepository repository,
        @NotNull MessageFormatter formatter,
        @NotNull LocaleResolver localeResolver,
        @NotNull MissingKeyTracker missingKeyTracker
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.localeResolver = Objects.requireNonNull(localeResolver, "localeResolver");
        this.missingKeyTracker = Objects.requireNonNull(missingKeyTracker, "missingKeyTracker");

        TranslationService.configure(new TranslationService.ServiceConfiguration(
            repository, formatter, localeResolver
        ));
    }

    @NotNull
    public Component get(@NotNull String key, @NotNull Player player) {
        return get(key, player, Map.of());
    }

    @NotNull
    public Component get(@NotNull String key, @NotNull Player player, @NotNull Map<String, Object> placeholders) {
        var translationKey = TranslationKey.of(key);
        var locale = resolveLocale(player);
        var translation = repository.getTranslation(translationKey, locale);

        if (translation.isEmpty()) {
            trackMissingKey(key, locale);
            return Component.text(key);
        }

        var placeholderList = placeholders.entrySet().stream()
            .map(e -> createPlaceholder(e.getKey(), e.getValue()))
            .toList();

        return formatter.formatComponent(translation.get(), placeholderList, locale);
    }

    @NotNull
    public Component getWithPrefix(@NotNull String key, @NotNull Player player) {
        return getWithPrefix(key, player, Map.of());
    }

    @NotNull
    public Component getWithPrefix(@NotNull String key, @NotNull Player player, @NotNull Map<String, Object> placeholders) {
        var prefix = get("prefix", player);
        var message = get(key, player, placeholders);
        return prefix.append(message);
    }

    public void sendMessage(@NotNull Player player, @NotNull String key) {
        sendMessage(player, key, Map.of());
    }

    public void sendMessage(@NotNull Player player, @NotNull String key, @NotNull Map<String, Object> placeholders) {
        var message = get(key, player, placeholders);
        player.sendMessage(message);
    }

    public void sendMessageWithPrefix(@NotNull Player player, @NotNull String key) {
        sendMessageWithPrefix(player, key, Map.of());
    }

    public void sendMessageWithPrefix(@NotNull Player player, @NotNull String key, @NotNull Map<String, Object> placeholders) {
        var message = getWithPrefix(key, player, placeholders);
        player.sendMessage(message);
    }

    public void sendActionBar(@NotNull Player player, @NotNull String key) {
        sendActionBar(player, key, Map.of());
    }

    public void sendActionBar(@NotNull Player player, @NotNull String key, @NotNull Map<String, Object> placeholders) {
        var message = get(key, player, placeholders);
        player.sendActionBar(message);
    }

    public void sendTitle(@NotNull Player player, @NotNull String titleKey, @NotNull String subtitleKey) {
        sendTitle(player, titleKey, subtitleKey, Map.of());
    }

    public void sendTitle(@NotNull Player player, @NotNull String titleKey, @NotNull String subtitleKey, @NotNull Map<String, Object> placeholders) {
        var title = get(titleKey, player, placeholders);
        var subtitle = get(subtitleKey, player, placeholders);
        player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle));
    }

    @NotNull
    public Optional<String> getRaw(@NotNull String key, @NotNull Locale locale) {
        var translationKey = TranslationKey.of(key);
        var translation = repository.getTranslation(translationKey, locale);
        if (translation.isEmpty()) {
            trackMissingKey(key, locale);
        }
        return translation;
    }

    @NotNull
    public Locale resolveLocale(@NotNull Player player) {
        return localeResolver.resolveLocale(player).orElse(DEFAULT_LOCALE);
    }

    @NotNull
    public Set<Locale> getAvailableLocales() {
        return repository.getAvailableLocales();
    }

    @NotNull
    public Set<String> getMissingKeys() {
        return Set.copyOf(reportedMissingKeys);
    }

    public void clearMissingKeys() {
        reportedMissingKeys.clear();
    }

    private void trackMissingKey(@NotNull String key, @NotNull Locale locale) {
        var trackingKey = key + ":" + locale.toLanguageTag();
        if (reportedMissingKeys.add(trackingKey)) {
            LOGGER.warning("Missing translation key: " + key + " for locale: " + locale);
            missingKeyTracker.trackMissing(TranslationKey.of(key), locale);
        }
    }

    @NotNull
    private Placeholder createPlaceholder(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            return Placeholder.of(key, "");
        }
        if (value instanceof String s) {
            return Placeholder.of(key, s);
        }
        if (value instanceof Number n) {
            return Placeholder.of(key, n);
        }
        if (value instanceof Component c) {
            return Placeholder.of(key, c);
        }
        return Placeholder.of(key, value.toString());
    }

    @NotNull
    public TranslationRepository getRepository() {
        return repository;
    }

    @NotNull
    public MessageFormatter getFormatter() {
        return formatter;
    }

    @NotNull
    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    @NotNull
    public MissingKeyTracker getMissingKeyTracker() {
        return missingKeyTracker;
    }
}
