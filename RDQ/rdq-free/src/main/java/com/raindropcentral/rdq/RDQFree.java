package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Main plugin class for RaindropQuests Free Edition.
 * <p>
 * This class serves as the entry point for the Bukkit plugin system and delegates
 * all functionality to {@link RDQFreeImpl}.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class RDQFree extends JavaPlugin {

    private RDQFreeImpl rdqImpl;

    /**
     * Loads the free edition implementation and configures dependency remapping prior to enabling the plugin.
     * <p>
     * Any failure during initialization is logged and the implementation reference is cleared so the plugin does not
     * attempt to enable with a partial state.
     * </p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDQFree.class);
            this.rdqImpl = new RDQFreeImpl(this);
            this.rdqImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ Free", exception);
            this.rdqImpl = null;
        }
    }

    /**
     * Enables the free edition implementation if it was successfully loaded during {@link #onLoad()}.
     */
    @Override
    public void onEnable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onEnable();
        }
    }

    /**
     * Delegates to the free edition implementation to perform shutdown routines when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onDisable();
        }
    }

    /**
     * Retrieves the active free edition implementation that backs the plugin lifecycle events.
     * <p>
     * Callers should ensure {@link #onLoad()} completed without errors before invoking this method so that the returned
     * reference is available.
     * </p>
     *
     * @return the active free edition implementation instance
     */
    public @NotNull RDQFreeImpl getImpl() {
        return this.rdqImpl;
    }
}