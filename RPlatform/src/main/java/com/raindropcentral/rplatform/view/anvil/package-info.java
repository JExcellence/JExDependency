/**
 * Inventory Framework anvil view support.
 * <p>
 * Custom anvil inputs wrap Inventory Framework's experimental APIs and should
 * only be registered once {@link com.raindropcentral.rplatform.RPlatform#initialize()}
 * has set up translations. Titles, prompts, and validation messages should be
 * sourced through {@link de.jexcellence.jextranslate.api.TranslationService}
 * just like standard views to ensure locale-aware UX.
 * </p>
 * <p>
 * Any heavy validation or completion logic triggered by
 * {@link com.raindropcentral.rplatform.view.anvil.CustomAnvilInput} must run on
 * the platform scheduler to keep the UI responsive. Use
 * {@link com.raindropcentral.rplatform.scheduler.ISchedulerAdapter#runAsync(Runnable)}
 * instead of blocking the Inventory Framework callback threads.
 * </p>
 */
package com.raindropcentral.rplatform.view.anvil;
