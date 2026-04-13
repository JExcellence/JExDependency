/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rda;

import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Delegate implementation for the RDA free edition.
 *
 * <p>This class owns the edition-specific bootstrap wiring and instantiates the shared
 * {@link RDA} runtime for the free edition.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RDAFreeImpl extends AbstractPluginDelegate<RDAFree> {

    private static final Logger LOGGER = Logger.getLogger(RDAFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private @Nullable RDA rda;

    /**
     * Creates a new free-edition delegate.
     *
     * @param plugin owning Bukkit plugin
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public RDAFreeImpl(final @NotNull RDAFree plugin) {
        super(plugin);
    }

    /**
     * Creates the shared RDA runtime for the free edition.
     */
    @Override
    public void onLoad() {
        try {
            this.rda = new RDA(this.getPlugin(), EDITION);
            this.rda.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDA (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Enables the shared runtime or disables the Bukkit plugin when initialization failed.
     */
    @Override
    public void onEnable() {
        if (this.rda == null) {
            LOGGER.severe("Cannot enable RDA (" + EDITION + ") because initialization failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }

        this.rda.onEnable();
    }

    /**
     * Shuts down the shared runtime.
     */
    @Override
    public void onDisable() {
        try {
            if (this.rda != null) {
                this.rda.onDisable();
            }
            LOGGER.info("RDA (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDA (" + EDITION + ") shutdown", exception);
        }
    }
}
