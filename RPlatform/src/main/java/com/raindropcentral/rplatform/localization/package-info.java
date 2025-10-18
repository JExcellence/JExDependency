/**
 * Glue components that bridge RPlatform services with the JExTranslate runtime.
 *
 * <p>The {@link com.raindropcentral.rplatform.localization.TranslationManager} bootstraps
 * {@link de.jexcellence.jextranslate.api.TranslationService} with a YAML-backed repository,
 * a MiniMessage formatter, and the auto-detecting locale resolver supplied by
 * {@link de.jexcellence.jextranslate.impl.LocaleResolverProvider}. Authors must
 * call {@link TranslationManager#initialize()} (indirectly triggered by
 * {@link com.raindropcentral.rplatform.RPlatform#initialize()}) before any
 * translation APIs are used so that {@link de.jexcellence.jextranslate.api.TranslationService#configure}
 * completes successfully.</p>
 *
 * <p>Locale resolution follows a strict fallback order:
 * explicit overrides supplied to {@link de.jexcellence.jextranslate.api.TranslationService#create}
 * or {@code createFresh(...)} &rarr; the resolved player locale from the active locale resolver
 * &rarr; the service default configured by {@link TranslationManager}. This mirrors the
 * expectations documented in {@code JExTranslate/AGENTS.md} and should be reflected in
 * any command or UI surfaces.</p>
 *
 * <p>Locale caches are scoped per player and are invalidated automatically when
 * {@link TranslationManager#reload()} completes or when a player's locale is
 * changed through {@link TranslationManager#setPlayerLocale}. Plugin authors must also
 * trigger {@link de.jexcellence.jextranslate.api.TranslationService#clearLocaleCache()}
 * whenever repository contents, formatter implementations, or resolver strategies change,
 * such as during plugin reloads or hot-swap testing.</p>
 *
 * <p>All outbound messages should be routed through
 * {@link de.jexcellence.jextranslate.api.TranslationService#create} so that MiniMessage
 * formatting, prefix handling, and placeholder chaining remain consistent. Avoid sending
 * raw strings—construct {@code TranslationService} instances via the fluent API and rely on
 * {@link com.raindropcentral.rplatform.localization.TranslationManager#getPlayerLocale(org.bukkit.entity.Player)}
 * when a manual locale lookup is required before selecting an override. Builders returned by
 * the service expose {@code with(...)} and {@code withPrefix()} for placeholder injection and
 * prefix control.</p>
 *
 * <p>Shared resources live under {@code &lt;plugin data folder&gt;/translations}. The translation manager
 * provisions this directory and loads YAML bundles via
 * {@link de.jexcellence.jextranslate.impl.YamlTranslationRepository}, ensuring that default
 * locale files shipped inside the plugin jar are copied to disk. Repository reloads will
 * pick up disk edits and refresh the locale cache automatically.</p>
 */
package com.raindropcentral.rplatform.localization;
