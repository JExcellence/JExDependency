package com.raindropcentral.rplatform.translation;

import de.jexcellence.jextranslate.api.MessageFormatter;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.api.TranslationService;
import de.jexcellence.jextranslate.impl.LocaleResolverProvider;
import de.jexcellence.jextranslate.impl.MiniMessageFormatter;
import de.jexcellence.jextranslate.impl.YamlTranslationRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class TranslationManager {

    private final JavaPlugin plugin;
    private final TranslationRepository repository;
    private final Locale defaultLocale;

    public TranslationManager(final @NotNull JavaPlugin plugin) {
        this(plugin, Locale.ENGLISH);
    }

    public TranslationManager(final @NotNull JavaPlugin plugin, final @NotNull Locale defaultLocale) {
        this.plugin = plugin;
        this.defaultLocale = defaultLocale;
        this.repository = createRepository();
    }

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

    public void reload() {
        repository.reload().thenRun(() -> {
            TranslationService.clearLocaleCache();
            plugin.getLogger().info("Translations reloaded: " + 
                    repository.getAvailableLocales().size() + " locales, " +
                    repository.getAllAvailableKeys().size() + " keys");
        });
    }

    public boolean setPlayerLocale(final @NotNull Player player, final @NotNull Locale locale) {
        final var resolver = TranslationService.getConfiguration().localeResolver();
        final boolean success = resolver.setPlayerLocale(player, locale);
        
        if (success) {
            TranslationService.clearLocaleCache(player);
        }
        
        return success;
    }

    public @NotNull Locale getPlayerLocale(final @NotNull Player player) {
        final var resolver = TranslationService.getConfiguration().localeResolver();
        return resolver.resolveLocale(player).orElse(defaultLocale);
    }

    private @NotNull TranslationRepository createRepository() {
        final Path translationsDir = plugin.getDataFolder().toPath().resolve("translations");
        return YamlTranslationRepository.create(translationsDir, defaultLocale);
    }
}
