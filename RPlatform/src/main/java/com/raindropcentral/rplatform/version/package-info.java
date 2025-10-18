/**
 * Runtime helpers that detect the active server distribution and expose compatibility flags.
 *
 * <h2>Runtime behaviour mapping</h2>
 * <p>{@link ServerEnvironment} inspects the Bukkit runtime to classify Paper, Purpur, Folia, or Spigot
 * environments and exposes convenience checks such as {@link ServerEnvironment#isPaper()} and
 * {@link ServerEnvironment#isModern()}. Builders and views across the platform use these guards to enable
 * Paper-only item APIs and Folia-safe scheduling paths without duplicating reflection logic.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/version/ServerEnvironment.java†L1-L120】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/utility/itembuilder/AItemBuilder.java†L1-L118】【F:RPlatform/src/main/java/com/raindropcentral/rplatform/view/BaseView.java†L290-L310】</p>
 *
 * <h2>Premium gating</h2>
 * <p>Edition-specific features should pair environment checks with
 * {@link com.raindropcentral.rplatform.RPlatform#detectPremiumVersion(Class, String)} during bootstrap. The
 * detection hook flips the shared premium flag when a marker resource is present, while
 * {@link ServerEnvironment} ensures those toggles only activate on compatible server flavours—for example,
 * premium GUI embellishments that rely on Paper APIs must confirm both {@code isPremiumVersion()} and
 * {@code ServerEnvironment#getInstance().isPaper()} before initialising.【F:RPlatform/src/main/java/com/raindropcentral/rplatform/RPlatform.java†L90-L143】</p>
 *
 * <h2>Update coordination</h2>
 * <p>When Mojang or Paper introduce new package names, extend the detection logic and update logging so ops
 * teams can verify the reported environment during upgrades. Maintain unit coverage for version parsing to
 * avoid regressing premium toggles that depend on accurate classification.</p>
 */
package com.raindropcentral.rplatform.version;
