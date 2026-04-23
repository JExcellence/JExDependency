package de.jexcellence.core.api;

/**
 * Marker entry point for the JExCore public API. Third-party plugins retrieve
 * a concrete implementation through Bukkit's {@code ServicesManager}.
 *
 * @since 1.0.0
 */
public interface JExCoreAPI {

    /**
     * Returns the edition name of the running JExCore instance.
     *
     * @return {@code "Free"} or {@code "Premium"}
     */
    String edition();

    /**
     * Returns the JExCore version currently loaded.
     *
     * @return semantic version string
     */
    String version();
}
