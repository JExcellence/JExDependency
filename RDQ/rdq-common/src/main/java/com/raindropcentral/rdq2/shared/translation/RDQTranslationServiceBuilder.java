package com.raindropcentral.rdq2.shared.translation;

import de.jexcellence.jextranslate.api.LocaleResolver;
import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.MissingKeyTracker;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.impl.HybridMessageFormatter;
import de.jexcellence.jextranslate.impl.LocaleResolverProvider;
import de.jexcellence.jextranslate.impl.SimpleMissingKeyTracker;
import de.jexcellence.jextranslate.impl.YamlTranslationRepository;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public final class RDQTranslationServiceBuilder {

    private static final Locale DEFAULT_LOCALE = Locale.US;

    private final JavaPlugin plugin;
    private @Nullable Path translationsPath;
    private @Nullable Locale defaultLocale;
    private @Nullable TranslationRepository repository;
    private @Nullable MessageFormatter formatter;
    private @Nullable LocaleResolver localeResolver;
    private @Nullable MissingKeyTracker missingKeyTracker;

    private RDQTranslationServiceBuilder(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @NotNull
    public static RDQTranslationServiceBuilder create(@NotNull JavaPlugin plugin) {
        return new RDQTranslationServiceBuilder(plugin);
    }

    @NotNull
    public RDQTranslationServiceBuilder translationsPath(@NotNull Path path) {
        this.translationsPath = Objects.requireNonNull(path, "path");
        return this;
    }

    @NotNull
    public RDQTranslationServiceBuilder defaultLocale(@NotNull Locale locale) {
        this.defaultLocale = Objects.requireNonNull(locale, "locale");
        return this;
    }

    @NotNull
    public RDQTranslationServiceBuilder repository(@NotNull TranslationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        return this;
    }

    @NotNull
    public RDQTranslationServiceBuilder formatter(@NotNull MessageFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        return this;
    }

    @NotNull
    public RDQTranslationServiceBuilder localeResolver(@NotNull LocaleResolver localeResolver) {
        this.localeResolver = Objects.requireNonNull(localeResolver, "localeResolver");
        return this;
    }

    @NotNull
    public RDQTranslationServiceBuilder missingKeyTracker(@NotNull MissingKeyTracker missingKeyTracker) {
        this.missingKeyTracker = Objects.requireNonNull(missingKeyTracker, "missingKeyTracker");
        return this;
    }

    @NotNull
    public RDQTranslationService build() {
        var locale = defaultLocale != null ? defaultLocale : DEFAULT_LOCALE;
        var path = translationsPath != null ? translationsPath : plugin.getDataFolder().toPath().resolve("translations");

        var repo = repository != null ? repository : YamlTranslationRepository.create(path, locale);
        var fmt = formatter != null ? formatter : new HybridMessageFormatter();
        var resolver = localeResolver != null ? localeResolver : LocaleResolverProvider.createAutoDetecting(locale);
        var tracker = missingKeyTracker != null ? missingKeyTracker : new SimpleMissingKeyTracker();

        return new RDQTranslationService(repo, fmt, resolver, tracker);
    }

    @NotNull
    public RDQTranslationService buildWithDefaults() {
        return build();
    }
}
