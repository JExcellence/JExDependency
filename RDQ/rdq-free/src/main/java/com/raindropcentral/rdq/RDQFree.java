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

    @Override
    public void onEnable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onEnable();
        }
    }

    @Override
    public void onDisable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onDisable();
        }
    }

    public @NotNull RDQFreeImpl getImpl() {
        return this.rdqImpl;
    }
}