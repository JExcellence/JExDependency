/**
 * Environment-specific implementations of {@link com.raindropcentral.rplatform.api.PlatformAPI PlatformAPI}.
 *
 * <h2>Selection flow</h2>
 * <p>{@link com.raindropcentral.rplatform.api.PlatformAPIFactory PlatformAPIFactory} detects the running server by
 * probing for Folia and modern Paper entry points before defaulting to Spigot. The factory then reflects the
 * matching implementation class in this package. Each constructor accepts the active {@link org.bukkit.plugin.java.JavaPlugin}
 * instance so lifecycle hooks can register listeners, metrics, and schedulers consistently across targets.</p>
 *
 * <h2>Fallback expectations</h2>
 * <p>If a preferred implementation fails to load (for example, because the class is missing from the shaded jar),
 * the factory logs the error and falls back to the next supported environment. Maintain deterministic fallbacks by
 * ensuring new adapters degrade gracefully—throwing only after the final Spigot implementation fails—so production
 * servers never boot without a usable API bridge.</p>
 *
 * <h2>Adding new environments</h2>
 * <p>When introducing support for another platform, create a sibling implementation here, add a {@link
 * com.raindropcentral.rplatform.api.PlatformType} constant, and extend the switch inside
 * {@link com.raindropcentral.rplatform.api.PlatformAPIFactory#create}. Keep capability behaviour aligned with the
 * existing implementations and update package documentation under {@code com.raindropcentral.rplatform.api} so
 * downstream teams know how the new adapter participates in the detection flow.</p>
 */
package com.raindropcentral.rplatform.api.impl;
