package com.raindropcentral.rplatform.localization;

import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.impl.LocaleResolverProvider;
import de.jexcellence.jextranslate.impl.MiniMessageFormatter;
import de.jexcellence.jextranslate.impl.YamlTranslationRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Coordinates translation resources for a {@link JavaPlugin} by wiring the {@link TranslationService}
 * to a YAML-backed {@link TranslationRepository} and exposing convenience accessors for player locales.
 * <p>
 * The manager should be constructed and {@link #initialize() initialized} during plugin startup on the
 * primary server thread so that the underlying service configuration is visible to synchronous code that
 * resolves translations. Subsequent calls such as {@link #reload()} leverage the repository's asynchronous
 * reloading capabilities and are safe to trigger from scheduler threads, while locale access delegates to
 * the thread-safe {@link TranslationService} locale resolver cache.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public class TranslationManager {

    /**
     * Owning plugin whose data directory and logger are used to store translation files and emit status output.
     */
    private final JavaPlugin plugin;
    /**
     * Repository that persists localized messages and supports asynchronous reloads backed by YAML resources.
     */
    private final TranslationRepository repository;
    /**
     * Locale applied for players whose preferred language cannot be resolved by the {@link TranslationService}.
     */
    private final Locale defaultLocale;

    /**
     * Creates a translation manager that defaults player locales to {@link Locale#ENGLISH} while configuring
     * the {@link TranslationService} repository under the plugin's data folder.
     *
     * @param plugin the plugin responsible for hosting translation files and service lifecycle hooks
     */
    public TranslationManager(final @NotNull JavaPlugin plugin) {
        this(plugin, Locale.ENGLISH);
    }

    /**
     * Creates a translation manager using the supplied {@code defaultLocale} when a player's locale cannot be
     * auto-detected by the {@link LocaleResolverProvider}. The repository is initialized immediately for later
     * service configuration.
     *
     * @param plugin        the plugin responsible for hosting translation files and service lifecycle hooks
     * @param defaultLocale the fallback locale applied when player locale detection fails
     */
    public TranslationManager(final @NotNull JavaPlugin plugin, final @NotNull Locale defaultLocale) {
        this.plugin = plugin;
        this.defaultLocale = defaultLocale;
        this.repository = createRepository();
    }

    /**
     * Configures the global {@link TranslationService} with the YAML repository, a MiniMessage formatter, and an
     * auto-detecting locale resolver. Must be invoked before other components attempt to translate messages.
     * Logs the number of available locales as a side effect.
     */
    public void initialize() {
        final MessageFormatter formatter = new MiniMessageFormatter();
        final var localeResolver = LocaleResolverProvider.createAutoDetecting(defaultLocale);

        TranslationService.configure(new TranslationService.ServiceConfiguration(
                repository,
                formatter,
                localeResolver
        ));

        plugin.getLogger().info("Translation service initialized with " + 
                repository.getAvailableLocales().size() + " locales");
    }

    /**
     * Reloads translation bundles from disk asynchronously and clears cached locale mappings via the
     * {@link TranslationService}. Emits reload statistics to the plugin logger when the reload completes.
     */
    public void reload() {
        repository.reload().thenRun(() -> {
            TranslationService.clearLocaleCache();
            plugin.getLogger().info("Translations reloaded: " +
                    repository.getAvailableLocales().size() + " locales, " +
                    repository.getAllAvailableKeys().size() + " keys");
        });
    }

    /**
     * Sets a player's locale preference using the configured {@link TranslationService} resolver and clears the
     * player's cached translations when successful.
     *
     * @param player the player whose locale should be updated
     * @param locale the locale to persist for the player
     * @return {@code true} if the resolver accepted the locale change; {@code false} otherwise
     */
    public boolean setPlayerLocale(final @NotNull Player player, final @NotNull Locale locale) {
        final var resolver = TranslationService.getConfiguration().localeResolver();
        final boolean success = resolver.setPlayerLocale(player, locale);

        if (success) {
            TranslationService.clearLocaleCache(player);
        }

        return success;
    }

    /**
     * Resolves the player's locale from the {@link TranslationService} cache or returns the configured fallback
     * locale when no preference has been stored.
     *
     * @param player the player whose locale should be resolved
     * @return the locale used for translating messages to the player
     */
    public @NotNull Locale getPlayerLocale(final @NotNull Player player) {
        final var resolver = TranslationService.getConfiguration().localeResolver();
        return resolver.resolveLocale(player).orElse(defaultLocale);
    }

    private @NotNull TranslationRepository createRepository() {
        final Path translationsDir = plugin.getDataFolder().toPath().resolve("translations");
        return YamlTranslationRepository.create(translationsDir, defaultLocale);
    }
}
