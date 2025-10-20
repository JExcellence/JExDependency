/**
 * Translation service bootstrapping utilities and extension points.
 * <p>
 * This package documents the lifecycle expectations for translation features
 * exposed by {@link com.raindropcentral.rplatform.localization.TranslationManager}.
 * The manager is created during {@link com.raindropcentral.rplatform.RPlatform#initialize()}
 * alongside database resources inside the scheduler adapter's async executor.
 * Packages such as {@code view}, {@code head}, and {@code placeholder} rely on
 * that initialization to resolve {@link de.jexcellence.jextranslate.api.TranslationService}
 * lookups when rendering UI, item names, or placeholder text.
 * </p>
 * <p>
 * Reload logic is asynchronous; {@link com.raindropcentral.rplatform.localization.TranslationManager#reload()}
 * returns a {@link java.util.concurrent.CompletableFuture} that clears cached
 * locales after the YAML repository finishes reloading. Callers should chain
 * work on that future rather than assuming immediate availability.
 * </p>
 * <p>
 * Future translation classes belong here when they bridge additional services
 * onto the shared {@link de.jexcellence.jextranslate.api.TranslationService}
 * pipeline. Typical responsibilities include registering MiniMessage format
 * transformers, exposing domain specific translation keys, or coordinating
 * repository migrations. Implementations must avoid configuring the global
 * {@link de.jexcellence.jextranslate.api.TranslationService} directly—invoke
 * {@link com.raindropcentral.rplatform.localization.TranslationManager#initialize()}
 * to ensure the YAML repository and locale resolver are already in place before
 * performing any translation lookups.
 * </p>
 * <p>
 * Implementations that trigger translation reloads should defer to the
 * scheduler adapter to avoid blocking the primary thread, mirroring the
 * behavior in {@link com.raindropcentral.rplatform.localization.TranslationManager#reload()}.
 * Cache-sensitive utilities are expected to call
 * {@link de.jexcellence.jextranslate.api.TranslationService#clearLocaleCache()}
 * for players whose data they mutate so downstream UI refreshes pick up the new
 * strings.
 * </p>
 */
package com.raindropcentral.rplatform.translation;
