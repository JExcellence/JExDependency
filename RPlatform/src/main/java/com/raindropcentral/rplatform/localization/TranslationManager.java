package com.raindropcentral.rplatform.localization;

import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.jextranslate.config.R18nConfiguration;
import de.jexcellence.jextranslate.core.TranslationMetrics;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Coordinates translation resources for a {@link JavaPlugin} using the modern {@link R18nManager} API.
 *
 * <p>The manager should be constructed and {@link #initialize() initialized} during plugin startup.
 * Initialization is asynchronous and returns a {@link CompletableFuture} that completes when
 * translations are loaded and ready to use.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 1.0.0
 */
public class TranslationManager {

    private static final Logger LOGGER = Logger.getLogger(TranslationManager.class.getName());

    private final JavaPlugin plugin;
    private final String defaultLocale;
    private final String[] supportedLocales;
    private final boolean enableMetrics;
    private final boolean enableFileWatcher;
    private R18nManager r18n;

    /**
     * Creates a translation manager with default English locale.
     *
     * @param plugin the plugin responsible for hosting translation files
     */
    public TranslationManager(final @NotNull JavaPlugin plugin) {
        this(plugin, "en_US", false, false, "en_US", "de_DE");
    }

    /**
     * Creates a translation manager with the specified default locale.
     *
     * @param plugin        the plugin responsible for hosting translation files
     * @param defaultLocale the fallback locale code (e.g., "en_US")
     * @param supportedLocales the supported locale codes
     */
    public TranslationManager(
            final @NotNull JavaPlugin plugin,
            final @NotNull String defaultLocale,
            final @NotNull String... supportedLocales
    ) {
        this(plugin, defaultLocale, false, false, supportedLocales);
    }

    /**
     * Creates a translation manager with full configuration options.
     *
     * @param plugin              the plugin responsible for hosting translation files
     * @param defaultLocale       the fallback locale code (e.g., "en_US")
     * @param enableMetrics       whether to enable translation metrics
     * @param enableFileWatcher   whether to enable file watcher for hot reload
     * @param supportedLocales    the supported locale codes
     */
    public TranslationManager(
            final @NotNull JavaPlugin plugin,
            final @NotNull String defaultLocale,
            final boolean enableMetrics,
            final boolean enableFileWatcher,
            final @NotNull String... supportedLocales
    ) {
        this.plugin = plugin;
        this.defaultLocale = defaultLocale;
        this.enableMetrics = enableMetrics;
        this.enableFileWatcher = enableFileWatcher;
        this.supportedLocales = supportedLocales.length > 0 ? supportedLocales : new String[]{defaultLocale};
    }

    /**
     * Initializes the R18n translation system asynchronously.
 *
 * <p>Must be called before any translation operations. The returned future completes
     * when translations are loaded and ready to use.
     *
     * @return a CompletableFuture that completes when initialization is done
     */
    public @NotNull CompletableFuture<Void> initialize() {
        // Build configuration with auto-detect locales (empty set enables auto-detection)
        R18nConfiguration config = R18nConfiguration.defaultConfiguration()
                .withAutoDetectLocales() // Must be called to enable auto-detection
                .withCacheEnabled(true)
                .withCacheMaxSize(2000)
                .withCacheExpireMinutes(60)
                .withMetricsEnabled(enableMetrics)
                .withMissingKeyHandler((key, locale, placeholders) -> {
                    LOGGER.warning("Missing translation key: " + key + " for locale: " + locale);
                    return "<gray>[" + key + "]</gray>";
                });

        var builder = R18nManager.builder(plugin)
                .defaultLocale(defaultLocale)
                .enableKeyValidation(true)
                .enablePlaceholderAPI(true)
                .translationDirectory("translations")
                .enableFileWatcher(enableFileWatcher)
                .configuration(config); // Configuration already has auto-detect enabled

        r18n = builder.build();

        return r18n.initialize();
    }

    /**
     * Shuts down the translation system and releases resources.
 *
 * <p>Should be called in the plugin's onDisable() method.
     */
    public void shutdown() {
        if (r18n != null) {
            r18n.shutdown();
            r18n = null;
        }
    }

    /**
     * Reloads translation files asynchronously.
     *
     * @return a CompletableFuture that completes when reloading is done
     */
    public @NotNull CompletableFuture<Void> reload() {
        if (r18n == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("TranslationManager not initialized")
            );
        }
        return r18n.reload();
    }

    /**
     * Gets the underlying R18nManager instance.
     *
     * @return the R18nManager, or null if not initialized
     */
    @Nullable
    public R18nManager getR18n() {
        return r18n;
    }

    /**
     * Gets the Adventure audiences instance for sending messages.
     *
     * @return the BukkitAudiences instance, or null if not initialized
     */
    @Nullable
    public BukkitAudiences getAudiences() {
        return r18n != null ? r18n.getAudiences() : null;
    }

    /**
     * Checks if the translation system is initialized and ready.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return r18n != null && r18n.isInitialized();
    }

    /**
     * Gets the default locale code.
     *
     * @return the default locale (e.g., "en_US")
     */
    public @NotNull String getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Gets the total number of loaded translation keys.
     *
     * @return the key count, or 0 if not initialized
     */
    public int getKeyCount() {
        return r18n != null ? r18n.getTranslationLoader().getTotalKeyCount() : 0;
    }

    /**
     * Gets the number of loaded locales.
     *
     * @return the locale count, or 0 if not initialized
     */
    public int getLocaleCount() {
        return r18n != null ? r18n.getTranslationLoader().getLoadedLocales().size() : 0;
    }

    /**
     * Gets the translation metrics if enabled.
     *
     * @return the metrics instance, or null if not enabled or not initialized
     */
    @Nullable
    public TranslationMetrics getMetrics() {
        return r18n != null ? r18n.getMetrics() : null;
    }

    /**
     * Checks if file watcher is enabled.
     *
     * @return true if file watcher is enabled
     */
    public boolean isFileWatcherEnabled() {
        return enableFileWatcher;
    }

    /**
     * Checks if metrics are enabled.
     *
     * @return true if metrics are enabled
     */
    public boolean isMetricsEnabled() {
        return enableMetrics;
    }

    /**
     * Manually cleans up translation files for unsupported locales.
     * This removes files that are not in the supportedLocales configuration.
     */
    public void cleanupUnsupportedFiles() {
        if (r18n == null || !isInitialized()) {
            LOGGER.fine("Skipping cleanup - TranslationManager not yet initialized");
            return;
        }
        r18n.getTranslationLoader().cleanupUnsupportedFiles();
    }

    /**
     * Creates a builder for configuring the TranslationManager.
     *
     * @param plugin the plugin instance
     * @return a new builder instance
     */
    public static Builder builder(final @NotNull JavaPlugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Builder for creating TranslationManager instances with custom configuration.
     */
    public static class Builder {
        private final JavaPlugin plugin;
        private String defaultLocale = "en_US";
        private String[] supportedLocales = {
                "en_US", "en_GB", "de_DE", "es_ES", "fr_FR", "ja_JP",
                "ko_KR", "zh_CN", "zh_TW", "pt_BR", "ru_RU", "it_IT",
                "nl_NL", "pl_PL", "tr_TR", "sv_SE", "no_NO", "da_DK"
        };
        private boolean enableMetrics = false;
        private boolean enableFileWatcher = false;

        /**
         * Executes Builder.
         */
        public Builder(final @NotNull JavaPlugin plugin) {
            this.plugin = plugin;
        }

        /**
         * Executes defaultLocale.
         */
        public Builder defaultLocale(final @NotNull String locale) {
            this.defaultLocale = locale;
            return this;
        }

        /**
         * Executes supportedLocales.
         */
        public Builder supportedLocales(final @NotNull String... locales) {
            this.supportedLocales = locales;
            return this;
        }

        /**
         * Executes enableMetrics.
         */
        public Builder enableMetrics(final boolean enable) {
            this.enableMetrics = enable;
            return this;
        }

        /**
         * Executes enableFileWatcher.
         */
        public Builder enableFileWatcher(final boolean enable) {
            this.enableFileWatcher = enable;
            return this;
        }

        /**
         * Executes build.
         */
        public TranslationManager build() {
            return new TranslationManager(
                    plugin,
                    defaultLocale,
                    enableMetrics,
                    enableFileWatcher,
                    supportedLocales
            );
        }
    }
}
