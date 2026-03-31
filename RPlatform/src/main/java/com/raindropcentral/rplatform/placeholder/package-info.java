/**
 * PlaceholderAPI integration helpers.
 *
 * <p>The classes in this package expose a thin reflective bridge to the optional
 * PlaceholderAPI runtime. {@link com.raindropcentral.rplatform.placeholder.PlaceholderManager}
 * defers registration until {@link com.raindropcentral.rplatform.RPlatform#initialize()}
 * has completed so that the shared {@link com.raindropcentral.rplatform.localization.TranslationManager}
 * and scheduler adapter are ready to serve placeholder lookups. Registering
 * earlier would emit placeholders before translations are available or before
 * async tasks can be delegated safely.
 *
 * <p>When extending {@link com.raindropcentral.rplatform.placeholder.AbstractPlaceholderExpansion}
 * prefer retrieving localized strings via the translation service and keep any
 * expensive data fetches on the scheduler returned by
 * {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter} to avoid
 * blocking the main thread. Ensure expansions are unregistered during the
 * plugin shutdown path to mirror {@link com.raindropcentral.rplatform.RPlatform#shutdown()}.
 */
package com.raindropcentral.rplatform.placeholder;
