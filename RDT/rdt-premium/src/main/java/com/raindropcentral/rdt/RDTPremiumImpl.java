package com.raindropcentral.rdt;

import com.raindropcentral.rdt.service.PremiumTownService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Delegate implementation for the RDT premium edition.
 *
 * <p>This class wires the shared {@link RDT} runtime with premium-edition behavior.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDTPremiumImpl extends AbstractPluginDelegate<RDTPremium> {

    private static final Logger LOGGER = Logger.getLogger(RDTPremiumImpl.class.getName());
    private static final String EDITION = "Premium";

    private @Nullable RDT rdt;

    /**
     * Creates a new premium-edition delegate.
     *
     * @param plugin owning Bukkit plugin
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public RDTPremiumImpl(final @NotNull RDTPremium plugin) {
        super(plugin);
    }

    /**
     * Creates the shared runtime for the premium edition.
     */
    @Override
    public void onLoad() {
        try {
            this.rdt = new RDT(this.getPlugin(), EDITION, new PremiumTownService());
            this.rdt.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDT (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Enables the shared runtime or disables the Bukkit plugin when initialization failed.
     */
    @Override
    public void onEnable() {
        if (this.rdt == null) {
            LOGGER.severe("Cannot enable RDT (" + EDITION + ") because initialization failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }

        this.rdt.onEnable();
    }

    /**
     * Shuts down the shared runtime.
     */
    @Override
    public void onDisable() {
        try {
            if (this.rdt != null) {
                this.rdt.onDisable();
            }
            LOGGER.info("RDT (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDT (" + EDITION + ") shutdown", exception);
        }
    }
}
