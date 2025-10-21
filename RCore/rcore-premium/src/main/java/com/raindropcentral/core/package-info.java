/**
 * Premium runtime wiring for the RCore plugin.
 *
 * <p>Edition bootstrap:</p>
 * <ul>
 *     <li>{@link com.raindropcentral.core.RCorePremium} extends the Bukkit {@code JavaPlugin}
 *     entrypoint, initializes shaded dependencies through {@code JEDependency}, and reflectively
 *     creates {@link com.raindropcentral.core.RCorePremiumImpl} before delegating lifecycle
 *     callbacks.【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremium.java†L1-L41】</li>
 *     <li>{@code RCorePremiumImpl} inherits {@code AbstractPluginDelegate} to reuse the shared
 *     startup scaffolding and implements {@code RCoreBackend} so the common
 *     {@link com.raindropcentral.core.api.RCoreAdapter RCoreAdapter} can expose a single
 *     {@link com.raindropcentral.core.service.RCoreService} to all modules.【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremiumImpl.java†L1-L166】</li>
 * </ul>
 *
 * <p>Service extensions:</p>
 * <ul>
 *     <li>Premium wires the same asynchronous repositories as the free edition, but registers the
 *     adapter-backed {@code RCoreService} provider only after commands, repositories, and optional
 *     integrations finish loading so premium can safely override default behaviour through Bukkit's
 *     {@code ServicesManager}.【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremiumImpl.java†L64-L166】</li>
 *     <li>Because both editions publish the identical API surface, downstream consumers simply bind
 *     to {@code RCoreService}; whichever jar is installed decides whether enhanced premium features
 *     or the baseline implementation answers those calls.【F:RCore/rcore-premium/src/main/java/com/raindropcentral/core/RCorePremiumImpl.java†L173-L209】</li>
 * </ul>
 *
 * <p>Premium detection:</p>
 * <ul>
 *     <li>{@link com.raindropcentral.rplatform.RPlatform#detectPremiumVersion(Class, String)} flips a
 *     module-wide flag when the class loader exposes a bundled marker resource. Consumers should call
 *     this helper during bootstrap before consulting {@code RPlatform#isPremiumVersion()}.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java†L18-L143】</li>
 *     <li>The premium module currently does not invoke the detection hook or ship a marker file, so
 *     {@code isPremiumVersion()} remains {@code false} by default. Projects that rely on the flag
 *     must add the marker resource to the shaded jar and trigger detection explicitly.</li>
 * </ul>
 *
 * <p>Configuration &amp; packaging notes:</p>
 * <ul>
 *     <li>Premium jars declare {@code has-open-classloader: true} in {@code paper-plugin.yml} so the
 *     remapped dependency loader can inject runtime libraries, mirroring the free variant's
 *     behaviour.【F:RCore/rcore-premium/src/main/resources/paper-plugin.yml†L1-L24】</li>
 *     <li>No YAML or code-level flag toggles premium behaviour; shipping the premium jar with the
 *     required marker resource is the only differentiation from the free distribution.</li>
 * </ul>
 */
package com.raindropcentral.core;
