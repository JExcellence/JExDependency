/**
 * Provides shared console tooling for Raindrop Central platform integrations.
 *
 * <p>Commands and UI helpers implemented in this package must:
 * <ul>
 *     <li>Use {@code JExTranslate} keys so every message inherits locale fallbacks and runtime overrides.</li>
 *     <li>Document command syntax for server operators and link usage examples with the platform command registry.</li>
 *     <li>Delegate outbound text through the translation bridge so third-party translation services remain in sync.</li>
 *     <li>Reuse {@link com.raindropcentral.rplatform.console.ConsoleMessenger} when logging translated diagnostics to
 *         ensure console output mirrors in-game prefix and placeholder formatting.</li>
 * </ul>
 *
 * <p>All console-facing prompts should mirror the phrasing, placeholders, and formatting published to in-game prompts so
 * administrators receive the same guidance as players regardless of delivery channel.</p>
 */
package com.raindropcentral.rplatform.console;
