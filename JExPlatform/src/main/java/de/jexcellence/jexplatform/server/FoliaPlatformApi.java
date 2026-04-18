package de.jexcellence.jexplatform.server;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Folia platform API extending the Paper implementation.
 *
 * <p>Folia is a Paper fork, so all Adventure and item APIs behave identically.
 * This class exists as a distinct type for server-type discrimination in
 * {@code switch} expressions and pattern matching.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class FoliaPlatformApi extends PaperPlatformApi {

    /**
     * Creates a Folia API bound to the given plugin.
     *
     * @param plugin owning plugin
     */
    FoliaPlatformApi(@NotNull JavaPlugin plugin) {
        super(plugin);
    }
}
