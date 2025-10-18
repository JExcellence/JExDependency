/**
 * Custom head definitions and categories.
 * <p>
 * Heads expose translation-friendly metadata via
 * {@link com.raindropcentral.rplatform.head.CustomHead#getTranslationKey()}.
 * Callers should defer creating head instances until the translation manager
 * from {@link com.raindropcentral.rplatform.localization.TranslationManager}
 * has been initialized by {@link com.raindropcentral.rplatform.RPlatform} so
 * that item display names resolve correctly. View implementations typically
 * render heads after metrics and placeholders are in place, which ensures
 * consistent analytics and text formatting.
 * </p>
 */
package com.raindropcentral.rplatform.head;
