package com.raindropcentral.rdt;

import com.raindropcentral.rdt.service.FreeTownService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Delegate implementation for the RDT free edition.
 *
 * <p>This class wires the shared {@link RDT} runtime with free-edition behavior.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDTFreeImpl extends AbstractPluginDelegate<RDTFree> {

    private static final Logger LOGGER = Logger.getLogger(RDTFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private @Nullable RDT rdt;

    /**
     * Creates a new free-edition delegate.
     *
     * @param plugin owning Bukkit plugin
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public RDTFreeImpl(final @NotNull RDTFree plugin) {
        super(plugin);
    }

    /**
     * Creates the shared runtime for the free edition.
     */
    @Override
    public void onLoad() {
        try {
            this.rdt = new RDT(this.getPlugin(), EDITION, new FreeTownService());
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
