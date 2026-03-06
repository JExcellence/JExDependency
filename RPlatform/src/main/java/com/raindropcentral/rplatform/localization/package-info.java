/**
 * Provides translation and localization support using the R18nManager API.
 *
 * <p>The {@link com.raindropcentral.rplatform.localization.TranslationManager} wraps
 * {@link de.jexcellence.jextranslate.R18nManager} to provide a simplified interface
 * for plugin translation management. Initialize during plugin startup and use
 * <code>I18n.of(String, Player)</code> for sending translated messages.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     private TranslationManager translations;
 *
 *     @Override
 *     public void onEnable() {
 *         translations = new TranslationManager(this);
 *         translations.initialize().thenRun(() -> {
 *             getLogger().info("Translations loaded!");
 *         });
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         if (translations != null) {
 *             translations.shutdown();
 *         }
 *     }
 *
 *     public void greetPlayer(Player player) {
 *         I18n.of("welcome.message", player)
 *             .with("player", player.getName())
 *             .withPrefix()
 *             .send();
 *     }
 * }
 * }</pre>
 *
 * @see com.raindropcentral.rplatform.localization.TranslationManager
 * @see de.jexcellence.jextranslate.R18nManager
 */
package com.raindropcentral.rplatform.localization;
