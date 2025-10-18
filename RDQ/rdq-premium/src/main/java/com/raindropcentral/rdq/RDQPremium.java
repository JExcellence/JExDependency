package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Main plugin class for RaindropQuests Premium Edition.
 * <p>
 * This class serves as the entry point for the Bukkit plugin system and delegates
 * all functionality to {@link RDQPremiumImpl}.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class RDQPremium extends JavaPlugin {

    private RDQPremiumImpl rdqImpl;

    public static @NotNull RDQPremium get() {
        return JavaPlugin.getPlugin(RDQPremium.class);
    }

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDQPremium.class);
            this.rdqImpl = new RDQPremiumImpl(this);
            this.rdqImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ Premium", exception);
            this.rdqImpl = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onEnable();
        } else {
            this.getLogger().severe("[RDQ] Cannot enable - RDQ Premium failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onDisable();
        }
    }

    public @NotNull RDQPremiumImpl getImpl() {
        return this.rdqImpl;
    }
}