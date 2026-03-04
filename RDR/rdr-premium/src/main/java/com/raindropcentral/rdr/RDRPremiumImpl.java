/*
 * RDRPremiumImpl.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.raindropcentral.rdr.service.PremiumStorageService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Delegate implementation for the RDR premium edition.
 *
 * <p>This class owns the edition-specific bootstrap wiring and instantiates the shared
 * {@link RDR} runtime with the premium storage service.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class RDRPremiumImpl extends AbstractPluginDelegate<RDRPremium> {

    private static final Logger LOGGER = Logger.getLogger(RDRPremiumImpl.class.getName());
    private static final String EDITION = "Premium";

    private @Nullable RDR rdr;

    /**
     * Creates a new premium-edition delegate.
     *
     * @param plugin owning Bukkit plugin
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public RDRPremiumImpl(final @NotNull RDRPremium plugin) {
        super(plugin);
    }

    /**
     * Creates the shared RDR runtime for the premium edition.
     */
    @Override
    public void onLoad() {
        try {
            this.rdr = new RDR(this.getPlugin(), EDITION, new PremiumStorageService());
            this.rdr.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDR (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Enables the shared runtime or disables the Bukkit plugin when initialization failed.
     */
    @Override
    public void onEnable() {
        if (this.rdr == null) {
            LOGGER.severe("Cannot enable RDR (" + EDITION + ") because initialization failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }

        this.rdr.onEnable();
    }

    /**
     * Shuts down the shared runtime.
     */
    @Override
    public void onDisable() {
        try {
            if (this.rdr != null) {
                this.rdr.onDisable();
            }
            LOGGER.info("RDR (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDR (" + EDITION + ") shutdown", exception);
        }
    }
}
