/**
 * Defines the runtime exceptions emitted by JExDependency during dependency bootstrap.
 *
 * <p>The dependency pipeline groups failures into three categories so callers can choose
 * the appropriate recovery strategy:
 * </p>
 *
 * <ul>
 *     <li><strong>Configuration failures</strong> cover malformed YAML inputs and invalid
 *     dependency coordinates. These scenarios surface as {@code DependencyException}
 *     instances and are accompanied by warning logs from the YAML loader (for example,
 *     messages such as {@code "Failed to load dependencies from"} or
 *     {@code "Error parsing YAML dependencies"}). Callers should treat them as
 *     actionable misconfigurations—propagate the warning to server owners and abort the
 *     initialization until the configuration is corrected rather than retrying. The
 *     loader emits detailed diagnostics via {@link java.util.logging.Logger} at
 *     {@link java.util.logging.Level#WARNING}, enabling operators to locate the faulty
 *     resource quickly.
 *     </li>
 *     <li><strong>Download failures</strong> occur when remote artifacts cannot be fetched
 *     or validated. {@link DownloadException} is paired with downloader warnings such as
 *     {@code "Failed to download from any repository"} or HTTP status messages. Plugins
 *     should surface these failures to the console, optionally perform a single retry if
 *     connectivity was transient, and then halt dependency usage because the required
 *     libraries were not written to disk.</li>
 *     <li><strong>Remapping failures</strong> indicate that the optional remapping
 *     pipeline did not complete. The bootstrapper logs phrases like
 *     {@code "Remapping initialization failed - falling back"} before raising a
 *     {@code DependencyException}. Callers are expected to continue startup with
 *     unremapped dependencies only when that fallback is acceptable; otherwise they
 *     should fail fast to avoid classpath conflicts.</li>
 * </ul>
 *
 * <p>Each exception aligns with the module's structured logging so consumers can rely on
 * predictable messages when triaging failures.</p>
 */
package de.jexcellence.dependency.exception;
