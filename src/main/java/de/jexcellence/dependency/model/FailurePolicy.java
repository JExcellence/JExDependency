package de.jexcellence.dependency.model;

/**
 * Controls how the dependency manager reacts when one or more dependencies fail to download or inject.
 *
 * <ul>
 *     <li>{@link #STRICT} — treat any failure as fatal and throw a {@link de.jexcellence.dependency.exception.DependencyException},
 *         preventing the plugin from continuing its lifecycle.</li>
 *     <li>{@link #DEGRADED} — log failures but allow the plugin to start. The plugin author is expected to inspect
 *         the {@link ProcessingResult} and disable features that depend on missing artifacts.</li>
 *     <li>{@link #CALLBACK} — delegate the decision to a {@link FailureCallback} supplied by the plugin author.
 *         If no callback is set, this mode falls back to {@link #DEGRADED} behaviour.</li>
 * </ul>
 */
public enum FailurePolicy {

    /**
     * Abort plugin initialization when any dependency fails. A
     * {@link de.jexcellence.dependency.exception.DependencyException} is thrown with details about the
     * failed coordinates.
     */
    STRICT,

    /**
     * Report failures via logging and the {@link ProcessingResult} but allow the plugin to continue starting.
     * This is the default behaviour for backward compatibility.
     */
    DEGRADED,

    /**
     * Invoke a {@link FailureCallback} with the {@link ProcessingResult} and let the plugin author decide
     * how to react. Falls back to {@link #DEGRADED} when no callback is registered.
     */
    CALLBACK
}
