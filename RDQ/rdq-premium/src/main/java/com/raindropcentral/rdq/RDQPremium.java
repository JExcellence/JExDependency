package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Main plugin class for RDQ Premium Edition.
 *
 * <p>This class serves as the entry point for the Bukkit plugin system and delegates all
 * functionality to {@link RDQPremiumImpl}. The delegate handles the staged enable pipeline:
 * asynchronous platform and executor preparation (stage 1), component and view wiring (stage 2),
 * and repository hydration (stage 3) that provides database-backed services for commands,
 * views, and cross-plugin integrations.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class RDQPremium extends JavaPlugin {

    private RDQPremiumImpl impl;

    /**
     * Executes onLoad.
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDQPremium.class);
            impl = new RDQPremiumImpl(this);
            impl.onLoad();
        } catch (final Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load RDQ Premium", exception);
            impl = null;
        }
    }

    /**
     * Executes onEnable.
     */
    @Override
    public void onEnable() {
        if (impl != null) {
            impl.onEnable();
        } else {
            getLogger().severe("Cannot enable - RDQ failed to load");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Executes onDisable.
     */
    @Override
    public void onDisable() {
        if (impl != null) {
            impl.onDisable();
        }
    }

    /**
     * Gets impl.
     */
    public @NotNull RDQPremiumImpl getImpl() {
        return impl;
    }
}
