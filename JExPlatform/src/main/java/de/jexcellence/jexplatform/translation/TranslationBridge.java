package de.jexcellence.jexplatform.translation;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jextranslate.R18nManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Thin bridge to JExTranslate for internationalization initialization.
 *
 * <p>Configures an {@link R18nManager} with sensible defaults and exposes
 * lifecycle methods for the platform orchestrator:
 *
 * <pre>{@code
 * var i18n = TranslationBridge.create(plugin, log, "en_US", "de_DE", "es_ES");
 * i18n.initialize().thenRun(() -> log.info("Translations loaded"));
 * // on shutdown:
 * i18n.shutdown();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class TranslationBridge {

    private final R18nManager manager;
    private final JExLogger log;

    private TranslationBridge(@NotNull R18nManager manager, @NotNull JExLogger log) {
        this.manager = manager;
        this.log = log;
    }

    /**
     * Creates a translation bridge with the specified locales.
     *
     * <p>Key validation and PlaceholderAPI integration are enabled by default.
     * The translation directory defaults to {@code translations/} inside the
     * plugin data folder.
     *
     * @param plugin         the owning plugin
     * @param log            logger for diagnostics
     * @param defaultLocale  the fallback locale (e.g. {@code "en_US"})
     * @param extraLocales   additional supported locales
     * @return a configured translation bridge ready for {@link #initialize()}
     */
    public static @NotNull TranslationBridge create(
            @NotNull JavaPlugin plugin,
            @NotNull JExLogger log,
            @NotNull String defaultLocale,
            @NotNull String... extraLocales
    ) {
        var manager = R18nManager.builder(plugin)
                .defaultLocale(defaultLocale)
                .supportedLocales(extraLocales)
                .enableKeyValidation(true)
                .enablePlaceholderAPI(true)
                .enableFileWatcher(true)
                .build();

        return new TranslationBridge(manager, log);
    }

    /**
     * Loads translation files asynchronously.
     *
     * @return future that completes when all translations are loaded
     */
    public @NotNull CompletableFuture<Void> initialize() {
        log.info("Loading translations...");
        return manager.initialize().thenRun(() ->
                log.info("Translations loaded"));
    }

    /**
     * Shuts down the translation system, releasing file watchers and caches.
     */
    public void shutdown() {
        manager.shutdown();
        log.debug("Translation system shut down");
    }

    /**
     * Returns the underlying R18nManager for direct access.
     *
     * @return the R18n manager instance
     */
    public @NotNull R18nManager manager() {
        return manager;
    }
}
