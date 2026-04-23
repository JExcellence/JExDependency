package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

/**
 * Public API surface for JExQuests. Third-party plugins retrieve a
 * concrete implementation via {@code Bukkit.getServicesManager()}.
 *
 * @since 1.0.0
 */
public interface JExQuestsAPI {

    /** Returns the edition name of the running JExQuests instance. */
    @NotNull String edition();

    /** Returns the JExQuests version currently loaded. */
    @NotNull String version();
}
