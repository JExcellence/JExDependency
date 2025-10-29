package com.raindropcentral.rdq.manager.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.perk.runtime.DefaultPerkRegistry;
import com.raindropcentral.rdq.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq.perk.runtime.PerkRuntime;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationRepository;
import de.jexcellence.jextranslate.api.TranslationService;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Synchronizes perk translation keys with the active translation repository.
 *
 * <p>The synchronizer inspects every loaded perk runtime and ensures that the
 * translation files contain MiniMessage-friendly entries for both the display
 * name and description keys. Missing keys are appended to the underlying YAML
 * files using neutral defaults so that administrators can later customize the
 * output without running into lookup errors.</p>
 *
 * <p>This class intentionally performs no blocking I/O on asynchronous threads;
 * the underlying repository handles the necessary file operations. The
 * synchronizer simply delegates to {@link TranslationRepository#ensureTranslation}
 * and aggregates statistics for logging purposes.</p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 3.2.1
 */
public final class PerkTranslationSynchronizer {

    private final RDQ rdq;

    public PerkTranslationSynchronizer(final @NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    public void synchronize(final @NotNull DefaultPerkRegistry registry) {
        final var configuration = TranslationService.getConfiguration();
        if (configuration == null) {
            return;
        }

        final TranslationRepository repository = configuration.repository();
        final Set<Locale> locales = resolveTargetLocales(repository);
        int addedEntries = 0;

        for (PerkRuntime runtime : registry.getAllPerkRuntimes()) {
            final LoadedPerk loadedPerk = registry.get(runtime.getId());
            if (loadedPerk == null) {
                continue;
            }
            final String friendlyName = formatIdentifier(runtime.getId());
            final String nameKey = loadedPerk.getDisplayName();
            final String descriptionKey = loadedPerk.config().description();

            for (Locale locale : locales) {
                if (repository.ensureTranslation(locale, TranslationKey.of(nameKey), defaultNameValue(friendlyName))) {
                    addedEntries++;
                }
                if (descriptionKey != null && !descriptionKey.isBlank()
                        && repository.ensureTranslation(locale, TranslationKey.of(descriptionKey), defaultDescriptionValue(friendlyName))) {
                    addedEntries++;
                }
            }
        }

        if (addedEntries > 0) {
            rdq.getPlugin().getLogger().info("Generated " + addedEntries + " perk translation placeholders");
        }
    }

    private @NotNull Set<Locale> resolveTargetLocales(@NotNull TranslationRepository repository) {
        final Set<Locale> available = repository.getAvailableLocales();
        if (!available.isEmpty()) {
            return new LinkedHashSet<>(available);
        }
        return new LinkedHashSet<>(Set.of(repository.getDefaultLocale()));
    }

    private @NotNull String formatIdentifier(@NotNull String identifier) {
        return Arrays.stream(identifier.split("[._-]"))
                .filter(part -> !part.isBlank())
                .map(this::capitalize)
                .reduce((left, right) -> left + " " + right)
                .orElseGet(() -> capitalize(identifier));
    }

    private @NotNull String capitalize(@NotNull String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private @NotNull String defaultNameValue(@NotNull String friendlyName) {
        return "<yellow>" + friendlyName + "</yellow>";
    }

    private @NotNull String defaultDescriptionValue(@NotNull String friendlyName) {
        return "<gray>No description configured for <white>" + friendlyName + "</white>.</gray>";
    }
}
