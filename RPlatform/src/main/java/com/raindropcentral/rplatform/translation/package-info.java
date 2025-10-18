/**
 * Translation service bootstrapping utilities.
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
 */
package com.raindropcentral.rplatform.translation;
